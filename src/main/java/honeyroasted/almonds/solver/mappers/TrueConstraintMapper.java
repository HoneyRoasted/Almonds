package honeyroasted.almonds.solver.mappers;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintNode;
import honeyroasted.almonds.solver.ConstraintMapper;

public class TrueConstraintMapper implements ConstraintMapper.Unary<Constraint.True> {
    ConstraintMapper INSTANCE = new TrueConstraintMapper();

    @Override
    public void process(Context context, ConstraintNode node, Constraint.True constraint) {
        node.overrideStatus(ConstraintNode.Status.TRUE);
    }
}
