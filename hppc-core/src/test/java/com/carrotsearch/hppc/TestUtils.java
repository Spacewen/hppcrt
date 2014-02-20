package com.carrotsearch.hppc;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Assert;

import com.carrotsearch.hppc.hash.MurmurHash3;

/**
 * Test utilities.
 */
public abstract class TestUtils
{
    private final static float delta = 0;

    // no instances.
    private TestUtils() {}

    /**
     * Reverse the order of elements in an array. Returns the <code>array</code> argument
     * for easier chaining.
     */
    public static Object [] reverse(final Object [] array)
    {
        for (int i = 0, mid = array.length / 2, j = array.length - 1; i < mid; i++, j--)
        {
            final Object t = array[i];
            array[i] = array[j];
            array[j] = t;
        }
        return array;
    }

    /**
     * Reverse the order of elements in an array. Returns the <code>array</code> argument
     * for easier chaining.
     */
    public static byte [] reverse(final byte [] array)
    {
        for (int i = 0, mid = array.length / 2, j = array.length - 1; i < mid; i++, j--)
        {
            final byte t = array[i];
            array[i] = array[j];
            array[j] = t;
        }
        return array;
    }

    /**
     * Reverse the order of elements in an array. Returns the <code>array</code> argument
     * for easier chaining.
     */
    public static char [] reverse(final char [] array)
    {
        for (int i = 0, mid = array.length / 2, j = array.length - 1; i < mid; i++, j--)
        {
            final char t = array[i];
            array[i] = array[j];
            array[j] = t;
        }
        return array;
    }

    /**
     * Reverse the order of elements in an array. Returns the <code>array</code> argument
     * for easier chaining.
     */
    public static short [] reverse(final short [] array)
    {
        for (int i = 0, mid = array.length / 2, j = array.length - 1; i < mid; i++, j--)
        {
            final short t = array[i];
            array[i] = array[j];
            array[j] = t;
        }
        return array;
    }

    /**
     * Reverse the order of elements in an array. Returns the <code>array</code> argument
     * for easier chaining.
     */
    public static int [] reverse(final int [] array)
    {
        for (int i = 0, mid = array.length / 2, j = array.length - 1; i < mid; i++, j--)
        {
            final int t = array[i];
            array[i] = array[j];
            array[j] = t;
        }
        return array;
    }

    /**
     * Reverse the order of elements in an array. Returns the <code>array</code> argument
     * for easier chaining.
     */
    public static float [] reverse(final float [] array)
    {
        for (int i = 0, mid = array.length / 2, j = array.length - 1; i < mid; i++, j--)
        {
            final float t = array[i];
            array[i] = array[j];
            array[j] = t;
        }
        return array;
    }

    /**
     * Reverse the order of elements in an array. Returns the <code>array</code> argument
     * for easier chaining.
     */
    public static double [] reverse(final double [] array)
    {
        for (int i = 0, mid = array.length / 2, j = array.length - 1; i < mid; i++, j--)
        {
            final double t = array[i];
            array[i] = array[j];
            array[j] = t;
        }
        return array;
    }

    /**
     * Reverse the order of elements in an array. Returns the <code>array</code> argument
     * for easier chaining.
     */
    public static long [] reverse(final long [] array)
    {
        for (int i = 0, mid = array.length / 2, j = array.length - 1; i < mid; i++, j--)
        {
            final long t = array[i];
            array[i] = array[j];
            array[j] = t;
        }
        return array;
    }

    /**
     * Check if the array's content is identical to a given sequence of elements.
     */
    public static void assertListEquals(final Object [] array, final Object... elements)
    {
        Assert.assertEquals(elements.length, array.length);
        Assert.assertArrayEquals(elements, array);
    }

    /**
     * Check if the array's content is identical to a given sequence of elements.
     */
    public static void assertListEquals(final double [] array, final double... elements)
    {
        Assert.assertEquals(elements.length, array.length);
        Assert.assertArrayEquals(elements, array, TestUtils.delta);
    }

