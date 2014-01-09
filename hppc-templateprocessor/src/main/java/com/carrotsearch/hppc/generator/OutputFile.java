package com.carrotsearch.hppc.generator;

import java.io.File;
import java.io.IOException;

class OutputFile
{
    public final File file;
    public boolean generated;
    public boolean updated = false;
    public String fullPath;

    public OutputFile(final File target, final boolean generated)
    {
        this.file = TemplateProcessor.canonicalFile(target);
        this.generated = generated;
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