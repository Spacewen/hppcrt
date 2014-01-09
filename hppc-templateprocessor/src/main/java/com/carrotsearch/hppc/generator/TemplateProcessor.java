package com.carrotsearch.hppc.generator;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.ExtendedProperties;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.RuntimeInstance;


/**
 * Template processor for HPPC templates.
 */
public final class TemplateProcessor
{
    public boolean verbose = false;
    public boolean incremental = true;
    public File templatesDir;
    public File outputDir;

    private final RuntimeInstance velocity;

    /**
     * 
     */
    public TemplateProcessor()
    {
        final ExtendedProperties p = new ExtendedProperties();
        final RuntimeInstance velocity = new RuntimeInstance();
        p.setProperty(RuntimeConstants.SET_NULL_ALLOWED, "false");
        velocity.setConfiguration(p);
        this.velocity = velocity;
    }

    /**
     * 
     */
    public void setVerbose(final boolean verbose)
    {
        this.verbose = verbose;
    }

    /**
     * 
     */
    public void setIncremental(final boolean incremental)
    {
        this.incremental = incremental;
    }

    /**
     * 
     */
    public void setDestDir(final File dir)
    {
        this.outputDir = dir;
    }

    /**
     * 
     */
    public void setTemplatesDir(final File dir)
    {
        this.templatesDir = dir;
    }

    /**
     * 
     */
    public void execute()
    {
        // Collect files/ checksums from the output folder.
        final List<OutputFile> outputs = collectOutputFiles(new ArrayList<OutputFile>(),
                outputDir);

        // Collect template files in the input folder.
        final List<TemplateFile> inputs = collectTemplateFiles(new ArrayList<TemplateFile>(),
                templatesDir);

        // Process templates
        System.out.println("Processing " + inputs.size() + " templates to: " + outputDir.getPath());
        final long start = System.currentTimeMillis();
        processTemplates(inputs, outputs);
        final long end = System.currentTimeMillis();
        System.out.println(String.format(Locale.ENGLISH, "Processed in %.2f sec.", (end - start) / 1000.0));

        // Remove non-marked files.
        int generated = 0;
        int updated = 0;
        int deleted = 0;
        for (final OutputFile f : outputs)
        {
            if (!f.generated)
            {
                deleted++;
                if (verbose) System.out.println("Deleted: " + f.file);
                f.file.delete();
            }

            if (f.generated) generated++;

            if (f.updated)
            {
                updated++;
                if (verbose) System.out.println("Updated: "
                        + relativePath(f.file, this.outputDir));
            }
        }

        System.out.println("Generated " + generated + " files (" + updated + " updated, "
                + deleted + " deleted).");
    }

    /**
     * Apply templates to <code>.ktype</code> files (single-argument).
     */
    private void processTemplates(final List<TemplateFile> inputs, final List<OutputFile> outputs)
    {
        for (final TemplateFile f : inputs)
        {
            final String fileName = f.file.getName();
            if (!fileName.contains("VType") && fileName.contains("KType"))
            {
                for (final Type t : Type.values())
                {
                    final TemplateOptions options = new TemplateOptions(t);
                    options.sourceFile = f.file;
                    generate(f, outputs, options);
                }
            }
            if (fileName.contains("KTypeVType"))
            {
                for (final Type ktype : Type.values())
                {
                    for (final Type vtype : Type.values())
                    {
                        final TemplateOptions options = new TemplateOptions(ktype, vtype);
                        options.sourceFile = f.file;
                        generate(f, outputs, options);
                    }
                }
            }
        }

        if (verbose)
            System.out.println(
                    "Velocity: " + timeVelocity + "\n" +
                            "Intrinsics: " + timeIntrinsics + "\n" +
                            "TypeClassRefs: " + timeTypeClassRefs + "\n" +
                            "Comments: " + timeComments + "\n");
    }

