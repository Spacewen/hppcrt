package com.carrotsearch.hppc;

import static com.carrotsearch.hppc.TestUtils.assertEquals2;
import static com.carrotsearch.hppc.TestUtils.assertSortedListEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Random;

import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import com.carrotsearch.hppc.cursors.IntCursor;
import com.carrotsearch.hppc.cursors.KTypeCursor;
import com.carrotsearch.hppc.predicates.KTypePredicate;
import com.carrotsearch.hppc.procedures.KTypeProcedure;

/**
 * Unit tests for {@link KTypeOpenHashSet}.
 */
/*! ${TemplateOptions.generatedAnnotation} !*/
public class KTypeOpenHashSetTest<KType> extends AbstractKTypeTest<KType>
{
    /**
     * Per-test fresh initialized instance.
     */
    public KTypeOpenHashSet<KType> set;

    public volatile long guard;

    /* */
    @Before
    public void initialize()
    {
        set = KTypeOpenHashSet.newInstance();
    }

    @After
    public void checkTrailingSpaceUninitialized()
    {
        if (set != null)
        {
            int occupied = 0;
            for (int i = 0; i < set.keys.length; i++)
            {
                if (!set.allocated[i])
                {
                    /*! #if ($TemplateOptions.KTypeGeneric) !*/
                    assertEquals2(Intrinsics.defaultKTypeValue(), set.keys[i]);
                    /*! #end !*/
                }
                else
                {
                    occupied++;
                }
            }
            assertEquals(occupied, set.assigned);
        }
    }

    @Test
    public void testAddRemoveSameHashCollision()
    {
        // This test is only applicable to selected key types.
        Assume.assumeTrue(
                int[].class.isInstance(set.keys) ||
                long[].class.isInstance(set.keys) ||
                Object[].class.isInstance(set.keys));

        IntArrayList hashChain = TestUtils.generateMurmurHash3CollisionChain(0x1fff, 0x7e, 0x1fff / 3);

        /*
         * Add all of the conflicting keys to a map.
         */
        for (IntCursor c : hashChain)
            set.add(cast(c.value));

        assertEquals(hashChain.size(), set.size());

        /*
         * Add some more keys (random).
         */
        Random rnd = new Random(0xbabebeef);
        IntSet chainKeys = IntOpenHashSet.from(hashChain);
        IntSet differentKeys = new IntOpenHashSet();
        while (differentKeys.size() < 500)
        {
            int k = rnd.nextInt();
            if (!chainKeys.contains(k) && !differentKeys.contains(k))
                differentKeys.add(k);
        }

        for (IntCursor c : differentKeys)
            set.add(cast(c.value));

        assertEquals(hashChain.size() + differentKeys.size(), set.size());

        /*
         * Verify the map contains all of the conflicting keys.
         */
        for (IntCursor c : hashChain)
            assertTrue(set.contains(cast(c.value)));

        /*
         * Verify the map contains all the other keys.
         */
        for (IntCursor c : differentKeys)
            assertTrue(set.contains(cast(c.value)));

        /*
         * Iteratively remove the keys, from first to last.
         */
        for (IntCursor c : hashChain)
            assertTrue(set.remove(cast(c.value)));

        assertEquals(differentKeys.size(), set.size());

        /*
         * Verify the map contains all the other keys.
         */
        for (IntCursor c : differentKeys)
            assertTrue(set.contains(cast(c.value)));
    }

    /* */
    @Test
    public void testInitiallyEmpty()
    {
        assertEquals(0, set.size());
    }

    /* */
    @Test
    public void testAdd()
    {
        assertTrue(set.add(key1));
        assertFalse(set.add(key1));
        assertEquals(1, set.size());
    }

    /* */
    @Test
    public void testAdd2()
    {
        set.add(key1, key1);
        assertEquals(1, set.size());
        assertEquals(1, set.add(key1, key2));
        assertEquals(2, set.size());
    }

    /* */
    @Test
    public void testAddVarArgs()
    {
        set.add(asArray(0, 1, 2, 1, 0));
        assertEquals(3, set.size());
        assertSortedListEquals(set.toArray(), 0, 1, 2);
    }

