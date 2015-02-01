package com.carrotsearch.hppcrt.generator;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.UnknownFormatConversionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections.ExtendedProperties;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.log.LogChute;
import org.apache.velocity.tools.generic.ClassTool;
import org.apache.velocity.tools.generic.ContextTool;
import org.apache.velocity.tools.generic.ConversionTool;
import org.apache.velocity.tools.generic.EscapeTool;
import org.apache.velocity.tools.generic.FieldTool;
import org.apache.velocity.tools.generic.MathTool;
import org.apache.velocity.tools.generic.NumberTool;
import org.apache.velocity.tools.generic.RenderTool;

import com.carrotsearch.hppcrt.generator.TemplateOptions.DoNotGenerateTypeException;

/**
 * Template processor for HPPC-RT templates.
 */
public final class TemplateProcessor
{
    private final Pattern INTRINSICS_METHOD_CALL;

    private final Pattern IMPORT_DECLARATION;

    public boolean verbose = false;
    public boolean incremental = true;
    public File templatesDir;
    public File outputDir;

    private final VelocityEngine velocity;

    private class VelocityLogger implements LogChute
    {
        @Override
        public void init(final RuntimeServices rs) throws Exception {
            // nothing strange here
        }

        @Override
        public void log(final int level, final String message) {

            System.out.println("[VELOCITY]-" + level + " : " + message);
        }

        @Override
        public void log(final int level, final String message, final Throwable t) {

            System.out.println("[VELOCITY]-" + level + "-!!EXCEPTION!! : " + message + " , exception msg: " + t.getMessage());
        }

        @Override
        public boolean isLevelEnabled(final int level) {
            // let Velocity talk
            return true;
        }
    }

    /**
     * Initialize Velocity engines and Regex Patterns
     * and so on.
     */
    public TemplateProcessor()
    {
        final ExtendedProperties p = new ExtendedProperties();
        p.setProperty(RuntimeConstants.SET_NULL_ALLOWED, "false");

        //Attach a Velocity logger to see internal Velocity log messages on console
        p.setProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM, new VelocityLogger());

        final VelocityEngine ve = new VelocityEngine();
        ve.setExtendedProperties(p);

        this.velocity = ve;

        ve.init();

        //compile basic patterns
        this.INTRINSICS_METHOD_CALL = Pattern.compile("(Intrinsics.\\s*)(<[^>]+>\\s*)?([a-zA-Z]+)",
                Pattern.MULTILINE | Pattern.DOTALL);

