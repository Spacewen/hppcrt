package com.carrotsearch.hppc.sorting;

import java.util.Comparator;

import com.carrotsearch.hppc.Intrinsics;

/**
 * Utility class for sorting algorithms of <code>KType</code>s arrays.
 */
/*! ${TemplateOptions.generatedAnnotation} !*/
public final class KTypeSort
{
    /**
     * Minimum window length to apply insertion sort in quick sort.
     */
    private static final int MIN_LENGTH_FOR_INSERTION_SORT_IN_QSORT = 17;

    private static final int DIST_SIZE_DUALQSORT = 13;

    private KTypeSort()
    {
        // Utility class, nothing to do
    }

    /**
     * Sort by  dual-pivot quicksort an array of naturally comparable <code>KType</code>s from [beginIndex, endIndex[
     * <p>
     * <b>
     * This routine uses Dual-pivot Quicksort, from [Yaroslavskiy 2009] #if ($TemplateOptions.KTypeGeneric), so is NOT stable. #end
     * </b>
     * </p>
     * @param table
     * @param beginIndex
     * @param endIndex
     */
    public static/*! #if ($TemplateOptions.KTypeGeneric) !*/<KType extends Comparable<? super KType>> /*! #end !*/void quicksort(KType[] table, int beginIndex, int endIndex)
    {
        if (endIndex - beginIndex > 1)
        {
            dualPivotQuicksort(table, beginIndex, endIndex - 1);
        }
    }

    /**
     * Sort by  dual-pivot quicksort a entire array of of naturally Comparable <code>KType</code>s
     * <p>
     * <b>
     * This routine uses Dual-pivot Quicksort, from [Yaroslavskiy 2009] #if ($TemplateOptions.KTypeGeneric), so is NOT stable. #end
     * </b>
     * </p>
     * @param table
     */
    public static/*! #if ($TemplateOptions.KTypeGeneric) !*/<KType extends Comparable<? super KType>> /*! #end !*/void quicksort(KType[] table)
    {
        if (table.length > 1)
        {
            dualPivotQuicksort(table, 0, table.length - 1);
        }
    }

    ////////////////////////////
    /**
     * Sort by  dual-pivot quicksort an array of <code>KType</code>s from [beginIndex, endIndex[
     * using a #if ($TemplateOptions.KTypeGeneric) <code>Comparator</code> #else <code>KTypeComparator<KType></code> #end
     * <p><b>
     * This routine uses Dual-pivot Quicksort, from [Yaroslavskiy 2009] #if ($TemplateOptions.KTypeGeneric), so is NOT stable. #end
     * </b></p>
     */
    public static/*! #if ($TemplateOptions.KTypeGeneric) !*/<KType> /*! #end !*/void quicksort(
            KType[] table, int beginIndex, int endIndex,
            /*! #if ($TemplateOptions.KTypeGeneric) !*/
            Comparator<? super KType>
            /*! #else
            KTypeComparator<? super KType>
             #end !*/
            comp)
    {

        if (endIndex - beginIndex > 1)
        {
            dualPivotQuicksort(table, beginIndex, endIndex - 1, comp);
        }
    }

    /**
     * Sort by  dual-pivot quicksort an entire array of <code>KType</code>s
     * using a #if ($TemplateOptions.KTypeGeneric) <code>Comparator</code> #else <code>KTypeComparator<KType></code> #end
     * <p><b>
     * This routine uses Dual-pivot Quicksort, from [Yaroslavskiy 2009] #if ($TemplateOptions.KTypeGeneric), so is NOT stable. #end
     * </b></p>
     */
    public static/*! #if ($TemplateOptions.KTypeGeneric) !*/<KType> /*! #end !*/void quicksort(KType[] table,
            /*! #if ($TemplateOptions.KTypeGeneric) !*/
            Comparator<? super KType>
    /*! #else
                    KTypeComparator<? super KType>
                    #end !*/
    comp)
    {

        if (table.length > 1)
        {
            dualPivotQuicksort(table, 0, table.length - 1, comp);
        }
    }

