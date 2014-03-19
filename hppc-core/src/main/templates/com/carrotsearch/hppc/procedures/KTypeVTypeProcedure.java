package com.carrotsearch.hppc.procedures;

/**
 * A procedure that applies to <code>KType</code>, <code>VType</code> pairs.
 */
//${TemplateOptions.doNotGenerateKType("BOOLEAN")}
/*! ${TemplateOptions.generatedAnnotation} !*/
public interface KTypeVTypeProcedure<KType, VType>
{
    void apply(KType key, VType value);
}
