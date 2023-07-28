/**
 * Copyright (c) Michael Steindorfer <Centrum Wiskunde & Informatica> and Contributors.
 * All rights reserved.
 *
 * This file is licensed under the BSD 2-Clause License, which accompanies this project
 * and is available under https://opensource.org/licenses/BSD-2-Clause.
 */
package io.usethesource.capsule;

import java.util.Optional;

public interface Vector<K> extends java.lang.Iterable<K> {

  int size();

  Optional<K> get(int index);

  interface Immutable<K> extends Vector<K> {

    Vector.Immutable<K> insertAt(int index, K item);

    Vector.Immutable<K> update(int index, K item);

    Vector.Immutable<K> pushFront(K item);

    Vector.Immutable<K> pushBack(K item);

    Vector.Immutable<K> take(int count);

    Vector.Immutable<K> drop(int count);

    Vector.Immutable<K> concatenate(Vector.Immutable<K> that);

    static <K> Vector.Immutable<K> of() {
      return io.usethesource.capsule.core.PersistentTrieVector.<K>of();
    }

    static <K> Vector.Immutable<K> of(K item) {
      return io.usethesource.capsule.core.PersistentTrieVector.<K>of().pushBack(item);
    }

    static <K> Vector.Immutable<K> of(K item0, K item1) {
      return io.usethesource.capsule.core.PersistentTrieVector.<K>of().pushBack(item0).pushBack(item1);
    }

    static <K> Vector.Immutable<K> of(K item0, K item1, K item2) {
      return io.usethesource.capsule.core.PersistentTrieVector.<K>of().pushBack(item0).pushBack(item1).pushBack(item2);
    }

    static <K> Vector.Immutable<K> of(K item0, K item1, K item2, K item3) {
      return io.usethesource.capsule.core.PersistentTrieVector.<K>of().pushBack(item0).pushBack(item1).pushBack(item2).pushBack(item3);
    }

    static <K> Vector.Immutable<K> of(K item0, K item1, K item2, K item3, K item4) {
      return io.usethesource.capsule.core.PersistentTrieVector.<K>of().pushBack(item0).pushBack(item1).pushBack(item2).pushBack(item3).pushBack(item4);
    }

  }

  interface Transient<K> extends Vector<K> {

  }

}
