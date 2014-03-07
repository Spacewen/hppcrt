package com.carrotsearch.hppc;

import static com.carrotsearch.hppc.TestUtils.*;
import static org.junit.Assert.*;

import java.util.*;

import org.junit.*;

import com.carrotsearch.hppc.cursors.KTypeCursor;
import com.carrotsearch.hppc.mutables.IntHolder;
import com.carrotsearch.hppc.predicates.KTypePredicate;
import com.carrotsearch.hppc.procedures.KTypeProcedure;
import com.carrotsearch.hppc.sorting.KTypeComparator;

// ${TemplateOptions.doNotGenerateKType("BOOLEAN")}
/**
 * Unit tests for {@link KTypeArrayDeque}.
 */
/*! ${TemplateOptions.generatedAnnotation} !*/
public class KTypeArrayDequeTest<KType> extends AbstractKTypeTest<KType>
{
    /**
     * Per-test fresh initialized instance.
     */
    public KTypeArrayDeque<KType> deque;

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
        deque = KTypeArrayDeque.newInstance();
        sequence = KTypeArrayList.newInstance();

        for (int i = 0; i < 10000; i++)
            sequence.add(cast(i));
    }

    @After
    public void checkConsistency()
    {
        if (deque != null)
        {
            for (int i = deque.tail; i < deque.head; i = Intrinsics.oneRight(i, deque.buffer.length))
            {
                /*! #if ($TemplateOptions.KTypeGeneric) !*/
                Assert.assertTrue(Intrinsics.<KType> defaultKTypeValue() == deque.buffer[i]);
                /*! #end !*/
            }
        }
    }

    /* */
    @Test
    public void testInitiallyEmpty()
    {
        Assert.assertEquals(0, deque.size());
    }

    /* */
    @Test
    public void testAddFirst()
    {
        deque.addFirst(k1);
        deque.addFirst(k2);
        deque.addFirst(k3);
        TestUtils.assertListEquals(deque.toArray(), 3, 2, 1);
        Assert.assertEquals(3, deque.size());
    }

    /* */
    @Test
    public void testAddLast()
    {
        deque.addLast(k1);
        deque.addLast(k2);
        deque.addLast(k3);
        TestUtils.assertListEquals(deque.toArray(), 1, 2, 3);
        Assert.assertEquals(3, deque.size());
    }

    /* */
    @Test
    public void testAddWithGrowth()
    {
        for (int i = 0; i < sequence.size(); i++)
        {
            deque.addFirst(sequence.buffer[i]);
        }

        TestUtils.assertListEquals(TestUtils.reverse(sequence.toArray()), deque.toArray());
    }

    /* */
    @Test
    public void testAddLastWithGrowth()
    {
        for (int i = 0; i < sequence.size(); i++)
            deque.addLast(sequence.buffer[i]);

        TestUtils.assertListEquals(sequence.toArray(), deque.toArray());
    }

    /* */
    @Test
    public void testAddAllFirst()
    {
        final KTypeArrayList<KType> list2 = KTypeArrayList.newInstance();
        list2.add(asArray(0, 1, 2));

        deque.addFirst(list2);
        TestUtils.assertListEquals(deque.toArray(), 2, 1, 0);
        deque.addFirst(list2);
        TestUtils.assertListEquals(deque.toArray(), 2, 1, 0, 2, 1, 0);

        deque.clear();
        final KTypeArrayDeque<KType> deque2 = new KTypeArrayDeque<KType>();
        deque2.addLast(asArray(0, 1, 2));
        deque.addFirst(deque2);
        TestUtils.assertListEquals(deque.toArray(), 2, 1, 0);
    }

    /* */
    @Test
    public void testAddAllLast()
    {
        final KTypeArrayList<KType> list2 = KTypeArrayList.newInstance();
        list2.add(asArray(0, 1, 2));

        deque.addLast(list2);
        TestUtils.assertListEquals(deque.toArray(), 0, 1, 2);
        deque.addLast(list2);
        TestUtils.assertListEquals(deque.toArray(), 0, 1, 2, 0, 1, 2);

        deque.clear();
        final KTypeArrayDeque<KType> deque2 = new KTypeArrayDeque<KType>();
        deque2.addLast(asArray(0, 1, 2));
        deque.addLast(deque2);
        TestUtils.assertListEquals(deque.toArray(), 0, 1, 2);
    }

    /* */
    @Test
    public void testRemoveFirst()
    {
        deque.addLast(k1);
        deque.addLast(k2);
        deque.addLast(k3);

        deque.removeFirst();
        TestUtils.assertListEquals(deque.toArray(), 2, 3);
        Assert.assertEquals(2, deque.size());

        deque.addFirst(k4);
        TestUtils.assertListEquals(deque.toArray(), 4, 2, 3);
        Assert.assertEquals(3, deque.size());

        deque.removeFirst();
        deque.removeFirst();
        deque.removeFirst();
        Assert.assertEquals(0, deque.toArray().length);
        Assert.assertEquals(0, deque.size());
    }

    /* */
    @Test(expected = AssertionError.class)
    public void testRemoveFirstEmpty()
    {
        deque.removeFirst();
    }

    /* */
    @Test
    public void testRemoveLast()
    {
        deque.addLast(k1);
        deque.addLast(k2);
        deque.addLast(k3);

        deque.removeLast();
        TestUtils.assertListEquals(deque.toArray(), 1, 2);
        Assert.assertEquals(2, deque.size());

        deque.addLast(k4);
        TestUtils.assertListEquals(deque.toArray(), 1, 2, 4);
        Assert.assertEquals(3, deque.size());

        deque.removeLast();
        deque.removeLast();
        deque.removeLast();
        Assert.assertEquals(0, deque.toArray().length);
        Assert.assertEquals(0, deque.size());
    }

    /* */
    @Test(expected = AssertionError.class)
    public void testRemoveLastEmpty()
    {
        deque.removeLast();
    }

    /* */
    @Test
    public void testGetFirst()
    {
        deque.addLast(k1);
        deque.addLast(k2);
        TestUtils.assertEquals2(k1, deque.getFirst());
        deque.addFirst(k3);
        TestUtils.assertEquals2(k3, deque.getFirst());
    }

    /* */
    @Test(expected = AssertionError.class)
    public void testGetFirstEmpty()
    {
        deque.getFirst();
    }

    /* */
    @Test
    public void testGetLast()
    {
        deque.addLast(k1);
        deque.addLast(k2);
        TestUtils.assertEquals2(k2, deque.getLast());
        deque.addLast(k3);
        TestUtils.assertEquals2(k3, deque.getLast());
    }

    /* */
    @Test(expected = AssertionError.class)
    public void testGetLastEmpty()
    {
        deque.getLast();
    }

    /* */
    @Test
    public void testIndexOf()
    {
        deque.addLast(asArray(0, 1, 2, 1, 0, 8, 7, 4, 3, 2));

        /*! #if ($TemplateOptions.KTypeGeneric) !*/
        deque.add((KType) null);
        Assert.assertEquals(10, deque.indexOf(null));
        /*! #end !*/

        Assert.assertEquals(0, deque.indexOf(k0));
        Assert.assertEquals(8, deque.indexOf(k3));
        Assert.assertEquals(-1, deque.indexOf(k9));
        Assert.assertEquals(2, deque.indexOf(k2));
        Assert.assertEquals(5, deque.indexOf(k8));
        Assert.assertEquals(7, deque.indexOf(k4));
    }

    /* */
    @Test
    public void testLastIndexOf()
    {
        deque.addLast(asArray(0, 1, 2, 1, 0, 8, 3, 4, 8, 2));

        /*! #if ($TemplateOptions.KTypeGeneric) !*/
        deque.add((KType) null);
        Assert.assertEquals(10, deque.lastIndexOf(null));
        /*! #end !*/

        Assert.assertEquals(4, deque.lastIndexOf(k0));
        Assert.assertEquals(6, deque.lastIndexOf(k3));
        Assert.assertEquals(-1, deque.indexOf(k9));
        Assert.assertEquals(9, deque.lastIndexOf(k2));
        Assert.assertEquals(8, deque.lastIndexOf(k8));
    }

    /* */
    @Test
    public void testSet()
    {
        deque.addLast(asArray(0, 1, 2));

        TestUtils.assertEquals2(0, deque.set(0, k3));
        TestUtils.assertEquals2(1, deque.set(1, k4));
        TestUtils.assertEquals2(2, deque.set(2, k5));

        TestUtils.assertListEquals(deque.toArray(), 3, 4, 5);
    }

    /* */
    @Test
    public void testGetAndGrowth()
    {
        final int maxGrowth = 10;
        final int count = 500;

        deque = new KTypeArrayDeque<KType>(0,
                new BoundedProportionalArraySizingStrategy(5, maxGrowth, 2));

        for (int i = 0; i < count; i++)
            deque.add(cast(i));

        Assert.assertEquals(count, deque.size());

        for (int i = 0; i < count; i++)
            TestUtils.assertEquals2(cast(i), deque.get(i));

        Assert.assertTrue("Buffer size: 510 <= " + deque.buffer.length,
                deque.buffer.length <= count + maxGrowth);
    }

    /* */
    @Test
    public void testRemove()
    {
        deque.addLast(asArray(0, 1, 2, 3, 4, 5, 6, 7, 8));

        Assert.assertEquals(0, castType(deque.remove(0)));
        Assert.assertEquals(3, castType(deque.remove(2)));
        Assert.assertEquals(2, castType(deque.remove(1)));
        Assert.assertEquals(6, castType(deque.remove(3)));

        TestUtils.assertListEquals(deque.toArray(), 1, 4, 5, 7, 8);
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
            //add in head an in queue indifferently
            if (i % 3 == 0) {
                deque.addFirst(cast(i % modulo));
                sequence.insert(0, cast(i % modulo));
            }
            else {
                deque.addLast(cast(i % modulo));
                sequence.add(cast(i % modulo));
            }
        }

        //at that point, both IndexedContainers are the same
        TestUtils.assertListEquals(deque.toArray(), sequence.toArray());

        final Random rnd = new Random(0xdeadbeef);

        //remove values randomly
        for (int i = 0; i < 500; i++)
        {
            final KType k = cast(rnd.nextInt(modulo));

            //both ArrayList and ArrayDequeue returns the same index:
            Assert.assertEquals(" at i = " + i,
                    sequence.removeFirstOccurrence(k),
                    deque.removeFirstOccurrence(k));
        }

        TestUtils.assertListEquals(deque.toArray(), sequence.toArray());

        //non-existent element
        Assert.assertTrue(0 > deque.removeFirstOccurrence(cast(modulo + 1)));

        //now existing
        deque.addLast(cast(modulo + 1));
        Assert.assertTrue(0 <= deque.removeFirstOccurrence(cast(modulo + 1)));
    }

    /* */
    @Test
    public void testIndexedContainerEquals()
    {
        final int modulo = 10;
        final int count = 10000;
        sequence.clear();

        for (int i = 0; i < count; i++)
        {
            //add in head an in queue indifferently
            if (i % 3 == 0) {
                deque.addFirst(cast(i % modulo));
                sequence.insert(0, cast(i % modulo));
            }
            else {
                deque.addLast(cast(i % modulo));
                sequence.add(cast(i % modulo));
            }
        }

        //both elements by elements are equal
        TestUtils.assertListEquals(deque.toArray(), sequence.toArray());

        //The array list and dequeue are indeed equal
        Assert.assertTrue(deque.equals(sequence));
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
            //add in head an in queue indifferently
            if (i % 3 == 0) {
                deque.addFirst(cast(i % modulo));
                sequence.insert(0, cast(i % modulo));
            }
            else {
                deque.addLast(cast(i % modulo));
                sequence.add(cast(i % modulo));
            }
        }

        //at that point, both IndexedContainers are the same
        TestUtils.assertListEquals(deque.toArray(), sequence.toArray());

        final Random rnd = new Random(0x11223344);

        //remove values randomly
        for (int i = 0; i < 500; i++)
        {
            final KType k = cast(rnd.nextInt(modulo));

            //both ArrayList and ArrayDequeue returns the same index:
            Assert.assertEquals(" at i = " + i,
                    sequence.removeLastOccurrence(k),
                    deque.removeLastOccurrence(k));
        }

        TestUtils.assertListEquals(deque.toArray(), sequence.toArray());

        //non existent element
        Assert.assertTrue(0 > deque.removeLastOccurrence(cast(modulo + 1)));

        //now existing
        deque.addFirst(cast(modulo + 1));
        Assert.assertTrue(0 <= deque.removeLastOccurrence(cast(modulo + 1)));
    }

    /* */
    @Test
    public void testRemoveAllOccurrences()
    {
        deque.addLast(asArray(0, 1, 2, 1, 0, 3, 0));

        Assert.assertEquals(0, deque.removeAllOccurrences(k4));
        Assert.assertEquals(3, deque.removeAllOccurrences(k0));
        TestUtils.assertListEquals(deque.toArray(), 1, 2, 1, 3);
        Assert.assertEquals(1, deque.removeAllOccurrences(k3));
        TestUtils.assertListEquals(deque.toArray(), 1, 2, 1);
        Assert.assertEquals(2, deque.removeAllOccurrences(k1));
        TestUtils.assertListEquals(deque.toArray(), 2);
        Assert.assertEquals(1, deque.removeAllOccurrences(k2));
        Assert.assertEquals(0, deque.size());
    }

    /* */
    @Test
    public void testRemoveAllInLookupContainer()
    {
        deque.addLast(asArray(0, 1, 2, 1, 0));

        final KTypeOpenHashSet<KType> set = KTypeOpenHashSet.newInstance();
        set.add(asArray(0, 2));

        Assert.assertEquals(3, deque.removeAll(set));
        Assert.assertEquals(0, deque.removeAll(set));

        TestUtils.assertListEquals(deque.toArray(), 1, 1);
    }

    /* */
    @Test
    public void testRemoveAllWithPredicate()
    {
        deque.addLast(newArray(k0, k1, k2, k1, k4));

        Assert.assertEquals(3, deque.removeAll(new KTypePredicate<KType>()
        {
            @Override
            public boolean apply(final KType v)
            {
                return v == key1 || v == key2;
            };
        }));

        TestUtils.assertListEquals(deque.toArray(), 0, 4);
    }

    /* */
    @Test
    public void testRemoveAllWithPredicateInterrupted()
    {
        deque.addLast(newArray(k0, k1, k2, k1, k4));

        final RuntimeException t = new RuntimeException();

        try
        {
            //the assert below should never be triggered because of the exception
            //so give it an invalid value in case the thing terminates  = initial size
            Assert.assertEquals(5, deque.removeAll(new KTypePredicate<KType>()
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

        // And check if the deque is in consistent state.
        TestUtils.assertListEquals(deque.toArray(), 0, key2, key1, 4);
        Assert.assertEquals(4, deque.size());
    }

    /* */
    @Test
    public void testClear()
    {
        deque.addLast(k1);
        deque.addLast(k2);
        deque.addFirst(k3);
        deque.clear();
        Assert.assertEquals(0, deque.size());
        Assert.assertEquals(0, deque.head);
        Assert.assertEquals(0, deque.tail);

        deque.addLast(k1);
        TestUtils.assertListEquals(deque.toArray(), 1);
    }

    /* */
    @Test
    public void testRelease()
    {
        deque.addLast(k1);
        deque.addLast(k2);
        deque.addFirst(k3);
        deque.release();
        Assert.assertEquals(0, deque.size());
        Assert.assertEquals(0, deque.head);
        Assert.assertEquals(0, deque.tail);

        deque.addLast(k1);
        TestUtils.assertListEquals(deque.toArray(), 1);
    }

    /* */
    @Test
    public void testIterable()
    {
        deque.addLast(sequence);

        int count = 0;
        for (final KTypeCursor<KType> cursor : deque)
        {
            TestUtils.assertEquals2(sequence.buffer[count], cursor.value);
            TestUtils.assertEquals2(deque.buffer[cursor.index], cursor.value);
            count++;
        }
        Assert.assertEquals(count, deque.size());
        Assert.assertEquals(count, sequence.size());

        count = 0;
        deque.clear();
        for (@SuppressWarnings("unused")
        final KTypeCursor<KType> cursor : deque)
        {
            count++;
        }
        Assert.assertEquals(0, count);
    }

    /* */
    @Test
    public void testIterator()
    {
        deque.addLast(asArray(0, 1, 2, 3));

        final Iterator<KTypeCursor<KType>> iterator = deque.iterator();
        int count = 0;
        while (iterator.hasNext())
        {
            iterator.hasNext();
            iterator.hasNext();
            iterator.hasNext();
            iterator.next();
            count++;
        }
        Assert.assertEquals(count, deque.size());

        deque.clear();
        Assert.assertFalse(deque.iterator().hasNext());
    }

    /* */
    @Test
    public void testDescendingIterator()
    {
        deque.addLast(sequence);

        int index = sequence.size() - 1;
        for (final Iterator<KTypeCursor<KType>> i = deque.descendingIterator(); i.hasNext();)
        {
            final KTypeCursor<KType> cursor = i.next();
            TestUtils.assertEquals2(sequence.buffer[index], cursor.value);
            TestUtils.assertEquals2(deque.buffer[cursor.index], cursor.value);
            index--;
        }
        Assert.assertEquals(-1, index);

        deque.clear();
        Assert.assertFalse(deque.descendingIterator().hasNext());
    }

    /* */
    @Test
    public void testForEachWithProcedure()
    {
        deque.addLast(sequence);

        final IntHolder count = new IntHolder();
        deque.forEach(new KTypeProcedure<KType>() {
            int index = 0;

            @Override
            public void apply(final KType v)
            {
                TestUtils.assertEquals2(sequence.buffer[index++], v);
                count.value++;
            }
        });
        Assert.assertEquals(count.value, deque.size());
    }

    /* */
    @Test
    public void testDescendingForEachWithProcedure()
    {
        deque.addLast(sequence);

        final IntHolder count = new IntHolder();
        deque.descendingForEach(new KTypeProcedure<KType>() {
            int index = sequence.size();

            @Override
            public void apply(final KType v)
            {
                TestUtils.assertEquals2(sequence.buffer[--index], v);
                count.value++;
            }
        });
        Assert.assertEquals(count.value, deque.size());
    }

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
                deque.addFirst(k);
                ad.addFirst(k);
            }
            else if (op < 4)
            {
                deque.addLast(k);
                ad.addLast(k);
            }
            else if (op < 5 && ad.size() > 0)
            {
                deque.removeLast();
                ad.removeLast();
            }
            else if (op < 6 && ad.size() > 0)
            {
                deque.removeLast();
                ad.removeLast();
            }
            else if (op < 7)
            {
                Assert.assertEquals(
                        ad.removeFirstOccurrence(k),
                        deque.removeFirstOccurrence(k) >= 0);
            }
            else if (op < 8)
            {
                Assert.assertEquals(
                        ad.removeLastOccurrence(k),
                        deque.removeLastOccurrence(k) >= 0);
            }
            Assert.assertEquals(ad.size(), deque.size());
        }

        Assert.assertArrayEquals(ad.toArray(), deque.toArray());
    }

    /*! #end !*/

    /*! #if ($TemplateOptions.KTypeGeneric) !*/
    @Test
    public void testAgainstArrayDequeVariousTailHeadPositions()
    {
        this.deque.clear();
        this.deque.head = this.deque.tail = 2;
        testAgainstArrayDeque();

        this.deque.clear();
        this.deque.head = this.deque.tail = this.deque.buffer.length - 2;
        testAgainstArrayDeque();

        this.deque.clear();
        this.deque.head = this.deque.tail = this.deque.buffer.length / 2;
        testAgainstArrayDeque();
    }

    /*! #end !*/

    /*! #if ($TemplateOptions.KTypeGeneric) !*/
    @SuppressWarnings("unchecked")
    /*! #end !*/
    @Test
    public void testHashCodeEquals()
    {
        final KTypeArrayDeque<KType> l0 = KTypeArrayDeque.newInstance();
        Assert.assertEquals(1, l0.hashCode());
        Assert.assertEquals(l0, KTypeArrayDeque.from());

        final KTypeArrayDeque<KType> l1 = KTypeArrayDeque.from(k1, k2, k3);
        final KTypeArrayDeque<KType> l2 = KTypeArrayDeque.from(k1, k2);
        l2.addLast(k3);

        Assert.assertEquals(l1.hashCode(), l2.hashCode());
        Assert.assertEquals(l1, l2);
    }

    /*! #if ($TemplateOptions.KTypeGeneric) !*/
    @SuppressWarnings("unchecked")
    @Test
    public void testHashCodeWithNulls()
    {
        final KTypeArrayDeque<KType> l1 = KTypeArrayDeque.from(k1, null, k3);
        final KTypeArrayDeque<KType> l2 = KTypeArrayDeque.from(k1, null, k3);
        Assert.assertEquals(l1.hashCode(), l2.hashCode());
        Assert.assertEquals(l1, l2);
    }

    /*! #end !*/

    /*! #if ($TemplateOptions.KTypeGeneric) !*/
    @Test
    public void testToArrayWithClass()
    {
        final KTypeArrayDeque<Integer> l1 = KTypeArrayDeque.from(1, 2, 3);
        final Integer[] result = l1.toArray(Integer.class);
        Assert.assertArrayEquals(new Integer[] { 1, 2, 3 }, result); // dummy
    }

    /*! #end !*/

    /*! #if ($TemplateOptions.KTypeGeneric) !*/
    @SuppressWarnings("unchecked")
    @Test
    public void testToArray()
    {
        final KTypeArrayDeque<KType> l1 = KTypeArrayDeque.from(k1, k2, k3);
        final Object[] result = l1.toArray();
        Assert.assertArrayEquals(new Object[] { k1, k2, k3 }, result); // dummy
    }

    /*! #end !*/

    /*! #if ($TemplateOptions.KTypeGeneric) !*/
    @SuppressWarnings("unchecked")
    /*! #end !*/
    @Test
    public void testClone()
    {
        this.deque.addLast(key1, key2, key3);

        final KTypeArrayDeque<KType> cloned = deque.clone();
        cloned.removeAllOccurrences(key1);

        TestUtils.assertSortedListEquals(deque.toArray(), key1, key2, key3);
        TestUtils.assertSortedListEquals(cloned.toArray(), key2, key3);
    }

    /*! #if ($TemplateOptions.KTypeGeneric) !*/
    @SuppressWarnings("unchecked")
    /*! #end !*/
    @Test
    public void testToString()
    {
        Assert.assertEquals("["
                + key1 + ", "
                + key2 + ", "
                + key3 + "]", KTypeArrayDeque.from(key1, key2, key3).toString());
    }

    @Test
    public void testPooledIteratorForEach()
    {
        // Unbroken for-each loop
        final int TEST_SIZE = 10000;
        final long TEST_ROUNDS = 100;

        final KTypeArrayDeque<KType> testContainer = createDequeWithOrderedData(TEST_SIZE);

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
        final int TEST_SIZE = 10000;
        final long TEST_ROUNDS = 100;

        final KTypeArrayDeque<KType> testContainer = createDequeWithOrderedData(TEST_SIZE);

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
        final int TEST_SIZE = 10000;
        final long TEST_ROUNDS = 100;

        final KTypeArrayDeque<KType> testContainer = createDequeWithOrderedData(TEST_SIZE);

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
        final int startingPoolDescendingSize = testContainer.descendingValueIteratorPool.size();

        for (int round = 0; round < TEST_ROUNDS; round++)
        {
            //A) Classical iterator loop, with manually allocated Iterator
            int initialPoolSize = testContainer.valueIteratorPool.size();

            Iterator<KTypeCursor<KType>> loopIterator = testContainer.iterator();

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

            //B) Descending iterator loop
            initialPoolSize = testContainer.descendingValueIteratorPool.size();

            loopIterator = testContainer.descendingIterator();

            Assert.assertEquals(initialPoolSize - 1, testContainer.descendingValueIteratorPool.size());

            testValue = 0;
            while (loopIterator.hasNext())
            {
                testValue += castType(loopIterator.next().value);
            } //end IteratorLoop

            //iterator is returned automatically to its pool, by normal iteration termination
            Assert.assertEquals(initialPoolSize, testContainer.descendingValueIteratorPool.size());

            //checksum
            Assert.assertEquals(checksum, testValue);

        } //end for rounds

        // pool initial size is untouched anyway
        Assert.assertEquals(startingPoolSize, testContainer.valueIteratorPool.size());
        Assert.assertEquals(startingPoolDescendingSize, testContainer.descendingValueIteratorPool.size());
    }

    @Test
    public void testPooledIteratorBrokenIteratorLoop()
    {
        final int TEST_SIZE = 10000;
        final long TEST_ROUNDS = 100;

        final KTypeArrayDeque<KType> testContainer = createDequeWithOrderedData(TEST_SIZE);
        final int startingPoolSize = testContainer.valueIteratorPool.size();
        final int startingPoolDescendingSize = testContainer.descendingValueIteratorPool.size();

        int count = 0;
        for (int round = 0; round < TEST_ROUNDS; round++)
        {
            //A) Classical iterator loop, with manually allocated Iterator, forward
            long initialPoolSize = testContainer.valueIteratorPool.size();

            Iterator<KTypeCursor<KType>> loopIterator = testContainer.iterator();

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
            ((KTypeArrayDeque<KType>.ValueIterator) loopIterator).release();

            //now the pool is restored
            Assert.assertEquals(initialPoolSize, testContainer.valueIteratorPool.size());

            //B) Descending iteration
            initialPoolSize = testContainer.descendingValueIteratorPool.size();

            loopIterator = testContainer.descendingIterator();

            Assert.assertEquals(initialPoolSize - 1, testContainer.descendingValueIteratorPool.size());

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
            Assert.assertEquals(initialPoolSize - 1, testContainer.descendingValueIteratorPool.size());

            //manual return to the pool
            ((KTypeArrayDeque<KType>.DescendingValueIterator) loopIterator).release();

            //now the pool is restored
            Assert.assertEquals(initialPoolSize, testContainer.descendingValueIteratorPool.size());

        } //end for rounds

        // pool initial size is untouched anyway
        Assert.assertEquals(startingPoolSize, testContainer.valueIteratorPool.size());
        Assert.assertEquals(startingPoolDescendingSize, testContainer.descendingValueIteratorPool.size());
    }

    @Test
    public void testPooledIteratorExceptionIteratorLoop()
    {
        final int TEST_SIZE = 10000;
        final long TEST_ROUNDS = 100;

        final KTypeArrayDeque<KType> testContainer = createDequeWithOrderedData(TEST_SIZE);

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
        Iterator<KTypeCursor<KType>> loopIterator = null;

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
                ((KTypeArrayDeque<KType>.ValueIterator) loopIterator).release();

                //now the pool is restored
                Assert.assertEquals(startingPoolSize, testContainer.valueIteratorPool.size());
            }
        } //end for rounds

        // pool initial size is untouched anyway
        Assert.assertEquals(startingPoolSize, testContainer.valueIteratorPool.size());
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
        //A) Sort a deque of random values of primitive types

        /*! #if ($TemplateOptions.KTypePrimitive)
        //A-1) full sort
        KTypeArrayDeque<KType> primitiveDeque = creatDequeWithRandomData(TEST_SIZE, 7882316546154612L);
        primitiveDeque.sort();
        assertOrder(primitiveDeque);
        #end !*/

        //B) Sort with Comparator
        //B-1) Full sort
        final KTypeArrayDeque<KType> comparatorDeque = creatDequeWithRandomData(TEST_SIZE, 8784163166131549L);
        comparatorDeque.sort(comp);
        assertOrder(comparatorDeque);
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
            final KTypeArrayDeque<KType> newDequeue = KTypeArrayDeque.newInstanceWithCapacity(PREALLOCATED_SIZE);

            //3) Add PREALLOCATED_SIZE different values. At the end, size() must be == PREALLOCATED_SIZE,
            //and internal buffer/allocated must not have changed of size
            final int contructorBufferSize = newDequeue.buffer.length;

            for (int i = 0; i < PREALLOCATED_SIZE; i++)
            {
                newDequeue.addLast(cast(randomVK.nextInt()));

                //internal size has not changed.
                Assert.assertEquals(contructorBufferSize, newDequeue.buffer.length);
            }

            Assert.assertEquals(PREALLOCATED_SIZE, newDequeue.size());
        } //end for test runs
    }

    /**
     * Test natural ordering in the deque
     * @param expected
     * @param actual
     * @param length
     */
    private void assertOrder(final KTypeArrayDeque<KType> order)
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

    private KTypeArrayDeque<KType> createDequeWithOrderedData(final int size)
    {
        final KTypeArrayDeque<KType> newArray = KTypeArrayDeque.newInstanceWithCapacity(KTypeArrayList.DEFAULT_CAPACITY);

        for (int i = 0; i < size; i++)
        {
            newArray.addLast(cast(i));
        }

        return newArray;
    }

    private KTypeArrayDeque<KType> creatDequeWithRandomData(final int size, final long randomSeed)
    {
        final Random prng = new Random(randomSeed);

        final KTypeArrayDeque<KType> newDeque = KTypeArrayDeque.newInstanceWithCapacity(KTypeArrayList.DEFAULT_CAPACITY);

        while (newDeque.size() < size)
        {
            final KType newValueToInsert = cast(prng.nextInt());
            final boolean insertInTail = prng.nextInt() % 7 == 0;
            final boolean deleteHead = prng.nextInt() % 17 == 0;

            if (deleteHead)
            {
                newDeque.removeFirst();
            }
            else if (insertInTail)
            {
                newDeque.addLast(newValueToInsert);
            }
            else
            {
                newDeque.addFirst(newValueToInsert);
            }
        }

        return newDeque;
    }

}
