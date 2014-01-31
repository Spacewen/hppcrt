package com.carrotsearch.hppc;

import java.util.*;

import com.carrotsearch.hppc.cursors.*;
import com.carrotsearch.hppc.predicates.*;
import com.carrotsearch.hppc.procedures.*;

import static com.carrotsearch.hppc.Internals.*;
import static com.carrotsearch.hppc.HashContainerUtils.*;

/**
 * A key-ordered map of <code>KType</code> to <code>VType</code>, supporting tje <code>KTypeVTypeMap</code> interface,
 * implemented using
 * a B+ tree allowing an optional multiple keys behavior like in C++ std::multimap.
 * Due to ordering, default iteration is made from min key value to max key value. A reversed iterator is also available.
 * In addition, the B+tree API offers fast range query methods.
 *
#if ($TemplateOptions.AllGeneric)
 * <p>
 * A brief comparison of the API against the Java Collections framework:
 * </p>
 * <table class="nice" summary="Java Collections TreeMap and HPPC ObjectObjectBPlusTreeMap, related methods.">
 * <caption>Java Collections TreeMap and HPPC {@link ObjectObjectOpenHashMap}, related methods.</caption>
 * <thead>
 *     <tr class="odd">
 *         <th scope="col">{@linkplain TreeMap java.util.TreeMap}</th>
 *         <th scope="col">{@link ObjectObjectBPlusTreeMap}</th>
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
 * <p>See {@link ObjectObjectBPlusTreeMap} class for API similarities and differences against Java
 * Collections.
#end
 * 
#if ($TemplateOptions.KTypeGeneric)
 * <p>This implementation DO NOT support <code>null</code> keys !</p>
 * 
 * </tbody>
 * </table>
#end
#if ($TemplateOptions.VTypeGeneric)
 * <p>This implementation supports <code>null</code> values.</p>
#end
 * 
 * 
 * @author This B+ tree is inspired by the
 *        <a href="http://panthema.net/2007/stx-btree/">the STX B+ tree C++ Template v0.9</a> project.
 */
// ${TemplateOptions.doNotGenerateKType("BOOLEAN")}
/*! ${TemplateOptions.generatedAnnotation} !*/
public class KTypeVTypeBPlusTreeMap<KType, VType> implements KTypeVTypeMap<KType, VType>, Cloneable
{
    /**
     * Optimum chunk size in bytes
     */
    private static final int OPTIMAL_CHUNK_SIZE_IN_BYTES = 128;

    /**
     * default chunk size in number of elements, equivalent to  MIN_CHUNK_SIZE_IN_BYTES
     */
    /*! #if ($TemplateOptions.KTypeGeneric) !*/
    public final static int DEFAULT_CHUNK_SIZE = KTypeVTypeBPlusTreeMap.OPTIMAL_CHUNK_SIZE_IN_BYTES / 4;
    /*!
     #elseif ($TemplateOptions.isKType("BYTE")
         public final static int DEFAULT_CHUNK_SIZE = OPTIMAL_CHUNK_SIZE_IN_BYTES;
     #elseif ($TemplateOptions.isKType("CHAR")
         public final static int DEFAULT_CHUNK_SIZE = OPTIMAL_CHUNK_SIZE_IN_BYTES / 2;
     #elseif ($TemplateOptions.isKType("SHORT")
          public final static int DEFAULT_CHUNK_SIZE = OPTIMAL_CHUNK_SIZE_IN_BYTES / 2;
     #elseif ($TemplateOptions.isKType("INT")
         public final static int DEFAULT_CHUNK_SIZE = OPTIMAL_CHUNK_SIZE_IN_BYTES / 4;
     #elseif ($TemplateOptions.isKType("LONG")
         public final static int DEFAULT_CHUNK_SIZE = OPTIMAL_CHUNK_SIZE_IN_BYTES / 8;
     #elseif ($TemplateOptions.isKType("FLOAT")
         public final static int DEFAULT_CHUNK_SIZE = OPTIMAL_CHUNK_SIZE_IN_BYTES / 4;
     #elseif ($TemplateOptions.isKType("DOUBLE")
         public final static int DEFAULT_CHUNK_SIZE = OPTIMAL_CHUNK_SIZE_IN_BYTES / 8;
     #end !*/

