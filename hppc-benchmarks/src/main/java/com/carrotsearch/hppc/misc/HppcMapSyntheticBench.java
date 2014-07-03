package com.carrotsearch.hppc.misc;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

import com.carrotsearch.hppc.DistributionGenerator;
import com.carrotsearch.hppc.DistributionGenerator.Generator;
import com.carrotsearch.hppc.HashingStrategy;
import com.carrotsearch.hppc.IntArrayList;
import com.carrotsearch.hppc.IntLongMap;
import com.carrotsearch.hppc.IntLongOpenHashMap;
import com.carrotsearch.hppc.LongArrayList;
import com.carrotsearch.hppc.ObjectArrayList;
import com.carrotsearch.hppc.ObjectLongMap;
import com.carrotsearch.hppc.ObjectLongOpenHashMap;
import com.carrotsearch.hppc.XorShiftRandom;
import com.carrotsearch.hppc.cursors.IntLongCursor;
import com.carrotsearch.hppc.procedures.LongProcedure;

public final class HppcMapSyntheticBench
{
    public static final int COUNT = (int) 1e6;

    public static final int COUNT_BIG_OBJECTS = (int) 100e3;

    public static final long RANDOM_SEED = 5487911234761188L;
    private static final long RAND_SEED = 15487012316864131L;
    private static final long RAND_SEED2 = 9988713416546546L;
    private static final long RAND_SEED3 = 412316451315451545L;
    private static final long RAND_SEED4 = 2345613216796312185L;

    public static final int NB_WARMUPS = 2;

    private static final int COUNT_ITERATION = (int) 10e6;

    public Random prng = new XorShiftRandom();

    private long totalSum;

    private final int nbWarmupsRuns;

    public enum MAP_LOOKUP_TEST
    {
        MOSTLY_TRUE,
        MOSTLY_FALSE,
        MIXED
    }

    public enum Distribution
    {
        RANDOM, LINEAR, LINEAR_DECREMENT, HIGHBITS;
    }

    enum HASH_QUALITY
    {
        NORMAL(0),
        BAD(3);

        public final int shift;

        private HASH_QUALITY(final int bitshift)
        {
            this.shift = bitshift;

        }
    }

    //closure like
    private final LongProcedure sumProcedureInstance = new LongProcedure() {

        @Override
        public void apply(final long value)
        {
            HppcMapSyntheticBench.this.totalSum += value;
        }
    };

    // inner comparator class
    public final class InverseComparator implements Comparator<ComparableLong>
    {

        @Override
        public int compare(final ComparableLong o1, final ComparableLong o2)
        {

            int res = 0;

            if (o1.value > o2.value)
            {

                res = -1;

            }
            else if (o1.value < o2.value)
            {

                res = 1;
            }

            return res;
        }
    }

    public static final class NaturalComparator implements Comparator<ComparableLong>
    {

        @Override
        public int compare(final ComparableLong o1, final ComparableLong o2)
        {

            int res = 0;

            if (o1.value < o2.value)
            {

                res = -1;

            }
            else if (o1.value > o2.value)
            {

                res = 1;
            }

            return res;
        }
    }

    // create comparable type
    public static class ComparableLong implements Comparable<ComparableLong>
    {

        public long value;

        public ComparableLong(final long initValue)
        {

            this.value = initValue;
        }

        @Override
        public int compareTo(final ComparableLong other)
        {
            int res = 0;

            if (this.value < other.value)
            {
                res = -1;
            }

            else if (this.value > other.value)
            {
                res = 1;
            }

            return res;
        }

        @Override
        public Object clone() throws CloneNotSupportedException
        {

            return new ComparableLong(this.value);
        }

        @Override
        public boolean equals(final Object obj)
        {

            return this.value == ((ComparableLong) obj).value;
        }

        @Override
        public int hashCode()
        {

            return (int) this.value;
        }
    }

