/**
 * Copyright (c) Michael Steindorfer <Centrum Wiskunde & Informatica> and Contributors.
 * All rights reserved.
 *
 * This file is licensed under the BSD 2-Clause License, which accompanies this project
 * and is available under https://opensource.org/licenses/BSD-2-Clause.
 */
package io.usethesource.capsule.jmh.impl.persistent.clojure;

import clojure.*;
import clojure.lang.IFn;
import clojure.lang.IPersistentCollection;
import clojure.lang.IPersistentVector;
import clojure.lang.ISeq;
import clojure.lang.PersistentVector;
import io.usethesource.capsule.jmh.api.JmhList;
import io.usethesource.capsule.jmh.api.JmhValue;
import java.util.Iterator;

class ClojureList implements JmhList {

  protected final IPersistentVector xs;

  protected ClojureList(IPersistentVector xs) {
    this.xs = xs;
  }

  protected ClojureList() {
    this(PersistentVector.EMPTY);
  }

  protected ClojureList(JmhValue... values) {
    this(PersistentVector.create((Object[]) values));
  }

  @SuppressWarnings("unchecked")
  @Override
  public Iterator<JmhValue> iterator() {
    return ((Iterable<JmhValue>) xs).iterator();
  }

  @Override
  public boolean isEmpty() {
    return size() == 0;
  }

  @Override
  public int size() {
    return xs.count();
  }

  @Override
  public boolean contains(JmhValue element) {
    for (JmhValue candidate : this) {
      if (candidate.equals(element)) {
        return true;
      }
    }
    return false;
  }

  /*
   * ```clojure
   * $ (vec (cons 0 (vector 1 2 3)))
   *
   * [0 1 2 3]
   * ```
   */
  @Override
  public JmhList pushFront(JmhValue item) {
    return new ClojureList(ClojureHelper.core$vec(ClojureHelper.core$cons(item, xs)));
  }

  /*
   * ```clojure
   * $ (conj (vector 1 2 3) 4)
   *
   * [1 2 3 4]
   * ```
   */
  @Override
  public JmhList pushBack(JmhValue item) {
    return new ClojureList(ClojureHelper.core$vec(ClojureHelper.core$conj(xs, item)));
  }

  @Override
  public JmhList concatenate(JmhList that) {
    return new ClojureList(
        PersistentVector.create(ClojureHelper.core$concat(xs, (IPersistentVector) that.unwrap())));
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

    if (other instanceof ClojureList) {
      ClojureList that = (ClojureList) other;

      return xs.equals(that.xs);
    }

    return false;
  }

  @Override
  public Object unwrap() {
    return xs;
  }
}

class ClojureHelper {

  public static final IFn core$concat = new core$concat();

  public static final ISeq core$concat(IPersistentCollection xs, IPersistentCollection ys) {
    return (ISeq) core$concat.invoke(xs, ys);
  }

  public static final IFn core$conj = new core$conj__5474();

  public static final IPersistentCollection core$conj(IPersistentCollection xs, Object x) {
    return (IPersistentCollection) core$conj.invoke(xs, x);
  }

  public static final IFn core$cons = new core$cons__5460();

  public static final ISeq core$cons(Object x, IPersistentCollection xs) {
    return (ISeq) core$cons.invoke(x, xs);
  }

  public static final IFn core$vec = new core$vec();

  public static final IPersistentVector core$vec(IPersistentCollection xs) {
    return (IPersistentVector) core$vec.invoke(xs);
  }
}
