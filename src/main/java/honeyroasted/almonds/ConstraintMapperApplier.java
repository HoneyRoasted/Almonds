package honeyroasted.almonds;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

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

        Set<ConstraintBranch> branches = new TreeSet<>();
        branches.add(branch);

        do {
            if (!exploreTrimmedPaths && branches.stream().anyMatch(ConstraintBranch::trimmedTrue)) {
                return;
            }

            for (ConstraintBranch sub : branches) {
                for (ConstraintMapper mapper : this.mappers) {
                    mapper.accept(sub);
                }
            }

            Set<ConstraintBranch> newTracked = new TreeSet<>();
            for (ConstraintBranch sub : branches) {
                if (sub.diverged()) {
                    sub.divergence().forEach(cb -> {
                        if (!cb.trimmedFalse() || this.exploreTrimmedPaths) newTracked.add(cb);
                    });
                } else if (!sub.trimmedFalse() || this.exploreTrimmedPaths) {
                    newTracked.add(sub);
                }
            }
            branches = newTracked;

        } while (tree.executeChanges());
    }

    public void accept(ConstraintTree tree) {
        do {
            if (!this.exploreTrimmedPaths && tree.branches().stream().anyMatch(ConstraintBranch::trimmedTrue)) {
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