    public static class ComparableInt implements Comparable<ComparableInt>
    {

        public int value;
        public final int bitshift;

        public ComparableInt(final int initValue, final HASH_QUALITY quality)
        {

            this.value = initValue;
            this.bitshift = quality.shift;
        }

        @Override
        public int compareTo(final ComparableInt other)
        {
            int res = 0;

            if (this.value < other.value)
            {
                res = -1;
            }

            else if (this.value > other.value)
            {
                res = 1;
            }

            return res;
        }

        @Override
        public int hashCode()
        {

            return this.value << this.bitshift;
        }

        @Override
        public boolean equals(final Object obj)
        {

            if (obj instanceof ComparableInt)
            {

                return ((ComparableInt) obj).value == this.value;
            }

            return false;
        }
    }

    // create comparable type
    public static class ComparableAsciiString implements Comparable<ComparableAsciiString>
    {

        public final int[] value;
        public final int bitshift;

        /**
         * Default constructor generates a random character content based on prng.
         */
        public ComparableAsciiString(final int[] characters, final HASH_QUALITY quality)
        {

            this.bitshift = quality.shift;

            this.value = characters;
        }

        @Override
        public int compareTo(final ComparableAsciiString other)
        {

            if (this.value.length < other.value.length)
            {

                return -1;
            }
            else if (this.value.length > other.value.length)
            {

                return 1;

            }
            else
            {
                //both length are equal
                for (int ii = 0; ii < this.value.length; ii++)
                {

                    if (this.value[ii] < other.value[ii])
                    {

                        return -1;

                    }
                    else if (this.value[ii] > other.value[ii])
                    {

                        return 1;
                    }
                } //end for

                return 0;
            }
        }

        @Override
        public boolean equals(final Object obj)
        {

            return Arrays.equals(this.value, ((ComparableAsciiString) obj).value);
        }

        // djb2 hash function
        public int goodHashCode()
        {

            int hash = 5381;

            for (final int character : this.value)
            {
                hash = (hash << 5) + hash + character; /* hash * 33 + b */
            }

            return hash;
        }

        @Override
        public int hashCode()
        {

            return goodHashCode() << this.bitshift;
        }
    }

    /**
     * Testing for strategies
     */
    private final HashingStrategy<ComparableInt> INTHOLDER_TRIVIAL_STRATEGY = new HashingStrategy<ComparableInt>() {

        @Override
        public int computeHashCode(final ComparableInt o)
        {
            return o.value;
        }

        @Override
        public boolean equals(final ComparableInt o1, final ComparableInt o2)
        {
            return o1.value == o2.value;
        }
    };

    private final HashingStrategy<ComparableAsciiString> ASCIISTRING_TRIVIAL_STRATEGY = new HashingStrategy<ComparableAsciiString>() {

        @Override
        public int computeHashCode(final ComparableAsciiString o)
        {
            return o.goodHashCode();
        }

        @Override
        public boolean equals(final ComparableAsciiString o1, final ComparableAsciiString o2)
        {
            return o1.equals(o2);
        }
    };

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * Constructor
     */
    public HppcMapSyntheticBench(final int nbWarmups)
    {

        this.nbWarmupsRuns = nbWarmups;
        this.prng.setSeed(HppcMapSyntheticBench.RANDOM_SEED);
    }

