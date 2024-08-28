package honeyroasted.almonds.solver.mappers;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintNode;
import honeyroasted.almonds.solver.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;

public class TrueConstraintMapper implements ConstraintMapper.Unary<Constraint.True> {
    public static ConstraintMapper INSTANCE = new TrueConstraintMapper();

    @Override
    public void process(PropertySet instanceContext, PropertySet branchContext, ConstraintNode node, Constraint.True constraint) {
        node.overrideStatus(ConstraintNode.Status.TRUE);
    }
}
