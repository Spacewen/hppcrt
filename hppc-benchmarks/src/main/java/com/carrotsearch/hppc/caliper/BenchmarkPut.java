package com.carrotsearch.hppc.caliper;

import com.carrotsearch.hppc.DistributionGenerator;
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
                "3000000"
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

        final DistributionGenerator gene = new DistributionGenerator(this.size, new XorShiftRandom(0x11223344));

        switch (this.distribution)
        {
            case RANDOM:
                this.keys = gene.RANDOM.prepare(this.size);
                break;
            case LINEAR:
                this.keys = gene.LINEAR.prepare(this.size);
                break;
            case HIGHBITS:
                this.keys = gene.HIGHBITS.prepare(this.size);
                break;
            case LINEAR_DECREMENT:
                this.keys = gene.LINEAR_DECREMENT.prepare(this.size);
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