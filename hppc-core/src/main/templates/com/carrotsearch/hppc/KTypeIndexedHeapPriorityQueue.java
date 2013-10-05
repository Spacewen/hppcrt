package com.carrotsearch.hppc;

import static com.carrotsearch.hppc.Internals.rehash;

import java.util.Arrays;
import java.util.Comparator;

import com.carrotsearch.hppc.cursors.*;
import com.carrotsearch.hppc.predicates.*;
import com.carrotsearch.hppc.procedures.*;
import com.carrotsearch.hppc.sorting.*;

/**
 * A Heap-based, Indexed min-priority queue of <code>KType</code>s.
 * (top() is the smallest element)
 * as defined by Sedgewick: Algorithms 4th Edition (2011)
 * It assure O(log2(N)) complexity for insertion, deletion of min element,
 * and constant time to examine the first element.
 * As it is indexed, it also supports contains() in constant time, delete()
 * and change priority in O(log2(N)) time.
 */
/*! ${TemplateOptions.generatedAnnotation} !*/
public class KTypeIndexedHeapPriorityQueue<KType> extends AbstractKTypeCollection<KType>
implements KTypeIndexedPriorityQueue<KType>, Cloneable
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
     * Direct indexed priority queue iteration: iterate pq[i] for i in [0; size()[ (included)
     * and buffer[pq[i]] to get value where pq[i] > 0
     * </p>
     */
    public KType[] buffer;

    /**
     * Internal array for storing index to buffer position matching
     * ({@link #size()}).
     * i.e for an index i, pq[i] is the position of element in priority queue buffer.
     * <p>
     * Direct iteration: iterate pq[i] for indices i in [0; size()[
     * where pq[i] > 0, then buffer[pq[i]] is the value associated with index i.
     * </p>
     */
    public int[] pq;

    /**
     * Internal array pq inversing :
     * ({@link #size()}).
     * i.e for a priority buffer position pos, qp[pos] is the index of the value.,
     * ie qp[pq|i]] = i
     */
    protected int[] qp;

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
    public KTypeIndexedHeapPriorityQueue(/*! #if ($TemplateOptions.KTypeGeneric) !*/Comparator<KType> comp,
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
                        return new ValueIterator<KType>(KTypeIndexedHeapPriorityQueue.this.buffer, pq);
                    }

                    @Override
                    public void initialize(ValueIterator<KType> obj)
                    {
                        obj.cursor.index = 0;
                        obj.buffer = KTypeIndexedHeapPriorityQueue.this.buffer;
                        obj.currentpqposition = 0;
                        obj.pq = KTypeIndexedHeapPriorityQueue.this.pq;
                    }
                });
    }

    /**
     * Create with default sizing strategy and initial capacity for storing
     * {@value #DEFAULT_CAPACITY} elements.
     * 
     * @see BoundedProportionalArraySizingStrategy
     */
    public KTypeIndexedHeapPriorityQueue(/*! #if ($TemplateOptions.KTypeGeneric) !*/Comparator<KType> comp
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
    public KTypeIndexedHeapPriorityQueue(int initialCapacity)
    {
        this(null, initialCapacity, new BoundedProportionalArraySizingStrategy());
    }

    /**
     * Create with a given initial capacity, using a
     * Comparator for ordering.
     * 
     * @see BoundedProportionalArraySizingStrategy
     */
    public KTypeIndexedHeapPriorityQueue(/*! #if ($TemplateOptions.KTypeGeneric) !*/Comparator<KType> comp,
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
        //remove by index
        int deleted = 0;
        final int size = this.pq.length;
        int pos = 0;

        for (int i = 0; i < size; i++)
        {
            pos = pq[i];

            if (pos > 0 && Intrinsics.equalsKType(e1, buffer[pos]))
            {
                this.deleteIndex(i);
                deleted++;
            }
        }

        assert isMinHeap();

        return deleted;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int removeAll(KTypePredicate<? super KType> predicate)
    {
        //remove by index
        int deleted = 0;
        final int size = this.pq.length;
        int pos = 0;

        for (int i = 0; i < size; i++)
        {
            pos = pq[i];

            if (pos > 0 && predicate.apply(buffer[pos]))
            {
                this.deleteIndex(i);
                deleted++;
            }
        }

        assert isMinHeap();

        return deleted;
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

        //need to init to zero, not -1 !!!
        Arrays.fill(pq, 0);

        //TODO : needed ?
        Arrays.fill(qp, -1);

        this.elementsCount = 0;
    }

    /**
     * An iterator implementation for {@link HeapPriorityQueue#iterator}.
     */
    public final static class ValueIterator<KType> extends AbstractIterator<KTypeCursor<KType>>
    {
        final KTypeCursor<KType> cursor;

        KType[] buffer;
        int[] pq;

        int currentpqposition = 0;

        public ValueIterator(KType[] buffer, int[] pq)
        {
            this.cursor = new KTypeCursor<KType>();
            //index 0 is not used in Priority queue
            this.cursor.index = 0;

            this.buffer = buffer;
            this.pq = pq;
            currentpqposition = 0;
        }

        @Override
        protected KTypeCursor<KType> fetch()
        {
            //advance to the first non-empty slot
            //1-based indexing
            int pqsize = pq.length;

            while (currentpqposition < pqsize)
            {
                if (pq[currentpqposition] <= 0)
                {
                    currentpqposition++;
                }
                else
                {
                    break;
                }
            }

            if (currentpqposition >= pqsize)
                return done();

            cursor.index = currentpqposition;
            cursor.value = buffer[pq[currentpqposition]];
            currentpqposition++;

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
        final int size = elementsCount;
        final KType[] buffer = this.buffer;

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
    public <T extends KTypeIndexedProcedure<? super KType>> T indexedForEach(T procedure)
    {
        final int size = this.pq.length;
        final KType[] buffer = this.buffer;
        int pos;

        for (int i = 0; i < size; i++)
        {
            pos = pq[i];
            if (pos > 0)
            {
                procedure.apply(i, buffer[pos]);
            }
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
     * {@inheritDoc}
     */
    @Override
    public <T extends KTypeIndexedPredicate<? super KType>> T indexedForEach(T predicate)
    {
        final int size = this.pq.length;
        final KType[] buffer = this.buffer;
        int pos;

        for (int i = 0; i < size; i++)
        {
            pos = pq[i];

            if (pos > 0)
            {
                if (!predicate.apply(i, buffer[pos]))
                {
                    break;
                }
            }
        }

        return predicate;
    }

    /**
     * {@inheritDoc}
     * cost: O(log2(N)) for a N sized queue
     */
    @Override
    public boolean insert(int index, KType element)
    {
        assert index >= 0;

        // pq must be sufficient to receive index by direct indexing,
        //resize if needed.
        ensureBufferSpace(index);

        if (pq[index] > 0)
        {
            //element already exists
            return false;
        }

        //add at the end
        elementsCount++;
        final int count = elementsCount;

        buffer[count] = element;

        //initial position
        pq[index] = count;
        qp[count] = index;

        //swim last element
        swim(count);
        //do not assert here, too slow to be done at each insert
        //assert isMinHeap();

        return true;
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
                //this is the only element to be removed
                pq[qp[1]] = -1;

                //TODO: needed ?
                qp[1] = -1;

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

                //the first element index is removed
                pq[qp[1]] = -1;

                final int lastElementIndex = qp[elementsCount];

                buffer[1] = buffer[elementsCount];
                qp[1] = lastElementIndex;
                pq[lastElementIndex] = 1;

                //TODO : needed ?
                qp[elementsCount] = -1;

                //for GC
                /*! #if ($TemplateOptions.KTypeGeneric) !*/
                buffer[elementsCount] = Intrinsics.<KType> defaultKTypeValue();
                /*! #end !*/

                //diminuish size
                elementsCount--;

                //percolate down the first element
                sink(1);
            }
        }

        return elem;
    }

    /**
     * {@inheritDoc}
     * cost: O(1)
     */
    @Override
    public KType getIndex(int index)
    {
        assert index >= pq.length || pq[index] > 0 : "Element of index " + " doesn't exist !";

        KType elem = Intrinsics.<KType> defaultKTypeValue();

        if (index < pq.length && pq[index] > 0)
        {
            elem = buffer[pq[index]];
        }

        return elem;
    }

    /**
     * Clone this object. The returned clone will resizing strategy.
     */
    @Override
    public KTypeIndexedHeapPriorityQueue<KType> clone()
    {
        try
        {
            /* #if ($TemplateOptions.KTypeGeneric) */
            @SuppressWarnings("unchecked")
            /* #end */
            final KTypeIndexedHeapPriorityQueue<KType> cloned = (KTypeIndexedHeapPriorityQueue<KType>) super.clone();

            cloned.buffer = buffer.clone();
            cloned.pq = pq.clone();
            cloned.qp = qp.clone();

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
        int h = 1;
        int size = pq.length;

        for (int i = 0; i < size; i++)
        {
            if (pq[i] > 0)
            {
                //rehash of the index
                h = 31 * h + rehash(i);
                //append the rehash of the value
                h = 31 * h + rehash(this.buffer[pq[i]]);
            }
        }

        return h;
    }

    /**
     * {@inheritDoc}
     * cost: O(log2(N))
     */
    @Override
    public boolean deleteIndex(int index)
    {
        if (index < pq.length && pq[index] > 0)
        {
            int deletedPos = pq[index];

            if (deletedPos == elementsCount)
            {
                //we remove the last element
                pq[index] = 0;

                //for GC
                /*! #if ($TemplateOptions.KTypeGeneric) !*/
                buffer[deletedPos] = Intrinsics.<KType> defaultKTypeValue();
                /*! #end !*/

                //TODO : needed ?
                qp[deletedPos] = -1;

                //diminuish size
                elementsCount--;
            }
            else
            {
                //We are not removing the last element
                assert (deletedPos > 0 && qp[deletedPos] == index) : String.format("pq[index] = %d, qp[pq[index]] = %d (index = %d)",
                        deletedPos, qp[deletedPos], index);

                int lastElementIndex = qp[elementsCount];

                assert (lastElementIndex >= 0 && pq[lastElementIndex] == elementsCount) : String.format("lastElementIndex = qp[elementsCount] = %d, pq[lastElementIndex] = %d, elementsCount = %d",
                        lastElementIndex, pq[lastElementIndex], elementsCount);

                //not needed, overwritten below :
                //qp[deletedPos] = -1;

                buffer[deletedPos] = buffer[elementsCount];
                //last element is now at pos deletedPos
                pq[lastElementIndex] = deletedPos;
                qp[deletedPos] = lastElementIndex;

                //mark the index element to be removed
                //we must reset with 0 so that qp[pq[index]] is always valid !
                pq[index] = 0;

                //TODO: needed ?
                qp[elementsCount] = -1;

                //for GC
                /*! #if ($TemplateOptions.KTypeGeneric) !*/
                buffer[elementsCount] = Intrinsics.<KType> defaultKTypeValue();
                /*! #end !*/

                //diminuish size
                elementsCount--;

                //after swapping positions
                assert (pq[lastElementIndex] == deletedPos) : String.format("pq[lastElementIndex = %d] = %d, while deletedPos = %d, (index = %d)",
                        lastElementIndex, pq[lastElementIndex], deletedPos, index);

                assert (qp[deletedPos] == lastElementIndex) : String.format("qp[deletedPos = %d] = %d, while lastElementIndex = %d, (index = %d)",
                        deletedPos, qp[deletedPos], lastElementIndex, index);

                if (elementsCount > 1)
                {
                    //re-establish heap
                    sink(pq[lastElementIndex]);
                    swim(pq[lastElementIndex]);
                }
            }
            return true;
        }

        return false;
    }

    /**
     * {@inheritDoc}
     * cost: O(log2(N))
     */
    @Override
    public void changePriority(int index)
    {
        if (index < pq.length && pq[index] > 0)
        {
            swim(pq[index]);
            sink(pq[index]);
        }
    }

    /**
     * {@inheritDoc}
     * cost: O(1)
     */
    @Override
    public boolean containsIndex(int index)
    {
        if (index < pq.length && pq[index] > 0)
        {
            return true;
        }

        return false;
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
            if (obj instanceof KTypeIndexedHeapPriorityQueue<?>)
            {
                KTypeIndexedHeapPriorityQueue<?> other = (KTypeIndexedHeapPriorityQueue<?>) obj;

                //both heaps must have the same comparison criteria
                boolean sameComp = (other.comparator == null && this.comparator == null) || //Comparable or natural ordering
                        (other.comparator != null && other.comparator == this.comparator);

                if (other.size() == this.size() && sameComp)
                {
                    //by index
                    int pos = 0;
                    for (int i = 0; i < this.pq.length; i++)
                    {
                        pos = pq[i];

                        if (pos > 0 && !other.containsIndex(i))
                        {
                            return false;
                        }

                        KType otherElement = (KType) other.getIndex(i);

                        if (!Intrinsics.equalsKType(this.buffer[i], otherElement))
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
     * Ensures the internal buffer has enough free slots to accomodate the index
     * <code>index</code>. Increases internal buffer size if needed.
     */
    protected void ensureBufferSpace(int index)
    {
        final int bufferLen = (buffer == null ? 0 : buffer.length - 1);
        if (index + 1 >= bufferLen)
        {
            final int newSize = resizer.grow(bufferLen, bufferLen, index + 1 - bufferLen);

            assert index < newSize;

            final KType[] newBuffer = Intrinsics.newKTypeArray(newSize);
            final int[] newPQIndex = new int[newSize];
            final int[] newQPIndex = new int[newSize];

            if (bufferLen > 0)
            {
                System.arraycopy(buffer, 0, newBuffer, 0, buffer.length);
                System.arraycopy(pq, 0, newPQIndex, 0, pq.length);
                System.arraycopy(qp, 0, newQPIndex, 0, qp.length);
            }
            this.buffer = newBuffer;
            this.pq = newPQIndex;
            this.qp = newQPIndex;
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

    @Override
    public String toString()
    {
        final StringBuilder buff = new StringBuilder();
        buff.append("[");

        boolean first = true;

        for (int i = 0; i < pq.length; i++)
        {
            if (pq[i] > 0)
            {
                if (!first)
                {
                    buff.append(", ");
                }

                buff.append(i);
                buff.append("=>");
                buff.append(this.buffer[pq[i]]);
                first = false;
            }
        }

        buff.append("]");
        return buff.toString();
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
        int indexK, indexChild;

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

            //swap references
            indexK = qp[k];
            indexChild = qp[child];

            pq[indexK] = child;
            pq[indexChild] = k;

            qp[k] = indexChild;
            qp[child] = indexK;

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
        int indexK, indexChild;

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

            //swap references
            indexK = qp[k];
            indexChild = qp[child];

            pq[indexK] = child;
            pq[indexChild] = k;

            qp[k] = indexChild;
            qp[child] = indexK;

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
        int indexK, indexParent;

        while (k > 1 && Intrinsics.isCompSupKTypeUnchecked(buffer[k >> 1], buffer[k]))
        {
            //swap k and its parent
            parent = k >> 1;

        //swap k and parent
        tmp = buffer[k];
        buffer[k] = buffer[parent];
        buffer[parent] = tmp;

        //swap references
        indexK = qp[k];
        indexParent = qp[parent];

        pq[indexK] = parent;
        pq[indexParent] = k;

        qp[k] = indexParent;
        qp[parent] = indexK;

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
        int indexK, indexParent;

        /*! #if ($TemplateOptions.KTypeGeneric) !*/
        Comparator<KType> comp = this.comparator;
        /*! #else
        KTypeComparator<KType> comp = this.comparator;
        #end !*/

        while (k > 1 && comp.compare(buffer[k >> 1], buffer[k]) > 0)
        {
            //swap k and its parent
            parent = k >> 1;

        //swap k and parent
        tmp = buffer[k];
        buffer[k] = buffer[parent];
        buffer[parent] = tmp;

        //swap references
        indexK = qp[k];
        indexParent = qp[parent];

        pq[indexK] = parent;
        pq[indexParent] = k;

        qp[k] = indexParent;
        qp[parent] = indexK;

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
    // is buffer[1..N] a min heap?
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
