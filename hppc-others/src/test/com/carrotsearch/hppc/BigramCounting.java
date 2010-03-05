package com.carrotsearch.hppc;

import gnu.trove.map.hash.TIntIntHashMap;
import it.unimi.dsi.fastutil.ints.*;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.junit.*;
import org.junit.rules.MethodRule;

import bak.pcj.map.IntKeyIntChainedHashMap;
import bak.pcj.map.IntKeyIntOpenHashMap;

import com.carrotsearch.junitbenchmarks.*;
import com.carrotsearch.junitbenchmarks.h2.*;

@BenchmarkHistoryChart(filePrefix = "CLASSNAME-history", maxRuns = 10)
@BenchmarkMethodChart(filePrefix = "CLASSNAME-methods")
@BenchmarkOptions(warmupRounds = 5, benchmarkRounds = 5)
public class BigramCounting
{
    private static H2Consumer consumer = new H2Consumer(new File(".mapadditions"));

    @Rule
    public MethodRule runBenchmarks = new BenchmarkRule(consumer, new WriterConsumer());

    /* Prepare some test data */
    private static char [] DATA;

    /* Prevent dead code removal. */
    @SuppressWarnings("unused")
    private volatile int guard;

    @BeforeClass
    public static void prepareData() throws IOException
    {
        byte [] dta = IOUtils.toByteArray(
            Thread.currentThread().getContextClassLoader().getResourceAsStream("books-polish.txt"));
        DATA = new String(dta, "UTF-8").toCharArray();
    }

    @AfterClass
    public static void cleanup()
    {
        consumer.close();
    }

    @Test
    public void trove()
    {
        final char [] CHARS = DATA;
        final TIntIntHashMap map = new TIntIntHashMap();

        for (int i = 0; i < CHARS.length - 1; i++)
        {
            final int bigram = CHARS[i] << 16 | CHARS[i+1];
            map.put(bigram, map.get(bigram) + 1);
        }

        guard = map.size();
    }

    @Test
    public void fastutilOpenHashMap()
    {
        final char [] CHARS = DATA;
        final Int2IntOpenHashMap map = new Int2IntOpenHashMap(); 
        map.defaultReturnValue(0);

        for (int i = 0; i < CHARS.length - 1; i++)
        {
            final int bigram = CHARS[i] << 16 | CHARS[i+1];
            map.put(bigram, map.get(bigram) + 1);
        }

        guard = map.size();
    }

    @Test
    public void fastutilLinkedOpenHashMap()
    {
        final char [] CHARS = DATA;
        final Int2IntLinkedOpenHashMap map = new Int2IntLinkedOpenHashMap(); 
        map.defaultReturnValue(0);

        for (int i = 0; i < CHARS.length - 1; i++)
        {
            final int bigram = CHARS[i] << 16 | CHARS[i+1];
            map.put(bigram, map.get(bigram) + 1);
        }

        guard = map.size();
    }

    @Test
    public void pcjOpenHashMap()
    {
        final char [] CHARS = DATA;
        final IntKeyIntOpenHashMap map = new IntKeyIntOpenHashMap();

        for (int i = 0; i < CHARS.length - 1; i++)
        {
            final int bigram = CHARS[i] << 16 | CHARS[i+1];
            map.put(bigram, map.get(bigram) + 1);
        }

        guard = map.size();
    }

    @Test
    public void pcjChainedHashMap()
    {
        final char [] CHARS = DATA;
        final IntKeyIntChainedHashMap map = new IntKeyIntChainedHashMap();

        for (int i = 0; i < CHARS.length - 1; i++)
        {
            final int bigram = CHARS[i] << 16 | CHARS[i+1];
            map.put(bigram, map.get(bigram) + 1);
        }

        guard = map.size();
    }

    @Test
    public void hppc()
    {
        // [[[start:bigram-counting]]]
        // Some character data
        final char [] CHARS = DATA;
        
        // We'll use a int -> int map for counting. A bigram can be encoded
        // as an int by shifting one of the bigram's characters by 16 bits
        // and then ORing the other character to form a 32-bit int.
        final IntIntOpenHashMap map = new IntIntOpenHashMap();
        for (int i = 0; i < CHARS.length - 1; i++)
        {
            final int bigram = CHARS[i] << 16 | CHARS[i+1];
            map.putOrAdd(bigram, 1, 1);
        }
        // [[[end:bigram-counting]]]
        
        guard = map.size();
    }

    @Test
    public void jcf()
    {
        final char [] CHARS = DATA;
        final Map<Integer, Integer> map = new HashMap<Integer, Integer>();
        for (int i = 0; i < CHARS.length - 1; i++)
        {
            final int bigram = CHARS[i] << 16 | CHARS[i+1];
            final Integer currentCount = map.get(bigram);
            if (currentCount == null)
            {
                map.put(bigram, 1);
            }
            else
            {
                map.put(bigram, currentCount + 1);
            }
        }
        
        guard = map.size();
    }

    @Test
    public void jcfWithHolder()
    {
        final char [] CHARS = DATA;
        final Map<Integer, IntHolder> map = new HashMap<Integer, IntHolder>();
        for (int i = 0; i < CHARS.length - 1; i++)
        {
            final int bigram = CHARS[i] << 16 | CHARS[i+1];
            final IntHolder currentCount = map.get(bigram);
            if (currentCount == null)
            {
                map.put(bigram, new IntHolder(1));
            }
            else
            {
                currentCount.value++;
            }
        }

        guard = map.size();
    }

    /* Mutable integer. */
    public static final class IntHolder
    {
        public int value;

        public IntHolder(int initial)
        {
            value = initial;
        }
    }
}
