package com.carrotsearch.hppc;

/**
 * Interface to support custom hashing strategies in Hash Maps and Hash Sets using Object
 * keys, as replacement of the original Object.equals() and Object.hashCode().
 * 
 * @param <T>
 */
public interface HashingStrategy<T> {

    /**
     * Compute the hash code for the specific object. There is no need to check
     * for null argument, since its hash code will always be considered as 0.
     * The final OpenHashMap/Set implementation is guaranteed to receive not null argument.
     * 
     * @param object
     * @return the customized hash value.
     */
     int computeHashCode(T object);

    /**
     * Compares the Object o1 and o2 for equality.
     * The final OpenHashMap/Set is guaranteed that o1 is never null.
     * @param o1
     * @param o2
     * @return true for equality.
     */
     boolean equals(T o1, T o2);
}
