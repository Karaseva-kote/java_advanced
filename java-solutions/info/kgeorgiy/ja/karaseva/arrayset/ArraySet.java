package info.kgeorgiy.ja.karaseva.arrayset;

import java.util.*;

public class ArraySet<T> extends AbstractSet<T> implements SortedSet<T> {
    private final List<T> list;
    private Comparator<? super T> comparator;

    public ArraySet() {
        list = Collections.emptyList();
    }

    public ArraySet(Collection<? extends T> collection) {
        list = new ArrayList<>(new TreeSet<>(collection));
    }

    public  ArraySet(Collection<? extends  T> collection, Comparator<? super T> cmp) {
        Set<T> set = new TreeSet<>(cmp);
        set.addAll(collection);
        list = new ArrayList<>(set);
        comparator = cmp;
    }

    public ArraySet(Comparator<? super T> cmp) {
        list = Collections.emptyList();
        comparator = cmp;
    }

    @Override
    public Iterator<T> iterator() {
        return Collections.unmodifiableList(list).iterator();
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public Comparator<? super T> comparator() {
        return comparator;
    }

    @Override
    public T first() {
        if (list.isEmpty()) {
            throw new NoSuchElementException("set is empty");
        }
        return list.get(0);
    }

    @Override
    public T last() {
        if (list.isEmpty()) {
            throw new NoSuchElementException("set is empty");
        }
        return list.get(list.size() - 1);
    }

    @Override
    public SortedSet<T> subSet(T fromElement, T toElement) {
        // NOTE: no null check
        if (comparator != null) {
            if (comparator.compare(fromElement,toElement) > 0) {
                throw new IllegalArgumentException("wrong arguments: fromElement > toElement");
            }
        } else {
            @SuppressWarnings("unchecked")
            Comparable<? super T> left = (Comparable<? super T>) fromElement;
            if (left.compareTo(toElement) > 0) {
                throw new IllegalArgumentException("wrong arguments: fromElement > toElement");
            }
        }
        if(list.size() == 0) {
            return new ArraySet<>(comparator);
        }
        int start = getIndex(fromElement);
        int finish = getIndex(toElement);
        return new ArraySet<>(list.subList(start, finish), comparator);
    }

    private int getIndex(T key) {
        int index = Collections.binarySearch(list, key, comparator);
        if (index < 0) {
            index = -index - 1;
        }
        return index;
    }

    @Override
    public SortedSet<T> headSet(T toElement) {
        if (list.size() == 0) {
            return new ArraySet<>(comparator);
        }
        return new ArraySet<>(list.subList(0, getIndex(toElement)), comparator);
    }

    @Override
    public SortedSet<T> tailSet(T fromElement) {
        if (list.size() == 0) {
            return new ArraySet<>(comparator);
        }
        return new ArraySet<>(list.subList(getIndex(fromElement), list.size()), comparator);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean contains(Object o) {
        return (Collections.binarySearch(list, (T) Objects.requireNonNull(o), comparator) >= 0);
    }
}
