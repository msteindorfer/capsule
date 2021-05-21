/**
 * Copyright (c) Michael Steindorfer <Centrum Wiskunde & Informatica> and Contributors.
 * All rights reserved.
 *
 * This file is licensed under the BSD 2-Clause License, which accompanies this project
 * and is available under https://opensource.org/licenses/BSD-2-Clause.
 */
package io.usethesource.capsule;

import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.generator.Size;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/*
 * NOTE: use e.g. @When(seed = 3666151076704776907L) to fix seed for reproducing test run.
 */
public abstract class AbstractVectorProperties<T, CT extends Vector.Immutable<T>> {

  private final int DEFAULT_TRIALS = 1_000;
  private final int MORE_TRIALS = 1_000;
  private final int LESS_TRIALS = 100;
  private final int MAX_SIZE = 50_000;
  private final Class<?> type;

  public AbstractVectorProperties(Class<?> type) {
    this.type = type;
  }

  @Property(trials = DEFAULT_TRIALS)
  public void containsAfterPushFront(@Size(min = 1, max = MAX_SIZE) final CT initialVector,
                                     @Size(min = 1, max = MAX_SIZE) final ArrayList<T> inputValues) {

    CT testVector = initialVector;

    for (T newValue : inputValues) {
      final CT tmpVector = (CT) testVector.pushFront(newValue);
      testVector = tmpVector;
    }

    final CT finalTestVector = testVector;
    assert finalTestVector.size() == initialVector.size() + inputValues.size();

    boolean containsInsertedValues = IntStream.range(0, inputValues.size())
        .allMatch(index -> {
          Optional<T> a = Optional.of(inputValues.get(index));
          Optional<T> b = finalTestVector.get(inputValues.size() - 1 - index);
          return Objects.equals(a, b);
        });

    assertTrue("Must contain all inserted values.", containsInsertedValues);
  }

  @Property(trials = DEFAULT_TRIALS)
  public void containsAfterPushBack(@Size(min = 1, max = MAX_SIZE) final CT initialVector,
      @Size(min = 1, max = MAX_SIZE) final ArrayList<T> inputValues) {

    CT testVector = initialVector;

    for (T newValue : inputValues) {
      final CT tmpVector = (CT) testVector.pushBack(newValue);
      testVector = tmpVector;
    }

    final CT finalTestVector = testVector;
    assert finalTestVector.size() == initialVector.size() + inputValues.size();

    boolean containsInsertedValues = IntStream.range(0, inputValues.size())
        .allMatch(index -> {
          Optional<T> a = Optional.of(inputValues.get(index));
          Optional<T> b = finalTestVector.get(initialVector.size() + index);
          return Objects.equals(a, b);
        });

    assertTrue("Must contain all inserted values.", containsInsertedValues);
  }

  @Property(trials = MORE_TRIALS)
  public void containsAfterConcatenate(
      @Size(min = 0, max = MAX_SIZE) final CT vectorOne,
      @Size(min = 0, max = MAX_SIZE) final CT vectorTwo) {

    final CT result = (CT) vectorOne.concatenate(vectorTwo);
    assert result.size() == vectorOne.size() + vectorTwo.size();

    boolean containsVectorOne = IntStream.range(0, vectorOne.size())
        .allMatch(index -> {
          Optional<T> a = vectorOne.get(index);
          Optional<T> b = result.get(index);
          return Objects.equals(a, b);
        });

    assertTrue("Must retain all original values.", containsVectorOne);

    boolean containsVectorTwo = IntStream.range(0, vectorTwo.size())
        .allMatch(index -> {
          Optional<T> a = vectorTwo.get(index);
          Optional<T> b = result.get(vectorOne.size() + index);
          return Objects.equals(a, b);
        });

    assertTrue("Must contain all newly inserted values.", containsVectorTwo);
  }

