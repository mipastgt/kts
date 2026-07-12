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

package org.locationtech.jts.operation.predicate

import kotlin.jvm.JvmStatic

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.Polygon

/**
 * Optimized implementation of the `contains` spatial predicate
 * for cases where the first [Geometry] is a rectangle.
 * This class works for all input geometries, including
 * [org.locationtech.jts.geom.GeometryCollection]s.
 *
 *
 * As a further optimization,
 * this class can be used to test
 * many geometries against a single
 * rectangle in a slightly more efficient way.
 *
 * @version 1.7
 */
class RectangleContains
/**
 * Create a new contains computer for two geometries.
 *
 * @param rectangle a rectangular geometry
 */
  (rectangle: Polygon) {

  private val rectEnv: Envelope = rectangle.getEnvelopeInternal()

  fun contains(geom: Geometry): Boolean {
    // the test geometry must be wholly contained in the rectangle envelope
    if (!rectEnv.contains(geom.getEnvelopeInternal()))
      return false

    /**
     * Check that geom is not contained entirely in the rectangle boundary.
     * According to the somewhat odd spec of the SFS, if this
     * is the case the geometry is NOT contained.
     */
    if (isContainedInBoundary(geom))
      return false
    return true
  }

  private fun isContainedInBoundary(geom: Geometry): Boolean {
    // polygons can never be wholely contained in the boundary
    if (geom is Polygon) return false
    if (geom is Point) return isPointContainedInBoundary(geom)
    if (geom is LineString) return isLineStringContainedInBoundary(geom)

    for (i in 0 until geom.getNumGeometries()) {
      val comp = geom.getGeometryN(i)
      if (!isContainedInBoundary(comp))
        return false
    }
    return true
  }

  private fun isPointContainedInBoundary(point: Point): Boolean {
    return isPointContainedInBoundary(point.getCoordinate()!!)
  }

  /**
   * Tests if a point is contained in the boundary of the target rectangle.
   *
   * @param pt the point to test
   * @return true if the point is contained in the boundary
   */
  private fun isPointContainedInBoundary(pt: Coordinate): Boolean {
    /**
     * contains = false if the point is properly contained in the rectangle.
     *
     * This code assumes that the point lies in the rectangle envelope
     */
    return pt.x == rectEnv.getMinX()
      || pt.x == rectEnv.getMaxX()
      || pt.y == rectEnv.getMinY()
      || pt.y == rectEnv.getMaxY()
  }

  /**
   * Tests if a linestring is completely contained in the boundary of the target rectangle.
   * @param line the linestring to test
   * @return true if the linestring is contained in the boundary
   */
  private fun isLineStringContainedInBoundary(line: LineString): Boolean {
    val seq = line.getCoordinateSequence()
    val p0 = Coordinate()
    val p1 = Coordinate()
    for (i in 0 until seq.size() - 1) {
      seq.getCoordinate(i, p0)
      seq.getCoordinate(i + 1, p1)

      if (!isLineSegmentContainedInBoundary(p0, p1))
        return false
    }
    return true
  }

  /**
   * Tests if a line segment is contained in the boundary of the target rectangle.
   * @param p0 an endpoint of the segment
   * @param p1 an endpoint of the segment
   * @return true if the line segment is contained in the boundary
   */
  private fun isLineSegmentContainedInBoundary(p0: Coordinate, p1: Coordinate): Boolean {
    if (p0 == p1)
      return isPointContainedInBoundary(p0)

    // we already know that the segment is contained in the rectangle envelope
    if (p0.x == p1.x) {
      if (p0.x == rectEnv.getMinX() ||
        p0.x == rectEnv.getMaxX()
      )
        return true
    } else if (p0.y == p1.y) {
      if (p0.y == rectEnv.getMinY() ||
        p0.y == rectEnv.getMaxY()
      )
        return true
    }
    /**
     * Either
     *   both x and y values are different
     * or
     *   one of x and y are the same, but the other ordinate is not the same as a boundary ordinate
     *
     * In either case, the segment is not wholely in the boundary
     */
    return false
  }

  companion object {
    /**
     * Tests whether a rectangle contains a given geometry.
     *
     * @param rectangle a rectangular Polygon
     * @param b a Geometry of any type
     * @return true if the geometries intersect
     */
    @JvmStatic
    fun contains(rectangle: Polygon, b: Geometry): Boolean {
      val rc = RectangleContains(rectangle)
      return rc.contains(b)
    }
  }
}
