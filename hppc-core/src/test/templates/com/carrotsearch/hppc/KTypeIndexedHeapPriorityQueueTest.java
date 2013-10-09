package com.carrotsearch.hppc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;
import java.util.TreeMap;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.carrotsearch.hppc.KTypeIndexedHeapPriorityQueue.ValueIterator;
import com.carrotsearch.hppc.cursors.*;
import com.carrotsearch.hppc.mutables.LongHolder;
import com.carrotsearch.hppc.predicates.*;
import com.carrotsearch.hppc.procedures.*;
import com.carrotsearch.hppc.sorting.*;

/**
 * Unit tests for {@link KTypeHeapPriorityQueue}.
 */
/*! ${TemplateOptions.generatedAnnotation} !*/
public class KTypeIndexedHeapPriorityQueueTest<KType> extends AbstractKTypeTest<KType>
{
    /**
     * Per-test fresh initialized instance.
     */
    public KTypeIndexedHeapPriorityQueue<KType> prioq;

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
        prioq = new KTypeIndexedHeapPriorityQueue<KType>(null);
    }

    @After
    public void checkTrailingSpaceUninitialized()
    {
        assertTrue(prioq.isMinHeap());
        assertTrue(prioq.isConsistent());

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
        prioq.insert(0, key1);
        prioq.insert(111, key2);
        assertPrioQueueEquals(prioq, 0, 1, 111, 2);
    }

    /* */
    @Test
    public void testInsert2()
    {
        prioq.insert(5000, key1);
        prioq.insert(0, key2);
        prioq.insert(4, key4);
        prioq.insert(3, key3);
        assertPrioQueueEquals(prioq, 5000, 1, 0, 2, 4, 4, 3, 3);
    }

    /* */
    @Test
    public void testDelete()
    {
        prioq.insert(0, key0);
        prioq.insert(1, key1);
        prioq.insert(2, key2);
        prioq.insert(3, key3);
        prioq.insert(4, key4);

        prioq.deleteIndex(0);
        assertFalse(prioq.containsIndex(0));
        prioq.deleteIndex(4);
        assertFalse(prioq.containsIndex(4));
        prioq.deleteIndex(3);
        assertFalse(prioq.containsIndex(3));
        prioq.deleteIndex(2);
        assertFalse(prioq.containsIndex(2));
        prioq.deleteIndex(1);
        assertFalse(prioq.containsIndex(1));
    }

    /* */
    @Test
    public void testDeleteRandom()
    {
        int COUNT = (int) 2e6;
        Random prng = new Random(791614611L);

        int[] reference = new int[COUNT];
        Arrays.fill(reference, -1);

        int refSize = 0;

        for (int i = 0; i < COUNT; i++)
        {
            int index = prng.nextInt(COUNT);
            int value = prng.nextInt(COUNT);

            //cast twice to narrow conversion
            if (reference[index] == -1)
            {
                reference[index] = value;
                refSize++;

                //add in map (there is a resize going on)
                assertTrue(prioq.insert(index, cast(value)));
                assertTrue(prioq.containsIndex(index));
            }

            if (i % 111587 == 0)
            {
                checkConsistency(prioq);
                assertTrue(prioq.isMinHeap());
            }
        }

        checkConsistency(prioq);
        assertTrue(prioq.isMinHeap());

        assertEquals(refSize, prioq.size());


        //B) delete some indices
        for (int i = 0; i < COUNT; i++)
        {
            if (i % 3 == 0)
            {
                reference[i] = -1;

                //remove also in the prio queue
                prioq.deleteIndex(i);
                assertFalse("" + i, prioq.containsIndex(i));
                assertFalse("" + i, prioq.deleteIndex(i));
            }

            if (i % 87450 == 0)
            {
                checkConsistency(prioq);
                assertTrue(prioq.isMinHeap());
            }
        }

        checkConsistency(prioq);
        assertTrue(prioq.isMinHeap());

        //B-2) the remaining slots exists
        for (int index = 0; index < reference.length; index++)
        {
            if (reference[index] != -1)
            {
                assertTrue(prioq.containsIndex(index));
                assertTrue(prioq.deleteIndex(index));
                assertFalse("" + index, prioq.containsIndex(index));
                assertFalse("" + index, prioq.deleteIndex(index));

                if (index % 55587 == 0)
                {
                    checkConsistency(prioq);
                    assertTrue(prioq.isMinHeap());
                }
            }
        }

        assertEquals(0, prioq.size());
    }

    /* */
    @Test
    public void testInsertRandom()
    {
        int COUNT = (int) 2e6;
        Random prng = new Random(87842154L);

        HashMap<Integer, Integer> reference = new HashMap<Integer, Integer>();

        for (int i = 0; i < COUNT; i++)
        {
            int index = prng.nextInt(COUNT);
            int value = prng.nextInt(COUNT);
            value = castType(cast(value));

            //cast twice to narrow conversion
            if (!reference.containsKey(index))
            {
                reference.put(index, value);

                //add in map (there is a resize going on)
                assertTrue(prioq.insert(index, cast(value)));
                int currentSize = prioq.size();
                //attempt to add twice
                assertFalse(prioq.insert(index, cast(value)));
                //size has not changed
                assertEquals(currentSize, prioq.size());
            }

            if (i % 110587 == 0)
            {
                checkConsistency(prioq);
                assertTrue(prioq.isMinHeap());
            }
        }

        checkConsistency(prioq);
        assertTrue(prioq.isMinHeap());

        assertEquals(reference.size(), prioq.size());

        //iterate all the reference and check it also exists in prio queue
        for (int index : reference.keySet())
        {
            assertTrue(prioq.containsIndex(index));
            assertEquals(reference.get(index).intValue(), castType(prioq.getIndex(index)));
        }
    }

    /* */
    @Test
    public void testPopTop()
    {
        insertElements(prioq, 0, 10, /**/1, 9, /**/2, 8, /**/3, 7, /**/4, 6, /**/5, 5, /**/6, 4, /**/7, 3);

        assertPrioQueueEquals(prioq, 0, 10, /**/1, 9, /**/2, 8, /**/3, 7, /**/4, 6, /**/5, 5, /**/6, 4, /**/7, 3);

        assertEquals(3, castType(prioq.popTop()));
        assertPrioQueueEquals(prioq, 6, 4, /**/5, 5, /**/4, 6, /**/3, 7, /**/2, 8, /**/1, 9, /**/0, 10);
        assertEquals(4, castType(prioq.popTop()));
        assertPrioQueueEquals(prioq, /**/5, 5, /**/4, 6, /**/3, 7, /**/2, 8, /**/1, 9, /**/0, 10);
        assertEquals(5, castType(prioq.popTop()));
        assertPrioQueueEquals(prioq, /**/4, 6, /**/3, 7, /**/2, 8, /**/1, 9, /**/0, 10);
        assertEquals(6, castType(prioq.popTop()));
        assertPrioQueueEquals(prioq, /**/3, 7, /**/2, 8, /**/1, 9, /**/0, 10);
        assertEquals(7, castType(prioq.popTop()));
        assertPrioQueueEquals(prioq, /**/2, 8, /**/1, 9, /**/0, 10);
        assertEquals(8, castType(prioq.popTop()));
        assertPrioQueueEquals(prioq, /**/1, 9, /**/0, 10);
        assertEquals(9, castType(prioq.popTop()));
        assertPrioQueueEquals(prioq, /**/0, 10);
    }

    /* */
    @Test
    public void testRemoveAll()
    {
        insertElements(prioq, 1000, 0, 1001, 1, 1002, 0, 1003, 1, 1004, 0);

        assertEquals(0, prioq.removeAllOccurrences(k2));
        assertEquals(3, prioq.removeAllOccurrences(k0));
        assertPrioQueueEquals(prioq, 1001, 1, 1003, 1);

        assertEquals(2, prioq.removeAllOccurrences(k1));
        assertTrue(prioq.isEmpty());
    }

    /* */
    @Test
    public void testRemoveAllFromLookupContainer()
    {
        insertElements(prioq, 10, 0, 11, 1, 12, 2, 13, 1, 0, 0);

        KTypeOpenHashSet<KType> list2 = KTypeOpenHashSet.newInstance();
        list2.add(asArray(0, 2));

        assertEquals(3, prioq.removeAll(list2));
        assertEquals(0, prioq.removeAll(list2));

        assertPrioQueueEquals(prioq, 11, 1, 13, 1);
    }

    /* */
    @Test
    public void testRemoveAllWithPredicate()
    {
        insertElements(prioq, 0, 0, 1, 1, 2, 2, 11, 1, 44, 4);

        assertEquals(3, prioq.removeAll(new KTypePredicate<KType>()
                {
            public boolean apply(KType v)
            {
                return v == key1 || v == key2;
            };
                }));

        assertPrioQueueEquals(prioq, 0, 0, 44, 4);
    }

    /* */
    @Test
    public void testRemoveAllEverything()
    {
        insertElements(prioq, 0, 0, 1, 1, 2, 2, 11, 1, 44, 4);

        assertEquals(5, prioq.removeAll(new KTypePredicate<KType>()
                {
            public boolean apply(KType v)
            {
                return true;
            };
                }));

        assertEquals(0, prioq.size());
    }

    /* */
    @Test
    public void testRemoveAllWithIndexedPredicate()
    {
        insertElements(prioq, 0, 0, 1, 1, 2, 2, 3, 4, 8, 12, 11, 1, 44, 4, 12, 13);

        assertEquals(3, prioq.removeAll(new KTypeIndexedPredicate<KType>()
                {
            public boolean apply(int index, KType v)
            {
                return index == 1 || index == 11 || index == 12;
            };
                }));

        assertPrioQueueEquals(prioq, 0, 0, 2, 2, 3, 4, 8, 12, 44, 4);
    }

    /* */
    @Test
    public void testRemoveAllWithIndexedPredicateEverything()
    {
        insertElements(prioq, 0, 0, 1, 1, 2, 2, 3, 4, 8, 12, 11, 1, 44, 4, 12, 13);

        assertEquals(8, prioq.removeAll(new KTypeIndexedPredicate<KType>()
                {
            public boolean apply(int index, KType v)
            {
                return true;
            };
                }));

        assertEquals(0, prioq.size());
    }

    /* */
    @Test
    public void testRetainAllWithPredicate()
    {
        insertElements(prioq, 10, 0, 11, 1, 12, 2, 13, 1, 14, 0);

        assertEquals(2, prioq.retainAll(new KTypePredicate<KType>()
                {
            public boolean apply(KType v)
            {
                return v == key1 || v == key2;
            };
                }));

        assertPrioQueueEquals(prioq, 11, 1, 13, 1, 12, 2);
    }

    /* */
    @Test
    public void testRetainAllWithIndexedPredicate()
    {
        insertElements(prioq, 10, 0, 11, 1, 12, 2, 13, 1, 14, 0);

        assertEquals(3, prioq.retainAll(new KTypeIndexedPredicate<KType>()
                {
            public boolean apply(int index, KType v)
            {
                return index == 11 || index == 13;
            };
                }));

        assertPrioQueueEquals(prioq, 11, 1, 13, 1);
    }

    /* */
    @Test
    public void testIterable()
    {
        int COUNT = (int) 1e6;
        Random prng = new Random(57894132145L);

        HashMap<Integer, Integer> reference = new HashMap<Integer, Integer>();

        for (int i = 0; i < COUNT; i++)
        {
            int index = prng.nextInt(COUNT);
            int value = prng.nextInt(COUNT);

            if (!reference.containsKey(index))
            {
                //cast twice to narrow conversion
                reference.put(index, castType(cast(value)));

                //add in map (there is a resize going on)
                prioq.insert(index, cast(value));
            }
        }
        checkConsistency(prioq);
        assertTrue(prioq.isMinHeap());

        assertEquals(reference.size(), prioq.size());

        //iterate the prio queue to see if it fits
        int count = 0;
        for (KTypeCursor<KType> cursor : prioq)
        {
            assertTrue(reference.containsKey(cursor.index));
            assertEquals(reference.get(cursor.index).intValue(), castType(cursor.value));
            count++;
        }

        assertEquals(reference.size(), count);

        //try to iterate a void Prio queue
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
        insertElements(prioq, 0, 0, 1, 1, 2, 2, 3, 3);
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
        int COUNT = (int) 1e4;
        Random prng = new Random(154447431644L);

        final HashMap<Integer, Integer> reference = new HashMap<Integer, Integer>();

        long checksum = 0;

        for (int i = 0; i < COUNT; i++)
        {
            int index = prng.nextInt(COUNT);
            int value = prng.nextInt(COUNT);
            value = castType(cast(value));

            if (!reference.containsKey(index))
            {
                //cast twice to narrow conversion
                reference.put(index, value);

                //add in map (there is a resize going on)
                prioq.insert(index, cast(value));

                checksum += value;
            }
        }

        assertEquals(reference.size(), prioq.size());

        //B) iterate and check that each value is taken
        final LongHolder val = new LongHolder();
        final LongHolder count = new LongHolder();

        prioq.forEach(new KTypeProcedure<KType>() {

            public void apply(KType v)
            {
                val.value += castType(v);
                count.value++;
            }
        });

        assertEquals(reference.size(), count.value);

        assertEquals(checksum, val.value);

        //C) Indexed forEach, test that each value is passed on to apply
        count.value = 0;

        prioq.indexedForEach(new KTypeIndexedProcedure<KType>() {

            @Override
            public void apply(int index, KType value)
            {
                assertTrue(reference.containsKey(index));
                assertEquals(reference.get(index).intValue(), castType(value));
                count.value++;
            }
        });

        assertEquals(reference.size(), count.value);
    }



    /* */
    @Test
    public void testClear()
    {
        insertElements(prioq, 1, 2, 3, 4, 5, 6, 7, 8);
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
        int COUNT = (int) 1e4;
        Random prng = new Random(1513164L);

        final HashMap<Integer, Integer> reference = new HashMap<Integer, Integer>();

        for (int i = 0; i < COUNT; i++)
        {
            int index = prng.nextInt(COUNT);
            int value = prng.nextInt(COUNT);
            value = castType(cast(value));

            if (!reference.containsKey(index))
            {
                //cast twice to narrow conversion
                reference.put(index, value);

                //add in map (there is a resize going on)
                prioq.insert(index, cast(value));
            }
        }

        //create another
        KTypeIndexedHeapPriorityQueue<KType> prioq2 = new KTypeIndexedHeapPriorityQueue<KType>(null);

        for (int index : reference.keySet())
        {
            prioq2.insert(index, cast(reference.get(index)));
        }

        assertEquals(prioq.hashCode(), prioq2.hashCode());
    }

    /* */
    /*! #if ($TemplateOptions.KTypeGeneric) !*/
    @SuppressWarnings("unchecked")
    /*! #end !*/
    @Test
    public void testClone()
    {
        insertElements(prioq, 1,1, 2,2, 3,3);

        KTypeIndexedHeapPriorityQueue<KType> cloned = prioq.clone();
        cloned.removeAllOccurrences(key1);

        assertPrioQueueEquals(prioq, 1, 1, 2, 2, 3, 3);
        assertPrioQueueEquals(cloned, 2,2, 3,3);
    }
    
    @Test
    public void testSyntheticComparable()
    {
        //A) Fill
        int COUNT = (int) 2e6;
        Random prng = new Random(10548708413L);

        KTypeIndexedHeapPriorityQueue<KType> testPQ = new KTypeIndexedHeapPriorityQueue<KType>(10);
        HashMap<Integer, Integer> reference = new HashMap<Integer, Integer>();

        for (int i = 0; i < COUNT; i++)
        {
            int index = prng.nextInt(COUNT);
            int value = prng.nextInt(COUNT);
            value = castType(cast(value));

            if (!reference.containsKey(index))
            {
                //cast twice to narrow conversion
                reference.put(index, value);

                //add in map (there is a resize going on)
                testPQ.insert(index, cast(value));
            }

            if (i % 407001 == 0)
            {
                checkConsistency(prioq);
                assertTrue(prioq.isMinHeap());
            }
        }

        checkConsistency(prioq);
        assertTrue(prioq.isMinHeap());

        //fill reference array
        int[] referenceArray = new int[reference.size()];

        int ii = 0;
        for(int value : reference.values()) {

            referenceArray[ii] = value;
            ii++;
        }

        //B) popTop elements one by one
        //they are supposed to come in natural order
        Arrays.sort(referenceArray);
        int currentSize = referenceArray.length;

        for (int i = 0; i < referenceArray.length - 1; i++)
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

            if (i % 307101 == 0)
            {
                checkConsistency(prioq);
                assertTrue(prioq.isMinHeap());
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
        KTypeIndexedHeapPriorityQueue<KType> testPQ = new KTypeIndexedHeapPriorityQueue<KType>(comp, 10);
        HashMap<Integer, Integer> reference = new HashMap<Integer, Integer>();

        for (int i = 0; i < COUNT; i++)
        {
            int index = prng.nextInt(COUNT);
            int value = prng.nextInt(COUNT);
            value = castType(cast(value));

            if (!reference.containsKey(index))
            {
                //cast twice to narrow conversion
                reference.put(index, value);

                //add in map (there is a resize going on)
                testPQ.insert(index, cast(value));
            }

            if (i % 287001 == 0)
            {
                checkConsistency(prioq);
                assertTrue(prioq.isMinHeap());
            }
        }

        checkConsistency(prioq);
        assertTrue(prioq.isMinHeap());

        //fill reference array
        KType[] referenceArray = Intrinsics.newKTypeArray(reference.size());

        int ii = 0;
        for (int value : reference.values())
        {
            referenceArray[ii] = cast(value);
            ii++;
        }

        //B) popTop elements one by one
        //they are supposed to come in inverse-natural order
        KTypeSort.quicksort(referenceArray, comp);

        int currentSize = referenceArray.length;

        for (int i = 0; i < referenceArray.length - 1; i++)
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

            if (i % 311117 == 0)
            {
                checkConsistency(prioq);
                assertTrue(prioq.isMinHeap());
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

        KTypeIndexedHeapPriorityQueue<KType> testContainer = new KTypeIndexedHeapPriorityQueue<KType>(10);

        //fill pq
        Random prng = new Random(215649612148461L);

        for (int i = 0; i < TEST_SIZE; i++)
        {
            testContainer.insert(prng.nextInt(TEST_SIZE), cast(prng.nextInt()));
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

        KTypeIndexedHeapPriorityQueue<KType> testContainer = new KTypeIndexedHeapPriorityQueue<KType>(10);

        //fill pq
        Random prng = new Random(484163441L);

        for (int i = 0; i < TEST_SIZE; i++)
        {

            testContainer.insert(prng.nextInt(TEST_SIZE), cast(prng.nextInt()));
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

        KTypeIndexedHeapPriorityQueue<KType> testContainer = new KTypeIndexedHeapPriorityQueue<KType>(10);

        //fill pq
        Random prng = new Random(89874156187414L);

        for (int i = 0; i < TEST_SIZE; i++)
        {
            testContainer.insert(prng.nextInt(TEST_SIZE), cast(prng.nextInt()));
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

        KTypeIndexedHeapPriorityQueue<KType> testContainer = new KTypeIndexedHeapPriorityQueue<KType>(10);

        //fill pq
        Random prng = new Random(994610788L);

        for (int i = 0; i < TEST_SIZE; i++)
        {
            testContainer.insert(prng.nextInt(TEST_SIZE), cast(prng.nextInt()));
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
        int TEST_SIZE = 100;
        long TEST_ROUNDS = 100;

        KTypeIndexedHeapPriorityQueue<KType> testContainer = new KTypeIndexedHeapPriorityQueue<KType>(10);

        //fill pq
        Random prng = new Random(78411114444L);

        for (int i = 0; i < TEST_SIZE; i++)
        {
            testContainer.insert(prng.nextInt(TEST_SIZE), cast(prng.nextInt()));
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

        KTypeIndexedHeapPriorityQueue<KType> testContainer = new KTypeIndexedHeapPriorityQueue<KType>(10);

        //fill pq
        Random prng = new Random(9774442154544L);

        for (int i = 0; i < TEST_SIZE; i++)
        {
            testContainer.insert(prng.nextInt(TEST_SIZE), cast(prng.nextInt()));
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
     * Check if the indexed, prio queue content is identical to
     * int...elemenents made of (indexes, values) alternated
     */
    public void assertPrioQueueEquals(KTypeIndexedHeapPriorityQueue<KType> obj, int... elements)
    {
        assertEquals(elements.length / 2, obj.size());

        //test
        int i = 0;
        while (i < elements.length)
        {
            assertTrue(obj.containsIndex(elements[i]));
            assertEquals(elements[i + 1], castType(obj.getIndex(elements[i])));
            i++;
            i++;
        }
    }

    public void insertElements(KTypeIndexedHeapPriorityQueue<KType> pq, int... elements)
    {
        int i = 0;
        while (i < elements.length) {

            pq.insert(elements[i], cast(elements[i + 1]));
            i++;
            i++;
        }
    }

    public void checkConsistency(KTypeIndexedHeapPriorityQueue<KType> prio)
    {
        if (prio.size() > 0)
        {
            //A) For each valid index, (in pq), there is match in position in qp
            for (int index = 0; index < prio.pq.length; index++)
            {
                if (prio.pq[index] > 0)
                {
                    if (index != prio.qp[prio.pq[index]])
                    {
                        assertTrue(String.format("index=%d, size=%d , pq[index] = %d, ==> qp[pq[index]] = %d",
                                index, prio.size(), prio.pq[index], prio.qp[prio.pq[index]]), index == prio.qp[prio.pq[index]]);
                    }
                }
            }
        }
    }
}
