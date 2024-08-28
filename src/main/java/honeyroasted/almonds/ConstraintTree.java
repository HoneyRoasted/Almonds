package honeyroasted.almonds;

import honeyroasted.collect.equivalence.Equivalence;
import honeyroasted.collect.multi.Pair;
import honeyroasted.collect.property.PropertySet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class ConstraintTree implements ConstraintNode {
    public static final Equivalence<ConstraintTree> STRUCTURAL = new Equivalence<>() {
        @Override
        protected boolean doEquals(ConstraintTree left, ConstraintTree right) {
            return left.operation == right.operation &&
                    Objects.equals(left.constraint, right.constraint) &&
                    ConstraintNode.structural().setEquals(left.children.keySet(), right.children.keySet());
        }

        @Override
        protected int doHashCode(ConstraintTree val) {
            return Objects.hash(val.operation, val.constraint.hashCode(),
                    ConstraintNode.structural().setHash(val.children.keySet()));
        }
    };

    private ConstraintTree parent;
    private Constraint constraint;
    private ConstraintNode.Operation operation;

    private Map<ConstraintNode, ConstraintNode> children;
    private PropertySet metadata = new PropertySet();

    public ConstraintTree(ConstraintTree parent, Constraint constraint, Operation operation, Map<ConstraintNode, ConstraintNode> children) {
        this.parent = parent;
        this.constraint = constraint;
        this.operation = operation;
        this.children = children;
    }

    public ConstraintTree(ConstraintTree parent, Constraint constraint, Operation operation) {
        this(parent, constraint, operation, new LinkedHashMap<>());
    }

    public ConstraintTree(Constraint constraint, Operation operation, Map<ConstraintNode, ConstraintNode> children) {
        this(null, constraint, operation, children);
    }

    public ConstraintTree(Constraint constraint, Operation operation) {
        this(null, constraint, operation, new LinkedHashMap<>());
    }

    public ConstraintTree preserve() {
        return this.attach(Constraint.preserved(this.constraint)
                .createLeaf().setStatus(Status.INFORMATION));
    }

    public ConstraintTree attach(Constraint... constraints) {
        for (Constraint con : constraints) {
            this.attach(con.createLeaf());
        }
        return this;
    }

    public ConstraintTree attach(ConstraintNode... nodes) {
        for (ConstraintNode node : nodes) {
            ConstraintNode existing = this.children.get(node);
            if (existing == null) {
                node.setParent(this);
                this.children.put(node, node);
            } else {
                existing.metadata().inheritFrom(node.metadata());
            }
        }
        return this;
    }

    public ConstraintTree attach(Collection<? extends ConstraintNode> nodes) {
        for (ConstraintNode node : nodes) {
            ConstraintNode existing = this.children.get(node);
            if (existing == null) {
                node.setParent(this);
                this.children.put(node, node);
            } else {
                existing.metadata().inheritFrom(node.metadata());
            }
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

    public ConstraintLeaf createLeaf(Constraint constraint, Status status) {
        ConstraintLeaf leaf = new ConstraintLeaf(constraint, status);
        this.attach(leaf);
        return leaf;
    }

    public ConstraintLeaf createLeaf(Constraint constraint) {
        return this.createLeaf(constraint, Status.UNKNOWN);
    }

    public ConstraintTree createTree(Constraint constraint, Operation operation) {
        ConstraintTree tree = new ConstraintTree(constraint, operation);
        this.attach(tree);
        return tree;
    }

    @Override
    public Status status() {
        if (this.children.isEmpty()) {
            return Status.TRUE;
        } else {
            Iterator<ConstraintNode> iterator = this.children.keySet().iterator();
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
            Iterator<ConstraintNode> iterator = this.children.keySet().iterator();
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
        this.children.forEach((cn, v) -> cn.overrideStatus(status));
        return this;
    }

    @Override
    public Set<ConstraintNode> children() {
        return this.children.keySet();
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
    public Constraint constraint() {
        return this.constraint;
    }

    @Override
    public Set<ConstraintLeaf> leaves() {
        Set<ConstraintLeaf> leaves = new LinkedHashSet<>();
        this.children().forEach(cn -> leaves.addAll(cn.leaves()));
        return leaves;
    }

    @Override
    public ConstraintTree expand(Operation operation, Collection<? extends ConstraintNode> newChildren, boolean preserve) {
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

                    ConstraintTree subTree = Constraint.multi(Operation.AND).createTree(Operation.AND);
                    subTree.attach(subChildren);
                    if (preserve) {
                        subTree.attach(Constraint.preserved(this.constraint).createLeaf().setStatus(Status.INFORMATION));
                    }

                    expand.attach(subTree);
                });
            } else if (this.operation == Operation.OR) {
                this.children().forEach(cn -> {
                    Set<ConstraintNode> subChildren = newChildren.stream().map(ConstraintNode::copy).collect(Collectors.toCollection(LinkedHashSet::new));
                    subChildren.add(cn.copy());

                    ConstraintTree subTree = Constraint.multi(Operation.OR).createTree(Operation.OR);
                    subTree.attach(subChildren);
                    if (preserve) {
                        subTree.attach(Constraint.preserved(this.constraint).createLeaf().setStatus(Status.INFORMATION));
                    }

                    expand.attach(subTree);
                });
            }
            return expand;
        }
    }

    @Override
    public ConstraintTree expandInPlace(Operation defaultOp, boolean preserve) {
        if (preserve) {
            this.preserve();
        }
        return this;
    }

    @Override
    public ConstraintLeaf collapse() {
        return new ConstraintLeaf(this.parent, this.constraint, this.status());
    }

    @Override
    public PropertySet metadata() {
        return this.metadata;
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
        flat.metadata().copyFrom(this.metadata);

        this.children.forEach((cn, v) -> {
            if (cn instanceof ConstraintTree tree) {
                ConstraintNode flatChild = tree.flattenedForm();
                if (tree.operation() == this.operation && flatChild instanceof ConstraintTree childTree) {
                    flat.attach(childTree.children());
                    flat.metadata().inheritFrom(childTree.metadata());
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
        or.metadata().copyFrom(this.metadata);

        if (this.operation == Operation.OR) {
            this.children().forEach(cn -> {
                ConstraintNode disjunct = cn.disjunctiveForm();
                if (disjunct instanceof ConstraintTree tree && tree.operation == Operation.OR) {
                    or.metadata().inheritFrom(tree.metadata());
                    or.attach(tree.children());
                } else {
                    or.attach(disjunct);
                }
            });
        } else if (this.operation == Operation.AND) {
            Set<ConstraintNode> children = this.children().stream().map(ConstraintNode::disjunctiveForm).collect(Collectors.toCollection(LinkedHashSet::new));

            List<List<Pair<ConstraintNode, PropertySet>>> building = new ArrayList<>();
            for (ConstraintNode child : children) {
                if (child instanceof ConstraintTree dnf && dnf.operation == Operation.OR) {
                    building.add(dnf.children().stream().map(cn -> Pair.of(cn, dnf.metadata())).toList());
                } else {
                    building.add(List.of(Pair.of(child, new PropertySet())));
                }
            }

            cartesianProduct(building, 0, new ArrayList<>(), products -> {
                ConstraintTree parent = new ConstraintTree(Constraint.multi(Operation.AND, products.stream().map(p -> p.left().constraint()).toList()), Operation.AND).attach(products.stream().map(p -> p.left().copy()).toList());
                parent.metadata().copyFrom(this.metadata);
                products.forEach(p -> parent.metadata().inheritFrom(p.right()));
                or.attach(parent);
            });
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
        return 1 + this.children.keySet().stream().mapToInt(ConstraintNode::size).sum();
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
        return Objects.equals(constraint, node.constraint());
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
        return "(" + this.children.keySet().stream().map(ConstraintNode::toEquationString).collect(Collectors.joining(" " + this.operation.operator() + " ")) + ")";
    }

    @Override
    public ConstraintTree copy(Void context) {
        ConstraintTree copy = new ConstraintTree(this.constraint, this.operation);
        copy.metadata().copyFrom(this.metadata);
        this.children.forEach((cn, v) -> copy.attach(cn.copy()));
        return copy;
    }
}
