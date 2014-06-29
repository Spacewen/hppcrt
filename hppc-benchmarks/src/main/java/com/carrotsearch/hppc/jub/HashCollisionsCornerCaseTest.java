package com.carrotsearch.hppc.jub;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.carrotsearch.hppc.IntIntOpenHashMap;
import com.carrotsearch.hppc.IntOpenHashSet;
import com.carrotsearch.junitbenchmarks.AbstractBenchmark;
import com.carrotsearch.junitbenchmarks.BenchmarkOptions;
import com.carrotsearch.junitbenchmarks.annotation.BenchmarkHistoryChart;
import com.carrotsearch.junitbenchmarks.annotation.BenchmarkMethodChart;

@BenchmarkHistoryChart(filePrefix = "CLASSNAME.history", maxRuns = 10)
@BenchmarkMethodChart(filePrefix = "CLASSNAME.methods")
@BenchmarkOptions(callgc = false, warmupRounds = 10, benchmarkRounds = 5)
public class HashCollisionsCornerCaseTest extends AbstractBenchmark
{

    static volatile private long dummySum = 0;

    private static int NB_ELEMENTS = (int) 10e6;
    private static float LOAD_FACTOR = 0.75f;

    private static final IntOpenHashSet testSet = new IntOpenHashSet(HashCollisionsCornerCaseTest.NB_ELEMENTS, HashCollisionsCornerCaseTest.LOAD_FACTOR);
    private static final IntIntOpenHashMap testMap = new IntIntOpenHashMap(HashCollisionsCornerCaseTest.NB_ELEMENTS, HashCollisionsCornerCaseTest.LOAD_FACTOR);

    private static final IntOpenHashSet testHashSet = new IntOpenHashSet(HashCollisionsCornerCaseTest.NB_ELEMENTS);
    private static final IntOpenHashSet testHashSetNoPerturb = IntOpenHashSet.newInstanceWithoutPerturbations(HashCollisionsCornerCaseTest.NB_ELEMENTS, IntOpenHashSet.DEFAULT_LOAD_FACTOR);

    private static final IntOpenHashSet testHashSet2 = new IntOpenHashSet(HashCollisionsCornerCaseTest.NB_ELEMENTS);
    private static final IntOpenHashSet testHashSetNoPerturb2 = IntOpenHashSet.newInstanceWithoutPerturbations(HashCollisionsCornerCaseTest.NB_ELEMENTS, IntOpenHashSet.DEFAULT_LOAD_FACTOR);

    private static final IntIntOpenHashMap testHashMap = new IntIntOpenHashMap(HashCollisionsCornerCaseTest.NB_ELEMENTS);
    private static final IntIntOpenHashMap testHashMapNoPerturb = IntIntOpenHashMap.newInstanceWithoutPerturbations(HashCollisionsCornerCaseTest.NB_ELEMENTS, IntOpenHashSet.DEFAULT_LOAD_FACTOR);

    private static final IntIntOpenHashMap testHashMap2 = new IntIntOpenHashMap(HashCollisionsCornerCaseTest.NB_ELEMENTS);
    private static final IntIntOpenHashMap testHashMapNoPerturb2 = IntIntOpenHashMap.newInstanceWithoutPerturbations(HashCollisionsCornerCaseTest.NB_ELEMENTS, IntOpenHashSet.DEFAULT_LOAD_FACTOR);

    @BeforeClass
    public static void beforeClass()
    {
        //we are to really push realNbElements to fill the sets up to their
        //max allowable load factor to really test the worst case here.
        final int realNbElements = HashCollisionsCornerCaseTest.testSet.capacity() - 1;

        for (int i = realNbElements; i-- != 0;) {

            HashCollisionsCornerCaseTest.testSet.add(i);
            HashCollisionsCornerCaseTest.testMap.put(i, 0);
        }
    }

    @AfterClass
    public static void afterClass()
    {
        System.err.println(HashCollisionsCornerCaseTest.dummySum);
    }

    @Before
    public void before()
    {
        HashCollisionsCornerCaseTest.testHashSet.clear();
        HashCollisionsCornerCaseTest.testHashMap.clear();
        HashCollisionsCornerCaseTest.testHashSet2.clear();
        HashCollisionsCornerCaseTest.testHashMap2.clear();
        HashCollisionsCornerCaseTest.testHashSetNoPerturb.clear();
        HashCollisionsCornerCaseTest.testHashSetNoPerturb2.clear();
        HashCollisionsCornerCaseTest.testHashMapNoPerturb.clear();
        HashCollisionsCornerCaseTest.testHashMapNoPerturb2.clear();
    }

