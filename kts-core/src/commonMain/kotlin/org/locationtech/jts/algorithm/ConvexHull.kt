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

import kotlin.jvm.JvmSuppressWildcards


import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.CoordinateArrays
import org.locationtech.jts.geom.CoordinateList
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.LinearRing
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.Polygon
import org.locationtech.jts.util.Assert

/**
 * Computes the convex hull of a [Geometry].
 * The convex hull is the smallest convex Geometry that contains all the
 * points in the input Geometry.
 *
 * Uses the Graham Scan algorithm.
 *
 *@version 1.7
 */
open class ConvexHull {

  private val geomFactory: GeometryFactory
  private val inputPts: Array<Coordinate>

  /**
   * Create a new convex hull construction for the input [Geometry].
   */
  constructor(geometry: Geometry) : this(geometry.getCoordinates(), geometry.getFactory())

  /**
   * Create a new convex hull construction for the input [Coordinate] array.
   */
  constructor(pts: Array<Coordinate>, geomFactory: GeometryFactory) {
    inputPts = pts
    this.geomFactory = geomFactory
  }

  /**
   * Returns a [Geometry] that represents the convex hull of the input
   * geometry.
   *
   * @return if the convex hull contains 3 or more points, a [Polygon];
   * 2 points, a [LineString];
   * 1 point, a [Point];
   * 0 points, an empty [org.locationtech.jts.geom.GeometryCollection].
   */
  fun getConvexHull(): Geometry {
    val fewPointsGeom = createFewPointsResult()
    if (fewPointsGeom != null)
      return fewPointsGeom

    var reducedPts = inputPts
    //-- use heuristic to reduce points, if large
    if (inputPts.size > TUNING_REDUCE_SIZE) {
      reducedPts = reduce(inputPts)
    } else {
      //-- the points must be made unique
      reducedPts = extractUnique(inputPts)
    }
    // sort points for Graham scan.
    val sortedPts = preSort(reducedPts)

    // Use Graham scan to find convex hull.
    val cHS = grahamScan(sortedPts)

    // Convert stack to an array.
    val cH = toCoordinateArray(cHS)

    // Convert array to appropriate output geometry.
    // (an empty or point result will be detected earlier)
    return lineOrPolygon(cH)
  }

  /**
   * Checks if there are <= 2 unique points,
   * which produce an obviously degenerate result.
   *
   * @return a degenerate hull geometry, or null if the number of input points is large
   */
  private fun createFewPointsResult(): Geometry? {
    val uniquePts = extractUnique(inputPts, 2)
    if (uniquePts == null) {
      return null
    } else if (uniquePts.size == 0) {
      return geomFactory.createGeometryCollection()
    } else if (uniquePts.size == 1) {
      return geomFactory.createPoint(uniquePts[0])
    } else {
      return geomFactory.createLineString(uniquePts)
    }
  }

  /**
   * An alternative to Stack.toArray, which is not present in earlier versions
   * of Java.
   */
  protected open fun toCoordinateArray(stack: @JvmSuppressWildcards List<Coordinate>): Array<Coordinate> {
    return Array(stack.size) { i -> stack[i] }
  }

  /**
   * Uses a heuristic to reduce the number of points scanned
   * to compute the hull.
   *
   * @param inputPts the points to reduce
   * @return the reduced list of points (at least 3)
   */
  private fun reduce(inputPts: Array<Coordinate>): Array<Coordinate> {
    val innerPolyPts = computeInnerOctolateralRing(inputPts)

    // unable to compute interior polygon for some reason
    if (innerPolyPts == null)
      return inputPts

    // add points defining polygon
    val reducedSet = HashSet<Coordinate>()
    for (i in innerPolyPts.indices) {
      reducedSet.add(innerPolyPts[i])
    }
    /**
     * Add all unique points not in the interior poly.
     */
    for (i in inputPts.indices) {
      if (!PointLocation.isInRing(inputPts[i], innerPolyPts)) {
        reducedSet.add(inputPts[i])
      }
    }
    val reducedPts = CoordinateArrays.toCoordinateArray(reducedSet)

    // ensure that computed array has at least 3 points (not necessarily unique)
    if (reducedPts.size < 3)
      return padArray3(reducedPts)
    return reducedPts
  }

