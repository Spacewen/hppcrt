package com.carrotsearch.hppc.caliper;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

import com.carrotsearch.hppc.Util;
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
        BenchmarkPopCnt.class, BenchmarkBigramCounting.class, BenchmarkPerturbedVsHashedOnly.class,
            HashCollisionsCornerCaseTest.class, BenchmarkPut.class, BenchmarkContainsWithRemoved.class
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

        Util.printSystemInfo("Benchmarks suite starting.");
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
            Util.printHeader(clz.getSimpleName() + " (" + (++i) + "/" + classes.size() + ")");
            new Runner().run(ObjectArrays.concat(args, clz.getName()));
        }
    }
}
