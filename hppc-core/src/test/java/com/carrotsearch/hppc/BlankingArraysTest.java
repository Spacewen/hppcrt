package com.carrotsearch.hppc;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;

public class BlankingArraysTest
{
    //this is a prime
    private static final int COUNT_PRIME = (int)1020379L;

    @Test
    public void testArrayOfObjects()
    {
        Integer[] testArray = null;

        //a) Blank all a prime
        testArray = buildArray(COUNT_PRIME);

        Internals.blankObjectArray(testArray, 0, testArray.length);

        //Check
        checkIntegerArray(testArray, 0, testArray.length);

        //b) Blank all a 1024 multiple
        testArray = buildArray(1024 * 991);
        Internals.blankObjectArray(testArray, 0, testArray.length);

        //Check
        checkIntegerArray(testArray, 0, testArray.length);

        //c) Blank a void array
        testArray = buildArray(0);
        Internals.blankObjectArray(testArray, 0, testArray.length);

        //c-1) Blank an array < 1024
        testArray = buildArray(991);
        Internals.blankObjectArray(testArray, 0, testArray.length);

        //d) Blank a null range (no change)
        testArray = buildArray(1136287);

        Integer[] referenceArray =  buildArray(1136287);

        //d-1)
        Internals.blankObjectArray(testArray, 0, 0);
        //Check identity
        assertArrayEquals(referenceArray, testArray);

        //d-2)
        Internals.blankObjectArray(testArray, 1024, 1024);
        //Check identity
        assertArrayEquals(referenceArray, testArray);

        //d-3)
        Internals.blankObjectArray(testArray, 2*1024, 2*1024);
        //Check identity
        assertArrayEquals(referenceArray, testArray);

        //d-4)
        Internals.blankObjectArray(testArray, 11*1024, 11*1024);
        //Check identity
        assertArrayEquals(referenceArray, testArray);

        //d-5)
        Internals.blankObjectArray(testArray, 1, 1);
        //Check identity
        assertArrayEquals(referenceArray, testArray);

        //d-6)
        Internals.blankObjectArray(testArray, 3, 3);
        //Check identity
        assertArrayEquals(referenceArray, testArray);

        //d-7)
        Internals.blankObjectArray(testArray, 2027, 2027);
        //Check identity
        assertArrayEquals(referenceArray, testArray);

        //d-8)
        Internals.blankObjectArray(testArray, 2609, 2609);
        //Check identity
        assertArrayEquals(referenceArray, testArray);


        //d-9)
        Internals.blankObjectArray(testArray, 99529, 99529);
        //Check identity
        assertArrayEquals(referenceArray, testArray);

        //d-10)
        Internals.blankObjectArray(testArray, testArray.length - 1, testArray.length - 1);
        //Check identity
        assertArrayEquals(referenceArray, testArray);

        //d-11)
        Internals.blankObjectArray(testArray, testArray.length - 2, testArray.length - 2);
        //Check identity
        assertArrayEquals(referenceArray, testArray);

        //d-12)
        Internals.blankObjectArray(testArray, testArray.length - 3, testArray.length - 3);
        //Check identity
        assertArrayEquals(referenceArray, testArray);

        //e) Blank non-null subrange

        //e-1) first element
        testArray = buildArray(1284047);
        Internals.blankObjectArray(testArray, 0, 1);
        //Check
        checkIntegerArray(testArray, 0, 1);

        //e-2) last element
        testArray = buildArray(1284047);
        Internals.blankObjectArray(testArray, testArray.length - 1, testArray.length);
        //Check
        checkIntegerArray(testArray, testArray.length - 1, testArray.length);

        //e-3) some range starting from start element
        testArray = buildArray(1284047);
        Internals.blankObjectArray(testArray, 0, 0 + 1282703);
        //Check
        checkIntegerArray(testArray, 0, 0 + 1282703);

        //e-4) some range ending at the last element
        testArray = buildArray(1284047);
        Internals.blankObjectArray(testArray, 1282703, 1284047);
        //Check
        checkIntegerArray(testArray,  1282703, 1284047);

        //e-5) Some range in the middle
        testArray = buildArray(1292251);
        Internals.blankObjectArray(testArray, 179939, 185533);
        //Check
        checkIntegerArray(testArray, 179939, 185533);

        testArray = buildArray(876257);
        Internals.blankObjectArray(testArray, 1024, 3*1024);
        //Check
        checkIntegerArray(testArray, 1024, 3*1024);

        testArray = buildArray(434293);
        Internals.blankObjectArray(testArray, 1024, 11*1024 + 17);
        //Check
        checkIntegerArray(testArray, 1024, 11*1024 + 17);

        testArray = buildArray(811);
        Internals.blankObjectArray(testArray, 111, 222);
        //Check
        checkIntegerArray(testArray, 111, 222);
    }

