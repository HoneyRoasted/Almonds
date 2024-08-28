package honeyroasted.almonds.solver.mappers;

import honeyroasted.almonds.Constraint;
import honeyroasted.almonds.ConstraintNode;
import honeyroasted.almonds.ConstraintTree;
import honeyroasted.almonds.solver.ConstraintMapper;
import honeyroasted.collect.property.PropertySet;

import java.util.LinkedHashSet;
import java.util.stream.Collectors;

public class MultiConstraintMapper implements ConstraintMapper.Unary<Constraint.Multi> {
    public static ConstraintMapper INSTANCE = new MultiConstraintMapper();

    @Override
    public boolean filter(PropertySet instanceContext, PropertySet branchContext, ConstraintNode node, Constraint.Multi constraint) {
        return node.isLeaf() || (node instanceof ConstraintTree tree && tree.children().isEmpty());
    }

    @Override
    public void process(PropertySet instanceContext, PropertySet branchContext, ConstraintNode node, Constraint.Multi constraint) {
        node.expand(constraint.operation(),
                constraint.constraints().stream().map(Constraint::createLeaf).collect(Collectors.toCollection(LinkedHashSet::new)),
                false);
    }
}