    /**
     * Private recursive sort method, [left, right] inclusive for Comparable objects
     * or natural ordering for primitives.
     * @param a
     * @param left
     * @param right
     */
    private static/*! #if ($TemplateOptions.KTypeGeneric) !*/<KType extends Comparable<? super KType>> /*! #end !*/void dualPivotQuicksort(KType[] a, int left, int right)
    {
        int len = right - left;

        KType x;

        //insertion sort
        //to prevent too-big recursion, swap to insertion sort below a certain size
        if (len < MIN_LENGTH_FOR_INSERTION_SORT_IN_QSORT)
        { // insertion sort on tiny array
            for (int i = left + 1; i <= right; i++)
            {
                for (int j = i; j > left && Intrinsics.isCompInfKType(a[j], a[j - 1]); j--)
                {
                    x = a[j - 1];
                    a[j - 1] = a[j];
                    a[j] = x;
                }
            }
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
        if (Intrinsics.isCompSupKType(a[m1], a[m2]) /* a[m1] > a[m2]*/)
        {
            x = a[m1];
            a[m1] = a[m2];
            a[m2] = x;
        }
        if (Intrinsics.isCompSupKType(a[m4], a[m5]) /* a[m4] > a[m5]*/)
        {
            x = a[m4];
            a[m4] = a[m5];
            a[m5] = x;
        }
        if (Intrinsics.isCompSupKType(a[m1], a[m3]) /*a[m1] > a[m3]*/)
        {
            x = a[m1];
            a[m1] = a[m3];
            a[m3] = x;
        }
        if (Intrinsics.isCompSupKType(a[m2], a[m3]) /* a[m2] > a[m3]*/)
        {
            x = a[m2];
            a[m2] = a[m3];
            a[m3] = x;
        }
        if (Intrinsics.isCompSupKType(a[m1], a[m4]) /* a[m1] > a[m4]*/)
        {
            x = a[m1];
            a[m1] = a[m4];
            a[m4] = x;
        }
        if (Intrinsics.isCompSupKType(a[m3], a[m4]) /*a[m3] > a[m4]*/)
        {
            x = a[m3];
            a[m3] = a[m4];
            a[m4] = x;
        }
        if (Intrinsics.isCompSupKType(a[m2], a[m5]) /* a[m2] > a[m5]*/)
        {
            x = a[m2];
            a[m2] = a[m5];
            a[m5] = x;
        }
        if (Intrinsics.isCompSupKType(a[m2], a[m3]) /*a[m2] > a[m3]*/)
        {
            x = a[m2];
            a[m2] = a[m3];
            a[m3] = x;
        }
        if (Intrinsics.isCompSupKType(a[m4], a[m5]) /* a[m4] > a[m5]*/)
        {
            x = a[m4];
            a[m4] = a[m5];
            a[m5] = x;
        }

        // pivots: [ < pivot1 | pivot1 <= && <= pivot2 | > pivot2 ]
        KType pivot1 = a[m2];
        KType pivot2 = a[m4];

        boolean diffPivots = !Intrinsics.isCompEqualKType(pivot1, pivot2);

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

                if (Intrinsics.isCompInfKType(x, pivot1)/* x < pivot1 */)
                {
                    a[k] = a[less];
                    a[less++] = x;
                }
                else if (Intrinsics.isCompSupKType(x, pivot2) /* x > pivot2 */)
                {
                    while (Intrinsics.isCompSupKType(a[great], pivot2) /* a[great] > pivot2 */&& k < great)
                    {
                        great--;
                    }
                    a[k] = a[great];
                    a[great--] = x;
                    x = a[k];

                    if (Intrinsics.isCompInfKType(x, pivot1) /*x < pivot1*/)
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
                if (Intrinsics.isCompEqualKType(x, pivot1) /*x == pivot1*/)
                {
                    continue;
                }
                if (Intrinsics.isCompInfKType(x, pivot1) /* x < pivot1 */)
                {
                    a[k] = a[less];
                    a[less++] = x;
                }
                else
                {
                    while (Intrinsics.isCompSupKType(a[great], pivot2) /* a[great] > pivot2*/&& k < great)
                    {
                        great--;
                    }
                    a[k] = a[great];
                    a[great--] = x;
                    x = a[k];

                    if (Intrinsics.isCompInfKType(x, pivot1) /*x < pivot1*/)
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
        dualPivotQuicksort(a, left, less - 2);
        dualPivotQuicksort(a, great + 2, right);

        // equal elements
        if (great - less > len - DIST_SIZE_DUALQSORT && diffPivots)
        {
            for (int k = less; k <= great; k++)
            {
                x = a[k];
                if (Intrinsics.isCompEqualKType(x, pivot1) /*x == pivot1*/)
                {
                    a[k] = a[less];
                    a[less++] = x;
                }
                else if (Intrinsics.isCompEqualKType(x, pivot2) /*x == pivot2*/)
                {
                    a[k] = a[great];
                    a[great--] = x;
                    x = a[k];

                    if (Intrinsics.isCompEqualKType(x, pivot1) /*x == pivot1*/)
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
            dualPivotQuicksort(a, less, great);
        }
    }

    /**
     * Private recursive sort method, [left, right] inclusive using KTypeComparator
     * for comparison.
     * @param a
     * @param left
     * @param right
     */
    private static/*! #if ($TemplateOptions.KTypeGeneric) !*/<KType> /*! #end !*/void dualPivotQuicksort(
            KType[] a,
            int left, int right,
            /*! #if ($TemplateOptions.KTypeGeneric) !*/
            Comparator<? super KType>
            /*! #else
            KTypeComparator<? super KType>
             #end !*/
            comp)
    {
        int len = right - left;

        KType x;

        //insertion sort
        //to prevent too-big recursion, swap to insertion sort below a certain size
        if (len < MIN_LENGTH_FOR_INSERTION_SORT_IN_QSORT)
        { // insertion sort on tiny array
            for (int i = left + 1; i <= right; i++)
            {
                for (int j = i; j > left && comp.compare(a[j], a[j - 1]) < 0; j--)
                {
                    x = a[j - 1];
                    a[j - 1] = a[j];
                    a[j] = x;
                }
            }
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
        KType pivot1 = a[m2];
        KType pivot2 = a[m4];

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
