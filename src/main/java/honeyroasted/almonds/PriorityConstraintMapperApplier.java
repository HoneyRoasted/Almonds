package honeyroasted.almonds;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class PriorityConstraintMapperApplier implements ConstraintMapperApplier {
    private List<ConstraintMapper> mappers;

    public PriorityConstraintMapperApplier(List<ConstraintMapper> mappers) {
        this.mappers = Collections.unmodifiableList(mappers);
    }

    @Override
    public List<ConstraintMapper> mappers() {
        return this.mappers;
    }

    @Override
    public List<ConstraintMapper> flattened() {
        List<ConstraintMapper> flat = new ArrayList<>();
        this.mappers.forEach(cm -> {
            if (cm instanceof ExhaustiveConstraintMapperApplier cma) {
                flat.addAll(cma.flattened());
            } else {
                flat.add(cm);
            }
        });
        return flat;
    }

    @Override
    public void accept(ConstraintBranch branch) {
        ConstraintTree tree = branch.parent();

        Set<ConstraintBranch> branches = new LinkedHashSet<>();
        branches.add(branch);

        do {
            Set<ConstraintBranch> newTracked = new LinkedHashSet<>();
            for (ConstraintBranch sub : branches) {
                if (sub.diverged()) {
                    sub.divergence().forEach(cb -> {
                        if (!cb.trimmed()) newTracked.add(cb);
                    });
                } else if (!sub.trimmed()) {
                    newTracked.add(sub);
                }
            }
            branches = newTracked;


            if (!branches.isEmpty()) {
                ConstraintBranch curr = branches.iterator().next();
                if (curr.status() == Constraint.Status.TRUE) {
                    return;
                }

                for (ConstraintMapper mapper : this.mappers) {
                    mapper.accept(curr);
                }
            }
        } while (tree.executeChanges());
    }

    @Override
    public void accept(ConstraintTree tree) {
        do {
            if (!tree.active().isEmpty()) {
                ConstraintBranch curr = tree.active().iterator().next();
                if (curr.status() == Constraint.Status.TRUE) {
                    return;
                }

                for (ConstraintMapper mapper : this.mappers) {
                    mapper.accept(curr);
                }
            }
        } while (tree.executeChanges());
    }
}
