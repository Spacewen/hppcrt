package com.carrotsearch.hppc;

import static com.carrotsearch.hppc.TestUtils.*;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Iterator;

import org.junit.*;

import com.carrotsearch.hppc.cursors.KTypeCursor;
import com.carrotsearch.hppc.mutables.IntHolder;
import com.carrotsearch.hppc.predicates.KTypePredicate;
import com.carrotsearch.hppc.procedures.KTypeProcedure;

/**
 * Unit tests for {@link KTypeArrayList}.
 */
/*! ${TemplateOptions.generatedAnnotation} !*/
public class KTypeArrayListTest<KType> extends AbstractKTypeTest<KType>
{
    /**
     * Per-test fresh initialized instance.
     */
    public KTypeArrayList<KType> list;

    public volatile long guard;

    /* */
    @Before
    public void initialize()
    {
        list = KTypeArrayList.newInstance();
    }

    @After
    public void checkTrailingSpaceUninitialized()
    {
        if (list != null)
        {
            for (int i = list.elementsCount; i < list.buffer.length; i++) {
                /*! #if ($TemplateOptions.KTypeGeneric) !*/
                assertTrue(Intrinsics.<KType> defaultKTypeValue() == list.buffer[i]);
                /*! #end !*/
            }
        }
    }

    /* */
    @Test
    public void testInitiallyEmpty()
    {
        assertEquals(0, list.size());
    }

    /* */
    @Test
    public void testAdd()
    {
        list.add(key1, key2);
        assertListEquals(list.toArray(), 1, 2);
    }

    /* */
    @Test
    public void testAddTwoArgs()
    {
        list.add(key1, key2);
        list.add(key3, key4);
        assertListEquals(list.toArray(), 1, 2, 3, 4);
    }

    /* */
    @Test
    public void testAddArray()
    {
        list.add(asArray(0, 1, 2, 3), 1, 2);
        assertListEquals(list.toArray(), 1, 2);
    }

    /* */
    /*! #if ($TemplateOptions.KTypeGeneric) !*/
    @SuppressWarnings("unchecked")
    /*! #end !*/
    @Test
    public void testAddVarArg()
    {
        list.add(asArray(0, 1, 2, 3));
        list.add(key4, key5, key6, key7);
        assertListEquals(list.toArray(), 0, 1, 2, 3, 4, 5, 6, 7);
    }

    /* */
    @Test
    public void testAddAll()
    {
        KTypeArrayList<KType> list2 = KTypeArrayList.newInstance();
        list2.add(asArray(0, 1, 2));

        list.addAll(list2);
        list.addAll(list2);

        assertListEquals(list.toArray(), 0, 1, 2, 0, 1, 2);
    }

    /*! #if ($TemplateOptions.KTypeGeneric) !*/
    @Test
    public void testAddAll_subclass()
    {
        class A {
        }

        class B extends A {
        }

        KTypeArrayList<B> list2 = new KTypeArrayList<B>();
        list2.add(new B());

        KTypeArrayList<A> list3 = new KTypeArrayList<A>();
        list3.add(new B());
        list3.add(new A());
        list3.addAll(list2);
        assertEquals(3, list3.size());
    }
    /*! #end !*/

    /* */
    @Test
    public void testInsert()
    {
        list.insert(0, k1);
        list.insert(0, k2);
        list.insert(2, k3);
        list.insert(1, k4);

        assertListEquals(list.toArray(), 2, 4, 1, 3);
    }

    /* */
    @Test
    public void testSet()
    {
        list.add(asArray(0, 1, 2));

        assertEquals2(0, list.set(0, k3));
        assertEquals2(1, list.set(1, k4));
        assertEquals2(2, list.set(2, k5));

        assertListEquals(list.toArray(), 3, 4, 5);
    }

    /* */
    @Test
    public void testRemove()
    {
        list.add(asArray(0, 1, 2, 3, 4));

        list.remove(0);
        list.remove(2);
        list.remove(1);

        assertListEquals(list.toArray(), 1, 4);
    }

