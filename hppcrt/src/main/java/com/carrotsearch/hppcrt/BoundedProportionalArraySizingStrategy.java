package com.carrotsearch.hppcrt;

import java.util.ArrayList;

/**
 * Array resizing proportional to the current buffer size, optionally kept within the
 * given minimum and maximum growth limits. Java's {@link ArrayList} uses:
 * <pre>
 * minGrow = 1
 * maxGrow = Integer.MAX_VALUE (unbounded)
 * growRatio = 1.5f
 * </pre>
 */
public final class BoundedProportionalArraySizingStrategy
        implements ArraySizingStrategy
{
    public static final int MAX_ARRAY_LENGTH = Integer.MAX_VALUE - /* aligned array header + slack */32;

    /** Minimum grow count. */
    public final static int DEFAULT_MIN_GROW_COUNT = 10;

    /** Maximum grow count (unbounded). */
    public final static int DEFAULT_MAX_GROW_COUNT = BoundedProportionalArraySizingStrategy.MAX_ARRAY_LENGTH;

    /** Default resize is by half the current buffer's size. */
    public final static float DEFAULT_GROW_RATIO = 1.5f;

    /** Minimum number of elements to grow, if limit exceeded. */
    public final int minGrowCount;

    /** Maximum number of elements to grow, if limit exceeded. */
    public final int maxGrowCount;

    /**
     * The current buffer length is multiplied by this ratio to get the
     * first estimate for the new size. To double the size of the current
     * buffer, for example, set to <code>2</code>.
     */
    public final float growRatio;

    /**
     * Create the default sizing strategy.
     */
    public BoundedProportionalArraySizingStrategy()
    {
        this(BoundedProportionalArraySizingStrategy.DEFAULT_MIN_GROW_COUNT, BoundedProportionalArraySizingStrategy.DEFAULT_MAX_GROW_COUNT, BoundedProportionalArraySizingStrategy.DEFAULT_GROW_RATIO);
    }

    /**
     * Create the sizing strategy with custom policies.
     */
    public BoundedProportionalArraySizingStrategy(final int minGrow, final int maxGrow, final float ratio)
    {
        assert minGrow >= 1 : "Min grow must be >= 1.";
        assert maxGrow >= minGrow : "Max grow must be >= min grow.";
        assert ratio >= 1f : "Growth ratio must be >= 1 (was " + ratio + ").";

        this.minGrowCount = minGrow;
        this.maxGrowCount = maxGrow;
        this.growRatio = ratio - 1.0f;
    }

    /**
     * Grow according to {@link #growRatio}, {@link #minGrowCount} and {@link #maxGrowCount}.
     */
    @Override
    public int grow(final int currentBufferLength, final int elementsCount, final int expectedAdditions)
    {
        long growBy = (long) (currentBufferLength * this.growRatio);

        growBy = Math.max(growBy, this.minGrowCount);
        growBy = Math.min(growBy, this.maxGrowCount);
        final long growTo = Math.min(BoundedProportionalArraySizingStrategy.MAX_ARRAY_LENGTH, growBy + currentBufferLength);

        final long newSize = Math.max((long) elementsCount + expectedAdditions, growTo);

        if (newSize > BoundedProportionalArraySizingStrategy.MAX_ARRAY_LENGTH) {

            throw new BufferAllocationException(
                    "Java array size exceeded (current length: %d, elements: %d, expected additions: %d)",
                    currentBufferLength,
                    elementsCount,
                    expectedAdditions);
        }

        return (int) newSize;
    }
}