    /**
     * Apply templates.
     */
    private void generate(final TemplateFile f, final List<OutputFile> outputs,
            final TemplateOptions templateOptions)
    {
        final String targetFileName = targetFileName(relativePath(f.file, templatesDir),
                templateOptions);
        final OutputFile output = findOrCreate(targetFileName, outputs);

        if (!incremental || !output.file.exists()
                || output.file.lastModified() <= f.file.lastModified())
        {
            String input = readFile(f.file);
            long t1, t0 = System.currentTimeMillis();

            //Apply velocity : if TemplateOptions.isDoNotGenerateKType() or TemplateOptions.isDoNotGenerateVType() is true, do not
            //generate the final file.
            input = filterVelocity(f, input, templateOptions);

            if (!templateOptions.isDoNotGenerateKType() && !templateOptions.isDoNotGenerateVType())
            {
                timeVelocity += (t1 = System.currentTimeMillis()) - t0;
                input = filterIntrinsics(f, input, templateOptions);
                timeIntrinsics += (t0 = System.currentTimeMillis()) - t1;
                input = filterTypeClassRefs(f, input, templateOptions);
                timeTypeClassRefs += (t1 = System.currentTimeMillis()) - t0;
                input = filterComments(f, input, templateOptions);
                timeComments += (t0 = System.currentTimeMillis()) - t1;

                output.updated = true;
                saveFile(output.file, input);
            }
            else
            {
                //indeed remove the generated file
                output.file.delete();
                outputs.remove(output);

                System.out.println("INFO : output from template '" + f.fullPath +
                        "' with KTYpe = " + templateOptions.getKType() + " and VType =  " +
                        (templateOptions.hasVType() ? templateOptions.getVType().toString() : "null") + " was bypassed...");
            }
        }
    }

    long timeVelocity, timeIntrinsics, timeTypeClassRefs, timeComments;

