package com.carrotsearch.hppc;

import com.carrotsearch.hppc.predicates.*;
import com.carrotsearch.hppc.procedures.*;

/**
 * An Indexed Priority queue of <code>KType</code>s.
 * similar to a simplified map (K,V) = (index, element V)
 * with priority queue capabilities.
 */
// ${TemplateOptions.doNotGenerateKType("BOOLEAN")}
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
     * Delete the element designated with its index from the priority queue
     * Returns the deleted element if deletion was successful, or else the default value
     * if the element doesn't exists.
     */
    KType deleteIndex(int index);

    /**
     * Return the element designated with its index,
     * or returns the default value if
     * no such element exists.
     */
    KType getIndex(int index);

/*! #if ($TemplateOptions.KTypeGeneric) !*/
    /**
     * Call this to re-establish the correct
     * priority of the Object indexed by index, if present in priority queue, in case its
     * internal state has changed such that its priority has indeed also changed,
     * towards the comparison criteria.
     */
    void changePriority(int index);
/*! #end !*/

    /**
     * Returns true if the element designated with its index
     *  exists in the queue.
     */
    boolean containsIndex(int index);


    /**
     * Retrieve, but not remove, the top element of the queue,
     * i.e. the min/max element with respect to the comparison criteria
     * (implementation defined)
     * of the queue. Returns the default value if empty.
     */
    KType top();


    /**
     * Retrieve, and remove the top element of the queue,
     * i.e. the min/max element with respect to the comparison criteria
     * (implementation defined) Returns the default value if empty.
     */
    KType popTop();

    /**
     * Applies a <code>procedure</code> to all indexed container elements. Returns the argument (any
     * subclass of {@link KTypeIndexedProcedure}. This lets the caller to call methods of the argument
     * by chaining the call (even if the argument is an anonymous type) to retrieve computed values
     */
    public <T extends KTypeIndexedProcedure<? super KType>> T indexedForEach(T procedure);

    /**
     * Applies a <code>predicate</code> to indexed container elements as long as the predicate
     * returns <code>true</code>. The iteration is interrupted otherwise.
     */
    public <T extends KTypeIndexedPredicate<? super KType>> T indexedForEach(T predicate);

    /**
     * Removes all elements in this collection for which the
     * given indexed predicate returns <code>true</code>.
     * @return Returns the number of removed elements.
     */
    public int removeAll(KTypeIndexedPredicate<? super KType> indexedPredicate);

    /**
     * Keeps all elements in this collection for which the
     * given indexed predicate returns <code>true</code>.
     * @return Returns the number of removed elements.
     */
    public int retainAll(KTypeIndexedPredicate<? super KType> indexedPredicate);

    /**
     * Update priorities of all the elements of the queue, to re-establish the correct priorities
     * towards the comparison criteria.
     */
    void refreshPriorities();
}
