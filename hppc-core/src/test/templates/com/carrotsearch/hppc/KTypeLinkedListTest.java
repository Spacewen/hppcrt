package com.carrotsearch.hppc;

import static com.carrotsearch.hppc.TestUtils.assertEquals2;
import static com.carrotsearch.hppc.TestUtils.assertListEquals;
import static com.carrotsearch.hppc.TestUtils.assertSortedListEquals;
import static com.carrotsearch.hppc.TestUtils.reverse;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Random;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.carrotsearch.hppc.KTypeLinkedList.ValueIterator;
import com.carrotsearch.hppc.cursors.*;
import com.carrotsearch.hppc.mutables.*;
import com.carrotsearch.hppc.predicates.*;
import com.carrotsearch.hppc.procedures.*;
import com.carrotsearch.hppc.sorting.*;

/**
 * Unit tests for {@link KTypeLinkedList}.
 */
//${TemplateOptions.doNotGenerateKType("BOOLEAN")}
/*! ${TemplateOptions.generatedAnnotation} !*/
public class KTypeLinkedListTest<KType> extends AbstractKTypeTest<KType>
{
    /**
     * Per-test fresh initialized instance.
     */
    public KTypeLinkedList<KType> list;

    public volatile long guard;

    /**
     * Some sequence values for tests.
     */
    private KTypeArrayList<KType> sequence;

    @BeforeClass
    public static void configure()
    {
        IteratorPool.configureInitialPoolSize(8);
    }

    /* */
    @Before
    public void initialize()
    {
        //create a vary small list to force reallocs.
        list = KTypeLinkedList.newInstanceWithCapacity(2);

        sequence = KTypeArrayList.newInstance();

        for (int i = 0; i < 10000; i++)
            sequence.add(cast(i));
    }

    @After
    public void checkConsistency()
    {
        if (list != null)
        {
            for (int i = list.elementsCount; i < list.buffer.length; i++) {
                /*! #if ($TemplateOptions.KTypeGeneric) !*/
                Assert.assertTrue(Intrinsics.<KType> defaultKTypeValue() == list.buffer[i]);
                /*! #end !*/
            }
        }
    }

    /* */
    @Test
    public void testInitiallyEmpty()
    {
        Assert.assertEquals(0, list.size());
    }

    /* */
    @Test
    public void testAdd()
    {
        list.add(key1, key2);
        TestUtils.assertListEquals(list.toArray(), 1, 2);
    }

    /* */
    @Test
    public void testAddTwoArgs()
    {
        list.add(key1, key2);
        list.add(key3, key4);
        TestUtils.assertListEquals(list.toArray(), 1, 2, 3, 4);
    }

    /* */
    @Test
    public void testAddArray()
    {
        list.add(asArray(0, 1, 2, 3), 1, 2);
        TestUtils.assertListEquals(list.toArray(), 1, 2);
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
        TestUtils.assertListEquals(list.toArray(), 0, 1, 2, 3, 4, 5, 6, 7);
    }

    /* */
    @Test
    public void testAddAll()
    {
        final KTypeLinkedList<KType> list2 = KTypeLinkedList.newInstance();
        list2.add(asArray(0, 1, 2));

        list.addAll(list2);
        list.addAll(list2);

        TestUtils.assertListEquals(list.toArray(), 0, 1, 2, 0, 1, 2);
    }

    /*! #if ($TemplateOptions.KTypeGeneric) !*/
    @Test
    public void testAddAll_subclass()
    {
        class A {
        }

        class B extends A {
        }

        final KTypeLinkedList<B> list2 = new KTypeLinkedList<B>();
        list2.add(new B());

        final KTypeLinkedList<A> list3 = new KTypeLinkedList<A>();
        list3.add(new B());
        list3.add(new A());
        list3.addAll(list2);
        Assert.assertEquals(3, list3.size());
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

        TestUtils.assertListEquals(list.toArray(), 2, 4, 1, 3);
    }

    /* */
    @Test
    public void testSet()
    {
        list.add(asArray(0, 1, 2));

        TestUtils.assertEquals2(0, list.set(0, k3));
        TestUtils.assertEquals2(1, list.set(1, k4));
        TestUtils.assertEquals2(2, list.set(2, k5));

        TestUtils.assertListEquals(list.toArray(), 3, 4, 5);
    }

    /* */
    @Test
    public void testRemove()
    {
        list.add(asArray(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));

        list.remove(0);
        list.remove(2);
        list.remove(1);
        list.remove(4);

        TestUtils.assertListEquals(list.toArray(), 1, 4, 5, 6, 8, 9);
    }

    /* */
    @Test
    public void testRemoveRange()
    {
        list.add(asArray(0, 1, 2, 3, 4));

        list.removeRange(0, 2);
        TestUtils.assertListEquals(list.toArray(), 2, 3, 4);

        list.removeRange(2, 3);
        TestUtils.assertListEquals(list.toArray(), 2, 3);

        list.removeRange(1, 1);
        TestUtils.assertListEquals(list.toArray(), 2, 3);

        list.removeRange(0, 1);
        TestUtils.assertListEquals(list.toArray(), 3);
    }

    /* */
    @Test
    public void testGotoIndex()
    {
        //fill with distinct values
        final int COUNT = (int) 1e4;

        for (int i = 0; i < COUNT; i++)
        {
            list.add(cast(i));
        }

        //check that we reach the good element, by index
        for (int i = 0; i < COUNT; i++)
        {
            Assert.assertEquals(castType(cast(i)), castType(list.buffer[list.gotoIndex(i)]));
        }
    }

    /* */
    @Test
    public void testRemoveFirstLast()
    {
        list.add(asArray(0, 1, 2, 1, 0));

        Assert.assertEquals(-1, list.removeFirstOccurrence(k5));
        Assert.assertEquals(-1, list.removeLastOccurrence(k5));
        TestUtils.assertListEquals(list.toArray(), 0, 1, 2, 1, 0);

        Assert.assertEquals(1, list.removeFirstOccurrence(k1));
        TestUtils.assertListEquals(list.toArray(), 0, 2, 1, 0);
        Assert.assertEquals(3, list.removeLastOccurrence(k0));
        TestUtils.assertListEquals(list.toArray(), 0, 2, 1);
        Assert.assertEquals(0, list.removeLastOccurrence(k0));
        TestUtils.assertListEquals(list.toArray(), 2, 1);
        Assert.assertEquals(-1, list.removeLastOccurrence(k0));

        /*! #if ($TemplateOptions.KTypeGeneric) !*/
        list.clear();
        list.add(newArray(k0, null, k2, null, k0));
        Assert.assertEquals(1, list.removeFirstOccurrence(null));
        Assert.assertEquals(2, list.removeLastOccurrence(null));
        TestUtils.assertListEquals(list.toArray(), 0, 2, 0);
        /*! #end !*/
    }

    /* */
    @Test
    public void testRemoveAll()
    {
        list.add(asArray(0, 1, 0, 1, 0));

        Assert.assertEquals(0, list.removeAllOccurrences(k2));
        Assert.assertEquals(3, list.removeAllOccurrences(k0));
        TestUtils.assertListEquals(list.toArray(), 1, 1);

        Assert.assertEquals(2, list.removeAllOccurrences(k1));
        Assert.assertTrue(list.isEmpty());

        /*! #if ($TemplateOptions.KTypeGeneric) !*/
        list.clear();
        list.add(newArray(k0, null, k2, null, k0));
        Assert.assertEquals(2, list.removeAllOccurrences(null));
        Assert.assertEquals(0, list.removeAllOccurrences(null));
        TestUtils.assertListEquals(list.toArray(), 0, 2, 0);
        /*! #end !*/
    }

    /* */
    @Test
    public void testRemoveAllEverything()
    {
        list.add(asArray(1, 1, 1, 1, 1));

        Assert.assertEquals(5, list.removeAllOccurrences(k1));
        Assert.assertEquals(0, list.size());

        //remove all a void
        Assert.assertEquals(0, list.removeAllOccurrences(k1));
        Assert.assertEquals(0, list.size());
    }

    /* */
    @Test
    public void testRemoveAllFromLookupContainer()
    {
        list.add(asArray(0, 1, 2, 1, 0));

        final KTypeOpenHashSet<KType> list2 = KTypeOpenHashSet.newInstance();
        list2.add(asArray(0, 2));

        Assert.assertEquals(3, list.removeAll(list2));
        Assert.assertEquals(0, list.removeAll(list2));

        TestUtils.assertListEquals(list.toArray(), 1, 1);
    }

    /* */
    @Test
    public void testRemoveAllFromLookupContainerEverything()
    {
        list.add(asArray(0, 1, 2, 1, 0));

        final KTypeOpenHashSet<KType> list2 = KTypeOpenHashSet.newInstance();
        list2.add(asArray(0, 1, 2));

        Assert.assertEquals(5, list.removeAll(list2));
        Assert.assertEquals(0, list.size());
        Assert.assertEquals(0, list.removeAll(list2));
    }

