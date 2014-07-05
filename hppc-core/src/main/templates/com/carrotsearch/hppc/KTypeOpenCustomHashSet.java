package com.carrotsearch.hppc;

import java.util.*;

import com.carrotsearch.hppc.cursors.*;
import com.carrotsearch.hppc.hash.MurmurHash3;
import com.carrotsearch.hppc.predicates.*;
import com.carrotsearch.hppc.procedures.*;
import com.carrotsearch.hppc.strategies.*;

import static com.carrotsearch.hppc.Internals.*;

import com.carrotsearch.hppc.hash.*;

import static com.carrotsearch.hppc.HashContainerUtils.*;

/*! ${TemplateOptions.doNotGenerateKType("BOOLEAN")} !*/
/*! #set( $ROBIN_HOOD_FOR_PRIMITIVES = false) !*/
/*! #set( $ROBIN_HOOD_FOR_GENERICS = true) !*/
/*! #set( $DEBUG = false) !*/
// If RH is defined, RobinHood Hashing is in effect :
/*! #set( $RH = (($TemplateOptions.KTypeGeneric && $ROBIN_HOOD_FOR_GENERICS) || ($TemplateOptions.KTypeNumeric && $ROBIN_HOOD_FOR_PRIMITIVES)) ) !*/
/**
 * A hash set of <code>KType</code>s, implemented using using open
 * addressing with linear probing for collision resolution.
 *
 * The difference with {@link KTypeOpenHashSet} is that it uses a
 * {@link KTypeHashingStrategy} to compare objects externally instead of using
 * the built-in hashCode() /  equals(). In particular, the management of <code>null</code>
 * keys is up to the {@link KTypeHashingStrategy} implementation.
 * <p>
 * The internal buffers of this implementation ({@link #keys}), {@link #allocated})
 * are always allocated to the nearest size that is a power of two. When
 * the capacity exceeds the given load factor, the buffer size is doubled.
 * </p>
 * <p><b>Important note.</b> The implementation uses power-of-two tables and linear
 * probing, which may cause poor performance (many collisions) if hash values are
 * not properly distributed. Therefore, it is up to the {@link KTypeHashingStrategy} to
 * assure good performance.</p>
 * 
 * 
 * @author This code is inspired by the collaboration and implementation in the <a
 *         href="http://fastutil.dsi.unimi.it/">fastutil</a> project.
 * 
#if ($RH)
 *   <p> Robin-Hood hashing algorithm is also used to minimize variance
 *  in insertion and search-related operations, for an all-around smother operation at the cost
 *  of smaller peak performance:</p>
 *  <p> - Pedro Celis (1986) for the original Robin-Hood hashing paper, </p>
 *  <p> - <a href="https://github.com/moonpolysoft">MoonPolySoft/Cliff Moon</a> for the initial Robin-hood on HPPC implementation,</p>
 *  <p> - <a href="https://github.com/vsonnier" >Vincent Sonnier</a> for the present implementation using cached hashes.</p>
#end
 */
