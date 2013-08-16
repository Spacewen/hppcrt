package com.carrotsearch.hppc;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Simplifies the implementation of iterators a bit. Modeled loosely
 * after Google Guava's API.
 */
abstract class AbstractIterator<E> implements Iterator<E>
{
    private final static int NOT_CACHED = 0;
    private final static int CACHED = 1;
    private final static int AT_END = 2;

    /** Current iterator state. */
    private int state = NOT_CACHED;

    /** 
     * The next element to be returned from {@link #next()} if 
     * fetched. 
     */
    private E nextElement;
    
    
    /**
     * The {@link IteratorPool} the iterator comes from, if any. (if != null).
     */
    private IteratorPool<E, AbstractIterator<E>> iteratorPool = null;
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasNext()
    {
        if (state == NOT_CACHED)
        {
            state = CACHED;
            nextElement = fetch();
        }
          
        //if there is an attached pool, auto-release this object when there is no element left.
        //this is especially useful in case of the for-each construct, which release 
        //the hidden iterator automatically when exiting the fully iterated for-each.
        if (state == AT_END && this.iteratorPool != null) {
            
            this.iteratorPool.release(this);
        }
        
        return (state == CACHED);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public E next()
    {
        if (!hasNext())
            throw new NoSuchElementException();

        state = NOT_CACHED;
        return nextElement;
    }
    
    /**
     * Default implementation throws {@link UnsupportedOperationException}.
     */
    @Override
    public void remove()
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Fetch next element. The implementation must
     * return {@link #done()} when all elements have been 
     * fetched.
     */
    protected abstract E fetch();
    
    /**
     * Call when done.
     */
    protected final E done()
    {
        state = AT_END;
        return null;
    }
    
    /**
     * Associate the pool the iterator instance came from.
     */
    protected final void setPool(IteratorPool<E, AbstractIterator<E>> pool) {
        
        assert pool != null;
        
        this.iteratorPool = pool;
    }
    
    /**
     * reset state of the Iterator, so it is ready to iterate
     * again, just as in a new creation.
     */
    protected final void resetState() {
        
        state = NOT_CACHED;
    }
    
    /**
     * Returns the iterator back to its associated pool, if any.
     * This method must be called if the iterator has not yet been automatically
     * recycled, in case of:
     * <pre>
     * Iterator obtained by explicit {@link Iterator}.iterator() or any other factory-like interface, 
     * so it needs to be returned to its pool explicitly in a symmetrical manner.</pre>
     * Of course, using the iterator after it has been released is a logical error,
     * since such object is supposed to be "freed".  
     */
    public final void release() {
        
       if (this.iteratorPool != null) {
           
           this.iteratorPool.release(this);
       }  
    }
    
    /**
     * returns the pool associated with this current instance of iterator, if any. (!= null)
     * @return
     */
    public final  IteratorPool<E, AbstractIterator<E>> getPool() {
        
        return this.iteratorPool;
    }
    
}
