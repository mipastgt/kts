/*
 * Copyright (c) 2019 Martin Davis.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.locationtech.jts.shape.fractal

import kotlin.jvm.JvmStatic
import kotlin.math.ln
import kotlin.math.pow

import org.locationtech.jts.geom.Coordinate

/**
 * Encodes points as the index along the planar Morton (Z-order) curve.
 *
 * @author Martin Davis
 *
 * @see MortonCurveBuilder
 * @see HilbertCode
 */
class MortonCode {
  companion object {
    /**
     * The maximum curve level that can be represented.
     */
    const val MAX_LEVEL = 16

    /**
     * The number of points in the curve for the given level.
     * The number of points is 2^(2 * level).
     *
     * @param level the level of the curve
     * @return the number of points
     */
    @JvmStatic
    fun size(level: Int): Int {
      checkLevel(level)
      return (2.0).pow((2 * level).toDouble()).toInt()
    }

    /**
     * The maximum ordinate value for points
     * in the curve for the given level.
     * The maximum ordinate is 2^level - 1.
     *
     * @param level the level of the curve
     * @return the maximum ordinate value
     */
    @JvmStatic
    fun maxOrdinate(level: Int): Int {
      checkLevel(level)
      return (2.0).pow(level.toDouble()).toInt() - 1
    }

    /**
     * The level of the finite Morton curve which contains at least
     * the given number of points.
     *
     * @param numPoints the number of points required
     * @return the level of the curve
     */
    @JvmStatic
    fun level(numPoints: Int): Int {
      val pow2 = ((ln(numPoints.toDouble()) / ln(2.0))).toInt()
      var level = pow2 / 2
      val size = size(level)
      if (size < numPoints) level += 1
      return level
    }

    private fun checkLevel(level: Int) {
      if (level > MAX_LEVEL) {
        throw IllegalArgumentException("Level must be in range 0 to $MAX_LEVEL")
      }
    }

    /**
     * Computes the index of the point (x,y)
     * in the Morton curve ordering.
     *
     * @param x the x ordinate of the point
     * @param y the y ordinate of the point
     * @return the index of the point along the Morton curve
     */
    @JvmStatic
    fun encode(x: Int, y: Int): Int {
      return (interleave(y) shl 1) + interleave(x)
    }

    private fun interleave(x0: Int): Int {
      var x = x0
      x = x and 0x0000ffff                    // x = ---- ---- ---- ---- fedc ba98 7654 3210
      x = (x xor (x shl 8)) and 0x00ff00ff     // x = ---- ---- fedc ba98 ---- ---- 7654 3210
      x = (x xor (x shl 4)) and 0x0f0f0f0f     // x = ---- fedc ---- ba98 ---- 7654 ---- 3210
      x = (x xor (x shl 2)) and 0x33333333     // x = --fe --dc --ba --98 --76 --54 --32 --10
      x = (x xor (x shl 1)) and 0x55555555     // x = -f-e -d-c -b-a -9-8 -7-6 -5-4 -3-2 -1-0
      return x
    }

    /**
     * Computes the point on the Morton curve
     * for a given index.
     *
     * @param index the index of the point on the curve
     * @return the point on the curve
     */
    @JvmStatic
    fun decode(index: Int): Coordinate {
      val x = deinterleave(index)
      val y = deinterleave(index shr 1)
      return Coordinate(x.toDouble(), y.toDouble())
    }

    private fun deinterleave(x0: Int): Long {
      var x = x0 and 0x55555555
      x = (x or (x shr 1)) and 0x33333333
      x = (x or (x shr 2)) and 0x0F0F0F0F
      x = (x or (x shr 4)) and 0x00FF00FF
      x = (x or (x shr 8)) and 0x0000FFFF
      return x.toLong()
    }
  }
}
