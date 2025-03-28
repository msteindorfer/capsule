/**
 * Copyright (c) Michael Steindorfer <Centrum Wiskunde & Informatica> and Contributors.
 * All rights reserved.
 *
 * This file is licensed under the BSD 2-Clause License, which accompanies this project
 * and is available under https://opensource.org/licenses/BSD-2-Clause.
 */
package io.usethesource.capsule.jmh.impl.persistent.capsule;

import io.usethesource.capsule.Vector;
import io.usethesource.capsule.jmh.api.JmhValue;
import io.usethesource.capsule.jmh.impl.AbstractListBuilder;

final class CapsuleListBuilder extends AbstractListBuilder<JmhValue, Vector.Immutable<JmhValue>> {

  CapsuleListBuilder() {
    super(Vector.Immutable.of(), vector -> vector::pushBack, CapsuleList::new);
  }
}
