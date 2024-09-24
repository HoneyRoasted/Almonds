package honeyroasted.almonds;

import honeyroasted.collect.property.PropertySet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

public class ConstraintBranch implements Comparable<ConstraintBranch> {
    private ConstraintTree parent;

    private PropertySet metadata = new PropertySet();
    private Map<Constraint, Constraint.Status> constraints = new LinkedHashMap<>();
    private Map<Class<? extends Constraint>, Set<Constraint>> typedConstraints = new HashMap<>();

    private Map<Constraint, Constraint.Status> constraintView = Collections.unmodifiableMap(constraints);
    private Map<Class<? extends Constraint>, Set<Constraint>> typedConstraintsView = Collections.unmodifiableMap(typedConstraints);

    private List<ConstraintBranch> divergence;
    private List<Predicate<ConstraintBranch>> changes = new ArrayList<>();

    private int priority;

    public boolean trimmedFalse() {
        return this.status() == Constraint.Status.FALSE && !this.constraints.isEmpty();
    }

    public boolean trimmedTrue() {
        return this.status() == Constraint.Status.TRUE && !this.constraints.isEmpty();
    }

    public record Snapshot(PropertySet metadata, Map<Constraint, Constraint.Status> constraints, int priority) {
        private static final Snapshot empty = new Snapshot(new PropertySet(), Collections.unmodifiableMap(new HashMap<>()), 0);

        public static Snapshot empty() {
            return empty;
        }
    }

    @Override
    public int compareTo(ConstraintBranch o) {
        return Integer.compare(this.priority, o.priority);
    }

    public int priority() {
        return this.priority;
    }

    public ConstraintBranch setPriority(int priority) {
        this.priority = priority;
        return this;
    }

    public ConstraintBranch(ConstraintTree parent) {
        this.parent = parent;
    }

    public boolean executeChanges() {
        boolean modified = false;
        for (Predicate<ConstraintBranch> change : this.changes) {
            if (change.test(this)) {
                modified = true;
            }
        }
        this.changes.clear();
        return modified;
    }

    private void change(Predicate<ConstraintBranch> change) {
        this.changes.add(change);
        if (this.divergence != null) {
            this.divergence.forEach(cb -> cb.change(change));
        }
    }

    public ConstraintBranch copy(ConstraintTree parent) {
        ConstraintBranch copy = new ConstraintBranch(parent);
        copy.metadata.copyFrom(this.metadata);
        copy.constraints.putAll(this.constraints);
        this.typedConstraints.forEach((k, v) -> copy.typedConstraints.put(k, new HashSet<>(v)));
        copy.changes.addAll(this.changes);

        copy.priority = this.priority;
        return copy;
    }

    public Snapshot snapshot() {
        ConstraintBranch copy = this.copy(this.parent);
        copy.executeChanges();

        PropertySet snapMeta = new PropertySet().copyFrom(copy.metadata());
        Map<Constraint, Constraint.Status> snapConstraints = new LinkedHashMap<>(copy.constraints());
        return new Snapshot(snapMeta, snapConstraints, this.priority);
    }

    public boolean diverged() {
        return this.divergence != null && !this.divergence.isEmpty();
    }

    public ConstraintTree parent() {
        return this.parent;
    }

    public Map<Constraint, Constraint.Status> constraints() {
        return this.constraintView;
    }

    public Map<Class<? extends Constraint>, Set<Constraint>> typedConstraints() {
        return this.typedConstraintsView;
    }

    public int size() {
        return this.constraints.size();
    }

    public PropertySet metadata() {
        return this.metadata;
    }

    public Constraint.Status status(Constraint constraint) {
        return this.constraints.getOrDefault(constraint, Constraint.Status.UNKNOWN);
    }

    private Constraint.Status cached;

    public Constraint.Status status() {
        if (this.cached == null) calculateStatus();
        return this.cached;
    }

    private void calculateStatus() {
        if (this.constraints.isEmpty()) {
            this.cached = Constraint.Status.TRUE;
        } else {
            Iterator<Constraint.Status> iter = this.constraints.values().iterator();
            Constraint.Status curr = iter.next();
            while (iter.hasNext()) {
                curr = curr.and(iter.next());
            }
            this.cached = curr;
        }
    }

    public void diverge(Constraint... constraints) {
        this.diverge(Set.of(constraints));
    }

