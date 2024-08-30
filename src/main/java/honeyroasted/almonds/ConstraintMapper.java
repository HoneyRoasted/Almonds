package honeyroasted.almonds;

import honeyroasted.collect.property.PropertySet;

import java.lang.reflect.ParameterizedType;
import java.util.Map;
import java.util.function.Consumer;

public interface ConstraintMapper extends Consumer<ConstraintBranch> {

    class True extends Unary<Constraint.True> {
        @Override
        protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, Constraint.True constraint, Constraint.Status status) {
            return status != Constraint.Status.TRUE;
        }

        @Override
        protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, Constraint.True constraint, Constraint.Status status) {
            branch.setStatus(constraint, Constraint.Status.TRUE);
        }
    }

    class False extends Unary<Constraint.False> {
        @Override
        protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, Constraint.False constraint, Constraint.Status status) {
            return status != Constraint.Status.FALSE;
        }

        @Override
        protected void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, Constraint.False constraint, Constraint.Status status) {
            branch.setStatus(constraint, Constraint.Status.FALSE);
        }
    }

    abstract class All implements ConstraintMapper {

        @Override
        public void accept(ConstraintBranch branch) {
            PropertySet allContext = branch.parent().metadata();
            PropertySet branchContext = branch.metadata();
            this.accept(allContext, branchContext, branch);
        }

        protected abstract void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch);
    }

    abstract class Unary<T extends Constraint> implements ConstraintMapper {
        private Class<T> type;

        public Unary(Class<T> type) {
            this.type = type;
        }

        public Unary() {
            this.type = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        }

        @Override
        public void accept(ConstraintBranch branch) {
            PropertySet allContext = branch.parent().metadata();
            PropertySet branchContext = branch.metadata();

            for (Map.Entry<Constraint, Constraint.Status> entry : branch.constraints().entrySet()) {
                if (this.type.isInstance(entry.getKey())) {
                    T con = (T) entry.getKey();
                    if (this.filter(allContext, branchContext, branch, con, entry.getValue())) {
                        this.accept(allContext, branchContext, branch, con, entry.getValue());
                        if (!branch.parent().has(branch)) return;
                    }
                }
            }
        }

        protected boolean filter(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, T constraint, Constraint.Status status) {
            return true;
        }

        protected abstract void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, T constraint, Constraint.Status status);
    }

    abstract class Binary<L extends Constraint, R extends Constraint> implements ConstraintMapper {
        private Class<L> left;
        private Class<R> right;

        public Binary(Class<L> left, Class<R> right) {
            this.left = left;
            this.right = right;
        }

        public Binary() {
            this.left = (Class<L>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
            this.right = (Class<R>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[1];
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

        @Override
        public void accept(ConstraintBranch branch) {
            PropertySet allContext = branch.parent().metadata();
            PropertySet branchContext = branch.metadata();

            for (Map.Entry<Constraint, Constraint.Status> leftEntry : branch.constraints().entrySet()) {
                if (this.left.isInstance(leftEntry.getKey())) {
                    L left = (L) leftEntry.getKey();
                    if (this.filterLeft(allContext, branchContext, branch, left, leftEntry.getValue())) {
                        for (Map.Entry<Constraint, Constraint.Status> rightEntry : branch.constraints().entrySet()) {
                            if (leftEntry.getValue() != rightEntry.getValue()) {
                                if (this.right.isInstance(rightEntry.getKey())) {
                                    R right = (R) rightEntry.getKey();
                                    if (this.filterRight(allContext, branchContext, branch, right, rightEntry.getValue())) {
                                        if (this.filter(allContext, branchContext, branch, left, leftEntry.getValue(), right, rightEntry.getValue())) {
                                            accept(allContext, branchContext, branch, left, leftEntry.getValue(), right, rightEntry.getValue());
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

        protected abstract void accept(PropertySet allContext, PropertySet branchContext, ConstraintBranch branch, L leftConstraint, Constraint.Status leftStatus, R rightConstraint, Constraint.Status rightStatus);
    }

}
