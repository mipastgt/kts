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
import kotlin.jvm.JvmField
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.PrecisionModel
import org.locationtech.jts.io.WKTWriter
import org.locationtech.jts.util.Assert

/**
 * A <code>LineIntersector</code> is an algorithm that can both test whether
 * two line segments intersect and compute the intersection point(s)
 * if they do.
 *
 * @version 1.7
 */
abstract class LineIntersector {

  @JvmField
  protected var result: Int = 0

  @JvmField
  protected var inputLines: Array<Array<Coordinate?>> = Array(2) { arrayOfNulls<Coordinate>(2) }

  @JvmField
  protected var intPt: Array<Coordinate> = Array(2) { Coordinate() }

  /**
   * The indexes of the endpoints of the intersection lines, in order along
   * the corresponding line
   */
  @JvmField
  protected var intLineIndex: Array<IntArray>? = null

  @JvmField
  protected var isProper: Boolean = false

  @JvmField
  protected var pa: Coordinate

  @JvmField
  protected var pb: Coordinate

  /**
   * If makePrecise is true, computed intersection coordinates will be made precise
   * using Coordinate#makePrecise
   */
  @JvmField
  protected var precisionModel: PrecisionModel? = null

  init {
    // alias the intersection points for ease of reference
    pa = intPt[0]
    pb = intPt[1]
    result = 0
  }

  /**
   * Force computed intersection to be rounded to a given precision model
   * @param precisionModel
   * @deprecated use <code>setPrecisionModel</code> instead
   */
  open fun setMakePrecise(precisionModel: PrecisionModel?) {
    this.precisionModel = precisionModel
  }

  /**
   * Force computed intersection to be rounded to a given precision model.
   * @param precisionModel
   */
  open fun setPrecisionModel(precisionModel: PrecisionModel?) {
    this.precisionModel = precisionModel
  }

  /**
   * Gets an endpoint of an input segment.
   *
   * @param segmentIndex the index of the input segment (0 or 1)
   * @param ptIndex the index of the endpoint (0 or 1)
   * @return the specified endpoint
   */
  open fun getEndpoint(segmentIndex: Int, ptIndex: Int): Coordinate? {
    return inputLines[segmentIndex][ptIndex]
  }

  /**
   * Compute the intersection of a point p and the line p1-p2.
   */
  abstract fun computeIntersection(p: Coordinate, p1: Coordinate, p2: Coordinate)

  protected open fun isCollinear(): Boolean {
    return result == COLLINEAR_INTERSECTION
  }

  /**
   * Computes the intersection of the lines p1-p2 and p3-p4.
   */
  open fun computeIntersection(p1: Coordinate, p2: Coordinate, p3: Coordinate, p4: Coordinate) {
    inputLines[0][0] = p1
    inputLines[0][1] = p2
    inputLines[1][0] = p3
    inputLines[1][1] = p4
    result = computeIntersect(p1, p2, p3, p4)
  }

  protected abstract fun computeIntersect(p1: Coordinate, p2: Coordinate, q1: Coordinate, q2: Coordinate): Int

  override fun toString(): String {
    return WKTWriter.toLineString(inputLines[0][0]!!, inputLines[0][1]!!) + " - " +
        WKTWriter.toLineString(inputLines[1][0]!!, inputLines[1][1]!!) +
        getTopologySummary()
  }

  private fun getTopologySummary(): String {
    val catBuilder = StringBuilder()
    if (isEndPoint()) catBuilder.append(" endpoint")
    if (isProper) catBuilder.append(" proper")
    if (isCollinear()) catBuilder.append(" collinear")
    return catBuilder.toString()
  }

  protected open fun isEndPoint(): Boolean {
    return hasIntersection() && !isProper
  }

  /**
   * Tests whether the input geometries intersect.
   *
   * @return true if the input geometries intersect
   */
  open fun hasIntersection(): Boolean {
    return result != NO_INTERSECTION
  }

  /**
   * Returns the number of intersection points found.  This will be either 0, 1 or 2.
   *
   * @return the number of intersection points found (0, 1, or 2)
   */
  open fun getIntersectionNum(): Int {
    return result
  }

  /**
   * Returns the intIndex'th intersection point
   *
   * @param intIndex is 0 or 1
   *
   * @return the intIndex'th intersection point
   */
  open fun getIntersection(intIndex: Int): Coordinate {
    return intPt[intIndex]
  }

  protected open fun computeIntLineIndex() {
    if (intLineIndex == null) {
      intLineIndex = Array(2) { IntArray(2) }
      computeIntLineIndex(0)
      computeIntLineIndex(1)
    }
  }

  /**
   * Test whether a point is a intersection point of two line segments.
   *
   * @return true if the input point is one of the intersection points.
   */
  open fun isIntersection(pt: Coordinate): Boolean {
    for (i in 0 until result) {
      if (intPt[i].equals2D(pt)) {
        return true
      }
    }
    return false
  }