    /**
     * Default capacity = root node, addressing KTypeVTypeBPlusTreeMap.DEFAULT_CHUNK_SIZE leaf chunks.
     */
    public final static int DEFAULT_CAPACITY = KTypeVTypeBPlusTreeMap.DEFAULT_CHUNK_SIZE * KTypeVTypeBPlusTreeMap.DEFAULT_CHUNK_SIZE;

    /**
     * The current chunk size
     */
    private int chunkSize;

    /**
     * Total allocated number of key-value pairs in the map.
     */
    private int allocatedSize;

    /**
     * Current number of keys in the map
     */
    private int size;

    /**
     * True in case of multimap behaviour.
     */
    private boolean allowDuplicates;


    /**
     * Comparator to use for keys ordering, if != null
     * else use
     * #if ($TemplateOptions.KTypePrimitive)
     * the natural comparison order.
     * #else
     * the Comparable interface of the key objects.
     * #end
     */
    /*! #if ($TemplateOptions.KTypeGeneric) !*/
    protected  final Comparator<? super KType> comparator;
    /*! #else
    protected final KTypeComparator<? super KType>  comparator;
     #end !*/

    protected VType defaultValue = Intrinsics.<VType> defaultVTypeValue();

    /**
     * Creates a Tree map with the default capacity of {@link #DEFAULT_CAPACITY} using
     * #if ($TemplateOptions.KTypePrimitive)
     * the natural comparison order.
     * #else
     * the Comparable interface of the key objects.
     * #end
     * @param isMultimap true if multiple identical keys are authorized (a.k.a multimap)
     */
    public KTypeVTypeBPlusTreeMap(final boolean isMultimap)
    {
        this(KTypeVTypeBPlusTreeMap.DEFAULT_CAPACITY, null, isMultimap);
    }

    /**
     * Creates a Tree map with the given initial capacity, using
     * #if ($TemplateOptions.KTypePrimitive)
     * the natural comparison order.
     * #else
     * the Comparable interface for the key objects
     * #end
     * if comp == null, else use the provided comparator comp for key ordering.
     * 
     * @param initialCapacity Initial capacity (bounded to a minimum of {@link #DEFAULT_CAPACITY}).
     * @param comp Comparator to use.
     * @param isMultimap true if multiple identical keys are authorized (a.k.a multimap)
     */
    public KTypeVTypeBPlusTreeMap(final int initialCapacity,
            /*! #if ($TemplateOptions.KTypeGeneric) !*/
            final Comparator<? super KType> comp
            /*! #else
            final  KTypeComparator<? super KType>  comp
             #end !*/, final boolean isMultimap)
    {
        this(initialCapacity, KTypeVTypeBPlusTreeMap.DEFAULT_CHUNK_SIZE, comp, isMultimap);
    }

    /**
     * Create a Tree map from all key-value pairs of another container, using
     * #if ($TemplateOptions.KTypePrimitive)
     *  the natural comparison order
     * #else
     *  the Comparable interface of the object keys
     * #end
     *  if comp == null, else use the provided comparator for key ordering.
     *  @param isMultimap true if multiple identical keys are authorized (a.k.a multimap)
     */
    public KTypeVTypeBPlusTreeMap(final KTypeVTypeAssociativeContainer<KType, VType> container,
            /*! #if ($TemplateOptions.KTypeGeneric) !*/
            final Comparator<? super KType> comp
            /*! #else
            final  KTypeComparator<? super KType>  comp
             #end !*/, final boolean isMultimap)
    {
        this(container.size(), KTypeVTypeBPlusTreeMap.DEFAULT_CHUNK_SIZE, comp, isMultimap);

        putAll(container);
    }

    /**
     * Creates a Tree map with the given initial capacity, chunk size, using
     * #if ($TemplateOptions.KTypePrimitive)
     *  the natural comparison order
     * #else
     *  the Comparable interface
     * #end
     *  if comp == null, else use the provided comparator for key ordering.
     * 
     * @param initialCapacity Initial capacity (greater than zero and automatically
     *            rounded to the next power of two).
     *
     * @param chunkSize the internal chunk size, affecting tree height and performance.
     * bounded by a minimum of {@link #DEFAULT_CHUNK_SIZE}
     * @param comp Comparator to use.
     * @param isMultimap true if multiple identical keys are authorized (a.k.a multimap)
     */
    public KTypeVTypeBPlusTreeMap(final int initialCapacity, final int chunkSize,
            /*! #if ($TemplateOptions.KTypeGeneric) !*/
            final Comparator<? super KType> comp
            /*! #else
            final  KTypeComparator<? super KType>  comp
             #end !*/, final boolean isMultimap)
    {
        this.allowDuplicates = isMultimap;
        this.comparator = comp;

        //A B+tree is at least 50 % full, so the allocated size must be 2x the expected capacity
        //to assure no reallocation ever occurs.
        this.allocatedSize = Math.max(KTypeVTypeBPlusTreeMap.DEFAULT_CAPACITY, initialCapacity * 2);

        //use DEFAULT chunk size as a minimum for the chunk sizes.
        this.chunkSize = Math.max(KTypeVTypeBPlusTreeMap.DEFAULT_CHUNK_SIZE, chunkSize);

        //TODO allocate
    }

