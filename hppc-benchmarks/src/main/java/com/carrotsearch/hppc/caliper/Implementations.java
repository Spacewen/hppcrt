package com.carrotsearch.hppc.caliper;

import com.carrotsearch.hppc.IntIntOpenHashMap;
import com.carrotsearch.hppc.IntIntRobinHoodHashMap;

/**
 * 
 */
public enum Implementations
{
    HPPC
    {
        @Override
        public MapImplementation<?> getInstance()
        {
            return new HppcMap(IntIntOpenHashMap.newInstance());
        }
    },

    RH_HPPC
    {
        @Override
        public MapImplementation<?> getInstance()
        {
            return new HppcMap(IntIntRobinHoodHashMap.newInstance());
        }
    },

    HPPC_NOPERTURBS
    {
        @Override
        public MapImplementation<?> getInstance()
        {
            return new HppcMap(IntIntOpenHashMap.newInstanceWithoutPerturbations());
        }
    },

    RH_HPPC_NOPERTURBS
    {
        @Override
        public MapImplementation<?> getInstance()
        {
            return new HppcMap(IntIntRobinHoodHashMap.newInstanceWithoutPerturbations());
        }
    },

    FASTUTIL
    {
        @Override
        public MapImplementation<?> getInstance()
        {
            return new FastUtilMap();
        }
    },

    JAVA
    {
        @Override
        public MapImplementation<?> getInstance()
        {
            return new JavaMap();
        }
    },

    TROVE
    {
        @Override
        public MapImplementation<?> getInstance()
        {
            return new TroveMap();
        }
    },

    MAHOUT
    {
        @Override
        public MapImplementation<?> getInstance()
        {
            return new MahoutMap();
        }
    };

    public abstract MapImplementation<?> getInstance();
}