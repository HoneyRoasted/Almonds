package honeyroasted.almonds.solver;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintNode;
import honeyroasted.almonds.ConstraintTree;
import honeyroasted.almonds.TrackedConstraint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ConstraintSolver {
    private List<ConstraintMapperApplier> appliers;

    public ConstraintSolver(List<ConstraintMapperApplier> appliers) {
        this.appliers = appliers;
    }

    private List<Constraint> constraints = new ArrayList<>();

    public ConstraintSolver bind(Constraint... constraints) {
        Collections.addAll(this.constraints, constraints);
        return this;
    }

    public ConstraintSolver reset() {
        this.constraints.clear();
        return this;
    }

    public SolveResult solve() {
        return this.solve(new ConstraintMapper.Context());
    }

    public SolveResult solve(ConstraintMapper.Context context) {
        List<TrackedConstraint> trackedConstraints = this.constraints.stream().map(Constraint::tracked).toList();

        ConstraintTree root = new ConstraintTree(Constraint.and().tracked(), ConstraintNode.Operation.AND);
        trackedConstraints.forEach(tr -> root.attach(tr.createLeaf()));

        ConstraintNode current = root;
        for (ConstraintMapperApplier applier : this.appliers) {
            current = applier.process(current, new ConstraintMapper.Context().inheritProperties(context));
        }

        return new SolveResult(current, trackedConstraints);
    }

}
