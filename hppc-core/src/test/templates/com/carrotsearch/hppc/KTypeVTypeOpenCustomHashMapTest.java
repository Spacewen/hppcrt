package com.carrotsearch.hppc;

import java.util.*;

import org.junit.*;

import static com.carrotsearch.hppc.TestUtils.*;
import static org.junit.Assert.*;

import com.carrotsearch.hppc.cursors.*;
import com.carrotsearch.hppc.predicates.*;
import com.carrotsearch.hppc.procedures.*;
import com.carrotsearch.hppc.strategies.*;

/**
 * Tests for {@link KTypeVTypeOpenCustomHashMap}.
 */
// ${TemplateOptions.doNotGenerateKType("BOOLEAN")}
//${TemplateOptions.doNotGenerateVType("BOOLEAN")}
/*! #set( $ROBIN_HOOD_FOR_PRIMITIVES = false) !*/
/*! #set( $ROBIN_HOOD_FOR_GENERICS = true) !*/
// If RH is defined, RobinHood Hashing is in effect :
/*! #set( $RH = (($TemplateOptions.KTypeGeneric && $ROBIN_HOOD_FOR_GENERICS) || ($TemplateOptions.KTypeNumeric && $ROBIN_HOOD_FOR_PRIMITIVES)) ) !*/
/*! ${TemplateOptions.generatedAnnotation} !*/
public class KTypeVTypeOpenCustomHashMapTest<KType, VType> extends AbstractKTypeVTypeTest<KType, VType>
{

    @BeforeClass
    public static void configure()
    {
        IteratorPool.configureInitialPoolSize(8);
    }

    /**
     * Per-test fresh initialized instance.
     */
    public KTypeVTypeOpenCustomHashMap<KType, VType> map = KTypeVTypeOpenCustomHashMap.newInstance(new KTypeStandardHash<KType>());

    /**
     * Check that the set is consistent, i.e all allocated slots are reachable by get(),
     * and all not-allocated contains nulls if Generic
     * @param set
     */
    @After
    public void checkConsistency()
    {
        if (map != null)
        {
            int occupied = 0;

            final int mask = map.allocated.length - 1;

            for (int i = 0; i < map.keys.length; i++)
            {
                if (/*! #if ($RH) !*/
                        map.allocated[i] == -1
                        /*!#else
                !map.allocated[i]
                #end !*/)
                {
                    //if not allocated, generic version if patched to null for GC sake

                    /*! #if ($TemplateOptions.KTypeGeneric) !*/
                    TestUtils.assertEquals2(Intrinsics.defaultKTypeValue(), map.keys[i]);
                    /*! #end !*/
                    /*! #if ($TemplateOptions.VTypeGeneric) !*/
                    TestUtils.assertEquals2(Intrinsics.defaultVTypeValue(), map.values[i]);
                    /*! #end !*/
                }
                else
                {
                    /*! #if ($RH) !*/
                    //check hash cache consistency
                    Assert.assertEquals(Internals.rehash(map.strategy().computeHashCode(map.keys[i])) & mask, map.allocated[i]);
                    /*! #end !*/

                    //try to reach the key by contains()
                    Assert.assertTrue(map.containsKey(map.keys[i]));

                    //check slot
                    Assert.assertEquals(i, map.lslot());

                    //get() test
                    Assert.assertEquals(vcastType(map.values[i]), vcastType(map.get(map.keys[i])));

                    //retreive again by lkey(), lget() :
                    Assert.assertEquals(castType(map.keys[i]), castType(map.lkey()));
                    Assert.assertEquals(vcastType(map.values[i]), vcastType(map.lget()));

                    occupied++;
                }
            }
            Assert.assertEquals(occupied, map.assigned);
        }
    }

    /* */
    @Test
    public void testCloningConstructor()
    {
        map.put(key1, value1);
        map.put(key2, value2);
        map.put(key3, value3);

        assertSameMap(map, KTypeVTypeOpenCustomHashMap.from(map, new KTypeStandardHash<KType>()));
        assertSameMap(map, new KTypeVTypeOpenCustomHashMap<KType, VType>(map, new KTypeStandardHash<KType>()));
    }

    /* */
    /*! #if ($TemplateOptions.VTypeGeneric) !*/
    @SuppressWarnings("unchecked")
    /*! #end !*/
    @Test
    public void testFromArrays()
    {
        map.put(key1, value1);
        map.put(key2, value2);
        map.put(key3, value3);

        final KTypeVTypeOpenCustomHashMap<KType, VType> map2 = KTypeVTypeOpenCustomHashMap.from(
                newArray(key1, key2, key3),
                newvArray(value1, value2, value3), new KTypeStandardHash<KType>());

        assertSameMap(map, map2);
    }

    /* */
    @Test
    public void testPut()
    {
        map.put(key1, value1);

        Assert.assertTrue(map.containsKey(key1));
        TestUtils.assertEquals2(value1, map.lget());
        TestUtils.assertEquals2(value1, map.get(key1));
    }

    /* */
    @Test
    public void testLPut()
    {
        map.put(key1, value2);
        if (map.containsKey(key1))
            map.lset(value3);

        Assert.assertTrue(map.containsKey(key1));
        TestUtils.assertEquals2(value3, map.lget());
        TestUtils.assertEquals2(value3, map.get(key1));
    }

    /* */
    @Test
    public void testPutOverExistingKey()
    {
        map.put(key1, value1);
        TestUtils.assertEquals2(value1, map.put(key1, value3));
        TestUtils.assertEquals2(value3, map.get(key1));
    }

    /* */
    @Test
    public void testPutWithExpansions()
    {
        final int COUNT = 10000;
        final Random rnd = new Random();
        final HashSet<Object> values = new HashSet<Object>();

        for (int i = 0; i < COUNT; i++)
        {
            final int v = rnd.nextInt();
            final boolean hadKey = values.contains(cast(v));
            values.add(cast(v));

            Assert.assertEquals(hadKey, map.containsKey(cast(v)));
            map.put(cast(v), vcast(v));
            Assert.assertEquals(values.size(), map.size());
        }
        Assert.assertEquals(values.size(), map.size());
    }

    /* */
    @Test
    public void testPutAll()
    {
        map.put(key1, value1);
        map.put(key2, value1);

        final KTypeVTypeOpenCustomHashMap<KType, VType> map2 =
                new KTypeVTypeOpenCustomHashMap<KType, VType>(new KTypeStandardHash<KType>());

        map2.put(key2, value2);
        map2.put(key3, value1);

        // One new key (key3).
        Assert.assertEquals(1, map.putAll(map2));

        // Assert the value under key2 has been replaced.
        TestUtils.assertEquals2(value2, map.get(key2));

        // And key3 has been added.
        TestUtils.assertEquals2(value1, map.get(key3));
        Assert.assertEquals(3, map.size());
    }

    /* */
    @Test
    public void testPutIfAbsent()
    {
        Assert.assertTrue(map.putIfAbsent(key1, value1));
        Assert.assertFalse(map.putIfAbsent(key1, value2));
        TestUtils.assertEquals2(value1, map.get(key1));
    }

    /*! #if ($TemplateOptions.VTypeNumeric)
    @Test
    public void testPutOrAdd()
    {
        assertEquals2(value1, map.putOrAdd(key1, value1, value2));
        assertEquals2(value1 + value2, map.putOrAdd(key1, value1, value2));
    }
    #end !*/

