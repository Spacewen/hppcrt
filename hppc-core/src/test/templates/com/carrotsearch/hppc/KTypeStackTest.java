package com.carrotsearch.hppc;

import static com.carrotsearch.hppc.TestUtils.*;
import static org.junit.Assert.*;

import java.util.Random;

import org.junit.*;
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
        TestUtils.assertEquals2(key1, stack.get(0));
        TestUtils.assertEquals2(key3, stack.get(1));
        TestUtils.assertEquals2(key3, stack.pop());
        TestUtils.assertEquals2(key1, stack.pop());
        Assert.assertEquals(0, stack.size());
    }

    /* */
    @Test
    public void testPushArray()
    {
        stack.push(asArray(1, 2, 3, 4), 1, 2);
        Assert.assertEquals(2, stack.size());
        TestUtils.assertEquals2(key2, stack.get(0));
        TestUtils.assertEquals2(key3, stack.get(1));
    }

    /* */
    @Test
    public void testAddAllPushAll()
    {
        final KTypeArrayList<KType> list2 = KTypeArrayList.newInstance();
        list2.add(asArray(0, 1, 2));

        stack.addAll(list2);
        stack.pushAll(list2);

        TestUtils.assertListEquals(stack.toArray(), 0, 1, 2, 0, 1, 2);
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

        TestUtils.assertEquals2(1, stack.get(0));
        TestUtils.assertEquals2(4, stack.get(1));

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

    /* */
    /*! #if ($TemplateOptions.KTypeGeneric) !*/
    @SuppressWarnings("unchecked")
    /*! #end !*/
    @Test
    public void testHashCodeEqualsWithOtherContainer()
    {
        final KTypeStack<KType> s1 = KTypeStack.from(key1, key2, key3);
        final KTypeArrayList<KType> s2 = KTypeArrayList.from(key1, key2, key3);

        Assert.assertEquals(s1.hashCode(), s2.hashCode());
        Assert.assertEquals(s1, s2);
    }

    /*! #if ($TemplateOptions.KTypeGeneric) !*/
    @Test
    public void testToArrayWithClass()
    {
        final KTypeStack<Integer> l1 = KTypeStack.from(1, 2, 3);
        final Integer[] result = l1.toArray(Integer.class);
        Assert.assertArrayEquals(new Integer [] {1, 2, 3}, result); // dummy
    }
    /*! #end !*/

    /*! #if ($TemplateOptions.KTypeGeneric) !*/
    @Test
    public void testToArray()
    {
        final KTypeStack<Integer> l1 = KTypeStack.from(1, 2, 3);
        final Object[] result = l1.toArray();
        Assert.assertArrayEquals(new Object [] {1, 2, 3}, result); // dummy
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
                + key1 + ", "
                + key2 + ", "
                + key3 + "]", KTypeStack.from(key1, key2, key3).toString());
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

}
