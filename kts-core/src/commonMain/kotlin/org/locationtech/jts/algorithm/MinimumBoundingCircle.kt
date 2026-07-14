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
import kotlin.math.hypot

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.CoordinateArrays
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.Triangle
import org.locationtech.jts.util.Assert

/**
 * Computes the **Minimum Bounding Circle** (MBC)
 * for the points in a [Geometry].
 *
 * @author Martin Davis
 *
 * @see MinimumDiameter
 *
 */
class MinimumBoundingCircle(private val input: Geometry) {
  /*
   * The algorithm used is based on the one by Jon Rokne in
   * the article "An Easy Bounding Circle" in *Graphic Gems II*.
   */

  private var extremalPts: Array<Coordinate>? = null
  private var centre: Coordinate? = null
  private var radius = 0.0

  /**
   * Gets a geometry which represents the Minimum Bounding Circle.
   *
   * @return a Geometry representing the Minimum Bounding Circle.
   */
  fun getCircle(): Geometry {
    compute()
    if (centre == null)
      return input.getFactory().createPolygon()
    val centrePoint = input.getFactory().createPoint(centre)
    if (radius == 0.0)
      return centrePoint
    return centrePoint.buffer(radius)
  }

  /**
   * Gets a geometry representing the maximum diameter of the input.
   *
   * @return a LineString between the two farthest points of the input
   */
  fun getMaximumDiameter(): Geometry {
    compute()
    return when (extremalPts!!.size) {
      0 -> input.getFactory().createLineString()
      1 -> input.getFactory().createPoint(centre)
      2 -> input.getFactory().createLineString(
        arrayOf(extremalPts!![0], extremalPts!![1]))
      else -> { // case 3
        val maxDiameter = farthestPoints(extremalPts!!)
        input.getFactory().createLineString(maxDiameter)
      }
    }
  }

  /**
   * Gets a geometry representing a line between the two farthest points
   * in the input.
   *
   * @return a LineString between the two farthest points of the input
   *
   * @deprecated use #getMaximumDiameter()
   */
  fun getFarthestPoints(): Geometry {
    return getMaximumDiameter()
  }

  /**
   * Gets a geometry representing the diameter of the computed Minimum Bounding
   * Circle.
   *
   * @return the diameter LineString of the Minimum Bounding Circle
   */
  fun getDiameter(): Geometry {
    compute()
    when (extremalPts!!.size) {
      0 -> return input.getFactory().createLineString()
      1 -> return input.getFactory().createPoint(centre)
    }
    // TODO: handle case of 3 extremal points, by computing a line from one of
    // them through the centre point with len = 2*radius
    val p0 = extremalPts!![0]
    val p1 = extremalPts!![1]
    return input.getFactory().createLineString(arrayOf(p0, p1))
  }

  /**
   * Gets the extremal points which define the computed Minimum Bounding Circle.
   *
   * @return the points defining the Minimum Bounding Circle
   */
  fun getExtremalPoints(): Array<Coordinate> {
    compute()
    return extremalPts!!
  }

  /**
   * Gets the centre point of the computed Minimum Bounding Circle.
   *
   * @return the centre point of the Minimum Bounding Circle
   */
  fun getCentre(): Coordinate? {
    compute()
    return centre
  }

  /**
   * Gets the radius of the computed Minimum Bounding Circle.
   *
   * @return the radius of the Minimum Bounding Circle
   */
  fun getRadius(): Double {
    compute()
    return radius
  }

  private fun computeCentre() {
    when (extremalPts!!.size) {
      0 -> centre = null
      1 -> centre = extremalPts!![0]
      2 -> centre = Coordinate(
        (extremalPts!![0].x + extremalPts!![1].x) / 2.0,
        (extremalPts!![0].y + extremalPts!![1].y) / 2.0)
      3 -> centre = Triangle.circumcentre(extremalPts!![0], extremalPts!![1], extremalPts!![2])
    }
  }

  private fun compute() {
    if (extremalPts != null) return

    computeCirclePoints()
    computeCentre()
    if (centre != null)
      radius = centre!!.distance(extremalPts!![0])
  }

