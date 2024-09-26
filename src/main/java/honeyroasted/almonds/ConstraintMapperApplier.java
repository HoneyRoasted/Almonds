package honeyroasted.almonds;

import java.util.List;

public interface ConstraintMapperApplier extends ConstraintMapper {

    List<ConstraintMapper> mappers();

    List<ConstraintMapper> flattened();

    void accept(ConstraintTree tree);

    static ConstraintMapperApplier of(List<ConstraintMapper> mappers, boolean exhaustive) {
        return exhaustive ? new ExhaustiveConstraintMapperApplier(mappers) : new PriorityConstraintMapperApplier(mappers);
    }

}
