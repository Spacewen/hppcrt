package com.carrotsearch.hppc;

import static com.carrotsearch.hppc.TestUtils.*;
import static org.junit.Assert.*;

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
}
