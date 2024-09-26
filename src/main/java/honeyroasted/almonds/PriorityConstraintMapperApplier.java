package honeyroasted.almonds;

import honeyroasted.almonds.util.SortedList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

        List<ConstraintBranch> branches = new SortedList<>();
        branches.add(branch);

        do {
            List<ConstraintBranch> newTracked = new SortedList<>();
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
                ConstraintBranch curr = branches.getFirst();
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
                ConstraintBranch curr = tree.active().getFirst();
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
