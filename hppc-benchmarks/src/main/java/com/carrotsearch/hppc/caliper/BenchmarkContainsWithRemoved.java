package com.carrotsearch.hppc.caliper;

import static com.carrotsearch.hppc.Util.shuffle;

import java.util.Random;

import org.apache.mahout.math.Arrays;

import com.carrotsearch.hppc.Util;
import com.google.caliper.*;

/**
 * Create a large map of int keys, remove a fraction of the keys and query with half/half keys
 * and a some random values.
 */
public class BenchmarkContainsWithRemoved extends SimpleBenchmark
{
    /* Prepare some test data */
    public int[] keys;
    public int[] queryKeys;

    @Param(
            {
            "0", "0.25", "0.5", "0.75", "1"
            })
    public double removedKeys;

    @Param
    public Implementations implementation;

    public MapImplementation<?> impl;

    @Param(
            {
            "1000000"
            })
    public int size;

    @Override
    protected void setUp() throws Exception
    {
        final Random rnd = new Random(0x11223344);

        // Our tested implementation.
        this.impl = this.implementation.getInstance(this.size);

        // Random keys
        this.keys = Util.prepareData(this.size, rnd);

        // Half keys, half random. Shuffle order.
        this.queryKeys = Arrays.copyOf(this.keys, this.keys.length);
        for (int i = 0; i < this.queryKeys.length / 2; i++)
        {
            this.queryKeys[i] = rnd.nextInt();
        }

        Util.shuffle(this.queryKeys, rnd);

        // Fill with random keys.
        for (int i = 0; i < this.keys.length; i++)
        {
            this.impl.put(this.keys[i], 0);
        }

        // Shuffle keys and remove a fraction of them.
        final int[] randomized = Util.shuffle(Arrays.copyOf(this.keys, this.keys.length), rnd);
        int removeKeys = (int) (this.removedKeys * this.keys.length);
        for (int i = 0; removeKeys > 0; removeKeys--, i++)
        {
            this.impl.remove(randomized[i]);
        }
    }

    public int timeContains(final int reps)
    {
        int count = 0;
        for (int i = 0; i < reps; i++)
        {
            count += this.impl.containKeys(this.keys);
        }
        return count;
    }

    @Override
    protected void tearDown() throws Exception
    {
        this.impl = null;
    }

    public static void main(final String[] args)
    {
        Runner.main(BenchmarkContainsWithRemoved.class, args);
    }
}