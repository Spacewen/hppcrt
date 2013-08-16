package com.carrotsearch.hppc;

/**
 * This class represents a specialized {@link ObjectPool} of Iterators of type ITERATOR_TYPE, which are  {@link AbstractIterator}s for OBJECT_TYPE objects.
 * @param <ITERATOR_TYPE>
 * @param <OBJECT_TYPE>
 */
public class IteratorPool<OBJECT_TYPE, ITERATOR_TYPE  extends AbstractIterator<OBJECT_TYPE>> extends ObjectPool<ITERATOR_TYPE> {

    public IteratorPool(ObjectFactory<ITERATOR_TYPE> objFactory) {
        super(objFactory, Internals.NB_OF_PROCESSORS, new ArraySizingStrategy() {
            
            @Override
            public int round(int capacity) {
                // not used 
                return capacity;
            }
            
            @Override
            public int grow(int currentBufferLength, int elementsCount, int expectedAdditions) {
                // Add at most Internals.NB_OF_PROCESSORS + expected new iterator instances
                return Math.max(elementsCount, currentBufferLength)  + Internals.NB_OF_PROCESSORS + expectedAdditions;
            }
        });
    }

    /**
     * Customized borrow(), that properly 
     * attach the borrowed iterator to its corresponding pool
     * and resets it to allow iteration as if the iterator was created for the first time.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public ITERATOR_TYPE borrow() {
        
        //extract an initialized object, never null by construction
        ITERATOR_TYPE newObject =  super.borrow();
        
        //attach instance to pool
        newObject.setPool((IteratorPool) this);
        
        //properly reset state of the Iterator for a new iteration
        newObject.resetState();
        
        return newObject;
    }

}
