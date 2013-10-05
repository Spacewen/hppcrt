package com.carrotsearch.hppc;

import static com.carrotsearch.hppc.Internals.rehash;

import java.util.Comparator;

import com.carrotsearch.hppc.cursors.*;
import com.carrotsearch.hppc.predicates.*;
import com.carrotsearch.hppc.procedures.*;
import com.carrotsearch.hppc.sorting.*;

/**
 * A Heap-based, min-priority queue of <code>KType</code>s.
 * (top() is the smallest element)
 * as defined by Sedgewick: Algorithms 4th Edition (2011)
 * It assure O(log2(N)) complexity for insertion, deletion of min element,
 * and constant time to examine the first element.
 */
/*! ${TemplateOptions.generatedAnnotation} !*/
public class KTypeHeapPriorityQueue<KType> extends AbstractKTypeCollection<KType>
implements KTypePriorityQueue<KType>, Cloneable
{
    /**
     * Default capacity if no other capacity is given in the constructor.
     */
    public final static int DEFAULT_CAPACITY = 16;


    /**
     * Internal array for storing the priority queue
     * The array may be larger than the current size
     * ({@link #size()}).
     * <p>
     * Direct priority queue iteration: iterate buffer[i] for i in [1; size()] (included)
     * </p>
     */
    public KType[] buffer;

    /**
     * Number of elements in the queue.
     */
    protected int elementsCount;

    /**
     * Defines the Comparator ordering of the queue,
     * If null, natural ordering is used.
     */
    /*! #if ($TemplateOptions.KTypeGeneric) !*/
    protected Comparator<KType> comparator;
    /*! #else
    protected KTypeComparator<KType> comparator;
    #end !*/

    /**
     * Buffer resizing strategy.
     */
    protected final ArraySizingStrategy resizer;

    /**
     * internal pool of ValueIterator (must be created in constructor)
     */
    protected final IteratorPool<KTypeCursor<KType>, ValueIterator<KType>> valueIteratorPool;

    /**
     * Create with a Comparator, an initial capacity, and a custom buffer resizing strategy.
     */
    public KTypeHeapPriorityQueue(/*! #if ($TemplateOptions.KTypeGeneric) !*/Comparator<KType> comp,
            /*! #else
            KTypeComparator<KType> comp,
            #end !*/int initialCapacity, ArraySizingStrategy resizer)
    {
        this.comparator = comp;

        assert initialCapacity >= 0 : "initialCapacity must be >= 0: " + initialCapacity;
        assert resizer != null;

        this.resizer = resizer;
        //1-based index buffer, assure allocation
        ensureBufferSpace(resizer.round(initialCapacity + 1));

        this.valueIteratorPool = new IteratorPool<KTypeCursor<KType>, ValueIterator<KType>>(
                new ObjectFactory<ValueIterator<KType>>() {

                    @Override
                    public ValueIterator<KType> create()
                    {
                        return new ValueIterator<KType>(KTypeHeapPriorityQueue.this.buffer, size());
                    }

                    @Override
                    public void initialize(ValueIterator<KType> obj)
                    {
                        obj.cursor.index = 0;
                        obj.size = size();
                        obj.buffer = KTypeHeapPriorityQueue.this.buffer;
                    }
                });
    }

    /**
     * Create with default sizing strategy and initial capacity for storing
     * {@value #DEFAULT_CAPACITY} elements.
     * 
     * @see BoundedProportionalArraySizingStrategy
     */
    public KTypeHeapPriorityQueue(/*! #if ($TemplateOptions.KTypeGeneric) !*/Comparator<KType> comp
            /*! #else
    KTypeComparator<KType> comp
    #end !*/)
    {
        this(comp, DEFAULT_CAPACITY);
    }

    /*! #if ($TemplateOptions.KTypeGeneric) !*/
    /**
     * Create with an initial capacity,
     * using the Comparable natural ordering
     */
    /*! #else !*/
    /**
     * Create with an initial capacity,
     * using the natural ordering of <code>KType</code>s
     */
    /*! #end !*/
    public KTypeHeapPriorityQueue(int initialCapacity)
    {
        this(null, initialCapacity, new BoundedProportionalArraySizingStrategy());
    }

    /**
     * Create with a given initial capacity, using a
     * Comparator for ordering.
     * 
     * @see BoundedProportionalArraySizingStrategy
     */
    public KTypeHeapPriorityQueue(/*! #if ($TemplateOptions.KTypeGeneric) !*/Comparator<KType> comp,
            /*! #else
            KTypeComparator<KType> comp,
            #end !*/int initialCapacity)
    {
        this(comp, initialCapacity, new BoundedProportionalArraySizingStrategy());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int removeAllOccurrences(KType e1)
    {
        int to = 1;
        int size = elementsCount;
        //1-based index
        for (int from = 1; from <= size; from++)
        {
            if (Intrinsics.equalsKType(e1, buffer[from]))
            {
                /*! #if ($TemplateOptions.KTypeGeneric) !*/
                buffer[from] = Intrinsics.<KType> defaultKTypeValue();
                /*! #end !*/
                continue;
            }

            if (to != from)
            {
                buffer[to] = buffer[from];
                /*! #if ($TemplateOptions.KTypeGeneric) !*/
                buffer[from] = Intrinsics.<KType> defaultKTypeValue();
                /*! #end !*/
            }
            to++;
        }

        final int deleted = size - to + 1;
        this.elementsCount = to - 1;

        //restore the heap
        refreshPriorities();
        assert isMinHeap();

        return deleted;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int removeAll(KTypePredicate<? super KType> predicate)
    {
        final int elementsCount = this.elementsCount;

        //1-based index
        int to = 1;
        int from = 1;

        try
        {
            //1-based index
            for (; from <= elementsCount; from++)
            {
                if (predicate.apply(buffer[from]))
                {
                    /*! #if ($TemplateOptions.KTypeGeneric) !*/
                    buffer[from] = Intrinsics.<KType> defaultKTypeValue();
                    /*! #end !*/
                    continue;
                }

                if (to != from)
                {
                    buffer[to] = buffer[from];
                    /*! #if ($TemplateOptions.KTypeGeneric) !*/
                    buffer[from] = Intrinsics.<KType> defaultKTypeValue();
                    /*! #end !*/
                }
                to++;
            }
        }
        finally
        {
            // Keep the list in a consistent state, even if the predicate throws an exception.
            for (; from <= elementsCount; from++)
            {
                if (to != from)
                {
                    buffer[to] = buffer[from];
                    /*! #if ($TemplateOptions.KTypeGeneric) !*/
                    buffer[from] = Intrinsics.<KType> defaultKTypeValue();
                    /*! #end !*/
                }
                to++;
            }

            this.elementsCount = to - 1;
        }

        //reestablish heap
        refreshPriorities();
        assert isMinHeap();

        return elementsCount - to + 1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear()
    {
        /*! #if ($TemplateOptions.KTypeGeneric) !*/
        //1-based indexing
        Internals.blankObjectArray(buffer, 1, elementsCount + 1);
        /*! #end !*/
        this.elementsCount = 0;
    }

    /**
     * An iterator implementation for {@link HeapPriorityQueue#iterator}.
     */
    public final static class ValueIterator<KType> extends AbstractIterator<KTypeCursor<KType>>
    {
        final KTypeCursor<KType> cursor;

        KType[] buffer;
        int size;

        public ValueIterator(KType[] buffer, int size)
        {
            this.cursor = new KTypeCursor<KType>();
            //index 0 is not used in Priority queue
            this.cursor.index = 0;
            this.size = size;
            this.buffer = buffer;
        }

        @Override
        protected KTypeCursor<KType> fetch()
        {
            //priority is 1-based index
            if (cursor.index == size)
                return done();

            cursor.value = buffer[++cursor.index];
            return cursor;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ValueIterator<KType> iterator()
    {
        //return new ValueIterator<KType>(buffer, size());
        return this.valueIteratorPool.borrow();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean contains(KType element)
    {
        //1-based index
        int size = elementsCount;

        for (int i = 1; i <= size; i++)
        {
            if (Intrinsics.equalsKType(element, buffer[i]))
            {
                return true;
            }
        } //end for

        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int size()
    {
        return elementsCount;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends KTypeProcedure<? super KType>> T forEach(T procedure)
    {
        final KType[] buffer = this.buffer;
        final int size = elementsCount;

        for (int i = 1; i <= size; i++)
        {
            procedure.apply(buffer[i]);
        }

        return procedure;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends KTypePredicate<? super KType>> T forEach(T predicate)
    {
        final KType[] buffer = this.buffer;
        final int size = elementsCount;

        for (int i = 1; i <= size; i++)
        {
            if (!predicate.apply(buffer[i]))
                break;
        }

        return predicate;
    }

    /**
     * Insert a KType into the queue.
     * cost: O(log2(N)) for a N sized queue
     */
    @Override
    public void insert(KType element)
    {
        ensureBufferSpace(1);

        //add at the end
        elementsCount++;
        buffer[elementsCount] = element;

        //swim last element
        swim(elementsCount);
        //do not use it here, too slow to be done at each insert
        // assert isMinHeap();
    }

    /**
     * {@inheritDoc}
     * cost: O(1)
     */
    @Override
    public KType top()
    {
        KType elem = Intrinsics.<KType> defaultKTypeValue();

        if (elementsCount > 0)
        {
            elem = buffer[1];
        }

        return elem;
    }

    /**
     * {@inheritDoc}
     * cost: O(log2(N)) for a N sized queue
     */
    @Override
    public KType popTop()
    {
        KType elem = Intrinsics.<KType> defaultKTypeValue();

        if (elementsCount > 0)
        {
            elem = buffer[1];

            if (elementsCount == 1)
            {
                //for GC
                /*! #if ($TemplateOptions.KTypeGeneric) !*/
                buffer[1] = Intrinsics.<KType> defaultKTypeValue();
                /*! #end !*/
                //diminuish size
                elementsCount = 0;
            }
            else
            {
                //at least 2 elements
                //put the last element in first position
                buffer[1] = buffer[elementsCount];

                //for GC
                /*! #if ($TemplateOptions.KTypeGeneric) !*/
                buffer[elementsCount] = Intrinsics.<KType> defaultKTypeValue();
                /*! #end !*/

                //diminuish size
                elementsCount--;

                //percolate down the first element
                sink(1);
                //do not use it here, too slow to be done at each popTop()
                // assert isMinHeap();
            }
        }

        return elem;
    }

    /**
     * Adds all elements from another container.
     * cost: O(N*log2(N)) for N elements
     */
    public int addAll(KTypeContainer<? extends KType> container)
    {
        final int size = container.size();
        ensureBufferSpace(size);

        for (KTypeCursor<? extends KType> cursor : container)
        {
            elementsCount++;
            buffer[elementsCount] = cursor.value;
        }

        //restore heap
        refreshPriorities();
        assert isMinHeap();

        return size;
    }

    /**
     * Adds all elements from another iterable.
     * cost: O(N*log2(N)) for N elements
     */
    public int addAll(Iterable<? extends KTypeCursor<? extends KType>> iterable)
    {
        int size = 0;
        for (KTypeCursor<? extends KType> cursor : iterable)
        {
            ensureBufferSpace(1);
            elementsCount++;
            buffer[elementsCount] = cursor.value;
            size++;
        }

        //restore heap
        refreshPriorities();
        assert isMinHeap();

        return size;
    }

    /**
     * Clone this object. The returned clone will resizing strategy.
     */
    @Override
    public KTypeHeapPriorityQueue<KType> clone()
    {
        try
        {
            /* #if ($TemplateOptions.KTypeGeneric) */
            @SuppressWarnings("unchecked")
            /* #end */
            final KTypeHeapPriorityQueue<KType> cloned = (KTypeHeapPriorityQueue<KType>) super.clone();
            cloned.buffer = buffer.clone();
            return cloned;
        }
        catch (CloneNotSupportedException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        int h = 1, max = elementsCount;
        //1-based index
        for (int i = 1; i <= max; i++)
        {
            h = 31 * h + rehash(this.buffer[i]);
        }
        return h;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void refreshPriorities()
    {
        if (this.comparator == null)
        {
            for (int k = elementsCount >> 1; k >= 1; k--)
            {
                sinkComparable(k);
            }
        }
        else
        {
            for (int k = elementsCount >> 1; k >= 1; k--)
            {
                sinkComparator(k);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    /* #if ($TemplateOptions.KTypeGeneric) */
    @SuppressWarnings("unchecked")
    /* #end */
    public boolean equals(Object obj)
    {
        if (obj != null)
        {
            //we can only compare both KTypeHeapPriorityQueue,
            //that has the same comparison function reference
            if (obj instanceof KTypeHeapPriorityQueue<?>)
            {
                KTypeHeapPriorityQueue<?> other = (KTypeHeapPriorityQueue<?>) obj;

                //both heaps must have the same comparison criteria
                boolean sameComp = (other.comparator == null && this.comparator == null) || //Comparable or natural ordering
                        (other.comparator != null && other.comparator == this.comparator);

                if (other.size() == this.size() && sameComp)
                {
                    for (int i = 1; i <= elementsCount; i++)
                    {
                        if (!Intrinsics.equalsKType(this.buffer[i], other.buffer[i]))
                        {
                            return false;
                        }
                    }

                    return true;
                } //end if size identical
            } //end if KTypeHeapPriorityQueue<?>
        }
        return false;
    }

    /**
     * Ensures the internal buffer has enough free slots to store
     * <code>expectedAdditions</code>. Increases internal buffer size if needed.
     */
    protected void ensureBufferSpace(int expectedAdditions)
    {
        final int bufferLen = (buffer == null ? 0 : buffer.length - 1);
        if (elementsCount >= bufferLen - expectedAdditions)
        {
            final int newSize = resizer.grow(bufferLen, elementsCount, expectedAdditions + 1);
            assert newSize >= elementsCount + expectedAdditions : "Resizer failed to" +
                    " return sensible new size: " + newSize + " <= "
                    + (elementsCount + expectedAdditions);

            final KType[] newBuffer = Intrinsics.newKTypeArray(newSize);
            if (bufferLen > 0)
            {
                System.arraycopy(buffer, 0, newBuffer, 0, buffer.length);
            }
            this.buffer = newBuffer;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public KType[] toArray(KType[] target)
    {
        //copy from index 1
        System.arraycopy(buffer, 1, target, 0, elementsCount);
        return target;
    }

    /**
     * Sink function for Comparable elements
     * @param k
     */
    private void sinkComparable(int k)
    {
        final int N = elementsCount;
        KType tmp;
        int child;

        while ((k << 1) <= N)
        {
            //get the child of k
            child = k << 1;

            if (child < N && Intrinsics.isCompSupKTypeUnchecked(buffer[child], buffer[child + 1]))
            {
                child++;
            }

            if (!Intrinsics.isCompSupKTypeUnchecked(buffer[k], buffer[child]))
            {
                break;
            }

            //swap k and child
            tmp = buffer[k];
            buffer[k] = buffer[child];
            buffer[child] = tmp;

            k = child;
        } //end while
    }

    /**
     * Sink function for KTypeComparator elements
     * @param k
     */
    private void sinkComparator(int k)
    {
        final int N = elementsCount;
        KType tmp;
        int child;
        /*! #if ($TemplateOptions.KTypeGeneric) !*/
        Comparator<KType> comp = this.comparator;
        /*! #else
        KTypeComparator<KType> comp = this.comparator;
        #end !*/

        while ((k << 1) <= N)
        {
            //get the child of k
            child = k << 1;

            if (child < N && comp.compare(buffer[child], buffer[child + 1]) > 0)
            {
                child++;
            }

            if (comp.compare(buffer[k], buffer[child]) <= 0)
            {
                break;
            }

            //swap k and child
            tmp = buffer[k];
            buffer[k] = buffer[child];
            buffer[child] = tmp;

            k = child;
        } //end while
    }

    /**
     * Swim function for Comparable elements
     * @param k
     */
    private void swimComparable(int k)
    {
        KType tmp;
        int parent;

        while (k > 1 && Intrinsics.isCompSupKTypeUnchecked(buffer[k >> 1], buffer[k]))
        {
            //swap k and its parent
            parent = k >> 1;

        tmp = buffer[k];
        buffer[k] = buffer[parent];
        buffer[parent] = tmp;

        k = parent;
        }
    }

    /**
     * Swim function for Comparator elements
     * @param k
     */
    private void swimComparator(int k)
    {
        KType tmp;
        int parent;
        /*! #if ($TemplateOptions.KTypeGeneric) !*/
        Comparator<KType> comp = this.comparator;
        /*! #else
        KTypeComparator<KType> comp = this.comparator;
        #end !*/

        while (k > 1 && comp.compare(buffer[k >> 1], buffer[k]) > 0)
        {
            //swap k and its parent
            parent = k >> 1;
        tmp = buffer[k];
        buffer[k] = buffer[parent];
        buffer[parent] = tmp;

        k = parent;
        }
    }

    private void swim(int k)
    {
        if (this.comparator == null)
        {
            swimComparable(k);
        }
        else
        {
            swimComparator(k);
        }
    }

    private void sink(int k)
    {
        if (this.comparator == null)
        {
            sinkComparable(k);
        }
        else
        {
            sinkComparator(k);
        }
    }



    /**
     * method to test invariant in assert
     */
    // is pq[1..N] a min heap?
    protected boolean isMinHeap()
    {
        if (this.comparator == null)
        {
            return isMinHeapComparable(1);
        }

        return isMinHeapComparator(1);
    }

    // is subtree of pq[1..N] rooted at k a min heap?
    private boolean isMinHeapComparable(int k)
    {
        int N = elementsCount;

        if (k > N)
            return true;
        int left = 2 * k, right = 2 * k + 1;

        if (left <= N && Intrinsics.isCompSupKTypeUnchecked(buffer[k], buffer[left]))
            return false;
        if (right <= N && Intrinsics.isCompSupKTypeUnchecked(buffer[k], buffer[right]))
            return false;
        //recursively test
        return isMinHeapComparable(left) && isMinHeapComparable(right);
    }

    // is subtree of pq[1..N] rooted at k a min heap?
    private boolean isMinHeapComparator(int k)
    {
        int N = elementsCount;

        /*! #if ($TemplateOptions.KTypeGeneric) !*/
        Comparator<KType> comp = this.comparator;
        /*! #else
        KTypeComparator<KType> comp = this.comparator;
        #end !*/

        if (k > N)
            return true;
        int left = 2 * k, right = 2 * k + 1;

        if (left <= N && comp.compare(buffer[k], buffer[left]) > 0)
            return false;
        if (right <= N && comp.compare(buffer[k], buffer[right]) > 0)
            return false;
        //recursively test
        return isMinHeapComparator(left) && isMinHeapComparator(right);
    }
}