  private fun padArray3(pts: Array<Coordinate>): Array<Coordinate> {
    return Array(3) { i ->
      if (i < pts.size) pts[i] else pts[0]
    }
  }

  /**
   * Sorts the points radially CW around the point with minimum Y and then X.
   *
   * @param pts the points to sort
   * @return the sorted points
   */
  private fun preSort(pts: Array<Coordinate>): Array<Coordinate> {
    var t: Coordinate

    /**
     * find the lowest point in the set. If two or more points have
     * the same minimum Y coordinate choose the one with the minimum X.
     * This focal point is put in array location pts[0].
     */
    for (i in 1 until pts.size) {
      if ((pts[i].y < pts[0].y) || ((pts[i].y == pts[0].y) && (pts[i].x < pts[0].x))) {
        t = pts[0]
        pts[0] = pts[i]
        pts[i] = t
      }
    }

    // sort the points radially around the focal point.
    pts.sortWith(RadialComparator(pts[0]), 1, pts.size)

    return pts
  }

  /**
   * Uses the Graham Scan algorithm to compute the convex hull vertices.
   *
   * @param c a list of points, with at least 3 entries
   * @return a Stack containing the ordered points of the convex hull ring
   */
  private fun grahamScan(c: Array<Coordinate>): ArrayDeque<Coordinate> {
    var p: Coordinate
    val ps = ArrayDeque<Coordinate>()
    ps.addLast(c[0])
    ps.addLast(c[1])
    ps.addLast(c[2])
    for (i in 3 until c.size) {
      val cp = c[i]
      p = ps.removeLast()
      // check for empty stack to guard against robustness problems
      while (!ps.isEmpty() &&
          Orientation.index(ps.last(), p, cp) > 0) {
        p = ps.removeLast()
      }
      ps.addLast(p)
      ps.addLast(cp)
    }
    ps.addLast(c[0])
    return ps
  }

  /**
   *@return    whether the three coordinates are collinear and c2 lies between
   *      c1 and c3 inclusive
   */
  private fun isBetween(c1: Coordinate, c2: Coordinate, c3: Coordinate): Boolean {
    if (Orientation.index(c1, c2, c3) != 0) {
      return false
    }
    if (c1.x != c3.x) {
      if (c1.x <= c2.x && c2.x <= c3.x) {
        return true
      }
      if (c3.x <= c2.x && c2.x <= c1.x) {
        return true
      }
    }
    if (c1.y != c3.y) {
      if (c1.y <= c2.y && c2.y <= c3.y) {
        return true
      }
      if (c3.y <= c2.y && c2.y <= c1.y) {
        return true
      }
    }
    return false
  }

  private fun computeInnerOctolateralRing(inputPts: Array<Coordinate>): Array<Coordinate>? {
    val octPts = computeInnerOctolateralPts(inputPts)
    val coordList = CoordinateList()
    coordList.add(octPts, false)

    // points must all lie in a line
    if (coordList.size < 3) {
      return null
    }
    coordList.closeRing()
    return coordList.toCoordinateArray()
  }

  /**
   * Computes the extremal points of an inner octolateral.
   * Some points may be duplicates - these are collapsed later.
   *
   * @param inputPts the points to compute the octolateral for
   * @return the extremal points of the octolateral
   */
  private fun computeInnerOctolateralPts(inputPts: Array<Coordinate>): Array<Coordinate> {
    val pts = Array(8) { inputPts[0] }
    for (i in 1 until inputPts.size) {
      if (inputPts[i].x < pts[0].x) {
        pts[0] = inputPts[i]
      }
      if (inputPts[i].x - inputPts[i].y < pts[1].x - pts[1].y) {
        pts[1] = inputPts[i]
      }
      if (inputPts[i].y > pts[2].y) {
        pts[2] = inputPts[i]
      }
      if (inputPts[i].x + inputPts[i].y > pts[3].x + pts[3].y) {
        pts[3] = inputPts[i]
      }
      if (inputPts[i].x > pts[4].x) {
        pts[4] = inputPts[i]
      }
      if (inputPts[i].x - inputPts[i].y > pts[5].x - pts[5].y) {
        pts[5] = inputPts[i]
      }
      if (inputPts[i].y < pts[6].y) {
        pts[6] = inputPts[i]
      }
      if (inputPts[i].x + inputPts[i].y < pts[7].x + pts[7].y) {
        pts[7] = inputPts[i]
      }
    }
    return pts
  }

