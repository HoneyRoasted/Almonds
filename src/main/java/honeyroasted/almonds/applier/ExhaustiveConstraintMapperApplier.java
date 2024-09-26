package honeyroasted.almonds.applier;

import honeyroasted.almonds.ConstraintBranch;
import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.almonds.ConstraintTree;

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
            for (ConstraintBranch sub : branches) {
                for (ConstraintMapper mapper : this.mappers) {
                    mapper.accept(sub);
                }
            }

            newTracked.clear();
            for (ConstraintBranch sub : branches) {
                if (sub.diverged()) {
                    newTracked.addAll(sub.divergence());
                } else {
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
            for (ConstraintBranch branch : tree.currentBranches().keySet()) {
                for (ConstraintMapper mapper : this.mappers) {
                    mapper.accept(branch);
                }
            }
        } while (tree.executeChanges());
    }
}