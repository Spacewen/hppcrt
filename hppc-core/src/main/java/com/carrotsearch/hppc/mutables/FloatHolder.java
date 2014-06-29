package com.carrotsearch.hppc.mutables;

/**
 * <code>float</code> holder.
 */
public class FloatHolder
{
    public float value;

    public FloatHolder()
    {
    }

    public FloatHolder(final float value)
    {
        this.value = value;
    }

    @Override
    public int hashCode()
    {
        return Float.floatToIntBits(value);
    }

    @Override
    public boolean equals(final Object other)
    {
        return (other instanceof FloatHolder) &&
                Float.floatToIntBits(value) ==
                Float.floatToIntBits(((FloatHolder) other).value);
    }
}
