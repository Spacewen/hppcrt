package com.carrotsearch.hppc.caliper;

import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.regex.*;
import java.io.File;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.*;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.ExecTask;

import com.google.caliper.Benchmark;
import com.google.caliper.Runner;
import com.google.common.collect.Lists;
import com.google.common.collect.ObjectArrays;

/**
 * Runs the entire suite of benchmarks.
 */
public class BenchmarkSuite
{
    @SuppressWarnings("unchecked")
    private final static Class<? extends Benchmark>[] ALL_BENCHMARKS = new Class[]
            {
            BenchmarkBigramCounting.class, BenchmarkContainsWithRemoved.class, BenchmarkPut.class
            };

    public static void main(final String[] args) throws Exception
    {
        if (args.length == 0)
        {
            System.out.println("Args: [all | class-name, class-name, ...] [-- <benchmark args>]");
            System.out.println("Known benchmark classes: ");
            for (final Class<?> clz : BenchmarkSuite.ALL_BENCHMARKS)
            {
                System.out.println("\t" + clz.getName());
            }
            return;
        }

        final Deque<String> argsList = new ArrayDeque<String>(Arrays.asList(args));
        final List<Class<? extends Benchmark>> classes = Lists.newArrayList();
        while (!argsList.isEmpty())
        {
            if ("--".equals(argsList.peekFirst()))
            {
                argsList.removeFirst();
                break;
            }
            else if ("all".equals(argsList.peekFirst()))
            {
                argsList.removeFirst();
                classes.addAll(Arrays.asList(BenchmarkSuite.ALL_BENCHMARKS));
            }
            else
            {
                final ClassLoader clLoader = Thread.currentThread()
                        .getContextClassLoader();

                final String clz = argsList.removeFirst();
                try
                {
                    @SuppressWarnings("unchecked")
                    final Class<? extends Benchmark> clzInstance =
                            (Class<? extends Benchmark>) Class.forName(clz, true, clLoader);

                    if (!Benchmark.class.isAssignableFrom(clzInstance))
                    {
                        System.out.println("Not a benchmark class: " + clz);
                        System.exit(-1);
                    }

                    classes.add(clzInstance);
                }
                catch (final ClassNotFoundException e)
                {
                    System.out.println("Class not found: " + clz);
                    System.exit(-1);
                }
            }
        }

        BenchmarkSuite.printSystemInfo();
        BenchmarkSuite.runBenchmarks(classes, argsList.toArray(new String[argsList.size()]));
    }

    /**
     * 
     */
    private static void runBenchmarks(final List<Class<? extends Benchmark>> classes,
            final String[] args) throws Exception
    {
        int i = 0;
        for (final Class<? extends Benchmark> clz : classes)
        {
            BenchmarkSuite.header(clz.getSimpleName() + " (" + (++i) + "/" + classes.size() + ")");
            new Runner().run(ObjectArrays.concat(args, clz.getName()));
        }
    }

    /**
     * 
     */
    private static void printSystemInfo()
    {
        System.out.println("Benchmarks suite starting.");
        System.out.println("Date now: " + new Date() + "\n");

        BenchmarkSuite.header("System properties");
        final Properties p = System.getProperties();
        for (final Object key : new TreeSet<Object>(p.keySet()))
        {
            System.out.println(key + ": "
                    + StringEscapeUtils.escapeJava((String) p.getProperty((String) key)));
        }

        BenchmarkSuite.header("CPU");

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

    private static void header(final String msg)
    {
        System.out.println();
        System.out.println(StringUtils.repeat("=", 80));
        System.out.println(StringUtils.center(" " + msg + " ", 80, "-"));
        System.out.println(StringUtils.repeat("=", 80));
        System.out.flush();
    }
}
