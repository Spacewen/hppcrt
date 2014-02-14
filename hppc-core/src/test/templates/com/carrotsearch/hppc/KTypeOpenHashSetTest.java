package com.carrotsearch.hppc;

import java.util.Arrays;
import java.util.Random;

import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.carrotsearch.hppc.cursors.IntCursor;
import com.carrotsearch.hppc.cursors.KTypeCursor;
import com.carrotsearch.hppc.predicates.KTypePredicate;
import com.carrotsearch.hppc.procedures.KTypeProcedure;

/**
 * Unit tests for {@link KTypeOpenHashSet}.
 */
//${TemplateOptions.doNotGenerateKType("BOOLEAN")}
/*! ${TemplateOptions.generatedAnnotation} !*/
public class KTypeOpenHashSetTest<KType> extends AbstractKTypeTest<KType>
{
    /**
     * Per-test fresh initialized instance.
     */
    public KTypeOpenHashSet<KType> set;

    public volatile long guard;

    @BeforeClass
    public static void configure()
    {
        IteratorPool.configureInitialPoolSize(8);
    }

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
                    TestUtils.assertEquals2(Intrinsics.defaultKTypeValue(), set.keys[i]);
                    /*! #end !*/
                }
                else
                {
                    occupied++;
                }
            }
            Assert.assertEquals(occupied, set.assigned);
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

        final IntArrayList hashChain = TestUtils.generateMurmurHash3CollisionChain(0x1fff, 0x7e, 0x1fff / 3);

        /*
         * Add all of the conflicting keys to a map.
         */
        for (final IntCursor c : hashChain)
            set.add(cast(c.value));

        Assert.assertEquals(hashChain.size(), set.size());

        /*
         * Add some more keys (random).
         */
        final Random rnd = new Random(0xbabebeef);
        final IntSet chainKeys = IntOpenHashSet.from(hashChain);
        final IntSet differentKeys = new IntOpenHashSet();
        while (differentKeys.size() < 500)
        {
            final int k = rnd.nextInt();
            if (!chainKeys.contains(k) && !differentKeys.contains(k))
                differentKeys.add(k);
        }

        for (final IntCursor c : differentKeys)
            set.add(cast(c.value));

        Assert.assertEquals(hashChain.size() + differentKeys.size(), set.size());

        /*
         * Verify the map contains all of the conflicting keys.
         */
        for (final IntCursor c : hashChain)
            Assert.assertTrue(set.contains(cast(c.value)));

        /*
         * Verify the map contains all the other keys.
         */
        for (final IntCursor c : differentKeys)
            Assert.assertTrue(set.contains(cast(c.value)));

        /*
         * Iteratively remove the keys, from first to last.
         */
        for (final IntCursor c : hashChain)
            Assert.assertTrue(set.remove(cast(c.value)));

        Assert.assertEquals(differentKeys.size(), set.size());

        /*
         * Verify the map contains all the other keys.
         */
        for (final IntCursor c : differentKeys)
            Assert.assertTrue(set.contains(cast(c.value)));
    }

    /* */
    @Test
    public void testInitiallyEmpty()
    {
        Assert.assertEquals(0, set.size());
    }

    /* */
    @Test
    public void testAdd()
    {
        Assert.assertTrue(set.add(key1));
        Assert.assertFalse(set.add(key1));
        Assert.assertEquals(1, set.size());
    }

    /* */
    @Test
    public void testAdd2()
    {
        set.add(key1, key1);
        Assert.assertEquals(1, set.size());
        Assert.assertEquals(1, set.add(key1, key2));
        Assert.assertEquals(2, set.size());
    }

    /* */
    @Test
    public void testAddVarArgs()
    {
        set.add(asArray(0, 1, 2, 1, 0));
        Assert.assertEquals(3, set.size());
        TestUtils.assertSortedListEquals(set.toArray(), 0, 1, 2);
    }

    /* */
    @Test
    public void testAddAll()
    {
        final KTypeOpenHashSet<KType> set2 = new KTypeOpenHashSet<KType>();
        set2.add(asArray(1, 2));
        set.add(asArray(0, 1));

        Assert.assertEquals(1, set.addAll(set2));
        Assert.assertEquals(0, set.addAll(set2));

        Assert.assertEquals(3, set.size());
        TestUtils.assertSortedListEquals(set.toArray(), 0, 1, 2);
    }

    /* */
    @Test
    public void testRemove()
    {
        set.add(asArray(0, 1, 2, 3, 4));

        Assert.assertTrue(set.remove(k2));
        Assert.assertFalse(set.remove(k2));
        Assert.assertEquals(4, set.size());
        TestUtils.assertSortedListEquals(set.toArray(), 0, 1, 3, 4);
    }

    /* */
    @Test
    public void testInitialCapacityAndGrowth()
    {
        for (int i = 0; i < 256; i++)
        {
            final KTypeOpenHashSet<KType> set = new KTypeOpenHashSet<KType>(i);

            for (int j = 0; j < i; j++)
            {
                set.add(cast(j));
            }

            Assert.assertEquals(i, set.size());
        }
    }

    /* */
    @Test
    public void testFullLoadFactor()
    {
        set = new KTypeOpenHashSet<KType>(1, 1f);

        // Fit in the byte key range.
        final int capacity = 0x80;
        final int max = capacity - 1;
        for (int i = 0; i < max; i++)
        {
            set.add(cast(i));
        }

        // Still not expanded.
        Assert.assertEquals(max, set.size());
        Assert.assertEquals(capacity, set.keys.length);
        // Won't expand (existing key).
        set.add(cast(0));
        Assert.assertEquals(capacity, set.keys.length);
        // Expanded.
        set.add(cast(0xff));
        Assert.assertEquals(2 * capacity, set.keys.length);
    }

    /* */
    @Test
    public void testBug_HPPC73_FullCapacityGet()
    {
        set = new KTypeOpenHashSet<KType>(1, 1f);
        final int capacity = 0x80;
        final int max = capacity - 1;
        for (int i = 0; i < max; i++)
        {
            set.add(cast(i));
        }

        Assert.assertEquals(max, set.size());
        Assert.assertEquals(capacity, set.keys.length);

        // Non-existent key.
        set.remove(cast(max + 1));
        Assert.assertFalse(set.contains(cast(max + 1)));

        // Should not expand because we're replacing an existing element.
        Assert.assertFalse(set.add(cast(0)));
        Assert.assertEquals(max, set.size());
        Assert.assertEquals(capacity, set.keys.length);

        // Remove from a full set.
        set.remove(cast(0));
        Assert.assertEquals(max - 1, set.size());
        Assert.assertEquals(capacity, set.keys.length);
    }


    /* */
    @Test
    public void testRemoveAllFromLookupContainer()
    {
        set.add(asArray(0, 1, 2, 3, 4));

        final KTypeOpenHashSet<KType> list2 = new KTypeOpenHashSet<KType>();
        list2.add(asArray(1, 3, 5));

        Assert.assertEquals(2, set.removeAll(list2));
        Assert.assertEquals(3, set.size());
        TestUtils.assertSortedListEquals(set.toArray(), 0, 2, 4);
    }

    /* */
    @Test
    public void testRemoveAllWithPredicate()
    {
        set.add(newArray(k0, k1, k2));

        Assert.assertEquals(1, set.removeAll(new KTypePredicate<KType>()
                {
            @Override
            public boolean apply(final KType v)
            {
                return v == key1;
            };
                }));

        TestUtils.assertSortedListEquals(set.toArray(), 0, key2);
    }

    /* */
    @Test
    public void testRetainAllWithPredicate()
    {
        set.add(newArray(k0, k1, k2, k3, k4, k5));

        Assert.assertEquals(4, set.retainAll(new KTypePredicate<KType>()
                {
            @Override
            public boolean apply(final KType v)
            {
                return v == key1 || v == key2;
            };
                }));

        TestUtils.assertSortedListEquals(set.toArray(), key1, key2);
    }

    /* */
    @Test
    public void testClear()
    {
        set.add(asArray(1, 2, 3));
        set.clear();
        checkTrailingSpaceUninitialized();
        Assert.assertEquals(0, set.size());
    }

    /* */
    @Test
    public void testIterable()
    {
        set.add(asArray(1, 2, 2, 3, 4));
        set.remove(k2);
        Assert.assertEquals(3, set.size());

        int count = 0;
        for (final KTypeCursor<KType> cursor : set)
        {
            count++;
            Assert.assertTrue(set.contains(cursor.value));
            /* #if ($TemplateOptions.KTypeGeneric) */
            TestUtils.assertEquals2(cursor.value, set.lkey());
            /* #end */
        }
        Assert.assertEquals(count, set.size());

        set.clear();
        Assert.assertFalse(set.iterator().hasNext());
    }

    /*! #if ($TemplateOptions.KTypeGeneric) !*/
    @Test
    public void testNullKey()
    {
        set.add((KType) null);
        Assert.assertEquals(1, set.size());
        Assert.assertTrue(set.contains(null));
        Assert.assertTrue(set.remove(null));
        Assert.assertEquals(0, set.size());
        Assert.assertFalse(set.contains(null));
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
                final Integer key = rnd.nextInt(size);

                if (rnd.nextBoolean())
                {
                    other.add(cast(key));
                    set.add(cast(key));

                    Assert.assertTrue(set.contains(cast(key)));
                    TestUtils.assertEquals2(key, set.lkey());
                }
                else
                {
                    Assert.assertEquals(other.remove(key), set.remove(cast(key)));
                }

                Assert.assertEquals(other.size(), set.size());
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
        final KTypeOpenHashSet<Integer> l0 = KTypeOpenHashSet.from();
        Assert.assertEquals(0, l0.hashCode());
        Assert.assertEquals(l0, KTypeOpenHashSet.newInstance());

        final KTypeOpenHashSet<KType> l1 = KTypeOpenHashSet.from(k1, k2, k3);
        final KTypeOpenHashSet<KType> l2 = KTypeOpenHashSet.from(k1, k2);
        l2.add(k3);

        Assert.assertEquals(l1.hashCode(), l2.hashCode());
        Assert.assertEquals(l1, l2);
    }

    /* */
    @Test
    public void testHashCodeEqualsDifferentPerturbance()
    {
        final KTypeOpenHashSet<KType> l0 = new KTypeOpenHashSet<KType>() {
            @Override
            protected int computePerturbationValue(final int capacity)
            {
                return 0xDEADBEEF;
            }
        };

        final KTypeOpenHashSet<KType> l1 = new KTypeOpenHashSet<KType>() {
            @Override
            protected int computePerturbationValue(final int capacity)
            {
                return 0xCAFEBABE;
            }
        };

        Assert.assertEquals(0, l0.hashCode());
        Assert.assertEquals(l0.hashCode(), l1.hashCode());
        Assert.assertEquals(l0, l1);

        l0.add(newArray(k1, k2, k3));
        l1.add(newArray(k1, k2, k3));

        Assert.assertEquals(l0.hashCode(), l1.hashCode());
        Assert.assertEquals(l0, l1);
    }

    /*! #if ($TemplateOptions.KTypeGeneric) !*/
    @SuppressWarnings("unchecked")
    @Test
    public void testHashCodeWithNulls()
    {
        final KTypeOpenHashSet<KType> l1 = KTypeOpenHashSet.from(k1, null, k3);
        final KTypeOpenHashSet<KType> l2 = KTypeOpenHashSet.from(k1, null);
        l2.add(k3);

        Assert.assertEquals(l1.hashCode(), l2.hashCode());
        Assert.assertEquals(l1, l2);
    }
    /*! #end !*/

    /*! #if ($TemplateOptions.KTypeGeneric) !*/
    @SuppressWarnings("unchecked")
    /*! #end !*/
    @Test
    public void testClone()
    {
        this.set.add(key1, key2, key3);

        final KTypeOpenHashSet<KType> cloned = set.clone();
        cloned.removeAllOccurrences(key1);

        TestUtils.assertSortedListEquals(set.toArray(), key1, key2, key3);
        TestUtils.assertSortedListEquals(cloned.toArray(), key2, key3);
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
        final char [] asCharArray = asString.toCharArray();
        Arrays.sort(asCharArray);
        Assert.assertEquals("12", new String(asCharArray));
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
        final long TEST_SEED = 23167132166456L;
        final int TEST_SIZE = (int)500e3;
        final KTypeOpenHashSet<KType> refSet = createSetWithRandomData(TEST_SIZE, null, TEST_SEED);
        KTypeOpenHashSet<KType> refSet2 =createSetWithRandomData(TEST_SIZE, null, TEST_SEED);

        Assert.assertEquals(refSet, refSet2);

        //b) Clone the above. All sets are now identical.
        KTypeOpenHashSet<KType> refSetclone = refSet.clone();
        KTypeOpenHashSet<KType> refSet2clone = refSet2.clone();

        //all strategies are null
        Assert.assertEquals(refSet.strategy(), refSet2.strategy());
        Assert.assertEquals(refSet2.strategy(), refSetclone.strategy());
        Assert.assertEquals(refSetclone.strategy(), refSet2clone.strategy());
        Assert.assertEquals(refSet2clone.strategy(), null);

        Assert.assertEquals(refSet, refSetclone);
        Assert.assertEquals(refSetclone, refSet2);
        Assert.assertEquals(refSet2, refSet2clone);
        Assert.assertEquals(refSet2clone, refSet);

        //cleanup
        refSetclone = null;
        refSet2 = null;
        refSet2clone = null;
        System.gc();

        //c) Create a set nb 3 with same integer content, but with a strategy mapping on equals.
        final KTypeOpenHashSet<KType> refSet3 = createSetWithRandomData(TEST_SIZE,
                new HashingStrategy<KType>() {

            @Override
            public int computeHashCode(final KType object) {

                return object.hashCode();
            }

            @Override
            public boolean equals(final KType o1, final KType o2) {

                return o1.equals(o2);
            }
        }, TEST_SEED);

        //because they do the same thing as above, but with semantically different strategies, ref3 is != ref
        Assert.assertFalse(refSet.equals(refSet3));

        //However, if we cloned refSet3
        final KTypeOpenHashSet<KType> refSet3clone = refSet3.clone();
        Assert.assertEquals(refSet3, refSet3clone);

        //strategies are copied by reference only
        Assert.assertTrue(refSet3.strategy() == refSet3clone.strategy());

        //d) Create identical set with same different strategy instances, but which consider themselves equals()
        KTypeOpenHashSet<KType> refSet4 = createSetWithRandomData(TEST_SIZE,
                new HashingStrategy<KType>() {

            @Override
            public boolean equals(final Object obj) {

                return true;
            }

            @Override
            public int computeHashCode(final KType object) {

                return object.hashCode();
            }

            @Override
            public boolean equals(final KType o1, final KType o2) {

                return o1.equals(o2);
            }
        }, TEST_SEED);

        KTypeOpenHashSet<KType> refSet4Image = createSetWithRandomData(TEST_SIZE,
                new HashingStrategy<KType>() {

            @Override
            public boolean equals(final Object obj) {

                return true;
            }

            @Override
            public int computeHashCode(final KType object) {

                return object.hashCode();
            }

            @Override
            public boolean equals(final KType o1, final KType o2) {

                return o1.equals(o2);
            }
        }, TEST_SEED);

        Assert.assertEquals(refSet4, refSet4Image);
        //but strategy instances are indeed 2 different objects
        Assert.assertFalse(refSet4.strategy() == refSet4Image.strategy());

        //cleanup
        refSet4 = null;
        refSet4Image = null;
        System.gc();

        //e) Do contrary to 4), hashStrategies always != from each other by equals.
        final HashingStrategy<KType> alwaysDifferentStrategy = new HashingStrategy<KType>() {

            @Override
            public boolean equals(final Object obj) {

                //never equal !!!
                return false;
            }

            @Override
            public int computeHashCode(final KType object) {

                return object.hashCode();
            }

            @Override
            public boolean equals(final KType o1, final KType o2) {

                return o1.equals(o2);
            }
        };

        final KTypeOpenHashSet<KType> refSet5 = createSetWithRandomData(TEST_SIZE, alwaysDifferentStrategy, TEST_SEED);
        final KTypeOpenHashSet<KType> refSet5alwaysDifferent = createSetWithRandomData(TEST_SIZE, alwaysDifferentStrategy, TEST_SEED);

        //both sets are NOT equal because their strategies said they are different
        Assert.assertFalse(refSet5.equals(refSet5alwaysDifferent));
    }

    @Test
    public void testHashingStrategyAddContainsRemove()
    {
        //Works only with keys as objects
        Assume.assumeTrue(Object[].class.isInstance(set.keys));

        final long TEST_SEED = 749741621030146103L;
        final int TEST_SIZE = (int)500e3;

        //those following 3  sets behave indeed the same in the test context:
        final KTypeOpenHashSet<KType> refSet = KTypeOpenHashSet.newInstance();

        final KTypeOpenHashSet<KType> refSetNullStrategy = KTypeOpenHashSet.newInstance(
                KTypeOpenHashSet.DEFAULT_CAPACITY,
                KTypeOpenHashSet.DEFAULT_LOAD_FACTOR, null);

        final KTypeOpenHashSet<KType> refSetIdenticalStrategy = KTypeOpenHashSet.newInstance(
                KTypeOpenHashSet.DEFAULT_CAPACITY,
                KTypeOpenHashSet.DEFAULT_LOAD_FACTOR,
                new HashingStrategy<KType>() {

                    @Override
                    public boolean equals(final Object obj) {

                        //always
                        return true;
                    }

                    @Override
                    public int computeHashCode(final KType object) {

                        return object.hashCode();
                    }

                    @Override
                    public boolean equals(final KType o1, final KType o2) {

                        return o1.equals(o2);
                    }
                });

        //compute the iterations doing multiple operations
        final Random prng = new Random(TEST_SEED);

        for (int i = 0; i < TEST_SIZE; i++)
        {
            //a) generate a value to put
            int putValue = prng.nextInt();

            refSet.add(cast(putValue));
            refSetNullStrategy.add(cast(putValue));
            refSetIdenticalStrategy.add(cast(putValue));

            Assert.assertEquals(refSet.contains(cast(putValue)), refSetNullStrategy.contains(cast(putValue)));
            Assert.assertEquals(refSet.contains(cast(putValue)), refSetIdenticalStrategy.contains(cast(putValue)));

            final boolean isToBeRemoved = (prng.nextInt() % 3  == 0);
            putValue = prng.nextInt();

            if (isToBeRemoved)
            {
                refSet.remove(cast(putValue));
                refSetNullStrategy.remove(cast(putValue));
                refSetIdenticalStrategy.remove(cast(putValue));

                Assert.assertFalse(refSet.contains(cast(putValue)));
                Assert.assertFalse(refSetNullStrategy.contains(cast(putValue)));
                Assert.assertFalse(refSetIdenticalStrategy.contains(cast(putValue)));
            }

            Assert.assertEquals(refSet.contains(cast(putValue)), refSetNullStrategy.contains(cast(putValue)));
            Assert.assertEquals(refSet.contains(cast(putValue)), refSetIdenticalStrategy.contains(cast(putValue)));

            //test size
            Assert.assertEquals(refSet.size(), refSetNullStrategy.size());
            Assert.assertEquals(refSet.size(), refSetIdenticalStrategy.size());
        }
    }

    private KTypeOpenHashSet<KType> createSetWithRandomData(final int size, final HashingStrategy<KType> strategy, final long randomSeed)
    {
        final Random prng = new Random(randomSeed);

        final KTypeOpenHashSet<KType> newSet = KTypeOpenHashSet.newInstance(KTypeOpenHashSet.DEFAULT_CAPACITY,
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
        final int TEST_SIZE = 126;
        final long TEST_ROUNDS = 10000;

        final KTypeOpenHashSet<KType> testContainer = createSetWithOrderedData(TEST_SIZE);

        final long checksum = testContainer.forEach(new KTypeProcedure<KType>() {

            long count;
            @Override
            public void apply(final KType value) {

                count += castType(value);
            }
        }).count;

        long testValue = 0;
        final long initialPoolSize = testContainer.entryIteratorPool.size();

        for (int round = 0; round < TEST_ROUNDS; round++)
        {
            //for-each in test :
            testValue = 0;
            for (final KTypeCursor<KType> cursor : testContainer)
            {
                //we consume 1 iterator for this loop
                Assert.assertEquals(initialPoolSize - 1, testContainer.entryIteratorPool.size());

                testValue +=  castType(cursor.value);
            }

            //check checksum the iteration
            Assert.assertEquals(checksum, testValue);

            //iterator is returned to its pool
            Assert.assertEquals(initialPoolSize, testContainer.entryIteratorPool.size());
        } //end for rounds
    }

    @Test
    public void testPooledIteratorBrokenForEach()
    {
        //A) for-each loop interrupted

        //must accommodate even the smallest primitive type
        //so that the iteration do not break before it should...
        final int TEST_SIZE = 126;
        final long TEST_ROUNDS = 10000;

        final KTypeOpenHashSet<KType> testContainer = createSetWithOrderedData(TEST_SIZE);

        int count = 0;
        for (int round = 0; round < TEST_ROUNDS; round++)
        {
            //for-each in test :
            final long initialPoolSize = testContainer.entryIteratorPool.size();

            count = 0;
            for (final KTypeCursor<KType> cursor : testContainer)
            {
                guard += castType(cursor.value);
                //we consume 1 iterator for this loop, but reallocs can happen,
                //so we can only say its != initialPoolSize
                Assert.assertTrue(initialPoolSize != testContainer.entryIteratorPool.size());

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
            Assert.assertTrue(initialPoolSize != testContainer.entryIteratorPool.size());
        } //end for rounds

        //Due to policy of the Iterator pool, the intended pool never get bigger that some limit
        //despite the Iterator leak.
        Assert.assertTrue(testContainer.entryIteratorPool.capacity() < IteratorPool.getMaxPoolSize() + 1);
    }

    @Test
    public void testPooledIteratorFullIteratorLoop()
    {
        //A) for-each loop interrupted

        //must accommodate even the smallest primitive type
        //so that the iteration do not break before it should...
        final int TEST_SIZE = 126;
        final long TEST_ROUNDS = 10000;

        final KTypeOpenHashSet<KType> testContainer = createSetWithOrderedData(TEST_SIZE);

        final long checksum = testContainer.forEach(new KTypeProcedure<KType>() {

            long count;
            @Override
            public void apply(final KType value)
            {
                count += castType(value);
            }
        }).count;

        long testValue = 0;
        final int startingPoolSize = testContainer.entryIteratorPool.size();

        for (int round = 0; round < TEST_ROUNDS; round++)
        {
            //Classical iterator loop, with manually allocated Iterator
            final int initialPoolSize = testContainer.entryIteratorPool.size();

            final KTypeOpenHashSet<KType>.EntryIterator loopIterator = testContainer.iterator();

            Assert.assertEquals(initialPoolSize - 1, testContainer.entryIteratorPool.size());

            testValue = 0;
            while (loopIterator.hasNext())
            {
                testValue += castType(loopIterator.next().value);
            } //end IteratorLoop

            //iterator is returned automatically to its pool, by normal iteration termination
            Assert.assertEquals(initialPoolSize, testContainer.entryIteratorPool.size());

            //checksum
            Assert.assertEquals(checksum, testValue);
        } //end for rounds

        // pool initial size is untouched anyway
        Assert.assertEquals(startingPoolSize, testContainer.entryIteratorPool.size());
    }

    @Test
    public void testPooledIteratorBrokenIteratorLoop()
    {
        //A) for-each loop interrupted

        //must accommodate even the smallest primitive type
        //so that the iteration do not break before it should...
        final int TEST_SIZE = 126;
        final long TEST_ROUNDS = 10000;

        final KTypeOpenHashSet<KType> testContainer = createSetWithOrderedData(TEST_SIZE);
        final int startingPoolSize = testContainer.entryIteratorPool.size();

        int count = 0;
        for (int round = 0; round < TEST_ROUNDS; round++)
        {
            //Classical iterator loop, with manually allocated Iterator
            final long initialPoolSize = testContainer.entryIteratorPool.size();

            final KTypeOpenHashSet<KType>.EntryIterator loopIterator = testContainer.iterator();

            Assert.assertEquals(initialPoolSize - 1, testContainer.entryIteratorPool.size());

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
            Assert.assertEquals(initialPoolSize - 1, testContainer.entryIteratorPool.size());

            //manual return to the pool
            loopIterator.release();

            //now the pool is restored
            Assert.assertEquals(initialPoolSize, testContainer.entryIteratorPool.size());

        } //end for rounds

        // pool initial size is untouched anyway
        Assert.assertEquals(startingPoolSize, testContainer.entryIteratorPool.size());
    }

    @Test
    public void testPooledIteratorExceptionIteratorLoop()
    {
        //must accommodate even the smallest primitive type
        //so that the iteration do not break before it should...
        final int TEST_SIZE = 126;
        final long TEST_ROUNDS = 10000;

        final KTypeOpenHashSet<KType> testContainer = createSetWithOrderedData(TEST_SIZE);

        final long checksum = testContainer.forEach(new KTypeProcedure<KType>() {

            long count;

            @Override
            public void apply(final KType value)
            {
                count += castType(value);
            }
        }).count;

        final int startingPoolSize = testContainer.entryIteratorPool.size();

        int count = 0;
        KTypeOpenHashSet<KType>.EntryIterator loopIterator = null;

        for (int round = 0; round < TEST_ROUNDS; round++)
        {
            try
            {
                loopIterator = testContainer.iterator();

                Assert.assertEquals(startingPoolSize - 1, testContainer.entryIteratorPool.size());

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
                Assert.assertEquals(startingPoolSize, testContainer.entryIteratorPool.size());
                Assert.assertEquals(checksum, guard);

            } catch (final Exception e)
            {
                //iterator is NOT returned to its pool because of the exception
                Assert.assertEquals(startingPoolSize - 1, testContainer.entryIteratorPool.size());

                //manual return to the pool then
                loopIterator.release();

                //now the pool is restored
                Assert.assertEquals(startingPoolSize, testContainer.entryIteratorPool.size());
            }
        } //end for rounds

        // pool initial size is untouched anyway
        Assert.assertEquals(startingPoolSize, testContainer.entryIteratorPool.size());
    }

    @Test
    public void testPreallocatedSize()
    {
        final Random randomVK = new Random();
        //Test that the container do not resize if less that the initial size

        final int NB_TEST_RUNS = 50;

        for (int run = 0; run < NB_TEST_RUNS; run++)
        {
            //1) Choose a random number of elements
            /*! #if ($TemplateOptions.isKType("GENERIC", "INT", "LONG", "FLOAT", "DOUBLE")) !*/
            final int PREALLOCATED_SIZE = randomVK.nextInt(100000);
            /*!
            #elseif ($TemplateOptions.isKType("SHORT", "CHAR"))
             int PREALLOCATED_SIZE = randomVK.nextInt(15000);
            #else
              int PREALLOCATED_SIZE = randomVK.nextInt(126);
            #end !*/

            //2) Preallocate to PREALLOCATED_SIZE :
            final KTypeOpenHashSet<KType> newSet = KTypeOpenHashSet.newInstanceWithCapacity(PREALLOCATED_SIZE,
                    KTypeOpenHashSet.DEFAULT_LOAD_FACTOR);

            //3) Add PREALLOCATED_SIZE different values. At the end, size() must be == PREALLOCATED_SIZE,
            //and internal buffer/allocated must not have changed of size
            final int contructorBufferSize = newSet.keys.length;

            Assert.assertEquals(contructorBufferSize, newSet.allocated.length);

            for (int i = 0 ; i < PREALLOCATED_SIZE; i++) {

                newSet.add(cast(i));

                //internal size has not changed.
                Assert.assertEquals(contructorBufferSize, newSet.keys.length);
                Assert.assertEquals(contructorBufferSize, newSet.allocated.length);
            }

            Assert.assertEquals(PREALLOCATED_SIZE, newSet.size());
        } //end for test runs
    }


    private KTypeOpenHashSet<KType> createSetWithOrderedData(final int size)
    {

        final KTypeOpenHashSet<KType> newSet = KTypeOpenHashSet.newInstanceWithCapacity(KTypeOpenHashSet.DEFAULT_CAPACITY,
                KTypeOpenHashSet.DEFAULT_LOAD_FACTOR);

        for (int i = 0; i < size ; i++) {

            newSet.add(cast(i));
        }

        return newSet;
    }

}
