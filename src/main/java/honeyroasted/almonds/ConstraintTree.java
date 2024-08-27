package honeyroasted.almonds;

import honeyroasted.collect.equivalence.Equivalence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class ConstraintTree implements ConstraintNode {
    public static final Equivalence<ConstraintTree> STRUCTURAL = new Equivalence<>() {
        @Override
        protected boolean doEquals(ConstraintTree left, ConstraintTree right) {
            return left.operation == right.operation &&
                    Objects.equals(left.constraint, right.constraint) &&
                    ConstraintNode.structural().setEquals(left.children, right.children);
        }

        @Override
        protected int doHashCode(ConstraintTree val) {
            return Objects.hash(val.operation, val.constraint.hashCode(),
                    ConstraintNode.structural().setHash(val.children));
        }
    };

    private ConstraintTree parent;
    private TrackedConstraint constraint;
    private ConstraintNode.Operation operation;

    private Set<ConstraintNode> children;

    public ConstraintTree(ConstraintTree parent, TrackedConstraint constraint, Operation operation, Set<ConstraintNode> children) {
        this.parent = parent;
        this.constraint = constraint;
        this.operation = operation;
        this.children = children;
    }

    public ConstraintTree(ConstraintTree parent, TrackedConstraint constraint, Operation operation) {
        this(parent, constraint, operation, new LinkedHashSet<>());
    }

    public ConstraintTree(TrackedConstraint constraint, Operation operation, Set<ConstraintNode> children) {
        this(null, constraint, operation, children);
    }

    public ConstraintTree(TrackedConstraint constraint, Operation operation) {
        this(null, constraint, operation, new LinkedHashSet<>());
    }

    public ConstraintTree attach(Constraint... constraints) {
        for (Constraint con : constraints) {
            this.attach(con.tracked(this.constraint).createLeaf());
        }
        return this;
    }

    public ConstraintTree attach(TrackedConstraint... constraints) {
        for (TrackedConstraint con : constraints) {
            this.attach(con.createLeaf());
        }
        return this;
    }

    public ConstraintTree attach(ConstraintNode... nodes) {
        for (ConstraintNode node : nodes) {
            node.setParent(this);
            this.children.add(node);
        }
        return this;
    }

    public ConstraintTree attach(Collection<? extends ConstraintNode> nodes) {
        for (ConstraintNode node : nodes) {
            node.setParent(this);
            this.children.add(node);
        }
        return this;
    }

    public ConstraintTree detach(ConstraintNode... nodes) {
        for (ConstraintNode node : nodes) {
            if (node.parent() == this) {
                node.setParent(null);
                this.children.remove(node);
            }
        }
        return this;
    }

    public ConstraintTree detach(Collection<? extends ConstraintNode> nodes) {
        for (ConstraintNode node : nodes) {
            if (node.parent() == this) {
                node.setParent(null);
                this.children.remove(node);
            }
        }
        return this;
    }

    public ConstraintLeaf createLeaf(TrackedConstraint constraint, Status status) {
        ConstraintLeaf leaf = new ConstraintLeaf(constraint, status);
        this.attach(leaf);
        return leaf;
    }

    public ConstraintLeaf createLeaf(TrackedConstraint constraint) {
        return this.createLeaf(constraint, Status.UNKNOWN);
    }

    public ConstraintTree createTree(TrackedConstraint constraint, Operation operation) {
        ConstraintTree tree = new ConstraintTree(constraint, operation);
        this.attach(tree);
        return tree;
    }

    @Override
    public Status status() {
        if (this.children.isEmpty()) {
            return Status.TRUE;
        } else {
            Iterator<ConstraintNode> iterator = this.children.iterator();
            Status curr = iterator.next().status();
            while (iterator.hasNext()) {
                Status next = iterator.next().status();
                if (this.operation == Operation.AND) {
                    curr = curr.and(next);
                } else {
                    curr = curr.or(next);
                }
            }

            return curr;
        }
    }

    @Override
    public Status strictStatus() {
        Status status;
        if (this.children.isEmpty()) {
            status = Status.UNKNOWN;
        } else {
            Iterator<ConstraintNode> iterator = this.children.iterator();
            Status curr = iterator.next().status();
            while (iterator.hasNext()) {
                Status next = iterator.next().strictStatus();
                if (this.operation == Operation.AND) {
                    curr = curr.and(next);
                } else {
                    curr = curr.or(next);
                }
            }

            status = curr;
        }

        return (operation == Operation.AND && status == Status.TRUE) ? Status.UNKNOWN :
                (operation == Operation.OR && status == Status.FALSE) ? Status.UNKNOWN :
                        status;
    }

    @Override
    public ConstraintTree overrideStatus(Status status) {
        this.children.forEach(cn -> cn.overrideStatus(status));
        return this;
    }

    @Override
    public Set<ConstraintNode> children() {
        return this.children;
    }

    @Override
    public ConstraintTree parent() {
        return this.parent;
    }

    @Override
    public ConstraintTree setParent(ConstraintTree parent) {
        this.parent = parent;
        return this;
    }

    @Override
    public TrackedConstraint trackedConstraint() {
        return this.constraint;
    }

    @Override
    public Set<ConstraintLeaf> leaves() {
        Set<ConstraintLeaf> leaves = new LinkedHashSet<>();
        this.children().forEach(cn -> leaves.addAll(cn.leaves()));
        return leaves;
    }

    @Override
    public ConstraintTree expand(Operation operation, Collection<? extends ConstraintNode> newChildren) {
        if (operation == this.operation) {
            this.attach(newChildren);
            return this;
        } else {
            ConstraintTree expand = new ConstraintTree(this.constraint, operation);
            if (this.parent != null) {
                this.parent.detach(this).attach(expand);
            }

            if (this.operation == Operation.AND) {
                newChildren.forEach(cn -> {
                    Set<ConstraintNode> subChildren = this.children().stream().map(ConstraintNode::copy).collect(Collectors.toCollection(LinkedHashSet::new));
                    subChildren.add(cn.copy());

                    ConstraintTree subTree = Constraint.multi(Operation.AND).tracked(this.constraint).createTree(Operation.AND);
                    subTree.attach(subChildren);

                    expand.attach(subTree);
                });
            } else if (this.operation == Operation.OR) {
                this.children().forEach(cn -> {
                    Set<ConstraintNode> subChildren = newChildren.stream().map(ConstraintNode::copy).collect(Collectors.toCollection(LinkedHashSet::new));
                    subChildren.add(cn.copy());

                    ConstraintTree subTree = Constraint.multi(Operation.OR).tracked(this.constraint).createTree(Operation.OR);
                    subTree.attach(subChildren);

                    expand.attach(subTree);
                });
            }
            return expand;
        }
    }

    @Override
    public ConstraintTree expandInPlace(Operation defaultOp) {
        return this;
    }

    @Override
    public ConstraintLeaf collapse() {
        return new ConstraintLeaf(this.parent, this.constraint, this.status());
    }

    @Override
    public void visit(Predicate<ConstraintNode> test, Predicate<ConstraintTree> snipper, Consumer<ConstraintNode> action) {
        if (test.test(this)) {
            action.accept(this);
        }

        if (!snipper.test(this)) {
            this.children().forEach(cn -> cn.visit(test, snipper, action));
        }
    }

    @Override
    public ConstraintNode flattenedForm() {
        ConstraintTree flat = new ConstraintTree(this.constraint, this.operation);

        this.children.forEach(cn -> {
            if (cn instanceof ConstraintTree tree) {
                ConstraintNode flatChild = tree.flattenedForm();
                if (tree.operation() == this.operation && flatChild instanceof ConstraintTree childTree) {
                    flat.attach(childTree.children());
                } else {
                    flat.attach(flatChild);
                }
            } else {
                flat.attach(cn.copy());
            }
        });

        return flat;
    }

    @Override
    public ConstraintNode disjunctiveForm() {
        ConstraintTree or = new ConstraintTree(this.constraint, Operation.OR);

        if (this.operation == Operation.OR) {
            this.children().forEach(cn -> {
                ConstraintNode disjunct = cn.disjunctiveForm();
                if (disjunct instanceof ConstraintTree tree && tree.operation == Operation.OR) {
                    or.attach(tree.children());
                } else {
                    or.attach(disjunct);
                }
            });
        } else if (this.operation == Operation.AND) {
            Set<ConstraintNode> children = this.children().stream().map(ConstraintNode::disjunctiveForm).collect(Collectors.toCollection(LinkedHashSet::new));

            List<List<ConstraintNode>> building = new ArrayList<>();
            for (ConstraintNode child : children) {
                if (child instanceof ConstraintTree dnf && dnf.operation == Operation.OR) {
                    building.add(new ArrayList<>(dnf.children()));
                } else {
                    building.add(List.of(child));
                }
            }

            cartesianProduct(building, 0, new ArrayList<>(), products ->
                    or.attach(new ConstraintTree(Constraint.multi(Operation.AND, products.stream().map(ConstraintNode::constraint).toList())
                            .tracked(Stream.concat(Stream.of(this), products.stream()).map(ConstraintNode::trackedConstraint).toArray(TrackedConstraint[]::new)),
                            Operation.AND).attach(products.stream().map(ConstraintNode::copy).toList())));
        }

        return or;
    }

    private static <T> void cartesianProduct(List<List<T>> sets, int index, List<T> current, Consumer<List<T>> result) {
        if (index == sets.size()) {
            result.accept(current);
            return;
        }
        List<T> currentSet = sets.get(index);
        for (T element : currentSet) {
            current.add(element);
            cartesianProduct(sets, index + 1, current, result);
            current.remove(current.size() - 1);
        }
    }

    @Override
    public int size() {
        return 1 + this.children.stream().mapToInt(ConstraintNode::size).sum();
    }

    @Override
    public boolean isLeaf() {
        return false;
    }

    public Operation operation() {
        return this.operation;
    }

    public ConstraintTree setOperation(Operation operation) {
        this.operation = operation;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ConstraintNode)) return false;
        ConstraintNode node = (ConstraintNode) o;
        return Objects.equals(constraint, node.trackedConstraint());
    }

    @Override
    public int hashCode() {
        return Objects.hash(constraint);
    }

    @Override
    public String toString() {
        return this.toString(false);
    }

    public String toString(boolean useSimpleName) {
        List<String> building = new ArrayList<>();
        this.toString(building, useSimpleName);
        return String.join("\n", building);
    }

    public void toString(List<String> building, boolean useSimpleName) {
        building.add("Condition: " + (useSimpleName ? this.constraint().simpleName() : this.constraint().toString()));
        building.add("Satisfied: " + this.status());

        if (!this.children.isEmpty()) {
            building.add("Operation: " + this.operation);
            building.add("Children: " + this.children.size());

            List<String> children = new ArrayList<>();
            Iterator<ConstraintNode> iterator = this.children().iterator();
            while (iterator.hasNext()) {
                iterator.next().toString(children, useSimpleName);
                if (iterator.hasNext()) {
                    children.add("");
                }
            }

            int maxLen = children.stream().mapToInt(String::length).max().getAsInt();
            String content = "-".repeat(maxLen + 8);
            String top = "+" + content + "+";
            building.add(top);
            for (String c : children) {
                building.add("|    " + c + (" ".repeat(maxLen - c.length() + 4)) + "|");
            }
            building.add(top);
        }
    }

    public String toEquationString() {
        return "(" + this.children.stream().map(ConstraintNode::toEquationString).collect(Collectors.joining(" " + this.operation.operator() + " ")) + ")";
    }

    @Override
    public ConstraintTree copy() {
        ConstraintTree copy = new ConstraintTree(this.constraint, this.operation);
        this.children.forEach(cn -> copy.attach(cn.copy()));
        return copy;
    }

    @Override
    public ConstraintTree copy(Void context) {
        ConstraintTree copy = new ConstraintTree(this.constraint, this.operation);
        this.children.forEach(cn -> copy.attach(cn.copy()));
        return copy;
    }
}
