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

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LineSegment
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.LinearRing
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.Polygon

/**
 * Computes the minimum diameter of a [Geometry].
 * The minimum diameter is defined to be the
 * width of the smallest band that contains the geometry,
 * where a band is a strip of the plane defined by two parallel lines.
 *
 * @see ConvexHull
 * @see MinimumAreaRectangle
 *
 * @version 1.7
 */
class MinimumDiameter {

  private val inputGeom: Geometry
  private val isConvex: Boolean

  private var convexHullPts: Array<Coordinate>? = null
  private var minBaseSeg: LineSegment? = LineSegment()
  private var minWidthPt: Coordinate? = null
  private var minPtIndex = 0
  private var minWidth = 0.0

  /**
   * Compute a minimum diameter for a given [Geometry].
   *
   * @param inputGeom a Geometry
   */
  constructor(inputGeom: Geometry) : this(inputGeom, false)

  /**
   * Compute a minimum diameter for a giver [Geometry],
   * with a hint if the Geometry is convex.
   *
   * @param inputGeom a Geometry which is convex
   * @param isConvex <code>true</code> if the input geometry is convex
   */
  constructor(inputGeom: Geometry, isConvex: Boolean) {
    this.inputGeom = inputGeom
    this.isConvex = isConvex
  }

  /**
   * Gets the length of the minimum diameter of the input Geometry
   *
   * @return the length of the minimum diameter
   */
  fun getLength(): Double {
    computeMinimumDiameter()
    return minWidth
  }

  /**
   * Gets the [Coordinate] forming one end of the minimum diameter
   *
   * @return a coordinate forming one end of the minimum diameter
   */
  fun getWidthCoordinate(): Coordinate? {
    computeMinimumDiameter()
    return minWidthPt
  }

  /**
   * Gets the segment forming the base of the minimum diameter
   *
   * @return the segment forming the base of the minimum diameter
   */
  fun getSupportingSegment(): LineString {
    computeMinimumDiameter()
    return inputGeom.getFactory().createLineString(arrayOf(minBaseSeg!!.p0, minBaseSeg!!.p1))
  }

  /**
   * Gets a [LineString] which is a minimum diameter
   *
   * @return a [LineString] which is a minimum diameter
   */
  fun getDiameter(): LineString {
    computeMinimumDiameter()

    // return empty linestring if no minimum width calculated
    if (minWidthPt == null)
      return inputGeom.getFactory().createLineString()

    val basePt = minBaseSeg!!.project(minWidthPt!!)
    return inputGeom.getFactory().createLineString(arrayOf(basePt, minWidthPt!!))
  }

  private fun computeMinimumDiameter() {
    // check if computation is cached
    if (minWidthPt != null)
      return

    if (isConvex)
      computeWidthConvex(inputGeom)
    else {
      val convexGeom = ConvexHull(inputGeom).getConvexHull()
      computeWidthConvex(convexGeom)
    }
  }

  private fun computeWidthConvex(convexGeom: Geometry) {
    if (convexGeom is Polygon)
      convexHullPts = convexGeom.getExteriorRing().getCoordinates()
    else
      convexHullPts = convexGeom.getCoordinates()

    // special cases for lines or points or degenerate rings
    if (convexHullPts!!.size == 0) {
      minWidth = 0.0
      minWidthPt = null
      minBaseSeg = null
    } else if (convexHullPts!!.size == 1) {
      minWidth = 0.0
      minWidthPt = convexHullPts!![0]
      minBaseSeg!!.p0 = convexHullPts!![0]
      minBaseSeg!!.p1 = convexHullPts!![0]
    } else if (convexHullPts!!.size == 2 || convexHullPts!!.size == 3) {
      minWidth = 0.0
      minWidthPt = convexHullPts!![0]
      minBaseSeg!!.p0 = convexHullPts!![0]
      minBaseSeg!!.p1 = convexHullPts!![1]
    } else
      computeConvexRingMinDiameter(convexHullPts!!)
  }

  /**
   * Compute the width information for a ring of [Coordinate]s.
   * Leaves the width information in the instance variables.
   *
   * @param pts
   */
  private fun computeConvexRingMinDiameter(pts: Array<Coordinate>) {
    // for each segment in the ring
    minWidth = Double.MAX_VALUE
    var currMaxIndex = 1

    val seg = LineSegment()
    // for each segment, find a vertex at max distance, and pick the minimum
    for (i in 0 until pts.size - 1) {
      seg.p0 = pts[i]
      seg.p1 = pts[i + 1]
      currMaxIndex = findMaxPerpDistance(pts, seg, currMaxIndex)
    }
  }

