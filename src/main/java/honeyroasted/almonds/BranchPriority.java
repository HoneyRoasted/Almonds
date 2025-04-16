package honeyroasted.almonds;

import java.util.Arrays;
import java.util.Comparator;

public record BranchPriority(int... priority) implements Comparable<BranchPriority> {

    public static int compare(BranchPriority o1, BranchPriority o2) {
        for (int i = 0; i < o1.priority.length && i < o2.priority.length; i++) {
            int cmp = Integer.compare(o1.priority[i], o2.priority[i]);
            if (cmp != 0) {
                return cmp;
            }
        }

        return Integer.compare(o1.priority.length, o2.priority.length);
    }

    public BranchPriority sub(int... next) {
        int[] newPrio = new int[priority.length + next.length];
        System.arraycopy(this.priority, 0, newPrio, 0, this.priority.length);
        System.arraycopy(next, 0, newPrio, this.priority.length, next.length);
        return new BranchPriority(newPrio);
    }

    @Override
    public int compareTo(BranchPriority o) {
        return compare(this, o);
    }

    @Override
    public String toString() {
        return Arrays.toString(this.priority);
    }
}