    @After
    public void after()
    {
        HashCollisionsCornerCaseTest.dummySum = HashCollisionsCornerCaseTest.testHashSet.size() +
                HashCollisionsCornerCaseTest.testHashMap.size() +
                HashCollisionsCornerCaseTest.testHashSet2.size() +
                HashCollisionsCornerCaseTest.testHashMap2.size() +
                HashCollisionsCornerCaseTest.testHashSetNoPerturb.size() +
                HashCollisionsCornerCaseTest.testHashMapNoPerturb.size() +
                HashCollisionsCornerCaseTest.testHashSetNoPerturb2.size() +
                HashCollisionsCornerCaseTest.testHashMapNoPerturb2.size();

    }

    /** @see "http://issues.carrot2.org/browse/HPPC-80" */
    @Test
    public void testHashSets()
    {
        HashCollisionsCornerCaseTest.testHashSet.addAll(HashCollisionsCornerCaseTest.testSet);
    }

    @Test
    public void testHashSetsSuccessiveAdd()
    {
        HashCollisionsCornerCaseTest.testHashSet.addAll(HashCollisionsCornerCaseTest.testSet);
        HashCollisionsCornerCaseTest.testHashSet2.addAll(HashCollisionsCornerCaseTest.testHashSet);
    }

    @Test
    public void testHashSetsNoPerturbation()
    {
        HashCollisionsCornerCaseTest.testHashSetNoPerturb.addAll(HashCollisionsCornerCaseTest.testSet);
    }

    @Test
    public void testHashSetsNoPerturbationSuccessiveAdd()
    {
        HashCollisionsCornerCaseTest.testHashSetNoPerturb.addAll(HashCollisionsCornerCaseTest.testSet);
        HashCollisionsCornerCaseTest.testHashSetNoPerturb2.addAll(HashCollisionsCornerCaseTest.testHashSetNoPerturb);
    }

    @Test
    public void testHashSetsWithReallocation()
    {
        final IntOpenHashSet test = new IntOpenHashSet(IntOpenHashSet.DEFAULT_CAPACITY, HashCollisionsCornerCaseTest.LOAD_FACTOR);

        test.addAll(HashCollisionsCornerCaseTest.testSet);
    }

    @Test
    public void testHashSetsNoPerturbationWithReallocation()
    {
        final IntOpenHashSet test = IntOpenHashSet.newInstanceWithoutPerturbations(IntOpenHashSet.DEFAULT_CAPACITY, HashCollisionsCornerCaseTest.LOAD_FACTOR);
        test.addAll(HashCollisionsCornerCaseTest.testSet);
    }

    /** @see "http://issues.carrot2.org/browse/HPPC-80" */
    @Test
    public void testHashMaps()
    {
        HashCollisionsCornerCaseTest.testHashMap.putAll(HashCollisionsCornerCaseTest.testMap);
    }

    @Test
    public void testHashMapsSuccessivePut()
    {
        HashCollisionsCornerCaseTest.testHashMap.putAll(HashCollisionsCornerCaseTest.testMap);
        HashCollisionsCornerCaseTest.testHashMap2.putAll(HashCollisionsCornerCaseTest.testHashMap);
    }

    /** @see "http://issues.carrot2.org/browse/HPPC-80" */
    @Test
    public void testHashMapsNoPerturbation()
    {
        HashCollisionsCornerCaseTest.testHashMapNoPerturb.putAll(HashCollisionsCornerCaseTest.testMap);
    }

    @Test
    public void testHashMapsNoPerturbationSuccessivePut()
    {
        HashCollisionsCornerCaseTest.testHashMapNoPerturb.putAll(HashCollisionsCornerCaseTest.testMap);
        HashCollisionsCornerCaseTest.testHashMapNoPerturb2.putAll(HashCollisionsCornerCaseTest.testHashMapNoPerturb);
    }

    /** @see "http://issues.carrot2.org/browse/HPPC-80" */
    @Test
    public void testHashMapsWithReallocation()
    {
        final IntIntOpenHashMap test = new IntIntOpenHashMap(IntIntOpenHashMap.DEFAULT_CAPACITY, HashCollisionsCornerCaseTest.LOAD_FACTOR);
        test.putAll(HashCollisionsCornerCaseTest.testMap);
    }

    /** @see "http://issues.carrot2.org/browse/HPPC-80" */
    @Test
    public void testHashMapsNoPerturbationWithReallocation()
    {
        final IntIntOpenHashMap test = IntIntOpenHashMap.newInstanceWithoutPerturbations(IntIntOpenHashMap.DEFAULT_CAPACITY, HashCollisionsCornerCaseTest.LOAD_FACTOR);
        test.putAll(HashCollisionsCornerCaseTest.testMap);
    }
}
