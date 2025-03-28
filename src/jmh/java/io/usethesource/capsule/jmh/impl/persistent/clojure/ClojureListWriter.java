/**
 * Copyright (c) Michael Steindorfer <Centrum Wiskunde & Informatica> and Contributors.
 * All rights reserved.
 *
 * This file is licensed under the BSD 2-Clause License, which accompanies this project
 * and is available under https://opensource.org/licenses/BSD-2-Clause.
 */
package io.usethesource.capsule.jmh.impl.persistent.clojure;

import clojure.lang.*;
import io.usethesource.capsule.jmh.api.JmhValue;
import io.usethesource.capsule.jmh.impl.AbstractListBuilder;

final class ClojureListWriter extends AbstractListBuilder<JmhValue, ITransientVector> {

  ClojureListWriter() {
    super(
        PersistentVector.EMPTY.asTransient(),
        list -> (item) -> (ITransientVector) list.conj(item),
        list -> new ClojureList((IPersistentVector) list.persistent()));
  }
}
