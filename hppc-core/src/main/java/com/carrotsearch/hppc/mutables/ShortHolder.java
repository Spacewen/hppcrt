package com.carrotsearch.hppc.mutables;

/**
 * <code>short</code> holder.
 */
public class ShortHolder
{
    public short value;

    public ShortHolder()
    {
    }

    public ShortHolder(final short value)
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
        return (other instanceof ShortHolder) && value == ((ShortHolder) other).value;
    }
}
