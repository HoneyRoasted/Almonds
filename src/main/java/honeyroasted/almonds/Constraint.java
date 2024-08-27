package honeyroasted.almonds;

import honeyroasted.almonds.solver.ConstraintMapper;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public interface Constraint {
    Constraint TRUE = new True();
    Constraint FALSE = new False();

    String simpleName();

    List<?> parameters();

    default  <T extends Constraint> T createNew(List<?> parameters) {
        try {
            return (T) getClass().getConstructors()[0].newInstance(parameters.toArray());
        } catch (ArrayIndexOutOfBoundsException | InstantiationException | IllegalAccessException |
                 InvocationTargetException e) {
            throw new IllegalStateException("Could not create new instance via reflection", e);
        }
    }
    default TrackedConstraint tracked() {
        return TrackedConstraint.of(this);
    }

    default TrackedConstraint tracked(TrackedConstraint... parents) {
        return TrackedConstraint.of(this, parents);
    }

    static Constraint preserved(Constraint constraint) {
        return new Information(constraint);
    }

    static Constraint label(String name) {
        return new Label(name);
    }

    static Constraint multi(ConstraintNode.Operation operation, Collection<Constraint> children) {
        return new Multi(operation, new LinkedHashSet<>(children));
    }

    static Constraint multi(ConstraintNode.Operation operation, Constraint... constraints) {
        Set<Constraint> children = new LinkedHashSet<>();
        Collections.addAll(children, constraints);
        return new Multi(operation, children);
    }

    static Constraint solve() {
        return new Solve();
    }

    static Constraint and() {
        return new And();
    }

    static Constraint or() {
        return new Or();
    }

    abstract class Unary<T> implements Constraint {
        private T value;

        public Unary(T value) {
            this.value = value;
        }

        public T value() {
            return this.value;
        }


        @Override
        public List<?> parameters() {
            return List.of(this.value);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Unary<?> unary = (Unary<?>) o;
            return Objects.equals(value, unary.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }
    }

    abstract class Binary<L, R> implements Constraint {
        private L left;
        private R right;

        public Binary(L left, R right) {
            this.left = left;
            this.right = right;
        }

        public L left() {
            return this.left;
        }

        public R right() {
            return this.right;
        }

        @Override
        public List<?> parameters() {
            return List.of(this.left, this.right);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Binary<?, ?> binary = (Binary<?, ?>) o;
            return Objects.equals(left, binary.left) && Objects.equals(right, binary.right);
        }

        @Override
        public int hashCode() {
            return Objects.hash(left, right);
        }
    }

    abstract class Trinary<L, M, R> implements Constraint {
        private L left;
        private M middle;
        private R right;

        public Trinary(L left, M middle, R right) {
            this.left = left;
            this.middle = middle;
            this.right = right;
        }

        public L left() {
            return this.left;
        }

        public M middle() {
            return this.middle;
        }

        public R right() {
            return this.right;
        }

        @Override
        public List<?> parameters() {
            return List.of(this.left, this.middle, this.right);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Trinary<?, ?, ?> trinary = (Trinary<?, ?, ?>) o;
            return Objects.equals(left, trinary.left) && Objects.equals(middle, trinary.middle) && Objects.equals(right, trinary.right);
        }

        @Override
        public int hashCode() {
            return Objects.hash(left, middle, right);
        }
    }

    abstract class UniqueNamed implements Constraint {
        private String name;

        public UniqueNamed(String name) {
            this.name = name;
        }

        @Override
        public String simpleName() {
            return this.name;
        }

        @Override
        public String toString() {
            return simpleName();
        }

        @Override
        public List<?> parameters() {
            return Collections.emptyList();
        }
    }

    class Solve extends UniqueNamed {
        public Solve() {
            super("Solve child constraints");
        }
    }

    class And extends UniqueNamed {
        public And() {
            super("and");
        }
    }

    class Or extends UniqueNamed {
        public Or() {
            super("or");
        }
    }

    class True extends UniqueNamed {
        public True() {
            super("true");
        }
    }

    class False extends UniqueNamed {
        public False() {
            super("false");
        }
    }

    class Label extends Unary<String> {

        public Label(String value) {
            super(value);
        }

        @Override
        public String simpleName() {
            return this.value();
        }


        @Override
        public <T extends Constraint> T createNew(List<?> parameters) {
            return (T) new Label(String.valueOf(parameters.get(0)));
        }

        @Override
        public String toString() {
            return "LABEL('" + this.value() + "')";
        }

    }

    class Information extends Unary<Constraint> {

        public Information(Constraint value) {
            super(value);
        }

        @Override
        public String simpleName() {
            return "info(" + this.value().simpleName() + ")";
        }

        @Override
        public String toString() {
            return "INFORMATIONAL: " + this.value();
        }
    }

    class Multi implements Constraint {
        private ConstraintNode.Operation operation;
        private Set<Constraint> constraints;

        public Multi(ConstraintNode.Operation operation, Set<Constraint> constraints) {
            this.operation = operation;
            this.constraints = constraints;
        }

        public ConstraintNode.Operation operation() {
            return this.operation;
        }

        public Set<Constraint> constraints() {
            return this.constraints;
        }

        @Override
        public String simpleName() {
            return "(" + this.constraints.stream().map(Constraint::simpleName).collect(Collectors.joining(" " + this.operation.operator() + " ")) + ")";
        }

        @Override
        public String toString() {
            return this.operation + "(" + this.constraints.stream().map(Constraint::toString).collect(Collectors.joining(", ")) + ")";
        }

        @Override
        public List<?> parameters() {
            List<Object> params = new ArrayList<>();
            params.add(this.operation);
            params.addAll(this.constraints);
            return params;
        }

        @Override
        public <T extends Constraint> T createNew(List<?> parameters) {
            return (T) new Multi((ConstraintNode.Operation) parameters.get(0), parameters.subList(1, parameters.size()).stream().map(t -> (Constraint) t).collect(Collectors.toCollection(LinkedHashSet::new)));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Multi multi = (Multi) o;
            return operation == multi.operation && Objects.equals(constraints, multi.constraints);
        }

        @Override
        public int hashCode() {
            return Objects.hash(operation, constraints);
        }
    }

}
