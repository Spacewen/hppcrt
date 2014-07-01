package com.carrotsearch.hppc.caliper;

import static com.carrotsearch.hppc.caliper.Util.prepareData;

import com.carrotsearch.hppc.XorShiftRandom;
import com.google.caliper.Param;
import com.google.caliper.Runner;
import com.google.caliper.SimpleBenchmark;

/**
 * Benchmark putting a given number of integers into a hashmap.
 */
public class BenchmarkPut extends SimpleBenchmark
{
    /* Prepare some test data */
    public int[] keys;

    public enum Distribution
    {
        RANDOM, LINEAR, DECREMENT_LINEAR, HIGHBITS;
    }

    @Param
    public Distribution distribution;

    @Param
    public Implementations implementation;

    public MapImplementation<?> impl;

    @Param(
    {
                "2000000"
    })
    public int size;

    /*
     * 
     */
    @Override
    protected void setUp() throws Exception
    {
        // Our tested implementation, uses preallocation
        impl = implementation.getInstance(size);

        switch (distribution)
        {
            case RANDOM:
                keys = Util.prepareData(size, new XorShiftRandom(0x11223344));
                break;
            case LINEAR:
                keys = Util.prepareLinear(size);
                break;
            case HIGHBITS:
                keys = Util.prepareHighbits(size);
                break;
            case DECREMENT_LINEAR:
                keys = Util.prepareLinearDecrement(size);
                break;
            default:
                throw new RuntimeException();
        }
    }

    /**
     * Time the 'put' operation.
     */
    public int timePut(final int reps)
    {
        int count = 0;
        for (int i = 0; i < reps; i++)
        {
            impl.clear();
            count += impl.putAll(keys, keys);
        }
        return count;
    }

    public static void main(final String[] args)
    {
        Runner.main(BenchmarkPut.class, args);
    }
}