package com.carrotsearch.hppc.misc;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javolution.util.FastMap;

import com.carrotsearch.hppc.HashingStrategy;
import com.carrotsearch.hppc.IntArrayList;
import com.carrotsearch.hppc.IntLongMap;
import com.carrotsearch.hppc.IntLongOpenHashMap;
import com.carrotsearch.hppc.IntLongRobinHoodHashMap;
import com.carrotsearch.hppc.IntOpenHashSet;
import com.carrotsearch.hppc.LongArrayList;
import com.carrotsearch.hppc.ObjectArrayList;
import com.carrotsearch.hppc.ObjectLongMap;
import com.carrotsearch.hppc.ObjectLongOpenHashMap;
import com.carrotsearch.hppc.ObjectLongRobinHoodHashMap;
import com.carrotsearch.hppc.XorShiftRandom;
import com.carrotsearch.hppc.cursors.IntLongCursor;
import com.carrotsearch.hppc.mutables.IntHolder;
import com.carrotsearch.hppc.procedures.LongProcedure;

public final class HppcMapSyntheticBench
{
    public static final int COUNT = (int) 4e6;

    public static final int COUNT_BIG_OBJECTS = (int) 100e3;

    public static final long RANDOM_SEED = 5487911234761188L;
    private static final long RAND_SEED = 15487012316864131L;
    private static final long RAND_SEED2 = 9988713416546546L;
    private static final long RAND_SEED3 = 412316451315451545L;
    private static final long RAND_SEED4 = 2345613216796312185L;

    public static final int NB_WARMUPS = 10;

    public static final int NB_WARMUPS_ITERATION_BENCH = 50;

    public Random prng = new XorShiftRandom();

    private long totalSum;

    enum MAP_LOOKUP_TEST {

        MOSTLY_TRUE,
        MOSTLY_FALSE,
        MIXED
    }

    //closure like
    private final LongProcedure sumProcedureInstance = new LongProcedure() {

        @Override
        public void apply(final long value) {
            HppcMapSyntheticBench.this.totalSum += value;
        }
    };

    // inner comparator class
    public final class InverseComparator implements Comparator<ComparableLong> {

        @Override
        public int compare(final ComparableLong o1, final ComparableLong o2) {

            int res = 0;

            if (o1.value > o2.value) {

                res = -1;

            }
            else if (o1.value < o2.value) {

                res = 1;
            }

            return res;
        }
    }

    public static final class NaturalComparator implements Comparator<ComparableLong> {

        @Override
        public int compare(final ComparableLong o1, final ComparableLong o2) {

            int res = 0;

            if (o1.value < o2.value) {

                res = -1;

            }
            else if (o1.value > o2.value) {

                res = 1;
            }

            return res;
        }
    }

    // create comparable type
    public static final class ComparableLong implements Comparable<ComparableLong> {

        public long value;

        public ComparableLong(final long initValue) {

            this.value = initValue;
        }

        @Override
        public int compareTo(final ComparableLong other) {
            int res = 0;

            if (this.value < other.value) {
                res = -1;
            }

            else if (this.value > other.value) {
                res = 1;
            }

            return res;
        }

        @Override
        public Object clone() throws CloneNotSupportedException {

            return new ComparableLong(this.value);
        }

        @Override
        public boolean equals(final Object obj) {

            return this.value == ((ComparableLong) obj).value;
        }

        @Override
        public int hashCode() {

            return (int) this.value;
        }
    }

    // create comparable type
    public static final class ComparableAsciiString implements Comparable<ComparableAsciiString> {

        public final byte[] value;

        /**
         * Default constructor generates a random character content based on prng.
         */
        public ComparableAsciiString(final int size, final Random prng) {

            this.value = new byte[size];
            //fill with random data
            prng.nextBytes(this.value);
        }

        /**
         * Copy constructor
         * @param src
         */
        public ComparableAsciiString(final ComparableAsciiString src) {

            this.value = src.value.clone();
        }

        @Override
        public int compareTo(final ComparableAsciiString other) {

            if (this.value.length < other.value.length) {

                return -1;
            }
            else if (this.value.length > other.value.length) {

                return 1;

            }
            else {
                //both length are equal
                for (int ii = 0; ii < this.value.length; ii++) {

                    if (this.value[ii] < other.value[ii]) {

                        return -1;

                    }
                    else if (this.value[ii] > other.value[ii]) {

                        return 1;
                    }
                } //end for

                return 0;
            }
        }

