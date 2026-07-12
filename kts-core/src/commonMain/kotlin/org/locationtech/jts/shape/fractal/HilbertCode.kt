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
 * Encodes points as the index along finite planar Hilbert curves.
 *
 * @author Martin Davis
 *
 * @see HilbertCurveBuilder
 * @see MortonCode
 */
class HilbertCode {
  companion object {
    /**
     * The maximum curve level that can be represented.
     */
    const val MAX_LEVEL = 16

    /**
     * The number of points in the curve for the given level.
     * The number of points is 2<sup>2 * level</sup>.
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
     * The maximum ordinate is 2<sup>level</sup> - 1.
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
     * The level of the finite Hilbert curve which contains at least
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
     * Encodes a point (x,y)
     * in the range of the the Hilbert curve at a given level
     * as the index of the point along the curve.
     * The index will lie in the range [0, 2<sup>level + 1</sup>].
     *
     * @param level the level of the Hilbert curve
     * @param x the x ordinate of the point
     * @param y the y ordinate of the point
     * @return the index of the point along the Hilbert curve
     */
    @JvmStatic
    fun encode(level: Int, x: Int, y: Int): Int {
      // Fast Hilbert curve algorithm by http://threadlocalmutex.com/
      // Ported from C++ https://github.com/rawrunprotected/hilbert_curves (public
      // domain)

      val lvl = levelClamp(level)

      var xv = x shl (16 - lvl)
      var yv = y shl (16 - lvl)

      var a = (xv xor yv).toLong()
      var b = 0xFFFFL xor a
      var c = (0xFFFF xor (xv or yv)).toLong()
      var d = (xv and (yv xor 0xFFFF)).toLong()

      var A = a or (b shr 1)
      var B = (a shr 1) xor a
      var C = ((c shr 1) xor (b and (d shr 1))) xor c
      var D = ((a and (c shr 1)) xor (d shr 1)) xor d

      a = A
      b = B
      c = C
      d = D
      A = ((a and (a shr 2)) xor (b and (b shr 2)))
      B = ((a and (b shr 2)) xor (b and ((a xor b) shr 2)))
      C = C xor ((a and (c shr 2)) xor (b and (d shr 2)))
      D = D xor ((b and (c shr 2)) xor ((a xor b) and (d shr 2)))

      a = A
      b = B
      c = C
      d = D
      A = ((a and (a shr 4)) xor (b and (b shr 4)))
      B = ((a and (b shr 4)) xor (b and ((a xor b) shr 4)))
      C = C xor ((a and (c shr 4)) xor (b and (d shr 4)))
      D = D xor ((b and (c shr 4)) xor ((a xor b) and (d shr 4)))

      a = A
      b = B
      c = C
      d = D
      C = C xor ((a and (c shr 8)) xor (b and (d shr 8)))
      D = D xor ((b and (c shr 8)) xor ((a xor b) and (d shr 8)))

      a = C xor (C shr 1)
      b = D xor (D shr 1)

      var i0 = (xv xor yv).toLong()
      var i1 = b or (0xFFFFL xor (i0 or a))

      i0 = (i0 or (i0 shl 8)) and 0x00FF00FFL
      i0 = (i0 or (i0 shl 4)) and 0x0F0F0F0FL
      i0 = (i0 or (i0 shl 2)) and 0x33333333L
      i0 = (i0 or (i0 shl 1)) and 0x55555555L

      i1 = (i1 or (i1 shl 8)) and 0x00FF00FFL
      i1 = (i1 or (i1 shl 4)) and 0x0F0F0F0FL
      i1 = (i1 or (i1 shl 2)) and 0x33333333L
      i1 = (i1 or (i1 shl 1)) and 0x55555555L

      val index = ((i1 shl 1) or i0) shr (32 - 2 * lvl)
      return index.toInt()
    }

    /**
     * Clamps a level to the range valid for
     * the index algorithm used.
     *
     * @param level the level of a Hilbert curve
     * @return a valid level
     */
    private fun levelClamp(level: Int): Int {
      // clamp order to [1, 16]
      var lvl = if (level < 1) 1 else level
      lvl = if (lvl > MAX_LEVEL) MAX_LEVEL else lvl
      return lvl
    }

    /**
     * Computes the point on a Hilbert curve
     * of given level for a given code index.
     * The point ordinates will lie in the range [0, 2<sup>level</sup></i> - 1].
     *
     * @param level the Hilbert curve level
     * @param index the index of the point on the curve
     * @return the point on the Hilbert curve
     */
    @JvmStatic
    fun decode(level: Int, index: Int): Coordinate {
      checkLevel(level)
      val lvl = levelClamp(level)

      val idx = index shl (32 - 2 * lvl)

      val i0 = deinterleave(idx)
      val i1 = deinterleave(idx shr 1)

      val t0 = (i0 or i1) xor 0xFFFFL
      val t1 = i0 and i1

      val prefixT0 = prefixScan(t0)
      val prefixT1 = prefixScan(t1)

      val a = (((i0 xor 0xFFFFL) and prefixT1) or (i0 and prefixT0))

      val x = (a xor i1) shr (16 - lvl)
      val y = (a xor i0 xor i1) shr (16 - lvl)

      return Coordinate(x.toDouble(), y.toDouble())
    }

    private fun prefixScan(x0: Long): Long {
      var x = x0
      x = (x shr 8) xor x
      x = (x shr 4) xor x
      x = (x shr 2) xor x
      x = (x shr 1) xor x
      return x
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
