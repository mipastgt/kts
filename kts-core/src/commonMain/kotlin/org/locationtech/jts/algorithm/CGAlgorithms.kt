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
import kotlin.math.hypot
import kotlin.math.sqrt

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.CoordinateSequence
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.geom.Location
import org.locationtech.jts.math.MathUtil

/**
 * Specifies and implements various fundamental Computational Geometric
 * algorithms.
 *
 * @deprecated See [Length], [Area], [Distance], [Orientation], [PointLocation]
 */
class CGAlgorithms {
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
     * Returns the index of the direction of the point `q` relative to
     * a vector specified by `p1-p2`.
     *
     * @deprecated Use [Orientation.index] instead.
     */
    @JvmStatic
    fun orientationIndex(p1: Coordinate, p2: Coordinate, q: Coordinate): Int {
      return CGAlgorithmsDD.orientationIndex(p1, p2, q)
    }

    /**
     * Tests whether a point lies inside or on a ring.
     *
     * @deprecated Use [PointLocation.isInRing] instead.
     */
    @JvmStatic
    fun isPointInRing(p: Coordinate, ring: Array<Coordinate>): Boolean {
      return locatePointInRing(p, ring) != Location.EXTERIOR
    }

    /**
     * Determines whether a point lies in the interior, on the boundary, or in the
     * exterior of a ring.
     *
     * @deprecated Use [PointLocation.locateInRing] instead.
     */
    @JvmStatic
    fun locatePointInRing(p: Coordinate, ring: Array<Coordinate>): Int {
      return RayCrossingCounter.locatePointInRing(p, ring)
    }

    /**
     * Tests whether a point lies on the line segments defined by a list of
     * coordinates.
     *
     * @deprecated Use [PointLocation.isOnLine] instead.
     */
    @JvmStatic
    fun isOnLine(p: Coordinate, pt: Array<Coordinate>): Boolean {
      for (i in 1 until pt.size) {
        val p0 = pt[i - 1]
        val p1 = pt[i]
        if (PointLocation.isOnSegment(p, p0, p1)) {
          return true
        }
      }
      return false
    }

    /**
     * Computes whether a ring defined by an array of [Coordinate]s is
     * oriented counter-clockwise.
     *
     * @deprecated Use [Orientation.isCCW] instead.
     */
    @JvmStatic
    fun isCCW(ring: Array<Coordinate>): Boolean {
      // # of points without closing endpoint
      val nPts = ring.size - 1
      // sanity check
      if (nPts < 3)
        throw IllegalArgumentException(
          "Ring has fewer than 4 points, so orientation cannot be determined")

      // find highest point
      var hiPt = ring[0]
      var hiIndex = 0
      for (i in 1..nPts) {
        val p = ring[i]
        if (p.y > hiPt.y) {
          hiPt = p
          hiIndex = i
        }
      }

      // find distinct point before highest point
      var iPrev = hiIndex
      do {
        iPrev = iPrev - 1
        if (iPrev < 0)
          iPrev = nPts
      } while (ring[iPrev].equals2D(hiPt) && iPrev != hiIndex)

      // find distinct point after highest point
      var iNext = hiIndex
      do {
        iNext = (iNext + 1) % nPts
      } while (ring[iNext].equals2D(hiPt) && iNext != hiIndex)

      val prev = ring[iPrev]
      val next = ring[iNext]

      /*
        This check catches cases where the ring contains an A-B-A configuration
        of points.
       */
      if (prev.equals2D(hiPt) || next.equals2D(hiPt) || prev.equals2D(next))
        return false

      val disc = computeOrientation(prev, hiPt, next)

      /*
        If disc is exactly 0, lines are collinear.
       */
      val isCCW: Boolean
      if (disc == 0) {
        // poly is CCW if prev x is right of next x
        isCCW = (prev.x > next.x)
      } else {
        // if area is positive, points are ordered CCW
        isCCW = (disc > 0)
      }
      return isCCW
    }

    /**
     * Computes the orientation of a point q to the directed line segment p1-p2.
     *
     * @deprecated Use [Orientation.index] instead.
     */
    @JvmStatic
    fun computeOrientation(p1: Coordinate, p2: Coordinate, q: Coordinate): Int {
      return orientationIndex(p1, p2, q)
    }

