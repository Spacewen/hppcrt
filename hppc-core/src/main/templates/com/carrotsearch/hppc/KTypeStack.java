package com.carrotsearch.hppc;

import java.util.*;

import com.carrotsearch.hppc.cursors.*;
import com.carrotsearch.hppc.predicates.*;
import com.carrotsearch.hppc.procedures.*;
import com.carrotsearch.hppc.sorting.*;

import static com.carrotsearch.hppc.Internals.*;

/**
 * An extension to {@link KTypeArrayList} adding stack-related utility methods. The top of
 * the stack is at the <code>{@link #size()} - 1</code> buffer position.
 * However, this stack is also a KTypeIndexedContainer, for which index 0 is the top of the stack, 
 * and index size() -1 is the bottom of the stack.
 * 
#if ($TemplateOptions.KTypeGeneric)
 * A brief comparison of the API against the Java Collections framework:
 * <table class="nice" summary="Java Collections Stack and HPPC ObjectStack, related methods.">
 * <caption>Java Collections Stack and HPPC {@link ObjectStack}, related methods.</caption>
 * <thead>
 *     <tr class="odd">
 *         <th scope="col">{@linkplain java.util.Stack java.util.Stack}</th>
 *         <th scope="col">{@link ObjectStack}</th>
 *     </tr>
 * </thead>
 * <tbody>
 * <tr            ><td>push           </td><td>push           </td></tr>
 * <tr class="odd"><td>pop            </td><td>pop, discard   </td></tr>
 * <tr            ><td>peek           </td><td>peek           </td></tr>
 * <tr class="odd"><td>removeRange,
 *                     removeElementAt</td><td>removeRange, remove, discard</td></tr>
 * <tr            ><td>size           </td><td>size           </td></tr>
 * <tr class="odd"><td>clear          </td><td>clear, release </td></tr>
 * <tr            ><td>               </td><td>+ other methods from {@link ObjectArrayList}</td></tr>
 * </tbody>
 * </table>
#else
 * <p>See {@link ObjectArrayList} class for API similarities and differences against Java
 * Collections.
#end
 */
/*! ${TemplateOptions.generatedAnnotation} !*/
public class KTypeStack<KType> extends KTypeArrayList<KType>
{
    /**
     * Create with default sizing strategy and initial capacity.
     * 
     * @see BoundedProportionalArraySizingStrategy
     */
    public KTypeStack()
    {
        super();
    }

    /**
     * Create with default sizing strategy and the given initial capacity.
     * 
     * @see BoundedProportionalArraySizingStrategy
     */
    public KTypeStack(final int initialCapacity)
    {
        super(initialCapacity);
    }

    /**
     * Create with a custom buffer resizing strategy.
     */
    public KTypeStack(final int initialCapacity, final ArraySizingStrategy resizer)
    {
        super(initialCapacity, resizer);
    }

    /**
     * Create a stack by pushing all elements of another container to it.
     */
    public KTypeStack(final KTypeContainer<KType> container)
    {
        super(container);
    }

    /**
     * {@inheritDoc}
     * @param index : counted from the top of the stack, i.e = 0 if top, bottom is index = size() - 1
     */
    @Override
    public void insert(final int index, final KType e1)
    {
        super.insert(this.elementsCount - index, e1);
    }

    /**
     * {@inheritDoc}
     * @param index : counted from the top of the stack, i.e = 0 if top, bottom is index = size() - 1
     */
    @Override
    public KType get(final int index)
    {
        return super.get(this.elementsCount - index - 1);
    }

    /**
     * {@inheritDoc}
     * @param index : counted from the top of the stack, i.e = 0 if top, bottom is index = size() - 1
     */
    @Override
    public KType set(final int index, final KType e1)
    {
        return super.set(this.elementsCount - index - 1, e1);
    }

    /**
     * {@inheritDoc}
     * @param index : counted from the top of the stack, i.e = 0 if top, bottom is index = size() - 1
     */
    @Override
    public KType remove(final int index)
    {
        return super.remove(this.elementsCount - index - 1);
    }

    /**
     * {@inheritDoc}
     * @param index : counted from the top of the stack, i.e = 0 if top, bottom is index = size() - 1
     */
    @Override
    public void removeRange(final int fromIndex, final int toIndex)
    {
        final int size = size();

        final int startRemoveRange = size - toIndex;
        final int endRemoveRange = size - fromIndex;

        super.removeRange(startRemoveRange, endRemoveRange);
    }

    /**
     * {@inheritDoc}
     * The first occurrence is counted from the top of the stack, going to the bottom.
     */
    @Override
    public int removeFirstOccurrence(final KType e1)
    {
        return super.removeFirstOccurrence(e1);
    }

    /**
     * {@inheritDoc}
     * The last occurrence is counted from the bottom of the stack, going upwards to the top.
     */
    @Override
    public int removeLastOccurrence(final KType e1)
    {
        return super.removeLastOccurrence(e1);
    }

