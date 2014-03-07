package com.carrotsearch.hppc;

import java.util.*;

import com.carrotsearch.hppc.cursors.*;
import com.carrotsearch.hppc.predicates.*;
import com.carrotsearch.hppc.procedures.*;
import com.carrotsearch.hppc.sorting.*;

import static com.carrotsearch.hppc.Internals.*;

/**
 * An array-backed list of KTypes. A single array is used to store and manipulate
 * all elements. Reallocations are governed by a {@link ArraySizingStrategy}
 * and may be expensive if they move around really large chunks of memory.
 * 
#if ($TemplateOptions.KTypeGeneric)
 * A brief comparison of the API against the Java Collections framework:
 * <table class="nice" summary="Java Collections ArrayList and HPPC ObjectArrayList, related methods.">
 * <caption>Java Collections ArrayList and HPPC {@link ObjectArrayList}, related methods.</caption>
 * <thead>
 *     <tr class="odd">
 *         <th scope="col">{@linkplain ArrayList java.util.ArrayList}</th>
 *         <th scope="col">{@link ObjectArrayList}</th>
 *     </tr>
 * </thead>
 * <tbody>
 * <tr            ><td>add            </td><td>add            </td></tr>
 * <tr class="odd"><td>add(index,v)   </td><td>insert(index,v)</td></tr>
 * <tr            ><td>get            </td><td>get            </td></tr>
 * <tr class="odd"><td>removeRange,
 *                     removeElementAt</td><td>removeRange, remove</td></tr>
 * <tr            ><td>remove(Object) </td><td>removeFirstOccurrence, removeLastOccurrence,
 *                                             removeAllOccurrences</td></tr>
 * <tr class="odd"><td>clear          </td><td>clear, release </td></tr>
 * <tr            ><td>size           </td><td>size           </td></tr>
 * <tr class="odd"><td>ensureCapacity </td><td>ensureCapacity, resize</td></tr>
 * <tr            ><td>indexOf        </td><td>indexOf        </td></tr>
 * <tr class="odd"><td>lastIndexOf    </td><td>lastIndexOf    </td></tr>
 * <tr            ><td>trimToSize     </td><td>trimtoSize</td></tr>
 * <tr class="odd"><td>Object[] toArray()</td><td>KType[] toArray()</td></tr>
 * <tr            ><td>iterator       </td><td>{@linkplain #iterator() cursor over values}</td></tr>
 * </tbody>
 * </table>
#else
 * <p>See {@link ObjectArrayList} class for API similarities and differences against Java
 * Collections.
#end
 */