    /**
     * Map HPPC (int ==> long) iteration bench
     * @param testMap
     * @param additionalInfo
     * @param nbWarmupRuns
     */
    private void runMapIterationBench(final String additionalInfo, final IntLongMap testMap,
            final int minPushedElements, final float loadFactor)
    {
        long tBefore = 0;
        long tAfter = 0;

        double tExecDirectMs = 0;
        double tExecIteratorMs = 0;
        double tExecForEachMs = 0;

        //////////////////////////////////
        int nbWarmups = 0;

        long sumForeach = 0L;
        long sumIt = 0L;
        long sumDirect = 0;

        final int initialCapacity = testMap.capacity();

        //Do a dry run for enumerating the values that are to be put in map :
        final IntArrayList Klist = new IntArrayList();
        final LongArrayList Vlist = new LongArrayList();

        this.prng.setSeed(HppcMapSyntheticBench.RAND_SEED);

        while (testMap.size() < minPushedElements || testMap.size() < testMap.capacity())
        {

            final int K = this.prng.nextInt();
            final long V = this.prng.nextLong();
            Klist.add(K);
            Vlist.add(V);

            testMap.put(K, V);
        }

        //we need the testMap to be not-empty !!
        System.gc();

        //Iteration Procedure to sum the elements

        while (nbWarmups <= this.nbWarmupsRuns)
        {

            nbWarmups++;

            //A) sum with iterator
            tBefore = System.nanoTime();

            sumIt = 0;
            for (final IntLongCursor cursor : testMap)
            {

                sumIt += cursor.value;
            }

            tAfter = System.nanoTime();

            tExecIteratorMs = (tAfter - tBefore) / 1e6;

            //B) sum with forEacch

            tBefore = System.nanoTime();

            sumForeach = 0;
            this.totalSum = 0L;

            testMap.values().forEach(this.sumProcedureInstance);

            sumForeach = this.totalSum;

            tAfter = System.nanoTime();

            tExecForEachMs = (tAfter - tBefore) / 1e6;

            //C) sum with direct iteration
            sumDirect = 0;
            tBefore = System.nanoTime();

            if (testMap instanceof IntLongOpenHashMap)
            {

                final IntLongOpenHashMap m = (IntLongOpenHashMap) testMap;

                for (int ii = 0; ii < m.allocated.length; ii++)
                {
                    if (m.allocated[ii])
                    {
                        sumDirect += m.values[ii];
                    }
                }
            }

            tAfter = System.nanoTime();

            tExecDirectMs = (tAfter - tBefore) / 1e6;

        } //end while

        if (!(sumForeach == sumIt && sumIt == sumDirect))
        {
            throw new AssertionError(String.format("ERROR, (Iterator sum = %d) != (forEachSum = %d) != (directSum = %d)", sumIt, sumForeach, sumDirect));
        }

        System.out.format(">>>> BENCH: HPPC Map (int, long), (%s) Iteration test, capacity = %d, load factor = %f, %d elements summed,\n"
                + " Iterator= %f ms, foreach= %f ms, Direct= %f ms (result = %d)\n\n",
                additionalInfo,
                testMap.capacity(),
                loadFactor,
                testMap.size(),
                tExecIteratorMs, tExecForEachMs, tExecDirectMs,
                sumForeach); //outputs the results to defeat optimizations
    }

