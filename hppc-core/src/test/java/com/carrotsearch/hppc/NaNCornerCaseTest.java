package com.carrotsearch.hppc;

import java.util.Arrays;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import com.carrotsearch.hppc.sorting.DoubleSort;
import com.carrotsearch.hppc.sorting.FloatSort;

public class NaNCornerCaseTest
{

    /** @see "http://issues.carrot2.org/browse/HPPC-93" */
    @Test
    public void testDoubleNaNInfinitesComparison()
    {
        final double NaN1 = Double.NaN;

        final double NaN2 = Double.longBitsToDouble(0x7ff8000000000001L);
        final double NaN3 = Double.longBitsToDouble(0x7ff8000000000010L);
        final double NaN4 = Double.longBitsToDouble(0x7ff8000000000011L);
        final double NaN5 = Double.longBitsToDouble(0x7ff8000000000111L);
        final double NaN6 = Double.longBitsToDouble(0x7ff8000000001111L);
        final double NaN7 = Double.longBitsToDouble(0x7ff8000000011111L);
        final double NaN8 = Double.longBitsToDouble(0x7ff8000000111111L);
        final double NaN9 = Double.longBitsToDouble(0x7ff8000000101101L);
        final double NaN10 = Double.longBitsToDouble(0x7ff8000011111111L);

        final double[] nanList = new double[] { NaN1, NaN2, NaN3, NaN4, NaN5, NaN6, NaN7, NaN8, NaN9, NaN10 };

        System.out.println(Arrays.toString(nanList));

        for (int i = 0; i < nanList.length; i++) {

            System.out.println(Double.doubleToLongBits(nanList[i]) + " raw : " + Double.doubleToRawLongBits(nanList[i]));

            if (i < nanList.length - 1) {

                //All NaN are equal by doubleToLongBits
                Assert.assertTrue(Double.doubleToLongBits(nanList[i]) == Double.doubleToLongBits(nanList[i + 1]));

                //Even if they are indeed binary different
                Assert.assertFalse(Double.doubleToRawLongBits(nanList[i]) == Double.doubleToRawLongBits(nanList[i + 1]));

                //All NaNs are equals by compare
                Assert.assertTrue(Double.compare(nanList[i], nanList[i + 1]) == 0);
            }

            //Double NaN are considered superior to everything by compare(), so support ordering
            Assert.assertTrue(Double.compare(nanList[i], Double.NEGATIVE_INFINITY) > 0);
            Assert.assertTrue(Double.compare(nanList[i], 0.0) > 0);
            Assert.assertTrue(Double.compare(nanList[i], 10.0) > 0);
            Assert.assertTrue(Double.compare(nanList[i], 1e89) > 0);
            Assert.assertTrue(Double.compare(nanList[i], Double.POSITIVE_INFINITY) > 0);
        } //end for

        //Try to sort 
        final int NB_SORT_ELEMENTS = (int) 1e6;

        final double[] referenceArray = new double[NB_SORT_ELEMENTS];
        final double[] testArray = new double[NB_SORT_ELEMENTS];
        final Random prng = new Random(11841548441L);
        final Random prngNaN = new Random(78995021461145L);

        for (int i = 0; i < NB_SORT_ELEMENTS; i++) {

            final double testValue = prng.nextDouble() * NB_SORT_ELEMENTS;

            if (i % 111 == 0) {

                //salt with different NaN values
                final int prngNaNIndex = prngNaN.nextInt(nanList.length);
                referenceArray[i] = nanList[prngNaNIndex];
                testArray[i] = nanList[prngNaNIndex];
            }
            else {
                referenceArray[i] = testValue;
                testArray[i] = testValue;
            }
        }

        //Compare the sorts JDK vs. HPPC
        Arrays.sort(referenceArray);
        DoubleSort.quicksort(testArray);

        for (int i = 0; i < NB_SORT_ELEMENTS; i++) {

            Assert.assertEquals(Double.doubleToLongBits(referenceArray[i]), Double.doubleToLongBits(testArray[i]));
            Assert.assertTrue(Double.compare(referenceArray[i], testArray[i]) == 0);

            if (!Double.isNaN(referenceArray[i])) {

                Assert.assertTrue(referenceArray[i] == testArray[i]);
            }
        }
    }

