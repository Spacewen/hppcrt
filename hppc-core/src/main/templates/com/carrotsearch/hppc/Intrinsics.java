package com.carrotsearch.hppc;

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
        return (T) new Object [arraySize];
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
        return (T) new Object [arraySize];
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
     * Primitive types are compared using <code>==</code>.
     */
    public static boolean equalsKType(final Object e1, final Object e2)
    {
        return e1 == null ? e2 == null : e1.equals(e2);
    }

    /**
     * Compare two keys by Comparable<T>.
     * Primitive types comparison result is <code>e1 - e2</code>.
     */
    public static <T extends Comparable<? super T>> int compareKType(final T e1, final T e2)
    {
        return e1.compareTo(e2);
    }

    /**
     * Compare two keys by Comparable<T>, unchecked without Comparable signature
     * Primitive types comparison result is <code>e1 - e2</code>.
     */
    @SuppressWarnings("unchecked")
    public static <T> int compareKTypeUnchecked(final T e1, final T e2)
    {
        return ((Comparable<? super T>) e1).compareTo(e2);
    }

    /**
     * Compare two keys by Comparable<T>, returns true if e1.compareTo(e2) > 0
     * Primitive types comparison result is <code>e1 > e2</code>.
     */
    public static <T extends Comparable<? super T>> boolean isCompSupKType(final T e1, final T e2)
    {
        return e1.compareTo(e2) > 0;
    }

    /**
     * Compare two keys by Comparable<T>, unchecked without signature. returns true if e1.compareTo(e2) > 0
     * Primitive types comparison result is <code>e1 > e2</code>.
     */
    @SuppressWarnings("unchecked")
    public static <T> boolean isCompSupKTypeUnchecked(final T e1, final T e2)
    {
        return ((Comparable<? super T>) e1).compareTo(e2) > 0;
    }

    /**
     * Compare two keys by Comparable<T>, returns true if e1.compareTo(e2) < 0
     * Primitive types comparison result is <code>e1 < e2</code>.
     */
    public static <T extends Comparable<? super T>> boolean isCompInfKType(final T e1, final T e2)
    {
        return e1.compareTo(e2) < 0;
    }

    /**
     * Compare two keys by Comparable<T>, unchecked without signature. returns true if e1.compareTo(e2) < 0
     * Primitive types comparison result is <code>e1 < e2</code>.
     */
    @SuppressWarnings("unchecked")
    public static <T> boolean isCompInfKTypeUnchecked(final T e1, final T e2)
    {
        return ((Comparable<? super T>) e1).compareTo(e2) < 0;
    }

    /**
     * Compare two keys by Comparable<T>, returns true if e1.compareTo(e2) == 0
     * Primitive types comparison result is <code>e1 == e2</code>.
     */
    public static <T extends Comparable<? super T>> boolean isCompEqualKType(final T e1, final T e2)
    {
        return e1.compareTo(e2) == 0;
    }

    /**
     * Compare two keys by Comparable<T>, unchecked without signature. returns true if e1.compareTo(e2) == 0
     * Primitive types comparison result is <code>e1 == e2</code>.
     */
    @SuppressWarnings("unchecked")
    public static <T> boolean isCompEqualKTypeUnchecked(final T e1, final T e2)
    {
        return ((Comparable<? super T>) e1).compareTo(e2) == 0;
    }

    /**
     * Compare two Objects for equivalence, using a {@link HashingStrategy}. Null references return <code>true</code>.
     * A null {@link HashingStrategy} is equivalent of calling {@link #equalsKType(Object e1, Object e2)}.
     */
    public static <T> boolean equalsKTypeHashStrategy(final T e1, final T e2, final HashingStrategy<T> customEquals)
    {
        return (e1 == null ? e2 == null : (customEquals ==null? e1.equals(e2) :customEquals.equals(e1, e2)));
    }


    /**
     * Compare two keys for equivalence. Null references return <code>true</code>.
     * Primitive types are compared using <code>==</code>.
     */
    public static boolean equalsVType(final Object e1, final Object e2)
    {
        return e1 == null ? e2 == null : e1.equals(e2);
    }

    /**
     * Move one index to the left, wrapping around buffer, wrapping around buffer of size modulus
     * Code is actually inlined in generated code
     */
    public static int oneLeft(final int index, final int modulus)
    {
        return (index >= 1) ? index - 1 : modulus - 1;
    }

    /**
     * Move one index to the right, wrapping around buffer of size modulus
     * Code is actually inlined in generated code
     */
    public static int oneRight(final int index, final int modulus)
    {
        return (index + 1 == modulus) ? 0 : index + 1;
    }

    /**
     * Buils a LinkList node value from its before an after links
     * Code is actually inlined in generated code
     * @param beforeIndex
     * @param afterIndex
     * @return long
     */
    public static long getLinkNodeValue(final int beforeIndex, final int afterIndex)
    {
        return ((long) beforeIndex << 32) | afterIndex;
    }

    public static int getLinkBefore(final long nodeValue)
    {
        return (int) (nodeValue >> 32);
    }

    public static int getLinkAfter(final long nodeValue)
    {
        return (int) (nodeValue & 0x00000000FFFFFFFFL);
    }

    public static long setLinkBeforeNodeValue(final long nodeValue, final int newBefore)
    {
        return ((long) newBefore << 32) | (nodeValue & 0x00000000FFFFFFFFL);
    }

    public static long setLinkAfterNodeValue(final long nodeValue, final int newAfter)
    {
        return newAfter | (nodeValue & 0xFFFFFFFF00000000L);
    }
}
