package com.carrotsearch.hppc.mutables;

/**
 * <code>char</code> holder.
 */
public class CharHolder
{
    public char value;

    public CharHolder()
    {
    }

    public CharHolder(final char value)
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
        return (other instanceof CharHolder) && value == ((CharHolder) other).value;
    }
}
