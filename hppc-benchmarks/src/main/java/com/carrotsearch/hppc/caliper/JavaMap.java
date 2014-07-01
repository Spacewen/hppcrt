package com.carrotsearch.hppc.caliper;

import java.util.HashMap;

public class JavaMap extends MapImplementation<HashMap<Integer, Integer>>
{
    public JavaMap()
    {
        super(new HashMap<Integer, Integer>());
    }

    public JavaMap(final int size)
    {
        super(new HashMap<Integer, Integer>(size));
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
        final HashMap<Integer, Integer> prepared = this.instance;
        int count = 0;
        for (int i = 0; i < keys.length; i++)
            count += prepared.containsKey(keys[i]) ? 1 : 0;
        return count;
    }

    @Override
    public int putAll(final int[] keys, final int[] values)
    {
        final HashMap<Integer, Integer> instance = this.instance;
        final int count = 0;
        for (int i = 0; i < keys.length; i++)
        {
            instance.put(keys[i], values[i]);
        }
        return count;
    }
}
