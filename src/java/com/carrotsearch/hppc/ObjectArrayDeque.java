package com.carrotsearch.hppc;

import java.util.*;

/**
 * An array-backed deque (doubly linked queue) of KTypes. A single array is used to store and 
 * manipulate all elements. Reallocations are governed by a {@link ArraySizingStrategy}
 * and may be expensive if they move around really large chunks of memory.
 *
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
 */
public class ObjectArrayDeque<KType> implements Iterable<ObjectCursor<KType>>
{
    /**
     * Default capacity if no other capacity is given in the constructor.
     */
    public final static int DEFAULT_CAPACITY = 5;

    /* removeIf:primitive */
    /*
     * The actual value in this field is always <code>Object[]</code>, regardless of the
     * generic type used. The JDK is inconsistent here too -- {@link ArrayList} declares
     * internal <code>Object[]</code> buffer, but {@link ArrayDeque} declares an array of
     * generic type objects like we do. The tradeoff is probably minimal, but you should
     * be aware of additional casts generated by <code>javac</code>
     * when <code>buffer</code> is directly accessed - these casts may result in exceptions
     * at runtime. A workaround is to cast directly to <code>Object[]</code> before
     * accessing the buffer's elements.
     */
    /* end:removeIf */

    /**
     * Internal array for storing elements.
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
     * Create with default sizing strategy and initial capacity for storing 
     * {@value #DEFAULT_CAPACITY} elements.
     * 
     * @see BoundedProportionalArraySizingStrategy
     */
    public ObjectArrayDeque()
    {
        this(DEFAULT_CAPACITY);
    }

    /**
     * Create with default sizing strategy and the given initial capacity.
     * 
     * @see BoundedProportionalArraySizingStrategy
     */
    public ObjectArrayDeque(int initialCapacity)
    {
        this(initialCapacity, new BoundedProportionalArraySizingStrategy());
    }

    /**
     * Create with a custom buffer resizing strategy.
     */
    public ObjectArrayDeque(int initialCapacity, ArraySizingStrategy resizer)
    {
        assert initialCapacity >= 0 : "initialCapacity must be >= 0: " + initialCapacity;
        assert resizer != null;

        this.resizer = resizer;
        initialCapacity = resizer.round(initialCapacity);
        buffer = Intrinsics.newKTypeArray(initialCapacity);
    }

    /**
     * Inserts the specified element at the front of this deque.
     *
     * @param e1 the element to add
     */
    public void addFirst(KType e1)
    {
        int h = oneLeft(head, buffer.length);
        if (h == tail)
        {
            ensureBufferSpace(1);
            h = oneLeft(head, buffer.length);
        }
        buffer[head = h] = e1;
    }

    /**
     * Vararg-signature method for adding elements at the front of this deque.
     * 
     * <p><b>This method is handy, but costly if used in tight loops (anonymous 
     * array passing)</b></p>
     */
    public void addFirstv(KType... elements)
    {
        // For now, naive loop.
        for (int i = 0; i < elements.length; i++)
            addFirst(elements[i]);
    }

    /**
     * Inserts all elements from the given cursor to the front of this deque.
     * 
     * @param iterator An iterator returning a cursor over a collection of KType elements. 
     * @return Returns the number of elements actually added as a result of this
     * call.
     */
    public final int addAllFirst(Iterator<? extends ObjectCursor<? extends KType>> iterator)
    {
        int count = 0;
        while (iterator.hasNext())
        {
            addFirst(iterator.next().value);
            count++;
        }

        return count;
    }

    /**
     * Inserts all elements from the given iterable to the front of this deque.
     * 
     * @see #addAllFirst(Iterator)
     */
    public final int addAllFirst(Iterable<? extends ObjectCursor<? extends KType>> iterable)
    {
        return addAllFirst(iterable.iterator());
    }

