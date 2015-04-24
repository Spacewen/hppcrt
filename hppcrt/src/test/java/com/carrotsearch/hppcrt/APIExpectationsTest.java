package com.carrotsearch.hppcrt;

import org.junit.Assert;
import org.junit.Test;

import com.carrotsearch.hppcrt.lists.IntArrayDeque;
import com.carrotsearch.hppcrt.lists.IntArrayList;
import com.carrotsearch.hppcrt.lists.IntStack;
import com.carrotsearch.hppcrt.lists.ObjectArrayDeque;
import com.carrotsearch.hppcrt.lists.ObjectArrayList;
import com.carrotsearch.hppcrt.lists.ObjectStack;
import com.carrotsearch.hppcrt.maps.IntIntHashMap;
import com.carrotsearch.hppcrt.maps.IntObjectHashMap;
import com.carrotsearch.hppcrt.maps.ObjectIntHashMap;
import com.carrotsearch.hppcrt.maps.ObjectObjectHashMap;
import com.carrotsearch.hppcrt.sets.IntHashSet;
import com.carrotsearch.hppcrt.sets.ObjectHashSet;
import com.carrotsearch.randomizedtesting.RandomizedTest;

/**
 * Various API expectations from generated classes.
 */
public class APIExpectationsTest extends RandomizedTest
{
    public volatile int[] t1;

    @Test
    public void testRemoveAllFromMap()
    {
        final ObjectIntHashMap<Integer> list = new ObjectIntHashMap<Integer>();
        list.put(1, 1);
        list.put(2, 2);
        list.put(3, 3);

        // Same type.
        final ObjectHashSet<Integer> other1 = new ObjectHashSet<Integer>();
        other1.add(1);
        list.removeAll(other1);

        // Supertype.
        final ObjectArrayList<Number> other2 = new ObjectArrayList<Number>();
        other2.add(1);
        list.removeAll(other2);

        // Object
        final ObjectArrayList<Object> other3 = new ObjectArrayList<Object>();
        other3.add(1);
        list.removeAll(other3);
    }

    @Test
    public void testRemoveAllWithLookupContainer()
    {
        final ObjectArrayList<Integer> list = new ObjectArrayList<Integer>();
        list.add(1);
        list.add(2);
        list.add(3);

        // Same type.
        final ObjectHashSet<Integer> other1 = new ObjectHashSet<Integer>();
        other1.add(1);
        list.removeAll(other1);

        // Supertype.
        final ObjectHashSet<Number> other2 = new ObjectHashSet<Number>();
        other2.add(1);
        list.removeAll(other2);

        // Object
        final ObjectHashSet<Object> other3 = new ObjectHashSet<Object>();
        other3.add(1);
        list.removeAll(other3);
    }

    @Test
    public void testToArrayWithClass()
    {
        final ObjectArrayDeque<Integer> l1 = ObjectArrayDeque.from(1, 2, 3);
        final Integer[] result1 = l1.toArray(Integer.class);
        Assert.assertArrayEquals(new Integer[] { 1, 2, 3 }, result1);

        final Number[] result2 = l1.toArray(Number.class);
        Assert.assertEquals(Number[].class, result2.getClass());
        Assert.assertArrayEquals(new Number[] { 1, 2, 3 }, result2);

        //To test if overriden toArray() from Stack works too:
        final ObjectStack<Integer> l2 = ObjectStack.from(1, 2, 3, 4);
        final Integer[] result3 = l2.toArray(Integer.class);
        Assert.assertArrayEquals(new Integer[] { 4, 3, 2, 1 }, result3);

        final Number[] result4 = l2.toArray(Number.class);
        Assert.assertEquals(Number[].class, result4.getClass());
        Assert.assertArrayEquals(new Number[] { 4, 3, 2, 1 }, result4);
    }

    @Test
    public void testPrimitiveToArray()
    {
        this.t1 = IntArrayList.from(1, 2, 3).toArray();
        this.t1 = IntStack.from(1, 2, 3).toArray();
        this.t1 = IntArrayDeque.from(1, 2, 3).toArray();
        this.t1 = IntHashSet.from(1, 2, 3).toArray();

        this.t1 = IntObjectHashMap.from(
                new int[] { 1, 2 }, new Long[] { 1L, 2L }).keys().toArray();
    }