    /*! #if ($TemplateOptions.VTypeNumeric)
    @Test
    public void testAddTo()
    {
        assertEquals2(value1, map.addTo(key1, value1));
        assertEquals2(value1 + value2, map.addTo(key1, value2));
    }
    #end !*/

    /* */
    @Test
    public void testRemove()
    {
        map.put(key1, value1);
        TestUtils.assertEquals2(value1, map.remove(key1));
        TestUtils.assertEquals2(Intrinsics.defaultVTypeValue(), map.remove(key1));
        Assert.assertEquals(0, map.size());

        // These are internals, but perhaps worth asserting too.
        Assert.assertEquals(0, map.assigned);
    }

    /* */
    @Test
    public void testRemoveAllWithContainer()
    {
        map.put(key1, value1);
        map.put(key2, value1);
        map.put(key3, value1);

        final KTypeArrayList<KType> list2 = KTypeArrayList.newInstance();
        list2.add(newArray(key2, key3, key4));

        map.removeAll(list2);
        Assert.assertEquals(1, map.size());
        Assert.assertTrue(map.containsKey(key1));
    }

    /* */
    @Test
    public void testRemoveAllWithPredicate()
    {
        map.put(key1, value1);
        map.put(key2, value1);
        map.put(key3, value1);

        map.removeAll(new KTypePredicate<KType>()
                {
            @Override
            public boolean apply(final KType value)
            {
                return value == key2 || value == key3;
            }
                });
        Assert.assertEquals(1, map.size());
        Assert.assertTrue(map.containsKey(key1));
    }

    /* */
    @Test
    public void testRemoveAllWithPredicateInterrupted()
    {
        map.put(key0, value1);
        map.put(key1, value1);
        map.put(key2, value1);
        map.put(key3, value1);
        map.put(key4, value1);
        map.put(key5, value1);
        map.put(key6, value1);
        map.put(key7, value1);
        map.put(key8, value1);

        final RuntimeException t = new RuntimeException();
        try
        {
            //the assert below should never be triggered because of the exception
            //so give it an invalid value in case the thing terminates  = initial size + 1
            Assert.assertEquals(10, map.removeAll(new KTypePredicate<KType>()
                    {
                @Override
                public boolean apply(final KType key)
                {
                    if (key == key7)
                        throw t;
                    return key == key2 || key == key9 || key == key5;
                };
                    }));

            Assert.fail();
        }
        catch (final RuntimeException e)
        {
            // Make sure it's really our exception...
            if (e != t)
                throw e;
        }

        // And check if the set is in consistent state. We cannot predict the pattern,
        //but we know that since key7 throws an exception, key7 is still present in the set.

        Assert.assertTrue(map.containsKey(key7));
        checkConsistency();
    }

    /* */
    @Test
    public void testRemoveAllViaKeySetView()
    {
        map.put(key1, value1);
        map.put(key2, value1);
        map.put(key3, value1);

        map.keys().removeAll(new KTypePredicate<KType>()
                {
            @Override
            public boolean apply(final KType value)
            {
                return value == key2 || value == key3;
            }
                });
        Assert.assertEquals(1, map.size());
        Assert.assertTrue(map.containsKey(key1));
    }

    /* */
    @Test
    public void testRemoveAllViaValueSetView()
    {
        map.put(key1, value1); //del
        map.put(key2, value2); //del
        map.put(key3, value1); //del
        map.put(key4, value3);
        map.put(key5, value5);
        map.put(key6, value5);
        map.put(key7, value1); //del
        map.put(key8, value8);
        map.put(key9, value2); //del

        final int nbRemoved = map.values().removeAll(new KTypePredicate<VType>()
                {
            @Override
            public boolean apply(final VType value)
            {
                return value == value1 || value == value2;
            }
                });

        Assert.assertEquals(5, nbRemoved);
        Assert.assertEquals(4, map.size());
        Assert.assertTrue(map.containsKey(key4));
        Assert.assertTrue(map.containsKey(key5));
        Assert.assertTrue(map.containsKey(key6));
        Assert.assertTrue(map.containsKey(key8));
    }

    /* */
    @Test
    public void testRemoveAllOccurencesViaValueSetView()
    {
        map.put(key1, value1); //del
        map.put(key2, value2);
        map.put(key3, value1); //del
        map.put(key4, value3);
        map.put(key5, value2);
        map.put(key6, value5);
        map.put(key7, value1); //del
        map.put(key8, value8);
        map.put(key9, value2);

        final int nbRemoved = map.values().removeAllOccurrences(value1);

        Assert.assertEquals(3, nbRemoved);
        Assert.assertEquals(6, map.size());
        Assert.assertTrue(map.containsKey(key2));
        Assert.assertTrue(map.containsKey(key4));
        Assert.assertTrue(map.containsKey(key5));
        Assert.assertTrue(map.containsKey(key6));
        Assert.assertTrue(map.containsKey(key8));
        Assert.assertTrue(map.containsKey(key9));
    }

    /* */
    @Test
    public void testMapsIntersection()
    {
        final KTypeVTypeOpenCustomHashMap<KType, VType> map2 =
                KTypeVTypeOpenCustomHashMap.newInstance(new KTypeStandardHash<KType>());

        map.put(key1, value1);
        map.put(key2, value1);
        map.put(key3, value1);

        map2.put(key2, value1);
        map2.put(key4, value1);

        Assert.assertEquals(2, map.keys().retainAll(map2.keys()));

        Assert.assertEquals(1, map.size());
        Assert.assertTrue(map.containsKey(key2));
    }

    /* */
    @Test
    public void testMapKeySet()
    {
        map.put(key1, value3);
        map.put(key2, value2);
        map.put(key3, value1);

        TestUtils.assertSortedListEquals(map.keys().toArray(), key1, key2, key3);
    }

    /* */
    @Test
    public void testMapKeySetIterator()
    {
        map.put(key1, value3);
        map.put(key2, value2);
        map.put(key3, value1);

        int counted = 0;
        for (final KTypeCursor<KType> c : map.keys())
        {
            TestUtils.assertEquals2(map.keys[c.index], c.value);
            counted++;
        }
        Assert.assertEquals(counted, map.size());
    }

    /* */
    @Test
    public void testClear()
    {
        map.put(key1, value1);
        map.put(key2, value1);
        map.clear();
        Assert.assertEquals(0, map.size());

        // These are internals, but perhaps worth asserting too.
        Assert.assertEquals(0, map.assigned);

        // Check if the map behaves properly upon subsequent use.
        testPutWithExpansions();
    }

    /* */
    @Test
    public void testRoundCapacity()
    {
        Assert.assertEquals(0x40000000, HashContainerUtils.roundCapacity(Integer.MAX_VALUE));
        Assert.assertEquals(0x40000000, HashContainerUtils.roundCapacity(Integer.MAX_VALUE / 2 + 1));
        Assert.assertEquals(0x40000000, HashContainerUtils.roundCapacity(Integer.MAX_VALUE / 2));
        Assert.assertEquals(KTypeVTypeOpenCustomHashMap.MIN_CAPACITY, HashContainerUtils.roundCapacity(0));
        Assert.assertEquals(Math.max(4, KTypeVTypeOpenCustomHashMap.MIN_CAPACITY), HashContainerUtils.roundCapacity(3));
    }

