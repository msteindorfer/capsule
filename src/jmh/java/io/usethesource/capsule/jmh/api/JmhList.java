/**
 * Copyright (c) Michael Steindorfer <Centrum Wiskunde & Informatica> and Contributors.
 * All rights reserved.
 *
 * This file is licensed under the BSD 2-Clause License, which accompanies this project
 * and is available under https://opensource.org/licenses/BSD-2-Clause.
 */
package io.usethesource.capsule.jmh.api;

import java.util.Iterator;

public interface JmhList extends JmhValue, Iterable<JmhValue> {

  boolean isEmpty();

  int size();

  boolean contains(JmhValue element);

  JmhList pushFront(JmhValue item);

  JmhList pushBack(JmhValue item);

  JmhList concatenate(JmhList that);

  @Override
  Iterator<JmhValue> iterator();

  interface Builder extends JmhBuilder {

    void pushBack(JmhValue... v);

    void pushBackAll(Iterable<? extends JmhValue> collection);

    @Override
    JmhList done();
  }
}
