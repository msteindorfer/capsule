/**
 * Copyright (c) Michael Steindorfer <Centrum Wiskunde & Informatica> and Contributors.
 * All rights reserved.
 *
 * This file is licensed under the BSD 2-Clause License, which accompanies this project
 * and is available under https://opensource.org/licenses/BSD-2-Clause.
 */
package io.usethesource.capsule.jmh.impl.persistent.scala;

import static scala.collection.JavaConverters.asJavaIterator;

import io.usethesource.capsule.jmh.api.JmhList;
import io.usethesource.capsule.jmh.api.JmhValue;
import java.util.Iterator;
import scala.collection.immutable.Vector;

class ScalaList implements JmhList {

  protected final Vector<JmhValue> xs;

  protected ScalaList(Vector<JmhValue> xs) {
    this.xs = xs;
  }

  @SuppressWarnings("unchecked")
  @Override
  public Iterator<JmhValue> iterator() {
    return asJavaIterator(xs.iterator());
  }

  @Override
  public boolean isEmpty() {
    return size() == 0;
  }

  @Override
  public int size() {
    return xs.size();
  }

  @Override
  public boolean contains(JmhValue element) {
    return xs.contains(element);
  }

  @Override
  public JmhList pushFront(JmhValue item) {
    return new ScalaList((Vector<JmhValue>) xs.$plus$colon(item));
  }

  @Override
  public JmhList pushBack(JmhValue item) {
    return new ScalaList((Vector<JmhValue>) xs.$colon$plus(item));
  }

  @Override
  public JmhList concatenate(JmhList that) {
    return new ScalaList((Vector<JmhValue>) xs.$colon$plus$plus((Vector<JmhValue>) that.unwrap()));
  }

  @Override
  public int hashCode() {
    return xs.hashCode();
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) {
      return true;
    }
    if (other == null) {
      return false;
    }

    if (other instanceof ScalaList) {
      ScalaList that = (ScalaList) other;

      return xs.equals(that.xs);
    }

    return false;
  }

  @Override
  public Object unwrap() {
    return xs;
  }
}
