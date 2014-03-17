package com.carrotsearch.hppc;

import java.util.*;

import com.carrotsearch.hppc.cursors.*;
import com.carrotsearch.hppc.predicates.*;
import com.carrotsearch.hppc.procedures.*;
import com.carrotsearch.hppc.sorting.*;

import static com.carrotsearch.hppc.Internals.*;

/**
 * A key-ordered set of <code>KType</code> , supporting the <code>KTypeSet</code> interface,
 * implemented using a B+ tree allowing an optional multiple keys behavior like in C++ std::multiset.
 * Due to ordering, default iteration is made from min key value to max key value. A reversed iterator is also available.
 * In addition, the B+tree API offers fast range query methods.
 *
#if ($TemplateOptions.AllGeneric)
 * <p>
 * A brief comparison of the API against the Java Collections framework:
 * </p>
 * <table class="nice" summary="Java Collections TreeMap and HPPC ObjectBPlusTreeSet, related methods.">
 * <caption>Java Collections TreeSet and HPPC {@link ObjectOpenHashSet}, related methods.</caption>
 * <thead>
 *     <tr class="odd">
 *         <th scope="col">{@linkplain TreeSet java.util.TreeSet}</th>
 *         <th scope="col">{@link ObjectObjectBPlusTreeMap}</th>
 *     </tr>
 * </thead>
 * <tbody>
 * <tr            ><td>V put(K)       </td><td>V put(K)      </td></tr>
 * <tr class="odd"><td>V get(K)       </td><td>V get(K)      </td></tr>
 * <tr            ><td>V remove(K)    </td><td>V remove(K)   </td></tr>
 * <tr class="odd"><td>size, clear,
 *                     isEmpty</td><td>size, clear, isEmpty</td></tr>
 * <tr            ><td>contains(K) </td><td>contains(K), lget()</td></tr>
 * <tr class="odd"><td>containsValue(K) </td><td>(no efficient equivalent)</td></tr>
 * <tr            ><td>keySet, entrySet </td><td>{@linkplain #iterator() iterator} over set entries,
 *                                               keySet, pseudo-closures</td></tr>
#else
 * <p>See {@link ObjectObjectBPlusTreeSet} class for API similarities and differences against Java
 * Collections.
#end
 * 
#if ($TemplateOptions.KTypeGeneric)
 * <p>This implementation DO NOT support <code>null</code> keys !</p>
 * 
 * </tbody>
 * </table>
#end
 * 
 * 
 * @author This B+ tree is inspired by the
 *        <a href="http://panthema.net/2007/stx-btree/">the STX B+ tree C++ Template v0.9</a> project.
 */
