/**
 * Copyright (c) Michael Steindorfer <Centrum Wiskunde & Informatica> and Contributors.
 * All rights reserved.
 *
 * This file is licensed under the BSD 2-Clause License, which accompanies this project
 * and is available under https://opensource.org/licenses/BSD-2-Clause.
 */
package io.usethesource.capsule.jmh.impl.persistent.paguro;

import io.usethesource.capsule.jmh.api.JmhList;
import io.usethesource.capsule.jmh.api.JmhValue;
import java.util.Iterator;
import org.organicdesign.fp.collections.RrbTree;

public final class PaguroList implements JmhList {

  private final RrbTree<JmhValue> content;

  public PaguroList(RrbTree<JmhValue> content) {
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
    return new PaguroList(content.insert(0, item));
  }

  @Override
  public JmhList pushBack(JmhValue item) {
    return new PaguroList(content.append(item));
  }

  @Override
  public JmhList concatenate(JmhList that) {
    return new PaguroList(content.join((RrbTree<JmhValue>) that.unwrap()));
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

    if (other instanceof PaguroList) {
      PaguroList that = (PaguroList) other;

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
