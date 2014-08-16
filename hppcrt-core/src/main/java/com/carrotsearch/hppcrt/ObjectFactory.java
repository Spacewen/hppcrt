package com.carrotsearch.hppcrt;

/**
 * Generic Object factory, returning new instances of objects E,
 * with the ability to reinitialize the instances of E. 
 * @param <E>
 */
public interface ObjectFactory<E> {

    /**
     * 
     * @return a new Object instance E
     */
    E create();

    /**
     * Method to initialize/re-initialize the object
     * when the object is borrowed from an {@link ObjectPool}. That way,
     * any object coming out of a pool is set properly
     * in a user-controlled state.
     * @param obj
     */
    void initialize(E obj);

    /**
     * Method to reset the object
     * when the object is released, to return to an {@link ObjectPool}. That way,
     * any object returning to a pool is properly cleaned-up
     * @param obj
     */
    void reset(E obj);
}
