package com.carrotsearch.hppc;

import java.util.*;

import com.carrotsearch.hppc.cursors.*;
import com.carrotsearch.hppc.hash.MurmurHash3;
import com.carrotsearch.hppc.predicates.*;
import com.carrotsearch.hppc.procedures.*;

import static com.carrotsearch.hppc.Internals.*;
import static com.carrotsearch.hppc.HashContainerUtils.*;

/**
 * A hash set of <code>KType</code>s, implemented using using open
 * addressing with linear probing for collision resolution.
 * 
 * <p>
 * The internal buffers of this implementation ({@link #keys}), {@link #allocated})
 * are always allocated to the nearest size that is a power of two. When
 * the capacity exceeds the given load factor, the buffer size is doubled.
 * </p>
#if ($TemplateOptions.KTypeGeneric)
 * <p>
 * A brief comparison of the API against the Java Collections framework:
 * </p>
 * <table class="nice" summary="Java Collections HashSet and HPPC ObjectOpenHashSet, related methods.">
 * <caption>Java Collections {@link HashSet} and HPPC {@link ObjectOpenHashSet}, related methods.</caption>
 * <thead>
 *     <tr class="odd">
 *         <th scope="col">{@linkplain HashSet java.util.HashSet}</th>
 *         <th scope="col">{@link ObjectOpenHashSet}</th>
 *     </tr>
 * </thead>
 * <tbody>
 * <tr            ><td>boolean add(E) </td><td>boolean add(E)</td></tr>
 * <tr class="odd"><td>boolean remove(E)    </td><td>int removeAllOccurrences(E)</td></tr>
 * <tr            ><td>size, clear,
 *                     isEmpty</td><td>size, clear, isEmpty</td></tr>
 * <tr class="odd"><td>contains(E)    </td><td>contains(E), lkey()</td></tr>
 * <tr            ><td>iterator       </td><td>{@linkplain #iterator() iterator} over set values,
 *                                               pseudo-closures</td></tr>
 * </tbody>
 * </table>
 * 
 * <p>This implementation supports <code>null</code> keys.</p>
 * <p><b>Important note.</b> The implementation uses power-of-two tables and linear
 * probing, which may cause poor performance (many collisions) if hash values are
 * not properly distributed. To counter this, use {@link HashingStrategy} to override equals() and hashCode().
 * This implementation uses rehashing
 * using {@link MurmurHash3}.</p>
#else
 * <p>See {@link ObjectOpenHashSet} class for API similarities and differences against Java
 * Collections.
#end
 * 
 * @author This code is inspired by the collaboration and implementation in the <a
 *         href="http://fastutil.dsi.unimi.it/">fastutil</a> project.
 */