    /**
     * {@inheritDoc}
     * @return counted from the top of the stack, i.e = 0 if top, bottom is index = size() - 1
     */
    @Override
    public int indexOf(final KType e1)
    {
        //reverse logic
        int res = super.lastIndexOf(e1);

        if (res != -1)
        {
            res = elementsCount - res - 1;
        }

        return res;
    }

    @Override
    public int lastIndexOf(final KType e1)
    {
        //reverse logic
        int res = super.indexOf(e1);

        if (res != -1)
        {
            res = elementsCount - res - 1;
        }

        return res;
    }

    /**
     * {@inheritDoc}
     * @return  Returns the target argument for chaining, built from top of the stack to bottom.
     */
    @Override
    public KType[] toArray(final KType[] target)
    {
        final int size = elementsCount;

        //copy the buffer backwards.
        for (int i = 0; i < size; i++)
        {
            target[i] = buffer[size - i - 1];
        }

        return target;
    }

    /**
     * Adds one KType to the stack.
     */
    public void push(final KType e1)
    {
        ensureBufferSpace(1);
        buffer[elementsCount++] = e1;
    }

    /**
     * Adds two KTypes to the stack.
     */
    public void push(final KType e1, final KType e2)
    {
        ensureBufferSpace(2);
        buffer[elementsCount++] = e1;
        buffer[elementsCount++] = e2;
    }

    /**
     * Adds three KTypes to the stack.
     */
    public void push(final KType e1, final KType e2, final KType e3)
    {
        ensureBufferSpace(3);
        buffer[elementsCount++] = e1;
        buffer[elementsCount++] = e2;
        buffer[elementsCount++] = e3;
    }

    /**
     * Adds four KTypes to the stack.
     */
    public void push(final KType e1, final KType e2, final KType e3, final KType e4)
    {
        ensureBufferSpace(4);
        buffer[elementsCount++] = e1;
        buffer[elementsCount++] = e2;
        buffer[elementsCount++] = e3;
        buffer[elementsCount++] = e4;
    }

    /**
     * Add a range of array elements to the stack.
     */
    public void push(final KType[] elements, final int start, final int len)
    {
        assert start >= 0 && len >= 0;

        ensureBufferSpace(len);
        System.arraycopy(elements, start, buffer, elementsCount, len);
        elementsCount += len;
    }

    /**
     * Vararg-signature method for pushing elements at the top of the stack.
     * <p><b>This method is handy, but costly if used in tight loops (anonymous
     * array passing)</b></p>
     */
    public void push(final KType... elements)
    {
        push(elements, 0, elements.length);
    }

    /**
     * Pushes all elements from another container to the top of the stack.
     */
    public int pushAll(final KTypeContainer<? extends KType> container)
    {
        return addAll(container);
    }

    /**
     * Pushes all elements from another iterable to the top of the stack.
     */
    public int pushAll(final Iterable<? extends KTypeCursor<? extends KType>> iterable)
    {
        return addAll(iterable);
    }

    /**
     * Discard an arbitrary number of elements from the top of the stack.
     */
    public void discard(final int count)
    {
        assert elementsCount >= count;

        elementsCount -= count;
        /*! #if ($TemplateOptions.KTypeGeneric) !*/
        Internals.blankObjectArray(buffer, elementsCount, elementsCount + count);
        /*! #end !*/
    }

    /**
     * Discard the top element from the stack.
     */
    public void discard()
    {
        assert elementsCount > 0;

        elementsCount--;
        /* #if ($TemplateOptions.KTypeGeneric) */
        buffer[elementsCount] = null;
        /* #end */
    }

    /**
     * Remove the top element from the stack and return it.
     */
    public KType pop()
    {
        assert elementsCount > 0;

        final KType v = buffer[--elementsCount];
        /* #if ($TemplateOptions.KTypeGeneric) */
        buffer[elementsCount] = null;
        /* #end */
        return v;
    }

    /**
     * Peek at the top element on the stack.
     */
    public KType peek()
    {
        assert elementsCount > 0;

        return buffer[elementsCount - 1];
    }

    /**
     * Returns a new object of this class with no need to declare generic type (shortcut
     * instead of using a constructor).
     */
    public static <KType> KTypeStack<KType> newInstance()
    {
        return new KTypeStack<KType>();
    }

    /**
     * Returns a new object of this list with no need to declare generic type (shortcut
     * instead of using a constructor).
     */
    public static <KType> KTypeStack<KType> newInstanceWithCapacity(final int initialCapacity)
    {
        return new KTypeStack<KType>(initialCapacity);
    }

    /**
     * Create a stack by pushing a variable number of arguments to it.
     */
    public static <KType> KTypeStack<KType> from(final KType... elements)
    {
        final KTypeStack<KType> stack = new KTypeStack<KType>(elements.length);
        stack.push(elements);
        return stack;
    }

    /**
     * Create a stack by pushing all elements of another container to it.
     */
    public static <KType> KTypeStack<KType> from(final KTypeContainer<KType> container)
    {
        return new KTypeStack<KType>(container);
    }

