package com.carrotsearch.hppcrt;

import com.carrotsearch.hppcrt.predicates.KTypePredicate;

/**
 * A collection allows basic, efficient operations on sets of elements
 * (difference and intersection).
 */
/*! ${TemplateOptions.generatedAnnotation} !*/
public interface KTypeCollection<KType> extends KTypeContainer<KType>
{
    /**
     * Removes all occurrences of <code>e</code> from this collection.
     * 
     * @param e Element to be removed from this collection, if present.
     * @return The number of removed elements as a result of this call.
     */
    int removeAll(KType e);

    /**
     * Removes all elements in this collection that are present
     * in <code>c</code>. Runs in time proportional to the number
     * of elements in this collection. Equivalent of sets difference.
     * 
     * @return Returns the number of removed elements.
     */
    int removeAll(KTypeLookupContainer<? extends KType> c);

    /**
     * Removes all elements in this collection for which the
     * given predicate returns <code>true</code>.
     * 
     * @return Returns the number of removed elements.
     */
    int removeAll(KTypePredicate<? super KType> predicate);

    /**
     * Keeps all elements in this collection that are present
     * in <code>c</code>. Runs in time proportional to the number
     * of elements in this collection. Equivalent of sets intersection.
     * 
     * @return Returns the number of removed elements.
     */
    int retainAll(KTypeLookupContainer<? extends KType> c);

    /**
     * Keeps all elements in this collection for which the
     * given predicate returns <code>true</code>.
     * 
     * @return Returns the number of removed elements.
     */
    int retainAll(KTypePredicate<? super KType> predicate);

    /**
     * Removes all elements from this collection.
     */
    void clear();
}
