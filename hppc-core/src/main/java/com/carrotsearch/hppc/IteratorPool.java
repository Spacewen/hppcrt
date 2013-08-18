package com.carrotsearch.hppc;

/**
 * This class represents a specialized {@link ObjectPool} of Iterators of type ITERATOR_TYPE, which are  {@link AbstractIterator}s for OBJECT_TYPE objects.
 * @param <ITERATOR_TYPE>
 * @param <OBJECT_TYPE>
 */
public class IteratorPool<OBJECT_TYPE, ITERATOR_TYPE  extends AbstractIterator<OBJECT_TYPE>> extends ObjectPool<ITERATOR_TYPE> {

    /**
     * An Iterator pool never get bigger than MAX_SIZE_GROWTH_FACTOR
     * times its original size.
     */
    public static final int MAX_SIZE_GROWTH_FACTOR = 4;
    public static final int LINEAR_GROWTH_SIZE = Internals.NB_OF_PROCESSORS;

    public static final int MAX_SIZE = MAX_SIZE_GROWTH_FACTOR * LINEAR_GROWTH_SIZE;

    public static final int DISCARDING_SIZE = LINEAR_GROWTH_SIZE;

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
                int newSize = Math.max(elementsCount, currentBufferLength) + LINEAR_GROWTH_SIZE + expectedAdditions;

                if (newSize > MAX_SIZE)
                {
                    //discard NB_OF_PROCESSORS objects
                    newSize = -DISCARDING_SIZE;
                }

                return newSize ;
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

        //flag this iterator as borrowed, now
        newObject.setBorrowed();

        return newObject;
    }

}