  /**
   *@param  coordinates  the vertices of a linear ring, which may or may not be
   *      flattened (i.e. vertices collinear)
   *@return           a 2-vertex <code>LineString</code> if the vertices are
   *      collinear; otherwise, a <code>Polygon</code> with unnecessary
   *      (collinear) vertices removed
   */
  private fun lineOrPolygon(coordinates: Array<Coordinate>): Geometry {
    val coords = cleanRing(coordinates)
    if (coords.size == 3) {
      return geomFactory.createLineString(arrayOf(coords[0], coords[1]))
    }
    val linearRing = geomFactory.createLinearRing(coords)
    return geomFactory.createPolygon(linearRing)
  }

  /**
   * Cleans a list of points by removing interior collinear vertices.
   *
   * @param  original  the vertices of a linear ring, which may or may not be
   *      flattened (i.e. vertices collinear)
   * @return the coordinates with unnecessary (collinear) vertices removed
   */
  private fun cleanRing(original: Array<Coordinate>): Array<Coordinate> {
    Assert.equals(original[0], original[original.size - 1])
    val cleanedRing = ArrayList<Coordinate>()
    var previousDistinctCoordinate: Coordinate? = null
    for (i in 0..original.size - 2) {
      val currentCoordinate = original[i]
      val nextCoordinate = original[i + 1]
      if (currentCoordinate.equals(nextCoordinate)) {
        continue
      }
      if (previousDistinctCoordinate != null &&
          isBetween(previousDistinctCoordinate, currentCoordinate, nextCoordinate)) {
        continue
      }
      cleanedRing.add(currentCoordinate)
      previousDistinctCoordinate = currentCoordinate
    }
    cleanedRing.add(original[original.size - 1])
    return cleanedRing.toTypedArray()
  }

  /**
   * Compares [Coordinate]s for their angle and distance
   * relative to an origin.
   *
   * @author Martin Davis
   * @version 1.7
   */
  private class RadialComparator(private val origin: Coordinate) : Comparator<Coordinate> {

    override fun compare(p1: Coordinate, p2: Coordinate): Int {
      val comp = polarCompare(origin, p1, p2)
      return comp
    }

    companion object {
      /**
       * Given two points p and q compare them with respect to their radial
       * ordering about point o.
       *
       * @param o the origin
       * @param p a point
       * @param q another point
       * @return -1, 0 or 1 depending on whether p is less than,
       * equal to or greater than q
       */
      private fun polarCompare(o: Coordinate, p: Coordinate, q: Coordinate): Int {
        val orient = Orientation.index(o, p, q)
        if (orient == Orientation.COUNTERCLOCKWISE) return 1
        if (orient == Orientation.CLOCKWISE) return -1

        /**
         * The points are collinear,
         * so compare based on distance from the origin.
         */
        if (p.y > q.y) return 1
        if (p.y < q.y) return -1

        /**
         * The points lie in a horizontal line.
         * Use the X ordinate to determine distance.
         */
        if (p.x > q.x) return 1
        if (p.x < q.x) return -1
        // Assert: p = q
        return 0
      }
    }
  }

  companion object {
    private const val TUNING_REDUCE_SIZE = 50

    private fun extractUnique(pts: Array<Coordinate>): Array<Coordinate> {
      return extractUnique(pts, -1)!!
    }

    /**
     * Extracts unique coordinates from an array of coordinates,
     * up to an (optional) maximum count of values.
     *
     * @param pts an array of Coordinates
     * @param maxPts the maximum number of unique points to scan, or -1
     * @return an array of unique values, or null
     */
    private fun extractUnique(pts: Array<Coordinate>, maxPts: Int): Array<Coordinate>? {
      val uniquePts = HashSet<Coordinate>()
      for (pt in pts) {
        uniquePts.add(pt)
        //-- if maxPts is provided, exit if more unique pts found
        if (maxPts >= 0 && uniquePts.size > maxPts) return null
      }
      return CoordinateArrays.toCoordinateArray(uniquePts)
    }
  }
}
