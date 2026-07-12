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
package org.locationtech.jts.noding

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.CoordinateArrays

/**
 * Allows comparing [Coordinate] arrays
 * in an orientation-independent way.
 *
 * @author Martin Davis
 * @version 1.7
 */
class OrientedCoordinateArray
/**
 * Creates a new [OrientedCoordinateArray]
 * for the given [Coordinate] array.
 *
 * @param pts the coordinates to orient
 */
  (private val pts: Array<Coordinate>) : Comparable<Any?> {

  private val orientation: Boolean = orientation(pts)

  /**
   * Compares two [OrientedCoordinateArray]s for their relative order
   *
   * @return -1 this one is smaller;
   * 0 the two objects are equal;
   * 1 this one is greater
   */
  override fun compareTo(o1: Any?): Int {
    val oca = o1 as OrientedCoordinateArray
    val comp = compareOriented(pts, orientation, oca.pts, oca.orientation)
    return comp
  }

  companion object {
    /**
     * Computes the canonical orientation for a coordinate array.
     *
     * @param pts the array to test
     * @return `true` if the points are oriented forwards
     * or `false` if the points are oriented in reverse
     */
    private fun orientation(pts: Array<Coordinate>): Boolean {
      return CoordinateArrays.increasingDirection(pts) == 1
    }

    private fun compareOriented(
      pts1: Array<Coordinate>,
      orientation1: Boolean,
      pts2: Array<Coordinate>,
      orientation2: Boolean
    ): Int {
      val dir1 = if (orientation1) 1 else -1
      val dir2 = if (orientation2) 1 else -1
      val limit1 = if (orientation1) pts1.size else -1
      val limit2 = if (orientation2) pts2.size else -1

      var i1 = if (orientation1) 0 else pts1.size - 1
      var i2 = if (orientation2) 0 else pts2.size - 1
      while (true) {
        val compPt = pts1[i1].compareTo(pts2[i2])
        if (compPt != 0)
          return compPt
        i1 += dir1
        i2 += dir2
        val done1 = i1 == limit1
        val done2 = i2 == limit2
        if (done1 && !done2) return -1
        if (!done1 && done2) return 1
        if (done1 && done2) return 0
      }
    }
  }
}
