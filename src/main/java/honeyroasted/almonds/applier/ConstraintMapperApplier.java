package honeyroasted.almonds.applier;

import honeyroasted.almonds.ConstraintMapper;
import honeyroasted.almonds.ConstraintTree;

import java.util.List;

public interface ConstraintMapperApplier extends ConstraintMapper {

    List<ConstraintMapper> mappers();

    List<ConstraintMapper> flattened();

    void accept(ConstraintTree tree);

    static ConstraintMapperApplier of(List<ConstraintMapper> mappers, Type type) {
        return switch (type) {
            case EXHAUSTIVE -> new ExhaustiveConstraintMapperApplier(mappers);
            case ORDERED -> new OrderedConstraintMapperApplier(mappers);
            case UNTRIMMED -> new UntrimmedConstraintMapperApplier(mappers);
        };
    }

    enum Type {
        EXHAUSTIVE,
        ORDERED,
        UNTRIMMED
    }

}
