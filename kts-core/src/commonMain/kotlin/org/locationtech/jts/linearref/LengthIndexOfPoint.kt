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

package org.locationtech.jts.linearref

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.LineSegment
import org.locationtech.jts.util.Assert

/**
 * Computes the length index of the point
 * on a linear [Geometry] nearest a given [Coordinate].
 * The nearest point is not necessarily unique; this class
 * always computes the nearest point closest to
 * the start of the geometry.
 */
internal class LengthIndexOfPoint(private val linearGeom: Geometry) {

  /**
   * Find the nearest location along a linear [Geometry] to a given point.
   *
   * @param inputPt the coordinate to locate
   * @return the location of the nearest point
   */
  fun indexOf(inputPt: Coordinate): Double {
    return indexOfFromStart(inputPt, -1.0)
  }

  /**
   * Finds the nearest index along the linear [Geometry]
   * to a given [Coordinate]
   * after the specified minimum index.
   * If possible the location returned will be strictly greater than the
   * `minLocation`.
   * If this is not possible, the
   * value returned will equal `minLocation`.
   * (An example where this is not possible is when
   * minLocation = [end of line] ).
   *
   * @param inputPt the coordinate to locate
   * @param minIndex the minimum location for the point location
   * @return the location of the nearest point
   */
  fun indexOfAfter(inputPt: Coordinate, minIndex: Double): Double {
    if (minIndex < 0.0) return indexOf(inputPt)

    // sanity check for minIndex at or past end of line
    val endIndex = linearGeom.getLength()
    if (endIndex < minIndex)
      return endIndex

    val closestAfter = indexOfFromStart(inputPt, minIndex)
    /*
     * Return the minDistanceLocation found.
     */
    Assert.isTrue(closestAfter >= minIndex,
        "computed index is before specified minimum index")
    return closestAfter
  }

  private fun indexOfFromStart(inputPt: Coordinate, minIndex: Double): Double {
    var minDistance = Double.MAX_VALUE

    var ptMeasure = minIndex
    var segmentStartMeasure = 0.0
    val seg = LineSegment()
    val it = LinearIterator(linearGeom)
    while (it.hasNext()) {
      if (!it.isEndOfLine()) {
        seg.p0 = it.getSegmentStart()
        seg.p1 = it.getSegmentEnd()!!
        val segDistance = seg.distance(inputPt)
        val segMeasureToPt = segmentNearestMeasure(seg, inputPt, segmentStartMeasure)
        if (segDistance < minDistance
            && segMeasureToPt > minIndex) {
          ptMeasure = segMeasureToPt
          minDistance = segDistance
        }
        segmentStartMeasure += seg.getLength()
      }
      it.next()
    }
    return ptMeasure
  }

  private fun segmentNearestMeasure(seg: LineSegment, inputPt: Coordinate,
                                    segmentStartMeasure: Double): Double {
    // found new minimum, so compute location distance of point
    val projFactor = seg.projectionFactor(inputPt)
    if (projFactor <= 0.0)
      return segmentStartMeasure
    if (projFactor <= 1.0)
      return segmentStartMeasure + projFactor * seg.getLength()
    // projFactor > 1.0
    return segmentStartMeasure + seg.getLength()
  }

  companion object {
    fun indexOf(linearGeom: Geometry, inputPt: Coordinate): Double {
      val locater = LengthIndexOfPoint(linearGeom)
      return locater.indexOf(inputPt)
    }

    fun indexOfAfter(linearGeom: Geometry, inputPt: Coordinate, minIndex: Double): Double {
      val locater = LengthIndexOfPoint(linearGeom)
      return locater.indexOfAfter(inputPt, minIndex)
    }
  }
}