    private String filterIntrinsics(final TemplateFile f, String input,
            final TemplateOptions templateOptions)
    {
        final Pattern p = Pattern.compile("(Intrinsics.\\s*)(<[^>]+>\\s*)?([a-zA-Z]+)",
                Pattern.MULTILINE | Pattern.DOTALL);

        final StringBuffer sb = new StringBuffer();

        while (true)
        {
            final Matcher m = p.matcher(input);
            if (m.find())
            {
                sb.append(input.substring(0, m.start()));

                final String method = m.group(3);

                int bracketCount = 0;
                int last = m.end() + 1;
                final ArrayList<String> params = new ArrayList<String>();
                outer: for (int i = m.end(); i < input.length(); i++)
                {
                    switch (input.charAt(i))
                    {
                        case '(':
                            bracketCount++;
                            break;
                        case ')':
                            bracketCount--;
                            if (bracketCount == 0)
                            {
                                params.add(input.substring(last, i).trim());
                                input = input.substring(i + 1);
                                break outer;
                            }
                            break;
                        case ',':
                            if (bracketCount == 1)
                            {
                                params.add(input.substring(last, i));
                                last = i + 1;
                            }
                            break;
                    }
                }

                if ("defaultKTypeValue".equals(method))
                {
                    sb.append(templateOptions.isKTypeGeneric()
                            ? "null" : TemplateProcessor.getDefaultValue(templateOptions.getKType().getType()));
                }
                else if ("defaultVTypeValue".equals(method))
                {
                    sb.append(templateOptions.isVTypeGeneric()
                            ? "null" : TemplateProcessor.getDefaultValue(templateOptions.getVType().getType()));
                }
                else if ("newKTypeArray".equals(method))
                {
                    sb.append(templateOptions.isKTypeGeneric()
                            ? "Internals.<KType[]>newArray(" + params.get(0) + ")"
                                    : "new " + templateOptions.getKType().getType() + " [" + params.get(0) + "]");
                }
                else if ("newVTypeArray".equals(method))
                {
                    sb.append(templateOptions.isVTypeGeneric()
                            ? "Internals.<VType[]>newArray(" + params.get(0) + ")"
                                    : "new " + templateOptions.getVType().getType() + " [" + params.get(0) + "]");
                }
                else if ("equalsKType".equals(method))
                {
                    if (templateOptions.isKTypeGeneric())
                    {
                        sb.append(String.format("((%1$s) == null ? (%2$s) == null : (%1$s).equals((%2$s)))",
                                params.toArray()));
                    }
                    else
                    {
                        sb.append(String.format("((%1$s) == (%2$s))", params.toArray()));
                    }
                }
                else if ("compareKType".equals(method))
                {
                    if (templateOptions.isKTypeGeneric())
                    {
                        sb.append(String.format("((%1$s).compareTo(%2$s))", params.toArray()));
                    }
                    else if (templateOptions.isKTypeNumeric())
                    {
                        sb.append(String.format("(%1$s - %2$s)", params.toArray()));
                    }
                    else
                    {
                        //particular case for booleans
                        sb.append(String.format("((%1$s == %2$s) ? 0 : (%1$s ? 1 : -1))", params.toArray()));
                    }
                }
                else if ("compareKTypeUnchecked".equals(method))
                {
                    if (templateOptions.isKTypeGeneric())
                    {
                        sb.append(String.format("(((Comparable<? super KType>)%1$s).compareTo(%2$s))", params.toArray()));
                    }
                    else if (templateOptions.isKTypeNumeric())
                    {
                        sb.append(String.format("(%1$s - %2$s)", params.toArray()));
                    }
                    else
                    {
                        //particular case for booleans
                        sb.append(String.format("((%1$s == %2$s) ? 0 : (%1$s ? 1 : -1))", params.toArray()));
                    }
                }
                else if ("isCompSupKType".equals(method))
                {
                    if (templateOptions.isKTypeGeneric())
                    {
                        sb.append(String.format("((%1$s).compareTo(%2$s) > 0)", params.toArray()));
                    }
                    else if (templateOptions.isKTypeNumeric())
                    {
                        sb.append(String.format("(%1$s > %2$s)", params.toArray()));
                    }
                    else
                    {
                        //particular case for booleans
                        sb.append(String.format("((%1$s == %2$s) ? false : %1$s)", params.toArray()));
                    }
                }
                else if ("isCompSupKTypeUnchecked".equals(method))
                {
                    if (templateOptions.isKTypeGeneric())
                    {
                        sb.append(String.format("(((Comparable<? super KType>)%1$s).compareTo(%2$s) > 0)", params.toArray()));
                    }
                    else if (templateOptions.isKTypeNumeric())
                    {
                        sb.append(String.format("(%1$s > %2$s)", params.toArray()));
                    }
                    else
                    {
                        //particular case for booleans
                        sb.append(String.format("((%1$s == %2$s) ? false : %1$s)", params.toArray()));
                    }
                }
                else if ("isCompInfKType".equals(method))
                {
                    if (templateOptions.isKTypeGeneric())
                    {
                        sb.append(String.format("((%1$s).compareTo(%2$s) < 0)", params.toArray()));
                    }
                    else if (templateOptions.isKTypeNumeric())
                    {
                        sb.append(String.format("(%1$s < %2$s)", params.toArray()));
                    }
                    else
                    {
                        //particular case for booleans
                        sb.append(String.format("((%1$s == %2$s) ? false : %2$s)", params.toArray()));
                    }
                }
                else if ("isCompInfKTypeUnchecked".equals(method))
                {
                    if (templateOptions.isKTypeGeneric())
                    {
                        sb.append(String.format("(((Comparable<? super KType>)%1$s).compareTo(%2$s) < 0)", params.toArray()));
                    }
                    else if (templateOptions.isKTypeNumeric())
                    {
                        sb.append(String.format("(%1$s < %2$s)", params.toArray()));
                    }
                    else
                    {
                        //particular case for booleans
                        sb.append(String.format("((%1$s == %2$s) ? false : %2$s)", params.toArray()));
                    }
                }
                else if ("isCompEqualKType".equals(method))
                {
                    if (templateOptions.isKTypeGeneric())
                    {
                        sb.append(String.format("((%1$s).compareTo(%2$s) == 0)", params.toArray()));
                    }
                    else
                    {
                        sb.append(String.format("(%1$s == %2$s)", params.toArray()));
                    }
                }
                else if ("isCompEqualKTypeUnchecked".equals(method))
                {
                    if (templateOptions.isKTypeGeneric())
                    {
                        sb.append(String.format("(((Comparable<? super KType>)%1$s).compareTo(%2$s) == 0)", params.toArray()));
                    }
                    else
                    {
                        sb.append(String.format("(%1$s == %2$s)", params.toArray()));
                    }
                }
                else if ("equalsKTypeHashStrategy".equals(method))
                {
                    if (templateOptions.isKTypeGeneric())
                    {
                        sb.append(String.format("((%1$s) == null ? (%2$s) == null :((%3$s) == null ? (%1$s).equals((%2$s)):(%3$s).equals((%1$s),(%2$s))))",
                                params.toArray()));
                    }
                    else
                    {
                        throw new RuntimeException("Intrinsic.equalsKTypeHashStrategy() call is only applicable to Objects");
                    }
                }
                else if ("equalsVType".equals(method))
                {
                    if (templateOptions.isVTypeGeneric())
                    {
                        sb.append(String.format("((%1$s) == null ? (%2$s) == null : (%1$s).equals((%2$s)))",
                                params.toArray()));
                    }
                    else
                    {
                        sb.append(String.format("((%1$s) == (%2$s))", params.toArray()));
                    }
                }
                else if ("oneLeft".equals(method))
                {
                    // (index >= 1) ? index - 1 : modulus - 1;
                    sb.append(String.format("((%1$s >= 1) ? (%1$s - 1): (%2$s - 1))",
                            params.toArray()));
                }
                else if ("oneRight".equals(method))
                {
                    // (index + 1 == modulus) ? 0 : index + 1;
                    sb.append(String.format("((%1$s + 1 == %2$s) ? 0: (%1$s + 1))",
                            params.toArray()));
                }
                else if ("getLinkNodeValue".equals(method))
                {
                    sb.append(String.format("(((long) (%1$s) << 32) | (%2$s))",
                            params.toArray()));
                }
                else if ("getLinkBefore".equals(method))
                {
                    sb.append(String.format("((int) ((%1$s) >> 32))",
                            params.toArray()));
                }
                else if ("getLinkAfter".equals(method))
                {
                    sb.append(String.format("((int) ((%1$s) & 0x00000000FFFFFFFFL))",
                            params.toArray()));
                }
                else if ("setLinkBeforeNodeValue".equals(method))
                {
                    sb.append(String.format("(((long) (%2$s) << 32) | ((%1$s) & 0x00000000FFFFFFFFL))",
                            params.toArray()));
                }
                else if ("setLinkAfterNodeValue".equals(method))
                {
                    sb.append(String.format("((%2$s) | ((%1$s) & 0xFFFFFFFF00000000L))",
                            params.toArray()));
                }
                else
                {
                    throw new RuntimeException("Unrecognized Intrinsic call: " + method);
                }
            }
            else
            {
                sb.append(input);
                break;
            }
        }

        return sb.toString();
    }

