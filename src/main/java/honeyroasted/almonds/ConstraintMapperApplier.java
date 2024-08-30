package honeyroasted.almonds;

import honeyroasted.collect.change.ExclusiveChangeAwareSet;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class ConstraintMapperApplier implements ConstraintMapper {
    private List<ConstraintMapper> mappers;

    public ConstraintMapperApplier(List<ConstraintMapper> mappers) {
        this.mappers = mappers;
    }

    @Override
    public void accept(ConstraintBranch branch) {
        ConstraintTree tree = branch.parent();

        AtomicBoolean modified = new AtomicBoolean(false);
        Runnable listener = () -> modified.set(true);
        tree.currentBranches().addListener(listener);

        Set<ConstraintBranch> branches = Collections.newSetFromMap(new IdentityHashMap<>());
        branches.add(branch);

        do {
            modified.set(false);

            for (ConstraintMapper mapper : this.mappers) {
                boolean restart = false;

                for (ConstraintBranch sub : branches) {
                    if (sub.trimmed()) {
                        sub.setShouldTrackDivergence(true);
                        mapper.accept(sub);

                        Set<ConstraintBranch> diverged = sub.divergence();
                        if (!diverged.isEmpty()) {
                            branches.remove(sub);
                            branches.addAll(diverged);

                            restart = true;
                            break;
                        }
                    }
                }
                if (restart) break;
            }
        } while (modified.get());
        tree.currentBranches().removeListener(listener);
    }

    public void accept(ConstraintTree tree) {
        AtomicBoolean modified = new AtomicBoolean(false);
        Runnable listener = () -> modified.set(true);
        tree.currentBranches().addListener(listener);

        do {
            modified.set(false);

            for (ExclusiveChangeAwareSet<ConstraintBranch>.StopOnModifyIterator it = tree.currentBranches().stopOnModifyIterator(); it.hasNext(); ) {
                ConstraintBranch branch = it.next();

                branch.setShouldTrackDivergence(false);
                for (ConstraintMapper mapper : this.mappers) {
                    mapper.accept(branch);
                    if (it.isDiverged()) break;
                }
                if (it.isDiverged()) break;
            }
        } while (modified.get());
        tree.currentBranches().removeListener(listener);
    }
}
