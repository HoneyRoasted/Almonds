package honeyroasted.almonds.solver;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintLeaf;
import honeyroasted.almonds.ConstraintNode;
import honeyroasted.almonds.ConstraintTree;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
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
    public boolean filter(ConstraintNode node) {
        return true;
    }

    @Override
    public boolean accepts(ConstraintNode... nodes) {
        return true;
    }

    @Override
    public void process(Context context, ConstraintNode... nodes) {
        ConstraintTree and = new ConstraintTree(Constraint.and().tracked(), ConstraintNode.Operation.AND);
        Stream.of(nodes).forEach(cn -> and.attach(cn.copy()));
        process(and, new Context().inheritProperties(context));
        context.attach(ConstraintMapper.REPLACE_BRANCH, and);
    }

    public ConstraintNode process(ConstraintNode node) {
        return this.process(node, new Context());
    }

    public ConstraintNode process(ConstraintNode node, Context context) {
        ConstraintNode previous = node.copy();
        ConstraintNode current = previous.disjunctiveForm().flattenedForm();

        do {
            previous = current.copy();

            for (ConstraintMapper mapper : this.mappers) {
                if (current instanceof ConstraintTree tree) {
                    Set<ConstraintNode> children = new LinkedHashSet<>(tree.children());

                    for (ConstraintNode child : children) {
                        if (child instanceof ConstraintLeaf leaf) {
                            consume(List.of(leaf), context, mapper);
                        } else if (child instanceof ConstraintTree childTree) {
                            consume(childTree.children(), context, mapper);
                        }

                        if (context.hasProperty(DISCARD_BRANCH)) {
                            tree.detach(child);
                        } else if (context.hasProperty(REPLACE_BRANCH)) {
                            ConstraintNode replacement = context.property(REPLACE_BRANCH);
                            tree.detach(child).attach(replacement.copy());
                        }
                    }
                } else if (current instanceof ConstraintLeaf leaf) {
                    consume(List.of(leaf), context, mapper);

                    if (context.hasProperty(DISCARD_BRANCH)) {
                        leaf.setStatus(ConstraintNode.Status.FALSE);
                    } else if (context.hasProperty(REPLACE_BRANCH)) {
                        ConstraintNode replacement = context.property(REPLACE_BRANCH);
                        current = replacement.copy();
                    }
                }

                context.remove(DISCARD_BRANCH);
                context.remove(REPLACE_BRANCH);

                current.updateConstraints();
                current = current.disjunctiveForm().flattenedForm();
            }


        } while (!ConstraintNode.structural().equals(previous, current));

        return current;
    }

    private static void consume(Collection<ConstraintNode> processing, ConstraintMapper.Context context, ConstraintMapper mapper) {
        consumeSubsets(processing.stream().filter(mapper::filter).toList(), mapper.arity(), mapper.commutative(), arr -> {
            if (mapper.accepts(arr)) {
                mapper.process(context, arr);
            }
        }, ConstraintNode.class);
    }

    private static <T> void consumeSubsets(List<T> processing, int size, boolean commutative, Consumer<T[]> baseCase, Class<T> component) {
        if (size <= 0 || size == processing.size()) {
            baseCase.accept(processing.toArray(i -> (T[]) Array.newInstance(component, i)));
        } else if (size < processing.size()) {
            T[] mem = (T[]) Array.newInstance(component, size);
            T[] input = processing.toArray(i -> (T[]) Array.newInstance(component, i));
            int[] subset = IntStream.range(0, size).toArray();

            consumeSubset(mem, input, subset, commutative, baseCase);
            while (true) {
                int i;
                for (i = size - 1; i >= 0 && subset[i] == input.length - size + i; i--) ;
                if (i < 0) break;

                subset[i]++;
                for (++i; i < size; i++) {
                    subset[i] = subset[i - 1] + 1;
                }
                consumeSubset(mem, input, subset, commutative, baseCase);
            }
        }
    }

    private static <T> void consumeSubset(T[] mem, T[] input, int[] subset, boolean commutative, Consumer<T[]> baseCase) {
        if (commutative) {
            copyMem(mem, input, subset);
            baseCase.accept(mem);
        } else {
            permuteAndConsumeSubset(mem, input, subset, 0, subset.length - 1, baseCase);
        }
    }

    private static <T> void permuteAndConsumeSubset(T[] mem, T[] input, int[] subset, int l, int h, Consumer<T[]> baseCase) {
        if (l == h) {
            copyMem(mem, input, subset);
            baseCase.accept(mem);
        } else {
            for (int i = l; i <= h; i++) {
                swap(subset, l, i);
                permuteAndConsumeSubset(mem, input, subset, l + 1, h, baseCase);
                swap(subset, l, i);
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