    private String filterComments(final TemplateFile f, final String input,
            final TemplateOptions templateOptions)
    {
        final Pattern p = Pattern.compile("(/\\*!)|(!\\*/)", Pattern.MULTILINE
                | Pattern.DOTALL);
        return p.matcher(input).replaceAll("");
    }

    private String filterTypeClassRefs(final TemplateFile f, String input, final TemplateOptions options)
    {
        input = unifyTypeWithSignature(f, input, options);
        input = rewriteSignatures(f, input, options);
        input = rewriteLiterals(f, input, options);
        return input;
    }

    private String unifyTypeWithSignature(final TemplateFile f, final String input,
            final TemplateOptions options)
    {
        // This is a hack. A better way would be a full source AST and
        // rewrite at the actual typeDecl level.
        // KTypePredicate<? super VType> => VTypePredicate<? super VType>
        return input.replaceAll("(KType)(?!VType)([A-Za-z]+)(<(?:(\\? super ))?VType>)", "VType$2$3");
    }

    private String rewriteSignatures(final TemplateFile f, final String input, final TemplateOptions options)
    {
        final Pattern p = Pattern.compile("<[\\?A-Z]");
        final Matcher m = p.matcher(input);

        final StringBuilder sb = new StringBuilder();
        int fromIndex = 0;
        while (m.find(fromIndex))
        {
            final int next = m.start();
            int end = next + 1;
            int bracketCount = 1;
            while (bracketCount > 0 && end < input.length())
            {
                switch (input.charAt(end++)) {
                    case '<': bracketCount++; break;
                    case '>': bracketCount--; break;
                }
            }
            sb.append(input.substring(fromIndex, next));
            sb.append(rewriteSignature(input.substring(next, end), options));
            fromIndex = end;
        }
        sb.append(input.substring(fromIndex, input.length()));
        return sb.toString();
    }

