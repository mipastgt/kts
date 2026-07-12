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
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.geom.Location

/**
 * Functions for locating points within basic geometric
 * structures such as line segments, lines and rings.
 *
 * @author Martin Davis
 *
 */
class PointLocation {
  companion object {
    /**
     * Tests whether a point lies on a line segment.
     *
     * @param p the point to test
     * @param p0 a point of the line segment
     * @param p1 a point of the line segment
     * @return true if the point lies on the line segment
     */
    @JvmStatic
    fun isOnSegment(p: Coordinate, p0: Coordinate, p1: Coordinate): Boolean {
      //-- test envelope first since it's faster
      if (!Envelope.intersects(p0, p1, p))
        return false
      //-- handle zero-length segments
      if (p.equals2D(p0))
        return true
      val isOnLine = Orientation.COLLINEAR == Orientation.index(p0, p1, p)
      return isOnLine
    }

    /**
     * Tests whether a point lies on the line defined by a list of
     * coordinates.
     *
     * @param p the point to test
     * @param line the line coordinates
     * @return true if the point is a vertex of the line or lies in the interior
     *         of a line segment in the line
     */
    @JvmStatic
    fun isOnLine(p: Coordinate, line: Array<Coordinate>): Boolean {
      for (i in 1 until line.size) {
        val p0 = line[i - 1]
        val p1 = line[i]
        if (isOnSegment(p, p0, p1)) {
          return true
        }
      }
      return false
    }

    /**
     * Tests whether a point lies on the line defined by a
     * [CoordinateSequence].
     *
     * @param p the point to test
     * @param line the line coordinates
     * @return true if the point is a vertex of the line or lies in the interior
     *         of a line segment in the line
     */
    @JvmStatic
    fun isOnLine(p: Coordinate, line: CoordinateSequence): Boolean {
      val p0 = Coordinate()
      val p1 = Coordinate()
      val n = line.size()
      for (i in 1 until n) {
        line.getCoordinate(i - 1, p0)
        line.getCoordinate(i, p1)
        if (isOnSegment(p, p0, p1)) {
          return true
        }
      }
      return false
    }

    /**
     * Tests whether a point lies inside or on a ring. The ring may be oriented in
     * either direction. A point lying exactly on the ring boundary is considered
     * to be inside the ring.
     *
     * @param p point to check for ring inclusion
     * @param ring an array of coordinates representing the ring (which must have
     *          first point identical to last point)
     * @return true if p is inside ring
     *
     * @see PointLocation.locateInRing
     */
    @JvmStatic
    fun isInRing(p: Coordinate, ring: Array<Coordinate>): Boolean {
      return locateInRing(p, ring) != Location.EXTERIOR
    }

    /**
     * Determines whether a point lies in the interior, on the boundary, or in the
     * exterior of a ring. The ring may be oriented in either direction.
     *
     * @param p point to check for ring inclusion
     * @param ring an array of coordinates representing the ring (which must have
     *          first point identical to last point)
     * @return the [Location] of p relative to the ring
     */
    @JvmStatic
    fun locateInRing(p: Coordinate, ring: Array<Coordinate>): Int {
      return RayCrossingCounter.locatePointInRing(p, ring)
    }
  }
}
