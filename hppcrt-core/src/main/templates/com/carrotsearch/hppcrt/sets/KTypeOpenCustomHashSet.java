package com.carrotsearch.hppcrt.sets;

import com.carrotsearch.hppcrt.*;
import com.carrotsearch.hppcrt.cursors.*;
import com.carrotsearch.hppcrt.predicates.*;
import com.carrotsearch.hppcrt.procedures.*;
import com.carrotsearch.hppcrt.strategies.*;
import com.carrotsearch.hppcrt.hash.*;

/*! #import("com/carrotsearch/hppcrt/Intrinsics.java") !*/
/*! ${TemplateOptions.doNotGenerateKType("BOOLEAN")} !*/
/*! #set( $ROBIN_HOOD_FOR_ALL = true) !*/
/*! #set( $DEBUG = false) !*/
//If RH is defined, RobinHood Hashing is in effect :
/*! #set( $RH = $ROBIN_HOOD_FOR_ALL) !*/

/**
 * A hash set of <code>KType</code>s, implemented using using open
 * addressing with linear probing for collision resolution.
 *
 * The difference with {@link KTypeOpenHashSet} is that it uses a
 * {@link KTypeHashingStrategy} to compare objects externally instead of using
 * the built-in hashCode() /  equals(). In particular, the management of <code>null</code>
 * keys is up to the {@link KTypeHashingStrategy} implementation.
 * <p>
 * The internal buffers of this implementation ({@link #keys}, etc...)
 * are always allocated to the nearest size that is a power of two. When
 * the capacity exceeds the given load factor, the buffer size is doubled.
 * </p>
 * <p><b>Important note.</b> The implementation uses power-of-two tables and linear
 * probing, which may cause poor performance (many collisions) if hash values are
 * not properly distributed. Therefore, it is up to the {@link KTypeHashingStrategy} to
 * assure good performance.</p>
 * 
 *
#if ($TemplateOptions.KTypeGeneric)
 * <p>This implementation support <code>null</code> keys. In addition, objects passed to the {@link KTypeHashingStrategy} are guaranteed to be not-<code>null</code>. </p>
#end
 *
 * @author This code is inspired by the collaboration and implementation in the <a
 *         href="http://fastutil.dsi.unimi.it/">fastutil</a> project.
 * 
#if ($RH)
 *   <p> Robin-Hood hashing algorithm is also used to minimize variance
 *  in insertion and search-related operations, for an all-around smother operation at the cost
 *  of smaller peak performance:</p>
 *  <p> - Pedro Celis (1986) for the original Robin-Hood hashing paper, </p>
 *  <p> - <a href="cliff@leaninto.it">MoonPolySoft/Cliff Moon</a> for the initial Robin-hood on HPPC implementation,</p>
 *  <p> - <a href="vsonnier@gmail.com" >Vincent Sonnier</a> for the present implementation using cached hashes.</p>
#end
 *
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
    #if ($TemplateOptions.KTypeGeneric)
     * <p><strong>Important!</strong>
     * The actual value in this field is always an instance of <code>Object[]</code>.
     * Be warned that <code>javac</code> emits additional casts when <code>keys</code>
     * are directly accessed; <strong>these casts
     * may result in exceptions at runtime</strong>. A workaround is to cast directly to
     * <code>Object[]</code> before accessing the buffer's elements (although it is highly
     * recommended to use a {@link #iterator()} instead.
     * </pre>
    #end
     * <p>
     * Direct set iteration: iterate  {keys[i]} for i in [0; keys.length[ where keys[i] != 0/null, then also
     * {0/null} is in the set if {@link #allocatedDefaultKey} = true.
     * </p>
     * 
     * <p><b>Direct iteration warning: </b>
     * If the iteration goal is to fill another hash container, please iterate {@link #keys} in reverse to prevent performance losses.
     * @see #keys
     */
    public KType[] keys;

    /*! #if ($RH) !*/
    /**
     * #if ($RH)
     * Caches the hash value = HASH(keys[i]) & mask, if keys[i] != 0/null,
     * for every index i.
     * #end
     * @see #assigned
     */
    /*! #end !*/
    /*! #if ($RH) !*/
    protected int[] hash_cache;
    /*! #end !*/

    /**
     *True if key = 0/null is in the map.
     */
    public boolean allocatedDefaultKey = false;

    /**
     * Cached number of assigned slots in {@link #keys}.
     */
    protected int assigned;

    /**
     * The load factor for this map (fraction of allocated slots
     * before the buffers must be rehashed or reallocated).
     */
    protected float loadFactor;

    /**
     * Resize buffers when {@link #keys} hits this value.
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
     * Per-instance, per-allocation size perturbation
     * introduced in rehashing to create a unique key distribution.
     */
    private final int perturbation = HashContainerUtils.computeUniqueIdentifier(this);

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

        /*! #if ($RH) !*/
        this.hash_cache = new int[internalCapacity];
        /*!  #end !*/

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
        if (e == Intrinsics.defaultKTypeValue()) {

            if (this.allocatedDefaultKey) {

                return false;
            }

            this.allocatedDefaultKey = true;

            return true;
        }

        final int mask = this.keys.length - 1;

        final KTypeHashingStrategy<? super KType> strategy = this.hashStrategy;

        final KType[] keys = this.keys;

        //copied straight from  fastutil "fast-path"
        int slot;
        KType curr;

        //1.1 The rehashed key slot is occupied...
        if ((curr = keys[slot = REHASH(strategy, e) & mask]) != Intrinsics.defaultKTypeValue()) {

            //1.2 the occupied place is indeed key, return false
            if (strategy.equals(curr, e)) {

                return false;
            }

            //1.3 key is colliding, manage below :
        }
        else if (this.assigned < this.resizeAt) {

            //1.4 key is not colliding, without resize, so insert, return true.
            keys[slot] = e;

            this.assigned++;

            /*! #if ($RH) !*/
            this.hash_cache[slot] = slot;
            /*! #end !*/

            return true;
        }

        /*! #if ($RH) !*/
        final int[] cached = this.hash_cache;
        KType tmpKey;
        int tmpAllocated;
        int initial_slot = slot;
        int dist = 0;
        int existing_distance = 0;
        /*! #end !*/

        while (is_allocated(slot, keys))
        {
            if (strategy.equals(e, keys[slot]))
            {
                return false;
            }

            /*! #if ($RH) !*/
            //re-shuffle keys to minimize variance
            existing_distance = probe_distance(slot, cached);

            if (dist > existing_distance)
            {
                //swap current (key, value, initial_slot) with slot places
                tmpKey = keys[slot];
                keys[slot] = e;
                e = tmpKey;

                tmpAllocated = cached[slot];
                cached[slot] = initial_slot;
                initial_slot = tmpAllocated;

                /*! #if($DEBUG) !*/
                //Check invariants
                assert cached[slot] == (REHASH(strategy, keys[slot]) & mask);
                assert initial_slot == (REHASH(strategy, e) & mask);
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
        if (this.assigned == this.resizeAt) {

            expandAndAdd(e, slot);
        }
        else {
            this.assigned++;
            /*! #if ($RH) !*/
            cached[slot] = initial_slot;
            /*!  #end !*/

            keys[slot] = e;

            /*! #if ($RH) !*/
            /*! #if($DEBUG) !*/
            //Check invariants
            assert cached[slot] == (REHASH(strategy, keys[slot]) & mask);
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
        if (add(e1)) {
            count++;
        }
        if (add(e2)) {
            count++;
        }
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
        for (final KType e : elements) {
            if (add(e)) {
                count++;
            }
        }
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
            if (add(cursor.value)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Expand the internal storage buffers (capacity) or rehash current
     * keys and values if there are a lot of deleted slots.
     */
    private void expandAndAdd(final KType pendingKey, final int freeSlot)
    {
        assert this.assigned == this.resizeAt;

        //default sentinel value is never in the keys[] array, so never trigger reallocs
        assert (pendingKey != Intrinsics.defaultKTypeValue());

        // Try to allocate new buffers first. If we OOM, it'll be now without
        // leaving the data structure in an inconsistent state.
        final KType[] oldKeys = this.keys;

        allocateBuffers(HashContainerUtils.nextCapacity(this.keys.length));

        // We have succeeded at allocating new data so insert the pending key/value at
        // the free slot in the old arrays before rehashing.
        this.lastSlot = -1;
        this.assigned++;

        oldKeys[freeSlot] = pendingKey;

        //Variables for adding
        final int mask = this.keys.length - 1;

        final KTypeHashingStrategy<? super KType> strategy = this.hashStrategy;

        KType e = Intrinsics.<KType> defaultKTypeValue();
        //adding phase
        int slot = -1;

        final KType[] keys = this.keys;

        /*! #if ($RH) !*/
        final int[] cached = this.hash_cache;
        /*! #end !*/

        /*! #if ($RH) !*/
        KType tmpKey = Intrinsics.<KType> defaultKTypeValue();
        int tmpAllocated = -1;
        int initial_slot = -1;
        int dist = -1;
        int existing_distance = -1;
        /*! #end !*/

        //iterate all the old arrays to add in the newly allocated buffers
        //It is important to iterate backwards to minimize the conflict chain length !
        for (int i = oldKeys.length; --i >= 0;)
        {
            if (is_allocated(i, oldKeys))
            {
                e = oldKeys[i];
                slot = REHASH(strategy, e) & mask;

                /*! #if ($RH) !*/
                initial_slot = slot;
                dist = 0;
                /*! #end !*/

                while (is_allocated(slot, keys))
                {
                    /*! #if ($RH) !*/
                    //re-shuffle keys to minimize variance
                    existing_distance = probe_distance(slot, cached);

                    if (dist > existing_distance)
                    {
                        //swap current (key, value, initial_slot) with slot places
                        tmpKey = keys[slot];
                        keys[slot] = e;
                        e = tmpKey;

                        tmpAllocated = cached[slot];
                        cached[slot] = initial_slot;
                        initial_slot = tmpAllocated;

                        /*! #if($DEBUG) !*/
                        //Check invariants
                        assert cached[slot] == (REHASH(strategy, keys[slot]) & mask);
                        assert initial_slot == (REHASH(strategy, e) & mask);
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
                cached[slot] = initial_slot;
                /*! #end !*/

                keys[slot] = e;

                /*! #if ($RH) !*/
                /*! #if($DEBUG) !*/
                //Check invariants
                assert cached[slot] == (REHASH(strategy, keys[slot]) & mask);
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
        /*! #end !*/

        this.keys = keys;

        /*! #if ($RH) !*/
        this.hash_cache = allocated;
        /*! #end !*/

        //allocate so that there is at least one slot that remains allocated = false
        //this is compulsory to guarantee proper stop in searching loops
        this.resizeAt = Math.max(3, (int) (capacity * this.loadFactor)) - 2;

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
        if (key == Intrinsics.defaultKTypeValue()) {

            if (this.allocatedDefaultKey) {

                this.allocatedDefaultKey = false;
                return true;
            }

            return false;
        }

        final int mask = this.keys.length - 1;

        final KTypeHashingStrategy<? super KType> strategy = this.hashStrategy;

        final KType[] keys = this.keys;

        //copied straight from  fastutil "fast-path"
        int slot;
        KType curr;

        //1.1 The rehashed slot is free, nothing to remove, return false
        if ((curr = keys[slot = REHASH(strategy, key) & mask]) == Intrinsics.defaultKTypeValue()) {

            return false;
        }

        //1.2) The rehashed entry is occupied by the key, remove it, return true
        if (strategy.equals(curr, key)) {

            this.assigned--;
            shiftConflictingKeys(slot);
            return true;
        }

        //2. Hash collision, search for the key along the path
        slot = (slot + 1) & mask;

        /*! #if ($RH) !*/
        int dist = 0;
        final int[] cached = this.hash_cache;
        /*! #end !*/

        while (is_allocated(slot, keys)
                /*! #if ($RH) !*/&& dist <= probe_distance(slot, cached) /*! #end !*/)
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
        final int mask = this.keys.length - 1;
        int slotPrev, slotOther;

        final KTypeHashingStrategy<? super KType> strategy = this.hashStrategy;

        final KType[] keys = this.keys;

        /*! #if ($RH) !*/
        final int[] cached = this.hash_cache;
        /*!  #end !*/

        while (true)
        {
            slotCurr = ((slotPrev = slotCurr) + 1) & mask;

            while (is_allocated(slotCurr, keys))
            {
                /*! #if ($RH) !*/
                //use the cached value, no need to recompute
                slotOther = cached[slotCurr];
                /*! #if($DEBUG) !*/
                //Check invariants
                assert slotOther == (REHASH(strategy, keys[slotCurr]) & mask);
                /*! #end !*/
                /*! #else
                 slotOther = REHASH(strategy , keys[slotCurr]) & mask;
                #end !*/

                if (slotPrev <= slotCurr)
                {
                    // We are on the right of the original slot.
                    if (slotPrev >= slotOther || slotOther > slotCurr) {
                        break;
                    }
                }
                else
                {
                    // We have wrapped around.
                    if (slotPrev >= slotOther && slotOther > slotCurr) {
                        break;
                    }
                }
                slotCurr = (slotCurr + 1) & mask;
            }

            if (!is_allocated(slotCurr, keys))
            {
                break;
            }

            /*! #if ($RH) !*/
            /*! #if($DEBUG) !*/
            //Check invariants
            assert cached[slotCurr] == (REHASH(strategy, keys[slotCurr]) & mask);
            assert cached[slotPrev] == (REHASH(strategy, keys[slotPrev]) & mask);
            /*! #end !*/
            /*! #end !*/

            // Shift key/allocated pair.
            keys[slotPrev] = keys[slotCurr];

            /*! #if ($RH) !*/
            cached[slotPrev] = cached[slotCurr];
            /*! #end !*/
        }

        //means not allocated
        keys[slotPrev] = Intrinsics.<KType> defaultKTypeValue();
    }

    /**
     * Returns the last key saved in a call to {@link #contains} if it returned <code>true</code>.
     * Precondition : {@link #contains} must have been called previously !
     * @see #contains
     */
    public KType lkey()
    {
        if (this.lastSlot == -2) {

            return Intrinsics.defaultKTypeValue();
        }

        assert this.lastSlot >= 0 : "Call containsKey() first.";

        assert (this.keys[this.lastSlot] != Intrinsics.defaultKTypeValue()) : "Last call to exists did not have any associated value.";

        return this.keys[this.lastSlot];
    }

    /**
     * @return Returns the slot of the last key looked up in a call to {@link #contains} if
     * it returned <code>true</code>.
     * or else -2 if {@link #contains} were succesfull on key = 0/null
     * @see #contains
     */
    public int lslot()
    {
        assert this.lastSlot >= 0 || this.lastSlot == -2 : "Call contains() first.";
        return this.lastSlot;
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
        if (key == Intrinsics.defaultKTypeValue()) {

            if (this.allocatedDefaultKey) {
                this.lastSlot = -2;
            }
            else {
                this.lastSlot = -1;
            }

            return this.allocatedDefaultKey;
        }

        final int mask = this.keys.length - 1;

        final KTypeHashingStrategy<? super KType> strategy = this.hashStrategy;

        final KType[] keys = this.keys;

        //copied straight from  fastutil "fast-path"
        int slot;
        KType curr;

        //1.1 The rehashed slot is free, return false
        if ((curr = keys[slot = REHASH(strategy, key) & mask]) == Intrinsics.defaultKTypeValue()) {

            this.lastSlot = -1;
            return false;
        }

        //1.2) The rehashed entry is occupied by the key, return true
        if (strategy.equals(curr, key)) {

            this.lastSlot = slot;
            return true;
        }

        //2. Hash collision, search for the key along the path
        slot = (slot + 1) & mask;

        /*! #if ($RH) !*/
        final int[] cached = this.hash_cache;
        int dist = 0;
        /*! #end !*/

        while (is_allocated(slot, keys)
                /*! #if ($RH) !*/&& dist <= probe_distance(slot, cached) /*! #end !*/)
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

        //unsuccessful search
        this.lastSlot = -1;

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
        this.allocatedDefaultKey = false;

        //Faster than Arrays.fill(keys, null); // Help the GC.
        KTypeArrays.blankArray(this.keys, 0, this.keys.length);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int size()
    {
        return this.assigned + (this.allocatedDefaultKey ? 1 : 0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int capacity() {

        return this.resizeAt - 1;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        final KTypeHashingStrategy<? super KType> strategy = this.hashStrategy;

        int h = 0;

        if (this.allocatedDefaultKey) {
            h += 0;
        }

        final KType[] keys = this.keys;

        for (int i = keys.length; --i >= 0;)
        {
            if (is_allocated(i, keys))
            {
                h += PhiMix.hash(strategy.computeHashCode(keys[i]));
            }
        }

        return h;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj)
    {
        if (obj != null)
        {
            if (obj == this) {
                return true;
            }

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
            this.cursor = new KTypeCursor<KType>();
            this.cursor.index = -2;
        }

        /**
         * Iterate backwards w.r.t the buffer, to
         * minimize collision chains when filling another hash container (ex. with putAll())
         */
        @Override
        protected KTypeCursor<KType> fetch()
        {
            if (this.cursor.index == KTypeOpenCustomHashSet.this.keys.length + 1) {

                if (KTypeOpenCustomHashSet.this.allocatedDefaultKey) {

                    this.cursor.index = KTypeOpenCustomHashSet.this.keys.length;
                    this.cursor.value = Intrinsics.defaultKTypeValue();

                    return this.cursor;

                }
                //no value associated with the default key, continue iteration...
                this.cursor.index = KTypeOpenCustomHashSet.this.keys.length;
            }

            int i = this.cursor.index - 1;

            while (i >= 0 &&
                    !is_allocated(i, KTypeOpenCustomHashSet.this.keys))
            {
                i--;
            }

            if (i == -1) {
                return done();
            }

            this.cursor.index = i;
            this.cursor.value = KTypeOpenCustomHashSet.this.keys[i];
            return this.cursor;
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
                    obj.cursor.index = KTypeOpenCustomHashSet.this.keys.length + 1;
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

        if (this.allocatedDefaultKey) {

            procedure.apply(Intrinsics.<KType> defaultKTypeValue());
        }

        final KType[] keys = this.keys;

        //Iterate in reverse for side-stepping the longest conflict chain
        //in another hash, in case apply() is actually used to fill another hash container.
        for (int i = keys.length - 1; i >= 0; i--)
        {
            if (is_allocated(i, keys)) {
                procedure.apply(keys[i]);
            }
        }

        return procedure;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public KType[] toArray(final KType[] target)
    {
        int count = 0;

        if (this.allocatedDefaultKey) {

            target[count++] = Intrinsics.defaultKTypeValue();
        }

        final KType[] keys = this.keys;

        for (int i = 0; i < keys.length; i++)
        {
            if (is_allocated(i, keys))
            {
                target[count++] = keys[i];
            }
        }
        assert count == this.size();

        return target;
    }

    /**
     * Clone this object.
     * #if ($TemplateOptions.KTypeGeneric)
     * The returned clone will use the same HashingStrategy strategy.
     * #end
     */
    @Override
    public KTypeOpenCustomHashSet<KType> clone()
    {
        final KTypeOpenCustomHashSet<KType> cloned = new KTypeOpenCustomHashSet<KType>(capacity(), this.loadFactor, this.hashStrategy);

        //We must NOT clone because of the independent perturbation seeds
        cloned.addAll(this);

        cloned.lastSlot = -1;

        cloned.allocatedDefaultKey = this.allocatedDefaultKey;
        cloned.defaultValue = this.defaultValue;

        return cloned;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends KTypePredicate<? super KType>> T forEach(final T predicate)
    {

        if (this.allocatedDefaultKey) {

            if (!predicate.apply(Intrinsics.<KType> defaultKTypeValue())) {

                return predicate;
            }
        }

        final KType[] keys = this.keys;

        //Iterate in reverse for side-stepping the longest conflict chain
        //in another hash, in case apply() is actually used to fill another hash container.
        for (int i = keys.length - 1; i >= 0; i--)
        {
            if (is_allocated(i, keys))
            {
                if (!predicate.apply(keys[i])) {
                    break;
                }
            }
        }

        return predicate;
    }

    /**
     * {@inheritDoc}
     * <p><strong>Important!</strong>
     * If the predicate actually injects the removed keys in another hash container, you may experience performance losses.
     */
    @Override
    public int removeAll(final KTypePredicate<? super KType> predicate)
    {
        final int before = this.size();

        if (this.allocatedDefaultKey) {

            if (predicate.apply(Intrinsics.<KType> defaultKTypeValue()))
            {
                this.allocatedDefaultKey = false;
            }
        }

        final KType[] keys = this.keys;

        for (int i = 0; i < keys.length;)
        {
            if (is_allocated(i, keys))
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

        return before - this.size();
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

    //Test for existence in template
    /*! #if ($TemplateOptions.inlineKType("is_allocated",
    "(slot, keys)",
    "keys[slot] != Intrinsics.defaultKTypeValue()")) !*/
    /**
     *  template version
     * (actual method is inlined in generated code)
     */
    private boolean is_allocated(final int slot, final KType[] keys) {

        return keys[slot] != Intrinsics.defaultKTypeValue();
    }

    /*! #end !*/

/*! #if ($TemplateOptions.inlineKType("probe_distance",
    "(slot, cached)",
    "slot < cached[slot] ? slot + cached.length - cached[slot] : slot - cached[slot]")) !*/
    /**
     * (actual method is inlined in generated code)
     */
    private int probe_distance(final int slot, final int[] cached) {

        final int rh = cached[slot];

        /*! #if($DEBUG) !*/
        //Check : cached hashed slot is == computed value
        final int mask = cached.length - 1;
        assert rh == (REHASH(this.hashStrategy, this.keys[slot]) & mask);
        /*! #end !*/

        if (slot < rh) {
            //wrap around
            return slot + cached.length - rh;
        }

        return slot - rh;
    }

/*! #end !*/

    /*! #if ($TemplateOptions.inlineKType("REHASH",
    "(strategy, value)",
    "PhiMix.hash(strategy.computeHashCode(value) ^ this.perturbation )")) !*/
    /**
     * (actual method is inlined in generated code)
     */
    private int REHASH(final KTypeHashingStrategy<? super KType> strategy, final KType value) {

        return PhiMix.hash(strategy.computeHashCode(value) ^ this.perturbation);
    }
    /*! #end !*/

}
