package com.carrotsearch.hppc.mutables;

/**
 * <code>long</code> holder.
 */
public class LongHolder
{
    public long value;

    public LongHolder()
    {
    }

    public LongHolder(final long value)
    {
        this.value = value;
    }

    @Override
    public int hashCode()
    {
        return (int) (value ^ (value >>> 32));
    }

    @Override
    public boolean equals(final Object other)
    {
        return (other instanceof LongHolder) && value == ((LongHolder) other).value;
    }
}
