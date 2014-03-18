package com.carrotsearch.hppc.predicates;

/**
 * A predicate that applies to <code>KType</code>, <code>VType</code> pairs.
 */
//${TemplateOptions.doNotGenerateKType("BOOLEAN")}
/*! ${TemplateOptions.generatedAnnotation} !*/
public interface KTypeVTypePredicate<KType, VType>
{
    boolean apply(KType key, VType value);
}
