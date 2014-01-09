package com.carrotsearch.hppc;

import java.util.*;

import com.carrotsearch.hppc.cursors.*;
import com.carrotsearch.hppc.predicates.*;
import com.carrotsearch.hppc.procedures.*;
import com.carrotsearch.hppc.sorting.*;
import static com.carrotsearch.hppc.Internals.*;

/**
 * An double-linked list of KTypes. This is an hybrid of {@link KTypeDeque} and {@link  KTypeIndexedContainer},
 * with the ability to push or remove values from either head or tail, in constant time.
 * The drawback is the double-linked list ones, i.e insert(index), remove(index) are O(N)
 * since they must traverse the collection to reach the index.
 * In addition, elements could by pushed or removed from the middle of the list without moving
 * big amounts of memory, contrary to {@link KTypeArrayList}s. Like {@link KTypeDeque}s, the double-linked list
 * can also be iterated in reverse.
 * Plus, the Iterator or reversed-iterator supports powerful methods to modify/delete/set the neighbors,
 * in replacement of the error-prone java.util.Iterator ones.
 * A compact representation is used to store and manipulate
 * all elements, without creating Objects for links. Reallocations are governed by a {@link ArraySizingStrategy}
 * and may be expensive if they move around really large chunks of memory.
 * <b>
 * Important note: DO NOT USE java.util.Iterator methods ! They are here only for enhanced-loop syntax. Use
 * the specialized methods of  {@link ValueIterator} or {@link DescendingValueIterator} instead !
 * </b>
 */
