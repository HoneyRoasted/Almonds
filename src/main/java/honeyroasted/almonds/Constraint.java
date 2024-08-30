package honeyroasted.almonds;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public interface Constraint {
    Constraint TRUE = new True();
    Constraint FALSE = new False();

    String simpleName();

    List<Object> parameters();

    default  <T extends Constraint> T createNew(List<?> parameters) {
        try {
            return (T) getClass().getConstructors()[0].newInstance(parameters.toArray());
        } catch (ArrayIndexOutOfBoundsException | InstantiationException | IllegalAccessException |
                 InvocationTargetException e) {
            throw new IllegalStateException("Could not create new instance via reflection", e);
        }
    }

    static Constraint label(String name) {
        return new Label(name);
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
        public List<Object> parameters() {
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
        public List<Object> parameters() {
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
        public List<Object> parameters() {
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
        public List<Object> parameters() {
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

    enum Status {
        TRUE {
            @Override
            public boolean isTrue() {
                return true;
            }

            @Override
            public boolean isKnown() {
                return true;
            }

            @Override
            public Status and(Status other) {
                return other;
            }

            @Override
            public Status or(Status other) {
                return this;
            }
        },
        ASSUMED {
            @Override
            public boolean isTrue() {
                return true;
            }

            @Override
            public boolean isKnown() {
                return false;
            }

            @Override
            public Status and(Status other) {
                return other == TRUE ? this : other;
            }

            @Override
            public Status or(Status other) {
                return other == TRUE ? other : this;
            }
        },
        FALSE {
            @Override
            public boolean isTrue() {
                return false;
            }

            @Override
            public boolean isKnown() {
                return true;
            }

            @Override
            public Status and(Status other) {
                return this;
            }

            @Override
            public Status or(Status other) {
                return other;
            }
        },
        UNKNOWN {
            @Override
            public boolean isTrue() {
                return false;
            }

            @Override
            public boolean isKnown() {
                return false;
            }

            @Override
            public Status and(Status other) {
                return other == FALSE ? other : this;
            }

            @Override
            public Status or(Status other) {
                return other == TRUE || other == ASSUMED ? other : this;
            }
        };


        public static Status known(boolean value) {
            return value ? TRUE : FALSE;
        }

        public static Status unknown(boolean value) {
            return value ? ASSUMED : UNKNOWN;
        }

        public abstract boolean isTrue();

        public boolean isFalse() {
            return !isTrue();
        }

        public abstract boolean isKnown();

        public boolean isUnknown() {
            return !isKnown();
        }

        public abstract Status and(Status other);

        public abstract Status or(Status other);
    }

}
