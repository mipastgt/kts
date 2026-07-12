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
package org.locationtech.jts.index.intervalrtree

import kotlin.jvm.JvmField

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.index.ItemVisitor
import org.locationtech.jts.io.WKTWriter

abstract class IntervalRTreeNode {
  @JvmField
  var min = Double.POSITIVE_INFINITY
  @JvmField
  var max = Double.NEGATIVE_INFINITY

  fun getMin(): Double = min
  fun getMax(): Double = max

  abstract fun query(queryMin: Double, queryMax: Double, visitor: ItemVisitor)

  protected fun intersects(queryMin: Double, queryMax: Double): Boolean {
    if (min > queryMax ||
        max < queryMin)
      return false
    return true
  }

  override fun toString(): String {
    return WKTWriter.toLineString(Coordinate(min, 0.0), Coordinate(max, 0.0))
  }

  class NodeComparator : Comparator<Any?> {
    override fun compare(o1: Any?, o2: Any?): Int {
      val n1 = o1 as IntervalRTreeNode
      val n2 = o2 as IntervalRTreeNode
      val mid1 = (n1.min + n1.max) / 2
      val mid2 = (n2.min + n2.max) / 2
      if (mid1 < mid2) return -1
      if (mid1 > mid2) return 1
      return 0
    }
  }
}
