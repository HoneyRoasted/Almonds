package honeyroasted.almonds.solver.mappers;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintNode;
import honeyroasted.almonds.ConstraintTree;
import honeyroasted.almonds.solver.ConstraintMapper;

public class MultiConstraintMapper implements ConstraintMapper.Unary<Constraint.Multi> {
    ConstraintMapper INSTANCE = new MultiConstraintMapper();

    @Override
    public boolean filter(ConstraintNode node, Constraint.Multi constraint) {
        return node.isLeaf() || (node instanceof ConstraintTree tree && tree.children().isEmpty());
    }

    @Override
    public void process(Context context, ConstraintNode node, Constraint.Multi constraint) {
        ConstraintTree tree = node.expand(constraint.operation());
        constraint.constraints().forEach(cn -> tree.attach(cn.tracked().createLeaf()));
    }
}
