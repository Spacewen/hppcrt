package com.carrotsearch.hppc;

import org.junit.Rule;
import org.junit.rules.MethodRule;


/**
 * Unit helpers for <code>KType</code>.
 */
/*! ${TemplateOptions.generatedAnnotation} !*/
/*! #if ($TemplateOptions.KTypeGeneric) !*/
@SuppressWarnings("unchecked")
/*! #end !*/
public abstract class AbstractKTypeTest<KType>
{
    /**
     * Require assertions for all tests.
     */
    @Rule
    public MethodRule requireAssertions = new RequireAssertionsRule();

    /* Ready to use key values. */

    protected KType key0 = cast(0), k0 = key0;
    protected KType key1 = cast(1), k1 = key1;
    protected KType key2 = cast(2), k2 = key2;
    protected KType key3 = cast(3), k3 = key3;
    protected KType key4 = cast(4), k4 = key4;
    protected KType key5 = cast(5), k5 = key5;
    protected KType key6 = cast(6), k6 = key6;
    protected KType key7 = cast(7), k7 = key7;
    protected KType key8 = cast(8), k8 = key8;
    protected KType key9 = cast(9), k9 = key9;

    /**
     * Convert to target type from an integer used to test stuff.
     */
    public KType cast(int v)
    {
        /*! #if ($TemplateOptions.KTypePrimitive)
            return (KType) v;
            #else !*/
        // @SuppressWarnings("unchecked")
        KType k = (KType) (Object) v;
        return k;
        /*! #end !*/
    }

    /**
     * Convert to target type from an integer used to test stuff.
     * return a Comparable object, indeed a number
     */
    /*! #if ($TemplateOptions.KTypeGeneric) !*/
    public <KType extends Comparable<KType>> KType castComparable(int v)
    /*! #else
    public KType castComparable(int v)
    #end !*/
    {
        /*! #if ($TemplateOptions.KTypePrimitive)
            return (KType) v;
            #else !*/
        // @SuppressWarnings("unchecked")
        KType k = (KType) (Object) v;
        return k;
        /*! #end !*/
    }

    /**
     * Convert a KType to int, (KType being a boxed elementary type or a primitive), else
     * returns 0.
     */
    @SuppressWarnings("hiding")
    public <KType> int castType(KType type)
    {
        /*! #if ($TemplateOptions.KTypePrimitive)
        return (int) type;
        #else !*/
        long k = 0L;

        if (type instanceof Character) {

            k = ((Character)type).charValue();

        } else if (type instanceof Number) {

            k = ((Number) type).longValue();
        }

        return (int) k;
        /*! #end !*/
    }

    public KType [] asArray(int... ints)
    {
        KType [] values = Intrinsics.newKTypeArray(ints.length);
        for (int i = 0; i < ints.length; i++)
            values[i] = (KType) /*! #if ($TemplateOptions.KTypeGeneric) !*/ (Object) /*! #end !*/ ints[i];
        return values;
    }

    /**
     * Create a new array of a given type and copy the arguments to this array.
     * These elements are indeed Comparable Numbers
     * (deep copy)
     */
    public KType[] newArray(KType... elements)
    {
        KType[] values = Intrinsics.newKTypeArray(elements.length);
        for (int i = 0; i < elements.length; i++)
        {
            /*! #if ($TemplateOptions.KTypeGeneric) !*/
            if (elements[i] != null)
            {
                values[i] = castComparable(castType(elements[i]));
            }
            else
            {
                values[i] = null;
            }
            /*! #else
            values[i] =  (KType)elements[i];
            #end !*/
        }

        return values;
    }

    /**
     * Create a new array of Comparable a given type and copy the arguments to this array.
     * These elements are indeed Comparable Numbers
     * (deep copy)
     */
    /*! #if ($TemplateOptions.KTypeGeneric) !*/
    public Comparable[] newComparableArray(KType... elements)
    /*! #else
    KType[] newComparableArray(KType... elements)
    #end !*/
    {
        /*! #if ($TemplateOptions.KTypeGeneric) !*/
        Comparable[] values = new Comparable[elements.length];
        /*! #else
        KType[] values = Intrinsics.newKTypeArray(elements.length);
        #end !*/
        for (int i = 0; i < elements.length; i++)
        {
            /*! #if ($TemplateOptions.KTypeGeneric) !*/
            if (elements[i] != null)
            {
                values[i] = castComparable(castType(elements[i]));
            }
            else
            {
                values[i] = null;
            }
            /*! #else
            values[i] =  (KType)elements[i];
            #end !*/
        }

        return values;
    }
}