  @Property(trials = MORE_TRIALS)
  public void splitShuffleConcatenateRepeat(@Size(min = 0, max = MAX_SIZE) final CT inputVector) {
    int repetitions = 10;
    int approximateSegments = 4;

    CT resultVector = inputVector;

    for (int r = 0; r < repetitions; r++) {
      int randUpperBound = resultVector.size() / approximateSegments;
      Random rand = new Random();

      List<CT> segments = new ArrayList<>();

      CT remainderVector = resultVector;
      while (remainderVector.size() > 0) {
        int nextUpperBound = rand.nextInt(randUpperBound + 1);

        CT nextVector = (CT) remainderVector.take(nextUpperBound);
        segments.add(nextVector);

        remainderVector = (CT) remainderVector.drop(nextUpperBound);
      }

      Collections.shuffle(segments);
      resultVector = segments.stream().reduce((a, b) -> (CT) a.concatenate(b)).get();
    }

    assertEquals(inputVector.size(), resultVector.size());

    List<Integer> inputListSorted = new ArrayList<>();
    for (int i = 0; i < inputVector.size(); i++) inputListSorted.add((Integer) inputVector.get(i).get());
    Collections.sort(inputListSorted);

    List<Integer> resultListSorted = new ArrayList<>();
    for (int i = 0; i < inputVector.size(); i++) resultListSorted.add((Integer) resultVector.get(i).get());
    Collections.sort(resultListSorted);

    assertEquals(inputListSorted, resultListSorted);
  }

  @Property(trials = DEFAULT_TRIALS)
  public void insertAt(final CT vector, final int seed, final T newItem) {

    final int index = new Random(seed).nextInt(vector.size() + 1);
    final CT result = (CT) vector.insertAt(index, newItem);

    assertEquals(result.size(), vector.size() + 1);

    assertEquals(result.get(index).get(), newItem);

    boolean containsValuesBeforeIndex = IntStream.range(0, index)
        .allMatch(i -> {
          Optional<T> a = vector.get(i);
          Optional<T> b = result.get(i);
          return Objects.equals(a, b);
        });

    assertTrue("Must contain all inserted values.", containsValuesBeforeIndex);

    boolean containsValuesAfterIndex = IntStream.range(index, vector.size())
        .allMatch(i -> {
          Optional<T> a = vector.get(i);
          Optional<T> b = result.get(i + 1);
          return Objects.equals(a, b);
        });

    assertTrue("Must contain all inserted values.", containsValuesAfterIndex);
  }

  @Property(trials = DEFAULT_TRIALS)
  public void take(final CT vector, final int seed) {

    final int count = new Random(seed).nextInt(vector.size() + 1);
    final CT result = (CT) vector.take(count);

    assert result.size() == count;

    boolean containsInsertedValues = IntStream.range(0, count)
        .allMatch(index -> {
          Optional<T> a = vector.get(index);
          Optional<T> b = result.get(index);
          return Objects.equals(a, b);
        });

    assertTrue("Must contain all inserted values.", containsInsertedValues);
  }

  @Property(trials = DEFAULT_TRIALS)
  public void drop(final CT vector, final int seed) {

    final int count = new Random(seed).nextInt(vector.size() + 1);
    final CT result = (CT) vector.drop(count);

    assert result.size() == vector.size() - count;

    boolean containsInsertedValues = IntStream.range(0, result.size())
        .allMatch(index -> {
          Optional<T> a = vector.get(count + index);
          Optional<T> b = result.get(index);
          return Objects.equals(a, b);
        });

    assertTrue("Must contain all inserted values.", containsInsertedValues);
  }

  @Property(trials = DEFAULT_TRIALS)
  public void update(@Size(min = 1, max = MAX_SIZE) final CT vector, final int seed,
      final T updatedItem) {

    final int index = new Random(seed).nextInt(vector.size());
    final CT result = (CT) vector.update(index, updatedItem);

    assertEquals(vector.size(), result.size());
    assertEquals(updatedItem, result.get(index).get());

    boolean unmodifiedItemsEqual = IntStream.range(0, vector.size())
        .filter(i -> i != index)
        .allMatch(i -> {
          Optional<T> a = vector.get(i);
          Optional<T> b = result.get(i);
          return Objects.equals(a, b);
        });

    assertTrue("Eleemnts at indices (excepted the updated) must equal.", unmodifiedItemsEqual);
  }
}