    private String rewriteSignature(final String signature, final TemplateOptions options)
    {
        if (!signature.contains("KType") && !signature.contains("VType"))
            return signature;

        final Pattern p = Pattern.compile("<[^<>]*>", Pattern.MULTILINE | Pattern.DOTALL);

        final StringBuilder sb = new StringBuilder(signature);
        Matcher m = p.matcher(sb);
        while (m.find())
        {
            String group = m.group();
            group = group.substring(1, group.length() - 1);
            final List<String> args = new ArrayList<String>(Arrays.asList(group.split(",")));
            final StringBuilder b = new StringBuilder();
            for (final Iterator<String> i = args.iterator(); i.hasNext();)
            {
                String arg = i.next().trim();

                if (options.isKTypePrimitive())
                {
                    if (isGenericOnly(arg, "KType"))
                        arg = "";
                    else
                        arg = arg.replace("KType", options.getKType().getBoxedType());
                }

                if (options.hasVType() && options.isVTypePrimitive())
                {
                    if (isGenericOnly(arg, "VType"))
                        arg = "";
                    else
                        arg = arg.replace("VType", options.getVType().getBoxedType());
                }

                if (arg.length() > 0)
                {
                    if (b.length() > 0) b.append(", ");
                    b.append(arg.trim());
                }
            }

            if (b.length() > 0)
            {
                b.insert(0, '{');
                b.append('}');
            }

            sb.replace(m.start(), m.end(), b.toString());
            m = p.matcher(sb);
        }
        return sb.toString().replace('{', '<').replace('}', '>');
    }

    private boolean isGenericOnly(final String arg, final String type)
    {
        return arg.equals(type) || arg.equals("? super " + type) || arg.equals("? extends " + type);
    }

    private String rewriteLiterals(final TemplateFile f, String input, final TemplateOptions options)
    {
        final Type k = options.getKType();

        if (options.hasVType())
        {
            final Type v = options.getVType();

            input = input.replaceAll("(KTypeVType)([A-Z][a-zA-Z]*)(<.+?>)?",
                    (k.isGeneric() ? "Object" : k.getBoxedType()) +
                    (v.isGeneric() ? "Object" : v.getBoxedType()) +
                    "$2" +
                    (options.isAnyGeneric() ? "$3" : ""));

            input = input.replaceAll("(VType)([A-Z][a-zA-Z]*)",
                    (v.isGeneric() ? "Object" : v.getBoxedType()) +  "$2");

            if (!v.isGeneric())
                input = input.replaceAll("VType", v.getType());
        }

        input = input.replaceAll("(KType)([A-Z][a-zA-Z]*)(<.+?>)?",
                k.isGeneric() ? "Object" + "$2$3": k.getBoxedType() + "$2");

        if (!k.isGeneric())
            input = input.replaceAll("KType", k.getType());

        return input;
    }

