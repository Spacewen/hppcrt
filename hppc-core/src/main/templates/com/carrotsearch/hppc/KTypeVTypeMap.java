package com.carrotsearch.hppc;

import com.carrotsearch.hppc.cursors.KTypeVTypeCursor;

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
     * 
     * @return The value previously stored under the given key in the map is returned, or else
     * the default value if no previous value existed.
     */
    VType put(KType key, VType value);

    /*! #if ($TemplateOptions.VTypeNumeric) !*/
    /**
     * An equivalent of calling
     * <pre>
     *  putOrAdd(key, additionValue, additionValue);
     * </pre>
     * 
     * @param key The key of the value to adjust.
     * @param additionValue The value to put or add to the existing value if <code>key</code> exists.
     * @return Returns the current value associated with <code>key</code> (after changes).
     */
    /*! #end !*/
    /*! #if ($TemplateOptions.VTypeNumeric)
     VType addTo(KType key, VType additionValue);
    #end !*/

    /*! #if ($TemplateOptions.VTypeNumeric)!*/
    /**
     * <a href="http://trove4j.sourceforge.net">Trove</a>-inspired API method. A logical 
     * equivalent of the following code (but does not update {@link #lastSlot):
     * <pre>
     *  if (containsKey(key))
     *  {
     *      VType v = (VType) (lget() + additionValue);
     *      lset(v);
     *      return v;
     *  }
     *  else
     *  {
     *     put(key, putValue);
     *     return putValue;
     *  }
     * </pre>
     * 
     * @param key The key of the value to adjust.
     * @param putValue The value to put if <code>key</code> does not exist.
     * @param additionValue The value to add to the existing value if <code>key</code> exists.
     * @return Returns the current value associated with <code>key</code> (after changes).
     */
    /*! #end !*/
    /*!#if ($TemplateOptions.VTypeNumeric)
     VType putOrAdd(KType key, VType putValue, VType additionValue);
    #end !*/

    /**
     * @return Returns the value associated with the given key or the default value
     * for the key type, if the key is not associated with any value.
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
}
