package com.carrotsearch.hppc;

import static com.carrotsearch.hppc.TestUtils.*;
import static org.junit.Assert.*;

import java.util.*;

import org.junit.*;

import com.carrotsearch.hppc.KTypeVTypeOpenHashMap.KeysContainer;
import com.carrotsearch.hppc.KTypeVTypeOpenHashMap.ValuesContainer;
import com.carrotsearch.hppc.cursors.*;
import com.carrotsearch.hppc.predicates.*;
import com.carrotsearch.hppc.procedures.*;

/**
 * Tests for {@link KTypeVTypeOpenHashMap}.
 */
/* ! ${TemplateOptions.generatedAnnotation} ! */
public class KTypeVTypeOpenHashMapTest<KType, VType> extends AbstractKTypeTest<KType>
{
    protected VType value0 = vcast(0);
    protected VType value1 = vcast(1);
    protected VType value2 = vcast(2);
    protected VType value3 = vcast(3);

    public volatile long guard;

    /**
     * Per-test fresh initialized instance.
     */
    public KTypeVTypeOpenHashMap<KType, VType> map = KTypeVTypeOpenHashMap.newInstance();

    @After
    public void checkEmptySlotsUninitialized()
    {
        if (map != null)
        {
            int occupied = 0;
            for (int i = 0; i < map.keys.length; i++)
            {
                if (map.allocated[i] == false)
                {
                    /*! #if ($TemplateOptions.KTypeGeneric) !*/
                    assertEquals2(Intrinsics.defaultKTypeValue(), map.keys[i]);
                    /*! #end !*/
                    /*! #if ($TemplateOptions.VTypeGeneric) !*/
                    assertEquals2(Intrinsics.defaultVTypeValue(), map.values[i]);
                    /*! #end !*/
                }
                else
                {
                    occupied++;
                }
            }
            assertEquals(occupied, map.assigned);
        }
    }

    /**
     * Convert to target type from an integer used to test stuff.
     */
    protected VType vcast(int value)
    {
        /*! #if ($TemplateOptions.VTypePrimitive)
            return (VType) value;
            #else !*/
        @SuppressWarnings("unchecked")
        VType v = (VType)(Object) value;
        return v;
        /*! #end !*/
    }

    /**
     * Convert a VType to long, (VType being a boxed elementary type or a primitive), else
     * returns 0L.
     */
    protected long vcastType(VType type)
    {
        /*! #if ($TemplateOptions.VTypePrimitive)
        return (long) type;
        #else !*/
        long k = 0L;

        if (type instanceof Character)
        {
            k = ((Character) type).charValue();
        }
        else if (type instanceof Number)
        {
            k = ((Number) type).longValue();
        }

        return k;
        /*! #end !*/
    }

    /**
     * Create a new array of a given type and copy the arguments to this array.
     */
    protected VType [] newvArray(VType... elements)
    {
        return elements;
    }

    private void assertSameMap(
            final KTypeVTypeMap<KType, VType> c1,
            final KTypeVTypeMap<KType, VType> c2)
    {
        assertEquals(c1.size(), c2.size());

        c1.forEach(new KTypeVTypeProcedure<KType, VType>()
                {
            public void apply(KType key, VType value)
            {
                assertTrue(c2.containsKey(key));
                assertEquals2(value, c2.get(key));
            }
                });
    }

    /* */
    @Test
    public void testCloningConstructor()
    {
        map.put(key1, value1);
        map.put(key2, value2);
        map.put(key3, value3);

        assertSameMap(map, KTypeVTypeOpenHashMap.from(map));
        assertSameMap(map, new KTypeVTypeOpenHashMap<KType, VType>(map));
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

        KTypeVTypeOpenHashMap<KType, VType> map2 = KTypeVTypeOpenHashMap.from(
                newArray(key1, key2, key3),
                newvArray(value1, value2, value3));

        assertSameMap(map, map2);
    }


    /* */
    @Test
    public void testPut()
    {
        map.put(key1, value1);

        assertTrue(map.containsKey(key1));
        assertEquals2(value1, map.lget());
        assertEquals2(value1, map.get(key1));
    }

    /* */
    @Test
    public void testLPut()
    {
        map.put(key1, value2);
        if (map.containsKey(key1))
            map.lset(value3);

        assertTrue(map.containsKey(key1));
        assertEquals2(value3, map.lget());
        assertEquals2(value3, map.get(key1));
    }

