package honeyroasted.almonds;

import honeyroasted.collect.copy.Copyable;
import honeyroasted.collect.equivalence.Equivalence;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public sealed interface ConstraintNode extends Copyable<ConstraintNode, Void>, Iterable<ConstraintNode> permits ConstraintLeaf, ConstraintTree {
    static Equivalence<ConstraintNode> structural() {
        return Equivalence.instancing(ConstraintNode.class, ConstraintLeaf.STRUCTURAL, ConstraintTree.STRUCTURAL);
    }

    default boolean satisfied() {
        return this.status().asBoolean();
    }

    @Override
    default Iterator<ConstraintNode> iterator() {
        return new ConstraintNodeVisitor(this);
    }

    default Iterator<ConstraintNode> iterator(Predicate<ConstraintNode> test, Predicate<ConstraintNode> snipper) {
        return new ConstraintNodeVisitor(this, snipper, test);
    }

    @Override
    default Spliterator<ConstraintNode> spliterator() {
        return Spliterators.spliterator(this.iterator(), this.size(), Spliterator.SIZED);
    }

    default Spliterator<ConstraintNode> spliterator(Predicate<ConstraintNode> test, Predicate<ConstraintNode> snipper) {
        return Spliterators.spliteratorUnknownSize(this.iterator(test, snipper), 0);
    }


    default Stream<ConstraintNode> stream() {
        return StreamSupport.stream(this.spliterator(), false);
    }

    default Stream<ConstraintNode> stream(Predicate<ConstraintNode> test, Predicate<ConstraintNode> snipper) {
        return StreamSupport.stream(this.spliterator(test, snipper), false);
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

    Constraint constraint();

    default boolean satisfied(Constraint constraint) {
        return this.stream().anyMatch(cn -> cn.constraint().equals(constraint) && cn.satisfied());
    }

    Set<ConstraintLeaf> leaves();

    default ConstraintTree expand(Operation operation, boolean preserve, Constraint... newChildren) {
        return this.expand(operation, Arrays.stream(newChildren).map(Constraint::createLeaf).toList(), preserve);
    }

    default ConstraintTree expand(Operation operation, boolean preserve, ConstraintNode... newChildren) {
        return this.expand(operation, List.of(newChildren), preserve);
    }

    default ConstraintTree expandInPlace(boolean preserve) {
        return expandInPlace(Operation.OR, preserve);
    }

    ConstraintTree expand(Operation operation, Collection<? extends ConstraintNode> newChildren, boolean preserve);

    ConstraintTree expandInPlace(Operation defaultOp, boolean preserve);

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

    default ConstraintTree expandRoot(Operation operation, boolean preserve) {
        return root(operation).expandInPlace(operation, preserve);
    }

    ConstraintNode flattenedForm();

    ConstraintNode disjunctiveForm();

    int size();

    boolean isLeaf();

    default boolean structuralEquals(Object other) {
        return structural().equals(this, other);
    }

    default int structuralHashCode() {
        return structural().hashCode(this);
    }

    void toString(List<String> building, boolean useSimpleName);

    String toString(boolean useSimpleName);

    String toEquationString();

    @Override
    default ConstraintNode copy() {
        return copy(null);
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
        },
        INFORMATION(true) {
            @Override
            public Status and(Status other) {
                return other;
            }

            @Override
            public Status or(Status other) {
                return other;
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
    }

}
