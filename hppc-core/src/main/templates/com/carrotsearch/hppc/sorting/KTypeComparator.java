package com.carrotsearch.hppc.sorting;

import java.util.Comparator;

/*! ${TemplateOptions.generatedAnnotation} !*/

public interface KTypeComparator<KType> /*! #if ($TemplateOptions.KTypeGeneric) !*/extends Comparator<KType> /*! #end !*/
{
    int compare(KType e1, KType e2);
}