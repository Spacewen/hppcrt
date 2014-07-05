package com.carrotsearch.hppc.misc;

import java.io.File;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.TreeSet;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.ExecTask;

import com.carrotsearch.hppc.caliper.BenchmarkContainsWithRemoved;
import com.carrotsearch.hppc.caliper.BenchmarkPerturbedVsHashedOnly;
import com.carrotsearch.hppc.caliper.BenchmarkPut;
import com.carrotsearch.hppc.caliper.HashCollisionsCornerCaseTest;
import com.google.caliper.Benchmark;
import com.google.caliper.Runner;
import com.google.common.collect.Lists;
import com.google.common.collect.ObjectArrays;

/**
 * Runs the suite of benchmarks about hash containers
 */
public class BenchmarkHashContainersSuite
{
    @SuppressWarnings("unchecked")
    private final static Class<?>[] ALL_BENCHMARKS = new Class[]
            {
        BenchmarkPerturbedVsHashedOnly.class,
        HashCollisionsCornerCaseTest.class, BenchmarkPut.class, BenchmarkContainsWithRemoved.class,
        HppcMapSyntheticBench.class
            };

    public static void main(final String[] args) throws Exception
    {
        String fullArgString = "";

        for (final String strInd : args) {

            fullArgString += strInd + " ";
        }

        final String[] parsedArgs = fullArgString.split("---");

        //argument check
        if (parsedArgs.length != 3)
        {
            System.out.println("Args: --- <caliper benchmark args> --- <other benchmark args>");
            System.out.println("Known benchmark classes: ");

            for (final Class<?> clz : BenchmarkHashContainersSuite.ALL_BENCHMARKS)
            {
                System.out.println("\t" + clz.getName());
            }
            return;
        }

        //list of arguments
        String[] argsListCaliper = new String[] {};

        for (final String arg : parsedArgs[1].trim().split("\\s")) {

            if (!arg.trim().isEmpty()) {

                argsListCaliper = ObjectArrays.concat(argsListCaliper, arg.trim());
            }
        }

        String[] argsListOther = new String[] {};

        for (final String arg : parsedArgs[args.length - 1].trim().split("\\s")) {

            argsListOther = ObjectArrays.concat(argsListOther, arg.trim());
        }

        final List<Class<? extends Benchmark>> classesCaliper = Lists.newArrayList();
        final List<Class<?>> classesOther = Lists.newArrayList();

        //enumerate
        for (final Class<?> clz : BenchmarkHashContainersSuite.ALL_BENCHMARKS) {

            if (Benchmark.class.isAssignableFrom(clz))
            {
                classesCaliper.add((Class<? extends Benchmark>) clz);
            }
            else {
                classesOther.add(clz);
            }

        } //end for

        BenchmarkHashContainersSuite.printSystemInfo();
        BenchmarkHashContainersSuite.runBenchmarks(classesCaliper, argsListCaliper,
                classesOther, argsListOther);
    }

    /**
     * 
     */
    private static void runBenchmarks(final List<Class<? extends Benchmark>> caliperClasses, final String[] argsCaliper,
            final List<Class<?>> otherClasses, final String[] argsOther
            ) throws Exception
    {
        int i = 0;

        final int totalSize = caliperClasses.size() + otherClasses.size();

        for (final Class<? extends Benchmark> clz : caliperClasses)
        {
            BenchmarkHashContainersSuite.header(clz.getSimpleName() + " (" + (++i) + "/" + totalSize + ")");
            try {
                new Runner().run(ObjectArrays.concat(argsCaliper, clz.getName()));
            }
            catch (final Exception e) {

                System.out.println("Benchmark aborted with error: " + e);
            }
        }

        for (final Class<?> clz : otherClasses)
        {
            BenchmarkHashContainersSuite.header(clz.getSimpleName() + " (" + (++i) + "/" + totalSize + ")");

            final Method method = clz.getDeclaredMethod("main", String[].class);

            method.invoke(null, new Object[] { argsOther });
        }
    }

    /**
     * 
     */
    private static void printSystemInfo()
    {
        System.out.println("Benchmarks suite starting.");
        System.out.println("Date now: " + new Date() + "\n");

        BenchmarkHashContainersSuite.header("System properties");
        final Properties p = System.getProperties();
        for (final Object key : new TreeSet<Object>(p.keySet()))
        {
            System.out.println(key + ": "
                    + StringEscapeUtils.escapeJava((String) p.getProperty((String) key)));
        }

        BenchmarkHashContainersSuite.header("CPU");

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