    /* */
    @Test
    public void testIterable()
    {
        map.put(key1, value1);
        map.put(key2, value2);
        map.put(key3, value3);
        map.remove(key2);

        int count = 0;
        for (final KTypeVTypeCursor<KType, VType> cursor : map)
        {
            count++;
            Assert.assertTrue(map.containsKey(cursor.key));
            TestUtils.assertEquals2(cursor.value, map.get(cursor.key));

            TestUtils.assertEquals2(cursor.value, map.values[cursor.index]);
            TestUtils.assertEquals2(cursor.key, map.keys[cursor.index]);

            Assert.assertTrue(map.allocated[cursor.index] /*! #if ($RH) !*/!= -1 /*! #end !*/);

        }
        Assert.assertEquals(count, map.size());

        map.clear();
        Assert.assertFalse(map.iterator().hasNext());
    }

    /* */
    @Test
    public void testFullLoadFactor()
    {
        map = new KTypeVTypeOpenCustomHashMap<KType, VType>(1, 1f, new KTypeStandardHash<KType>());

        // Fit in the byte key range.
        final int capacity = 0x80;
        final int max = capacity - 2;
        for (int i = 0; i < max; i++)
        {
            map.put(cast(i), value1);
        }

        // Still not expanded.
        Assert.assertEquals(max, map.size());
        Assert.assertEquals(capacity, map.keys.length);
        // Won't expand (existing key).
        map.put(cast(0), value2);
        Assert.assertEquals(capacity, map.keys.length);
        // Expanded.
        map.put(cast(0xff), value2);
        Assert.assertEquals(2 * capacity, map.keys.length);
    }

    /* */
    @Test
    public void testBug_HPPC73_FullCapacityGet()
    {
        map = new KTypeVTypeOpenCustomHashMap<KType, VType>(1, 1f, new KTypeStandardHash<KType>());
        final int capacity = 0x80;
        final int max = capacity - 2;
        for (int i = 0; i < max; i++)
        {
            map.put(cast(i), value1);
        }
        Assert.assertEquals(max, map.size());
        Assert.assertEquals(capacity, map.keys.length);

        // Non-existent key.
        map.remove(cast(max + 1));
        Assert.assertFalse(map.containsKey(cast(max + 1)));
        TestUtils.assertEquals2(Intrinsics.defaultVTypeValue(), map.get(cast(max + 1)));

        // Should not expand because we're replacing an existing element.
        map.put(cast(0), value2);
        Assert.assertEquals(max, map.size());
        Assert.assertEquals(capacity, map.keys.length);

        map.putIfAbsent(cast(0), value3);
        Assert.assertEquals(max, map.size());
        Assert.assertEquals(capacity, map.keys.length);

        // Remove from a full map.
        map.remove(cast(0));
        Assert.assertEquals(max - 1, map.size());
        Assert.assertEquals(capacity, map.keys.length);
    }

    /* */
    @Test
    public void testHalfLoadFactor()
    {
        map = new KTypeVTypeOpenCustomHashMap<KType, VType>(1, 0.5f, new KTypeStandardHash<KType>());

        final int capacity = 0x80;
        final int max = capacity - 2;
        for (int i = 0; i < max; i++)
        {
            map.put(cast(i), value1);
        }

        Assert.assertEquals(max, map.size());
        // Still not expanded.
        Assert.assertEquals(2 * capacity, map.keys.length);
        // Won't expand (existing key);
        map.put(cast(0), value2);
        Assert.assertEquals(2 * capacity, map.keys.length);
        // Expanded.
        map.put(cast(0xff), value1);
        Assert.assertEquals(4 * capacity, map.keys.length);
    }

    /*! #if ($TemplateOptions.VTypeGeneric) !*/
    @SuppressWarnings("unchecked")
    /*! #end !*/
    @Test
    public void testHashCodeEquals()
    {
        final KTypeVTypeOpenCustomHashMap<KType, VType> l0 =
                new KTypeVTypeOpenCustomHashMap<KType, VType>(new KTypeStandardHash<KType>());
        Assert.assertEquals(0, l0.hashCode());
        Assert.assertEquals(l0, new KTypeVTypeOpenCustomHashMap<KType, VType>(new KTypeStandardHash<KType>()));

        final KTypeVTypeOpenCustomHashMap<KType, VType> l1 = KTypeVTypeOpenCustomHashMap.from(
                newArray(key1, key2, key3),
                newvArray(value1, value2, value3), new KTypeStandardHash<KType>());

        final KTypeVTypeOpenCustomHashMap<KType, VType> l2 = KTypeVTypeOpenCustomHashMap.from(
                newArray(key2, key1, key3),
                newvArray(value2, value1, value3), new KTypeStandardHash<KType>());

        final KTypeVTypeOpenCustomHashMap<KType, VType> l3 = KTypeVTypeOpenCustomHashMap.from(
                newArray(key1, key2),
                newvArray(value2, value1), new KTypeStandardHash<KType>());

        Assert.assertEquals(l1.hashCode(), l2.hashCode());
        Assert.assertEquals(l1, l2);

        Assert.assertFalse(l1.equals(l3));
        Assert.assertFalse(l2.equals(l3));
    }

    /*! #if ($TemplateOptions.VTypeGeneric) !*/
    @SuppressWarnings("unchecked")
    /*! #end !*/
    @Test
    public void testBug_HPPC37()
    {
        final KTypeVTypeOpenCustomHashMap<KType, VType> l1 = KTypeVTypeOpenCustomHashMap.from(
                newArray(key1),
                newvArray(value1), new KTypeStandardHash<KType>());

        final KTypeVTypeOpenCustomHashMap<KType, VType> l2 = KTypeVTypeOpenCustomHashMap.from(
                newArray(key2),
                newvArray(value1), new KTypeStandardHash<KType>());

        Assert.assertFalse(l1.equals(l2));
        Assert.assertFalse(l2.equals(l1));
    }

    /*! #if ($TemplateOptions.KTypeGeneric) !*/
    @Test
    public void testNullKey()
    {
        map.put(null, vcast(10));
        TestUtils.assertEquals2(vcast(10), map.get(null));
        Assert.assertTrue(map.containsKey(null));
        TestUtils.assertEquals2(vcast(10), map.lget());
        TestUtils.assertEquals2(null, map.lkey());
        map.remove(null);
        Assert.assertEquals(0, map.size());
    }

    /*! #end !*/

    @Test
    public void testLkey()
    {
        map.put(key1, vcast(10));
        map.put(key8, vcast(5));
        map.put(key5, vcast(5));
        map.put(key7, vcast(6));
        map.put(key2, vcast(5));
        map.put(key4, vcast(32));
        map.put(key9, vcast(25));

        Assert.assertTrue(map.containsKey(key1));

        /*! #if ($TemplateOptions.KTypeGeneric) !*/
        Assert.assertSame(key1, map.lkey());
        /*! #end !*/

        KType key1_ = cast(1);

        /*! #if ($TemplateOptions.KTypeGeneric) !*/
        key1_ = (KType) new Integer(1);
        Assert.assertNotSame(key1, key1_);
        /*! #end !*/

        Assert.assertEquals(castType(key1), castType(key1_));

        Assert.assertTrue(map.containsKey(key1_));

        /*! #if ($TemplateOptions.KTypeGeneric) !*/
        Assert.assertSame(key1, map.lkey());
        /*! #end !*/

        Assert.assertEquals(castType(key1_), castType(map.lkey()));
    }

