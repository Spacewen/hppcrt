package com.carrotsearch.hppc;

import java.util.Arrays;

import com.carrotsearch.hppc.hash.MurmurHash3;

/**
 * Internal utilities.
 */
final class Internals
{

    final static int NB_OF_PROCESSORS = Runtime.getRuntime().availableProcessors();

    final static int BLANK_ARRAY_SIZE_IN_BIT_SHIFT = 10;

    /**
     * Batch blanking array size
     */
    final static int BLANK_ARRAY_SIZE = 1 << Internals.BLANK_ARRAY_SIZE_IN_BIT_SHIFT;

    /**
     * Batch blanking array with Object nulls
     */
    final static Object[] BLANKING_OBJECT_ARRAY = new Object[Internals.BLANK_ARRAY_SIZE];

    /**
     * Batch blanking array with boolean false
     */
    final static boolean[] BLANKING_BOOLEAN_ARRAY = new boolean[Internals.BLANK_ARRAY_SIZE];



    static int rehash(final Object o, final int p) { return o == null ? 0 : MurmurHash3.hash(o.hashCode() ^ p); }
    static int rehash(final byte v, final int p)   { return MurmurHash3.hash(v ^ p); }
    static int rehash(final short v, final int p)  { return MurmurHash3.hash(v ^ p); }
    static int rehash(final int v, final int p)    { return MurmurHash3.hash(v ^ p); }
    static int rehash(final long v, final int p)   { return (int) MurmurHash3.hash(v ^ p); }
    static int rehash(final char v, final int p)   { return MurmurHash3.hash(v ^ p); }
    static int rehash(final float v, final int p)  { return MurmurHash3.hash(Float.floatToIntBits(v) ^ p); }
    static int rehash(final double v, final int p) { return (int) MurmurHash3.hash(Double.doubleToLongBits(v) ^ p); }

    static int rehash(final Object o) { return o == null ? 0 : MurmurHash3.hash(o.hashCode()); }
    static int rehash(final byte v)   { return MurmurHash3.hash(v); }
    static int rehash(final short v)  { return MurmurHash3.hash(v); }
    static int rehash(final int v)    { return MurmurHash3.hash(v); }
    static int rehash(final long v)   { return (int) MurmurHash3.hash(v); }
    static int rehash(final char v)   { return MurmurHash3.hash(v); }
    static int rehash(final float v)  { return MurmurHash3.hash(Float.floatToIntBits(v)); }
    static int rehash(final double v) { return (int) MurmurHash3.hash(Double.doubleToLongBits(v)); }

    /**
     * Create and return an array of template objects (<code>Object</code>s in the generic
     * version, corresponding key-primitive type in the generated version).
     * 
     * @param arraySize The size of the array to return.
     */
    @SuppressWarnings("unchecked")
    static <T> T newArray(final int arraySize)
    {
        return (T) new Object [arraySize];
    }

    /**
     * if specificHash == null, equivalent to rehash()
     * @param object
     * @param p
     * @param specificHash
     * @return
     */
    static <T> int rehashSpecificHash(final T o, final int p, final HashingStrategy<? super T> specificHash)
    {
        return o == null ? 0 : (specificHash == null? MurmurHash3.hash(o.hashCode() ^ p) :(MurmurHash3.hash(specificHash.computeHashCode(o) ^ p)));
    }

    /**
     * Method to blank any Object[] array to "null"
     * from [startIndex; endIndex[, equivalent to {@link Arrays}.fill(objectArray, startIndex, endIndex, null)
     */
    static <T> void blankObjectArray(final T[] objectArray, final int startIndex, final int endIndex) {

        assert startIndex <= endIndex;

        final int size = endIndex - startIndex;
        final int nbChunks = size >> Internals.BLANK_ARRAY_SIZE_IN_BIT_SHIFT;
        //compute remainder
        final int rem = size & (Internals.BLANK_ARRAY_SIZE - 1);

        for (int i = 0 ; i < nbChunks; i++) {

            System.arraycopy(Internals.BLANKING_OBJECT_ARRAY, 0,
                    objectArray, startIndex + (i << Internals.BLANK_ARRAY_SIZE_IN_BIT_SHIFT),
                    Internals.BLANK_ARRAY_SIZE);
        } //end for

        //fill the reminder
        if (rem > 0) {
            Arrays.fill(objectArray, startIndex + (nbChunks << Internals.BLANK_ARRAY_SIZE_IN_BIT_SHIFT),
                    startIndex + (nbChunks << Internals.BLANK_ARRAY_SIZE_IN_BIT_SHIFT) + rem , null);
        }
    }

    /**
     * Method to blank any boolean[] array to false
     * from [startIndex; endIndex[, equivalent to {@link Arrays}.fill(boolArray, startIndex, endIndex, false)
     */
    static void blankBooleanArray(final boolean[] boolArray, final int startIndex, final int endIndex) {

        assert startIndex <= endIndex;

        final int size = endIndex - startIndex;
        final int nbChunks = size >> Internals.BLANK_ARRAY_SIZE_IN_BIT_SHIFT;
        //compute remainder
        final int rem = size & (Internals.BLANK_ARRAY_SIZE - 1);

        for (int i = 0 ; i < nbChunks; i++) {

            System.arraycopy(Internals.BLANKING_BOOLEAN_ARRAY, 0,
                    boolArray, startIndex + (i << Internals.BLANK_ARRAY_SIZE_IN_BIT_SHIFT),
                    Internals.BLANK_ARRAY_SIZE);
        } //end for

        //fill the reminder
        if (rem > 0) {
            Arrays.fill(boolArray, startIndex + (nbChunks << Internals.BLANK_ARRAY_SIZE_IN_BIT_SHIFT),
                    startIndex + (nbChunks << Internals.BLANK_ARRAY_SIZE_IN_BIT_SHIFT) + rem , false);
        }

    }
}
