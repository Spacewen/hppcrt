package com.carrotsearch.hppc.caliper;

import org.apache.mahout.math.map.AbstractIntIntMap;
import org.apache.mahout.math.map.OpenIntIntHashMap;
import org.apache.mahout.math.set.AbstractSet;

import com.carrotsearch.hppc.IntIntOpenHashMap;

public class MahoutMap extends MapImplementation<OpenIntIntHashMap>
{
    public MahoutMap()
    {
        super(new OpenIntIntHashMap(
                IntIntOpenHashMap.DEFAULT_CAPACITY,
                AbstractSet.DEFAULT_MIN_LOAD_FACTOR,
                IntIntOpenHashMap.DEFAULT_LOAD_FACTOR));
    }

    public MahoutMap(final int size)
    {
        super(new OpenIntIntHashMap(
                size,
                AbstractSet.DEFAULT_MIN_LOAD_FACTOR,
                IntIntOpenHashMap.DEFAULT_LOAD_FACTOR));
    }

    @Override
    public void remove(final int k) {
        instance.removeKey(k);
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
        final OpenIntIntHashMap prepared = this.instance;
        int count = 0;
        for (int i = 0; i < keys.length; i++)
            count += prepared.containsKey(keys[i]) ? 1 : 0;
        return count;
    }

    @Override
    public int putAll(final int[] keys, final int[] values)
    {
        final OpenIntIntHashMap instance = this.instance;
        int count = 0;
        for (int i = 0; i < keys.length; i++)
        {
            count += instance.put(keys[i], values[i]) ? 1 : 0;
        }
        return count;
    }
}