package honeyroasted.almonds;

import honeyroasted.collect.property.PropertySet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ConstraintSolver {
    private List<ConstraintMapperApplier> appliers;
    private PropertySet context = new PropertySet();
    private int capacityFactor = 2;

    public ConstraintSolver(List<ConstraintMapperApplier> appliers) {
        this.appliers = appliers;
    }

    private List<Constraint> constraints = new ArrayList<>();

    public ConstraintSolver setInitialCapacity(int size) {
        this.capacityFactor = -size;
        return this;
    }

    public ConstraintSolver setCapacityFactor(int factor) {
        this.capacityFactor = factor;
        return this;
    }

    public ConstraintSolver bind(Constraint... constraints) {
        Collections.addAll(this.constraints, constraints);
        return this;
    }

    public ConstraintSolver withContext(PropertySet context) {
        this.context.inheritFrom(context);
        return this;
    }

    public ConstraintTree solve() {
        return this.solve(new PropertySet());
    }

    public ConstraintTree solve(PropertySet context) {
        ConstraintTree tree = new ConstraintTree();
        tree.metadata().inheritFrom(context);
        tree.metadata().inheritFrom(this.context);

        ConstraintBranch branch = new ConstraintBranch(tree);
        this.constraints.forEach(branch::add);
        tree.addBranch(branch);

        for (ConstraintMapperApplier applier : this.appliers) {
            applier.accept(tree);
        }

        return tree;
    }
}