    /* */
    @Test
    public void testAddAll()
    {
        KTypeOpenHashSet<KType> set2 = new KTypeOpenHashSet<KType>();
        set2.add(asArray(1, 2));
        set.add(asArray(0, 1));

        assertEquals(1, set.addAll(set2));
        assertEquals(0, set.addAll(set2));

        assertEquals(3, set.size());
        assertSortedListEquals(set.toArray(), 0, 1, 2);
    }

    /* */
    @Test
    public void testRemove()
    {
        set.add(asArray(0, 1, 2, 3, 4));

        assertTrue(set.remove(k2));
        assertFalse(set.remove(k2));
        assertEquals(4, set.size());
        assertSortedListEquals(set.toArray(), 0, 1, 3, 4);
    }

    /* */
    @Test
    public void testInitialCapacityAndGrowth()
    {
        for (int i = 0; i < 256; i++)
        {
            KTypeOpenHashSet<KType> set = new KTypeOpenHashSet<KType>(i);

            for (int j = 0; j < i; j++)
            {
                set.add(cast(j));
            }

            assertEquals(i, set.size());
        }
    }

    /* */
    @Test
    public void testBug_HPPC73_FullCapacityGet()
    {
        set = new KTypeOpenHashSet<KType>(1, 1f);
        int capacity = 0x80;
        int max = capacity - 1;
        for (int i = 0; i < max; i++)
        {
            set.add(cast(i));
        }

        assertEquals(max, set.size());
        assertEquals(capacity, set.keys.length);

        // Non-existent key.
        set.remove(cast(max + 1));
        assertFalse(set.contains(cast(max + 1)));

        // Should not expand because we're replacing an existing element.
        assertFalse(set.add(cast(0)));
        assertEquals(max, set.size());
        assertEquals(capacity, set.keys.length);

        // Remove from a full set.
        set.remove(cast(0));
        assertEquals(max - 1, set.size());
        assertEquals(capacity, set.keys.length);
    }


    /* */
    @Test
    public void testRemoveAllFromLookupContainer()
    {
        set.add(asArray(0, 1, 2, 3, 4));

        KTypeOpenHashSet<KType> list2 = new KTypeOpenHashSet<KType>();
        list2.add(asArray(1, 3, 5));

        assertEquals(2, set.removeAll(list2));
        assertEquals(3, set.size());
        assertSortedListEquals(set.toArray(), 0, 2, 4);
    }

    /* */
    @Test
    public void testRemoveAllWithPredicate()
    {
        set.add(newArray(k0, k1, k2));

        assertEquals(1, set.removeAll(new KTypePredicate<KType>()
                {
            public boolean apply(KType v)
            {
                return v == key1;
            };
                }));

        assertSortedListEquals(set.toArray(), 0, key2);
    }

    /* */
    @Test
    public void testRetainAllWithPredicate()
    {
        set.add(newArray(k0, k1, k2, k3, k4, k5));

        assertEquals(4, set.retainAll(new KTypePredicate<KType>()
                {
            public boolean apply(KType v)
            {
                return v == key1 || v == key2;
            };
                }));

        assertSortedListEquals(set.toArray(), key1, key2);
    }

    /* */
    @Test
    public void testClear()
    {
        set.add(asArray(1, 2, 3));
        set.clear();
        checkTrailingSpaceUninitialized();
        assertEquals(0, set.size());
    }

    /* */
    @Test
    public void testIterable()
    {
        set.add(asArray(1, 2, 2, 3, 4));
        set.remove(k2);
        assertEquals(3, set.size());

        int count = 0;
        for (KTypeCursor<KType> cursor : set)
        {
            count++;
            assertTrue(set.contains(cursor.value));
            /* #if ($TemplateOptions.KTypeGeneric) */
            assertEquals2(cursor.value, set.lkey());
            /* #end */
        }
        assertEquals(count, set.size());

        set.clear();
        assertFalse(set.iterator().hasNext());
    }

    /*! #if ($TemplateOptions.KTypeGeneric) !*/
    @Test
    public void testNullKey()
    {
        set.add((KType) null);
        assertEquals(1, set.size());
        assertTrue(set.contains(null));
        assertTrue(set.remove(null));
        assertEquals(0, set.size());
        assertFalse(set.contains(null));
    }
    /*! #end !*/

