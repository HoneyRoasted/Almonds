package honeyroasted.almonds;

import honeyroasted.collect.copy.Copyable;
import honeyroasted.collect.equivalence.Equivalence;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class TrackedConstraint implements Copyable<TrackedConstraint, Void> {
    public static final Equivalence<TrackedConstraint> STRUCTURAL = new Equivalence<>() {
        @Override
        protected boolean doEquals(TrackedConstraint left, TrackedConstraint right) {
            return Objects.equals(left.constraint, right.constraint) && left.parents.size() == right.parents.size() && setEquals(left.children, right.children);
        }

        @Override
        protected int doHashCode(TrackedConstraint val) {
            return Objects.hash(val.constraint, setHash(val.children), val.parents.size());
        }
    };

    private Constraint constraint;
    private Set<TrackedConstraint> parents;
    private Set<TrackedConstraint> children;

    public TrackedConstraint(Constraint constraint, Set<TrackedConstraint> parents, Set<TrackedConstraint> children) {
        this.constraint = constraint;
        this.parents = parents;
        this.children = children;
    }

    public TrackedConstraint(Constraint constraint) {
        this(constraint, new LinkedHashSet<>(), new LinkedHashSet<>());
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

    public TrackedConstraint collapse() {
        if (!this.children.isEmpty() && this.children.stream().allMatch(tr -> tr.constraint.equals(this.constraint))) {
            return this.children.iterator().next().collapse();
        } else {
            TrackedConstraint copy = new TrackedConstraint(this.constraint);
            this.children.forEach(tr -> {
                if (!tr.constraint.equals(copy.constraint)) {
                    TrackedConstraint collapsed = tr.collapse();
                    copy.addChildren(tr.collapse());
                    collapsed.addParents(copy);
                }
            });
            return copy;
        }
    }

    public int size() {
        return 1 + this.children.stream().mapToInt(TrackedConstraint::size).sum();
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

    public Set<TrackedConstraint> parents() {
        return this.parents;
    }

    public TrackedConstraint addParents(TrackedConstraint... parents) {
        Collections.addAll(this.parents, parents);
        return this;
    }

    public TrackedConstraint setParents(Set<TrackedConstraint> parents) {
        this.parents = parents;
        return this;
    }

    public Set<TrackedConstraint> children() {
        return this.children;
    }

    public TrackedConstraint addChildren(TrackedConstraint... children) {
        Collections.addAll(this.children, children);
        return this;
    }

    public TrackedConstraint setChildren(Set<TrackedConstraint> children) {
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

    @Override
    public TrackedConstraint copy(Void context) {
        return new TrackedConstraint(this.constraint)
                .addChildren(this.children().stream().map(TrackedConstraint::copy).toArray(TrackedConstraint[]::new));
    }
}
