package honeyroasted.almonds.solver;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintNode;
import honeyroasted.collect.property.PropertySet;

import java.lang.reflect.ParameterizedType;

public interface ConstraintMapper {
    String DISCARD_BRANCH = "honeyroasted.almonds.discard_branch";
    String REPLACE_BRANCH = "honeyroasted.almonds.replace_branch";

    int ALL_BRANCH_NODES = -1;
    int PARENT_BRANCH_NODE = 0;

    default int arity() {
        return ALL_BRANCH_NODES;
    }

    default boolean commutative() {
        return true;
    }

    boolean filter(PropertySet context, ConstraintNode node);

    boolean accepts(PropertySet context, ConstraintNode... nodes);

    void process(PropertySet context, ConstraintNode... nodes);

    interface Unary<T extends Constraint> extends ConstraintMapper {
        @Override
        default int arity() {
            return 1;
        }

        @Override
        default boolean filter(PropertySet context, ConstraintNode node) {
            return node.status() != ConstraintNode.Status.INFORMATION &&
                    type().isInstance(node.constraint()) &&
                    filter(context, node, (T) node.constraint());
        }

        default boolean filter(PropertySet context, ConstraintNode node, T constraint) {
            return true;
        }


        default Class<T> type() {
            return (Class<T>) ((ParameterizedType) getClass().getGenericInterfaces()[0]).getActualTypeArguments()[0];
        }

        @Override
        default boolean accepts(PropertySet context, ConstraintNode... nodes) {
            return true;
        }

        @Override
        default void process(PropertySet context, ConstraintNode... nodes) {
            process(context, nodes[0], (T) nodes[0].constraint());
        }

        void process(PropertySet context, ConstraintNode node, T constraint);
    }

    interface Binary<L extends Constraint, R extends Constraint> extends ConstraintMapper {

        @Override
        default int arity() {
            return 2;
        }

        @Override
        default boolean filter(PropertySet context, ConstraintNode node) {
            return node.status() != ConstraintNode.Status.INFORMATION &&
                    (leftType().isInstance(node.constraint()) && filterLeft(context, node, (L) node.constraint())) ||
                    (rightType().isInstance(node.constraint()) && filterRight(context, node, (R) node.constraint()));
        }

        default Class<L> leftType() {
            return (Class<L>) ((ParameterizedType) getClass().getGenericInterfaces()[0]).getActualTypeArguments()[0];
        }

        default Class<R> rightType() {
            return (Class<R>) ((ParameterizedType) getClass().getGenericInterfaces()[0]).getActualTypeArguments()[1];
        }

        @Override
        default boolean accepts(PropertySet context, ConstraintNode... nodes) {
            if (leftType().isInstance(nodes[0].constraint()) && rightType().isInstance(nodes[1].constraint())) {
                return filter(context, nodes[0], (L) nodes[0].constraint(), nodes[1], (R) nodes[1].constraint());
            } else if (this.commutative() && rightType().isInstance(nodes[0].constraint()) && leftType().isInstance(nodes[1].constraint())) {
                return filter(context, nodes[1], (L) nodes[1].constraint(), nodes[0], (R) nodes[0].constraint());
            }
            return false;
        }

        @Override
        default void process(PropertySet context, ConstraintNode... nodes) {
            if (leftType().isInstance(nodes[0].constraint()) && rightType().isInstance(nodes[1].constraint())) {
                process(context, nodes[0], (L) nodes[0].constraint(), nodes[1], (R) nodes[1].constraint());
            } else if (this.commutative() && rightType().isInstance(nodes[0].constraint()) && leftType().isInstance(nodes[1].constraint())) {
                process(context, nodes[1], (L) nodes[1].constraint(), nodes[0], (R) nodes[0].constraint());
            }
        }

        default boolean filterLeft(PropertySet context, ConstraintNode node, L constraint) {
            return true;
        }

        default boolean filterRight(PropertySet context, ConstraintNode node, R constraint) {
            return true;
        }

        default boolean filter(PropertySet context, ConstraintNode leftNode, L leftConstraint, ConstraintNode rightNode, R rightConstraint) {
            return true;
        }

        void process(PropertySet context, ConstraintNode leftNode, L leftConstraint, ConstraintNode rightNode, R rightConstraint);

    }

}
