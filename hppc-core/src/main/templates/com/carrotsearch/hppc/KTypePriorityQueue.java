package com.carrotsearch.hppc;

/**
 * A Priority queue of <code>KType</code>s.
 */
/*! ${TemplateOptions.generatedAnnotation} !*/
public interface KTypePriorityQueue<KType> extends KTypeCollection<KType>
{
    /**
     * Insert a <code>k</code> in the priority queue
     * @param k
     */
    void insert(KType k);

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
     * Update priorities to re-establish the priority queue state,
     * whatever the previous elements state.
     */
    void refreshPriorities();
}
