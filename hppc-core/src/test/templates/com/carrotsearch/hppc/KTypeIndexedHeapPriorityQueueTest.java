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
import org.junit.Assert;
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
//${TemplateOptions.doNotGenerateKType("BOOLEAN")}
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
        Assert.assertTrue(prioq.isMinHeap());
        Assert.assertTrue(prioq.isConsistent());

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

        Assert.assertEquals(0, castType(prioq.deleteIndex(0)));
        Assert.assertFalse(prioq.containsIndex(0));
        Assert.assertEquals(4, castType(prioq.deleteIndex(4)));
        Assert.assertFalse(prioq.containsIndex(4));
        Assert.assertEquals(3, castType(prioq.deleteIndex(3)));
        Assert.assertFalse(prioq.containsIndex(3));
        Assert.assertEquals(2, castType(prioq.deleteIndex(2)));
        Assert.assertFalse(prioq.containsIndex(2));
        Assert.assertEquals(1, castType(prioq.deleteIndex(1)));
        Assert.assertFalse(prioq.containsIndex(1));

        //try to delete a non-existent element index
        Assert.assertEquals(castType(prioq.getDefaultValue()), castType(prioq.deleteIndex(10)));
    }

    /* */
    @Test
    public void testDeleteRandom()
    {
        final int COUNT = (int) 2e6;
        final Random prng = new Random(791614611L);

        final int[] reference = new int[COUNT];
        Arrays.fill(reference, -1);

        int refSize = 0;

        for (int i = 0; i < COUNT; i++)
        {
            final int index = prng.nextInt(COUNT);
            final int value = prng.nextInt(COUNT);

            //cast twice to narrow conversion
            if (reference[index] == -1)
            {
                reference[index] = value;
                refSize++;

                //add in map (there is a resize going on)
                Assert.assertTrue(prioq.insert(index, cast(value)));
                Assert.assertTrue(prioq.containsIndex(index));
            }

            if (i % 111587 == 0)
            {
                checkConsistency(prioq);
                Assert.assertTrue(prioq.isMinHeap());
            }
        }

        checkConsistency(prioq);
        Assert.assertTrue(prioq.isMinHeap());

        Assert.assertEquals(refSize, prioq.size());


        //B) delete some indices
        for (int i = 0; i < COUNT; i++)
        {
            if (i % 3 == 0)
            {
                reference[i] = -1;

                //remove also in the prio queue
                prioq.deleteIndex(i);
                Assert.assertFalse("" + i, prioq.containsIndex(i));
                Assert.assertEquals("" + i, castType(prioq.getDefaultValue()), castType(prioq.deleteIndex(i)));
            }

            if (i % 87450 == 0)
            {
                checkConsistency(prioq);
                Assert.assertTrue(prioq.isMinHeap());
            }
        }

        checkConsistency(prioq);
        Assert.assertTrue(prioq.isMinHeap());

        //B-2) the remaining slots exists
        for (int index = 0; index < reference.length; index++)
        {
            if (reference[index] != -1)
            {
                Assert.assertTrue(prioq.containsIndex(index));
                prioq.deleteIndex(index);
                Assert.assertFalse("" + index, prioq.containsIndex(index));
                Assert.assertEquals("" + index, castType(prioq.getDefaultValue()), castType(prioq.deleteIndex(index)));

                if (index % 55587 == 0)
                {
                    checkConsistency(prioq);
                    Assert.assertTrue(prioq.isMinHeap());
                }
            }
        }

        Assert.assertEquals(0, prioq.size());
    }

    /* */
    @Test
    public void testInsertRandom()
    {
        final int COUNT = (int) 2e6;
        final Random prng = new Random(87842154L);

        final HashMap<Integer, Integer> reference = new HashMap<Integer, Integer>();

        for (int i = 0; i < COUNT; i++)
        {
            final int index = prng.nextInt(COUNT);
            int value = prng.nextInt(COUNT);
            value = castType(cast(value));

            //cast twice to narrow conversion
            if (!reference.containsKey(index))
            {
                reference.put(index, value);

                //add in map (there is a resize going on)
                Assert.assertTrue(prioq.insert(index, cast(value)));
                final int currentSize = prioq.size();
                //attempt to add twice
                Assert.assertFalse(prioq.insert(index, cast(value)));
                //size has not changed
                Assert.assertEquals(currentSize, prioq.size());
            }

            if (i % 110587 == 0)
            {
                checkConsistency(prioq);
                Assert.assertTrue(prioq.isMinHeap());
            }
        }

        checkConsistency(prioq);
        Assert.assertTrue(prioq.isMinHeap());

        Assert.assertEquals(reference.size(), prioq.size());

        //iterate all the reference and check it also exists in prio queue
        for (final int index : reference.keySet())
        {
            Assert.assertTrue(prioq.containsIndex(index));
            Assert.assertEquals(reference.get(index).intValue(), castType(prioq.getIndex(index)));
        }
    }

    /* */
    @Test
    public void testPopTop()
    {
        insertElements(prioq, 0, 10, /**/1, 9, /**/2, 8, /**/3, 7, /**/4, 6, /**/5, 5, /**/6, 4, /**/7, 3);

        assertPrioQueueEquals(prioq, 0, 10, /**/1, 9, /**/2, 8, /**/3, 7, /**/4, 6, /**/5, 5, /**/6, 4, /**/7, 3);

        Assert.assertEquals(3, castType(prioq.popTop()));
        assertPrioQueueEquals(prioq, 6, 4, /**/5, 5, /**/4, 6, /**/3, 7, /**/2, 8, /**/1, 9, /**/0, 10);
        Assert.assertEquals(4, castType(prioq.popTop()));
        assertPrioQueueEquals(prioq, /**/5, 5, /**/4, 6, /**/3, 7, /**/2, 8, /**/1, 9, /**/0, 10);
        Assert.assertEquals(5, castType(prioq.popTop()));
        assertPrioQueueEquals(prioq, /**/4, 6, /**/3, 7, /**/2, 8, /**/1, 9, /**/0, 10);
        Assert.assertEquals(6, castType(prioq.popTop()));
        assertPrioQueueEquals(prioq, /**/3, 7, /**/2, 8, /**/1, 9, /**/0, 10);
        Assert.assertEquals(7, castType(prioq.popTop()));
        assertPrioQueueEquals(prioq, /**/2, 8, /**/1, 9, /**/0, 10);
        Assert.assertEquals(8, castType(prioq.popTop()));
        assertPrioQueueEquals(prioq, /**/1, 9, /**/0, 10);
        Assert.assertEquals(9, castType(prioq.popTop()));
        assertPrioQueueEquals(prioq, /**/0, 10);
    }

    /* */
    @Test
    public void testRemoveAll()
    {
        insertElements(prioq, 1000, 0, 1001, 1, 1002, 0, 1003, 1, 1004, 0);

        Assert.assertEquals(0, prioq.removeAllOccurrences(k2));
        Assert.assertEquals(3, prioq.removeAllOccurrences(k0));
        assertPrioQueueEquals(prioq, 1001, 1, 1003, 1);

        Assert.assertEquals(2, prioq.removeAllOccurrences(k1));
        Assert.assertTrue(prioq.isEmpty());
    }

    /* */
    @Test
    public void testRemoveAllFromLookupContainer()
    {
        insertElements(prioq, 10, 0, 11, 1, 12, 2, 13, 1, 0, 0);

        final KTypeOpenHashSet<KType> list2 = KTypeOpenHashSet.newInstance();
        list2.add(asArray(0, 2));

        Assert.assertEquals(3, prioq.removeAll(list2));
        Assert.assertEquals(0, prioq.removeAll(list2));

        assertPrioQueueEquals(prioq, 11, 1, 13, 1);
    }

    /* */
    @Test
    public void testRemoveAllWithPredicate()
    {
        insertElements(prioq, 0, 0, 1, 1, 2, 2, 11, 1, 44, 4);

        Assert.assertEquals(3, prioq.removeAll(new KTypePredicate<KType>()
                {
            @Override
            public boolean apply(final KType v)
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

        Assert.assertEquals(5, prioq.removeAll(new KTypePredicate<KType>()
                {
            @Override
            public boolean apply(final KType v)
            {
                return true;
            };
                }));

        Assert.assertEquals(0, prioq.size());
    }

    /* */
    @Test
    public void testRemoveAllWithIndexedPredicate()
    {
        insertElements(prioq, 0, 0, 1, 1, 2, 2, 3, 4, 8, 12, 11, 1, 44, 4, 12, 13);

        Assert.assertEquals(3, prioq.removeAll(new KTypeIndexedPredicate<KType>()
                {
            @Override
            public boolean apply(final int index, final KType v)
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

        Assert.assertEquals(8, prioq.removeAll(new KTypeIndexedPredicate<KType>()
                {
            @Override
            public boolean apply(final int index, final KType v)
            {
                return true;
            };
                }));

        Assert.assertEquals(0, prioq.size());
    }

    /* */
    @Test
    public void testRetainAllWithPredicate()
    {
        insertElements(prioq, 10, 0, 11, 1, 12, 2, 13, 1, 14, 0);

        Assert.assertEquals(2, prioq.retainAll(new KTypePredicate<KType>()
                {
            @Override
            public boolean apply(final KType v)
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

        Assert.assertEquals(3, prioq.retainAll(new KTypeIndexedPredicate<KType>()
                {
            @Override
            public boolean apply(final int index, final KType v)
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
        final int COUNT = (int) 1e6;
        final Random prng = new Random(57894132145L);

        final HashMap<Integer, Integer> reference = new HashMap<Integer, Integer>();

        for (int i = 0; i < COUNT; i++)
        {
            final int index = prng.nextInt(COUNT);
            final int value = prng.nextInt(COUNT);

            if (!reference.containsKey(index))
            {
                //cast twice to narrow conversion
                reference.put(index, castType(cast(value)));

                //add in map (there is a resize going on)
                prioq.insert(index, cast(value));
            }
        }
        checkConsistency(prioq);
        Assert.assertTrue(prioq.isMinHeap());

        Assert.assertEquals(reference.size(), prioq.size());

        //iterate the prio queue to see if it fits
        int count = 0;
        for (final KTypeCursor<KType> cursor : prioq)
        {
            Assert.assertTrue(reference.containsKey(cursor.index));
            Assert.assertEquals(reference.get(cursor.index).intValue(), castType(cursor.value));
            count++;
        }

        Assert.assertEquals(reference.size(), count);

        //try to iterate a void Prio queue
        count = 0;
        prioq.clear();
        Assert.assertEquals(0, prioq.size());
        Assert.assertTrue(prioq.isEmpty());

        for (@SuppressWarnings("unused") final
                KTypeCursor<KType> cursor : prioq)
        {
            count++;
        }
        Assert.assertEquals(0, count);
    }

    /* */
    @Test
    public void testIterator()
    {
        insertElements(prioq, 0, 0, 1, 1, 2, 2, 3, 3);
        final Iterator<KTypeCursor<KType>> iterator = prioq.iterator();
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
        final int COUNT = (int) 1e4;
        final Random prng = new Random(154447431644L);

        final HashMap<Integer, Integer> reference = new HashMap<Integer, Integer>();

        long checksum = 0;

        for (int i = 0; i < COUNT; i++)
        {
            final int index = prng.nextInt(COUNT);
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

        Assert.assertEquals(reference.size(), prioq.size());

        //B) iterate and check that each value is taken
        final LongHolder val = new LongHolder();
        final LongHolder count = new LongHolder();

        prioq.forEach(new KTypeProcedure<KType>() {

            @Override
            public void apply(final KType v)
            {
                val.value += castType(v);
                count.value++;
            }
        });

        Assert.assertEquals(reference.size(), count.value);

        Assert.assertEquals(checksum, val.value);

        //C) Indexed forEach, test that each value is passed on to apply
        count.value = 0;

        prioq.indexedForEach(new KTypeIndexedProcedure<KType>() {

            @Override
            public void apply(final int index, final KType value)
            {
                Assert.assertTrue(reference.containsKey(index));
                Assert.assertEquals(reference.get(index).intValue(), castType(value));
                count.value++;
            }
        });

        Assert.assertEquals(reference.size(), count.value);
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
        final int COUNT = (int) 1e4;
        final Random prng = new Random(1513164L);

        final HashMap<Integer, Integer> reference = new HashMap<Integer, Integer>();

        for (int i = 0; i < COUNT; i++)
        {
            final int index = prng.nextInt(COUNT);
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
        final KTypeIndexedHeapPriorityQueue<KType> prioq2 = new KTypeIndexedHeapPriorityQueue<KType>(null);

        for (final int index : reference.keySet())
        {
            prioq2.insert(index, cast(reference.get(index)));
        }

        Assert.assertEquals(prioq.hashCode(), prioq2.hashCode());
    }

    /* */
    /*! #if ($TemplateOptions.KTypeGeneric) !*/
    @SuppressWarnings("unchecked")
    /*! #end !*/
    @Test
    public void testClone()
    {
        insertElements(prioq, 1,1, 2,2, 3,3);

        final KTypeIndexedHeapPriorityQueue<KType> cloned = prioq.clone();
        cloned.removeAllOccurrences(key1);

        assertPrioQueueEquals(prioq, 1, 1, 2, 2, 3, 3);
        assertPrioQueueEquals(cloned, 2,2, 3,3);
    }

    @Test
    public void testSyntheticComparable()
    {
        //A) Fill
        final int COUNT = (int) 2e6;
        final Random prng = new Random(10548708413L);

        final KTypeIndexedHeapPriorityQueue<KType> testPQ = new KTypeIndexedHeapPriorityQueue<KType>(10);
        final HashMap<Integer, Integer> reference = new HashMap<Integer, Integer>();

        for (int i = 0; i < COUNT; i++)
        {
            final int index = prng.nextInt(COUNT);
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
                Assert.assertTrue(prioq.isMinHeap());
            }
        }

        checkConsistency(prioq);
        Assert.assertTrue(prioq.isMinHeap());

        //fill reference array
        final int[] referenceArray = new int[reference.size()];

        int ii = 0;
        for(final int value : reference.values()) {

            referenceArray[ii] = value;
            ii++;
        }

        //B) popTop elements one by one
        //they are supposed to come in natural order
        Arrays.sort(referenceArray);
        int currentSize = referenceArray.length;

        for (int i = 0; i < referenceArray.length - 1; i++)
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

            if (i % 307101 == 0)
            {
                checkConsistency(prioq);
                Assert.assertTrue(prioq.isMinHeap());
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

        final int COUNT = (int) 2e6;

        //A) fill COUNT random values in prio-queue
        final KTypeIndexedHeapPriorityQueue<KType> testPQ = new KTypeIndexedHeapPriorityQueue<KType>(comp, 10);
        final HashMap<Integer, Integer> reference = new HashMap<Integer, Integer>();

        for (int i = 0; i < COUNT; i++)
        {
            final int index = prng.nextInt(COUNT);
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
                Assert.assertTrue(prioq.isMinHeap());
            }
        }

        checkConsistency(prioq);
        Assert.assertTrue(prioq.isMinHeap());

        //fill reference array
        final KType[] referenceArray = Intrinsics.newKTypeArray(reference.size());

        int ii = 0;
        for (final int value : reference.values())
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

            if (i % 311117 == 0)
            {
                checkConsistency(prioq);
                Assert.assertTrue(prioq.isMinHeap());
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

        final KTypeIndexedHeapPriorityQueue<KType> testContainer = new KTypeIndexedHeapPriorityQueue<KType>(10);

        //fill pq
        final Random prng = new Random(215649612148461L);

        for (int i = 0; i < TEST_SIZE; i++)
        {
            testContainer.insert(prng.nextInt(TEST_SIZE), cast(prng.nextInt()));
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

        final KTypeIndexedHeapPriorityQueue<KType> testContainer = new KTypeIndexedHeapPriorityQueue<KType>(10);

        //fill pq
        final Random prng = new Random(484163441L);

        for (int i = 0; i < TEST_SIZE; i++)
        {

            testContainer.insert(prng.nextInt(TEST_SIZE), cast(prng.nextInt()));
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

        final KTypeIndexedHeapPriorityQueue<KType> testContainer = new KTypeIndexedHeapPriorityQueue<KType>(10);

        //fill pq
        final Random prng = new Random(89874156187414L);

        for (int i = 0; i < TEST_SIZE; i++)
        {
            testContainer.insert(prng.nextInt(TEST_SIZE), cast(prng.nextInt()));
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

            final KTypeIndexedHeapPriorityQueue<KType>.ValueIterator loopIterator = testContainer.iterator();

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

        final KTypeIndexedHeapPriorityQueue<KType> testContainer = new KTypeIndexedHeapPriorityQueue<KType>(10);

        //fill pq
        final Random prng = new Random(994610788L);

        for (int i = 0; i < TEST_SIZE; i++)
        {
            testContainer.insert(prng.nextInt(TEST_SIZE), cast(prng.nextInt()));
        }

        final int startingPoolSize = testContainer.valueIteratorPool.size();

        int count = 0;
        for (int round = 0; round < TEST_ROUNDS; round++)
        {
            //Classical iterator loop, with manually allocated Iterator
            final long initialPoolSize = testContainer.valueIteratorPool.size();

            final KTypeIndexedHeapPriorityQueue<KType>.ValueIterator loopIterator = testContainer.iterator();

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
        final int TEST_SIZE = 100;
        final long TEST_ROUNDS = 100;

        final KTypeIndexedHeapPriorityQueue<KType> testContainer = new KTypeIndexedHeapPriorityQueue<KType>(10);

        //fill pq
        final Random prng = new Random(78411114444L);

        for (int i = 0; i < TEST_SIZE; i++)
        {
            testContainer.insert(prng.nextInt(TEST_SIZE), cast(prng.nextInt()));
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
        KTypeIndexedHeapPriorityQueue<KType>.ValueIterator loopIterator = null;

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

        final KTypeIndexedHeapPriorityQueue<KType> testContainer = new KTypeIndexedHeapPriorityQueue<KType>(10);

        //fill pq
        final Random prng = new Random(9774442154544L);

        for (int i = 0; i < TEST_SIZE; i++)
        {
            testContainer.insert(prng.nextInt(TEST_SIZE), cast(prng.nextInt()));
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
        KTypeIndexedHeapPriorityQueue<KType>.ValueIterator loopIterator = null;

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

    /**
     * Check if the indexed, prio queue content is identical to
     * int...elemenents made of (indexes, values) alternated
     */
    public void assertPrioQueueEquals(final KTypeIndexedHeapPriorityQueue<KType> obj, final int... elements)
    {
        Assert.assertEquals(elements.length / 2, obj.size());

        //test
        int i = 0;
        while (i < elements.length)
        {
            Assert.assertTrue(obj.containsIndex(elements[i]));
            Assert.assertEquals(elements[i + 1], castType(obj.getIndex(elements[i])));
            i++;
            i++;
        }
    }

    public void insertElements(final KTypeIndexedHeapPriorityQueue<KType> pq, final int... elements)
    {
        int i = 0;
        while (i < elements.length) {

            pq.insert(elements[i], cast(elements[i + 1]));
            i++;
            i++;
        }
    }

    public void checkConsistency(final KTypeIndexedHeapPriorityQueue<KType> prio)
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
                        Assert.assertTrue(String.format("index=%d, size=%d , pq[index] = %d, ==> qp[pq[index]] = %d",
                                index, prio.size(), prio.pq[index], prio.qp[prio.pq[index]]), index == prio.qp[prio.pq[index]]);
                    }
                }
            }
        }
    }
}