    /**
     * Inserts the specified element at the end of this deque.
     *
     * @param e1 the element to add
     */
    public void addLast(KType e1)
    {
        int t = oneRight(tail, buffer.length);
        if (head == t)
        {
            ensureBufferSpace(1);
            t = oneRight(tail, buffer.length);
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
    public void addLastv(KType... elements)
    {
        // For now, naive loop.
        for (int i = 0; i < elements.length; i++)
            addLast(elements[i]);
    }

    /**
     * Inserts all elements from the given cursor to the end of this deque.
     * 
     * @param cursor An iterator returning a cursor over a collection of KType elements. 
     * @return Returns the number of elements actually added as a result of this
     * call.
     */
    public final int addAllLast(Iterator<? extends ObjectCursor<? extends KType>> cursor)
    {
        int count = 0;
        while (cursor.hasNext())
        {
            addLast(cursor.next().value);
            count++;
        }

        return count;
    }

    /**
     * Inserts all elements from the given iterable to the end of this deque.
     * 
     * @see #addAllLast(Iterator)
     */
    public final int addAllLast(Iterable<? extends ObjectCursor<? extends KType>> iterable)
    {
        return addAllLast(iterable.iterator());
    }

    /**
     * Retrieves and removes the first element of this deque.
     *
     * @return the head element of this deque.
     * @throws AssertionError if this deque is empty and assertions are enabled.
     */
    public KType removeFirst()
    {
        assert size() > 0 : "The deque is empty.";

        final KType result = buffer[head];
        buffer[head] = Intrinsics.<KType>defaultKTypeValue();
        head = oneRight(head, buffer.length); 
        return result;
    }

    /**
     * Retrieves and removes the last element of this deque.
     *
     * @return the tail of this deque.
     * @throws AssertionError if this deque is empty and assertions are enabled.
     */
    public KType removeLast()
    {
        assert size() > 0 : "The deque is empty.";

        tail = oneLeft(tail, buffer.length); 
        final KType result = buffer[tail];
        buffer[tail] = Intrinsics.<KType>defaultKTypeValue();
        return result;
    }

    /**
     * Retrieves, but does not remove, the first element of this deque.
     *
     * @return the head of this deque.
     * @throws AssertionError if this deque is empty and assertions are enabled.
     */
    public KType getFirst()
    {
        assert size() > 0 : "The deque is empty.";

        return buffer[head];
    }

    /**
     * Retrieves, but does not remove, the last element of this deque.
     *
     * @return the tail of this deque.
     * @throws AssertionError if this deque is empty and assertions are enabled.
     */
    public KType getLast()
    {
        assert size() > 0 : "The deque is empty.";

        return buffer[oneLeft(tail, buffer.length)];
    }

    /**
     * Removes the first occurrence of the specified element from this deque.
     * If the deque does not contain the element, it is unchanged.
     * 
     * <p>Returns <tt>true</tt> if this deque contained the specified element
     * (or equivalently, if this deque changed as a result of the call).
     *
     * @param e1 element to be removed from this deque, if present
     * @return <tt>true</tt> if an element was removed as a result of this call
     */
    public boolean removeFirstOccurrence(KType e1)
    {
        final int index = bufferIndexOf(e1);
        if (index >= 0) removeAtBufferIndex(index);
        return index >= 0;
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
        for (int i = head; i != last; i = oneRight(i, bufLen))
        {
            if (Intrinsics.equals(e1, buffer[i]))
                return i;
        }

        return -1;
    }

    /**
     * Removes the last occurrence of the specified element from this deque.
     * If the deque does not contain the element, it is unchanged.
     * 
     * <p>Returns <tt>true</tt> if this deque contained the specified element
     * (or equivalently, if this deque changed as a result of the call).
     *
     * @param e1 element to be removed from this deque, if present
     * @return <tt>true</tt> if an element was removed as a result of this call
     */
    public boolean removeLastOccurrence(KType e1)
    {
        final int index = lastBufferIndexOf(e1);
        if (index >= 0) removeAtBufferIndex(index);
        return index >= 0;
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
        final int last = oneLeft(head, bufLen);
        for (int i = oneLeft(tail, bufLen); i != last; i = oneLeft(i, bufLen))
        {
            if (Intrinsics.equals(e1, buffer[i]))
                return i;
        }

        return -1;
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
            b[head] = Intrinsics.<KType>defaultKTypeValue();
            this.head = oneRight(head, bufLen);
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
            b[tail] = Intrinsics.<KType>defaultKTypeValue();
            this.tail = oneLeft(tail, bufLen);
        }
    }
    
    /**
     * @return Return <code>true</code> if this deque has no elements.
     */
    public boolean isEmpty()
    {
        return size() == 0;
    }

    /**
     * @return the number of elements in this deque
     */
    public int size()
    {
        if (head <= tail)
            return tail - head;
        else
            return (tail - head + buffer.length);
    }

    /**
     * Clear the deque without releasing internal buffers. The buffer's content is
     * set to default values for the deque's type.
     */
    public void clear()
    {
        if (head < tail)
        {
            Arrays.fill(buffer, head, tail, Intrinsics.<KType>defaultKTypeValue());
        }
        else
        {
            Arrays.fill(buffer, 0, tail, Intrinsics.<KType>defaultKTypeValue());
            Arrays.fill(buffer, head, buffer.length, Intrinsics.<KType>defaultKTypeValue());
        }
        this.head = tail = 0;
    }

    /**
     * Release internal buffers of this deque and reallocate the smallest buffer possible.
     */
    public void release()
    {
        this.head = tail = 0;
        final int size = resizer.round(DEFAULT_CAPACITY);
        buffer = Intrinsics.newKTypeArray(size);
    }

    /**
     * Ensures the internal buffer has enough free slots to store
     * <code>expectedAdditions</code>. Increases internal buffer size if needed.
     */
    protected final void ensureBufferSpace(int expectedAdditions)
    {
        final int bufferLen = (buffer == null ? 0 : buffer.length);
        final int elementsCount = size();
        // +1 because there is always one empty slot in a deque.
        final int requestedMinimum = 1 + elementsCount + expectedAdditions; 
        if (requestedMinimum >= bufferLen)
        {
            final int newSize = resizer.grow(bufferLen, elementsCount, expectedAdditions);
            assert newSize >= requestedMinimum : "Resizer failed to" +
                    " return sensible new size: " + newSize + " <= " 
                    + (elementsCount + expectedAdditions);

            final KType [] newBuffer = Intrinsics.<KType[]>newKTypeArray(newSize);
            if (bufferLen > 0)
            {
                toArray(newBuffer);
                tail = elementsCount;
                head = 0;

                /* removeIf:primitiveKType */
                Arrays.fill(buffer, null); // Help the GC.
                /* end:removeIf */
            }
            this.buffer = newBuffer;
        }
    }

    /**
     * Creates a new array that will contain the elements in this deque. The content of
     * the <code>target</code> array is filled from index 0 (head of the queue) to index
     * <code>size() - 1</code> (tail of the queue).
     */
    public KType [] toArray()
    {
        final int size = size();
        return toArray(Intrinsics.<KType[]>newKTypeArray(size));
    }

    /**
     * Copies elements of this deque to an array. The content of the <code>target</code>
     * array is filled from index 0 (head of the queue) to index <code>size() - 1</code>
     * (tail of the queue).
     * 
     * @param target The target array must be large enough to hold all elements.
     */
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
     * Move one index to the left, wrapping around buffer. 
     */
    final static int oneLeft(int index, int modulus)
    {
        if (index >= 1) return index - 1;
        return modulus - 1;
    }

    /**
     * Move one index to the right, wrapping around buffer. 
     */
    final static int oneRight(int index, int modulus)
    {
        if (index + 1 == modulus) return 0;
        return index + 1;
    }

    /**
     * An iterator implementation for {@link ObjectArrayDeque#iterator}.
     */
    private final class ValueIterator implements Iterator<ObjectCursor<KType>>
    {
        private final ObjectCursor<KType> cursor;
        private final int last;

        public ValueIterator()
        {
            cursor = new ObjectCursor<KType>();
            cursor.index = head;
            this.last = tail;
        }

        public boolean hasNext()
        {
            return cursor.index != last;
        }

        public ObjectCursor<KType> next()
        {
            if (cursor.index == tail)
                throw new NoSuchElementException();

            final int i = cursor.index;
            cursor.value = buffer[i];
            cursor.index = oneRight(i, buffer.length);
            return cursor;
        }

        public void remove()
        {
            /* 
             * It will be much more efficient to have a removal using a closure-like 
             * structure (then we can simply move elements to proper slots as we iterate
             * over the array as in #removeAll). 
             */
            throw new UnsupportedOperationException();
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
     * 
     * @see #values()
     */
    public Iterator<ObjectCursor<KType>> iterator()
    {
        return new ValueIterator();
    }

    /**
     * Returns an iterable view of the values in this {@link ObjectArrayDeque}, effectively
     * an alias for <code>this</code> because {@link ObjectArrayDeque} is already
     * iterable over the stored values.
     * 
     * @see #iterator()
     */
    public Iterable<ObjectCursor<KType>> values()
    {
        return this;
    }

    /**
     * Applies <code>procedure</code> to all elements of this list. This method
     * is about twice as fast as running an iterator and nearly as fast
     * as running a code loop over the buffer content (!).
     *
     * @see "HPPC benchmarks." 
     */
    public void forEach(ObjectProcedure<? super KType> procedure)
    {
        forEach(procedure, head, tail);
    }

    /**
     * Applies <code>procedure</code> to a slice of the deque,
     * <code>fromIndex</code>, inclusive, to <code>toIndex</code>, 
     * exclusive.
     */
    private void forEach(ObjectProcedure<? super KType> procedure, int fromIndex, final int toIndex)
    {
        final KType [] buffer = this.buffer;
        for (int i = fromIndex; i != toIndex; i = oneRight(i, buffer.length))
        {
            procedure.apply(buffer[i]);
        }
    }
}
