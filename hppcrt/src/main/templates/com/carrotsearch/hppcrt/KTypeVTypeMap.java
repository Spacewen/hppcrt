package com.carrotsearch.hppcrt;

import com.carrotsearch.hppcrt.cursors.KTypeVTypeCursor;

/**
 * An associative container with unique binding from keys to a single value.
 */
/*! ${TemplateOptions.doNotGenerateKType("BOOLEAN")} !*/
/*! ${TemplateOptions.generatedAnnotation} !*/
public interface KTypeVTypeMap<KType, VType>
        extends KTypeVTypeAssociativeContainer<KType, VType>
{
    /**
     * Place a given key and value in the container.
     * @return Returns the value previously stored under the given key in the map if an equal key is part of the map, <b>and replaces the existing
     *  value only </b> with the argument value. If no previous key existed,
     * the default value is returned and the (key, value) pair is inserted.
     */
    VType put(KType key, VType value);

    /**
     * <a href="http://trove4j.sourceforge.net">Trove</a>-inspired API method. An equivalent
     * of the following code:
     * <pre>
     * if (!map.containsKey(key))
     *      map.put(key, value);
     * </pre>
     * 
     * @param key The key of the value to check.
     * @param value The value to put if <code>key</code> does not exist.
     * @return <code>true</code> if <code>key</code> did not exist and <code>value</code>
     * was placed in the map.
     */
    boolean putIfAbsent(final KType key, final VType value);

    /*! #if ($TemplateOptions.VTypeNumeric)!*/
    /**
     * If <code>key</code> exists, <code>putValue</code> is inserted into the map,
     * otherwise any existing value is incremented by <code>additionValue</code>.
     * 
     * @param key
     *          The key of the value to adjust.
     * @param putValue
     *          The value to put if <code>key</code> does not exist.
     * @param incrementValue
     *          The value to add to the existing value if <code>key</code> exists.
     * @return Returns the current value associated with <code>key</code> (after
     *         changes).
     */
    VType putOrAdd(KType key, VType putValue, VType additionValue);

    /*! #end !*/

    /*! #if ($TemplateOptions.VTypeNumeric) !*/
    /**
     * An equivalent of calling
     * 
     * <pre>
     * putOrAdd(key, additionValue, additionValue);
     * </pre>
     * 
     * @param key
     *          The key of the value to adjust.
     * @param additionValue
     *          The value to put or add to the existing value if <code>key</code>
     *          exists.
     * @return Returns the current value associated with <code>key</code> (after
     *         changes).
     */
    VType addTo(KType key, VType additionValue);

    /*! #end !*/

    /**
     * @return Returns the value associated with the given key or the default value
     * for the value type, if the key is not associated with any value.
     *
     */
    VType get(KType key);

    /**
     * Puts all keys from another container to this map, replacing the values
     * of existing keys, if such keys are present.
     * 
     * @return Returns the number of keys added to the map as a result of this
     * call (not previously present in the map). Values of existing keys are overwritten.
     */
    int putAll(KTypeVTypeAssociativeContainer<? extends KType, ? extends VType> container);

    /**
     * Puts all keys from an iterable cursor to this map, replacing the values
     * of existing keys, if such keys are present.
     * 
     * @return Returns the number of keys added to the map as a result of this
     * call (not previously present in the map). Values of existing keys are overwritten.
     */
    int putAll(Iterable<? extends KTypeVTypeCursor<? extends KType, ? extends VType>> iterable);

    /**
     * Remove all values at the given key. The default value for the key type is returned
     * if the value does not exist in the map.
     */
    VType remove(KType key);

    /**
     * Clear all keys and values in the container.
     */
    void clear();

    /**
     * Returns the "default value" value used in containers methods returning
     * "default value"
     */
    VType getDefaultValue();

    /**
     * Set the "default value" value to be used in containers methods returning
     * "default value"
     * 
     * @return
     */
    void setDefaultValue(final VType defaultValue);

}
