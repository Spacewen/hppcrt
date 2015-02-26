package com.carrotsearch.hppcrt.lists;

import java.util.*;

import com.carrotsearch.hppcrt.*;
import com.carrotsearch.hppcrt.cursors.*;
import com.carrotsearch.hppcrt.predicates.*;
import com.carrotsearch.hppcrt.procedures.*;
import com.carrotsearch.hppcrt.sorting.*;
import com.carrotsearch.hppcrt.strategies.*;

/*! #import("com/carrotsearch/hppcrt/Intrinsics.java") !*/
/**
 * An array-backed deque (double-ended queue)  of KTypes. A single array is used to store and
 * manipulate all elements. Reallocations are governed by a {@link ArraySizingStrategy}
 * and may be expensive if they move around really large chunks of memory.
 * This dequeue is also a KTypeIndexedContainer, where index 0 is the head of the queue, and
 * size() - 1 index is the last element.
#if ($TemplateOptions.KTypeGeneric)
 * A brief comparison of the API against the Java Collections framework:
 * <table class="nice" summary="Java Collections ArrayDeque and HPPC ObjectArrayDeque, related methods.">
 * <caption>Java Collections ArrayDeque and HPPC {@link ObjectArrayDeque}, related methods.</caption>
 * <thead>
 *     <tr class="odd">
 *         <th scope="col">{@linkplain ArrayDeque java.util.ArrayDeque}</th>
 *         <th scope="col">{@link ObjectArrayDeque}</th>
 *     </tr>
 * </thead>
 * <tbody>
 * <tr            ><td>addFirst       </td><td>addFirst       </td></tr>
 * <tr class="odd"><td>addLast        </td><td>addLast        </td></tr>
 * <tr            ><td>removeFirst    </td><td>removeLast     </td></tr>
 * <tr class="odd"><td>getFirst       </td><td>getFirst       </td></tr>
 * <tr            ><td>getLast        </td><td>getLast        </td></tr>
 * <tr class="odd"><td>removeFirstOccurrence,
 *                     removeLastOccurrence
 *                                    </td><td>removeFirstOccurrence,
 *                                             removeLastOccurrence
 *                                                            </td></tr>
 * <tr            ><td>size           </td><td>size           </td></tr>
 * <tr class="odd"><td>Object[] toArray()</td><td>KType[] toArray()</td></tr>
 * <tr            ><td>iterator       </td><td>{@linkplain #iterator cursor over values}</td></tr>
 * <tr class="odd"><td>other methods inherited from Stack, Queue</td><td>not implemented</td></tr>
 * </tbody>
 * </table>
#else
 * <p>See {@link ObjectArrayDeque} class for API similarities and differences against Java
 * Collections.
#end
 */
