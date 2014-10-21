package com.carrotsearch.hppc;

import java.util.*;

import com.carrotsearch.hppc.cursors.*;
import com.carrotsearch.hppc.hash.MurmurHash3;
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
 * {@link #keys}) are always allocated to the nearest size that is a power of two. When
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
 * </tbody>
 * </table>
#else
 * <p>See {@link ObjectObjectOpenHashMap} class for API similarities and differences against Java
 * Collections.  
#end
 * 
#if ($TemplateOptions.KTypeGeneric)
 * <p>This implementation supports <code>null</code> keys.</p>
#end
#if ($TemplateOptions.VTypeGeneric)
 * <p>This implementation supports <code>null</code> values.</p>
#end
 * 
 * <p><b>Important node.</b> The implementation uses power-of-two tables and linear
 * probing, which may cause poor performance (many collisions) if hash values are
 * not properly distributed. This implementation uses rehashing 
 * using {@link MurmurHash3}.</p>
 * 
 * @author This code is inspired by the collaboration and implementation in the <a
 *         href="http://fastutil.dsi.unimi.it/">fastutil</a> project.
 */
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

    /**
     * Hash-indexed array holding all keys.
     *
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
     * 
     * @see #values
     */
    public KType [] keys;

    /**
     * Hash-indexed array holding all values associated to the keys
     * stored in {@link #keys}.
     * 
#if ($TemplateOptions.KTypeGeneric)
     * <p><strong>Important!</strong> 
     * The actual value in this field is always an instance of <code>Object[]</code>.
     * Be warned that <code>javac</code> emits additional casts when <code>values</code> 
     * are directly accessed; <strong>these casts
     * may result in exceptions at runtime</strong>. A workaround is to cast directly to
     * <code>Object[]</code> before accessing the buffer's elements (although it is highly
     * recommended to use a {@link #iterator()} instead.
     * </pre>
#end
     * 
     * @see #keys
     */
    public VType [] values;

    /**
     * True if key = 0 is in the map.
     * 
     */
    public boolean allocatedDefaultKey = false;

    /**
     * if allocatedDefaultKey = true, contains the associated VType to the key = 0
     */
    public VType defaultKeyValue;

    /**
     * Cached number of assigned slots in {@link #keys}.
     */
    public int assigned;

    /**
     * The load factor for this map (fraction of allocated slots
     * before the buffers must be rehashed or reallocated).
     */
    public final float loadFactor;

    /**
     * Resize buffers when {@link #keys} hits this value. 
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

    /**
     * Creates a hash map with the default capacity of {@value #DEFAULT_CAPACITY},
     * load factor of {@value #DEFAULT_LOAD_FACTOR}.
     * 
     * <p>See class notes about hash distribution importance.</p>
     */
    public KTypeVTypeOpenHashMap()
    {
        this(DEFAULT_CAPACITY);
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
    public KTypeVTypeOpenHashMap(int initialCapacity)
    {
        this(initialCapacity, DEFAULT_LOAD_FACTOR);
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
    public KTypeVTypeOpenHashMap(int initialCapacity, float loadFactor)
    {
        initialCapacity = Math.max(initialCapacity, MIN_CAPACITY);

        assert initialCapacity > 0
            : "Initial capacity must be between (0, " + Integer.MAX_VALUE + "].";
        assert loadFactor > 0 && loadFactor <= 1
            : "Load factor must be between (0, 1].";

        this.loadFactor = loadFactor;
        allocateBuffers(roundCapacity(initialCapacity));
    }
    
    /**
     * Create a hash map from all key-value pairs of another container.
     */
    public KTypeVTypeOpenHashMap(KTypeVTypeAssociativeContainer<KType, VType> container)
    {
        this((int)(container.size() * (1 + DEFAULT_LOAD_FACTOR)));
        putAll(container);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public VType put(KType key, VType value)
    {
     
         if (Intrinsics.equalsKTypeDefault(key)) {

            if (this.allocatedDefaultKey) {

                VType previousValue = this.defaultKeyValue;
                this.defaultKeyValue = value;

                return previousValue;
            }

            this.defaultKeyValue = value;
            this.allocatedDefaultKey = true;

            return Intrinsics.<VType> defaultVTypeValue();
        }

        final int mask = keys.length - 1;

        int slot = rehash(key, perturbation) & mask;

        while (! Intrinsics.equalsKTypeDefault(keys[slot]))
        {
            if (Intrinsics.equalsKType(key, keys[slot]))
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
        
            keys[slot] = key;                
            values[slot] = value;
        }
        return Intrinsics.<VType> defaultVTypeValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int putAll(
        KTypeVTypeAssociativeContainer<? extends KType, ? extends VType> container)
    {
        final int count = this.size();
        for (KTypeVTypeCursor<? extends KType, ? extends VType> c : container)
        {
            put(c.key, c.value);
        }
        return this.size() - count;
    }

    /**
     * Puts all key/value pairs from a given iterable into this map.
     */
    @Override
    public int putAll(
        Iterable<? extends KTypeVTypeCursor<? extends KType, ? extends VType>> iterable)
    {
        final int count = this.size();
        for (KTypeVTypeCursor<? extends KType, ? extends VType> c : iterable)
        {
            put(c.key, c.value);
        }
        return this.size() - count;
    }

    /**
     * <a href="http://trove4j.sourceforge.net">Trove</a>-inspired API method. An equivalent
     * of the following code:
     * <pre>
     * if (!map.containsKey(key)) map.put(value);
     * </pre>
     * 
     * <p>This method saves to {@link #lastSlot} as a side effect of each call.</p>
     * 
     * @param key The key of the value to check.
     * @param value The value to put if <code>key</code> does not exist.
     * @return <code>true</code> if <code>key</code> did not exist and <code>value</code>
     * was placed in the map.
     */
    public boolean putIfAbsent(KType key, VType value)
    {
        if (!containsKey(key))
        {
            put(key, value);
            return true;
        }
        return false;
    }

    /*! #if ($TemplateOptions.VTypePrimitive) !*/ 
    /**
     * <a href="http://trove4j.sourceforge.net">Trove</a>-inspired API method. A logical 
     * equivalent of the following code (but does not update {@link #lastSlot}):
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
    /*! #end !*/
    /*! #if ($TemplateOptions.VTypePrimitive) 
    public VType putOrAdd(KType key, VType putValue, VType additionValue)
    {
          if (Intrinsics.equalsKTypeDefault(key)) {

            if (this.allocatedDefaultKey) {

                this.defaultKeyValue += additionValue;

                return this.defaultKeyValue;
            }

            this.defaultKeyValue = putValue;

            this.allocatedDefaultKey = true;

            return putValue;
        }

        final int mask = keys.length - 1;
        int slot = rehash(key, perturbation) & mask;
        
        while (! Intrinsics.equalsKTypeDefault(keys[slot]))
        {
            if (Intrinsics.equalsKType(key, keys[slot]))
            {
                return values[slot] = (VType) (values[slot] + additionValue);
            }

            slot = (slot + 1) & mask;
        }

        if (assigned == resizeAt) {
            expandAndPut(key, putValue, slot);
        } else {
            assigned++;
            
            keys[slot] = key;                
            values[slot] = putValue;
        }
        return putValue;
    }
    #end !*/

    /*! #if ($TemplateOptions.VTypePrimitive) !*/ 
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
    /*! #end !*/
    /*! #if ($TemplateOptions.VTypePrimitive) 
    public VType addTo(KType key, VType additionValue)
    {
        return putOrAdd(key, additionValue, additionValue);
    }
    #end !*/

    /**
     * Expand the internal storage buffers (capacity) and rehash.
     */
    private void expandAndPut(KType pendingKey, VType pendingValue, int freeSlot)
    {
        //default sentinel value is never in the keys[] array, so never trigger reallocs
        assert !Intrinsics.equalsKTypeDefault(pendingKey);

        // Try to allocate new buffers first. If we OOM, it'll be now without
        // leaving the data structure in an inconsistent state.
        final KType   [] oldKeys      = this.keys;
        final VType   [] oldValues    = this.values;
       
        allocateBuffers(nextCapacity(keys.length));

        // We have succeeded at allocating new data so insert the pending key/value at
        // the free slot in the old arrays before rehashing.
        lastSlot = -1;
        assigned++;
       
        oldKeys[freeSlot] = pendingKey;
        oldValues[freeSlot] = pendingValue;
        
        // Rehash all stored keys into the new buffers.
        final KType []   keys = this.keys;
        final VType []   values = this.values;
        

        final int mask = keys.length - 1;
        
        for (int i = oldKeys.length; --i >= 0;)
        {
            if (! Intrinsics.equalsKTypeDefault(oldKeys[i]))
            {
                final KType k = oldKeys[i];
                final VType v = oldValues[i];

                int slot = rehash(k, perturbation) & mask;

                while (!Intrinsics.equalsKTypeDefault(keys[slot]))
                {
                    slot = (slot + 1) & mask;
                }

                keys[slot] = k;                
                values[slot] = v;
            }
        }

        /* #if ($TemplateOptions.KTypeGeneric) */ Arrays.fill(oldKeys,   null); /* #end */
        /* #if ($TemplateOptions.VTypeGeneric) */ Arrays.fill(oldValues, null); /* #end */
    }

    /**
     * Allocate internal buffers for a given capacity.
     * 
     * @param capacity New capacity (must be a power of two).
     */
    private void allocateBuffers(int capacity)
    {
        KType [] keys = Intrinsics.newKTypeArray(capacity);
        VType [] values = Intrinsics.newVTypeArray(capacity);
       
        this.keys = keys;
        this.values = values;
       
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
    protected int computePerturbationValue(int capacity)
    {
        return PERTURBATIONS[Integer.numberOfLeadingZeros(capacity)];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public VType remove(KType key)
    {
         if (Intrinsics.equalsKTypeDefault(key)) {

            if (this.allocatedDefaultKey) {

                VType previousValue = this.defaultKeyValue;

                this.allocatedDefaultKey = false;
                return previousValue;
            }

            return Intrinsics.<VType> defaultVTypeValue();
        }

        final int mask = keys.length - 1;
        int slot = rehash(key, perturbation) & mask; 

        while (!Intrinsics.equalsKTypeDefault(keys[slot]))
        {
            if (Intrinsics.equalsKType(key, keys[slot]))
             {
                assigned--;
                VType v = values[slot];
                shiftConflictingKeys(slot);
                return v;
             }
             slot = (slot + 1) & mask;
        }

        return Intrinsics.<VType> defaultVTypeValue();
    }

    /**
     * Shift all the slot-conflicting keys allocated to (and including) <code>slot</code>. 
     */
    protected void shiftConflictingKeys(int slotCurr)
    {
        // Copied nearly verbatim from fastutil's impl.
        final int mask = keys.length - 1;
        int slotPrev, slotOther;
        while (true)
        {
            slotCurr = ((slotPrev = slotCurr) + 1) & mask;

            while (!Intrinsics.equalsKTypeDefault(keys[slotCurr]))
            {
                slotOther = rehash(keys[slotCurr], perturbation) & mask;
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

            if (Intrinsics.equalsKTypeDefault(keys[slotCurr])) 
                break;

            // Shift key/value pair.
            keys[slotPrev] = keys[slotCurr];           
            values[slotPrev] = values[slotCurr];           
        }

        keys[slotPrev] = Intrinsics.<KType> defaultKTypeValue(); 
      
        /* #if ($TemplateOptions.VTypeGeneric) */
        values[slotPrev] = Intrinsics.<VType> defaultVTypeValue(); 
        /* #end */
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int removeAll(KTypeContainer<? extends KType> container)
    {
        final int before = this.size();

        for (KTypeCursor<? extends KType> cursor : container)
        {
            remove(cursor.value);
        }

        return before - this.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int removeAll(KTypePredicate<? super KType> predicate)
    {
        final int before = this.size();

        if (this.allocatedDefaultKey) {

            if (predicate.apply(Intrinsics.<KType> defaultKTypeValue()))
            {
                 this.allocatedDefaultKey = false;
            }
        }

        final KType [] keys = this.keys;
       
        for (int i = 0; i < keys.length;)
        {
            if (!Intrinsics.equalsKTypeDefault(keys[i]))
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
        return before - this.size();
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Use the following snippet of code to check for key existence
     * first and then retrieve the value if it exists.</p>
     * <pre>
     * if (map.containsKey(key))
     *   value = map.lget(); 
     * </pre>
     * <p>The above code <strong>cannot</strong> be used by multiple concurrent
     * threads because a call to {@link #containsKey} stores
     * the temporary slot number in {@link #lastSlot}. An alternative to the above
     * conditional statement is to use {@link #getOrDefault} and
     * provide a custom default value sentinel (not present in the value set).</p>
     */
    @Override
    public VType get(KType key)
    {
         if (Intrinsics.equalsKTypeDefault(key)) {

            if (this.allocatedDefaultKey) {

                return this.defaultKeyValue;
            }

            return Intrinsics.<VType> defaultVTypeValue();
        }

        // Same as:
        // getOrDefault(key, Intrinsics.<VType> defaultVTypeValue())
        // but let's keep it duplicated for VMs that don't have advanced inlining.
        final int mask = keys.length - 1;
        int slot = rehash(key, perturbation) & mask;

        while (!Intrinsics.equalsKTypeDefault(keys[slot]))
        {
            if (Intrinsics.equalsKType(key, keys[slot]))
            {
                return values[slot]; 
            }
            
            slot = (slot + 1) & mask;
        }
        return Intrinsics.<VType> defaultVTypeValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public VType getOrDefault(KType key, VType defaultValue)
    {
        if (Intrinsics.equalsKTypeDefault(key)) {

            if (this.allocatedDefaultKey) {

                return this.defaultKeyValue;
            }

            return defaultValue;
        }

        final int mask = keys.length - 1;

        int slot = rehash(key, perturbation) & mask;
        
        while (!Intrinsics.equalsKTypeDefault(keys[slot]))
        {
            if (Intrinsics.equalsKType(key, keys[slot]))
            {
                return values[slot]; 
            }
            
            slot = (slot + 1) & mask;
        }
        return defaultValue;
    }

    /* #if ($TemplateOptions.KTypeGeneric) */
    /**
     * Returns the last key stored in this has map for the corresponding
     * most recent call to {@link #containsKey}.
     * 
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
        if (this.lastSlot == -2) {

            return Intrinsics.<KType> defaultKTypeValue();
        }

        assert this.lastSlot >= 0 : "Call containsKey() first.";
        assert ! Intrinsics.equalsKTypeDefault(this.keys[lastSlot]) : "Last call to exists did not have any associated value.";
        return keys[lslot()];
    }
    /* #end */

    /**
     * Returns the last value saved in a call to {@link #containsKey}.
     * 
     * @see #containsKey
     */
    public VType lget()
    {
        if (this.lastSlot == -2) {

            return this.defaultKeyValue;
        }

        assert lastSlot >= 0 : "Call containsKey() first.";
        assert !Intrinsics.equalsKTypeDefault(this.keys[lastSlot]) : "Last call to exists did not have any associated value.";
    
        return values[lastSlot];
    }

    /**
     * Sets the value corresponding to the key saved in the last
     * call to {@link #containsKey}, if and only if the key exists
     * in the map already.
     * 
     * @see #containsKey
     * @return Returns the previous value stored under the given key.
     */
    public VType lset(VType value)
    {
         if (this.lastSlot == -2) {

            VType previous = this.defaultKeyValue;
            this.defaultKeyValue = value;
            return previous;
        }

        assert lastSlot >= 0 : "Call containsKey() first.";
        assert ! Intrinsics.equalsKTypeDefault(this.keys[lastSlot]) : "Last call to exists did not have any associated value.";

        final VType previous = values[lastSlot];
        values[lastSlot] = value;
        return previous;
    }

    /**
     * @return Returns the slot of the last key looked up in a call to {@link #containsKey} if
     * it returned <code>true</code>.
      * or else -2 if {@link #containsKey} were successful on key = 0
     * @see #containsKey
     */
    public int lslot()
    {
        assert lastSlot >= 0 || this.lastSlot == -2 : "Call containsKey() first.";
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
     * 
     * #if ($TemplateOptions.KTypeGeneric)
     * <p><strong>Important:</strong> {@link #containsKey} and consecutive {@link #lget}, {@link #lset}
     * or {@link #lkey} must not be used by concurrent threads because {@link #lastSlot} is 
     * used to store state.</p>
     * #end
     */
    @Override
    public boolean containsKey(KType key)
    {
        if (Intrinsics.equalsKTypeDefault(key)) {

            if (this.allocatedDefaultKey) {
                this.lastSlot = -2;
            } else {
                this.lastSlot = -1;
            }

            return this.allocatedDefaultKey;
        }

        final int mask = keys.length - 1;

        int slot = rehash(key, perturbation) & mask;

        while (!Intrinsics.equalsKTypeDefault(keys[slot]))
        {
            if (Intrinsics.equalsKType(key, keys[slot]))
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

        Arrays.fill(keys, Intrinsics.<KType> defaultKTypeValue()); // Help the GC and mark as not allocated
      
        /* #if ($TemplateOptions.VTypeGeneric) */
        Arrays.fill(values, null); // Help the GC.
        /* #end */
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int size()
    {
        return assigned + (this.allocatedDefaultKey?1:0) ;
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Note that an empty container may still contain many deleted keys (that occupy buffer
     * space). Adding even a single element to such a container may cause rehashing.</p>
     */
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

        if (this.allocatedDefaultKey) {
            h +=  Internals.rehash(Intrinsics.<KType> defaultKTypeValue()) + Internals.rehash(this.defaultKeyValue);
        }

        for (KTypeVTypeCursor<KType, VType> c : this)
        {
            h += rehash(c.key) + rehash(c.value);
        }
        return h;
    }

    /**
     * {@inheritDoc} 
     */
    @Override
    public boolean equals(Object obj)
    {
        if (obj != null)
        {
            if (obj == this) return true;

            if (obj instanceof KTypeVTypeMap)
            {
                /* #if ($TemplateOptions.AnyGeneric) */
                @SuppressWarnings("unchecked")
                /* #end */
                KTypeVTypeMap<KType, VType> other = (KTypeVTypeMap<KType, VType>) obj;
                if (other.size() == this.size())
                {
                    for (KTypeVTypeCursor<KType, VType> c : this)
                    {
                        if (other.containsKey(c.key))
                        {
                            VType v = other.get(c.key);
                            if (Intrinsics.equalsVType(c.value, v))
                            {
                                continue;
                            }
                        }
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
    private final class EntryIterator extends AbstractIterator<KTypeVTypeCursor<KType, VType>>
    {
        private final KTypeVTypeCursor<KType, VType> cursor;

        public EntryIterator()
        {
            cursor = new KTypeVTypeCursor<KType, VType>();
            cursor.index = -2;
        }

        @Override
        protected KTypeVTypeCursor<KType, VType> fetch()
        {

            if (this.cursor.index == -2) {

                   if (KTypeVTypeOpenHashMap.this.allocatedDefaultKey) {

                       this.cursor.index = -1;
                       this.cursor.key = Intrinsics.<KType> defaultKTypeValue();
                       this.cursor.value = KTypeVTypeOpenHashMap.this.defaultKeyValue;

                       return this.cursor;

                   } else {
                       //no value associated with the default key, continue iteration...
                       this.cursor.index = -1;
                   }
               }

            int i = cursor.index + 1;
            final int max = keys.length;

            while (i < max && Intrinsics.equalsKTypeDefault(keys[i]))
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
     * {@inheritDoc}
     */
    @Override
    public Iterator<KTypeVTypeCursor<KType, VType>> iterator()
    {
        return new EntryIterator();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends KTypeVTypeProcedure<? super KType, ? super VType>> T forEach(T procedure)
    {
        final KType [] keys = this.keys;
        final VType [] values = this.values;
       
        for (int i = 0; i < keys.length; i++)
        {
            if (!Intrinsics.equalsKTypeDefault(keys[i]))
                procedure.apply(keys[i], values[i]);
        }
        
        return procedure;
    }

    /**
     * Returns a specialized view of the keys of this associated container. 
     * The view additionally implements {@link ObjectLookupContainer}.
     */
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
        public boolean contains(KType e)
        {
            return containsKey(e);
        }
        
        @Override
        public <T extends KTypeProcedure<? super KType>> T forEach(T procedure)
        {
            if (this.owner.allocatedDefaultKey) {

                 procedure.apply(Intrinsics.<KType>defaultKTypeValue());
            }

            final KType [] localKeys = owner.keys;
           
            for (int i = 0; i < localKeys.length; i++)
            {
                if (!Intrinsics.equalsKTypeDefault(localKeys[i]))
                    procedure.apply(localKeys[i]);
            }

            return procedure;
        }

        @Override
        public <T extends KTypePredicate<? super KType>> T forEach(T predicate)
        {
            if (this.owner.allocatedDefaultKey) {

                 if(! predicate.apply(Intrinsics.<KType> defaultKTypeValue())) {

                       return predicate;
                 }
            }

            final KType [] localKeys = owner.keys;
           
            for (int i = 0; i < localKeys.length; i++)
            {
                if (!Intrinsics.equalsKTypeDefault(localKeys[i]))
                {
                    if (!predicate.apply(localKeys[i]))
                        break;
                }
            }

            return predicate;
        }

        @Override
        public boolean isEmpty()
        {
            return owner.isEmpty();
        }

        @Override
        public Iterator<KTypeCursor<KType>> iterator()
        {
            return new KeysIterator();
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
        public int removeAll(KTypePredicate<? super KType> predicate)
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
    };
    
    /**
     * An iterator over the set of assigned keys.
     */
    private final class KeysIterator extends AbstractIterator<KTypeCursor<KType>>
    {
        private final KTypeCursor<KType> cursor;

        public KeysIterator()
        {
            cursor = new KTypeCursor<KType>();
            cursor.index = -2;
        }

        @Override
        protected KTypeCursor<KType> fetch()
        {
            if (this.cursor.index == -2) {

                if (KTypeVTypeOpenHashMap.this.allocatedDefaultKey) {

                    this.cursor.index = -1;
                    this.cursor.value = Intrinsics.<KType> defaultKTypeValue();

                    return this.cursor;

                } else {
                    //no value associated with the default key, continue iteration...
                    this.cursor.index = -1;
                }
            }

            int i = cursor.index + 1;
            final int max = keys.length;

            while (i < max && Intrinsics.equalsKTypeDefault(keys[i]))
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
    public KTypeContainer<VType> values()                                                                                                    
    {                                                                                                                                         
        return new ValuesContainer();                                                                                                         
    }                                                                                                                                         

    /**                                                                                                                                       
     * A view over the set of values of this map.                                                                                                         
     */                                                                                                                                       
    private final class ValuesContainer extends AbstractKTypeCollection<VType>                                                               
    {                                                                                                                                         
        @Override                                                                                                                             
        public int size()                                                                                                                     
        {                                                                                                                                     
            return KTypeVTypeOpenHashMap.this.size();                                                                                       
        }                                                                                                                                     
                                                                                                                                              
        @Override                                                                                                                             
        public boolean isEmpty()                                                                                                              
        {                                                                                                                                     
            return KTypeVTypeOpenHashMap.this.isEmpty();                                                                                    
        }                                                                                                                                     
                                                                                                                                              
        @Override                                                                                                                             
        public boolean contains(VType value)                                                                                                  
        {
             if (KTypeVTypeOpenHashMap.this.allocatedDefaultKey && Intrinsics.equalsVType(value, KTypeVTypeOpenHashMap.this.defaultKeyValue)) {

                 return true;
             }
                                                                                                                                     
            // This is a linear scan over the values, but it's in the contract, so be it.                                                     
                                                                   
            final VType [] values = KTypeVTypeOpenHashMap.this.values;  
            final KType[] keys = KTypeVTypeOpenHashMap.this.keys;                                                                    
                                                                                                                                              
            for (int slot = 0; slot < keys.length; slot++)                                                                               
            {                                                                                                                                 
                if (!Intrinsics.equalsKTypeDefault(keys[slot]) && Intrinsics.equalsVType(value, values[slot]))                                    
                {                                                                                                                             
                    return true;                                                                                                              
                }                                                                                                                             
            }
            return false;                                                                                                                     
        }                                                                                                                                     
                                                                                                                                              
        @Override                                                                                                                             
        public <T extends KTypeProcedure<? super VType>> T forEach(T procedure)                                                              
        {
             if (KTypeVTypeOpenHashMap.this.allocatedDefaultKey) {

                  procedure.apply(KTypeVTypeOpenHashMap.this.defaultKeyValue);
            }
                                                                                                                                     
            final KType[] keys = KTypeVTypeOpenHashMap.this.keys;                                                              
            final VType [] values = KTypeVTypeOpenHashMap.this.values;                                                                      
                                                                                                                                              
            for (int i = 0; i < keys.length; i++)                                                                                        
            {                                                                                                                                 
                if (!Intrinsics.equalsKTypeDefault(keys[i]))                                                                                                             
                    procedure.apply(values[i]);                                                                                               
            }                                                                                                                                 
                                                                                                                                              
            return procedure;                                                                                                                 
        }                                                                                                                                     
                                                                                                                                              
        @Override                                                                                                                             
        public <T extends KTypePredicate<? super VType>> T forEach(T predicate)                                                              
        {

           if (KTypeVTypeOpenHashMap.this.allocatedDefaultKey) {

               if (!predicate.apply(KTypeVTypeOpenHashMap.this.defaultKeyValue))
               {
                   return predicate;
               }
            }
                                                                                                                                     
             final KType[] keys = KTypeVTypeOpenHashMap.this.keys;                                                               
            final VType [] values = KTypeVTypeOpenHashMap.this.values;                                                                      
                                                                                                                                              
            for (int i = 0; i < keys.length; i++)                                                                                        
            {                                                                                                                                 
                if (!Intrinsics.equalsKTypeDefault(keys[i]))                                                                                                             
                {                                                                                                                             
                    if (!predicate.apply(values[i]))                                                                                          
                        break;                                                                                                                
                }                                                                                                                             
            }                                                                                                                                 
                                                                                                                                              
            return predicate;                                                                                                                 
        }                                                                                                                                     
                                                                                                                                              
        @Override                                                                                                                             
        public Iterator<KTypeCursor<VType>> iterator()                                                                                       
        {                                                                                                                                     
            return new ValuesIterator();                                                                                                      
        }                                                                                                                                     

        @Override                                                                                                                             
        public int removeAllOccurrences(VType e)                                                                                              
        {                                                                                                                                     
            throw new UnsupportedOperationException();                                                                                        
        }                                                                                                                                     
                                                                                                                                              
        @Override                                                                                                                             
        public int removeAll(KTypePredicate<? super VType> predicate)                                                                        
        {                                                                                                                                     
            throw new UnsupportedOperationException();                                                                                        
        }                                                                                                                                     

        @Override                                                                                                                             
        public void clear()                                                                                                                   
        {                                                                                                                                     
            throw new UnsupportedOperationException();                                                                                        
        }                                                                                                                                     
    }                                                                                                                                         
                                                                                                                                              
    /**                                                                                                                                       
     * An iterator over the set of assigned values.                                                                                           
     */                                                                                                                                       
    private final class ValuesIterator extends AbstractIterator<KTypeCursor<VType>>                                                               
    {                                                                                                                                         
        private final KTypeCursor<VType> cursor;                                                                                             
                                                                                                                                              
        public ValuesIterator()                                                                                                               
        {                                                                                                                                     
            cursor = new KTypeCursor<VType>();                                                                                               
            cursor.index = -2;                                                                                                        
        }                                                                                                                                     
        
        @Override
        protected KTypeCursor<VType> fetch()
        {
           if (this.cursor.index == -2) {

                if (KTypeVTypeOpenHashMap.this.allocatedDefaultKey) {

                    this.cursor.index = -1;
                    this.cursor.value = KTypeVTypeOpenHashMap.this.defaultKeyValue;

                    return this.cursor;

                } else {
                    //no value associated with the default key, continue iteration...
                    this.cursor.index = -1;
                }
            }

            int i = cursor.index + 1;
            final int max = keys.length;

            while (i < max && Intrinsics.equalsKTypeDefault(keys[i]))
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
     * {@inheritDoc}
     */
    @Override
    public KTypeVTypeOpenHashMap<KType, VType> clone()
    {
        try
        {
            /* #if ($TemplateOptions.AnyGeneric) */
            @SuppressWarnings("unchecked")
            /* #end */
            KTypeVTypeOpenHashMap<KType, VType> cloned = 
                (KTypeVTypeOpenHashMap<KType, VType>) super.clone();

            cloned.allocatedDefaultKey = this.allocatedDefaultKey;
            cloned.defaultKeyValue = this.defaultKeyValue;
 
            cloned.keys = keys.clone();
            cloned.values = values.clone();
            
            return cloned;
        }
        catch (CloneNotSupportedException e)
        {
            throw new RuntimeException(e);
        }
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
        for (KTypeVTypeCursor<KType, VType> cursor : this)
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
    public static <KType, VType> KTypeVTypeOpenHashMap<KType, VType> from(KType [] keys, VType [] values)
    {
        if (keys.length != values.length) 
            throw new IllegalArgumentException("Arrays of keys and values must have an identical length."); 

        KTypeVTypeOpenHashMap<KType, VType> map = new KTypeVTypeOpenHashMap<KType, VType>();
        for (int i = 0; i < keys.length; i++)
        {
            map.put(keys[i], values[i]);
        }
        return map;
    }

    /**
     * Create a hash map from another associative container.
     */
    public static <KType, VType> KTypeVTypeOpenHashMap<KType, VType> from(KTypeVTypeAssociativeContainer<KType, VType> container)
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
     * Returns a new object with no key perturbations (see
     * {@link #computePerturbationValue(int)}). Only use when sure the container will not
     * be used for direct copying of keys to another hash container.
     */
    public static <KType, VType> KTypeVTypeOpenHashMap<KType, VType> newInstanceWithoutPerturbations()
    {
        return new KTypeVTypeOpenHashMap<KType, VType>() {
            @Override
            protected int computePerturbationValue(int capacity) { return 0; }
        };
    }

    /**
     * Create a new hash map without providing the full generic signature (constructor
     * shortcut). 
     */
    public static <KType, VType> KTypeVTypeOpenHashMap<KType, VType> newInstance(int initialCapacity, float loadFactor)
    {
        return new KTypeVTypeOpenHashMap<KType, VType>(initialCapacity, loadFactor);
    }

    /**
     * Create a new hash map without providing the full generic signature (constructor
     * shortcut). The returned instance will have enough initial capacity to hold
     * <code>expectedSize</code> elements without having to resize.
     */
    public static <KType, VType> KTypeVTypeOpenHashMap<KType, VType> newInstanceWithExpectedSize(int expectedSize)
    {
        return newInstanceWithExpectedSize(expectedSize, DEFAULT_LOAD_FACTOR);
    }

    /**
     * Create a new hash map without providing the full generic signature (constructor
     * shortcut). The returned instance will have enough initial capacity to hold
     * <code>expectedSize</code> elements without having to resize.
     */
    public static <KType, VType> KTypeVTypeOpenHashMap<KType, VType> newInstanceWithExpectedSize(int expectedSize, float loadFactor)
    {
        return newInstance((int) (expectedSize / loadFactor) + 1, loadFactor);
    }
}
