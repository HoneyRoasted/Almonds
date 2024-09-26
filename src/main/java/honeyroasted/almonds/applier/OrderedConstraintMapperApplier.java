package honeyroasted.almonds.applier;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.almonds.ConstraintTree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class OrderedConstraintMapperApplier implements ConstraintMapperApplier {
    private List<ConstraintMapper> mappers;

    public OrderedConstraintMapperApplier(List<ConstraintMapper> mappers) {
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
            if (cm instanceof UntrimmedConstraintMapperApplier cma) {
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

        List<ConstraintBranch> branches = new ArrayList<>();
        List<ConstraintBranch> newTracked = new ArrayList<>();
        branches.add(branch);

        do {
            if (!branches.isEmpty()) {
                ConstraintBranch curr = branches.iterator().next();
                if (curr.status() == Constraint.Status.TRUE) {
                    return;
                }

                for (ConstraintMapper mapper : this.mappers) {
                    mapper.accept(curr);
                }
            }

            newTracked.clear();
            for (ConstraintBranch sub : branches) {
                if (sub.diverged()) {
                    for (ConstraintBranch cb : sub.divergence()) {
                        if (!cb.trimmed()) newTracked.add(cb);
                    }
                } else if (!sub.trimmed()) {
                    newTracked.add(sub);
                }
            }

            List<ConstraintBranch> temp = branches;
            branches = newTracked;
            newTracked = temp;
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