    /*! #if ($TemplateOptions.KTypeGeneric) !*/
    /**
     * Run some random insertions/ deletions and compare the results
     * against <code>java.util.HashSet</code>.
     */
    @Test
    public void testAgainstHashMap()
    {
        final java.util.Random rnd = new java.util.Random();
        final java.util.HashSet<KType> other = new java.util.HashSet<KType>();

        for (int size = 1000; size < 20000; size += 4000)
        {
            other.clear();
            set.clear();

            for (int round = 0; round < size * 20; round++)
            {
                Integer key = rnd.nextInt(size);

                if (rnd.nextBoolean())
                {
                    other.add(cast(key));
                    set.add(cast(key));

                    assertTrue(set.contains(cast(key)));
                    assertEquals2(key, set.lkey());
                }
                else
                {
                    assertEquals(other.remove(key), set.remove(cast(key)));
                }

                assertEquals(other.size(), set.size());
            }
        }
    }
    /*! #end !*/

    /* */
    @Test
    /*! #if ($TemplateOptions.KTypeGeneric) !*/
    @SuppressWarnings("unchecked")
    /*! #end !*/
    public void testHashCodeEquals()
    {
        KTypeOpenHashSet<Integer> l0 = KTypeOpenHashSet.from();
        assertEquals(0, l0.hashCode());
        assertEquals(l0, KTypeOpenHashSet.newInstance());

        KTypeOpenHashSet<KType> l1 = KTypeOpenHashSet.from(k1, k2, k3);
        KTypeOpenHashSet<KType> l2 = KTypeOpenHashSet.from(k1, k2);
        l2.add(k3);

        assertEquals(l1.hashCode(), l2.hashCode());
        assertEquals(l1, l2);
    }

    /* */
    @Test
    public void testHashCodeEqualsDifferentPerturbance()
    {
        KTypeOpenHashSet<KType> l0 = new KTypeOpenHashSet<KType>() {
            @Override
            protected int computePerturbationValue(int capacity)
            {
                return 0xDEADBEEF;
            }
        };

        KTypeOpenHashSet<KType> l1 = new KTypeOpenHashSet<KType>() {
            @Override
            protected int computePerturbationValue(int capacity)
            {
                return 0xCAFEBABE;
            }
        };

        assertEquals(0, l0.hashCode());
        assertEquals(l0.hashCode(), l1.hashCode());
        assertEquals(l0, l1);

        l0.add(newArray(k1, k2, k3));
        l1.add(newArray(k1, k2, k3));

        assertEquals(l0.hashCode(), l1.hashCode());
        assertEquals(l0, l1);
    }

    /*! #if ($TemplateOptions.KTypeGeneric) !*/
    @SuppressWarnings("unchecked")
    @Test
    public void testHashCodeWithNulls()
    {
        KTypeOpenHashSet<KType> l1 = KTypeOpenHashSet.from(k1, null, k3);
        KTypeOpenHashSet<KType> l2 = KTypeOpenHashSet.from(k1, null);
        l2.add(k3);

        assertEquals(l1.hashCode(), l2.hashCode());
        assertEquals(l1, l2);
    }
    /*! #end !*/

    /*! #if ($TemplateOptions.KTypeGeneric) !*/
    @SuppressWarnings("unchecked")
    /*! #end !*/
    @Test
    public void testClone()
    {
        this.set.add(key1, key2, key3);

        KTypeOpenHashSet<KType> cloned = set.clone();
        cloned.removeAllOccurrences(key1);

        assertSortedListEquals(set.toArray(), key1, key2, key3);
        assertSortedListEquals(cloned.toArray(), key2, key3);
    }

    /*
     * 
     */
    @Test
    public void testToString()
    {
        Assume.assumeTrue(
                int[].class.isInstance(set.keys)     ||
                short[].class.isInstance(set.keys)   ||
                byte[].class.isInstance(set.keys)    ||
                long[].class.isInstance(set.keys)    ||
                Object[].class.isInstance(set.keys));

        this.set.add(key1, key2);
        String asString = set.toString();
        asString = asString.replaceAll("[\\[\\],\\ ]", "");
        char [] asCharArray = asString.toCharArray();
        Arrays.sort(asCharArray);
        assertEquals("12", new String(asCharArray));
    }

