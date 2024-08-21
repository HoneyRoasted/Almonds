package honeyroasted.almonds;

import honeyroasted.collect.copy.Copyable;
import honeyroasted.collect.equivalence.Equivalence;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public sealed interface ConstraintNode extends Copyable<ConstraintNode, Void> permits ConstraintLeaf, ConstraintTree {
    Equivalence<ConstraintNode> STRUCTURAL = Equivalence.instancing(ConstraintNode.class, ConstraintLeaf.STRUCTURAL, ConstraintTree.STRUCTURAL);

    default boolean satisfied() {
        return this.status().asBoolean();
    }

    Status status();

    default boolean strictlySatisfied() {
        return this.strictStatus().asBoolean();
    }

    Status strictStatus();

    default void overrideStatus(boolean status) {
        this.overrideStatus(Status.fromBoolean(status));
    }

    ConstraintNode overrideStatus(Status status);

    default boolean statusCouldChange() {
        return strictStatus() == Status.UNKNOWN;
    }

    Set<ConstraintNode> children();

    ConstraintTree parent();

    ConstraintNode setParent(ConstraintTree parent);

    default Constraint constraint() {
        return trackedConstraint().constraint();
    }

    TrackedConstraint trackedConstraint();

    ConstraintNode updateConstraints();

    default ConstraintTree expand(Operation operation, Constraint... newChildren) {
        return this.expand(operation, Arrays.stream(newChildren).map(c -> c.tracked(this.trackedConstraint()).createLeaf()).toList());
    }

    default ConstraintTree expand(Operation operation, TrackedConstraint... newChildren) {
        return this.expand(operation, Arrays.stream(newChildren).map(TrackedConstraint::createLeaf).toList());
    }

    default ConstraintTree expand(Operation operation, ConstraintNode... newChildren) {
        return this.expand(operation, List.of(newChildren));
    }

    ConstraintTree expand(Operation operation, Collection<? extends ConstraintNode> newChildren);

    default ConstraintTree expandInPlace() {
        return expandInPlace(Operation.OR);
    }

    ConstraintTree expandInPlace(Operation defaultOp);

    ConstraintLeaf collapse();

    default Set<ConstraintNode> neighbors(Operation operation) {
        if (this.parent() != null && this.parent().operation() == operation) {
            Set<ConstraintNode> result = new LinkedHashSet<>();
            result.addAll(this.parent().children());
            result.addAll(this.parent().neighbors(operation));
            return result;
        }

        return Set.of(this);
    }

    default ConstraintNode root(Operation operation) {
        if (this.parent() != null && this.parent().operation() == operation){
            return this.parent().root(operation);
        }
        return this;
    }

    default ConstraintNode root() {
        return this.parent() != null ? this.parent().root() : this;
    }

    ConstraintNode flattenedForm();

    ConstraintNode disjunctiveForm();

    int size();

    boolean isLeaf();

    default boolean structuralEquals(Object other) {
        return STRUCTURAL.equals(this, other);
    }

    default int structuralHashCode() {
        return STRUCTURAL.hashCode(this);
    }

    void toString(List<String> building, boolean useSimpleName);

    String toString(boolean useSimpleName);

    String toEquationString();

    @Override
    default ConstraintNode copy() {
        return Copyable.super.copy();
    }

    enum Operation {
        AND("&"), OR("|");

        private String operator;

        Operation(String operator) {
            this.operator = operator;
        }

        public String operator() {
            return this.operator;
        }
    }

    enum Status {
        TRUE(true) {
            @Override
            public Status and(Status other) {
                return other;
            }

            @Override
            public Status or(Status other) {
                return this;
            }

            @Override
            public Status not() {
                return FALSE;
            }
        },
        FALSE(false) {
            @Override
            public Status and(Status other) {
                return this;
            }

            @Override
            public Status or(Status other) {
                return other;
            }

            @Override
            public Status not() {
                return TRUE;
            }
        },
        UNKNOWN(false) {
            @Override
            public Status and(Status other) {
                return other == FALSE ? other : this;
            }

            @Override
            public Status or(Status other) {
                return other == TRUE ? other : this;
            }

            @Override
            public Status not() {
                return this;
            }
        };

        private boolean value;

        Status(boolean value) {
            this.value = value;
        }

        public static Status fromBoolean(boolean value) {
            return value ? TRUE : FALSE;
        }

        public boolean asBoolean() {
            return this.value;
        }

        public abstract Status and(Status other);

        public abstract Status or(Status other);

        public abstract Status not();
    }

}