  private fun computeCirclePoints() {
    // handle degenerate or trivial cases
    if (input.isEmpty()) {
      extremalPts = arrayOf()
      return
    }
    if (input.getNumPoints() == 1) {
      val pts = input.getCoordinates()
      extremalPts = arrayOf(Coordinate(pts[0]))
      return
    }

    /**
     * The problem is simplified by reducing to the convex hull.
     * Computing the convex hull also has the useful effect of eliminating duplicate points
     */
    val convexHull = input.convexHull()

    val hullPts = convexHull.getCoordinates()

    // strip duplicate final point, if any
    var pts: Array<Coordinate> = hullPts
    if (hullPts[0].equals2D(hullPts[hullPts.size - 1])) {
      @Suppress("UNCHECKED_CAST")
      pts = arrayOfNulls<Coordinate>(hullPts.size - 1) as Array<Coordinate>
      CoordinateArrays.copyDeep(hullPts, 0, pts, 0, hullPts.size - 1)
    }

    /**
     * Optimization for the trivial case where the CH has fewer than 3 points
     */
    if (pts.size <= 2) {
      extremalPts = CoordinateArrays.copyDeep(pts)
      return
    }

    // find a point P with minimum Y ordinate
    var P = lowestPoint(pts)

    // find a point Q such that the angle that PQ makes with the x-axis is minimal
    var Q = pointWitMinAngleWithX(pts, P)!!

    /**
     * Iterate over the remaining points to find
     * a pair or triplet of points which determine the minimal circle.
     */
    for (i in 0 until pts.size) {
      val R = pointWithMinAngleWithSegment(pts, P, Q)!!

      if (Angle.isObtuse(P, R, Q)) {
        // if PRQ is obtuse, then MBC is determined by P and Q
        extremalPts = arrayOf(Coordinate(P), Coordinate(Q))
        return
      } else if (Angle.isObtuse(R, P, Q)) {
        // if RPQ is obtuse, update baseline and iterate
        P = R
        continue
      } else if (Angle.isObtuse(R, Q, P)) {
        // if RQP is obtuse, update baseline and iterate
        Q = R
        continue
      } else {
        // otherwise all angles are acute, and the MBC is determined by the triangle PQR
        extremalPts = arrayOf(Coordinate(P), Coordinate(Q), Coordinate(R))
        return
      }
    }
    Assert.shouldNeverReachHere("Logic failure in Minimum Bounding Circle algorithm!")
  }

  companion object {
    /**
     * Finds the farthest pair out of 3 extremal points
     * @param pts the array of extremal points
     * @return the pair of farthest points
     */
    private fun farthestPoints(pts: Array<Coordinate>): Array<Coordinate> {
      val dist01 = pts[0].distance(pts[1])
      val dist12 = pts[1].distance(pts[2])
      val dist20 = pts[2].distance(pts[0])
      if (dist01 >= dist12 && dist01 >= dist20) {
        return arrayOf(pts[0], pts[1])
      }
      if (dist12 >= dist01 && dist12 >= dist20) {
        return arrayOf(pts[1], pts[2])
      }
      return arrayOf(pts[2], pts[0])
    }

    private fun lowestPoint(pts: Array<Coordinate>): Coordinate {
      var min = pts[0]
      for (i in 1 until pts.size) {
        if (pts[i].y < min.y)
          min = pts[i]
      }
      return min
    }

    private fun pointWitMinAngleWithX(pts: Array<Coordinate>, P: Coordinate): Coordinate? {
      var minSin = Double.MAX_VALUE
      var minAngPt: Coordinate? = null
      for (i in 0 until pts.size) {
        val p = pts[i]
        if (p === P) continue

        /**
         * The sin of the angle is a simpler proxy for the angle itself
         */
        val dx = p.x - P.x
        var dy = p.y - P.y
        if (dy < 0) dy = -dy
        val len = hypot(dx, dy)
        val sin = dy / len

        if (sin < minSin) {
          minSin = sin
          minAngPt = p
        }
      }
      return minAngPt
    }

    private fun pointWithMinAngleWithSegment(pts: Array<Coordinate>, P: Coordinate, Q: Coordinate): Coordinate? {
      var minAng = Double.MAX_VALUE
      var minAngPt: Coordinate? = null
      for (i in 0 until pts.size) {
        val p = pts[i]
        if (p === P) continue
        if (p === Q) continue

        val ang = Angle.angleBetween(P, p, Q)
        if (ang < minAng) {
          minAng = ang
          minAngPt = p
        }
      }
      return minAngPt
    }
  }
}
