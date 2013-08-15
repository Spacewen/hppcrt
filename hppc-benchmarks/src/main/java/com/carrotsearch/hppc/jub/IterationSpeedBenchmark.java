package com.carrotsearch.hppc.jub;

import java.util.Random;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.carrotsearch.hppc.IntArrayList;
import com.carrotsearch.hppc.cursors.IntCursor;
import com.carrotsearch.hppc.mutables.IntHolder;
import com.carrotsearch.hppc.procedures.IntProcedure;
import com.carrotsearch.junitbenchmarks.AbstractBenchmark;
import com.carrotsearch.junitbenchmarks.BenchmarkOptions;
import com.carrotsearch.junitbenchmarks.annotation.BenchmarkHistoryChart;
import com.carrotsearch.junitbenchmarks.annotation.BenchmarkMethodChart;

/**
 * Various iteration approaches on an integer list.
 */
@BenchmarkHistoryChart(filePrefix="CLASSNAME.history", maxRuns=50)
@BenchmarkMethodChart(filePrefix="CLASSNAME.methods")
@BenchmarkOptions(callgc = false, warmupRounds = 10, benchmarkRounds = 10)

public class IterationSpeedBenchmark extends AbstractBenchmark
{
    public static final int CELLS = (1024 * 1024) * 200;
    private static IntArrayList list;
    public volatile int guard; 

    /* */
    @BeforeClass
    public static void before()
    {
        list = new IntArrayList();
        list.resize(CELLS);
        
        //fill with random values
        Random prng = new Random(210654641246431L);
        
        for (int i = 0 ; i < list.size(); i++) {
          
            list.set(i, prng.nextInt());
        }
    }

    @AfterClass
    public static void after()
    {
        list = null;
        
    }

    /* */
    @Test
    public void testSimpleGetLoop() throws Exception
    {
        int count = 0;
        for (int i = 0; i < list.size(); i++)
        {
            count += list.get(i);
        }

       guard = count;
    }

    /* */
    @Test
    public void testDirectBufferLoop() throws Exception
    {
        final int size = list.size();
        final int [] buffer = list.buffer;
        int count = 0;
        for (int i = 0; i < size; i++)
        {
            count += buffer[i];
        }
        guard = count;
    }
    
    /* */
    @Test
    public void testIterableCursor() throws Exception
    {
        int count = 0;
        for (IntCursor c : list)
        {
            count += c.value;
        }
        guard = count;
    }

    /* */
    @Test
    public void testWithProcedureClosure()
    {
        final IntHolder holder = new IntHolder();
        list.forEach(new IntProcedure() {
            public void apply(int v)
            {
                holder.value += v;
            }
        });
        guard = holder.value;
    }

    /* */
    @Test
    public void testDirectBufferWithNewFor() throws Exception
    {
        int count = 0;
        for (int c : list.buffer)
        {
            count += c;
        }
        guard = count;
    }
}
