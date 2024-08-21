package honeyroasted.almonds;

import honeyroasted.collect.equivalence.Equivalence;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class TrackedConstraint {
    public static final Equivalence<TrackedConstraint> STRUCTURAL = new Equivalence<>() {
        @Override
        protected boolean doEquals(TrackedConstraint left, TrackedConstraint right) {
            return left.success == right.success && Objects.equals(left.constraint, right.constraint) && left.parents.size() == right.parents.size() && listEquals(left.children, right.children);
        }

        @Override
        protected int doHashCode(TrackedConstraint val) {
            return Objects.hash(val.constraint, val.success, listHash(val.children), val.parents.size());
        }
    };

    private Constraint constraint;
    private boolean success;

    private List<TrackedConstraint> parents;
    private List<TrackedConstraint> children;

    public TrackedConstraint(Constraint constraint, boolean success, List<TrackedConstraint> parents, List<TrackedConstraint> children) {
        this.constraint = constraint;
        this.success = success;
        this.parents = parents;
        this.children = children;
    }

    public TrackedConstraint(Constraint constraint) {
        this(constraint, false, new ArrayList<>(), new ArrayList<>());
    }

    public static TrackedConstraint of(Constraint constraint) {
        return new TrackedConstraint(constraint);
    }

    public static TrackedConstraint of(Constraint constraint, TrackedConstraint... originators) {
        TrackedConstraint tr = new TrackedConstraint(constraint);
        for (TrackedConstraint originator : originators) {
            tr.addParents(originator);
            originator.addChildren(tr);
        }
        return tr;
    }

    public ConstraintLeaf createLeaf() {
        return new ConstraintLeaf(this);
    }

    public ConstraintTree createTree(ConstraintNode.Operation operation) {
        return new ConstraintTree(this, operation);
    }

    public Constraint constraint() {
        return this.constraint;
    }

    public TrackedConstraint setConstraint(Constraint constraint) {
        this.constraint = constraint;
        return this;
    }

    public boolean success() {
        return this.success;
    }

    public TrackedConstraint setSuccess(boolean success) {
        this.success = success;
        return this;
    }

    public List<TrackedConstraint> parents() {
        return this.parents;
    }

    public TrackedConstraint addParents(TrackedConstraint... parents) {
        Collections.addAll(this.parents, parents);
        return this;
    }

    public TrackedConstraint setParents(List<TrackedConstraint> parents) {
        this.parents = parents;
        return this;
    }

    public List<TrackedConstraint> children() {
        return this.children;
    }

    public TrackedConstraint addChildren(TrackedConstraint... children) {
        Collections.addAll(this.children, children);
        return this;
    }

    public TrackedConstraint setChildren(List<TrackedConstraint> children) {
        this.children = children;
        return this;
    }

    public boolean structuralEquals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TrackedConstraint that = (TrackedConstraint) o;
        return STRUCTURAL.equals(this, that);
    }

    public int structuralHashCode() {
        return STRUCTURAL.hashCode(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TrackedConstraint that = (TrackedConstraint) o;
        return Objects.equals(constraint, that.constraint);
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
        building.add("Satisfied: " + this.success);

        if (!this.children.isEmpty()) {
            building.add("Children: " + this.children.size());

            List<String> children = new ArrayList<>();
            Iterator<TrackedConstraint> iterator = this.children().iterator();
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
}
