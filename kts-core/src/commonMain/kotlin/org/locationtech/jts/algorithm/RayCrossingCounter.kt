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
import org.locationtech.jts.geom.Location
import org.locationtech.jts.geom.Polygonal

/**
 * Counts the number of segments crossed by a horizontal ray extending to the right
 * from a given point, in an incremental fashion.
 * This can be used to determine whether a point lies in a [Polygonal] geometry.
 *
 * @author Martin Davis
 *
 */
class RayCrossingCounter(private val p: Coordinate) {

  private var crossingCount = 0
  // true if the test point lies on an input segment
  private var isPointOnSegment = false

  /**
   * Counts a segment
   *
   * @param p1 an endpoint of the segment
   * @param p2 another endpoint of the segment
   */
  fun countSegment(p1: Coordinate, p2: Coordinate) {
    /*
     * For each segment, check if it crosses
     * a horizontal ray running from the test point in the positive x direction.
     */

    // check if the segment is strictly to the left of the test point
    if (p1.x < p.x && p2.x < p.x)
      return

    // check if the point is equal to the current ring vertex
    if (p.x == p2.x && p.y == p2.y) {
      isPointOnSegment = true
      return
    }
    /*
     * For horizontal segments, check if the point is on the segment.
     * Otherwise, horizontal segments are not counted.
     */
    if (p1.y == p.y && p2.y == p.y) {
      var minx = p1.x
      var maxx = p2.x
      if (minx > maxx) {
        minx = p2.x
        maxx = p1.x
      }
      if (p.x >= minx && p.x <= maxx) {
        isPointOnSegment = true
      }
      return
    }
    /*
     * Evaluate all non-horizontal segments which cross a horizontal ray to the
     * right of the test pt.
     */
    if (((p1.y > p.y) && (p2.y <= p.y)) ||
        ((p2.y > p.y) && (p1.y <= p.y))) {
      var orient = Orientation.index(p1, p2, p)
      if (orient == Orientation.COLLINEAR) {
        isPointOnSegment = true
        return
      }
      // Re-orient the result if needed to ensure effective segment direction is upwards
      if (p2.y < p1.y) {
        orient = -orient
      }
      // The upward segment crosses the ray if the test point lies to the left (CCW) of the segment.
      if (orient == Orientation.LEFT) {
        crossingCount++
      }
    }
  }

  /**
   * Gets the count of crossings.
   *
   * @return the crossing count
   */
  fun getCount(): Int {
    return crossingCount
  }

  /**
   * Reports whether the point lies exactly on one of the supplied segments.
   *
   * @return true if the point lies exactly on a segment
   */
  fun isOnSegment(): Boolean {
    return isPointOnSegment
  }

  /**
   * Gets the [Location] of the point relative to
   * the ring, polygon or multipolygon from which the processed segments were provided.
   *
   * @return the Location of the point
   */
  fun getLocation(): Int {
    if (isPointOnSegment)
      return Location.BOUNDARY

    // The point is in the interior of the ring if the number of X-crossings is odd.
    if ((crossingCount % 2) == 1) {
      return Location.INTERIOR
    }
    return Location.EXTERIOR
  }

  /**
   * Tests whether the point lies in or on
   * the ring, polygon or multipolygon from which the processed segments were provided.
   *
   * @return true if the point lies in or on the supplied polygon
   */
  fun isPointInPolygon(): Boolean {
    return getLocation() != Location.EXTERIOR
  }

  companion object {
    /**
     * Determines the [Location] of a point in a ring.
     * This method is an exemplar of how to use this class.
     *
     * @param p the point to test
     * @param ring an array of Coordinates forming a ring
     * @return the location of the point in the ring
     */
    @JvmStatic
    fun locatePointInRing(p: Coordinate, ring: Array<Coordinate>): Int {
      val counter = RayCrossingCounter(p)

      for (i in 1 until ring.size) {
        val p1 = ring[i]
        val p2 = ring[i - 1]
        counter.countSegment(p1, p2)
        if (counter.isOnSegment())
          return counter.getLocation()
      }
      return counter.getLocation()
    }

    /**
     * Determines the [Location] of a point in a ring.
     *
     * @param p the point to test
     * @param ring a coordinate sequence forming a ring
     * @return the location of the point in the ring
     */
    @JvmStatic
    fun locatePointInRing(p: Coordinate, ring: CoordinateSequence): Int {
      val counter = RayCrossingCounter(p)

      val p1 = Coordinate()
      val p2 = Coordinate()
      for (i in 1 until ring.size()) {
        p1.x = ring.getOrdinate(i, CoordinateSequence.X)
        p1.y = ring.getOrdinate(i, CoordinateSequence.Y)
        p2.x = ring.getOrdinate(i - 1, CoordinateSequence.X)
        p2.y = ring.getOrdinate(i - 1, CoordinateSequence.Y)
        counter.countSegment(p1, p2)
        if (counter.isOnSegment())
          return counter.getLocation()
      }
      return counter.getLocation()
    }
  }
}
