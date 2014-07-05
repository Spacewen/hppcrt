package com.carrotsearch.hppc.caliper;

import com.carrotsearch.hppc.DistributionGenerator;
import com.carrotsearch.hppc.IntIntOpenHashMap;
import com.carrotsearch.hppc.IntOpenHashSet;
import com.carrotsearch.hppc.XorShiftRandom;
import com.google.caliper.Param;
import com.google.caliper.Runner;
import com.google.caliper.SimpleBenchmark;

/**
 * Benchmark putting All of a hashed container in another.
 */
public class HashCollisionsCornerCaseTest extends SimpleBenchmark
{
    /* Prepare some test data */
    public IntOpenHashSet testSet;
    public IntIntOpenHashMap testMap;

    public IntOpenHashSet currentUnderTestSet;

    public IntOpenHashSet currentUnderTestSet2;

    public IntIntOpenHashMap currentUnderTestMap;

    public IntIntOpenHashMap currentUnderTestMap2;

    public enum Distribution
    {
        RANDOM, LINEAR, LINEAR_DECREMENT, HIGHBITS;
    }

    public enum Perturbation
    {
        NOT_PERTURBED, PERTURBED;
    }

    public enum Allocation
    {
        DEFAULT_SIZE, PREALLOCATED;
    }

    @Param
    public Allocation allocation;

    @Param
    public Perturbation perturbation;

    @Param
    public Distribution distribution;

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
        //Instead of this.size, fill up
        //prepare testSet to be filled up to their specified load factor.

        testSet = new IntOpenHashSet(this.size);
        testMap = new IntIntOpenHashMap(this.size);

        final DistributionGenerator gene = new DistributionGenerator(this.size, new XorShiftRandom(87955214455L));

        int nextValue = -1;

        while (testSet.size() < testSet.capacity()) {

            if (this.distribution == Distribution.RANDOM) {

                nextValue = gene.RANDOM.getNext();
            }
            else if (this.distribution == Distribution.LINEAR) {

                nextValue = gene.LINEAR.getNext();
            }
            else if (this.distribution == Distribution.HIGHBITS) {

                nextValue = gene.HIGHBITS.getNext();
            }
            else if (this.distribution == Distribution.LINEAR_DECREMENT) {

                nextValue = gene.LINEAR_DECREMENT.getNext();
            }

            testSet.add(nextValue);
            testMap.put(nextValue, ~nextValue);
        }

        //Preallocate of not the tested hash containers
        if (perturbation == Perturbation.PERTURBED) {

            if (allocation == Allocation.DEFAULT_SIZE) {

                currentUnderTestSet = IntOpenHashSet.newInstance();
                currentUnderTestSet2 = IntOpenHashSet.newInstance();
                currentUnderTestMap = IntIntOpenHashMap.newInstance();
                currentUnderTestMap2 = IntIntOpenHashMap.newInstance();
            }
            else if (allocation == Allocation.PREALLOCATED) {

                currentUnderTestSet = IntOpenHashSet.newInstanceWithCapacity(this.size, IntOpenHashSet.DEFAULT_LOAD_FACTOR);
                currentUnderTestSet2 = IntOpenHashSet.newInstanceWithCapacity(this.size, IntOpenHashSet.DEFAULT_LOAD_FACTOR);
                currentUnderTestMap = IntIntOpenHashMap.newInstance(this.size, IntIntOpenHashMap.DEFAULT_LOAD_FACTOR);
                currentUnderTestMap2 = IntIntOpenHashMap.newInstance(this.size, IntIntOpenHashMap.DEFAULT_LOAD_FACTOR);
            }
        }
        else if (perturbation == Perturbation.NOT_PERTURBED) {

            if (allocation == Allocation.DEFAULT_SIZE) {

                currentUnderTestSet = IntOpenHashSet.newInstanceWithoutPerturbations();
                currentUnderTestSet2 = IntOpenHashSet.newInstanceWithoutPerturbations();
                currentUnderTestMap = IntIntOpenHashMap.newInstanceWithoutPerturbations(IntIntOpenHashMap.DEFAULT_CAPACITY, IntIntOpenHashMap.DEFAULT_LOAD_FACTOR);
                currentUnderTestMap2 = IntIntOpenHashMap.newInstanceWithoutPerturbations(IntIntOpenHashMap.DEFAULT_CAPACITY, IntIntOpenHashMap.DEFAULT_LOAD_FACTOR);
            }
            else if (allocation == Allocation.PREALLOCATED) {
                currentUnderTestSet = IntOpenHashSet.newInstanceWithoutPerturbations(this.size, IntOpenHashSet.DEFAULT_LOAD_FACTOR);
                currentUnderTestSet2 = IntOpenHashSet.newInstanceWithoutPerturbations(this.size, IntOpenHashSet.DEFAULT_LOAD_FACTOR);
                currentUnderTestMap = IntIntOpenHashMap.newInstanceWithoutPerturbations(this.size, IntIntOpenHashMap.DEFAULT_LOAD_FACTOR);
                currentUnderTestMap2 = IntIntOpenHashMap.newInstanceWithoutPerturbations(this.size, IntIntOpenHashMap.DEFAULT_LOAD_FACTOR);
            }
        }
    }

    @Override
    protected void tearDown() throws Exception
    {
        this.testSet = null;
        this.testMap = null;
    }

    /**
     * Time the 'putAll' operation.
     */
    public int timeSet_AddAll(final int reps)
    {
        int count = 0;
        for (int i = 0; i < reps; i++)
        {
            this.currentUnderTestSet.clear();
            count += this.currentUnderTestSet.addAll(this.testSet);
        }
        return count;
    }

    /**
     * Time the 'putAll' operation.
     */
    public int timeSet_AddAll_Successive(final int reps)
    {
        int count = 0;
        for (int i = 0; i < reps; i++)
        {
            this.currentUnderTestSet.clear();
            this.currentUnderTestSet2.clear();
            count += this.currentUnderTestSet.addAll(this.testSet);
            count += this.currentUnderTestSet2.addAll(this.currentUnderTestSet);
        }
        return count;
    }

    /**
     * Time the 'putAll' operation.
     */
    public int timeMap_PutAll(final int reps)
    {
        int count = 0;
        for (int i = 0; i < reps; i++)
        {
            this.currentUnderTestMap.clear();
            count += this.currentUnderTestMap.putAll(this.testMap);
        }
        return count;
    }

    /**
     * Time the 'putAll' operation.
     */
    public int timeMap_PutAll_Successive(final int reps)
    {
        int count = 0;
        for (int i = 0; i < reps; i++)
        {
            this.currentUnderTestMap.clear();
            this.currentUnderTestMap2.clear();
            count += this.currentUnderTestMap.putAll(this.testMap);
            count += this.currentUnderTestMap2.putAll(this.currentUnderTestMap);
        }
        return count;
    }

    /**
     * Running main
     * @param args
     */
    public static void main(final String[] args)
    {
        Runner.main(HashCollisionsCornerCaseTest.class, args);
    }
}
