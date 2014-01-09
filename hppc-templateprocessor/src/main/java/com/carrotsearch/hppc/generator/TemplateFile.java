package com.carrotsearch.hppc.generator;

import java.io.File;
import java.io.IOException;

class TemplateFile
{
    public final File file;
    public String fullPath;

    public TemplateFile(final File target)
    {
        this.file = target;

        try
        {
            fullPath = this.file.getCanonicalPath();
        }
        catch (final IOException e)
        {
            //nothing
            e.printStackTrace();
        }
    }
}