    /* */
    @Test
    public void testRemoveRange()
    {
        list.add(asArray(0, 1, 2, 3, 4));

        list.removeRange(0, 2);
        assertListEquals(list.toArray(), 2, 3, 4);

        list.removeRange(2, 3);
        assertListEquals(list.toArray(), 2, 3);

        list.removeRange(1, 1);
        assertListEquals(list.toArray(), 2, 3);

        list.removeRange(0, 1);
        assertListEquals(list.toArray(), 3);
    }

    /* */
    @Test
    public void testRemoveFirstLast()
    {
        list.add(asArray(0, 1, 2, 1, 0));

        assertEquals(-1, list.removeFirstOccurrence(k5));
        assertEquals(-1, list.removeLastOccurrence(k5));
        assertListEquals(list.toArray(), 0, 1, 2, 1, 0);

        assertEquals(1, list.removeFirstOccurrence(k1));
        assertListEquals(list.toArray(), 0, 2, 1, 0);
        assertEquals(3, list.removeLastOccurrence(k0));
        assertListEquals(list.toArray(), 0, 2, 1);
        assertEquals(0, list.removeLastOccurrence(k0));
        assertListEquals(list.toArray(), 2, 1);
        assertEquals(-1, list.removeLastOccurrence(k0));

        /*! #if ($TemplateOptions.KTypeGeneric) !*/
        list.clear();
        list.add(newArray(k0, null, k2, null, k0));
        assertEquals(1, list.removeFirstOccurrence(null));
        assertEquals(2, list.removeLastOccurrence(null));
        assertListEquals(list.toArray(), 0, 2, 0);
        /*! #end !*/
    }

    /* */
    @Test
    public void testRemoveAll()
    {
        list.add(asArray(0, 1, 0, 1, 0));

        assertEquals(0, list.removeAllOccurrences(k2));
        assertEquals(3, list.removeAllOccurrences(k0));
        assertListEquals(list.toArray(), 1, 1);

        assertEquals(2, list.removeAllOccurrences(k1));
        assertTrue(list.isEmpty());

        /*! #if ($TemplateOptions.KTypeGeneric) !*/
        list.clear();
        list.add(newArray(k0, null, k2, null, k0));
        assertEquals(2, list.removeAllOccurrences(null));
        assertEquals(0, list.removeAllOccurrences(null));
        assertListEquals(list.toArray(), 0, 2, 0);
        /*! #end !*/
    }

    /* */
    @Test
    public void testRemoveAllFromLookupContainer()
    {
        list.add(asArray(0, 1, 2, 1, 0));

        KTypeOpenHashSet<KType> list2 = KTypeOpenHashSet.newInstance();
        list2.add(asArray(0, 2));

        assertEquals(3, list.removeAll(list2));
        assertEquals(0, list.removeAll(list2));

        assertListEquals(list.toArray(), 1, 1);
    }

    /* */
    @Test
    public void testRemoveAllWithPredicate()
    {
        list.add(newArray(k0, k1, k2, k1, k4));

        assertEquals(3, list.removeAll(new KTypePredicate<KType>()
                {
            public boolean apply(KType v)
            {
                return v == key1 || v == key2;
            };
                }));

        assertListEquals(list.toArray(), 0, 4);
    }

    /* */
    @Test
    public void testRetainAllWithPredicate()
    {
        list.add(newArray(k0, k1, k2, k1, k0));

        assertEquals(2, list.retainAll(new KTypePredicate<KType>()
                {
            public boolean apply(KType v)
            {
                return v == key1 || v == key2;
            };
                }));

        assertListEquals(list.toArray(), 1, 2, 1);
    }

    /* */
    @Test
    public void testRemoveAllWithPredicateInterrupted()
    {
        list.add(newArray(k0, k1, k2, k1, k4));

        final RuntimeException t = new RuntimeException();
        try
        {
            assertEquals(3, list.removeAll(new KTypePredicate<KType>()
                    {
                public boolean apply(KType v)
                {
                    if (v == key2) throw t;
                    return v == key1;
                };
                    }));
            fail();
        }
        catch (RuntimeException e)
        {
            // Make sure it's really our exception...
            if (e != t) throw e;
        }

        // And check if the list is in consistent state.
        assertListEquals(list.toArray(), 0, key2, key1, 4);
        assertEquals(4, list.size());
    }