    @Test
    @SuppressWarnings("unused")
    public void testNewInstance()
    {
        final IntArrayList v1 = IntArrayList.newInstance();
        final ObjectArrayList<Integer> v2 = ObjectArrayList.newInstance();
        final ObjectArrayList<Long> v3 = ObjectArrayList.newInstance();

        final IntStack v4 = IntStack.newInstance();
        final ObjectStack<Integer> v5 = ObjectStack.newInstance();
        final ObjectStack<Long> v6 = ObjectStack.newInstance();

        final IntHashSet v7 = IntHashSet.newInstance();
        final ObjectHashSet<Integer> v8 = ObjectHashSet.newInstance();
        final ObjectHashSet<Long> v9 = ObjectHashSet.newInstance();

        final IntArrayDeque v10 = IntArrayDeque.newInstance();
        final ObjectArrayDeque<Integer> v11 = ObjectArrayDeque.newInstance();
        final ObjectArrayDeque<Long> v12 = ObjectArrayDeque.newInstance();

        final IntIntHashMap v13 = IntIntHashMap.newInstance();
        final ObjectIntHashMap<Integer> v14 = ObjectIntHashMap.newInstance();
        final IntObjectHashMap<Integer> v15 = IntObjectHashMap.newInstance();
    }

    @Test
    public void testObjectToArray()
    {
        isObjectArray(ObjectArrayList.from(1, 2, 3).toArray());
        isObjectArray(ObjectStack.from(1, 2, 3).toArray());
        isObjectArray(ObjectArrayDeque.from(1, 2, 3).toArray());
        isObjectArray(ObjectHashSet.from(1, 2, 3).toArray());

        isObjectArray(ObjectObjectHashMap.from(
                new Integer[] { 1, 2 }, new Long[] { 1L, 2L }).keys().toArray());
    }

    @Test
    public void testWithClassToArray()
    {
        isIntegerArray(ObjectArrayList.from(1, 2, 3).toArray(Integer.class));
        isIntegerArray(ObjectStack.from(1, 2, 3).toArray(Integer.class));
        isIntegerArray(ObjectArrayDeque.from(1, 2, 3).toArray(Integer.class));
        isIntegerArray(ObjectHashSet.from(1, 2, 3).toArray(Integer.class));

        isIntegerArray(ObjectObjectHashMap.from(
                new Integer[] { 1, 2 }, new Long[] { 1L, 2L }).keys().toArray(Integer.class));
    }

    @Test
    public void testWildcards()
    {
        ObjectArrayList<? extends Number> t = ObjectArrayList.from(1, 2, 3);
        isTypeArray(Number.class, t.toArray(Number.class));

        t = ObjectArrayList.from(1L, 2L, 3L);
        isTypeArray(Number.class, t.toArray(Number.class));
    }

    @Test
    public void testPutOrAddOnEqualKeys()
    {
        final ObjectIntHashMap<Integer> map = ObjectIntHashMap.newInstance();

        final Integer k1 = 1;
        final Integer k1b = new Integer(k1.intValue());

        Assert.assertTrue(k1 != k1b);
        TestUtils.assertEquals2(1, map.putOrAdd(k1, 1, 2));
        Assert.assertTrue(map.containsKey(k1b));
        TestUtils.assertEquals2(3, map.putOrAdd(k1b, 1, 2));
    }

    /**
     * Check if the array is indeed of Object component type.
     */
    private void isObjectArray(final Object[] array)
    {
        isTypeArray(Object.class, array);
    }

    /**
     * 
     */
    private void isTypeArray(final Class<?> clazz, final Object[] array)
    {
        Assert.assertEquals(clazz, array.getClass().getComponentType());
    }

    /**
     * Check if the array is indeed of Integer component type.
     */
    private void isIntegerArray(final Integer[] array)
    {
        isTypeArray(Integer.class, array);
    }
}
