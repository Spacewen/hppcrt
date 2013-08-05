package com.carrotsearch.hppc.jub;

import java.util.HashMap;
import java.util.Random;

import org.junit.*;

import com.carrotsearch.hppc.HashingStrategy;
import com.carrotsearch.hppc.ObjectObjectOpenHashMap;
import com.carrotsearch.junitbenchmarks.AbstractBenchmark;

/**
 * A micro-benchmark test case for comparing {@link HashMap} against
 * {@link ObjectObjectOpenHashMap}.
 */
public class ObjectObjectOpenHashMapBenchmark extends AbstractBenchmark
{
    public static int fakeOutput;
    public static final int COUNT = (int)4e6;
    static Integer [] numbers = new Integer [COUNT];

    private static ObjectObjectOpenHashMap<Integer, Integer> hppc = 
        new ObjectObjectOpenHashMap<Integer, Integer>();
    
    private static ObjectObjectOpenHashMap<Integer, Integer> hppc_preallocated = 
            new ObjectObjectOpenHashMap<Integer, Integer>(COUNT,ObjectObjectOpenHashMap.DEFAULT_LOAD_FACTOR);
    
    //behaves identically as hppc, with overriding by an external strategy
    private static ObjectObjectOpenHashMap<Integer, Integer> hppc_with_strategy = 
            new ObjectObjectOpenHashMap<Integer, Integer>(ObjectObjectOpenHashMap.DEFAULT_CAPACITY, ObjectObjectOpenHashMap.DEFAULT_LOAD_FACTOR,
                    new HashingStrategy<Integer>() {

                        @Override
                        public int computeHashCode(Integer object) {
                            
                            return object.intValue();
                        }

                        @Override
                        public boolean equals(Integer o1, Integer o2) {
                            
                            return o1.intValue() == o2.intValue();
                        }  
                    });
    
    //behaves identically as hppc, with overriding by an external strategy, and preallocated
    private static ObjectObjectOpenHashMap<Integer, Integer> hppc_with_strategy_preallocation = 
            new ObjectObjectOpenHashMap<Integer, Integer>(COUNT, ObjectObjectOpenHashMap.DEFAULT_LOAD_FACTOR,
                    new HashingStrategy<Integer>() {

                        @Override
                        public int computeHashCode(Integer object) {
                            
                            return object.intValue();
                        }

                        @Override
                        public boolean equals(Integer o1, Integer o2) {
                            
                            return o1.intValue() == o2.intValue();
                        }  
                    });
   
    
    private static ObjectObjectOpenHashMap<Integer, Integer> hppc_with_bad_hash_strategy = 
            new ObjectObjectOpenHashMap<Integer, Integer>(ObjectObjectOpenHashMap.DEFAULT_CAPACITY, ObjectObjectOpenHashMap.DEFAULT_LOAD_FACTOR,
                    new HashingStrategy<Integer>() {

                        @Override
                        public int computeHashCode(Integer object) {
                            
                            //dimininuish the quality of hashing by striping bits
                            return object.intValue() << 13 ;
                        }

                        @Override
                        public boolean equals(Integer o1, Integer o2) {
                            
                            return o1.intValue() == o2.intValue();
                        }
                
                    });

    
    private static ObjectObjectOpenHashMap<Integer, Integer> hppc_with_bad_hash_strategy_preallocation = 
            new ObjectObjectOpenHashMap<Integer, Integer>(COUNT, ObjectObjectOpenHashMap.DEFAULT_LOAD_FACTOR,
                    new HashingStrategy<Integer>() {

                        @Override
                        public int computeHashCode(Integer object) {
                            
                            //dimininuish the quality of hashing by striping bits
                            return object.intValue() << 13 ;
                        }

                        @Override
                        public boolean equals(Integer o1, Integer o2) {
                            
                            return o1.intValue() == o2.intValue();
                        }
                
                    });

   
    /* */
    @BeforeClass
    public static void createTestSequence()
    {
        final Random rnd = new Random(0x11223344);
        for (int i = 0; i < COUNT; i++)
            numbers[i] = rnd.nextInt();
    }
    
    @AfterClass
    public static void cleanup()
    {
        numbers = null;
        //this forces the computation result "out", so
        //that the tests compuatation cannot be optimized away.
        System.err.print(fakeOutput << 31);
    }

    /* */
    @Before
    public void before()
    {
        hppc.clear();
        hppc_preallocated.clear();
        hppc_with_strategy.clear();
        hppc_with_strategy_preallocation.clear();
        hppc_with_bad_hash_strategy.clear();
        hppc_with_bad_hash_strategy_preallocation.clear();
    }

    /* */
    @Test
    public void testMultipleOperations() throws Exception
    {
        runWithParticularMap(hppc);
    }
    
    /* */
    @Test
    public void testMultipleOperationsWithPreallocation() throws Exception
    {
        runWithParticularMap(hppc_preallocated);   
    }
    
    /* */
    @Test
    public void testMultipleOperationsWithStrategy() throws Exception
    {
        runWithParticularMap(hppc_with_strategy);   
    }
    
    /* */
    @Test
    public void testMultipleOperationsWithStrategyPreallocation() throws Exception
    {
        runWithParticularMap(hppc_with_strategy_preallocation);   
    }
    
    /* */
    @Test
    public void testMultipleOperationsWithBadStrategy() throws Exception
    {
        runWithParticularMap(hppc_with_bad_hash_strategy);   
    }
    
    /* */
    @Test
    public void testMultipleOperationsWithBadStrategyPreallocation() throws Exception
    {
        runWithParticularMap(hppc_with_bad_hash_strategy_preallocation);   
    }
    
    
    private void runWithParticularMap(ObjectObjectOpenHashMap<Integer, Integer> mapToTest) {
        
        int sum = 0;
        for (int r = 0; r < 2; r++)
        {
            for (int i = 0; i < numbers.length - r; i++)
            {
                if ((numbers[i] & 0x1) == 0)
                {
                    mapToTest.remove(numbers[i + r]);
                }
                else
                {
                    mapToTest.put(numbers[i], numbers[i]);
                }
            }
            
            //Re-run using a contains / lget pattern
            for (int i = 0; i < numbers.length - r; i++)
            { 
                if (mapToTest.containsKey(numbers[i])) {
                    
                    sum += mapToTest.lget();
                }   
            }
        }
        
        mapToTest.clear(); 
        
        fakeOutput += sum;
    }
}