    private void saveFile(final File file, final String input)
    {
        try
        {
            if (!file.getParentFile().exists())
                file.getParentFile().mkdirs();

            final FileOutputStream fos = new FileOutputStream(file);
            fos.write(input.getBytes("UTF-8"));
            fos.close();
        }
        catch (final IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Apply velocity to the input.
     */
    private String filterVelocity(final TemplateFile f, final String template, final TemplateOptions options)
    {
        final VelocityContext ctx = new VelocityContext();
        ctx.put("TemplateOptions", options);

        final StringWriter sw = new StringWriter();
        velocity.evaluate(ctx, sw, f.file.getName(), template);
        return sw.toString();
    }

    /**
     * 
     */
    private String readFile(final File file)
    {
        try
        {
            final byte [] contents = new byte [(int) file.length()];
            final DataInputStream dataInputStream = new DataInputStream(new FileInputStream(
                    file));
            dataInputStream.readFully(contents);
            dataInputStream.close();
            return new String(contents, "UTF-8");
        }
        catch (final Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private OutputFile findOrCreate(final String targetFileName, final List<OutputFile> outputs)
    {
        final File candidate = TemplateProcessor.canonicalFile(new File(this.outputDir, targetFileName));
        for (final OutputFile o : outputs)
        {
            if (o.file.equals(candidate))
            {
                o.generated = true;
                return o;
            }
        }

        final OutputFile o = new OutputFile(candidate, true);
        outputs.add(o);
        return o;
    }

    private String targetFileName(String relativePath, final TemplateOptions templateOptions)
    {
        if (templateOptions.hasVType()) relativePath = relativePath.replace("KTypeVType",
                templateOptions.getKType().getBoxedType()
                + templateOptions.getVType().getBoxedType());

        relativePath = relativePath.replace("KType", templateOptions.getKType()
                .getBoxedType());
        return relativePath;
    }

    /**
     * Relative path name.
     */
    private String relativePath(final File sub, final File parent)
    {
        try
        {
            return sub.getCanonicalPath().toString()
                    .substring(parent.getCanonicalPath().length());
        }
        catch (final IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Collect files present in the output.
     */
    private List<OutputFile> collectOutputFiles(final List<OutputFile> list, final File dir)
    {
        if (!dir.exists()) return list;

        for (final File file : dir.listFiles(new FilenameFilter()
        {
            @Override
            public boolean accept(final File dir, final String name)
            {
                final File f = new File(dir, name);
                if (f.isDirectory())
                {
                    collectOutputFiles(list, f);
                    return false;
                }

                return name.endsWith(".java");
            }
        }))
        {
            list.add(new OutputFile(file, false));
        }
        return list;
    }

    /**
     * Collect all template files from this and subdirectories.
     */
    private List<TemplateFile> collectTemplateFiles(final List<TemplateFile> list,
            final File dir)
            {
        for (final File file : dir.listFiles(new FilenameFilter()
        {
            @Override
            public boolean accept(final File dir, final String name)
            {
                final File f = new File(dir, name);
                if (f.isDirectory())
                {
                    collectTemplateFiles(list, f);
                    return false;
                }

                return name.endsWith(".java");
            }
        }))
        {
            list.add(new TemplateFile(file));
        }
        return list;
            }

    static File canonicalFile(final File target)
    {
        try
        {
            return target.getCanonicalFile();
        }
        catch (final IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    private static String getDefaultValue(final String typeName) {

        final String litteral = typeName.trim();

        String defaultValue = null;

        if (litteral.equals("byte")) {
            defaultValue = "(byte)0";
        } else if (litteral.equals( "char")) {
            defaultValue = "\'\\u0000\'";
        }else if (litteral.equals( "short")) {
            defaultValue = "(short)0";
        }else if (litteral.equals( "int")) {
            defaultValue = "0";
        }else if (litteral.equals( "long")) {
            defaultValue = "0L";
        }else if (litteral.equals( "float")) {
            defaultValue = "0f";
        }else if (litteral.equals( "double")) {
            defaultValue = "0.0D";
        }else if (litteral.equals( "boolean")) {
            defaultValue = "false";
        } else {
            defaultValue = "null";
        }

        return "(" + defaultValue  + ")";
    }

    /**
     * Command line entry point.
     */
    public static void main(final String [] args)
    {
        final TemplateProcessor processor = new TemplateProcessor();
        processor.setTemplatesDir(new File(args[0]));
        processor.setDestDir(new File(args[1]));
        processor.execute();
    }
}