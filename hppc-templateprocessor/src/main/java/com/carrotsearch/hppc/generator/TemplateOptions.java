package com.carrotsearch.hppc.generator;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;

/**
 * Template options for velocity directives in templates.
 */
public class TemplateOptions
{
    public Type ktype;
    public Type vtype;

    public boolean doNotGenerateKType = false;
    public boolean doNotGenerateVType = false;

    public HashSet<String> definesSet = new HashSet<String>();

    public File sourceFile;

    public TemplateOptions(final Type ktype)
    {
        this(ktype, null);
    }

    public TemplateOptions(final Type ktype, final Type vtype)
    {
        this.ktype = ktype;
        this.vtype = vtype;
    }

    public boolean isKTypePrimitive()
    {
        return ktype != Type.GENERIC;
    }

    public boolean isKTypeNumeric()
    {
        return (ktype != Type.GENERIC && ktype != Type.BOOLEAN);
    }

    public boolean isKTypeBoolean()
    {
        return (ktype == Type.BOOLEAN);
    }

    public boolean isKType(final String... strKind)
    {
        //return true if it matches any type of the list
        for (final String kind : strKind)
        {
            if (Type.valueOf(kind) == this.ktype)
            {

                return true;
            }
        }

        return false;
    }

    public boolean isVTypePrimitive()
    {
        return getVType() != Type.GENERIC;
    }

    public boolean isVTypeNumeric()
    {
        return (getVType() != Type.GENERIC && getVType() != Type.BOOLEAN);
    }

    public boolean isVTypeBoolean()
    {
        return (vtype == Type.BOOLEAN);
    }

    public boolean isVType(final String... strKind)
    {
        if (this.vtype == null)
        {
            return false;
        }

        //return true if it matches any type of the list
        for (final String kind : strKind)
        {
            if (Type.valueOf(kind) == this.vtype)
            {
                return true;
            }
        }

        return false;
    }

    public boolean isKTypeGeneric()
    {
        return ktype == Type.GENERIC;
    }

    public boolean isVTypeGeneric()
    {
        if (this.vtype == null)
        {
            return false;
        }

        return getVType() == Type.GENERIC;
    }


    public boolean isAllGeneric()
    {
        return isKTypeGeneric() && isVTypeGeneric();
    }

    public boolean isAnyPrimitive()
    {
        return isKTypePrimitive() || isVTypePrimitive();
    }

    public boolean isAnyGeneric()
    {
        return isKTypeGeneric() || (hasVType() && isVTypeGeneric());
    }

    public boolean hasVType()
    {
        return vtype != null;
    }

    public Type getKType()
    {
        return ktype;
    }

    public Type getVType()
    {
        if (vtype == null) throw new RuntimeException("VType is null.");
        return vtype;
    }

    public void doNotGenerateKType(final String... notGeneratingType)
    {
        this.doNotGenerateKType = false;

        //if any of the notGeneratingType is this.ktype, then do not generate
        for (final String  notToBeGenerated : notGeneratingType) {

            if (Type.valueOf(notToBeGenerated) == this.ktype) {

                this.doNotGenerateKType = true;
                return;
            }
        }
    }

    public void doNotGenerateVType(final String... notGeneratingType)
    {
        this.doNotGenerateVType = false;

        if (this.vtype == null)
        {
            return;
        }

        //if any of the notGeneratingType is this.ktype, then do not generate
        for (final String notToBeGenerated : notGeneratingType)
        {
            if (Type.valueOf(notToBeGenerated) == this.vtype)
            {
                this.doNotGenerateVType = true;
                return;
            }
        }
    }

    public boolean isDoNotGenerateKType()
    {
        return this.doNotGenerateKType;
    }

    public boolean isDoNotGenerateVType()
    {
        return this.doNotGenerateVType;
    }

    public void define(final String... defines)
    {
        for (final String def : defines)
        {
            this.definesSet.add(def);
        }
    }

    public void unDefine(final String... defines)
    {
        for (final String def : defines)
        {
            this.definesSet.remove(def);
        }
    }

    public boolean isDefined(final String... defines)
    {
        for (final String def : defines)
        {
            if (this.definesSet.contains(def))
            {
                return true;
            }
        }

        return false;
    }

    public boolean isUnDefined(final String... defines)
    {
        for (final String def : defines)
        {
            if (this.definesSet.contains(def))
            {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns the current time in ISO format.
     */
     public String getTimeNow()
     {
         final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ENGLISH);
         return format.format(new Date());
     }

     public String getSourceFile()
     {
         return sourceFile.getName();
     }

     public String getGeneratedAnnotation()
     {
         return "@javax.annotation.Generated(date = \"" +
                 getTimeNow() + "\", value = \"HPPC generated from: " +
                 sourceFile.getName() + "\")";
     }
}