/*! ${TemplateOptions.generatedAnnotation} !*/
public class KTypeLinkedList<KType>
extends AbstractKTypeCollection<KType> implements KTypeIndexedContainer<KType>, KTypeDeque<KType>, Cloneable
{
    /**
     * Default capacity if no other capacity is given in the constructor.
     */
    public final static int DEFAULT_CAPACITY = 16;

    /**
     * Internal array for storing the list. The array may be larger than the current size
     * ({@link #size()}).
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
     * Direct list iteration: iterate buffer[i] for i in [2; size()+2[, but is out of order !
     * </p>
     */
    public KType[] buffer;

    /**
     * Represent the before / after nodes for each element of the buffer,
     * such as buffer[i] nodes are beforeAfterPointers[i] where
     * the 32 highest bits represent the unsigned before index in buffer : (beforeAfterPointers[i] & 0xFFFFFFFF00000000) >> 32
     * the 32 lowest bits represent the unsigned after index in buffer :   (beforeAfterPointers[i] & 0x00000000FFFFFFFF)
     */
    protected long[] beforeAfterPointers;

    /**
     * Respective positions of head and tail elements of the list,
     * as a position in buffer, such as
     * after head is the actual first element of the list,
     * before tail is the actual last element of the list.
     * Technically, head and tail have hardcoded positions of 0 and 1,
     * and occupy buffer[0] and buffer[1] virtually.
     * So real buffer elements starts at index 2.
     */
    protected static final int HEAD_POSITION = 0;
    protected static final int TAIL_POSITION = KTypeLinkedList.HEAD_POSITION + 1;

    /**
     * Current number of elements stored in {@link #buffer}.
     * Beware, the real number of elements is elementsCount - 2
     */
    protected int elementsCount = 2;

    /**
     * Buffer resizing strategy.
     */
    protected final ArraySizingStrategy resizer;

    /**
     * internal pool of ValueIterator (must be created in constructor)
     */
    protected final IteratorPool<KTypeCursor<KType>, ValueIterator> valueIteratorPool;

    /**
     * internal pool of DescendingValueIterator (must be created in constructor)
     */
    protected final IteratorPool<KTypeCursor<KType>, DescendingValueIterator> descendingValueIteratorPool;

    /**
     * Create with default sizing strategy and initial capacity for storing
     * {@value #DEFAULT_CAPACITY} elements.
     * 
     * @see BoundedProportionalArraySizingStrategy
     */
    public KTypeLinkedList()
    {
        this(KTypeLinkedList.DEFAULT_CAPACITY);
    }

    /**
     * Create with default sizing strategy and the given initial capacity.
     * 
     * @see BoundedProportionalArraySizingStrategy
     */
    public KTypeLinkedList(final int initialCapacity)
    {
        this(initialCapacity, new BoundedProportionalArraySizingStrategy());
    }

    /**
     * Create with a custom buffer resizing strategy.
     */
    public KTypeLinkedList(final int initialCapacity, final ArraySizingStrategy resizer)
    {
        assert initialCapacity >= 0 : "initialCapacity must be >= 0: " + initialCapacity;
        assert resizer != null;

        this.resizer = resizer;
        ensureBufferSpace(resizer.round(initialCapacity));

        //initialize
        elementsCount = 2;

        //initialize head and tail: initially, they are linked to each other.
        beforeAfterPointers[KTypeLinkedList.HEAD_POSITION] = Intrinsics.getLinkNodeValue(KTypeLinkedList.HEAD_POSITION, KTypeLinkedList.TAIL_POSITION);
        beforeAfterPointers[KTypeLinkedList.TAIL_POSITION] = Intrinsics.getLinkNodeValue(KTypeLinkedList.HEAD_POSITION, KTypeLinkedList.TAIL_POSITION);

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
                        obj.internalIndex = -1;
                        obj.cursor.index = -1;
                        obj.cursor.value = KTypeLinkedList.this.defaultValue;
                        obj.buffer = KTypeLinkedList.this.buffer;
                        obj.pointers = KTypeLinkedList.this.beforeAfterPointers;
                        obj.internalPos = KTypeLinkedList.HEAD_POSITION;
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
                        obj.internalIndex = KTypeLinkedList.this.size();
                        obj.cursor.index = KTypeLinkedList.this.size();
                        obj.cursor.value = KTypeLinkedList.this.defaultValue;
                        obj.buffer = KTypeLinkedList.this.buffer;
                        obj.pointers = KTypeLinkedList.this.beforeAfterPointers;
                        obj.internalPos = KTypeLinkedList.TAIL_POSITION;
                    }
                });
    }

    /**
     * Creates a new list from elements of another container.
     */
    public KTypeLinkedList(final KTypeContainer<? extends KType> container)
    {
        this(container.size());
        addAll(container);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void add(final KType e1)
    {
        addLast(e1);
    }

    /**
     * Appends two elements at the end of the list. To add more than two elements,
     * use <code>add</code> (vararg-version) or access the buffer directly (tight
     * loop).
     */
    public void add(final KType e1, final KType e2)
    {
        ensureBufferSpace(2);
        insertAfterPosNoCheck(e1, Intrinsics.getLinkBefore(this.beforeAfterPointers[KTypeLinkedList.TAIL_POSITION]));;
        insertAfterPosNoCheck(e2, Intrinsics.getLinkBefore(this.beforeAfterPointers[KTypeLinkedList.TAIL_POSITION]));;
    }

    public void addLast(final KType... elements)
    {
        addLast(elements, 0, elements.length);
    }

    /**
     * Add all elements from a range of given array to the list.
     */
    public void addLast(final KType[] elements, final int start, final int length)
    {
        assert length + start <= elements.length : "Length is smaller than required";

        ensureBufferSpace(length);

        for (int i = 0; i < length; i++)
        {
            insertAfterPosNoCheck(elements[start + i], Intrinsics.getLinkBefore(this.beforeAfterPointers[KTypeLinkedList.TAIL_POSITION]));
        }
    }

    /**
     * Add all elements from a range of given array to the list, equivalent to addLast()
     */
    public void add(final KType[] elements, final int start, final int length)
    {
        addLast(elements, start, length);
    }

    /**
     * Vararg-signature method for adding elements at the end of the list.
     * <p><b>This method is handy, but costly if used in tight loops (anonymous
     * array passing)</b></p>
     */
    public void add(final KType... elements)
    {
        addLast(elements, 0, elements.length);
    }

    /**
     * Adds all elements from another container, equivalent to addLast()
     */
    public int addAll(final KTypeContainer<? extends KType> container)
    {
        return addLast(container);
    }

    /**
     * Adds all elements from another iterable, equivalent to addLast()
     */
    public int addAll(final Iterable<? extends KTypeCursor<? extends KType>> iterable)
    {
        return addLast(iterable);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void insert(final int index, final KType e1)
    {
        assert (index >= 0 && index <= size()) : "Index " + index + " out of bounds [" + 0 + ", " + size() + "].";

        ensureBufferSpace(1);

        if (index == 0)
        {
            insertAfterPosNoCheck(e1, KTypeLinkedList.HEAD_POSITION);
        }
        else
        {
            insertAfterPosNoCheck(e1, gotoIndex(index - 1));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public KType get(final int index)
    {
        assert (index >= 0 && index < size()) : "Index " + index + " out of bounds [" + 0 + ", " + size() + ").";

        //get object at pos currentPos in buffer
        return buffer[gotoIndex(index)];

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public KType set(final int index, final KType e1)
    {
        assert (index >= 0 && index < size()) : "Index " + index + " out of bounds [" + 0 + ", " + size() + ").";

        //get object at pos currentPos in buffer
        final int pos = gotoIndex(index);

        //keep the previous value
        final KType elem = buffer[pos];

        //new value
        buffer[pos] = e1;

        return elem;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public KType remove(final int index)
    {
        assert (index >= 0 && index < size()) : "Index " + index + " out of bounds [" + 0 + ", " + size() + ").";

        //get object at pos currentPos in buffer
        final int currentPos = gotoIndex(index);

        final KType elem = buffer[currentPos];

        //remove
        removeAtPosNoCheck(currentPos);

        return elem;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeRange(final int fromIndex, final int toIndex)
    {
        assert (fromIndex >= 0 && fromIndex <= size()) : "Index " + fromIndex + " out of bounds [" + 0 + ", " + size() + ").";

        assert (toIndex >= 0 && toIndex <= size()) : "Index " + toIndex + " out of bounds [" + 0 + ", " + size() + "].";

        assert fromIndex <= toIndex : "fromIndex must be <= toIndex: "
                + fromIndex + ", " + toIndex;

        //goto pos
        int currentPos = gotoIndex(fromIndex);

        //start removing size elements...
        final int size = toIndex - fromIndex;
        int count = 0;

        while (count < size)
        {
            currentPos = removeAtPosNoCheck(currentPos);
            count++;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int removeFirstOccurrence(final KType e1)
    {
        final long[] pointers = this.beforeAfterPointers;
        final KType[] buffer = this.buffer;

        int currentPos = Intrinsics.getLinkAfter(pointers[KTypeLinkedList.HEAD_POSITION]);
        int count = 0;

        while (currentPos != KTypeLinkedList.TAIL_POSITION)
        {
            if (Intrinsics.equalsKType(e1, buffer[currentPos]))
            {
                removeAtPosNoCheck(currentPos);
                return count;
            }

            //increment
            currentPos = Intrinsics.getLinkAfter(pointers[currentPos]);
            count++;
        }

        return -1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int removeLastOccurrence(final KType e1)
    {
        final long[] pointers = this.beforeAfterPointers;
        final KType[] buffer = this.buffer;
        final int size = size();

        int currentPos = Intrinsics.getLinkBefore(pointers[KTypeLinkedList.TAIL_POSITION]);
        int count = 0;

        while (currentPos != KTypeLinkedList.HEAD_POSITION)
        {
            if (Intrinsics.equalsKType(e1, buffer[currentPos]))
            {
                removeAtPosNoCheck(currentPos);
                return size - count - 1;
            }

            currentPos = Intrinsics.getLinkBefore(pointers[currentPos]);
            count++;
        }

        return -1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int removeAllOccurrences(final KType e1)
    {
        final KType[] buffer = this.buffer;

        int deleted = 0;

        //real elements starts in postion 2.
        int pos = 2;

        //directly iterate the buffer, so out of order.
        while (pos < elementsCount)
        {
            if (Intrinsics.equalsKType(e1, buffer[pos]))
            {
                //each time a pos is removed, pos is patched with the last element,
                //so continue to test the same position
                removeAtPosNoCheck(pos);
                deleted++;
            }
            else
            {
                pos++;
            }
        } //end while

        return deleted;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean contains(final KType e1)
    {
        return indexOf(e1) >= 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int indexOf(final KType e1)
    {
        final long[] pointers = this.beforeAfterPointers;
        final KType[] buffer = this.buffer;

        int currentPos = Intrinsics.getLinkAfter(pointers[KTypeLinkedList.HEAD_POSITION]);
        int count = 0;

        while (currentPos != KTypeLinkedList.TAIL_POSITION)
        {
            if (Intrinsics.equalsKType(e1, buffer[currentPos]))
            {
                return count;
            }

            currentPos = Intrinsics.getLinkAfter(pointers[currentPos]);
            count++;
        }

        return -1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int lastIndexOf(final KType e1)
    {
        final long[] pointers = this.beforeAfterPointers;
        final KType[] buffer = this.buffer;

        int currentPos = Intrinsics.getLinkBefore(pointers[KTypeLinkedList.TAIL_POSITION]);
        int count = 0;

        while (currentPos != KTypeLinkedList.HEAD_POSITION)
        {
            if (Intrinsics.equalsKType(e1, buffer[currentPos]))
            {
                return size() - count - 1;
            }

            currentPos = Intrinsics.getLinkBefore(pointers[currentPos]);
            count++;
        }

        return -1;
    }

    /**
     * Increases the capacity of this instance, if necessary, to ensure
     * that it can hold at least the number of elements specified by
     * the minimum capacity argument.
     */
    public void ensureCapacity(final int minCapacity)
    {
        if (minCapacity > this.buffer.length)
            ensureBufferSpace(minCapacity - size());
    }

    /**
     * Ensures the internal buffer has enough free slots to store
     * <code>expectedAdditions</code>. Increases internal buffer size if needed.
     */
    protected void ensureBufferSpace(final int expectedAdditions)
    {
        final int bufferLen = (buffer == null ? 0 : buffer.length);
        if (elementsCount >= bufferLen - expectedAdditions)
        {
            final int newSize = resizer.grow(bufferLen, elementsCount, expectedAdditions);
            assert newSize >= elementsCount + expectedAdditions : "Resizer failed to" +
                    " return sensible new size: " + newSize + " <= "
                    + (elementsCount + expectedAdditions);

            //the first 2 slots are head/tail placeholders
            final KType[] newBuffer = Intrinsics.newKTypeArray(newSize + 2);
            final long[] newPointers = new long[newSize + 2];

            if (bufferLen > 0)
            {
                System.arraycopy(buffer, 0, newBuffer, 0, buffer.length);
                System.arraycopy(beforeAfterPointers, 0, newPointers, 0, beforeAfterPointers.length);
            }
            this.buffer = newBuffer;
            this.beforeAfterPointers = newPointers;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int size()
    {
        return elementsCount - 2;
    }

    /**
     * Sets the number of stored elements to zero.
     */
    @Override
    public void clear()
    {
        /*! #if ($TemplateOptions.KTypeGeneric) !*/
        Internals.blankObjectArray(buffer, 0, elementsCount);
        /*! #end !*/

        //the first two are placeholders
        this.elementsCount = 2;

        //rebuild head/tail
        beforeAfterPointers[KTypeLinkedList.HEAD_POSITION] = Intrinsics.getLinkNodeValue(KTypeLinkedList.HEAD_POSITION, KTypeLinkedList.TAIL_POSITION);
        beforeAfterPointers[KTypeLinkedList.TAIL_POSITION] = Intrinsics.getLinkNodeValue(KTypeLinkedList.HEAD_POSITION, KTypeLinkedList.TAIL_POSITION);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public KType[] toArray(final KType[] target)
    {
        //Return in-order
        final long[] pointers = this.beforeAfterPointers;
        final KType[] buffer = this.buffer;

        int index = 0;

        int currentPos = Intrinsics.getLinkAfter(pointers[KTypeLinkedList.HEAD_POSITION]);

        while (currentPos != KTypeLinkedList.TAIL_POSITION)
        {
            target[index] = buffer[currentPos];
            //increment both
            currentPos = Intrinsics.getLinkAfter(pointers[currentPos]);
            index++;
        }

        return target;
    }

    /**
     * Clone this object. The returned clone will use the same resizing strategy.
     */
    @Override
    public KTypeLinkedList<KType> clone()
    {
        /* #if ($TemplateOptions.KTypeGeneric) */
        @SuppressWarnings("unchecked")
        /* #end */
        final KTypeLinkedList<KType> cloned = new KTypeLinkedList<KType>(this.buffer.length, this.resizer);

        // safe to clone, only "primitive" arrays
        cloned.buffer = this.buffer.clone();
        cloned.beforeAfterPointers = this.beforeAfterPointers.clone();
        cloned.elementsCount = this.elementsCount;
        cloned.defaultValue = this.defaultValue;

        return cloned;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        final long[] pointers = this.beforeAfterPointers;
        final KType[] buffer = this.buffer;
        int h = 1;

        int currentPos = Intrinsics.getLinkAfter(pointers[KTypeLinkedList.HEAD_POSITION]);

        while (currentPos != KTypeLinkedList.TAIL_POSITION)
        {
            h = 31 * h + Internals.rehash(buffer[currentPos]);

            //increment
            currentPos = Intrinsics.getLinkAfter(pointers[currentPos]);
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
            final long[] pointers = this.beforeAfterPointers;
            final KType[] buffer = this.buffer;

            if (obj instanceof KTypeLinkedList<?>)
            {
                final KTypeLinkedList<?> other = (KTypeLinkedList<?>) obj;

                if (other.size() != this.size())
                {
                    return false;
                }

                final long[] pointersOther = other.beforeAfterPointers;
                final KType[] bufferOther = (KType[]) other.buffer;

                //compare index/index
                int currentPos = Intrinsics.getLinkAfter(pointers[KTypeLinkedList.HEAD_POSITION]);
                int currentPosOther = Intrinsics.getLinkAfter(pointersOther[KTypeLinkedList.HEAD_POSITION]);

                while (currentPos != KTypeLinkedList.TAIL_POSITION && currentPosOther != KTypeLinkedList.TAIL_POSITION)
                {
                    if (!Intrinsics.equalsKType(buffer[currentPos], bufferOther[currentPosOther]))
                    {
                        return false;
                    }

                    //increment both
                    currentPos = Intrinsics.getLinkAfter(pointers[currentPos]);
                    currentPosOther = Intrinsics.getLinkAfter(pointersOther[currentPosOther]);
                }

                return true;
            }
            else if (obj instanceof KTypeIndexedContainer<?>)
            {
                final KTypeIndexedContainer<?> other = (KTypeIndexedContainer<?>) obj;

                if (other.size() != this.size())
                {
                    return false;
                }

                //compare index/index
                int currentPos = Intrinsics.getLinkAfter(pointers[KTypeLinkedList.HEAD_POSITION]);
                int index = 0;

                while (currentPos != KTypeLinkedList.TAIL_POSITION)
                {
                    if (!Intrinsics.equalsKType(buffer[currentPos], other.get(index)))
                    {
                        return false;
                    }

                    //increment both
                    currentPos = Intrinsics.getLinkAfter(pointers[currentPos]);
                    index++;
                }

                return true;
            }
        }
        return false;
    }

    /**
     * Traverse the list to index, return the position in buffer
     * when reached
     * @param index
     * @return
     */
    protected int gotoIndex(final int index)
    {
        int currentPos = KTypeLinkedList.TAIL_POSITION;
        int currentIndex = -1;
        final long[] pointers = this.beforeAfterPointers;

        //A) Search by head
        if (index <= (elementsCount / 2.0))
        {
            //reach index - 1 position, from head, insert after
            currentIndex = 0;
            currentPos = Intrinsics.getLinkAfter(pointers[KTypeLinkedList.HEAD_POSITION]);

            while (currentIndex < index && currentPos != KTypeLinkedList.TAIL_POSITION)
            {
                currentPos = Intrinsics.getLinkAfter(pointers[currentPos]);
                currentIndex++;
            }
        }
        else
        {
            //B) short path to go from tail,
            //reach index - 1 position, from head, insert after
            currentIndex = size() - 1;
            currentPos = Intrinsics.getLinkBefore(pointers[KTypeLinkedList.TAIL_POSITION]);

            while (currentIndex > index && currentPos != KTypeLinkedList.HEAD_POSITION)
            {
                currentPos = Intrinsics.getLinkBefore(pointers[currentPos]);
                currentIndex--;
            }
        }

        //assert currentIndex == index;

        return currentPos;
    }

    /**
     * Insert after position insertionPos.
     * Insert an element just after insertionPos element.
     * @param e1
     * @param insertionPos
     */
    private void insertAfterPosNoCheck(final KType e1, final int insertionPos)
    {
        final long[] pointers = this.beforeAfterPointers;

        //we insert between insertionPos and its successor, keep reference of the successor
        final int nextAfterInsertionPos = Intrinsics.getLinkAfter(pointers[insertionPos]);

        //the new element is taken at the end of the buffer, at elementsCount.
        //link it as: [insertionPos | nextAfterInsertionPos]
        pointers[elementsCount] = Intrinsics.getLinkNodeValue(insertionPos, nextAfterInsertionPos);

        //re-link insertionPos element (left) :
        //[.. | nextAfterInsertionPos] ==> [.. | elementsCount]
        pointers[insertionPos] = Intrinsics.setLinkAfterNodeValue(pointers[insertionPos], this.elementsCount);

        //re-link nextAfterInsertionPos element (right)
        //[insertionPos |..] ==> [elementsCount | ..]
        pointers[nextAfterInsertionPos] = Intrinsics.setLinkBeforeNodeValue(pointers[nextAfterInsertionPos], this.elementsCount);

        //really add element to buffer at position elementsCount
        buffer[elementsCount] = e1;
        elementsCount++;
    }

    /**
     * Remove element at position removalPos in buffer
     * @param removalPos
     * @return the next valid position after the removal, supposing head to tail iteration.
     */
    private int removeAtPosNoCheck(final int removalPos)
    {
        final long[] pointers = this.beforeAfterPointers;

        //A) Unlink removalPos properly
        final int beforeRemovalPos = Intrinsics.getLinkBefore(pointers[removalPos]);
        final int afterRemovalPos = Intrinsics.getLinkAfter(pointers[removalPos]);

        //the element before element removalPos is now linked to afterRemovalPos :
        //[... | removalPos] ==> [... | afterRemovalPos]
        pointers[beforeRemovalPos] = Intrinsics.setLinkAfterNodeValue(pointers[beforeRemovalPos], afterRemovalPos);

        //the element after element removalPos is now linked to beforeRemovalPos :
        //[removalPos | ...] ==> [beforeRemovalPos | ...]
        pointers[afterRemovalPos] = Intrinsics.setLinkBeforeNodeValue(pointers[afterRemovalPos], beforeRemovalPos);

        //if the removed element is not the last of the buffer, move it to the "hole" created at removalPos
        if (removalPos != elementsCount - 1)
        {
            //B) To keep the buffer compact, take now the last buffer element and put it to  removalPos
            //keep the positions of the last buffer element, because we'll need to re-link it after
            //moving the element elementsCount - 1
            final int beforeLastElementPos = Intrinsics.getLinkBefore(pointers[elementsCount - 1]);
            final int afterLastElementPos = Intrinsics.getLinkAfter(pointers[elementsCount - 1]);

            //To keep the buffer compact, take now the last buffer element and put it to  removalPos
            buffer[removalPos] = buffer[elementsCount - 1];
            pointers[removalPos] = pointers[elementsCount - 1];

            //B-2) Re-link the elements that where neighbours of "elementsCount - 1" to point to removalPos element now
            //update the pointer of the before the last element = [... | elementsCount - 1] ==> [... | removalPos]
            pointers[beforeLastElementPos] = Intrinsics.setLinkAfterNodeValue(pointers[beforeLastElementPos], removalPos);

            //update the pointer of the after the last element = [... | elementsCount - 1] ==> [... | removalPos]
            pointers[afterLastElementPos] = Intrinsics.setLinkBeforeNodeValue(pointers[afterLastElementPos], removalPos);
        }

        //for GC
        /*! #if ($TemplateOptions.KTypeGeneric) !*/
        buffer[elementsCount - 1] = Intrinsics.<KType> defaultKTypeValue();
        /*! #end !*/

        elementsCount--;

        return Intrinsics.getLinkAfter(pointers[beforeRemovalPos]);
    }

    /**
     * An iterator implementation for {@link ObjectLinkedList#iterator}.
     * <pre>
     * <b>
     * Important note: DO NOT USE java.util.Iterator methods ! They are here only for enhanced-loop syntax and compatibility. Use
     * the specialized methods instead !
     * Iteration example:
     * </b></pre>
     * <pre>
     *   KTypeLinkedList<KType>.ValueIterator it = null;
     *   try
     *   {
     *       for (it = list.iterator().gotoNext(); !it.isTail(); it.gotoNext())
     *       {
     *
     *          // do something
     *       }
     *   }
     *   finally
     *   {
     *       //do not forget to release the iterator !
     *       it.release();
     *   }
     * </pre>
     */
    public class ValueIterator extends AbstractIterator<KTypeCursor<KType>>
    {
        public final KTypeCursor<KType> cursor;

        protected final KTypeCursor<KType> tmpCursor;

        KType[] buffer;
        int internalPos;
        long[] pointers;
        int internalIndex = -1;

        public ValueIterator()
        {
            this.cursor = new KTypeCursor<KType>();
            this.tmpCursor = new KTypeCursor<KType>();

            internalIndex = -1;
            this.cursor.index = -1;
            this.cursor.value = KTypeLinkedList.this.defaultValue;

            this.buffer = KTypeLinkedList.this.buffer;
            this.pointers = KTypeLinkedList.this.beforeAfterPointers;
            this.internalPos = KTypeLinkedList.HEAD_POSITION;
        }

        /**
         * <b>
         * DO NOT USE, unsupported operation.
         * </b>
         */
        @Override
        protected KTypeCursor<KType> fetch()
        {
            throw new UnsupportedOperationException();
        }

        /**
         * <b>
         * DO NOT USE directly, It is here only for the enhanced for loop syntax support.
         * </b>
         */
        @Override
        public boolean hasNext()
        {
            final int nextPos = Intrinsics.getLinkAfter(this.pointers[internalPos]);

            //auto-release when hasNext() returns false;
            if (nextPos == KTypeLinkedList.TAIL_POSITION && this.iteratorPool != null && !this.isFree)
            {
                this.iteratorPool.release(this);
                this.isFree = true;
            }

            return nextPos != KTypeLinkedList.TAIL_POSITION;
        }

        /**
         * <b>
         * DO NOT USE directly, It is here only for the enhanced for loop syntax support.
         * </b>
         */
        @Override
        public KTypeCursor<KType> next()
        {
            //search for the next position
            final int nextPos = Intrinsics.getLinkAfter(this.pointers[internalPos]);

            //we are at tail already.
            if (nextPos == KTypeLinkedList.TAIL_POSITION)
            {
                throw new NoSuchElementException();
            }

            //point to next
            this.internalPos = nextPos;
            this.internalIndex++;

            cursor.index = internalIndex;
            cursor.value = buffer[nextPos];

            return cursor;
        }

        ///////////////////////// Forward iteration methods //////////////////////////////////////
        /**
         * True is iterator points to the "head", i.e such as gotoNext() point to the first
         * element, with respect to the forward iteration.
         * @return
         */
        public boolean isHead()
        {
            return this.internalPos == KTypeLinkedList.HEAD_POSITION;
        }

        /**
         * True is iterator points to the "tail", i.e such as gotoPrevious() point to the last
         * element, with respect to the forward iteration.
         * @return
         */
        public boolean isTail()
        {
            return this.internalPos == KTypeLinkedList.TAIL_POSITION;
        }

        /**
         * True if the iterator points to the first element
         * with respect to the forward iteration. Always true if the list is empty.
         */
        public boolean isFirst()
        {
            final int nextPos = Intrinsics.getLinkAfter(pointers[KTypeLinkedList.HEAD_POSITION]);

            return (nextPos == KTypeLinkedList.TAIL_POSITION) ? true : (nextPos == this.internalPos);
        }

        /**
         * True if the iterator points to the last element
         * with respect to the forward iteration. Always true if the list is empty.
         */
        public boolean isLast()
        {
            final int nextPos = Intrinsics.getLinkBefore(pointers[KTypeLinkedList.TAIL_POSITION]);

            return (nextPos == KTypeLinkedList.HEAD_POSITION) ? true : (nextPos == this.internalPos);
        }

        /**
         * Move the iterator to the "head", returning itself for chaining.
         */
        public ValueIterator gotoHead()
        {
            this.internalIndex = -1;
            this.internalPos = KTypeLinkedList.HEAD_POSITION;

            //update cursor
            this.cursor.index = -1;
            this.cursor.value = KTypeLinkedList.this.defaultValue;

            return this;
        }

        /**
         * Move the iterator to the "tail", returning itself for chaining.
         */
        public ValueIterator gotoTail()
        {
            this.internalIndex = KTypeLinkedList.this.size();
            this.internalPos = KTypeLinkedList.TAIL_POSITION;

            //update cursor
            this.cursor.index = this.internalIndex;
            this.cursor.value = KTypeLinkedList.this.defaultValue;

            return this;
        }

        /**
         * Move the iterator to the next element, with respect to the forward iteration,
         * returning itself for chaining. When "tail" is reached, gotoNext() stays in place, at tail.
         */
        public ValueIterator gotoNext()
        {
            this.internalPos = Intrinsics.getLinkAfter(this.pointers[this.internalPos]);

            if (this.internalPos == KTypeLinkedList.TAIL_POSITION)
            {
                this.internalIndex = KTypeLinkedList.this.size();
                //update cursor
                this.cursor.index = this.internalIndex;
                this.cursor.value = KTypeLinkedList.this.defaultValue;
            }
            else
            {
                this.internalIndex++;

                //update cursor
                this.cursor.index = this.internalIndex;
                this.cursor.value = buffer[this.internalPos];
            }

            return this;
        }

        /**
         * Move the iterator to the previous element, with respect to the forward iteration,
         * returning itself for chaining. When "head" is reached, gotoPrevious() stays in place, at head.
         */
        public ValueIterator gotoPrevious()
        {
            this.internalPos = Intrinsics.getLinkBefore(this.pointers[this.internalPos]);

            if (this.internalPos == KTypeLinkedList.HEAD_POSITION)
            {
                this.internalIndex = -1;
                this.cursor.index = this.internalIndex;
                this.cursor.value = KTypeLinkedList.this.defaultValue;
            }
            else
            {
                this.internalIndex--;
                this.cursor.index = this.internalIndex;
                this.cursor.value = buffer[this.internalPos];
            }

            return this;
        }

        /**
         * Get the next element value with respect to the forward iteration,
         * without moving the iterator.
         * Returns null if no such element exists.
         */
        public KTypeCursor<KType> getNext()
        {
            final int nextPos = Intrinsics.getLinkAfter(this.pointers[this.internalPos]);

            if (nextPos == KTypeLinkedList.TAIL_POSITION)
            {
                return null;
            }
            //use the temporary Cursor in order to protect the iterator cursor
            this.tmpCursor.index = this.internalIndex + 1;
            this.tmpCursor.value = buffer[nextPos];

            return this.tmpCursor;
        }

        /**
         * Get the previous element value with respect to the forward iteration,
         * Returns null if no such element exists.
         */
        public KTypeCursor<KType> getPrevious()
        {
            final int beforePos = Intrinsics.getLinkBefore(this.pointers[this.internalPos]);

            if (beforePos == KTypeLinkedList.HEAD_POSITION)
            {
                return null;
            }

            //use the temporary Cursor in order to protect the iterator cursor
            this.tmpCursor.index = this.internalIndex - 1;
            this.tmpCursor.value = buffer[beforePos];

            return this.tmpCursor;
        }

        /**
         * Removes the next element, without moving the iterator.
         * Returns the removed element cursor, or null if the removal failed.
         * (because there is no element left after)
         */
        public KTypeCursor<KType> removeNext()
        {
            final int nextPos = Intrinsics.getLinkAfter(this.pointers[this.internalPos]);

            if (nextPos == KTypeLinkedList.TAIL_POSITION)
            {
                return null;
            }

            //store the next positions in returned cursor
            this.tmpCursor.index = this.internalIndex + 1;
            this.tmpCursor.value = this.buffer[nextPos];

            // the current position is unaffected.
            removeAtPosNoCheck(nextPos);

            return this.tmpCursor;
        }

        /**
         * Removes the previous element, without moving the iterator.
         * Returns the removed element cursor, or null if the removal failed.
         * (because there is no element left before)
         */
        public KTypeCursor<KType> removePrevious()
        {
            final int previousPos = Intrinsics.getLinkBefore(this.pointers[this.internalPos]);

            if (previousPos == KTypeLinkedList.HEAD_POSITION)
            {
                return null;
            }

            //store the next positions in returned cursor
            this.tmpCursor.index = this.internalIndex - 1;
            this.tmpCursor.value = this.buffer[previousPos];

            //update the new current position
            this.internalPos = removeAtPosNoCheck(previousPos);

            //the internal index changed, update the cursor index, the buffer stays unchanged.
            this.internalIndex--;
            this.cursor.index = this.internalIndex;

            return this.tmpCursor;
        }

        /**
         * Insert e1 before the iterator position, without moving the iterator.
         */
        public void insertBefore(final KType e1)
        {
            //protect the head
            if (this.internalPos != KTypeLinkedList.HEAD_POSITION)
            {
                final int beforePos = Intrinsics.getLinkBefore(this.pointers[this.internalPos]);

                //we insert after the previous
                insertAfterPosNoCheck(e1, beforePos);

                //the internal index changed, update the cursor index, the buffer stays unchanged.
                this.internalIndex++;
                this.cursor.index = this.internalIndex;
            }
        }

        /**
         * Insert e1 after the iterator position, without moving the iterator.
         */
        public void insertAfter(final KType e1)
        {
            //protect the tail
            if (this.internalPos != KTypeLinkedList.TAIL_POSITION)
            {
                //we insert after us
                insertAfterPosNoCheck(e1, this.internalPos);
            }

            //the internal index doesn't change...
        }

        /**
         * Set e1 to the current iterator position, without moving the iterator,
         * while returning the previous value. If no such previous value exists, returns the default value.
         */
        public KType set(final KType e1)
        {
            KType elem = KTypeLinkedList.this.defaultValue;

            //protect the heads/tails
            if (this.internalPos != KTypeLinkedList.HEAD_POSITION && this.internalPos != KTypeLinkedList.TAIL_POSITION)
            {
                elem = this.buffer[this.internalPos];

                this.buffer[this.internalPos] = e1;
            }

            return elem;
        }

        /**
         * Delete the current iterator position, and moves to the next valid
         * element after this removal, with respect to the forward iteration.
         * Returns the iterator itself for chaining.
         */
        public ValueIterator delete()
        {
            //protect the heads/tails
            if (this.internalPos != KTypeLinkedList.HEAD_POSITION && this.internalPos != KTypeLinkedList.TAIL_POSITION)
            {
                this.internalPos = removeAtPosNoCheck(this.internalPos);

                //update cursor returned by iterator
                cursor.value = buffer[this.internalPos];
                //internal index doesn't change
            }

            return this;
        }

    }

    /**
     * An iterator implementation for {@link ObjectLinkedList#descendingIterator()}.
     * <pre><b>
     * Important note: DO NOT USE java.util.Iterator methods ! They are here only for compatibility. Use
     * the specialized methods instead !
     * </b></pre>
     * Iteration example:
     * </b>
     * <pre>
     *   KTypeLinkedList<KType>.ValueIterator it = null;
     *   try
     *   {
     *       for (it = list.iterator().gotoNext(); !it.isTail(); it.gotoNext())
     *       {
     *          // do something
     *       }
     *   }
     *   finally
     *   {
     *       //do not forget to release the iterator !
     *       it.release();
     *   }
     * </pre>
     */
    public final class DescendingValueIterator extends ValueIterator
    {
        public DescendingValueIterator()
        {
            super();

            internalIndex = KTypeLinkedList.this.size();

            this.cursor.index = KTypeLinkedList.this.size();
            this.cursor.value = KTypeLinkedList.this.defaultValue;

            this.buffer = KTypeLinkedList.this.buffer;
            this.pointers = KTypeLinkedList.this.beforeAfterPointers;
            this.internalPos = KTypeLinkedList.TAIL_POSITION;
        }

        /**
         * <b>
         * DO NOT USE.
         * </b>
         */
        @Override
        public boolean hasNext()
        {
            final int nextPos = Intrinsics.getLinkBefore(this.pointers[internalPos]);

            //auto-release when hasNext() returns false;
            if (nextPos == KTypeLinkedList.HEAD_POSITION && this.iteratorPool != null && !this.isFree)
            {
                this.iteratorPool.release(this);
                this.isFree = true;
            }

            return nextPos != KTypeLinkedList.HEAD_POSITION;
        }

        /**
         * <b>
         * DO NOT USE directly, It is here only for the enhanced for loop syntax support.
         * </b>
         */
        @Override
        public KTypeCursor<KType> next()
        {
            //search for the next position
            final int nextPos = Intrinsics.getLinkBefore(this.pointers[internalPos]);

            //we are at tail already.
            if (nextPos == KTypeLinkedList.HEAD_POSITION)
            {
                throw new NoSuchElementException();
            }

            //point to next
            this.internalPos = nextPos;
            this.internalIndex--;

            cursor.index = internalIndex;
            cursor.value = buffer[nextPos];

            return cursor;
        }

        ///////////////////////// Descending iteration methods //////////////////////////////////////
        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isHead()
        {
            return super.isTail();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isTail()
        {
            return super.isHead();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isFirst()
        {
            return super.isLast();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isLast()
        {
            return super.isFirst();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ValueIterator gotoHead()
        {
            return super.gotoTail();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        @SuppressWarnings("unchecked")
        public DescendingValueIterator gotoTail()
        {
            return (DescendingValueIterator) super.gotoHead();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        @SuppressWarnings("unchecked")
        public DescendingValueIterator gotoNext()
        {
            return (DescendingValueIterator) super.gotoPrevious();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        @SuppressWarnings("unchecked")
        public DescendingValueIterator gotoPrevious()
        {
            return (DescendingValueIterator) super.gotoNext();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public KTypeCursor<KType> getNext()
        {
            return super.getPrevious();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public KTypeCursor<KType> getPrevious()
        {
            return super.getNext();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public KTypeCursor<KType> removeNext()
        {
            return super.removePrevious();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public KTypeCursor<KType> removePrevious()
        {
            return super.removeNext();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void insertBefore(final KType e1)
        {
            super.insertAfter(e1);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void insertAfter(final KType e1)
        {
            super.insertBefore(e1);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public DescendingValueIterator delete()
        {
            if (this.internalPos != KTypeLinkedList.HEAD_POSITION && this.internalPos != KTypeLinkedList.TAIL_POSITION)
            {
                //this is the next position in the normal iteration direction, so we must go back one step
                final int nextPos = removeAtPosNoCheck(this.internalPos);

                this.internalPos = Intrinsics.getLinkBefore(this.pointers[nextPos]);

                this.internalIndex--;
                //update cursor
                cursor.value = buffer[this.internalPos];
                //index is decremented
                cursor.index = this.internalIndex;
            }

            return this;
        }
    }

    /**
     * 
     * Request a new iterator. The iterator points to "head", such as ValueIterator.gotoNext()
     * is the first element of the list.
     * <b>
     * Important note: java.util.Iterator methods are error-prone, and only there for compatibility and enhanced for loop usage:
     * <pre>
     * for (KTypeCursor<KType> c : container) {
     *      System.out.println("index=" + c.index + " value=" + c.value);
     *    }
     * </pre>
     * Prefer the specialized methods instead :
     * <pre>
     * ValueIterator it = list.iterator();
     * while (!it.isTail())
     * {
     *     final KTypeCursor c = it.gotoNext();
     *     System.out.println(&quot;buffer index=&quot;
     *         + c.index + &quot; value=&quot; + c.value);
     * }
     * 
     * // release the iterator at the end !
     * it.release()
     * </pre>
     * </b>
     * @see ValueIterator#isHead()
     */
    @Override
    public ValueIterator iterator()
    {
        //return new ValueIterator<KType>();
        return this.valueIteratorPool.borrow();
    }

    /**
     * Returns a cursor over the values of this list (in tail to head order).
     * The iterator points to the "head", such as DescendingValueIterator.gotoNext()
     * is the last element of the list (since it is a "reversed" iteration).
     * The iterator is implemented as a cursor and it returns <b>the same cursor instance</b>
     * on every call to {@link Iterator#next()} (to avoid boxing of primitive types). To
     * read the current value (or index in the deque's buffer) use the cursor's public
     * fields. An example is shown below.
     * <b>
     * Important note: java.util.Iterator methods are error-prone, and only there for compatibility. Use
     * the specialized methods instead :
     * * <pre>
     * DescendingValueIterator it = list.descendingIterator();
     * while (!it.isTail())
     * {
     *     final KTypeCursor c = it.gotoNext();
     *     System.out.println(&quot;buffer index=&quot;
     *         + c.index + &quot; value=&quot; + c.value);
     * }
     * 
     * // release the iterator at the end !
     * it.release()
     * </pre>
     * </b>
     * @see ValueIterator#isHead()
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
        final long[] pointers = this.beforeAfterPointers;

        int currentPos = Intrinsics.getLinkAfter(pointers[KTypeLinkedList.HEAD_POSITION]);

        while (currentPos != KTypeLinkedList.TAIL_POSITION)
        {
            procedure.apply(buffer[currentPos]);

            currentPos = Intrinsics.getLinkAfter(pointers[currentPos]);

        } //end while

        return procedure;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends KTypePredicate<? super KType>> T forEach(final T predicate)
    {
        final long[] pointers = this.beforeAfterPointers;

        int currentPos = Intrinsics.getLinkAfter(pointers[KTypeLinkedList.HEAD_POSITION]);

        while (currentPos != KTypeLinkedList.TAIL_POSITION)
        {
            if (!predicate.apply(buffer[currentPos]))
            {
                break;
            }

            currentPos = Intrinsics.getLinkAfter(pointers[currentPos]);

        } //end while

        return predicate;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends KTypeProcedure<? super KType>> T descendingForEach(final T procedure)
    {
        final long[] pointers = this.beforeAfterPointers;

        int currentPos = Intrinsics.getLinkBefore(pointers[KTypeLinkedList.TAIL_POSITION]);

        while (currentPos != KTypeLinkedList.HEAD_POSITION)
        {
            procedure.apply(buffer[currentPos]);

            currentPos = Intrinsics.getLinkBefore(pointers[currentPos]);

        } //end while

        return procedure;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends KTypePredicate<? super KType>> T descendingForEach(final T predicate)
    {
        final long[] pointers = this.beforeAfterPointers;

        int currentPos = Intrinsics.getLinkBefore(pointers[KTypeLinkedList.TAIL_POSITION]);

        while (currentPos != KTypeLinkedList.HEAD_POSITION)
        {
            if (!predicate.apply(buffer[currentPos]))
            {
                break;
            }

            currentPos = Intrinsics.getLinkAfter(pointers[currentPos]);

        } //end while

        return predicate;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int removeAll(final KTypePredicate<? super KType> predicate)
    {
        final KType[] buffer = this.buffer;

        int deleted = 0;

        //real elements starts in position 2.
        int pos = 2;

        //directly iterate the buffer, so out of order.
        while (pos < elementsCount)
        {
            if (predicate.apply(buffer[pos]))
            {
                //each time a pos is removed, pos itself is patched with the last element,
                //so continue to test the same position.
                removeAtPosNoCheck(pos);
                deleted++;
            }
            else
            {
                pos++;
            }
        } //end while

        return deleted;
    }


    /**
     * Returns a new object of this class with no need to declare generic type (shortcut
     * instead of using a constructor).
     */
    public static/* #if ($TemplateOptions.KTypeGeneric) */<KType> /* #end */
    KTypeLinkedList<KType> newInstance()
    {
        return new KTypeLinkedList<KType>();
    }

    /**
     * Returns a new object of this class with no need to declare generic type (shortcut
     * instead of using a constructor).
     */
    public static/* #if ($TemplateOptions.KTypeGeneric) */<KType> /* #end */
    KTypeLinkedList<KType> newInstanceWithCapacity(final int initialCapacity)
    {
        return new KTypeLinkedList<KType>(initialCapacity);
    }

    /**
     * Create a list from a variable number of arguments or an array of <code>KType</code>.
     * The elements are copied from the argument to the internal buffer.
     */
    public static/* #if ($TemplateOptions.KTypeGeneric) */<KType> /* #end */
    KTypeLinkedList<KType> from(final KType... elements)
    {
        final KTypeLinkedList<KType> list = new KTypeLinkedList<KType>(elements.length);
        list.add(elements);
        return list;
    }

    /**
     * Create a list from elements of another container.
     */
    public static/* #if ($TemplateOptions.KTypeGeneric) */<KType> /* #end */
    KTypeLinkedList<KType> from(final KTypeContainer<KType> container)
    {
        return new KTypeLinkedList<KType>(container);
    }

    /**
     * Sort the whole list by natural ordering (smaller first)
     * <p><b>
     * This routine uses Dual-pivot Quicksort, from [Yaroslavskiy 2009]
     * </b></p>
     * @param beginIndex
     * @param endIndex
     */
/*! #if ($TemplateOptions.KTypePrimitive)
    public void sort()
    {
       if (elementsCount > 3)
        {
            int elementsCount = this.elementsCount;
            final long[] pointers = this.beforeAfterPointers;

            KTypeSort.quicksort(buffer, 2, elementsCount);

            //rebuild nodes, in order
            //a) rebuild head/tail

            //ties HEAD to the first element, and first element to HEAD
            pointers[HEAD_POSITION] = Intrinsics.getLinkNodeValue(HEAD_POSITION, 2);
            pointers[2] = Intrinsics.getLinkNodeValue(HEAD_POSITION, 3);

            for (int pos = 3; pos < elementsCount - 1; pos++)
            {
                pointers[pos] = Intrinsics.getLinkNodeValue(pos - 1, pos + 1);
            }

            //ties the last element to tail, and tail to last element
            pointers[elementsCount - 1] = Intrinsics.getLinkNodeValue(elementsCount - 2, TAIL_POSITION);
            pointers[TAIL_POSITION] = Intrinsics.getLinkNodeValue(elementsCount - 1, TAIL_POSITION);
        }
    }
    #end !*/

////////////////////////////

    /**
     * Sort by  dual-pivot quicksort an entire list
     * using a #if ($TemplateOptions.KTypeGeneric) <code>Comparator</code> #else <code>KTypeComparator<KType></code> #end
     * <p><b>
     * This routine uses Dual-pivot Quicksort, from [Yaroslavskiy 2009] #if ($TemplateOptions.KTypeGeneric), so is NOT stable. #end
     * </b></p>
     */
    public void sort(
            /*! #if ($TemplateOptions.KTypeGeneric) !*/
            final Comparator<KType>
            /*! #else
            KTypeComparator<KType>
            #end !*/
            comp)
    {
        if (elementsCount > 3)
        {
            final int elementsCount = this.elementsCount;
            final long[] pointers = this.beforeAfterPointers;

            KTypeSort.quicksort(buffer, 2, elementsCount, comp);

            //rebuild nodes, in order
            //a) rebuild head/tail

            //ties HEAD to the first element, and first element to HEAD
            pointers[KTypeLinkedList.HEAD_POSITION] = Intrinsics.getLinkNodeValue(KTypeLinkedList.HEAD_POSITION, 2);
            pointers[2] = Intrinsics.getLinkNodeValue(KTypeLinkedList.HEAD_POSITION, 3);

            for (int pos = 3; pos < elementsCount - 1; pos++)
            {
                pointers[pos] = Intrinsics.getLinkNodeValue(pos - 1, pos + 1);
            }

            //ties the last element to tail, and tail to last element
            pointers[elementsCount - 1] = Intrinsics.getLinkNodeValue(elementsCount - 2, KTypeLinkedList.TAIL_POSITION);
            pointers[KTypeLinkedList.TAIL_POSITION] = Intrinsics.getLinkNodeValue(elementsCount - 1, KTypeLinkedList.TAIL_POSITION);
        }
    }

    @Override
    public void addFirst(final KType e1)
    {
        ensureBufferSpace(1);
        insertAfterPosNoCheck(e1, KTypeLinkedList.HEAD_POSITION);
    }

    /**
     * Inserts all elements from the given container to the front of this list.
     * 
     * @return Returns the number of elements actually added as a result of this
     * call.
     */
    public int addFirst(final KTypeContainer<? extends KType> container)
    {
        final int size = container.size();
        ensureBufferSpace(size);

        for (final KTypeCursor<? extends KType> cursor : container)
        {
            insertAfterPosNoCheck(cursor.value, KTypeLinkedList.HEAD_POSITION);
        }

        return size;
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
        for (int i = 0; i < elements.length; i++)
        {
            insertAfterPosNoCheck(elements[i], KTypeLinkedList.HEAD_POSITION);
        }
    }

    /**
     * Inserts all elements from the given iterable to the front of this list.
     * 
     * @return Returns the number of elements actually added as a result of this call.
     */
    public int addFirst(final Iterable<? extends KTypeCursor<? extends KType>> iterable)
    {
        int size = 0;

        for (final KTypeCursor<? extends KType> cursor : iterable)
        {
            ensureBufferSpace(1);
            insertAfterPosNoCheck(cursor.value, KTypeLinkedList.HEAD_POSITION);
            size++;
        }
        return size;
    }

    @Override
    public void addLast(final KType e1)
    {
        ensureBufferSpace(1);
        insertAfterPosNoCheck(e1, Intrinsics.getLinkBefore(this.beforeAfterPointers[KTypeLinkedList.TAIL_POSITION]));
    }

    /**
     * Inserts all elements from the given container to the end of this list.
     * 
     * @return Returns the number of elements actually added as a result of this
     * call.
     */
    public int addLast(final KTypeContainer<? extends KType> container)
    {
        final int size = container.size();
        ensureBufferSpace(size);

        for (final KTypeCursor<? extends KType> cursor : container)
        {
            insertAfterPosNoCheck(cursor.value, Intrinsics.getLinkBefore(this.beforeAfterPointers[KTypeLinkedList.TAIL_POSITION]));
        }

        return size;
    }

    /**
     * Inserts all elements from the given iterable to the end of this list.
     * 
     * @return Returns the number of elements actually added as a result of this call.
     */
    public int addLast(final Iterable<? extends KTypeCursor<? extends KType>> iterable)
    {
        int size = 0;
        for (final KTypeCursor<? extends KType> cursor : iterable)
        {
            ensureBufferSpace(1);
            insertAfterPosNoCheck(cursor.value, Intrinsics.getLinkBefore(this.beforeAfterPointers[KTypeLinkedList.TAIL_POSITION]));
            size++;
        }
        return size;
    }

    @Override
    public KType removeFirst()
    {
        assert size() > 0;

        final int removedPos = Intrinsics.getLinkAfter(this.beforeAfterPointers[KTypeLinkedList.HEAD_POSITION]);

        final KType elem = buffer[removedPos];

        removeAtPosNoCheck(removedPos);

        return elem;
    }

    @Override
    public KType removeLast()
    {
        assert size() > 0;

        final int removedPos = Intrinsics.getLinkBefore(this.beforeAfterPointers[KTypeLinkedList.TAIL_POSITION]);

        final KType elem = buffer[removedPos];

        removeAtPosNoCheck(removedPos);

        return elem;
    }

    @Override
    public KType getFirst()
    {
        assert size() > 0;

        return buffer[Intrinsics.getLinkAfter(this.beforeAfterPointers[KTypeLinkedList.HEAD_POSITION])];
    }

    @Override
    public KType getLast()
    {
        assert size() > 0;

        return buffer[Intrinsics.getLinkBefore(this.beforeAfterPointers[KTypeLinkedList.TAIL_POSITION])];
    }
}