    /**
     * {@inheritDoc}
     * If the map is a multimap, multiple identical keys may be present (matching valuesmay be different)
     * else if the map is not a multimap, the existing key (-value pair) is replaced with this one.
     */
    @Override
    public VType put(final KType key, final VType value)
    {
        //TODO
        return null;
    }

    /**
     * {@inheritDoc}
     * If the map is a multimap, multiple identical keys may be present, in this case
     * the method returns one matching key-value pair among others.
     */
    @Override
    public VType get(final KType key)
    {
        // TODO Auto-generated method stub
        return null;
    }


    /**
     * Puts all keys from another container to this map, replacing the values of existing keys, if such keys are present and if
     * the Tree Map is not a multimap. Else, if the Tree map is a multimap, all the container keys-value pairs are added.
     */
    @Override
    public int putAll(final KTypeVTypeAssociativeContainer<? extends KType, ? extends VType> container)
    {
        final int count = this.size;

        for (final KTypeVTypeCursor<? extends KType, ? extends VType> c : container)
        {
            put(c.key, c.value);
        }

        return this.size - count;
    }

    /**
     * Puts all key/value pairs from a given iterable into this map, replacing the values of existing keys, if such keys are present and if
     * the Tree Map is not a multimap. Else, if the Tree map is a multimap, all the iterable key-value pairs are added.
     */
    @Override
    public int putAll(final Iterable<? extends KTypeVTypeCursor<? extends KType, ? extends VType>> iterable)
    {
        final int count = this.size;

        for (final KTypeVTypeCursor<? extends KType, ? extends VType> c : iterable)
        {
            put(c.key, c.value);
        }
        return this.size - count;
    }

    /**
     * {@inheritDoc}
     * If the map is a multimap, only one matching key/value pair is removed,
     * so use {@link #removeAllKeys(Object)} instead.
     */
    @Override
    public VType remove(final KType key)
    {
        //TODO
        return null;
    }

