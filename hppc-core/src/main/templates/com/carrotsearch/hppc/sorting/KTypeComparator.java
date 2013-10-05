package com.carrotsearch.hppc.sorting;

import java.util.Comparator;

/*! ${TemplateOptions.generatedAnnotation} !*/

public interface KTypeComparator<KType> /*! #if ($TemplateOptions.KTypeGeneric) !*/extends Comparator<KType> /*! #end !*/
{
    /**
     * Defines the relative ordering of e1 and e2:
     * @return 0 if e1 is "equal" to e2, or else < 0 if e1 is "smaller" than e2, or else  > 0 if e1 is "bigger" than e2
     */
    int compare(KType e1, KType e2);
}