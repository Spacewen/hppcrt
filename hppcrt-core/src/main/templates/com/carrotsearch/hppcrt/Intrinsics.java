package com.carrotsearch.hppcrt;

/**
 * Intrinsic methods that are fully functional for the generic ({@link Object}) versions
 * of collection classes, but are replaced with low-level corresponding structures for
 * primitive types.
 * 
 * <p><b>This class should not appear in the final distribution (all methods are replaced
 * in templates.</b></p>
 */
public final class Intrinsics
{
    private Intrinsics()
    {
        // no instances.
    }

    /**
     * Create and return an array of template objects (<code>Object</code>s in the generic
     * version, corresponding key-primitive type in the generated version).
     * 
     * @param arraySize The size of the array to return.
     */
    @SuppressWarnings("unchecked")
    public static <T> T newKTypeArray(final int arraySize)
    {
        return (T) new Object[arraySize];
    }

    /**
     * Create and return an array of template objects (<code>Object</code>s in the generic
     * version, corresponding value-primitive type in the generated version).
     * 
     * @param arraySize The size of the array to return.
     */
    @SuppressWarnings("unchecked")
    public static <T> T newVTypeArray(final int arraySize)
    {
        return (T) new Object[arraySize];
    }

    /**
     * Returns the default value for keys (<code>null</code> or <code>0</code>
     * for primitive types).
     * 
     * @see "http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6302954"
     */
    public static <T> T defaultKTypeValue()
    {
        return (T) null;
    }

    /**
     * Returns the default value for values (<code>null</code> or <code>0</code>
     * for primitive types).
     * 
     * @see "http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6302954"
     */
    public static <T> T defaultVTypeValue()
    {
        return (T) null;
    }

    /**
     * Compare two keys for equivalence. Null references return <code>true</code>.
     * Primitive types are compared using <code>==</code>, except for floating-point types
     * where they're compared by their actual representation bits as returned from
     * {@link Double#doubleToLongBits(double)} and {@link Float#floatToIntBits(float)}.
     */
    public static boolean equalsKType(final Object e1, final Object e2)
    {
        return e1 == null ? e2 == null : e1.equals(e2);
    }

    /**
     * Identical as {@link equalsKType} except that
     *  e1 Objects are assumed to be not-null.
     */
    public static boolean equalsKTypeNotNull(final Object e1, final Object e2)
    {
        return e1.equals(e2);
    }

    /**
     * Compare two keys for equivalence. Null references return <code>true</code>.
     * Primitive types are compared using <code>==</code>, except for floating-point types
     * where they're compared by their actual representation bits as returned from
     * {@link Double#doubleToLongBits(double)} and {@link Float#floatToIntBits(float)}.
     */
    public static boolean equalsVType(final Object e1, final Object e2)
    {
        return e1 == null ? e2 == null : e1.equals(e2);
    }

    /**
     * Identical as {@link equalsVType} except that
     * e1 Objects are assumed to be not-null.
     */
    public static boolean equalsVTypeNotNull(final Object e1, final Object e2)
    {
        return e1.equals(e2);
    }

    /**
     * Compare key e1 for equality with {@link #defaultKTypeValue}.
     */
    public static <T> boolean equalsKTypeDefault(final Object e1)
    {
        return e1 == (T) null;
    }

    /**
     * Compare two keys by Comparable<T>.
     * Primitive types comparison result is <code>e1 - e2</code>, except for floating-point types
     * where they're compared by their actual representation bits using the integrated comparison methods
     * {@link Double#compare(e1 , e2)} and {@link Float#compare(e1, e2)}.
     */
    public static <T extends Comparable<? super T>> int compareKType(final T e1, final T e2)
    {
        return e1.compareTo(e2);
    }

    /**
     * Compare two keys by Comparable<T>, unchecked without Comparable signature
     * Primitive types comparison result is <code>e1 - e2</code>, except for floating-point types
     * where they're compared by their actual representation bits using the integrated comparison methods
     * {@link Double#compare(e1 , e2)} and {@link Float#compare(e1, e2)}.
     */
    @SuppressWarnings("unchecked")
    public static <T> int compareKTypeUnchecked(final T e1, final T e2)
    {
        return ((Comparable<? super T>) e1).compareTo(e2);
    }

    /**
     * Compare two keys by Comparable<T>, returns true if e1.compareTo(e2) > 0
     * Primitive types comparison result is <code>e1 > e2</code>, except for floating-point types
     * where they're compared by their actual representation bits using the integrated comparison methods
     * {@link Double#compare(e1 , e2) > 0} and {@link Float#compare(e1, e2) > 0}.
     */
    public static <T extends Comparable<? super T>> boolean isCompSupKType(final T e1, final T e2)
    {
        return e1.compareTo(e2) > 0;
    }

    /**
     * Compare two keys by Comparable<T>, unchecked without signature. returns true if e1.compareTo(e2) > 0
     * Primitive types comparison result is <code>e1 > e2</code>, except for floating-point types
     * where they're compared by their actual representation bits using the integrated comparison methods
     * {@link Double#compare(e1 , e2) > 0} and {@link Float#compare(e1, e2) > 0}.
     */
    @SuppressWarnings("unchecked")
    public static <T> boolean isCompSupKTypeUnchecked(final T e1, final T e2)
    {
        return ((Comparable<? super T>) e1).compareTo(e2) > 0;
    }

    /**
     * Compare two keys by Comparable<T>, returns true if e1.compareTo(e2) < 0
     * Primitive types comparison result is <code>e1 < e2</code>, except for floating-point types
     * where they're compared by their actual representation bits using the integrated comparison methods
     * {@link Double#compare(e1 , e2) < 0} and {@link Float#compare(e1, e2) < 0}.
     */
    public static <T extends Comparable<? super T>> boolean isCompInfKType(final T e1, final T e2)
    {
        return e1.compareTo(e2) < 0;
    }

    /**
     * Compare two keys by Comparable<T>, unchecked without signature. returns true if e1.compareTo(e2) < 0
     * Primitive types comparison result is <code>e1 < e2</code>, except for floating-point types
     * where they're compared by their actual representation bits using the integrated comparison methods
     * {@link Double#compare(e1 , e2) < 0} and {@link Float#compare(e1, e2) < 0}.
     */
    @SuppressWarnings("unchecked")
    public static <T> boolean isCompInfKTypeUnchecked(final T e1, final T e2)
    {
        return ((Comparable<? super T>) e1).compareTo(e2) < 0;
    }

    /**
     * Compare two keys by Comparable<T>, returns true if e1.compareTo(e2) == 0
     * Primitive types comparison result is <code>e1 == e2</code>, except for floating-point types
     * where they're compared by their actual representation bits as returned from
     * {@link Double#doubleToLongBits(double)} and {@link Float#floatToIntBits(float)}.
     */
    public static <T extends Comparable<? super T>> boolean isCompEqualKType(final T e1, final T e2)
    {
        return e1.compareTo(e2) == 0;
    }

    /**
     * Compare two keys by Comparable<T>, unchecked without signature. returns true if e1.compareTo(e2) == 0
     * Primitive types comparison result is <code>e1 == e2</code>, except for floating-point types
     * where they're compared by their actual representation bits as returned from
     * {@link Double#doubleToLongBits(double)} and {@link Float#floatToIntBits(float)}.
     */
    @SuppressWarnings("unchecked")
    public static <T> boolean isCompEqualKTypeUnchecked(final T e1, final T e2)
    {
        return ((Comparable<? super T>) e1).compareTo(e2) == 0;
    }

}