    /* */
    @Test
    public void testIndexOf()
    {
        list.add(asArray(0, 1, 2, 1, 0));

        /*! #if ($TemplateOptions.KTypeGeneric) !*/
        list.add((KType) null);
        assertEquals(5, list.indexOf(null));
        /*! #end !*/

        assertEquals(0, list.indexOf(k0));
        assertEquals(-1, list.indexOf(k3));
        assertEquals(2, list.indexOf(k2));
    }

    /* */
    @Test
    public void testLastIndexOf()
    {
        list.add(asArray(0, 1, 2, 1, 0));

        /*! #if ($TemplateOptions.KTypeGeneric) !*/
        list.add((KType) null);
        assertEquals(5, list.lastIndexOf(null));
        /*! #end !*/

        assertEquals2(4, list.lastIndexOf(k0));
        assertEquals2(-1, list.lastIndexOf(k3));
        assertEquals2(2, list.lastIndexOf(k2));
    }

    /* */
    @Test
    public void testEnsureCapacity()
    {
        list.ensureCapacity(100);
        assertTrue(list.buffer.length >= 100);

        list.ensureCapacity(1000);
        list.ensureCapacity(1000);
        assertTrue(list.buffer.length >= 1000);
    }

    @Test
    public void testResizeAndCleanBuffer()
    {
        list.ensureCapacity(20);
        Arrays.fill(list.buffer, k1);

        list.resize(10);
        assertEquals(10, list.size());

        for (int i = 0; i < list.size(); i++) {

            assertEquals2(Intrinsics.<KType>defaultKTypeValue(), list.get(i));

        }

        Arrays.fill(list.buffer, Intrinsics.<KType>defaultKTypeValue());

        for (int i = 5; i < list.size(); i++)
            list.set(i, k1);

        list.resize(5);
        assertEquals(5, list.size());

        for (int i = list.size(); i < list.buffer.length; i++) {
            //only objects get cleared for GC sake.
            /*! #if ($TemplateOptions.KTypeGeneric) !*/
            assertEquals2(Intrinsics.<KType>defaultKTypeValue(), list.buffer[i]);
            /*! #end !*/
        }

    }

    /* */
    @Test
    public void testTrimToSize()
    {
        list.add(asArray(1, 2));
        list.trimToSize();
        assertEquals(2, list.buffer.length);
    }

    /* */
    @Test
    public void testRelease()
    {
        list.add(asArray(1, 2));
        list.release();
        assertEquals(0, list.size());
        list.add(asArray(1, 2));
        assertEquals(2, list.size());
    }

    /* */
    @Test
    public void testGrowth()
    {
        final int maxGrowth = 10;
        final int count = 500;

        list = new KTypeArrayList<KType>(0,
                new BoundedProportionalArraySizingStrategy(5, maxGrowth, 2));

        for (int i = 0; i < count; i++)
            list.add(cast(i));

        assertEquals(count, list.size());

        for (int i = 0; i < count; i++)
            assertEquals2(cast(i), list.get(i));

        assertTrue("Buffer size: 510 <= " + list.buffer.length,
                list.buffer.length <= count + maxGrowth);
    }

    /* */
    @Test
    public void testIterable()
    {
        list.add(asArray( 0, 1, 2, 3));
        int count = 0;
        for (KTypeCursor<KType> cursor : list)
        {
            count++;
            assertEquals2(list.get(cursor.index), cursor.value);
            assertEquals2(list.buffer[cursor.index], cursor.value);
        }
        assertEquals(count, list.size());

        count = 0;
        list.resize(0);
        for (@SuppressWarnings("unused") KTypeCursor<KType> cursor : list)
        {
            count++;
        }
        assertEquals(0, count);
    }

    /* */
    @Test
    public void testIterator()
    {
        list.add(asArray( 0, 1, 2, 3));
        Iterator<KTypeCursor<KType>> iterator = list.iterator();
        int count = 0;
        while (iterator.hasNext())
        {
            iterator.hasNext();
            iterator.hasNext();
            iterator.hasNext();
            iterator.next();
            count++;
        }
        assertEquals(count, list.size());

        list.resize(0);
        assertFalse(list.iterator().hasNext());
    }

