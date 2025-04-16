package honeyroasted.almonds;

public class Test {

    public static void main(String[] args) {
        BranchPriority p1 = new BranchPriority(0, 1).sub(4, 3, 0);
        BranchPriority p2 = new BranchPriority(0, 1).sub(4, 3);

        System.out.println(p1 + " cmp " + p2 + ": " + p1.compareTo(p2));
    }

}
