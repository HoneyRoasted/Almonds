package honeyroasted.almonds;

import honeyroasted.collect.property.PropertySet;

import java.lang.reflect.ParameterizedType;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public interface ConstraintMapper extends Consumer<ConstraintBranch> {

    class True extends Unary<Constraint.True> {
        @Override
        protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, Constraint.True constraint, Constraint.Status status) {
            return status != Constraint.Status.TRUE;
        }

        @Override
        protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, Constraint.True constraint, Constraint.Status status) {
            branch.set(constraint, Constraint.Status.TRUE);
        }
    }

    class False extends Unary<Constraint.False> {
        @Override
        protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, Constraint.False constraint, Constraint.Status status) {
            return status != Constraint.Status.FALSE;
        }

        @Override
        protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, Constraint.False constraint, Constraint.Status status) {
            branch.set(constraint, Constraint.Status.FALSE);
        }
    }

    abstract class All implements ConstraintMapper {
        protected abstract boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch);

        protected abstract void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch);

        @Override
        public final void accept(ConstraintBranch branch) {
            PropertySet allContext = branch.parent().metadata();
            PropertySet branchContext = branch.metadata();
            if (filter(allContext, branchContext, branch)) {
                this.accept(allContext, branchContext, branch);
            }
        }
    }

    abstract class Unary<T extends Constraint> implements ConstraintMapper {
        private Class<T> type;
        private boolean strict;

        public Unary(Class<T> type, boolean strict) {
            this.type = type;
            this.strict = true;
        }

        public Unary(boolean strict) {
            this.type = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
            this.strict = strict;
        }

        public Unary() {
            this(true);
        }

        protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, T constraint, Constraint.Status status) {
            return true;
        }

        protected abstract void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, T constraint, Constraint.Status status);

        @Override
        public final void accept(ConstraintBranch branch) {
            PropertySet allContext = branch.parent().metadata();
            PropertySet branchContext = branch.metadata();

            if (this.strict) {
                for (Constraint constraint : branch.typedConstraints().getOrDefault(this.type, Collections.emptySet())) {
                    T con = (T) constraint;
                    Constraint.Status status = branch.status(constraint);
                    if (this.filter(allContext, branchContext, branch, con, status)) {
                        this.accept(allContext, branchContext, branch, con, status);
                        if (!branch.parent().has(branch)) return;
                    }
                }
            } else {
                for (Map.Entry<Class<? extends Constraint>, Set<Constraint>> entry : branch.typedConstraints().entrySet()) {
                    Class<? extends Constraint> t = entry.getKey();
                    Set<Constraint> cons = entry.getValue();
                    if (this.type.isAssignableFrom(t)) {
                        for (Constraint constraint : cons) {
                            T con = (T) constraint;
                            Constraint.Status status = branch.status(constraint);
                            if (this.filter(allContext, branchContext, branch, con, status)) {
                                this.accept(allContext, branchContext, branch, con, status);
                                if (!branch.parent().has(branch)) return;
                            }
                        }
                    }
                }
            }
        }
    }

    abstract class Binary<L extends Constraint, R extends Constraint> implements ConstraintMapper {
        private Class<L> left;
        private Class<R> right;
        private boolean strict;

        public Binary(Class<L> left, Class<R> right, boolean strict) {
            this.left = left;
            this.right = right;
            this.strict = strict;
        }

        public Binary(boolean strict) {
            this.left = (Class<L>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
            this.right = (Class<R>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[1];
            this.strict = strict;
        }

        public Binary() {
            this(true);
        }

        protected boolean filterLeft(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, L constraint, Constraint.Status status) {
            return true;
        }

        protected boolean filterRight(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, R constraint, Constraint.Status status) {
            return true;
        }

        protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, L leftConstraint, Constraint.Status leftStatus, R rightConstraint, Constraint.Status rightStatus) {
            return true;
        }

        protected abstract void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, L leftConstraint, Constraint.Status leftStatus, R rightConstraint, Constraint.Status rightStatus);

        @Override
        public final void accept(ConstraintBranch branch) {
            PropertySet allContext = branch.parent().metadata();
            PropertySet branchContext = branch.metadata();

            if (this.strict) {
                for (Constraint leftConstraint : branch.typedConstraints().getOrDefault(this.left, Collections.emptySet())) {
                    L left = (L) leftConstraint;
                    Constraint.Status leftStat = branch.status(leftConstraint);
                    if (this.filterLeft(allContext, branchContext, branch, left, leftStat)) {
                        for (Constraint rightConstraint : branch.typedConstraints().getOrDefault(this.right, Collections.emptySet())) {
                            R right = (R) rightConstraint;
                            Constraint.Status rightStat = branch.status(rightConstraint);
                            if (this.filterRight(allContext, branchContext, branch, right, rightStat)) {
                                this.accept(allContext, branchContext, branch, left, leftStat, right, rightStat);
                                if (!branch.parent().has(branch)) return;
                            }
                        }
                    }
                }
            } else {
                for (Map.Entry<Class<? extends Constraint>, Set<Constraint>> leftCons : branch.typedConstraints().entrySet()) {
                    if (this.left.isAssignableFrom(leftCons.getKey())) {
                        for (Map.Entry<Class<? extends Constraint>, Set<Constraint>> rightCons : branch.typedConstraints().entrySet()) {
                            if (this.right.isAssignableFrom(rightCons.getKey())) {
                                for (Constraint leftConstraint : leftCons.getValue()) {
                                    L left = (L) leftConstraint;
                                    Constraint.Status leftStat = branch.status(leftConstraint);
                                    if (this.filterLeft(allContext, branchContext, branch, left, leftStat)) {
                                        for (Constraint rightConstraint : rightCons.getValue()) {
                                            R right = (R) rightConstraint;
                                            Constraint.Status rightStat = branch.status(rightConstraint);
                                            if (this.filterRight(allContext, branchContext, branch, right, rightStat)) {
                                                this.accept(allContext, branchContext, branch, left, leftStat, right, rightStat);
                                                if (!branch.parent().has(branch)) return;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

}
