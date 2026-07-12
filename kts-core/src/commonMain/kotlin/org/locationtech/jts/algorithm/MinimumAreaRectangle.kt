/*
 * Copyright (c) 2023 Martin Davis.
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

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LineSegment
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.Polygon

/**
 * Computes the minimum-area rectangle enclosing a [Geometry].
 * Unlike the [Envelope], the rectangle may not be axis-parallel.
 *
 * @see MinimumDiameter
 * @see ConvexHull
 *
 */
class MinimumAreaRectangle {

  private val inputGeom: Geometry
  private val isConvex: Boolean

  /**
   * Compute a minimum-area rectangle for a given [Geometry].
   *
   * @param inputGeom a Geometry
   */
  constructor(inputGeom: Geometry) : this(inputGeom, false)

  /**
   * Compute a minimum rectangle for a [Geometry],
   * with a hint if the geometry is convex.
   *
   * @param inputGeom a Geometry which is convex
   * @param isConvex <code>true</code> if the input geometry is convex
   */
  constructor(inputGeom: Geometry, isConvex: Boolean) {
    this.inputGeom = inputGeom
    this.isConvex = isConvex
  }

  private fun getMinimumRectangle(): Geometry {
    if (inputGeom.isEmpty()) {
      return inputGeom.getFactory().createPolygon()
    }
    if (isConvex) {
      return computeConvex(inputGeom)
    }
    val convexGeom = ConvexHull(inputGeom).getConvexHull()
    return computeConvex(convexGeom)
  }

  private fun computeConvex(convexGeom: Geometry): Geometry {
    val convexHullPts: Array<Coordinate> = if (convexGeom is Polygon)
      convexGeom.getExteriorRing().getCoordinates()
    else
      convexGeom.getCoordinates()

    // special cases for lines or points or degenerate rings
    if (convexHullPts.size == 0) {
    } else if (convexHullPts.size == 1) {
      return inputGeom.getFactory().createPoint(convexHullPts[0].copy())
    } else if (convexHullPts.size == 2 || convexHullPts.size == 3) {
      //-- Min rectangle is a line. Use the diagonal of the extent
      return computeMaximumLine(convexHullPts, inputGeom.getFactory())
    }
    //TODO: ensure ring is CW
    return computeConvexRing(convexHullPts)
  }

  /**
   * Computes the minimum-area rectangle for a convex ring of [Coordinate]s.
   *
   * This algorithm uses the "dual rotating calipers" technique.
   * Performance is linear in the number of segments.
   *
   * @param ring the convex ring to scan
   */
  private fun computeConvexRing(ring: Array<Coordinate>): Polygon {
    // Assert: ring is oriented CW

    var minRectangleArea = Double.MAX_VALUE
    var minRectangleBaseIndex = -1
    var minRectangleDiamIndex = -1
    var minRectangleLeftIndex = -1
    var minRectangleRightIndex = -1

    //-- start at vertex after first one
    var diameterIndex = 1
    var leftSideIndex = 1
    var rightSideIndex = -1 // initialized once first diameter is found

    val segBase = LineSegment()
    val segDiam = LineSegment()
    // for each segment, find the next vertex which is at maximum distance
    for (i in 0 until ring.size - 1) {
      segBase.p0 = ring[i]
      segBase.p1 = ring[i + 1]
      diameterIndex = findFurthestVertex(ring, segBase, diameterIndex, 0)

      val diamPt = ring[diameterIndex]
      val diamBasePt = segBase.project(diamPt)
      segDiam.p0 = diamBasePt
      segDiam.p1 = diamPt

      leftSideIndex = findFurthestVertex(ring, segDiam, leftSideIndex, 1)

      //-- init the max right index
      if (i == 0) {
        rightSideIndex = diameterIndex
      }
      rightSideIndex = findFurthestVertex(ring, segDiam, rightSideIndex, -1)

      val rectWidth = segDiam.distancePerpendicular(ring[leftSideIndex]) +
          segDiam.distancePerpendicular(ring[rightSideIndex])
      val rectArea = segDiam.getLength() * rectWidth

      if (rectArea < minRectangleArea) {
        minRectangleArea = rectArea
        minRectangleBaseIndex = i
        minRectangleDiamIndex = diameterIndex
        minRectangleLeftIndex = leftSideIndex
        minRectangleRightIndex = rightSideIndex
      }
    }
    return Rectangle.createFromSidePts(
      ring[minRectangleBaseIndex], ring[minRectangleBaseIndex + 1],
      ring[minRectangleDiamIndex],
      ring[minRectangleLeftIndex], ring[minRectangleRightIndex],
      inputGeom.getFactory())
  }

