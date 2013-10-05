package com.carrotsearch.hppc;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Random;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.carrotsearch.hppc.KTypeHeapPriorityQueue.ValueIterator;
import com.carrotsearch.hppc.cursors.*;
import com.carrotsearch.hppc.mutables.IntHolder;
import com.carrotsearch.hppc.mutables.LongHolder;
import com.carrotsearch.hppc.predicates.*;
import com.carrotsearch.hppc.procedures.*;
import com.carrotsearch.hppc.sorting.*;

/**
 * Unit tests for {@link KTypeHeapPriorityQueue}.
 */
/*! ${TemplateOptions.generatedAnnotation} !*/
public class KTypeHeapPriorityQueueTest<KType> extends AbstractKTypeTest<KType>
{
    /**
     * Per-test fresh initialized instance.
     */
    public KTypeHeapPriorityQueue<KType> prioq;

    public volatile long guard;

    @BeforeClass
    public static void configure()
    {
        IteratorPool.configureInitialPoolSize(8);
    }

    /* */
    @Before
    public void initialize()
    {
        prioq = new KTypeHeapPriorityQueue<KType>(null);
    }

    @After
    public void checkTrailingSpaceUninitialized()
    {
        if (prioq != null)
        {
            //scan beyond the active zone
            //1-based indexing
            for (int i = prioq.elementsCount + 1; i < prioq.buffer.length; i++)
            {
                /*! #if ($TemplateOptions.KTypeGeneric) !*/
                assertTrue(Intrinsics.<KType> defaultKTypeValue() == prioq.buffer[i]);
                /*! #end !*/
            }
        }

        assertTrue(prioq.isMinHeap());
    }

    /* */
    @Test
    public void testInitiallyEmpty()
    {
        assertEquals(0, prioq.size());
    }

    /* */
    @Test
    public void testInsert()
    {
        prioq.insert(key1);
        prioq.insert(key2);
        assertPrioQueueEquals(prioq, 1, 2);
    }

    /* */
    @Test
    public void testInsert2()
    {
        prioq.insert(key1);
        prioq.insert(key2);
        prioq.insert(key4);
        prioq.insert(key3);
        assertPrioQueueEquals(prioq, 1, 2, 3, 4);
    }

    /* */
    @Test
    public void testAddAll()
    {
        KTypeArrayList<KType> list2 = KTypeArrayList.newInstance();
        list2.add(asArray(0, 1, 2));

        KTypeArrayList<KType> list3 = KTypeArrayList.newInstance();
        list3.add(asArray(9, 8, 7));

        prioq.addAll(list2);
        prioq.addAll(list2);
        prioq.addAll(list3);

        assertPrioQueueEquals(prioq, 0, 0, 1, 1, 2, 2, 7, 8, 9);

    }

    /* */
    @Test
    public void testPopTop()
    {
        insertElements(prioq, 10, 9, 8, 7, 6, 5, 4, 3);
        assertPrioQueueEquals(prioq, 3, 4, 5, 6, 7, 8, 9, 10);

        assertEquals(3, castType(prioq.popTop()));
        assertPrioQueueEquals(prioq, 4, 5, 6, 7, 8, 9, 10);
        assertEquals(4, castType(prioq.popTop()));
        assertPrioQueueEquals(prioq, 5, 6, 7, 8, 9, 10);
        assertEquals(5, castType(prioq.popTop()));
        assertPrioQueueEquals(prioq, 6, 7, 8, 9, 10);
        assertEquals(6, castType(prioq.popTop()));
        assertPrioQueueEquals(prioq, 7, 8, 9, 10);
        assertEquals(7, castType(prioq.popTop()));
        assertPrioQueueEquals(prioq, 8, 9, 10);
        assertEquals(8, castType(prioq.popTop()));
        assertPrioQueueEquals(prioq, 9, 10);
        assertEquals(9, castType(prioq.popTop()));
        assertPrioQueueEquals(prioq, 10);

        assertTrue(prioq.isMinHeap());
    }

    /* */
    @Test
    public void testRemoveAll()
    {
        insertElements(prioq,0, 1, 0, 1, 0);

        assertEquals(0, prioq.removeAllOccurrences(k2));
        assertEquals(3, prioq.removeAllOccurrences(k0));
        assertPrioQueueEquals(prioq, 1, 1);

        assertEquals(2, prioq.removeAllOccurrences(k1));
        assertTrue(prioq.isEmpty());
    }

    /* */
    @Test
    public void testRemoveAllFromLookupContainer()
    {
        insertElements(prioq, 0, 1, 2, 1, 0);

        KTypeOpenHashSet<KType> list2 = KTypeOpenHashSet.newInstance();
        list2.add(asArray(0, 2));

        assertEquals(3, prioq.removeAll(list2));
        assertEquals(0, prioq.removeAll(list2));

        assertPrioQueueEquals(prioq, 1, 1);
    }