        @Override
        public Object clone() throws CloneNotSupportedException {

            return new ComparableAsciiString(this);
        }

        @Override
        public boolean equals(final Object obj) {

            return Arrays.equals(this.value, ((ComparableAsciiString) obj).value);
        }

        @Override
        public int hashCode() {

            int h = 31;

            for (final byte b : this.value) {

                h += b;
            }

            return h;
        }
    }

    /**
     * Constructor
     */
    public HppcMapSyntheticBench() {

        this.prng.setSeed(HppcMapSyntheticBench.RANDOM_SEED);
    }

    /**
     * Bench for Primitive int ==> long, fill the container up to its load factor:
     */
    public void runMapPrimitiveInt(final String additionalInfo, final IntLongMap testMap,
            final int minPushedElements, final float loadFactor, final int nbWarmupRuns, final MAP_LOOKUP_TEST getKind)
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
        int finalSize = 0;
        int putSize = 0;

        final int initialCapacity = testMap.capacity();

        //Do a dry run for enumerating the values that are to be put in map :
        final IntArrayList Klist = new IntArrayList();
        final LongArrayList Vlist = new LongArrayList();

        this.prng.setSeed(HppcMapSyntheticBench.RAND_SEED);

        while (testMap.capacity() < minPushedElements || testMap.size() < testMap.capacity()) {

            final int K = this.prng.nextInt();
            final long V = this.prng.nextLong();
            Klist.add(K);
            Vlist.add(V);

            testMap.put(K, V);
        }

        testMap.clear();
        System.gc();

