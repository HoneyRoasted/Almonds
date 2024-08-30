package honeyroasted.almonds;

import honeyroasted.collect.change.ChangingMergingElement;
import honeyroasted.collect.property.PropertySet;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ConstraintBranch implements ChangingMergingElement<ConstraintBranch> {
    private ConstraintTree parent;

    private PropertySet metadata = new PropertySet();
    private Map<Constraint, Constraint.Status> constraints = new ConcurrentHashMap<>();

    private boolean shouldTrackDivergence = false;
    private Set<ConstraintBranch> divergence;

    private boolean trimmed;

    public record Snapshot(PropertySet metadata, Map<Constraint, Constraint.Status> constraints) {};

    public ConstraintBranch(ConstraintTree parent) {
        this.parent = parent;
    }

    public ConstraintBranch copy(ConstraintTree parent) {
        ConstraintBranch snap = new ConstraintBranch(parent);
        snap.metadata.copyFrom(this.metadata);
        snap.constraints.putAll(this.constraints);
        return snap;
    }

    public Snapshot snapshot() {
        PropertySet snapMeta = new PropertySet().copyFrom(metadata);
        Map<Constraint, Constraint.Status> snapConstraints = new HashMap<>(this.constraints);
        return new Snapshot(snapMeta, snapConstraints);
    }

    public ConstraintTree parent() {
        return this.parent;
    }

    public boolean trimmed() {
        return this.trimmed;
    }

    public ConstraintBranch setTrimmed(boolean trimmed) {
        this.trimmed = trimmed;
        return this;
    }

    public Map<Constraint, Constraint.Status> constraints() {
        return Collections.unmodifiableMap(this.constraints);
    }

    public int size() {
        return this.constraints.size();
    }

    public PropertySet metadata() {
        return this.metadata;
    }

    public Constraint.Status status() {
        if (this.constraints.isEmpty()) {
            return Constraint.Status.UNKNOWN;
        } else {
            Iterator<Constraint.Status> iter = this.constraints.values().iterator();
            Constraint.Status curr = iter.next();
            while (iter.hasNext()) {
                curr = curr.and(iter.next());
            }
            return curr;
        }
    }

    public void diverge(Constraint... constraints) {
        this.diverge(Set.of(constraints));
    }

    public void diverge(Collection<? extends Constraint> constraints) {
        this.diverge(constraints.stream().map(cn -> Map.of(cn, Constraint.Status.UNKNOWN)).toList());
    }

    public void diverge(List<? extends Map<? extends Constraint, Constraint.Status>> constraints) {
        this.divergeBranches(constraints.stream().map(mp -> new Snapshot(new PropertySet(), (Map) mp)).toList());
    }

    public void divergeBranches(List<Snapshot> branches) {
        if (branches.size() == 1) {
            branches.forEach(snap -> {
                this.metadata.inheritFrom(snap.metadata);
                snap.constraints.forEach((con, stat) -> {
                    this.add(con);
                    this.setStatus(con, stat);
                });
            });
        } else if (!branches.isEmpty()) {
            if (divergence == null && shouldTrackDivergence) this.divergence = new HashSet<>();

            this.parent.removeBranch(this);
            branches.forEach(snap -> {
                ConstraintBranch newBranch = new ConstraintBranch(this.parent);
                newBranch.metadata().copyFrom(this.metadata);
                newBranch.metadata.inheritFrom(snap.metadata);
                newBranch.constraints.putAll(this.constraints);
                snap.constraints.forEach((con, stat) -> newBranch.constraints.putIfAbsent(con, stat));
                this.parent.addBranch(newBranch);
                if (shouldTrackDivergence) this.divergence.add(newBranch);
            });
        }
    }

    public ConstraintBranch setShouldTrackDivergence(boolean shouldTrackDivergence) {
        this.shouldTrackDivergence = shouldTrackDivergence;
        return this;
    }

    public Set<ConstraintBranch> divergence() {
        return this.divergence == null ? Collections.emptySet() : divergence;
    }

    public ConstraintBranch setStatus(Constraint constraint, Constraint.Status status) {
        this.parent.currentBranches().doChange(this, () -> {
            Constraint.Status current = this.constraints.get(constraint);
            if (current != null && current != status) {
                if (status == Constraint.Status.FALSE) this.setTrimmed(true);
                this.constraints.put(constraint, status);
                return true;
            }
            return false;
        });
        return this;
    }

    public ConstraintBranch add(Constraint constraint, Constraint.Status status) {
        this.parent.currentBranches().doChange(this, () -> {
            if (status == Constraint.Status.FALSE) this.setTrimmed(true);
            Object prev = this.constraints.putIfAbsent(constraint, status);
            return prev == null;
        });
        return this;
    }

    public ConstraintBranch add(Constraint constraint) {
        return this.add(constraint, Constraint.Status.UNKNOWN);
    }

    public ConstraintBranch drop(Constraint constraint) {
        this.parent.currentBranches().doChange(this, () -> {
            Constraint.Status prev = this.constraints.remove(constraint);
            return prev != null;
        });
        return this;
    }

    public String toString(boolean simpleName, String indent) {
        StringBuilder sb = new StringBuilder();
        if (!this.metadata().all(Object.class).isEmpty()) {
            sb.append(indent).append("Metadata:\n");
            this.metadata.all(Object.class).forEach(obj -> sb.append(indent).append(obj).append("\n"));
            sb.append("\n");
        }

        sb.append(indent).append("Constraints:\n");
        this.constraints.forEach((con, stat) -> sb.append(indent).append(stat).append(": ").append(simpleName ? con.simpleName() : con).append("\n"));

        return sb.toString();
    }

    @Override
    public String toString() {
        return this.toString(false, "");
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        ConstraintBranch that = (ConstraintBranch) object;
        return Objects.equals(constraints.keySet(), that.constraints.keySet());
    }

    @Override
    public int hashCode() {
        return Objects.hash(constraints.keySet());
    }

    @Override
    public void merge(ConstraintBranch other) {
        this.metadata.inheritFrom(other.metadata);
        other.metadata.inheritFrom(this.metadata);
    }
}
