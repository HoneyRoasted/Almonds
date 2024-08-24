package honeyroasted.almonds.solver;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintNode;
import honeyroasted.almonds.ConstraintTree;
import honeyroasted.almonds.TrackedConstraint;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class SolveResult {
    private ConstraintNode constraintTree;
    private List<TrackedConstraint> constraints;

    private Set<TrackedConstraint> parents;
    private Set<TrackedConstraint> all;

    public SolveResult(ConstraintNode constraintTree, List<TrackedConstraint> constraints) {
        this.constraintTree = constraintTree.disjunctiveForm().flattenedForm();
        this.constraints = constraints;
    }

    public boolean success() {
        return this.constraintTree.satisfied();
    }

    public Set<ConstraintNode> validBranches() {
        if (this.constraintTree instanceof ConstraintTree tree) {
            return tree.children().stream().filter(ConstraintNode::satisfied).collect(Collectors.toCollection(LinkedHashSet::new));
        }

        return Set.of(this.constraintTree);
    }

    public boolean satisfied(Constraint constraint) {
        return this.constraintTree.stream().anyMatch(cn -> cn.constraint().equals(constraint) && cn.satisfied());
    }

    public Set<TrackedConstraint> parentTrackedConstraints() {
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

    public Set<TrackedConstraint> allTrackedConstraints() {
        if (this.all == null) {
            Set<TrackedConstraint> all = Collections.newSetFromMap(new IdentityHashMap<>());
            this.parentTrackedConstraints().forEach(tr -> allImpl(tr, all));
            this.all = Collections.unmodifiableSet(all);
        }
        return this.all;
    }

    private void allImpl(TrackedConstraint constraint, Set<TrackedConstraint> building) {
        building.add(constraint);
        constraint.children().forEach(tr -> allImpl(tr, building));
    }

    @Override
    public String toString() {
        return this.toString(false);
    }

    public String toString(boolean useSimpleName) {
        StringBuilder sb = new StringBuilder();
        sb.append("============= Solve Result, Tree Nodes: ").append(constraintTree.size())
                .append(", Constraints: ").append(parentTrackedConstraints().size())
                .append(", Success: ").append(this.constraintTree.status().asBoolean()).append(" =============\n")
                .append("########## CONSTRAINT TREE ##########\n")
                .append(constraintTree.toString(useSimpleName))
                .append("\n");

        sb.append("########## TRACKED CONSTRAINTS ##########\n");
        this.parentTrackedConstraints().forEach(tr -> sb.append(tr.toString(useSimpleName)).append("\n"));


        return sb.toString();
    }

}
