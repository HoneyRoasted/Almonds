package honeyroasted.almonds.solver.mappers;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintNode;
import honeyroasted.almonds.solver.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;

public class FalseConstraintMapper implements ConstraintMapper.Unary<Constraint.False> {
    public static ConstraintMapper INSTANCE = new FalseConstraintMapper();

    @Override
    public void process(PropertySet context, ConstraintNode node, Constraint.False constraint) {
        node.overrideStatus(ConstraintNode.Status.FALSE);
    }
}
