package com.carrotsearch.hppc.mutables;

/**
 * <code>boolean</code> holder.
 */
public final class BooleanHolder
{
    public boolean value;

    public BooleanHolder()
    {
    }

    public BooleanHolder(boolean value)
    {
        this.value = value;
    }

    public int hashCode()
    {
        return value ? 1231 : 1237;
    }

    public boolean equals(Object other)
    {
        return (other instanceof BooleanHolder) && value == ((BooleanHolder) other).value;
    }
}