/*! ${TemplateOptions.generatedAnnotation} !*/
public class KTypeArrayList<KType>
        extends AbstractKTypeCollection<KType> implements KTypeIndexedContainer<KType>, Cloneable
{
    /**
     * Default capacity if no other capacity is given in the constructor.
     */
    public final static int DEFAULT_CAPACITY = 5;

    /**
     * Internal static instance of an empty buffer.
     */
    private final static Object EMPTY = /*!
                                        #if ($TemplateOptions.KTypePrimitive)
                                        new KType [0];
                                        #else !*/
            new Object[0];
    /*! #end !*/

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
     * Direct list iteration: iterate buffer[i] for i in [0; size()[
     * </p>
     */
    public KType[] buffer;

    /**
     * Current number of elements stored in {@link #buffer}.
     */
    protected int elementsCount;

    /**
     * Buffer resizing strategy.
     */
    protected final ArraySizingStrategy resizer;

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
    public KTypeArrayList()
    {
        this(KTypeArrayList.DEFAULT_CAPACITY);
    }

    /**
     * Create with default sizing strategy and the given initial capacity.
     * 
     * @see BoundedProportionalArraySizingStrategy
     */
    public KTypeArrayList(final int initialCapacity)
    {
        this(initialCapacity, new BoundedProportionalArraySizingStrategy());
    }

    /**
     * Create with a custom buffer resizing strategy.
     */
    public KTypeArrayList(final int initialCapacity, final ArraySizingStrategy resizer)
    {
        assert initialCapacity >= 0 : "initialCapacity must be >= 0: " + initialCapacity;
        assert resizer != null;

        this.resizer = resizer;

        final int internalSize = resizer.round(initialCapacity);

        //allocate internal buffer
        this.buffer = Intrinsics.newKTypeArray(internalSize);

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
                        obj.cursor.index = -1;
                        obj.size = KTypeArrayList.this.size();
                        obj.buffer = KTypeArrayList.this.buffer;
                    }

                    @Override
                    public void reset(final ValueIterator obj) {
                        // for GC sake
                        obj.buffer = null;
                    }
                });
    }

    /**
     * Creates a new list from elements of another container.
     */
    public KTypeArrayList(final KTypeContainer<? extends KType> container)
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
        ensureBufferSpace(1);
        buffer[elementsCount++] = e1;
    }

    /**
     * Appends two elements at the end of the list. To add more than two elements,
     * use <code>add</code> (vararg-version) or access the buffer directly (tight
     * loop).
     */
    public void add(final KType e1, final KType e2)
    {
        ensureBufferSpace(2);
        buffer[elementsCount++] = e1;
        buffer[elementsCount++] = e2;
    }

    /**
     * Add all elements from a range of given array to the list.
     */
    public void add(final KType[] elements, final int start, final int length)
    {
        assert length >= 0 : "Length must be >= 0";

        ensureBufferSpace(length);
        System.arraycopy(elements, start, buffer, elementsCount, length);
        elementsCount += length;
    }

    /**
     * Vararg-signature method for adding elements at the end of the list.
     * <p><b>This method is handy, but costly if used in tight loops (anonymous
     * array passing)</b></p>
     */
    public void add(final KType... elements)
    {
        add(elements, 0, elements.length);
    }

    /**
     * Adds all elements from another container.
     */
    public int addAll(final KTypeContainer<? extends KType> container)
    {
        final int size = container.size();
        ensureBufferSpace(size);

        for (final KTypeCursor<? extends KType> cursor : container)
        {
            add(cursor.value);
        }

        return size;
    }

    /**
     * Adds all elements from another iterable.
     */
    public int addAll(final Iterable<? extends KTypeCursor<? extends KType>> iterable)
    {
        int size = 0;
        for (final KTypeCursor<? extends KType> cursor : iterable)
        {
            add(cursor.value);
            size++;
        }
        return size;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void insert(final int index, final KType e1)
    {
        assert (index >= 0 && index <= size()) : "Index " + index + " out of bounds [" + 0 + ", " + size() + "].";

        ensureBufferSpace(1);
        System.arraycopy(buffer, index, buffer, index + 1, elementsCount - index);
        buffer[index] = e1;
        elementsCount++;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public KType get(final int index)
    {
        assert (index >= 0 && index < size()) : "Index " + index + " out of bounds [" + 0 + ", " + size() + ").";

        return buffer[index];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public KType set(final int index, final KType e1)
    {
        assert (index >= 0 && index < size()) : "Index " + index + " out of bounds [" + 0 + ", " + size() + ").";

        final KType v = buffer[index];
        buffer[index] = e1;
        return v;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public KType remove(final int index)
    {
        assert (index >= 0 && index < size()) : "Index " + index + " out of bounds [" + 0 + ", " + size() + ").";

        final KType v = buffer[index];
        if (index + 1 < elementsCount)
            System.arraycopy(buffer, index + 1, buffer, index, elementsCount - index - 1);
        elementsCount--;
        /*! #if ($TemplateOptions.KTypeGeneric) !*/
        buffer[elementsCount] = Intrinsics.<KType> defaultKTypeValue();
        /*! #end !*/
        return v;
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

        System.arraycopy(buffer, toIndex, buffer, fromIndex, elementsCount - toIndex);

        final int count = toIndex - fromIndex;
        elementsCount -= count;

        /*! #if ($TemplateOptions.KTypeGeneric) !*/
        Internals.blankObjectArray(buffer, elementsCount, elementsCount + count);
        /*! #end !*/
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int removeFirstOccurrence(final KType e1)
    {
        final int index = indexOf(e1);
        if (index >= 0)
            remove(index);
        return index;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int removeLastOccurrence(final KType e1)
    {
        final int index = lastIndexOf(e1);
        if (index >= 0)
            remove(index);
        return index;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int removeAllOccurrences(final KType e1)
    {
        int to = 0;
        final KType[] buffer = this.buffer;

        for (int from = 0; from < elementsCount; from++)
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

        final int deleted = elementsCount - to;
        this.elementsCount = to;
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
        final KType[] buffer = this.buffer;

        for (int i = 0; i < elementsCount; i++)
            if (Intrinsics.equalsKType(e1, buffer[i]))
                return i;

        return -1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int lastIndexOf(final KType e1)
    {
        final KType[] buffer = this.buffer;

        for (int i = elementsCount - 1; i >= 0; i--)
            if (Intrinsics.equalsKType(e1, buffer[i]))
                return i;

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

        if (elementsCount > bufferLen - expectedAdditions)
        {
            final int newSize = resizer.grow(bufferLen, elementsCount, expectedAdditions);
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
     * Truncate or expand the list to the new size. If the list is truncated, the buffer
     * will not be reallocated (use {@link #trimToSize()} if you need a truncated buffer).
     * If the list is expanded, the elements beyond the current size are initialized with JVM-defaults
     * (zero or <code>null</code> values).
     */
    public void resize(final int newSize)
    {
        if (newSize <= buffer.length)
        {
            if (newSize < elementsCount)
            {
                //there is no point in resetting to "null" elements
                //that becomes non-observable anyway. Still,
                //resetting is needed for GC in case of Objects because they may become "free"
                //if not referenced anywhere else.
                /*! #if ($TemplateOptions.KTypeGeneric) !*/
                Internals.blankObjectArray(buffer, newSize, elementsCount);
                /*! #end !*/
            }
            else
            {
                //in all cases, the contract of resize if that new elements
                //are set to default values.
                Arrays.fill(buffer, elementsCount, newSize,
                        Intrinsics.<KType> defaultKTypeValue());
            }
        }
        else
        {
            ensureCapacity(newSize);
        }

        this.elementsCount = newSize;
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
     * Trim the internal buffer to the current size.
     */
    /* #if ($TemplateOptions.KTypeGeneric) */
    @SuppressWarnings("unchecked")
    /* #end */
    public void trimToSize()
    {
        if (size() != this.buffer.length)
            this.buffer = (KType[]) toArray();
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
        this.elementsCount = 0;
    }

    /**
     * Sets the number of stored elements to zero and releases the internal storage array.
     */
    /* #if ($TemplateOptions.KTypeGeneric) */
    @SuppressWarnings("unchecked")
    /* #end */
    public void release()
    {
        this.buffer = (KType[]) KTypeArrayList.EMPTY;
        this.elementsCount = 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public KType[] toArray(final KType[] target)
    {
        System.arraycopy(buffer, 0, target, 0, elementsCount);
        return target;
    }

    /**
     * Clone this object. The returned clone will use the same resizing strategy.
     */
    @Override
    public KTypeArrayList<KType> clone()
    {
        /* #if ($TemplateOptions.KTypeGeneric) */
        @SuppressWarnings("unchecked")
        /* #end */
        final KTypeArrayList<KType> cloned = new KTypeArrayList<KType>(this.buffer.length, this.resizer);

        cloned.defaultValue = this.defaultValue;
        //add all in order, by construction.
        cloned.addAll(this);

        return cloned;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        int h = 1;
        final int max = elementsCount;
        final KType[] buffer = this.buffer;

        for (int i = 0; i < max; i++)
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
            if (obj == this)
                return true;

            if (obj instanceof KTypeArrayList<?>)
            {
                final KTypeArrayList<?> other = (KTypeArrayList<?>) obj;
                return other.size() == this.size() &&
                        rangeEquals(other.buffer, this.buffer, size());
            }
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
     * Compare a range of values in two arrays.
     */
    /*! #if ($TemplateOptions.KTypePrimitive)
    private boolean rangeEquals(KType [] b1, KType [] b2, int length)
        #else !*/
    private boolean rangeEquals(final Object[] b1, final Object[] b2, final int length)
    /*! #end !*/
    {
        for (int i = 0; i < length; i++)
        {
            if (!Intrinsics.equalsKType(b1[i], b2[i]))
            {
                return false;
            }
        }

        return true;
    }

    /**
     * Compare index-aligned KTypeIndexedContainer objects
     */
    final protected boolean allIndexesEqual(
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
     * An iterator implementation for {@link ObjectArrayList#iterator}.
     */
    public final class ValueIterator extends AbstractIterator<KTypeCursor<KType>>
    {
        public final KTypeCursor<KType> cursor;

        KType[] buffer;
        int size;

        public ValueIterator()
        {
            this.cursor = new KTypeCursor<KType>();
            this.cursor.index = -1;
            this.size = KTypeArrayList.this.size();
            this.buffer = KTypeArrayList.this.buffer;
        }

        @Override
        protected KTypeCursor<KType> fetch()
        {
            if (cursor.index + 1 == size)
                return done();

            cursor.value = buffer[++cursor.index];
            return cursor;
        }
    }

    /**
     * {@inheritDoc}
     * @return
     */
    @Override
    public ValueIterator iterator()
    {
        //return new ValueIterator<KType>(buffer, size());
        return this.valueIteratorPool.borrow();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends KTypeProcedure<? super KType>> T forEach(final T procedure)
    {
        return forEach(procedure, 0, size());
    }

    /**
     * Applies <code>procedure</code> to a slice of the list,
     * <code>fromIndex</code>, inclusive, to <code>toIndex</code>,
     * exclusive.
     */
    public <T extends KTypeProcedure<? super KType>> T forEach(final T procedure,
            final int fromIndex, final int toIndex)
    {
        assert (fromIndex >= 0 && fromIndex <= size()) : "Index " + fromIndex + " out of bounds [" + 0 + ", " + size() + ").";

        assert (toIndex >= 0 && toIndex <= size()) : "Index " + toIndex + " out of bounds [" + 0 + ", " + size() + "].";

        assert fromIndex <= toIndex : "fromIndex must be <= toIndex: "
                + fromIndex + ", " + toIndex;

        final KType[] buffer = this.buffer;

        for (int i = fromIndex; i < toIndex; i++)
        {
            procedure.apply(buffer[i]);
        }

        return procedure;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int removeAll(final KTypePredicate<? super KType> predicate)
    {
        final int elementsCount = this.elementsCount;
        final KType[] buffer = this.buffer;

        int to = 0;
        int from = 0;
        try
        {
            for (; from < elementsCount; from++)
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
            for (; from < elementsCount; from++)
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

            this.elementsCount = to;
        }

        return elementsCount - to;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends KTypePredicate<? super KType>> T forEach(final T predicate)
    {
        return forEach(predicate, 0, size());
    }

    /**
     * Applies <code>predicate</code> to a slice of the list,
     * <code>fromIndex</code>, inclusive, to <code>toIndex</code>,
     * exclusive, or until predicate returns <code>false</code>.
     */
    public <T extends KTypePredicate<? super KType>> T forEach(final T predicate,
            final int fromIndex, final int toIndex)
    {
        assert (fromIndex >= 0 && fromIndex <= size()) : "Index " + fromIndex + " out of bounds [" + 0 + ", " + size() + ").";

        assert (toIndex >= 0 && toIndex <= size()) : "Index " + toIndex + " out of bounds [" + 0 + ", " + size() + "].";

        assert fromIndex <= toIndex : "fromIndex must be <= toIndex: "
                + fromIndex + ", " + toIndex;

        final KType[] buffer = this.buffer;

        for (int i = fromIndex; i < toIndex; i++)
        {
            if (!predicate.apply(buffer[i]))
                break;
        }

        return predicate;
    }

    /**
     * Returns a new object of this class with no need to declare generic type (shortcut
     * instead of using a constructor).
     */
    public static/* #if ($TemplateOptions.KTypeGeneric) */<KType> /* #end */
            KTypeArrayList<KType> newInstance()
    {
        return new KTypeArrayList<KType>();
    }

    /**
     * Returns a new object of this class with no need to declare generic type (shortcut
     * instead of using a constructor).
     */
    public static/* #if ($TemplateOptions.KTypeGeneric) */<KType> /* #end */
            KTypeArrayList<KType> newInstanceWithCapacity(final int initialCapacity)
    {
        return new KTypeArrayList<KType>(initialCapacity);
    }

    /**
     * Create a list from a variable number of arguments or an array of <code>KType</code>.
     * The elements are copied from the argument to the internal buffer.
     */
    public static/* #if ($TemplateOptions.KTypeGeneric) */<KType> /* #end */
            KTypeArrayList<KType> from(final KType... elements)
    {
        final KTypeArrayList<KType> list = new KTypeArrayList<KType>(elements.length);
        list.add(elements);
        return list;
    }

    /**
     * Create a list from elements of another container.
     */
    public static/* #if ($TemplateOptions.KTypeGeneric) */<KType> /* #end */
            KTypeArrayList<KType> from(final KTypeContainer<KType> container)
    {
        return new KTypeArrayList<KType>(container);
    }

    /**
     * Sort the list from [beginIndex, endIndex[
     * by natural ordering (smaller first)
     * @param beginIndex
     * @param endIndex
     */
    /*! #if ($TemplateOptions.KTypePrimitive)
    public void sort(int beginIndex, int endIndex)
    {
        assert endIndex <= elementsCount;

        if (endIndex - beginIndex > 1)
        {
            KTypeSort.quicksort(buffer, beginIndex, endIndex);
        }
    }
    #end !*/

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
        if (elementsCount > 1)
        {
            KTypeSort.quicksort(buffer, 0, elementsCount);
        }
    }
    #end !*/

    ////////////////////////////
    /**
     * Sort the list of <code>KType</code>s from [beginIndex, endIndex[
     * using a #if ($TemplateOptions.KTypeGeneric) <code>Comparator</code> #else <code>KTypeComparator<KType></code> #end
     * <p><b>
     * This routine uses Dual-pivot Quicksort, from [Yaroslavskiy 2009] #if ($TemplateOptions.KTypeGeneric), so is NOT stable. #end
     * </b></p>
     */
    public void sort(
            final int beginIndex, final int endIndex,
            /*! #if ($TemplateOptions.KTypeGeneric) !*/
            final Comparator<KType>
            /*! #else
            KTypeComparator<KType>
            #end !*/
            comp)
    {
        assert endIndex <= elementsCount;

        if (endIndex - beginIndex > 1)
        {
            KTypeSort.quicksort(buffer, beginIndex, endIndex, comp);
        }
    }

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
        if (elementsCount > 1)
        {
            KTypeSort.quicksort(buffer, 0, elementsCount, comp);
        }
    }
}
