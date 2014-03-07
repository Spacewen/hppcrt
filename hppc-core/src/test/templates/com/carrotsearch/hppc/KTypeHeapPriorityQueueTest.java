package com.carrotsearch.hppc;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.carrotsearch.hppc.cursors.KTypeCursor;
import com.carrotsearch.hppc.mutables.IntHolder;
import com.carrotsearch.hppc.mutables.LongHolder;
import com.carrotsearch.hppc.predicates.KTypePredicate;
import com.carrotsearch.hppc.procedures.KTypeProcedure;
import com.carrotsearch.hppc.sorting.KTypeComparator;
import com.carrotsearch.hppc.sorting.KTypeSort;

/**
 * Unit tests for {@link KTypeHeapPriorityQueue}.
 */

//${TemplateOptions.doNotGenerateKType("BOOLEAN")}
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
    public void checkConsistency()
    {
        if (prioq != null)
        {
            //scan beyond the active zone
            //1-based indexing
            for (int i = prioq.elementsCount + 1; i < prioq.buffer.length; i++)
            {
                /*! #if ($TemplateOptions.KTypeGeneric) !*/
                Assert.assertTrue(Intrinsics.<KType> defaultKTypeValue() == prioq.buffer[i]);
                /*! #end !*/
            }
        }

        //check heap property
        Assert.assertTrue(isMinHeap(prioq));
    }

    /* */
    @Test
    public void testInitiallyEmpty()
    {
        Assert.assertEquals(0, prioq.size());
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
        final KTypeArrayList<KType> list2 = KTypeArrayList.newInstance();
        list2.add(asArray(0, 1, 2));

        final KTypeArrayList<KType> list3 = KTypeArrayList.newInstance();
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
        Assert.assertEquals(8, prioq.size());
        Assert.assertTrue(isMinHeap(prioq));
        Assert.assertFalse(prioq.isEmpty());

        Assert.assertEquals(3, castType(prioq.popTop()));
        assertPrioQueueEquals(prioq, 4, 5, 6, 7, 8, 9, 10);
        Assert.assertEquals(7, prioq.size());
        Assert.assertEquals(4, castType(prioq.top()));
        Assert.assertTrue(isMinHeap(prioq));
        Assert.assertFalse(prioq.isEmpty());

        Assert.assertEquals(4, castType(prioq.popTop()));
        assertPrioQueueEquals(prioq, 5, 6, 7, 8, 9, 10);
        Assert.assertEquals(6, prioq.size());
        Assert.assertEquals(5, castType(prioq.top()));
        Assert.assertTrue(isMinHeap(prioq));
        Assert.assertFalse(prioq.isEmpty());

        Assert.assertEquals(5, castType(prioq.popTop()));
        assertPrioQueueEquals(prioq, 6, 7, 8, 9, 10);
        Assert.assertEquals(5, prioq.size());
        Assert.assertEquals(6, castType(prioq.top()));
        Assert.assertTrue(isMinHeap(prioq));
        Assert.assertFalse(prioq.isEmpty());

        Assert.assertEquals(6, castType(prioq.popTop()));
        assertPrioQueueEquals(prioq, 7, 8, 9, 10);
        Assert.assertEquals(4, prioq.size());
        Assert.assertEquals(7, castType(prioq.top()));
        Assert.assertTrue(isMinHeap(prioq));
        Assert.assertFalse(prioq.isEmpty());

        Assert.assertEquals(7, castType(prioq.popTop()));
        assertPrioQueueEquals(prioq, 8, 9, 10);
        Assert.assertEquals(3, prioq.size());
        Assert.assertEquals(8, castType(prioq.top()));
        Assert.assertTrue(isMinHeap(prioq));
        Assert.assertFalse(prioq.isEmpty());

        Assert.assertEquals(8, castType(prioq.popTop()));
        assertPrioQueueEquals(prioq, 9, 10);
        Assert.assertEquals(2, prioq.size());
        Assert.assertEquals(9, castType(prioq.top()));
        Assert.assertTrue(isMinHeap(prioq));
        Assert.assertFalse(prioq.isEmpty());

        Assert.assertEquals(9, castType(prioq.popTop()));
        assertPrioQueueEquals(prioq, 10);
        Assert.assertEquals(1, prioq.size());
        Assert.assertEquals(10, castType(prioq.top()));
        Assert.assertTrue(isMinHeap(prioq));
        Assert.assertFalse(prioq.isEmpty());

        Assert.assertEquals(10, castType(prioq.popTop()));
        Assert.assertEquals(0, prioq.size());
        Assert.assertTrue(prioq.isEmpty());

    }

    /* */
    @Test
    public void testRemoveAll()
    {
        insertElements(prioq, 0, 1, 0, 1, 0);

        Assert.assertEquals(0, prioq.removeAllOccurrences(k2));
        Assert.assertEquals(3, prioq.removeAllOccurrences(k0));
        assertPrioQueueEquals(prioq, 1, 1);

        Assert.assertEquals(2, prioq.removeAllOccurrences(k1));
        Assert.assertTrue(prioq.isEmpty());
    }

    /* */
    @Test
    public void testRemoveAllFromLookupContainer()
    {
        insertElements(prioq, 0, 1, 2, 1, 0);

        final KTypeOpenHashSet<KType> list2 = KTypeOpenHashSet.newInstance();
        list2.add(asArray(0, 2));

        Assert.assertEquals(3, prioq.removeAll(list2));
        Assert.assertEquals(0, prioq.removeAll(list2));

        assertPrioQueueEquals(prioq, 1, 1);
    }

    /* */
    @Test
    public void testRemoveAllWithPredicate()
    {
        insertElements(prioq, 0, 1, 2, 1, 4);

        Assert.assertEquals(3, prioq.removeAll(new KTypePredicate<KType>()
        {
            @Override
            public boolean apply(final KType v)
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

        Assert.assertEquals(2, prioq.retainAll(new KTypePredicate<KType>()
        {
            @Override
            public boolean apply(final KType v)
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
            //the assert below should never be triggered because of the exception
            //so give it an invalid value in case the thing terminates  = initial size
            Assert.assertEquals(5, prioq.removeAll(new KTypePredicate<KType>()
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
        assertPrioQueueEquals(prioq, 0, 1, 2, 4);
        Assert.assertEquals(4, prioq.size());
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

        Assert.assertEquals(count, prioq.size());

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

        Assert.assertEquals(count, prioq.size());

        //B) Iterate and check internal buffer
        final int[] testArray = new int[prioq.size()];

        count = 0;
        for (final KTypeCursor<KType> cursor : prioq)
        {
            testArray[count] = castType(cursor.value);
            count++;
        }
        Assert.assertEquals(count, prioq.size());

        //compare test Array
        Arrays.sort(testArray);
        assertPrioQueueEquals(prioq, testArray);

        //C) try to iterate a void Prio queue
        count = 0;
        prioq.clear();
        Assert.assertEquals(0, prioq.size());
        Assert.assertTrue(prioq.isEmpty());

        for (@SuppressWarnings("unused")
        final KTypeCursor<KType> cursor : prioq)
        {
            count++;
        }
        Assert.assertEquals(0, count);

    }

    /* */
    @Test
    public void testIterator()
    {
        insertElements(prioq, 0, 1, 2, 3);

        final KTypeHeapPriorityQueue<KType>.ValueIterator iterator = prioq.iterator();

        int count = 0;
        while (iterator.hasNext())
        {

            iterator.hasNext();
            iterator.hasNext();
            iterator.hasNext();
            iterator.next();
            count++;
        }
        Assert.assertEquals(count, prioq.size());

        prioq.clear();
        Assert.assertFalse(prioq.iterator().hasNext());
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

            @Override
            public void apply(final KType v)
            {
                holder.value += castType(v);
            }
        });

        Assert.assertEquals(checksum, holder.value);
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

        final long sumResult = prioq.forEach(new KTypeProcedure<KType>() {
            long sum = 0L;

            @Override
            public void apply(final KType v)
            {
                sum += castType(v);
            }
        }).sum;

        Assert.assertEquals(checksum, sumResult);
    }

    /* */
    @Test
    public void testClear()
    {
        insertElements(prioq, 1, 2, 3, 4, 5, 6, 7);
        prioq.clear();
        checkConsistency();
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
        final KTypeHeapPriorityQueue<KType> list2 = new KTypeHeapPriorityQueue<KType>(10);

        for (int i = 0; i < count; i++)
        {
            prioq.insert(cast(i));
            list2.insert(cast(i));
        }

        Assert.assertEquals(prioq.hashCode(), list2.hashCode());

    }

    /* */
    /*! #if ($TemplateOptions.KTypeGeneric) !*/
    @SuppressWarnings("unchecked")
    /*! #end !*/
    @Test
    public void testClone()
    {
        insertElements(prioq, 1, 2, 3);

        final KTypeHeapPriorityQueue<KType> cloned = prioq.clone();
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
        Assert.assertEquals("["
                + key1 + ", "
                + key2 + ", "
                + key3 + "]", prioq.toString());
    }

    @Test
    public void testEqualsComparable()
    {
        final Random prng = new Random(45872243156464L);

        final int COUNT = (int) 100e3;

        //A) fill COUNT random values in prio-queue
        final KTypeHeapPriorityQueue<KType> testPQ = new KTypeHeapPriorityQueue<KType>(74);
        final KTypeHeapPriorityQueue<KType> testPQ2 = new KTypeHeapPriorityQueue<KType>((int) 1e6);

        for (int i = 0; i < COUNT; i++)
        {
            //use a Random number on a small enough range so that it can be exactly represented
            //in Float or Double with no conversion error back and forth.
            final int randomInt = prng.nextInt(1000 * 1000);

            testPQ.insert(cast(randomInt));
            testPQ2.insert(cast(randomInt));
        } //end for

        Assert.assertEquals(COUNT, testPQ.size());
        Assert.assertEquals(COUNT, testPQ2.size());

        //B) compare both
        Assert.assertTrue(testPQ.equals(testPQ2));

    }

    @Test
    public void testEqualsComparator()
    {
        //Inverse natural ordering comparator
        final KTypeComparator<KType> compIndepinstance = new KTypeComparator<KType>() {

            @Override
            public int compare(final KType e1, final KType e2)
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

        //Inverse natural ordering comparator
        final KTypeComparator<KType> compIndepinstance2 = new KTypeComparator<KType>() {

            @Override
            public int compare(final KType e1, final KType e2)
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

        //Inverse natural ordering comparator
        final KTypeComparator<KType> compAlwaysSame = new KTypeComparator<KType>() {

            @Override
            public boolean equals(final Object obj) {

                return true;
            }

            @Override
            public int compare(final KType e1, final KType e2)
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

        //Inverse natural ordering comparator
        final KTypeComparator<KType> compAlwaysSame2 = new KTypeComparator<KType>() {

            @Override
            public boolean equals(final Object obj) {

                return true;
            }

            @Override
            public int compare(final KType e1, final KType e2)
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

        final Random prng = new Random(74172243156464L);

        final int COUNT = (int) 100e3;

        //A) fill COUNT random values in prio-queue
        final KTypeHeapPriorityQueue<KType> testPQ = new KTypeHeapPriorityQueue<KType>(compIndepinstance, 471);
        final KTypeHeapPriorityQueue<KType> testPQSameInstance = new KTypeHeapPriorityQueue<KType>(compIndepinstance, 92365);
        final KTypeHeapPriorityQueue<KType> testPQOtherInstance = new KTypeHeapPriorityQueue<KType>(compIndepinstance2, 1545);
        final KTypeHeapPriorityQueue<KType> testPQSame = new KTypeHeapPriorityQueue<KType>(compAlwaysSame, 10);
        final KTypeHeapPriorityQueue<KType> testPQSame2 = new KTypeHeapPriorityQueue<KType>(compAlwaysSame2, 874122);

        for (int i = 0; i < COUNT; i++)
        {
            //use a Random number on a small enough range so that it can be exactly represented
            //in Float or Double with no conversion error back and forth.
            final int randomInt = prng.nextInt(1000 * 1000);

            testPQ.insert(cast(randomInt));
            testPQSameInstance.insert(cast(randomInt));
            testPQOtherInstance.insert(cast(randomInt));
            testPQSame.insert(cast(randomInt));
            testPQSame2.insert(cast(randomInt));

        } //end for

        //B) compare both

        //same instance , by reference
        Assert.assertTrue(testPQ.equals(testPQSameInstance));

        //different instances, diff by default
        Assert.assertFalse(testPQ.equals(testPQOtherInstance));

        //different instances, but properly compared because instances are equals() = true
        Assert.assertTrue(testPQSame.equals(testPQSame2));
    }

    @Test
    public void testSyntheticComparable()
    {
        final Random prng = new Random(45874131463156464L);

        final int COUNT = (int) 1e6;

        //A) fill COUNT random values in prio-queue
        final KTypeHeapPriorityQueue<KType> testPQ = new KTypeHeapPriorityQueue<KType>(10);
        final int[] referenceArray = new int[COUNT];

        for (int i = 0; i < COUNT; i++)
        {
            //use a Random number on a small enough range so that it can be exactly represented
            //in Float or Double with no conversion error back and forth.
            final int randomInt = prng.nextInt(1000 * 1000);

            final int size = testPQ.size();
            testPQ.insert(cast(randomInt));
            Assert.assertEquals(size + 1, testPQ.size());
            referenceArray[i] = castType(cast(randomInt));

            if (i % 33548 == 0)
            {
                Assert.assertTrue(isMinHeap(testPQ));
            }
        }

        Assert.assertEquals(COUNT, testPQ.size());
        Assert.assertTrue(isMinHeap(testPQ));

        //B) popTop elements one by one
        //they are supposed to come in natural order
        Arrays.sort(referenceArray);
        int currentSize = COUNT;

        for (int i = 0; i < COUNT - 1; i++)
        {
            final int expected = referenceArray[i];
            final int expected_next = referenceArray[i + 1];

            Assert.assertEquals(expected, castType(testPQ.top()));
            //size doesn't change
            Assert.assertEquals(currentSize, testPQ.size());
            Assert.assertEquals(expected, castType(testPQ.top()));

            Assert.assertEquals(expected, castType(testPQ.popTop()));
            //size is smaller by one element
            Assert.assertEquals(currentSize - 1, testPQ.size());
            //top() point now to the next smallest element
            Assert.assertEquals(expected_next, castType(testPQ.top()));

            if (i % 11101 == 0)
            {
                Assert.assertTrue(isMinHeap(testPQ));
            }

            currentSize--;
        }

        Assert.assertEquals(1, testPQ.size());
        testPQ.clear();
        Assert.assertTrue(testPQ.isEmpty());
    }

    @Test
    public void testSyntheticComparator()
    {
        //Inverse natural ordering comparator
        final KTypeComparator<KType> comp = new KTypeComparator<KType>() {

            @Override
            public int compare(final KType e1, final KType e2)
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

        final Random prng = new Random(98754131654131L);

        final int COUNT = (int) 1e6;

        //A) fill COUNT random values in prio-queue
        final KTypeHeapPriorityQueue<KType> testPQ = new KTypeHeapPriorityQueue<KType>(comp, 10);
        final KType[] referenceArray = Intrinsics.newKTypeArray(COUNT);

        for (int i = 0; i < COUNT; i++)
        {
            //use a Random number on a small enough range so that it can be exactly represented
            //in Float of Double with no conversion error back and forth.
            final int randomInt = prng.nextInt(1000 * 1000);

            final int size = testPQ.size();
            testPQ.insert(cast(randomInt));
            Assert.assertEquals(size + 1, testPQ.size());
            referenceArray[i] = cast(randomInt);

            if (i % 50548 == 0)
            {
                Assert.assertTrue(isMinHeap(testPQ));
            }
        }

        Assert.assertEquals(COUNT, testPQ.size());
        Assert.assertTrue(isMinHeap(testPQ));

        //B) popTop elements one by one
        //they are supposed to come in inverse-natural order
        KTypeSort.quicksort(referenceArray, comp);

        int currentSize = COUNT;

        for (int i = 0; i < COUNT - 1; i++)
        {
            final KType expected = referenceArray[i];
            final KType expected_next = referenceArray[i + 1];

            //assure it is indeed in inverse natural order : bigger first
            Assert.assertTrue(castType(expected) >= castType(expected_next));

            Assert.assertEquals(castType(expected), castType(testPQ.top()));
            //size doesn't change
            Assert.assertEquals(currentSize, testPQ.size());
            Assert.assertEquals(castType(expected), castType(testPQ.top()));

            Assert.assertEquals(castType(expected), castType(testPQ.popTop()));
            //size is smaller by one element
            Assert.assertEquals(currentSize - 1, testPQ.size());
            //top() point now to the next smallest element
            Assert.assertEquals(castType(expected_next), castType(testPQ.top()));

            if (i % 28548 == 0)
            {
                Assert.assertTrue(isMinHeap(testPQ));
            }

            currentSize--;
        }

        Assert.assertEquals(1, testPQ.size());
        testPQ.clear();
        Assert.assertTrue(testPQ.isEmpty());
    }

    @Test
    public void testPooledIteratorForEach()
    {
        // Unbroken for-each loop
        final int TEST_SIZE = 10000;
        final long TEST_ROUNDS = 100;

        final KTypeHeapPriorityQueue<KType> testContainer = new KTypeHeapPriorityQueue<KType>(10);

        //fill pq
        final Random prng = new Random(215649612148461L);

        for (int i = 0; i < TEST_SIZE; i++)
        {

            testContainer.insert(cast(prng.nextInt()));
        }

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

        final KTypeHeapPriorityQueue<KType> testContainer = new KTypeHeapPriorityQueue<KType>(10);

        //fill pq
        final Random prng = new Random(484163441L);

        for (int i = 0; i < TEST_SIZE; i++)
        {

            testContainer.insert(cast(prng.nextInt()));
        }

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

        final KTypeHeapPriorityQueue<KType> testContainer = new KTypeHeapPriorityQueue<KType>(10);

        //fill pq
        final Random prng = new Random(89874156187414L);

        for (int i = 0; i < TEST_SIZE; i++)
        {
            testContainer.insert(cast(prng.nextInt()));
        }

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

            final KTypeHeapPriorityQueue<KType>.ValueIterator loopIterator = testContainer.iterator();

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

        final KTypeHeapPriorityQueue<KType> testContainer = new KTypeHeapPriorityQueue<KType>(10);

        //fill pq
        final Random prng = new Random(994610788L);

        for (int i = 0; i < TEST_SIZE; i++)
        {
            testContainer.insert(cast(prng.nextInt()));
        }

        final int startingPoolSize = testContainer.valueIteratorPool.size();

        int count = 0;
        for (int round = 0; round < TEST_ROUNDS; round++)
        {
            //Classical iterator loop, with manually allocated Iterator
            final long initialPoolSize = testContainer.valueIteratorPool.size();

            final KTypeHeapPriorityQueue<KType>.ValueIterator loopIterator = testContainer.iterator();

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

        final KTypeHeapPriorityQueue<KType> testContainer = new KTypeHeapPriorityQueue<KType>(10);

        //fill pq
        final Random prng = new Random(78411114444L);

        for (int i = 0; i < TEST_SIZE; i++)
        {
            testContainer.insert(cast(prng.nextInt()));
        }

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
        KTypeHeapPriorityQueue<KType>.ValueIterator loopIterator = null;

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

        final KTypeHeapPriorityQueue<KType> testContainer = new KTypeHeapPriorityQueue<KType>(10);

        //fill pq
        final Random prng = new Random(9774442154544L);

        for (int i = 0; i < TEST_SIZE; i++)
        {
            testContainer.insert(cast(prng.nextInt()));
        }

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
        KTypeHeapPriorityQueue<KType>.ValueIterator loopIterator = null;

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
    public void testPreallocatedSize()
    {
        final Random randomVK = new Random();
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
            final KTypeHeapPriorityQueue<KType> newHeap = new KTypeHeapPriorityQueue<KType>(PREALLOCATED_SIZE);

            //3) Add PREALLOCATED_SIZE different values. At the end, size() must be == PREALLOCATED_SIZE,
            //and internal buffer/allocated must not have changed of size
            final int contructorBufferSize = newHeap.buffer.length;

            for (int i = 0; i < PREALLOCATED_SIZE; i++)
            {
                newHeap.insert(cast(randomVK.nextInt()));

                //internal size has not changed.
                Assert.assertEquals(contructorBufferSize, newHeap.buffer.length);
            }

            Assert.assertEquals(PREALLOCATED_SIZE, newHeap.size());
        } //end for test runs
    }

    /**
     * Check if the prio queue content is identical to a given ordered sequence of elements.
     */
    private void assertPrioQueueEquals(final KTypeHeapPriorityQueue<KType> obj, final int... elements)
    {
        Assert.assertEquals(elements.length, obj.size());

        final KType[] arrayExport = (KType[]) obj.toArray();

        Arrays.sort(arrayExport);

        for (int ii = 0; ii < arrayExport.length; ii++)
        {
            Assert.assertEquals(elements[ii], castType(arrayExport[ii]));
        }
    }

    private void insertElements(final KTypeHeapPriorityQueue<KType> pq, final int... elements)
    {
        for (int i = 0; i < elements.length; i++)
        {
            pq.insert(cast(elements[i]));
        }
    }

    /**
     * method to test invariant in assert
     */
// is pq[1..N] a min heap?
    private boolean isMinHeap(final KTypeHeapPriorityQueue<KType> q)
    {
        if (q.comparator == null)
        {
            return isMinHeapComparable(q, 1);
        }

        return isMinHeapComparator(q, 1);
    }

// is subtree of pq[1..N] rooted at k a min heap?
    private boolean isMinHeapComparable(final KTypeHeapPriorityQueue<KType> q, final int k)
    {
        final int N = q.elementsCount;

        if (k > N)
            return true;
        final int left = 2 * k, right = 2 * k + 1;

        if (left <= N && Intrinsics.isCompSupKTypeUnchecked(q.buffer[k], q.buffer[left]))
            return false;
        if (right <= N && Intrinsics.isCompSupKTypeUnchecked(q.buffer[k], q.buffer[right]))
            return false;
        //recursively test
        return isMinHeapComparable(q, left) && isMinHeapComparable(q, right);
    }

// is subtree of pq[1..N] rooted at k a min heap?
    private boolean isMinHeapComparator(final KTypeHeapPriorityQueue<KType> q, final int k)
    {
        final int N = q.elementsCount;

        /*! #if ($TemplateOptions.KTypeGeneric) !*/
        final Comparator<KType> comp = q.comparator;
        /*! #else
        KTypeComparator<KType> comp = q.comparator;
        #end !*/

        if (k > N)
            return true;
        final int left = 2 * k, right = 2 * k + 1;

        if (left <= N && comp.compare(q.buffer[k], q.buffer[left]) > 0)
            return false;
        if (right <= N && comp.compare(q.buffer[k], q.buffer[right]) > 0)
            return false;
        //recursively test
        return isMinHeapComparator(q, left) && isMinHeapComparator(q, right);
    }
}
