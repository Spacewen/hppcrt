package com.carrotsearch.hppc;

import com.carrotsearch.hppc.predicates.KTypeIndexedPredicate;
import com.carrotsearch.hppc.procedures.KTypeIndexedProcedure;

/**
 * A Indexed Priority queue of <code>KType</code>s.
 * similar to a simplified map (K,V) = (index, element K)
 * with priority queue capabilities.
 */
/*! ${TemplateOptions.generatedAnnotation} !*/
public interface KTypeIndexedPriorityQueue<KType> extends KTypeCollection<KType>
{
    /**
     * Insert a <code>k</code> in the priority queue associated with a defined index >= 0
     * Returns true if insertion was successful,
     * false if the element already exists.
     * @param k
     */
    boolean insert(int index, KType k);

    /**
     * Delete the element <code>k</code> designed with its index
     * it was previously insert() before. Returns true if deletion was successful,
     * false if the element doesn't exists.
     */
    boolean deleteIndex(int index);

    /**
     * Return the element <code>k</code> designed with its index
     * it was previously insert() before. For generics version, returns null if
     * no such element exists, else return the default value.
     */
    KType getIndex(int index);

    /**
     * Change priority of the element <code>k</code> designed with its index
     * it was previously insert() before.
     */
    void changePriority(int index);

    /**
     * Returns true if the element <code>k</code> designed with its index
     * it was previously insert() before, exists in the queue.
     */
    boolean containsIndex(int index);

    /*! #if ($TemplateOptions.KTypeGeneric) !*/
    /**
     * Retreive, but not remove, the top element of the queue,
     * i.e. the min/max element with respect to the comparison criteria
     * (implementation defined)
     * of the queue. Returns null if the queue is empty.
     */
    /*! #else !*/
    /**
     * Retreive, but not remove, the top element of the queue,
     * i.e. the min/max element with respect to the comparison criteria
     * (implementation defined)
     * of the queue. Returns KType default value if empty
     */
    /*! #end !*/
    KType top();

    /*! #if ($TemplateOptions.KTypeGeneric) !*/
    /**
     * Retreive, and remove the top element of the queue,
     * i.e. the min/max element with respect to the comparison criteria
     * (implementation defined)
     * of the queue. Returns the removed element. Returns null if the queue is empty.
     */
    /*! #else !*/
    /**
     * Retreive, and remove the top element of the queue,
     * i.e. the min/max element with respect to the comparison criteria
     * (implementation defined)
     */
    /*! #end !*/
    KType popTop();

    /**
     * Applies a <code>procedure</code> to all indexed container elements. Returns the argument (any
     * subclass of {@link KTypeIndexedProcedure}. This lets the caller to call methods of the argument
     * by chaining the call (even if the argument is an anonymous type) to retrieve computed values
     */
    public <T extends KTypeIndexedProcedure<? super KType>> T indexedForEach(T procedure);

    /**
     * Applies a <code>predicate</code> to indexed container elements as long, as the predicate
     * returns <code>true</code>. The iteration is interrupted otherwise.
     */
    public <T extends KTypeIndexedPredicate<? super KType>> T indexedForEach(T predicate);
}