    /**
     * Computes the distance from a point p to a line segment AB
     *
     * @deprecated Use [Distance.pointToSegment] instead.
     */
    @JvmStatic
    fun distancePointLine(p: Coordinate, A: Coordinate, B: Coordinate): Double {
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
     * @deprecated Use [Distance.pointToLinePerpendicular] instead.
     */
    @JvmStatic
    fun distancePointLinePerpendicular(p: Coordinate, A: Coordinate, B: Coordinate): Double {
      // use comp.graphics.algorithms Frequently Asked Questions method
      val len2 = (B.x - A.x) * (B.x - A.x) + (B.y - A.y) * (B.y - A.y)
      val s = ((A.y - p.y) * (B.x - A.x) - (A.x - p.x) * (B.y - A.y)) / len2

      return abs(s) * sqrt(len2)
    }

    /**
     * Computes the distance from a point to a sequence of line segments.
     *
     * @deprecated Use [Distance.pointToSegmentString] instead.
     */
    @JvmStatic
    fun distancePointLine(p: Coordinate, line: Array<Coordinate>): Double {
      if (line.isEmpty())
        throw IllegalArgumentException("Line array must contain at least one vertex")
      // this handles the case of length = 1
      var minDistance = p.distance(line[0])
      for (i in 0 until line.size - 1) {
        val dist = distancePointLine(p, line[i], line[i + 1])
        if (dist < minDistance) {
          minDistance = dist
        }
      }
      return minDistance
    }

    /**
     * Computes the distance from a line segment AB to a line segment CD
     *
     * @deprecated Use [Distance.segmentToSegment] instead.
     */
    @JvmStatic
    fun distanceLineLine(A: Coordinate, B: Coordinate, C: Coordinate, D: Coordinate): Double {
      // check for zero-length segments
      if (A.equals(B))
        return distancePointLine(A, C, D)
      if (C.equals(D))
        return distancePointLine(D, A, B)

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
          distancePointLine(A, C, D),
          distancePointLine(B, C, D),
          distancePointLine(C, A, B),
          distancePointLine(D, A, B))
      }
      // segments intersect
      return 0.0
    }

    /**
     * Computes the signed area for a ring.
     *
     * @deprecated Use [Area.ofRing] or [Area.ofRingSigned] instead.
     */
    @JvmStatic
    fun signedArea(ring: Array<Coordinate>): Double {
      if (ring.size < 3)
        return 0.0
      var sum = 0.0
      /*
        Based on the Shoelace formula.
       */
      val x0 = ring[0].x
      for (i in 1 until ring.size - 1) {
        val x = ring[i].x - x0
        val y1 = ring[i + 1].y
        val y2 = ring[i - 1].y
        sum += x * (y2 - y1)
      }
      return sum / 2.0
    }

    /**
     * Computes the signed area for a ring.
     *
     * @deprecated Use [Area.ofRing] or [Area.ofRingSigned] instead.
     */
    @JvmStatic
    fun signedArea(ring: CoordinateSequence): Double {
      val n = ring.size()
      if (n < 3)
        return 0.0
      /*
        Based on the Shoelace formula.
       */
      val p0 = Coordinate()
      val p1 = Coordinate()
      val p2 = Coordinate()
      ring.getCoordinate(0, p1)
      ring.getCoordinate(1, p2)
      val x0 = p1.x
      p2.x -= x0
      var sum = 0.0
      for (i in 1 until n - 1) {
        p0.y = p1.y
        p1.x = p2.x
        p1.y = p2.y
        ring.getCoordinate(i + 1, p2)
        p2.x -= x0
        sum += p1.x * (p0.y - p2.y)
      }
      return sum / 2.0
    }

    /**
     * Computes the length of a linestring specified by a sequence of points.
     *
     * @deprecated Use [Length.ofLine] instead.
     */
    @JvmStatic
    fun length(pts: CoordinateSequence): Double {
      // optimized for processing CoordinateSequences
      val n = pts.size()
      if (n <= 1)
        return 0.0

      var len = 0.0

      val p = Coordinate()
      pts.getCoordinate(0, p)
      var x0 = p.x
      var y0 = p.y

      for (i in 1 until n) {
        pts.getCoordinate(i, p)
        val x1 = p.x
        val y1 = p.y
        val dx = x1 - x0
        val dy = y1 - y0

        len += hypot(dx, dy)

        x0 = x1
        y0 = y1
      }
      return len
    }
  }
}