/*! ${TemplateOptions.generatedAnnotation} !*/
public class KTypeArrayDeque<KType>
extends AbstractKTypeCollection<KType> implements KTypeDeque<KType>, KTypeIndexedContainer<KType>, Cloneable
{
    /**
     * Default capacity if no other capacity is given in the constructor.
     */
    public final static int DEFAULT_CAPACITY = 5;

    /**
     * Internal array for storing elements.
     * 
    #if ($TemplateOptions.KTypeGeneric)
     * <p><strong>Important!</strong>
     * The actual value in this field is always an instance of <code>Object[]</code>.
     * Be warned that <code>javac</code> emits additional casts when <code>buffer</code>
     * is directly accessed; <strong>these casts
     * may result in exceptions at runtime</strong>. A workaround is to cast directly to
     * <code>Object[]</code> before accessing the buffer's elements (although it is highly
     * recommended to use a {@link #iterator()} instead.
     * </pre>
    #end
     * <p>
     * Direct deque iteration from head to tail: iterate buffer[i % buffer.length] for i in [this.head; this.head + size()[
     * </p>
     */
    public KType[] buffer;

    /**
     * The index of the element at the head of the deque or an
     * arbitrary number equal to tail if the deque is empty.
     */
    public int head;

    /**
     * The index at which the next element would be added to the tail
     * of the deque.
     */
    public int tail;

    /**
     * Buffer resizing strategy.
     */
    protected final ArraySizingStrategy resizer;

    /**
     * internal pool of DescendingValueIterator (must be created in constructor)
     */
    protected final IteratorPool<KTypeCursor<KType>, DescendingValueIterator> descendingValueIteratorPool;

    /**
     * internal pool of ValueIterator (must be created in constructor)
     */
    protected final IteratorPool<KTypeCursor<KType>, ValueIterator> valueIteratorPool;

    /**
     * Create with default sizing strategy and initial capacity for storing
     * {@value #DEFAULT_CAPACITY} elements.
     * 
     * @see BoundedProportionalArraySizingStrategy
     */
    public KTypeArrayDeque()
    {
        this(KTypeArrayDeque.DEFAULT_CAPACITY);
    }

    /**
     * Create with default sizing strategy and the given initial capacity.
     * 
     * @see BoundedProportionalArraySizingStrategy
     */
    public KTypeArrayDeque(final int initialCapacity)
    {
        this(initialCapacity, new BoundedProportionalArraySizingStrategy());
    }

    /**
     * Create with a custom buffer resizing strategy.
     */
    public KTypeArrayDeque(int initialCapacity, final ArraySizingStrategy resizer)
    {
        assert initialCapacity >= 0 : "initialCapacity must be >= 0: " + initialCapacity;
        assert resizer != null;

        this.resizer = resizer;

        // +1 because there is always one empty slot in a deque. (seen ensure buffer space)
        initialCapacity = resizer.round(initialCapacity + 1);

        this.buffer = Intrinsics.newKTypeArray(initialCapacity);

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
                        obj.cursor.index = KTypeArrayDeque.oneLeft(KTypeArrayDeque.this.head, KTypeArrayDeque.this.buffer.length);
                        obj.remaining = KTypeArrayDeque.this.size();
                    }

                    @Override
                    public void reset(final ValueIterator obj) {
                        // nothing

                    }
                });

        this.descendingValueIteratorPool = new IteratorPool<KTypeCursor<KType>, DescendingValueIterator>(
                new ObjectFactory<DescendingValueIterator>() {

                    @Override
                    public DescendingValueIterator create()
                    {
                        return new DescendingValueIterator();
                    }

                    @Override
                    public void initialize(final DescendingValueIterator obj)
                    {

                        obj.cursor.index = KTypeArrayDeque.this.tail;
                        obj.remaining = KTypeArrayDeque.this.size();
                    }

                    @Override
                    public void reset(final DescendingValueIterator obj) {
                        // nothing
                    }
                });
    }

    /**
     * Creates a new deque from elements of another container, appending them
     * at the end of this deque.
     */
    public KTypeArrayDeque(final KTypeContainer<? extends KType> container)
    {
        this(container.size());
        addLast(container);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addFirst(final KType e1)
    {
        int h = KTypeArrayDeque.oneLeft(this.head, this.buffer.length);
        if (h == this.tail)
        {
            ensureBufferSpace(1);
            h = KTypeArrayDeque.oneLeft(this.head, this.buffer.length);
        }
        this.buffer[this.head = h] = e1;
    }

    /**
     * Vararg-signature method for adding elements at the front of this deque.
     * 
     * <p><b>This method is handy, but costly if used in tight loops (anonymous
     * array passing)</b></p>
     */
    public void addFirst(final KType... elements)
    {
        ensureBufferSpace(elements.length);

        // For now, naive loop.
        for (int i = 0; i < elements.length; i++) {
            addFirst(elements[i]);
        }
    }

    /**
     * Inserts all elements from the given container to the front of this deque.
     * 
     * @return Returns the number of elements actually added as a result of this
     * call.
     */
    public int addFirst(final KTypeContainer<? extends KType> container)
    {
        return addFirst((Iterable<? extends KTypeCursor<? extends KType>>) container);
    }

    /**
     * Inserts all elements from the given iterable to the front of this deque.
     * 
     * @return Returns the number of elements actually added as a result of this call.
     */
    public int addFirst(final Iterable<? extends KTypeCursor<? extends KType>> iterable)
    {
        int size = 0;
        for (final KTypeCursor<? extends KType> cursor : iterable)
        {
            addFirst(cursor.value);
            size++;
        }
        return size;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addLast(final KType e1)
    {
        int t = KTypeArrayDeque.oneRight(this.tail, this.buffer.length);
        if (this.head == t)
        {
            ensureBufferSpace(1);
            t = KTypeArrayDeque.oneRight(this.tail, this.buffer.length);
        }
        this.buffer[this.tail] = e1;
        this.tail = t;
    }

    /**
     * Vararg-signature method for adding elements at the end of this deque.
     * 
     * <p><b>This method is handy, but costly if used in tight loops (anonymous
     * array passing)</b></p>
     */
    public void addLast(final KType... elements)
    {
        ensureBufferSpace(1);

        // For now, naive loop.
        for (int i = 0; i < elements.length; i++) {
            addLast(elements[i]);
        }
    }

    /**
     * Inserts all elements from the given container to the end of this deque.
     * 
     * @return Returns the number of elements actually added as a result of this
     * call.
     */
    public int addLast(final KTypeContainer<? extends KType> container)
    {
        return addLast((Iterable<? extends KTypeCursor<? extends KType>>) container);
    }

    /**
     * Inserts all elements from the given iterable to the end of this deque.
     * 
     * @return Returns the number of elements actually added as a result of this call.
     */
    public int addLast(final Iterable<? extends KTypeCursor<? extends KType>> iterable)
    {
        int size = 0;
        for (final KTypeCursor<? extends KType> cursor : iterable)
        {
            addLast(cursor.value);
            size++;
        }
        return size;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public KType removeFirst()
    {
        assert size() > 0 : "The deque is empty.";

        final KType result = this.buffer[this.head];
        /*! #if ($TemplateOptions.KTypeGeneric) !*/
        this.buffer[this.head] = Intrinsics.<KType> defaultKTypeValue();
        /*! #end !*/
        this.head = KTypeArrayDeque.oneRight(this.head, this.buffer.length);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public KType removeLast()
    {
        assert size() > 0 : "The deque is empty.";

        this.tail = KTypeArrayDeque.oneLeft(this.tail, this.buffer.length);
        final KType result = this.buffer[this.tail];
        /*! #if ($TemplateOptions.KTypeGeneric) !*/
        this.buffer[this.tail] = Intrinsics.<KType> defaultKTypeValue();
        /*! #end !*/
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public KType getFirst()
    {
        assert size() > 0 : "The deque is empty.";

        return this.buffer[this.head];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public KType getLast()
    {
        assert size() > 0 : "The deque is empty.";

        return this.buffer[KTypeArrayDeque.oneLeft(this.tail, this.buffer.length)];
    }

    /**
     * {@inheritDoc}
     * The returned position is relative to the head,
     * i.e w.r.t the {@link KTypeIndexedContainer}, index 0 is the head of the queue, size() - 1 is the last element position.
     */
    @Override
    public int removeFirstOccurrence(final KType e1)
    {
        int pos = -1;

        final int index = bufferIndexOf(e1);

        if (index >= 0) {

            pos = bufferIndexToPosition(index);
            removeAtBufferIndex(index);
        }

        return pos;
    }

    /**
     * Return the index of the first element equal to
     * <code>e1</code>. The index points to the {@link #buffer} array.
     * 
     * @param e1 The element to look for.
     * @return Returns the index in {@link #buffer} of the first element equal to <code>e1</code>
     * or <code>-1</code> if not found.
     */
    public int bufferIndexOf(final KType e1)
    {
        final int last = this.tail;
        final int bufLen = this.buffer.length;
        final KType[] buffer = this.buffer;

        for (int i = this.head; i != last; i = KTypeArrayDeque.oneRight(i, bufLen))
        {
            if (Intrinsics.equalsKType(e1, buffer[i])) {
                return i;
            }
        }

        return -1;
    }

    /**
     * {@inheritDoc}
     * The returned position is relative to the head,
     * i.e w.r.t the {@link KTypeIndexedContainer}, index 0 is the head of the queue, size() - 1 is the last element position.
     */
    @Override
    public int removeLastOccurrence(final KType e1)
    {
        int pos = -1;

        final int index = lastBufferIndexOf(e1);

        if (index >= 0) {

            pos = bufferIndexToPosition(index);
            removeAtBufferIndex(index);
        }

        return pos;
    }

    /**
     * Return the index of the last element equal to
     * <code>e1</code>. The index points to the {@link #buffer} array.
     * 
     * @param e1 The element to look for.
     * @return Returns the index in {@link #buffer} of the first element equal to <code>e1</code>
     * or <code>-1</code> if not found.
     */
    public int lastBufferIndexOf(final KType e1)
    {
        final int bufLen = this.buffer.length;
        final int last = KTypeArrayDeque.oneLeft(this.head, bufLen);
        final KType[] buffer = this.buffer;

        for (int i = KTypeArrayDeque.oneLeft(this.tail, bufLen); i != last; i = KTypeArrayDeque.oneLeft(i, bufLen))
        {
            if (Intrinsics.equalsKType(e1, buffer[i])) {
                return i;
            }
        }

        return -1;
    }

    /**
     * KTypeIndexedContainer methods
     */

    /**
     * {@inheritDoc}
     * The returned position is relative to the head,
     * i.e w.r.t the {@link KTypeIndexedContainer}, index 0 is the head of the queue, size() - 1 is the last element position.
     */
    @Override
    public int indexOf(final KType e1) {

        return bufferIndexToPosition(bufferIndexOf(e1));
    }

    /**
     * {@inheritDoc}
     * The returned position is relative to the head,
     * i.e w.r.t the {@link KTypeIndexedContainer}, index 0 is the head of the queue, size() - 1 is the last element position.
     */
    @Override
    public int lastIndexOf(final KType e1) {

        return bufferIndexToPosition(lastBufferIndexOf(e1));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int removeAllOccurrences(final KType e1)
    {
        int removed = 0;
        final int last = this.tail;
        final int bufLen = this.buffer.length;
        final KType[] buffer = this.buffer;

        int from, to;
        for (from = to = this.head; from != last; from = KTypeArrayDeque.oneRight(from, bufLen))
        {
            if (Intrinsics.equalsKType(e1, buffer[from]))
            {
                /*! #if ($TemplateOptions.KTypeGeneric) !*/
                buffer[from] = Intrinsics.<KType> defaultKTypeValue();
                /*! #end !*/
                removed++;
                continue;
            }

            if (to != from)
            {
                buffer[to] = buffer[from];
                /*! #if ($TemplateOptions.KTypeGeneric) !*/
                buffer[from] = Intrinsics.<KType> defaultKTypeValue();
                /*! #end !*/
            }

            to = KTypeArrayDeque.oneRight(to, bufLen);
        }

        this.tail = to;
        return removed;
    }

    /**
     * Removes the element at <code>index</code> in the internal
     * {@link #buffer} array.
     * 
     * @param index Index of the element to remove. The index must be located between
     * {@link #head} and {@link #tail} in modulo {@link #buffer} arithmetic.
     */
    public void removeAtBufferIndex(final int index)
    {
        assert (this.head <= this.tail
                ? index >= this.head && index < this.tail
                : index >= this.head || index < this.tail) : "Index out of range (head="
                + this.head + ", tail=" + this.tail + ", index=" + index + ").";

                // Cache fields in locals (hopefully moved to registers).
                final KType[] b = this.buffer;
                final int bufLen = b.length;
                final int lastIndex = bufLen - 1;
                final int head = this.head;
                final int tail = this.tail;

                final int leftChunk = Math.abs(index - head) % bufLen;
                final int rightChunk = Math.abs(tail - index) % bufLen;

                if (leftChunk < rightChunk)
                {
                    if (index >= head)
                    {
                        System.arraycopy(b, head, b, head + 1, leftChunk);
                    }
                    else
                    {
                        System.arraycopy(b, 0, b, 1, index);
                        b[0] = b[lastIndex];
                        System.arraycopy(b, head, b, head + 1, lastIndex - head);
                    }
                    /*! #if ($TemplateOptions.KTypeGeneric) !*/
                    b[head] = Intrinsics.<KType> defaultKTypeValue();
                    /*! #end !*/
                    this.head = KTypeArrayDeque.oneRight(head, bufLen);
                }
                else
                {
                    if (index < tail)
                    {
                        System.arraycopy(b, index + 1, b, index, rightChunk);
                    }
                    else
                    {
                        System.arraycopy(b, index + 1, b, index, lastIndex - index);
                        b[lastIndex] = b[0];
                        System.arraycopy(b, 1, b, 0, tail);
                    }
                    /*! #if ($TemplateOptions.KTypeGeneric) !*/
                    b[tail] = Intrinsics.<KType> defaultKTypeValue();
                    /*! #end !*/
                    this.tail = KTypeArrayDeque.oneLeft(tail, bufLen);
                }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int size()
    {
        if (this.head <= this.tail)
        {
            return this.tail - this.head;
        }

        return (this.tail - this.head + this.buffer.length);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int capacity() {

        return this.buffer.length;
    }

    /**
     * {@inheritDoc}
     * <p>The internal array buffers are not released as a result of this call.</p>
     */
    @Override
    public void clear()
    {
        /*! #if ($TemplateOptions.KTypeGeneric) !*/
        if (this.head < this.tail)
        {
            KTypeArrays.blankArray(this.buffer, this.head, this.tail);
        }
        else
        {
            KTypeArrays.blankArray(this.buffer, 0, this.tail);
            KTypeArrays.blankArray(this.buffer, this.head, this.buffer.length);
        }
        /*! #end !*/

        this.head = this.tail = 0;
    }

    /**
     * Compact the internal buffer to prepare sorting
     * Beware, this changes the relative order of elements, so is only useful to
     * not-stable sorts while sorting the WHOLE buffer !
     */
    private void compactBeforeSorting()
    {
        if (this.head > this.tail)
        {
            final int size = size();

            /*! #if ($TemplateOptions.KTypeGeneric) !*/
            final int hole = this.head - this.tail;
            /*! #end !*/

            //pack the separated chunk to the beginning of the buffer
            System.arraycopy(this.buffer, this.head, this.buffer, this.tail, this.buffer.length - this.head);

            //reset of the positions
            this.head = 0;
            this.tail = size;

            //for GC sake, reset hole elements now at the end of buffer
            /*! #if ($TemplateOptions.KTypeGeneric) !*/
            KTypeArrays.blankArray(this.buffer, this.tail, this.tail + hole);
            /*! #end !*/
        }
    }

    /**
     * Release internal buffers of this deque and reallocate the smallest buffer possible.
     */
    public void release()
    {
        this.head = this.tail = 0;
        this.buffer = Intrinsics.newKTypeArray(this.resizer.round(KTypeArrayDeque.DEFAULT_CAPACITY));
    }

    /**
     * Ensures the internal buffer has enough free slots to store
     * <code>expectedAdditions</code>. Increases internal buffer size if needed.
     */
    protected void ensureBufferSpace(final int expectedAdditions)
    {
        final int bufferLen = (this.buffer == null ? 0 : this.buffer.length);
        final int elementsCount = size();
        // +1 because there is always one empty slot in a deque.
        if (elementsCount >= bufferLen - expectedAdditions - 1)
        {
            final int newSize = this.resizer.grow(bufferLen, elementsCount, expectedAdditions + 1);
            assert newSize >= (elementsCount + expectedAdditions + 1) : "Resizer failed to" +
                    " return sensible new size: " + newSize + " <= "
                    + (elementsCount + expectedAdditions);

            final KType[] newBuffer = Intrinsics.<KType[]> newKTypeArray(newSize);
            if (bufferLen > 0)
            {
                toArray(newBuffer);
                this.tail = elementsCount;
                this.head = 0;
            }
            this.buffer = newBuffer;
        }
    }

    /**
     * Copies elements of this deque to an array. The content of the <code>target</code>
     * array is filled from index 0 (head of the queue) to index <code>size() - 1</code>
     * (tail of the queue).
     * 
     * @param target The target array must be large enough to hold all elements.
     * @return Returns the target argument for chaining.
     */
    @Override
    public KType[] toArray(final KType[] target)
    {
        assert target.length >= size() : "Target array must be >= " + size();

        if (this.head < this.tail)
        {
            // The contents is not wrapped around. Just copy.
            System.arraycopy(this.buffer, this.head, target, 0, size());
        }
        else if (this.head > this.tail)
        {
            // The contents is split. Merge elements from the following indexes:
            // [head...buffer.length - 1][0, tail - 1]
            final int rightCount = this.buffer.length - this.head;
            System.arraycopy(this.buffer, this.head, target, 0, rightCount);
            System.arraycopy(this.buffer, 0, target, rightCount, this.tail);
        }

        return target;
    }

    /**
     * Clone this object. The returned clone will reuse the same array resizing strategy.
     * It also realizes a trim-to- this.size() in the process.
     */
    @Override
    public KTypeArrayDeque<KType> clone()
    {
        /* #if ($TemplateOptions.KTypeGeneric) */
        @SuppressWarnings("unchecked")
        /* #end */
        //real constructor call
        final KTypeArrayDeque<KType> cloned = new KTypeArrayDeque<KType>(this.size(), this.resizer);

        cloned.defaultValue = this.defaultValue;

        //copied in-order by construction.
        cloned.addLast(this);

        return cloned;

    }

    /**
     * An iterator implementation for {@link ObjectArrayDeque#iterator}.
     */
    public final class ValueIterator extends AbstractIterator<KTypeCursor<KType>>
    {
        public final KTypeCursor<KType> cursor;
        private int remaining;

        public ValueIterator()
        {
            this.cursor = new KTypeCursor<KType>();
            this.cursor.index = KTypeArrayDeque.oneLeft(KTypeArrayDeque.this.head, KTypeArrayDeque.this.buffer.length);
            this.remaining = KTypeArrayDeque.this.size();
        }

        @Override
        protected KTypeCursor<KType> fetch()
        {
            if (this.remaining == 0) {
                return done();
            }

            this.remaining--;
            this.cursor.value = KTypeArrayDeque.this.buffer[this.cursor.index = KTypeArrayDeque.oneRight(this.cursor.index, KTypeArrayDeque.this.buffer.length)];
            return this.cursor;
        }
    }

    /**
     * An iterator implementation for {@link ObjectArrayDeque#descendingIterator()}.
     */
    public final class DescendingValueIterator extends AbstractIterator<KTypeCursor<KType>>
    {
        public final KTypeCursor<KType> cursor;
        private int remaining;

        public DescendingValueIterator()
        {
            this.cursor = new KTypeCursor<KType>();
            this.cursor.index = KTypeArrayDeque.this.tail;
            this.remaining = KTypeArrayDeque.this.size();
        }

        @Override
        protected KTypeCursor<KType> fetch()
        {
            if (this.remaining == 0) {
                return done();
            }

            this.remaining--;
            this.cursor.value = KTypeArrayDeque.this.buffer[this.cursor.index = KTypeArrayDeque.oneLeft(this.cursor.index, KTypeArrayDeque.this.buffer.length)];
            return this.cursor;
        }
    }

    /**
     * Returns a cursor over the values of this deque (in head to tail order). The
     * iterator is implemented as a cursor and it returns <b>the same cursor instance</b>
     * on every call to {@link Iterator#next()} (to avoid boxing of primitive types). To
     * read the current value (or index in the deque's buffer) use the cursor's public
     * fields. An example is shown below.
     * 
     * <pre>
     * for (IntValueCursor c : intDeque)
     * {
     *     System.out.println(&quot;buffer index=&quot;
     *         + c.index + &quot; value=&quot; + c.value);
     * }
     * </pre>
     * @return
     */
    @Override
    public ValueIterator iterator()
    {
        //return new ValueIterator();
        return this.valueIteratorPool.borrow();
    }

    /**
     * Returns a cursor over the values of this deque (in tail to head order). The
     * iterator is implemented as a cursor and it returns <b>the same cursor instance</b>
     * on every call to {@link Iterator#next()} (to avoid boxing of primitive types). To
     * read the current value (or index in the deque's buffer) use the cursor's public
     * fields. An example is shown below.
     * 
     * <pre>
     * for (Iterator<IntCursor> i = intDeque.descendingIterator(); i.hasNext(); )
     * {
     *     final IntCursor c = i.next();
     *     System.out.println(&quot;buffer index=&quot;
     *         + c.index + &quot; value=&quot; + c.value);
     * }
     * </pre>
     * @return
     */
    @Override
    public DescendingValueIterator descendingIterator()
    {
        //return new DescendingValueIterator();
        return this.descendingValueIteratorPool.borrow();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends KTypeProcedure<? super KType>> T forEach(final T procedure)
    {
        forEach(procedure, this.head, this.tail);
        return procedure;
    }

    /**
     * Applies <code>procedure</code> to a slice of the deque,
     * <code>fromIndex</code>, inclusive, to <code>toIndex</code>,
     * exclusive, indices are in {@link #buffer} array.
     */
    private void forEach(final KTypeProcedure<? super KType> procedure, final int fromIndex, final int toIndex)
    {
        final KType[] buffer = this.buffer;
        for (int i = fromIndex; i != toIndex; i = KTypeArrayDeque.oneRight(i, buffer.length))
        {
            procedure.apply(buffer[i]);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends KTypePredicate<? super KType>> T forEach(final T predicate)
    {
        final int fromIndex = this.head;
        final int toIndex = this.tail;

        final KType[] buffer = this.buffer;
        for (int i = fromIndex; i != toIndex; i = KTypeArrayDeque.oneRight(i, buffer.length))
        {
            if (!predicate.apply(buffer[i])) {
                break;
            }
        }

        return predicate;
    }

    /**
     * Applies <code>procedure</code> to all elements of this deque, tail to head.
     */
    @Override
    public <T extends KTypeProcedure<? super KType>> T descendingForEach(final T procedure)
    {
        descendingForEach(procedure, this.head, this.tail);
        return procedure;
    }

    /**
     * Applies <code>procedure</code> to a slice of the deque,
     * <code>toIndex</code>, exclusive, down to <code>fromIndex</code>, inclusive.
     */
    private void descendingForEach(final KTypeProcedure<? super KType> procedure,
            final int fromIndex, final int toIndex)
    {
        if (fromIndex == toIndex) {
            return;
        }

        final KType[] buffer = this.buffer;
        int i = toIndex;
        do
        {
            i = KTypeArrayDeque.oneLeft(i, buffer.length);
            procedure.apply(buffer[i]);
        } while (i != fromIndex);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends KTypePredicate<? super KType>> T descendingForEach(final T predicate)
    {
        descendingForEach(predicate, this.head, this.tail);
        return predicate;
    }

    /**
     * Applies <code>predicate</code> to a slice of the deque,
     * <code>toIndex</code>, exclusive, down to <code>fromIndex</code>, inclusive
     * or until the predicate returns <code>false</code>.
     * Indices are in {@link #buffer} array.
     */
    private void descendingForEach(final KTypePredicate<? super KType> predicate,
            final int fromIndex, final int toIndex)
    {
        if (fromIndex == toIndex) {
            return;
        }

        final KType[] buffer = this.buffer;
        int i = toIndex;
        do
        {
            i = KTypeArrayDeque.oneLeft(i, buffer.length);
            if (!predicate.apply(buffer[i])) {
                break;
            }
        } while (i != fromIndex);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int removeAll(final KTypePredicate<? super KType> predicate)
    {
        int removed = 0;
        final int last = this.tail;
        final int bufLen = this.buffer.length;
        final KType[] buffer = this.buffer;

        int from, to;
        from = to = this.head;
        try
        {
            for (from = to = this.head; from != last; from = KTypeArrayDeque.oneRight(from, bufLen))
            {
                if (predicate.apply(buffer[from]))
                {
                    /*! #if ($TemplateOptions.KTypeGeneric) !*/
                    buffer[from] = Intrinsics.<KType> defaultKTypeValue();
                    /*! #end !*/
                    removed++;
                    continue;
                }

                if (to != from)
                {
                    buffer[to] = buffer[from];
                    /*! #if ($TemplateOptions.KTypeGeneric) !*/
                    buffer[from] = Intrinsics.<KType> defaultKTypeValue();
                    /*! #end !*/
                }

                to = KTypeArrayDeque.oneRight(to, bufLen);
            }
        }
        finally
        {
            // Keep the deque in consistent state even if the predicate throws an exception.
            for (; from != last; from = KTypeArrayDeque.oneRight(from, bufLen))
            {
                if (to != from)
                {
                    buffer[to] = buffer[from];
                    /*! #if ($TemplateOptions.KTypeGeneric) !*/
                    buffer[from] = Intrinsics.<KType> defaultKTypeValue();
                    /*! #end !*/
                }

                to = KTypeArrayDeque.oneRight(to, bufLen);
            }
            this.tail = to;
        }

        return removed;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean contains(final KType e)
    {
        final int fromIndex = this.head;
        final int toIndex = this.tail;

        final KType[] buffer = this.buffer;

        for (int i = fromIndex; i != toIndex; i = KTypeArrayDeque.oneRight(i, buffer.length))
        {
            if (Intrinsics.equalsKType(e, buffer[i])) {
                return true;
            }
        }

        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        int h = 1;
        final int fromIndex = this.head;
        final int toIndex = this.tail;

        final KType[] buffer = this.buffer;

        for (int i = fromIndex; i != toIndex; i = KTypeArrayDeque.oneRight(i, buffer.length))
        {
            h = 31 * h + Internals.rehash(buffer[i]);
        }
        return h;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    /* #if ($TemplateOptions.KTypeGeneric) */
    @SuppressWarnings("unchecked")
    /* #end */
    public boolean equals(final Object obj)
    {
        if (obj != null)
        {
            if (obj == this) {
                return true;
            }

            if (obj instanceof KTypeDeque<?>)
            {
                final KTypeDeque<Object> other = (KTypeDeque<Object>) obj;

                if (other.size() != this.size()) {

                    return false;
                }

                final int fromIndex = this.head;
                final KType[] buffer = this.buffer;
                int i = fromIndex;

                //request a pooled iterator
                /*! #if ($TemplateOptions.KTypeGeneric) !*/
                final Iterator<KTypeCursor<Object>> it = other.iterator();
                /*! #else
                final Iterator<KTypeCursor> it = other.iterator();
                #end !*/

                /*! #if ($TemplateOptions.KTypeGeneric) !*/
                KTypeCursor<Object> c;
                /*! #else
                KTypeCursor c;
                #end !*/

                while (it.hasNext())
                {
                    c = it.next();

                    if (!Intrinsics.equalsKType(c.value, buffer[i]))
                    {
                        //if iterator was pooled, recycled it
                        if (it instanceof AbstractIterator<?>)
                        {
                            ((AbstractIterator<?>) it).release();
                        }

                        return false;
                    }
                    i = KTypeArrayDeque.oneRight(i, buffer.length);
                }

                return true;

            } //end if KTypeDeque
            else if (obj instanceof KTypeIndexedContainer<?>)
            {
                final KTypeIndexedContainer<?> other = (KTypeIndexedContainer<?>) obj;
                return other.size() == this.size() &&
                        allIndexesEqual(this, (KTypeIndexedContainer<KType>) other, this.size());
            }
        }
        return false;
    }

    /**
     * Compare index-aligned KTypeIndexedContainer objects
     */
    private boolean allIndexesEqual(
            final KTypeIndexedContainer<KType> b1,
            final KTypeIndexedContainer<KType> b2, final int length)
    {
        for (int i = 0; i < length; i++)
        {
            final KType o1 = b1.get(i);
            final KType o2 = b2.get(i);

            if (!Intrinsics.equalsKType(o1, o2))
            {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns a new object of this class with no need to declare generic type (shortcut
     * instead of using a constructor).
     */
    public static/* #if ($TemplateOptions.KTypeGeneric) */<KType> /* #end */
    KTypeArrayDeque<KType> newInstance()
    {
        return new KTypeArrayDeque<KType>();
    }

    /**
     * Returns a new object of this class with no need to declare generic type (shortcut
     * instead of using a constructor).
     */
    public static/* #if ($TemplateOptions.KTypeGeneric) */<KType> /* #end */
    KTypeArrayDeque<KType> newInstanceWithCapacity(final int initialCapacity)
    {
        return new KTypeArrayDeque<KType>(initialCapacity);
    }

    /**
     * Create a new deque by pushing a variable number of arguments to the end of it.
     */
    public static/* #if ($TemplateOptions.KTypeGeneric) */<KType> /* #end */
    KTypeArrayDeque<KType> from(final KType... elements)
    {
        final KTypeArrayDeque<KType> coll = new KTypeArrayDeque<KType>(elements.length);
        coll.addLast(elements);
        return coll;
    }

    /**
     * Create a new deque by pushing a variable number of arguments to the end of it.
     */
    public static/* #if ($TemplateOptions.KTypeGeneric) */<KType> /* #end */
    KTypeArrayDeque<KType> from(final KTypeArrayDeque<KType> container)
    {
        return new KTypeArrayDeque<KType>(container);
    }

    ////////////////////////////
    /**
     * In-place sort the dequeue from [beginIndex, endIndex[
     * by natural ordering (smaller first)
     * @param beginIndex the start index to be sorted
     * @param endIndex the end index to be sorted (excluded)
     #if ($TemplateOptions.KTypeGeneric)
     * @throws ClassCastException if the deque contains elements that are not mutually Comparable.
     #end
     */
    public void sort(final int beginIndex, final int endIndex)
    {
        assert endIndex <= size();

        if (endIndex - beginIndex > 1)
        {
            //Fast path : if the actual indices matching [beginIndex; endIndex[
            //in the underlying buffer are in increasing order (means there is no folding of buffer in the interval),
            // use quicksort array version directly.
            final int bufferPosStart = indexToBufferPosition(beginIndex);
            final int bufferPosEnd = indexToBufferPosition(endIndex);

            if (bufferPosEnd > bufferPosStart) {

                KTypeSort.quicksort(this.buffer, bufferPosStart, bufferPosEnd);
            }
            else {
                //Use the slower KTypeIndexedContainer sort
                KTypeSort.quicksort(this, beginIndex, endIndex);
            }
        }
    }

    /**
     * In-place sort the dequeue from [beginIndex, endIndex[
     * using a #if ($TemplateOptions.KTypeGeneric) <code>Comparator</code> #else <code>KTypeComparator<? super KType></code> #end
     * <p><b>
     * This routine uses Dual-pivot Quicksort, from [Yaroslavskiy 2009] #if ($TemplateOptions.KTypeGeneric), so is NOT stable. #end
     * </b></p>
     * @param beginIndex the start index to be sorted
     * @param endIndex the end index to be sorted (excluded)
     */
    public void sort(
            final int beginIndex, final int endIndex,
            /*! #if ($TemplateOptions.KTypeGeneric) !*/
            final Comparator<? super KType>
            /*! #else
            KTypeComparator<? super KType>
            #end !*/
            comp)
    {
        assert endIndex <= size();

        if (endIndex - beginIndex > 1)
        {
            //Fast path : if the actual indices matching [beginIndex; endIndex[
            //in the underlying buffer are in increasing order (means there is no folding of buffer in the interval),
            // use quicksort array version directly.
            final int bufferPosStart = indexToBufferPosition(beginIndex);
            final int bufferPosEnd = indexToBufferPosition(endIndex);

            if (bufferPosEnd > bufferPosStart) {

                KTypeSort.quicksort(this.buffer, bufferPosStart, bufferPosEnd, comp);
            }
            else {
                //Use the slower KTypeIndexedContainer sort
                KTypeSort.quicksort(this, beginIndex, endIndex, comp);
            }
        }
    }

    /**
     * In-place sort the whole dequeue by natural ordering (smaller first)
     * <p><b>
     * This routine uses Dual-pivot Quicksort, from [Yaroslavskiy 2009].
     * </b></p>
     * @param beginIndex
     * @param endIndex
      #if ($TemplateOptions.KTypeGeneric)
     * @throws ClassCastException if the deque contains elements that are not mutually Comparable.
     #end
     */
    public void sort()
    {
        if (size() > 1)
        {
            compactBeforeSorting();
            KTypeSort.quicksort(this.buffer, this.head, this.tail);
        }
    }

    ////////////////////////////

    /**
     * In-place sort the whole dequeue
     * using a #if ($TemplateOptions.KTypeGeneric) <code>Comparator</code> #else <code>KTypeComparator<? super KType></code> #end
     * <p><b>
     * This routine uses Dual-pivot Quicksort, from [Yaroslavskiy 2009] #if ($TemplateOptions.KTypeGeneric), so is NOT stable. #end
     * </b></p>
     */
    public void sort(
            /*! #if ($TemplateOptions.KTypeGeneric) !*/
            final Comparator<? super KType>
            /*! #else
            KTypeComparator<? super KType>
            #end !*/
            comp)
    {
        if (size() > 1)
        {
            compactBeforeSorting();
            KTypeSort.quicksort(this.buffer, this.head, this.tail, comp);
        }
    }

    /**
     * KTypeIndexedContainer methods
     */

    /**
     * {@inheritDoc}
     */
    @Override
    public void add(final KType e1) {
        addLast(e1);
    }

    /**
     * Beware: This operation is not supported.
     */
    @Override
    public void insert(final int index, final KType e1) {
        throw new UnsupportedOperationException("insert(final int index, final KType e1) operation is not supported on KTypeArrayDeque");
    }

    /**
     * {@inheritDoc}
     * The position is relative to the head,
     * i.e w.r.t the {@link KTypeIndexedContainer}, index 0 is the head of the queue, size() - 1 is the last element position.
     */
    @Override
    public KType set(final int index, final KType e1) {

        assert (index >= 0 && index < size()) : "Index " + index + " out of bounds [" + 0 + ", " + size() + ").";

        final int indexInBuffer = indexToBufferPosition(index);

        final KType previous = this.buffer[indexInBuffer];

        this.buffer[indexInBuffer] = e1;

        return previous;
    }

    /**
     * {@inheritDoc}
     * The position is relative to the head,
     * i.e w.r.t the {@link KTypeIndexedContainer}, index 0 is the head of the queue, size() - 1 is the last element position.
     */
    @Override
    public KType get(final int index) {

        assert (index >= 0 && index < size()) : "Index " + index + " out of bounds [" + 0 + ", " + size() + ").";

        return this.buffer[indexToBufferPosition(index)];
    }

    /**
     * {@inheritDoc}
     * The position is relative to the head,
     * i.e w.r.t the {@link KTypeIndexedContainer}, index 0 is the head of the queue, size() - 1 is the last element position.
     */
    @Override
    public KType remove(final int index) {

        assert (index >= 0 && index < size()) : "Index " + index + " out of bounds [" + 0 + ", " + size() + ").";

        final int indexInBuffer = indexToBufferPosition(index);

        final KType previous = this.buffer[indexInBuffer];

        removeAtBufferIndex(indexInBuffer);

        return previous;
    }

    /**
     * Beware: This operation is not supported.
     */
    @Override
    public void removeRange(final int fromIndex, final int toIndex) {

        throw new UnsupportedOperationException("removeRange(final int fromIndex, final int toIndex) operation is not supported on KTypeArrayDeque");
    }

    /**
     * convert the internal {@link #buffer} index to equivalent {@link #KTypeIndexedContainer}
     * position.
     * @param bufferIndex
     * @return
     */
    private int bufferIndexToPosition(final int bufferIndex) {

        int pos = -1;

        if (bufferIndex >= 0) {

            pos = bufferIndex - this.head;

            if (pos < 0) {

                //fold it
                pos += this.buffer.length;
            }
        }

        return pos;
    }

    /*! #if ($TemplateOptions.inlineKType("indexToBufferPosition",
    "(index)", "(index + this.head < this.buffer.length) ? index + this.head : index + this.head - this.buffer.length")) !*/
    /**
     * Convert the {@link #KTypeIndexedContainer}
     * index to the internal position in buffer{@link #buffer}.
     * (actual method is inlined in generated code)
     * @param index
     * @return
     */
    private int indexToBufferPosition(final int index) {

        assert (index >= 0 && index < size()) : "Index " + index + " out of bounds [" + 0 + ", " + size() + ").";

        //yes, this can overflow. I don't care for now.
        int bufferPos = index + this.head;

        if (bufferPos >= this.buffer.length) {

            //fold it
            bufferPos -= this.buffer.length;
        }

        return bufferPos;
    }

    /*! #end !*/

    /*! #if ($TemplateOptions.inlineKType("KTypeArrayDeque.oneLeft",
     "(index, modulus)", "(index >= 1) ? index - 1 : modulus - 1")) !*/
    /**
     * Move one index to the left, wrapping around buffer of size modulus.
     * (actual method is inlined in generated code)
     */
    private static int oneLeft(final int index, final int modulus)
    {
        return (index >= 1) ? index - 1 : modulus - 1;
    }

    /*! #end !*/

    /*! #if ($TemplateOptions.inlineKType("KTypeArrayDeque.oneRight","(index, modulus)",
       "(index + 1 == modulus) ? 0 : index + 1")) !*/
    /**
     * Move one index to the right, wrapping around buffer of size modulus
     * (actual method is inlined in generated code)
     */
    private static int oneRight(final int index, final int modulus)
    {
        return (index + 1 == modulus) ? 0 : index + 1;
    }
    /*! #end !*/

}
