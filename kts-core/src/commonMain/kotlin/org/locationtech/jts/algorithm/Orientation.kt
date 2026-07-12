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
package org.locationtech.jts.algorithm

import kotlin.jvm.JvmStatic

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.CoordinateSequence
import org.locationtech.jts.geom.impl.CoordinateArraySequence

/**
 * Functions to compute the orientation of basic geometric structures
 * including point triplets (triangles) and rings.
 *
 * @author Martin Davis
 *
 */
class Orientation {
  companion object {
    /**
     * A value that indicates an orientation of clockwise, or a right turn.
     */
    const val CLOCKWISE = -1

    /**
     * A value that indicates an orientation of clockwise, or a right turn.
     */
    const val RIGHT = CLOCKWISE

    /**
     * A value that indicates an orientation of counterclockwise, or a left turn.
     */
    const val COUNTERCLOCKWISE = 1

    /**
     * A value that indicates an orientation of counterclockwise, or a left turn.
     */
    const val LEFT = COUNTERCLOCKWISE

    /**
     * A value that indicates an orientation of collinear, or no turn (straight).
     */
    const val COLLINEAR = 0

    /**
     * A value that indicates an orientation of collinear, or no turn (straight).
     */
    const val STRAIGHT = COLLINEAR

    /**
     * Returns the orientation index of the direction of the point <code>q</code> relative to
     * a directed infinite line specified by <code>p1-p2</code>.
     *
     * @param p1 the origin point of the line vector
     * @param p2 the final point of the line vector
     * @param q the point to compute the direction to
     *
     * @return -1 ( [CLOCKWISE] or [RIGHT] ) if q is clockwise (right) from p1-p2;
     *         1 ( [COUNTERCLOCKWISE] or [LEFT] ) if q is counter-clockwise (left) from p1-p2;
     *         0 ( [COLLINEAR] or [STRAIGHT] ) if q is collinear with p1-p2
     */
    @JvmStatic
    fun index(p1: Coordinate, p2: Coordinate, q: Coordinate): Int {
      return CGAlgorithmsDD.orientationIndex(p1, p2, q)
    }

    /**
     * Tests if a ring defined by an array of [Coordinate]s is
     * oriented counter-clockwise.
     *
     * @param ring an array of Coordinates forming a ring (with first and last point identical)
     * @return true if the ring is oriented counter-clockwise.
     * @throws IllegalArgumentException if there are too few points to determine orientation (&lt; 4)
     */
    @JvmStatic
    fun isCCW(ring: Array<Coordinate>): Boolean {
      // wrap with an XY CoordinateSequence
      return isCCW(CoordinateArraySequence(ring, 2, 0))
    }

    /**
     * Tests if a ring defined by a [CoordinateSequence] is
     * oriented counter-clockwise.
     *
     * @param ring a CoordinateSequence forming a ring (with first and last point identical)
     * @return true if the ring is oriented counter-clockwise.
     */
    @JvmStatic
    fun isCCW(ring: CoordinateSequence): Boolean {
      // # of points without closing endpoint
      val nPts = ring.size() - 1
      // return default value if ring is flat
      if (nPts < 3) return false

      /**
       * Find first highest point after a lower point, if one exists
       * (e.g. a rising segment)
       * If one does not exist, hiIndex will remain 0
       * and the ring must be flat.
       */
      var upHiPt = ring.getCoordinate(0)
      var prevY = upHiPt.y
      var upLowPt: Coordinate? = null
      var iUpHi = 0
      for (i in 1..nPts) {
        val py = ring.getOrdinate(i, Coordinate.Y)
        /**
         * If segment is upwards and endpoint is higher, record it
         */
        if (py > prevY && py >= upHiPt.y) {
          upHiPt = ring.getCoordinate(i)
          iUpHi = i
          upLowPt = ring.getCoordinate(i - 1)
        }
        prevY = py
      }
      /**
       * Check if ring is flat and return default value if so
       */
      if (iUpHi == 0) return false

      /**
       * Find the next lower point after the high point
       * (e.g. a falling segment).
       * This must exist since ring is not flat.
       */
      var iDownLow = iUpHi
      do {
        iDownLow = (iDownLow + 1) % nPts
      } while (iDownLow != iUpHi && ring.getOrdinate(iDownLow, Coordinate.Y) == upHiPt.y)

      val downLowPt = ring.getCoordinate(iDownLow)
      val iDownHi = if (iDownLow > 0) iDownLow - 1 else nPts - 1
      val downHiPt = ring.getCoordinate(iDownHi)

      /**
       * Two cases can occur:
       * 1) the hiPt and the downPrevPt are the same.
       * 2) The hiPt and the downPrevPt are different.
       */
      if (upHiPt.equals2D(downHiPt)) {
        /**
         * Check for the case where the cap has configuration A-B-A.
         */
        if (upLowPt!!.equals2D(upHiPt) || downLowPt.equals2D(upHiPt) || upLowPt.equals2D(downLowPt))
          return false

        /**
         * It can happen that the top segments are coincident.
         * This is an invalid ring, which cannot be computed correctly.
         */
        val index = index(upLowPt, upHiPt, downLowPt)
        return index == COUNTERCLOCKWISE
      } else {
        /**
         * Flat cap - direction of flat top determines orientation
         */
        val delX = downHiPt.x - upHiPt.x
        return delX < 0
      }
    }

    /**
     * Tests if a ring defined by an array of [Coordinate]s is
     * oriented counter-clockwise, using the signed area of the ring.
     *
     * @param ring an array of Coordinates forming a ring (with first and last point identical)
     * @return true if the ring is oriented counter-clockwise.
     */
    @JvmStatic
    fun isCCWArea(ring: Array<Coordinate>): Boolean {
      return Area.ofRingSigned(ring) < 0
    }
  }
}