        while (nbWarmups <= nbWarmupRuns) {

            testMap.clear();

            nbWarmups++;

            tBeforePut = System.nanoTime();

            //A) fill until the capacity, and at least minPushedElements
            for (int ii = 0; ii < Klist.size(); ii++) {

                //put all, then for even key number, delete by key
                testMap.put(Klist.get(ii), Vlist.get(ii));
            }

            tAfterPut = System.nanoTime();

            putSize = testMap.size();

            tBeforeGet = System.nanoTime();

            // B) Process by get/contains
            for (int ii = 0; ii < Klist.size(); ii++) {

                if ((getKind == MAP_LOOKUP_TEST.MOSTLY_TRUE || getKind == MAP_LOOKUP_TEST.MIXED) && this.prng.nextBoolean()) {

                    sum += testMap.get(Klist.get(ii));
                }
                else if (getKind == MAP_LOOKUP_TEST.MOSTLY_FALSE || getKind == MAP_LOOKUP_TEST.MIXED) {
                    //this element may or may not be in the set
                    sum += testMap.get(this.prng.nextInt());
                }
            }

            tAfterGet = System.nanoTime();
            ///////////////////////////////////////

            finalSize = testMap.size();

            //C) Remove Op
            tBeforeRemove = System.nanoTime();

            for (int ii = 0; ii < Klist.size(); ii++) {

                if ((getKind == MAP_LOOKUP_TEST.MOSTLY_TRUE || getKind == MAP_LOOKUP_TEST.MIXED) && this.prng.nextBoolean()) {

                    sum += testMap.remove(Klist.get(ii));
                }
                else if (getKind == MAP_LOOKUP_TEST.MOSTLY_FALSE || getKind == MAP_LOOKUP_TEST.MIXED) {
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

        System.out.format(">>>> BENCH: HPPC Map (int, long), (%s),  initial capacity = %d, load factor = %f, %d elements pushed, actual final size = %d\n"
                + " Put = %f ms, Get (%s) = %f ms,  Remove (%s) = %f ms,  Clear =  %f ms (dummy = %d)\n\n",
                additionalInfo,
                initialCapacity,
                loadFactor,
                putSize,
                finalSize,
                (tAfterPut - tBeforePut) / 1e6,
                getKind,
                (tAfterGet - tBeforeGet) / 1e6,
                getKind,
                (tAfterRemove - tBeforeRemove) / 1e6,
                (tAfterClear - tBeforeClear) / 1e6,
                sum); //outputs the results to defeat optimizations
    }

    /**
     * Map HPPC (int ==> long) iteration bench
     * @param testMap
     * @param additionalInfo
     * @param nbWarmupRuns
     */
    private void runMapIterationBench(final String additionalInfo, final IntLongMap testMap,
            final int minPushedElements, final float loadFactor, final int nbWarmupRuns)
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

        while (testMap.capacity() < minPushedElements || testMap.size() < testMap.capacity()) {

            final int K = this.prng.nextInt();
            final long V = this.prng.nextLong();
            Klist.add(K);
            Vlist.add(V);

            testMap.put(K, V);
        }

        //we need the testMap to be not-empty !!
        System.gc();

        //Iteration Procedure to sum the elements

        while (nbWarmups <= nbWarmupRuns) {

            nbWarmups++;

            //A) sum with iterator
            tBefore = System.nanoTime();

            sumIt = 0;
            for (final IntLongCursor cursor : testMap) {

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

            if (testMap instanceof IntLongOpenHashMap) {

                final IntLongOpenHashMap m = (IntLongOpenHashMap) testMap;

                for (int ii = 0; ii < m.allocated.length; ii++)
                {
                    if (m.allocated[ii])
                    {
                        sumDirect += m.values[ii];
                    }
                }
            }
            else if (testMap instanceof IntLongRobinHoodHashMap)
            {
                final IntLongRobinHoodHashMap m = (IntLongRobinHoodHashMap) testMap;

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

        if (!((sumForeach == sumIt) && (sumIt == sumDirect))) {
            throw new AssertionError(String.format("ERROR, (Iterator sum = %d) != (forEachSum = %d) != (directSum = %d)", sumIt, sumForeach, sumDirect));
        }

        System.out.format(">>>> BENCH: HPPC Map (int, long), (%s) Iteration test,  initial capacity = %d, load factor = %f, %d elements summed,\n"
                + " Iterator= %f ms, foreach= %f ms, Direct= %f ms (result = %d)\n\n",
                additionalInfo,
                initialCapacity,
                loadFactor,
                testMap.size(),
                tExecIteratorMs, tExecForEachMs, tExecDirectMs,
                sumForeach); //outputs the results to defeat optimizations
    }

    /**
     * Bench for HPPC (Integer Object ==> long).
     *
     */
    public void runMapIntegerObjectLong(final String additionalInfo, final ObjectLongMap<IntHolder> testMap,
            final int minPushedElements, final float loadFactor, final int nbWarmupRuns, final MAP_LOOKUP_TEST getKind)
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
        int finalSize = 0;
        int putSize = 0;

        final int initialCapacity = testMap.capacity();

        //Do a dry run for pre-boxing the values that are to be put in map :
        final ObjectArrayList<IntHolder> Klist = new ObjectArrayList<IntHolder>();
        final LongArrayList Vlist = new LongArrayList();

        this.prng.setSeed(HppcMapSyntheticBench.RAND_SEED);

        while (testMap.capacity() < minPushedElements || testMap.size() < testMap.capacity()) {

            //put all, then for even key number, delete by key
            final IntHolder K = new IntHolder(this.prng.nextInt());
            final long V = this.prng.nextLong();
            Klist.add(K);
            Vlist.add(V);

            testMap.put(K, V);
        }

        testMap.clear();
        System.gc();

        //The main measuring loop
        while (nbWarmups <= nbWarmupRuns) {

            testMap.clear();

            nbWarmups++;

            tBeforePut = System.nanoTime();

            //A) fill until the capacity, and at least minPushedElements
            for (int ii = 0; ii < Klist.size(); ii++) {

                //put all, then for even key number, delete by key
                testMap.put(Klist.get(ii), Vlist.get(ii));
            }

            tAfterPut = System.nanoTime();

            putSize = testMap.size();

            tBeforeGet = System.nanoTime();

            // B) Process by get/contains
            final IntHolder tmpAbsentIntHolder = new IntHolder();

            //Rerun to compute a containsKey lget() pattern
            for (int ii = 0; ii < Klist.size(); ii++) {

                if ((getKind == MAP_LOOKUP_TEST.MOSTLY_TRUE || getKind == MAP_LOOKUP_TEST.MIXED) && this.prng.nextBoolean()) {

                    sum += testMap.get(Klist.get(ii));
                }
                else if (getKind == MAP_LOOKUP_TEST.MOSTLY_FALSE || getKind == MAP_LOOKUP_TEST.MIXED) {

                    //this element may or may not be in the set
                    tmpAbsentIntHolder.value = this.prng.nextInt();
                    sum += testMap.get(tmpAbsentIntHolder);
                }
            }

            tAfterGet = System.nanoTime();
            ///////////////////////////////////////

            finalSize = testMap.size();

            //C) Remove Op
            tBeforeRemove = System.nanoTime();

            for (int ii = 0; ii < Klist.size(); ii++) {

                if ((getKind == MAP_LOOKUP_TEST.MOSTLY_TRUE || getKind == MAP_LOOKUP_TEST.MIXED) && this.prng.nextBoolean()) {

                    sum += testMap.remove(Klist.get(ii));
                }
                else if (getKind == MAP_LOOKUP_TEST.MOSTLY_FALSE || getKind == MAP_LOOKUP_TEST.MIXED) {

                    //this element may or may not be in the set
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

        System.out.format(">>>> BENCH: HPPC Map (IntHolder object, long), (%s),  initial capacity = %d, load factor = %f, %d elements pushed, actual final size = %d\n"
                + " Put = %f ms, Get (%s) = %f ms, Remove (%s) = %f ms,  Clear =  %f ms (dummy = %d)\n\n",
                additionalInfo,
                initialCapacity,
                loadFactor,
                putSize,
                finalSize,
                (tAfterPut - tBeforePut) / 1e6,
                getKind,
                (tAfterGet - tBeforeGet) / 1e6,
                getKind,
                (tAfterRemove - tBeforeRemove) / 1e6,
                (tAfterClear - tBeforeClear) / 1e6,
                sum); //outputs the results to defeat optimizations
    }

    /**
     * java.util.Map compatible test
     * @param testMap
     * @param nbWarmupRuns
     */
    public void runMapJavaUtilInteger(final Map<IntHolder, Long> testMap, final int minPushedElements, final int nbWarmupRuns, final MAP_LOOKUP_TEST getKind) {

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
        int finalSize = 0;
        int putSize = 0;

        //Do a dry run for pre-boxing the values that are to be put in map :
        final ObjectArrayList<IntHolder> Klist = new ObjectArrayList<IntHolder>();
        final ObjectArrayList<Long> Vlist = new ObjectArrayList<Long>();

        this.prng.setSeed(HppcMapSyntheticBench.RAND_SEED);

        //use a HPPC int set to determine the final size
        IntOpenHashSet testSet = new IntOpenHashSet(minPushedElements);

        while (testSet.capacity() < minPushedElements || testSet.size() < testSet.capacity()) {

            //put all, then for even key number, delete by key
            final IntHolder K = new IntHolder(this.prng.nextInt());
            final Long V = new Long(this.prng.nextLong());
            Klist.add(K);
            Vlist.add(V);

            testSet.add(K.value);
            testMap.put(K, V);
        }

        //no longer used
        testSet = null;

        testMap.clear();
        System.gc();

        while (nbWarmups <= nbWarmupRuns) {

            testMap.clear();

            nbWarmups++;

            tBeforePut = System.nanoTime();

            //A) fill until the capacity, and at least minPushedElements
            for (int ii = 0; ii < Klist.size(); ii++) {

                //put all, then for even key number, delete by key
                testMap.put(Klist.get(ii), Vlist.get(ii));
            }

            tAfterPut = System.nanoTime();

            putSize = testMap.size();

            tBeforeGet = System.nanoTime();

            // B) Process by get/contains
            final IntHolder tmpAbsentIntHolder = new IntHolder();

            Long currentValue = null;

            for (int ii = 0; ii < Klist.size(); ii++) {

                if ((getKind == MAP_LOOKUP_TEST.MOSTLY_TRUE || getKind == MAP_LOOKUP_TEST.MIXED) && this.prng.nextBoolean()) {

                    currentValue = testMap.get(Klist.get(ii));
                    if (currentValue != null) {
                        sum += currentValue.longValue();
                    }
                }
                else if (getKind == MAP_LOOKUP_TEST.MOSTLY_FALSE || getKind == MAP_LOOKUP_TEST.MIXED) {

                    //this element may or may not be in the set
                    tmpAbsentIntHolder.value = this.prng.nextInt();
                    currentValue = testMap.get(tmpAbsentIntHolder);

                    if (currentValue != null) {
                        sum += currentValue.longValue();
                    }
                }
            }

            tAfterGet = System.nanoTime();
            ///////////////////////////////////////

            finalSize = testMap.size();

            //C) Remove Op
            tBeforeRemove = System.nanoTime();

            for (int ii = 0; ii < Klist.size(); ii++) {

                if ((getKind == MAP_LOOKUP_TEST.MOSTLY_TRUE || getKind == MAP_LOOKUP_TEST.MIXED) && this.prng.nextBoolean()) {

                    currentValue = testMap.remove(Klist.get(ii));
                    if (currentValue != null) {
                        sum += currentValue.longValue();
                    }
                }
                else if (getKind == MAP_LOOKUP_TEST.MOSTLY_FALSE || getKind == MAP_LOOKUP_TEST.MIXED) {

                    //this element may or may not be in the set
                    //this element may or may not be in the set
                    tmpAbsentIntHolder.value = this.prng.nextInt();
                    currentValue = testMap.remove(tmpAbsentIntHolder);

                    if (currentValue != null) {
                        sum += currentValue.longValue();
                    }
                }
            }

            tAfterRemove = System.nanoTime();

            //clear op
            tBeforeClear = System.nanoTime();

            testMap.clear();

            tAfterClear = System.nanoTime();

        } //end while

        System.out.format(">>>> BENCH: java.util.Map of kind '%s', (IntHolder object, Long),  initial capacity = %d, %d elements pushed, actual final size = %d\n"
                + " Put = %f ms, Get (%s) = %f ms, Remove (%s) = %f ms,  Clear =  %f ms (dummy = %d)\n\n",
                testMap.getClass().getCanonicalName(),
                minPushedElements,
                putSize,
                finalSize,
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
            final int minPushedElements, final float loadFactor, final int nbWarmupRuns, final MAP_LOOKUP_TEST getKind, final int stringSize)
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
        int finalSize = 0;
        int putSize = 0;

        final int initialCapacity = testMap.capacity();

        //Do a dry run for pre-boxing the values that are to be put in map :
        final ObjectArrayList<ComparableAsciiString> Klist = new ObjectArrayList<ComparableAsciiString>();
        final LongArrayList Vlist = new LongArrayList();

        this.prng.setSeed(HppcMapSyntheticBench.RAND_SEED);

        while (testMap.capacity() < minPushedElements || testMap.size() < testMap.capacity()) {

            //put all, then for even key number, delete by key
            final ComparableAsciiString K = new ComparableAsciiString(stringSize, this.prng);
            final long V = this.prng.nextLong();
            Klist.add(K);
            Vlist.add(V);

            testMap.put(K, V);
        }

        final ObjectArrayList<ComparableAsciiString> KlistNotFound = new ObjectArrayList<ComparableAsciiString>();

        for (int ii = 0; ii < testMap.capacity() * 0.7; ii++) {

            KlistNotFound.add(new ComparableAsciiString(stringSize, this.prng));
        }

        testMap.clear();
        System.gc();

        //The main measuring loop
        while (nbWarmups <= nbWarmupRuns) {

            testMap.clear();

            nbWarmups++;

            tBeforePut = System.nanoTime();

            //A) fill until the capacity, and at least minPushedElements
            for (int ii = 0; ii < Klist.size(); ii++) {

                //put all, then for even key number, delete by key
                testMap.put(Klist.get(ii), Vlist.get(ii));
            }

            tAfterPut = System.nanoTime();

            putSize = testMap.size();

            tBeforeGet = System.nanoTime();

            // B) Process by get/contains
            final int notFoundCount = 0;

            //Rerun to compute a containsKey lget() pattern
            for (int ii = 0; ii < Klist.size(); ii++) {

                if ((getKind == MAP_LOOKUP_TEST.MOSTLY_TRUE || getKind == MAP_LOOKUP_TEST.MIXED) && this.prng.nextBoolean()) {

                    sum += testMap.get(Klist.get(ii));
                }
                else if (getKind == MAP_LOOKUP_TEST.MOSTLY_FALSE || getKind == MAP_LOOKUP_TEST.MIXED) {

                    //this element is not in the set
                    sum += testMap.get(KlistNotFound.get(notFoundCount));
                }
            }

            tAfterGet = System.nanoTime();
            ///////////////////////////////////////

            finalSize = testMap.size();

            //C) Remove Op
            tBeforeRemove = System.nanoTime();

            for (int ii = 0; ii < Klist.size(); ii++) {

                if ((getKind == MAP_LOOKUP_TEST.MOSTLY_TRUE || getKind == MAP_LOOKUP_TEST.MIXED) && this.prng.nextBoolean()) {

                    sum += testMap.remove(Klist.get(ii));
                }
                else if (getKind == MAP_LOOKUP_TEST.MOSTLY_FALSE || getKind == MAP_LOOKUP_TEST.MIXED) {

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

        System.out.format(">>>> BENCH: HPPC Map (ComparableAsciiString %d bytes long, long), (%s),  initial capacity = %d, load factor = %f, %d elements pushed, actual final size = %d\n"
                + " Put = %f ms, Get (%s) = %f ms, Remove (%s) = %f ms, Clear =  %f ms (dummy = %d)\n\n",
                stringSize,
                additionalInfo,
                initialCapacity,
                loadFactor,
                putSize,
                finalSize,
                (tAfterPut - tBeforePut) / 1e6,
                getKind,
                (tAfterGet - tBeforeGet) / 1e6,
                getKind,
                (tAfterRemove - tBeforeRemove) / 1e6,
                (tAfterClear - tBeforeClear) / 1e6,
                sum); //outputs the results to defeat optimizations
    }

    public void runMapSyntheticBenchPrimitives(final MAP_LOOKUP_TEST getKind, final int nbWarmups) {

        // Preallocate at maximum size
        runMapPrimitiveInt("IntLongOpenHashMap",
                IntLongOpenHashMap.newInstance(HppcMapSyntheticBench.COUNT, IntLongOpenHashMap.DEFAULT_LOAD_FACTOR),
                HppcMapSyntheticBench.COUNT, IntLongOpenHashMap.DEFAULT_LOAD_FACTOR,
                nbWarmups, getKind);
        System.gc();

        runMapPrimitiveInt("IntLongRobinHoodHashMap",
                IntLongRobinHoodHashMap.newInstance(HppcMapSyntheticBench.COUNT, IntLongRobinHoodHashMap.DEFAULT_LOAD_FACTOR),
                HppcMapSyntheticBench.COUNT, IntLongRobinHoodHashMap.DEFAULT_LOAD_FACTOR,
                nbWarmups, getKind);
        System.gc();
    }

    public void runMapSyntheticBenchObjects(final MAP_LOOKUP_TEST getKind, final int nbWarmups) {

        //Javolution comparison
        runMapJavaUtilInteger(new FastMap<IntHolder, Long>(HppcMapSyntheticBench.COUNT), HppcMapSyntheticBench.COUNT, nbWarmups, getKind);
        System.gc();

        //Default java.util.HashMap
        runMapJavaUtilInteger(new HashMap<IntHolder, Long>(HppcMapSyntheticBench.COUNT), HppcMapSyntheticBench.COUNT, nbWarmups, getKind);
        System.gc();

        runMapIntegerObjectLong("ObjectLongOpenHashMap",
                ObjectLongOpenHashMap.<IntHolder> newInstance(HppcMapSyntheticBench.COUNT, ObjectLongOpenHashMap.DEFAULT_LOAD_FACTOR),
                HppcMapSyntheticBench.COUNT, ObjectLongOpenHashMap.DEFAULT_LOAD_FACTOR,
                nbWarmups, getKind);
        System.gc();

        runMapIntegerObjectLong("ObjectLongRobinHoodHashMap",
                ObjectLongRobinHoodHashMap.<IntHolder> newInstance(HppcMapSyntheticBench.COUNT, ObjectLongRobinHoodHashMap.DEFAULT_LOAD_FACTOR),
                HppcMapSyntheticBench.COUNT, ObjectLongRobinHoodHashMap.DEFAULT_LOAD_FACTOR,
                nbWarmups, getKind);
        System.gc();

        // use specialized strategy
        runMapIntegerObjectLong("ObjectLongOpenHashMap with strategy",
                ObjectLongOpenHashMap.<IntHolder> newInstance(
                        HppcMapSyntheticBench.COUNT, IntLongOpenHashMap.DEFAULT_LOAD_FACTOR,
                        new HashingStrategy<IntHolder>() {

                            @Override
                            public int computeHashCode(final IntHolder o)
                            {
                                return o.value;
                            }

                            @Override
                            public boolean equals(final IntHolder o1, final IntHolder o2)
                            {
                                return o1.value == o2.value;
                            }

                        }),
                HppcMapSyntheticBench.COUNT, ObjectLongOpenHashMap.DEFAULT_LOAD_FACTOR, nbWarmups, getKind);

        System.gc();

        // use specialized strategy
        runMapIntegerObjectLong("ObjectLongRobinHoodHashMap with strategy",
                ObjectLongRobinHoodHashMap.<IntHolder> newInstance(
                        HppcMapSyntheticBench.COUNT, IntLongOpenHashMap.DEFAULT_LOAD_FACTOR,
                        new HashingStrategy<IntHolder>() {

                            @Override
                            public int computeHashCode(final IntHolder o)
                            {
                                return o.value;
                            }

                            @Override
                            public boolean equals(final IntHolder o1, final IntHolder o2)
                            {
                                return o1.value == o2.value;
                            }
                        }),
                HppcMapSyntheticBench.COUNT, ObjectLongRobinHoodHashMap.DEFAULT_LOAD_FACTOR, nbWarmups, getKind);

        System.gc();

        runMapAsciiStringObjectLong("ObjectLongOpenHashMap",
                ObjectLongOpenHashMap.<ComparableAsciiString> newInstance(HppcMapSyntheticBench.COUNT_BIG_OBJECTS, ObjectLongOpenHashMap.DEFAULT_LOAD_FACTOR),
                HppcMapSyntheticBench.COUNT_BIG_OBJECTS, ObjectLongOpenHashMap.DEFAULT_LOAD_FACTOR,
                nbWarmups, getKind, 16);
        System.gc();

        runMapAsciiStringObjectLong("ObjectLongRobinHoodHashMap",
                ObjectLongRobinHoodHashMap.<ComparableAsciiString> newInstance(HppcMapSyntheticBench.COUNT_BIG_OBJECTS, ObjectLongRobinHoodHashMap.DEFAULT_LOAD_FACTOR),
                HppcMapSyntheticBench.COUNT_BIG_OBJECTS, ObjectLongRobinHoodHashMap.DEFAULT_LOAD_FACTOR,
                nbWarmups, getKind, 16);
        System.gc();

        runMapAsciiStringObjectLong("ObjectLongOpenHashMap",
                ObjectLongOpenHashMap.<ComparableAsciiString> newInstance(HppcMapSyntheticBench.COUNT_BIG_OBJECTS, ObjectLongOpenHashMap.DEFAULT_LOAD_FACTOR),
                HppcMapSyntheticBench.COUNT_BIG_OBJECTS, ObjectLongOpenHashMap.DEFAULT_LOAD_FACTOR,
                nbWarmups, getKind, 32);
        System.gc();

        runMapAsciiStringObjectLong("ObjectLongRobinHoodHashMap",
                ObjectLongRobinHoodHashMap.<ComparableAsciiString> newInstance(HppcMapSyntheticBench.COUNT_BIG_OBJECTS, ObjectLongRobinHoodHashMap.DEFAULT_LOAD_FACTOR),
                HppcMapSyntheticBench.COUNT_BIG_OBJECTS, ObjectLongRobinHoodHashMap.DEFAULT_LOAD_FACTOR,
                nbWarmups, getKind, 32);
        System.gc();

        runMapAsciiStringObjectLong("ObjectLongOpenHashMap",
                ObjectLongOpenHashMap.<ComparableAsciiString> newInstance(HppcMapSyntheticBench.COUNT_BIG_OBJECTS, ObjectLongOpenHashMap.DEFAULT_LOAD_FACTOR),
                HppcMapSyntheticBench.COUNT_BIG_OBJECTS, ObjectLongOpenHashMap.DEFAULT_LOAD_FACTOR,
                nbWarmups, getKind, 64);
        System.gc();

        runMapAsciiStringObjectLong("ObjectLongRobinHoodHashMap",
                ObjectLongRobinHoodHashMap.<ComparableAsciiString> newInstance(HppcMapSyntheticBench.COUNT_BIG_OBJECTS, ObjectLongRobinHoodHashMap.DEFAULT_LOAD_FACTOR),
                HppcMapSyntheticBench.COUNT_BIG_OBJECTS, ObjectLongRobinHoodHashMap.DEFAULT_LOAD_FACTOR,
                nbWarmups, getKind, 64);
        System.gc();

        runMapAsciiStringObjectLong("ObjectLongOpenHashMap",
                ObjectLongOpenHashMap.<ComparableAsciiString> newInstance(HppcMapSyntheticBench.COUNT_BIG_OBJECTS, ObjectLongOpenHashMap.DEFAULT_LOAD_FACTOR),
                HppcMapSyntheticBench.COUNT_BIG_OBJECTS, ObjectLongOpenHashMap.DEFAULT_LOAD_FACTOR,
                nbWarmups, getKind, 128);
        System.gc();

        runMapAsciiStringObjectLong("ObjectLongRobinHoodHashMap",
                ObjectLongRobinHoodHashMap.<ComparableAsciiString> newInstance(HppcMapSyntheticBench.COUNT_BIG_OBJECTS, ObjectLongRobinHoodHashMap.DEFAULT_LOAD_FACTOR),
                HppcMapSyntheticBench.COUNT_BIG_OBJECTS, ObjectLongRobinHoodHashMap.DEFAULT_LOAD_FACTOR,
                nbWarmups, getKind, 128);
        System.gc();

        runMapAsciiStringObjectLong("ObjectLongOpenHashMap",
                ObjectLongOpenHashMap.<ComparableAsciiString> newInstance(HppcMapSyntheticBench.COUNT_BIG_OBJECTS, ObjectLongOpenHashMap.DEFAULT_LOAD_FACTOR),
                HppcMapSyntheticBench.COUNT_BIG_OBJECTS, ObjectLongOpenHashMap.DEFAULT_LOAD_FACTOR,
                nbWarmups, getKind, 256);
        System.gc();

        runMapAsciiStringObjectLong("ObjectLongRobinHoodHashMap",
                ObjectLongRobinHoodHashMap.<ComparableAsciiString> newInstance(HppcMapSyntheticBench.COUNT_BIG_OBJECTS, ObjectLongRobinHoodHashMap.DEFAULT_LOAD_FACTOR),
                HppcMapSyntheticBench.COUNT_BIG_OBJECTS, ObjectLongRobinHoodHashMap.DEFAULT_LOAD_FACTOR,
                nbWarmups, getKind, 256);
        System.gc();
    }

    public void runMapIterationBench() {

        System.gc();
        runMapIterationBench("IntLongOpenHashMap", new IntLongOpenHashMap(HppcMapSyntheticBench.COUNT), HppcMapSyntheticBench.COUNT, IntLongOpenHashMap.DEFAULT_LOAD_FACTOR,
                HppcMapSyntheticBench.NB_WARMUPS_ITERATION_BENCH);
        System.gc();
        runMapIterationBench("IntLongRobinHoodHashMap", new IntLongRobinHoodHashMap(HppcMapSyntheticBench.COUNT), HppcMapSyntheticBench.COUNT, IntLongRobinHoodHashMap.DEFAULT_LOAD_FACTOR,
                HppcMapSyntheticBench.NB_WARMUPS_ITERATION_BENCH);
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /**
     * main
     */
    public static void main(final String[] args) {

        final HppcMapSyntheticBench testClass = new HppcMapSyntheticBench();

        //Map synthetic bench
        System.out.println(">>>>>>> MAP SYNTHETIC BENCH : With preallocation to maximum size :  \n");

        //map iteration benchs
        testClass.runMapIterationBench();
        System.gc();

        testClass.runMapSyntheticBenchPrimitives(MAP_LOOKUP_TEST.MOSTLY_TRUE, HppcMapSyntheticBench.NB_WARMUPS);
        System.gc();

        testClass.runMapSyntheticBenchPrimitives(MAP_LOOKUP_TEST.MIXED, HppcMapSyntheticBench.NB_WARMUPS);
        System.gc();

        testClass.runMapSyntheticBenchPrimitives(MAP_LOOKUP_TEST.MOSTLY_FALSE, HppcMapSyntheticBench.NB_WARMUPS);
        System.gc();

        System.out.println("\n");
        System.out.println("\n");

        testClass.runMapSyntheticBenchObjects(MAP_LOOKUP_TEST.MOSTLY_TRUE, HppcMapSyntheticBench.NB_WARMUPS);
        System.gc();

        System.out.println("\n");
        testClass.runMapSyntheticBenchObjects(MAP_LOOKUP_TEST.MIXED, HppcMapSyntheticBench.NB_WARMUPS);

        System.out.println("\n");
        testClass.runMapSyntheticBenchObjects(MAP_LOOKUP_TEST.MOSTLY_FALSE, HppcMapSyntheticBench.NB_WARMUPS);
    }
}