    @Test
    public void testArrayOfBooleans()
    {
        boolean[] testArray = null;

        //a) Blank all a prime
        testArray = buildBooleanArray(COUNT_PRIME);

        Internals.blankBooleanArray(testArray, 0, testArray.length);

        //Check
        checkBooleanArray(testArray, 0, testArray.length);

        //b) Blank all a 1024 multiple
        testArray = buildBooleanArray(1024 * 991);
        Internals.blankBooleanArray(testArray, 0, testArray.length);

        //Check
        checkBooleanArray(testArray, 0, testArray.length);

        //c) Blank a void array
        testArray = buildBooleanArray(0);
        Internals.blankBooleanArray(testArray, 0, testArray.length);

        //c-1) Blank an array < 1024
        testArray = buildBooleanArray(991);
        Internals.blankBooleanArray(testArray, 0, testArray.length);

        //d) Blank a null range (no change)
        testArray = buildBooleanArray(1136287);

        boolean[] referenceArray =  buildBooleanArray(1136287);

        //d-1)
        Internals.blankBooleanArray(testArray, 0, 0);
        //Check identity
        checkBooleanArray(referenceArray, testArray);

        //d-2)
        Internals.blankBooleanArray(testArray, 1024, 1024);
        //Check identity
        checkBooleanArray(referenceArray, testArray);

        //d-3)
        Internals.blankBooleanArray(testArray, 2*1024, 2*1024);
        //Check identity
        checkBooleanArray(referenceArray, testArray);

        //d-4)
        Internals.blankBooleanArray(testArray, 11*1024, 11*1024);
        //Check identity
        checkBooleanArray(referenceArray, testArray);

        //d-5)
        Internals.blankBooleanArray(testArray, 1, 1);
        //Check identity
        checkBooleanArray(referenceArray, testArray);

        //d-6)
        Internals.blankBooleanArray(testArray, 3, 3);
        //Check identity
        checkBooleanArray(referenceArray, testArray);

        //d-7)
        Internals.blankBooleanArray(testArray, 2027, 2027);
        //Check identity
        checkBooleanArray(referenceArray, testArray);

        //d-8)
        Internals.blankBooleanArray(testArray, 2609, 2609);
        //Check identity
        checkBooleanArray(referenceArray, testArray);


        //d-9)
        Internals.blankBooleanArray(testArray, 99529, 99529);
        //Check identity
        checkBooleanArray(referenceArray, testArray);

        //d-10)
        Internals.blankBooleanArray(testArray, testArray.length - 1, testArray.length - 1);
        //Check identity
        checkBooleanArray(referenceArray, testArray);

        //d-11)
        Internals.blankBooleanArray(testArray, testArray.length - 2, testArray.length - 2);
        //Check identity
        checkBooleanArray(referenceArray, testArray);

        //d-12)
        Internals.blankBooleanArray(testArray, testArray.length - 3, testArray.length - 3);
        //Check identity
        checkBooleanArray(referenceArray, testArray);

        //e) Blank non-null subrange

        //e-1) first element
        testArray = buildBooleanArray(1284047);
        Internals.blankBooleanArray(testArray, 0, 1);
        //Check
        checkBooleanArray(testArray, 0, 1);

        //e-2) last element
        testArray = buildBooleanArray(1284047);
        Internals.blankBooleanArray(testArray, testArray.length - 1, testArray.length);
        //Check
        checkBooleanArray(testArray, testArray.length - 1, testArray.length);

        //e-3) some range starting from start element
        testArray = buildBooleanArray(1284047);
        Internals.blankBooleanArray(testArray, 0, 0 + 1282703);
        //Check
        checkBooleanArray(testArray, 0, 0 + 1282703);

        //e-4) some range ending at the last element
        testArray = buildBooleanArray(1284047);
        Internals.blankBooleanArray(testArray, 1282703, 1284047);
        //Check
        checkBooleanArray(testArray,  1282703, 1284047);

        //e-5) Some range in the middle
        testArray = buildBooleanArray(1292251);
        Internals.blankBooleanArray(testArray, 179939, 185533);
        //Check
        checkBooleanArray(testArray, 179939, 185533);

        testArray = buildBooleanArray(876257);
        Internals.blankBooleanArray(testArray, 1024, 3*1024);
        //Check
        checkBooleanArray(testArray, 1024, 3*1024);

        testArray = buildBooleanArray(434293);
        Internals.blankBooleanArray(testArray, 1024, 11*1024 + 17);
        //Check
        checkBooleanArray(testArray, 1024, 11*1024 + 17);

        testArray = buildBooleanArray(811);
        Internals.blankBooleanArray(testArray, 111, 222);
        //Check
        checkBooleanArray(testArray, 111, 222);
    }



    private Integer[] buildArray(int size) {

        Integer[] testArray = new Integer[size];

        for (int i = 0 ; i < size; i++) {

            //use autoboxing
            testArray[i] = new Integer(i);
        }

        return testArray;
    }

    private boolean[] buildBooleanArray(int size) {

        boolean[] testArray = new boolean[size];

        for (int i = 0 ; i < size; i++) {

            //use autoboxing
            if ((i & 0x1) == 0) {
                testArray[i] = true;
            } else {
                testArray[i] = false;
            }

        }

        return testArray;
    }

    private void checkBooleanArray(boolean[] arrayToTest, int startBlankingIndex, int endBlankingIndex) {

        for (int i = 0 ; i < arrayToTest.length; i++) {

            if ((i >=  startBlankingIndex) && (i < endBlankingIndex)) {

                //this is blanked
                if (arrayToTest[i]) {
                    assertTrue(Integer.toString(i), false);
                }
            } else {
                //check the original pattern
                if (((i & 0x1) ==  0  && !arrayToTest[i]) ||
                        ((i & 0x1) !=  0  && arrayToTest[i])) {
                    assertTrue(Integer.toString(i), false);
                }
            }
        }
    }

    private void checkIntegerArray(Integer[] arrayToTest, int startBlankingIndex, int endBlankingIndex) {

        for (int i = 0 ; i < arrayToTest.length; i++) {

            if ((i >=  startBlankingIndex) && (i < endBlankingIndex)) {

                //this is blanked
                if (arrayToTest[i] != null) {
                    assertTrue(Integer.toString(i), false);
                }
            } else {
                //check the original pattern
                if (arrayToTest[i].intValue() != i) {
                    assertTrue(Integer.toString(i), false);
                }
            }
        }
    }

    private void checkBooleanArray(boolean[] expected, boolean[] actual) {

        assertTrue(Arrays.equals(expected, actual));
    }
}
