package com.carrotsearch.hppc;

import static com.carrotsearch.hppc.TestUtils.*;
import static org.junit.Assert.*;

import java.util.Random;

import org.junit.*;

import com.carrotsearch.hppc.sorting.KTypeComparator;
/**
 * Unit tests for {@link KTypeStack}.
 */
//${TemplateOptions.doNotGenerateKType("BOOLEAN")}
/*! ${TemplateOptions.generatedAnnotation} !*/
public class KTypeStackTest<KType> extends AbstractKTypeTest<KType>
{
    /**
     * Per-test fresh initialized instance.
     */
    public KTypeStack<KType> stack;

    @BeforeClass
    public static void configure()
    {
        IteratorPool.configureInitialPoolSize(8);
    }

    /* */
    @Before
    public void initialize()
    {
        stack = KTypeStack.newInstance();
    }

    /* */
    @Test
    public void testInitiallyEmpty()
    {
        Assert.assertEquals(0, stack.size());
    }

    /* */
    @Test
    public void testPush1()
    {
        stack.push(key1);
        Assert.assertEquals(1, stack.size());
        TestUtils.assertEquals2(key1, stack.peek());
        TestUtils.assertEquals2(key1, stack.pop());
        Assert.assertEquals(0, stack.size());
    }

    /* */
    @Test
    public void testPush2()
    {
        stack.push(key1, key3);
        Assert.assertEquals(2, stack.size());
        TestUtils.assertEquals2(key3, stack.peek());
        TestUtils.assertEquals2(key3, stack.get(0));
        TestUtils.assertEquals2(key1, stack.get(1));
        TestUtils.assertEquals2(key3, stack.pop());
        TestUtils.assertEquals2(key1, stack.pop());
        Assert.assertEquals(0, stack.size());
    }

    /* */
    @Test
    public void testPushArray()
    {
        stack.push(asArray(1, 2, 3, 4, 5), 1, 3);
        Assert.assertEquals(3, stack.size());
        TestUtils.assertEquals2(key4, stack.get(0));
        TestUtils.assertEquals2(key3, stack.get(1));
        TestUtils.assertEquals2(key2, stack.get(2));
    }

    /* */
    @Test
    public void testAddAllPushAll()
    {
        final KTypeArrayList<KType> list2 = KTypeArrayList.newInstance();
        list2.add(asArray(0, 1, 2, 3, 4, 5));

        stack.addAll(list2);
        stack.pushAll(list2);

        TestUtils.assertListEquals(stack.toArray(), 5, 4, 3, 2, 1, 0, 5, 4, 3, 2, 1, 0);
    }

    /*! #if ($TemplateOptions.KTypeGeneric) !*/
    /* */
    @Test
    public void testNullify()
    {
        stack.push(asArray(1, 2, 3, 4));
        stack.pop();
        stack.discard();
        stack.discard(2);
        Assert.assertEquals(0, stack.size());

        /*
         * Cleanup only for the generic version (to allow GCing of references).
         */
        for (int i = 0; i < stack.buffer.length; i++)
        {
            TestUtils.assertEquals2(Intrinsics.defaultKTypeValue(), stack.buffer[i]);
        }
    }
    /*! #end !*/

    /* */
    @Test
    public void testDiscard()
    {
        stack.push(key1, key3);
        Assert.assertEquals(2, stack.size());

        stack.discard();
        Assert.assertEquals(1, stack.size());

        stack.push(key4);
        Assert.assertEquals(2, stack.size());

        TestUtils.assertEquals2(4, stack.get(0));
        TestUtils.assertEquals2(1, stack.get(1));

        stack.discard(2);
        Assert.assertEquals(0, stack.size());
    }

    /* */
    @Test(expected = AssertionError.class)
    public void testGetAssertions()
    {
        stack.push(key1);
        stack.pop();
        stack.get(0);
    }

    /* */
    @Test(expected = AssertionError.class)
    public void testDiscardAssertions()
    {
        stack.push(key1);
        stack.discard(2);
    }

    /* */
    /*! #if ($TemplateOptions.KTypeGeneric) !*/
    @SuppressWarnings("unchecked")
    /*! #end !*/
    @Test
    public void testHashCodeEquals()
    {
        final KTypeStack<KType> s0 = KTypeStack.newInstance();
        Assert.assertEquals(1, s0.hashCode());
        Assert.assertEquals(s0, KTypeArrayList.newInstance());

        KTypeStack<KType> s1 = KTypeStack.from(key1, key2, key3);
        final KTypeStack<KType> s2 = KTypeStack.from(key1, key2, key3);

        Assert.assertEquals(s1.hashCode(), s2.hashCode());
        Assert.assertEquals(s1, s2);

        s1 = KTypeStack.from(key1, key2);
        Assert.assertFalse(s1.equals(s2));
    }

    /*! #if ($TemplateOptions.KTypeGeneric) !*/
    @Test
    public void testToArrayWithClass()
    {
        final KTypeStack<Integer> l1 = KTypeStack.from(1, 2, 3);
        final Integer[] result = l1.toArray(Integer.class);
        Assert.assertArrayEquals(new Integer[] { 3, 2, 1 }, result); // dummy
    }
    /*! #end !*/

