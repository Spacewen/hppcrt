package com.carrotsearch.hppc.caliper;

import com.carrotsearch.hppc.IntOpenHashSet;
import com.carrotsearch.hppc.XorShiftRandom;
import com.carrotsearch.hppc.caliper.BenchmarkPut.Distribution;
import com.google.caliper.Param;
import com.google.caliper.Runner;
import com.google.caliper.SimpleBenchmark;

/**
 * Benchmark perturbations vs. no-perturbations.
 */
public class BenchmarkPerturbedVsHashedOnly extends SimpleBenchmark
{

    public enum Perturbation
    {
        NOT_PERTURBED, PERTURBED;
    }

    @Param(
    {
                "2000000"
    })
    public int size;

    @Param
    public Perturbation perturbation;

    public enum Distribution
    {
        RANDOM, LINEAR, DECREMENT_LINEAR, HIGHBITS;
    }

    @Param
    public Distribution distribution;

    public IntOpenHashSet impl;

    /* Prepare some test data */
    public int[] keys;

    @Override
    protected void setUp() throws Exception
    {
        switch (perturbation)
        {
            case NOT_PERTURBED:
                // Our tested implementation, uses preallocation
                impl = IntOpenHashSet.newInstanceWithoutPerturbations(size, IntOpenHashSet.DEFAULT_LOAD_FACTOR);
                break;
            case PERTURBED:
                // Our tested implementation, uses preallocation
                impl = IntOpenHashSet.newInstanceWithCapacity(size, IntOpenHashSet.DEFAULT_LOAD_FACTOR);
                break;
            default:
                throw new RuntimeException();
        }

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
     * Time the 'add' operation.
     */
    public int timePerturbation(int reps)
    {
        int count = 0;
        while (reps-- > 0) {
            for (int i = 0; i < size; i++)
            {
                impl.add(keys[i]);
                count += keys[i];
            }
            count += impl.size();
            impl.clear();
        }
        return count;
    }

    public static void main(final String[] args)
    {
        Runner.main(BenchmarkPerturbedVsHashedOnly.class, args);
    }
}