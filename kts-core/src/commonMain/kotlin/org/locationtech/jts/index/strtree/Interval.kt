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
package org.locationtech.jts.index.strtree
import kotlin.math.max
import kotlin.math.min

import org.locationtech.jts.util.Assert

/**
 * A contiguous portion of 1D-space. Used internally by SIRtree.
 * @see SIRtree
 *
 * @version 1.7
 */
class Interval(min: Double, max: Double) {

  private var min: Double
  private var max: Double

  init {
    Assert.isTrue(min <= max)
    this.min = min
    this.max = max
  }

  constructor(other: Interval) : this(other.min, other.max)

  fun getCentre(): Double {
    return (min + max) / 2
  }

  /**
   * @return this
   */
  fun expandToInclude(other: Interval): Interval {
    max = max(max, other.max)
    min = min(min, other.min)
    return this
  }

  fun intersects(other: Interval): Boolean {
    return !(other.min > max || other.max < min)
  }

  override fun equals(o: Any?): Boolean {
    if (o !is Interval) {
      return false
    }
    return min == o.min && max == o.max
  }

  override fun hashCode(): Int {
    val prime = 31
    var result = 1
    var temp: Long
    temp = max.toBits()
    result = prime * result + (temp xor (temp ushr 32)).toInt()
    temp = min.toBits()
    result = prime * result + (temp xor (temp ushr 32)).toInt()
    return result
  }
}
