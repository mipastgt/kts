/*
 * Copyright (c) 2016 Vivid Solutions, and others.
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
import kotlin.math.sqrt

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Coordinates
import org.locationtech.jts.geom.Envelope

/**
 * A robust version of [LineIntersector].
 *
 * @version 1.7
 */
open class RobustLineIntersector : LineIntersector() {

  override fun computeIntersection(p: Coordinate, p1: Coordinate, p2: Coordinate) {
    isProper = false
    // do between check first, since it is faster than the orientation test
    if (Envelope.intersects(p1, p2, p)) {
      if ((Orientation.index(p1, p2, p) == 0) &&
          (Orientation.index(p2, p1, p) == 0)) {
        isProper = true
        if (p.equals(p1) || p.equals(p2)) {
          isProper = false
        }
        result = POINT_INTERSECTION
        return
      }
    }
    result = NO_INTERSECTION
  }

  override fun computeIntersect(p1: Coordinate, p2: Coordinate, q1: Coordinate, q2: Coordinate): Int {
    isProper = false

    // first try a fast test to see if the envelopes of the lines intersect
    if (!Envelope.intersects(p1, p2, q1, q2))
      return NO_INTERSECTION

    // for each endpoint, compute which side of the other segment it lies
    val Pq1 = Orientation.index(p1, p2, q1)
    val Pq2 = Orientation.index(p1, p2, q2)

    if ((Pq1 > 0 && Pq2 > 0) || (Pq1 < 0 && Pq2 < 0)) {
      return NO_INTERSECTION
    }

    val Qp1 = Orientation.index(q1, q2, p1)
    val Qp2 = Orientation.index(q1, q2, p2)

    if ((Qp1 > 0 && Qp2 > 0) || (Qp1 < 0 && Qp2 < 0)) {
      return NO_INTERSECTION
    }
    /**
     * Intersection is collinear if each endpoint lies on the other line.
     */
    val collinear = Pq1 == 0 &&
        Pq2 == 0 &&
        Qp1 == 0 &&
        Qp2 == 0
    if (collinear) {
      return computeCollinearIntersection(p1, p2, q1, q2)
    }

    /**
     *  Check if the intersection is an endpoint. If it is, copy the endpoint as
     *  the intersection point.
     */
    var p: Coordinate? = null
    var z = Double.NaN
    if (Pq1 == 0 || Pq2 == 0 || Qp1 == 0 || Qp2 == 0) {
      isProper = false

      /**
       * Check for two equal endpoints.
       */
      if (p1.equals2D(q1)) {
        p = p1
        z = zGet(p1, q1)
      } else if (p1.equals2D(q2)) {
        p = p1
        z = zGet(p1, q2)
      } else if (p2.equals2D(q1)) {
        p = p2
        z = zGet(p2, q1)
      } else if (p2.equals2D(q2)) {
        p = p2
        z = zGet(p2, q2)
      } else if (Pq1 == 0) {
        p = q1
        z = zGetOrInterpolate(q1, p1, p2)
      } else if (Pq2 == 0) {
        p = q2
        z = zGetOrInterpolate(q2, p1, p2)
      } else if (Qp1 == 0) {
        p = p1
        z = zGetOrInterpolate(p1, q1, q2)
      } else if (Qp2 == 0) {
        p = p2
        z = zGetOrInterpolate(p2, q1, q2)
      }
    } else {
      isProper = true
      p = intersection(p1, p2, q1, q2)
      z = zInterpolate(p, p1, p2, q1, q2)
    }
    intPt[0] = copyWithZ(p!!, z)
    return POINT_INTERSECTION
  }

