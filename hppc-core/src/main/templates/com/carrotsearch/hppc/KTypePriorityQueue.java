package com.carrotsearch.hppc;

/**
 * A Priority queue of <code>KType</code>s.
 */
/*! ${TemplateOptions.doNotGenerateKType("BOOLEAN")} !*/
/*! ${TemplateOptions.generatedAnnotation} !*/
public interface KTypePriorityQueue<KType> extends KTypeCollection<KType>
{
    /**
     * Insert a <code>k</code> element in the priority queue
     * @param k
     */
    void insert(KType k);

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
     * Update priorities of all the elements of the queue, to re-establish the correct priorities
     * towards the comparison criteria.
     */
    void refreshPriorities();
}