    /*! #if ($TemplateOptions.KTypeGeneric) !*/
    //only applicable to generic types keys
    @Test
    public void testHashingStrategyCloneEquals()
    {

        //Works only with keys as objects
        Assume.assumeTrue(Object[].class.isInstance(set.keys));

        //a) Check that 2 different sets filled the same way with same values and strategies = null
        //are indeed equal.
        long TEST_SEED = 23167132166456L;
        int TEST_SIZE = (int)500e3;
        KTypeOpenHashSet<KType> refSet = createSetWithRandomData(TEST_SIZE, null, TEST_SEED);
        KTypeOpenHashSet<KType> refSet2 =createSetWithRandomData(TEST_SIZE, null, TEST_SEED);

        assertEquals(refSet, refSet2);

        //b) Clone the above. All sets are now identical.
        KTypeOpenHashSet<KType> refSetclone = refSet.clone();
        KTypeOpenHashSet<KType> refSet2clone = refSet2.clone();

        //all strategies are null
        assertEquals(refSet.strategy(), refSet2.strategy());
        assertEquals(refSet2.strategy(), refSetclone.strategy());
        assertEquals(refSetclone.strategy(), refSet2clone.strategy());
        assertEquals(refSet2clone.strategy(), null);

        assertEquals(refSet, refSetclone);
        assertEquals(refSetclone, refSet2);
        assertEquals(refSet2, refSet2clone);
        assertEquals(refSet2clone, refSet);

        //cleanup
        refSetclone = null;
        refSet2 = null;
        refSet2clone = null;
        System.gc();

        //c) Create a set nb 3 with same integer content, but with a strategy mapping on equals.
        KTypeOpenHashSet<KType> refSet3 = createSetWithRandomData(TEST_SIZE,
                new HashingStrategy<KType>() {

            @Override
            public int computeHashCode(KType object) {

                return object.hashCode();
            }

            @Override
            public boolean equals(KType o1, KType o2) {

                return o1.equals(o2);
            }
        }, TEST_SEED);

        //because they do the same thing as above, but with semantically different strategies, ref3 is != ref
        assertFalse(refSet.equals(refSet3));

        //However, if we cloned refSet3
        KTypeOpenHashSet<KType> refSet3clone = refSet3.clone();
        assertEquals(refSet3, refSet3clone);

        //strategies are copied by reference only
        assertTrue(refSet3.strategy() == refSet3clone.strategy());

        //d) Create identical set with same different strategy instances, but which consider themselves equals()
        KTypeOpenHashSet<KType> refSet4 = createSetWithRandomData(TEST_SIZE,
                new HashingStrategy<KType>() {

            @Override
            public boolean equals(Object obj) {

                return true;
            }

            @Override
            public int computeHashCode(KType object) {

                return object.hashCode();
            }

            @Override
            public boolean equals(KType o1, KType o2) {

                return o1.equals(o2);
            }
        }, TEST_SEED);

        KTypeOpenHashSet<KType> refSet4Image = createSetWithRandomData(TEST_SIZE,
                new HashingStrategy<KType>() {

            @Override
            public boolean equals(Object obj) {

                return true;
            }

            @Override
            public int computeHashCode(KType object) {

                return object.hashCode();
            }

            @Override
            public boolean equals(KType o1, KType o2) {

                return o1.equals(o2);
            }
        }, TEST_SEED);

        assertEquals(refSet4, refSet4Image);
        //but strategy instances are indeed 2 different objects
        assertFalse(refSet4.strategy() == refSet4Image.strategy());

        //cleanup
        refSet4 = null;
        refSet4Image = null;
        System.gc();

        //e) Do contrary to 4), hashStrategies always != from each other by equals.
        HashingStrategy<KType> alwaysDifferentStrategy = new HashingStrategy<KType>() {

            @Override
            public boolean equals(Object obj) {

                //never equal !!!
                return false;
            }

            @Override
            public int computeHashCode(KType object) {

                return object.hashCode();
            }

            @Override
            public boolean equals(KType o1, KType o2) {

                return o1.equals(o2);
            }
        };

        KTypeOpenHashSet<KType> refSet5 = createSetWithRandomData(TEST_SIZE, alwaysDifferentStrategy, TEST_SEED);
        KTypeOpenHashSet<KType> refSet5alwaysDifferent = createSetWithRandomData(TEST_SIZE, alwaysDifferentStrategy, TEST_SEED);

        //both sets are NOT equal because their strategies said they are different
        assertFalse(refSet5.equals(refSet5alwaysDifferent));
    }

