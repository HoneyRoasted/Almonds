package honeyroasted.almonds;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class ConstraintMapperApplier implements ConstraintMapper {
    private List<ConstraintMapper> mappers;
    private boolean exploreTrimmedPaths;

    public ConstraintMapperApplier(List<ConstraintMapper> mappers, boolean exploreTrimmedPaths) {
        this.mappers = Collections.unmodifiableList(mappers);
        this.exploreTrimmedPaths = exploreTrimmedPaths;
    }

    public ConstraintMapperApplier(List<ConstraintMapper> mappers) {
        this(mappers, false);
    }

    public List<ConstraintMapper> mappers() {
        return this.mappers;
    }

    public List<ConstraintMapper> flattened() {
        List<ConstraintMapper> flat = new ArrayList<>();
        this.mappers.forEach(cm -> {
            if (cm instanceof ConstraintMapperApplier cma) {
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

        Map<ConstraintBranch, ConstraintBranch> branches = new TreeMap<>();
        branches.put(branch, branch);

        do {
            if (!exploreTrimmedPaths && branches.keySet().stream().anyMatch(cb -> cb.status() == Constraint.Status.TRUE)) {
                return;
            }

            for (ConstraintBranch sub : branches.keySet()) {
                for (ConstraintMapper mapper : this.mappers) {
                    mapper.accept(sub);
                }
            }

            Map<ConstraintBranch, ConstraintBranch> newTracked = new TreeMap<>();
            for (ConstraintBranch sub : branches.keySet()) {
                if (sub.diverged()) {
                    sub.divergence().forEach(cb -> {
                        if (!cb.trimmed() || this.exploreTrimmedPaths) newTracked.put(cb, cb);
                    });
                } else if (!sub.trimmed() || this.exploreTrimmedPaths) {
                    newTracked.put(sub, sub);
                }
            }
            branches = newTracked;

        } while (tree.executeChanges());
    }

    public void accept(ConstraintTree tree) {
        do {
            if (!exploreTrimmedPaths && tree.branches().stream().anyMatch(cb -> cb.status() == Constraint.Status.TRUE)) {
                return;
            }

            for (ConstraintBranch branch : tree.active()) {
                for (ConstraintMapper mapper : this.mappers) {
                    mapper.accept(branch);
                }
            }
        } while (tree.executeChanges());
    }
}
