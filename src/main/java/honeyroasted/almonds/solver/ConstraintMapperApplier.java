package honeyroasted.almonds.solver;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintLeaf;
import honeyroasted.almonds.ConstraintNode;
import honeyroasted.almonds.ConstraintTree;
import honeyroasted.collect.property.PropertySet;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class ConstraintMapperApplier implements ConstraintMapper {
    private List<ConstraintMapper> mappers;

    public ConstraintMapperApplier(List<ConstraintMapper> mappers) {
        this.mappers = mappers;
    }

    @Override
    public int arity() {
        return -1;
    }

    @Override
    public boolean filter(PropertySet context, ConstraintNode node) {
        return true;
    }

    @Override
    public boolean accepts(PropertySet context, ConstraintNode... nodes) {
        return true;
    }

    @Override
    public void process(PropertySet context, ConstraintNode... nodes) {
        ConstraintTree tree = new ConstraintTree(Constraint.solve().tracked(), ConstraintNode.Operation.AND);
        Stream.of(nodes).forEach(cn -> tree.attach(cn.copy()));

        ConstraintNode original = tree.copy().disjunctiveForm().flattenedForm();
        ConstraintNode replacement = process(tree, new PropertySet().inheritUnique(context));

        if (!original.structuralEquals(replacement)) {
            context.attach(new ReplaceBranch(replacement));
        }
    }

    public record ReplaceBranch(ConstraintNode replacement) {

    }

    public record DiscardBranch(boolean value) {

    }

    public record RestartProcessing(boolean value) {

    }

    public ConstraintNode process(ConstraintNode node) {
        return this.process(node, new PropertySet());
    }

    public ConstraintNode process(ConstraintNode node, PropertySet context) {
        ConstraintNode previous;
        ConstraintNode current = node.disjunctiveForm().flattenedForm().collapseConstraints();

        do {
            previous = current.copy();

            for (ConstraintMapper mapper : this.mappers) {
                boolean restart = false;
                if (current instanceof ConstraintTree tree) { //Should always be true due to disjunctive form
                    Set<ConstraintNode> children = new LinkedHashSet<>(tree.children());
                    for (ConstraintNode child : children) {
                        PropertySet branchContext = new PropertySet().inheritFrom(context);

                        if (child instanceof ConstraintLeaf leaf) {
                            consume(leaf, List.of(leaf), branchContext, mapper);
                        } else if (child instanceof ConstraintTree childTree) {
                            consume(childTree, childTree.children(), branchContext, mapper);
                        }

                        if (branchContext.has(DiscardBranch.class) && branchContext.first(DiscardBranch.class).get().value()) {
                            tree.detach(child);
                            restart = true;
                            break;
                        } else if (branchContext.has(ReplaceBranch.class)) {
                            ConstraintNode replacement = branchContext.first(ReplaceBranch.class).get().replacement().copy();
                            tree.detach(child).attach(replacement);
                            restart = true;
                            break;
                        } else if (branchContext.has(RestartProcessing.class) && branchContext.first(RestartProcessing.class).get().value) {
                            restart = true;
                            break;
                        }
                    }
                }

                current = current.disjunctiveForm().flattenedForm().collapseConstraints();
                if (restart) break;
            }
        } while (!ConstraintNode.structural().equals(previous, current));

        return current.copy();
    }

    private static boolean shouldRestart(PropertySet context) {
        return context.has(ReplaceBranch.class) ||
                (context.has(DiscardBranch.class) && context.first(DiscardBranch.class).get().value()) ||
                (context.has(RestartProcessing.class) && context.first(RestartProcessing.class).get().value);
    }

    private static void consume(ConstraintNode parent, Collection<ConstraintNode> processing, PropertySet context, ConstraintMapper mapper) {
        if (mapper.arity() == ConstraintMapper.PARENT_BRANCH_NODE) {
            if (mapper.filter(context, parent) && mapper.accepts(context, parent)) {
                mapper.process(context, parent);
            }
        } else {
            consumeSubsets(processing.stream().filter(cn -> mapper.filter(context, cn)).toList(), mapper.arity(), mapper.commutative(), arr -> {
                if (mapper.accepts(context, arr)) {
                    mapper.process(context, arr);
                }
            }, ConstraintNode.class, () -> shouldRestart(context));
        }
    }

    private static <T> void consumeSubsets(List<T> processing, int size, boolean commutative, Consumer<T[]> baseCase, Class<T> component, Supplier<Boolean> breaker) {
        if (!breaker.get()) {
            if (size <= 0 || size == processing.size()) {
                baseCase.accept(processing.toArray(i -> (T[]) Array.newInstance(component, i)));
            } else if (size < processing.size()) {
                T[] mem = (T[]) Array.newInstance(component, size);
                T[] input = processing.toArray(i -> (T[]) Array.newInstance(component, i));
                int[] subset = IntStream.range(0, size).toArray();

                consumeSubset(mem, input, subset, commutative, baseCase, breaker);
                while (true) {
                    int i;
                    for (i = size - 1; i >= 0 && subset[i] == input.length - size + i; i--) ;
                    if (i < 0) break;

                    subset[i]++;
                    for (++i; i < size; i++) {
                        subset[i] = subset[i - 1] + 1;
                    }
                    consumeSubset(mem, input, subset, commutative, baseCase, breaker);
                }
            }
        }
    }

    private static <T> void consumeSubset(T[] mem, T[] input, int[] subset, boolean commutative, Consumer<T[]> baseCase, Supplier<Boolean> breaker) {
        if (!breaker.get()) {
            if (commutative) {
                copyMem(mem, input, subset);
                baseCase.accept(mem);
            } else {
                permuteAndConsumeSubset(mem, input, subset, 0, subset.length - 1, baseCase, breaker);
            }
        }
    }

    private static <T> void permuteAndConsumeSubset(T[] mem, T[] input, int[] subset, int l, int h, Consumer<T[]> baseCase, Supplier<Boolean> breaker) {
        if (!breaker.get()) {
            if (l == h) {
                copyMem(mem, input, subset);
                baseCase.accept(mem);
            } else {
                for (int i = l; i <= h && breaker.get(); i++) {
                    swap(subset, l, i);
                    permuteAndConsumeSubset(mem, input, subset, l + 1, h, baseCase, breaker);
                    swap(subset, l, i);
                }
            }
        }
    }

    private static void swap(int nums[], int l, int i) {
        int temp = nums[l];
        nums[l] = nums[i];
        nums[i] = temp;
    }


    private static <T> void copyMem(T[] mem, T[] input, int[] subset) {
        for (int i = 0; i < subset.length; i++) {
            mem[i] = input[subset[i]];
        }
    }
}
