package com.carrotsearch.hppcrt.implementations;

import java.util.Random;

import org.openjdk.jmh.infra.Blackhole;

import com.carrotsearch.hppcrt.Util;
import com.carrotsearch.hppcrt.XorShiftRandom;
import com.carrotsearch.hppcrt.maps.ObjectIntOpenCustomHashMap;
import com.carrotsearch.hppcrt.strategies.ObjectHashingStrategy;

public class HppcObjectIntCustomMap extends MapImplementation<ObjectIntOpenCustomHashMap<MapImplementation.ComparableInt>>
{

    private ComparableInt[] insertKeys;
    private ComparableInt[] containsKeys;
    private ComparableInt[] removedKeys;
    private int[] insertValues;

    protected HppcObjectIntCustomMap(final int size, final float loadFactor)
    {
        super(new ObjectIntOpenCustomHashMap<MapImplementation.ComparableInt>(size, loadFactor,
                //A good behaved startegy that compensates bad hashCode() implementation.
                new ObjectHashingStrategy<MapImplementation.ComparableInt>() {

            @Override
            public int computeHashCode(final MapImplementation.ComparableInt object) {

                //eat some CPU to simulate method cost
                Blackhole.consumeCPU(MapImplementation.METHOD_CALL_CPU_COST);

                return object.value;
            }

            @Override
            public boolean equals(final MapImplementation.ComparableInt o1, final MapImplementation.ComparableInt o2) {

                //eat some CPU to simulate method cost
                Blackhole.consumeCPU(MapImplementation.METHOD_CALL_CPU_COST);

                return o1.value == o2.value;
            }
        }));
    }

    /**
     * Setup
     */
    @Override
    public void setup(final int[] keysToInsert, final MapImplementation.HASH_QUALITY hashQ, final int[] keysForContainsQuery, final int[] keysForRemovalQuery) {

        final Random prng = new XorShiftRandom(0x122335577L);

        this.insertKeys = new ComparableInt[keysToInsert.length];

        this.containsKeys = new ComparableInt[keysForContainsQuery.length];
        this.removedKeys = new ComparableInt[keysForRemovalQuery.length];

        this.insertValues = new int[keysToInsert.length];

        //Auto box into Integers, they must have the same length anyway.
        for (int i = 0; i < keysToInsert.length; i++) {

            this.insertKeys[i] = new ComparableInt(keysToInsert[i], hashQ);

            this.insertValues[i] = prng.nextInt();
        }

        //Auto box into Integers
        for (int i = 0; i < keysForContainsQuery.length; i++) {

            this.containsKeys[i] = new ComparableInt(keysForContainsQuery[i], hashQ);
        }

        //Auto box into Integers
        for (int i = 0; i < keysForRemovalQuery.length; i++) {

            this.removedKeys[i] = new ComparableInt(keysForRemovalQuery[i], hashQ);
        }
    }

    @Override
    public void clear() {
        this.instance.clear();
    }

    @Override
    public int size() {

        return this.instance.size();
    }

    @Override
    public int benchPutAll() {

        final ObjectIntOpenCustomHashMap<MapImplementation.ComparableInt> instance = this.instance;

        final int[] values = this.insertValues;

        int count = 0;

        final ComparableInt[] keys = this.insertKeys;

        for (int i = 0; i < keys.length; i++) {

            count += instance.put(keys[i], values[i]);
        }

        return count;
    }

    @Override
    public int benchContainKeys()
    {
        final ObjectIntOpenCustomHashMap<MapImplementation.ComparableInt> instance = this.instance;

        int count = 0;

        final ComparableInt[] keys = this.containsKeys;

        for (int i = 0; i < keys.length; i++) {

            count += instance.containsKey(keys[i]) ? 1 : 0;
        }

        return count;
    }

    @Override
    public int benchRemoveKeys() {

        final ObjectIntOpenCustomHashMap<MapImplementation.ComparableInt> instance = this.instance;

        int count = 0;

        final ComparableInt[] keys = this.removedKeys;

        for (int i = 0; i < keys.length; i++) {

            count += instance.remove(keys[i]);
        }

        return count;
    }

    @Override
    public void setCopyOfInstance(final MapImplementation<?> toCloneFrom) {

        this.instance = ((ObjectIntOpenCustomHashMap<MapImplementation.ComparableInt>) toCloneFrom.instance).clone();

    }
}