  /**
   * Tests whether either intersection point is an interior point of one of the input segments.
   *
   * @return <code>true</code> if either intersection point is in the interior of one of the input segments
   */
  open fun isInteriorIntersection(): Boolean {
    if (isInteriorIntersection(0)) return true
    if (isInteriorIntersection(1)) return true
    return false
  }

  /**
   * Tests whether either intersection point is an interior point of the specified input segment.
   *
   * @return <code>true</code> if either intersection point is in the interior of the input segment
   */
  open fun isInteriorIntersection(inputLineIndex: Int): Boolean {
    for (i in 0 until result) {
      if (!(intPt[i].equals2D(inputLines[inputLineIndex][0]!!) ||
            intPt[i].equals2D(inputLines[inputLineIndex][1]!!))) {
        return true
      }
    }
    return false
  }

  /**
   * Tests whether an intersection is proper.
   *
   * @return true if the intersection is proper
   */
  open fun isProper(): Boolean {
    return hasIntersection() && isProper
  }

  /**
   * Computes the intIndex'th intersection point in the direction of
   * a specified input line segment
   *
   * @param segmentIndex is 0 or 1
   * @param intIndex is 0 or 1
   *
   * @return the intIndex'th intersection point in the direction of the specified input line segment
   */
  open fun getIntersectionAlongSegment(segmentIndex: Int, intIndex: Int): Coordinate {
    // lazily compute int line array
    computeIntLineIndex()
    return intPt[intLineIndex!![segmentIndex][intIndex]]
  }

  /**
   * Computes the index (order) of the intIndex'th intersection point in the direction of
   * a specified input line segment
   *
   * @param segmentIndex is 0 or 1
   * @param intIndex is 0 or 1
   *
   * @return the index of the intersection point along the input segment (0 or 1)
   */
  open fun getIndexAlongSegment(segmentIndex: Int, intIndex: Int): Int {
    computeIntLineIndex()
    return intLineIndex!![segmentIndex][intIndex]
  }

  protected open fun computeIntLineIndex(segmentIndex: Int) {
    val dist0 = getEdgeDistance(segmentIndex, 0)
    val dist1 = getEdgeDistance(segmentIndex, 1)
    if (dist0 > dist1) {
      intLineIndex!![segmentIndex][0] = 0
      intLineIndex!![segmentIndex][1] = 1
    } else {
      intLineIndex!![segmentIndex][0] = 1
      intLineIndex!![segmentIndex][1] = 0
    }
  }

  /**
   * Computes the "edge distance" of an intersection point along the specified input line segment.
   *
   * @param segmentIndex is 0 or 1
   * @param intIndex is 0 or 1
   *
   * @return the edge distance of the intersection point
   */
  open fun getEdgeDistance(segmentIndex: Int, intIndex: Int): Double {
    val dist = computeEdgeDistance(intPt[intIndex], inputLines[segmentIndex][0]!!, inputLines[segmentIndex][1]!!)
    return dist
  }

  companion object {
    /**
     * These are deprecated, due to ambiguous naming
     */
    const val DONT_INTERSECT = 0
    const val DO_INTERSECT = 1
    const val COLLINEAR = 2

    /**
     * Indicates that line segments do not intersect
     */
    const val NO_INTERSECTION = 0

    /**
     * Indicates that line segments intersect in a single point
     */
    const val POINT_INTERSECTION = 1

    /**
     * Indicates that line segments intersect in a line segment
     */
    const val COLLINEAR_INTERSECTION = 2

    /**
     * Computes the "edge distance" of an intersection point p along a segment.
     */
    @JvmStatic
    fun computeEdgeDistance(p: Coordinate, p0: Coordinate, p1: Coordinate): Double {
      val dx = abs(p1.x - p0.x)
      val dy = abs(p1.y - p0.y)

      var dist = -1.0 // sentinel value
      if (p.equals(p0)) {
        dist = 0.0
      } else if (p.equals(p1)) {
        if (dx > dy)
          dist = dx
        else
          dist = dy
      } else {
        val pdx = abs(p.x - p0.x)
        val pdy = abs(p.y - p0.y)
        if (dx > dy)
          dist = pdx
        else
          dist = pdy
        // <FIX>
        // hack to ensure that non-endpoints always have a non-zero distance
        if (dist == 0.0 && !p.equals(p0)) {
          dist = max(pdx, pdy)
        }
      }
      Assert.isTrue(!(dist == 0.0 && !p.equals(p0)), "Bad distance calculation")
      return dist
    }

    @JvmStatic
    fun nonRobustComputeEdgeDistance(p: Coordinate, p1: Coordinate, p2: Coordinate): Double {
      val dx = p.x - p1.x
      val dy = p.y - p1.y
      val dist = hypot(dx, dy) // dummy value
      Assert.isTrue(!(dist == 0.0 && !p.equals(p1)), "Invalid distance calculation")
      return dist
    }
  }
}