/*! ${TemplateOptions.generatedAnnotation} !*/
public class KTypeOpenCustomHashSet<KType>
extends AbstractKTypeCollection<KType>
implements KTypeLookupContainer<KType>, KTypeSet<KType>, Cloneable
{
    /**
     * Minimum capacity for the map.
     */
    public final static int MIN_CAPACITY = HashContainerUtils.MIN_CAPACITY;

    /**
     * Default capacity.
     */
    public final static int DEFAULT_CAPACITY = HashContainerUtils.DEFAULT_CAPACITY;

    /**
     * Default load factor.
     */
    public final static float DEFAULT_LOAD_FACTOR = HashContainerUtils.DEFAULT_LOAD_FACTOR;

    /**
     * Hash-indexed array holding all set entries.
     * <p>
     * Direct set iteration: iterate keys[i] for i in [0; keys.length[ where this.allocated[i] is true.
     * </p>
     * @see #allocated
     */
    public KType[] keys;

    /**
     * Information if an entry (slot) in the {@link #values} table is allocated
     * or empty.
     * #if ($RH)
     * In addition it caches hash value :  If = -1, it means not allocated, else = HASH(keys[i]) & mask
     * for every index i.
     * #end
     * @see #assigned
     */
    /*! #if ($RH) !*/
    public int[] allocated;
    /*! #else
    public boolean[] allocated;
    #end !*/

    /**
     * Cached number of assigned slots in {@link #allocated}.
     */
    protected int assigned;

    /**
     * The load factor for this map (fraction of allocated slots
     * before the buffers must be rehashed or reallocated).
     */
    protected float loadFactor;

    /**
     * Resize buffers when {@link #allocated} hits this value.
     */
    protected int resizeAt;

    /**
     * The most recent slot accessed in {@link #contains}.
     * 
     * @see #contains
     * @see #lkey
     */
    protected int lastSlot;

    /**
     * Custom hashing strategy :
     * comparisons and hash codes of keys will be computed
     * with the strategy methods instead of the native Object equals() and hashCode() methods.
     */
    protected final KTypeHashingStrategy<? super KType> hashStrategy;

    /**
     * Creates a hash set with the default capacity of {@value #DEFAULT_CAPACITY},
     * load factor of {@value #DEFAULT_LOAD_FACTOR}, using the hashStrategy as {@link KTypeHashingStrategy}
     */
    public KTypeOpenCustomHashSet(final KTypeHashingStrategy<? super KType> hashStrategy)
    {
        this(KTypeOpenCustomHashSet.DEFAULT_CAPACITY, KTypeOpenCustomHashSet.DEFAULT_LOAD_FACTOR, hashStrategy);
    }

    /**
     * Creates a hash set with the given capacity,
     * load factor of {@value #DEFAULT_LOAD_FACTOR}, using the hashStrategy as {@link KTypeHashingStrategy}
     */
    public KTypeOpenCustomHashSet(final int initialCapacity, final KTypeHashingStrategy<? super KType> hashStrategy)
    {
        this(initialCapacity, KTypeOpenCustomHashSet.DEFAULT_LOAD_FACTOR, hashStrategy);
    }

    /**
     * Creates a hash set with the given capacity and load factor, using the hashStrategy as {@link KTypeHashingStrategy}
     */
    public KTypeOpenCustomHashSet(final int initialCapacity, final float loadFactor, final KTypeHashingStrategy<? super KType> hashStrategy)
    {
        //only accept not-null strategies.
        if (hashStrategy != null)
        {
            this.hashStrategy = hashStrategy;
        }
        else {

            throw new IllegalArgumentException("KTypeOpenCustomHashSet() cannot have a null hashStrategy !");
        }

        assert loadFactor > 0 && loadFactor <= 1 : "Load factor must be between (0, 1].";

        this.loadFactor = loadFactor;

        //take into account of the load factor to garantee no reallocations before reaching  initialCapacity.
        int internalCapacity = (int) (initialCapacity / loadFactor) + KTypeOpenCustomHashSet.MIN_CAPACITY;

        //align on next power of two
        internalCapacity = HashContainerUtils.roundCapacity(internalCapacity);

        this.keys = Intrinsics.newKTypeArray(internalCapacity);

        //fill with "not allocated" value
        /*! #if ($RH) !*/
        this.allocated = new int[internalCapacity];
        Internals.blankIntArrayMinusOne(this.allocated, 0, this.allocated.length);
        /*! #else
        this.allocated = new boolean[internalCapacity];
        #end !*/

        //Take advantage of the rounding so that the resize occur a bit later than expected.
        //allocate so that there is at least one slot that remains allocated = false
        //this is compulsory to guarantee proper stop in searching loops
        this.resizeAt = Math.max(3, (int) (internalCapacity * loadFactor)) - 2;
    }

    /**
     * Creates a hash set from elements of another container. Default load factor is used.
     */
    public KTypeOpenCustomHashSet(final KTypeContainer<KType> container, final KTypeHashingStrategy<? super KType> hashStrategy)
    {
        this(container.size(), hashStrategy);
        addAll(container);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean add(KType e)
    {
        assert assigned < allocated.length;

        final int mask = allocated.length - 1;

        final KTypeHashingStrategy<? super KType> strategy = this.hashStrategy;

        int slot = Internals.rehash(strategy.computeHashCode(e)) & mask;

        final KType[] keys = this.keys;

        /*! #if ($RH) !*/
        final int[] allocated = this.allocated;
        /*! #else
        final boolean[] allocated = this.allocated;
        #end !*/

        /*! #if ($RH) !*/
        KType tmpKey;
        int tmpAllocated;
        int initial_slot = slot;
        int dist = 0;
        int existing_distance = 0;
        /*! #end !*/

        while (allocated[slot] /*! #if ($RH) !*/!= -1 /*! #end !*/)
        {
            if (strategy.equals(e, keys[slot]))
            {
                return false;
            }

            /*! #if ($RH) !*/
            //re-shuffle keys to minimize variance
            existing_distance = probe_distance(slot, allocated);

            if (dist > existing_distance)
            {
                //swap current (key, value, initial_slot) with slot places
                tmpKey = keys[slot];
                keys[slot] = e;
                e = tmpKey;

                tmpAllocated = allocated[slot];
                allocated[slot] = initial_slot;
                initial_slot = tmpAllocated;

                /*! #if($DEBUG) !*/
                //Check invariants
                assert allocated[slot] == (Internals.rehash(strategy.computeHashCode(keys[slot])) & mask);
                assert initial_slot == (Internals.rehash(strategy.computeHashCode(e)) & mask);
                /*! #end !*/

                dist = existing_distance;
            }
            /*! #end !*/

            slot = (slot + 1) & mask;
            /*! #if ($RH) !*/
            dist++;
            /*! #end !*/
        }

        // Check if we need to grow. If so, reallocate new data,
        // fill in the last element and rehash.
        if (assigned == resizeAt) {

            expandAndAdd(e, slot);
        }
        else {
            assigned++;
            /*! #if ($RH) !*/
            allocated[slot] = initial_slot;
            /*! #else
            allocated[slot] = true;
            #end !*/

            keys[slot] = e;

            /*! #if ($RH) !*/
            /*! #if($DEBUG) !*/
            //Check invariants
            assert allocated[slot] == (Internals.rehash(strategy.computeHashCode(keys[slot])) & mask);
            /*! #end !*/
            /*! #end !*/
        }
        return true;
    }

    /**
     * Adds two elements to the set.
     */
    public int add(final KType e1, final KType e2)
    {
        int count = 0;
        if (add(e1))
            count++;
        if (add(e2))
            count++;
        return count;
    }

    /**
     * Vararg-signature method for adding elements to this set.
     * <p><b>This method is handy, but costly if used in tight loops (anonymous
     * array passing)</b></p>
     * 
     * @return Returns the number of elements that were added to the set
     * (were not present in the set).
     */
    public int add(final KType... elements)
    {
        int count = 0;
        for (final KType e : elements)
            if (add(e))
                count++;
        return count;
    }

    /**
     * Adds all elements from a given container to this set.
     * 
     * @return Returns the number of elements actually added as a result of this
     * call (not previously present in the set).
     */
    public int addAll(final KTypeContainer<? extends KType> container)
    {
        return addAll((Iterable<? extends KTypeCursor<? extends KType>>) container);
    }

    /**
     * Adds all elements from a given iterable to this set.
     * 
     * @return Returns the number of elements actually added as a result of this
     * call (not previously present in the set).
     */
    public int addAll(final Iterable<? extends KTypeCursor<? extends KType>> iterable)
    {
        int count = 0;
        for (final KTypeCursor<? extends KType> cursor : iterable)
        {
            if (add(cursor.value))
                count++;
        }
        return count;
    }

    /**
     * Expand the internal storage buffers (capacity) or rehash current
     * keys and values if there are a lot of deleted slots.
     */
    private void expandAndAdd(final KType pendingKey, final int freeSlot)
    {
        assert assigned == resizeAt;

        /*! #if ($RH) !*/
        assert allocated[freeSlot] == -1;
        /*! #else
        assert !allocated[freeSlot];
         #end !*/

        // Try to allocate new buffers first. If we OOM, it'll be now without
        // leaving the data structure in an inconsistent state.
        final KType[] oldKeys = this.keys;

        /*! #if ($RH) !*/
        final int[] oldAllocated = this.allocated;
        /*! #else
        final boolean[] oldAllocated = this.allocated;
        #end !*/

        allocateBuffers(HashContainerUtils.nextCapacity(keys.length));

        // We have succeeded at allocating new data so insert the pending key/value at
        // the free slot in the old arrays before rehashing.
        lastSlot = -1;
        assigned++;

        //We don't care of the oldAllocated value, so long it means "allocated = true", since the whole set is rebuilt from scratch.
        /*! #if ($RH) !*/
        oldAllocated[freeSlot] = 1;
        /*!#else
        oldAllocated[freeSlot] = true;
        #end !*/

        oldKeys[freeSlot] = pendingKey;

        //Variables for adding
        final int mask = this.allocated.length - 1;

        final KTypeHashingStrategy<? super KType> strategy = this.hashStrategy;

        KType e = Intrinsics.<KType> defaultKTypeValue();
        //adding phase
        int slot = -1;

        final KType[] keys = this.keys;

        /*! #if ($RH) !*/
        final int[] allocated = this.allocated;
        /*! #else
        final boolean[] allocated = this.allocated;
        #end !*/

        /*! #if ($RH) !*/
        KType tmpKey = Intrinsics.<KType> defaultKTypeValue();
        int tmpAllocated = -1;
        int initial_slot = -1;
        int dist = -1;
        int existing_distance = -1;
        /*! #end !*/

        //iterate all the old arrays to add in the newly allocated buffers
        //It is important to iterate backwards to minimize the conflict chain length !
        for (int i = oldAllocated.length; --i >= 0;)
        {
            if (oldAllocated[i] /*! #if ($RH) !*/!= -1 /*! #end !*/)
            {
                e = oldKeys[i];
                slot = Internals.rehash(strategy.computeHashCode(e)) & mask;

                /*! #if ($RH) !*/
                initial_slot = slot;
                dist = 0;
                /*! #end !*/

                while (allocated[slot] /*! #if ($RH) !*/!= -1 /*! #end !*/)
                {
                    /*! #if ($RH) !*/
                    //re-shuffle keys to minimize variance
                    existing_distance = probe_distance(slot, allocated);

                    if (dist > existing_distance)
                    {
                        //swap current (key, value, initial_slot) with slot places
                        tmpKey = keys[slot];
                        keys[slot] = e;
                        e = tmpKey;

                        tmpAllocated = allocated[slot];
                        allocated[slot] = initial_slot;
                        initial_slot = tmpAllocated;

                        /*! #if($DEBUG) !*/
                        //Check invariants
                        assert allocated[slot] == (Internals.rehash(strategy.computeHashCode(keys[slot])) & mask);
                        assert initial_slot == (Internals.rehash(strategy.computeHashCode(e)) & mask);
                        /*! #end !*/

                        dist = existing_distance;
                    } //endif
                    /*! #end !*/

                    slot = (slot + 1) & mask;

                    /*! #if ($RH) !*/
                    dist++;
                    /*! #end !*/
                } //end while

                //place it at that position
                /*! #if ($RH) !*/
                allocated[slot] = initial_slot;
                /*! #else
                allocated[slot] = true;
                #end !*/

                keys[slot] = e;

                /*! #if ($RH) !*/
                /*! #if($DEBUG) !*/
                //Check invariants
                assert allocated[slot] == (Internals.rehash(strategy.computeHashCode(keys[slot])) & mask);
                /*! #end !*/
                /*! #end !*/
            }
        }
    }

    /**
     * Allocate internal buffers for a given capacity.
     * 
     * @param capacity New capacity (must be a power of two).
     */
    private void allocateBuffers(final int capacity)
    {
        final KType[] keys = Intrinsics.newKTypeArray(capacity);

        /*! #if ($RH) !*/
        final int[] allocated = new int[capacity];
        Internals.blankIntArrayMinusOne(allocated, 0, allocated.length);
        /*! #else
         final boolean[] allocated = new boolean[capacity];
        #end !*/

        this.keys = keys;
        this.allocated = allocated;

        //allocate so that there is at least one slot that remains allocated = false
        //this is compulsory to guarantee proper stop in searching loops
        this.resizeAt = Math.max(3, (int) (capacity * loadFactor)) - 2;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int removeAllOccurrences(final KType key)
    {
        return remove(key) ? 1 : 0;
    }

    /**
     * An alias for the (preferred) {@link #removeAllOccurrences}.
     */
    public boolean remove(final KType key)
    {
        final int mask = allocated.length - 1;

        final KTypeHashingStrategy<? super KType> strategy = this.hashStrategy;

        int slot = Internals.rehash(strategy.computeHashCode(key)) & mask;

        /*! #if ($RH) !*/
        int dist = 0;
        /*! #end !*/

        final KType[] keys = this.keys;

        /*! #if ($RH) !*/
        final int[] states = this.allocated;
        /*! #else
        final boolean[] states = this.allocated;
        #end !*/

        while (states[slot] /*! #if ($RH) !*/!= -1 /*! #end !*/
                /*! #if ($RH) !*/&& dist <= probe_distance(slot, states) /*! #end !*/)
        {
            if (strategy.equals(key, keys[slot]))
            {
                this.assigned--;
                shiftConflictingKeys(slot);
                return true;
            }
            slot = (slot + 1) & mask;

            /*! #if ($RH) !*/
            dist++;
            /*! #end !*/
        } //end while true

        return false;
    }

    /**
     * Shift all the slot-conflicting keys allocated to (and including) <code>slot</code>.
     */
    protected void shiftConflictingKeys(int slotCurr)
    {
        // Copied nearly verbatim from fastutil's impl.
        final int mask = allocated.length - 1;
        int slotPrev, slotOther;

        final KTypeHashingStrategy<? super KType> strategy = this.hashStrategy;

        final KType[] keys = this.keys;
        /*! #if ($RH) !*/
        final int[] allocated = this.allocated;
        /*! #else
         final boolean[] allocated = this.allocated;
        #end !*/

        while (true)
        {
            slotCurr = ((slotPrev = slotCurr) + 1) & mask;

            while (allocated[slotCurr] /*! #if ($RH) !*/!= -1 /*! #end !*/)
            {
                slotOther = Internals.rehash(strategy.computeHashCode(keys[slotCurr])) & mask;

                if (slotPrev <= slotCurr)
                {
                    // We are on the right of the original slot.
                    if (slotPrev >= slotOther || slotOther > slotCurr)
                        break;
                }
                else
                {
                    // We have wrapped around.
                    if (slotPrev >= slotOther && slotOther > slotCurr)
                        break;
                }
                slotCurr = (slotCurr + 1) & mask;
            }

            if (/*! #if ($RH) !*/
                    allocated[slotCurr] == -1
                    /*! #else
            !allocated[slotCurr]
            #end !*/)
            {
                break;
            }

            /*! #if ($RH) !*/
            /*! #if($DEBUG) !*/
            //Check invariants
            assert allocated[slotCurr] == (Internals.rehash(strategy.computeHashCode(keys[slotCurr])) & mask);
            assert allocated[slotPrev] == (Internals.rehash(strategy.computeHashCode(keys[slotPrev])) & mask);
            /*! #end !*/
            /*! #end !*/

            // Shift key/allocated pair.
            keys[slotPrev] = keys[slotCurr];

            /*! #if ($RH) !*/
            allocated[slotPrev] = allocated[slotCurr];
            /*! #end !*/
        }

        //means not allocated
        /*! #if ($RH) !*/
        allocated[slotPrev] = -1;
        /*! #else
         allocated[slotPrev] = false;
        #end !*/

        /* #if ($TemplateOptions.KTypeGeneric) */
        keys[slotPrev] = Intrinsics.<KType> defaultKTypeValue();
        /* #end */
    }

    /**
     * Returns the last key saved in a call to {@link #contains} if it returned <code>true</code>.
     * Precondition : {@link #contains} must have been called previously !
     * @see #contains
     */
    public KType lkey()
    {
        assert lastSlot >= 0 : "Call contains() first.";

        /*! #if ($RH) !*/
        assert allocated[lastSlot] != -1 : "Last call to exists did not have any associated value.";
        /*! #else
         assert allocated[lastSlot] : "Last call to exists did not have any associated value.";
        #end !*/

        return keys[lastSlot];
    }

    /**
     * @return Returns the slot of the last key looked up in a call to {@link #contains} if
     * it returned <code>true</code>.
     * Precondition : {@link #contains} must have been called previously !
     * @see #contains
     */
    public int lslot()
    {
        assert lastSlot >= 0 : "Call contains() first.";
        return lastSlot;
    }

    /**
     * {@inheritDoc}
     * 
     * #if ($TemplateOptions.KTypeGeneric) <p>Saves the associated value for fast access using {@link #lkey()}.</p>
     * <pre>
     * if (map.contains(key))
     *     value = map.lkey();
     * 
     * </pre> #end
     */
    @Override
    public boolean contains(final KType key)
    {
        final int mask = allocated.length - 1;

        final KTypeHashingStrategy<? super KType> strategy = this.hashStrategy;

        int slot = Internals.rehash(strategy.computeHashCode(key)) & mask;

        /*! #if ($RH) !*/
        int dist = 0;
        /*! #end !*/

        final KType[] keys = this.keys;

        /*! #if ($RH) !*/
        final int[] states = this.allocated;
        /*! #else
        final boolean[] states = this.allocated;
        #end !*/

        while (states[slot] /*! #if ($RH) !*/!= -1 /*! #end !*/
                /*! #if ($RH) !*/&& dist <= probe_distance(slot, states) /*! #end !*/)
        {
            if (strategy.equals(key, keys[slot]))
            {
                this.lastSlot = slot;
                return true;
            }
            slot = (slot + 1) & mask;

            /*! #if ($RH) !*/
            dist++;
            /*! #end !*/
        } //end while true

        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Does not release internal buffers.</p>
     */
    @Override
    public void clear()
    {
        this.assigned = 0;
        this.lastSlot = -1;

        // States are always cleared.
        /*! #if ($RH) !*/
        Internals.blankIntArrayMinusOne(allocated, 0, allocated.length);
        /*! #else
         Internals.blankBooleanArray(allocated, 0, allocated.length);
        #end !*/

        /*! #if ($TemplateOptions.KTypeGeneric) !*/
        //Faster than Arrays.fill(keys, null); // Help the GC.
        Internals.blankObjectArray(keys, 0, keys.length);
        /*! #end !*/
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int size()
    {
        return assigned;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int capacity() {

        return resizeAt - 1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        int h = 0;

        final KType[] keys = this.keys;

        /*! #if ($RH) !*/
        final int[] states = this.allocated;
        /*! #else
        final boolean[] states = this.allocated;
        #end !*/

        final KTypeHashingStrategy<? super KType> strategy = this.hashStrategy;

        for (int i = states.length; --i >= 0;)
        {
            if (states[i]/*! #if ($RH) !*/!= -1 /*! #end !*/)
            {
                //This hash is an intrinsic property of the container contents,
                //consequently is independent from the KTypeHashStrategy, so do not use it !
                h += Internals.rehash(strategy.computeHashCode(keys[i]));
            }
        }

        return h;
    }

    /**
     * this instance and obj can only be equal if : <pre>
     * (both are  KTypeOpenCustomHashSet)
     * and
     * (both have equal hash strategies defined by {@link #KTypeHashingStrategy}.equals(obj.hashStrategy))</pre>
     * then, both sets are compared as follows, using their {@link #KTypeHashingStrategy}.
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(final Object obj)
    {
        if (obj != null)
        {
            if (obj == this)
                return true;

            if (!(obj instanceof KTypeOpenCustomHashSet)) {

                return false;
            }

            if (!this.hashStrategy.equals(((KTypeOpenCustomHashSet<KType>) obj).hashStrategy)) {

                return false;
            }

            final KTypeOpenCustomHashSet<KType> other = (KTypeOpenCustomHashSet<KType>) obj;

            if (other.size() == this.size())
            {
                final EntryIterator it = this.iterator();

                while (it.hasNext())
                {
                    if (!other.contains(it.next().value))
                    {
                        //recycle
                        it.release();
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    /**
     * An iterator implementation for {@link #iterator}.
     */
    public final class EntryIterator extends AbstractIterator<KTypeCursor<KType>>
    {
        public final KTypeCursor<KType> cursor;

        public EntryIterator()
        {
            cursor = new KTypeCursor<KType>();
            cursor.index = -2;
        }

        /**
         * Iterate backwards w.r.t the buffer, to
         * minimize collision chains when filling another hash container (ex. with putAll())
         */
        @Override
        protected KTypeCursor<KType> fetch()
        {
            int i = cursor.index - 1;

            while (i >= 0 &&
                    /*! #if ($RH) !*/
                    allocated[i] == -1
                    /*! #else
            !allocated[i]
            #end  !*/)
            {
                i--;
            }

            if (i == -1)
                return done();

            cursor.index = i;
            cursor.value = keys[i];
            return cursor;
        }
    }

    /**
     * internal pool of EntryIterator
     */
    protected final IteratorPool<KTypeCursor<KType>, EntryIterator> entryIteratorPool = new IteratorPool<KTypeCursor<KType>, EntryIterator>(
            new ObjectFactory<EntryIterator>() {

                @Override
                public EntryIterator create() {

                    return new EntryIterator();
                }

                @Override
                public void initialize(final EntryIterator obj) {
                    obj.cursor.index = keys.length;
                }

                @Override
                public void reset(final EntryIterator obj) {
                    // nothing
                }
            });

    /**
     * {@inheritDoc}
     * @return
     */
    @Override
    public EntryIterator iterator()
    {
        //return new EntryIterator();
        return this.entryIteratorPool.borrow();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends KTypeProcedure<? super KType>> T forEach(final T procedure)
    {
        final KType[] keys = this.keys;

        /*! #if ($RH) !*/
        final int[] states = this.allocated;
        /*! #else
            final boolean[] states = this.allocated;
            #end !*/

        for (int i = 0; i < states.length; i++)
        {
            if (states[i] /*! #if ($RH) !*/!= -1 /*! #end !*/)
                procedure.apply(keys[i]);
        }

        return procedure;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public KType[] toArray(final KType[] target)
    {
        final KType[] keys = this.keys;

        /*! #if ($RH) !*/
        final int[] states = this.allocated;
        /*! #else
            final boolean[] states = this.allocated;
            #end !*/

        for (int i = 0, j = 0; i < keys.length; i++) {

            if (states[i] /*! #if ($RH) !*/!= -1 /*! #end !*/)
            {
                target[j++] = keys[i];
            }
        }

        return target;
    }

    /**
     * Clone this object.
     * #if ($TemplateOptions.KTypeGeneric)
     * The returned clone will use the same HashingStrategy strategy.
     * It also realizes a trim-to- this.size() in the process.
     * #end
     */
    @Override
    public KTypeOpenCustomHashSet<KType> clone()
    {
        final KTypeOpenCustomHashSet<KType> cloned = new KTypeOpenCustomHashSet<KType>(this.size(), this.loadFactor, this.hashStrategy);

        cloned.addAll(this);

        cloned.defaultValue = this.defaultValue;

        return cloned;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends KTypePredicate<? super KType>> T forEach(final T predicate)
    {
        final KType[] keys = this.keys;

        /*! #if ($RH) !*/
        final int[] states = this.allocated;
        /*! #else
            final boolean[] states = this.allocated;
            #end !*/

        for (int i = 0; i < states.length; i++)
        {
            if (states[i]/*! #if ($RH) !*/!= -1 /*! #end !*/)
            {
                if (!predicate.apply(keys[i]))
                    break;
            }
        }

        return predicate;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int removeAll(final KTypePredicate<? super KType> predicate)
    {
        final KType[] keys = this.keys;

        /*! #if ($RH) !*/
        final int[] states = this.allocated;
        /*! #else
            final boolean[] states = this.allocated;
            #end !*/

        final int before = assigned;

        for (int i = 0; i < states.length;)
        {
            if (states[i] /*! #if ($RH) !*/!= -1 /*! #end !*/)
            {
                if (predicate.apply(keys[i]))
                {
                    this.assigned--;
                    shiftConflictingKeys(i);
                    // Repeat the check for the same i.
                    continue;
                }
            }
            i++;
        }

        return before - this.assigned;
    }

    /**
     * Create a set from a variable number of arguments or an array of <code>KType</code>.
     */
    public static <KType> KTypeOpenCustomHashSet<KType> from(final KTypeHashingStrategy<? super KType> hashStrategy, final KType... elements)
    {
        final KTypeOpenCustomHashSet<KType> set = new KTypeOpenCustomHashSet<KType>(elements.length, hashStrategy);
        set.add(elements);
        return set;
    }

    /**
     * Create a set from elements of another container.
     */
    public static <KType> KTypeOpenCustomHashSet<KType> from(final KTypeContainer<KType> container, final KTypeHashingStrategy<? super KType> hashStrategy)
    {
        return new KTypeOpenCustomHashSet<KType>(container, hashStrategy);
    }

    /**
     * Create a new hash set with default parameters (shortcut
     * instead of using a constructor).
     */
    public static <KType> KTypeOpenCustomHashSet<KType> newInstance(final KTypeHashingStrategy<? super KType> hashStrategy)
    {
        return new KTypeOpenCustomHashSet<KType>(hashStrategy);
    }

    /**
     * Returns a new object of this class with no need to declare generic type (shortcut
     * instead of using a constructor).
     */
    public static <KType> KTypeOpenCustomHashSet<KType> newInstanceWithCapacity(final int initialCapacity, final float loadFactor, final KTypeHashingStrategy<? super KType> hashStrategy)
    {
        return new KTypeOpenCustomHashSet<KType>(initialCapacity, loadFactor, hashStrategy);
    }

    /**
     * Return the current {@link HashingStrategy} in use.
     * @return
     */
    public KTypeHashingStrategy<? super KType> strategy()
    {
        return this.hashStrategy;
    }

/*! #if ($TemplateOptions.inline("probe_distance",
    "(slot, alloc)",
    "slot < alloc[slot] ? slot + alloc.length - alloc[slot] : slot - alloc[slot]")) !*/
    /**
     * Resulting code in inlined in generated code
     */
    private int probe_distance(final int slot, final int[] alloc) {

        final int rh = alloc[slot];

        /*! #if($DEBUG) !*/
        //Check : cached hashed slot is == computed value
        final int mask = alloc.length - 1;
        assert rh == (Internals.rehash(this.hashStrategy.computeHashCode(this.keys[slot])) & mask);
        /*! #end !*/

        if (slot < rh) {
            //wrap around
            return slot + alloc.length - rh;
        }

        return slot - rh;
    }
/*! #end !*/

}
