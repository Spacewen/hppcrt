package com.carrotsearch.hppc.sorting;

import java.util.Comparator;

/**
 * Sorting routines that return an array of sorted indices implied by a given comparator
 * rather than move elements of whatever the comparator is using for comparisons.
 * <p>
 * A practical use case for this class is when the index of an array is meaningful and one
 * wants to acquire the order of values in that array. None of the methods in Java
 * Collections would provide such functionality directly and creating a collection of
 * boxed {@link Integer} objects for indices seems to be too costly.
 */
public final class IndirectSort
{
    /**
     * Minimum window length to apply insertion sort in merge sort.
     */
    private static final int MIN_LENGTH_FOR_INSERTION_SORT = 30;

    /**
     * Minimum window length to apply insertion sort in quick sort.
     */
    private static final int MIN_LENGTH_FOR_INSERTION_SORT_IN_QSORT = 17;

    private static final int DIST_SIZE_DUALQSORT = 13;

    /**
     * No instantiation.
     */
    private IndirectSort()
    {
        // No instantiation.
    }

    /**
     * Returns the order of elements between indices <code>start</code> and
     * <code>start + length</code> excluded, as indicated by the given <code>comparator</code>.
     * <p>
     * This routine uses merge sort. It is guaranteed to be stable.
     * Take note this method generate temporaries.
     * </p>
     */
    public static int [] mergesort(int start, int length, IndirectComparator comparator)
    {
        int[] sorted = new int[length];
        mergesort(start, length, comparator, new int[length], sorted);
        return sorted;
    }

    /**
     * Returns the order of elements between indices <code>start</code> and
     * <code>start + length</code> excluded, as indicated by the given <code>comparator</code>.
     * This method entirely work by using pre-existing arrays, so is fitting for realtime.
     * <p>
     * This routine uses merge sort. It is guaranteed to be stable.
     * </p>
     * @param tmpArray : a temporary array for usage in intermediate computation, its size must be >= length
     * @param sorted : the sorted indices result. The array must be at least of size length,
     * and the real validity range is [0; length[
     */
    public static void mergesort(int start, int length, IndirectComparator comparator, int[] tmpArray, int[] sorted)
    {
        assert length <= sorted.length;

        //build an array of indices
        fillOrderArray(start, length, tmpArray);
        System.arraycopy(tmpArray, 0, sorted, 0, length);

        if (length > 1)
        {
            topDownMergeSort(tmpArray, sorted, 0, length, comparator);
        }
    }

    /**
     * Returns the order of elements between indices <code>start</code> and
     * <code>start + length</code> excluded, as indicated by the given <code>comparator</code>.
     * This method entirely work in-place, so is fitting for realtime.
     * <p>
     * This routine uses Dual-pivot Quicksort, from [Yaroslavskiy 2009], so is NOT stable.
     * </p>
     * @param sorted : the sorted indices result. The array must be at least of size length,
     * and the real validity range is [0; length[
     */
    public static void quicksort(int start, int length, IndirectComparator comparator, int[] sorted)
    {
        assert length <= sorted.length;

        //build an array of indices
        fillOrderArray(start, length, sorted);

        if (length > 1)
        {
            dualPivotQuicksort(sorted, start, start + length - 1, comparator);
        }
    }

    /**
     * Returns the order of elements between indices <code>start</code> and
     * <code>length + start</code> excluded, as indicated by the given <code>comparator</code>. This method
     * is equivalent to calling {@link #mergesort(int, int, IndirectComparator)} with
     * {@link IndirectComparator.DelegatingComparator}.
     * <p>
     * This routine uses merge sort. It is guaranteed to be stable.
     * </p>
     * Take note this method generate temporaries.
     */
    public static <T> int [] mergesort(T [] input, int start, int length,
            Comparator<? super T> comparator)
    {
        return mergesort(start, length, new IndirectComparator.DelegatingComparator<T>(
                input, comparator));
    }