    public void diverge(Collection<? extends Constraint> constraints) {
        this.diverge(constraints.stream().map(cn -> Map.of(cn, Constraint.Status.UNKNOWN)).toList());
    }

    public void diverge(List<? extends Map<? extends Constraint, Constraint.Status>> constraints) {
        List<Snapshot> divergence = new ArrayList<>();
        for (int i = 0; i < constraints.size(); i++) {
            Snapshot snapshot = new Snapshot(new PropertySet(), (Map<Constraint, Constraint.Status>) constraints.get(i), i);
        }
        this.divergeBranches(divergence);
    }

    public List<ConstraintBranch> divergence() {
        return this.divergence == null ? Collections.emptyList() : divergence;
    }


    public void divergeBranches(List<Snapshot> branches) {
        if (!branches.isEmpty()) {
            if (!this.diverged() && branches.size() == 1) {
                //Just adding the branch to this one
                branches.forEach(branch -> {
                    this.metadata.inheritFrom(branch.metadata);
                    branch.constraints().forEach(this::add);
                });
            } else {
                if (this.divergence == null) this.divergence = new ArrayList<>();

                if (this.divergence.isEmpty()) {
                    branches.forEach(branch -> {
                        ConstraintBranch newBranch = this.copy(this.parent);
                        newBranch.metadata().inheritFrom(branch.metadata());

                        branch.constraints().forEach(newBranch::add);
                        newBranch.executeChanges();
                        this.divergence.add(newBranch);
                    });
                } else {
                    List<ConstraintBranch> newDiverge = new ArrayList<>();
                    for (Snapshot snapshot : branches) {
                        for (ConstraintBranch diverge : this.divergence) {
                            ConstraintBranch newBranch = this.copy(this.parent);
                            newBranch.metadata().inheritFrom(diverge.metadata)
                                    .inheritFrom(snapshot.metadata);

                            diverge.constraints().forEach(newBranch::add);
                            snapshot.constraints().forEach(newBranch::add);
                            newBranch.executeChanges();

                            newDiverge.add(newBranch);
                        }
                    }
                    this.divergence = newDiverge;
                }
            }
        }
    }

    //Changes ---------

    public ConstraintBranch put(Constraint constraint, Constraint.Status status) {
        this.change(branch -> {
            Constraint.Status current = branch.constraints.get(constraint);
            branch.constraints.put(constraint, status);
            branch.typedConstraints.computeIfAbsent(constraint.getClass(), k -> new HashSet<>()).add(constraint);

            boolean modified = current == null || current != status;
            if (current != null && current != status) {
                if (current.isTrue()) {
                    //If the previous status was true, that invalidates the status
                    this.cached = null;
                } else {
                    this.cached = this.status().and(status);
                }
            }
            return modified;
        });
        return this;
    }

    public ConstraintBranch add(Constraint constraint, Constraint.Status status) {
        this.change(branch -> {
            Object prev = branch.constraints.putIfAbsent(constraint, status);
            branch.typedConstraints.computeIfAbsent(constraint.getClass(), k -> new HashSet<>()).add(constraint);

            if (prev == null) {
                this.cached = this.status().and(status);
                return true;
            } else {
                return false;
            }
        });
        return this;
    }

    public ConstraintBranch drop(Constraint constraint) {
        this.change(branch -> {
            Constraint.Status prev = branch.constraints.remove(constraint);

            Set<Constraint> typedConstrains = this.typedConstraints.get(constraint.getClass());
            if (typedConstrains != null) {
                typedConstrains.remove(constraint);
                if (typedConstrains.isEmpty()) {
                    this.typedConstraints.remove(constraint.getClass());
                }
            }

            if (prev != null) {
                this.cached = null;
                return true;
            } else {
                return false;
            }
        });
        return this;
    }


    public ConstraintBranch set(Constraint constraint, Constraint.Status status) {
        this.change(branch -> {
            Constraint.Status curr = branch.constraints.get(constraint);
            if (curr == null || curr != status) {


                branch.constraints.put(constraint, status);
                return true;
            }
            return false;
        });
        return this;
    }

    //---------------------------

    public ConstraintBranch add(Constraint constraint) {
        return this.add(constraint, Constraint.Status.UNKNOWN);
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
}
