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
import kotlin.math.abs
import kotlin.math.sqrt

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.math.MathUtil

/**
 * Functions to compute distance between basic geometric structures.
 *
 * @author Martin Davis
 *
 */
class Distance {
  companion object {
    /**
     * Computes the distance from a line segment AB to a line segment CD
     *
     * Note: NON-ROBUST!
     *
     * @param A a point of one line
     * @param B the second point of (must be different to A)
     * @param C one point of the line
     * @param D another point of the line (must be different to A)
     */
    @JvmStatic
    fun segmentToSegment(A: Coordinate, B: Coordinate, C: Coordinate, D: Coordinate): Double {
      // check for zero-length segments
      if (A.equals(B))
        return pointToSegment(A, C, D)
      if (C.equals(D))
        return pointToSegment(D, A, B)

      // AB and CD are line segments
      var noIntersection = false
      if (!Envelope.intersects(A, B, C, D)) {
        noIntersection = true
      } else {
        val denom = (B.x - A.x) * (D.y - C.y) - (B.y - A.y) * (D.x - C.x)

        if (denom == 0.0) {
          noIntersection = true
        } else {
          val rNum = (A.y - C.y) * (D.x - C.x) - (A.x - C.x) * (D.y - C.y)
          val sNum = (A.y - C.y) * (B.x - A.x) - (A.x - C.x) * (B.y - A.y)

          val s = sNum / denom
          val r = rNum / denom

          if ((r < 0) || (r > 1) || (s < 0) || (s > 1)) {
            noIntersection = true
          }
        }
      }
      if (noIntersection) {
        return MathUtil.min(
          pointToSegment(A, C, D),
          pointToSegment(B, C, D),
          pointToSegment(C, A, B),
          pointToSegment(D, A, B))
      }
      // segments intersect
      return 0.0
    }

    /**
     * Computes the distance from a point to a sequence of line segments.
     *
     * @param p a point
     * @param line a sequence of contiguous line segments defined by their vertices
     * @return the minimum distance between the point and the line segments
     */
    @JvmStatic
    fun pointToSegmentString(p: Coordinate, line: Array<Coordinate>): Double {
      if (line.isEmpty())
        throw IllegalArgumentException("Line array must contain at least one vertex")
      // this handles the case of length = 1
      var minDistance = p.distance(line[0])
      for (i in 0 until line.size - 1) {
        val dist = pointToSegment(p, line[i], line[i + 1])
        if (dist < minDistance) {
          minDistance = dist
        }
      }
      return minDistance
    }

    /**
     * Computes the distance from a point p to a line segment AB
     *
     * Note: NON-ROBUST!
     *
     * @param p the point to compute the distance for
     * @param A one point of the line
     * @param B another point of the line (must be different to A)
     * @return the distance from p to line segment AB
     */
    @JvmStatic
    fun pointToSegment(p: Coordinate, A: Coordinate, B: Coordinate): Double {
      // if start = end, then just compute distance to one of the endpoints
      if (A.x == B.x && A.y == B.y)
        return p.distance(A)

      // otherwise use comp.graphics.algorithms Frequently Asked Questions method
      val len2 = (B.x - A.x) * (B.x - A.x) + (B.y - A.y) * (B.y - A.y)
      val r = ((p.x - A.x) * (B.x - A.x) + (p.y - A.y) * (B.y - A.y)) / len2

      if (r <= 0.0)
        return p.distance(A)
      if (r >= 1.0)
        return p.distance(B)

      val s = ((A.y - p.y) * (B.x - A.x) - (A.x - p.x) * (B.y - A.y)) / len2
      return abs(s) * sqrt(len2)
    }

    /**
     * Computes the perpendicular distance from a point p to the (infinite) line
     * containing the points AB
     *
     * @param p the point to compute the distance for
     * @param A one point of the line
     * @param B another point of the line (must be different to A)
     * @return the distance from p to line AB
     */
    @JvmStatic
    fun pointToLinePerpendicular(p: Coordinate, A: Coordinate, B: Coordinate): Double {
      // use comp.graphics.algorithms Frequently Asked Questions method
      val len2 = (B.x - A.x) * (B.x - A.x) + (B.y - A.y) * (B.y - A.y)
      val s = ((A.y - p.y) * (B.x - A.x) - (A.x - p.x) * (B.y - A.y)) / len2

      return abs(s) * sqrt(len2)
    }

    @JvmStatic
    fun pointToLinePerpendicularSigned(p: Coordinate, A: Coordinate, B: Coordinate): Double {
      // use comp.graphics.algorithms Frequently Asked Questions method
      val len2 = (B.x - A.x) * (B.x - A.x) + (B.y - A.y) * (B.y - A.y)
      val s = ((A.y - p.y) * (B.x - A.x) - (A.x - p.x) * (B.y - A.y)) / len2

      return s * sqrt(len2)
    }
  }
}