    ////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Bench for Primitive int ==> long, fill the container up to its load factor:
     */
    public void runMapPrimitiveInt(final String additionalInfo, final IntLongMap testMap,
            final int minPushedElements, final float loadFactor, final MAP_LOOKUP_TEST getKind, final Distribution dis)
    {
        long sum = 0;
        long tBeforePut = 0;
        long tAfterPut = 0;
        long tBeforeGet = 0;
        long tAfterGet = 0;
        long tBeforeClear = 0;
        long tAfterClear = 0;

        long tBeforeRemove = 0;
        long tAfterRemove = 0;
        final int finalSize = 0;
        //////////////////////////////////
        int nbWarmups = 0;
        int putSize = 0;

        final int initialCapacity = testMap.capacity();

        //Do a dry run for enumerating the values that are to be put in map :
        final IntArrayList Klist = new IntArrayList();
        final LongArrayList Vlist = new LongArrayList();

        this.prng.setSeed(HppcMapSyntheticBench.RAND_SEED);

        final Generator gene = getGenerator(dis, minPushedElements);

        while (testMap.size() < minPushedElements || testMap.size() < testMap.capacity())
        {

            final int K = gene.getNext();
            final long V = this.prng.nextLong();
            Klist.add(K);
            Vlist.add(V);

            testMap.put(K, V);
        }

        testMap.clear();
        System.gc();

        while (nbWarmups <= this.nbWarmupsRuns)
        {
            testMap.clear();

            nbWarmups++;

            tBeforePut = System.nanoTime();

            //A) fill until the capacity, and at least minPushedElements
            for (int ii = 0; ii < Klist.size(); ii++)
            {

                //put all, then for even key number, delete by key
                testMap.put(Klist.get(ii), Vlist.get(ii));
            }

            tAfterPut = System.nanoTime();

            putSize = testMap.size();

            tBeforeGet = System.nanoTime();

            // B) Process by get/contains
            for (int ii = 0; ii < Klist.size(); ii++)
            {

                if ((getKind == MAP_LOOKUP_TEST.MOSTLY_TRUE || getKind == MAP_LOOKUP_TEST.MIXED) && this.prng.nextBoolean())
                {

                    sum += testMap.get(Klist.get(ii));
                }
                else if (getKind == MAP_LOOKUP_TEST.MOSTLY_FALSE || getKind == MAP_LOOKUP_TEST.MIXED)
                {
                    //this element may or may not be in the set
                    sum += testMap.get(this.prng.nextInt());
                }
            }

            tAfterGet = System.nanoTime();
            ///////////////////////////////////////

            //C) Remove Op
            tBeforeRemove = System.nanoTime();

            for (int ii = 0; ii < Klist.size(); ii++)
            {

                if ((getKind == MAP_LOOKUP_TEST.MOSTLY_TRUE || getKind == MAP_LOOKUP_TEST.MIXED) && this.prng.nextBoolean())
                {

                    sum += testMap.remove(Klist.get(ii));
                }
                else if (getKind == MAP_LOOKUP_TEST.MOSTLY_FALSE || getKind == MAP_LOOKUP_TEST.MIXED)
                {
                    //this element may or may not be in the set
                    sum += testMap.remove(this.prng.nextInt());
                }
            }

            tAfterRemove = System.nanoTime();

            //clear op
            tBeforeClear = System.nanoTime();

            testMap.clear();

            tAfterClear = System.nanoTime();

        } //end while

        System.out.format(">>>> BENCH: HPPC Map (int of dis %s, long), (%s) capacity = %d, load factor = %f, %d elements pushed\n"
                + " Put = %f ms, Get (%s) = %f ms,  Remove (%s) = %f ms,  Clear =  %f ms (dummy = %d)\n\n",
                dis,
                additionalInfo,
                testMap.capacity(),
                loadFactor,
                putSize,
                (tAfterPut - tBeforePut) / 1e6,
                getKind,
                (tAfterGet - tBeforeGet) / 1e6,
                getKind,
                (tAfterRemove - tBeforeRemove) / 1e6,
                (tAfterClear - tBeforeClear) / 1e6,
                sum); //outputs the results to defeat optimizations
    }

