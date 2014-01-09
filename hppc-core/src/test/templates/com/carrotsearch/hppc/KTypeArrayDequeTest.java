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
    public void checkTrailingSpaceUninitialized()
    {
        if (deque != null)
        {
            for (int i = deque.tail; i < deque.head; i = Intrinsics.oneRight(i, deque.buffer.length))
            {
                /*! #if ($TemplateOptions.KTypeGeneric) !*/
                Assert.assertTrue(Intrinsics.<KType>defaultKTypeValue() == deque.buffer[i]);
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
    public void testRemoveFirstOccurrence()
    {
        final int modulo = 10;
        final int count = 10000;
        sequence.clear();
        for (int i = 0; i < count; i++)
        {
            deque.addLast(cast(i % modulo));
            sequence.add(cast(i % modulo));
        }

        final Random rnd = new Random(0xdeadbeef);
        for (int i = 0; i < 500; i++)
        {
            final KType k = cast(rnd.nextInt(modulo));
            Assert.assertEquals(
                    deque.removeFirstOccurrence(k) >= 0,
                    sequence.removeFirstOccurrence(k) >= 0);
        }

        TestUtils.assertListEquals(deque.toArray(), sequence.toArray());

        Assert.assertTrue(0 > deque.removeFirstOccurrence(cast(modulo + 1)));
        deque.addLast(cast(modulo + 1));
        Assert.assertTrue(0 <= deque.removeFirstOccurrence(cast(modulo + 1)));
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
            deque.addLast(cast(i % modulo));
            sequence.add(cast(i % modulo));
        }

        final Random rnd = new Random(0x11223344);
        for (int i = 0; i < 500; i++)
        {
            final KType k = cast(rnd.nextInt(modulo));
            Assert.assertEquals(
                    deque.removeLastOccurrence(k) >= 0,
                    sequence.removeLastOccurrence(k) >= 0);
        }

        TestUtils.assertListEquals(deque.toArray(), sequence.toArray());

        Assert.assertTrue(0 > deque.removeLastOccurrence(cast(modulo + 1)));
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
            Assert.assertEquals(3, deque.removeAll(new KTypePredicate<KType>()
                    {
                @Override
                public boolean apply(final KType v)
                {
                    if (v == key2) throw t;
                    return v == key1;
                };
                    }));
            Assert.fail();
        }
        catch (final RuntimeException e)
        {
            // Make sure it's really our exception...
            if (e != t) throw e;
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
        for (@SuppressWarnings("unused") final KTypeCursor<KType> cursor : deque)
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
        for (final Iterator<KTypeCursor<KType>> i = deque.descendingIterator(); i.hasNext(); )
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
        Assert.assertArrayEquals(new Integer [] {1, 2, 3}, result); // dummy
    }
    /*! #end !*/

    /*! #if ($TemplateOptions.KTypeGeneric) !*/
    @SuppressWarnings("unchecked")
    @Test
    public void testToArray()
    {
        final KTypeArrayDeque<KType> l1 = KTypeArrayDeque.from(k1, k2, k3);
        final Object[] result = l1.toArray();
        Assert.assertArrayEquals(new Object [] {k1, k2, k3}, result); // dummy
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

            } catch (final Exception e)
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
