package honeyroasted.almonds;

import honeyroasted.collect.property.PropertySet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

public class ConstraintBranch {
    private ConstraintTree parent;

    private PropertySet metadata = new PropertySet();

    private Map<Constraint, Constraint.Status> constraints = new LinkedHashMap<>();
    private Map<Constraint, PropertySet> constraintMetadata = new HashMap<>();
    private Map<Constraint, Constraint.Status> constraintsView = Collections.unmodifiableMap(constraints);

    private Map<Class<?>, Set<Constraint>> typedConstraints = new HashMap<>();
    private Map<Class<?>, Set<Constraint>> typedConstraintsView = Collections.unmodifiableMap(typedConstraints);

    private List<ConstraintBranch> divergence;
    private List<ConstraintBranch> newDivergence;

    private List<Predicate<ConstraintBranch>> changes = new ArrayList<>();

    private boolean trimmed;

    public record Snapshot(PropertySet metadata, Map<Constraint, Constraint.Status> constraints,
                           Map<Constraint, PropertySet> constraintMetadata) {
        private static final Snapshot empty = new Snapshot(new PropertySet(), Collections.emptyMap(), Collections.emptyMap());

        public Snapshot(PropertySet metadata, Map<Constraint, Constraint.Status> constraints) {
            this(metadata, constraints, new HashMap<>());
        }

        public static Snapshot empty() {
            return empty;
        }
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

    public ConstraintBranch copy() {
        return this.copy(null);
    }

    public ConstraintBranch copy(ConstraintTree parent) {
        ConstraintBranch copy = new ConstraintBranch(parent);
        copy.metadata.copyFrom(this.metadata);
        copy.constraints.putAll(this.constraints);
        this.constraintMetadata.forEach((con, meta) -> copy.constraintMetadata.put(con, meta.copy()));
        this.typedConstraints.forEach((t, cons) -> copy.typedConstraints.put(t, new HashSet<>(cons)));
        copy.changes.addAll(this.changes);
        copy.trimmed = this.trimmed;
        return copy;
    }

    public Snapshot snapshot() {
        ConstraintBranch copy = this.copy();
        copy.executeChanges();
        return new Snapshot(copy.metadata, copy.constraints);
    }

    public boolean diverged() {
        return this.divergence != null && !this.divergence.isEmpty();
    }

    public ConstraintTree parent() {
        return this.parent;
    }

    public boolean trimmed() {
        return this.trimmed;
    }

    public Map<Constraint, Constraint.Status> constraints() {
        return this.constraintsView;
    }

    public Map<Class<?>, Set<Constraint>> typeConstraints() {
        return this.typedConstraintsView;
    }

    public Set<Constraint> constraintsByType(Class<?> cls) {
        return this.typedConstraints.getOrDefault(cls, Collections.emptySet());
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

    private static PropertySet emptyProperties = new PropertySet();

    public void diverge(List<? extends Map<? extends Constraint, Constraint.Status>> constraints) {
        List<Snapshot> branches = new ArrayList<>();
        for (int i = 0; i < constraints.size(); i++) {
            branches.add(new Snapshot(emptyProperties, (Map<Constraint, Constraint.Status>) constraints.get(i)));
        }
        this.divergeBranches(branches);
    }

    public List<ConstraintBranch> divergence() {
        return this.divergence == null ? Collections.emptyList() : divergence;
    }

    public void divergeBranches(List<Snapshot> branches) {
        if (!branches.isEmpty()) {
            if (!this.diverged() && branches.size() == 1) {
                //Just adding the branch to this one
                branches.forEach(branch -> {
                    this.metadata().copyFrom(new PropertySet()
                            .inheritFrom(branch.metadata())
                            .inheritFrom(this.metadata()));
                    branch.constraints().forEach(this::add);
                    branch.constraintMetadata().forEach(this::addMetadata);
                });
            } else {
                if (this.divergence == null) {
                    this.divergence = new ArrayList<>();
                }

                if (this.divergence.isEmpty()) {
                    branches.forEach(snapshot -> {
                        ConstraintBranch newBranch = this.copyForDivergence(this.parent);
                        newBranch.metadata().inheritFrom(snapshot.metadata())
                                .inheritFrom(this.metadata);

                        snapshot.constraints().forEach(newBranch::add);
                        snapshot.constraintMetadata().forEach(newBranch::addMetadata);
                        newBranch.executeChanges();
                        this.divergence.add(newBranch);
                    });
                } else {
                    if (this.newDivergence == null) {
                        this.newDivergence = new ArrayList<>();
                    } else {
                        this.newDivergence.clear();
                        ;
                    }

                    for (Snapshot snapshot : branches) {
                        for (ConstraintBranch diverge : this.divergence) {
                            ConstraintBranch newBranch = this.copyForDivergence(this.parent);
                            newBranch.metadata().inheritFrom(snapshot.metadata())
                                    .inheritFrom(diverge.metadata())
                                    .inheritFrom(this.metadata());

                            diverge.constraints().forEach(newBranch::add);
                            diverge.constraintMetadata.forEach(newBranch::addMetadata);
                            snapshot.constraints().forEach(newBranch::add);
                            snapshot.constraintMetadata().forEach(newBranch::addMetadata);
                            newBranch.executeChanges();
                            newDivergence.add(newBranch);
                        }
                    }

                    List<ConstraintBranch> temp = this.divergence;
                    this.divergence = this.newDivergence;
                    this.newDivergence = temp;
                }
            }
        }
    }

    private ConstraintBranch copyForDivergence(ConstraintTree parent) {
        ConstraintBranch copy = new ConstraintBranch(parent);
        copy.constraints.putAll(this.constraints);
        this.constraintMetadata.forEach((con, meta) -> copy.constraintMetadata.put(con, meta.copy()));
        this.typedConstraints.forEach((t, cons) -> copy.typedConstraints.put(t, new HashSet<>(cons)));
        copy.changes.addAll(this.changes);
        copy.trimmed = this.trimmed;
        return copy;
    }

    public void mergeFrom(ConstraintBranch other) {
        this.metadata.inheritFrom(other.metadata());
        other.constraintMetadata.forEach((con, ps) -> {
            if (this.constraints.containsKey(con)) {
                PropertySet md = this.constraintMetadata.get(con);
                if (md == null) {
                    this.constraintMetadata.put(con, ps);
                } else {
                    md.inheritFrom(ps);
                }
            }
        });
    }

    //Changes ---------

    private void addMetadata(Constraint constraint, PropertySet ps) {
        this.change(branch -> {
            PropertySet md = branch.constraintMetadata.get(constraint);
            if (md == null) {
                branch.constraintMetadata.put(constraint, ps);
            } else {
                md.inheritFrom(ps);
            }
            return false;
        });
    }

    public ConstraintBranch attachMetadata(Constraint constraint, Object o) {
        this.change(branch -> {
            PropertySet md = branch.constraintMetadata.get(constraint);
            if (md != null) {
                md.attach(o);
            }
            return false;
        });
        return this;
    }

    public ConstraintBranch removeMetadata(Constraint constraint, Object o) {
        this.change(branch -> {
            PropertySet md = branch.constraintMetadata.get(constraint);
            if (md != null) {
                md.remove(o);
            }
            return false;
        });
        return this;
    }

    public <T> Set<T> allMetadata(Constraint constraint, Class<T> type) {
        PropertySet md = this.constraintMetadata.get(constraint);
        if (md != null) {
            return md.all(type);
        }
        return Collections.emptySet();
    }

    public <T> Optional<T> firstMetadata(Constraint constraint, Class<T> type) {
        PropertySet md = this.constraintMetadata.get(constraint);
        if (md != null) {
            return md.first(type);
        }
        return Optional.empty();
    }

    public <T> T firstOrMetadata(Constraint constraint, Class<T> type, T failback) {
        PropertySet md = this.constraintMetadata.get(constraint);
        if (md != null) {
            return md.firstOr(type, failback);
        }
        return failback;
    }

    public <T> boolean hasMetadata(Constraint constraint, Class<T> type) {
        PropertySet md = this.constraintMetadata.get(constraint);
        if (md != null) {
            return md.has(type);
        }
        return false;
    }

    public <T> long countMetadata(Constraint constraint, Class<T> type) {
        PropertySet md = this.constraintMetadata.get(constraint);
        if (md != null) {
            return md.count(type);
        }
        return 0;
    }

    public ConstraintBranch put(Constraint constraint, Constraint.Status status) {
        this.change(branch -> {
            Constraint.Status current = branch.constraints.get(constraint);
            branch.constraints.put(constraint, status);
            branch.typedConstraints.computeIfAbsent(constraint.getClass(), k -> new LinkedHashSet<>()).add(constraint);
            if (status == Constraint.Status.FALSE) branch.trimmed = true;
            return current == null || current != status;
        });
        return this;
    }

    public ConstraintBranch add(Constraint constraint, Constraint.Status status) {
        this.change(branch -> {
            Object prev = branch.constraints.putIfAbsent(constraint, status);
            branch.typedConstraints.computeIfAbsent(constraint.getClass(), k -> new LinkedHashSet<>()).add(constraint);
            if (prev == null && status == Constraint.Status.FALSE) branch.trimmed = true;
            return prev == null;
        });
        return this;
    }

    public ConstraintBranch reduce(Constraint result, Constraint... parents) {
        return this.reduce(result, Constraint.Status.UNKNOWN, parents);
    }

    public ConstraintBranch reduce(Constraint result, Constraint.Status status, Constraint... parents) {
        return this.reduce(result, status, new PropertySet(), parents);
    }

    public ConstraintBranch reduce(Constraint result, Constraint.Status status, PropertySet meta, Constraint... parents) {
        for (Constraint con : parents) {
            this.drop(con);
            PropertySet md = this.constraintMetadata.get(con);
            if (md != null) {
                meta.inheritFrom(md);
            }
        }

        this.set(result, status);
        this.addMetadata(result, meta);

        return this;
    }

    public ConstraintBranch drop(Constraint constraint) {
        this.change(branch -> {
            Constraint.Status prev = branch.constraints.remove(constraint);
            branch.constraintMetadata.remove(constraint);

            Set<Constraint> consTyped = branch.typedConstraints.get(constraint.getClass());
            if (consTyped != null) {
                consTyped.remove(constraint);
                if (consTyped.isEmpty()) {
                    branch.typedConstraints.remove(constraint.getClass());
                }
            }

            if (prev == Constraint.Status.FALSE) branch.trimmed = branch.status() == Constraint.Status.FALSE;
            return prev != null;
        });
        return this;
    }


    public ConstraintBranch set(Constraint constraint, Constraint.Status status) {
        this.change(branch -> {
            Constraint.Status curr = branch.constraints.get(constraint);
            if (curr == null || curr != status) {
                if (status == Constraint.Status.FALSE) branch.trimmed = true;
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