    /**
     * Bench for HPPC (ComparableInt Object ==> long).
     *
     */
    public void runMapIntegerObjectLong(final String additionalInfo, final ObjectLongMap<ComparableInt> testMap,
            final int minPushedElements, final float loadFactor, final MAP_LOOKUP_TEST getKind, final HASH_QUALITY quality, final Distribution dis)
    {

        long sum = 0;
        long tBeforePut = 0;
        long tAfterPut = 0;
        long tBeforeGet = 0;
        long tAfterGet = 0;
        long tBeforeClear = 0;
        long tAfterClear = 0;

        long tBeforeRemove = 0;
        long tAfterRemove = 0;

        //////////////////////////////////
        int nbWarmups = 0;

        int putSize = 0;

        final int initialCapacity = testMap.capacity();

        //Do a dry run for pre-boxing the values that are to be put in map :
        final ObjectArrayList<ComparableInt> Klist = new ObjectArrayList<ComparableInt>();
        final LongArrayList Vlist = new LongArrayList();

        this.prng.setSeed(HppcMapSyntheticBench.RAND_SEED);

        final Generator gene = getGenerator(dis, minPushedElements);

        while (testMap.size() < minPushedElements || testMap.size() < testMap.capacity())
        {

            //put all, then for even key number, delete by key
            final ComparableInt K = new ComparableInt(gene.getNext(), quality);
            final long V = this.prng.nextLong();
            Klist.add(K);
            Vlist.add(V);

            testMap.put(K, V);
        }

        testMap.clear();
        System.gc();

        //The main measuring loop
        while (nbWarmups <= this.nbWarmupsRuns)
        {
            testMap.clear();

            nbWarmups++;

            tBeforePut = System.nanoTime();

            //A) fill until the capacity, and at least minPushedElements
            for (int ii = 0; ii < Klist.size(); ii++)
            {

                //put all, then for even key number, delete by key
                testMap.put(Klist.get(ii), Vlist.get(ii));
            }

            tAfterPut = System.nanoTime();

            putSize = testMap.size();

            tBeforeGet = System.nanoTime();

            // B) Process by get/contains
            final ComparableInt tmpAbsentIntHolder = new ComparableInt(0, quality);

            //Rerun to compute a containsKey lget() pattern
            for (int ii = 0; ii < Klist.size(); ii++)
            {

                if ((getKind == MAP_LOOKUP_TEST.MOSTLY_TRUE || getKind == MAP_LOOKUP_TEST.MIXED) && this.prng.nextBoolean())
                {

                    sum += testMap.get(Klist.get(ii));
                }
                else if (getKind == MAP_LOOKUP_TEST.MOSTLY_FALSE || getKind == MAP_LOOKUP_TEST.MIXED)
                {

                    //this element may or may not be in the set
                    tmpAbsentIntHolder.value = this.prng.nextInt();
                    sum += testMap.get(tmpAbsentIntHolder);
                }
            }

            tAfterGet = System.nanoTime();
            ///////////////////////////////////////

            //C) Remove Op
            tBeforeRemove = System.nanoTime();

            for (int ii = 0; ii < Klist.size(); ii++)
            {

                if ((getKind == MAP_LOOKUP_TEST.MOSTLY_TRUE || getKind == MAP_LOOKUP_TEST.MIXED) && this.prng.nextBoolean())
                {

                    sum += testMap.remove(Klist.get(ii));
                }
                else if (getKind == MAP_LOOKUP_TEST.MOSTLY_FALSE || getKind == MAP_LOOKUP_TEST.MIXED)
                {

                    //this element may or may not be in the set
                    tmpAbsentIntHolder.value = this.prng.nextInt();
                    sum += testMap.remove(tmpAbsentIntHolder);
                }
            }

            tAfterRemove = System.nanoTime();

            //clear op
            tBeforeClear = System.nanoTime();

            testMap.clear();

            tAfterClear = System.nanoTime();

        } //end while

        System.out.format(">>>> BENCH: HPPC Map (ComparableInt object of dis %s, hash %s, long), (%s), capacity = %d, load factor = %f, %d elements pushed\n"
                + " Put = %f ms, Get (%s) = %f ms, Remove (%s) = %f ms,  Clear =  %f ms (dummy = %d)\n\n",
                dis,
                quality,
                additionalInfo,
                testMap.capacity(),
                loadFactor,
                putSize,
                (tAfterPut - tBeforePut) / 1e6,
                getKind,
                (tAfterGet - tBeforeGet) / 1e6,
                getKind,
                (tAfterRemove - tBeforeRemove) / 1e6,
                (tAfterClear - tBeforeClear) / 1e6,
                sum); //outputs the results to defeat optimizations
    }

