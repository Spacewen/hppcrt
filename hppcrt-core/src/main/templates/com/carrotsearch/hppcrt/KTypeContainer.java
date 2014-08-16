package com.carrotsearch.hppcrt;

import java.util.Iterator;

import com.carrotsearch.hppcrt.cursors.KTypeCursor;
import com.carrotsearch.hppcrt.predicates.KTypePredicate;
import com.carrotsearch.hppcrt.procedures.KTypeProcedure;

/**
 * A generic container holding <code>KType</code>s. An overview of interface relationships
 * is given in the figure below:
 * 
 * <p><img src="doc-files/interfaces.png"
 *      alt="HPPC interfaces" /></p>
 */
/*! ${TemplateOptions.generatedAnnotation} !*/
public interface KTypeContainer<KType> extends Iterable<KTypeCursor<KType>>
{
    /**
     * Returns an iterator to a cursor traversing the collection. The order of traversal
     * is not defined. More than one cursor may be active at a time. The behavior of
     * iterators is undefined if structural changes are made to the underlying collection.
     * 
     * <p>The iterator is implemented as a
     * cursor and it returns <b>the same cursor instance</b> on every call to
     * {@link Iterator#next()} (to avoid boxing of primitive types). To read the current
     * list's value (or index in the list) use the cursor's public fields. An example is
     * shown below.</p>
     * 
     * <pre>
     * for (KTypeCursor&lt;KType&gt; c : container) {
     *   System.out.println("index=" + c.index + " value=" + c.value);
     * }
     * </pre>
     */
    @Override
    Iterator<KTypeCursor<KType>> iterator();

    /**
     * Lookup a given element in the container. This operation has no speed
     * guarantees (may be linear with respect to the size of this container).
     * 
     * @return Returns <code>true</code> if this container has an element
     * equal to <code>e</code>.
     */
    boolean contains(KType e);

    /**
     * Return the current number of elements in this container. The time for calculating
     * the container's size may take <code>O(n)</code> time, although implementing classes
     * should try to maintain the current size and return in constant time.
     */
    int size();

    /**
     * Return the maximum number of elements this container is guaranteed to hold without reallocating. 
     * The time for calculating the container's capacity may take <code>O(n)</code> time.
     */
    int capacity();

    /**
     * True if there is no elements in the container,
     * equivalent to <code>size() == 0</code>
     */
    boolean isEmpty();

    /*! #if ($TemplateOptions.KTypeGeneric) !*/
    /**
     * Copies all elements from this container to an array of this container's component
     * type.
     * <p>The returned array is sized to match exactly
     * the number of elements of the container.</p>
     * The returned array is always a copy, regardless of the storage used by the container.
     */
    KType[] toArray(Class<? super KType> clazz);

    /*! #end !*/

    /**
     * Copies all elements from this container to an array.
     * <p>The returned array is sized to match exactly
     * the number of elements of the container.</p>
     * If you need an array of the type identical with this container's generic type, use {@link #toArray(Class)}.
     * 
     * @see #toArray(Class)
     */
    /*! #if ($TemplateOptions.KTypePrimitive)
    public KType [] toArray();
    #else !*/
    Object[] toArray();

    /*! #end !*/

    /**
     * Copies all elements of this container to an existing array of the same type.
     * @param target The target array must be large enough to hold all elements, i.e >= {@link #size()}.
     * @return Returns the target argument for chaining.
     */
    KType[] toArray(KType[] target);

    /**
     * Applies a <code>procedure</code> to all container elements. Returns the argument (any
     * subclass of {@link KTypeProcedure}. This lets the caller to call methods of the argument
     * by chaining the call (even if the argument is an anonymous type) to retrieve computed values,
     * for example (IntContainer):
     * <pre>
     * int count = container.forEach(new IntProcedure() {
     *      int count; // this is a field declaration in an anonymous class.
     *      public void apply(int value) { count++; }}).count;
     * </pre>
     */
    <T extends KTypeProcedure<? super KType>> T forEach(T procedure);

    /**
     * Applies a <code>predicate</code> to container elements, as long as the predicate
     * returns <code>true</code>. The iteration is interrupted otherwise.
     */
    <T extends KTypePredicate<? super KType>> T forEach(T predicate);
}
