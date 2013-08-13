package com.carrotsearch.hppc;

import java.util.Arrays;

import com.carrotsearch.hppc.hash.MurmurHash3;

/**
 * Internal utilities.
 */
final class Internals
{
    final static int BLANK_ARRAY_SIZE_IN_BIT_SHIFT = 10;
    
    /**
     * Batch blanking array size
     */
     final static int BLANK_ARRAY_SIZE = 1 << BLANK_ARRAY_SIZE_IN_BIT_SHIFT;
    
    /**
     * Batch blanking array with Object nulls
     */
     final static Object[] BLANKING_OBJECT_ARRAY = new Object[BLANK_ARRAY_SIZE];
    
    /**
     * Batch blanking array with boolean false
     */
     final static boolean[] BLANKING_BOOLEAN_ARRAY = new boolean[BLANK_ARRAY_SIZE];

    
    
    static int rehash(Object o, int p) { return o == null ? 0 : MurmurHash3.hash(o.hashCode() ^ p); }
    static int rehash(byte v, int p)   { return MurmurHash3.hash(v ^ p); }
    static int rehash(short v, int p)  { return MurmurHash3.hash(v ^ p); }
    static int rehash(int v, int p)    { return MurmurHash3.hash(v ^ p); }
    static int rehash(long v, int p)   { return (int) MurmurHash3.hash(v ^ p); }
    static int rehash(char v, int p)   { return MurmurHash3.hash(v ^ p); }
    static int rehash(float v, int p)  { return MurmurHash3.hash(Float.floatToIntBits(v) ^ p); }
    static int rehash(double v, int p) { return (int) MurmurHash3.hash(Double.doubleToLongBits(v) ^ p); }

    static int rehash(Object o) { return o == null ? 0 : MurmurHash3.hash(o.hashCode()); }
    static int rehash(byte v)   { return MurmurHash3.hash(v); }
    static int rehash(short v)  { return MurmurHash3.hash(v); }
    static int rehash(int v)    { return MurmurHash3.hash(v); }
    static int rehash(long v)   { return (int) MurmurHash3.hash(v); }
    static int rehash(char v)   { return MurmurHash3.hash(v); }
    static int rehash(float v)  { return MurmurHash3.hash(Float.floatToIntBits(v)); }
    static int rehash(double v) { return (int) MurmurHash3.hash(Double.doubleToLongBits(v)); }

    /**
     * Create and return an array of template objects (<code>Object</code>s in the generic
     * version, corresponding key-primitive type in the generated version).
     * 
     * @param arraySize The size of the array to return.
     */
    @SuppressWarnings("unchecked")
    static <T> T newArray(int arraySize)
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
    static<T> int rehashSpecificHash(T o, int p, HashingStrategy<T> specificHash) 
    { 
        return o == null ? 0 : (specificHash == null? MurmurHash3.hash(o.hashCode() ^ p) :(MurmurHash3.hash(specificHash.computeHashCode(o) ^ p))); 
    }
    
    /**
     * Method to blank any Object[] array to "null"
     * from [startIndex; endIndex[, equivalent to {@link Arrays}.fill(objectArray, startIndex, endIndex, null)
     */
    static <T> void blankObjectArray(T[] objectArray, int startIndex, int endIndex) {
        
        assert startIndex <= endIndex;
        
        final int size = endIndex - startIndex;
        final int nbChunks = size >> BLANK_ARRAY_SIZE_IN_BIT_SHIFT;
        //compute remainder
        final int rem = size & (BLANK_ARRAY_SIZE - 1);
        
        for (int i = 0 ; i < nbChunks; i++) {
            
            System.arraycopy(BLANKING_OBJECT_ARRAY, 0, 
                    objectArray, startIndex + (i << BLANK_ARRAY_SIZE_IN_BIT_SHIFT), 
                    BLANK_ARRAY_SIZE);
        } //end for
        
        //fill the reminder
        if (rem > 0) {
            Arrays.fill(objectArray, startIndex + (nbChunks << BLANK_ARRAY_SIZE_IN_BIT_SHIFT), 
                     startIndex + (nbChunks << BLANK_ARRAY_SIZE_IN_BIT_SHIFT) + rem , null);
        }
    }
    
    /**
     * Method to blank any boolean[] array to false
     * from [startIndex; endIndex[, equivalent to {@link Arrays}.fill(boolArray, startIndex, endIndex, false)
     */
    static <T> void blankBooleanArray(boolean[] boolArray, int startIndex, int endIndex) {
        
        assert startIndex <= endIndex;
        
        final int size = endIndex - startIndex;
        final int nbChunks = size >> BLANK_ARRAY_SIZE_IN_BIT_SHIFT;
        //compute remainder
        final int rem = size & (BLANK_ARRAY_SIZE - 1);
        
        for (int i = 0 ; i < nbChunks; i++) {
            
            System.arraycopy(BLANKING_BOOLEAN_ARRAY, 0, 
                    boolArray, startIndex + (i << BLANK_ARRAY_SIZE_IN_BIT_SHIFT), 
                    BLANK_ARRAY_SIZE);
        } //end for
        
        //fill the reminder
        if (rem > 0) {
            Arrays.fill(boolArray, startIndex + (nbChunks << BLANK_ARRAY_SIZE_IN_BIT_SHIFT), 
                        startIndex + (nbChunks << BLANK_ARRAY_SIZE_IN_BIT_SHIFT) + rem , false);
        }
       
    }
}
