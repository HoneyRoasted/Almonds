package honeyroasted.almonds;

public class Test {

    public static void main(String[] args) {
        Constraint a = Constraint.label("a");
        Constraint b = Constraint.label("b");

        Constraint c = Constraint.label("c");
        Constraint d = Constraint.label("d");

        Constraint e = Constraint.label("e");
        Constraint f = Constraint.label("f");

        ConstraintTree and = new ConstraintTree(Constraint.multi(ConstraintNode.Operation.AND,
                Constraint.multi(ConstraintNode.Operation.OR, a, b),
                Constraint.multi(ConstraintNode.Operation.OR, c, d),
                Constraint.multi(ConstraintNode.Operation.OR, e, f)).tracked(), ConstraintNode.Operation.AND);


        ConstraintTree aOrB = new ConstraintTree(Constraint.or().tracked(), ConstraintNode.Operation.OR);
        aOrB.attach(a.tracked().createLeaf(), b.tracked().createLeaf());
        and.attach(aOrB);


        ConstraintTree cOrD = new ConstraintTree(Constraint.or().tracked(), ConstraintNode.Operation.OR);
        cOrD.attach(c.tracked().createLeaf(), d.tracked().createLeaf());
        and.attach(cOrD);

        ConstraintTree eOrF = new ConstraintTree(Constraint.or().tracked(), ConstraintNode.Operation.OR);
        eOrF.attach(e.tracked().createLeaf(), f.tracked().createLeaf());
        and.attach(eOrF);


        System.out.println(and.toEquationString());
        System.out.println("=================================================================");
        System.out.println(and.disjunctiveForm().toEquationString());
        System.out.println("=================================================================");
        System.out.println(and.disjunctiveForm());
    }

}
