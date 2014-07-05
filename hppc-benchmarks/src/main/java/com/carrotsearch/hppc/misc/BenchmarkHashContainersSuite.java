package com.carrotsearch.hppc.misc;

import java.lang.reflect.Method;
import java.util.List;

import com.carrotsearch.hppc.Util;
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

        Util.printSystemInfo("Benchmark suite for hash containers starting.");
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
            Util.printHeader(clz.getSimpleName() + " (" + (++i) + "/" + totalSize + ")");
            try {
                new Runner().run(ObjectArrays.concat(argsCaliper, clz.getName()));
            }
            catch (final Exception e) {

                System.out.println("Benchmark aborted with error: " + e);
            }
        }

        for (final Class<?> clz : otherClasses)
        {
            Util.printHeader(clz.getSimpleName() + " (" + (++i) + "/" + totalSize + ")");

            final Method method = clz.getDeclaredMethod("main", String[].class);

            method.invoke(null, new Object[] { argsOther });
        }
    }
}