    /* */
    @Test
    public void testRemoveAllWithPredicate()
    {
        list.add(newArray(k0, k1, k2, k1, k4));

        Assert.assertEquals(3, list.removeAll(new KTypePredicate<KType>()
                {
            @Override
            public boolean apply(final KType v)
            {
                return v == key1 || v == key2;
            };
                }));

        TestUtils.assertListEquals(list.toArray(), 0, 4);
    }

    /* */
    @Test
    public void testRemoveAllWithPredicateEverything()
    {
        list.add(newArray(k0, k1, k2, k1, k4));

        Assert.assertEquals(5, list.removeAll(new KTypePredicate<KType>()
                {
            @Override
            public boolean apply(final KType v)
            {
                return true;
            };
                }));

        Assert.assertEquals(0, list.size());

        //try again
        Assert.assertEquals(0, list.removeAll(new KTypePredicate<KType>()
                {
            @Override
            public boolean apply(final KType v)
            {
                return true;
            };
                }));

        Assert.assertEquals(0, list.size());
    }

    /* */
    @Test
    public void testRetainAllWithPredicate()
    {
        list.add(newArray(k0, k1, k2, k1, k0));

        Assert.assertEquals(2, list.retainAll(new KTypePredicate<KType>()
                {
            @Override
            public boolean apply(final KType v)
            {
                return v == key1 || v == key2;
            };
                }));

        TestUtils.assertListEquals(list.toArray(), 1, 2, 1);
    }

    /* */
    @Test
    public void testRemoveAllWithPredicateInterrupted()
    {
        list.add(newArray(k0, k1, k2, k1, k4));

        final RuntimeException t = new RuntimeException();
        try
        {
            //the assert below should never be triggered because of the exception
            //so give it an invalid value in case the thing terminates  = initial size
            Assert.assertEquals(5, list.removeAll(new KTypePredicate<KType>()
                    {
                @Override
                public boolean apply(final KType v)
                {
                    if (v == key2)
                        throw t;
                    return v == key1;
                };
                    }));
            Assert.fail();
        }
        catch (final RuntimeException e)
        {
            // Make sure it's really our exception...
            if (e != t)
                throw e;
        }

        // And check if the list is in consistent state.
        TestUtils.assertListEquals(list.toArray(), 0, key2, key1, 4);
        Assert.assertEquals(4, list.size());
    }

    /* */
    @Test
    public void testIndexOf()
    {
        list.add(asArray(0, 1, 2, 1, 0));

        /*! #if ($TemplateOptions.KTypeGeneric) !*/
        list.add((KType) null);
        Assert.assertEquals(5, list.indexOf(null));
        /*! #end !*/

        Assert.assertEquals(0, list.indexOf(k0));
        Assert.assertEquals(-1, list.indexOf(k3));
        Assert.assertEquals(2, list.indexOf(k2));
    }

    /* */
    @Test
    public void testLastIndexOf()
    {
        list.add(asArray(0, 1, 2, 1, 0));

        /*! #if ($TemplateOptions.KTypeGeneric) !*/
        list.add((KType) null);
        Assert.assertEquals(5, list.lastIndexOf(null));
        /*! #end !*/

        TestUtils.assertEquals2(4, list.lastIndexOf(k0));
        TestUtils.assertEquals2(-1, list.lastIndexOf(k3));
        TestUtils.assertEquals2(2, list.lastIndexOf(k2));
    }

    /* */
    @Test
    public void testEnsureCapacity()
    {
        list.ensureCapacity(100);
        Assert.assertTrue(list.buffer.length >= 100);

        list.ensureCapacity(1000);
        list.ensureCapacity(1000);
        Assert.assertTrue(list.buffer.length >= 1000);
    }

    /* */
    @Test
    public void testForEachWithProcedure()
    {
        list.add(asArray(1, 2, 3));
        final IntHolder holder = new IntHolder();
        list.forEach(new KTypeProcedure<KType>() {
            int index = 0;

            @Override
            public void apply(final KType v)
            {
                TestUtils.assertEquals2(v, list.get(index++));
                holder.value = index;
            }
        });
        Assert.assertEquals(holder.value, list.size());
    }

    /* */
    @Test
    public void testForEachReturnValueFromAnonymousClass()
    {
        list.add(asArray(1, 2, 3));
        final int result = list.forEach(new KTypeProcedure<KType>() {
            int index = 0;

            @Override
            public void apply(final KType v)
            {
                TestUtils.assertEquals2(v, list.get(index++));
            }
        }).index;
        Assert.assertEquals(result, list.size());
    }

    /* */
    @Test
    public void testClear()
    {
        list.add(asArray(1, 2, 3));
        list.clear();
        checkConsistency();
    }

    /* */
    /*! #if ($TemplateOptions.KTypeGeneric) !*/
    @SuppressWarnings("unchecked")
    /*! #end !*/
    @Test
    public void testFrom()
    {
        final KTypeLinkedList<KType> variable = KTypeLinkedList.from(k1, k2, k3);
        Assert.assertEquals(3, variable.size());
        TestUtils.assertListEquals(variable.toArray(), 1, 2, 3);
    }

    /* */
    /*! #if ($TemplateOptions.KTypeGeneric) !*/
    @SuppressWarnings("unchecked")
    /*! #end !*/
    @Test
    public void testHashCodeEquals()
    {
        final ObjectArrayList<Integer> l0 = ObjectArrayList.from();
        Assert.assertEquals(1, l0.hashCode());
        Assert.assertEquals(l0, ObjectArrayList.from());

        final KTypeLinkedList<KType> l1 = KTypeLinkedList.from(k1, k2, k3);
        final KTypeLinkedList<KType> l2 = KTypeLinkedList.from(k1, k2);
        l2.add(k3);

        Assert.assertEquals(l1.hashCode(), l2.hashCode());
        Assert.assertEquals(l1, l2);
    }

    /*! #if ($TemplateOptions.KTypeGeneric) !*/
    @Test
    @SuppressWarnings("unchecked")
    public void testHashCodeWithNulls()
    {
        final KTypeLinkedList<KType> l1 = KTypeLinkedList.from(k1, null, k3);
        final KTypeLinkedList<KType> l2 = KTypeLinkedList.from(k1, null, k3);

        Assert.assertEquals(l1.hashCode(), l2.hashCode());
        Assert.assertEquals(l1, l2);
    }

    /*! #end !*/

    /*! #if ($TemplateOptions.KTypeGeneric) !*/
    @Test
    public void testToArrayWithClass()
    {
        final KTypeLinkedList<Integer> l1 = KTypeLinkedList.from(1, 2, 3);
        final Integer[] result = l1.toArray(Integer.class);
        Assert.assertArrayEquals(new Integer[] { 1, 2, 3 }, result); // dummy
    }

    /*! #end !*/

    /*! #if ($TemplateOptions.KTypeGeneric) !*/
    @SuppressWarnings("unchecked")
    @Test
    public void testToArray()
    {
        final KTypeLinkedList<KType> l1 = KTypeLinkedList.from(k1, k2, k3, k4, k5, k7);
        final Object[] result = l1.toArray();
        Assert.assertArrayEquals(new Object[] { k1, k2, k3, k4, k5, k7 }, result);
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

        final KTypeLinkedList<KType> cloned = list.clone();
        cloned.removeAllOccurrences(key1);

        TestUtils.assertSortedListEquals(list.toArray(), key1, key2, key3);
        TestUtils.assertSortedListEquals(cloned.toArray(), key2, key3);
    }

    /* */
    /*! #if ($TemplateOptions.KTypeGeneric) !*/
    @SuppressWarnings("unchecked")
    /*! #end !*/
    @Test
    public void testToString()
    {
        Assert.assertEquals("["
                + key1 + ", "
                + key2 + ", "
                + key3 + ", "
                + key1 + ", "
                + key7 + ", "
                + key4 + ", "
                + key3 + "]", KTypeLinkedList.from(k1, k2, k3, k1, k7, k4, k3).toString());
    }

    @Test
    public void testPooledIteratorForEach()
    {
        // Unbroken for-each loop
        final int TEST_SIZE = 10000;
        final long TEST_ROUNDS = 100;

        final KTypeLinkedList<KType> testContainer = createArrayWithOrderedData(TEST_SIZE);

        final long checksum = testContainer.forEach(new KTypeProcedure<KType>() {

            long count;

            @Override
            public void apply(final KType value)
            {
                count += castType(value);
            }
        }).count;

        long testValue = 0;
        final long initialPoolSize = testContainer.valueIteratorPool.size();

        for (int round = 0; round < TEST_ROUNDS; round++)
        {
            //for-each in test :
            testValue = 0;
            for (final KTypeCursor<KType> cursor : testContainer)
            {
                //we consume 1 iterator for this loop
                Assert.assertEquals(initialPoolSize - 1, testContainer.valueIteratorPool.size());

                testValue += castType(cursor.value);
            }

            //check checksum the iteration
            Assert.assertEquals(checksum, testValue);

            //iterator is returned to its pool
            Assert.assertEquals(initialPoolSize, testContainer.valueIteratorPool.size());
        } //end for rounds
    }

