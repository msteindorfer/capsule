/*
 * Copyright (c) Michael Steindorfer <Centrum Wiskunde & Informatica> and Contributors.
 * All rights reserved.
 *
 * This file is licensed under the BSD 2-Clause License, which accompanies this project
 * and is available under https://opensource.org/licenses/BSD-2-Clause.
 */
package io.usethesource.capsule.jmh;

import io.usethesource.capsule.jmh.api.JmhList;
import io.usethesource.capsule.jmh.api.JmhValue;
import io.usethesource.capsule.jmh.api.JmhValueFactory;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class JmhListBenchmarks {

  @Param({"LIST"})
  public BenchmarkUtils.DataType dataType;

  @Param({"MATCH"})
  public BenchmarkUtils.SampleDataSelection sampleDataSelection;

  @Param({"VF_CAPSULE", "VF_SCALA", "VF_CLOJURE", "VF_PAGURO"})
  public BenchmarkUtils.ValueFactoryFactory valueFactoryFactory;

  /*
   * (for (i <- 0 to 23) yield s"'${Math.pow(2, i).toInt}'").mkString(", ").replace("'", "\"")
   */
  @Param({
    "1", "2", "4", "8", "16", "32", "64", "128", "256", "512", "1024", "2048", "4096", "8192",
    "16384", "32768", "65536", "131072", "262144", "524288", "1048576", "2097152", "4194304",
    "8388608"
  })
  protected int size;

  @Param({"0"}) // "1", "2", "3", "4", "5", "6", "7", "8", "9"
  protected int run;

  @Param public ElementProducer producer;

  public JmhValueFactory valueFactory;

  public JmhList testList;
  public JmhList testListRealDuplicate;

  public JmhValue VALUE_EXISTING;

  public static final int CACHED_NUMBERS_SIZE = 8;
  public JmhValue[] cachedNumbers = new JmhValue[CACHED_NUMBERS_SIZE];
  public JmhValue[] cachedNumbersNotContained = new JmhValue[CACHED_NUMBERS_SIZE];

  @Setup(Level.Trial)
  public void setUp() {
    setUpTestSetWithRandomContent(size, run);

    switch (sampleDataSelection) {

      /*
       * random integers might or might not be in the dataset
       */
      case RANDOM:
        {
          // random data generator with fixed seed
          /* seed == Mersenne Prime #8 */
          Random randForOperations = new Random(2147483647L);

          for (int i = 0; i < CACHED_NUMBERS_SIZE; i++) {
            cachedNumbers[i] = producer.createFromInt(randForOperations.nextInt());
          }
        }

      /*
       * random integers are in the dataset
       */
      case MATCH:
        {
          // random data generator with fixed seed
          int seedForThisTrial = BenchmarkUtils.seedFromSizeAndRun(size, run);
          Random rand = new Random(seedForThisTrial);

          for (int i = 0; i < CACHED_NUMBERS_SIZE; i++) {
            if (i >= size) {
              cachedNumbers[i] = cachedNumbers[i % size];
            } else {
              cachedNumbers[i] = producer.createFromInt(rand.nextInt());
            }
          }

          // random data generator with fixed seed
          /* seed == Mersenne Prime #8 */
          Random anotherRand = new Random(2147483647L);

          for (int i = 0; i < CACHED_NUMBERS_SIZE; i++) {
            /*
             * generate random values until a value not part of the data strucure is found
             */
            boolean found = false;
            while (!found) {
              final JmhValue candidate = producer.createFromInt(anotherRand.nextInt());

              if (testList.contains(candidate)) {
                continue;
              } else {
                cachedNumbersNotContained[i] = candidate;
                found = true;
              }
            }
          }

          // assert (contained)
          for (JmhValue sample : cachedNumbers) {
            if (!testList.contains(sample)) {
              throw new IllegalStateException();
            }
          }

          // assert (not contained)
          for (JmhValue sample : cachedNumbersNotContained) {
            if (testList.contains(sample)) {
              throw new IllegalStateException();
            }
          }
        }
    }
  }

  @SuppressWarnings("unused")
  public static final JmhList createTestObject(
      final JmhValueFactory valueFactory,
      final ElementProducer producer,
      final int size,
      final int run) {

    final JmhList.Builder builder = valueFactory.listBuilder();
    final Random rand = new Random(BenchmarkUtils.seedFromSizeAndRun(size, run) + 43);

    final int[] data = BenchmarkUtils.generateTestData(size, rand);

    for (int i = size - 1; i >= 0; i--) {
      builder.pushBack(producer.createFromInt(data[i]));
    }

    return builder.done();
  }

  protected void setUpTestSetWithRandomContent(int size, int run) {
    valueFactory = valueFactoryFactory.getInstance();

    JmhList.Builder writer1 = valueFactory.listBuilder();
    JmhList.Builder writer2 = valueFactory.listBuilder();

    int seedForThisTrial = BenchmarkUtils.seedFromSizeAndRun(size, run);
    Random rand = new Random(seedForThisTrial + 13);
    int existingValueIndex = rand.nextInt(size);

    int[] data = BenchmarkUtils.generateTestData(size, run);

    for (int i = size - 1; i >= 0; i--) {
      writer1.pushBack(producer.createFromInt(data[i]));
      writer2.pushBack(producer.createFromInt(data[i]));

      if (i == existingValueIndex) {
        VALUE_EXISTING = producer.createFromInt(data[i]);
      }
    }

    testList = writer1.done();
    testListRealDuplicate = writer2.done();
  }

  @Benchmark
  @OperationsPerInvocation(CACHED_NUMBERS_SIZE)
  public void timePushFront(Blackhole bh) {
    for (int i = 0; i < CACHED_NUMBERS_SIZE; i++) {
      bh.consume(testList.pushFront(cachedNumbers[i]));
    }
  }

  @Benchmark
  @OperationsPerInvocation(CACHED_NUMBERS_SIZE)
  public void timePushBack(Blackhole bh) {
    for (int i = 0; i < CACHED_NUMBERS_SIZE; i++) {
      bh.consume(testList.pushBack(cachedNumbers[i]));
    }
  }

  @Benchmark
  public void timeConcatenate(Blackhole bh) {
    bh.consume(testList.concatenate(testListRealDuplicate));
  }

  @Benchmark
  public void timeIteration(Blackhole bh) {
    for (JmhValue jmhValue : testList) {
      bh.consume(jmhValue);
    }
  }

  @Benchmark
  public void footprint(Blackhole bh) {
    bh.consume(testList); // no-op returning identity
  }
}