    /**
     * Bench for HPPC (ComparableAsciiString ==> long).
     *
     */
    public void runMapAsciiStringObjectLong(final String additionalInfo, final ObjectLongMap<ComparableAsciiString> testMap,
            final int minPushedElements, final float loadFactor, final MAP_LOOKUP_TEST getKind, final int stringSize, final HASH_QUALITY quality, final Distribution dis)
    {

        long sum = 0;
        long tBeforePut = 0;
        long tAfterPut = 0;
        long tBeforeGet = 0;
        long tAfterGet = 0;
        long tBeforeClear = 0;
        long tAfterClear = 0;

        long tBeforeRemove = 0;
        long tAfterRemove = 0;
        final int finalSize = 0;
        //////////////////////////////////
        int nbWarmups = 0;

        int putSize = 0;

        final int initialCapacity = testMap.capacity();

        //Do a dry run for pre-boxing the values that are to be put in map :
        final ObjectArrayList<ComparableAsciiString> Klist = new ObjectArrayList<ComparableAsciiString>();
        final LongArrayList Vlist = new LongArrayList();

        this.prng.setSeed(HppcMapSyntheticBench.RAND_SEED);

        final Generator gene = getGenerator(dis, minPushedElements);

        while (testMap.size() < minPushedElements || testMap.size() < testMap.capacity())
        {

            //put all, then for even key number, delete by key
            final ComparableAsciiString K = new ComparableAsciiString(gene.prepare(stringSize), quality);
            final long V = this.prng.nextLong();
            Klist.add(K);
            Vlist.add(V);

            testMap.put(K, V);
        }

        final ObjectArrayList<ComparableAsciiString> KlistNotFound = new ObjectArrayList<ComparableAsciiString>();

        for (int ii = 0; ii < testMap.capacity() * 0.7; ii++)
        {

            KlistNotFound.add(new ComparableAsciiString(gene.prepare(stringSize), quality));
        }

        testMap.clear();
        System.gc();

        //The main measuring loop
        while (nbWarmups <= this.nbWarmupsRuns)
        {

            testMap.clear();

            nbWarmups++;

            tBeforePut = System.nanoTime();

            //A) fill until the capacity, and at least minPushedElements
            for (int ii = 0; ii < Klist.size(); ii++)
            {

                //put all, then for even key number, delete by key
                testMap.put(Klist.get(ii), Vlist.get(ii));
            }

            tAfterPut = System.nanoTime();

            putSize = testMap.size();

            tBeforeGet = System.nanoTime();

            // B) Process by get/contains
            final int notFoundCount = 0;

            //Rerun to compute a containsKey lget() pattern
            for (int ii = 0; ii < Klist.size(); ii++)
            {

                if ((getKind == MAP_LOOKUP_TEST.MOSTLY_TRUE || getKind == MAP_LOOKUP_TEST.MIXED) && this.prng.nextBoolean())
                {

                    sum += testMap.get(Klist.get(ii));
                }
                else if (getKind == MAP_LOOKUP_TEST.MOSTLY_FALSE || getKind == MAP_LOOKUP_TEST.MIXED)
                {

                    //this element is not in the set
                    sum += testMap.get(KlistNotFound.get(notFoundCount));
                }
            }

            tAfterGet = System.nanoTime();
            ///////////////////////////////////////

            //C) Remove Op
            tBeforeRemove = System.nanoTime();

            for (int ii = 0; ii < Klist.size(); ii++)
            {

                if ((getKind == MAP_LOOKUP_TEST.MOSTLY_TRUE || getKind == MAP_LOOKUP_TEST.MIXED) && this.prng.nextBoolean())
                {

                    sum += testMap.remove(Klist.get(ii));
                }
                else if (getKind == MAP_LOOKUP_TEST.MOSTLY_FALSE || getKind == MAP_LOOKUP_TEST.MIXED)
                {

                    //this element is not in the set
                    sum += testMap.remove(KlistNotFound.get(notFoundCount));
                }
            }

            tAfterRemove = System.nanoTime();

            //clear op
            tBeforeClear = System.nanoTime();

            testMap.clear();

            tAfterClear = System.nanoTime();

        } //end while

        System.out.format(
                ">>>> BENCH: HPPC Map (ComparableAsciiString %d ints long of dis %s, hash %s, long), (%s), initial capacity = %d, load factor = %f, %d elements pushed\n"
                        + " Put = %f ms, Get (%s) = %f ms, Remove (%s) = %f ms, Clear =  %f ms (dummy = %d)\n\n",
                        stringSize,
                        dis,
                        quality,
                        additionalInfo,
                        testMap.capacity(),
                        loadFactor,
                        putSize,

                        (tAfterPut - tBeforePut) / 1e6,
                        getKind,
                        (tAfterGet - tBeforeGet) / 1e6,
                        getKind,
                        (tAfterRemove - tBeforeRemove) / 1e6,
                        (tAfterClear - tBeforeClear) / 1e6,
                        sum); //outputs the results to defeat optimizations
    }

