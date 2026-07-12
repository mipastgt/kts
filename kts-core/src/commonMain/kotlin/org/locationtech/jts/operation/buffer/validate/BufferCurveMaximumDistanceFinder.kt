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
package org.locationtech.jts.operation.buffer.validate

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.CoordinateFilter
import org.locationtech.jts.geom.CoordinateSequence
import org.locationtech.jts.geom.CoordinateSequenceFilter
import org.locationtech.jts.geom.Geometry

/**
 * Finds the approximate maximum distance from a buffer curve to
 * the originating geometry.
 * This is similar to the Discrete Oriented Hausdorff distance
 * from the buffer curve to the input.
 * <p>
 * The approximate maximum distance is determined by testing
 * all vertices in the buffer curve, as well
 * as midpoints of the curve segments.
 * Due to the way buffer curves are constructed, this
 * should be a very close approximation.
 *
 * @author mbdavis
 *
 */
class BufferCurveMaximumDistanceFinder(private val inputGeom: Geometry) {
  private val maxPtDist = PointPairDistance()

  fun findDistance(bufferCurve: Geometry): Double {
    computeMaxVertexDistance(bufferCurve)
    computeMaxMidpointDistance(bufferCurve)
    return maxPtDist.getDistance()
  }

  fun getDistancePoints(): PointPairDistance {
    return maxPtDist
  }

  private fun computeMaxVertexDistance(curve: Geometry) {
    val distFilter = MaxPointDistanceFilter(inputGeom)
    curve.apply(distFilter)
    maxPtDist.setMaximum(distFilter.getMaxPointDistance())
  }

  private fun computeMaxMidpointDistance(curve: Geometry) {
    val distFilter = MaxMidpointDistanceFilter(inputGeom)
    curve.apply(distFilter)
    maxPtDist.setMaximum(distFilter.getMaxPointDistance())
  }

  class MaxPointDistanceFilter(private val geom: Geometry) : CoordinateFilter {
    private val maxPtDist = PointPairDistance()
    private val minPtDist = PointPairDistance()

    override fun filter(pt: Coordinate) {
      minPtDist.initialize()
      DistanceToPointFinder.computeDistance(geom, pt, minPtDist)
      maxPtDist.setMaximum(minPtDist)
    }

    fun getMaxPointDistance(): PointPairDistance {
      return maxPtDist
    }
  }

  class MaxMidpointDistanceFilter(private val geom: Geometry) : CoordinateSequenceFilter {
    private val maxPtDist = PointPairDistance()
    private val minPtDist = PointPairDistance()

    override fun filter(seq: CoordinateSequence, index: Int) {
      if (index == 0) return

      val p0 = seq.getCoordinate(index - 1)
      val p1 = seq.getCoordinate(index)
      val midPt = Coordinate(
        (p0.x + p1.x) / 2,
        (p0.y + p1.y) / 2
      )

      minPtDist.initialize()
      DistanceToPointFinder.computeDistance(geom, midPt, minPtDist)
      maxPtDist.setMaximum(minPtDist)
    }

    override fun isGeometryChanged(): Boolean {
      return false
    }

    override fun isDone(): Boolean {
      return false
    }

    fun getMaxPointDistance(): PointPairDistance {
      return maxPtDist
    }
  }
}
