package honeyroasted.almonds.solver;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintNode;
import honeyroasted.almonds.TrackedConstraint;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class SolveResult {
    private ConstraintNode constraintTree;
    private List<TrackedConstraint> constraints;

    private Set<TrackedConstraint> parents;
    private Set<TrackedConstraint> all;
    private Set<TrackedConstraint> leaves;

    private Map<Predicate<TrackedConstraint>, Set<TrackedConstraint>> filterCache = new IdentityHashMap<>();
    private Map<Predicate<TrackedConstraint>, Set<TrackedConstraint>> filterParentsCache = new IdentityHashMap<>();

    public SolveResult(ConstraintNode constraintTree, List<TrackedConstraint> constraints) {
        this.constraintTree = constraintTree;
        this.constraints = constraints;
    }
    
    public boolean success() {
        return this.constraintTree.satisfied();
    }

    public boolean satisfied(Constraint constraint) {
        return this.all().stream().anyMatch(tr -> tr.success() && tr.constraint().equals(constraint));
    }

    public Set<TrackedConstraint> parents() {
        if (this.parents == null) {
            Set<TrackedConstraint> parents = Collections.newSetFromMap(new IdentityHashMap<>());
            this.constraints.forEach(tr -> parentsImpl(tr, parents));
            this.parents = Collections.unmodifiableSet(parents);
        }
        return this.parents;
    }

    private void parentsImpl(TrackedConstraint constraint, Set<TrackedConstraint> building) {
        if (constraint.parents().isEmpty()) {
            building.add(constraint);
        } else {
            constraint.parents().forEach(tr -> parentsImpl(tr, building));
        }
    }

    public Set<TrackedConstraint> leaves() {
        if (this.leaves == null) {
            Set<TrackedConstraint> leaves = Collections.newSetFromMap(new IdentityHashMap<>());
            this.constraints.forEach(tr -> leavesImpl(tr, leaves));
            this.leaves = Collections.unmodifiableSet(leaves);
        }
        return this.leaves;
    }

    private void leavesImpl(TrackedConstraint constraint, Set<TrackedConstraint> building) {
        if (constraint.children().isEmpty()) {
            building.add(constraint);
        } else {
            constraint.children().forEach(tr -> leavesImpl(tr, building));
        }
    }

    public Set<TrackedConstraint> all() {
        if (this.all == null) {
            Set<TrackedConstraint> all = Collections.newSetFromMap(new IdentityHashMap<>());
            this.parents().forEach(tr -> allImpl(tr, all));
            this.all = Collections.unmodifiableSet(all);
        }
        return this.all;
    }

    private void allImpl(TrackedConstraint constraint, Set<TrackedConstraint> building) {
        building.add(constraint);
        constraint.children().forEach(tr -> allImpl(constraint, building));
    }

    public Set<TrackedConstraint> filter(Predicate<TrackedConstraint> fn) {
        return filterCache.computeIfAbsent(fn, k -> Collections.unmodifiableSet(this.all().stream().filter(fn).collect(Collectors.toCollection(() -> Collections.newSetFromMap(new IdentityHashMap<>())))));
    }

    public Set<TrackedConstraint> filterParents(Predicate<TrackedConstraint> fn) {
        return filterParentsCache.computeIfAbsent(fn, k -> Collections.unmodifiableSet(this.parents().stream().filter(fn).collect(Collectors.toCollection(() -> Collections.newSetFromMap(new IdentityHashMap<>())))));
    }

    public Set<TrackedConstraint> satisfied() {
        return this.filterParents(TrackedConstraint::success);
    }

    public Set<TrackedConstraint> unsatisfied() {
        return this.filterParents(tr -> !tr.success());
    }

    public Set<TrackedConstraint> allSatisfied() {
        return this.filter(TrackedConstraint::success);
    }

    public Set<TrackedConstraint> allUnsatisfied() {
        return this.filter(tr -> !tr.success());
    }

    @Override
    public String toString() {
        return this.toString(false);
    }

    public String toString(boolean useSimpleName) {
        StringBuilder sb = new StringBuilder();
        sb.append("============= Solve Result, Tree Nodes: ").append(constraintTree.size())
                .append(", Constraints: ").append(parents().size()).append("=============\n")
                .append("SUCCESS: ").append(this.constraintTree.status().asBoolean()).append("\n")
                .append("TRACKED CONSTRAINTS:\n");

        this.parents().forEach(tr -> sb.append(tr.toString(useSimpleName)).append("\n"));

        sb.append("\n")
                .append("CONSTRAINT TREE:\n")
                .append(constraintTree.toString(useSimpleName));


        return sb.toString();
    }

}
