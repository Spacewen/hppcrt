package com.carrotsearch.hppc;

/**
 * Interface to support custom hashing strategies in maps and sets using Object
 * keys, as replacement of the original Object.equals() and Object.hashCode().
 * 
 * @param <T>
 */
public interface HashingStrategy<T> {

    /**
     * Compute the hash code for the specific object. There is no need to check
     * for null argument, since its hash code will always be considered as 0.
     * The implementation is guaranteed to receive not null argument.
     * 
     * @param object
     * @return the customized hash value.
     */
    public int computeHashCode(T object);

    /**
     * Compares the Object o1 and o2 for equality.
     * 
     * @param o1
     * @param o2
     * @return true for equality.
     */
    public boolean equals(T o1, T o2);
}
