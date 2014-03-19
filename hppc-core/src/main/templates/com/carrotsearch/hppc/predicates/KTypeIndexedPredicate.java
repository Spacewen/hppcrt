package com.carrotsearch.hppc.predicates;

/**
 * A predicate that applies to indexed <code>KType</code> objects.
 */
/*! ${TemplateOptions.generatedAnnotation} !*/
public interface KTypeIndexedPredicate<KType>
{
    boolean apply(int index, KType value);
}
