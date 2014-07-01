package com.carrotsearch.hppc.caliper;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

import com.carrotsearch.hppc.IntIntOpenHashMap;

public class FastUtilMap extends MapImplementation<Int2IntOpenHashMap>
{
    public FastUtilMap()
    {
        super(new Int2IntOpenHashMap(
                IntIntOpenHashMap.DEFAULT_CAPACITY,
                IntIntOpenHashMap.DEFAULT_LOAD_FACTOR));
    }

    public FastUtilMap(final int size)
    {
        super(new Int2IntOpenHashMap(
                size,
                IntIntOpenHashMap.DEFAULT_LOAD_FACTOR));
    }

    @Override
    public void remove(final int k) {
        instance.remove(k);
    }

    @Override
    public void clear() {
        instance.clear();
    }

    @Override
    public void put(final int k, final int v) {
        instance.put(k, v);
    }

    @Override
    public int get(final int k) {
        return instance.get(k);
    }

    @Override
    public int containKeys(final int[] keys)
    {
        final Int2IntOpenHashMap instance = this.instance;
        int count = 0;
        for (int i = 0; i < keys.length; i++)
            count += instance.containsKey(keys[i]) ? 1 : 0;
        return count;
    }

    @Override
    public int putAll(final int[] keys, final int[] values)
    {
        final Int2IntOpenHashMap instance = this.instance;
        int count = 0;
        for (int i = 0; i < keys.length; i++)
        {
            count += instance.put(keys[i], values[i]);
        }
        return count;
    }
}