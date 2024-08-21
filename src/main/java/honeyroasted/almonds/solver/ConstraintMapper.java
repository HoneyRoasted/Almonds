package honeyroasted.almonds.solver;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintNode;
import honeyroasted.almonds.ConstraintTree;

import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.Map;

public interface ConstraintMapper {
    String DISCARD_BRANCH = "honeyroasted.almonds.discard_branch";
    String REPLACE_BRANCH = "honeyroasted.almonds.replace_branch";

    default int arity() {
        return -1;
    }

    default boolean commutative() {
        return true;
    }

    boolean filter(ConstraintNode node);

    boolean accepts(ConstraintNode... nodes);

    void process(Context context, ConstraintNode... nodes);

    class Context {
        private Map<String, Object> global = new HashMap<>();

        public Context inheritProperties(Context other) {
            other.global.forEach((k, v) -> this.global.putIfAbsent(k, v));
            return this;
        }

        public Context attach(String name, Object value) {
            this.global.put(name, value);
            return this;
        }

        public Context remove(String name) {
            this.global.remove(name);
            return this;
        }

        public boolean hasProperty(String name) {
            return this.global.containsKey(name);
        }

        public <T> T property(String name) {
            return (T) this.global.get(name);
        }

        public Context and(ConstraintNode current, ConstraintNode... toAdd) {
            if (current.parent() != null && current.parent().operation() == ConstraintNode.Operation.AND) {
                current.parent().attach(toAdd);
            } else {
                ConstraintTree parent = current.parent();

                ConstraintTree expanded = new ConstraintTree(Constraint.and().tracked(), ConstraintNode.Operation.AND);
                expanded.attach(current);
                expanded.attach(toAdd);

                if (parent != null) {
                    parent.children().remove(current);
                    parent.attach(expanded);
                }
            }

            return this;
        }
    }

    interface Unary<T extends Constraint> extends ConstraintMapper {
        @Override
        default int arity() {
            return 1;
        }

        @Override
        default boolean filter(ConstraintNode node) {
            return node.statusCouldChange() &&
                    type().isInstance(node.constraint()) &&
                    filter(node, (T) node.constraint());
        }

        default boolean filter(ConstraintNode node, T constraint) {
            return true;
        }


        default Class<T> type() {
            return (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        }

        @Override
        default boolean accepts(ConstraintNode... nodes) {
            return true;
        }

        @Override
        default void process(Context context, ConstraintNode... nodes) {
            process(context, nodes[0], (T) nodes[0].constraint());
        }

        void process(Context context, ConstraintNode node, T constraint);
    }

    interface Binary<L extends Constraint, R extends Constraint> extends ConstraintMapper {

        @Override
        default int arity() {
            return 2;
        }

        @Override
        default boolean filter(ConstraintNode node) {
            return node.statusCouldChange() &&
                    ((leftType().isInstance(node.constraint()) && filterLeft(node, (L) node.constraint())) ||
                            (rightType().isInstance(node.constraint()) && filterRight(node, (R) node.constraint())));
        }

        default Class<L> leftType() {
            return (Class<L>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        }

        default Class<R> rightType() {
            return (Class<R>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[1];
        }

        @Override
        default boolean accepts(ConstraintNode... nodes) {
            if (leftType().isInstance(nodes[0].constraint()) && rightType().isInstance(nodes[1].constraint())) {
                return filter(nodes[0], (L) nodes[0].constraint(), nodes[1], (R) nodes[1].constraint());
            } else if (this.commutative() && rightType().isInstance(nodes[0].constraint()) && leftType().isInstance(nodes[1].constraint())) {
                return filter(nodes[1], (L) nodes[1].constraint(), nodes[0], (R) nodes[0].constraint());
            }
            return false;
        }

        @Override
        default void process(Context context, ConstraintNode... nodes) {
            if (leftType().isInstance(nodes[0].constraint()) && rightType().isInstance(nodes[1].constraint())) {
                process(context, nodes[0], (L) nodes[0].constraint(), nodes[1], (R) nodes[1].constraint());
            } else if (this.commutative() && rightType().isInstance(nodes[0].constraint()) && leftType().isInstance(nodes[1].constraint())) {
                process(context, nodes[1], (L) nodes[1].constraint(), nodes[0], (R) nodes[0].constraint());
            }
        }

        default boolean filterLeft(ConstraintNode node, L constraint) {
            return true;
        }

        default boolean filterRight(ConstraintNode node, R constraint) {
            return true;
        }

        default boolean filter(ConstraintNode leftNode, L leftConstraint, ConstraintNode rightNode, R rightConstraint) {
            return true;
        }

        void process(Context context, ConstraintNode leftNode, L leftConstraint, ConstraintNode rightNode, R rightConstraint);

    }

}
