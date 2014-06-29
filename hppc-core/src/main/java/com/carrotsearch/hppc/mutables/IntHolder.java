package com.carrotsearch.hppc.mutables;

/**
 * <code>int</code> holder.
 */
public class IntHolder
{
    public int value;

    public IntHolder()
    {
    }

    public IntHolder(final int value)
    {
        this.value = value;
    }

    @Override
    public int hashCode()
    {
        return value;
    }

    @Override
    public boolean equals(final Object other)
    {
        return (other instanceof IntHolder) && value == ((IntHolder) other).value;
    }
}
