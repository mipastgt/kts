/*
 * Copyright (c) 2019 martin Davis
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

/**
 * Functions to compute intersection points between lines and line segments.
 *
 * The Z-ordinate is ignored, and not populated.
 *
 * @author Martin Davis
 *
 */
class Intersection {
  companion object {
    /**
     * Computes the intersection point of two lines.
     * If the lines are parallel or collinear this case is detected
     * and <code>null</code> is returned.
     *
     * @param p1 an endpoint of line 1
     * @param p2 an endpoint of line 1
     * @param q1 an endpoint of line 2
     * @param q2 an endpoint of line 2
     * @return the intersection point between the lines, if there is one,
     * or null if the lines are parallel or collinear
     *
     * @see CGAlgorithmsDD.intersection
     */
    @JvmStatic
    fun intersection(p1: Coordinate, p2: Coordinate, q1: Coordinate, q2: Coordinate): Coordinate? {
      return CGAlgorithmsDD.intersection(p1, p2, q1, q2)
      //-- this is less robust
      //return intersectionFP(p1, p2, q1, q2);
    }

    /**
     * Compute intersection of two lines, using a floating-point algorithm.
     * This is less accurate than [CGAlgorithmsDD.intersection].
     * It has caused spatial predicate failures in some cases.
     * This is kept for testing purposes.
     *
     * @param p1 an endpoint of line 1
     * @param p2 an endpoint of line 1
     * @param q1 an endpoint of line 2
     * @param q2 an endpoint of line 2
     * @return the intersection point between the lines, if there is one,
     * or null if the lines are parallel or collinear
     */
    private fun intersectionFP(p1: Coordinate, p2: Coordinate, q1: Coordinate, q2: Coordinate): Coordinate? {
      // compute midpoint of "kernel envelope"
      val minX0 = if (p1.x < p2.x) p1.x else p2.x
      val minY0 = if (p1.y < p2.y) p1.y else p2.y
      val maxX0 = if (p1.x > p2.x) p1.x else p2.x
      val maxY0 = if (p1.y > p2.y) p1.y else p2.y

      val minX1 = if (q1.x < q2.x) q1.x else q2.x
      val minY1 = if (q1.y < q2.y) q1.y else q2.y
      val maxX1 = if (q1.x > q2.x) q1.x else q2.x
      val maxY1 = if (q1.y > q2.y) q1.y else q2.y

      val intMinX = if (minX0 > minX1) minX0 else minX1
      val intMaxX = if (maxX0 < maxX1) maxX0 else maxX1
      val intMinY = if (minY0 > minY1) minY0 else minY1
      val intMaxY = if (maxY0 < maxY1) maxY0 else maxY1

      val midx = (intMinX + intMaxX) / 2.0
      val midy = (intMinY + intMaxY) / 2.0

      // condition ordinate values by subtracting midpoint
      val p1x = p1.x - midx
      val p1y = p1.y - midy
      val p2x = p2.x - midx
      val p2y = p2.y - midy
      val q1x = q1.x - midx
      val q1y = q1.y - midy
      val q2x = q2.x - midx
      val q2y = q2.y - midy

      // unrolled computation using homogeneous coordinates eqn
      val px = p1y - p2y
      val py = p2x - p1x
      val pw = p1x * p2y - p2x * p1y

      val qx = q1y - q2y
      val qy = q2x - q1x
      val qw = q1x * q2y - q2x * q1y

      val x = py * qw - qy * pw
      val y = qx * pw - px * qw
      val w = px * qy - qx * py

      val xInt = x / w
      val yInt = y / w

      // check for parallel lines
      if ((xInt.isNaN()) || (xInt.isInfinite() ||
            yInt.isNaN()) || (yInt.isInfinite())) {
        return null
      }
      // de-condition intersection point
      return Coordinate(xInt + midx, yInt + midy)
    }

    /**
     * Computes the intersection point of a line and a line segment (if any).
     *
     * @param line1 a point on the line
     * @param line2 a point on the line
     * @param seg1 an endpoint of the line segment
     * @param seg2 an endpoint of the line segment
     * @return the intersection point, or null if it is not possible to find an intersection
     */
    @JvmStatic
    fun lineSegment(line1: Coordinate, line2: Coordinate, seg1: Coordinate, seg2: Coordinate): Coordinate? {
      val orientS1 = Orientation.index(line1, line2, seg1)
      if (orientS1 == 0) return seg1.copy()

      val orientS2 = Orientation.index(line1, line2, seg2)
      if (orientS2 == 0) return seg2.copy()

      /**
       * If segment lies completely on one side of the line, it does not intersect
       */
      if ((orientS1 > 0 && orientS2 > 0) || (orientS1 < 0 && orientS2 < 0)) {
        return null
      }

      /**
       * The segment intersects the line.
       * The full line-line intersection is used to compute the intersection point.
       */
      val intPt = intersection(line1, line2, seg1, seg2)
      if (intPt != null) return intPt

      /**
       * Due to robustness failure it is possible the intersection computation will return null.
       * In this case choose the closest point
       */
      val dist1 = Distance.pointToLinePerpendicular(seg1, line1, line2)
      val dist2 = Distance.pointToLinePerpendicular(seg2, line1, line2)
      if (dist1 < dist2)
        return seg1.copy()
      return seg2
    }
  }
}