    /**
     * Check if the array's content is identical to a given sequence of elements.
     */
    public static void assertListEquals(final float [] array, final float... elements)
    {
        Assert.assertEquals(elements.length, array.length);
        Assert.assertArrayEquals(elements, array, TestUtils.delta);
    }

    /**
     * Check if the array's content is identical to a given sequence of elements.
     */
    public static void assertListEquals(final int [] array, final int... elements)
    {
        Assert.assertEquals(elements.length, array.length);
        Assert.assertArrayEquals(elements, array);
    }

    /**
     * Check if the array's content is identical to a given sequence of elements.
     */
    public static void assertListEquals(final long [] array, final long... elements)
    {
        Assert.assertEquals(elements.length, array.length);
        Assert.assertArrayEquals(elements, array);
    }

    /**
     * Check if the array's content is identical to a given sequence of elements.
     */
    public static void assertListEquals(final short [] array, final int... elements)
    {
        Assert.assertEquals(elements.length, array.length);
        Assert.assertArrayEquals(TestUtils.newArray(array, elements), array);
    }

    /**
     * Check if the array's content is identical to a given sequence of elements.
     */
    public static void assertListEquals(final short [] array, final short... elements)
    {
        Assert.assertArrayEquals(elements, array);
    }

    /**
     * Check if the array's content is identical to a given sequence of elements.
     */
    public static void assertListEquals(final byte [] array, final int... elements)
    {
        Assert.assertEquals(elements.length, array.length);
        Assert.assertArrayEquals(TestUtils.newArray(array, elements), array);
    }

    /**
     * Check if the array's content is identical to a given sequence of elements.
     */
    public static void assertListEquals(final byte [] array, final byte... elements)
    {
        Assert.assertArrayEquals(elements, array);
    }

    /**
     * Check if the array's content is identical to a given sequence of elements.
     */
    public static void assertListEquals(final char [] array, final int... elements)
    {
        Assert.assertEquals(elements.length, array.length);
        Assert.assertArrayEquals(TestUtils.newArray(array, elements), array);
    }

    /**
     * Check if the array's content is identical to a given sequence of elements.
     */
    public static void assertListEquals(final char [] array, final char... elements)
    {
        Assert.assertArrayEquals(elements, array);
    }

    /**
     * Check if the array's content is identical to a given sequence of elements.
     */
    public static void assertSortedListEquals(final Object [] array, final Object... elements)
    {
        Assert.assertEquals(elements.length, array.length);
        Arrays.sort(array);
        Assert.assertArrayEquals(elements, array);
    }

    /**
     * Check if the array's content is identical to a given sequence of elements
     */
    public static void assertSortedListEquals(final double [] array, final double... elements)
    {
        Assert.assertEquals(elements.length, array.length);
        Arrays.sort(array);
        Assert.assertArrayEquals(elements, array, TestUtils.delta);
    }

    /**
     * Check if the array's content is identical to a given sequence of elements.
     */
    public static void assertSortedListEquals(final float [] array, final float... elements)
    {
        Assert.assertEquals(elements.length, array.length);
        Arrays.sort(array);
        Assert.assertArrayEquals(elements, array, TestUtils.delta);
    }

    /**
     * Check if the array's content is identical to a given sequence of elements.
     */
    public static void assertSortedListEquals(final int [] array, final int... elements)
    {
        Assert.assertEquals(elements.length, array.length);
        Arrays.sort(array);
        Arrays.sort(elements);
        Assert.assertArrayEquals(elements, array);
    }

    /**
     * Check if the array's content is identical to a given sequence of elements.
     */
    public static void assertSortedListEquals(final long [] array, final long... elements)
    {
        Assert.assertEquals(elements.length, array.length);
        Arrays.sort(array);
        Assert.assertArrayEquals(elements, array);
    }

    /**
     * Check if the array's content is identical to a given sequence of elements.
     */
    public static void assertSortedListEquals(final short [] array, final int... elements)
    {
        Assert.assertEquals(elements.length, array.length);
        Arrays.sort(array);
        Assert.assertArrayEquals(TestUtils.newArray(array, elements), array);
    }