    /**
     * Perform a recursive, descending merge sort.
     * 
     * @param fromIndex inclusive
     * @param toIndex exclusive
     */
    private static void topDownMergeSort(int [] src, int [] dst, int fromIndex, int toIndex,
            IndirectComparator comp)
    {
        if (toIndex - fromIndex <= MIN_LENGTH_FOR_INSERTION_SORT)
        {
            insertionSort(fromIndex, toIndex - fromIndex, dst, comp);
            return;
        }

        final int mid = (fromIndex + toIndex) >>> 1;
        topDownMergeSort(dst, src, fromIndex, mid, comp);
        topDownMergeSort(dst, src, mid, toIndex, comp);

        /*
         * Both splits in of src are now sorted.
         */
        if (comp.compare(src[mid - 1], src[mid]) <= 0)
        {
            /*
             * If the lowest element in upper slice is larger than the highest element in
             * the lower slice, simply copy over, the data is fully sorted.
             */
            System.arraycopy(src, fromIndex, dst, fromIndex, toIndex - fromIndex);
        }
        else
        {
            /*
             * Run a manual merge.
             */
            for (int i = fromIndex, j = mid, k = fromIndex; k < toIndex; k++)
            {
                if (j == toIndex || (i < mid && comp.compare(src[i], src[j]) <= 0))
                {
                    dst[k] = src[i++];
                }
                else
                {
                    dst[k] = src[j++];
                }
            }
        }
    }

    /**
     * Internal insertion sort for <code>int</code>s.
     */
    private static void insertionSort(final int off, final int len, int [] order,
            IndirectComparator intComparator)
    {
        int v, j, t;

        for (int i = off + 1; i < off + len; i++)
        {
            v = order[i];

            j = i;

            while (j > off && intComparator.compare(t = order[j - 1], v) > 0)
            {
                order[j--] = t;
            }
            order[j] = v;
        }
    }

    /**
     * Fill an existing array to build an order array.
     * @param start
     * @param order
     */
    private static void fillOrderArray(final int start, final int length, int[] order)
    {
        for (int i = 0; i < length; i++)
        {
            order[i] = start + i;
        }
    }

