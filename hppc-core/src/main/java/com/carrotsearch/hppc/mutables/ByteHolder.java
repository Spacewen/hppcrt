package com.carrotsearch.hppc.mutables;

/**
 * <code>byte</code> holder.
 */
public class ByteHolder
{
    public byte value;

    public ByteHolder()
    {
    }

    public ByteHolder(final byte value)
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
        return (other instanceof ByteHolder) && value == ((ByteHolder) other).value;
    }
}
