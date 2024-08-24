package honeyroasted.almonds.solver;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintNode;
import honeyroasted.almonds.ConstraintTree;
import honeyroasted.almonds.TrackedConstraint;
import honeyroasted.collect.property.PropertySet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ConstraintSolver {
    private List<ConstraintMapperApplier> appliers;
    private PropertySet context = new PropertySet();

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
        this.context.remove(Object.class);
        return this;
    }

    public ConstraintSolver withContext(PropertySet context) {
        this.context.inheritFrom(context);
        return this;
    }

    public SolveResult solve() {
        return this.solve(new PropertySet());
    }

    public SolveResult solve(PropertySet context) {
        List<TrackedConstraint> trackedConstraints = this.constraints.stream().map(Constraint::tracked).toList();

        ConstraintTree root = new ConstraintTree(Constraint.solve().tracked(), ConstraintNode.Operation.AND);
        trackedConstraints.forEach(tr -> root.attach(tr.createLeaf()));

        ConstraintNode current = root;
        for (ConstraintMapperApplier applier : this.appliers) {
            current = applier.process(current, new PropertySet().inheritFrom(context).inheritFrom(this.context));
        }

        return new SolveResult(current.copy(), trackedConstraints);
    }

}
