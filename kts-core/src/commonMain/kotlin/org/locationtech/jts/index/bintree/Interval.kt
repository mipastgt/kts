/*
 * Copyright (c) 2016 Vivid Solutions.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.locationtech.jts.index.bintree

import kotlin.jvm.JvmField

/**
 * Represents an (1-dimensional) closed interval on the Real number line.
 *
 */
class Interval {

  @JvmField
  var min: Double = 0.0
  @JvmField
  var max: Double = 0.0

  constructor() {
    min = 0.0
    max = 0.0
  }

  constructor(min: Double, max: Double) {
    init(min, max)
  }

  constructor(interval: Interval) {
    init(interval.min, interval.max)
  }

  fun init(min: Double, max: Double) {
    this.min = min
    this.max = max
    if (min > max) {
      this.min = max
      this.max = min
    }
  }

  fun getMin(): Double = min
  fun getMax(): Double = max
  fun getWidth(): Double = max - min

  fun expandToInclude(interval: Interval) {
    if (interval.max > max) max = interval.max
    if (interval.min < min) min = interval.min
  }

  fun overlaps(interval: Interval): Boolean {
    return overlaps(interval.min, interval.max)
  }

  fun overlaps(min: Double, max: Double): Boolean {
    if (this.min > max || this.max < min) return false
    return true
  }

  fun contains(interval: Interval): Boolean {
    return contains(interval.min, interval.max)
  }

  fun contains(min: Double, max: Double): Boolean {
    return (min >= this.min && max <= this.max)
  }

  fun contains(p: Double): Boolean {
    return (p >= this.min && p <= this.max)
  }

  override fun toString(): String {
    return "[" + min + ", " + max + "]"
  }
}