    /*! #if ($TemplateOptions.VTypeGeneric) !*/
    @Test
    public void testNullValue()
    {
        map.put(key1, null);
        Assert.assertEquals(null, map.get(key1));
        Assert.assertTrue(map.containsKey(key1));
        map.remove(key1);
        Assert.assertFalse(map.containsKey(key1));
        Assert.assertEquals(0, map.size());
    }

    /*! #end !*/

    /*! #if ($TemplateOptions.AllGeneric) !*/
    /**
     * Run some random insertions/ deletions and compare the results
     * against <code>java.util.HashMap</code>.
     */
    @Test
    public void testAgainstHashMap()
    {
        final Random rnd = new Random();
        final java.util.HashMap<KType, VType> other =
                new java.util.HashMap<KType, VType>();

        for (int size = 1000; size < 20000; size += 4000)
        {
            other.clear();
            map.clear();

            for (int round = 0; round < size * 20; round++)
            {
                final KType key = cast(rnd.nextInt(size));
                final VType value = vcast(rnd.nextInt());

                if (rnd.nextBoolean())
                {
                    map.put(key, value);
                    other.put(key, value);

                    Assert.assertEquals(vcastType(value), vcastType(map.get(key)));
                    Assert.assertTrue(map.containsKey(key));
                    Assert.assertEquals(vcastType(value), vcastType(map.lget()));
                }
                else
                {
                    Assert.assertEquals("size= " + size + ", round = " + round,
                            vcastType(other.remove(key)), vcastType(map.remove(key)));
                }

                Assert.assertEquals(other.size(), map.size());
            }
        }
    }

    /*! #end !*/

    /*
     * 
     */
    @Test
    public void testClone()
    {
        this.map.put(key1, value1);
        this.map.put(key2, value2);
        this.map.put(key3, value3);

        final KTypeVTypeOpenCustomHashMap<KType, VType> cloned = map.clone();
        cloned.remove(key1);

        TestUtils.assertSortedListEquals(map.keys().toArray(), key1, key2, key3);
        TestUtils.assertSortedListEquals(cloned.keys().toArray(), key2, key3);
    }

    /*
     * 
     */
    @Test
    public void testToString()
    {
        Assume.assumeTrue(
                (int[].class.isInstance(map.keys) ||
                        short[].class.isInstance(map.keys) ||
                        byte[].class.isInstance(map.keys) ||
                        long[].class.isInstance(map.keys) ||
                        Object[].class.isInstance(map.keys)) &&
                        (int[].class.isInstance(map.values) ||
                                byte[].class.isInstance(map.values) ||
                                short[].class.isInstance(map.values) ||
                                long[].class.isInstance(map.values) ||
                                Object[].class.isInstance(map.values)));

        this.map.put(key1, value1);
        this.map.put(key2, value2);

        String asString = map.toString();
        asString = asString.replaceAll("[^0-9]", "");
        final char[] asCharArray = asString.toCharArray();
        Arrays.sort(asCharArray);
        Assert.assertEquals("1122", new String(asCharArray));
    }

    @Test
    public void testAddRemoveSameHashCollision()
    {
        // This test is only applicable to selected key types.
        Assume.assumeTrue(
                int[].class.isInstance(map.keys) ||
                long[].class.isInstance(map.keys) ||
                Object[].class.isInstance(map.keys));

        final IntArrayList hashChain = TestUtils.generateMurmurHash3CollisionChain(0x1fff, 0x7e, 0x1fff / 3);

        /*
         * Add all of the conflicting keys to a map.
         */
        for (final IntCursor c : hashChain)
            map.put(cast(c.value), this.value1);

        Assert.assertEquals(hashChain.size(), map.size());

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
            map.put(cast(c.value), value2);

        Assert.assertEquals(hashChain.size() + differentKeys.size(), map.size());

        /*
         * Verify the map contains all of the conflicting keys.
         */
        for (final IntCursor c : hashChain)
            Assert.assertEquals(vcastType(value1), vcastType(map.get(cast(c.value))));

        /*
         * Verify the map contains all the other keys.
         */
        for (final IntCursor c : differentKeys)
            TestUtils.assertEquals2(value2, map.get(cast(c.value)));

        /*
         * Iteratively remove the keys, from first to last.
         */
        for (final IntCursor c : hashChain)
            TestUtils.assertEquals2(value1, map.remove(cast(c.value)));

        Assert.assertEquals(differentKeys.size(), map.size());

        /*
         * Verify the map contains all the other keys.
         */
        for (final IntCursor c : differentKeys)
            TestUtils.assertEquals2(value2, map.get(cast(c.value)));
    }

    /* */
    @Test
    public void testMapValues()
    {
        map.put(key1, value3);
        map.put(key2, value2);
        map.put(key3, value1);
        TestUtils.assertSortedListEquals(map.values().toArray(), value1, value2, value3);

        map.clear();
        map.put(key1, value1);
        map.put(key2, value2);
        map.put(key3, value2);
        TestUtils.assertSortedListEquals(map.values().toArray(), value1, value2, value2);
    }

    /* */
    @Test
    public void testMapValuesIterator()
    {
        map.put(key1, value3);
        map.put(key2, value2);
        map.put(key3, value1);

        int counted = 0;
        for (final KTypeCursor<VType> c : map.values())
        {
            TestUtils.assertEquals2(map.values[c.index], c.value);
            counted++;
        }
        Assert.assertEquals(counted, map.size());
    }

    /* */
    @Test
    public void testMapValuesContainer()
    {
        map.put(key1, value1);
        map.put(key2, value2);
        map.put(key3, value2);

        // contains()
        for (final KTypeVTypeCursor<KType, VType> c : map)
            Assert.assertTrue(map.values().contains(c.value));
        Assert.assertFalse(map.values().contains(value3));

        Assert.assertEquals(map.isEmpty(), map.values().isEmpty());
        Assert.assertEquals(map.size(), map.values().size());

        final KTypeArrayList<VType> values = new KTypeArrayList<VType>();
        map.values().forEach(new KTypeProcedure<VType>()
                {
            @Override
            public void apply(final VType value)
            {
                values.add(value);
            }
                });
        TestUtils.assertSortedListEquals(map.values().toArray(), value1, value2, value2);

        values.clear();
        map.values().forEach(new KTypePredicate<VType>()
                {
            @Override
            public boolean apply(final VType value)
            {
                values.add(value);
                return true;
            }
                });
        TestUtils.assertSortedListEquals(map.values().toArray(), value1, value2, value2);
    }