    /* */
    @Test
    public void testForEachWithProcedure()
    {
        list.add(asArray( 1, 2, 3));
        final IntHolder holder = new IntHolder();
        list.forEach(new KTypeProcedure<KType>() {
            int index = 0;
            public void apply(KType v)
            {
                assertEquals2(v, list.get(index++));
                holder.value = index;
            }
        });
        assertEquals(holder.value, list.size());
    }

    /* */
    @Test
    public void testForEachReturnValueFromAnonymousClass()
    {
        list.add(asArray( 1, 2, 3));
        int result = list.forEach(new KTypeProcedure<KType>() {
            int index = 0;
            public void apply(KType v)
            {
                assertEquals2(v, list.get(index++));
            }
        }).index;
        assertEquals(result, list.size());
    }

    /* */
    @Test
    public void testClear()
    {
        list.add(asArray( 1, 2, 3));
        list.clear();
        checkTrailingSpaceUninitialized();
    }

    /* */
    /*! #if ($TemplateOptions.KTypeGeneric) !*/
    @SuppressWarnings("unchecked")
    /*! #end !*/
    @Test
    public void testFrom()
    {
        KTypeArrayList<KType> variable = KTypeArrayList.from(k1, k2, k3);
        assertEquals(3, variable.size());
        assertListEquals(variable.toArray(), 1, 2, 3);
    }

    /* */
    /*! #if ($TemplateOptions.KTypeGeneric) !*/
    @SuppressWarnings("unchecked")
    /*! #end !*/
    @Test
    public void testHashCodeEquals()
    {
        ObjectArrayList<Integer> l0 = ObjectArrayList.from();
        assertEquals(1, l0.hashCode());
        assertEquals(l0, ObjectArrayList.from());

        KTypeArrayList<KType> l1 = KTypeArrayList.from(k1, k2, k3);
        KTypeArrayList<KType> l2 = KTypeArrayList.from(k1, k2);
        l2.add(k3);

        assertEquals(l1.hashCode(), l2.hashCode());
        assertEquals(l1, l2);
    }

    /* */
    /*! #if ($TemplateOptions.KTypeGeneric) !*/
    @SuppressWarnings("unchecked")
    /*! #end !*/
    @Test
    public void testHashCodeEqualsWithOtherContainer()
    {
        KTypeStack<KType> l1 = KTypeStack.from(k1, k2, k3);
        KTypeArrayList<KType> l2 = KTypeArrayList.from(k1, k2);
        l2.add(k3);

        assertEquals(l1.hashCode(), l2.hashCode());
        assertEquals(l1, l2);
    }

    /*! #if ($TemplateOptions.KTypeGeneric) !*/
    @Test
    @SuppressWarnings("unchecked")
    public void testHashCodeWithNulls()
    {
        KTypeArrayList<KType> l1 = KTypeArrayList.from(k1, null, k3);
        KTypeArrayList<KType> l2 = KTypeArrayList.from(k1, null, k3);

        assertEquals(l1.hashCode(), l2.hashCode());
        assertEquals(l1, l2);
    }
    /*! #end !*/

    /*! #if ($TemplateOptions.KTypeGeneric) !*/
    @Test
    public void testToArrayWithClass()
    {
        KTypeArrayList<Integer> l1 = KTypeArrayList.from(1, 2, 3);
        Integer[] result = l1.toArray(Integer.class);
        assertArrayEquals(new Integer [] {1, 2, 3}, result); // dummy
    }
    /*! #end !*/

    /*! #if ($TemplateOptions.KTypeGeneric) !*/
    @SuppressWarnings("unchecked")
    @Test
    public void testToArray()
    {
        KTypeArrayList<KType> l1 = KTypeArrayList.from(k1, k2, k3);
        Object[] result = l1.toArray();
        assertArrayEquals(new Object [] {k1, k2, k3}, result);
    }
    /*! #end !*/

    /* */
    /*! #if ($TemplateOptions.KTypeGeneric) !*/
    @SuppressWarnings("unchecked")
    /*! #end !*/
    @Test
    public void testClone()
    {
        list.add(k1, k2, k3);

        KTypeArrayList<KType> cloned = list.clone();
        cloned.removeAllOccurrences(key1);

        assertSortedListEquals(list.toArray(), key1, key2, key3);
        assertSortedListEquals(cloned.toArray(), key2, key3);
    }

