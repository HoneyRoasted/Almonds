package honeyroasted.almonds;

import honeyroasted.almonds.util.SortedList;
import honeyroasted.collect.property.PropertySet;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ConstraintTree {
    private PropertySet metadata = new PropertySet();

    private List<ConstraintBranch> active = new SortedList<>();
    private Map<ConstraintBranch, ConstraintBranch> branches = new LinkedHashMap<>();

    public int numBranches() {
        return this.branches.size();
    }

    public Set<ConstraintBranch> validBranches() {
        return this.branches(cn -> cn.status().isTrue());
    }

    public Set<ConstraintBranch> invalidBranches() {
        return this.branches(cn -> !cn.status().isTrue());
    }

    public Set<ConstraintBranch> branches(Predicate<ConstraintBranch> filter) {
        return this.branches.keySet().stream().filter(filter).collect(Collectors.toUnmodifiableSet());
    }

    public List<ConstraintBranch> active() {
        return this.active;
    }

    public Set<ConstraintBranch> branches() {
        return Collections.unmodifiableSet(new HashSet<>(this.branches.keySet()));
    }

    public Map<ConstraintBranch, ConstraintBranch> currentBranches() {
        return this.branches;
    }

    public Constraint.Status status() {
        if (this.branches.isEmpty()) {
            return Constraint.Status.UNKNOWN;
        } else {
            Iterator<ConstraintBranch> iter = this.branches.keySet().iterator();
            ;
            Constraint.Status curr = iter.next().status();
            while (iter.hasNext()) {
                curr = curr.or(iter.next().status());
            }
            return curr;
        }
    }

    public PropertySet metadata() {
        return this.metadata;
    }

    public String toString(boolean useSimpleName) {
        StringBuilder sb = new StringBuilder();
        sb.append("Status: " + this.status()).append("\n");
        if (!this.metadata().all(Object.class).isEmpty()) {
            sb.append("Metadata:\n");
            this.metadata().all(Object.class).forEach(obj -> sb.append(obj).append("\n"));
            sb.append("\n");
        }

        Set<ConstraintBranch> valid = this.validBranches();
        Set<ConstraintBranch> invalid = this.invalidBranches();

        sb.append("Valid Branches: ").append(valid.size()).append("\n");
        valid.forEach(cb -> sb.append(cb.toString(useSimpleName, "  ")).append("  ").append("-".repeat(100)).append("\n"));

        sb.append("Invalid Branches: ").append(invalid.size()).append("\n");
        invalid.forEach(cb -> sb.append(cb.toString(useSimpleName, "  ")).append("  ").append("-".repeat(100)).append("\n"));

        return sb.toString();
    }

    @Override
    public String toString() {
        return this.toString(false);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        ConstraintTree tree = (ConstraintTree) object;
        return Objects.equals(this.branches, tree.branches);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.branches);
    }


    public boolean executeChanges() {
        boolean modified = false;

        Map<ConstraintBranch, ConstraintBranch> newBranches = new LinkedHashMap<>();
        List<ConstraintBranch> newActive = new SortedList<>();
        List<ConstraintBranch> all = new SortedList<>();

        int currPrio = 0;
        for (ConstraintBranch branch : this.branches.keySet()) {
            if (branch.diverged()) {
                for (ConstraintBranch newBranch : branch.divergence()) {
                    currPrio += newBranch.priority();
                    newBranch.setPriority(currPrio);

                    newBranch.executeChanges();
                    addBranch(newBranch, newBranches, newActive, all);
                }
                modified = true;
            } else {
                currPrio += branch.priority();
                branch.setPriority(currPrio);

                boolean changed = branch.executeChanges();
                addBranch(branch, newBranches, newActive, all);
                modified |= changed;
            }
        }

        int curr = 0;
        int prev = 0;

        for (int i = 0; i < all.size(); i++) {
            ConstraintBranch branch = all.get(i);
            int prioTemp = branch.priority();

            if (prev == branch.priority()) {
                branch.setPriority(curr);
            } else {
                branch.setPriority(curr++);
            }

            prev = prioTemp;
        }

        this.branches = newBranches;
        this.active = newActive;

        return modified;
    }

    public void addBranch(ConstraintBranch branch) {
        addBranch(branch, this.branches, this.active, null);
    }

    private static void addBranch(ConstraintBranch branch, Map<ConstraintBranch, ConstraintBranch> branches, List<ConstraintBranch> active, List<ConstraintBranch> all) {
        ConstraintBranch prev = branches.putIfAbsent(branch, branch);
        if (prev != null) {
            prev.metadata().inheritFrom(branch.metadata());
        } else {
            if (!branch.trimmed()) {
                active.add(branch);
            }

            if (all != null) {
                all.add(branch);
            }
        }
    }

    public boolean has(ConstraintBranch branch) {
        return this.branches.containsKey(branch);
    }
}