    /* */
    @Test
    public void testPutOverExistingKey()
    {
        map.put(key1, value1);
        assertEquals2(value1, map.put(key1, value3));
        assertEquals2(value3, map.get(key1));
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

            assertEquals(hadKey, map.containsKey(cast(v)));
            map.put(cast(v), vcast(v));
            assertEquals(values.size(), map.size());
        }
        assertEquals(values.size(), map.size());
    }

    /* */
    @Test
    public void testPutAll()
    {
        map.put(key1, value1);
        map.put(key2, value1);

        KTypeVTypeOpenHashMap<KType, VType> map2 =
                new KTypeVTypeOpenHashMap<KType, VType>();

        map2.put(key2, value2);
        map2.put(key3, value1);

        // One new key (key3).
        assertEquals(1, map.putAll(map2));

        // Assert the value under key2 has been replaced.
        assertEquals2(value2, map.get(key2));

        // And key3 has been added.
        assertEquals2(value1, map.get(key3));
        assertEquals(3, map.size());
    }

    /* */
    @Test
    public void testPutIfAbsent()
    {
        assertTrue(map.putIfAbsent(key1, value1));
        assertFalse(map.putIfAbsent(key1, value2));
        assertEquals2(value1, map.get(key1));
    }

    /*! #if ($TemplateOptions.VTypePrimitive)
    @Test
    public void testPutOrAdd()
    {
        assertEquals2(value1, map.putOrAdd(key1, value1, value2));
        assertEquals2(value1 + value2, map.putOrAdd(key1, value1, value2));
    }
    #end !*/

    /*! #if ($TemplateOptions.VTypePrimitive)
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
        assertEquals2(value1, map.remove(key1));
        assertEquals2(Intrinsics.defaultVTypeValue(), map.remove(key1));
        assertEquals(0, map.size());

        // These are internals, but perhaps worth asserting too.
        assertEquals(0, map.assigned);
    }

    /* */
    @Test
    public void testRemoveAllWithContainer()
    {
        map.put(key1, value1);
        map.put(key2, value1);
        map.put(key3, value1);

        KTypeArrayList<KType> list2 = KTypeArrayList.newInstance();
        list2.add(newArray(key2, key3, key4));

        map.removeAll(list2);
        assertEquals(1, map.size());
        assertTrue(map.containsKey(key1));
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
            public boolean apply(KType value)
            {
                return value == key2 || value == key3;
            }
                });
        assertEquals(1, map.size());
        assertTrue(map.containsKey(key1));
    }

    /* */
    @Test
    public void testRemoveViaKeySetView()
    {
        map.put(key1, value1);
        map.put(key2, value1);
        map.put(key3, value1);

        map.keys().removeAll(new KTypePredicate<KType>()
                {
            public boolean apply(KType value)
            {
                return value == key2 || value == key3;
            }
                });
        assertEquals(1, map.size());
        assertTrue(map.containsKey(key1));
    }

    /* */
    @Test
    public void testMapsIntersection()
    {
        KTypeVTypeOpenHashMap<KType, VType> map2 =
                KTypeVTypeOpenHashMap.newInstance();

        map.put(key1, value1);
        map.put(key2, value1);
        map.put(key3, value1);

        map2.put(key2, value1);
        map2.put(key4, value1);

        assertEquals(2, map.keys().retainAll(map2.keys()));

        assertEquals(1, map.size());
        assertTrue(map.containsKey(key2));
    }

    /* */
    @Test
    public void testMapKeySet()
    {
        map.put(key1, value3);
        map.put(key2, value2);
        map.put(key3, value1);

        assertSortedListEquals(map.keys().toArray(), key1, key2, key3);
    }

    /* */
    @Test
    public void testMapKeySetIterator()
    {
        map.put(key1, value3);
        map.put(key2, value2);
        map.put(key3, value1);

        int counted = 0;
        for (KTypeCursor<KType> c : map.keys())
        {
            assertEquals2(map.keys[c.index], c.value);
            counted++;
        }
        assertEquals(counted, map.size());
    }

    /* */
    @Test
    public void testClear()
    {
        map.put(key1, value1);
        map.put(key2, value1);
        map.clear();
        assertEquals(0, map.size());

        // These are internals, but perhaps worth asserting too.
        assertEquals(0, map.assigned);

        // Check if the map behaves properly upon subsequent use.
        testPutWithExpansions();
    }

    /* */
    @Test
    public void testRoundCapacity()
    {
        assertEquals(0x40000000, HashContainerUtils.roundCapacity(Integer.MAX_VALUE));
        assertEquals(0x40000000, HashContainerUtils.roundCapacity(Integer.MAX_VALUE / 2 + 1));
        assertEquals(0x40000000, HashContainerUtils.roundCapacity(Integer.MAX_VALUE / 2));
        assertEquals(KTypeVTypeOpenHashMap.MIN_CAPACITY, HashContainerUtils.roundCapacity(0));
        assertEquals(Math.max(4, KTypeVTypeOpenHashMap.MIN_CAPACITY), HashContainerUtils.roundCapacity(3));
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
        for (KTypeVTypeCursor<KType, VType> cursor : map)
        {
            count++;
            assertTrue(map.containsKey(cursor.key));
            assertEquals2(cursor.value, map.get(cursor.key));

            assertEquals2(cursor.value, map.values[cursor.index]);
            assertEquals2(cursor.key, map.keys[cursor.index]);
            assertEquals2(true, map.allocated[cursor.index]);
        }
        assertEquals(count, map.size());

        map.clear();
        assertFalse(map.iterator().hasNext());
    }

    /* */
    @Test
    public void testFullLoadFactor()
    {
        map = new KTypeVTypeOpenHashMap<KType, VType>(1, 1f);

        // Fit in the byte key range.
        int capacity = 0x80;
        int max = capacity - 1;
        for (int i = 0; i < max; i++)
        {
            map.put(cast(i), value1);
        }

        // Still not expanded.
        assertEquals(max, map.size());
        assertEquals(capacity, map.keys.length);
        // Won't expand (existing key).
        map.put(cast(0), value2);
        assertEquals(capacity, map.keys.length);
        // Expanded.
        map.put(cast(0xff), value2);
        assertEquals(2 * capacity, map.keys.length);
    }


    /* */
    @Test
    public void testBug_HPPC73_FullCapacityGet()
    {
        map = new KTypeVTypeOpenHashMap<KType, VType>(1, 1f);
        int capacity = 0x80;
        int max = capacity - 1;
        for (int i = 0; i < max; i++)
        {
            map.put(cast(i), value1);
        }
        assertEquals(max, map.size());
        assertEquals(capacity, map.keys.length);

        // Non-existent key.
        map.remove(cast(max + 1));
        assertFalse(map.containsKey(cast(max + 1)));
        assertEquals2(Intrinsics.defaultVTypeValue(), map.get(cast(max + 1)));

        // Should not expand because we're replacing an existing element.
        map.put(cast(0), value2);
        assertEquals(max, map.size());
        assertEquals(capacity, map.keys.length);

        map.putIfAbsent(cast(0), value3);
        assertEquals(max, map.size());
        assertEquals(capacity, map.keys.length);

        // Remove from a full map.
        map.remove(cast(0));
        assertEquals(max - 1, map.size());
        assertEquals(capacity, map.keys.length);
    }

    /* */
    @Test
    public void testHalfLoadFactor()
    {
        map = new KTypeVTypeOpenHashMap<KType, VType>(1, 0.5f);

        int capacity = 0x80;
        int max = capacity - 1;
        for (int i = 0; i < max; i++)
        {
            map.put(cast(i), value1);
        }

        assertEquals(max, map.size());
        // Still not expanded.
        assertEquals(2 * capacity, map.keys.length);
        // Won't expand (existing key);
        map.put(cast(0), value2);
        assertEquals(2 * capacity, map.keys.length);
        // Expanded.
        map.put(cast(0xff), value1);
        assertEquals(4 * capacity, map.keys.length);
    }

    /*! #if ($TemplateOptions.VTypeGeneric) !*/
    @SuppressWarnings("unchecked")
    /*! #end !*/
    @Test
    public void testHashCodeEquals()
    {
        KTypeVTypeOpenHashMap<KType, VType> l0 =
                new KTypeVTypeOpenHashMap<KType, VType>();
        assertEquals(0, l0.hashCode());
        assertEquals(l0, new KTypeVTypeOpenHashMap<KType, VType>());

        KTypeVTypeOpenHashMap<KType, VType> l1 = KTypeVTypeOpenHashMap.from(
                newArray(key1, key2, key3),
                newvArray(value1, value2, value3));

        KTypeVTypeOpenHashMap<KType, VType> l2 = KTypeVTypeOpenHashMap.from(
                newArray(key2, key1, key3),
                newvArray(value2, value1, value3));

        KTypeVTypeOpenHashMap<KType, VType> l3 = KTypeVTypeOpenHashMap.from(
                newArray(key1, key2),
                newvArray(value2, value1));

        assertEquals(l1.hashCode(), l2.hashCode());
        assertEquals(l1, l2);

        assertFalse(l1.equals(l3));
        assertFalse(l2.equals(l3));
    }

    /* */
    @Test
    /*! #if ($TemplateOptions.VTypeGeneric) !*/
    @SuppressWarnings("unchecked")
    /*! #end !*/
    public void testHashCodeEqualsDifferentPerturbance()
    {
        KTypeVTypeOpenHashMap<KType, VType> l0 =
                new KTypeVTypeOpenHashMap<KType, VType>() {
            @Override
            protected int computePerturbationValue(int capacity)
            {
                return 0xDEADBEEF;
            }
        };

        KTypeVTypeOpenHashMap<KType, VType> l1 =
                new KTypeVTypeOpenHashMap<KType, VType>() {
            @Override
            protected int computePerturbationValue(int capacity)
            {
                return 0xCAFEBABE;
            }
        };

        assertEquals(0, l0.hashCode());
        assertEquals(l0.hashCode(), l1.hashCode());
        assertEquals(l0, l1);

        KTypeVTypeOpenHashMap<KType, VType> l2 = KTypeVTypeOpenHashMap.from(
                newArray(key1, key2, key3),
                newvArray(value1, value2, value3));

        l0.putAll(l2);
        l1.putAll(l2);

        assertEquals(l0.hashCode(), l1.hashCode());
        assertEquals(l0, l1);
    }

    /*! #if ($TemplateOptions.VTypeGeneric) !*/
    @SuppressWarnings("unchecked")
    /*! #end !*/
    @Test
    public void testBug_HPPC37()
    {
        KTypeVTypeOpenHashMap<KType, VType> l1 = KTypeVTypeOpenHashMap.from(
                newArray(key1),
                newvArray(value1));

        KTypeVTypeOpenHashMap<KType, VType> l2 = KTypeVTypeOpenHashMap.from(
                newArray(key2),
                newvArray(value1));

        assertFalse(l1.equals(l2));
        assertFalse(l2.equals(l1));
    }

    /*! #if ($TemplateOptions.KTypeGeneric) !*/
    @Test
    public void testNullKey()
    {
        map.put(null, vcast(10));
        assertEquals2(vcast(10), map.get(null));
        assertTrue(map.containsKey(null));
        assertEquals2(vcast(10), map.lget());
        assertEquals2(null, map.lkey());
        map.remove(null);
        assertEquals(0, map.size());
    }
    /*! #end !*/

    /*! #if ($TemplateOptions.KTypeGeneric) !*/
    @Test
    @SuppressWarnings("unchecked")
    public void testLkey()
    {
        map.put(key1, vcast(10));
        assertTrue(map.containsKey(key1));
        assertSame(key1, map.lkey());
        KType key1_ = (KType) new Integer(1);
        assertNotSame(key1, key1_);
        assertEquals(key1, key1_);
        assertTrue(map.containsKey(key1_));
        assertSame(key1, map.lkey());
    }
    /*! #end !*/

    /*! #if ($TemplateOptions.VTypeGeneric) !*/
    @Test
    public void testNullValue()
    {
        map.put(key1, null);
        assertEquals(null, map.get(key1));
        assertTrue(map.containsKey(key1));
        map.remove(key1);
        assertFalse(map.containsKey(key1));
        assertEquals(0, map.size());
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
                KType key = cast(rnd.nextInt(size));
                VType value = vcast(rnd.nextInt());

                if (rnd.nextBoolean())
                {
                    map.put(key, value);
                    other.put(key, value);

                    assertEquals(value, map.get(key));
                    assertTrue(map.containsKey(key));
                    assertEquals(value, map.lget());
                }
                else
                {
                    assertEquals(other.remove(key), map.remove(key));
                }

                assertEquals(other.size(), map.size());
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

        KTypeVTypeOpenHashMap<KType, VType> cloned = map.clone();
        cloned.remove(key1);

        assertSortedListEquals(map.keys().toArray(), key1, key2, key3);
        assertSortedListEquals(cloned.keys().toArray(), key2, key3);
    }

    /*
     * 
     */
    @Test
    public void testToString()
    {
        Assume.assumeTrue(
                (int[].class.isInstance(map.keys)     ||
                        short[].class.isInstance(map.keys)   ||
                        byte[].class.isInstance(map.keys)    ||
                        long[].class.isInstance(map.keys)    ||
                        Object[].class.isInstance(map.keys)) &&
                        (int[].class.isInstance(map.values)   ||
                                byte[].class.isInstance(map.values)  ||
                                short[].class.isInstance(map.values) ||
                                long[].class.isInstance(map.values)  ||
                                Object[].class.isInstance(map.values)));

        this.map.put(key1, value1);
        this.map.put(key2, value2);

        String asString = map.toString();
        asString = asString.replaceAll("[^0-9]", "");
        char [] asCharArray = asString.toCharArray();
        Arrays.sort(asCharArray);
        assertEquals("1122", new String(asCharArray));
    }

    @Test
    public void testAddRemoveSameHashCollision()
    {
        // This test is only applicable to selected key types.
        Assume.assumeTrue(
                int[].class.isInstance(map.keys) ||
                long[].class.isInstance(map.keys) ||
                Object[].class.isInstance(map.keys));

        IntArrayList hashChain = TestUtils.generateMurmurHash3CollisionChain(0x1fff, 0x7e, 0x1fff /3);

        /*
         * Add all of the conflicting keys to a map.
         */
        for (IntCursor c : hashChain)
            map.put(cast(c.value), this.value1);

        assertEquals(hashChain.size(), map.size());

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
            map.put(cast(c.value), value2);

        assertEquals(hashChain.size() + differentKeys.size(), map.size());

        /*
         * Verify the map contains all of the conflicting keys.
         */
        for (IntCursor c : hashChain)
            assertEquals2(value1, map.get(cast(c.value)));

        /*
         * Verify the map contains all the other keys.
         */
        for (IntCursor c : differentKeys)
            assertEquals2(value2, map.get(cast(c.value)));

        /*
         * Iteratively remove the keys, from first to last.
         */
        for (IntCursor c : hashChain)
            assertEquals2(value1, map.remove(cast(c.value)));

        assertEquals(differentKeys.size(), map.size());

        /*
         * Verify the map contains all the other keys.
         */
        for (IntCursor c : differentKeys)
            assertEquals2(value2, map.get(cast(c.value)));
    }

    /* */
    @Test
    public void testMapValues()
    {
        map.put(key1, value3);
        map.put(key2, value2);
        map.put(key3, value1);
        assertSortedListEquals(map.values().toArray(), value1, value2, value3);

        map.clear();
        map.put(key1, value1);
        map.put(key2, value2);
        map.put(key3, value2);
        assertSortedListEquals(map.values().toArray(), value1, value2, value2);
    }

    /* */
    @Test
    public void testMapValuesIterator()
    {
        map.put(key1, value3);
        map.put(key2, value2);
        map.put(key3, value1);

        int counted = 0;
        for (KTypeCursor<VType> c : map.values())
        {
            assertEquals2(map.values[c.index], c.value);
            counted++;
        }
        assertEquals(counted, map.size());
    }

    /* */
    @Test
    public void testMapValuesContainer()
    {
        map.put(key1, value1);
        map.put(key2, value2);
        map.put(key3, value2);

        // contains()
        for (KTypeVTypeCursor<KType, VType> c : map)
            assertTrue(map.values().contains(c.value));
        assertFalse(map.values().contains(value3));

        assertEquals(map.isEmpty(), map.values().isEmpty());
        assertEquals(map.size(), map.values().size());

        final KTypeArrayList<VType> values = new KTypeArrayList<VType>();
        map.values().forEach(new KTypeProcedure<VType>()
                {
            public void apply(VType value)
            {
                values.add(value);
            }
                });
        assertSortedListEquals(map.values().toArray(), value1, value2, value2);

        values.clear();
        map.values().forEach(new KTypePredicate<VType>()
                {
            public boolean apply(VType value)
            {
                values.add(value);
                return true;
            }
                });
        assertSortedListEquals(map.values().toArray(), value1, value2, value2);
    }

    /*! #if ($TemplateOptions.KTypeGeneric) !*/
    //only applicable to generic types keys
    @Test
    public void testHashingStrategyCloneEquals() {

        //Works only with keys as objects
        Assume.assumeTrue(Object[].class.isInstance(map.keys));

        //a) Check that 2 different sets filled the same way with same values and strategies = null
        //are indeed equal.
        long TEST_SEED = 4987013210686416456L;
        int TEST_SIZE = (int)500e3;
        KTypeVTypeOpenHashMap<KType, VType> refMap = createMapWithRandomData(TEST_SIZE, null, TEST_SEED);
        KTypeVTypeOpenHashMap<KType, VType> refMap2 =createMapWithRandomData(TEST_SIZE, null, TEST_SEED);

        assertEquals(refMap, refMap2);

        //b) Clone the above. All sets are now identical.
        KTypeVTypeOpenHashMap<KType, VType> refMapclone = refMap.clone();
        KTypeVTypeOpenHashMap<KType, VType> refMap2clone = refMap2.clone();

        //all strategies are null
        assertEquals(refMap.strategy(), refMap2.strategy());
        assertEquals(refMap2.strategy(), refMapclone.strategy());
        assertEquals(refMapclone.strategy(), refMap2clone.strategy());
        assertEquals(refMap2clone.strategy(), null);

        assertEquals(refMap, refMapclone);
        assertEquals(refMapclone, refMap2);
        assertEquals(refMap2, refMap2clone);
        assertEquals(refMap2clone, refMap);

        //cleanup
        refMapclone = null;
        refMap2 = null;
        refMap2clone = null;
        System.gc();

        //c) Create a set nb 3 with same integer content, but with a strategy mapping on equals.
        KTypeVTypeOpenHashMap<KType, VType> refMap3 = createMapWithRandomData(TEST_SIZE,
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
        assertFalse(refMap.equals(refMap3));

        //However, if we cloned refMap3
        KTypeVTypeOpenHashMap<KType, VType> refMap3clone = refMap3.clone();
        assertEquals(refMap3, refMap3clone);

        //strategies are copied by reference only
        assertTrue(refMap3.strategy() == refMap3clone.strategy());

        //d) Create identical set with same different strategy instances, but which consider themselves equals()
        KTypeVTypeOpenHashMap<KType, VType> refMap4 = createMapWithRandomData(TEST_SIZE,
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

        KTypeVTypeOpenHashMap<KType, VType> refMap4Image = createMapWithRandomData(TEST_SIZE,
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

        assertEquals(refMap4, refMap4Image);
        //but strategy instances are indeed 2 different objects
        assertFalse(refMap4.strategy() == refMap4Image.strategy());

        //cleanup
        refMap4 = null;
        refMap4Image = null;
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

        KTypeVTypeOpenHashMap<KType, VType> refMap5 = createMapWithRandomData(TEST_SIZE, alwaysDifferentStrategy, TEST_SEED);
        KTypeVTypeOpenHashMap<KType, VType> refMap5alwaysDifferent = createMapWithRandomData(TEST_SIZE, alwaysDifferentStrategy, TEST_SEED);

        //both sets are NOT equal because their strategies said they are different
        assertFalse(refMap5.equals(refMap5alwaysDifferent));
    }

    @Test
    public void testHashingStrategyAddContainsGetRemove() {

        //Works only with keys as objects
        Assume.assumeTrue(Object[].class.isInstance(map.keys));


        long TEST_SEED = 15249155965216185L;
        int TEST_SIZE = (int)500e3;

        //those following 3  maps behave indeed the same in the test context:
        KTypeVTypeOpenHashMap<KType, VType> refMap = KTypeVTypeOpenHashMap.newInstance();

        KTypeVTypeOpenHashMap<KType, VType> refMapNullStrategy = KTypeVTypeOpenHashMap.newInstance(
                KTypeVTypeOpenHashMap.DEFAULT_CAPACITY,
                KTypeVTypeOpenHashMap.DEFAULT_LOAD_FACTOR, null);

        KTypeVTypeOpenHashMap<KType, VType> refMapIdenticalStrategy = KTypeVTypeOpenHashMap.newInstance(
                KTypeVTypeOpenHashMap.DEFAULT_CAPACITY,
                KTypeVTypeOpenHashMap.DEFAULT_LOAD_FACTOR,
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

        for (int i = 0 ; i < TEST_SIZE; i++) {

            //a) generate a value to put
            int putKey = prng.nextInt();
            int putValue = prng.nextInt();

            refMap.put(cast(putKey), vcast(putValue));
            refMapNullStrategy.put(cast(putKey), vcast(putValue));
            refMapIdenticalStrategy.put(cast(putKey), vcast(putValue));

            assertEquals(refMap.containsKey(cast(putKey)), refMapNullStrategy.containsKey(cast(putKey)));
            assertEquals(refMap.containsKey(cast(putKey)), refMapIdenticalStrategy.containsKey(cast(putKey)));

            /*! #if ($TemplateOptions.VTypeGeneric) !*/
            assertEquals(refMap.get(cast(putKey)), refMapNullStrategy.get(cast(putKey)));
            assertEquals(refMap.get(cast(putKey)) ,  refMapIdenticalStrategy.get(cast(putKey)));
            /*! #else
            assertTrue(refMap.get(cast(putKey)) == refMapNullStrategy.get(cast(putKey)));
            assertTrue(refMap.get(cast(putKey)) ==  refMapIdenticalStrategy.get(cast(putKey)));
             #end !*/

            boolean isToBeRemoved = (prng.nextInt() % 3  == 0);
            putKey = prng.nextInt();

            if (isToBeRemoved) {

                refMap.remove(cast(putKey));
                refMapNullStrategy.remove(cast(putKey));
                refMapIdenticalStrategy.remove(cast(putKey));

                assertFalse(refMap.containsKey(cast(putKey)));
                assertFalse(refMapNullStrategy.containsKey(cast(putKey)));
                assertFalse(refMapIdenticalStrategy.containsKey(cast(putKey)));
            }

            assertEquals(refMap.containsKey(cast(putKey)), refMapNullStrategy.containsKey(cast(putKey)));
            assertEquals(refMap.containsKey(cast(putKey)), refMapIdenticalStrategy.containsKey(cast(putKey)));

            /*! #if ($TemplateOptions.VTypeGeneric) !*/
            assertEquals(refMap.get(cast(putKey)), refMapNullStrategy.get(cast(putKey)));
            assertEquals(refMap.get(cast(putKey)) ,  refMapIdenticalStrategy.get(cast(putKey)));
            /*! #else
            assertTrue(refMap.get(cast(putKey)) == refMapNullStrategy.get(cast(putKey)));
            assertTrue(refMap.get(cast(putKey)) ==  refMapIdenticalStrategy.get(cast(putKey)));
             #end !*/

            //test size
            assertEquals(refMap.size(), refMapNullStrategy.size());
            assertEquals(refMap.size(), refMapIdenticalStrategy.size());
        }
    }

    private KTypeVTypeOpenHashMap<KType, VType> createMapWithRandomData(int size, HashingStrategy<KType> strategy, long randomSeed) {

        Random prng = new Random(randomSeed);

        KTypeVTypeOpenHashMap<KType, VType> newMap = KTypeVTypeOpenHashMap.newInstance(KTypeVTypeOpenHashMap.DEFAULT_CAPACITY,
                KTypeVTypeOpenHashMap.DEFAULT_LOAD_FACTOR, strategy);

        for (int i = 0; i < size ; i++) {

            newMap.put(cast(prng.nextInt()), vcast(prng.nextInt()));
        }

        return newMap;
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

        KTypeVTypeOpenHashMap<KType, VType> testContainer = createMapWithOrderedData(TEST_SIZE);
        KTypeVTypeOpenHashMap<KType, VType>.KeysContainer keyset = testContainer.keys();
        KTypeVTypeOpenHashMap<KType, VType>.ValuesContainer valueset = testContainer.values();

        long checksum = testContainer.forEach(new KTypeVTypeProcedure<KType,VType>() {

            long count;

            @Override
            public void apply(KType key, VType value)
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
            for (KTypeVTypeCursor<KType,VType> cursor : testContainer)
            {
                //we consume 1 iterator for this loop
                assertEquals(initialPoolSize - 1, testContainer.entryIteratorPool.size());

                testValue += vcastType(cursor.value);
            }

            //check checksum the iteration
            assertEquals(checksum, testValue);

            //iterator is returned to its pool
            assertEquals(initialPoolSize, testContainer.entryIteratorPool.size());

            //B) Loop on keys == same value as values
            testValue = 0;
            initialPoolSize = keyset.keyIteratorPool.size();

            for (KTypeCursor<KType> cursor : keyset)
            {
                //we consume 1 iterator for this loop
                assertEquals(initialPoolSize - 1, keyset.keyIteratorPool.size());

                testValue += castType(cursor.value);
            }

            //check checksum the iteration
            assertEquals(checksum, testValue);

            //iterator is returned to its pool
            assertEquals(initialPoolSize, keyset.keyIteratorPool.size());

            //C) Loop on values
            testValue = 0;
            initialPoolSize = valueset.valuesIteratorPool.size();

            for (KTypeCursor<VType> cursor : valueset)
            {
                //we consume 1 iterator for this loop
                assertEquals(initialPoolSize - 1, valueset.valuesIteratorPool.size());

                testValue += vcastType(cursor.value);
            }

            //check checksum the iteration
            assertEquals(checksum, testValue);

            //iterator is returned to its pool
            assertEquals(initialPoolSize, valueset.valuesIteratorPool.size());

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

        KTypeVTypeOpenHashMap<KType, VType> testContainer = createMapWithOrderedData(TEST_SIZE);
        KTypeVTypeOpenHashMap<KType, VType>.KeysContainer keyset = testContainer.keys();
        KTypeVTypeOpenHashMap<KType, VType>.ValuesContainer valueset = testContainer.values();

        int count = 0;
        for (int round = 0; round < TEST_ROUNDS; round++)
        {
            //A) for-each in test :
            long initialPoolSize = testContainer.entryIteratorPool.size();
            count = 0;
            for (KTypeVTypeCursor<KType, VType> cursor : testContainer)
            {
                guard += vcastType(cursor.value);
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

            //B) Loop on keys
            initialPoolSize = keyset.keyIteratorPool.size();
            count = 0;
            for (KTypeCursor<KType> cursor : keyset)
            {
                guard += castType(cursor.value);
                //we consume 1 iterator for this loop, but reallocs can happen,
                //so we can only say its != initialPoolSize
                assertTrue(initialPoolSize != keyset.keyIteratorPool.size());

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
            assertTrue(initialPoolSize != keyset.keyIteratorPool.size());

            //C) Loop on values
            initialPoolSize = valueset.valuesIteratorPool.size();
            count = 0;
            for (KTypeCursor<VType> cursor : valueset)
            {
                guard += vcastType(cursor.value);
                //we consume 1 iterator for this loop, but reallocs can happen,
                //so we can only say its != initialPoolSize
                assertTrue(initialPoolSize != valueset.valuesIteratorPool.size());

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
            assertTrue(initialPoolSize != valueset.valuesIteratorPool.size());
        } //end for rounds

        //Due to policy of the Iterator pool, the intended pool never get bigger that some limit
        //despite the Iterator leak.
        assertTrue(testContainer.entryIteratorPool.capacity() < IteratorPool.MAX_SIZE_GROWTH_FACTOR * Internals.NB_OF_PROCESSORS + 1);
        assertTrue(keyset.keyIteratorPool.capacity() < IteratorPool.MAX_SIZE_GROWTH_FACTOR * Internals.NB_OF_PROCESSORS + 1);
        assertTrue(valueset.valuesIteratorPool.capacity() < IteratorPool.MAX_SIZE_GROWTH_FACTOR * Internals.NB_OF_PROCESSORS + 1);
    }

    @Test
    public void testPooledIteratorFullIteratorLoop()
    {
        // for-each loop interrupted

        //must accommodate even the smallest primitive type
        //so that the iteration do not break before it should...
        int TEST_SIZE = 126;
        long TEST_ROUNDS = 10000;

        KTypeVTypeOpenHashMap<KType, VType> testContainer = createMapWithOrderedData(TEST_SIZE);
        KTypeVTypeOpenHashMap<KType, VType>.KeysContainer keyset = testContainer.keys();
        KTypeVTypeOpenHashMap<KType, VType>.ValuesContainer valueset = testContainer.values();

        long checksum = testContainer.forEach(new KTypeVTypeProcedure<KType, VType>() {

            long count;

            @Override
            public void apply(KType key, VType value)
            {
                count += vcastType(value);
            }
        }).count;

        long testValue = 0;
        int startingPoolSize = testContainer.entryIteratorPool.size();

        for (int round = 0; round < TEST_ROUNDS; round++)
        {
            //A) Classical iterator loop, with manually allocated Iterator
            int initialPoolSize = testContainer.entryIteratorPool.size();

            Iterator<KTypeVTypeCursor<KType, VType>> loopIterator = testContainer.iterator();

            assertEquals(initialPoolSize - 1, testContainer.entryIteratorPool.size());

            testValue = 0;
            while (loopIterator.hasNext())
            {
                testValue += vcastType(loopIterator.next().value);
            } //end IteratorLoop

            //iterator is returned automatically to its pool, by normal iteration termination
            assertEquals(initialPoolSize, testContainer.entryIteratorPool.size());

            //checksum
            assertEquals(checksum, testValue);

            //B) Loop on keys
            initialPoolSize = keyset.keyIteratorPool.size();

            Iterator<KTypeCursor<KType>> keyLoopIterator = keyset.iterator();

            assertEquals(initialPoolSize - 1, keyset.keyIteratorPool.size());

            testValue = 0;
            while (keyLoopIterator.hasNext())
            {
                testValue += castType(keyLoopIterator.next().value);
            } //end IteratorLoop

            //iterator is returned automatically to its pool, by normal iteration termination
            assertEquals(initialPoolSize, keyset.keyIteratorPool.size());

            //checksum
            assertEquals(checksum, testValue);

            //C) Loop on values
            initialPoolSize = valueset.valuesIteratorPool.size();

            Iterator<KTypeCursor<VType>> valueLoopIterator = valueset.iterator();

            assertEquals(initialPoolSize - 1, valueset.valuesIteratorPool.size());

            testValue = 0;
            while (valueLoopIterator.hasNext())
            {
                testValue += vcastType(valueLoopIterator.next().value);
            } //end IteratorLoop

            //iterator is returned automatically to its pool, by normal iteration termination
            assertEquals(initialPoolSize, valueset.valuesIteratorPool.size());

            //checksum
            assertEquals(checksum, testValue);

        } //end for rounds

        // pool initial size is untouched anyway
        assertEquals(startingPoolSize, testContainer.entryIteratorPool.size());
        assertEquals(startingPoolSize, keyset.keyIteratorPool.size());
        assertEquals(startingPoolSize, valueset.valuesIteratorPool.size());
    }

    @Test
    public void testPooledIteratorBrokenIteratorLoop()
    {
        //for-each loop interrupted

        //must accommodate even the smallest primitive type
        //so that the iteration do not break before it should...
        int TEST_SIZE = 126;
        long TEST_ROUNDS = 10000;

        KTypeVTypeOpenHashMap<KType, VType> testContainer = createMapWithOrderedData(TEST_SIZE);
        KTypeVTypeOpenHashMap<KType, VType>.KeysContainer keyset = testContainer.keys();
        KTypeVTypeOpenHashMap<KType, VType>.ValuesContainer valueset = testContainer.values();

        int startingPoolSize = testContainer.entryIteratorPool.size();

        int count = 0;
        for (int round = 0; round < TEST_ROUNDS; round++)
        {
            //A) Classical iterator loop, with manually allocated Iterator
            long initialPoolSize = testContainer.entryIteratorPool.size();

            AbstractIterator<KTypeVTypeCursor<KType, VType>> loopIterator = (AbstractIterator<KTypeVTypeCursor<KType, VType>>) testContainer.iterator();

            assertEquals(initialPoolSize - 1, testContainer.entryIteratorPool.size());

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
            assertEquals(initialPoolSize - 1, testContainer.entryIteratorPool.size());

            //manual return to the pool
            loopIterator.release();

            //now the pool is restored
            assertEquals(initialPoolSize, testContainer.entryIteratorPool.size());

            //B) Iterate on keys
            initialPoolSize = keyset.keyIteratorPool.size();

            AbstractIterator<KTypeCursor<KType>> keyLoopIterator = (AbstractIterator<KTypeCursor<KType>>) keyset.iterator();

            assertEquals(initialPoolSize - 1, keyset.keyIteratorPool.size());

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
            assertEquals(initialPoolSize - 1, keyset.keyIteratorPool.size());

            //manual return to the pool
            keyLoopIterator.release();

            //now the pool is restored
            assertEquals(initialPoolSize, keyset.keyIteratorPool.size());

            //C) Iterate on values
            initialPoolSize = valueset.valuesIteratorPool.size();

            AbstractIterator<KTypeCursor<VType>> valueLoopIterator = (AbstractIterator<KTypeCursor<VType>>) valueset.iterator();

            assertEquals(initialPoolSize - 1, valueset.valuesIteratorPool.size());

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
            assertEquals(initialPoolSize - 1, valueset.valuesIteratorPool.size());

            //manual return to the pool
            valueLoopIterator.release();

            //now the pool is restored
            assertEquals(initialPoolSize, valueset.valuesIteratorPool.size());

        } //end for rounds

        // pool initial size is untouched anyway
        assertEquals(startingPoolSize, testContainer.entryIteratorPool.size());
        assertEquals(startingPoolSize, keyset.keyIteratorPool.size());
        assertEquals(startingPoolSize, valueset.valuesIteratorPool.size());
    }

    @Test
    public void testPooledIteratorExceptionIteratorLoop()
    {
        //must accommodate even the smallest primitive type
        //so that the iteration do not break before it should...
        int TEST_SIZE = 126;
        long TEST_ROUNDS = 10000;

        KTypeVTypeOpenHashMap<KType, VType> testContainer = createMapWithOrderedData(TEST_SIZE);

        long checksum = testContainer.forEach(new KTypeVTypeProcedure<KType, VType>() {

            long count;

            @Override
            public void apply(KType key, VType value)
            {
                count += vcastType(value);
            }
        }).count;

        int startingPoolSize = testContainer.entryIteratorPool.size();

        int count = 0;
        AbstractIterator<KTypeVTypeCursor<KType, VType>> loopIterator = null;

        for (int round = 0; round < TEST_ROUNDS; round++)
        {
            try
            {
                loopIterator = (AbstractIterator<KTypeVTypeCursor<KType, VType>>) testContainer.iterator();

                assertEquals(startingPoolSize - 1, testContainer.entryIteratorPool.size());

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

    private KTypeVTypeOpenHashMap<KType, VType> createMapWithOrderedData(int size)
    {

        KTypeVTypeOpenHashMap<KType, VType> newMap = KTypeVTypeOpenHashMap.newInstance(KTypeVTypeOpenHashMap.DEFAULT_CAPACITY,
                KTypeVTypeOpenHashMap.DEFAULT_LOAD_FACTOR);

        for (int i = 0; i < size; i++)
        {
            newMap.put(cast(i), vcast(i));
        }

        return newMap;
    }
}