  private fun findMaxPerpDistance(pts: Array<Coordinate>, seg: LineSegment, startIndex: Int): Int {
    var maxPerpDistance = seg.distancePerpendicular(pts[startIndex])
    var nextPerpDistance = maxPerpDistance
    var maxIndex = startIndex
    var nextIndex = maxIndex
    while (nextPerpDistance >= maxPerpDistance) {
      maxPerpDistance = nextPerpDistance
      maxIndex = nextIndex

      nextIndex = nextIndex(pts, maxIndex)
      if (nextIndex == startIndex)
        break
      nextPerpDistance = seg.distancePerpendicular(pts[nextIndex])
    }
    // found maximum width for this segment - update global min dist if appropriate
    if (maxPerpDistance < minWidth) {
      minPtIndex = maxIndex
      minWidth = maxPerpDistance
      minWidthPt = pts[minPtIndex]
      minBaseSeg = LineSegment(seg)
    }
    return maxIndex
  }

  /**
   * Gets the rectangular [Polygon] which encloses the input geometry
   * and is based on the minimum diameter supporting segment.
   *
   * @return a rectangle enclosing the input (or a line or point if degenerate)
   *
   * @see MinimumAreaRectangle
   */
  fun getMinimumRectangle(): Geometry {
    computeMinimumDiameter()

    // check if minimum rectangle is degenerate (a point or line segment)
    if (minWidth == 0.0) {
      //-- Min rectangle is a point
      if (minBaseSeg!!.p0.equals2D(minBaseSeg!!.p1)) {
        return inputGeom.getFactory().createPoint(minBaseSeg!!.p0.copy())
      }
      //-- Min rectangle is a line. Use the diagonal of the extent
      return computeMaximumLine(convexHullPts!!, inputGeom.getFactory())
    }

    // deltas for the base segment of the minimum diameter
    val dx = minBaseSeg!!.p1.x - minBaseSeg!!.p0.x
    val dy = minBaseSeg!!.p1.y - minBaseSeg!!.p0.y

    var minPara = Double.MAX_VALUE
    var maxPara = -Double.MAX_VALUE
    var minPerp = Double.MAX_VALUE
    var maxPerp = -Double.MAX_VALUE

    // compute maxima and minima of lines parallel and perpendicular to base segment
    for (i in convexHullPts!!.indices) {
      val paraC = computeC(dx, dy, convexHullPts!![i])
      if (paraC > maxPara) maxPara = paraC
      if (paraC < minPara) minPara = paraC

      val perpC = computeC(-dy, dx, convexHullPts!![i])
      if (perpC > maxPerp) maxPerp = perpC
      if (perpC < minPerp) minPerp = perpC
    }

    // compute lines along edges of minimum rectangle
    val maxPerpLine = computeSegmentForLine(-dx, -dy, maxPerp)
    val minPerpLine = computeSegmentForLine(-dx, -dy, minPerp)
    val maxParaLine = computeSegmentForLine(-dy, dx, maxPara)
    val minParaLine = computeSegmentForLine(-dy, dx, minPara)

    // compute vertices of rectangle (where the para/perp max & min lines intersect)
    val p0 = maxParaLine.lineIntersection(maxPerpLine)
    val p1 = minParaLine.lineIntersection(maxPerpLine)
    val p2 = minParaLine.lineIntersection(minPerpLine)
    val p3 = maxParaLine.lineIntersection(minPerpLine)

    val shell = inputGeom.getFactory().createLinearRing(
      arrayOf(p0!!, p1!!, p2!!, p3!!, p0))
    return inputGeom.getFactory().createPolygon(shell)
  }

  companion object {
    /**
     * Gets the minimum-width rectangular [Polygon] which encloses the input geometry
     * and is based along the supporting segment.
     *
     * @param geom the geometry
     * @return the minimum-width rectangle enclosing the geometry
     *
     * @see MinimumAreaRectangle
     */
    @JvmStatic
    fun getMinimumRectangle(geom: Geometry): Geometry {
      return MinimumDiameter(geom).getMinimumRectangle()
    }

    /**
     * Gets the length of the minimum diameter enclosing a geometry
     * @param geom the geometry
     * @return the length of the minimum diameter of the geometry
     */
    @JvmStatic
    fun getMinimumDiameter(geom: Geometry): Geometry {
      return MinimumDiameter(geom).getDiameter()
    }

    private fun nextIndex(pts: Array<Coordinate>, index: Int): Int {
      var index = index
      index++
      if (index >= pts.size) index = 0
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

    private fun computeC(a: Double, b: Double, p: Coordinate): Double {
      return a * p.y - b * p.x
    }

    private fun computeSegmentForLine(a: Double, b: Double, c: Double): LineSegment {
      val p0: Coordinate
      val p1: Coordinate
      /*
      * Line eqn is ax + by = c
      * Slope is a/b.
      * If slope is steep, use y values as the inputs
      */
      if (abs(b) > abs(a)) {
        p0 = Coordinate(0.0, c / b)
        p1 = Coordinate(1.0, c / b - a / b)
      } else {
        p0 = Coordinate(c / a, 0.0)
        p1 = Coordinate(c / a - b / a, 1.0)
      }
      return LineSegment(p0, p1)
    }
  }
}