    @Test
    public void testHashingStrategyAddContainsRemove()
    {
        //Works only with keys as objects
        Assume.assumeTrue(Object[].class.isInstance(set.keys));

        long TEST_SEED = 749741621030146103L;
        int TEST_SIZE = (int)500e3;

        //those following 3  sets behave indeed the same in the test context:
        KTypeOpenHashSet<KType> refSet = KTypeOpenHashSet.newInstance();

        KTypeOpenHashSet<KType> refSetNullStrategy = KTypeOpenHashSet.newInstanceWithCapacityAndStrategy(
                KTypeOpenHashSet.DEFAULT_CAPACITY,
                KTypeOpenHashSet.DEFAULT_LOAD_FACTOR, null);

        KTypeOpenHashSet<KType> refSetIdenticalStrategy = KTypeOpenHashSet.newInstanceWithCapacityAndStrategy(
                KTypeOpenHashSet.DEFAULT_CAPACITY,
                KTypeOpenHashSet.DEFAULT_LOAD_FACTOR,
                new HashingStrategy<KType>() {

                    @Override
                    public boolean equals(Object obj) {

                        //always
                        return true;
                    }

                    @Override
                    public int computeHashCode(KType object) {

                        return object.hashCode();
                    }

                    @Override
                    public boolean equals(KType o1, KType o2) {

                        return o1.equals(o2);
                    }
                });

        //compute the iterations doing multiple operations
        Random prng = new Random(TEST_SEED);

        for (int i = 0; i < TEST_SIZE; i++)
        {
            //a) generate a value to put
            int putValue = prng.nextInt();

            refSet.add(cast(putValue));
            refSetNullStrategy.add(cast(putValue));
            refSetIdenticalStrategy.add(cast(putValue));

            assertEquals(refSet.contains(cast(putValue)), refSetNullStrategy.contains(cast(putValue)));
            assertEquals(refSet.contains(cast(putValue)), refSetIdenticalStrategy.contains(cast(putValue)));

            boolean isToBeRemoved = (prng.nextInt() % 3  == 0);
            putValue = prng.nextInt();

            if (isToBeRemoved)
            {
                refSet.remove(cast(putValue));
                refSetNullStrategy.remove(cast(putValue));
                refSetIdenticalStrategy.remove(cast(putValue));

                assertFalse(refSet.contains(cast(putValue)));
                assertFalse(refSetNullStrategy.contains(cast(putValue)));
                assertFalse(refSetIdenticalStrategy.contains(cast(putValue)));
            }

            assertEquals(refSet.contains(cast(putValue)), refSetNullStrategy.contains(cast(putValue)));
            assertEquals(refSet.contains(cast(putValue)), refSetIdenticalStrategy.contains(cast(putValue)));

            //test size
            assertEquals(refSet.size(), refSetNullStrategy.size());
            assertEquals(refSet.size(), refSetIdenticalStrategy.size());
        }
    }

    private KTypeOpenHashSet<KType> createSetWithRandomData(int size, HashingStrategy<KType> strategy, long randomSeed)
    {
        Random prng = new Random(randomSeed);

        KTypeOpenHashSet<KType> newSet = KTypeOpenHashSet.newInstanceWithCapacityAndStrategy(KTypeOpenHashSet.DEFAULT_CAPACITY,
                KTypeOpenHashSet.DEFAULT_LOAD_FACTOR, strategy);

        for (int i = 0; i < size; i++)
        {
            newSet.add(cast(prng.nextInt()));
        }

        return newSet;
    }
    /*! #end !*/