    /** @see "http://issues.carrot2.org/browse/HPPC-93" */
    @Test
    public void testFloatNaNInfinitesComparison()
    {
        final float NaN1 = Float.NaN;

        final float NaN2 = Float.intBitsToFloat(0x7fc00011);
        final float NaN3 = Float.intBitsToFloat(0x7fc00001);
        final float NaN4 = Float.intBitsToFloat(0x7fc00010);
        final float NaN5 = Float.intBitsToFloat(0x7fc00101);
        final float NaN6 = Float.intBitsToFloat(0x7fc00110);
        final float NaN7 = Float.intBitsToFloat(0x7fc00111);
        final float NaN8 = Float.intBitsToFloat(0x7fc01011);
        final float NaN9 = Float.intBitsToFloat(0x7fc01111);
        final float NaN10 = Float.intBitsToFloat(0x7fc10001);

        final float[] nanList = new float[] { NaN1, NaN2, NaN3, NaN4, NaN5, NaN6, NaN7, NaN8, NaN9, NaN10 };

        System.out.println(Arrays.toString(nanList));

        for (int i = 0; i < nanList.length; i++) {

            System.out.println(Float.floatToIntBits(nanList[i]) + " raw : " + Float.floatToRawIntBits(nanList[i]));

            if (i < nanList.length - 1) {

                //All NaN are equal by floatToIntBits
                Assert.assertTrue(Float.floatToIntBits(nanList[i]) == Float.floatToIntBits(nanList[i + 1]));

                //Even if they are indeed binary different
                Assert.assertFalse(Float.floatToRawIntBits(nanList[i]) == Float.floatToRawIntBits(nanList[i + 1]));

                //All NaNs are equals by compare
                Assert.assertTrue(Float.compare(nanList[i], nanList[i + 1]) == 0);
            }

            //Float NaN are considered superior to everything by compare(), so support ordering
            Assert.assertTrue(Float.compare(nanList[i], Float.NEGATIVE_INFINITY) > 0);
            Assert.assertTrue(Float.compare(nanList[i], 0f) > 0);
            Assert.assertTrue(Float.compare(nanList[i], 10f) > 0);
            Assert.assertTrue(Float.compare(nanList[i], 44887774f) > 0);
            Assert.assertTrue(Float.compare(nanList[i], Float.POSITIVE_INFINITY) > 0);
        } //end for

        //Try to sort 
        final int NB_SORT_ELEMENTS = (int) 1e6;

        final float[] referenceArray = new float[NB_SORT_ELEMENTS];
        final float[] testArray = new float[NB_SORT_ELEMENTS];
        final Random prng = new Random(97456114L);
        final Random prngNaN = new Random(12544885441L);

        for (int i = 0; i < NB_SORT_ELEMENTS; i++) {

            final float testValue = prng.nextFloat() * NB_SORT_ELEMENTS;

            if (i % 111 == 0) {

                //salt with different NaN values
                final int prngNaNIndex = prngNaN.nextInt(nanList.length);
                referenceArray[i] = nanList[prngNaNIndex];
                testArray[i] = nanList[prngNaNIndex];
            }
            else {
                referenceArray[i] = testValue;
                testArray[i] = testValue;
            }
        }

        //Compare the sorts JDK vs. HPPC
        Arrays.sort(referenceArray);
        FloatSort.quicksort(testArray);

        for (int i = 0; i < NB_SORT_ELEMENTS; i++) {

            Assert.assertEquals(Float.floatToIntBits(referenceArray[i]), Float.floatToIntBits(testArray[i]));
            Assert.assertTrue(Float.compare(referenceArray[i], testArray[i]) == 0);

            if (!Float.isNaN(referenceArray[i])) {

                Assert.assertTrue(referenceArray[i] == testArray[i]);
            }
        }
    }

