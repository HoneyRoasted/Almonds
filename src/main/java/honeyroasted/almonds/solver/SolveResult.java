package honeyroasted.almonds.solver;

import honeyroasted.almonds.ConstraintNode;
import honeyroasted.almonds.ConstraintTree;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class SolveResult {
    private ConstraintNode constraintTree;
    private Set<ConstraintNode> branches;
    private Set<ConstraintNode> validBranches;
    private Set<ConstraintNode> invalidBranches;

    public SolveResult(ConstraintNode constraintTree) {
        this.constraintTree = constraintTree.disjunctiveForm().flattenedForm();
    }

    public boolean success() {
        return this.constraintTree.satisfied();
    }

    public Set<ConstraintNode> branches() {
        if (this.branches == null) {
            this.branches = new LinkedHashSet<>();
            if (this.constraintTree instanceof ConstraintTree tree) {
                this.branches.addAll(tree.children());
            } else {
                this.branches.add(this.constraintTree);
            }
        }
        return this.branches;
    }

    public Set<ConstraintNode> validBranches() {
        if (this.validBranches == null) {
            this.validBranches = this.branches().stream().filter(ConstraintNode::satisfied).collect(Collectors.toCollection(LinkedHashSet::new));
        }
        return this.validBranches;
    }

    public Set<ConstraintNode> invalidBranches() {
        if (this.invalidBranches == null) {
            this.invalidBranches = this.branches().stream().filter(b -> !b.satisfied()).collect(Collectors.toCollection(LinkedHashSet::new));
        }
        return this.invalidBranches;
    }

    public ConstraintNode tree() {
        return this.constraintTree;
    }

    @Override
    public String toString() {
        return this.toString(false);
    }

    public String toString(boolean useSimpleName) {
        StringBuilder sb = new StringBuilder();
        sb.append("=============== Solve Result ===============").append("\n")
                .append("Tree Nodes: ").append(constraintTree.size()).append("\n")
                .append("Success: ").append(this.constraintTree.status().asBoolean()).append("\n")
                .append("Branches: ").append(this.branches().size()).append(" total, ").append(this.validBranches().size()).append(" valid\n")
                .append("#################### Valid Branches ####################\n")
                .append(this.validBranches().stream().map(br -> br.toString(useSimpleName)).collect(Collectors.joining("\n\n")))
                .append("\n#################### Other Branches ####################\n")
                .append(this.invalidBranches().stream().map(br -> br.toString(useSimpleName)).collect(Collectors.joining("\n")));
        return sb.toString();
    }

}
