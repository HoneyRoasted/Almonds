package honeyroasted.almonds.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

//Via https://stackoverflow.com/a/12638571/4484294
public class SortedList<T> extends ArrayList<T> {
    private static final long serialVersionUID = 1L;
    private Comparator<? super T> comparator = null;

    public SortedList() {}

    public SortedList(Comparator<? super T> comparator) {
        this.comparator = comparator;
    }

    public SortedList(Collection<? extends T> collection) {
        addAll(collection);
    }

    public SortedList(Collection<? extends T> collection, Comparator<? super T> comparator) {
        this(comparator);
        addAll(collection);
    }

    @Override
    public boolean add(T paramT) {
        int initialSize = this.size();
        int insertionPoint = Collections.binarySearch(this, paramT, comparator);
        super.add((insertionPoint > -1) ? insertionPoint : (-insertionPoint) - 1, paramT);
        return (this.size() != initialSize);
    }

    @Override
    public boolean addAll(Collection<? extends T> paramCollection) {
        boolean result = false;
        if (paramCollection.size() > 4) {
            result = super.addAll(paramCollection);
            Collections.sort(this, comparator);
        } else {
            for (T paramT:paramCollection) {
                result |= add(paramT);
            }
        }
        return result;
    }

    public boolean containsElement(T paramT) {
        return (Collections.binarySearch(this, paramT, comparator) > -1);
    }

    public Comparator<? super T> getComparator() {
        return comparator;
    }

    public void setComparator(Comparator<? super T> comparator) {
        this.comparator = comparator;
        Collections.sort(this, comparator);
    }
}