  private fun computeCollinearIntersection(
    p1: Coordinate, p2: Coordinate,
    q1: Coordinate, q2: Coordinate
  ): Int {
    val q1inP = Envelope.intersects(p1, p2, q1)
    val q2inP = Envelope.intersects(p1, p2, q2)
    val p1inQ = Envelope.intersects(q1, q2, p1)
    val p2inQ = Envelope.intersects(q1, q2, p2)

    if (q1inP && q2inP) {
      intPt[0] = copyWithZInterpolate(q1, p1, p2)
      intPt[1] = copyWithZInterpolate(q2, p1, p2)
      return COLLINEAR_INTERSECTION
    }
    if (p1inQ && p2inQ) {
      intPt[0] = copyWithZInterpolate(p1, q1, q2)
      intPt[1] = copyWithZInterpolate(p2, q1, q2)
      return COLLINEAR_INTERSECTION
    }
    if (q1inP && p1inQ) {
      // if pts are equal Z is chosen arbitrarily
      intPt[0] = copyWithZInterpolate(q1, p1, p2)
      intPt[1] = copyWithZInterpolate(p1, q1, q2)
      return if (q1.equals(p1) && !q2inP && !p2inQ) POINT_INTERSECTION else COLLINEAR_INTERSECTION
    }
    if (q1inP && p2inQ) {
      // if pts are equal Z is chosen arbitrarily
      intPt[0] = copyWithZInterpolate(q1, p1, p2)
      intPt[1] = copyWithZInterpolate(p2, q1, q2)
      return if (q1.equals(p2) && !q2inP && !p1inQ) POINT_INTERSECTION else COLLINEAR_INTERSECTION
    }
    if (q2inP && p1inQ) {
      // if pts are equal Z is chosen arbitrarily
      intPt[0] = copyWithZInterpolate(q2, p1, p2)
      intPt[1] = copyWithZInterpolate(p1, q1, q2)
      return if (q2.equals(p1) && !q1inP && !p2inQ) POINT_INTERSECTION else COLLINEAR_INTERSECTION
    }
    if (q2inP && p2inQ) {
      // if pts are equal Z is chosen arbitrarily
      intPt[0] = copyWithZInterpolate(q2, p1, p2)
      intPt[1] = copyWithZInterpolate(p2, q1, q2)
      return if (q2.equals(p2) && !q1inP && !p1inQ) POINT_INTERSECTION else COLLINEAR_INTERSECTION
    }
    return NO_INTERSECTION
  }

  /**
   * This method computes the actual value of the intersection point.
   * It is rounded to the precision model if being used.
   */
  private fun intersection(p1: Coordinate, p2: Coordinate, q1: Coordinate, q2: Coordinate): Coordinate {
    var intPt = intersectionSafe(p1, p2, q1, q2)

    if (!isInSegmentEnvelopes(intPt)) {
      // compute a safer result
      // copy the coordinate, since it may be rounded later
      intPt = copy(nearestEndpoint(p1, p2, q1, q2))
    }
    if (precisionModel != null) {
      precisionModel!!.makePrecise(intPt)
    }
    return intPt
  }

  /**
   * Computes a segment intersection.
   *
   * @return the computed intersection point
   */
  private fun intersectionSafe(p1: Coordinate, p2: Coordinate, q1: Coordinate, q2: Coordinate): Coordinate {
    var intPt = Intersection.intersection(p1, p2, q1, q2)
    if (intPt == null)
      intPt = nearestEndpoint(p1, p2, q1, q2)
    return intPt
  }

  /**
   * Tests whether a point lies in the envelopes of both input segments.
   *
   * @return <code>true</code> if the input point lies within both input segment envelopes
   */
  private fun isInSegmentEnvelopes(intPt: Coordinate): Boolean {
    val env0 = Envelope(inputLines[0][0]!!, inputLines[0][1]!!)
    val env1 = Envelope(inputLines[1][0]!!, inputLines[1][1]!!)
    return env0.contains(intPt) && env1.contains(intPt)
  }

