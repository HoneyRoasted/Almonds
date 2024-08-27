package honeyroasted.almonds;

import honeyroasted.collect.equivalence.Equivalence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

public final class ConstraintLeaf implements ConstraintNode {
    public static final Equivalence<ConstraintLeaf> STRUCTURAL = new Equivalence<>() {
        @Override
        protected boolean doEquals(ConstraintLeaf left, ConstraintLeaf right) {
            return left.status == right.status &&
                    Objects.equals(left.constraint, right.constraint);
        }

        @Override
        protected int doHashCode(ConstraintLeaf val) {
            return Objects.hash(val.status, val.constraint);
        }
    };

    private ConstraintTree parent;
    private Status status;

    private TrackedConstraint constraint;

    public ConstraintLeaf(ConstraintTree parent, TrackedConstraint constraint, Status status) {
        this.parent = parent;
        this.constraint = constraint;
        this.status = status;
    }

    public ConstraintLeaf(TrackedConstraint constraint, Status status) {
        this(null, constraint, status);
    }

    public ConstraintLeaf(TrackedConstraint constraint) {
        this(constraint, Status.UNKNOWN);
    }

    @Override
    public ConstraintTree expand(Operation operation, Collection<? extends ConstraintNode> newChildren) {
        ConstraintTree tree = new ConstraintTree(this.constraint, operation);
        if (this.parent != null) {
            this.parent.detach(this).attach(tree);
        }
        tree.attach(newChildren);
        return tree;
    }

    @Override
    public ConstraintTree expandInPlace(Operation defaultOp) {
        return this.expand(defaultOp, Collections.emptySet());
    }

    @Override
    public ConstraintLeaf collapse() {
        return this;
    }

    @Override
    public void visit(Predicate<ConstraintNode> test, Predicate<ConstraintTree> snipper, Consumer<ConstraintNode> action) {
        if (test.test(this)) {
            action.accept(this);
        }
    }

    @Override
    public ConstraintNode flattenedForm() {
        return this.copy();
    }

    @Override
    public ConstraintNode disjunctiveForm() {
        ConstraintTree or = new ConstraintTree(this.constraint, Operation.OR);
        ConstraintTree and = new ConstraintTree(this.constraint, Operation.AND);
        or.attach(and);
        and.attach(this.copy());
        return or;
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public boolean isLeaf() {
        return true;
    }

    @Override
    public Status status() {
        return this.status;
    }

    @Override
    public Status strictStatus() {
        return this.status;
    }

    @Override
    public ConstraintLeaf overrideStatus(Status status) {
        this.setStatus(status);
        return this;
    }

    @Override
    public Set<ConstraintNode> children() {
        return Collections.emptySet();
    }

    @Override
    public ConstraintTree parent() {
        return this.parent;
    }

    @Override
    public ConstraintLeaf setParent(ConstraintTree parent) {
        this.parent = parent;
        return this;
    }

    @Override
    public TrackedConstraint trackedConstraint() {
        return this.constraint;
    }

    @Override
    public Set<ConstraintLeaf> leaves() {
        return Set.of(this);
    }

    public ConstraintLeaf setStatus(Status status) {
        this.status = status;
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

    @Override
    public String toEquationString() {
        return this.constraint().simpleName();
    }

    public void toString(List<String> building, boolean useSimpleName) {
        building.add("Condition: " + (useSimpleName ? this.constraint().simpleName() : this.constraint().toString()));
        building.add("Satisfied: " + this.status);
    }

    @Override
    public ConstraintLeaf copy(Void context) {
        return new ConstraintLeaf(this.constraint, this.status);
    }
}
