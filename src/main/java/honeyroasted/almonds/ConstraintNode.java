package honeyroasted.almonds;

import honeyroasted.collect.copy.Copyable;
import honeyroasted.collect.equivalence.Equivalence;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

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

    default ConstraintNode overrideStatus(boolean status) {
        return this.overrideStatus(Status.fromBoolean(status));
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

    Set<ConstraintLeaf> leaves();

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

    default void visit(Predicate<ConstraintNode> test, Consumer<ConstraintNode> action) {
        visit(test, c -> false, action);
    }

    void visit(Predicate<ConstraintNode> test, Predicate<ConstraintTree> snipper, Consumer<ConstraintNode> action);

    default void visitNeighbors(Operation operation, Predicate<ConstraintNode> test, Consumer<ConstraintNode> action) {
        this.root(operation).visit(test, ct -> ct.operation() != operation, action);
    }

    default Set<ConstraintNode> neighbors(Operation operation) {
        return neighbors(operation, c -> true);
    }

    default Set<ConstraintNode> neighbors(Operation operation, Status status) {
        return neighbors(operation, c -> c.status() == status);
    }

    default Set<ConstraintNode> neighbors(Operation operation, boolean status) {
        return neighbors(operation, c -> c.satisfied() == status);
    }

    default Set<ConstraintNode> neighbors(Operation operation, Predicate<ConstraintNode> test) {
        Set<ConstraintNode> neighbors = new LinkedHashSet<>();
        visitNeighbors(operation, test, neighbors::add);
        return neighbors;
    }

    default ConstraintNode root(Operation operation) {
        if (this.parent() != null && this.parent().operation() == operation) {
            return this.parent().root(operation);
        }
        return this;
    }

    default ConstraintNode root() {
        return this.parent() != null ? this.parent().root() : this;
    }

    default ConstraintTree expandRoot(Operation operation) {
        return root(operation).expandInPlace(operation);
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