    /* */
    @Test
    public void testRemoveAllWithPredicate()
    {
        insertElements(prioq, 0, 1, 2, 1, 4);

        assertEquals(3, prioq.removeAll(new KTypePredicate<KType>()
                {
            public boolean apply(KType v)
            {
                return v == key1 || v == key2;
            };
                }));

        assertPrioQueueEquals(prioq, 0, 4);
    }

    /* */
    @Test
    public void testRetainAllWithPredicate()
    {
        insertElements(prioq, 0, 1, 2, 1, 0);

        assertEquals(2, prioq.retainAll(new KTypePredicate<KType>()
                {
            public boolean apply(KType v)
            {
                return v == key1 || v == key2;
            };
                }));

        assertPrioQueueEquals(prioq, 1, 1, 2);
    }

    /* */
    @Test
    public void testRemoveAllWithPredicateInterrupted()
    {
        insertElements(prioq, 0, 1, 2, 1, 4);

        final RuntimeException t = new RuntimeException();
        try
        {
            assertEquals(3, prioq.removeAll(new KTypePredicate<KType>()
                    {
                public boolean apply(KType v)
                {
                    if (v == key2)
                        throw t;
                    return v == key1;
                };
                    }));
            fail();
        }
        catch (RuntimeException e)
        {
            // Make sure it's really our exception...
            if (e != t)
                throw e;
        }

        // And check if the list is in consistent state.
        assertPrioQueueEquals(prioq, 0, 1, 2, 4);
        assertEquals(4, prioq.size());
    }

    /* */
    @Test
    public void testGrowth()
    {
        final int count = 500;

        prioq = new KTypeHeapPriorityQueue<KType>(10);

        for (int i = 0; i < count; i++)
        {
            prioq.insert(cast(i));
        }

        assertEquals(count, prioq.size());

    }

    /* */
    @Test
    public void testIterable()
    {
        int count = (int) 1e6;

        //A) fill the prio queue
        prioq = new KTypeHeapPriorityQueue<KType>(10);

        for (int i = 0; i < count; i++)
        {
            prioq.insert(cast(i));
        }

        assertEquals(count, prioq.size());

        //B) Iterate and check internal buffer
        int[] testArray = new int[prioq.size()];

        count = 0;
        for (KTypeCursor<KType> cursor : prioq)
        {
            testArray[count] = castType(cursor.value);
            count++;
        }
        assertEquals(count, prioq.size());

        //compare test Array
        Arrays.sort(testArray);
        assertPrioQueueEquals(prioq, testArray);


        //C) try to iterate a void Prio queue
        count = 0;
        prioq.clear();
        assertEquals(0, prioq.size());
        assertTrue(prioq.isEmpty());

        for (@SuppressWarnings("unused")
        KTypeCursor<KType> cursor : prioq)
        {
            count++;
        }
        assertEquals(0, count);

    }

    /* */
    @Test
    public void testIterator()
    {
        insertElements(prioq, 0, 1, 2, 3);
        Iterator<KTypeCursor<KType>> iterator = prioq.iterator();
        int count = 0;
        while (iterator.hasNext())
        {
            iterator.hasNext();
            iterator.hasNext();
            iterator.hasNext();
            iterator.next();
            count++;
        }
        assertEquals(count, prioq.size());

        prioq.clear();
        assertFalse(prioq.iterator().hasNext());
    }

    /* */
    @Test
    public void testForEachWithProcedure()
    {
        //A) Fill
        final int count = 12587;

        prioq = new KTypeHeapPriorityQueue<KType>(10);

        long checksum = 0;

        for (int i = 0; i < count; i++)
        {
            prioq.insert(cast(i));
            checksum += castType(cast(i));
        }

        final LongHolder holder = new LongHolder();
        holder.value = 0;

        prioq.forEach(new KTypeProcedure<KType>() {

            public void apply(KType v)
            {
                holder.value += castType(v);
            }
        });

        assertEquals(checksum, holder.value);
    }

    /* */
    @Test
    public void testForEachReturnValueFromAnonymousClass()
    {
        //A) Fill
        final int count = 8741;

        prioq = new KTypeHeapPriorityQueue<KType>(10);

        long checksum = 0L;

        for (int i = 0; i < count; i++)
        {
            prioq.insert(cast(i));
            checksum += castType(cast(i));
        }

        final IntHolder holder = new IntHolder();
        holder.value = 0;

        long sumResult = prioq.forEach(new KTypeProcedure<KType>() {
            long sum = 0L;
            public void apply(KType v)
            {
                sum += castType(v);
            }
        }).sum;

        assertEquals(checksum, sumResult);
    }

