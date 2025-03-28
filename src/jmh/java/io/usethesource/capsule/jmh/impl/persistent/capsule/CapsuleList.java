/**
 * Copyright (c) Michael Steindorfer <Centrum Wiskunde & Informatica> and Contributors.
 * All rights reserved.
 *
 * This file is licensed under the BSD 2-Clause License, which accompanies this project
 * and is available under https://opensource.org/licenses/BSD-2-Clause.
 */
package io.usethesource.capsule.jmh.impl.persistent.capsule;

import io.usethesource.capsule.Vector;
import io.usethesource.capsule.jmh.api.JmhList;
import io.usethesource.capsule.jmh.api.JmhValue;
import java.util.Iterator;

public final class CapsuleList implements JmhList {

  private final Vector.Immutable<JmhValue> content;

  public CapsuleList(Vector.Immutable<JmhValue> content) {
    this.content = content;
  }

  @Override
  public boolean isEmpty() {
    return content.isEmpty();
  }

  @Override
  public int size() {
    return content.size();
  }

  @Override
  public boolean contains(JmhValue element) {
    return content.contains(element);
  }

  @Override
  public JmhList pushFront(JmhValue item) {
    return new CapsuleList(content.pushFront(item));
  }

  @Override
  public JmhList pushBack(JmhValue item) {
    return new CapsuleList(content.pushBack(item));
  }

  @Override
  public JmhList concatenate(JmhList that) {
    return new CapsuleList(content.concatenate((Vector.Immutable<JmhValue>) that.unwrap()));
  }

  @Override
  public Iterator<JmhValue> iterator() {
    return content.iterator();
  }

  @Override
  public int hashCode() {
    return content.hashCode();
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) {
      return true;
    }
    if (other == null) {
      return false;
    }

    if (other instanceof CapsuleList) {
      CapsuleList that = (CapsuleList) other;

      if (this.size() != that.size()) {
        return false;
      }

      return content.equals(that.content);
    }

    return false;
  }

  @Override
  public Object unwrap() {
    return content;
  }
}