    /* */
    /*! #if ($TemplateOptions.KTypeGeneric) !*/
    @SuppressWarnings("unchecked")
    /*! #end !*/
    @Test
    public void testToString()
    {
        assertEquals("["
                + key1 + ", "
                + key2 + ", "
                + key3 + "]", KTypeArrayList.from(k1, k2, k3).toString());
    }

    @Test
    public void testPooledIteratorForEach()
    {
        // Unbroken for-each loop
        int TEST_SIZE = 10000;
        long TEST_ROUNDS = 100;

        KTypeArrayList<KType> testContainer = createArrayWithOrderedData(TEST_SIZE);

        long checksum = testContainer.forEach(new KTypeProcedure<KType>() {

            long count;

            @Override
            public void apply(KType value)
            {
                count += castType(value);
            }
        }).count;

        long testValue = 0;
        long initialPoolSize = testContainer.valueIteratorPool.size();

        for (int round = 0; round < TEST_ROUNDS; round++)
        {
            //for-each in test :
            testValue = 0;
            for (KTypeCursor<KType> cursor : testContainer)
            {
                //we consume 1 iterator for this loop
                assertEquals(initialPoolSize - 1, testContainer.valueIteratorPool.size());

                testValue += castType(cursor.value);
            }

            //check checksum the iteration
            assertEquals(checksum, testValue);

            //iterator is returned to its pool
            assertEquals(initialPoolSize, testContainer.valueIteratorPool.size());
        } //end for rounds
    }

    @Test
    public void testPooledIteratorBrokenForEach()
    {
        // for-each loop interrupted

        int TEST_SIZE = 10000;
        long TEST_ROUNDS = 100;

        KTypeArrayList<KType> testContainer = createArrayWithOrderedData(TEST_SIZE);

        int count = 0;
        for (int round = 0; round < TEST_ROUNDS; round++)
        {
            //for-each in test :
            long initialPoolSize = testContainer.valueIteratorPool.size();

            count = 0;
            for (KTypeCursor<KType> cursor : testContainer)
            {
                guard += castType(cursor.value);
                //we consume 1 iterator for this loop, but reallocs can happen,
                //so we can only say its != initialPoolSize
                assertTrue(initialPoolSize != testContainer.valueIteratorPool.size());

                //brutally interrupt in the middle
                if (count > TEST_SIZE / 2)
                {
                    break;
                }

                count++;
            } //end for-each

            //iterator is NOT returned to its pool, due to the break.
            //reallocation could happen, so that the only testable thing
            //is that the size is != full pool
            assertTrue(initialPoolSize != testContainer.valueIteratorPool.size());
        } //end for rounds

        //Due to policy of the Iterator pool, the intended pool never get bigger that some limit
        //despite the Iterator leak.
        assertTrue(testContainer.valueIteratorPool.capacity() < IteratorPool.MAX_SIZE_GROWTH_FACTOR * Internals.NB_OF_PROCESSORS + 1);
    }

    @Test
    public void testPooledIteratorFullIteratorLoop()
    {
        // for-each loop interrupted

        int TEST_SIZE = 10000;
        long TEST_ROUNDS = 100;

        KTypeArrayList<KType> testContainer = createArrayWithOrderedData(TEST_SIZE);

        long checksum = testContainer.forEach(new KTypeProcedure<KType>() {

            long count;

            @Override
            public void apply(KType value)
            {
                count += castType(value);
            }
        }).count;

        long testValue = 0;
        int startingPoolSize = testContainer.valueIteratorPool.size();

        for (int round = 0; round < TEST_ROUNDS; round++)
        {
            //Classical iterator loop, with manually allocated Iterator
            int initialPoolSize = testContainer.valueIteratorPool.size();

            Iterator<KTypeCursor<KType>> loopIterator = testContainer.iterator();

            assertEquals(initialPoolSize - 1, testContainer.valueIteratorPool.size());

            testValue = 0;
            while (loopIterator.hasNext())
            {
                testValue += castType(loopIterator.next().value);
            } //end IteratorLoop

            //iterator is returned automatically to its pool, by normal iteration termination
            assertEquals(initialPoolSize, testContainer.valueIteratorPool.size());

            //checksum
            assertEquals(checksum, testValue);
        } //end for rounds

        // pool initial size is untouched anyway
        assertEquals(startingPoolSize, testContainer.valueIteratorPool.size());
    }