    /* */
    @Test
    public void testClear()
    {
        insertElements(prioq, 1, 2, 3, 4, 5, 6, 7);
        prioq.clear();
        checkTrailingSpaceUninitialized();
    }

    /* */
    /*! #if ($TemplateOptions.KTypeGeneric) !*/
    @SuppressWarnings("unchecked")
    /*! #end !*/
    @Test
    public void testHashCodeEquals()
    {
        //A) Fill
        final int count = 12784;

        prioq = new KTypeHeapPriorityQueue<KType>(10);
        KTypeHeapPriorityQueue<KType> list2 = new KTypeHeapPriorityQueue<KType>(10);

        for (int i = 0; i < count; i++)
        {
            prioq.insert(cast(i));
            list2.insert(cast(i));
        }

        assertEquals(prioq.hashCode(), list2.hashCode());

    }

    /* */
    /*! #if ($TemplateOptions.KTypeGeneric) !*/
    @SuppressWarnings("unchecked")
    /*! #end !*/
    @Test
    public void testClone()
    {
        insertElements(prioq, 1, 2, 3);

        KTypeHeapPriorityQueue<KType> cloned = prioq.clone();
        cloned.removeAllOccurrences(key1);

        assertPrioQueueEquals(prioq, 1, 2, 3);
        assertPrioQueueEquals(cloned, 2, 3);
    }

    /* */
    /*! #if ($TemplateOptions.KTypeGeneric) !*/
    @SuppressWarnings("unchecked")
    /*! #end !*/
    @Test
    public void testToString()
    {
        insertElements(prioq, 1, 2, 3);
        assertEquals("["
                + key1 + ", "
                + key2 + ", "
                + key3 + "]", prioq.toString());
    }

    @Test
    public void testSyntheticComparable()
    {
        Random prng = new Random(45874131463156464L);

        int COUNT = (int) 2e6;

        //A) fill COUNT random values in prio-queue
        KTypeHeapPriorityQueue<KType> testPQ = new KTypeHeapPriorityQueue<KType>(10);
        int[] referenceArray = new int[COUNT];

        for (int i = 0; i < COUNT; i++)
        {
            //use a Random number on a small enough range so that it can be exactly represented
            //in Float or Double with no conversion error back and forth.
            int randomInt = prng.nextInt(1000 * 1000);

            int size = testPQ.size();
            testPQ.insert(cast(randomInt));
            assertEquals(size + 1, testPQ.size());
            referenceArray[i] = castType(cast(randomInt));

            if (i % 33548 == 0)
            {
                assertTrue(testPQ.isMinHeap());
            }
        }

        assertEquals(COUNT, testPQ.size());
        assertTrue(testPQ.isMinHeap());

        //B) popTop elements one by one
        //they are supposed to come in natural order
        Arrays.sort(referenceArray);
        int currentSize = COUNT;

        for (int i = 0; i < COUNT - 1; i++)
        {
            int expected = referenceArray[i];
            int expected_next = referenceArray[i + 1];

            assertEquals(expected, castType(testPQ.top()));
            //size doesn't change
            assertEquals(currentSize, testPQ.size());
            assertEquals(expected, castType(testPQ.top()));

            assertEquals(expected, castType(testPQ.popTop()));
            //size is smaller by one element
            assertEquals(currentSize - 1, testPQ.size());
            //top() point now to the next smallest element
            assertEquals(expected_next, castType(testPQ.top()));

            if (i % 11101 == 0)
            {
                assertTrue(testPQ.isMinHeap());
            }

            currentSize--;
        }

        assertEquals(1, testPQ.size());
        testPQ.clear();
        assertTrue(testPQ.isEmpty());
    }

