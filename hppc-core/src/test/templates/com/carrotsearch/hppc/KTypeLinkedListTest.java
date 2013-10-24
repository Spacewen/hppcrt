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
        list = KTypeLinkedList.newInstance();

        sequence = KTypeArrayList.newInstance();

        for (int i = 0; i < 10000; i++)
            sequence.add(cast(i));
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
        KTypeLinkedList<KType> list2 = KTypeLinkedList.newInstance();
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

        KTypeLinkedList<B> list2 = new KTypeLinkedList<B>();
        list2.add(new B());

        KTypeLinkedList<A> list3 = new KTypeLinkedList<A>();
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
        list.add(asArray(0, 1, 2, 3, 4, 5, 6, 7, 8, 9));

        list.remove(0);
        list.remove(2);
        list.remove(1);
        list.remove(4);

        assertListEquals(list.toArray(), 1, 4, 5, 6, 8, 9);
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
    public void testGotoIndex()
    {
        //fill with distinct values
        int COUNT = (int) 1e4;

        for (int i = 0; i < COUNT; i++)
        {
            list.add(cast(i));
        }

        //check that we reach the good element, by index
        for (int i = 0; i < COUNT; i++)
        {
            assertEquals(castType(cast(i)), castType(list.buffer[list.gotoIndex(i)]));
        }
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
    public void testRemoveAllEverything()
    {
        list.add(asArray(1, 1, 1, 1, 1));

        assertEquals(5, list.removeAllOccurrences(k1));
        assertEquals(0, list.size());

        //remove all a void
        assertEquals(0, list.removeAllOccurrences(k1));
        assertEquals(0, list.size());
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
    public void testRemoveAllFromLookupContainerEverything()
    {
        list.add(asArray(0, 1, 2, 1, 0));

        KTypeOpenHashSet<KType> list2 = KTypeOpenHashSet.newInstance();
        list2.add(asArray(0, 1, 2));

        assertEquals(5, list.removeAll(list2));
        assertEquals(0, list.size());
        assertEquals(0, list.removeAll(list2));
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
    public void testRemoveAllWithPredicateEverything()
    {
        list.add(newArray(k0, k1, k2, k1, k4));

        assertEquals(5, list.removeAll(new KTypePredicate<KType>()
                {
            public boolean apply(KType v)
            {
                return true;
            };
                }));

        assertEquals(0, list.size());

        //try again
        assertEquals(0, list.removeAll(new KTypePredicate<KType>()
                {
            public boolean apply(KType v)
            {
                return true;
            };
                }));

        assertEquals(0, list.size());
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
        KTypeLinkedList<KType> variable = KTypeLinkedList.from(k1, k2, k3);
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

        KTypeLinkedList<KType> l1 = KTypeLinkedList.from(k1, k2, k3);
        KTypeLinkedList<KType> l2 = KTypeLinkedList.from(k1, k2);
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
        KTypeLinkedList<KType> l2 = KTypeLinkedList.from(k1, k2);
        l2.add(k3);

        assertEquals(l1.hashCode(), l2.hashCode());
        assertEquals(l1, l2);
    }

    /*! #if ($TemplateOptions.KTypeGeneric) !*/
    @Test
    @SuppressWarnings("unchecked")
    public void testHashCodeWithNulls()
    {
        KTypeLinkedList<KType> l1 = KTypeLinkedList.from(k1, null, k3);
        KTypeLinkedList<KType> l2 = KTypeLinkedList.from(k1, null, k3);

        assertEquals(l1.hashCode(), l2.hashCode());
        assertEquals(l1, l2);
    }
    /*! #end !*/

    /*! #if ($TemplateOptions.KTypeGeneric) !*/
    @Test
    public void testToArrayWithClass()
    {
        KTypeLinkedList<Integer> l1 = KTypeLinkedList.from(1, 2, 3);
        Integer[] result = l1.toArray(Integer.class);
        assertArrayEquals(new Integer [] {1, 2, 3}, result); // dummy
    }
    /*! #end !*/

    /*! #if ($TemplateOptions.KTypeGeneric) !*/
    @SuppressWarnings("unchecked")
    @Test
    public void testToArray()
    {
        KTypeLinkedList<KType> l1 = KTypeLinkedList.from(k1, k2, k3, k4, k5, k7);
        Object[] result = l1.toArray();
        assertArrayEquals(new Object[] { k1, k2, k3, k4, k5, k7 }, result);
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

        KTypeLinkedList<KType> cloned = list.clone();
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
        int TEST_SIZE = 10000;
        long TEST_ROUNDS = 100;

        KTypeLinkedList<KType> testContainer = createArrayWithOrderedData(TEST_SIZE);

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

        KTypeLinkedList<KType> testContainer = createArrayWithOrderedData(TEST_SIZE);

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
        assertTrue(testContainer.valueIteratorPool.capacity() < IteratorPool.getMaxPoolSize() + 1);
    }

    @Test
    public void testPooledIteratorFullIteratorLoop()
    {
        // for-each loop interrupted

        int TEST_SIZE = 10000;
        long TEST_ROUNDS = 100;

        KTypeLinkedList<KType> testContainer = createArrayWithOrderedData(TEST_SIZE);

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

            KTypeLinkedList<KType>.ValueIterator loopIterator = testContainer.iterator();

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

        KTypeLinkedList<KType> testContainer = createArrayWithOrderedData(TEST_SIZE);
        int startingPoolSize = testContainer.valueIteratorPool.size();

        int count = 0;
        for (int round = 0; round < TEST_ROUNDS; round++)
        {
            //Classical iterator loop, with manually allocated Iterator
            long initialPoolSize = testContainer.valueIteratorPool.size();

            KTypeLinkedList<KType>.ValueIterator loopIterator = testContainer.iterator();

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

        KTypeLinkedList<KType> testContainer = createArrayWithOrderedData(TEST_SIZE);

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
        KTypeLinkedList<KType>.ValueIterator loopIterator = null;

        for (int round = 0; round < TEST_ROUNDS; round++)
        {
            try
            {
                loopIterator = testContainer.iterator();
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

    @Test
    public void testPooledIteratorExceptionSafe()
    {
        int TEST_SIZE = 224171;
        long TEST_ROUNDS = 15;

        KTypeLinkedList<KType> testContainer = createArrayWithOrderedData(TEST_SIZE);

        long checksum = testContainer.forEach(new KTypeProcedure<KType>() {

            long count;

            @Override
            public void apply(KType value)
            {
                count += castType(value);
            }
        }).count;


        int initialPoolSize = testContainer.valueIteratorPool.size();

        //start with a non full pool, remove 3 elements
        AbstractIterator<KTypeCursor<KType>> loopIteratorFake = testContainer.iterator();
        AbstractIterator<KTypeCursor<KType>> loopIteratorFake2 = testContainer.iterator();
        AbstractIterator<KTypeCursor<KType>> loopIteratorFake3 = testContainer.iterator();

        int startingTestPoolSize = testContainer.valueIteratorPool.size();

        assertEquals(initialPoolSize - 3, startingTestPoolSize);

        int count = 0;
        KTypeLinkedList<KType>.ValueIterator loopIterator = null;

        for (int round = 0; round < TEST_ROUNDS; round++)
        {
            try
            {
                loopIterator = testContainer.iterator();
                assertEquals(startingTestPoolSize - 1, testContainer.valueIteratorPool.size());

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
                assertEquals(startingTestPoolSize, testContainer.valueIteratorPool.size());
                assertEquals(checksum, guard);

                //still, try to return it ....
                loopIterator.release();

                //nothing has changed
                assertEquals(startingTestPoolSize, testContainer.valueIteratorPool.size());
            }
            catch (Exception e)
            {
                //iterator is NOT returned to its pool because of the exception
                assertEquals(startingTestPoolSize - 1, testContainer.valueIteratorPool.size());

                //manual return to the pool then
                loopIterator.release();

                //now the pool is restored
                assertEquals(startingTestPoolSize, testContainer.valueIteratorPool.size());

                //continue to try to release...
                loopIterator.release();

                //nothing has changed
                assertEquals(startingTestPoolSize, testContainer.valueIteratorPool.size());
            }
        } //end for rounds

        // pool initial size is untouched anyway
        assertEquals(startingTestPoolSize, testContainer.valueIteratorPool.size());

        //finally return the fake ones, several times
        loopIteratorFake.release();
        loopIteratorFake.release();
        loopIteratorFake.release();
        loopIteratorFake2.release();
        loopIteratorFake2.release();
        loopIteratorFake2.release();
        loopIteratorFake3.release();
        loopIteratorFake3.release();

        assertEquals(initialPoolSize, testContainer.valueIteratorPool.size());
    }

    @Test
    public void testSort()
    {
        //natural ordering comparator
        KTypeComparator<KType> comp = new KTypeComparator<KType>() {

            @Override
            public int compare(KType e1, KType e2)
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

        int TEST_SIZE = (int) 1e6;
        //A) Sort an array of random values of primitive types

        /*! #if ($TemplateOptions.KTypePrimitive)
        //A-1) full sort
        KTypeLinkedList<KType> primitiveList = createArrayWithRandomData(TEST_SIZE, 1515411541215L);
        primitiveList.sort();
        assertOrder(primitiveList);
        #end !*/

        //B) Sort with Comparator
        //B-1) Full sort
        KTypeLinkedList<KType> comparatorList = createArrayWithRandomData(TEST_SIZE, 4871164545215L);
        comparatorList.sort(comp);
        assertOrder(comparatorList);
    }

    private KTypeLinkedList<KType> createArrayWithOrderedData(int size)
    {
        KTypeLinkedList<KType> newArray = KTypeLinkedList.newInstanceWithCapacity(KTypeLinkedList.DEFAULT_CAPACITY);

        for (int i = 0; i < size; i++)
        {
            newArray.add(cast(i));
        }

        return newArray;
    }

    private KTypeLinkedList<KType> createArrayWithRandomData(int size, long randomSeed)
    {
        Random prng = new Random(randomSeed);

        KTypeLinkedList<KType> newArray = KTypeLinkedList.newInstanceWithCapacity(KTypeLinkedList.DEFAULT_CAPACITY);

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
        assertListEquals(list.toArray(), 5, 4, 1, 7, 3, 2, 1);
        assertEquals(7, list.size());
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
        assertListEquals(list.toArray(), 1, 2, 3, 7, 1, 4, 5);
        assertEquals(7, list.size());
    }

    /* */
    @Test
    public void testAddWithGrowth()
    {
        for (int i = 0; i < sequence.size(); i++)
        {
            list.addFirst(sequence.buffer[i]);
        }

        assertListEquals(reverse(sequence.toArray()), list.toArray());
    }

    /* */
    @Test
    public void testAddLastWithGrowth()
    {
        for (int i = 0; i < sequence.size(); i++)
            list.addLast(sequence.buffer[i]);

        assertListEquals(sequence.toArray(), list.toArray());
    }

    /* */
    @Test
    public void testAddAllFirst()
    {
        KTypeArrayList<KType> list2 = KTypeArrayList.newInstance();
        list2.add(asArray(0, 1, 2));

        list.addFirst(list2);
        assertListEquals(list.toArray(), 2, 1, 0);
        list.addFirst(list2);
        assertListEquals(list.toArray(), 2, 1, 0, 2, 1, 0);

        list.clear();
        KTypeArrayDeque<KType> deque2 = new KTypeArrayDeque<KType>();
        deque2.addLast(asArray(0, 1, 2));
        list.addFirst(deque2);
        assertListEquals(list.toArray(), 2, 1, 0);
    }

    /* */
    @Test
    public void testAddAllLast()
    {
        KTypeArrayList<KType> list2 = KTypeArrayList.newInstance();
        list2.add(asArray(0, 1, 2));

        list.addLast(list2);
        assertListEquals(list.toArray(), 0, 1, 2);
        list.addLast(list2);
        assertListEquals(list.toArray(), 0, 1, 2, 0, 1, 2);

        list.clear();
        KTypeArrayDeque<KType> deque2 = new KTypeArrayDeque<KType>();
        deque2.addLast(asArray(0, 1, 2));
        list.addLast(deque2);
        assertListEquals(list.toArray(), 0, 1, 2);
    }

    /* */
    @Test
    public void testRemoveFirst()
    {
        list.addLast(k1);
        list.addLast(k2);
        list.addLast(k3);

        list.removeFirst();
        assertListEquals(list.toArray(), 2, 3);
        assertEquals(2, list.size());

        list.addFirst(k4);
        assertListEquals(list.toArray(), 4, 2, 3);
        assertEquals(3, list.size());

        list.removeFirst();
        list.removeFirst();
        list.removeFirst();
        assertEquals(0, list.toArray().length);
        assertEquals(0, list.size());
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
        assertListEquals(list.toArray(), 1, 2);
        assertEquals(2, list.size());

        list.addLast(k4);
        assertListEquals(list.toArray(), 1, 2, 4);
        assertEquals(3, list.size());

        list.removeLast();
        list.removeLast();
        list.removeLast();
        assertEquals(0, list.toArray().length);
        assertEquals(0, list.size());
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
        assertEquals2(k1, list.getFirst());
        list.addFirst(k3);
        assertEquals2(k3, list.getFirst());
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
        assertEquals2(k2, list.getLast());
        list.addLast(k3);
        assertEquals2(k3, list.getLast());
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
        int modulo = 10;
        int count = 10000;
        sequence.clear();
        for (int i = 0; i < count; i++)
        {
            list.addLast(cast(i % modulo));
            sequence.add(cast(i % modulo));
        }

        assertListEquals(list.toArray(), sequence.toArray());

        Random rnd = new Random(0xdeadbeef);
        for (int i = 0; i < 500; i++)
        {
            KType k = cast(rnd.nextInt(modulo));
            assertEquals(
                    list.removeFirstOccurrence(k) >= 0,
                    sequence.removeFirstOccurrence(k) >= 0);
        }

        assertListEquals(list.toArray(), sequence.toArray());

        assertTrue(0 > list.removeFirstOccurrence(cast(modulo + 1)));
        list.addLast(cast(modulo + 1));
        assertTrue(0 <= list.removeFirstOccurrence(cast(modulo + 1)));
    }

    /* */
    @Test
    public void testRemoveLastOccurrence()
    {
        int modulo = 10;
        int count = 10000;
        sequence.clear();

        for (int i = 0; i < count; i++)
        {
            list.addLast(cast(i % modulo));
            sequence.add(cast(i % modulo));
        }

        assertListEquals(list.toArray(), sequence.toArray());

        Random rnd = new Random(0x11223344);

        for (int i = 0; i < 500; i++)
        {
            KType k = cast(rnd.nextInt(modulo));
            assertEquals(
                    list.removeLastOccurrence(k) >= 0,
                    sequence.removeLastOccurrence(k) >= 0);
        }

        assertListEquals(list.toArray(), sequence.toArray());

        int removedIndex = list.removeLastOccurrence(cast(modulo + 1));
        assertTrue("removeLastOccurrence(cast(modulo + 1) = " + removedIndex, removedIndex < 0);

        list.addFirst(cast(modulo + 1));

        removedIndex = list.removeLastOccurrence(cast(modulo + 1));
        assertTrue("removeLastOccurrence(cast(modulo + 1) = " + removedIndex, removedIndex == 0);
    }

    /* */
    @Test
    public void testRemoveAllOccurrences()
    {
        list.add(asArray(0, 1, 2, 1, 0, 3, 0));

        assertEquals(0, list.removeAllOccurrences(k4));
        assertEquals(3, list.removeAllOccurrences(k0));
        assertListEquals(list.toArray(), 1, 2, 1, 3);
        assertEquals(1, list.removeAllOccurrences(k3));
        assertListEquals(list.toArray(), 1, 2, 1);
        assertEquals(2, list.removeAllOccurrences(k1));
        assertListEquals(list.toArray(), 2);
        assertEquals(1, list.removeAllOccurrences(k2));
        assertEquals(0, list.size());
    }

    /* */
    @Test
    public void testRemoveAllInLookupContainer()
    {
        list.add(asArray(0, 1, 2, 1, 0));

        KTypeOpenHashSet<KType> set = KTypeOpenHashSet.newInstance();
        set.add(asArray(0, 2));

        assertEquals(3, list.removeAll(set));
        assertEquals(0, list.removeAll(set));

        assertListEquals(list.toArray(), 1, 1);
    }

    /* */
    @Test
    public void testClear2()
    {
        list.addLast(k1);
        list.addLast(k2);
        list.addFirst(k3);
        list.clear();
        assertEquals(0, list.size());

        list.addLast(k1);
        assertListEquals(list.toArray(), 1);
    }


    /* */
    @Test
    public void testIterable()
    {
        list.addLast(sequence);

        int count = 0;
        for (KTypeCursor<KType> cursor : list)
        {
            assertEquals2(sequence.buffer[count], cursor.value);
            assertEquals2(count, cursor.index);
            count++;
        }
        assertEquals(count, list.size());
        assertEquals(count, sequence.size());

        count = 0;
        list.clear();
        for (@SuppressWarnings("unused")
        KTypeCursor<KType> cursor : list)
        {
            count++;
        }
        assertEquals(0, count);
    }

    /* */
    @Test
    public void testIterator()
    {
        list.addLast(asArray(0, 1, 2, 3));

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

        list.clear();
        assertFalse(list.iterator().hasNext());
    }

    /* */
    @Test
    public void testDescendingIterator()
    {
        list.addLast(sequence);

        int index = sequence.size() - 1;
        for (Iterator<KTypeCursor<KType>> i = list.descendingIterator(); i.hasNext();)
        {
            KTypeCursor<KType> cursor = i.next();
            assertEquals2(sequence.buffer[index], cursor.value);
            assertEquals2(index, cursor.index);
            index--;
        }
        assertEquals(-1, index);

        list.clear();
        assertFalse(list.descendingIterator().hasNext());
    }

    /* */
    @Test
    public void testDescendingForEachWithProcedure()
    {
        list.addLast(sequence);

        final IntHolder count = new IntHolder();
        list.descendingForEach(new KTypeProcedure<KType>() {
            int index = sequence.size();

            public void apply(KType v)
            {
                assertEquals2(sequence.buffer[--index], v);
                count.value++;
            }
        });
        assertEquals(count.value, list.size());
    }

    ///////////////////////////////// iteration special methods ////////////////////////////////////

    /* */
    @Test
    public void testIterationHeadTail()
    {
        list.add(asArray(0, 11, 22, 33, 44, 55));

        KTypeLinkedList<KType>.ValueIterator it = list.iterator();

        assertEquals(-1, it.cursor.index);
        assertTrue(it.isHead());
        assertFalse(it.isFirst());
        assertFalse(it.isLast());
        assertFalse(it.isTail());
        //obtain next
        assertEquals(null, it.getPrevious());
        assertEquals(0, castType(it.getNext().value));
        assertEquals(0, it.getNext().index);

        //Try to move backwards, we stay in head
        it.gotoPrevious();
        assertEquals(-1, it.cursor.index);
        assertTrue(it.isHead());
        assertFalse(it.isFirst());
        assertFalse(it.isLast());
        assertFalse(it.isTail());
        //obtain next
        assertEquals(null, it.getPrevious());
        assertEquals(0, castType(it.getNext().value));
        assertEquals(0, it.getNext().index);

        //iteration 0
        it.gotoNext();
        assertEquals(0, castType(it.cursor.value));
        assertEquals(0, it.cursor.index);
        assertFalse(it.isHead());
        assertTrue(it.isFirst());
        assertFalse(it.isLast());
        assertFalse(it.isTail());
        //obtain previous / next
        assertEquals(null, it.getPrevious());
        assertEquals(11, castType(it.getNext().value));
        assertEquals(1, it.getNext().index);

        //iteration 1
        it.gotoNext();
        assertEquals(11, castType(it.cursor.value));
        assertEquals(1, it.cursor.index);
        assertFalse(it.isHead());
        assertFalse(it.isFirst());
        assertFalse(it.isLast());
        assertFalse(it.isTail());
        //obtain previous / next
        assertEquals(0, castType(it.getPrevious().value));
        assertEquals(0, it.getPrevious().index);
        assertEquals(22, castType(it.getNext().value));
        assertEquals(2, it.getNext().index);

        //iteration 2
        it.gotoNext();
        assertEquals(22, castType(it.cursor.value));
        assertEquals(2, it.cursor.index);
        assertFalse(it.isHead());
        assertFalse(it.isFirst());
        assertFalse(it.isLast());
        assertFalse(it.isTail());
        //obtain previous / next
        assertEquals(11, castType(it.getPrevious().value));
        assertEquals(1, it.getPrevious().index);
        assertEquals(33, castType(it.getNext().value));
        assertEquals(3, it.getNext().index);

        //iteration 3
        it.gotoNext();
        assertEquals(33, castType(it.cursor.value));
        assertEquals(3, it.cursor.index);
        assertFalse(it.isHead());
        assertFalse(it.isFirst());
        assertFalse(it.isLast());
        assertFalse(it.isTail());
        //obtain previous / next
        assertEquals(22, castType(it.getPrevious().value));
        assertEquals(2, it.getPrevious().index);
        assertEquals(44, castType(it.getNext().value));
        assertEquals(4, it.getNext().index);

        //iteration 4
        it.gotoNext();
        assertEquals(44, castType(it.cursor.value));
        assertEquals(4, it.cursor.index);
        assertFalse(it.isHead());
        assertFalse(it.isFirst());
        assertFalse(it.isLast());
        assertFalse(it.isTail());
        //obtain previous / next
        assertEquals(33, castType(it.getPrevious().value));
        assertEquals(3, it.getPrevious().index);
        assertEquals(55, castType(it.getNext().value));
        assertEquals(5, it.getNext().index);

        //iteration 5
        it.gotoNext();
        assertEquals(55, castType(it.cursor.value));
        assertEquals(5, it.cursor.index);
        assertFalse(it.isHead());
        assertFalse(it.isFirst());
        assertTrue(it.isLast());
        assertFalse(it.isTail());
        //obtain previous / next
        assertEquals(44, castType(it.getPrevious().value));
        assertEquals(4, it.getPrevious().index);
        assertEquals(null, it.getNext());

        //iteration 6
        it.gotoNext();
        assertEquals(list.size(), it.cursor.index);
        assertFalse(it.isHead());
        assertFalse(it.isFirst());
        assertFalse(it.isLast());
        assertTrue(it.isTail());
        //obtain previous / next
        assertEquals(55, castType(it.getPrevious().value));
        assertEquals(5, it.getPrevious().index);
        assertEquals(null, it.getNext());

        //iteration 7 : we are already at tail, we don't move further
        it.gotoNext();
        assertEquals(list.size(), it.cursor.index);
        assertFalse(it.isHead());
        assertFalse(it.isFirst());
        assertFalse(it.isLast());
        assertTrue(it.isTail());
        //obtain previous / next
        assertEquals(55, castType(it.getPrevious().value));
        assertEquals(5, it.getPrevious().index);
        assertEquals(null, it.getNext());

        //Goes back to head
        it.gotoHead();
        assertEquals(-1, it.cursor.index);
        assertTrue(it.isHead());
        assertFalse(it.isFirst());
        assertFalse(it.isLast());
        assertFalse(it.isTail());
        //obtain next
        assertEquals(null, it.getPrevious());
        assertEquals(0, castType(it.getNext().value));
        assertEquals(0, it.getNext().index);

        //Goes again to tail:
        it.gotoTail();
        assertEquals(list.size(), it.cursor.index);
        assertFalse(it.isHead());
        assertFalse(it.isFirst());
        assertFalse(it.isLast());
        assertTrue(it.isTail());
        //obtain previous / next
        assertEquals(55, castType(it.getPrevious().value));
        assertEquals(5, it.getPrevious().index);
        assertEquals(null, it.getNext());

    }

    /* */
    @Test
    public void testIterationHeadTailReversed()
    {
        list.add(asArray(0, 11, 22, 33, 44, 55));

        //this is a reversed iteration
        KTypeLinkedList<KType>.DescendingValueIterator it = list.descendingIterator();

        assertEquals(list.size(), it.cursor.index);
        assertTrue(it.isHead());
        assertFalse(it.isFirst());
        assertFalse(it.isLast());
        assertFalse(it.isTail());
        //obtain next
        assertEquals(null, it.getPrevious());
        assertEquals(55, castType(it.getNext().value));
        assertEquals(5, it.getNext().index);

        //Try to move backwards, we stay in head
        it.gotoPrevious();
        assertEquals(list.size(), it.cursor.index);
        assertTrue(it.isHead());
        assertFalse(it.isFirst());
        assertFalse(it.isLast());
        assertFalse(it.isTail());
        //obtain next
        assertEquals(null, it.getPrevious());
        assertEquals(55, castType(it.getNext().value));
        assertEquals(5, it.getNext().index);

        //iteration 0
        it.gotoNext();
        assertEquals(55, castType(it.cursor.value));
        assertEquals(5, it.cursor.index);
        assertFalse(it.isHead());
        assertTrue(it.isFirst());
        assertFalse(it.isLast());
        assertFalse(it.isTail());
        //obtain previous / next
        assertEquals(null, it.getPrevious());
        assertEquals(44, castType(it.getNext().value));
        assertEquals(4, it.getNext().index);

        //iteration 1
        it.gotoNext();
        assertEquals(44, castType(it.cursor.value));
        assertEquals(4, it.cursor.index);
        assertFalse(it.isHead());
        assertFalse(it.isFirst());
        assertFalse(it.isLast());
        assertFalse(it.isTail());
        //obtain previous / next
        assertEquals(55, castType(it.getPrevious().value));
        assertEquals(5, it.getPrevious().index);
        assertEquals(33, castType(it.getNext().value));
        assertEquals(3, it.getNext().index);

        //iteration 2
        it.gotoNext();
        assertEquals(33, castType(it.cursor.value));
        assertEquals(3, it.cursor.index);
        assertFalse(it.isHead());
        assertFalse(it.isFirst());
        assertFalse(it.isLast());
        assertFalse(it.isTail());
        //obtain previous / next
        assertEquals(44, castType(it.getPrevious().value));
        assertEquals(4, it.getPrevious().index);
        assertEquals(22, castType(it.getNext().value));
        assertEquals(2, it.getNext().index);

        //iteration 3
        it.gotoNext();
        assertEquals(22, castType(it.cursor.value));
        assertEquals(2, it.cursor.index);
        assertFalse(it.isHead());
        assertFalse(it.isFirst());
        assertFalse(it.isLast());
        assertFalse(it.isTail());
        //obtain previous / next
        assertEquals(33, castType(it.getPrevious().value));
        assertEquals(3, it.getPrevious().index);
        assertEquals(11, castType(it.getNext().value));
        assertEquals(1, it.getNext().index);

        //iteration 4
        it.gotoNext();
        assertEquals(11, castType(it.cursor.value));
        assertEquals(1, it.cursor.index);
        assertFalse(it.isHead());
        assertFalse(it.isFirst());
        assertFalse(it.isLast());
        assertFalse(it.isTail());
        //obtain previous / next
        assertEquals(22, castType(it.getPrevious().value));
        assertEquals(2, it.getPrevious().index);
        assertEquals(0, castType(it.getNext().value));
        assertEquals(0, it.getNext().index);

        //iteration 5
        it.gotoNext();
        assertEquals(0, castType(it.cursor.value));
        assertEquals(0, it.cursor.index);
        assertFalse(it.isHead());
        assertFalse(it.isFirst());
        assertTrue(it.isLast());
        assertFalse(it.isTail());
        //obtain previous / next
        assertEquals(11, castType(it.getPrevious().value));
        assertEquals(1, it.getPrevious().index);
        assertEquals(null, it.getNext());

        //iteration 6
        it.gotoNext();
        assertEquals(-1, it.cursor.index);
        assertFalse(it.isHead());
        assertFalse(it.isFirst());
        assertFalse(it.isLast());
        assertTrue(it.isTail());
        //obtain previous / next
        assertEquals(0, castType(it.getPrevious().value));
        assertEquals(0, it.getPrevious().index);
        assertEquals(null, it.getNext());

        //iteration 7 : we are already at tail, we don't move further
        it.gotoNext();
        assertEquals(-1, it.cursor.index);
        assertFalse(it.isHead());
        assertFalse(it.isFirst());
        assertFalse(it.isLast());
        assertTrue(it.isTail());
        //obtain previous / next
        assertEquals(0, castType(it.getPrevious().value));
        assertEquals(0, it.getPrevious().index);
        assertEquals(null, it.getNext());

        //Goes back to head
        it.gotoHead();
        assertEquals(list.size(), it.cursor.index);
        assertTrue(it.isHead());
        assertFalse(it.isFirst());
        assertFalse(it.isLast());
        assertFalse(it.isTail());
        //obtain next
        assertEquals(null, it.getPrevious());
        assertEquals(55, castType(it.getNext().value));
        assertEquals(5, it.getNext().index);

        //Goes again to tail:
        it.gotoTail();
        assertEquals(-1, it.cursor.index);
        assertFalse(it.isHead());
        assertFalse(it.isFirst());
        assertFalse(it.isLast());
        assertTrue(it.isTail());
        //obtain previous / next
        assertEquals(0, castType(it.getPrevious().value));
        assertEquals(0, it.getPrevious().index);
        assertEquals(null, it.getNext());
    }

    /* */
    @Test
    public void testIterationBackAndForth()
    {
        list.add(asArray(0, 11, 22, 33, 44, 55));

        KTypeLinkedList<KType>.ValueIterator it = list.iterator();

        //goes 3 steps ==> , 1 step <==
        it.gotoNext().gotoNext().gotoNext().gotoPrevious();
        assertEquals(11, castType(it.cursor.value));
        assertEquals(1, it.cursor.index);

        //goes 2 steps ==> , 2 step <==
        it.gotoNext().gotoNext().gotoPrevious().gotoPrevious();
        assertEquals(11, castType(it.cursor.value));
        assertEquals(1, it.cursor.index);

        //goes 3 steps <== , 5 step ==> (back is clamped at head, in fact)
        it.gotoPrevious().gotoPrevious().gotoPrevious();
        it.gotoNext().gotoNext().gotoNext().gotoNext().gotoNext();
        assertEquals(44, castType(it.cursor.value));
        assertEquals(4, it.cursor.index);
    }

    /* */
    @Test
    public void testIterationBackAndForthReversed()
    {
        list.add(asArray(0, 11, 22, 33, 44, 55));

        KTypeLinkedList<KType>.DescendingValueIterator it = list.descendingIterator();

        //goes 3 steps ==> , 1 step <==
        it.gotoNext().gotoNext().gotoNext().gotoPrevious();
        assertEquals(44, castType(it.cursor.value));
        assertEquals(4, it.cursor.index);

        //goes 2 steps ==> , 2 step <==
        it.gotoNext().gotoNext().gotoPrevious().gotoPrevious();
        assertEquals(44, castType(it.cursor.value));
        assertEquals(4, it.cursor.index);

        //goes 3 steps <== , 5 step ==> (back is clamped at head, in fact)
        it.gotoPrevious().gotoPrevious().gotoPrevious();
        it.gotoNext().gotoNext().gotoNext().gotoNext().gotoNext();
        assertEquals(11, castType(it.cursor.value));
        assertEquals(1, it.cursor.index);
    }

    /* */
    @Test
    public void testIterationWithInsertionRemoveSetDelete()
    {
        list.add(asArray(0, 11, 22, 33, 44, 55));

        KTypeLinkedList<KType>.ValueIterator it = list.iterator();

        //goes to 11
        it.gotoNext().gotoNext();
        //list.add(asArray(0, 11, 22, 33, 44, 55));
        it.insertAfter(cast(111));
        it.insertAfter(cast(100));
        it.insertAfter(cast(99));
        //==> list.add(asArray(0, 11, 99, 100, 111, 22, 33, 44, 55));
        assertListEquals(list.toArray(), 0, 11, 99, 100, 111, 22, 33, 44, 55);
        it.insertBefore(cast(3));
        it.insertBefore(cast(33));
        it.insertBefore(cast(20));
        //==> list.add(asArray(0, 3,33,20, 11, 99, 100, 111, 22, 33, 44, 55));
        assertListEquals(list.toArray(), 0, 3, 33, 20, 11, 99, 100, 111, 22, 33, 44, 55);
        it.gotoPrevious();
        assertEquals(20, castType(it.cursor.value));

        //insert at head
        it.gotoHead();
        it.insertAfter(cast(111));
        it.insertAfter(cast(112));
        // ==> list.add(asArray(112,111,0, 3,33,20, 11, 99, 100, 111, 22, 33, 44, 55));
        assertListEquals(list.toArray(), 112, 111, 0, 3, 33, 20, 11, 99, 100, 111, 22, 33, 44, 55);

        //insert at tail
        it.gotoTail();
        it.insertBefore(cast(7));
        it.insertBefore(cast(9));
        // ==> list.add(asArray(112,111,0, 3,33,20, 11, 99, 100, 111, 22, 33, 44, 55,7, 9));
        assertListEquals(list.toArray(), 112, 111, 0, 3, 33, 20, 11, 99, 100, 111, 22, 33, 44, 55, 7, 9);

        //////// remove / set / delete /////////////////////

        //rewind to first
        it.gotoHead().gotoNext();
        //set -88 at 0 value
        it.gotoNext().gotoNext();
        assertEquals(0, castType(it.set(cast(88))));
        // ==> list.add(asArray(112,111,88, 3,33,20, 11, 99, 100, 111, 22, 33, 44, 55,7, 9));
        assertListEquals(list.toArray(), 112, 111, 88, 3, 33, 20, 11, 99, 100, 111, 22, 33, 44, 55, 7, 9);

        //move forward by 2 elements : 33
        it.gotoNext().gotoNext();
        //remove next = 20
        KTypeCursor<KType> removed = it.removeNext();
        assertEquals(5, removed.index);
        assertEquals(20, castType(removed.value));
        // ==> list.add(asArray(112, 111,88, 3, 33, 11, 99, 100, 111, 22, 33, 44, 55,7, 9));
        //it still points to 33
        assertListEquals(list.toArray(), 112, 111, 88, 3, 33, /*20 */11, 99, 100, 111, 22, 33, 44, 55, 7, 9);
        assertEquals(33, castType(it.cursor.value));
        assertEquals(4, it.cursor.index);

        //move again of 3 : 100
        it.gotoNext().gotoNext().gotoNext();
        //remove the previous = 99
        removed = it.removePrevious();
        assertEquals(6, removed.index);
        assertEquals(99, castType(removed.value));

        assertListEquals(list.toArray(), 112, 111, 88, 3, 33, /*20 */11, /*99 */100, 111, 22, 33, 44, 55, 7, 9);
        //the iterator still points to 100
        assertEquals(100, castType(it.cursor.value));
        assertEquals(6, it.cursor.index);

        //move again of 4 : 44
        it.gotoNext().gotoNext().gotoNext().gotoNext();
        //remove itself
        it.delete();
        //the iterator now points to 55
        assertListEquals(list.toArray(), 112, 111, 88, 3, 33, /*20 */11, /*99 */100, 111, 22, 33, /* 44 */55, 7, 9);
        assertEquals(55, castType(it.cursor.value));
        assertEquals(10, it.cursor.index);
    }

    /* */
    @Test
    public void testIterationLoopWithDelete()
    {
        list.add(asArray(112, 111, 88, 3, 33, 20, 11, 99, 100, 111, 22, 33, 44, 55, 7, 9));

        int poolSize = list.valueIteratorPool.size();

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

            assertListEquals(list.toArray(), 112, 111, 3, 33, 20, 11, 100, 111, 22, 33, 44, 7, 9);
            it.release();
            //empty all
            for (it = list.iterator().gotoNext(); !it.isTail(); it.gotoNext())
            {
                it.delete();
                //the for advanced itself, so go back one position
                it.gotoPrevious();
            }

            assertEquals(0, list.size());
            it.release();
            //try to iterate an empty list
            for (it = list.iterator().gotoNext(); !it.isTail(); it.gotoNext())
            {
                it.delete();
                //the for advanced itself, so go back one position
                it.gotoPrevious();
            }

            assertEquals(0, list.size());
        }
        finally
        {
            it.release();
        }

        assertEquals(poolSize, list.valueIteratorPool.size());

    }

    /* */
    @Test
    public void testIterationLoopWithDeleteReversed()
    {
        list.add(asArray(112, 111, 88, 3, 33, 20, 11, 99, 100, 111, 22, 33, 44, 55, 7, 9));

        int poolSize = list.valueIteratorPool.size();

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

            assertListEquals(list.toArray(), 112, 111, 3, 33, 20, 11, 100, 111, 22, 33, 44, 7, 9);
            it.release();
            //empty all
            for (it = list.descendingIterator().gotoNext(); !it.isTail(); it.gotoNext())
            {
                it.delete();
                //the for advanced itself, so go back one position
                it.gotoPrevious();
            }

            assertEquals(0, list.size());
            it.release();
            //try to iterate an empty list
            for (it = list.descendingIterator().gotoNext(); !it.isTail(); it.gotoNext())
            {
                it.delete();
                //the for advanced itself, so go back one position
                it.gotoPrevious();
            }

            assertEquals(0, list.size());
        }
        finally
        {
            it.release();
        }

        assertEquals(poolSize, list.valueIteratorPool.size());
    }

    /* */
    @Test
    public void testIterationWithInsertionRemoveSetDeleteReversed()
    {
        list.add(asArray(0, 11, 22, 33, 44, 55));

        KTypeLinkedList<KType>.DescendingValueIterator it = list.descendingIterator();

        //goes to 44
        it.gotoNext().gotoNext();
        //list.add(asArray(0, 11, 22, 33, 44, 55));
        it.insertAfter(cast(111));
        it.insertAfter(cast(100));
        it.insertAfter(cast(99));
        //==> list.add(asArray(0, 11, 22, 33, 111, 100, 99, 44, 55));
        assertListEquals(list.toArray(), 0, 11, 22, 33, 111, 100, 99, 44, 55);
        it.insertBefore(cast(3));
        it.insertBefore(cast(33));
        it.insertBefore(cast(20));
        //==> list.add(asArray(0, 11, 22, 33, 111, 100, 99, 44, 20, 33,3,55));
        assertListEquals(list.toArray(), 0, 11, 22, 33, 111, 100, 99, 44, 20, 33, 3, 55);
        it.gotoPrevious();
        assertEquals(20, castType(it.cursor.value));

        //insert at head
        it.gotoHead();
        it.insertAfter(cast(111));
        it.insertAfter(cast(112));
        // ==> list.add(asArray(0, 11, 22, 33, 111, 100, 99, 44, 20, 33,3,55,111,112));
        assertListEquals(list.toArray(), 0, 11, 22, 33, 111, 100, 99, 44, 20, 33, 3, 55, 111, 112);

        //insert at tail
        it.gotoTail();
        it.insertBefore(cast(7));
        it.insertBefore(cast(9));
        // ==> list.add(asArray(9,7, 0, 11, 22, 33, 111, 100, 99, 44, 20, 33,3,55,111,112));
        assertListEquals(list.toArray(), 9, 7, 0, 11, 22, 33, 111, 100, 99, 44, 20, 33, 3, 55, 111, 112);

        //////// remove / set / delete /////////////////////

        //rewind to first
        it.gotoHead().gotoNext();
        //set 88 at 55 value
        it.gotoNext().gotoNext();
        assertEquals(55, castType(it.set(cast(88))));
        // ==> list.add(asArray( 9, 7, 0, 11, 22, 33, 111, 100, 99, 44, 20, 33,3,-88,111,112));
        assertListEquals(list.toArray(), 9, 7, 0, 11, 22, 33, 111, 100, 99, 44, 20, 33, 3, 88, 111, 112);

        //move forward by 2 elements : 33
        it.gotoNext().gotoNext();
        //remove next = 20
        KTypeCursor<KType> removed = it.removeNext();
        assertEquals(10, removed.index);
        assertEquals(20, castType(removed.value));
        // ==> list.add(asArray(9, 7, 0, 11, 22, 33, 111, 100, 99, 44, 20, 33, 3,88,111,112));
        //it still points to 33
        assertListEquals(list.toArray(), 9, 7, 0, 11, 22, 33, 111, 100, 99, 44 /*20 */, 33, 3, 88, 111, 112);
        assertEquals(33, castType(it.cursor.value));
        assertEquals(10, it.cursor.index);

        //move again of 3 : 100
        it.gotoNext().gotoNext().gotoNext();
        //remove the previous = 99
        removed = it.removePrevious();
        assertEquals(8, removed.index);
        assertEquals(99, castType(removed.value));

        assertListEquals(list.toArray(), 9, 7, 0, 11, 22, 33, 111, 100, /*99*/44 /*20 */, 33, 3, 88, 111, 112);
        //the iterator still points to 100
        assertEquals(100, castType(it.cursor.value));
        assertEquals(7, it.cursor.index);

        //move again of 4 : 11
        it.gotoNext().gotoNext().gotoNext().gotoNext();
        //remove itself
        it.delete();
        //the iterator now points to 0
        assertListEquals(list.toArray(), 9, 7, 0 /*11 */, 22, 33, 111, 100, /*99*/44 /*20 */, 33, 3, 88, 111, 112);
        assertEquals(0, castType(it.cursor.value));
        assertEquals(2, it.cursor.index);
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
            KType k = cast(rnd.nextInt(modulo));

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
                assertEquals(
                        ad.removeFirstOccurrence(k),
                        list.removeFirstOccurrence(k) >= 0);
            }
            else if (op < 8)
            {
                assertEquals(
                        ad.removeLastOccurrence(k),
                        list.removeLastOccurrence(k) >= 0);
            }
            assertEquals(ad.size(), list.size());
        }

        assertArrayEquals(ad.toArray(), list.toArray());
    }

    /*! #end !*/

    /*! #if ($TemplateOptions.KTypeGeneric) !*/
    @SuppressWarnings("unchecked")
    /*! #end !*/
    @Test
    public void testHashCodeEquals2()
    {
        KTypeLinkedList<KType> l0 = KTypeLinkedList.newInstance();
        assertEquals(1, l0.hashCode());
        assertEquals(l0, KTypeLinkedList.from());

        KTypeLinkedList<KType> l1 = KTypeLinkedList.from(k1, k2, k3);
        KTypeLinkedList<KType> l2 = KTypeLinkedList.from(k1, k2);
        l2.addLast(k3);

        assertEquals(l1.hashCode(), l2.hashCode());
        assertEquals(l1, l2);
    }

    /**
     * Test natural ordering in the deque
     * @param expected
     * @param actual
     * @param length
     */
    private void assertOrder(KTypeLinkedList<KType> order)
    {
        //first, export to an array
        KType[] export = (KType[]) order.toArray();

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
