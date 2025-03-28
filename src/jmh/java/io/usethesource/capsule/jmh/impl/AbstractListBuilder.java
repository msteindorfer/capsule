/**
 * Copyright (c) Michael Steindorfer <Centrum Wiskunde & Informatica> and Contributors.
 * All rights reserved.
 *
 * This file is licensed under the BSD 2-Clause License, which accompanies this project
 * and is available under https://opensource.org/licenses/BSD-2-Clause.
 */
package io.usethesource.capsule.jmh.impl;

import io.usethesource.capsule.jmh.api.JmhList;
import io.usethesource.capsule.jmh.api.JmhValue;
import java.util.Arrays;
import java.util.Iterator;
import java.util.function.Function;

public class AbstractListBuilder<C extends JmhValue, CC> implements JmhList.Builder {

  protected CC listContent;
  protected JmhList constructedList;

  final Function<CC, JmhList> functionWrap;
  final Function<CC, Function<JmhValue, CC>> methodInsert;

  public AbstractListBuilder(
      final CC empty,
      final Function<CC, Function<JmhValue, CC>> methodInsert,
      final Function<CC, JmhList> functionWrap) {

    this.listContent = empty;
    this.constructedList = null;

    this.methodInsert = methodInsert;
    this.functionWrap = functionWrap;
  }

  @Override
  public final void pushBack(JmhValue... items) {
    checkMutation();
    pushBackAll(Arrays.asList(items));
  }

  @Override
  public void pushBackAll(Iterable<? extends JmhValue> collection) {
    checkMutation();

    final Iterator<? extends JmhValue> iterator = collection.iterator();

    while (iterator.hasNext()) {
      final JmhValue item = iterator.next();
      listContent = methodInsert.apply(listContent).apply(item);
    }
  }

  private void checkMutation() {
    if (constructedList != null) {
      throw new UnsupportedOperationException("Mutation of a finalized builder is not supported.");
    }
  }

  @Override
  public final JmhList done() {
    if (constructedList == null) {
      constructedList = functionWrap.apply(listContent);
    }

    return constructedList;
  }
}