    //public methods

    public void runMapSyntheticBenchPrimitives(final MAP_LOOKUP_TEST getKind)
    {
        for (final Distribution dis : Distribution.values())
        {
            // Preallocate at maximum size
            runMapPrimitiveInt("IntLongOpenHashMap",
                    IntLongOpenHashMap.newInstance(HppcMapSyntheticBench.COUNT, IntLongOpenHashMap.DEFAULT_LOAD_FACTOR),
                    HppcMapSyntheticBench.COUNT, IntLongOpenHashMap.DEFAULT_LOAD_FACTOR,
                    getKind, dis);
            System.gc();
        }
    }

    public void runMapSyntheticBenchIntegers(final MAP_LOOKUP_TEST getKind)
    {
        for (final Distribution dis : Distribution.values())
        {
            for (final HASH_QUALITY q : HASH_QUALITY.values())
            {
                runMapIntegerObjectLong("ObjectLongOpenHashMap",
                        ObjectLongOpenHashMap.<ComparableInt> newInstance(HppcMapSyntheticBench.COUNT, ObjectLongOpenHashMap.DEFAULT_LOAD_FACTOR),
                        HppcMapSyntheticBench.COUNT, ObjectLongOpenHashMap.DEFAULT_LOAD_FACTOR,
                        getKind, q, dis);
                System.gc();
            }

            // use specialized strategy
            runMapIntegerObjectLong("ObjectLongOpenHashMap with strategy",
                    ObjectLongOpenHashMap.<ComparableInt> newInstance(
                            HppcMapSyntheticBench.COUNT, IntLongOpenHashMap.DEFAULT_LOAD_FACTOR,
                            this.INTHOLDER_TRIVIAL_STRATEGY),
                            HppcMapSyntheticBench.COUNT, ObjectLongOpenHashMap.DEFAULT_LOAD_FACTOR, getKind, HASH_QUALITY.BAD, dis);
            System.gc();
        }
    }

    public void runMapSyntheticBenchObjects(final MAP_LOOKUP_TEST getKind)
    {
        final int[] stringSizes = new int[] { 16, 32, 64 };

        for (final int stringSize : stringSizes)
        {
            for (final Distribution dis : Distribution.values())
            {
                for (final HASH_QUALITY q : HASH_QUALITY.values())
                {
                    runMapAsciiStringObjectLong("ObjectLongOpenHashMap",
                            ObjectLongOpenHashMap.<ComparableAsciiString> newInstance(HppcMapSyntheticBench.COUNT_BIG_OBJECTS, ObjectLongOpenHashMap.DEFAULT_LOAD_FACTOR),
                            HppcMapSyntheticBench.COUNT_BIG_OBJECTS, ObjectLongOpenHashMap.DEFAULT_LOAD_FACTOR,
                            getKind, stringSize, q, dis);
                    System.gc();
                }

                runMapAsciiStringObjectLong("ObjectLongOpenHashMap with strategy",
                        ObjectLongOpenHashMap.<ComparableAsciiString> newInstance(HppcMapSyntheticBench.COUNT_BIG_OBJECTS, ObjectLongOpenHashMap.DEFAULT_LOAD_FACTOR,
                                this.ASCIISTRING_TRIVIAL_STRATEGY),
                                HppcMapSyntheticBench.COUNT_BIG_OBJECTS, ObjectLongOpenHashMap.DEFAULT_LOAD_FACTOR,
                                getKind, stringSize, HASH_QUALITY.BAD, dis);
            }
        }
    }

