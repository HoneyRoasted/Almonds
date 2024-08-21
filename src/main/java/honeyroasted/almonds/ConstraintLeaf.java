package honeyroasted.almonds;

import honeyroasted.collect.equivalence.Equivalence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

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
    public ConstraintTree expand(Operation operation, Collection<ConstraintNode> newChildren) {
        ConstraintTree tree = new ConstraintTree(this.constraint, operation);
        this.parent.attach(tree)
                .detach(this);
        tree.attach(newChildren);
        return tree;
    }

    @Override
    public ConstraintLeaf collapse() {
        return this;
    }

    @Override
    public ConstraintNode flattenedForm() {
        return this.copy();
    }

    @Override
    public ConstraintNode disjunctiveForm() {
        return this.copy();
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
    public void overrideStatus(Status status) {
        this.setStatus(status);
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
    public void setParent(ConstraintTree parent) {
        this.parent = parent;
    }

    @Override
    public TrackedConstraint trackedConstraint() {
        return this.constraint;
    }

    @Override
    public void updateConstraints() {
        this.constraint.setSuccess(this.status.value());
    }

    public ConstraintLeaf setStatus(Status status) {
        this.status = status;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConstraintLeaf leaf = (ConstraintLeaf) o;
        return Objects.equals(constraint, leaf.constraint);
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
        return new ConstraintLeaf(null, this.constraint, this.status);
    }
}
