// Copyright The Tuweni Authors
// SPDX-License-Identifier: Apache-2.0
package org.apache.tuweni.devp2p

import java.util.concurrent.atomic.AtomicLong
import kotlin.reflect.KProperty

// Extension methods that allow an AtomicLong to be treated as a Long property

internal operator fun AtomicLong.getValue(thisRef: Any?, property: KProperty<*>): Long = this.get()

internal operator fun AtomicLong.setValue(thisRef: Any?, property: KProperty<*>, value: Long) {
  this.set(value)
}

internal operator fun AtomicLong.inc(): AtomicLong {
  this.incrementAndGet()
  return this
}

internal operator fun AtomicLong.dec(): AtomicLong {
  this.decrementAndGet()
  return this
}