    @Test
    public void testHashingStrategyCloneEquals() {

        //a) Check that 2 different sets filled the same way with same values and strategies = null
        //are indeed equal.
        final long TEST_SEED = 4987013210686416456L;
        final int TEST_SIZE = (int) 500e3;
        final KTypeVTypeOpenCustomHashMap<KType, VType> refMap = createMapWithRandomData(TEST_SIZE, new KTypeStandardHash<KType>(), TEST_SEED);
        KTypeVTypeOpenCustomHashMap<KType, VType> refMap2 = createMapWithRandomData(TEST_SIZE, new KTypeStandardHash<KType>(), TEST_SEED);

        Assert.assertEquals(refMap, refMap2);

        //b) Clone the above. All maps are now identical.
        KTypeVTypeOpenCustomHashMap<KType, VType> refMapclone = refMap.clone();
        KTypeVTypeOpenCustomHashMap<KType, VType> refMap2clone = refMap2.clone();

        //all strategies are equivalent
        Assert.assertEquals(refMap.strategy(), refMap2.strategy());
        Assert.assertEquals(refMap2.strategy(), refMapclone.strategy());
        Assert.assertEquals(refMapclone.strategy(), refMap2clone.strategy());
        Assert.assertEquals(refMap2clone.strategy(), new KTypeStandardHash<KType>());

        Assert.assertEquals(refMap, refMapclone);
        Assert.assertEquals(refMapclone, refMap2);
        Assert.assertEquals(refMap2, refMap2clone);
        Assert.assertEquals(refMap2clone, refMap);

        //cleanup
        refMapclone = null;
        refMap2 = null;
        refMap2clone = null;
        System.gc();

        //c) Create a set nb 3 with same integer content, but with a strategy mapping on equals.
        final KTypeVTypeOpenCustomHashMap<KType, VType> refMap3 = createMapWithRandomData(TEST_SIZE,
                new KTypeHashingStrategy<KType>() {

            @Override
            public int computeHashCode(final KType object) {

                return Internals.rehash(object);
            }

            @Override
            public boolean equals(final KType o1, final KType o2) {

                return Intrinsics.equalsKType(o1, o2);
            }
        }, TEST_SEED);

        //because they do the same thing as above, but with semantically different strategies, because ref3 strategy object is != ref strategy object
        Assert.assertFalse(refMap.equals(refMap3));

        //However, if we cloned refMap3
        final KTypeVTypeOpenCustomHashMap<KType, VType> refMap3clone = refMap3.clone();
        Assert.assertEquals(refMap3, refMap3clone);

        //strategies are copied by reference only
        Assert.assertTrue(refMap3.strategy() == refMap3clone.strategy());

        //d) Create identical set with same different strategy instances, but which consider themselves equals()
        KTypeVTypeOpenCustomHashMap<KType, VType> refMap4 = createMapWithRandomData(TEST_SIZE,
                new KTypeHashingStrategy<KType>() {

            @Override
            public boolean equals(final Object obj) {

                return true;
            }

            @Override
            public int computeHashCode(final KType object) {

                return Internals.rehash(object);
            }

            @Override
            public boolean equals(final KType o1, final KType o2) {

                return Intrinsics.equalsKType(o1, o2);
            }
        }, TEST_SEED);

        KTypeVTypeOpenCustomHashMap<KType, VType> refMap4Image = createMapWithRandomData(TEST_SIZE,
                new KTypeHashingStrategy<KType>() {

            @Override
            public boolean equals(final Object obj) {

                return true;
            }

            @Override
            public int computeHashCode(final KType object) {

                return Internals.rehash(object);
            }

            @Override
            public boolean equals(final KType o1, final KType o2) {

                return Intrinsics.equalsKType(o1, o2);
            }
        }, TEST_SEED);

        Assert.assertEquals(refMap4, refMap4Image);
        //but strategy instances are indeed 2 different objects
        Assert.assertFalse(refMap4.strategy() == refMap4Image.strategy());

        //cleanup
        refMap4 = null;
        refMap4Image = null;
        System.gc();

        //e) Do contrary to 4), hashStrategies always != from each other by equals.
        final KTypeHashingStrategy<KType> alwaysDifferentStrategy = new KTypeHashingStrategy<KType>() {

            @Override
            public boolean equals(final Object obj) {

                //never equal !!!
                return false;
            }

            @Override
            public int computeHashCode(final KType object) {

                return Internals.rehash(object);
            }

            @Override
            public boolean equals(final KType o1, final KType o2) {

                return Intrinsics.equalsKType(o1, o2);
            }
        };

        final KTypeVTypeOpenCustomHashMap<KType, VType> refMap5 = createMapWithRandomData(TEST_SIZE, alwaysDifferentStrategy, TEST_SEED);
        final KTypeVTypeOpenCustomHashMap<KType, VType> refMap5alwaysDifferent = createMapWithRandomData(TEST_SIZE, alwaysDifferentStrategy, TEST_SEED);

        //both sets are NOT equal because their strategies said they are different
        Assert.assertFalse(refMap5.equals(refMap5alwaysDifferent));
    }

    @Test
    public void testHashingStrategyAddContainsGetRemove() {

        final long TEST_SEED = 15249155965216185L;
        final int TEST_SIZE = (int) 500e3;

        //those following 3  maps behave indeed the same in the test context:
        final KTypeVTypeOpenCustomHashMap<KType, VType> refMap = KTypeVTypeOpenCustomHashMap.newInstance(new KTypeStandardHash<KType>());

        final KTypeVTypeOpenCustomHashMap<KType, VType> refMapIdenticalStrategy = KTypeVTypeOpenCustomHashMap.newInstance(
                KTypeVTypeOpenCustomHashMap.DEFAULT_CAPACITY,
                KTypeVTypeOpenCustomHashMap.DEFAULT_LOAD_FACTOR,
                new KTypeHashingStrategy<KType>() {

                    @Override
                    public boolean equals(final Object obj) {

                        //always
                        return true;
                    }

                    @Override
                    public int computeHashCode(final KType object) {

                        return Internals.rehash(object);
                    }

                    @Override
                    public boolean equals(final KType o1, final KType o2) {

                        return Intrinsics.equalsKType(o1, o2);
                    }
                });

        //compute the iterations doing multiple operations
        final Random prng = new Random(TEST_SEED);

        for (int i = 0; i < TEST_SIZE; i++) {

            //a) generate a value to put
            int putKey = prng.nextInt();
            final int putValue = prng.nextInt();

            refMap.put(cast(putKey), vcast(putValue));

            refMapIdenticalStrategy.put(cast(putKey), vcast(putValue));

            Assert.assertEquals(refMap.containsKey(cast(putKey)), refMapIdenticalStrategy.containsKey(cast(putKey)));

            /*! #if ($TemplateOptions.VTypeGeneric) !*/
            Assert.assertEquals(refMap.get(cast(putKey)), refMapIdenticalStrategy.get(cast(putKey)));
            /*! #else
            assertTrue(refMap.get(cast(putKey)) ==  refMapIdenticalStrategy.get(cast(putKey)));
             #end !*/

            final boolean isToBeRemoved = (prng.nextInt() % 3 == 0);
            putKey = prng.nextInt();

            if (isToBeRemoved) {

                refMap.remove(cast(putKey));

                refMapIdenticalStrategy.remove(cast(putKey));

                Assert.assertFalse(refMap.containsKey(cast(putKey)));

                Assert.assertFalse(refMapIdenticalStrategy.containsKey(cast(putKey)));
            }

            Assert.assertEquals(refMap.containsKey(cast(putKey)), refMapIdenticalStrategy.containsKey(cast(putKey)));

            /*! #if ($TemplateOptions.VTypeGeneric) !*/
            Assert.assertEquals(refMap.get(cast(putKey)), refMapIdenticalStrategy.get(cast(putKey)));
            /*! #else
            assertTrue(refMap.get(cast(putKey)) ==  refMapIdenticalStrategy.get(cast(putKey)));
             #end !*/

            //test size
            Assert.assertEquals(refMap.size(), refMapIdenticalStrategy.size());
        }
    }

