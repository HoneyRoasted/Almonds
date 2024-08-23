package honeyroasted.almonds;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.function.Predicate;

public class ConstraintNodeVisitor implements Iterator<ConstraintNode> {
    private Deque<ConstraintNode> visiting = new ArrayDeque<>();
    private Predicate<ConstraintNode> snipper;
    private Predicate<ConstraintNode> tester;

    public ConstraintNodeVisitor(ConstraintNode node, Predicate<ConstraintNode> snipper, Predicate<ConstraintNode> tester) {
        this.visiting.add(node);
        this.snipper = snipper;
        this.tester = tester;
    }

    public ConstraintNodeVisitor(ConstraintNode node) {
        this(node, c -> false, c -> true);
    }

    @Override
    public boolean hasNext() {
        return this.visiting.stream().anyMatch(this.tester);
    }

    @Override
    public ConstraintNode next() {
        ConstraintNode next;
        do {
            next = this.visiting.pollFirst();
            if (!this.snipper.test(next)) {
                this.visiting.addAll(next.children());
            }
        } while (!this.tester.test(next));

        return next;
    }
}
