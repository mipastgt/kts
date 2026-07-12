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
import org.locationtech.jts.geom.CoordinateFilter
import org.locationtech.jts.geom.CoordinateSequence
import org.locationtech.jts.geom.CoordinateSequenceFilter
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.LineSegment
import org.locationtech.jts.geom.LineString

/**
 * Computes the minimum clearance of a geometry or
 * set of geometries.
 *
 * This class uses an inefficient O(N^2) scan.
 * It is primarily for testing purposes.
 *
 * @see MinimumClearance
 * @author Martin Davis
 */
class SimpleMinimumClearance(private val inputGeom: Geometry) {

  private var minClearance = 0.0
  private var minClearancePts: Array<Coordinate?>? = null

  fun getDistance(): Double {
    compute()
    return minClearance
  }

  fun getLine(): LineString {
    compute()
    @Suppress("UNCHECKED_CAST")
    return inputGeom.getFactory().createLineString(minClearancePts as Array<Coordinate>?)
  }

  private fun compute() {
    if (minClearancePts != null) return
    minClearancePts = arrayOfNulls(2)
    minClearance = Double.MAX_VALUE
    inputGeom.apply(VertexCoordinateFilter(this))
  }

  private fun updateClearance(candidateValue: Double, p0: Coordinate, p1: Coordinate) {
    if (candidateValue < minClearance) {
      minClearance = candidateValue
      minClearancePts!![0] = Coordinate(p0)
      minClearancePts!![1] = Coordinate(p1)
    }
  }

  private fun updateClearance(candidateValue: Double, p: Coordinate,
                              seg0: Coordinate, seg1: Coordinate) {
    if (candidateValue < minClearance) {
      minClearance = candidateValue
      minClearancePts!![0] = Coordinate(p)
      val seg = LineSegment(seg0, seg1)
      minClearancePts!![1] = Coordinate(seg.closestPoint(p))
    }
  }

  private class VertexCoordinateFilter(var smc: SimpleMinimumClearance) : CoordinateFilter {
    override fun filter(coord: Coordinate) {
      smc.inputGeom.apply(ComputeMCCoordinateSequenceFilter(smc, coord))
    }
  }

  private class ComputeMCCoordinateSequenceFilter(
      var smc: SimpleMinimumClearance,
      private val queryPt: Coordinate
  ) : CoordinateSequenceFilter {

    override fun filter(seq: CoordinateSequence, i: Int) {
      // compare to vertex
      checkVertexDistance(seq.getCoordinate(i))

      // compare to segment, if this is one
      if (i > 0) {
        checkSegmentDistance(seq.getCoordinate(i - 1), seq.getCoordinate(i))
      }
    }

    private fun checkVertexDistance(vertex: Coordinate) {
      val vertexDist = vertex.distance(queryPt)
      if (vertexDist > 0) {
        smc.updateClearance(vertexDist, queryPt, vertex)
      }
    }

    private fun checkSegmentDistance(seg0: Coordinate, seg1: Coordinate) {
      if (queryPt.equals2D(seg0) || queryPt.equals2D(seg1))
        return
      val segDist = Distance.pointToSegment(queryPt, seg1, seg0)
      if (segDist > 0)
        smc.updateClearance(segDist, queryPt, seg1, seg0)
    }

    override fun isDone(): Boolean {
      return false
    }

    override fun isGeometryChanged(): Boolean {
      return false
    }
  }

  companion object {
    @JvmStatic
    fun getDistance(g: Geometry): Double {
      val rp = SimpleMinimumClearance(g)
      return rp.getDistance()
    }

    @JvmStatic
    fun getLine(g: Geometry): Geometry {
      val rp = SimpleMinimumClearance(g)
      return rp.getLine()
    }
  }
}