/*! ${TemplateOptions.doNotGenerateKType("BOOLEAN")} !*/
/*! #set( $ROBIN_HOOD_FOR_PRIMITIVES = false) !*/
/*! #set( $DEBUG = false) !*/
// If RH is defined, RobinHood Hashing is in effect :
/*! #set( $RH = $TemplateOptions.KTypeGeneric || $ROBIN_HOOD_FOR_PRIMITIVES ) !*/
/*! ${TemplateOptions.generatedAnnotation} !*/
public class KTypeRobinHoodHashSet<KType>
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
     * We perturb hashed values with the array size to avoid problems with
     * nearly-sorted-by-hash values on iterations.
     * 
     * @see "http://issues.carrot2.org/browse/HPPC-80"
     */
    protected int perturbation;

    /*! #if ($TemplateOptions.KTypeGeneric) !*/
    /**
     * Custom hashing strategy : if != null,
     * comparisons and hash codes of keys will be computed
     * with the strategy methods instead of the native Object equals() and hashCode() methods.
     */
    protected HashingStrategy<? super KType> hashStrategy = null;

    /*! #end !*/

    /**
     * Creates a hash set with the default capacity of {@value #DEFAULT_CAPACITY},
     * load factor of {@value #DEFAULT_LOAD_FACTOR}.
    `     */
    public KTypeRobinHoodHashSet()
    {
        this(KTypeRobinHoodHashSet.DEFAULT_CAPACITY, KTypeRobinHoodHashSet.DEFAULT_LOAD_FACTOR);
    }

    /**
     * Creates a hash set with the given capacity,
     * load factor of {@value #DEFAULT_LOAD_FACTOR}.
     */
    public KTypeRobinHoodHashSet(final int initialCapacity)
    {
        this(initialCapacity, KTypeRobinHoodHashSet.DEFAULT_LOAD_FACTOR);
    }

    /**
     * Creates a hash set with the given capacity and load factor.
     */
    public KTypeRobinHoodHashSet(final int initialCapacity, final float loadFactor)
    {
        assert loadFactor > 0 && loadFactor <= 1 : "Load factor must be between (0, 1].";

        this.loadFactor = loadFactor;

        //take into account of the load factor to garantee no reallocations before reaching  initialCapacity.
        int internalCapacity = (int) (initialCapacity / loadFactor) + KTypeRobinHoodHashSet.MIN_CAPACITY;

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

        this.perturbation = computePerturbationValue(internalCapacity);
    }

    /* #if ($TemplateOptions.KTypeGeneric) */
    /**
     * Creates a hash set with the given capacity, load factor, and hash strategy.
     * Providing a null strategy is equivalent at setting no strategy at all.
     */
    public KTypeRobinHoodHashSet(final int initialCapacity, final float loadFactor, final HashingStrategy<? super KType> strategy)
    {
        this(initialCapacity, loadFactor);

        //only accept not-null strategies.
        if (strategy != null)
        {
            this.hashStrategy = strategy;
        }
    }

    /* #end */

    /**
     * Creates a hash set from elements of another container. Default load factor is used.
     */
    public KTypeRobinHoodHashSet(final KTypeContainer<KType> container)
    {
        this(container.size());
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

        /*! #if ($TemplateOptions.KTypeGeneric) !*/
        final HashingStrategy<? super KType> strategy = this.hashStrategy;
        int slot = KTypeRobinHoodHashSet.rehashSpecificHash(e, perturbation, strategy) & mask;
        /*! #else
        int slot = rehash(e, perturbation) & mask;
        #end !*/

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
            /*! #if ($RH) !*/
            existing_distance = probe_distance(slot, allocated);
            /*! #end !*/

            if (/*! #if ($TemplateOptions.KTypeGeneric) !*/
            KTypeRobinHoodHashSet.equalsKTypeHashStrategy(e, keys[slot], strategy)
            /*! #else
            Intrinsics.equalsKType(e, keys[slot])
            #end !*/)
            {
                return false;
            }
            /*! #if ($RH) !*/
            else if (dist > existing_distance)
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
                /*! #if ($TemplateOptions.KTypeGeneric) !*/
                assert allocated[slot] == (KTypeRobinHoodHashSet.rehashSpecificHash(keys[slot], perturbation, strategy) & mask);
                assert initial_slot == (KTypeRobinHoodHashSet.rehashSpecificHash(e, perturbation, strategy) & mask);
                /*! #else
                        assert allocated[slot] == (rehash(keys[slot], perturbation) & mask);
                        assert initial_slot == (rehash(e, perturbation) & mask);
                #end !*/
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
            /*! #if ($TemplateOptions.KTypeGeneric) !*/
            assert allocated[slot] == (KTypeRobinHoodHashSet.rehashSpecificHash(keys[slot], perturbation, strategy) & mask);
            /*! #else
                    assert allocated[slot] == (rehash(keys[slot], perturbation) & mask);
            #end !*/
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
        assigned = 0;

        //We don't care of the oldAllocated value, so long it means "allocated = true", since the whole set is rebuilt from scratch. 
        /*! #if ($RH) !*/
        oldAllocated[freeSlot] = 1;
        /*!#else
        oldAllocated[freeSlot] = true;
        #end !*/

        oldKeys[freeSlot] = pendingKey;

        //iterate all the old arrays to add in the newly allocated buffers
        for (int i = oldAllocated.length; --i >= 0;)
        {
            if (oldAllocated[i] /*! #if ($RH) !*/!= -1 /*! #end !*/)
            {
                this.add(oldKeys[i]);
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

        this.perturbation = computePerturbationValue(capacity);
    }

    /**
     * <p>Compute the key perturbation value applied before hashing. The returned value
     * should be non-zero and ideally different for each capacity. This matters because
     * keys are nearly-ordered by their hashed values so when adding one container's
     * values to the other, the number of collisions can skyrocket into the worst case
     * possible.
     * 
     * <p>If it is known that hash containers will not be added to each other
     * (will be used for counting only, for example) then some speed can be gained by
     * not perturbing keys before hashing and returning a value of zero for all possible
     * capacities. The speed gain is a result of faster rehash operation (keys are mostly
     * in order).
     */
    protected int computePerturbationValue(final int capacity)
    {
        return HashContainerUtils.PERTURBATIONS[Integer.numberOfLeadingZeros(capacity)];
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
        final int slot = lookupSlot(key);

        if (slot == -1)
        {
            return false;
        }

        this.assigned--;
        shiftConflictingKeys(slot);
        return true;
    }

    /**
     * Shift all the slot-conflicting keys allocated to (and including) <code>slot</code>.
     */
    protected void shiftConflictingKeys(int slotCurr)
    {
        // Copied nearly verbatim from fastutil's impl.
        final int mask = allocated.length - 1;
        int slotPrev, slotOther;

        /*! #if ($TemplateOptions.KTypeGeneric) !*/
        final HashingStrategy<? super KType> strategy = this.hashStrategy;
        /*! #end !*/

        final KType[] keys = this.keys;
        /*! #if ($RH) !*/
        final int[] allocated = this.allocated;
        /*! #else
         final boolean[] allocated = this.allocated;
        #end !*/

        final int perturbation = this.perturbation;

        while (true)
        {
            slotCurr = ((slotPrev = slotCurr) + 1) & mask;

            while (allocated[slotCurr] /*! #if ($RH) !*/!= -1 /*! #end !*/)
            {
                /*! #if ($TemplateOptions.KTypeGeneric) !*/
                slotOther = KTypeRobinHoodHashSet.rehashSpecificHash(keys[slotCurr], perturbation, strategy) & mask;
                /*! #else
                slotOther = rehash(keys[slotCurr], perturbation) & mask;
                #end !*/

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
            /*! #if ($TemplateOptions.KTypeGeneric) !*/
            assert allocated[slotCurr] == (KTypeRobinHoodHashSet.rehashSpecificHash(keys[slotCurr], perturbation, strategy) & mask);
            assert allocated[slotPrev] == (KTypeRobinHoodHashSet.rehashSpecificHash(keys[slotPrev], perturbation, strategy) & mask);
            /*! #else
                assert allocated[slotCurr] == (rehash(keys[slotCurr], perturbation) & mask);
                assert allocated[slotPrev] == (rehash(keys[slotPrev], perturbation) & mask);
            #end !*/
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
        this.lastSlot = lookupSlot(key);
        return this.lastSlot != -1;
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

        for (int i = states.length; --i >= 0;)
        {
            if (states[i]/*! #if ($RH) !*/!= -1 /*! #end !*/)
            {
                /*! #if ($TemplateOptions.KTypeGeneric) !*/
                //This hash is an intrinsic property of the container contents,
                //consequently is independent from the HashStrategy, so do not use it !
                /*! #end !*/
                h += Internals.rehash(keys[i]);
            }
        }

        return h;
    }

    /*! #if ($TemplateOptions.KTypeGeneric) !*/
    /**
     * this instance and obj can only be equal if either: <pre>
     * (both don't have set hash strategies)
     * or
     * (both have equal hash strategies defined by {@link #hashStrategy}.equals(obj.hashStrategy))</pre>
     * then, both maps are compared as follows: <pre>
     * {@inheritDoc}</pre>
     */
    @SuppressWarnings({ "unchecked" })
    /*! #else  !*/
    /**
     * {@inheritDoc}
     */
    /*! #end !*/
    @Override
    public boolean equals(final Object obj)
    {
        if (obj != null)
        {
            if (obj == this)
                return true;

            /*! #if ($TemplateOptions.KTypeGeneric) !*/
            if (obj instanceof KTypeRobinHoodHashSet<?> &&
                    !Intrinsics.equalsKType(this.hashStrategy, ((KTypeRobinHoodHashSet<KType>) obj).hashStrategy)) {

                return false;
            }
            /*! #end !*/

            if (obj instanceof KTypeSet<?>)
            {
                final KTypeSet<Object> other = (KTypeSet<Object>) obj;

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
            cursor.index = -1;
        }

        @Override
        protected KTypeCursor<KType> fetch()
        {
            final int max = keys.length;

            int i = cursor.index + 1;

            while (i < max &&
                    /*! #if ($RH) !*/
                    allocated[i] == -1
            /*! #else
            !allocated[i]
            #end  !*/)

            {
                i++;
            }

            if (i == max)
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
                    obj.cursor.index = -1;
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
     * #end
     */
    @Override
    public KTypeRobinHoodHashSet<KType> clone()
    {
        /* #if ($TemplateOptions.KTypeGeneric) */
        @SuppressWarnings("unchecked")
        /* #end */
        final KTypeRobinHoodHashSet<KType> cloned = new KTypeRobinHoodHashSet<KType>(this.keys.length, this.loadFactor
                /* #if ($TemplateOptions.KTypeGeneric) */
                , this.hashStrategy
                /* #end */);

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
    public static <KType> KTypeRobinHoodHashSet<KType> from(final KType... elements)
    {
        final KTypeRobinHoodHashSet<KType> set = new KTypeRobinHoodHashSet<KType>(elements.length);
        set.add(elements);
        return set;
    }

    /**
     * Create a set from elements of another container.
     */
    public static <KType> KTypeRobinHoodHashSet<KType> from(final KTypeContainer<KType> container)
    {
        return new KTypeRobinHoodHashSet<KType>(container);
    }

    /**
     * Create a new hash set with default parameters (shortcut
     * instead of using a constructor).
     */
    public static <KType> KTypeRobinHoodHashSet<KType> newInstance()
    {
        return new KTypeRobinHoodHashSet<KType>();
    }

    /**
     * Create a new hash set with no key perturbations, and default parameters (see
     * {@link #computePerturbationValue(int)}). This may lead to increased performance, but only use when sure the container will not
     * be used for direct copying of keys to another hash container.
     */
    public static <KType> KTypeRobinHoodHashSet<KType> newInstanceWithoutPerturbations()
    {
        return new KTypeRobinHoodHashSet<KType>() {
            @Override
            protected final int computePerturbationValue(final int capacity)
            {
                return 0;
            }
        };
    }

    /**
     * Returns a new object of this class with no need to declare generic type (shortcut
     * instead of using a constructor).
     */
    public static <KType> KTypeRobinHoodHashSet<KType> newInstanceWithCapacity(final int initialCapacity, final float loadFactor)
    {
        return new KTypeRobinHoodHashSet<KType>(initialCapacity, loadFactor);
    }

    /**
     * Create a new hash set with initial capacity and load factor control, with no key perturbations. (see
     * {@link #computePerturbationValue(int)}). This may lead to increased performance, but only use when sure the container will not
     * be used for direct copying of keys to another hash container.
     */
    public static <KType> KTypeRobinHoodHashSet<KType> newInstanceWithoutPerturbations(final int initialCapacity, final float loadFactor)
    {
        return new KTypeRobinHoodHashSet<KType>(initialCapacity, loadFactor) {
            @Override
            protected final int computePerturbationValue(final int capacity)
            {
                return 0;
            }
        };
    }

    /* #if ($TemplateOptions.KTypeGeneric) */
    /**
     * Create a new hash set with full parameter control, using a specific hash strategy.
     * A strategy = null is equivalent at providing no strategy at all.
     */
    public static <KType> KTypeRobinHoodHashSet<KType> newInstance(final int initialCapacity, final float loadFactor, final HashingStrategy<? super KType> strategy)
    {
        return new KTypeRobinHoodHashSet<KType>(initialCapacity, loadFactor, strategy);
    }

    /**
     * Create a new hash set with full parameter control, using a specific hash strategy, with no key perturbations (see
     * {@link #computePerturbationValue(int)}).
     * A strategy = null is equivalent at providing no strategy at all.
     * This may lead to increased performance, but only use when sure the container will not
     * be used for direct copying of keys to another hash container.
     */
    public static <KType> KTypeRobinHoodHashSet<KType> newInstanceWithoutPerturbations(final int initialCapacity, final float loadFactor, final HashingStrategy<? super KType> strategy)
    {
        return new KTypeRobinHoodHashSet<KType>(initialCapacity, loadFactor, strategy) {
            @Override
            protected final int computePerturbationValue(final int capacity)
            {
                return 0;
            }
        };
    }

    /**
     * Return the current {@link HashingStrategy} in use, or {@code null} if none was set.
     * @return
     */
    public HashingStrategy<? super KType> strategy()
    {
        return this.hashStrategy;
    }

    /* #end */

    private int lookupSlot(final KType key) {

        final int mask = allocated.length - 1;

        /*! #if ($TemplateOptions.KTypeGeneric) !*/
        final HashingStrategy<? super KType> strategy = this.hashStrategy;
        int slot = KTypeRobinHoodHashSet.rehashSpecificHash(key, perturbation, strategy) & mask;
        /*! #else
        int slot = rehash(key, perturbation) & mask;
        #end !*/

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
            if (/*! #if ($TemplateOptions.KTypeGeneric) !*/
            KTypeRobinHoodHashSet.equalsKTypeHashStrategy(key, keys[slot], strategy)
            /*! #else
            Intrinsics.equalsKType(key, keys[slot])
            #end !*/)
            {
                return slot;
            }
            slot = (slot + 1) & mask;

            /*! #if ($RH) !*/
            dist++;
            /*! #end !*/
        } //end while true

        return -1;
    }

    /*! #if ($TemplateOptions.inlineGenericAndPrimitive("KTypeRobinHoodHashSet.equalsKTypeHashStrategy",
      "(e1,  e2, customEquals)",
      "(e1 == null ? e2 == null : (customEquals == null ? e1.equals(e2) : customEquals.equals(e1, e2)))",
     "")) !*/
    /**
     * Compare two Objects for equivalence, using a {@link HashingStrategy}. Null references return <code>true</code>.
     * A null {@link HashingStrategy} is equivalent of calling {@link #equalsKType(Object e1, Object e2)}.
     * This method is inlined in generated code
     */
    private static <T> boolean equalsKTypeHashStrategy(final T e1, final T e2, final HashingStrategy<? super T> customEquals)
    {
        return (e1 == null ? e2 == null : (customEquals == null ? e1.equals(e2) : customEquals.equals(e1, e2)));
    }

    /*! #end !*/

    /*! #if ($TemplateOptions.inlineGenericAndPrimitive("KTypeRobinHoodHashSet.rehashSpecificHash",
    "( o,  p, specificHash)",
    "o == null ? 0 : (specificHash == null? MurmurHash3.hash(o.hashCode() ^ p) :(MurmurHash3.hash(specificHash.computeHashCode(o) ^ p)))",
    "")) !*/
    /**
     * if specificHash == null, equivalent to rehash()
     * The actual code is inlined in generated code
     * @param object
     * @param p
     * @param specificHash
     * @return
     */
    private static <T> int rehashSpecificHash(final T o, final int p, final HashingStrategy<? super T> specificHash)
    {
        return o == null ? 0 : (specificHash == null ? MurmurHash3.hash(o.hashCode() ^ p) : (MurmurHash3.hash(specificHash.computeHashCode(o) ^ p)));
    }

    /*! #end !*/

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
        /*! #if ($TemplateOptions.KTypeGeneric) !*/
        final int mask = alloc.length - 1;
        assert rh == (KTypeRobinHoodHashSet.rehashSpecificHash(this.keys[slot], perturbation, this.hashStrategy) & mask);
        /*! #else
            final int mask = alloc.length - 1;
            assert rh == (rehash(this.keys[slot], perturbation) & mask);
        #end !*/
        /*! #end !*/

        if (slot < rh) {
            //wrap around
            return slot + alloc.length - rh;
        }

        return slot - rh;
    }
    /*! #end !*/

}
