package com.carrotsearch.hppc.jub;

import java.util.Random;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.carrotsearch.hppc.BitSet;
import com.carrotsearch.hppc.IntDoubleLinkedSet;
import com.carrotsearch.hppc.IntOpenHashSet;
import com.carrotsearch.junitbenchmarks.AbstractBenchmark;
import com.carrotsearch.junitbenchmarks.BenchmarkOptions;
import com.carrotsearch.junitbenchmarks.annotation.BenchmarkHistoryChart;
import com.carrotsearch.junitbenchmarks.annotation.BenchmarkMethodChart;

/**
 * Repeated cardinality calculation, very sparse data sets.
 */
@BenchmarkHistoryChart(filePrefix = "CLASSNAME.history", maxRuns = 50)
@BenchmarkMethodChart(filePrefix = "CLASSNAME.methods")
@BenchmarkOptions(callgc = false, warmupRounds = 2, benchmarkRounds = 10)
public class CardinalityBenchmark extends AbstractBenchmark
{
    private static final int WND = 100;
    private static int[] numbers = new int[50000];

    private static BitSet bitset = new BitSet();
    private static IntDoubleLinkedSet dlinked = new IntDoubleLinkedSet();
    private static IntOpenHashSet hset = new IntOpenHashSet();

    @SuppressWarnings("unused")
    private static volatile int guard;

    /* */
    @BeforeClass
    public static void prepare()
    {
        final int MAX_RANGE = 0xfffff;
        final Random rnd = new Random(0x11223344);
        for (int i = 0; i < CardinalityBenchmark.numbers.length; i++)
            CardinalityBenchmark.numbers[i] = Math.abs(rnd.nextInt()) & MAX_RANGE;
    }

    @AfterClass
    public static void cleanup()
    {
        CardinalityBenchmark.numbers = null;
        CardinalityBenchmark.dlinked = null;
        CardinalityBenchmark.bitset = null;
        CardinalityBenchmark.hset = null;
    }

    @Before
    public void roundCleanup()
    {
        CardinalityBenchmark.dlinked.clear();
        CardinalityBenchmark.bitset.clear();
        CardinalityBenchmark.hset.clear();
    }

    /**
     * Simple cardinality calculations, double-linked set (very sparse data).
     */
    @Test
    public void testCardinality_dlinked()
    {
        int card = 0;
        for (int i = 0; i < CardinalityBenchmark.numbers.length - CardinalityBenchmark.WND; i++)
        {
            CardinalityBenchmark.dlinked.clear();
            for (int j = 0; j < CardinalityBenchmark.WND; j++)
            {
                CardinalityBenchmark.dlinked.add(CardinalityBenchmark.numbers[i + j]);
            }
            card += CardinalityBenchmark.dlinked.size();
        }
        CardinalityBenchmark.guard = card;
    }

    /**
     * Simple cardinality calculations, hash set (very sparse data).
     */
    @Test
    public void testCardinality_hset()
    {
        int card = 0;
        for (int i = 0; i < CardinalityBenchmark.numbers.length - CardinalityBenchmark.WND; i++)
        {
            CardinalityBenchmark.hset.clear();
            for (int j = 0; j < CardinalityBenchmark.WND; j++)
            {
                CardinalityBenchmark.hset.add(CardinalityBenchmark.numbers[i + j]);
            }
            card += CardinalityBenchmark.hset.size();
        }
        CardinalityBenchmark.guard = card;
    }

    /**
     * Simple cardinality calculations, bitset (very sparse data).
     */
    @Test
    @BenchmarkOptions(callgc = false, warmupRounds = 1, benchmarkRounds = 2)
    public void testCardinality_bset()
    {
        int card = 0;
        for (int i = 0; i < CardinalityBenchmark.numbers.length - CardinalityBenchmark.WND; i++)
        {
            CardinalityBenchmark.bitset.clear();
            for (int j = 0; j < CardinalityBenchmark.WND; j++)
            {
                CardinalityBenchmark.bitset.set(CardinalityBenchmark.numbers[i + j]);
            }
            card += CardinalityBenchmark.bitset.cardinality();
        }
        CardinalityBenchmark.guard = card;
    }
}
