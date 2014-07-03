package com.carrotsearch.hppc.caliper;

import static com.carrotsearch.hppc.Util.prepareData;

import com.carrotsearch.hppc.Util;
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
        RANDOM, LINEAR, LINEAR_DECREMENT, HIGHBITS;
    }

    @Param
    public Distribution distribution;

    @Param
    public Implementations implementation;

    public MapImplementation<?> impl;

    @Param(
    {
                "1000000"
    })
    public int size;

    /*
     * 
     */
    @Override
    protected void setUp() throws Exception
    {
        // Our tested implementation, uses preallocation
        this.impl = this.implementation.getInstance(this.size);

        switch (this.distribution)
        {
            case RANDOM:
                this.keys = Util.prepareData(this.size, new XorShiftRandom(0x11223344));
                break;
            case LINEAR:
                this.keys = Util.prepareLinear(this.size);
                break;
            case HIGHBITS:
                this.keys = Util.prepareHighbits(this.size);
                break;
            case LINEAR_DECREMENT:
                this.keys = Util.prepareLinearDecrement(this.size);
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
            this.impl.clear();
            count += this.impl.putAll(this.keys, this.keys);
        }
        return count;
    }

    public static void main(final String[] args)
    {
        Runner.main(BenchmarkPut.class, args);
    }
}