    private KTypeVTypeOpenCustomHashMap<KType, VType> createMapWithRandomData(final int size, final KTypeHashingStrategy<? super KType> strategy, final long randomSeed)
    {

        final Random prng = new Random(randomSeed);

        final KTypeVTypeOpenCustomHashMap<KType, VType> newMap = KTypeVTypeOpenCustomHashMap.newInstance(KTypeVTypeOpenCustomHashMap.DEFAULT_CAPACITY,
                KTypeVTypeOpenCustomHashMap.DEFAULT_LOAD_FACTOR, strategy);

        for (int i = 0; i < size; i++) {

            newMap.put(cast(prng.nextInt()), vcast(prng.nextInt()));
        }

        return newMap;
    }

    @Test
    public void testPooledIteratorForEach()
    {
        //A) Unbroken for-each loop
        //must accommodate even the smallest primitive type
        //so that the iteration do not break before it should...
        final int TEST_SIZE = 126;
        final long TEST_ROUNDS = 5000;

        final KTypeVTypeOpenCustomHashMap<KType, VType> testContainer = createMapWithOrderedData(TEST_SIZE);
        final KTypeVTypeOpenCustomHashMap<KType, VType>.KeysContainer keyset = testContainer.keys();
        final KTypeVTypeOpenCustomHashMap<KType, VType>.ValuesContainer valueset = testContainer.values();

        final long checksum = testContainer.forEach(new KTypeVTypeProcedure<KType, VType>() {

            long count;

            @Override
            public void apply(final KType key, final VType value)
            {

                count += vcastType(value);
            }
        }).count;

        long testValue = 0;
        long initialPoolSize = testContainer.entryIteratorPool.size();

        for (int round = 0; round < TEST_ROUNDS; round++)
        {
            //for-each in test :

            //A) Loop on entries
            testValue = 0;
            for (final KTypeVTypeCursor<KType, VType> cursor : testContainer)
            {
                //we consume 1 iterator for this loop
                Assert.assertEquals(initialPoolSize - 1, testContainer.entryIteratorPool.size());

                testValue += vcastType(cursor.value);
            }

            //check checksum the iteration
            Assert.assertEquals(checksum, testValue);

            //iterator is returned to its pool
            Assert.assertEquals(initialPoolSize, testContainer.entryIteratorPool.size());

            //B) Loop on keys == same value as values
            testValue = 0;
            initialPoolSize = keyset.keyIteratorPool.size();

            for (final KTypeCursor<KType> cursor : keyset)
            {
                //we consume 1 iterator for this loop
                Assert.assertEquals(initialPoolSize - 1, keyset.keyIteratorPool.size());

                testValue += castType(cursor.value);
            }

            //check checksum the iteration
            Assert.assertEquals(checksum, testValue);

            //iterator is returned to its pool
            Assert.assertEquals(initialPoolSize, keyset.keyIteratorPool.size());

            //C) Loop on values
            testValue = 0;
            initialPoolSize = valueset.valuesIteratorPool.size();

            for (final KTypeCursor<VType> cursor : valueset)
            {
                //we consume 1 iterator for this loop
                Assert.assertEquals(initialPoolSize - 1, valueset.valuesIteratorPool.size());

                testValue += vcastType(cursor.value);
            }

            //check checksum the iteration
            Assert.assertEquals(checksum, testValue);

            //iterator is returned to its pool
            Assert.assertEquals(initialPoolSize, valueset.valuesIteratorPool.size());

        } //end for rounds
    }

