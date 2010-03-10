package com.carrotsearch.hppc;


/**
 * An associative container with unique binding from keys to a single value.
 */
public interface ObjectObjectMap<KType, VType> 
    extends ObjectObjectAssociativeContainer<KType, VType>
{
    /**
     * Place a given key and value in the container.
     * 
     * @return The value previously stored under the given key in the map is returned.
     */
    public VType put(KType key, VType value);

    /**
     * @return Returns the value associated with the given key or the default value
     * for the key type, if the key is not associated with any value. 
     */
    public VType get(KType key);

    /**
     * Puts all keys from an iterable cursor to this map, replacing the values
     * of existing keys, if such keys are present.   
     * 
     * @return Returns the number of keys added to the map as a result of this
     * call (not previously present in the map). Values of existing keys are overwritten.
     */
    public int putAll(
        ObjectObjectAssociativeContainer<? extends KType, ? extends VType> container);
    
    /**
     * Remove all values at the given key. The default value for the key type is returned
     * if the value does not exist in the map. 
     */
    public VType remove(KType key);
}