    /**
     * Remove all key/value pairs matching the key,
     * then returns the number of removed pairs.
     * (this is only useful for multimaps)
     */
    public int removeAllKeys(final KType key)
    {
        //TODO
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int removeAll(final KTypeContainer<? extends KType> container)
    {
        final int before = this.size;

        for (final KTypeCursor<? extends KType> cursor : container)
        {
            removeAllKeys(cursor.value);
        }

        return before - this.size;
    }

    /**
     * Removes all keys (and associated values) present in a given container,
     * for all keys in the ordered range [lowerBound, upperBound [
     * @param container
     * @param lowerBound
     * @param upperBound
     * @return Returns the number of elements actually removed as a result of this call.
     */
    public int removeAllInRange(final KTypeContainer<? extends KType> container, final KType lowerBound, final KType upperBound)
    {
        //TODO
        return 0;
    }

    /**
     * Remove all keys-value pairs for keys in the ordered range [lowerBound, upperBound [
     * The iteration is interrupted otherwise.
     * @param lowerBound
     * @param upperBound
     * @return The number of removed elements as a result of this call.
     */
    public int removeRange(final KType lowerBound, final KType upperBound)
    {
        //TODO
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int removeAll(final KTypePredicate<? super KType> predicate)
    {
        //TODO
        return 0;
    }

    /**
     * Removes all keys (and associated values) for which the predicate returns true
     * AND keys in the ordered range [lowerBound, upperBound[.
     * @param predicate
     * @param lowerBound
     * @param upperBound
     * @return Returns the number of elements actually removed as a result of this call.
     */
    public int removeAllInRange(final KTypePredicate<? super KType> predicate, final KType lowerBound, final KType upperBound)
    {
        //TODO
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean containsKey(final KType key)
    {
        //TODO
        return false;
    }

    /**
     * Count all keys-value pairs for keys in the ordered range [lowerBound, upperBound [
     * @param lowerBound
     * @param upperBound
     * @return The number of key-value pairs with matching key.
     */
    public int countRange(final KType lowerBound, final KType upperBound)
    {
        //TODO
        return 0;
    }

    /**
     * Count the (keys-value) pairs numbers matching for key
     */
    public int count(final KType key)
    {
        //TODO ?
        return countRange(key, key);
    }

    /**
     * {@inheritDoc}
     * 
     * <p>Does not release internal buffers.</p>
     */
    @Override
    public void clear()
    {
        //TODO
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int size()
    {
        return size;
    }

    /**
     * {@inheritDoc}
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
    @SuppressWarnings({ "rawtypes" })
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

            if (obj instanceof KTypeVTypeBPlusTreeMap &&
                    this.allowDuplicates != ((KTypeVTypeBPlusTreeMap) obj).allowDuplicates)
            {
                return false;
            }

            //we also cannot compare a classic k-V map and a B+tree multimap
            //because of the duplicates in the B+tree
            if ( ! (obj instanceof KTypeVTypeBPlusTreeMap) && this.allowDuplicates)
            {
                return false;
            }

            if (obj instanceof KTypeVTypeMap)
            {
                /* #if ($TemplateOptions.AnyGeneric) */
                @SuppressWarnings("unchecked")
                final/* #end */
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
        }

        @Override
        protected KTypeVTypeCursor<KType, VType> fetch()
        {
            //TODO
            return null;
        }
    }

    /**
     * internal pool of EntryIterator
     */
    protected final IteratorPool<KTypeVTypeCursor<KType, VType>, EntryIterator> entryIteratorPool = new IteratorPool<KTypeVTypeCursor<KType, VType>, EntryIterator>(
            new ObjectFactory<EntryIterator>() {

                @Override
                public EntryIterator create()
                {
                    return new EntryIterator();
                }

                @Override
                public void initialize(final EntryIterator obj)
                {
                    //TODO
                }
            });

    /**
     * {@inheritDoc}
     * This iterator goes through the key-value pairs in
     * ascending key ordering.
     */
    @Override
    public EntryIterator iterator()
    {
        //return new EntryIterator();
        return this.entryIteratorPool.borrow();
        //TODO
    }

    /**
     * {@inheritDoc}
     * This iterator goes through the key-value pairs in
     * ascending key ordering, for keys in range [lowerBound, upperBound[
     */
    public EntryIterator rangeIterator(final KType lowerBound, final KType upperBound)
    {
        //return new EntryIterator();
        return this.entryIteratorPool.borrow();
        //TODO
    }

    /**
     * Return a reversed iterator, i.e this iterator goes through the key-value pairs in
     * descending key ordering.
     */
    public EntryIterator reversedIterator()
    {
        //TODO
        return null;
    }

    /**
     * Return a reversed iterator, i.e this iterator goes through the key-value pairs in
     * descending key ordering, for keys in range [lowerBound, upperBound[
     */
    public EntryIterator reversedRangeIterator()
    {
        //TODO
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends KTypeVTypeProcedure<? super KType, ? super VType>> T forEach(final T procedure)
    {
        //TODO
        return procedure;
    }

    /**
     * Applies a given procedure to keys-value pairs in this container,
     * for all keys in the ordered range [lowerBound, upperBound [
     * @param container
     * @param lowerBound
     * @param upperBound
     * @return Returns the number of elements actually removed as a result of this call.
     */
    public <T extends KTypeVTypeProcedure<? super KType, ? super VType>> T forEachInRange(final T procedure, final KType lowerBound, final KType upperBound)
    {
        //TODO
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
     * A view of the keys inside this Tree map.
     */
    public final class KeysContainer
    extends AbstractKTypeCollection<KType> implements KTypeLookupContainer<KType>
    {
        private final KTypeVTypeBPlusTreeMap<KType, VType> owner =
                KTypeVTypeBPlusTreeMap.this;

        @Override
        public boolean contains(final KType e)
        {
            return containsKey(e);
        }

        @Override
        public <T extends KTypeProcedure<? super KType>> T forEach(final T procedure)
        {
            //TODO
            return procedure;
        }

        @Override
        public <T extends KTypePredicate<? super KType>> T forEach(final T predicate)
        {
            //TODO
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
            //TODO
            return 0;
        }

        /**
         * internal pool of KeysIterator
         */
        protected final IteratorPool<KTypeCursor<KType>, KeysIterator> keyIteratorPool = new IteratorPool<KTypeCursor<KType>, KeysIterator>(
                new ObjectFactory<KeysIterator>() {

                    @Override
                    public KeysIterator create()
                    {
                        return new KeysIterator();
                    }

                    @Override
                    public void initialize(final KeysIterator obj)
                    {
                        //TODO
                    }
                });

        @Override
        public KType[] toArray(final KType[] target)
        {
            //TODO
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
            //TODO
        }

        @Override
        protected KTypeCursor<KType> fetch()
        {
            //TODO
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
        private final KTypeVTypeBPlusTreeMap<KType, VType> owner = KTypeVTypeBPlusTreeMap.this;

        @Override
        public int size()
        {
            return owner.size();
        }

        @Override
        public boolean contains(final VType value)
        {
            // This is a linear scan over the values, but it's in the contract, so be it.
            //TODO
            return false;
        }

        @Override
        public <T extends KTypeProcedure<? super VType>> T forEach(final T procedure)
        {
            //TODO
            return procedure;
        }

        @Override
        public <T extends KTypePredicate<? super VType>> T forEach(final T predicate)
        {
            //TODO
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
        protected final IteratorPool<KTypeCursor<VType>, ValuesIterator> valuesIteratorPool = new IteratorPool<KTypeCursor<VType>, ValuesIterator>(
                new ObjectFactory<ValuesIterator>() {

                    @Override
                    public ValuesIterator create()
                    {
                        return new ValuesIterator();
                    }

                    @Override
                    public void initialize(final ValuesIterator obj)
                    {
                        //TODO
                    }
                });

        @Override
        public VType[] toArray(final VType[] target)
        {
            //TODO
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
            //TODO
        }

        @Override
        protected KTypeCursor<VType> fetch()
        {
            //TODO
            return cursor;
        }
    }

    /**
     * Clone this object.
     */
    @Override
    public KTypeVTypeBPlusTreeMap<KType, VType> clone()
    {
        //TODO
        return null;
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
            if (!first)
                buffer.append(", ");
            buffer.append(cursor.key);
            buffer.append("=>");
            buffer.append(cursor.value);
            first = false;
        }
        buffer.append("]");
        return buffer.toString();
    }

    /**
     * Create a new Tree map without providing the full generic signature (constructor
     * shortcut).
     */
    public static <KType, VType> KTypeVTypeBPlusTreeMap<KType, VType> newInstance(final boolean isMultimap)
    {
        return new KTypeVTypeBPlusTreeMap<KType, VType>(isMultimap);
    }

    /**
     * Create a new Tree map with initial capacity, comparator and multimap setting. (constructor
     * shortcut).
     */
    public static <KType, VType> KTypeVTypeBPlusTreeMap<KType, VType> newInstance(final int initialCapacity,
            /*! #if ($TemplateOptions.KTypeGeneric) !*/
            final Comparator<? super KType> comp
            /*! #else
            final  KTypeComparator<? super KType>  comp
             #end !*/, final boolean isMultimap)
             {
        return new KTypeVTypeBPlusTreeMap<KType, VType>(initialCapacity, comp, isMultimap);
             }

    /**
     * Create a new Tree map with full parameter control, using a specific capacity, chunk size, comparator,
     * and multimap setting.
     */
    public static <KType, VType> KTypeVTypeBPlusTreeMap<KType, VType> newInstance(final int initialCapacity, final int chunkSize,
            /*! #if ($TemplateOptions.KTypeGeneric) !*/
            final Comparator<? super KType> comp
            /*! #else
            final  KTypeComparator<? super KType>  comp
             #end !*/, final boolean isMultimap)
             {
        return new KTypeVTypeBPlusTreeMap<KType, VType>(initialCapacity, chunkSize, comp, isMultimap);
             }



    /**
     * Return the current {@link Comparator} in use, or {@code null} if none was set.
     */
    public/*! #if ($TemplateOptions.KTypeGeneric) !*/
    final Comparator<? super KType>
    /*! #else
            final  KTypeComparator<? super KType>
             #end !*/comparator()
             {
        return this.comparator;
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
}
