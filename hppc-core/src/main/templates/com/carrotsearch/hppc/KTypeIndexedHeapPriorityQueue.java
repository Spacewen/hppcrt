package com.carrotsearch.hppc;

import java.util.*;

import com.carrotsearch.hppc.cursors.*;
import com.carrotsearch.hppc.predicates.*;
import com.carrotsearch.hppc.procedures.*;
import com.carrotsearch.hppc.sorting.*;

/**
 * A Heap-based, indexed min-priority queue of <code>KType</code>s.
 * i.e. top() is the smallest element of the queue.
 * as defined by Sedgewick: Algorithms 4th Edition (2011)
 * It assure O(log2(N)) complexity for insertion, deletion of the first element,
 * and constant time to examine the first element.
 * As it is indexed, it also supports containsIndex() in constant time, deleteIndex()
 * and change priority in O(log2(N)) time.
 */
/*! ${TemplateOptions.doNotGenerateKType("BOOLEAN")} !*/
/*! #set( $DEBUG = false) !*/
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
    protected Comparator<? super KType> comparator;
    /*! #else
    protected KTypeComparator<? super KType> comparator;
    #end !*/

    /**
     * internal pool of ValueIterator (must be created in constructor)
     */
    protected final IteratorPool<KTypeCursor<KType>, ValueIterator> valueIteratorPool;

    protected KTypeIndexedPredicate<? super KType> testIndexedPredicate;
    protected KType testContainsPredicate;

    protected final KTypeIndexedPredicate<KType> negateIndexedPredicate = new KTypeIndexedPredicate<KType>() {

        @Override
        public final boolean apply(final int index, final KType k)
        {
            return !testIndexedPredicate.apply(index, k);
        }
    };

    protected final KTypePredicate<KType> containsPredicate = new KTypePredicate<KType>() {

        @Override
        public final boolean apply(final KType k)
        {
            return Intrinsics.equalsKType(testContainsPredicate, k);
        }
    };

    /**
     * Create with a given initial capacity, using a
     * Comparator for ordering.
     */
    public KTypeIndexedHeapPriorityQueue(/*! #if ($TemplateOptions.KTypeGeneric) !*/final Comparator<? super KType> comp,
            /*! #else
            KTypeComparator<? super KType> comp,
            #end !*/final int initialCapacity)
    {
        this.comparator = comp;

        assert initialCapacity >= 0 : "initialCapacity must be >= 0: " + initialCapacity;

        //1-based index buffer, assure allocation
        ensureBufferSpace(initialCapacity + 1);

        this.valueIteratorPool = new IteratorPool<KTypeCursor<KType>, ValueIterator>(
                new ObjectFactory<ValueIterator>() {

                    @Override
                    public ValueIterator create()
                    {
                        return new ValueIterator();
                    }

                    @Override
                    public void initialize(final ValueIterator obj)
                    {
                        obj.cursor.index = 0;
                        obj.buffer = KTypeIndexedHeapPriorityQueue.this.buffer;
                        obj.size = KTypeIndexedHeapPriorityQueue.this.elementsCount;
                        obj.currentPosition = 0;
                        obj.qp = KTypeIndexedHeapPriorityQueue.this.qp;
                    }

                    @Override
                    public void reset(final ValueIterator obj) {
                        // for GC sake
                        obj.qp = null;
                        obj.buffer = null;
                    }
                });
    }

    /**
     * Create with default sizing strategy and initial capacity for storing
     * {@value #DEFAULT_CAPACITY} elements.
     * 
     * @see BoundedProportionalArraySizingStrategy
     */
    public KTypeIndexedHeapPriorityQueue(/*! #if ($TemplateOptions.KTypeGeneric) !*/final Comparator<? super KType> comp
    /*! #else
    KTypeComparator<? super KType> comp
    #end !*/)
    {
        this(comp, KTypeIndexedHeapPriorityQueue.DEFAULT_CAPACITY);
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
    public KTypeIndexedHeapPriorityQueue(final int initialCapacity)
    {
        this(null, initialCapacity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int removeAllOccurrences(final KType e1)
    {
        this.testContainsPredicate = e1;
        return this.removeAllInternal(this.containsPredicate, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int removeAll(final KTypePredicate<? super KType> predicate)
    {
        return removeAllInternal(predicate, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int removeAll(final KTypeIndexedPredicate<? super KType> indexedPredicate)
    {
        return removeAllInternal(null, indexedPredicate);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int retainAll(final KTypeIndexedPredicate<? super KType> indexedPredicate)
    {
        this.testIndexedPredicate = indexedPredicate;
        return this.removeAllInternal(null, this.negateIndexedPredicate);
    }

    private int removeAllInternal(final KTypePredicate<? super KType> predicate, final KTypeIndexedPredicate<? super KType> indexedPredicate)
    {
        //remove by position
        int deleted = 0;
        final KType[] buffer = this.buffer;

        final int[] qp = this.qp;
        final int[] pq = this.pq;

        int lastElementIndex = -1;

        int elementsCount = this.elementsCount;

        //1-based index
        int pos = 1;

        try
        {
            while (pos <= elementsCount)
            {
                //delete it
                if (predicate == null ? indexedPredicate.apply(qp[pos], buffer[pos]) : predicate.apply(buffer[pos]))
                {
                    lastElementIndex = qp[elementsCount];

                    //put the last element at position pos, like in deleteIndex()

                    buffer[pos] = buffer[elementsCount];
                    //last element is now at deleted position pos
                    pq[lastElementIndex] = pos;

                    //mark the index element to be removed
                    //we must reset with 0 so that qp[pq[index]] is always valid !
                    pq[qp[pos]] = 0;

                    qp[pos] = lastElementIndex;

                    //Not really needed
                    /*! #if($DEBUG) !*/
                    qp[elementsCount] = -1;
                    /*! #end !*/

                    //for GC
                    /*! #if ($TemplateOptions.KTypeGeneric) !*/
                    buffer[elementsCount] = Intrinsics.<KType> defaultKTypeValue();
                    /*! #end !*/

                    //Diminish size
                    elementsCount--;
                    deleted++;
                } //end if to delete
                else
                {
                    pos++;
                }
            } //end while
        }
        finally
        {
            this.elementsCount = elementsCount;
            //reestablish heap
            refreshPriorities();
        }

        /*! #if($DEBUG) !*/
        assert isMinHeap();
        assert isConsistent();
        /*! #end !*/

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

        //we need to init to zero, not -1 !!!
        Arrays.fill(pq, 0);

        //This is not really needed to reset this,
        //but is useful to catch inconsistencies in assertions
        /*! #if($DEBUG) !*/
        Arrays.fill(qp, -1);
        /*! #end !*/

        this.elementsCount = 0;
    }

    /**
     * An iterator implementation for {@link HeapPriorityQueue#iterator}.
     */
    public final class ValueIterator extends AbstractIterator<KTypeCursor<KType>>
    {
        public final KTypeCursor<KType> cursor;

        KType[] buffer;
        int size;
        int[] qp;
        int currentPosition = 0;

        public ValueIterator()
        {
            this.cursor = new KTypeCursor<KType>();
            //index 0 is not used in Priority queue
            this.cursor.index = 0;
            this.buffer = KTypeIndexedHeapPriorityQueue.this.buffer;
            this.size = KTypeIndexedHeapPriorityQueue.this.size();
            this.qp = KTypeIndexedHeapPriorityQueue.this.qp;
            currentPosition = 0;
        }

        @Override
        protected KTypeCursor<KType> fetch()
        {
            //priority is 1-based index
            if (currentPosition == size)
                return done();

            cursor.value = buffer[++currentPosition];
            cursor.index = qp[currentPosition];

            return cursor;

        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ValueIterator iterator()
    {
        return this.valueIteratorPool.borrow();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean contains(final KType element)
    {
        //1-based index
        final int size = elementsCount;
        final KType[] buffer = this.buffer;

        for (int pos = 1; pos <= size; pos++)
        {
            if (Intrinsics.equalsKType(element, buffer[pos]))
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
    public int capacity() {

        return buffer.length - 1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends KTypeProcedure<? super KType>> T forEach(final T procedure)
    {
        final KType[] buffer = this.buffer;
        final int size = elementsCount;

        for (int pos = 1; pos <= size; pos++)
        {
            procedure.apply(buffer[pos]);
        }

        return procedure;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends KTypeIndexedProcedure<? super KType>> T indexedForEach(final T procedure)
    {
        final KType[] buffer = this.buffer;
        final int size = elementsCount;

        final int[] qp = this.qp;

        for (int pos = 1; pos <= size; pos++)
        {
            procedure.apply(qp[pos], buffer[pos]);
        }

        return procedure;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends KTypePredicate<? super KType>> T forEach(final T predicate)
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
    public <T extends KTypeIndexedPredicate<? super KType>> T indexedForEach(final T predicate)
    {
        final KType[] buffer = this.buffer;
        final int size = elementsCount;

        final int[] qp = this.qp;

        for (int pos = 1; pos <= size; pos++)
        {
            if (!predicate.apply(qp[pos], buffer[pos]))
                break;
        }

        return predicate;
    }

    /**
     * {@inheritDoc}
     * cost: O(log2(N)) for a N sized queue
     */
    @Override
    public boolean insert(final int index, final KType element)
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

        return true;
    }

    /**
     * {@inheritDoc}
     * cost: O(1)
     */
    @Override
    public KType top()
    {
        KType elem = this.defaultValue;

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
        KType elem = this.defaultValue;

        if (elementsCount > 0)
        {
            elem = buffer[1];

            deleteIndex(qp[1]);
        }

        return elem;
    }

    /**
     * {@inheritDoc}
     * cost: O(1)
     */
    @Override
    public KType getIndex(final int index)
    {
        /*! #if($DEBUG) !*/
        assert index >= pq.length || pq[index] > 0 : "Element of index " + " doesn't exist !";
        /*! #end !*/

        KType elem = this.defaultValue;

        if (index < pq.length && pq[index] > 0)
        {
            elem = buffer[pq[index]];
        }

        return elem;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        int h = 1;
        final int size = pq.length;
        final KType[] buffer = this.buffer;
        final int[] pq = this.pq;

        for (int i = 0; i < size; i++)
        {
            if (pq[i] > 0)
            {
                //rehash of the index
                h = 31 * h + Internals.rehash(i);
                //append the rehash of the value
                h = 31 * h + Internals.rehash(buffer[pq[i]]);
            }
        }

        return h;
    }

    /**
     * {@inheritDoc}
     * cost: O(log2(N))
     */
    @Override
    public KType deleteIndex(final int index)
    {
        KType deletedElement = this.defaultValue;

        final int[] qp = this.qp;
        final int[] pq = this.pq;
        final KType[] buffer = this.buffer;

        if (index < pq.length && pq[index] > 0)
        {
            final int deletedPos = pq[index];
            deletedElement = buffer[deletedPos];

            if (deletedPos == elementsCount)
            {
                //we remove the last element
                pq[index] = 0;

                //for GC
                /*! #if ($TemplateOptions.KTypeGeneric) !*/
                buffer[deletedPos] = Intrinsics.<KType> defaultKTypeValue();
                /*! #end !*/

                //Not really needed, but usefull to catch inconsistencies
                /*! #if($DEBUG) !*/
                qp[deletedPos] = -1;
                /*! #end !*/

                //diminuish size
                elementsCount--;
            }
            else
            {
                //We are not removing the last element

                /*! #if($DEBUG) !*/
                assert (deletedPos > 0 && qp[deletedPos] == index) : String.format("pq[index] = %d, qp[pq[index]] = %d (index = %d)",
                        deletedPos, qp[deletedPos], index);
                /*! #end !*/

                final int lastElementIndex = qp[elementsCount];

                /*! #if($DEBUG) !*/
                assert (lastElementIndex >= 0 && pq[lastElementIndex] == elementsCount) : String.format("lastElementIndex = qp[elementsCount] = %d, pq[lastElementIndex] = %d, elementsCount = %d",
                        lastElementIndex, pq[lastElementIndex], elementsCount);
                /*! #end !*/

                //not needed, overwritten below :
                /*! #if($DEBUG) !*/
                qp[deletedPos] = -1;
                /*! #end !*/

                buffer[deletedPos] = buffer[elementsCount];
                //last element is now at pos deletedPos
                pq[lastElementIndex] = deletedPos;
                qp[deletedPos] = lastElementIndex;

                //mark the index element to be removed
                //we must reset with 0 so that qp[pq[index]] is always valid !
                pq[index] = 0;

                //Not really needed, but usefull to catch inconsistencies
                /*! #if($DEBUG) !*/
                qp[elementsCount] = -1;
                /*! #end !*/

                //for GC
                /*! #if ($TemplateOptions.KTypeGeneric) !*/
                buffer[elementsCount] = Intrinsics.<KType> defaultKTypeValue();
                /*! #end !*/

                //diminuish size
                elementsCount--;

                //after swapping positions
                /*! #if($DEBUG) !*/
                assert (pq[lastElementIndex] == deletedPos) : String.format("pq[lastElementIndex = %d] = %d, while deletedPos = %d, (index = %d)",
                        lastElementIndex, pq[lastElementIndex], deletedPos, index);

                assert (qp[deletedPos] == lastElementIndex) : String.format("qp[deletedPos = %d] = %d, while lastElementIndex = %d, (index = %d)",
                        deletedPos, qp[deletedPos], lastElementIndex, index);
                /*! #end !*/

                if (elementsCount > 1)
                {
                    //re-establish heap
                    sink(pq[lastElementIndex]);
                    swim(pq[lastElementIndex]);
                }
            }
        }

        return deletedElement;
    }

    /*! #if ($TemplateOptions.KTypeGeneric) !*/
    /**
     * {@inheritDoc}
     * cost: O(log2(N))
     */
    @Override
    public void changePriority(final int index)
    {
        if (index < pq.length && pq[index] > 0)
        {
            swim(pq[index]);
            sink(pq[index]);
        }
    }

    /*! #end !*/

    /**
     * {@inheritDoc}
     * cost: O(1)
     */
    @Override
    public boolean containsIndex(final int index)
    {
        if (index < pq.length && pq[index] > 0)
        {
            return true;
        }

        return false;
    }

    /**
     * this instance and obj can only be equal if either: <pre>
     * (both don't have set comparators)
     * or
     * (both have equal comparators defined by {@link #comparator}.equals(obj.comparator))</pre>
     * then, both heaps are compared as follows: <pre>
     * {@inheritDoc}</pre>
     */
    @Override
    /* #if ($TemplateOptions.KTypeGeneric) */
    @SuppressWarnings("unchecked")
    /* #end */
    public boolean equals(final Object obj)
    {
        if (obj != null)
        {
            if (obj == this)
                return true;

            //we can only compare both KTypeHeapPriorityQueue,
            //that has the same comparison function reference
            if (!(obj instanceof KTypeIndexedHeapPriorityQueue<?>))
                return false;

            final KTypeIndexedHeapPriorityQueue<KType> other = (KTypeIndexedHeapPriorityQueue<KType>) obj;

            if (other.size() != this.size())
                return false;

            //Iterate over the smallest pq buffer of the two.
            int[] pqBuffer, otherPqBuffer;
            KType[] buffer, otherBuffer;

            if (this.pq.length < other.pq.length) {

                pqBuffer = this.pq;
                otherPqBuffer = other.pq;
                buffer = this.buffer;
                otherBuffer = other.buffer;
            }
            else {

                pqBuffer = other.pq;
                otherPqBuffer = this.pq;
                buffer = other.buffer;
                otherBuffer = this.buffer;
            }

            final int pqBufferSize = pqBuffer.length;
            final KType currentValue, otherValue;
            int currentIndex, otherIndex;

            //Both have null comparators
            if (this.comparator == null && other.comparator == null) {

                for (int i = 0; i < pqBufferSize; i++)
                {
                    currentIndex = pqBuffer[i];

                    if (currentIndex > 0)
                    {
                        //check that currentIndex exists in otherBuffer at the same i
                        otherIndex = otherPqBuffer[i];

                        if (otherIndex <= 0)
                            return false;

                        //compare both elements with Comparable, or natural ordering
                        if (!Intrinsics.isCompEqualKTypeUnchecked(buffer[currentIndex], otherBuffer[otherIndex]))
                        {
                            return false;
                        }
                    }
                }

                return true;

            }
            else if (this.comparator != null && this.comparator.equals(other.comparator)) {

                /*! #if ($TemplateOptions.KTypeGeneric) !*/
                final Comparator<? super KType> comp = this.comparator;
                /*! #else
                KTypeComparator<? super KType> comp = this.comparator;
                #end !*/

                for (int i = 0; i < pqBufferSize; i++)
                {
                    currentIndex = pqBuffer[i];

                    if (currentIndex > 0)
                    {
                        //check that currentIndex exists in otherBuffer
                        otherIndex = otherPqBuffer[i];

                        if (otherIndex <= 0)
                            return false;

                        //compare both elements with Comparator
                        if (comp.compare(buffer[i], otherBuffer[i]) != 0)
                        {
                            return false;
                        }
                    }
                } //end for 

                return true;
            } //end else comparator
        }

        return false;
    }

    /**
     * Clone this object. The returned clone will use the same comparator.
     */
    @Override
    public KTypeIndexedHeapPriorityQueue<KType> clone()
    {
        final KTypeIndexedHeapPriorityQueue<KType> cloned = new KTypeIndexedHeapPriorityQueue<KType>(this.comparator, this.buffer.length + 1);

        for (final KTypeCursor<KType> cursor : this)
        {
            cloned.insert(cursor.index, cursor.value);
        }

        cloned.defaultValue = this.defaultValue;

        return cloned;
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
     * Ensures the internal buffer has enough free slots to accomodate the index
     * <code>index</code>. Increases internal buffer size if needed.
     */
    protected void ensureBufferSpace(final int index)
    {
        final int pqLen = (pq == null ? 0 : pq.length);
        if (index > pqLen - 1)
        {
            //resize to acomodate this index.
            final int newPQSize = index + KTypeIndexedHeapPriorityQueue.DEFAULT_CAPACITY;

            final int[] newPQIndex = new int[newPQSize];
            final KType[] newBuffer = Intrinsics.newKTypeArray(newPQSize + 1);
            final int[] newQPIndex = new int[newPQSize + 1];

            if (pqLen > 0)
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
    public KType[] toArray(final KType[] target)
    {
        //copy from index 1
        System.arraycopy(buffer, 1, target, 0, elementsCount);
        return target;
    }

    @Override
    public String toString()
    {
        final KType[] buffer = this.buffer;
        final int[] pq = this.pq;

        final StringBuilder buff = new StringBuilder();
        buff.append("[");

        boolean first = true;

        //Indices are displayed in ascending order, for easier reading.
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
                buff.append(buffer[pq[i]]);
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

        final KType[] buffer = this.buffer;
        final int[] pq = this.pq;
        final int[] qp = this.qp;

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

        final KType[] buffer = this.buffer;
        final int[] pq = this.pq;
        final int[] qp = this.qp;

        /*! #if ($TemplateOptions.KTypeGeneric) !*/
        final Comparator<? super KType> comp = this.comparator;
        /*! #else
        KTypeComparator<? super KType> comp = this.comparator;
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

        final KType[] buffer = this.buffer;
        final int[] pq = this.pq;
        final int[] qp = this.qp;

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

        final KType[] buffer = this.buffer;
        final int[] pq = this.pq;
        final int[] qp = this.qp;

        /*! #if ($TemplateOptions.KTypeGeneric) !*/
        final Comparator<? super KType> comp = this.comparator;
        /*! #else
        KTypeComparator<? super KType> comp = this.comparator;
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

    private void swim(final int k)
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

    private void sink(final int k)
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

/*! #if($DEBUG) !*/
    /**
     * methods to test invariant in assert, not present in final generated code
     */

    /**
     * method to test pq[]/qp[]/buffer[] consistency in assert expressions
     */
    private boolean isConsistent()
    {
        if (elementsCount > 0)
        {
            //A) For each valid index, (in pq), there is match in position in qp
            for (int index = 0; index < pq.length; index++)
            {
                if (pq[index] > 0)
                {
                    if (index != qp[pq[index]])
                    {
                        assert false : String.format("Inconsistent Index: index=%d, size=%d , pq[index] = %d, ==> qp[pq[index]] = %d",
                                index, size(), pq[index], qp[pq[index]]);
                    }
                }
            }

            //B) Reverse check : for each element of position pos in buffer, there is a match in pq
            for (int pos = 1; pos <= elementsCount; pos++)
            {

                if (pos != pq[qp[pos]])
                {
                    assert false : String.format("Inconsistent position: pos=%d, size=%d , qp[pos] = %d, ==> pq[qp[pos]] = %d",
                            pos, size(), qp[pos], pq[qp[pos]]);

                }
            }
        }

        return true;
    }

    /**
     * method to test heap invariant in assert expressions
     */
    // is buffer[1..N] a min heap?
    private boolean isMinHeap()
    {
        if (this.comparator == null)
        {
            return isMinHeapComparable(1);
        }

        return isMinHeapComparator(1);
    }

    // is subtree of pq[1..N] rooted at k a min heap?
    private boolean isMinHeapComparable(final int k)
    {
        final int N = elementsCount;
        final KType[] buffer = this.buffer;

        if (k > N)
            return true;
        final int left = 2 * k, right = 2 * k + 1;

        if (left <= N && Intrinsics.isCompSupKTypeUnchecked(buffer[k], buffer[left]))
            return false;
        if (right <= N && Intrinsics.isCompSupKTypeUnchecked(buffer[k], buffer[right]))
            return false;
        //recursively test
        return isMinHeapComparable(left) && isMinHeapComparable(right);
    }

    // is subtree of pq[1..N] rooted at k a min heap?
    private boolean isMinHeapComparator(final int k)
    {
        final int N = elementsCount;
        final KType[] buffer = this.buffer;

        /*! #if ($TemplateOptions.KTypeGeneric) !*/
        final Comparator<? super KType> comp = this.comparator;
        /*! #else
        KTypeComparator<? super KType> comp = this.comparator;
        #end !*/

        if (k > N)
            return true;
        final int left = 2 * k, right = 2 * k + 1;

        if (left <= N && comp.compare(buffer[k], buffer[left]) > 0)
            return false;
        if (right <= N && comp.compare(buffer[k], buffer[right]) > 0)
            return false;
        //recursively test
        return isMinHeapComparator(left) && isMinHeapComparator(right);
    }

    //end ifdef debug
/*! #end !*/
}