    /**
     * Dual-pivot Quicksort, from [Yaroslavskiy 2009]  paper
     * applied on the indirectSort
     * @param a
     * @param left
     * @param right
     */
    private static void dualPivotQuicksort(int[] a, int left, int right, IndirectComparator comp)
    {
        int len = right - left;

        int x;

        //insertion sort
        //to prevent too-big recursion, swap to insertion sort below a certain size
        if (len <= MIN_LENGTH_FOR_INSERTION_SORT_IN_QSORT)
        {
            insertionSort(left, len + 1, a, comp);
            return;
        }

        // median indexes
        int sixth = len / 6;
        int m1 = left + sixth;
        int m2 = m1 + sixth;
        int m3 = m2 + sixth;
        int m4 = m3 + sixth;
        int m5 = m4 + sixth;

        // 5-element sorting network
        if (comp.compare(a[m1], a[m2]) > 0 /* a[m1] > a[m2]*/)
        {
            x = a[m1];
            a[m1] = a[m2];
            a[m2] = x;
        }
        if (comp.compare(a[m4], a[m5]) > 0 /* a[m4] > a[m5]*/)
        {
            x = a[m4];
            a[m4] = a[m5];
            a[m5] = x;
        }
        if (comp.compare(a[m1], a[m3]) > 0 /*a[m1] > a[m3]*/)
        {
            x = a[m1];
            a[m1] = a[m3];
            a[m3] = x;
        }
        if (comp.compare(a[m2], a[m3]) > 0 /* a[m2] > a[m3]*/)
        {
            x = a[m2];
            a[m2] = a[m3];
            a[m3] = x;
        }
        if (comp.compare(a[m1], a[m4]) > 0 /* a[m1] > a[m4]*/)
        {
            x = a[m1];
            a[m1] = a[m4];
            a[m4] = x;
        }
        if (comp.compare(a[m3], a[m4]) > 0 /*a[m3] > a[m4]*/)
        {
            x = a[m3];
            a[m3] = a[m4];
            a[m4] = x;
        }
        if (comp.compare(a[m2], a[m5]) > 0 /* a[m2] > a[m5]*/)
        {
            x = a[m2];
            a[m2] = a[m5];
            a[m5] = x;
        }
        if (comp.compare(a[m2], a[m3]) > 0 /*a[m2] > a[m3]*/)
        {
            x = a[m2];
            a[m2] = a[m3];
            a[m3] = x;
        }
        if (comp.compare(a[m4], a[m5]) > 0 /* a[m4] > a[m5]*/)
        {
            x = a[m4];
            a[m4] = a[m5];
            a[m5] = x;
        }

        // pivots: [ < pivot1 | pivot1 <= && <= pivot2 | > pivot2 ]
        int pivot1 = a[m2];
        int pivot2 = a[m4];

        boolean diffPivots = comp.compare(pivot1, pivot2) != 0;

        a[m2] = a[left];
        a[m4] = a[right];
        // center part pointers
        int less = left + 1;
        int great = right - 1;
        // sorting
        if (diffPivots)
        {
            for (int k = less; k <= great; k++)
            {
                x = a[k];

                if (comp.compare(x, pivot1) < 0/* x < pivot1 */)
                {
                    a[k] = a[less];
                    a[less++] = x;
                }
                else if (comp.compare(x, pivot2) > 0 /* x > pivot2 */)
                {
                    while (comp.compare(a[great], pivot2) > 0 /* a[great] > pivot2 */&& k < great)
                    {
                        great--;
                    }
                    a[k] = a[great];
                    a[great--] = x;
                    x = a[k];

                    if (comp.compare(x, pivot1) < 0 /*x < pivot1*/)
                    {
                        a[k] = a[less];
                        a[less++] = x;
                    }
                }
            }
        }
        else
        {
            for (int k = less; k <= great; k++)
            {
                x = a[k];
                if (comp.compare(x, pivot1) == 0 /*x == pivot1*/)
                {
                    continue;
                }
                if (comp.compare(x, pivot1) < 0 /* x < pivot1 */)
                {
                    a[k] = a[less];
                    a[less++] = x;
                }
                else
                {
                    while (comp.compare(a[great], pivot2) > 0 /* a[great] > pivot2*/&& k < great)
                    {
                        great--;
                    }
                    a[k] = a[great];
                    a[great--] = x;
                    x = a[k];

                    if (comp.compare(x, pivot1) < 0 /*x < pivot1*/)
                    {
                        a[k] = a[less];
                        a[less++] = x;
                    }
                }
            }
        }
        // swap
        a[left] = a[less - 1];
        a[less - 1] = pivot1;
        a[right] = a[great + 1];
        a[great + 1] = pivot2;
        // left and right parts
        dualPivotQuicksort(a, left, less - 2, comp);
        dualPivotQuicksort(a, great + 2, right, comp);

        // equal elements
        if (great - less > len - DIST_SIZE_DUALQSORT && diffPivots)
        {
            for (int k = less; k <= great; k++)
            {
                x = a[k];
                if (comp.compare(x, pivot1) == 0 /*x == pivot1*/)
                {
                    a[k] = a[less];
                    a[less++] = x;
                }
                else if (comp.compare(x, pivot2) == 0 /*x == pivot2*/)
                {
                    a[k] = a[great];
                    a[great--] = x;
                    x = a[k];

                    if (comp.compare(x, pivot1) == 0 /*x == pivot1*/)
                    {
                        a[k] = a[less];
                        a[less++] = x;
                    }
                }
            }
        }
        // center part
        if (diffPivots)
        {
            dualPivotQuicksort(a, less, great, comp);
        }
    }


}
