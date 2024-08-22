package honeyroasted.almonds.solver.mappers;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintNode;
import honeyroasted.almonds.solver.ConstraintMapper;

public class FalseConstraintMapper implements ConstraintMapper.Unary<Constraint.False> {
    public static ConstraintMapper INSTANCE = new FalseConstraintMapper();

    @Override
    public void process(Context context, ConstraintNode node, Constraint.False constraint) {
        node.overrideStatus(ConstraintNode.Status.FALSE);
    }
}
