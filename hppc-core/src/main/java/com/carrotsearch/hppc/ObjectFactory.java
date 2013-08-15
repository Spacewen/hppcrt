package com.carrotsearch.hppc;

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
     * when the object is borrowed from the pool. That way,
     * any object coming out of a pool is set properly
     * in a user-controlled state.
     * @param obj
     */
    void initialize(E obj);
    
}
