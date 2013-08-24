package com.carrotsearch.hppc;

import java.util.Arrays;

import com.carrotsearch.hppc.cursors.KTypeCursor;
import com.carrotsearch.hppc.predicates.KTypePredicate;

/**
 * Common superclass for collections.
 */
/*! ${TemplateOptions.generatedAnnotation} !*/
abstract class AbstractKTypeCollection<KType> implements KTypeCollection<KType>
{
    protected KTypeContainer<KType> testContainer;
    protected KTypePredicate<? super KType> testPredicate;

    protected KTypePredicate<KType> containsTestPredicate = new KTypePredicate<KType>() {

        public boolean apply(KType k)
        {
            return testContainer.contains(k);
        }
    };

    protected KTypePredicate<KType> containsNegateTestPredicate = new KTypePredicate<KType>() {

        public boolean apply(KType k)
        {
            return !testContainer.contains(k);
        }
    };

    protected KTypePredicate<KType> negatePredicate = new KTypePredicate<KType>() {

        public boolean apply(KType k)
        {
            return !testPredicate.apply(k);
        }
    };

    /**
     * Default implementation uses a predicate for removal.
     */
    /* #if ($TemplateOptions.KTypeGeneric) */
    @SuppressWarnings("unchecked")
    /* #end */
    @Override
    public int removeAll(final KTypeLookupContainer<? extends KType> c)
    {
        // We know c holds sub-types of KType and we're not modifying c, so go unchecked.
        this.testContainer = (KTypeContainer<KType>) c;
        return this.removeAll(this.containsTestPredicate);
    }

    /**
     * Default implementation uses a predicate for retaining.
     */
    /* #if ($TemplateOptions.KTypeGeneric) */
    @SuppressWarnings("unchecked")
    /* #end */
    @Override
    public int retainAll(final KTypeLookupContainer<? extends KType> c)
    {
        // We know c holds sub-types of KType and we're not modifying c, so go unchecked.
        this.testContainer = (KTypeContainer<KType>) c;
        return this.removeAll(this.containsNegateTestPredicate);
    }

    /**
     * Default implementation redirects to {@link #removeAll(KTypePredicate)}
     * and negates the predicate.
     */
    @Override
    public int retainAll(final KTypePredicate<? super KType> predicate)
    {
        this.testPredicate = predicate;
        return this.removeAll(this.negatePredicate);
    }

    /*! #if ($TemplateOptions.KTypeGeneric) !*/
    /**
     * Default implementation for:
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public KType [] toArray(Class<? super KType> clazz)
    {
        final int size = size();

        final KType[] array = (KType[]) java.lang.reflect.Array.newInstance(clazz, size);

        return toArray(array);
    }
    /*! #end !*/

    /**
     * Default implementation for:
     * {@inheritDoc}
     */
    @Override
    public KType[] toArray(KType[] target)
    {
        assert target.length >= size() : "Target array must be >= " + size();

        int i = 0;
        //use default iterator capability
        for (KTypeCursor<KType> c : this)
        {
            target[i++] = c.value;
        }

        return target;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    /*! #if ($TemplateOptions.KTypePrimitive)
    public KType [] toArray()
    #else !*/
    public Object[] toArray()
    /*! #end !*/
    {
        return toArray(Intrinsics.<KType[]> newKTypeArray(size()));
    }

    /**
     * Convert the contents of this container to a human-friendly string.
     */
    @Override
    public String toString()
    {
        return Arrays.toString(this.toArray());
    }

    @Override
    public boolean isEmpty()
    {
        return size() == 0;
    }

}
