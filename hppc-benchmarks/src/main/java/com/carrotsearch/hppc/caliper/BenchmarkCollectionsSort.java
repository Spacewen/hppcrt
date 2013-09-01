package com.carrotsearch.hppc.caliper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

import com.carrotsearch.hppc.sorting.IndirectComparator;
import com.carrotsearch.hppc.sorting.IndirectSort;
import com.google.caliper.Param;
import com.google.caliper.Runner;
import com.google.caliper.SimpleBenchmark;
import com.google.common.collect.Lists;

/**
 * Benchmark {@link Collections#sort(java.util.List)}, in particular for differences
 * in the new JDK 1.7 (TimSort). The point of this benchmark is to get the order
 * of elements (their indexes), not an array or collection of sorted elements (!).
 */
public class BenchmarkCollectionsSort extends SimpleBenchmark
{
    @Param("1000000")
    public int size;

    private ArrayList<String> data;

    //represent the initial index of data
    private int[] indexes;
    private int[] indexesClone;
    private Integer[] indexesInteger;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        this.data = Lists.newArrayList();
        this.indexes = new int[size];
        this.indexesClone = new int[size];
        this.indexesInteger = new Integer[size];

        for (int i = 0; i < size; i++)
        {
            data.add(Integer.toString(i));
            indexes[i] = i;
            indexesInteger[i] = i;
        }

        Collections.shuffle(data);
    }

    /*
     * 
     */
    public int timeIndirectMergeSort(int reps)
    {
        int count = 0;
        for (int i = 0; i < reps; i++)
        {
            final Object [] input = data.toArray();
            int [] ordered = IndirectSort.mergesort(input, 0, input.length, new Comparator<Object>()
                    {
                @SuppressWarnings({"unchecked", "rawtypes"})
                public int compare(Object o1, Object o2)
                {
                    return ((Comparable) o1).compareTo(o2);
                }
                    });

            count += ordered[0];
        }

        return count;
    }

    /*
     * 
     */
    public int timeLegacySort(int reps)
    {
        System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
        return timeNewSort(reps);
    }

    /*
     * 
     */
    public int timeNewSort(int reps)
    {
        final Integer[] indexesClone = indexesInteger.clone();

        int count = 0;
        for (int i = 0; i < reps; i++)
        {
            Arrays.sort(indexesClone, new Comparator<Integer>()
                    {
                final ArrayList<String> dta = data;

                @Override
                public int compare(Integer o1, Integer o2)
                {
                    return dta.get(o1.intValue()).compareTo(
                            dta.get(o2.intValue()));
                }
                    });
            count += indexesClone[0];
        }

        return count;
    }

    /*
     * 
     */
    public int timeIndirectQuickSort(int reps)
    {
        int count = 0;

        //retry reps times
        for (int i = 0; i < reps; i++)
        {
            //overwrite the same array, since it is modified in-place
            System.arraycopy(indexes, 0, indexesClone, 0, indexes.length);

            IndirectSort.quicksort(0, indexesClone.length, new IndirectComparator() {

                final ArrayList<String> dta = BenchmarkCollectionsSort.this.data;

                @Override
                public int compare(int o1, int o2)
                {
                    return dta.get(o1).compareTo(dta.get(o2));
                }
            }, indexes);

            count += indexesClone[0];
        } //end for

        return count;
    }

    public static void main(String [] args)
    {
        Runner.main(BenchmarkCollectionsSort.class, args);
    }
}