    public void runMapIterationBench()
    {

        runMapIterationBench("IntLongOpenHashMap", new IntLongOpenHashMap(HppcMapSyntheticBench.COUNT_ITERATION), HppcMapSyntheticBench.COUNT_ITERATION, IntLongOpenHashMap.DEFAULT_LOAD_FACTOR);
        System.gc();
    }

    private Generator getGenerator(final Distribution disKind, final int size)
    {
        Generator generator = null;

        final DistributionGenerator disGene = new DistributionGenerator(size, this.prng);

        switch (disKind)
        {

            case RANDOM:
                generator = disGene.RANDOM;
                break;

            case LINEAR:
                generator = disGene.LINEAR;
                break;

            case LINEAR_DECREMENT:
                generator = disGene.LINEAR_DECREMENT;
                break;

            case HIGHBITS:
                generator = disGene.HIGHBITS;
                break;

            default:
                throw new RuntimeException();
        }

        return generator;
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * main
     */
    public static void main(final String[] args)
    {

        if (args.length != 1 || !args[0].contains("--warmup="))
        {

            System.out.println("Usage : " + HppcMapSyntheticBench.class.getName() + " --warmup=[nb warmup runs]");
        }
        else
        {
            final int nbWarmup = new Integer(args[0].split("--warmup=")[1]);

            final HppcMapSyntheticBench testClass = new HppcMapSyntheticBench(nbWarmup);

            //Map synthetic bench
            System.out.println(String.format(">>>>>>>>>>>>>>>>>>>> HPPC HASH MAPS SYNTHETIC BENCH with %d warmup runs ... <<<<<<<<<<<<<<<<<<<<\n", nbWarmup));

            //map iteration benchs
            testClass.runMapIterationBench();
            System.gc();

            testClass.runMapSyntheticBenchPrimitives(MAP_LOOKUP_TEST.MOSTLY_TRUE);
            System.gc();

            testClass.runMapSyntheticBenchPrimitives(MAP_LOOKUP_TEST.MIXED);
            System.gc();

            testClass.runMapSyntheticBenchPrimitives(MAP_LOOKUP_TEST.MOSTLY_FALSE);
            System.gc();

            System.out.println("\n");
            System.out.println("\n");

            testClass.runMapSyntheticBenchIntegers(MAP_LOOKUP_TEST.MOSTLY_TRUE);
            System.gc();

            System.out.println("\n");
            testClass.runMapSyntheticBenchIntegers(MAP_LOOKUP_TEST.MIXED);
            System.gc();

            System.out.println("\n");
            testClass.runMapSyntheticBenchIntegers(MAP_LOOKUP_TEST.MOSTLY_FALSE);
            System.gc();

            System.out.println("\n");
            System.out.println("\n");

            testClass.runMapSyntheticBenchObjects(MAP_LOOKUP_TEST.MOSTLY_TRUE);
            System.gc();

            System.out.println("\n");
            testClass.runMapSyntheticBenchObjects(MAP_LOOKUP_TEST.MIXED);
            System.gc();

            System.out.println("\n");
            testClass.runMapSyntheticBenchObjects(MAP_LOOKUP_TEST.MOSTLY_FALSE);
        }
    }
}
