package com.carrotsearch.hppc;

/**
 * This class represents a specialized {@link ObjectPool} of Iterators of type ITERATOR_TYPE, which are  {@link AbstractIterator}s for OBJECT_TYPE objects.
 * All Iterator pools have the same allocation scheme :
 * <pre>
 * - Initial size is given either by global {@link #configureInitialPoolSize()} call,
 *   by system property "HPPC_ITERATOR_POOLSIZE",
 *   or by default, the number of processors.
 * - Grows by chunks of initial size,
 * - When its size gets bigger than MAX_SIZE_GROWTH_FACTOR * initial size,
 *   capacity remains constant while 'initial size'  old objects are disposed of.
 * @param <ITERATOR_TYPE>
 * @param <OBJECT_TYPE>
 */
public final class IteratorPool<OBJECT_TYPE, ITERATOR_TYPE extends AbstractIterator<OBJECT_TYPE>> extends ObjectPool<ITERATOR_TYPE>
{
    /**
     * An Iterator pool never get bigger than MAX_SIZE_GROWTH_FACTOR
     * times its original size.
     */
    public static final int MAX_SIZE_GROWTH_FACTOR = 4;
    public static final String POOL_INITIAL_SIZE_PROPERTY = "HPPC_ITERATOR_POOLSIZE";

    private static int INITIAL_SIZE;

    static
    {
        try
        {
            //read a configurable poperty to limit the number of preallocated iterators per pool.
            INITIAL_SIZE = Integer.parseInt(System.getProperty(POOL_INITIAL_SIZE_PROPERTY));

            if (INITIAL_SIZE < 1)
            {
                INITIAL_SIZE = 1;
            }
        }
        catch (Exception e)
        {
            INITIAL_SIZE = Internals.NB_OF_PROCESSORS;
        }
    } //end static initializer

    private static int LINEAR_GROWTH_SIZE = INITIAL_SIZE;
    private static int MAX_SIZE = MAX_SIZE_GROWTH_FACTOR * LINEAR_GROWTH_SIZE;
    private static int DISCARDING_SIZE = LINEAR_GROWTH_SIZE;

    public IteratorPool(ObjectFactory<ITERATOR_TYPE> objFactory) {
        super(objFactory, INITIAL_SIZE, new ArraySizingStrategy() {

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
                    newSize = -1 * DISCARDING_SIZE;
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

    /**
     * Configure the initial pool size for all iterators, for all containers.
     * This is primarily destined to be used at application startup.
     * However, calling it at runtime on new and existing containers results
     * a smooth capacity adaptation.
     * @param initialSize
     */
    public static final void configureInitialPoolSize(int initialSize)
    {
        INITIAL_SIZE = initialSize;
        LINEAR_GROWTH_SIZE = INITIAL_SIZE;
        DISCARDING_SIZE = INITIAL_SIZE;
        MAX_SIZE = MAX_SIZE_GROWTH_FACTOR * LINEAR_GROWTH_SIZE;
    }

    /**
     * returns the expected max size all iterator pools will be limited to.
     * @return
     */
    public static final int getMaxPoolSize()
    {
        return MAX_SIZE;
    }
}
