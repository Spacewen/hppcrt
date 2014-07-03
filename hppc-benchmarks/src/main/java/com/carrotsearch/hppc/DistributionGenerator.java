package com.carrotsearch.hppc;

import java.util.Random;

public class DistributionGenerator
{
    public abstract class Generator
    {
        public abstract int[] prepare(int size);

        public abstract int getNext();
    }

    public final Generator RANDOM = new Generator() {

        @Override
        public int[] prepare(final int size)
        {
            return Util.prepareData(size, DistributionGenerator.this.prng);
        }

        @Override
        public int getNext()
        {
            return DistributionGenerator.this.prng.nextInt();
        }

    };

    public final Generator LINEAR = new Generator() {

        @Override
        public int[] prepare(final int size)
        {
            final int[] t = new int[size];

            for (int i = 0; i < size; i++)
            {
                t[i] = getNext() * 2;
            }
            return t;
        }

        @Override
        public int getNext()
        {
            DistributionGenerator.this.initValue++;
            return DistributionGenerator.this.initValue + 1;
        }
    };

    public final Generator LINEAR_DECREMENT = new Generator() {

        @Override
        public int[] prepare(final int size)
        {
            final int[] t = new int[size];

            for (int i = 0; i < size; i++)
            {
                t[i] = DistributionGenerator.this.maxSize + getNext();
            }
            return t;
        }

        @Override
        public int getNext()
        {
            DistributionGenerator.this.initValue++;
            return DistributionGenerator.this.initValue - 2;
        }
    };

    /**
     * Linear increments on 12 high bits first, then on lower bits.
     */
    public final Generator HIGHBITS = new Generator() {

        @Override
        public int[] prepare(final int size)
        {
            final int[] t = new int[size];
            for (int i = 0; i < size; i++)
            {
                final int nextVal = getNext();

                t[i] = nextVal << 32 - 12 | nextVal >>> 12;
            }
            return t;
        }

        @Override
        public int getNext()
        {
            DistributionGenerator.this.initValue++;
            return DistributionGenerator.this.initValue << 32 - 12 | DistributionGenerator.this.initValue >>> 12;
        }
    };

    private final Random prng;

    private final int maxSize;

    private int initValue = 1;

    public DistributionGenerator(final int maxSize, final Random randomGene)
    {
        this.maxSize = maxSize;
        this.prng = randomGene;
    }
}