  private fun findFurthestVertex(pts: Array<Coordinate>, baseSeg: LineSegment, startIndex: Int, orient: Int): Int {
    var maxDistance = orientedDistance(baseSeg, pts[startIndex], orient)
    var nextDistance = maxDistance
    var maxIndex = startIndex
    var nextIndex = maxIndex
    //-- rotate "caliper" while distance from base segment is non-decreasing
    while (isFurtherOrEqual(nextDistance, maxDistance, orient)) {
      maxDistance = nextDistance
      maxIndex = nextIndex

      nextIndex = nextIndex(pts, maxIndex)
      if (nextIndex == startIndex)
        break
      nextDistance = orientedDistance(baseSeg, pts[nextIndex], orient)
    }
    return maxIndex
  }

  private fun isFurtherOrEqual(d1: Double, d2: Double, orient: Int): Boolean {
    return when (orient) {
      0 -> abs(d1) >= abs(d2)
      1 -> d1 >= d2
      -1 -> d1 <= d2
      else -> throw IllegalArgumentException("Invalid orientation index: " + orient)
    }
  }

  companion object {
    /**
     * Gets the minimum-area rectangular [Polygon] which encloses the input geometry.
     *
     * @param geom the geometry
     * @return the minimum rectangle enclosing the geometry
     */
    @JvmStatic
    fun getMinimumRectangle(geom: Geometry): Geometry {
      return MinimumAreaRectangle(geom).getMinimumRectangle()
    }

    private fun orientedDistance(seg: LineSegment, p: Coordinate, orient: Int): Double {
      val dist = seg.distancePerpendicularOriented(p)
      if (orient == 0) {
        return abs(dist)
      }
      return dist
    }

    private fun nextIndex(ring: Array<Coordinate>, index: Int): Int {
      var index = index
      index++
      if (index >= ring.size - 1) index = 0
      return index
    }

    /**
     * Creates a line of maximum extent from the provided vertices
     * @param pts the vertices
     * @param factory the geometry factory
     * @return the line of maximum extent
     */
    private fun computeMaximumLine(pts: Array<Coordinate>, factory: GeometryFactory): LineString {
      //-- find max and min pts for X and Y
      var ptMinX: Coordinate? = null
      var ptMaxX: Coordinate? = null
      var ptMinY: Coordinate? = null
      var ptMaxY: Coordinate? = null
      for (p in pts) {
        if (ptMinX == null || p.getX() < ptMinX.getX()) ptMinX = p
        if (ptMaxX == null || p.getX() > ptMaxX.getX()) ptMaxX = p
        if (ptMinY == null || p.getY() < ptMinY.getY()) ptMinY = p
        if (ptMaxY == null || p.getY() > ptMaxY.getY()) ptMaxY = p
      }
      var p0 = ptMinX
      var p1 = ptMaxX
      //-- line is vertical - use Y pts
      if (p0!!.getX() == p1!!.getX()) {
        p0 = ptMinY
        p1 = ptMaxY
      }
      return factory.createLineString(arrayOf(p0!!.copy(), p1!!.copy()))
    }
  }
}
