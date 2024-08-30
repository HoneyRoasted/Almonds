package honeyroasted.almonds;

import honeyroasted.collect.change.ExclusiveChangeAwareSet;
import honeyroasted.collect.property.PropertySet;

import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ConstraintTree {
    private PropertySet metadata = new PropertySet();
    private ExclusiveChangeAwareSet<ConstraintBranch> branches;

    public ConstraintTree(int initialCapacity) {
        this.branches = new ExclusiveChangeAwareSet<>(initialCapacity);
    }

    public ConstraintTree() {
        this(1024);
    }

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
        return this.branches.stream().filter(filter).collect(Collectors.toUnmodifiableSet());
    }

    public Set<ConstraintBranch> branches() {
        return this.branches.setCopy();
    }

    ExclusiveChangeAwareSet<ConstraintBranch> currentBranches() {
        return this.branches;
    }

    public Constraint.Status status() {
        if (this.branches.isEmpty()) {
            return Constraint.Status.UNKNOWN;
        } else {
            Iterator<ConstraintBranch> iter = this.branches.iterator();;
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

    public void addBranch(ConstraintBranch newBranch) {
        this.branches.add(newBranch);
    }

    public void removeBranch(ConstraintBranch constraintBranch) {
        this.branches.remove(constraintBranch);
    }

    public boolean has(ConstraintBranch branch) {
        return this.branches.contains(branch);
    }
}