    @Test
    public void testPooledIteratorBrokenForEach()
    {
        // for-each loop interrupted

        final int TEST_SIZE = 10000;
        final long TEST_ROUNDS = 100;

        final KTypeLinkedList<KType> testContainer = createArrayWithOrderedData(TEST_SIZE);

        int count = 0;
        for (int round = 0; round < TEST_ROUNDS; round++)
        {
            //for-each in test :
            final long initialPoolSize = testContainer.valueIteratorPool.size();

            count = 0;
            for (final KTypeCursor<KType> cursor : testContainer)
            {
                guard += castType(cursor.value);
                //we consume 1 iterator for this loop, but reallocs can happen,
                //so we can only say its != initialPoolSize
                Assert.assertTrue(initialPoolSize != testContainer.valueIteratorPool.size());

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
            Assert.assertTrue(initialPoolSize != testContainer.valueIteratorPool.size());
        } //end for rounds

        //Due to policy of the Iterator pool, the intended pool never get bigger that some limit
        //despite the Iterator leak.
        Assert.assertTrue(testContainer.valueIteratorPool.capacity() < IteratorPool.getMaxPoolSize() + 1);
    }

    @Test
    public void testPooledIteratorFullIteratorLoop()
    {
        // for-each loop interrupted

        final int TEST_SIZE = 10000;
        final long TEST_ROUNDS = 100;

        final KTypeLinkedList<KType> testContainer = createArrayWithOrderedData(TEST_SIZE);

        final long checksum = testContainer.forEach(new KTypeProcedure<KType>() {

            long count;

            @Override
            public void apply(final KType value)
            {
                count += castType(value);
            }
        }).count;

        long testValue = 0;
        final int startingPoolSize = testContainer.valueIteratorPool.size();

        for (int round = 0; round < TEST_ROUNDS; round++)
        {
            //Classical iterator loop, with manually allocated Iterator
            final int initialPoolSize = testContainer.valueIteratorPool.size();

            final KTypeLinkedList<KType>.ValueIterator loopIterator = testContainer.iterator();

            Assert.assertEquals(initialPoolSize - 1, testContainer.valueIteratorPool.size());

            testValue = 0;
            while (loopIterator.hasNext())
            {
                testValue += castType(loopIterator.next().value);
            } //end IteratorLoop

            //iterator is returned automatically to its pool, by normal iteration termination
            Assert.assertEquals(initialPoolSize, testContainer.valueIteratorPool.size());

            //checksum
            Assert.assertEquals(checksum, testValue);
        } //end for rounds

        // pool initial size is untouched anyway
        Assert.assertEquals(startingPoolSize, testContainer.valueIteratorPool.size());
    }

    @Test
    public void testPooledIteratorBrokenIteratorLoop()
    {
        // for-each loop interrupted

        final int TEST_SIZE = 10000;
        final long TEST_ROUNDS = 100;

        final KTypeLinkedList<KType> testContainer = createArrayWithOrderedData(TEST_SIZE);
        final int startingPoolSize = testContainer.valueIteratorPool.size();

        int count = 0;
        for (int round = 0; round < TEST_ROUNDS; round++)
        {
            //Classical iterator loop, with manually allocated Iterator
            final long initialPoolSize = testContainer.valueIteratorPool.size();

            final KTypeLinkedList<KType>.ValueIterator loopIterator = testContainer.iterator();

            Assert.assertEquals(initialPoolSize - 1, testContainer.valueIteratorPool.size());

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
            Assert.assertEquals(initialPoolSize - 1, testContainer.valueIteratorPool.size());

            //manual return to the pool
            loopIterator.release();

            //now the pool is restored
            Assert.assertEquals(initialPoolSize, testContainer.valueIteratorPool.size());

        } //end for rounds

        // pool initial size is untouched anyway
        Assert.assertEquals(startingPoolSize, testContainer.valueIteratorPool.size());
    }

    @Test
    public void testPooledIteratorExceptionIteratorLoop()
    {
        final int TEST_SIZE = 10000;
        final long TEST_ROUNDS = 100;

        final KTypeLinkedList<KType> testContainer = createArrayWithOrderedData(TEST_SIZE);

        final long checksum = testContainer.forEach(new KTypeProcedure<KType>() {

            long count;

            @Override
            public void apply(final KType value)
            {
                count += castType(value);
            }
        }).count;

        final int startingPoolSize = testContainer.valueIteratorPool.size();

        int count = 0;
        KTypeLinkedList<KType>.ValueIterator loopIterator = null;

        for (int round = 0; round < TEST_ROUNDS; round++)
        {
            try
            {
                loopIterator = testContainer.iterator();
                Assert.assertEquals(startingPoolSize - 1, testContainer.valueIteratorPool.size());

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
                Assert.assertEquals(startingPoolSize, testContainer.valueIteratorPool.size());
                Assert.assertEquals(checksum, guard);

            }
            catch (final Exception e)
            {
                //iterator is NOT returned to its pool because of the exception
                Assert.assertEquals(startingPoolSize - 1, testContainer.valueIteratorPool.size());

                //manual return to the pool then
                loopIterator.release();

                //now the pool is restored
                Assert.assertEquals(startingPoolSize, testContainer.valueIteratorPool.size());
            }
        } //end for rounds

        // pool initial size is untouched anyway
        Assert.assertEquals(startingPoolSize, testContainer.valueIteratorPool.size());
    }

    @Test
    public void testPooledIteratorExceptionSafe()
    {
        final int TEST_SIZE = 224171;
        final long TEST_ROUNDS = 15;

        final KTypeLinkedList<KType> testContainer = createArrayWithOrderedData(TEST_SIZE);

        final long checksum = testContainer.forEach(new KTypeProcedure<KType>() {

            long count;

            @Override
            public void apply(final KType value)
            {
                count += castType(value);
            }
        }).count;

        final int initialPoolSize = testContainer.valueIteratorPool.size();

        //start with a non full pool, remove 3 elements
        final AbstractIterator<KTypeCursor<KType>> loopIteratorFake = testContainer.iterator();
        final AbstractIterator<KTypeCursor<KType>> loopIteratorFake2 = testContainer.iterator();
        final AbstractIterator<KTypeCursor<KType>> loopIteratorFake3 = testContainer.iterator();

        final int startingTestPoolSize = testContainer.valueIteratorPool.size();

        Assert.assertEquals(initialPoolSize - 3, startingTestPoolSize);

        int count = 0;
        KTypeLinkedList<KType>.ValueIterator loopIterator = null;

        for (int round = 0; round < TEST_ROUNDS; round++)
        {
            try
            {
                loopIterator = testContainer.iterator();
                Assert.assertEquals(startingTestPoolSize - 1, testContainer.valueIteratorPool.size());

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
                Assert.assertEquals(startingTestPoolSize, testContainer.valueIteratorPool.size());
                Assert.assertEquals(checksum, guard);

                //still, try to return it ....
                loopIterator.release();

                //nothing has changed
                Assert.assertEquals(startingTestPoolSize, testContainer.valueIteratorPool.size());
            }
            catch (final Exception e)
            {
                //iterator is NOT returned to its pool because of the exception
                Assert.assertEquals(startingTestPoolSize - 1, testContainer.valueIteratorPool.size());

                //manual return to the pool then
                loopIterator.release();

                //now the pool is restored
                Assert.assertEquals(startingTestPoolSize, testContainer.valueIteratorPool.size());

                //continue to try to release...
                loopIterator.release();

                //nothing has changed
                Assert.assertEquals(startingTestPoolSize, testContainer.valueIteratorPool.size());
            }
        } //end for rounds

        // pool initial size is untouched anyway
        Assert.assertEquals(startingTestPoolSize, testContainer.valueIteratorPool.size());

        //finally return the fake ones, several times
        loopIteratorFake.release();
        loopIteratorFake.release();
        loopIteratorFake.release();
        loopIteratorFake2.release();
        loopIteratorFake2.release();
        loopIteratorFake2.release();
        loopIteratorFake3.release();
        loopIteratorFake3.release();

        Assert.assertEquals(initialPoolSize, testContainer.valueIteratorPool.size());
    }

    @Test
    public void testSort()
    {
        //natural ordering comparator
        final KTypeComparator<KType> comp = new KTypeComparator<KType>() {

            @Override
            public int compare(final KType e1, final KType e2)
            {
                int res = 0;

                if (castType(e1) < castType(e2))
                {
                    res = -1;
                }
                else if (castType(e1) > castType(e2))
                {
                    res = 1;
                }

                return res;
            }
        };

        final int TEST_SIZE = (int) 1e6;
        //A) Sort an array of random values of primitive types

        /*! #if ($TemplateOptions.KTypePrimitive)
        //A-1) full sort
        KTypeLinkedList<KType> primitiveList = createArrayWithRandomData(TEST_SIZE, 1515411541215L);
        primitiveList.sort();
        assertOrder(primitiveList);
        #end !*/

        //B) Sort with Comparator
        //B-1) Full sort
        final KTypeLinkedList<KType> comparatorList = createArrayWithRandomData(TEST_SIZE, 4871164545215L);
        comparatorList.sort(comp);
        assertOrder(comparatorList);
    }

    private KTypeLinkedList<KType> createArrayWithOrderedData(final int size)
    {
        final KTypeLinkedList<KType> newArray = KTypeLinkedList.newInstanceWithCapacity(KTypeLinkedList.DEFAULT_CAPACITY);

        for (int i = 0; i < size; i++)
        {
            newArray.add(cast(i));
        }

        return newArray;
    }

    private KTypeLinkedList<KType> createArrayWithRandomData(final int size, final long randomSeed)
    {
        final Random prng = new Random(randomSeed);

        final KTypeLinkedList<KType> newArray = KTypeLinkedList.newInstanceWithCapacity(KTypeLinkedList.DEFAULT_CAPACITY);

        for (int i = 0; i < size; i++)
        {
            newArray.add(cast(prng.nextInt()));
        }

        return newArray;
    }

    ////////////////////////////////// Dequeue-like tests //////////////////////////////////////////////////////////////////

    /* */
    @Test
    public void testAddFirst()
    {
        list.addFirst(k1);
        list.addFirst(k2);
        list.addFirst(k3);
        list.addFirst(k7);
        list.addFirst(k1);
        list.addFirst(k4);
        list.addFirst(k5);
        TestUtils.assertListEquals(list.toArray(), 5, 4, 1, 7, 3, 2, 1);
        Assert.assertEquals(7, list.size());
    }

    /* */
    @Test
    public void testAddLast()
    {
        list.addLast(k1);
        list.addLast(k2);
        list.addLast(k3);
        list.addLast(k7);
        list.addLast(k1);
        list.addLast(k4);
        list.addLast(k5);
        TestUtils.assertListEquals(list.toArray(), 1, 2, 3, 7, 1, 4, 5);
        Assert.assertEquals(7, list.size());
    }

    /* */
    @Test
    public void testAddWithGrowth()
    {
        for (int i = 0; i < sequence.size(); i++)
        {
            list.addFirst(sequence.buffer[i]);
        }

        TestUtils.assertListEquals(TestUtils.reverse(sequence.toArray()), list.toArray());
    }

    /* */
    @Test
    public void testAddLastWithGrowth()
    {
        for (int i = 0; i < sequence.size(); i++)
            list.addLast(sequence.buffer[i]);

        TestUtils.assertListEquals(sequence.toArray(), list.toArray());
    }

    /* */
    @Test
    public void testAddAllFirst()
    {
        final KTypeArrayList<KType> list2 = KTypeArrayList.newInstance();
        list2.add(asArray(0, 1, 2));

        list.addFirst(list2);
        TestUtils.assertListEquals(list.toArray(), 2, 1, 0);
        list.addFirst(list2);
        TestUtils.assertListEquals(list.toArray(), 2, 1, 0, 2, 1, 0);

        list.clear();
        final KTypeArrayDeque<KType> deque2 = new KTypeArrayDeque<KType>();
        deque2.addLast(asArray(0, 1, 2));
        list.addFirst(deque2);
        TestUtils.assertListEquals(list.toArray(), 2, 1, 0);
    }

    /* */
    @Test
    public void testAddAllLast()
    {
        final KTypeArrayList<KType> list2 = KTypeArrayList.newInstance();
        list2.add(asArray(0, 1, 2));

        list.addLast(list2);
        TestUtils.assertListEquals(list.toArray(), 0, 1, 2);
        list.addLast(list2);
        TestUtils.assertListEquals(list.toArray(), 0, 1, 2, 0, 1, 2);

        list.clear();
        final KTypeArrayDeque<KType> deque2 = new KTypeArrayDeque<KType>();
        deque2.addLast(asArray(0, 1, 2));
        list.addLast(deque2);
        TestUtils.assertListEquals(list.toArray(), 0, 1, 2);
    }

    /* */
    @Test
    public void testRemoveFirst()
    {
        list.addLast(k1);
        list.addLast(k2);
        list.addLast(k3);

        list.removeFirst();
        TestUtils.assertListEquals(list.toArray(), 2, 3);
        Assert.assertEquals(2, list.size());

        list.addFirst(k4);
        TestUtils.assertListEquals(list.toArray(), 4, 2, 3);
        Assert.assertEquals(3, list.size());

        list.removeFirst();
        list.removeFirst();
        list.removeFirst();
        Assert.assertEquals(0, list.toArray().length);
        Assert.assertEquals(0, list.size());
    }

    /* */
    @Test(expected = AssertionError.class)
    public void testRemoveFirstEmpty()
    {
        list.removeFirst();
    }

    /* */
    @Test
    public void testRemoveLast()
    {
        list.addLast(k1);
        list.addLast(k2);
        list.addLast(k3);

        list.removeLast();
        TestUtils.assertListEquals(list.toArray(), 1, 2);
        Assert.assertEquals(2, list.size());

        list.addLast(k4);
        TestUtils.assertListEquals(list.toArray(), 1, 2, 4);
        Assert.assertEquals(3, list.size());

        list.removeLast();
        list.removeLast();
        list.removeLast();
        Assert.assertEquals(0, list.toArray().length);
        Assert.assertEquals(0, list.size());
    }

    /* */
    @Test(expected = AssertionError.class)
    public void testRemoveLastEmpty()
    {
        list.removeLast();
    }

    /* */
    @Test
    public void testGetFirst()
    {
        list.addLast(k1);
        list.addLast(k2);
        TestUtils.assertEquals2(k1, list.getFirst());
        list.addFirst(k3);
        TestUtils.assertEquals2(k3, list.getFirst());
    }

    /* */
    @Test(expected = AssertionError.class)
    public void testGetFirstEmpty()
    {
        list.getFirst();
    }

    /* */
    @Test
    public void testGetLast()
    {
        list.addLast(k1);
        list.addLast(k2);
        TestUtils.assertEquals2(k2, list.getLast());
        list.addLast(k3);
        TestUtils.assertEquals2(k3, list.getLast());
    }

    /* */
    @Test(expected = AssertionError.class)
    public void testGetLastEmpty()
    {
        list.getLast();
    }

    /* */
    @Test
    public void testRemoveFirstOccurrence()
    {
        final int modulo = 10;
        final int count = 10000;
        sequence.clear();
        for (int i = 0; i < count; i++)
        {
            list.addLast(cast(i % modulo));
            sequence.add(cast(i % modulo));
        }

        TestUtils.assertListEquals(list.toArray(), sequence.toArray());

        final Random rnd = new Random(0xdeadbeef);
        for (int i = 0; i < 500; i++)
        {
            final KType k = cast(rnd.nextInt(modulo));
            Assert.assertEquals(
                    list.removeFirstOccurrence(k) >= 0,
                    sequence.removeFirstOccurrence(k) >= 0);
        }

        TestUtils.assertListEquals(list.toArray(), sequence.toArray());

        Assert.assertTrue(0 > list.removeFirstOccurrence(cast(modulo + 1)));
        list.addLast(cast(modulo + 1));
        Assert.assertTrue(0 <= list.removeFirstOccurrence(cast(modulo + 1)));
    }

    /* */
    @Test
    public void testRemoveLastOccurrence()
    {
        final int modulo = 10;
        final int count = 10000;
        sequence.clear();

        for (int i = 0; i < count; i++)
        {
            list.addLast(cast(i % modulo));
            sequence.add(cast(i % modulo));
        }

        TestUtils.assertListEquals(list.toArray(), sequence.toArray());

        final Random rnd = new Random(0x11223344);

        for (int i = 0; i < 500; i++)
        {
            final KType k = cast(rnd.nextInt(modulo));
            Assert.assertEquals(
                    list.removeLastOccurrence(k) >= 0,
                    sequence.removeLastOccurrence(k) >= 0);
        }

        TestUtils.assertListEquals(list.toArray(), sequence.toArray());

        int removedIndex = list.removeLastOccurrence(cast(modulo + 1));
        Assert.assertTrue("removeLastOccurrence(cast(modulo + 1) = " + removedIndex, removedIndex < 0);

        list.addFirst(cast(modulo + 1));

        removedIndex = list.removeLastOccurrence(cast(modulo + 1));
        Assert.assertTrue("removeLastOccurrence(cast(modulo + 1) = " + removedIndex, removedIndex == 0);
    }

    /* */
    @Test
    public void testRemoveAllOccurrences()
    {
        list.add(asArray(0, 1, 2, 1, 0, 3, 0));

        Assert.assertEquals(0, list.removeAllOccurrences(k4));
        Assert.assertEquals(3, list.removeAllOccurrences(k0));
        TestUtils.assertListEquals(list.toArray(), 1, 2, 1, 3);
        Assert.assertEquals(1, list.removeAllOccurrences(k3));
        TestUtils.assertListEquals(list.toArray(), 1, 2, 1);
        Assert.assertEquals(2, list.removeAllOccurrences(k1));
        TestUtils.assertListEquals(list.toArray(), 2);
        Assert.assertEquals(1, list.removeAllOccurrences(k2));
        Assert.assertEquals(0, list.size());
    }

    /* */
    @Test
    public void testRemoveAllInLookupContainer()
    {
        list.add(asArray(0, 1, 2, 1, 0));

        final KTypeOpenHashSet<KType> set = KTypeOpenHashSet.newInstance();
        set.add(asArray(0, 2));

        Assert.assertEquals(3, list.removeAll(set));
        Assert.assertEquals(0, list.removeAll(set));

        TestUtils.assertListEquals(list.toArray(), 1, 1);
    }

    /* */
    @Test
    public void testClear2()
    {
        list.addLast(k1);
        list.addLast(k2);
        list.addFirst(k3);
        list.clear();
        Assert.assertEquals(0, list.size());

        list.addLast(k1);
        TestUtils.assertListEquals(list.toArray(), 1);
    }

    /* */
    @Test
    public void testIterable()
    {
        list.addLast(sequence);

        int count = 0;
        for (final KTypeCursor<KType> cursor : list)
        {
            TestUtils.assertEquals2(sequence.buffer[count], cursor.value);
            TestUtils.assertEquals2(count, cursor.index);
            count++;
        }
        Assert.assertEquals(count, list.size());
        Assert.assertEquals(count, sequence.size());

        count = 0;
        list.clear();
        for (@SuppressWarnings("unused")
        final KTypeCursor<KType> cursor : list)
        {
            count++;
        }
        Assert.assertEquals(0, count);
    }

    /* */
    @Test
    public void testIterator()
    {
        list.addLast(asArray(0, 1, 2, 3));

        final Iterator<KTypeCursor<KType>> iterator = list.iterator();
        int count = 0;
        while (iterator.hasNext())
        {
            iterator.hasNext();
            iterator.hasNext();
            iterator.hasNext();
            iterator.next();
            count++;
        }
        Assert.assertEquals(count, list.size());

        list.clear();
        Assert.assertFalse(list.iterator().hasNext());
    }

    /* */
    @Test
    public void testDescendingIterator()
    {
        list.addLast(sequence);

        int index = sequence.size() - 1;
        for (final Iterator<KTypeCursor<KType>> i = list.descendingIterator(); i.hasNext();)
        {
            final KTypeCursor<KType> cursor = i.next();
            TestUtils.assertEquals2(sequence.buffer[index], cursor.value);
            TestUtils.assertEquals2(index, cursor.index);
            index--;
        }
        Assert.assertEquals(-1, index);

        list.clear();
        Assert.assertFalse(list.descendingIterator().hasNext());
    }

    /* */
    @Test
    public void testDescendingForEachWithProcedure()
    {
        list.addLast(sequence);

        final IntHolder count = new IntHolder();
        list.descendingForEach(new KTypeProcedure<KType>() {
            int index = sequence.size();

            @Override
            public void apply(final KType v)
            {
                TestUtils.assertEquals2(sequence.buffer[--index], v);
                count.value++;
            }
        });
        Assert.assertEquals(count.value, list.size());
    }

    ///////////////////////////////// iteration special methods ////////////////////////////////////

    /* */
    @Test
    public void testIterationHeadTail()
    {
        list.add(asArray(0, 11, 22, 33, 44, 55));

        final KTypeLinkedList<KType>.ValueIterator it = list.iterator();

        Assert.assertEquals(-1, it.cursor.index);
        Assert.assertTrue(it.isHead());
        Assert.assertFalse(it.isFirst());
        Assert.assertFalse(it.isLast());
        Assert.assertFalse(it.isTail());
        //obtain next
        Assert.assertEquals(null, it.getPrevious());
        Assert.assertEquals(0, castType(it.getNext().value));
        Assert.assertEquals(0, it.getNext().index);

        //Try to move backwards, we stay in head
        it.gotoPrevious();
        Assert.assertEquals(-1, it.cursor.index);
        Assert.assertTrue(it.isHead());
        Assert.assertFalse(it.isFirst());
        Assert.assertFalse(it.isLast());
        Assert.assertFalse(it.isTail());
        //obtain next
        Assert.assertEquals(null, it.getPrevious());
        Assert.assertEquals(0, castType(it.getNext().value));
        Assert.assertEquals(0, it.getNext().index);

        //iteration 0
        it.gotoNext();
        Assert.assertEquals(0, castType(it.cursor.value));
        Assert.assertEquals(0, it.cursor.index);
        Assert.assertFalse(it.isHead());
        Assert.assertTrue(it.isFirst());
        Assert.assertFalse(it.isLast());
        Assert.assertFalse(it.isTail());
        //obtain previous / next
        Assert.assertEquals(null, it.getPrevious());
        Assert.assertEquals(11, castType(it.getNext().value));
        Assert.assertEquals(1, it.getNext().index);

        //iteration 1
        it.gotoNext();
        Assert.assertEquals(11, castType(it.cursor.value));
        Assert.assertEquals(1, it.cursor.index);
        Assert.assertFalse(it.isHead());
        Assert.assertFalse(it.isFirst());
        Assert.assertFalse(it.isLast());
        Assert.assertFalse(it.isTail());
        //obtain previous / next
        Assert.assertEquals(0, castType(it.getPrevious().value));
        Assert.assertEquals(0, it.getPrevious().index);
        Assert.assertEquals(22, castType(it.getNext().value));
        Assert.assertEquals(2, it.getNext().index);

        //iteration 2
        it.gotoNext();
        Assert.assertEquals(22, castType(it.cursor.value));
        Assert.assertEquals(2, it.cursor.index);
        Assert.assertFalse(it.isHead());
        Assert.assertFalse(it.isFirst());
        Assert.assertFalse(it.isLast());
        Assert.assertFalse(it.isTail());
        //obtain previous / next
        Assert.assertEquals(11, castType(it.getPrevious().value));
        Assert.assertEquals(1, it.getPrevious().index);
        Assert.assertEquals(33, castType(it.getNext().value));
        Assert.assertEquals(3, it.getNext().index);

        //iteration 3
        it.gotoNext();
        Assert.assertEquals(33, castType(it.cursor.value));
        Assert.assertEquals(3, it.cursor.index);
        Assert.assertFalse(it.isHead());
        Assert.assertFalse(it.isFirst());
        Assert.assertFalse(it.isLast());
        Assert.assertFalse(it.isTail());
        //obtain previous / next
        Assert.assertEquals(22, castType(it.getPrevious().value));
        Assert.assertEquals(2, it.getPrevious().index);
        Assert.assertEquals(44, castType(it.getNext().value));
        Assert.assertEquals(4, it.getNext().index);

        //iteration 4
        it.gotoNext();
        Assert.assertEquals(44, castType(it.cursor.value));
        Assert.assertEquals(4, it.cursor.index);
        Assert.assertFalse(it.isHead());
        Assert.assertFalse(it.isFirst());
        Assert.assertFalse(it.isLast());
        Assert.assertFalse(it.isTail());
        //obtain previous / next
        Assert.assertEquals(33, castType(it.getPrevious().value));
        Assert.assertEquals(3, it.getPrevious().index);
        Assert.assertEquals(55, castType(it.getNext().value));
        Assert.assertEquals(5, it.getNext().index);

        //iteration 5
        it.gotoNext();
        Assert.assertEquals(55, castType(it.cursor.value));
        Assert.assertEquals(5, it.cursor.index);
        Assert.assertFalse(it.isHead());
        Assert.assertFalse(it.isFirst());
        Assert.assertTrue(it.isLast());
        Assert.assertFalse(it.isTail());
        //obtain previous / next
        Assert.assertEquals(44, castType(it.getPrevious().value));
        Assert.assertEquals(4, it.getPrevious().index);
        Assert.assertEquals(null, it.getNext());

        //iteration 6
        it.gotoNext();
        Assert.assertEquals(list.size(), it.cursor.index);
        Assert.assertFalse(it.isHead());
        Assert.assertFalse(it.isFirst());
        Assert.assertFalse(it.isLast());
        Assert.assertTrue(it.isTail());
        //obtain previous / next
        Assert.assertEquals(55, castType(it.getPrevious().value));
        Assert.assertEquals(5, it.getPrevious().index);
        Assert.assertEquals(null, it.getNext());

        //iteration 7 : we are already at tail, we don't move further
        it.gotoNext();
        Assert.assertEquals(list.size(), it.cursor.index);
        Assert.assertFalse(it.isHead());
        Assert.assertFalse(it.isFirst());
        Assert.assertFalse(it.isLast());
        Assert.assertTrue(it.isTail());
        //obtain previous / next
        Assert.assertEquals(55, castType(it.getPrevious().value));
        Assert.assertEquals(5, it.getPrevious().index);
        Assert.assertEquals(null, it.getNext());

        //Goes back to head
        it.gotoHead();
        Assert.assertEquals(-1, it.cursor.index);
        Assert.assertTrue(it.isHead());
        Assert.assertFalse(it.isFirst());
        Assert.assertFalse(it.isLast());
        Assert.assertFalse(it.isTail());
        //obtain next
        Assert.assertEquals(null, it.getPrevious());
        Assert.assertEquals(0, castType(it.getNext().value));
        Assert.assertEquals(0, it.getNext().index);

        //Goes again to tail:
        it.gotoTail();
        Assert.assertEquals(list.size(), it.cursor.index);
        Assert.assertFalse(it.isHead());
        Assert.assertFalse(it.isFirst());
        Assert.assertFalse(it.isLast());
        Assert.assertTrue(it.isTail());
        //obtain previous / next
        Assert.assertEquals(55, castType(it.getPrevious().value));
        Assert.assertEquals(5, it.getPrevious().index);
        Assert.assertEquals(null, it.getNext());

    }

    /* */
    @Test
    public void testIterationHeadTailReversed()
    {
        list.add(asArray(0, 11, 22, 33, 44, 55));

        //this is a reversed iteration
        final KTypeLinkedList<KType>.DescendingValueIterator it = list.descendingIterator();

        Assert.assertEquals(list.size(), it.cursor.index);
        Assert.assertTrue(it.isHead());
        Assert.assertFalse(it.isFirst());
        Assert.assertFalse(it.isLast());
        Assert.assertFalse(it.isTail());
        //obtain next
        Assert.assertEquals(null, it.getPrevious());
        Assert.assertEquals(55, castType(it.getNext().value));
        Assert.assertEquals(5, it.getNext().index);

        //Try to move backwards, we stay in head
        it.gotoPrevious();
        Assert.assertEquals(list.size(), it.cursor.index);
        Assert.assertTrue(it.isHead());
        Assert.assertFalse(it.isFirst());
        Assert.assertFalse(it.isLast());
        Assert.assertFalse(it.isTail());
        //obtain next
        Assert.assertEquals(null, it.getPrevious());
        Assert.assertEquals(55, castType(it.getNext().value));
        Assert.assertEquals(5, it.getNext().index);

        //iteration 0
        it.gotoNext();
        Assert.assertEquals(55, castType(it.cursor.value));
        Assert.assertEquals(5, it.cursor.index);
        Assert.assertFalse(it.isHead());
        Assert.assertTrue(it.isFirst());
        Assert.assertFalse(it.isLast());
        Assert.assertFalse(it.isTail());
        //obtain previous / next
        Assert.assertEquals(null, it.getPrevious());
        Assert.assertEquals(44, castType(it.getNext().value));
        Assert.assertEquals(4, it.getNext().index);

        //iteration 1
        it.gotoNext();
        Assert.assertEquals(44, castType(it.cursor.value));
        Assert.assertEquals(4, it.cursor.index);
        Assert.assertFalse(it.isHead());
        Assert.assertFalse(it.isFirst());
        Assert.assertFalse(it.isLast());
        Assert.assertFalse(it.isTail());
        //obtain previous / next
        Assert.assertEquals(55, castType(it.getPrevious().value));
        Assert.assertEquals(5, it.getPrevious().index);
        Assert.assertEquals(33, castType(it.getNext().value));
        Assert.assertEquals(3, it.getNext().index);

        //iteration 2
        it.gotoNext();
        Assert.assertEquals(33, castType(it.cursor.value));
        Assert.assertEquals(3, it.cursor.index);
        Assert.assertFalse(it.isHead());
        Assert.assertFalse(it.isFirst());
        Assert.assertFalse(it.isLast());
        Assert.assertFalse(it.isTail());
        //obtain previous / next
        Assert.assertEquals(44, castType(it.getPrevious().value));
        Assert.assertEquals(4, it.getPrevious().index);
        Assert.assertEquals(22, castType(it.getNext().value));
        Assert.assertEquals(2, it.getNext().index);

        //iteration 3
        it.gotoNext();
        Assert.assertEquals(22, castType(it.cursor.value));
        Assert.assertEquals(2, it.cursor.index);
        Assert.assertFalse(it.isHead());
        Assert.assertFalse(it.isFirst());
        Assert.assertFalse(it.isLast());
        Assert.assertFalse(it.isTail());
        //obtain previous / next
        Assert.assertEquals(33, castType(it.getPrevious().value));
        Assert.assertEquals(3, it.getPrevious().index);
        Assert.assertEquals(11, castType(it.getNext().value));
        Assert.assertEquals(1, it.getNext().index);

        //iteration 4
        it.gotoNext();
        Assert.assertEquals(11, castType(it.cursor.value));
        Assert.assertEquals(1, it.cursor.index);
        Assert.assertFalse(it.isHead());
        Assert.assertFalse(it.isFirst());
        Assert.assertFalse(it.isLast());
        Assert.assertFalse(it.isTail());
        //obtain previous / next
        Assert.assertEquals(22, castType(it.getPrevious().value));
        Assert.assertEquals(2, it.getPrevious().index);
        Assert.assertEquals(0, castType(it.getNext().value));
        Assert.assertEquals(0, it.getNext().index);

        //iteration 5
        it.gotoNext();
        Assert.assertEquals(0, castType(it.cursor.value));
        Assert.assertEquals(0, it.cursor.index);
        Assert.assertFalse(it.isHead());
        Assert.assertFalse(it.isFirst());
        Assert.assertTrue(it.isLast());
        Assert.assertFalse(it.isTail());
        //obtain previous / next
        Assert.assertEquals(11, castType(it.getPrevious().value));
        Assert.assertEquals(1, it.getPrevious().index);
        Assert.assertEquals(null, it.getNext());

        //iteration 6
        it.gotoNext();
        Assert.assertEquals(-1, it.cursor.index);
        Assert.assertFalse(it.isHead());
        Assert.assertFalse(it.isFirst());
        Assert.assertFalse(it.isLast());
        Assert.assertTrue(it.isTail());
        //obtain previous / next
        Assert.assertEquals(0, castType(it.getPrevious().value));
        Assert.assertEquals(0, it.getPrevious().index);
        Assert.assertEquals(null, it.getNext());

        //iteration 7 : we are already at tail, we don't move further
        it.gotoNext();
        Assert.assertEquals(-1, it.cursor.index);
        Assert.assertFalse(it.isHead());
        Assert.assertFalse(it.isFirst());
        Assert.assertFalse(it.isLast());
        Assert.assertTrue(it.isTail());
        //obtain previous / next
        Assert.assertEquals(0, castType(it.getPrevious().value));
        Assert.assertEquals(0, it.getPrevious().index);
        Assert.assertEquals(null, it.getNext());

        //Goes back to head
        it.gotoHead();
        Assert.assertEquals(list.size(), it.cursor.index);
        Assert.assertTrue(it.isHead());
        Assert.assertFalse(it.isFirst());
        Assert.assertFalse(it.isLast());
        Assert.assertFalse(it.isTail());
        //obtain next
        Assert.assertEquals(null, it.getPrevious());
        Assert.assertEquals(55, castType(it.getNext().value));
        Assert.assertEquals(5, it.getNext().index);

        //Goes again to tail:
        it.gotoTail();
        Assert.assertEquals(-1, it.cursor.index);
        Assert.assertFalse(it.isHead());
        Assert.assertFalse(it.isFirst());
        Assert.assertFalse(it.isLast());
        Assert.assertTrue(it.isTail());
        //obtain previous / next
        Assert.assertEquals(0, castType(it.getPrevious().value));
        Assert.assertEquals(0, it.getPrevious().index);
        Assert.assertEquals(null, it.getNext());
    }

    /* */
    @Test
    public void testIterationBackAndForth()
    {
        list.add(asArray(0, 11, 22, 33, 44, 55));

        final KTypeLinkedList<KType>.ValueIterator it = list.iterator();

        //goes 3 steps ==> , 1 step <==
        it.gotoNext().gotoNext().gotoNext().gotoPrevious();
        Assert.assertEquals(11, castType(it.cursor.value));
        Assert.assertEquals(1, it.cursor.index);

        //goes 2 steps ==> , 2 step <==
        it.gotoNext().gotoNext().gotoPrevious().gotoPrevious();
        Assert.assertEquals(11, castType(it.cursor.value));
        Assert.assertEquals(1, it.cursor.index);

        //goes 3 steps <== , 5 step ==> (back is clamped at head, in fact)
        it.gotoPrevious().gotoPrevious().gotoPrevious();
        it.gotoNext().gotoNext().gotoNext().gotoNext().gotoNext();
        Assert.assertEquals(44, castType(it.cursor.value));
        Assert.assertEquals(4, it.cursor.index);
    }

    /* */
    @Test
    public void testIterationBackAndForthReversed()
    {
        list.add(asArray(0, 11, 22, 33, 44, 55));

        final KTypeLinkedList<KType>.DescendingValueIterator it = list.descendingIterator();

        //goes 3 steps ==> , 1 step <==
        it.gotoNext().gotoNext().gotoNext().gotoPrevious();
        Assert.assertEquals(44, castType(it.cursor.value));
        Assert.assertEquals(4, it.cursor.index);

        //goes 2 steps ==> , 2 step <==
        it.gotoNext().gotoNext().gotoPrevious().gotoPrevious();
        Assert.assertEquals(44, castType(it.cursor.value));
        Assert.assertEquals(4, it.cursor.index);

        //goes 3 steps <== , 5 step ==> (back is clamped at head, in fact)
        it.gotoPrevious().gotoPrevious().gotoPrevious();
        it.gotoNext().gotoNext().gotoNext().gotoNext().gotoNext();
        Assert.assertEquals(11, castType(it.cursor.value));
        Assert.assertEquals(1, it.cursor.index);
    }

    /* */
    @Test
    public void testIterationWithInsertionRemoveSetDelete()
    {
        list.add(asArray(0, 11, 22, 33, 44, 55));

        final KTypeLinkedList<KType>.ValueIterator it = list.iterator();

        //goes to 11
        it.gotoNext().gotoNext();
        //list.add(asArray(0, 11, 22, 33, 44, 55));
        it.insertAfter(cast(111));
        it.insertAfter(cast(100));
        it.insertAfter(cast(99));
        //==> list.add(asArray(0, 11, 99, 100, 111, 22, 33, 44, 55));
        TestUtils.assertListEquals(list.toArray(), 0, 11, 99, 100, 111, 22, 33, 44, 55);
        it.insertBefore(cast(3));
        it.insertBefore(cast(33));
        it.insertBefore(cast(20));
        //==> list.add(asArray(0, 3,33,20, 11, 99, 100, 111, 22, 33, 44, 55));
        TestUtils.assertListEquals(list.toArray(), 0, 3, 33, 20, 11, 99, 100, 111, 22, 33, 44, 55);
        it.gotoPrevious();
        Assert.assertEquals(20, castType(it.cursor.value));

        //insert at head
        it.gotoHead();
        it.insertAfter(cast(111));
        it.insertAfter(cast(112));
        // ==> list.add(asArray(112,111,0, 3,33,20, 11, 99, 100, 111, 22, 33, 44, 55));
        TestUtils.assertListEquals(list.toArray(), 112, 111, 0, 3, 33, 20, 11, 99, 100, 111, 22, 33, 44, 55);

        //insert at tail
        it.gotoTail();
        it.insertBefore(cast(7));
        it.insertBefore(cast(9));
        // ==> list.add(asArray(112,111,0, 3,33,20, 11, 99, 100, 111, 22, 33, 44, 55,7, 9));
        TestUtils.assertListEquals(list.toArray(), 112, 111, 0, 3, 33, 20, 11, 99, 100, 111, 22, 33, 44, 55, 7, 9);

        //////// remove / set / delete /////////////////////

        //rewind to first
        it.gotoHead().gotoNext();
        //set -88 at 0 value
        it.gotoNext().gotoNext();
        Assert.assertEquals(0, castType(it.set(cast(88))));
        // ==> list.add(asArray(112,111,88, 3,33,20, 11, 99, 100, 111, 22, 33, 44, 55,7, 9));
        TestUtils.assertListEquals(list.toArray(), 112, 111, 88, 3, 33, 20, 11, 99, 100, 111, 22, 33, 44, 55, 7, 9);

        //move forward by 2 elements : 33
        it.gotoNext().gotoNext();
        //remove next = 20
        KTypeCursor<KType> removed = it.removeNext();
        Assert.assertEquals(5, removed.index);
        Assert.assertEquals(20, castType(removed.value));
        // ==> list.add(asArray(112, 111,88, 3, 33, 11, 99, 100, 111, 22, 33, 44, 55,7, 9));
        //it still points to 33
        TestUtils.assertListEquals(list.toArray(), 112, 111, 88, 3, 33, /*20 */11, 99, 100, 111, 22, 33, 44, 55, 7, 9);
        Assert.assertEquals(33, castType(it.cursor.value));
        Assert.assertEquals(4, it.cursor.index);

        //move again of 3 : 100
        it.gotoNext().gotoNext().gotoNext();
        //remove the previous = 99
        removed = it.removePrevious();
        Assert.assertEquals(6, removed.index);
        Assert.assertEquals(99, castType(removed.value));

        TestUtils.assertListEquals(list.toArray(), 112, 111, 88, 3, 33, /*20 */11, /*99 */100, 111, 22, 33, 44, 55, 7, 9);
        //the iterator still points to 100
        Assert.assertEquals(100, castType(it.cursor.value));
        Assert.assertEquals(6, it.cursor.index);

        //move again of 4 : 44
        it.gotoNext().gotoNext().gotoNext().gotoNext();
        //remove itself
        it.delete();
        //the iterator now points to 55
        TestUtils.assertListEquals(list.toArray(), 112, 111, 88, 3, 33, /*20 */11, /*99 */100, 111, 22, 33, /* 44 */55, 7, 9);
        Assert.assertEquals(55, castType(it.cursor.value));
        Assert.assertEquals(10, it.cursor.index);
    }

    /* */
    @Test
    public void testIterationLoopWithDelete()
    {
        list.add(asArray(112, 111, 88, 3, 33, 20, 11, 99, 100, 111, 22, 33, 44, 55, 7, 9));

        final int poolSize = list.valueIteratorPool.size();

        KTypeLinkedList<KType>.ValueIterator it = null;
        try
        {
            for (it = list.iterator().gotoNext(); !it.isTail(); it.gotoNext())
            {
                if (it.getNext() != null && castType(it.getNext().value) == 88 ||
                        it.getNext() != null && castType(it.getNext().value) == 99 ||
                        it.getNext() != null && castType(it.getNext().value) == 55)
                {

                    it.removeNext();
                }
            }

            TestUtils.assertListEquals(list.toArray(), 112, 111, 3, 33, 20, 11, 100, 111, 22, 33, 44, 7, 9);
            it.release();
            //empty all
            for (it = list.iterator().gotoNext(); !it.isTail(); it.gotoNext())
            {
                it.delete();
                //the for advanced itself, so go back one position
                it.gotoPrevious();
            }

            Assert.assertEquals(0, list.size());
            it.release();
            //try to iterate an empty list
            for (it = list.iterator().gotoNext(); !it.isTail(); it.gotoNext())
            {
                it.delete();
                //the for advanced itself, so go back one position
                it.gotoPrevious();
            }

            Assert.assertEquals(0, list.size());
        }
        finally
        {
            it.release();
        }

        Assert.assertEquals(poolSize, list.valueIteratorPool.size());

    }

    /* */
    @Test
    public void testIterationLoopWithDeleteReversed()
    {
        list.add(asArray(112, 111, 88, 3, 33, 20, 11, 99, 100, 111, 22, 33, 44, 55, 7, 9));

        final int poolSize = list.valueIteratorPool.size();

        KTypeLinkedList<KType>.DescendingValueIterator it = null;
        try
        {
            for (it = list.descendingIterator().gotoNext(); !it.isTail(); it.gotoNext())
            {
                if (it.getNext() != null && castType(it.getNext().value) == 88 ||
                        it.getNext() != null && castType(it.getNext().value) == 99 ||
                        it.getNext() != null && castType(it.getNext().value) == 55)
                {

                    it.removeNext();
                }
            }

            TestUtils.assertListEquals(list.toArray(), 112, 111, 3, 33, 20, 11, 100, 111, 22, 33, 44, 7, 9);
            it.release();
            //empty all
            for (it = list.descendingIterator().gotoNext(); !it.isTail(); it.gotoNext())
            {
                it.delete();
                //the for advanced itself, so go back one position
                it.gotoPrevious();
            }

            Assert.assertEquals(0, list.size());
            it.release();
            //try to iterate an empty list
            for (it = list.descendingIterator().gotoNext(); !it.isTail(); it.gotoNext())
            {
                it.delete();
                //the for advanced itself, so go back one position
                it.gotoPrevious();
            }

            Assert.assertEquals(0, list.size());
        }
        finally
        {
            it.release();
        }

        Assert.assertEquals(poolSize, list.valueIteratorPool.size());
    }

    /* */
    @Test
    public void testIterationWithInsertionRemoveSetDeleteReversed()
    {
        list.add(asArray(0, 11, 22, 33, 44, 55));

        final KTypeLinkedList<KType>.DescendingValueIterator it = list.descendingIterator();

        //goes to 44
        it.gotoNext().gotoNext();
        //list.add(asArray(0, 11, 22, 33, 44, 55));
        it.insertAfter(cast(111));
        it.insertAfter(cast(100));
        it.insertAfter(cast(99));
        //==> list.add(asArray(0, 11, 22, 33, 111, 100, 99, 44, 55));
        TestUtils.assertListEquals(list.toArray(), 0, 11, 22, 33, 111, 100, 99, 44, 55);
        it.insertBefore(cast(3));
        it.insertBefore(cast(33));
        it.insertBefore(cast(20));
        //==> list.add(asArray(0, 11, 22, 33, 111, 100, 99, 44, 20, 33,3,55));
        TestUtils.assertListEquals(list.toArray(), 0, 11, 22, 33, 111, 100, 99, 44, 20, 33, 3, 55);
        it.gotoPrevious();
        Assert.assertEquals(20, castType(it.cursor.value));

        //insert at head
        it.gotoHead();
        it.insertAfter(cast(111));
        it.insertAfter(cast(112));
        // ==> list.add(asArray(0, 11, 22, 33, 111, 100, 99, 44, 20, 33,3,55,111,112));
        TestUtils.assertListEquals(list.toArray(), 0, 11, 22, 33, 111, 100, 99, 44, 20, 33, 3, 55, 111, 112);

        //insert at tail
        it.gotoTail();
        it.insertBefore(cast(7));
        it.insertBefore(cast(9));
        // ==> list.add(asArray(9,7, 0, 11, 22, 33, 111, 100, 99, 44, 20, 33,3,55,111,112));
        TestUtils.assertListEquals(list.toArray(), 9, 7, 0, 11, 22, 33, 111, 100, 99, 44, 20, 33, 3, 55, 111, 112);

        //////// remove / set / delete /////////////////////

        //rewind to first
        it.gotoHead().gotoNext();
        //set 88 at 55 value
        it.gotoNext().gotoNext();
        Assert.assertEquals(55, castType(it.set(cast(88))));
        // ==> list.add(asArray( 9, 7, 0, 11, 22, 33, 111, 100, 99, 44, 20, 33,3,-88,111,112));
        TestUtils.assertListEquals(list.toArray(), 9, 7, 0, 11, 22, 33, 111, 100, 99, 44, 20, 33, 3, 88, 111, 112);

        //move forward by 2 elements : 33
        it.gotoNext().gotoNext();
        //remove next = 20
        KTypeCursor<KType> removed = it.removeNext();
        Assert.assertEquals(10, removed.index);
        Assert.assertEquals(20, castType(removed.value));
        // ==> list.add(asArray(9, 7, 0, 11, 22, 33, 111, 100, 99, 44, 20, 33, 3,88,111,112));
        //it still points to 33
        TestUtils.assertListEquals(list.toArray(), 9, 7, 0, 11, 22, 33, 111, 100, 99, 44 /*20 */, 33, 3, 88, 111, 112);
        Assert.assertEquals(33, castType(it.cursor.value));
        Assert.assertEquals(10, it.cursor.index);

        //move again of 3 : 100
        it.gotoNext().gotoNext().gotoNext();
        //remove the previous = 99
        removed = it.removePrevious();
        Assert.assertEquals(8, removed.index);
        Assert.assertEquals(99, castType(removed.value));

        TestUtils.assertListEquals(list.toArray(), 9, 7, 0, 11, 22, 33, 111, 100, /*99*/44 /*20 */, 33, 3, 88, 111, 112);
        //the iterator still points to 100
        Assert.assertEquals(100, castType(it.cursor.value));
        Assert.assertEquals(7, it.cursor.index);

        //move again of 4 : 11
        it.gotoNext().gotoNext().gotoNext().gotoNext();
        //remove itself
        it.delete();
        //the iterator now points to 0
        TestUtils.assertListEquals(list.toArray(), 9, 7, 0 /*11 */, 22, 33, 111, 100, /*99*/44 /*20 */, 33, 3, 88, 111, 112);
        Assert.assertEquals(0, castType(it.cursor.value));
        Assert.assertEquals(2, it.cursor.index);
    }

    ////////////////////////////////// END iteration special methods  ////////////////////////////////////

    /*! #if ($TemplateOptions.KTypeGeneric) !*/
    @Test
    public void testAgainstArrayDeque()
    {
        final Random rnd = new Random();
        final int rounds = 10000;
        final int modulo = 100;

        final ArrayDeque<KType> ad = new ArrayDeque<KType>();
        for (int i = 0; i < rounds; i++)
        {
            final KType k = cast(rnd.nextInt(modulo));

            final int op = rnd.nextInt(8);
            if (op < 2)
            {
                list.addFirst(k);
                ad.addFirst(k);
            }
            else if (op < 4)
            {
                list.addLast(k);
                ad.addLast(k);
            }
            else if (op < 5 && ad.size() > 0)
            {
                list.removeLast();
                ad.removeLast();
            }
            else if (op < 6 && ad.size() > 0)
            {
                list.removeLast();
                ad.removeLast();
            }
            else if (op < 7)
            {
                Assert.assertEquals(
                        ad.removeFirstOccurrence(k),
                        list.removeFirstOccurrence(k) >= 0);
            }
            else if (op < 8)
            {
                Assert.assertEquals(
                        ad.removeLastOccurrence(k),
                        list.removeLastOccurrence(k) >= 0);
            }
            Assert.assertEquals(ad.size(), list.size());
        }

        Assert.assertArrayEquals(ad.toArray(), list.toArray());
    }

    /*! #end !*/

    /*! #if ($TemplateOptions.KTypeGeneric) !*/
    @SuppressWarnings("unchecked")
    /*! #end !*/
    @Test
    public void testHashCodeEquals2()
    {
        final KTypeLinkedList<KType> l0 = KTypeLinkedList.newInstance();
        Assert.assertEquals(1, l0.hashCode());
        Assert.assertEquals(l0, KTypeLinkedList.from());

        final KTypeLinkedList<KType> l1 = KTypeLinkedList.from(k1, k2, k3);
        final KTypeLinkedList<KType> l2 = KTypeLinkedList.from(k1, k2);
        l2.addLast(k3);

        Assert.assertEquals(l1.hashCode(), l2.hashCode());
        Assert.assertEquals(l1, l2);
    }

    @Test
    public void testPreallocatedSize()
    {
        final Random randomVK = new Random(649841215L);
        //Test that the container do not resize if less that the initial size

        final int NB_TEST_RUNS = 50;

        for (int run = 0; run < NB_TEST_RUNS; run++)
        {
            //1) Choose a random number of elements
            /*! #if ($TemplateOptions.isKType("GENERIC", "INT", "LONG", "FLOAT", "DOUBLE")) !*/
            final int PREALLOCATED_SIZE = randomVK.nextInt(100000);
            /*!
            #elseif ($TemplateOptions.isKType("SHORT", "CHAR"))
             int PREALLOCATED_SIZE = randomVK.nextInt(100000);
            #else
              int PREALLOCATED_SIZE = randomVK.nextInt(100000);
            #end !*/

            //2) Preallocate to PREALLOCATED_SIZE :
            final KTypeLinkedList<KType> newList = KTypeLinkedList.newInstanceWithCapacity(PREALLOCATED_SIZE);

            //3) Add PREALLOCATED_SIZE different values. At the end, size() must be == PREALLOCATED_SIZE,
            //and internal buffer/allocated must not have changed of size
            final int contructorBufferSize = newList.buffer.length;

            for (int i = 0; i < PREALLOCATED_SIZE; i++)
            {
                newList.add(cast(randomVK.nextInt()));

                //internal size has not changed.
                Assert.assertEquals(contructorBufferSize, newList.buffer.length);
            }

            Assert.assertEquals(PREALLOCATED_SIZE, newList.size());
        } //end for test runs
    }

    /* */
    @Test
    public void testEquals()
    {
        final int modulo = 127;
        final int count = 15000;

        final KTypeLinkedList<KType> list2 = KTypeLinkedList.newInstance();

        for (int i = 0; i < count; i++)
        {
            //add in head an in queue indifferently
            if (i % 3 == 0) {
                list.addFirst(cast(i % modulo));
                list2.addFirst(cast(i % modulo));
            }
            else {
                list.addLast(cast(i % modulo));
                list2.addLast(cast(i % modulo));
            }
        }

        //both elements by elements are equal
        TestUtils.assertListEquals(list.toArray(), list2.toArray());

        //Both dequeue are indeed equal
        Assert.assertTrue(list.equals(list2));
    }

    /* */
    @Test
    public void testIndexedContainerEquals()
    {
        final int modulo = 127;
        final int count = 10000;
        sequence.clear();

        for (int i = 0; i < count; i++)
        {
            //add in head an in queue indifferently
            if (i % 3 == 0) {
                list.addFirst(cast(i % modulo));
                sequence.insert(0, cast(i % modulo));
            }
            else {
                list.addLast(cast(i % modulo));
                sequence.add(cast(i % modulo));
            }
        }

        //both elements by elements are equal
        TestUtils.assertListEquals(list.toArray(), sequence.toArray());

        //The array list and dequeue are indeed equal
        Assert.assertTrue(list.equals(sequence));
    }

    /* */
    @Test
    public void testDequeContainerEquals()
    {
        final int modulo = 127;
        final int count = 15000;

        final KTypeArrayDeque<KType> deque2 = KTypeArrayDeque.newInstance();

        for (int i = 0; i < count; i++)
        {
            //add in head an in queue indifferently
            if (i % 3 == 0) {
                list.addFirst(cast(i % modulo));
                deque2.addFirst(cast(i % modulo));
            }
            else {
                list.addLast(cast(i % modulo));
                deque2.addLast(cast(i % modulo));
            }
        }

        //both elements by elements are equal
        TestUtils.assertListEquals(list.toArray(), deque2.toArray());

        //Both Dequeues are indeed equal: explicitely cast it in KTypeDeque
        Assert.assertTrue(list.equals((KTypeDeque<KType>) deque2));
    }

    /**
     * Test natural ordering in the deque
     * @param expected
     * @param actual
     * @param length
     */
    private void assertOrder(final KTypeLinkedList<KType> order)
    {
        //first, export to an array
        final KType[] export = (KType[]) order.toArray();

        for (int i = 1; i < export.length; i++)
        {
            if (castType(export[i - 1]) > castType(export[i]))
            {
                Assert.assertTrue(String.format("Not ordered: (previous, next) = (%d, %d) at index %d",
                        castType(export[i - 1]), castType(export[i]), i), false);
            }
        }
    }
}
