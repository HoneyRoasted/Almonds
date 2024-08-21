package honeyroasted.almonds;

import honeyroasted.collect.copy.Copyable;
import honeyroasted.collect.equivalence.Equivalence;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public sealed interface ConstraintNode extends Copyable<ConstraintNode, Void> permits ConstraintLeaf, ConstraintTree {
    Equivalence<ConstraintNode> STRUCTURAL = Equivalence.instancing(ConstraintNode.class, ConstraintLeaf.STRUCTURAL, ConstraintTree.STRUCTURAL);

    Status status();

    Status strictStatus();

    void overrideStatus(Status status);

    default boolean statusCouldChange() {
        return strictStatus() == Status.UNKNOWN;
    }

    Set<ConstraintNode> children();

    ConstraintTree parent();

    void setParent(ConstraintTree parent);

    default Constraint constraint() {
        return trackedConstraint().constraint();
    }

    TrackedConstraint trackedConstraint();

    void updateConstraints();

    ConstraintTree expand(Operation operation, Collection<ConstraintNode> newChildren);

    ConstraintLeaf collapse();

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

        public boolean value() {
            return this.value;
        }

        public abstract Status and(Status other);

        public abstract Status or(Status other);

        public abstract Status not();
    }

}
