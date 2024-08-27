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
                Constraint.multi(ConstraintNode.Operation.OR, e, f)), ConstraintNode.Operation.AND);


        ConstraintTree aOrB = new ConstraintTree(Constraint.or(), ConstraintNode.Operation.OR);
        aOrB.attach(a, b);
        and.attach(aOrB);


        ConstraintTree cOrD = new ConstraintTree(Constraint.or(), ConstraintNode.Operation.OR);
        cOrD.attach(c, d);
        and.attach(cOrD);

        ConstraintTree eOrF = new ConstraintTree(Constraint.or(), ConstraintNode.Operation.OR);
        eOrF.attach(e, f);
        and.attach(eOrF);

        System.out.println(and.disjunctiveForm().flattenedForm().toString(true));
    }

}
