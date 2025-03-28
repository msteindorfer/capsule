/*
 * Copyright (c) Michael Steindorfer <Centrum Wiskunde & Informatica> and Contributors.
 * All rights reserved.
 *
 * This file is licensed under the BSD 2-Clause License, which accompanies this project
 * and is available under https://opensource.org/licenses/BSD-2-Clause.
 */
package io.usethesource.capsule.jmh.impl.persistent.bifurcan;

import io.lacuna.bifurcan.List;
import io.usethesource.capsule.jmh.api.JmhList;
import io.usethesource.capsule.jmh.api.JmhValue;
import java.util.Iterator;

public final class BifurcanList implements JmhList {

  private final List<JmhValue> content;

  public BifurcanList(List<JmhValue> content) {
    this.content = content;
  }

  @Override
  public boolean isEmpty() {
    return content.size() == 0;
  }

  @Override
  public int size() {
    return Math.toIntExact(content.size());
  }

  @Override
  public boolean contains(JmhValue value) {
    return content.stream().anyMatch(it -> it.equals(value));
  }

  @Override
  public JmhList pushFront(JmhValue item) {
    return new BifurcanList(content.addFirst(item));
  }

  @Override
  public JmhList pushBack(JmhValue item) {
    return new BifurcanList(content.addLast(item));
  }

  @Override
  public JmhList concatenate(JmhList that) {
    return new BifurcanList((List<JmhValue>) content.concat((List<JmhValue>) that.unwrap()));
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

    if (other instanceof BifurcanList) {
      BifurcanList that = (BifurcanList) other;

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