/*! ${TemplateOptions.doNotGenerateKType("BOOLEAN")} !*/
/*! ${TemplateOptions.generatedAnnotation} !*/
public class KTypeBPlusTreeSet<KType> extends AbstractKTypeCollection<KType>
implements KTypeLookupContainer<KType>, KTypeSet<KType>, Cloneable
{
    /**
     * Optimum chunk size in bytes : 256 bytes
     */
    private static final int CHUNK_SIZE_IN_BYTES_IN_BITSHIFTS = 8;

    /**
     * Chunk chunk size in number of elements.
     */
    /*! #if ($TemplateOptions.KTypeGeneric) !*/
    private final static int CHUNK_SIZE_IN_BITSHIFTS = KTypeBPlusTreeSet.CHUNK_SIZE_IN_BYTES_IN_BITSHIFTS - 2;
    private final static int CHUNK_SIZE = 1 << KTypeBPlusTreeSet.CHUNK_SIZE_IN_BITSHIFTS;
    /*!
     #elseif ($TemplateOptions.isKType("byte"))
         private final static int CHUNK_SIZE_IN_BITSHIFTS = CHUNK_SIZE_IN_BYTES_IN_BITSHIFTS;
         private final static int CHUNK_SIZE = 1 << CHUNK_SIZE_IN_BITSHIFTS;
     #elseif ($TemplateOptions.isKType("char"))
         private final static int CHUNK_SIZE_IN_BITSHIFTS = CHUNK_SIZE_IN_BYTES_IN_BITSHIFTS - 1;
         private final static int CHUNK_SIZE = 1 << CHUNK_SIZE_IN_BITSHIFTS;
     #elseif ($TemplateOptions.isKType("short"))
         private final static int CHUNK_SIZE_IN_BITSHIFTS = CHUNK_SIZE_IN_BYTES_IN_BITSHIFTS - 1;
         private final static int CHUNK_SIZE = 1 << CHUNK_SIZE_IN_BITSHIFTS;
     #elseif ($TemplateOptions.isKType("int"))
         private final static int CHUNK_SIZE_IN_BITSHIFTS = CHUNK_SIZE_IN_BYTES_IN_BITSHIFTS - 2;
         private final static int CHUNK_SIZE = 1 << CHUNK_SIZE_IN_BITSHIFTS;
     #elseif ($TemplateOptions.isKType("long"))
         private final static int CHUNK_SIZE_IN_BITSHIFTS = CHUNK_SIZE_IN_BYTES_IN_BITSHIFTS - 3;
         private final static int CHUNK_SIZE = 1 << CHUNK_SIZE_IN_BITSHIFTS;
     #elseif ($TemplateOptions.isKType("float"))
         private final static int CHUNK_SIZE_IN_BITSHIFTS = CHUNK_SIZE_IN_BYTES_IN_BITSHIFTS - 2;
         private final static int CHUNK_SIZE = 1 << CHUNK_SIZE_IN_BITSHIFTS;
     #elseif ($TemplateOptions.isKType("double"))
         private final static int CHUNK_SIZE_IN_BITSHIFTS = CHUNK_SIZE_IN_BYTES_IN_BITSHIFTS - 3;
         private final static int CHUNK_SIZE = 1 << CHUNK_SIZE_IN_BITSHIFTS;
     #end !*/

    /**
     * Default capacity = root node, addressing 4 leaf chunks.
     * Since the B+tree is by construction at least 50 % full, that leads to a default user capacity of : (not counting root node)
     */
    private final static int DEFAULT_CAPACITY = (4 * KTypeBPlusTreeSet.CHUNK_SIZE) / 2;

    /**
     * Maximum possible capacity
     */
    public static final int MAX_CAPACITY = (Integer.MAX_VALUE / 2) - 8;

    /**
     * Total allocated number of key-value pairs in the map.
     */
    private int allocatedSize;

    /**
     * Current number of keys in the map
     */
    private int size;

    /**
     * True in case of multiset behaviour.
     */
    private boolean allowDuplicates;

    /**
     * Min key value of the map, w.r.t to ordering criteria.
     */
    private KType minKey;

    /**
     * Max key value of the map, w.r.t to ordering criteria.
     */
    private KType maxKey;


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
    protected final Comparator<? super KType> comparator;

    /*! #else
    protected final KTypeComparator<? super KType>  comparator;
     #end !*/

    /**
     * Creates a Tree set with the default capacity of {@link #DEFAULT_CAPACITY} using
     * #if ($TemplateOptions.KTypePrimitive)
     * the natural comparison order.
     * #else
     * the Comparable interface of the key objects.
     * #end
     * @param isMultimap true if multiple identical keys are authorized (a.k.a multiset)
     */
    public KTypeBPlusTreeSet(final boolean isMultiset)
    {
        this(KTypeBPlusTreeSet.DEFAULT_CAPACITY, null, isMultiset);
    }

    /**
     * Creates a B+Tree set with the given initial capacity, using
     * #if ($TemplateOptions.KTypePrimitive)
     * the natural comparison order.
     * #else
     * the Comparable interface for the key objects
     * #end
     * if comp == null, else use the provided comparator comp for key ordering.
     * 
     * @param initialCapacity Initial capacity.
     * @param comp Comparator to use.
     * @param isMultiset true if multiple identical keys are authorized (a.k.a multiset)
     */
    public KTypeBPlusTreeSet(final int initialCapacity,
            /*! #if ($TemplateOptions.KTypeGeneric) !*/
            final Comparator<? super KType> comp
            /*! #else
            final  KTypeComparator<? super KType>  comp
             #end !*/, final boolean isMultiset)
    {
        this.allowDuplicates = isMultiset;
        this.comparator = comp;

        //A B+tree is at least 50 % full, so the allocated size must be 2x the expected capacity
        //to assure no reallocation ever occurs.
        this.allocatedSize = Math.max(KTypeBPlusTreeSet.DEFAULT_CAPACITY * 2,
                Math.min(initialCapacity * 2, KTypeBPlusTreeSet.MAX_CAPACITY));

        //TODO allocate
    }

    /**
     * Create a B+Tree set from all key pairs of another container, using
     * #if ($TemplateOptions.KTypePrimitive)
     *  the natural comparison order
     * #else
     *  the Comparable interface of the object keys
     * #end
     *  if comp == null, else use the provided comparator for key ordering.
     *  @param isMultimap true if multiple identical keys are authorized (a.k.a multiset)
     */
    public KTypeBPlusTreeSet(final KTypeContainer<KType> container,
            /*! #if ($TemplateOptions.KTypeGeneric) !*/
            final Comparator<? super KType> comp
            /*! #else
            final  KTypeComparator<? super KType>  comp
             #end !*/, final boolean isMultiset)
    {
        this(container.size(), comp, isMultiset);

        addAll(container);
    }

    /**
     * {@inheritDoc}
     * If the set is a multiset, multiple identical keys may be present,
     * else if the set is not a multiset, the existing key is replaced with this one.
     */
    @Override
    public boolean add(final KType e)
    {
        //TODO
        return false;
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
     * call.
     */
    public int addAll(final KTypeContainer<? extends KType> container)
    {
        return addAll((Iterable<? extends KTypeCursor<? extends KType>>) container);
    }

    /**
     * Adds all elements from a given iterable to this set.
     * 
     * @return Returns the number of elements actually added as a result of this
     * call.
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
     * Removes one matching key from the container w.r.t the comparison criteria.
     * Indeed, if the set is a multiset only the last inserted matching key pair is removed,
     * so use {@link #removeAll(key)} instead to remove all matching keys.
     */
    public boolean remove(final KType key)
    {
        //TODO
        return false;
    }

    /**
     * Returns the last key saved in a call to {@link #contains} if it returned <code>true</code>.
     * Precondition : {@link #contains} must have been called previously !
     * If the set is a multiset, returns the last inserted matching key.
     * @see #contains
     */
    public KType lkey()
    {
        //TODO
        return Intrinsics.defaultKTypeValue();
    }

    /**
     * {@inheritDoc}
     * 
     * #if ($TemplateOptions.KTypeGeneric) <p>Saves the associated value for fast access using {@link #lkey()}.</p>
     * <pre>
     * if (set.contains(key))
     *     value = set.lkey();
     * 
     * </pre> #end
     */
    @Override
    public boolean contains(final KType key)
    {
        //TODO
        return false;
    }

    /**
     * {@inheritDoc}
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
    public int hashCode()
    {
        int h = 0;

        //iteration is done in-order
        for (final KTypeCursor<KType> c : this)
        {
            h += Internals.rehash(c.value);
        }
        return h;
    }

    /* #if ($TemplateOptions.KTypeGeneric) */
    @SuppressWarnings("unchecked")
    /* #end */
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj)
    {
        if (obj != null)
        {
            if (obj == this)
                return true;

            //we can only compare KTypeBPlusTreeSet instances
            if (!(obj instanceof KTypeBPlusTreeSet))
            {
                return false;
            }

            //if the other object is a B+tree set, we can only compare both KTypeBPlusTreeSet,
            //that has the same comparison function reference
            final KTypeBPlusTreeSet<KType> other = (KTypeBPlusTreeSet<KType>) obj;

            if ((this.comparator == null && this.comparator != other.comparator) ||
                    (this.comparator != null && !this.comparator.equals(other.comparator)))
            {
                return false;
            }

            //both must be of the same size
            if (other.size() == this.size())
            {
                return false;
            }

            //proceed comparision of common iterators
            final EntryIterator it = this.iterator();
            final EntryIterator otherIt = this.iterator();

            //Case 1: No comparator set, use natural ordering or Comparable interface
            if (this.comparator == null)
            {
                while (it.hasNext())
                {
                    final KTypeCursor<KType> c = it.next();
                    final KTypeCursor<KType> otherc = it.next();

                    if (!Intrinsics.isCompEqualKTypeUnchecked(c.value, otherc.value))
                    {
                        //not equal
                        //recycle
                        it.release();
                        otherIt.release();
                        return false;
                    }
                } //end while

                return true;
            }
            else if (this.comparator != null && this.comparator.equals(other.comparator))
            {
                /*! #if ($TemplateOptions.KTypeGeneric) !*/
                final Comparator<? super KType> comp = this.comparator;
                /*! #else
                KTypeComparator<? super KType> comp = this.comparator;
                #end !*/

                while (it.hasNext())
                {
                    final KTypeCursor<KType> c = it.next();
                    final KTypeCursor<KType> otherc = it.next();

                    if (comp.compare(c.value, otherc.value) != 0)
                    {
                        //not equal
                        //recycle
                        it.release();
                        otherIt.release();
                        return false;
                    }
                } //end while

                return true;
            }
        }

        return false;
    }

    /**
     * Clone this object.
     */
    @Override
    public KTypeBPlusTreeSet<KType> clone()
    {
        //real constructor call
        final KTypeBPlusTreeSet<KType> cloned = new KTypeBPlusTreeSet<KType>(this.size, this.comparator, this.allowDuplicates);

        cloned.addAll(this);

        cloned.defaultValue = this.defaultValue;

        return cloned;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public KType[] toArray(final KType[] target)
    {
        //TODO
        return target;
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
     * {@inheritDoc}
     */
    @Override
    public int removeAllOccurrences(final KType e)
    {
        // TODO Auto-generated method stub
        return 0;
    }

    ////////////////////////////////
    // Tree map specific methods
    ////////////////////////////////

    /**
     * Remove all keys pairs
     * for all keys in the ordered range [lowerBound, upperBound ][ with inclusive upperBound if  includeUpperBound = true
     * @param lowerBound
     * @param upperBound
     * @param includeUpperBound
     * @return The number of removed elements as a result of this call.
     */
    public int removeInRange(final KType lowerBound, final KType upperBound, final boolean includeUpperBound)
    {
        //TODO
        return 0;
    }

    /**
     * Removes all keys for which the predicate returns true
     * in the ordered range [lowerBound, upperBound ][ with inclusive upperBound if  includeUpperBound = true
     * @param predicate
     * @param lowerBound
     * @param upperBound
     * @param includeUpperBound
     * @return Returns the number of elements actually removed as a result of this call.
     */
    public int removeInRange(final KTypePredicate<? super KType> predicate, final KType lowerBound, final KType upperBound, final boolean includeUpperBound)
    {
        //TODO
        return 0;
    }

    /**
     * Count the number of keys
     * in the ordered range [lowerBound, upperBound ][ with inclusive upperBound if includeUpperBound = true
     * @param lowerBound
     * @param upperBound
     * @param includeUpperBound
     * @return The number of key-value pairs with matching key.
     */
    public int countRange(final KType lowerBound, final KType upperBound, final boolean includeUpperBound)
    {
        //TODO
        return 0;
    }

    /**
     * Count the number of keys for which the predicate returns true
     * in the ordered range [lowerBound, upperBound ][ with inclusive upperBound if includeUpperBound = true
     * @param lowerBound
     * @param upperBound
     * @param includeUpperBound
     * @return The number of key-value pairs with matching key.
     */
    public int countRange(final KTypePredicate<? super KType> predicate, final KType upperBound, final boolean includeUpperBound)
    {
        //TODO
        return 0;
    }

    /**
     * Count the numbers en entries for keys in the set.
     */
    public int count(final KType key)
    {
        //TODO
        return 0;
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
                    //TODO;
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
     * This iterator goes through the key-value pairs in
     * ascending key ordering, in [lowerBound, upperBound ][ with inclusive upperBound if includeUpperBound = true
     */
    public EntryIterator rangeIterator(final KType lowerBound, final KType upperBound, final boolean includeUpperBound)
    {
        //return new EntryIterator();
        return this.entryIteratorPool.borrow();
        //TODO align the iterator over the first position.
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
     * descending key ordering, in [lowerBound, upperBound ][ with inclusive upperBound if includeUpperBound = true
     */
    public EntryIterator reversedRangeIterator(final KType lowerBound, final KType upperBound, final boolean includeUpperBound)
    {
        //TODO
        return null;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends KTypeProcedure<? super KType>> T forEach(final T procedure)
    {
        //TODO
        return procedure;
    }

    /**
     * Applies a given procedure to keys in this container,
     * for all keys in the ordered range [lowerBound, upperBound ][ with inclusive upperBound if  includeUpperBound = true
     * @param container
     * @param lowerBound
     * @param upperBound
     * @param includeUpperBound
     * @return Returns the number of elements actually removed as a result of this call.
     */
    public <T extends KTypeProcedure<? super KType>> T forEachRange(final T procedure,
            final KType lowerBound, final KType upperBound, final boolean includeUpperBound)
    {
        //TODO
        return procedure;
    }

    /**
     * {@inheritDoc}
     * The iteration is done in order w.r.t the comparison criteria.
     */
    @Override
    public <T extends KTypePredicate<? super KType>> T forEach(final T predicate)
    {
        //TODO
        return predicate;
    }

    /**
     * Applies a given predicate to keys in this container,
     * for all keys in the ordered range [lowerBound, upperBound ][ with inclusive upperBound if  includeUpperBound = true
     * Applies the predicate to elements, as long as the predicate returns true. The iteration is interrupted otherwise.
     * The iteration is done in order with respect to the comparison criteria.
     * @param container
     * @param lowerBound
     * @param upperBound
     * @param includeUpperBound
     * @return Returns the number of elements actually removed as a result of this call.
     */
    public <T extends KTypePredicate<? super KType>> T forEachInRange(final T predicate,
            final KType lowerBound, final KType upperBound, final boolean includeUpperBound)
    {
        //TODO
        return predicate;
    }

    /**
     * Create a new Tree set without providing the full generic signature (constructor
     * shortcut).
     */
    public static <KType> KTypeBPlusTreeSet<KType> newInstance(final boolean isMultimap)
    {
        return new KTypeBPlusTreeSet<KType>(isMultimap);
    }

    /**
     * Create a new Tree set with initial capacity, comparator and multiset setting. (constructor
     * shortcut).
     */
    public static <KType> KTypeBPlusTreeSet<KType> newInstance(final int initialCapacity,
            /*! #if ($TemplateOptions.KTypeGeneric) !*/
            final Comparator<? super KType> comp
            /*! #else
            final  KTypeComparator<? super KType>  comp
             #end !*/, final boolean isMultimap)
             {
        return new KTypeBPlusTreeSet<KType>(initialCapacity, comp, isMultimap);
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
     * Returns the minimum key, w.r.t to ordering criteria.
     * Precondition: the container is not empty !
     */
    public KType getMinKey()
    {
        assert this.size > 0;
        return this.minKey;
    }

    /**
     * Returns the maximum key, w.r.t to ordering criteria.
     * Precondition: the container is not empty !
     */
    public KType getMaxKey()
    {
        assert this.size > 0;
        return this.maxKey;
    }
}
