package com.carrotsearch.hppc;


import java.util.*;

import com.carrotsearch.hppc.cursors.*;
import com.carrotsearch.hppc.predicates.*;
import com.carrotsearch.hppc.procedures.*;

import static com.carrotsearch.hppc.Internals.*;
import static com.carrotsearch.hppc.HashContainerUtils.*;

/**
 * A hash map of <code>KType</code> to <code>VType</code>, implemented using open
 * addressing with linear probing for collision resolution.
 * 
 * <p>
 * The internal buffers of this implementation ({@link #keys}, {@link #values},
 * {@link #allocated}) are always allocated to the nearest size that is a power of two. When
 * the capacity exceeds the given load factor, the buffer size is doubled.
 * </p>
 *
#if ($TemplateOptions.AllGeneric)
 * <p>
 * A brief comparison of the API against the Java Collections framework:
 * </p>
 * <table class="nice" summary="Java Collections HashMap and HPPC ObjectObjectOpenHashMap, related methods.">
 * <caption>Java Collections HashMap and HPPC {@link ObjectObjectOpenHashMap}, related methods.</caption>
 * <thead>
 *     <tr class="odd">
 *         <th scope="col">{@linkplain HashMap java.util.HashMap}</th>
 *         <th scope="col">{@link ObjectObjectOpenHashMap}</th>
 *     </tr>
 * </thead>
 * <tbody>
 * <tr            ><td>V put(K)       </td><td>V put(K)      </td></tr>
 * <tr class="odd"><td>V get(K)       </td><td>V get(K)      </td></tr>
 * <tr            ><td>V remove(K)    </td><td>V remove(K)   </td></tr>
 * <tr class="odd"><td>size, clear,
 *                     isEmpty</td><td>size, clear, isEmpty</td></tr>
 * <tr            ><td>containsKey(K) </td><td>containsKey(K), lget()</td></tr>
 * <tr class="odd"><td>containsValue(K) </td><td>(no efficient equivalent)</td></tr>
 * <tr            ><td>keySet, entrySet </td><td>{@linkplain #iterator() iterator} over map entries,
 *                                               keySet, pseudo-closures</td></tr>
#else
 * <p>See {@link ObjectObjectOpenHashMap} class for API similarities and differences against Java
 * Collections.
#end
 * 
#if ($TemplateOptions.KTypeGeneric)
 * <p>This implementation supports <code>null</code> keys.</p>
 * 
 * </tbody>
 * </table>
 * <p><b>Important note.</b> The implementation uses power-of-two tables and linear
 * probing, which may cause poor performance (many collisions) if hash values are
 * not properly distributed. To counter this, use {@link HashingStrategy} to override equals() and hashCode().
 * This implementation uses rehashing
 * using {@link MurmurHash3}.</p>
#end
#if ($TemplateOptions.VTypeGeneric)
 * <p>This implementation supports <code>null</code> values.</p>
#end
 * 
 * 
 * @author This code is inspired by the collaboration and implementation in the <a
 *         href="http://fastutil.dsi.unimi.it/">fastutil</a> project.
 */
