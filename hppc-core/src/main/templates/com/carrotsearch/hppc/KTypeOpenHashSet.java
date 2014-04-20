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
/*! ${TemplateOptions.generatedAnnotation} !*/
public class KTypeOpenHashSet<KType>
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
     * Information if an entry (slot) in the {@link #keys} table is allocated
     * or empty.
     * 
     * @see #assigned
     */
    public boolean[] allocated;

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
    public KTypeOpenHashSet()
    {
        this(KTypeOpenHashSet.DEFAULT_CAPACITY, KTypeOpenHashSet.DEFAULT_LOAD_FACTOR);
    }

    /**
     * Creates a hash set with the given capacity,
     * load factor of {@value #DEFAULT_LOAD_FACTOR}.
     */
    public KTypeOpenHashSet(final int initialCapacity)
    {
        this(initialCapacity, KTypeOpenHashSet.DEFAULT_LOAD_FACTOR);
    }

    /**
     * Creates a hash set with the given capacity and load factor.
     */
    public KTypeOpenHashSet(final int initialCapacity, final float loadFactor)
    {
        assert loadFactor > 0 && loadFactor <= 1 : "Load factor must be between (0, 1].";

        this.loadFactor = loadFactor;

        //take into account of the load factor to garantee no reallocations before reaching  initialCapacity.
        int internalCapacity = (int) (initialCapacity / loadFactor) + KTypeOpenHashSet.MIN_CAPACITY;

        //align on next power of two
        internalCapacity = HashContainerUtils.roundCapacity(internalCapacity);

        this.keys = Intrinsics.newKTypeArray(internalCapacity);
        this.allocated = new boolean[internalCapacity];

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
    public KTypeOpenHashSet(final int initialCapacity, final float loadFactor, final HashingStrategy<? super KType> strategy)
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
    public KTypeOpenHashSet(final KTypeContainer<KType> container)
    {
        this(container.size());
        addAll(container);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean add(final KType e)
    {
        assert assigned < allocated.length;

        final int mask = allocated.length - 1;

        /*! #if ($TemplateOptions.KTypeGeneric) !*/
        final HashingStrategy<? super KType> strategy = this.hashStrategy;
        int slot = KTypeOpenHashSet.rehashSpecificHash(e, perturbation, strategy) & mask;
        /*! #else
        int slot = rehash(e, perturbation) & mask;
        #end !*/

        final KType[] keys = this.keys;
        final boolean[] allocated = this.allocated;

        while (allocated[slot])
        {
            if (/*! #if ($TemplateOptions.KTypeGeneric) !*/
            KTypeOpenHashSet.equalsKTypeHashStrategy(e, keys[slot], strategy)
            /*! #else
            Intrinsics.equalsKType(e, keys[slot])
            #end !*/)
            {
                return false;
            }

            slot = (slot + 1) & mask;
        }

        // Check if we need to grow. If so, reallocate new data,
        // fill in the last element and rehash.
        if (assigned == resizeAt) {
            expandAndAdd(e, slot);
        }
        else {
            assigned++;
            allocated[slot] = true;
            keys[slot] = e;
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
        assert !allocated[freeSlot];

        // Try to allocate new buffers first. If we OOM, it'll be now without
        // leaving the data structure in an inconsistent state.
        final KType[] oldKeys = this.keys;
        final boolean[] oldAllocated = this.allocated;

        allocateBuffers(HashContainerUtils.nextCapacity(keys.length));

        // We have succeeded at allocating new data so insert the pending key/value at
        // the free slot in the old arrays before rehashing.
        lastSlot = -1;
        assigned++;
        oldAllocated[freeSlot] = true;
        oldKeys[freeSlot] = pendingKey;

        // Rehash all stored keys into the new buffers.
        final KType[] keys = this.keys;
        final boolean[] allocated = this.allocated;
        final int perturbation = this.perturbation;

        /*! #if ($TemplateOptions.KTypeGeneric) !*/
        final HashingStrategy<? super KType> strategy = this.hashStrategy;
        /*! #end !*/
        final int mask = allocated.length - 1;

        for (int i = oldAllocated.length; --i >= 0;)
        {
            if (oldAllocated[i])
            {
                final KType k = oldKeys[i];

                /*! #if ($TemplateOptions.KTypeGeneric) !*/
                int slot = KTypeOpenHashSet.rehashSpecificHash(k, perturbation, strategy) & mask;
                /*! #else
                int slot = rehash(k, perturbation) & mask;
                #end !*/
                while (allocated[slot])
                {
                    slot = (slot + 1) & mask;
                }

                allocated[slot] = true;
                keys[slot] = k;
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
        final boolean[] allocated = new boolean[capacity];

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
        final int mask = allocated.length - 1;

        /*! #if ($TemplateOptions.KTypeGeneric) !*/
        final HashingStrategy<? super KType> strategy = this.hashStrategy;
        int slot = KTypeOpenHashSet.rehashSpecificHash(key, perturbation, strategy) & mask;
        /*! #else
        int slot = rehash(key, perturbation) & mask;
        #end !*/

        final KType[] keys = this.keys;
        final boolean[] allocated = this.allocated;

        while (allocated[slot])
        {
            if (/*! #if ($TemplateOptions.KTypeGeneric) !*/
            KTypeOpenHashSet.equalsKTypeHashStrategy(key, keys[slot], strategy)
            /*! #else
            Intrinsics.equalsKType(key, keys[slot])
            #end !*/)
            {
                assigned--;
                shiftConflictingKeys(slot);
                return true;
            }
            slot = (slot + 1) & mask;
        }

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

        /*! #if ($TemplateOptions.KTypeGeneric) !*/
        final HashingStrategy<? super KType> strategy = this.hashStrategy;
        /*! #end !*/

        final KType[] keys = this.keys;
        final boolean[] allocated = this.allocated;
        final int perturbation = this.perturbation;

        while (true)
        {
            slotCurr = ((slotPrev = slotCurr) + 1) & mask;

            while (allocated[slotCurr])
            {
                /*! #if ($TemplateOptions.KTypeGeneric) !*/
                slotOther = KTypeOpenHashSet.rehashSpecificHash(keys[slotCurr], perturbation, strategy) & mask;
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

            if (!allocated[slotCurr])
                break;

            // Shift key/value pair.
            keys[slotPrev] = keys[slotCurr];
        }

        allocated[slotPrev] = false;

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
        assert allocated[lastSlot] : "Last call to exists did not have any associated value.";

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

        /*! #if ($TemplateOptions.KTypeGeneric) !*/
        final HashingStrategy<? super KType> strategy = this.hashStrategy;
        int slot = KTypeOpenHashSet.rehashSpecificHash(key, perturbation, this.hashStrategy) & mask;
        /*! #else
        int slot = rehash(key, perturbation) & mask;
        #end !*/

        final KType[] keys = this.keys;
        final boolean[] allocated = this.allocated;

        while (allocated[slot])
        {
            if (/*! #if ($TemplateOptions.KTypeGeneric) !*/
            KTypeOpenHashSet.equalsKTypeHashStrategy(key, keys[slot], strategy)
            /*! #else
            Intrinsics.equalsKType(key, keys[slot])
            #end !*/)
            {
                lastSlot = slot;
                return true;
            }
            slot = (slot + 1) & mask;
        }
        lastSlot = -1;
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
        assigned = 0;
        lastSlot = -1;
        Internals.blankBooleanArray(allocated, 0, allocated.length);

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
    public int hashCode()
    {
        int h = 0;

        final KType[] keys = this.keys;
        final boolean[] states = this.allocated;

        for (int i = states.length; --i >= 0;)
        {
            if (states[i])
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
            if (obj instanceof KTypeOpenHashSet<?> &&
                    !Intrinsics.equalsKType(this.hashStrategy, ((KTypeOpenHashSet<KType>) obj).hashStrategy)) {

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
            while (i < keys.length && !allocated[i])
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
        final boolean[] states = this.allocated;

        for (int i = 0; i < states.length; i++)
        {
            if (states[i])
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
        final boolean[] allocated = this.allocated;

        for (int i = 0, j = 0; i < keys.length; i++) {

            if (allocated[i])
                target[j++] = keys[i];
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
    public KTypeOpenHashSet<KType> clone()
    {
        /* #if ($TemplateOptions.KTypeGeneric) */
        @SuppressWarnings("unchecked")
        /* #end */
        final KTypeOpenHashSet<KType> cloned = new KTypeOpenHashSet<KType>((int) (this.keys.length * this.loadFactor) + 1, this.loadFactor
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
        final boolean[] states = this.allocated;

        for (int i = 0; i < states.length; i++)
        {
            if (states[i])
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
        final boolean[] allocated = this.allocated;

        final int before = assigned;
        for (int i = 0; i < allocated.length;)
        {
            if (allocated[i])
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
    public static <KType> KTypeOpenHashSet<KType> from(final KType... elements)
    {
        final KTypeOpenHashSet<KType> set = new KTypeOpenHashSet<KType>(elements.length);
        set.add(elements);
        return set;
    }

    /**
     * Create a set from elements of another container.
     */
    public static <KType> KTypeOpenHashSet<KType> from(final KTypeContainer<KType> container)
    {
        return new KTypeOpenHashSet<KType>(container);
    }

    /**
     * Create a new hash set with default parameters (shortcut
     * instead of using a constructor).
     */
    public static <KType> KTypeOpenHashSet<KType> newInstance()
    {
        return new KTypeOpenHashSet<KType>();
    }

    /**
     * Create a new hash set with no key perturbations, and default parameters (see
     * {@link #computePerturbationValue(int)}). This may lead to increased performance, but only use when sure the container will not
     * be used for direct copying of keys to another hash container.
     */
    public static <KType> KTypeOpenHashSet<KType> newInstanceWithoutPerturbations()
    {
        return new KTypeOpenHashSet<KType>() {
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
    public static <KType> KTypeOpenHashSet<KType> newInstanceWithCapacity(final int initialCapacity, final float loadFactor)
    {
        return new KTypeOpenHashSet<KType>(initialCapacity, loadFactor);
    }

    /**
     * Create a new hash set with initial capacity and load factor control, with no key perturbations. (see
     * {@link #computePerturbationValue(int)}). This may lead to increased performance, but only use when sure the container will not
     * be used for direct copying of keys to another hash container.
     */
    public static <KType> KTypeOpenHashSet<KType> newInstanceWithoutPerturbations(final int initialCapacity, final float loadFactor)
    {
        return new KTypeOpenHashSet<KType>(initialCapacity, loadFactor) {
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
    public static <KType> KTypeOpenHashSet<KType> newInstance(final int initialCapacity, final float loadFactor, final HashingStrategy<? super KType> strategy)
    {
        return new KTypeOpenHashSet<KType>(initialCapacity, loadFactor, strategy);
    }

    /**
     * Create a new hash set with full parameter control, using a specific hash strategy, with no key perturbations (see
     * {@link #computePerturbationValue(int)}).
     * A strategy = null is equivalent at providing no strategy at all.
     * This may lead to increased performance, but only use when sure the container will not
     * be used for direct copying of keys to another hash container.
     */
    public static <KType> KTypeOpenHashSet<KType> newInstanceWithoutPerturbations(final int initialCapacity, final float loadFactor, final HashingStrategy<? super KType> strategy)
    {
        return new KTypeOpenHashSet<KType>(initialCapacity, loadFactor, strategy) {
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

    /*! #if ($TemplateOptions.inlineGenericAndPrimitive("KTypeOpenHashSet.equalsKTypeHashStrategy",
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

    /*! #if ($TemplateOptions.inlineGenericAndPrimitive("KTypeOpenHashSet.rehashSpecificHash",
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

}
