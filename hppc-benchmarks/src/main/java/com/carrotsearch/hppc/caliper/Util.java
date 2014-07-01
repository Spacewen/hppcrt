package com.carrotsearch.hppc.caliper;

import java.util.Random;

public class Util
{
    /**
     * Prepare pseudo-random data from a fixed seed.
     */
    public static int[] prepareData(final int len, final Random rnd)
    {
        final int[] randomData = new int[len];
        for (int i = 0; i < len; i++)
            randomData[i] = rnd.nextInt();

        return randomData;
    }

    public static int[] shuffle(final int[] array, final Random rnd)
    {
        for (int i = array.length - 1; i > 0; i--)
        {
            final int pos = rnd.nextInt(i + 1);
            final int t = array[pos];
            array[pos] = array[i];
            array[i] = t;
        }
        return array;
    }

    /**
     * Linear increment by 2.
     */
    public static int[] prepareLinear(final int size)
    {
        final int[] t = new int[size];
        for (int i = 0; i < size; i++)
            t[i] = i * 2;
        return t;
    }

    /**
     * Linear decrement by 1.
     */
    public static int[] prepareLinearDecrement(final int size)
    {
        final int[] t = new int[size];

        for (int i = 0; i < size; i++)
        {
            t[i] = size - i;
        }
        return t;
    }

    /**
     * Linear increments on 12 high bits first, then on lower bits.
     */
    public static int[] prepareHighbits(final int size)
    {
        final int[] t = new int[size];
        for (int i = 0; i < size; i++)
            t[i] = (i << (32 - 12)) | (i >>> 12);
        return t;
    }
}
