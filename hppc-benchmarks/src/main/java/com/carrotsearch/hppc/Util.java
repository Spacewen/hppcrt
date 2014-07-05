package com.carrotsearch.hppc;

import java.io.File;
import java.io.StringReader;
import java.util.Date;
import java.util.Properties;
import java.util.Random;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.ExecTask;

public class Util
{

    /**
     * shuffle array contents
     * @param array
     * @param rnd
     * @return
     */
    public static int[] shuffle(final int[] array, final Random rnd)
    {
        for (int i = array.length - 1; i > 0; i--)
        {
            final int pos = rnd.nextInt(i + 1);
            final int t = array[pos];
            array[pos] = array[i];
            array[i] = t;
        }
        return array;
    }

    /**
     * 
     */
    public static void printSystemInfo(final String title)
    {
        System.out.println(title);
        System.out.println("Date now: " + new Date() + "\n");

        Util.printHeader("System properties");
        final Properties p = System.getProperties();
        for (final Object key : new TreeSet<Object>(p.keySet()))
        {
            System.out.println(key + ": "
                    + StringEscapeUtils.escapeJava((String) p.getProperty((String) key)));
        }

        Util.printHeader("CPU");

        // Try to determine CPU.
        final ExecTask task = new ExecTask();
        task.setVMLauncher(true);
        task.setOutputproperty("stdout");
        task.setErrorProperty("stderr");

        task.setFailIfExecutionFails(true);
        task.setFailonerror(true);

        final Project project = new Project();
        task.setProject(project);

        String pattern = ".*";
        if (SystemUtils.IS_OS_WINDOWS)
        {
            task.setExecutable("cmd");
            task.createArg().setLine("/c set");
            pattern = "PROCESSOR";
        }
        else
        {
            if (new File("/proc/cpuinfo").exists())
            {
                task.setExecutable("cat");
                task.createArg().setLine("/proc/cpuinfo");
            }
            else
            {
                task.setExecutable("sysctl");
                task.createArg().setLine("-a");
                pattern = "(kern\\..*)|(hw\\..*)|(machdep\\..*)";
            }
        }

        try
        {
            task.execute();

            final String property = project.getProperty("stdout");
            // Restrict to processor related data only.
            final Pattern patt = Pattern.compile(pattern);
            for (final String line : IOUtils.readLines(new StringReader(property)))
            {
                if (patt.matcher(line).find())
                {
                    System.out.println(line);
                }
            }
        }
        catch (final Throwable e)
        {
            System.out.println("WARN: CPU information could not be extracted: "
                    + e.getMessage());
        }
    }

    public static void printHeader(final String msg)
    {
        System.out.println();
        System.out.println(StringUtils.repeat("=", 80));
        System.out.println(StringUtils.center(" " + msg + " ", 80, "-"));
        System.out.println(StringUtils.repeat("=", 80));
        System.out.flush();
    }

}
