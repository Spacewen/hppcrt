package com.carrotsearch.hppc;

import java.util.*;

import com.carrotsearch.hppc.cursors.*;
import com.carrotsearch.hppc.predicates.*;
import com.carrotsearch.hppc.procedures.*;
import com.carrotsearch.hppc.sorting.*;

import static com.carrotsearch.hppc.Internals.*;

/**
 * An array-backed deque (double-ended queue)  of KTypes. A single array is used to store and
 * manipulate all elements. Reallocations are governed by a {@link ArraySizingStrategy}
 * and may be expensive if they move around really large chunks of memory.
 *
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
extends AbstractKTypeCollection<KType> implements KTypeDeque<KType>, Cloneable
{
    /**
     * Default capacity if no other capacity is given in the constructor.
     */
    public final static int DEFAULT_CAPACITY = 5;

    /**
     * Internal array for storing elements.
     * 
     * #if ($TemplateOptions.KTypeGeneric)
     * <p>The actual value in this field is always an instance of <code>Object[]</code>,
     * regardless of the generic type used. The JDK is inconsistent here too --
     * {@link ArrayList} declares internal <code>Object[]</code> buffer, but
     * {@link ArrayDeque} declares an array of generic type objects like we do. The
     * tradeoff is probably minimal, but you should be aware of additional casts generated
     * by <code>javac</code> when <code>buffer</code> is directly accessed - these casts
     * may result in exceptions at runtime. A workaround is to cast directly to
     * <code>Object[]</code> before accessing the buffer's elements.#end
     * <p>
     * Direct deque iteration from head to tail: iterate buffer[i % buffer.length] for i in [this.head; this.head + size()[
     * </p>
     */
    public KType [] buffer;

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
        this(DEFAULT_CAPACITY);
    }

    /**
     * Create with default sizing strategy and the given initial capacity.
     * 
     * @see BoundedProportionalArraySizingStrategy
     */
    public KTypeArrayDeque(int initialCapacity)
    {
        this(initialCapacity, new BoundedProportionalArraySizingStrategy());
    }

    /**
     * Create with a custom buffer resizing strategy.
     */
    public KTypeArrayDeque(int initialCapacity, ArraySizingStrategy resizer)
    {
        assert initialCapacity >= 0 : "initialCapacity must be >= 0: " + initialCapacity;
        assert resizer != null;

        this.resizer = resizer;
        initialCapacity = resizer.round(initialCapacity);
        buffer = Intrinsics.newKTypeArray(initialCapacity);

        this.valueIteratorPool = new IteratorPool<KTypeCursor<KType>, ValueIterator>(
                new ObjectFactory<ValueIterator>() {

                    @Override
                    public ValueIterator create() {

                        return new ValueIterator();
                    }

                    @Override
                    public void initialize(ValueIterator obj) {

                        obj.cursor.index = Intrinsics.oneLeft(KTypeArrayDeque.this.head, KTypeArrayDeque.this.buffer.length);
                        obj.remaining = KTypeArrayDeque.this.size();
                    }
                });

        this.descendingValueIteratorPool = new IteratorPool<KTypeCursor<KType>, DescendingValueIterator>(
                new ObjectFactory<DescendingValueIterator>() {

                    @Override
                    public DescendingValueIterator create() {

                        return new DescendingValueIterator();
                    }

                    @Override
                    public void initialize(DescendingValueIterator obj) {

                        obj.cursor.index = KTypeArrayDeque.this.tail;
                        obj.remaining = KTypeArrayDeque.this.size();
                    }
                });
    }

    /**
     * Creates a new deque from elements of another container, appending them
     * at the end of this deque.
     */
    public KTypeArrayDeque(KTypeContainer<? extends KType> container)
    {
        this(container.size());
        addLast(container);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addFirst(KType e1)
    {
        int h = Intrinsics.oneLeft(head, buffer.length);
        if (h == tail)
        {
            ensureBufferSpace(1);
            h = Intrinsics.oneLeft(head, buffer.length);
        }
        buffer[head = h] = e1;
    }

    /**
     * Vararg-signature method for adding elements at the front of this deque.
     * 
     * <p><b>This method is handy, but costly if used in tight loops (anonymous
     * array passing)</b></p>
     */
    public void addFirst(KType... elements)
    {
        ensureBufferSpace(elements.length);

        // For now, naive loop.
        for (int i = 0; i < elements.length; i++)
            addFirst(elements[i]);
    }

    /**
     * Inserts all elements from the given container to the front of this deque.
     * 
     * @return Returns the number of elements actually added as a result of this
     * call.
     */
    public int addFirst(KTypeContainer<? extends KType> container)
    {
        int size = container.size();
        ensureBufferSpace(size);

        for (KTypeCursor<? extends KType> cursor : container)
        {
            addFirst(cursor.value);
        }

        return size;
    }

    /**
     * Inserts all elements from the given iterable to the front of this deque.
     * 
     * @return Returns the number of elements actually added as a result of this call.
     */
    public int addFirst(Iterable<? extends KTypeCursor<? extends KType>> iterable)
    {
        int size = 0;
        for (KTypeCursor<? extends KType> cursor : iterable)
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
    public void addLast(KType e1)
    {
        int t = Intrinsics.oneRight(tail, buffer.length);
        if (head == t)
        {
            ensureBufferSpace(1);
            t = Intrinsics.oneRight(tail, buffer.length);
        }
        buffer[tail] = e1;
        tail = t;
    }

    /**
     * Vararg-signature method for adding elements at the end of this deque.
     * 
     * <p><b>This method is handy, but costly if used in tight loops (anonymous
     * array passing)</b></p>
     */
    public void addLast(KType... elements)
    {
        ensureBufferSpace(1);

        // For now, naive loop.
        for (int i = 0; i < elements.length; i++)
            addLast(elements[i]);
    }

    /**
     * Inserts all elements from the given container to the end of this deque.
     * 
     * @return Returns the number of elements actually added as a result of this
     * call.
     */
    public int addLast(KTypeContainer<? extends KType> container)
    {
        int size = container.size();
        ensureBufferSpace(size);

        for (KTypeCursor<? extends KType> cursor : container)
        {
            addLast(cursor.value);
        }

        return size;
    }

    /**
     * Inserts all elements from the given iterable to the end of this deque.
     * 
     * @return Returns the number of elements actually added as a result of this call.
     */
    public int addLast(Iterable<? extends KTypeCursor<? extends KType>> iterable)
    {
        int size = 0;
        for (KTypeCursor<? extends KType> cursor : iterable)
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

        final KType result = buffer[head];
        /*! #if ($TemplateOptions.KTypeGeneric) !*/
        buffer[head] = Intrinsics.<KType>defaultKTypeValue();
        /*! #end !*/
        head = Intrinsics.oneRight(head, buffer.length);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public KType removeLast()
    {
        assert size() > 0 : "The deque is empty.";

        tail = Intrinsics.oneLeft(tail, buffer.length);
        final KType result = buffer[tail];
        /*! #if ($TemplateOptions.KTypeGeneric) !*/
        buffer[tail] = Intrinsics.<KType>defaultKTypeValue();
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

        return buffer[head];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public KType getLast()
    {
        assert size() > 0 : "The deque is empty.";

        return buffer[Intrinsics.oneLeft(tail, buffer.length)];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int removeFirstOccurrence(KType e1)
    {
        final int index = bufferIndexOf(e1);
        if (index >= 0) removeAtBufferIndex(index);
        return index;
    }

    /**
     * Return the index of the first (counting from head) element equal to
     * <code>e1</code>. The index points to the {@link #buffer} array.
     * 
     * @param e1 The element to look for.
     * @return Returns the index of the first element equal to <code>e1</code>
     * or <code>-1</code> if not found.
     */
    public int bufferIndexOf(KType e1)
    {
        final int last = tail;
        final int bufLen = buffer.length;
        for (int i = head; i != last; i = Intrinsics.oneRight(i, bufLen))
        {
            if (Intrinsics.equalsKType(e1, buffer[i]))
                return i;
        }

        return -1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int removeLastOccurrence(KType e1)
    {
        final int index = lastBufferIndexOf(e1);
        if (index >= 0) removeAtBufferIndex(index);
        return index;
    }

    /**
     * Return the index of the last (counting from tail) element equal to
     * <code>e1</code>. The index points to the {@link #buffer} array.
     * 
     * @param e1 The element to look for.
     * @return Returns the index of the first element equal to <code>e1</code>
     * or <code>-1</code> if not found.
     */
    public int lastBufferIndexOf(KType e1)
    {
        final int bufLen = buffer.length;
        final int last = Intrinsics.oneLeft(head, bufLen);
        for (int i = Intrinsics.oneLeft(tail, bufLen); i != last; i = Intrinsics.oneLeft(i, bufLen))
        {
            if (Intrinsics.equalsKType(e1, buffer[i]))
                return i;
        }

        return -1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int removeAllOccurrences(KType e1)
    {
        int removed = 0;
        final int last = tail;
        final int bufLen = buffer.length;
        int from, to;
        for (from = to = head; from != last; from = Intrinsics.oneRight(from, bufLen))
        {
            if (Intrinsics.equalsKType(e1, buffer[from]))
            {
                /*! #if ($TemplateOptions.KTypeGeneric) !*/
                buffer[from] = Intrinsics.<KType>defaultKTypeValue();
                /*! #end !*/
                removed++;
                continue;
            }

            if (to != from)
            {
                buffer[to] = buffer[from];
                /*! #if ($TemplateOptions.KTypeGeneric) !*/
                buffer[from] = Intrinsics.<KType>defaultKTypeValue();
                /*! #end !*/
            }

            to = Intrinsics.oneRight(to, bufLen);
        }

        tail = to;
        return removed;
    }

    /**
     * Removes the element at <code>index</code> in the internal
     * {#link {@link #buffer}} array, returning its value.
     * 
     * @param index Index of the element to remove. The index must be located between
     * {@link #head} and {@link #tail} in modulo {@link #buffer} arithmetic.
     */
    public void removeAtBufferIndex(int index)
    {
        assert (head <= tail
                ? index >= head && index < tail
                : index >= head || index < tail) : "Index out of range (head="
                + head + ", tail=" + tail + ", index=" + index + ").";

                // Cache fields in locals (hopefully moved to registers).
                final KType [] b = this.buffer;
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
                    b[head] = Intrinsics.<KType>defaultKTypeValue();
                    /*! #end !*/
                    this.head = Intrinsics.oneRight(head, bufLen);
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
                    b[tail] = Intrinsics.<KType>defaultKTypeValue();
                    /*! #end !*/
                    this.tail = Intrinsics.oneLeft(tail, bufLen);
                }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int size()
    {
        if (head <= tail)
        {
            return tail - head;
        }

        return (tail - head + buffer.length);
    }

    /**
     * {@inheritDoc}
     * 
     * <p>The internal array buffers are not released as a result of this call.</p>
     * 
     * @see #release()
     */
    @Override
    public void clear()
    {
        /*! #if ($TemplateOptions.KTypeGeneric) !*/
        if (head < tail)
        {
            Internals.blankObjectArray(buffer, head, tail);
        }
        else
        {
            Internals.blankObjectArray(buffer, 0, tail);
            Internals.blankObjectArray(buffer, head, buffer.length);
        }
        /*! #end !*/

        this.head = tail = 0;
    }

    /**
     * Compact the internal buffer to prepare sorting
     */
    private void compactBeforeSorting()
    {
        if (head > tail)
        {
            int size = size();
            int hole = head - tail;

            //pack the separated chunk to the beginning of the buffer
            System.arraycopy(buffer, head, buffer, tail, buffer.length - head);

            //reset of the positions
            head = 0;
            tail = size;

            //for GC sake, reset hole elements now at the end of buffer
            /*! #if ($TemplateOptions.KTypeGeneric) !*/
            Internals.blankObjectArray(buffer, tail, tail + hole);
            /*! #end !*/
        }
    }

    /**
     * Release internal buffers of this deque and reallocate the smallest buffer possible.
     */
    public void release()
    {
        this.head = tail = 0;
        buffer = Intrinsics.newKTypeArray(resizer.round(DEFAULT_CAPACITY));
    }

    /**
     * Ensures the internal buffer has enough free slots to store
     * <code>expectedAdditions</code>. Increases internal buffer size if needed.
     */
    protected void ensureBufferSpace(int expectedAdditions)
    {
        final int bufferLen = (buffer == null ? 0 : buffer.length);
        final int elementsCount = size();
        // +1 because there is always one empty slot in a deque.
        if (elementsCount >= bufferLen - expectedAdditions - 1)
        {
            final int newSize = resizer.grow(bufferLen, elementsCount, expectedAdditions + 1);
            assert newSize >= (elementsCount + expectedAdditions + 1) : "Resizer failed to" +
                    " return sensible new size: " + newSize + " <= "
                    + (elementsCount + expectedAdditions);

            final KType [] newBuffer = Intrinsics.<KType[]>newKTypeArray(newSize);
            if (bufferLen > 0)
            {
                toArray(newBuffer);
                tail = elementsCount;
                head = 0;
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
    public KType [] toArray(KType [] target)
    {
        assert target.length >= size() : "Target array must be >= " + size();

        if (head < tail)
        {
            // The contents is not wrapped around. Just copy.
            System.arraycopy(buffer, head, target, 0, size());
        }
        else if (head > tail)
        {
            // The contents is split. Merge elements from the following indexes:
            // [head...buffer.length - 1][0, tail - 1]
            final int rightCount = buffer.length - head;
            System.arraycopy(buffer, head, target, 0, rightCount);
            System.arraycopy(buffer,    0, target, rightCount, tail);
        }

        return target;
    }

    /**
     * Clone this object. The returned clone will reuse the same array resizing strategy.
     */
    @Override
    public KTypeArrayDeque<KType> clone()
    {
        /* #if ($TemplateOptions.KTypeGeneric) */
        @SuppressWarnings("unchecked")
        /* #end */
        //real constructor call
        final KTypeArrayDeque<KType> cloned = new KTypeArrayDeque<KType>(this.buffer.length, this.resizer);

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
            cursor = new KTypeCursor<KType>();
            cursor.index = Intrinsics.oneLeft(KTypeArrayDeque.this.head, KTypeArrayDeque.this.buffer.length);
            this.remaining = KTypeArrayDeque.this.size();
        }

        @Override
        protected KTypeCursor<KType> fetch()
        {
            if (remaining == 0)
                return done();

            remaining--;
            cursor.value = buffer[cursor.index = Intrinsics.oneRight(cursor.index, buffer.length)];
            return cursor;
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
            cursor = new KTypeCursor<KType>();
            cursor.index = KTypeArrayDeque.this.tail;
            this.remaining = KTypeArrayDeque.this.size();
        }

        @Override
        protected KTypeCursor<KType> fetch()
        {
            if (remaining == 0)
                return done();

            remaining--;
            cursor.value = buffer[cursor.index = Intrinsics.oneLeft(cursor.index, buffer.length)];
            return cursor;
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
    public DescendingValueIterator descendingIterator()
    {
        //return new DescendingValueIterator();
        return this.descendingValueIteratorPool.borrow();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends KTypeProcedure<? super KType>> T forEach(T procedure)
    {
        forEach(procedure, head, tail);
        return procedure;
    }

    /**
     * Applies <code>procedure</code> to a slice of the deque,
     * <code>fromIndex</code>, inclusive, to <code>toIndex</code>,
     * exclusive.
     */
    private void forEach(KTypeProcedure<? super KType> procedure, int fromIndex, final int toIndex)
    {
        final KType [] buffer = this.buffer;
        for (int i = fromIndex; i != toIndex; i = Intrinsics.oneRight(i, buffer.length))
        {
            procedure.apply(buffer[i]);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends KTypePredicate<? super KType>> T forEach(T predicate)
    {
        int fromIndex = head;
        int toIndex = tail;

        final KType [] buffer = this.buffer;
        for (int i = fromIndex; i != toIndex; i = Intrinsics.oneRight(i, buffer.length))
        {
            if (!predicate.apply(buffer[i]))
                break;
        }

        return predicate;
    }

    /**
     * Applies <code>procedure</code> to all elements of this deque, tail to head.
     */
    @Override
    public <T extends KTypeProcedure<? super KType>> T descendingForEach(T procedure)
    {
        descendingForEach(procedure, head, tail);
        return procedure;
    }

    /**
     * Applies <code>procedure</code> to a slice of the deque,
     * <code>toIndex</code>, exclusive, down to <code>fromIndex</code>, inclusive.
     */
    private void descendingForEach(KTypeProcedure<? super KType> procedure,
            int fromIndex, final int toIndex)
    {
        if (fromIndex == toIndex)
            return;

        final KType [] buffer = this.buffer;
        int i = toIndex;
        do
        {
            i = Intrinsics.oneLeft(i, buffer.length);
            procedure.apply(buffer[i]);
        } while (i != fromIndex);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends KTypePredicate<? super KType>> T descendingForEach(T predicate)
    {
        descendingForEach(predicate, head, tail);
        return predicate;
    }

    /**
     * Applies <code>predicate</code> to a slice of the deque,
     * <code>toIndex</code>, exclusive, down to <code>fromIndex</code>, inclusive
     * or until the predicate returns <code>false</code>.
     */
    private void descendingForEach(KTypePredicate<? super KType> predicate,
            int fromIndex, final int toIndex)
    {
        if (fromIndex == toIndex)
            return;

        final KType [] buffer = this.buffer;
        int i = toIndex;
        do
        {
            i = Intrinsics.oneLeft(i, buffer.length);
            if (!predicate.apply(buffer[i]))
                break;
        } while (i != fromIndex);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int removeAll(KTypePredicate<? super KType> predicate)
    {
        int removed = 0;
        final int last = tail;
        final int bufLen = buffer.length;
        int from, to;
        from = to = head;
        try
        {
            for (from = to = head; from != last; from = Intrinsics.oneRight(from, bufLen))
            {
                if (predicate.apply(buffer[from]))
                {
                    /*! #if ($TemplateOptions.KTypeGeneric) !*/
                    buffer[from] = Intrinsics.<KType>defaultKTypeValue();
                    /*! #end !*/
                    removed++;
                    continue;
                }

                if (to != from)
                {
                    buffer[to] = buffer[from];
                    /*! #if ($TemplateOptions.KTypeGeneric) !*/
                    buffer[from] = Intrinsics.<KType>defaultKTypeValue();
                    /*! #end !*/
                }

                to = Intrinsics.oneRight(to, bufLen);
            }
        }
        finally
        {
            // Keep the deque in consistent state even if the predicate throws an exception.
            for (; from != last; from = Intrinsics.oneRight(from, bufLen))
            {
                if (to != from)
                {
                    buffer[to] = buffer[from];
                    /*! #if ($TemplateOptions.KTypeGeneric) !*/
                    buffer[from] = Intrinsics.<KType>defaultKTypeValue();
                    /*! #end !*/
                }

                to = Intrinsics.oneRight(to, bufLen);
            }
            tail = to;
        }

        return removed;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean contains(KType e)
    {
        int fromIndex = head;
        int toIndex = tail;

        final KType [] buffer = this.buffer;
        for (int i = fromIndex; i != toIndex; i = Intrinsics.oneRight(i, buffer.length))
        {
            if (Intrinsics.equalsKType(e, buffer[i]))
                return true;
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
        int fromIndex = head;
        int toIndex = tail;

        final KType [] buffer = this.buffer;
        for (int i = fromIndex; i != toIndex; i = Intrinsics.oneRight(i, buffer.length))
        {
            h = 31 * h + rehash(this.buffer[i]);
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
    public boolean equals(Object obj)
    {
        if (obj != null)
        {
            if (obj instanceof KTypeDeque<?>)
            {
                KTypeDeque<Object> other = (KTypeDeque<Object>) obj;

                if (other.size() == this.size())
                {
                    int fromIndex = head;
                    final KType [] buffer = this.buffer;
                    int i = fromIndex;

                    //request a pooled iterator
                    /*! #if ($TemplateOptions.KTypeGeneric) !*/
                    final Iterator<KTypeCursor<Object>> it = other.iterator();
                    /*! #else
                    final Iterator<KTypeCursor> it = other.iterator();
                    #end !*/

                    while (it.hasNext())
                    {
                        /*! #if ($TemplateOptions.KTypeGeneric) !*/
                        final KTypeCursor<Object> c = it.next();
                        /*! #else
                        final KTypeCursor c = it.next();
                        #end !*/
                        if (!Intrinsics.equalsKType(c.value, buffer[i]))
                        {
                            //if iterator was pooled, recycled it
                            if (it instanceof AbstractIterator<?>)
                            {
                                ((AbstractIterator<?>) it).release();
                            }

                            return false;
                        }
                        i = Intrinsics.oneRight(i, buffer.length);
                    }

                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns a new object of this class with no need to declare generic type (shortcut
     * instead of using a constructor).
     */
    public static /* #if ($TemplateOptions.KTypeGeneric) */ <KType> /* #end */
    KTypeArrayDeque<KType> newInstance()
    {
        return new KTypeArrayDeque<KType>();
    }

    /**
     * Returns a new object of this class with no need to declare generic type (shortcut
     * instead of using a constructor).
     */
    public static /* #if ($TemplateOptions.KTypeGeneric) */ <KType> /* #end */
    KTypeArrayDeque<KType> newInstanceWithCapacity(int initialCapacity)
    {
        return new KTypeArrayDeque<KType>(initialCapacity);
    }

    /**
     * Create a new deque by pushing a variable number of arguments to the end of it.
     */
    public static /* #if ($TemplateOptions.KTypeGeneric) */ <KType> /* #end */
    KTypeArrayDeque<KType> from(KType... elements)
    {
        final KTypeArrayDeque<KType> coll = new KTypeArrayDeque<KType>(elements.length);
        coll.addLast(elements);
        return coll;
    }

    /**
     * Create a new deque by pushing a variable number of arguments to the end of it.
     */
    public static /* #if ($TemplateOptions.KTypeGeneric) */ <KType> /* #end */
    KTypeArrayDeque<KType> from(KTypeArrayDeque<KType> container)
    {
        return new KTypeArrayDeque<KType>(container);
    }

    /**
     * Sort the whole deque by natural ordering (smaller first)
     * <p><b>
     * This routine uses Dual-pivot Quicksort, from [Yaroslavskiy 2009].
     * </b></p>
     * @param beginIndex
     * @param endIndex
     */
    /*! #if ($TemplateOptions.KTypePrimitive)
    public void sort()
    {
        if (size() > 1)
        {
            compactBeforeSorting();
            KTypeSort.quicksort(buffer, head, tail);
        }
    }
    #end !*/

    ////////////////////////////

    /**
     * Sort by  dual-pivot quicksort an entire deque
     * using a #if ($TemplateOptions.KTypeGeneric) <code>Comparator</code> #else <code>KTypeComparator<KType></code> #end
     * <p><b>
     * This routine uses Dual-pivot Quicksort, from [Yaroslavskiy 2009] #if ($TemplateOptions.KTypeGeneric), so is NOT stable. #end
     * </b></p>
     */
    public void sort(
            /*! #if ($TemplateOptions.KTypeGeneric) !*/
            Comparator<KType>
            /*! #else
            KTypeComparator<KType>
            #end !*/
            comp)
    {
        if (size() > 1)
        {
            compactBeforeSorting();
            KTypeSort.quicksort(buffer, head, tail, comp);
        }
    }
}
