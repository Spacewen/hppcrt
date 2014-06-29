package com.carrotsearch.hppc.mutables;

/**
 * <code>double</code> holder.
 */
public class DoubleHolder
{
    public double value;

    public DoubleHolder()
    {
    }

    public DoubleHolder(final double value)
    {
        this.value = value;
    }

    @Override
    public int hashCode()
    {
        final long bits = Double.doubleToLongBits(value);
        return (int) (bits ^ (bits >>> 32));
    }

    @Override
    public boolean equals(final Object other)
    {
        return (other instanceof DoubleHolder) &&
                Double.doubleToLongBits(value) ==
                Double.doubleToLongBits(((DoubleHolder) other).value);
    }
}
