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
package org.locationtech.jts.index.quadtree

import kotlin.jvm.JvmStatic
import kotlin.math.abs
import kotlin.math.max

/**
 * Provides a test for whether an interval is
 * so small it should be considered as zero for the purposes of
 * inserting it into a binary tree.
 *
 */
class IntervalSize {
  companion object {
    /**
     * This value is chosen to be a few powers of 2 less than the
     * number of bits available in the double representation (i.e. 53).
     */
    const val MIN_BINARY_EXPONENT = -50

    /**
     * Computes whether the interval [min, max] is effectively zero width.
     */
    @JvmStatic
    fun isZeroWidth(min: Double, max: Double): Boolean {
      val width = max - min
      if (width == 0.0) return true

      val maxAbs = max(abs(min), abs(max))
      val scaledInterval = width / maxAbs
      val level = DoubleBits.exponent(scaledInterval)
      return level <= MIN_BINARY_EXPONENT
    }
  }
}