    @Test
    public void testPooledIteratorForEach()
    {
        //A) Unbroken for-each loop
        //must accommodate even the smallest primitive type
        //so that the iteration do not break before it should...
        int TEST_SIZE = 126;
        long TEST_ROUNDS = 10000;

        KTypeOpenHashSet<KType> testContainer = createSetWithOrderedData(TEST_SIZE);

        long checksum = testContainer.forEach(new KTypeProcedure<KType>() {

            long count;
            @Override
            public void apply(KType value) {

                count += castType(value);
            }
        }).count;

        long testValue = 0;
        long initialPoolSize = testContainer.entryIteratorPool.size();

        for (int round = 0; round < TEST_ROUNDS; round++)
        {
            //for-each in test :
            testValue = 0;
            for (KTypeCursor<KType> cursor : testContainer)
            {
                //we consume 1 iterator for this loop
                assertEquals(initialPoolSize - 1, testContainer.entryIteratorPool.size());

                testValue +=  castType(cursor.value);
            }

            //check checksum the iteration
            assertEquals(checksum, testValue);

            //iterator is returned to its pool
            assertEquals(initialPoolSize, testContainer.entryIteratorPool.size());
        } //end for rounds
    }

    @Test
    public void testPooledIteratorBrokenForEach()
    {
        //A) for-each loop interrupted

        //must accommodate even the smallest primitive type
        //so that the iteration do not break before it should...
        int TEST_SIZE = 126;
        long TEST_ROUNDS = 10000;

        KTypeOpenHashSet<KType> testContainer = createSetWithOrderedData(TEST_SIZE);

        int count = 0;
        for (int round = 0; round < TEST_ROUNDS; round++)
        {
            //for-each in test :
            long initialPoolSize = testContainer.entryIteratorPool.size();

            count = 0;
            for (KTypeCursor<KType> cursor : testContainer)
            {
                guard += castType(cursor.value);
                //we consume 1 iterator for this loop, but reallocs can happen,
                //so we can only say its != initialPoolSize
                assertTrue(initialPoolSize != testContainer.entryIteratorPool.size());

                //brutally interrupt in the middle
                if (count > TEST_SIZE / 2)
                {
                    break;
                }

                count++;
            } //end for-each

            //iterator is NOT returned to its pool, due to the break.
            //reallocation could happen, so that the only testable thing
            //is that the size is != full pool
            assertTrue(initialPoolSize != testContainer.entryIteratorPool.size());
        } //end for rounds

        //Due to policy of the Iterator pool, the intended pool never get bigger that some limit
        //despite the Iterator leak.
        assertTrue(testContainer.entryIteratorPool.capacity() < IteratorPool.MAX_SIZE + 1);
    }

    @Test
    public void testPooledIteratorFullIteratorLoop()
    {
        //A) for-each loop interrupted

        //must accommodate even the smallest primitive type
        //so that the iteration do not break before it should...
        int TEST_SIZE = 126;
        long TEST_ROUNDS = 10000;

        KTypeOpenHashSet<KType> testContainer = createSetWithOrderedData(TEST_SIZE);

        long checksum = testContainer.forEach(new KTypeProcedure<KType>() {

            long count;
            @Override
            public void apply(KType value)
            {
                count += castType(value);
            }
        }).count;

        long testValue = 0;
        int startingPoolSize = testContainer.entryIteratorPool.size();

        for (int round = 0; round < TEST_ROUNDS; round++)
        {
            //Classical iterator loop, with manually allocated Iterator
            int initialPoolSize = testContainer.entryIteratorPool.size();

            Iterator<KTypeCursor<KType>> loopIterator = testContainer.iterator();

            assertEquals(initialPoolSize - 1, testContainer.entryIteratorPool.size());

            testValue = 0;
            while (loopIterator.hasNext())
            {
                testValue += castType(loopIterator.next().value);
            } //end IteratorLoop

            //iterator is returned automatically to its pool, by normal iteration termination
            assertEquals(initialPoolSize, testContainer.entryIteratorPool.size());

            //checksum
            assertEquals(checksum, testValue);
        } //end for rounds

        // pool initial size is untouched anyway
        assertEquals(startingPoolSize, testContainer.entryIteratorPool.size());
    }

