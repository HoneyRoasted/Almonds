package honeyroasted.almonds.solver;

import honeyroasted.almonds.ConstraintNode;
import honeyroasted.almonds.ConstraintTree;
import honeyroasted.almonds.TrackedConstraint;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class SolveResult {
    private ConstraintNode constraintTree;
    private Set<Branch> branches;
    private Set<Branch> validBranches;
    private Set<Branch> invalidBranches;

    public SolveResult(ConstraintNode constraintTree) {
        this.constraintTree = constraintTree.disjunctiveForm().flattenedForm();
    }

    public boolean success() {
        return this.constraintTree.satisfied();
    }

    public Set<Branch> branches() {
        if (this.branches == null) {
            this.branches = new LinkedHashSet<>();
            if (this.constraintTree instanceof ConstraintTree tree) {
                tree.children().forEach(cn -> this.branches.add(new Branch(cn)));
            } else {
                this.branches.add(new Branch(this.constraintTree));
            }
        }
        return this.branches;
    }

    public Set<Branch> validBranches() {
        if (this.validBranches == null) {
            this.validBranches = this.branches().stream().filter(b -> b.branchRoot().satisfied()).collect(Collectors.toCollection(LinkedHashSet::new));
        }
        return this.validBranches;
    }

    public Set<Branch> invalidBranches() {
        if (this.invalidBranches == null) {
            this.invalidBranches = this.branches().stream().filter(b -> !b.branchRoot().satisfied()).collect(Collectors.toCollection(LinkedHashSet::new));
        }
        return this.invalidBranches;
    }

    public ConstraintNode tree() {
        return this.constraintTree;
    }

    public static class Branch {
        private ConstraintNode branch;

        private Set<TrackedConstraint> allBranch;

        public Branch(ConstraintNode branch) {
            this.branch = branch;
        }

        public ConstraintNode branchRoot() {
            return this.branch;
        }

        public Set<TrackedConstraint> all() {
            if (this.allBranch == null) {
                this.allBranch = new LinkedHashSet<>();
                this.branch.visit(cn -> true, cn -> allImpl(cn.trackedConstraint(), this.allBranch));
            }
            return this.allBranch;
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
            return "--------- Branch ---------\n" +
                    this.branch.toString(useSimpleName) + "\n" +
                    "\n--------- Tracked Constraint Tree ---------\n" +
                    this.branch.trackedConstraint().toString(useSimpleName);
        }
    }

    @Override
    public String toString() {
        return this.toString(false);
    }

    public String toString(boolean useSimpleName) {
        StringBuilder sb = new StringBuilder();
        sb.append("=============== Solve Result ===============").append("\n")
                .append("Tree Nodes: ").append(constraintTree.size()).append("\n")
                .append("Tracked Constraint Nodes: ").append(this.validBranches().stream().mapToInt(br -> br.all().stream().mapToInt(TrackedConstraint::size).sum()).sum()).append("\n")
                .append("Success: ").append(this.constraintTree.status().asBoolean()).append("\n")
                .append("Branches: ").append(this.branches().size()).append(" total, ").append(this.validBranches().size()).append(" valid\n")
                .append("#################### Valid Branches ####################\n")
                .append(this.validBranches().stream().map(br -> br.toString(useSimpleName)).collect(Collectors.joining("\n")))
                .append("#################### Other Branches ####################\n")
                .append(this.invalidBranches().stream().map(br -> br.toString(useSimpleName)).collect(Collectors.joining("\n")));
        return sb.toString();
    }

}