    /**
     * Sort the stack from [beginIndex, endIndex[
     * by natural ordering (smaller first, from top to bottom of stack)
     * @param beginIndex
     * @param endIndex
     */
    /*! #if ($TemplateOptions.KTypePrimitive)
    public void sort(int beginIndex, int endIndex)
    {
        assert endIndex <= elementsCount;

        if (endIndex - beginIndex > 1)
        {
            //take care of ordering the right range : [startSortingRange, endSortingRange[

            final int size = size();

            final int startSortingRange = size - endIndex;
            final int endSortingRange = size - beginIndex;

            KTypeSort.quicksort(buffer, startSortingRange, endSortingRange);

            //reverse [startSortingRange, endSortingRange [
            KType tmpValue;

            final int halfSize = (endSortingRange - startSortingRange) / 2;

            for (int i = 0; i < halfSize; i++)
            {
                tmpValue = this.buffer[i + startSortingRange];
                this.buffer[i + startSortingRange] = this.buffer[endSortingRange - i - 1];
                this.buffer[endSortingRange - i - 1] = tmpValue;
            }
        }
    }
    #end !*/

    /**
     * Sort the whole stack by natural ordering (smaller first, from top to bottom of stack)
     * <p><b>
     * This routine uses Dual-pivot Quicksort, from [Yaroslavskiy 2009]
     * </b></p>
     * @param beginIndex
     * @param endIndex
     */
    /*! #if ($TemplateOptions.KTypePrimitive)
    public void sort()
    {
        //sort full
        super.sort();

        //reverse all elements
        final int size = size();
        final int halfSize = size / 2;

        KType tmpValue;

        for (int i = 0; i < halfSize; i++)
        {
            tmpValue = this.buffer[i];
            this.buffer[i] = this.buffer[size - i - 1];
            this.buffer[size - i - 1] = tmpValue;
        }
    }
    #end !*/

    /**
     * Sort the stack of <code>KType</code>s from [beginIndex, endIndex[
     * where [beginIndex, endIndex[ is counted from the top of the stack, i.e top is = index 0, bottom is endIndex[. That way,
     * the smallest elements are at the top of the stack.
     * It uses a #if ($TemplateOptions.KTypeGeneric) <code>Comparator</code> #else <code>KTypeComparator<? super KType></code> #end
     * <p><b>
     * This routine uses Dual-pivot Quicksort, from [Yaroslavskiy 2009] #if ($TemplateOptions.KTypeGeneric), so is NOT stable. #end
     * </b></p>
     */
    @Override
    public void sort(
            final int beginIndex, final int endIndex,
            /*! #if ($TemplateOptions.KTypeGeneric) !*/
            final Comparator<? super KType>
            /*! #else
            KTypeComparator<? super KType>
            #end !*/
            comp)
    {
        assert endIndex <= elementsCount;

        if (endIndex - beginIndex > 1)
        {
            //take care of ordering the right range : [startSortingRange, endSortingRange[

            final int size = size();

            final int startSortingRange = size - endIndex;
            final int endSortingRange = size - beginIndex;

            KTypeSort.quicksort(buffer, startSortingRange, endSortingRange, comp);

            //reverse [startSortingRange, endSortingRange [
            KType tmpValue;

            final int halfSize = (endSortingRange - startSortingRange) / 2;

            for (int i = 0; i < halfSize; i++)
            {
                tmpValue = this.buffer[i + startSortingRange];
                this.buffer[i + startSortingRange] = this.buffer[endSortingRange - i - 1];
                this.buffer[endSortingRange - i - 1] = tmpValue;
            }
        }
    }

    /**
     * Sort by dual-pivot quicksort an entire stack of <code>KType</code>s, the way
     * the smallest elements are at the top of the stack.
     * It uses a #if ($TemplateOptions.KTypeGeneric) <code>Comparator</code> #else <code>KTypeComparator<? super KType></code> #end
     * <p><b>
     * This routine uses Dual-pivot Quicksort, from [Yaroslavskiy 2009] #if ($TemplateOptions.KTypeGeneric), so is NOT stable. #end
     * </b></p>
     */
    @Override
    public void sort(
            /*! #if ($TemplateOptions.KTypeGeneric) !*/
            final Comparator<? super KType>
            /*! #else
            KTypeComparator<? super KType>
            #end !*/
            comp)
    {
        //sort full
        super.sort(comp);

        //reverse all elements
        final int size = size();
        final int halfSize = size / 2;

        KType tmpValue;

        for (int i = 0; i < halfSize; i++)
        {
            tmpValue = this.buffer[i];
            this.buffer[i] = this.buffer[size - i - 1];
            this.buffer[size - i - 1] = tmpValue;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public KTypeStack<KType> clone()
    {
        final KTypeStack<KType> cloned = new KTypeStack<KType>(this.buffer.length, this.resizer);

        cloned.defaultValue = this.defaultValue;

        //in order by construction
        cloned.addAll(this);

        return cloned;
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

            if (obj instanceof KTypeIndexedContainer<?>)
            {
                final KTypeIndexedContainer<?> other = (KTypeIndexedContainer<?>) obj;

                return other.size() == this.size() &&
                        allIndexesEqual(this, (KTypeIndexedContainer<KType>) other, this.size());
            }
        }
        return false;
    }
}
