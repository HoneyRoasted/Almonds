package honeyroasted.almonds;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ExhaustiveConstraintMapperApplier implements ConstraintMapperApplier {
    private List<ConstraintMapper> mappers;

    public ExhaustiveConstraintMapperApplier(List<ConstraintMapper> mappers) {
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

        List<ConstraintBranch> branches = new ArrayList<>();
        branches.add(branch);

        do {
            for (ConstraintBranch sub : branches) {
                for (ConstraintMapper mapper : this.mappers) {
                    mapper.accept(sub);
                }
            }

            List<ConstraintBranch> newTracked = new ArrayList<>();
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

        } while (tree.executeChanges());
    }

    @Override
    public void accept(ConstraintTree tree) {
        do {
            for (ConstraintBranch branch : tree.active()) {
                for (ConstraintMapper mapper : this.mappers) {
                    mapper.accept(branch);
                }
            }
        } while (tree.executeChanges());
    }
}