    @Test
    public void testPooledIteratorBrokenForEach()
    {
        //A) for-each loop interrupted

        //must accommodate even the smallest primitive type
        //so that the iteration do not break before it should...
        final int TEST_SIZE = 126;
        final long TEST_ROUNDS = 5000;

        final KTypeVTypeOpenCustomHashMap<KType, VType> testContainer = createMapWithOrderedData(TEST_SIZE);
        final KTypeVTypeOpenCustomHashMap<KType, VType>.KeysContainer keyset = testContainer.keys();
        final KTypeVTypeOpenCustomHashMap<KType, VType>.ValuesContainer valueset = testContainer.values();

        int count = 0;
        for (int round = 0; round < TEST_ROUNDS; round++)
        {
            //A) for-each in test :
            long initialPoolSize = testContainer.entryIteratorPool.size();
            count = 0;
            for (final KTypeVTypeCursor<KType, VType> cursor : testContainer)
            {
                guard += vcastType(cursor.value);
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

            //B) Loop on keys
            initialPoolSize = keyset.keyIteratorPool.size();
            count = 0;
            for (final KTypeCursor<KType> cursor : keyset)
            {
                guard += castType(cursor.value);
                //we consume 1 iterator for this loop, but reallocs can happen,
                //so we can only say its != initialPoolSize
                Assert.assertTrue(initialPoolSize != keyset.keyIteratorPool.size());

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
            Assert.assertTrue(initialPoolSize != keyset.keyIteratorPool.size());

            //C) Loop on values
            initialPoolSize = valueset.valuesIteratorPool.size();
            count = 0;
            for (final KTypeCursor<VType> cursor : valueset)
            {
                guard += vcastType(cursor.value);
                //we consume 1 iterator for this loop, but reallocs can happen,
                //so we can only say its != initialPoolSize
                Assert.assertTrue(initialPoolSize != valueset.valuesIteratorPool.size());

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
            Assert.assertTrue(initialPoolSize != valueset.valuesIteratorPool.size());
        } //end for rounds

        //Due to policy of the Iterator pool, the intended pool never get bigger that some limit
        //despite the Iterator leak.
        Assert.assertTrue(testContainer.entryIteratorPool.capacity() < IteratorPool.getMaxPoolSize() + 1);
        Assert.assertTrue(keyset.keyIteratorPool.capacity() < IteratorPool.getMaxPoolSize() + 1);
        Assert.assertTrue(valueset.valuesIteratorPool.capacity() < IteratorPool.getMaxPoolSize() + 1);
    }

    @Test
    public void testPooledIteratorFullIteratorLoop()
    {
        // for-each loop interrupted

        //must accommodate even the smallest primitive type
        //so that the iteration do not break before it should...
        final int TEST_SIZE = 126;
        final long TEST_ROUNDS = 5000;

        final KTypeVTypeOpenCustomHashMap<KType, VType> testContainer = createMapWithOrderedData(TEST_SIZE);
        final KTypeVTypeOpenCustomHashMap<KType, VType>.KeysContainer keyset = testContainer.keys();
        final KTypeVTypeOpenCustomHashMap<KType, VType>.ValuesContainer valueset = testContainer.values();

        final long checksum = testContainer.forEach(new KTypeVTypeProcedure<KType, VType>() {

            long count;

            @Override
            public void apply(final KType key, final VType value)
            {
                count += vcastType(value);
            }
        }).count;

        long testValue = 0;
        final int startingPoolSize = testContainer.entryIteratorPool.size();

        for (int round = 0; round < TEST_ROUNDS; round++)
        {
            //A) Classical iterator loop, with manually allocated Iterator
            int initialPoolSize = testContainer.entryIteratorPool.size();

            final KTypeVTypeOpenCustomHashMap<KType, VType>.EntryIterator loopIterator = testContainer.iterator();

            Assert.assertEquals(initialPoolSize - 1, testContainer.entryIteratorPool.size());

            testValue = 0;
            while (loopIterator.hasNext())
            {
                testValue += vcastType(loopIterator.next().value);
            } //end IteratorLoop

            //iterator is returned automatically to its pool, by normal iteration termination
            Assert.assertEquals(initialPoolSize, testContainer.entryIteratorPool.size());

            //checksum
            Assert.assertEquals(checksum, testValue);

            //B) Loop on keys
            initialPoolSize = keyset.keyIteratorPool.size();

            final KTypeVTypeOpenCustomHashMap<KType, VType>.KeysIterator keyLoopIterator = keyset.iterator();

            Assert.assertEquals(initialPoolSize - 1, keyset.keyIteratorPool.size());

            testValue = 0;
            while (keyLoopIterator.hasNext())
            {
                testValue += castType(keyLoopIterator.next().value);
            } //end IteratorLoop

            //iterator is returned automatically to its pool, by normal iteration termination
            Assert.assertEquals(initialPoolSize, keyset.keyIteratorPool.size());

            //checksum
            Assert.assertEquals(checksum, testValue);

            //C) Loop on values
            initialPoolSize = valueset.valuesIteratorPool.size();

            final KTypeVTypeOpenCustomHashMap<KType, VType>.ValuesIterator valueLoopIterator = valueset.iterator();

            Assert.assertEquals(initialPoolSize - 1, valueset.valuesIteratorPool.size());

            testValue = 0;
            while (valueLoopIterator.hasNext())
            {
                testValue += vcastType(valueLoopIterator.next().value);
            } //end IteratorLoop

            //iterator is returned automatically to its pool, by normal iteration termination
            Assert.assertEquals(initialPoolSize, valueset.valuesIteratorPool.size());

            //checksum
            Assert.assertEquals(checksum, testValue);

        } //end for rounds

        // pool initial size is untouched anyway
        Assert.assertEquals(startingPoolSize, testContainer.entryIteratorPool.size());
        Assert.assertEquals(startingPoolSize, keyset.keyIteratorPool.size());
        Assert.assertEquals(startingPoolSize, valueset.valuesIteratorPool.size());
    }

    @Test
    public void testPooledIteratorBrokenIteratorLoop()
    {
        //for-each loop interrupted

        //must accommodate even the smallest primitive type
        //so that the iteration do not break before it should...
        final int TEST_SIZE = 126;
        final long TEST_ROUNDS = 5000;

        final KTypeVTypeOpenCustomHashMap<KType, VType> testContainer = createMapWithOrderedData(TEST_SIZE);
        final KTypeVTypeOpenCustomHashMap<KType, VType>.KeysContainer keyset = testContainer.keys();
        final KTypeVTypeOpenCustomHashMap<KType, VType>.ValuesContainer valueset = testContainer.values();

        final int startingPoolSize = testContainer.entryIteratorPool.size();

        int count = 0;
        for (int round = 0; round < TEST_ROUNDS; round++)
        {
            //A) Classical iterator loop, with manually allocated Iterator
            long initialPoolSize = testContainer.entryIteratorPool.size();

            final KTypeVTypeOpenCustomHashMap<KType, VType>.EntryIterator loopIterator = testContainer.iterator();

            Assert.assertEquals(initialPoolSize - 1, testContainer.entryIteratorPool.size());

            count = 0;
            while (loopIterator.hasNext())
            {
                guard += vcastType(loopIterator.next().value);

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

            //B) Iterate on keys
            initialPoolSize = keyset.keyIteratorPool.size();

            final KTypeVTypeOpenCustomHashMap<KType, VType>.KeysIterator keyLoopIterator = keyset.iterator();

            Assert.assertEquals(initialPoolSize - 1, keyset.keyIteratorPool.size());

            count = 0;
            while (keyLoopIterator.hasNext())
            {
                guard += castType(keyLoopIterator.next().value);

                //brutally interrupt in the middle
                if (count > TEST_SIZE / 2)
                {
                    break;
                }
                count++;
            } //end IteratorLoop

            //iterator is NOT returned to its pool, due to the break.
            Assert.assertEquals(initialPoolSize - 1, keyset.keyIteratorPool.size());

            //manual return to the pool
            keyLoopIterator.release();

            //now the pool is restored
            Assert.assertEquals(initialPoolSize, keyset.keyIteratorPool.size());

            //C) Iterate on values
            initialPoolSize = valueset.valuesIteratorPool.size();

            final KTypeVTypeOpenCustomHashMap<KType, VType>.ValuesIterator valueLoopIterator = valueset.iterator();

            Assert.assertEquals(initialPoolSize - 1, valueset.valuesIteratorPool.size());

            count = 0;
            while (valueLoopIterator.hasNext())
            {
                guard += vcastType(valueLoopIterator.next().value);

                //brutally interrupt in the middle
                if (count > TEST_SIZE / 2)
                {
                    break;
                }
                count++;
            } //end IteratorLoop

            //iterator is NOT returned to its pool, due to the break.
            Assert.assertEquals(initialPoolSize - 1, valueset.valuesIteratorPool.size());

            //manual return to the pool
            valueLoopIterator.release();

            //now the pool is restored
            Assert.assertEquals(initialPoolSize, valueset.valuesIteratorPool.size());

        } //end for rounds

        // pool initial size is untouched anyway
        Assert.assertEquals(startingPoolSize, testContainer.entryIteratorPool.size());
        Assert.assertEquals(startingPoolSize, keyset.keyIteratorPool.size());
        Assert.assertEquals(startingPoolSize, valueset.valuesIteratorPool.size());
    }

    @Test
    public void testPooledIteratorExceptionIteratorLoop()
    {
        //must accommodate even the smallest primitive type
        //so that the iteration do not break before it should...
        final int TEST_SIZE = 126;
        final long TEST_ROUNDS = 5000;

        final KTypeVTypeOpenCustomHashMap<KType, VType> testContainer = createMapWithOrderedData(TEST_SIZE);

        final long checksum = testContainer.forEach(new KTypeVTypeProcedure<KType, VType>() {

            long count;

            @Override
            public void apply(final KType key, final VType value)
            {
                count += vcastType(value);
            }
        }).count;

        final int startingPoolSize = testContainer.entryIteratorPool.size();

        int count = 0;
        KTypeVTypeOpenCustomHashMap<KType, VType>.EntryIterator loopIterator = null;

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
                    guard += vcastType(loopIterator.next().value);

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

            }
            catch (final Exception e)
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
        final Random randomVK = new Random(16465131545L);
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
            final KTypeVTypeOpenCustomHashMap<KType, VType> newMap = KTypeVTypeOpenCustomHashMap.newInstance(PREALLOCATED_SIZE,
                    KTypeVTypeOpenCustomHashMap.DEFAULT_LOAD_FACTOR, new KTypeStandardHash<KType>());

            //3) Add PREALLOCATED_SIZE different values. At the end, size() must be == PREALLOCATED_SIZE,
            //and internal buffer/allocated must not have changed of size
            final int contructorBufferSize = newMap.keys.length;

            Assert.assertEquals(contructorBufferSize, newMap.allocated.length);
            Assert.assertEquals(contructorBufferSize, newMap.values.length);

            for (int i = 0; i < PREALLOCATED_SIZE; i++)
            {

                newMap.put(cast(i), vcast(randomVK.nextInt()));

                //internal size has not changed.
                Assert.assertEquals(contructorBufferSize, newMap.keys.length);
                Assert.assertEquals(contructorBufferSize, newMap.allocated.length);
                Assert.assertEquals(contructorBufferSize, newMap.values.length);
            }

            Assert.assertEquals(PREALLOCATED_SIZE, newMap.size());
        } //end for test runs
    }

    @Test
    public void testForEachProcedureWithException()
    {
        final Random randomVK = new Random(9521455645L);

        //Test that the container do not resize if less that the initial size

        //1) Choose a map to build
        /*! #if ($TemplateOptions.isKType("GENERIC", "int", "long", "float", "double")) !*/
        final int NB_ELEMENTS = 5000;
        /*!
            #elseif ($TemplateOptions.isKType("short", "char"))
             int NB_ELEMENTS = 4000;
            #else
              int NB_ELEMENTS = 126;
            #end !*/

        final KTypeVTypeOpenCustomHashMap<KType, VType> newMap = KTypeVTypeOpenCustomHashMap.newInstance(new KTypeStandardHash<KType>());

        //add a randomized number of key/values pairs
        for (int i = 0; i < NB_ELEMENTS; i++) {

            newMap.put(cast(randomVK.nextInt((int) (0.7 * NB_ELEMENTS))), vcast(randomVK.nextInt()));
        }

        //List the keys in the order of the internal buffer :
        final ArrayList<Integer> keyList = new ArrayList<Integer>();
        final ArrayList<Integer> valueList = new ArrayList<Integer>();

        for (int i = 0; i < newMap.allocated.length; i++) {

            if (newMap.allocated[i] /*! #if ($RH) !*/!= -1 /*! #end !*/) {

                keyList.add(castType(newMap.keys[i]));
                valueList.add(vcastType(newMap.values[i]));
            }
        }

        //Test forEach predicate and stop at each key in turn.
        final ArrayList<Integer> keyListTest = new ArrayList<Integer>();
        final ArrayList<Integer> valueListTest = new ArrayList<Integer>();

        for (int i = 0; i < keyList.size(); i++)
        {
            final int currentPairIndexSizeToIterate = i + 1;

            keyListTest.clear();
            valueListTest.clear();

            //Run for each
            try
            {
                newMap.forEach(new KTypeVTypeProcedure<KType, VType>() {

                    @Override
                    public void apply(final KType key, final VType value)
                    {
                        keyListTest.add(castType(key));
                        valueListTest.add(vcastType(value));

                        //when the stopping key/value pair is encountered, add to list and stop iteration
                        if (castType(key) == keyList.get(currentPairIndexSizeToIterate - 1))
                        {
                            //interrupt iteration by an exception
                            throw new RuntimeException("Interrupted treatment by test");
                        }
                    }
                });
            }
            catch (final RuntimeException e)
            {

                if (!e.getMessage().equals("Interrupted treatment by test"))
                {

                    throw e;
                }
            }
            finally
            {
                //despite the exception, the procedure terminates cleanly

                //check that keyList/keyListTest and valueList/valueListTest are identical for the first
                //currentPairIndexToIterate + 1 elements
                Assert.assertEquals(currentPairIndexSizeToIterate, keyListTest.size());
                Assert.assertEquals(currentPairIndexSizeToIterate, valueListTest.size());

                for (int j = 0; j < currentPairIndexSizeToIterate; j++)
                {
                    Assert.assertEquals(keyList.get(j), keyListTest.get(j));
                    Assert.assertEquals(valueList.get(j), valueListTest.get(j));
                }
            }

        } //end for each index
    }

    @Test
    public void testForEachPredicate()
    {
        final Random randomVK = new Random(897154957L);

        //Test that the container do not resize if less that the initial size

        //1) Choose a map to build
        /*! #if ($TemplateOptions.isKType("GENERIC", "int", "long", "float", "double")) !*/
        final int NB_ELEMENTS = 5000;
        /*!
            #elseif ($TemplateOptions.isKType("short", "char"))
             int NB_ELEMENTS = 4000;
            #else
              int NB_ELEMENTS = 126;
            #end !*/

        final KTypeVTypeOpenCustomHashMap<KType, VType> newMap = KTypeVTypeOpenCustomHashMap.newInstance(new KTypeStandardHash<KType>());

        //add a randomized number of key/values pairs
        for (int i = 0; i < NB_ELEMENTS; i++) {

            newMap.put(cast(randomVK.nextInt((int) (0.7 * NB_ELEMENTS))), vcast(randomVK.nextInt()));
        }

        //List the keys in the order of the internal buffer :
        final ArrayList<Integer> keyList = new ArrayList<Integer>();
        final ArrayList<Integer> valueList = new ArrayList<Integer>();

        for (int i = 0; i < newMap.allocated.length; i++) {

            if (newMap.allocated[i] /*! #if ($RH) !*/!= -1 /*! #end !*/) {

                keyList.add(castType(newMap.keys[i]));
                valueList.add(vcastType(newMap.values[i]));
            }
        }

        //Test forEach predicate and stop at each key in turn.
        final ArrayList<Integer> keyListTest = new ArrayList<Integer>();
        final ArrayList<Integer> valueListTest = new ArrayList<Integer>();

        for (int i = 0; i < keyList.size(); i++)
        {
            final int currentPairIndexSizeToIterate = i + 1;

            keyListTest.clear();
            valueListTest.clear();

            //Run for each with predicate
            newMap.forEach(new KTypeVTypePredicate<KType, VType>() {

                @Override
                public boolean apply(final KType key, final VType value)
                {
                    keyListTest.add(castType(key));
                    valueListTest.add(vcastType(value));

                    //when the stopping key/value pair is encountered, add to list and stop iteration
                    if (castType(key) == keyList.get(currentPairIndexSizeToIterate - 1))
                    {
                        //interrupt iteration
                        return false;
                    }

                    return true;
                }
            });

            //despite the exception, the procedure terminates cleanly

            //check that keyList/keyListTest and valueList/valueListTest are identical for the first
            //currentPairIndexToIterate + 1 elements
            Assert.assertEquals(currentPairIndexSizeToIterate, keyListTest.size());
            Assert.assertEquals(currentPairIndexSizeToIterate, valueListTest.size());

            for (int j = 0; j < currentPairIndexSizeToIterate; j++)
            {
                Assert.assertEquals(keyList.get(j), keyListTest.get(j));
                Assert.assertEquals(valueList.get(j), valueListTest.get(j));
            }

        } //end for each index
    }

    private KTypeVTypeOpenCustomHashMap<KType, VType> createMapWithOrderedData(final int size)
    {
        final KTypeVTypeOpenCustomHashMap<KType, VType> newMap = KTypeVTypeOpenCustomHashMap.newInstance(KTypeVTypeOpenCustomHashMap.DEFAULT_CAPACITY,
                KTypeVTypeOpenCustomHashMap.DEFAULT_LOAD_FACTOR, new KTypeStandardHash<KType>());

        for (int i = 0; i < size; i++)
        {
            newMap.put(cast(i), vcast(i));
        }

        return newMap;
    }
}