  companion object {
    private fun copyWithZInterpolate(p: Coordinate, p1: Coordinate, p2: Coordinate): Coordinate {
      return copyWithZ(p, zGetOrInterpolate(p, p1, p2))
    }

    private fun copyWithZ(p: Coordinate, z: Double): Coordinate {
      val pCopy = copy(p)
      if (!z.isNaN() && Coordinates.hasZ(pCopy)) {
        pCopy.setZ(z)
      }
      return pCopy
    }

    private fun copy(p: Coordinate): Coordinate {
      return p.copy()
    }

    /**
     * Finds the endpoint of the segments P and Q which
     * is closest to the other segment.
     *
     * @return the nearest endpoint to the other segment
     */
    private fun nearestEndpoint(
      p1: Coordinate, p2: Coordinate,
      q1: Coordinate, q2: Coordinate
    ): Coordinate {
      var nearestPt = p1
      var minDist = Distance.pointToSegment(p1, q1, q2)

      var dist = Distance.pointToSegment(p2, q1, q2)
      if (dist < minDist) {
        minDist = dist
        nearestPt = p2
      }
      dist = Distance.pointToSegment(q1, p1, p2)
      if (dist < minDist) {
        minDist = dist
        nearestPt = q1
      }
      dist = Distance.pointToSegment(q2, p1, p2)
      if (dist < minDist) {
        minDist = dist
        nearestPt = q2
      }
      return nearestPt
    }

    /**
     * Gets the Z value of the first argument if present,
     * otherwise the value of the second argument.
     *
     * @return the Z value if present
     */
    private fun zGet(p: Coordinate, q: Coordinate): Double {
      var z = p.getZ()
      if (z.isNaN()) {
        z = q.getZ() // may be NaN
      }
      return z
    }

    /**
     * Gets the Z value of a coordinate if present, or
     * interpolates it from the segment it lies on.
     *
     * @return the extracted or interpolated Z value (may be NaN)
     */
    private fun zGetOrInterpolate(p: Coordinate, p1: Coordinate, p2: Coordinate): Double {
      val z = p.getZ()
      if (!z.isNaN())
        return z
      return zInterpolate(p, p1, p2) // may be NaN
    }

    /**
     * Interpolates a Z value for a point along
     * a line segment between two points.
     *
     * @return the interpolated Z value (may be NaN)
     */
    private fun zInterpolate(p: Coordinate, p1: Coordinate, p2: Coordinate): Double {
      val p1z = p1.getZ()
      val p2z = p2.getZ()
      if (p1z.isNaN()) {
        return p2z // may be NaN
      }
      if (p2z.isNaN()) {
        return p1z // may be NaN
      }
      if (p.equals2D(p1)) {
        return p1z // not NaN
      }
      if (p.equals2D(p2)) {
        return p2z // not NaN
      }
      val dz = p2z - p1z
      if (dz == 0.0) {
        return p1z
      }
      // interpolate Z from distance of p along p1-p2
      val dx = (p2.x - p1.x)
      val dy = (p2.y - p1.y)
      // seg has non-zero length since p1 < p < p2
      val seglen = (dx * dx + dy * dy)
      val xoff = (p.x - p1.x)
      val yoff = (p.y - p1.y)
      val plen = (xoff * xoff + yoff * yoff)
      val frac = sqrt(plen / seglen)
      val zoff = dz * frac
      val zInterpolated = p1z + zoff
      return zInterpolated
    }

    /**
     * Interpolates a Z value for a point along
     * two line segments and computes their average.
     *
     * @return the averaged interpolated Z value (may be NaN)
     */
    private fun zInterpolate(p: Coordinate, p1: Coordinate, p2: Coordinate, q1: Coordinate, q2: Coordinate): Double {
      val zp = zInterpolate(p, p1, p2)
      val zq = zInterpolate(p, q1, q2)
      if (zp.isNaN()) {
        return zq // may be NaN
      }
      if (zq.isNaN()) {
        return zp // may be NaN
      }
      // both Zs have values, so average them
      return (zp + zq) / 2.0
    }
  }
}
