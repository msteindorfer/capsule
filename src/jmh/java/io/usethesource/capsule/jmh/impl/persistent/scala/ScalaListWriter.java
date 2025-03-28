/**
 * Copyright (c) Michael Steindorfer <Centrum Wiskunde & Informatica> and Contributors.
 * All rights reserved.
 *
 * This file is licensed under the BSD 2-Clause License, which accompanies this project
 * and is available under https://opensource.org/licenses/BSD-2-Clause.
 */
package io.usethesource.capsule.jmh.impl.persistent.scala;

import io.usethesource.capsule.jmh.api.JmhValue;
import io.usethesource.capsule.jmh.impl.AbstractListBuilder;
import scala.collection.immutable.VectorBuilder;

final class ScalaListWriter extends AbstractListBuilder<JmhValue, VectorBuilder<JmhValue>> {

  ScalaListWriter() {
    super(
        new VectorBuilder<>(),
        list -> (item) -> (VectorBuilder) list.$plus$eq(item),
        list -> new ScalaList(list.result()));
  }
}