// ${TemplateOptions.doNotGenerateKType("BOOLEAN")}
/*! ${TemplateOptions.generatedAnnotation} !*/
public class KTypeVTypeOpenHashMap<KType, VType>
implements KTypeVTypeMap<KType, VType>, Cloneable
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

    protected VType defaultValue = Intrinsics.<VType> defaultVTypeValue();

    /**
     * Hash-indexed array holding all keys.
     * <p>
     * Direct map iteration: iterate  {keys[i], values[i]} for i in [0; keys.length[ where this.allocated[i] is true.
     * </p>
     * @see #values
     * @see #allocated
     */
    public KType [] keys;

    /**
     * Hash-indexed array holding all values associated to the keys
     * stored in {@link #keys}.
     * 
     * @see #keys
     */
    public VType [] values;

    /**
     * Information if an entry (slot) in the {@link #values} table is allocated
     * or empty.
     * 
     * @see #assigned
     */
    public boolean [] allocated;

    /**
     * Cached number of assigned slots in {@link #allocated}.
     */
    protected int assigned;

    /**
     * The load factor for this map (fraction of allocated slots
     * before the buffers must be rehashed or reallocated).
     */
    protected final float loadFactor;

    /**
     * Resize buffers when {@link #allocated} hits this value.
     */
    protected int resizeAt;

    /**
     * The most recent slot accessed in {@link #containsKey} (required for
     * {@link #lget}).
     * 
     * @see #containsKey
     * @see #lget
     */
    protected int lastSlot;

    /**
     * We perturb hashed values with the array size to avoid problems with
     * nearly-sorted-by-hash values on iterations.
     * 
     * @see "http://issues.carrot2.org/browse/HPPC-80"
     */
    protected int perturbation;

    /* #if ($TemplateOptions.KTypeGeneric) */
    /**
     * Custom hashing strategy : if != null,
     * comparisons and hash codes of keys will be computed
     * with the strategy methods instead of the native Object equals() and hashCode() methods.
     */
    protected HashingStrategy<KType> hashStrategy = null;
    /* #end */

    /**
     * Creates a hash map with the default capacity of {@value #DEFAULT_CAPACITY},
     * load factor of {@value #DEFAULT_LOAD_FACTOR}.
     * 
     * <p>See class notes about hash distribution importance.</p>
     */
    public KTypeVTypeOpenHashMap()
    {
        this(KTypeVTypeOpenHashMap.DEFAULT_CAPACITY);
    }

    /**
     * Creates a hash map with the given initial capacity, default load factor of
     * {@value #DEFAULT_LOAD_FACTOR}.
     * 
     * <p>See class notes about hash distribution importance.</p>
     * 
     * @param initialCapacity Initial capacity (greater than zero and automatically
     *            rounded to the next power of two).
     */
    public KTypeVTypeOpenHashMap(final int initialCapacity)
    {
        this(initialCapacity, KTypeVTypeOpenHashMap.DEFAULT_LOAD_FACTOR);
    }

    /**
     * Creates a hash map with the given initial capacity,
     * load factor.
     * 
     * <p>See class notes about hash distribution importance.</p>
     * 
     * @param initialCapacity Initial capacity (greater than zero and automatically
     *            rounded to the next power of two).
     *
     * @param loadFactor The load factor (greater than zero and smaller than 1).
     */
    public KTypeVTypeOpenHashMap(int initialCapacity, final float loadFactor)
    {
        initialCapacity = Math.max(initialCapacity, KTypeVTypeOpenHashMap.MIN_CAPACITY);

        assert initialCapacity > 0
        : "Initial capacity must be between (0, " + Integer.MAX_VALUE + "].";
        assert loadFactor > 0 && loadFactor <= 1
                : "Load factor must be between (0, 1].";

        this.loadFactor = loadFactor;
        allocateBuffers(HashContainerUtils.roundCapacity(initialCapacity));
    }

    /* #if ($TemplateOptions.KTypeGeneric) */
    /**
     * Creates a hash map with the given initial capacity, load factor, and hash
     * strategy.
     * Providing a null strategy is equivalent at setting no strategy at all.
     * <p>See class notes about hash distribution importance. This constructor precisely allows control over equality and hash code computation.</p>
     * 
     * @param initialCapacity Initial capacity (greater than zero and automatically
     *            rounded to the next power of two).
     *
     * @param loadFactor The load factor (greater than zero and smaller than 1).
     * 
     * @param strategy {@link HashingStrategy<KType>} which customizes the equality and hash code for KType objects.
     */
    public KTypeVTypeOpenHashMap(final int initialCapacity, final float loadFactor , final HashingStrategy<KType> strategy)
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
     * Create a hash map from all key-value pairs of another container.
     */
    public KTypeVTypeOpenHashMap(final KTypeVTypeAssociativeContainer<KType, VType> container)
    {
        this((int)(container.size() * (1 + KTypeVTypeOpenHashMap.DEFAULT_LOAD_FACTOR)));
        putAll(container);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public VType put(final KType key, final VType value)
    {
        assert assigned < allocated.length;

        final int mask = allocated.length - 1;

        /*! #if ($TemplateOptions.KTypeGeneric) !*/
        final HashingStrategy<KType> strategy = this.hashStrategy;
        int slot = Internals.rehashSpecificHash(key, perturbation, strategy) & mask;
        /*! #else
        int slot = rehash(key, perturbation) & mask;
        #end !*/

        while (allocated[slot])
        {
            if (/*! #if ($TemplateOptions.KTypeGeneric) !*/
                    Intrinsics.equalsKTypeHashStrategy(key, keys[slot], strategy)
                    /*! #else
                Intrinsics.equalsKType(key, keys[slot])
                #end !*/)
            {
                final VType oldValue = values[slot];
                values[slot] = value;
                return oldValue;
            }

            slot = (slot + 1) & mask;
        }

        // Check if we need to grow. If so, reallocate new data, fill in the last element
        // and rehash.
        if (assigned == resizeAt) {
            expandAndPut(key, value, slot);
        } else {
            assigned++;
            allocated[slot] = true;
            keys[slot] = key;
            values[slot] = value;
        }
        return this.defaultValue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int putAll(
            final KTypeVTypeAssociativeContainer<? extends KType, ? extends VType> container)
    {
        final int count = this.assigned;
        for (final KTypeVTypeCursor<? extends KType, ? extends VType> c : container)
        {
            put(c.key, c.value);
        }
        return this.assigned - count;
    }

    /**
     * Puts all key/value pairs from a given iterable into this map.
     */
    @Override
    public int putAll(
            final Iterable<? extends KTypeVTypeCursor<? extends KType, ? extends VType>> iterable)
    {
        final int count = this.assigned;
        for (final KTypeVTypeCursor<? extends KType, ? extends VType> c : iterable)
        {
            put(c.key, c.value);
        }
        return this.assigned - count;
    }

    /**
     * <a href="http://trove4j.sourceforge.net">Trove</a>-inspired API method. An equivalent
     * of the following code:
     * <pre>
     * if (!map.containsKey(key)) map.put(value);
     * </pre>
     * 
     * @param key The key of the value to check.
     * @param value The value to put if <code>key</code> does not exist.
     * @return <code>true</code> if <code>key</code> did not exist and <code>value</code>
     * was placed in the map.
     */
    public boolean putIfAbsent(final KType key, final VType value)
    {
        if (!containsKey(key))
        {
            put(key, value);
            return true;
        }
        return false;
    }

    /**
     * <a href="http://trove4j.sourceforge.net">Trove</a>-inspired API method. An equivalent
     * of the following code:
     * <pre>
     *  if (containsKey(key))
     *  {
     *      VType v = (VType) (lget() + additionValue);
     *      lset(v);
     *      return v;
     *  }
     *  else
     *  {
     *     put(key, putValue);
     *     return putValue;
     *  }
     * </pre>
     * 
     * @param key The key of the value to adjust.
     * @param putValue The value to put if <code>key</code> does not exist.
     * @param additionValue The value to add to the existing value if <code>key</code> exists.
     * @return Returns the current value associated with <code>key</code> (after changes).
     */
    /*! #if ($TemplateOptions.VTypeNumeric && $TemplateOptions.KTypePrimitive)
    public VType putOrAdd(KType key, VType putValue, VType additionValue)
    {
        assert assigned < allocated.length;

        final int mask = allocated.length - 1;
        int slot = rehash(key, perturbation) & mask;

        while (allocated[slot])
        {
            if (key == keys[slot])
            {
                values[slot] += additionValue;
                return values[slot];
            }

            slot = (slot + 1) & mask;
        }

        if (assigned == resizeAt) {
            expandAndPut(key, putValue, slot);
        } else {
            assigned++;
            allocated[slot] = true;
            keys[slot] = key;
            values[slot] = putValue;
        }
        return putValue;
    }
    #end !*/

    /*! #if ($TemplateOptions.VTypeNumeric && $TemplateOptions.KTypeGeneric)
    public VType putOrAdd(KType key, VType putValue, VType additionValue)
    {
        assert assigned < allocated.length;

        final int mask = allocated.length - 1;
        final HashingStrategy<KType> strategy = this.hashStrategy;
        int slot = rehashSpecificHash(key, perturbation, strategy) & mask;
        while (allocated[slot])
        {
            if (Intrinsics.equalsKTypeHashStrategy(key, keys[slot], strategy))
            {
                values[slot] += additionValue;
                return values[slot];
            }

            slot = (slot + 1) & mask;
        }

        if (assigned == resizeAt) {
            expandAndPut(key, putValue, slot);
        } else {
            assigned++;
            allocated[slot] = true;
            keys[slot] = key;
            values[slot] = putValue;
        }
        return putValue;
    }
    #end !*/

    /**
     * An equivalent of calling
     * <pre>
     *  if (containsKey(key))
     *  {
     *      VType v = (VType) (lget() + additionValue);
     *      lset(v);
     *      return v;
     *  }
     *  else
     *  {
     *     put(key, additionValue);
     *     return additionValue;
     *  }
     * </pre>
     * 
     * @param key The key of the value to adjust.
     * @param additionValue The value to put or add to the existing value if <code>key</code> exists.
     * @return Returns the current value associated with <code>key</code> (after changes).
     */
    /*! #if ($TemplateOptions.VTypeNumeric)
    public VType addTo(KType key, VType additionValue)
    {
        return putOrAdd(key, additionValue, additionValue);
    }
    #end !*/

    /**
     * Expand the internal storage buffers (capacity) and rehash.
     */
    private void expandAndPut(final KType pendingKey, final VType pendingValue, final int freeSlot)
    {
        assert assigned == resizeAt;
        assert !allocated[freeSlot];

        // Try to allocate new buffers first. If we OOM, it'll be now without
        // leaving the data structure in an inconsistent state.
        final KType   [] oldKeys      = this.keys;
        final VType   [] oldValues    = this.values;
        final boolean [] oldAllocated = this.allocated;

        allocateBuffers(HashContainerUtils.nextCapacity(keys.length));

        // We have succeeded at allocating new data so insert the pending key/value at
        // the free slot in the old arrays before rehashing.
        lastSlot = -1;
        assigned++;
        oldAllocated[freeSlot] = true;
        oldKeys[freeSlot] = pendingKey;
        oldValues[freeSlot] = pendingValue;

        // Rehash all stored keys into the new buffers.
        final KType []   keys = this.keys;
        final VType []   values = this.values;
        final boolean [] allocated = this.allocated;
        /*! #if ($TemplateOptions.KTypeGeneric) !*/
        final HashingStrategy<KType> strategy = this.hashStrategy;
        /*! #end !*/
        final int mask = allocated.length - 1;
        for (int i = oldAllocated.length; --i >= 0;)
        {
            if (oldAllocated[i])
            {
                final KType k = oldKeys[i];
                final VType v = oldValues[i];

                /*! #if ($TemplateOptions.KTypeGeneric) !*/
                int slot = Internals.rehashSpecificHash(k, perturbation, strategy) & mask;
                /*! #else
                int slot = rehash(k, perturbation) & mask;
                #end !*/
                while (allocated[slot])
                {
                    slot = (slot + 1) & mask;
                }

                allocated[slot] = true;
                keys[slot] = k;
                values[slot] = v;
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
        final KType [] keys = Intrinsics.newKTypeArray(capacity);
        final VType [] values = Intrinsics.newVTypeArray(capacity);
        final boolean [] allocated = new boolean [capacity];

        this.keys = keys;
        this.values = values;
        this.allocated = allocated;

        this.resizeAt = Math.max(2, (int) Math.ceil(capacity * loadFactor)) - 1;
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
    public VType remove(final KType key)
    {
        final int mask = allocated.length - 1;

        /*! #if ($TemplateOptions.KTypeGeneric) !*/
        final HashingStrategy<KType> strategy = this.hashStrategy;
        int slot = Internals.rehashSpecificHash(key, perturbation, strategy) & mask;
        /*! #else
        int slot = rehash(key, perturbation) & mask;
        #end !*/
        final int wrappedAround = slot;
        while (allocated[slot])
        {
            if (/*! #if ($TemplateOptions.KTypeGeneric) !*/
                    Intrinsics.equalsKTypeHashStrategy(key, keys[slot], strategy)
                    /*! #else
                Intrinsics.equalsKType(key, keys[slot])
                #end !*/)
            {
                assigned--;
                final VType v = values[slot];
                shiftConflictingKeys(slot);
                return v;
            }
            slot = (slot + 1) & mask;
            if (slot == wrappedAround) break;
        }

        return this.defaultValue;
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
        final HashingStrategy<KType> strategy = this.hashStrategy;
        /*! #end !*/

        while (true)
        {
            slotCurr = ((slotPrev = slotCurr) + 1) & mask;

            while (allocated[slotCurr])
            {
                /*! #if ($TemplateOptions.KTypeGeneric) !*/
                slotOther = Internals.rehashSpecificHash(keys[slotCurr], perturbation, strategy) & mask;
                /*! #else
                slotOther = rehash(keys[slotCurr], perturbation) & mask;
                #end !*/

                if (slotPrev <= slotCurr)
                {
                    // we're on the right of the original slot.
                    if (slotPrev >= slotOther || slotOther > slotCurr)
                        break;
                }
                else
                {
                    // we've wrapped around.
                    if (slotPrev >= slotOther && slotOther > slotCurr)
                        break;
                }
                slotCurr = (slotCurr + 1) & mask;
            }

            if (!allocated[slotCurr])
                break;

            // Shift key/value pair.
            keys[slotPrev] = keys[slotCurr];
            values[slotPrev] = values[slotCurr];
        }

        allocated[slotPrev] = false;

        /* #if ($TemplateOptions.KTypeGeneric) */
        keys[slotPrev] = Intrinsics.<KType> defaultKTypeValue();
        /* #end */
        /* #if ($TemplateOptions.VTypeGeneric) */
        values[slotPrev] = Intrinsics.<VType> defaultVTypeValue();
        /* #end */
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int removeAll(final KTypeContainer<? extends KType> container)
    {
        final int before = this.assigned;

        for (final KTypeCursor<? extends KType> cursor : container)
        {
            remove(cursor.value);
        }

        return before - this.assigned;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int removeAll(final KTypePredicate<? super KType> predicate)
    {
        final int before = this.assigned;

        final KType [] keys = this.keys;
        final boolean [] states = this.allocated;

        for (int i = 0; i < states.length;)
        {
            if (states[i])
            {
                if (predicate.apply(keys[i]))
                {
                    assigned--;
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
     * {@inheritDoc}
     * 
     * <p> Use the following snippet of code to check for key existence
     * first and then retrieve the value if it exists.</p>
     * <pre>
     * if (map.containsKey(key))
     *   value = map.lget();
     * </pre>
     */
    @Override
    public VType get(final KType key)
    {
        final int mask = allocated.length - 1;

        /*! #if ($TemplateOptions.KTypeGeneric) !*/
        final HashingStrategy<KType> strategy = this.hashStrategy;
        int slot = Internals.rehashSpecificHash(key, perturbation, strategy) & mask;
        /*! #else
        int slot = rehash(key, perturbation) & mask;
        #end !*/
        final int wrappedAround = slot;
        while (allocated[slot])
        {
            if (/*! #if ($TemplateOptions.KTypeGeneric) !*/
                    Intrinsics.equalsKTypeHashStrategy(key, keys[slot], strategy)
                    /*! #else
                Intrinsics.equalsKType(key, keys[slot])
                #end !*/)
            {
                return values[slot];
            }

            slot = (slot + 1) & mask;
            if (slot == wrappedAround) break;
        }
        return this.defaultValue;
    }

    /* #if ($TemplateOptions.KTypeGeneric) */
    /**
     * Returns the last key stored in this has map for the corresponding
     * most recent call to {@link #containsKey}.
     * Precondition : {@link #containsKey} must have been called previously !
     * <p>Use the following snippet of code to check for key existence
     * first and then retrieve the key value if it exists.</p>
     * <pre>
     * if (map.containsKey(key))
     *   value = map.lkey();
     * </pre>
     * 
     * <p>This is equivalent to calling:</p>
     * <pre>
     * if (map.containsKey(key))
     *   key = map.keys[map.lslot()];
     * </pre>
     */
    public KType lkey()
    {
        return keys[lslot()];
    }
    /* #end */

    /**
     * Returns the last value saved in a call to {@link #containsKey}.
     * Precondition : {@link #containsKey} must have been called previously !
     * @see #containsKey
     */
    public VType lget()
    {
        assert lastSlot >= 0 : "Call containsKey() first.";
        assert allocated[lastSlot] : "Last call to exists did not have any associated value.";

        return values[lastSlot];
    }

    /**
     * Sets the value corresponding to the key saved in the last
     * call to {@link #containsKey}, if and only if the key exists
     * in the map already.
     * Precondition : {@link #containsKey} must have been called previously !
     * @see #containsKey
     * @return Returns the previous value stored under the given key.
     */
    public VType lset(final VType key)
    {
        assert lastSlot >= 0 : "Call containsKey() first.";
        assert allocated[lastSlot] : "Last call to exists did not have any associated value.";

        final VType previous = values[lastSlot];
        values[lastSlot] = key;
        return previous;
    }

    /**
     * @return Returns the slot of the last key looked up in a call to {@link #containsKey} if
     * it returned <code>true</code>.
     * 
     * @see #containsKey
     */
    public int lslot()
    {
        assert lastSlot >= 0 : "Call containsKey() first.";
        return lastSlot;
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Saves the associated value for fast access using {@link #lget}
     * or {@link #lset}.</p>
     * <pre>
     * if (map.containsKey(key))
     *   value = map.lget();
     * </pre>
     * or, to modify the value at the given key without looking up
     * its slot twice:
     * <pre>
     * if (map.containsKey(key))
     *   map.lset(map.lget() + 1);
     * </pre>
     * #if ($TemplateOptions.KTypeGeneric) or, to retrieve the key-equivalent object from the map:
     * <pre>
     * if (map.containsKey(key))
     *   map.lkey();
     * </pre>#end
     */
    @Override
    public boolean containsKey(final KType key)
    {
        final int mask = allocated.length - 1;

        /*! #if ($TemplateOptions.KTypeGeneric) !*/
        final HashingStrategy<KType> strategy = this.hashStrategy;
        int slot = Internals.rehashSpecificHash(key, perturbation, strategy) & mask;
        /*! #else int slot = rehash(key, perturbation) & mask;
        #end !*/
        final int wrappedAround = slot;
        while (allocated[slot])
        {
            if (/*! #if ($TemplateOptions.KTypeGeneric) !*/
                    Intrinsics.equalsKTypeHashStrategy(key, keys[slot], strategy)
                    /*! #else
                Intrinsics.equalsKType(key, keys[slot])
                #end !*/)
            {
                lastSlot = slot;
                return true;
            }
            slot = (slot + 1) & mask;
            if (slot == wrappedAround) break;
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

        // States are always cleared.
        Internals.blankBooleanArray(allocated, 0, allocated.length);

        /*! #if ($TemplateOptions.KTypeGeneric) !*/
        //Faster than Arrays.fill(keys, null); // Help the GC.
        Internals.blankObjectArray(keys, 0, keys.length);
        /*! #end !*/

        /*! #if ($TemplateOptions.VTypeGeneric) !*/
        //Faster than Arrays.fill(values, null); // Help the GC.
        Internals.blankObjectArray(values, 0, values.length);
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
     * 
     * <p>Note that an empty container may still contain many deleted keys (that occupy buffer
     * space). Adding even a single element to such a container may cause rehashing.</p>
     */
    @Override
    public boolean isEmpty()
    {
        return size() == 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        int h = 0;
        for (final KTypeVTypeCursor<KType, VType> c : this)
        {
            /*! #if ($TemplateOptions.KTypeGeneric) !*/
            //This hash is an intrinsic property of the container contents,
            //consequently is independent from the HashStrategy, so do not use it !
            /*! #end !*/
            h += Internals.rehash(c.key) + Internals.rehash(c.value);
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
    @SuppressWarnings({"rawtypes" })
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
            if (obj == this) return true;

            /*! #if ($TemplateOptions.KTypeGeneric) !*/
            if(obj instanceof KTypeVTypeOpenHashMap &&
                    !Intrinsics.equalsKType(this.hashStrategy, ((KTypeVTypeOpenHashMap) obj).hashStrategy)) {

                return false;
            }
            /*! #end !*/

            if (obj instanceof KTypeVTypeMap)
            {
                /* #if ($TemplateOptions.AnyGeneric) */
                @SuppressWarnings("unchecked")
                final
                /* #end */
                KTypeVTypeMap<KType, VType> other = (KTypeVTypeMap<KType, VType>) obj;
                if (other.size() == this.size())
                {
                    final EntryIterator it = this.iterator();

                    while (it.hasNext())
                    {
                        final KTypeVTypeCursor<KType, VType> c = it.next();

                        if (other.containsKey(c.key))
                        {
                            final VType v = other.get(c.key);
                            if (Intrinsics.equalsVType(c.value, v))
                            {
                                continue;
                            }
                        }
                        //recycle
                        it.release();
                        return false;
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
    public final class EntryIterator extends AbstractIterator<KTypeVTypeCursor<KType, VType>>
    {
        public final KTypeVTypeCursor<KType, VType> cursor;

        public EntryIterator()
        {
            cursor = new KTypeVTypeCursor<KType, VType>();
            cursor.index = -1;
        }

        @Override
        protected KTypeVTypeCursor<KType, VType> fetch()
        {
            int i = cursor.index + 1;
            final int max = keys.length;
            while (i < max && !allocated[i])
            {
                i++;
            }

            if (i == max)
                return done();

            cursor.index = i;
            cursor.key = keys[i];
            cursor.value = values[i];

            return cursor;
        }
    }

    /**
     * internal pool of EntryIterator
     */
    protected final  IteratorPool<KTypeVTypeCursor<KType, VType>, EntryIterator> entryIteratorPool = new IteratorPool<KTypeVTypeCursor<KType, VType>, EntryIterator>(
            new ObjectFactory<EntryIterator>() {

                @Override
                public EntryIterator create() {

                    return new EntryIterator();
                }

                @Override
                public void initialize(final EntryIterator obj) {
                    obj.cursor.index = -1;
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
    public <T extends KTypeVTypeProcedure<? super KType, ? super VType>> T forEach(final T procedure)
    {
        final KType [] keys = this.keys;
        final VType [] values = this.values;
        final boolean [] states = this.allocated;

        for (int i = 0; i < states.length; i++)
        {
            if (states[i])
                procedure.apply(keys[i], values[i]);
        }

        return procedure;
    }

    /**
     * Returns a specialized view of the keys of this associated container.
     * The view additionally implements {@link ObjectLookupContainer}.
     */
    @Override
    public KeysContainer keys()
    {
        return new KeysContainer();
    }

    /**
     * A view of the keys inside this hash map.
     */
    public final class KeysContainer
    extends AbstractKTypeCollection<KType> implements KTypeLookupContainer<KType>
    {
        private final KTypeVTypeOpenHashMap<KType, VType> owner =
                KTypeVTypeOpenHashMap.this;

        @Override
        public boolean contains(final KType e)
        {
            return containsKey(e);
        }

        @Override
        public <T extends KTypeProcedure<? super KType>> T forEach(final T procedure)
        {
            final KType [] localKeys = owner.keys;
            final boolean [] localStates = owner.allocated;

            for (int i = 0; i < localStates.length; i++)
            {
                if (localStates[i])
                    procedure.apply(localKeys[i]);
            }

            return procedure;
        }

        @Override
        public <T extends KTypePredicate<? super KType>> T forEach(final T predicate)
        {
            final KType [] localKeys = owner.keys;
            final boolean [] localStates = owner.allocated;

            for (int i = 0; i < localStates.length; i++)
            {
                if (localStates[i])
                {
                    if (!predicate.apply(localKeys[i]))
                        break;
                }
            }

            return predicate;
        }

        @Override
        public KeysIterator iterator()
        {
            //return new KeysIterator();
            return this.keyIteratorPool.borrow();
        }

        @Override
        public int size()
        {
            return owner.size();
        }

        @Override
        public void clear()
        {
            owner.clear();
        }

        @Override
        public int removeAll(final KTypePredicate<? super KType> predicate)
        {
            return owner.removeAll(predicate);
        }

        @Override
        public int removeAllOccurrences(final KType e)
        {
            final boolean hasKey = owner.containsKey(e);
            int result = 0;
            if (hasKey)
            {
                owner.remove(e);
                result = 1;
            }
            return result;
        }

        /**
         * internal pool of KeysIterator
         */
        protected final  IteratorPool<KTypeCursor<KType>, KeysIterator> keyIteratorPool = new IteratorPool<KTypeCursor<KType>, KeysIterator>(
                new ObjectFactory<KeysIterator>() {

                    @Override
                    public KeysIterator create() {

                        return new KeysIterator();
                    }

                    @Override
                    public void initialize(final KeysIterator obj) {
                        obj.cursor.index = -1;
                    }
                });

        @Override
        public KType[] toArray(final KType[] target)
        {
            final boolean[] alloc = owner.allocated;

            int count = 0;
            for (int i = 0; i < keys.length; i++)
            {
                if (alloc[i])
                {
                    target[count++] = owner.keys[i];
                }
            }

            assert count == owner.assigned;
            return target;
        }
    };

    /**
     * An iterator over the set of assigned keys.
     */
    public final class KeysIterator extends AbstractIterator<KTypeCursor<KType>>
    {
        public final KTypeCursor<KType> cursor;

        public KeysIterator()
        {
            cursor = new KTypeCursor<KType>();
            cursor.index = -1;
        }

        @Override
        protected KTypeCursor<KType> fetch()
        {
            int i = cursor.index + 1;
            final int max = keys.length;
            while (i < max && !allocated[i])
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
     * @return Returns a container with all values stored in this map.
     */
    @Override
    public ValuesContainer values()
    {
        return new ValuesContainer();
    }

    /**
     * A view over the set of values of this map.
     */
    public final class ValuesContainer extends AbstractKTypeCollection<VType>
    {
        private final KTypeVTypeOpenHashMap<KType, VType> owner =
                KTypeVTypeOpenHashMap.this;

        @Override
        public int size()
        {
            return owner.size();
        }

        @Override
        public boolean contains(final VType value)
        {
            // This is a linear scan over the values, but it's in the contract, so be it.
            final boolean [] allocated = owner.allocated;
            final VType [] values = owner.values;

            for (int slot = 0; slot < allocated.length; slot++)
            {
                if (allocated[slot] && Intrinsics.equalsVType(value, values[slot]))
                {
                    return true;
                }
            }
            return false;
        }

        @Override
        public <T extends KTypeProcedure<? super VType>> T forEach(final T procedure)
        {
            final boolean [] allocated = owner.allocated;
            final VType [] values = owner.values;

            for (int i = 0; i < allocated.length; i++)
            {
                if (allocated[i])
                    procedure.apply(values[i]);
            }

            return procedure;
        }

        @Override
        public <T extends KTypePredicate<? super VType>> T forEach(final T predicate)
        {
            final boolean [] allocated = owner.allocated;
            final VType [] values = owner.values;

            for (int i = 0; i < allocated.length; i++)
            {
                if (allocated[i])
                {
                    if (!predicate.apply(values[i]))
                        break;
                }
            }

            return predicate;
        }

        @Override
        public ValuesIterator iterator()
        {
            // return new ValuesIterator();
            return valuesIteratorPool.borrow();
        }

        @Override
        public int removeAllOccurrences(final VType e)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public int removeAll(final KTypePredicate<? super VType> predicate)
        {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear()
        {
            throw new UnsupportedOperationException();
        }

        /**
         * internal pool of ValuesIterator
         */
        protected final  IteratorPool<KTypeCursor<VType>, ValuesIterator> valuesIteratorPool = new IteratorPool<KTypeCursor<VType>, ValuesIterator>(
                new ObjectFactory<ValuesIterator>() {

                    @Override
                    public ValuesIterator create() {

                        return new ValuesIterator();
                    }

                    @Override
                    public void initialize(final ValuesIterator obj) {
                        obj.cursor.index = -1;
                    }
                });

        @Override
        public VType[] toArray(final VType[] target)
        {
            final boolean[] alloc = owner.allocated;

            int count = 0;
            for (int i = 0; i < values.length; i++)
            {
                if (alloc[i])
                {
                    target[count++] = owner.values[i];
                }
            }

            assert count == owner.assigned;
            return target;
        }
    }

    /**
     * An iterator over the set of assigned values.
     */
    public final class ValuesIterator extends AbstractIterator<KTypeCursor<VType>>
    {
        public final KTypeCursor<VType> cursor;

        public ValuesIterator()
        {
            cursor = new KTypeCursor<VType>();
            cursor.index = -1;
        }

        @Override
        protected KTypeCursor<VType> fetch()
        {
            int i = cursor.index + 1;
            final int max = keys.length;
            while (i < max && !allocated[i])
            {
                i++;
            }

            if (i == max)
                return done();

            cursor.index = i;
            cursor.value = values[i];

            return cursor;
        }
    }

    /**
     * Clone this object.
     * #if ($TemplateOptions.AnyGeneric)
     * The returned clone will use the same HashingStrategy strategy.
     * #end
     */
    @Override
    public KTypeVTypeOpenHashMap<KType, VType> clone()
    {
        /* #if ($TemplateOptions.AnyGeneric) */
        @SuppressWarnings("unchecked")
        final
        /* #end */
        KTypeVTypeOpenHashMap<KType, VType> cloned =
        new KTypeVTypeOpenHashMap<KType, VType>((int) (this.keys.length * this.loadFactor) + 1, this.loadFactor
                /* #if ($TemplateOptions.KTypeGeneric) */
                , this.hashStrategy
                /* #end */);

        cloned.putAll(this);

        cloned.defaultValue = this.defaultValue;

        return cloned;

    }

    /**
     * Convert the contents of this map to a human-friendly string.
     */
    @Override
    public String toString()
    {
        final StringBuilder buffer = new StringBuilder();
        buffer.append("[");

        boolean first = true;
        for (final KTypeVTypeCursor<KType, VType> cursor : this)
        {
            if (!first) buffer.append(", ");
            buffer.append(cursor.key);
            buffer.append("=>");
            buffer.append(cursor.value);
            first = false;
        }
        buffer.append("]");
        return buffer.toString();
    }

    /**
     * Creates a hash map from two index-aligned arrays of key-value pairs.
     */
    public static <KType, VType> KTypeVTypeOpenHashMap<KType, VType> from(final KType [] keys, final VType [] values)
    {
        if (keys.length != values.length)
            throw new IllegalArgumentException("Arrays of keys and values must have an identical length.");

        final KTypeVTypeOpenHashMap<KType, VType> map = new KTypeVTypeOpenHashMap<KType, VType>();
        for (int i = 0; i < keys.length; i++)
        {
            map.put(keys[i], values[i]);
        }
        return map;
    }

    /**
     * Create a hash map from another associative container.
     */
    public static <KType, VType> KTypeVTypeOpenHashMap<KType, VType> from(final KTypeVTypeAssociativeContainer<KType, VType> container)
    {
        return new KTypeVTypeOpenHashMap<KType, VType>(container);
    }

    /**
     * Create a new hash map without providing the full generic signature (constructor
     * shortcut).
     */
    public static <KType, VType> KTypeVTypeOpenHashMap<KType, VType> newInstance()
    {
        return new KTypeVTypeOpenHashMap<KType, VType>();
    }

    /**
     * Create a new hash map with no key perturbations, and default parameters (see
     * {@link #computePerturbationValue(int)}). This may lead to increased performance, but only use when sure the container will not
     * be used for direct copying of keys to another hash container.
     */
    public static <KType, VType> KTypeVTypeOpenHashMap<KType, VType> newInstanceWithoutPerturbations()
    {
        return new KTypeVTypeOpenHashMap<KType, VType>() {
            @Override
            protected final int computePerturbationValue(final int capacity)
            {
                return 0;
            }
        };
    }

    /**
     * Create a new hash map with initial capacity and load factor control. (constructor
     * shortcut).
     */
    public static <KType, VType> KTypeVTypeOpenHashMap<KType, VType> newInstance(final int initialCapacity, final float loadFactor)
    {
        return new KTypeVTypeOpenHashMap<KType, VType>(initialCapacity, loadFactor);
    }

    /**
     * Create a new hash map with initial capacity and load factor control, with no key perturbations. (see
     * {@link #computePerturbationValue(int)}). This may lead to increased performance, but only use when sure the container will not
     * be used for direct copying of keys to another hash container.
     */
    public static <KType, VType> KTypeVTypeOpenHashMap<KType, VType> newInstanceWithoutPerturbations(final int initialCapacity, final float loadFactor)
    {
        return new KTypeVTypeOpenHashMap<KType, VType>(initialCapacity, loadFactor) {
            @Override
            protected final int computePerturbationValue(final int capacity)
            {
                return 0;
            }
        };
    }

    /* #if ($TemplateOptions.KTypeGeneric) */
    /**
     * Create a new hash map with full parameter control, using a specific hash strategy.
     * A strategy = null is equivalent at providing no strategy at all.
     */
    public static <KType, VType> KTypeVTypeOpenHashMap<KType, VType> newInstance(final int initialCapacity, final float loadFactor, final HashingStrategy<KType> strategy)
    {
        return new KTypeVTypeOpenHashMap<KType, VType>(initialCapacity, loadFactor, strategy);
    }

    /**
     * Create a new hash map with full parameter control, using a specific hash strategy, with no key perturbations (see
     * {@link #computePerturbationValue(int)}).
     * A strategy = null is equivalent at providing no strategy at all.
     * This may lead to increased performance, but only use when sure the container will not
     * be used for direct copying of keys to another hash container.
     */
    public static <KType, VType> KTypeVTypeOpenHashMap<KType, VType> newInstanceWithoutPerturbations(final int initialCapacity, final float loadFactor, final HashingStrategy<KType> strategy)
    {
        return new KTypeVTypeOpenHashMap<KType, VType>(initialCapacity, loadFactor, strategy) {
            @Override
            protected final int computePerturbationValue(final int capacity)
            {
                return 0;
            }
        };
    }

    /**
     * Return the current {@link HashingStrategy} in use, or {@code null} if none was set.
     */
    public HashingStrategy<KType> strategy()
    {
        return this.hashStrategy;
    }

    /**
     * Returns the "default value" value used
     * in containers methods returning "default value"
     * @return
     */
    public VType getDefaultValue()
    {
        return defaultValue;
    }

    /**
     * Set the "default value" value to be used
     * in containers methods returning "default value"
     * @return
     */
    public void setDefaultValue(final VType defaultValue)
    {
        this.defaultValue = defaultValue;
    }

    /* #end */
}