    @Test
    public void testPooledIteratorBrokenIteratorLoop()
    {
        // for-each loop interrupted

        int TEST_SIZE = 10000;
        long TEST_ROUNDS = 100;

        KTypeArrayList<KType> testContainer = createArrayWithOrderedData(TEST_SIZE);
        int startingPoolSize = testContainer.valueIteratorPool.size();

        int count = 0;
        for (int round = 0; round < TEST_ROUNDS; round++)
        {
            //Classical iterator loop, with manually allocated Iterator
            long initialPoolSize = testContainer.valueIteratorPool.size();

            AbstractIterator<KTypeCursor<KType>> loopIterator = (AbstractIterator<KTypeCursor<KType>>) testContainer.iterator();

            assertEquals(initialPoolSize - 1, testContainer.valueIteratorPool.size());

            count = 0;
            while (loopIterator.hasNext())
            {
                guard += castType(loopIterator.next().value);

                //brutally interrupt in the middle
                if (count > TEST_SIZE / 2)
                {
                    break;
                }
                count++;
            } //end IteratorLoop

            //iterator is NOT returned to its pool, due to the break.
            assertEquals(initialPoolSize - 1, testContainer.valueIteratorPool.size());

            //manual return to the pool
            loopIterator.release();

            //now the pool is restored
            assertEquals(initialPoolSize, testContainer.valueIteratorPool.size());

        } //end for rounds

        // pool initial size is untouched anyway
        assertEquals(startingPoolSize, testContainer.valueIteratorPool.size());
    }

    @Test
    public void testPooledIteratorExceptionIteratorLoop()
    {
        int TEST_SIZE = 10000;
        long TEST_ROUNDS = 100;

        KTypeArrayList<KType> testContainer = createArrayWithOrderedData(TEST_SIZE);

        long checksum = testContainer.forEach(new KTypeProcedure<KType>() {

            long count;

            @Override
            public void apply(KType value)
            {
                count += castType(value);
            }
        }).count;

        int startingPoolSize = testContainer.valueIteratorPool.size();

        int count = 0;
        AbstractIterator<KTypeCursor<KType>> loopIterator = null;

        for (int round = 0; round < TEST_ROUNDS; round++)
        {
            try
            {
                loopIterator = (AbstractIterator<KTypeCursor<KType>>) testContainer.iterator();
                assertEquals(startingPoolSize - 1, testContainer.valueIteratorPool.size());

                guard = 0;
                count = 0;
                while (loopIterator.hasNext())
                {
                    guard += castType(loopIterator.next().value);

                    //brutally interrupt in the middle some of the loops, but not all
                    if (round > TEST_ROUNDS / 2 && count > TEST_SIZE / 2)
                    {
                        throw new Exception("Oups some problem in the loop occured");
                    }
                    count++;
                } //end while

                //iterator is returned to its pool in case of normal loop termination
                assertEquals(startingPoolSize, testContainer.valueIteratorPool.size());
                assertEquals(checksum, guard);

            } catch (Exception e)
            {
                //iterator is NOT returned to its pool because of the exception
                assertEquals(startingPoolSize - 1, testContainer.valueIteratorPool.size());

                //manual return to the pool then
                loopIterator.release();

                //now the pool is restored
                assertEquals(startingPoolSize, testContainer.valueIteratorPool.size());
            }
        } //end for rounds

        // pool initial size is untouched anyway
        assertEquals(startingPoolSize, testContainer.valueIteratorPool.size());
    }

    private KTypeArrayList<KType> createArrayWithOrderedData(int size)
    {
        KTypeArrayList<KType> newArray = KTypeArrayList.newInstanceWithCapacity(KTypeArrayList.DEFAULT_CAPACITY);

        for (int i = 0; i < size; i++)
        {

            newArray.add(cast(i));
        }

        return newArray;
    }

}
