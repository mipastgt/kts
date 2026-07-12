/*
 * Copyright (c) 2016 Martin Davis.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.locationtech.jts.precision

import kotlin.jvm.JvmStatic

import org.locationtech.jts.algorithm.Distance
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.LineSegment
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.index.strtree.ItemBoundable
import org.locationtech.jts.index.strtree.ItemDistance
import org.locationtech.jts.index.strtree.STRtree
import org.locationtech.jts.operation.distance.FacetSequence
import org.locationtech.jts.operation.distance.FacetSequenceTreeBuilder

/**
 * Computes the Minimum Clearance of a [Geometry].
 *
 * @author Martin Davis
 */
class MinimumClearance(private val inputGeom: Geometry) {

  private var minClearance = 0.0
  private var minClearancePts: Array<Coordinate?>? = null

  /**
   * Gets the Minimum Clearance distance.
   *
   * @return the value of the minimum clearance distance
   * or `Double.MAX_VALUE` if no Minimum Clearance distance exists
   */
  fun getDistance(): Double {
    compute()
    return minClearance
  }

  /**
   * Gets a LineString containing two points
   * which are at the Minimum Clearance distance.
   *
   * @return the value of the minimum clearance distance
   * or `LINESTRING EMPTY` if no Minimum Clearance distance exists
   */
  fun getLine(): LineString {
    compute()
    // return empty line string if no min pts where found
    val pts = minClearancePts
    if (pts == null || pts[0] == null)
      return inputGeom.getFactory().createLineString()
    @Suppress("UNCHECKED_CAST")
    return inputGeom.getFactory().createLineString(pts as Array<Coordinate>)
  }

  private fun compute() {
    // already computed
    if (minClearancePts != null) return

    // initialize to "No Distance Exists" state
    minClearancePts = arrayOfNulls(2)
    minClearance = Double.MAX_VALUE

    // handle empty geometries
    if (inputGeom.isEmpty()) {
      return
    }

    val geomTree = FacetSequenceTreeBuilder.build(inputGeom)

    val nearest = geomTree.nearestNeighbour(MinClearanceDistance())!!
    val mcd = MinClearanceDistance()
    minClearance = mcd.distance(
        nearest[0] as FacetSequence,
        nearest[1] as FacetSequence)
    minClearancePts = mcd.getCoordinates()
  }

  /**
   * Implements the MinimumClearance distance function.
   *
   * @author Martin Davis
   */
  private class MinClearanceDistance : ItemDistance {
    private var minDist = Double.MAX_VALUE
    private val minPts = arrayOfNulls<Coordinate>(2)

    fun getCoordinates(): Array<Coordinate?> {
      return minPts
    }

    override fun distance(b1: ItemBoundable, b2: ItemBoundable): Double {
      val fs1 = b1.getItem() as FacetSequence
      val fs2 = b2.getItem() as FacetSequence
      minDist = Double.MAX_VALUE
      return distance(fs1, fs2)
    }

    fun distance(fs1: FacetSequence, fs2: FacetSequence): Double {
      // compute MinClearance distance metric

      vertexDistance(fs1, fs2)
      if (fs1.size() == 1 && fs2.size() == 1) return minDist
      if (minDist <= 0.0) return minDist
      segmentDistance(fs1, fs2)
      if (minDist <= 0.0) return minDist
      segmentDistance(fs2, fs1)
      return minDist
    }

    private fun vertexDistance(fs1: FacetSequence, fs2: FacetSequence): Double {
      for (i1 in 0 until fs1.size()) {
        for (i2 in 0 until fs2.size()) {
          val p1 = fs1.getCoordinate(i1)
          val p2 = fs2.getCoordinate(i2)
          if (!p1.equals2D(p2)) {
            val d = p1.distance(p2)
            if (d < minDist) {
              minDist = d
              minPts[0] = p1
              minPts[1] = p2
              if (d == 0.0)
                return d
            }
          }
        }
      }
      return minDist
    }

    private fun segmentDistance(fs1: FacetSequence, fs2: FacetSequence): Double {
      for (i1 in 0 until fs1.size()) {
        for (i2 in 1 until fs2.size()) {

          val p = fs1.getCoordinate(i1)

          val seg0 = fs2.getCoordinate(i2 - 1)
          val seg1 = fs2.getCoordinate(i2)

          if (!(p.equals2D(seg0) || p.equals2D(seg1))) {
            val d = Distance.pointToSegment(p, seg0, seg1)
            if (d < minDist) {
              minDist = d
              updatePts(p, seg0, seg1)
              if (d == 0.0)
                return d
            }
          }
        }
      }
      return minDist
    }

    private fun updatePts(p: Coordinate, seg0: Coordinate, seg1: Coordinate) {
      minPts[0] = p
      val seg = LineSegment(seg0, seg1)
      minPts[1] = Coordinate(seg.closestPoint(p))
    }
  }

  companion object {
    /**
     * Computes the Minimum Clearance distance for
     * the given Geometry.
     *
     * @param g the input geometry
     * @return the Minimum Clearance distance
     */
    @JvmStatic
    fun getDistance(g: Geometry): Double {
      val rp = MinimumClearance(g)
      return rp.getDistance()
    }

    /**
     * Gets a LineString containing two points
     * which are at the Minimum Clearance distance
     * for the given Geometry.
     *
     * @param g the input geometry
     * @return the value of the minimum clearance distance
     * or `LINESTRING EMPTY` if no Minimum Clearance distance exists
     */
    @JvmStatic
    fun getLine(g: Geometry): Geometry {
      val rp = MinimumClearance(g)
      return rp.getLine()
    }
  }
}