    @Test
    public void testSyntheticComparator()
    {
        //Inverse natural ordering comparator
        KTypeComparator<KType> comp = new KTypeComparator<KType>() {

            @Override
            public int compare(KType e1, KType e2)
            {
                int res = 0;

                if (castType(e1) < castType(e2))
                {
                    res = 1;
                }
                else if (castType(e1) > castType(e2))
                {
                    res = -1;
                }

                return res;
            }
        };

        Random prng = new Random(98754131654131L);

        int COUNT = (int) 2e6;

        //A) fill COUNT random values in prio-queue
        KTypeHeapPriorityQueue<KType> testPQ = new KTypeHeapPriorityQueue<KType>(comp, 10);
        KType[] referenceArray = Intrinsics.newKTypeArray(COUNT);

        for (int i = 0; i < COUNT; i++)
        {
            //use a Random number on a small enough range so that it can be exactly represented
            //in Float of Double with no conversion error back and forth.
            int randomInt = prng.nextInt(1000 * 1000);

            int size = testPQ.size();
            testPQ.insert(cast(randomInt));
            assertEquals(size + 1, testPQ.size());
            referenceArray[i] = cast(randomInt);

            if (i % 50548 == 0)
            {
                assertTrue(testPQ.isMinHeap());
            }
        }

        assertEquals(COUNT, testPQ.size());
        assertTrue(testPQ.isMinHeap());

        //B) popTop elements one by one
        //they are supposed to come in inverse-natural order
        KTypeSort.quicksort(referenceArray, comp);

        int currentSize = COUNT;

        for (int i = 0; i < COUNT - 1; i++)
        {
            KType expected = referenceArray[i];
            KType expected_next = referenceArray[i + 1];

            //assure it is indeed in inverse natural order : bigger first
            assertTrue(castType(expected) >= castType(expected_next));

            assertEquals(castType(expected), castType(testPQ.top()));
            //size doesn't change
            assertEquals(currentSize, testPQ.size());
            assertEquals(castType(expected), castType(testPQ.top()));

            assertEquals(castType(expected), castType(testPQ.popTop()));
            //size is smaller by one element
            assertEquals(currentSize - 1, testPQ.size());
            //top() point now to the next smallest element
            assertEquals(castType(expected_next), castType(testPQ.top()));

            if (i % 28548 == 0)
            {
                assertTrue(testPQ.isMinHeap());
            }

            currentSize--;
        }

        assertEquals(1, testPQ.size());
        testPQ.clear();
        assertTrue(testPQ.isEmpty());
    }

    @Test
    public void testPooledIteratorForEach()
    {
        // Unbroken for-each loop
        int TEST_SIZE = 10000;
        long TEST_ROUNDS = 100;

        KTypeHeapPriorityQueue<KType> testContainer = new KTypeHeapPriorityQueue<KType>(10);

        //fill pq
        Random prng = new Random(215649612148461L);

        for (int i = 0; i < TEST_SIZE; i++)
        {

            testContainer.insert(cast(prng.nextInt()));
        }

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

        KTypeHeapPriorityQueue<KType> testContainer = new KTypeHeapPriorityQueue<KType>(10);

        //fill pq
        Random prng = new Random(484163441L);

        for (int i = 0; i < TEST_SIZE; i++)
        {

            testContainer.insert(cast(prng.nextInt()));
        }

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

        KTypeHeapPriorityQueue<KType> testContainer = new KTypeHeapPriorityQueue<KType>(10);

        //fill pq
        Random prng = new Random(89874156187414L);

        for (int i = 0; i < TEST_SIZE; i++)
        {
            testContainer.insert(cast(prng.nextInt()));
        }

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

            ValueIterator<KType> loopIterator = testContainer.iterator();

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

        KTypeHeapPriorityQueue<KType> testContainer = new KTypeHeapPriorityQueue<KType>(10);

        //fill pq
        Random prng = new Random(994610788L);

        for (int i = 0; i < TEST_SIZE; i++)
        {
            testContainer.insert(cast(prng.nextInt()));
        }

        int startingPoolSize = testContainer.valueIteratorPool.size();

        int count = 0;
        for (int round = 0; round < TEST_ROUNDS; round++)
        {
            //Classical iterator loop, with manually allocated Iterator
            long initialPoolSize = testContainer.valueIteratorPool.size();

            ValueIterator<KType> loopIterator = testContainer.iterator();

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

        KTypeHeapPriorityQueue<KType> testContainer = new KTypeHeapPriorityQueue<KType>(10);

        //fill pq
        Random prng = new Random(78411114444L);

        for (int i = 0; i < TEST_SIZE; i++)
        {
            testContainer.insert(cast(prng.nextInt()));
        }

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
        ValueIterator<KType> loopIterator = null;

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

            }
            catch (Exception e)
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

        KTypeHeapPriorityQueue<KType> testContainer = new KTypeHeapPriorityQueue<KType>(10);

        //fill pq
        Random prng = new Random(9774442154544L);

        for (int i = 0; i < TEST_SIZE; i++)
        {
            testContainer.insert(cast(prng.nextInt()));
        }

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
        ValueIterator<KType> loopIterator = null;

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

    /**
     * Check if the prio queue content is identical to a given ordered sequence of elements.
     */
    public void assertPrioQueueEquals(KTypeHeapPriorityQueue<KType> obj, int... elements)
    {
        assertEquals(elements.length, obj.size());

        KType[] arrayExport = (KType[]) obj.toArray();

        Arrays.sort(arrayExport);

        for (int ii = 0; ii < arrayExport.length; ii++)
        {
            assertEquals(elements[ii], castType(arrayExport[ii]));
        }
    }

    public void insertElements(KTypeHeapPriorityQueue<KType> pq, int... elements)
    {
        for (int i = 0; i < elements.length; i++)
        {
            pq.insert(cast(elements[i]));
        }
    }
}
