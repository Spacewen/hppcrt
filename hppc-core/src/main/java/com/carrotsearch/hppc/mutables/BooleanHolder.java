package com.carrotsearch.hppc.mutables;

/**
 * <code>boolean</code> holder.
 */
public class BooleanHolder
{
    public boolean value;

    public BooleanHolder()
    {
    }

    public BooleanHolder(final boolean value)
    {
        this.value = value;
    }

    @Override
    public int hashCode()
    {
        return value ? 1231 : 1237;
    }

    @Override
    public boolean equals(final Object other)
    {
        return (other instanceof BooleanHolder) && value == ((BooleanHolder) other).value;
    }
}