    /*! #if ($TemplateOptions.KTypeGeneric) !*/
    @Test
    public void testToArray()
    {
        final KTypeStack<Integer> l1 = KTypeStack.from(1, 2, 3);
        final Object[] result = l1.toArray();
        Assert.assertArrayEquals(new Object[] { 3, 2, 1 }, result); // dummy
    }
    /*! #end !*/

    /* */
    @Test
    public void testClone()
    {
        stack.push(key1, key2, key3);

        final KTypeStack<KType> cloned = stack.clone();
        cloned.removeAllOccurrences(key1);

        TestUtils.assertSortedListEquals(stack.toArray(), key1, key2, key3);
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
                + key3 + ", "
                + key2 + ", "
                + key1 + "]", KTypeStack.from(key1, key2, key3).toString());
    }

    @Test
    public void testPreallocatedSize()
    {
        final Random randomVK = new Random(96321587L);
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
            final KTypeStack<KType> newStack = KTypeStack.newInstanceWithCapacity(PREALLOCATED_SIZE);

            //3) Add PREALLOCATED_SIZE different values. At the end, size() must be == PREALLOCATED_SIZE,
            //and internal buffer/allocated must not have changed of size
            final int contructorBufferSize = newStack.buffer.length;

            for (int i = 0; i < PREALLOCATED_SIZE; i++)
            {

                newStack.add(cast(randomVK.nextInt()));

                //internal size has not changed.
                Assert.assertEquals(contructorBufferSize, newStack.buffer.length);
            }

            Assert.assertEquals(PREALLOCATED_SIZE, newStack.size());
        } //end for test runs
    }

    /////////////////////
    // Overridden list methods
    ////////////////////////
    @Test
    public void testInsert()
    {
        stack.insert(0, k1);
        stack.insert(0, k2);
        stack.insert(2, k3);
        stack.insert(1, k4);
        stack.insert(1, k6);
        stack.insert(3, k7);
        stack.insert(6, k8);

        TestUtils.assertListEquals(stack.toArray(), 2, 6, 4, 7, 1, 3, 8);

        Assert.assertEquals(castType(k2), castType(stack.get(0)));
        Assert.assertEquals(castType(k6), castType(stack.get(1)));
        Assert.assertEquals(castType(k4), castType(stack.get(2)));
        Assert.assertEquals(castType(k7), castType(stack.get(3)));
        Assert.assertEquals(castType(k1), castType(stack.get(4)));
        Assert.assertEquals(castType(k3), castType(stack.get(5)));
        Assert.assertEquals(castType(k8), castType(stack.get(6)));


    }

    /* */
    @Test
    public void testIndexOf()
    {
        stack.add(asArray(0, 1, 2, 1, 0, 5));

        /*! #if ($TemplateOptions.KTypeGeneric) !*/
        stack.add((KType) null);
        Assert.assertEquals(0, stack.indexOf(null));

        Assert.assertEquals(2, stack.indexOf(k0));
        Assert.assertEquals(-1, stack.indexOf(k3));
        Assert.assertEquals(4, stack.indexOf(k2));

        /*! #else
        Assert.assertEquals(1, stack.indexOf(k0));
        Assert.assertEquals(-1, stack.indexOf(k3));
        Assert.assertEquals(3, stack.indexOf(k2));
        #end !*/
    }

    /* */
    @Test
    public void testLastIndexOf()
    {
        stack.add(asArray(0, 1, 2, 3, 0, 1, 2, 3));

        /*! #if ($TemplateOptions.KTypeGeneric) !*/
        stack.add((KType) null);
        Assert.assertEquals(0, stack.lastIndexOf(null));

        TestUtils.assertEquals2(8, stack.lastIndexOf(k0));
        TestUtils.assertEquals2(-1, stack.lastIndexOf(k7));
        TestUtils.assertEquals2(6, stack.lastIndexOf(k2));

        /*! #else
        TestUtils.assertEquals2(7, stack.lastIndexOf(k0));
        TestUtils.assertEquals2(-1, stack.lastIndexOf(k7));
        TestUtils.assertEquals2(5, stack.lastIndexOf(k2));
        #end !*/

    }

    /* */
    @Test
    public void testSet()
    {
        stack.add(asArray(0, 1, 2, 3, 4));

        TestUtils.assertEquals2(4, stack.set(0, k3));
        TestUtils.assertEquals2(3, stack.set(1, k4));
        TestUtils.assertEquals2(2, stack.set(2, k5));

        //set again
        TestUtils.assertEquals2(3, stack.set(0, k7));
        TestUtils.assertEquals2(4, stack.set(1, k6));
        TestUtils.assertEquals2(5, stack.set(2, k5));

        TestUtils.assertListEquals(stack.toArray(), 7, 6, 5, 1, 0);
    }

    /* */
    @Test
    public void testRemoveFirstLast()
    {
        stack.add(asArray(0, 1, 2, 3, 4, 0, 1, 2, 3, 4));

        Assert.assertEquals(-1, stack.removeFirstOccurrence(k5));
        Assert.assertEquals(-1, stack.removeLastOccurrence(k5));
        TestUtils.assertListEquals(stack.toArray(), 4, 3, 2, 1, 0, 4, 3, 2, 1, 0);

        Assert.assertEquals(3, stack.removeFirstOccurrence(k1));
        TestUtils.assertListEquals(stack.toArray(), 4, 3, 2, 0, 4, 3, 2, 1, 0);
        Assert.assertEquals(8, stack.removeLastOccurrence(k0));
        TestUtils.assertListEquals(stack.toArray(), 4, 3, 2, 0, 4, 3, 2, 1);
        Assert.assertEquals(6, stack.removeLastOccurrence(k2));
        TestUtils.assertListEquals(stack.toArray(), 4, 3, 2, 0, 4, 3, 1);
        Assert.assertEquals(1, stack.removeFirstOccurrence(k3));
        TestUtils.assertListEquals(stack.toArray(), 4, 2, 0, 4, 3, 1);

        Assert.assertEquals(2, stack.removeLastOccurrence(k0));
        Assert.assertEquals(-1, stack.removeLastOccurrence(k0));
        TestUtils.assertListEquals(stack.toArray(), 4, 2, 4, 3, 1);

        /*! #if ($TemplateOptions.KTypeGeneric) !*/
        stack.clear();
        stack.add(newArray(k0, null, k2, null, k0));
        Assert.assertEquals(1, stack.removeFirstOccurrence(null));
        Assert.assertEquals(2, stack.removeLastOccurrence(null));
        TestUtils.assertListEquals(stack.toArray(), 0, 2, 0);
        /*! #end !*/
    }

    /* */
    @Test
    public void testRemove()
    {
        stack.add(asArray(0, 1, 2, 3, 4, 5, 6));

        stack.remove(0);
        stack.remove(2);
        stack.remove(1);

        TestUtils.assertListEquals(stack.toArray(), 5, 2, 1, 0);
    }

    /* */
    @Test
    public void testRemoveRange()
    {
        stack.add(asArray(0, 1, 2, 3, 4, 5, 6, 7, 8));

        stack.removeRange(0, 2);
        TestUtils.assertListEquals(stack.toArray(), 6, 5, 4, 3, 2, 1, 0);

        stack.removeRange(2, 3);
        TestUtils.assertListEquals(stack.toArray(), 6, 5, 3, 2, 1, 0);

        stack.removeRange(1, 1);
        TestUtils.assertListEquals(stack.toArray(), 6, 5, 3, 2, 1, 0);

        stack.removeRange(0, 1);
        TestUtils.assertListEquals(stack.toArray(), 5, 3, 2, 1, 0);
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

        final int TEST_SIZE = (int) 1e6 - 1;
        //A) Sort an array of random values of primitive types

        /*! #if ($TemplateOptions.KTypePrimitive)
        //A-1) full sort
        KTypeStack<KType> primitiveStack = createStackWithRandomData(TEST_SIZE, 8741631654L);
        primitiveStack.sort();
        assertOrder(primitiveStack, 0, primitiveStack.size());
        //A-2) Partial sort
        primitiveStack = createStackWithRandomData(TEST_SIZE, 1114478824455L);
        primitiveStack.sort(11478,448745);
        assertOrder(primitiveStack, 11478,448745);
        #end !*/

        //B) Sort with Comparator
        //B-1) Full sort
        KTypeStack<KType> comparatorStack = createStackWithRandomData(TEST_SIZE, 1215443161L);
        comparatorStack.sort(comp);
        assertOrder(comparatorStack, 0, comparatorStack.size());
        //B-2) Partial sort
        comparatorStack = createStackWithRandomData(TEST_SIZE, 87771125444L);
        comparatorStack.sort(53781, 818741, comp);
        assertOrder(comparatorStack, 53781, 818741);
    }

    /**
     * Test natural ordering between [startIndex; endIndex[
     * @param expected
     * @param actual
     * @param length
     */
    private void assertOrder(final KTypeStack<KType> order, final int startIndex, final int endIndex)
    {
        for (int i = startIndex + 1; i < endIndex; i++)
        {
            if (castType(order.get(i - 1)) > castType(order.get(i)))
            {
                Assert.assertTrue(String.format("Not ordered: (previous, next) = (%d, %d) at index %d",
                        castType(order.get(i - 1)), castType(order.get(i)), i), false);
            }
        }
    }

    private KTypeStack<KType> createStackWithRandomData(final int size, final long randomSeed)
    {
        final Random prng = new Random(randomSeed);

        final KTypeStack<KType> newArray = KTypeStack.newInstanceWithCapacity(KTypeArrayList.DEFAULT_CAPACITY);

        for (int i = 0; i < size; i++)
        {
            newArray.add(cast(prng.nextInt(size)));
        }

        return newArray;
    }

}