        this.IMPORT_DECLARATION = Pattern.compile("(import\\s+[\\w.\\*\\s]*;)",
                Pattern.MULTILINE | Pattern.DOTALL);

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
                this.outputDir);

        // Collect template files in the input folder.
        final List<TemplateFile> inputs = collectTemplateFiles(new ArrayList<TemplateFile>(),
                this.templatesDir);

        // Process templates
        System.out.println("Processing " + inputs.size() + " templates to: " + this.outputDir.getPath());
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
                if (this.verbose) {
                    System.out.println("Deleted: " + f.file);
                }
                f.file.delete();
            }

            if (f.generated) {
                generated++;
            }

            if (f.updated)
            {
                updated++;
                if (this.verbose) {
                    System.out.println("Updated: "
                            + relativePath(f.file, this.outputDir));
                }
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
        //For each template file
        for (final TemplateFile f : inputs)
        {
            final String fileName = f.file.getName();

            //A) KType only specialization
            if (fileName.contains("KType") && !fileName.contains("VType"))
            {
                for (final Type t : Type.values())
                {
                    final TemplateOptions options = new TemplateOptions(t, null);
                    options.sourceFile = f.file;
                    generate(f, outputs, options);
                }
            }
            //B) (KType * VType) specializations
            else if (fileName.contains("KType") && fileName.contains("VType"))
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

        if (this.verbose) {
            System.out.println(
                    "Velocity: " + this.timeVelocity + "\n" +
                            "Intrinsics: " + this.timeIntrinsics + "\n" +
                            "TypeClassRefs: " + this.timeTypeClassRefs + "\n" +
                            "Comments: " + this.timeComments + "\n");
        }
    }

    /**
     * Apply templates.
     */
    private void generate(final TemplateFile f, final List<OutputFile> outputs,
            final TemplateOptions templateOptions)
    {
        final String targetFileName = targetFileName(relativePath(f.file, this.templatesDir), templateOptions);

        final OutputFile output = findOrCreate(targetFileName, outputs);

        if (!this.incremental || !output.file.exists()
                || output.file.lastModified() <= f.file.lastModified())
        {
            String input = readFile(f.file);
            long t1, t0 = System.currentTimeMillis();

            //0) Apply velocity : if TemplateOptions.isDoNotGenerateKType() or TemplateOptions.isDoNotGenerateVType() throw a
            //DoNotGenerateTypeException , do not generate the final file.
            try {

                input = filterVelocity(f, input, templateOptions);

                this.timeVelocity += (t1 = System.currentTimeMillis()) - t0;

                //1) Apply inlining
                input = filterInlines(f, input, templateOptions);

                //2) filter intrinsics last, low-level calls replacements
                input = filterIntrinsics(f, input, templateOptions);

                this.timeIntrinsics += (t0 = System.currentTimeMillis()) - t1;

                //3) convert signatures
                input = filterTypeClassRefs(f, input, templateOptions);
                this.timeTypeClassRefs += (t1 = System.currentTimeMillis()) - t0;

                //4) Filter comments
                input = filterComments(f, input, templateOptions);
                this.timeComments += (t0 = System.currentTimeMillis()) - t1;

                output.updated = true;
                saveFile(output.file, input);
            }
            catch (final ParseErrorException e) {

                System.out.println("[ERROR] : parsing template '" + f.fullPath +
                        "' with KType = " + templateOptions.getKType() + " and VType =  " +
                        (templateOptions.hasVType() ? templateOptions.getVType().toString() : "null") + " with error: '" + e.getMessage() + "'");

                //rethrow the beast to stop the thing dead.
                throw e;
            }
            catch (final ResourceNotFoundException e) {

                System.out.println("[ERROR] : resource not found for template '" + f.fullPath +
                        "' with KType = " + templateOptions.getKType() + " and VType =  " +
                        (templateOptions.hasVType() ? templateOptions.getVType().toString() : "null") + " with error: '" + e.getMessage() + "'");

                //rethrow the beast to stop the thing dead.
                throw e;
            }
            catch (final MethodInvocationException e) {

                if (e.getCause() instanceof DoNotGenerateTypeException) {

                    final DoNotGenerateTypeException doNotGenException = (DoNotGenerateTypeException) e.getCause();
                    //indeed remove the generated file
                    output.file.delete();
                    outputs.remove(output);

                    System.out.println("[INFO] : output from template '" + f.fullPath +
                            "' with KType = " + doNotGenException.currentKType + " and VType =  " +
                            doNotGenException.currentVType + " was bypassed...");
                }
                else {

                    System.out.println("[ERROR] : method invocation from template '" + f.fullPath +
                            "' with KType = " + templateOptions.getKType() + " and VType =  " +
                            (templateOptions.hasVType() ? templateOptions.getVType().toString() : "null") + " failed with error: '" + e.getMessage() + "'");

                    //rethrow the beast to stop the thing dead.
                    throw e;
                }

            } //end MethodInvocationException
        }
    }

    long timeVelocity, timeIntrinsics, timeTypeClassRefs, timeComments;

    private String filterIntrinsics(final TemplateFile f, String input,
            final TemplateOptions templateOptions)
    {
        final StringBuffer sb = new StringBuffer();

        while (true)
        {
            //0) First, check for imports
            Matcher m = this.IMPORT_DECLARATION.matcher(input);

            while (m.find())
            {
                //append what is before
                sb.append(input.substring(0, m.end()));

                //truncate after the found region
                input = input.substring(m.end());

                //restart search :
                m = this.IMPORT_DECLARATION.matcher(input);
            }

            //1) Now try to find real method calls
            m = this.INTRINSICS_METHOD_CALL.matcher(input);

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
                else if ("equalsKType".equals(method) || "equalsKTypeNotNull".equals(method) ||
                        "equalsVType".equals(method) || "equalsVTypeNotNull".equals(method))
                {
                    final boolean isK = "equalsKType".equals(method) || "equalsKTypeNotNull".equals(method);
                    final boolean isV = "equalsVType".equals(method) || "equalsVTypeNotNull".equals(method);

                    final boolean isDouble = (isK && templateOptions.ktype == Type.DOUBLE) || (isV && templateOptions.vtype == Type.DOUBLE);
                    final boolean isFloat = (isK && templateOptions.ktype == Type.FLOAT) || (isV && templateOptions.vtype == Type.FLOAT);

                    if ((templateOptions.isKTypeGeneric() && "equalsKType".equals(method)) ||
                            (templateOptions.isVTypeGeneric() && "equalsVType".equals(method)))
                    {
                        sb.append(String.format("((%1$s) == null ? (%2$s) == null : (%1$s).equals(%2$s))", params.toArray()));
                    }
                    else if ((templateOptions.isKTypeGeneric() && "equalsKTypeNotNull".equals(method)) ||
                            (templateOptions.isVTypeGeneric() && "equalsVTypeNotNull".equals(method)))
                    {
                        sb.append(String.format("((%1$s).equals(%2$s))", params.toArray()));
                    }
                    else if (isDouble)
                    {
                        sb.append(String.format("(Double.doubleToLongBits(%1$s) == Double.doubleToLongBits(%2$s))", params.toArray()));
                    }
                    else if (isFloat)
                    {
                        sb.append(String.format("(Float.floatToIntBits(%1$s) == Float.floatToIntBits(%2$s))", params.toArray()));
                    }
                    else //all other cases.
                    {
                        sb.append(String.format("((%1$s) == (%2$s))", params.toArray()));
                    }
                }
                else if ("equalsKTypeDefault".equals(method))
                {
                    sb.append(String.format("((%1$s) == " +
                            TemplateProcessor.getDefaultValue(templateOptions.getKType().getType()) + ")", params.toArray()));
                }
                else if ("compareKType".equals(method) || "compareKTypeUnchecked".equals(method))
                {
                    if (templateOptions.isKTypeGeneric() && "compareKType".equals(method))
                    {
                        sb.append(String.format("((%1$s).compareTo(%2$s))", params.toArray()));
                    }
                    else if (templateOptions.isKTypeGeneric() && "compareKTypeUnchecked".equals(method))
                    {
                        sb.append(String.format("(((Comparable<? super KType>)%1$s).compareTo(%2$s))", params.toArray()));
                    }
                    else if (templateOptions.ktype == Type.DOUBLE)
                    {
                        sb.append(String.format("(Double.compare(%1$s , %2$s))", params.toArray()));
                    }
                    else if (templateOptions.ktype == Type.FLOAT)
                    {
                        sb.append(String.format("(Float.compare(%1$s , %2$s))", params.toArray()));
                    }
                    else if (templateOptions.ktype == Type.BYTE)
                    {
                        sb.append(String.format("(Byte.compare(%1$s , %2$s))", params.toArray()));
                    }
                    else if (templateOptions.ktype == Type.CHAR)
                    {
                        sb.append(String.format("(Character.compare(%1$s , %2$s))", params.toArray()));
                    }
                    else if (templateOptions.ktype == Type.SHORT)
                    {
                        sb.append(String.format("(Short.compare(%1$s , %2$s))", params.toArray()));
                    }
                    else if (templateOptions.ktype == Type.INT)
                    {
                        sb.append(String.format("(Integer.compare(%1$s , %2$s))", params.toArray()));
                    }
                    else if (templateOptions.ktype == Type.LONG)
                    {
                        sb.append(String.format("(Long.compare(%1$s , %2$s))", params.toArray()));
                    }
                    else
                    {
                        //particular case for booleans
                        sb.append(String.format("((%1$s == %2$s) ? 0 : (%1$s ? 1 : -1))", params.toArray()));
                    }
                }
                else if ("isCompSupKType".equals(method) || "isCompSupKTypeUnchecked".equals(method))
                {
                    if (templateOptions.isKTypeGeneric() && "isCompSupKType".equals(method))
                    {
                        sb.append(String.format("((%1$s).compareTo(%2$s) > 0)", params.toArray()));
                    }
                    else if (templateOptions.isKTypeGeneric() && "isCompSupKTypeUnchecked".equals(method))
                    {
                        sb.append(String.format("(((Comparable<? super KType>)%1$s).compareTo(%2$s) > 0)", params.toArray()));
                    }
                    else if (templateOptions.ktype == Type.DOUBLE)
                    {
                        sb.append(String.format("(Double.compare(%1$s , %2$s) > 0)", params.toArray()));
                    }
                    else if (templateOptions.ktype == Type.FLOAT)
                    {
                        sb.append(String.format("(Float.compare(%1$s , %2$s) > 0)", params.toArray()));
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
                else if ("isCompInfKType".equals(method) || "isCompInfKTypeUnchecked".equals(method))
                {
                    if (templateOptions.isKTypeGeneric() && "isCompInfKType".equals(method))
                    {
                        sb.append(String.format("((%1$s).compareTo(%2$s) < 0)", params.toArray()));
                    }
                    else if (templateOptions.isKTypeGeneric() && "isCompInfKTypeUnchecked".equals(method))
                    {
                        sb.append(String.format("(((Comparable<? super KType>)%1$s).compareTo(%2$s) < 0)", params.toArray()));
                    }
                    else if (templateOptions.ktype == Type.DOUBLE)
                    {
                        sb.append(String.format("(Double.compare(%1$s , %2$s) < 0)", params.toArray()));
                    }
                    else if (templateOptions.ktype == Type.FLOAT)
                    {
                        sb.append(String.format("(Float.compare(%1$s , %2$s) < 0)", params.toArray()));
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
                else if ("isCompEqualKType".equals(method) || "isCompEqualKTypeUnchecked".equals(method))
                {
                    if (templateOptions.isKTypeGeneric() && "isCompEqualKType".equals(method))
                    {
                        sb.append(String.format("((%1$s).compareTo(%2$s) == 0)", params.toArray()));
                    }
                    else if (templateOptions.isKTypeGeneric() && "isCompEqualKTypeUnchecked".equals(method))
                    {
                        sb.append(String.format("(((Comparable<? super KType>)%1$s).compareTo(%2$s) == 0)", params.toArray()));
                    }
                    else if (templateOptions.ktype == Type.DOUBLE)
                    {
                        sb.append(String.format("(Double.doubleToLongBits(%1$s) == Double.doubleToLongBits(%2$s))", params.toArray()));
                    }
                    else if (templateOptions.ktype == Type.FLOAT)
                    {
                        sb.append(String.format("(Float.floatToIntBits(%1$s) == Float.floatToIntBits(%2$s))", params.toArray()));
                    }
                    else
                    {
                        sb.append(String.format("(%1$s == %2$s)", params.toArray()));
                    }
                }
                else
                {
                    throw new ParseErrorException("[ERROR] : Unrecognized Intrinsic call: " + method);
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

    private String filterInlines(final TemplateFile f, String input, final TemplateOptions templateOptions)
    {
        final StringBuffer sb = new StringBuffer();

        //Make 1 pass per call name of localInlinesMap
        for (final String callName : templateOptions.localInlinesMap.keySet()) {

            String bodyPattern = "";

            //Rebuild matchers by admitting spaces between "." (this . foo )
            final String[] splittedCallName = callName.split(".");

            String callNamePattern = callName;

            if (splittedCallName.length > 1) {

                callNamePattern = "";

                //Re-collapse without spaces
                for (int j = 0; j < splittedCallName.length - 1; j++) {

                    splittedCallName[j].trim();
                    callNamePattern += splittedCallName[j] + ".";
                }

                callNamePattern += splittedCallName[splittedCallName.length - 1].trim();
            } //end if splittedCallName.length > 1

            final Pattern p = Pattern.compile(callNamePattern, Pattern.MULTILINE | Pattern.DOTALL);

            //flush
            sb.setLength(0);

            while (true)
            {
                final Matcher m = p.matcher(input);

                //end if found matcher
                if (m.find())
                {
                    sb.append(input.substring(0, m.start()));

                    //parsing of the arguments
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

                    //fill-in the arguments depending of the type
                    bodyPattern = "";

                    if (templateOptions.ktype == Type.GENERIC) {

                        bodyPattern = templateOptions.localInlinesMap.get(callName).genericBody;
                    }
                    else if (templateOptions.ktype == Type.LONG) {

                        bodyPattern = templateOptions.localInlinesMap.get(callName).longBody;
                    }
                    else if (templateOptions.ktype == Type.BOOLEAN) {

                        bodyPattern = templateOptions.localInlinesMap.get(callName).booleanBody;
                    }
                    else if (templateOptions.ktype == Type.FLOAT) {

                        bodyPattern = templateOptions.localInlinesMap.get(callName).floatBody;
                    }
                    else if (templateOptions.ktype == Type.DOUBLE) {

                        bodyPattern = templateOptions.localInlinesMap.get(callName).doubleBody;
                    }
                    else {
                        //all other integers : int, short, char, byte
                        bodyPattern = templateOptions.localInlinesMap.get(callName).integerBody;
                    }

                    //apply replacement, if not null
                    if (!bodyPattern.isEmpty()) {

                        try {

                            sb.append(String.format("(" + bodyPattern + ")", params.toArray()));

                        }
                        catch (final UnknownFormatConversionException e) {

                            throw new ParseErrorException("[ERROR] : filterInlines() unable to apply body '" + bodyPattern + "' with params " + params.toString());
                        }
                    }

                } //else m.find()
                else
                {
                    //not found, append the contents directly
                    sb.append(input);
                    break;
                }
            } //end while true

            //re-process the same input for each method call
            input = sb.toString();

        } //end for call names

        return input.toString();
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
                    case '<':
                        bracketCount++;
                        break;
                    case '>':
                        bracketCount--;
                        break;
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
        if (!signature.contains("KType") && !signature.contains("VType")) {
            return signature;
        }

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
                    if (options.hasKType() && isGenericOnly(arg, "KType")) {

                        arg = "";
                    }
                    else {
                        arg = arg.replace("KType", options.getKType().getBoxedType());
                    }
                }

                if (options.hasVType() && options.isVTypePrimitive())
                {
                    if (isGenericOnly(arg, "VType")) {
                        arg = "";
                    }
                    else {
                        arg = arg.replace("VType", options.getVType().getBoxedType());
                    }
                }

                if (arg.length() > 0)
                {
                    if (b.length() > 0) {
                        b.append(", ");
                    }
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
                    (v.isGeneric() ? "Object" : v.getBoxedType()) + "$2");

            if (!v.isGeneric()) {
                input = input.replaceAll("VType", v.getType());
            }
        }

        input = input.replaceAll("(KType)([A-Z][a-zA-Z]*)(<.+?>)?",
                k.isGeneric() ? "Object" + "$2$3" : k.getBoxedType() + "$2");

        if (!k.isGeneric()) {
            input = input.replaceAll("KType", k.getType());
        }

        return input;
    }

    private void saveFile(final File file, final String input)
    {
        try
        {
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }

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

        //Attach some GenericTools that may be useful for code generation :
        ctx.put("esct", new EscapeTool());
        ctx.put("classt", new ClassTool());
        ctx.put("contextt", new ContextTool());
        ctx.put("convt", new ConversionTool());
        ctx.put("fieldt", new FieldTool());
        ctx.put("matht", new MathTool());
        ctx.put("numbert", new NumberTool());
        ctx.put("rendert", new RenderTool());

        final StringWriter sw = new StringWriter();

        //reference the context itself into the TemplateOptions object
        options.context = ctx;

        this.velocity.evaluate(ctx, sw, f.file.getName(), template);

        return sw.toString();
    }

    /**
     * 
     */
    private String readFile(final File file)
    {
        try
        {
            final byte[] contents = new byte[(int) file.length()];
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
        if (templateOptions.hasKType()) {

            relativePath = relativePath.replace("KType", templateOptions.getKType().getBoxedType());
        }

        if (templateOptions.hasVType()) {

            relativePath = relativePath.replace("VType", templateOptions.getVType().getBoxedType());
        }

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
        if (!dir.exists()) {
            return list;
        }

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
    private List<TemplateFile> collectTemplateFiles(final List<TemplateFile> list, final File dir)
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
        }
        else if (litteral.equals("char")) {
            defaultValue = "\'\\u0000\'";
        }
        else if (litteral.equals("short")) {
            defaultValue = "(short)0";
        }
        else if (litteral.equals("int")) {
            defaultValue = "0";
        }
        else if (litteral.equals("long")) {
            defaultValue = "0L";
        }
        else if (litteral.equals("float")) {
            defaultValue = "0f";
        }
        else if (litteral.equals("double")) {
            defaultValue = "0.0D";
        }
        else if (litteral.equals("boolean")) {
            defaultValue = "false";
        }
        else {
            defaultValue = "null";
        }

        return "(" + defaultValue + ")";
    }

    /**
     * Command line entry point.
     */
    public static void main(final String[] args)
    {
        final TemplateProcessor processor = new TemplateProcessor();
        processor.setTemplatesDir(new File(args[0]));
        processor.setDestDir(new File(args[1]));
        processor.execute();
    }
}