    /** @see "http://issues.carrot2.org/browse/HPPC-93" */
    @Test
    public void testNaNAsDoubleKey()
    {
        final DoubleObjectMap<String> map = DoubleObjectOpenHashMap.newInstance();
        map.put(Double.NaN, "a");
        map.put(Double.NaN, "b");

        Assert.assertEquals(1, map.size());
        Assert.assertEquals("b", map.get(Double.NaN));

        map.put(Double.longBitsToDouble(0xfff8000000000000L), "c");
        Assert.assertEquals(1, map.size());
        Assert.assertEquals("c", map.get(Double.NaN));
        Assert.assertEquals(
                (Double) map.keys().iterator().next().value,
                (Double) Double.NaN);

        final DoubleOpenHashSet set = DoubleOpenHashSet.newInstance();
        set.add(Double.NaN);
        set.add(Double.NaN);
        set.add(Double.longBitsToDouble(0xfff8000000000000L));
        Assert.assertEquals(1, set.size());
        Assert.assertEquals(
                (Double) set.iterator().next().value,
                (Double) Double.NaN);
    }

    /** @see "http://issues.carrot2.org/browse/HPPC-93" */
    @Test
    public void testNaNAsFloatKey()
    {
        final FloatObjectMap<String> map = FloatObjectOpenHashMap.newInstance();
        map.put(Float.NaN, "a");
        map.put(Float.NaN, "b");

        Assert.assertEquals(1, map.size());
        Assert.assertEquals("b", map.get(Float.NaN));

        map.put(Float.intBitsToFloat(0xfff80000), "c");
        Assert.assertEquals(1, map.size());
        Assert.assertEquals("c", map.get(Float.NaN));
        Assert.assertEquals(
                (Float) map.keys().iterator().next().value,
                (Float) Float.NaN);

        final FloatOpenHashSet set = FloatOpenHashSet.newInstance();
        set.add(Float.NaN);
        set.add(Float.NaN);
        set.add(Float.intBitsToFloat(0xfff80000));
        Assert.assertEquals(1, set.size());
        Assert.assertEquals(
                (Float) set.iterator().next().value,
                (Float) Float.NaN);
    }

    /** @see "http://issues.carrot2.org/browse/HPPC-93" */
    @Test
    public void testNaNAsValue()
    {
        {
            final IntDoubleMap m1 = IntDoubleOpenHashMap.newInstance();
            m1.put(1, Double.NaN);
            final IntDoubleMap m2 = IntDoubleOpenHashMap.newInstance();
            m2.put(1, Double.NaN);
            Assert.assertEquals(m1, m2);
        }

        {
            final IntFloatMap m1 = IntFloatOpenHashMap.newInstance();
            m1.put(1, Float.NaN);
            final IntFloatMap m2 = IntFloatOpenHashMap.newInstance();
            m2.put(1, Float.NaN);
            Assert.assertEquals(m1, m2);
        }

        {
            final FloatArrayList list = FloatArrayList.newInstance();
            Assert.assertFalse(list.contains(Float.NaN));
            list.add(0, Float.NaN, 1);
            Assert.assertTrue(list.contains(Float.NaN));
        }

        {
            final DoubleArrayList list = DoubleArrayList.newInstance();
            Assert.assertFalse(list.contains(Double.NaN));
            list.add(0, Double.NaN, 1);
            Assert.assertTrue(list.contains(Double.NaN));
        }

        {

            final DoubleArrayList l1 = DoubleArrayList.newInstance();
            l1.add(0, Double.NaN, 1);
            final DoubleArrayList l2 = DoubleArrayList.newInstance();
            l2.add(0, Double.NaN, 1);
            Assert.assertEquals(l1, l2);
        }
    }
}