    /**
     * Check if the array's content is identical to a given sequence of elements.
     */
    public static void assertSortedListEquals(final byte [] array, final int... elements)
    {
        Assert.assertEquals(elements.length, array.length);
        Arrays.sort(array);
        Assert.assertArrayEquals(TestUtils.newArray(array, elements), array);
    }

    /**
     * Check if the array's content is identical to a given sequence of elements.
     */
    public static void assertSortedListEquals(final char [] array, final int... elements)
    {
        Assert.assertEquals(elements.length, array.length);
        Arrays.sort(array);
        Assert.assertArrayEquals(TestUtils.newArray(array, elements), array);
    }

    /**
     * Create a new array of a given type and copy the arguments to this array.
     */
    public static <T> T [] newArray(final T [] arrayType, final T... elements)
    {
        return elements;
    }

    /**
     * Create a new array of ints.
     */
    public static int [] newArray(final int [] arrayType, final int... elements)
    {
        return elements;
    }

    /**
     * Create a new array of doubles.
     */
    public static double [] newArray(final double [] arrayType, final double... elements)
    {
        return elements;
    }

    /**
     * Create a new array of float.
     */
    public static float [] newArray(final float [] arrayType, final float... elements)
    {
        return elements;
    }

    /**
     * Create a new array of longs.
     */
    public static long [] newArray(final long [] arrayType, final long... elements)
    {
        return elements;
    }

    /**
     * Create a new array of shorts.
     */
    public static short [] newArray(final short [] arrayType, final int... elements)
    {
        final short [] result = new short [elements.length];
        for (int i = 0; i < elements.length; i++)
        {
            org.junit.Assert.assertTrue(
                    elements[i] >= Short.MIN_VALUE && elements[i] <= Short.MAX_VALUE);
            result[i] = (short) elements[i];
        }
        return result;
    }

    /**
     * Create a new array of chars.
     */
    public static char [] newArray(final char [] arrayType, final int... elements)
    {
        final char [] result = new char [elements.length];
        for (int i = 0; i < elements.length; i++)
        {
            org.junit.Assert.assertTrue(
                    elements[i] >= Character.MIN_VALUE && elements[i] <= Character.MAX_VALUE);
            result[i] = (char) elements[i];
        }
        return result;
    }

    /**
     * Create a new array of bytes.
     */
    public static byte [] newArray(final byte [] arrayType, final int... elements)
    {
        final byte [] result = new byte [elements.length];
        for (int i = 0; i < elements.length; i++)
        {
            org.junit.Assert.assertTrue(
                    elements[i] >= Byte.MIN_VALUE && elements[i] <= Byte.MAX_VALUE);
            result[i] = (byte) elements[i];
        }
        return result;
    }

    /** Override for generated templates. */
    public static void assertEquals2(final double a, final double b)
    {
        org.junit.Assert.assertEquals(a, b, TestUtils.delta);
    }

    public static void assertEquals2(final int a, final int b)
    {
        org.junit.Assert.assertEquals(a, b);
    }

    public static void assertEquals2(final char a, final char b)
    {
        org.junit.Assert.assertEquals(a, b);
    }

    public static void assertEquals2(final short a, final short b)
    {
        org.junit.Assert.assertEquals(a, b);
    }

    public static void assertEquals2(final byte a, final byte b)
    {
        org.junit.Assert.assertEquals(a, b);
    }

    /** Override for generated templates. */
    public static void assertEquals2(final float a, final float b)
    {
        org.junit.Assert.assertEquals(a, b, TestUtils.delta);
    }

    /** Override for generated templates. */
    public static void assertEquals2(final Object a, final Object b)
    {
        org.junit.Assert.assertEquals(a, b);
    }

    /**
     * Generate a sequence of numbers with the same lower bits of their
     * hash (MurmurHash3).
     */
    public static IntArrayList generateMurmurHash3CollisionChain(final int mask,
            final int maskedSeed, final int values)
    {
        final IntArrayList hashChain = new IntArrayList();
        for (int i = 1; i != 0; i++)
        {
            final int hash = MurmurHash3.hash(i) & mask;

            if (hash == maskedSeed)
            {
                hashChain.add(i);
                if (hashChain.size() > values)
                    break;
            }
        }

        return hashChain;
    }
}