    @Test
    public void testPooledIteratorBrokenIteratorLoop()
    {
        //A) for-each loop interrupted

        //must accommodate even the smallest primitive type
        //so that the iteration do not break before it should...
        int TEST_SIZE = 126;
        long TEST_ROUNDS = 10000;

        KTypeOpenHashSet<KType> testContainer = createSetWithOrderedData(TEST_SIZE);
        int startingPoolSize = testContainer.entryIteratorPool.size();

        int count = 0;
        for (int round = 0; round < TEST_ROUNDS; round++)
        {
            //Classical iterator loop, with manually allocated Iterator
            long initialPoolSize = testContainer.entryIteratorPool.size();

            AbstractIterator<KTypeCursor<KType>> loopIterator = (AbstractIterator<KTypeCursor<KType>>) testContainer.iterator();

            assertEquals(initialPoolSize - 1, testContainer.entryIteratorPool.size());

            count = 0;
            while (loopIterator.hasNext())
            {
                guard += castType(loopIterator.next().value);

                //brutally interrupt in the middle
                if (count > TEST_SIZE / 2)
                {
                    break;
                }
                count++;
            } //end IteratorLoop

            //iterator is NOT returned to its pool, due to the break.
            assertEquals(initialPoolSize - 1, testContainer.entryIteratorPool.size());

            //manual return to the pool
            loopIterator.release();

            //now the pool is restored
            assertEquals(initialPoolSize, testContainer.entryIteratorPool.size());

        } //end for rounds

        // pool initial size is untouched anyway
        assertEquals(startingPoolSize, testContainer.entryIteratorPool.size());
    }

    @Test
    public void testPooledIteratorExceptionIteratorLoop()
    {
        //must accommodate even the smallest primitive type
        //so that the iteration do not break before it should...
        int TEST_SIZE = 126;
        long TEST_ROUNDS = 10000;

        KTypeOpenHashSet<KType> testContainer = createSetWithOrderedData(TEST_SIZE);

        long checksum = testContainer.forEach(new KTypeProcedure<KType>() {

            long count;

            @Override
            public void apply(KType value)
            {
                count += castType(value);
            }
        }).count;

        int startingPoolSize = testContainer.entryIteratorPool.size();

        int count = 0;
        AbstractIterator<KTypeCursor<KType>> loopIterator = null;

        for (int round = 0; round < TEST_ROUNDS; round++)
        {
            try
            {
                loopIterator = (AbstractIterator<KTypeCursor<KType>>) testContainer.iterator();

                assertEquals(startingPoolSize - 1, testContainer.entryIteratorPool.size());

                guard = 0;
                count = 0;
                while (loopIterator.hasNext())
                {
                    guard += castType(loopIterator.next().value);

                    //brutally interrupt in the middle some of the loops, but not all
                    if (round > TEST_ROUNDS / 2 && count > TEST_SIZE / 2)
                    {
                        throw new Exception("Oups some problem in the loop occured");
                    }
                    count++;
                } //end while

                //iterator is returned to its pool in case of normal loop termination
                assertEquals(startingPoolSize, testContainer.entryIteratorPool.size());
                assertEquals(checksum, guard);

            } catch (Exception e)
            {
                //iterator is NOT returned to its pool because of the exception
                assertEquals(startingPoolSize - 1, testContainer.entryIteratorPool.size());

                //manual return to the pool then
                loopIterator.release();

                //now the pool is restored
                assertEquals(startingPoolSize, testContainer.entryIteratorPool.size());
            }
        } //end for rounds

        // pool initial size is untouched anyway
        assertEquals(startingPoolSize, testContainer.entryIteratorPool.size());
    }

    private KTypeOpenHashSet<KType> createSetWithOrderedData(int size)
    {

        KTypeOpenHashSet<KType> newSet = KTypeOpenHashSet.newInstanceWithCapacity(KTypeOpenHashSet.DEFAULT_CAPACITY,
                KTypeOpenHashSet.DEFAULT_LOAD_FACTOR);

        for (int i = 0; i < size ; i++) {

            newSet.add(cast(i));
        }

        return newSet;
    }

}
