package com.carrotsearch.hppc.caliper;

import com.carrotsearch.hppc.DistributionGenerator;
import com.carrotsearch.hppc.IntOpenHashSet;
import com.carrotsearch.hppc.XorShiftRandom;
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
                "3000000"
    })
    public int size;

    @Param
    public Perturbation perturbation;

    public enum Distribution
    {
        RANDOM, LINEAR, LINEAR_DECREMENT, HIGHBITS;
    }

    @Param
    public Distribution distribution;

    public IntOpenHashSet impl;

    /* Prepare some test data */
    public int[] keys;

    @Override
    protected void setUp() throws Exception
    {
        final DistributionGenerator gene = new DistributionGenerator(this.size, new XorShiftRandom(0x11223344));

        switch (this.perturbation)
        {
            case NOT_PERTURBED:
                // Our tested implementation, uses preallocation
                this.impl = IntOpenHashSet.newInstanceWithoutPerturbations(this.size, IntOpenHashSet.DEFAULT_LOAD_FACTOR);
                break;
            case PERTURBED:
                // Our tested implementation, uses preallocation
                this.impl = IntOpenHashSet.newInstanceWithCapacity(this.size, IntOpenHashSet.DEFAULT_LOAD_FACTOR);
                break;
            default:
                throw new RuntimeException();
        }

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
     * Time the 'add' operation.
     */
    public int timePerturbation(int reps)
    {
        int count = 0;
        while (reps-- > 0)
        {
            for (int i = 0; i < this.size; i++)
            {
                this.impl.add(this.keys[i]);
            }
            count += this.impl.size();
            this.impl.clear();
        }
        return count;
    }

    public static void main(final String[] args)
    {
        Runner.main(BenchmarkPerturbedVsHashedOnly.class, args);
    }
}