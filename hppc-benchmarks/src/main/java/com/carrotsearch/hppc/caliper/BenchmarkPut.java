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
        RANDOM, LINEAR, HIGHBITS;
    }

    @Param
    public Distribution distribution;

    @Param
    public Implementations implementation;

    @Param(
    {
                "5000000"
    })
    public int size;

    /*
     * 
     */
    @Override
    protected void setUp() throws Exception
    {
        switch (distribution)
        {
            case RANDOM:
                keys = Util.prepareData(size, new XorShiftRandom(0x11223344));
                break;
            case LINEAR:
                keys = prepareLinear(size);
                break;
            case HIGHBITS:
                keys = prepareHighbits(size);
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
            final MapImplementation<?> impl = implementation.getInstance();
            count += impl.putAll(keys, keys);
        }
        return count;
    }

    /**
     * Linear increment by 1.
     */
    private int[] prepareLinear(final int size)
    {
        final int[] t = new int[size];
        for (int i = 0; i < size; i++)
            t[i] = i * 2;
        return t;
    }

    /**
     * Linear increments on 12 high bits first, then on lower bits.
     */
    private int[] prepareHighbits(final int size)
    {
        final int[] t = new int[size];
        for (int i = 0; i < size; i++)
            t[i] = (i << (32 - 12)) | (i >>> 12);
        return t;
    }

    public static void main(final String[] args)
    {
        Runner.main(BenchmarkPut.class, args);
    }
}