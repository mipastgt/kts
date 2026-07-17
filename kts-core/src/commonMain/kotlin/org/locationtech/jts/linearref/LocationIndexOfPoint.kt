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

import kotlin.jvm.JvmStatic

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.LineSegment
import org.locationtech.jts.util.Assert

/**
 * Computes the [LinearLocation] of the point
 * on a linear [Geometry] nearest a given [Coordinate].
 * The nearest point is not necessarily unique; this class
 * always computes the nearest point closest to
 * the start of the geometry.
 */
class LocationIndexOfPoint(private val linearGeom: Geometry) {

  /**
   * Find the nearest location along a linear [Geometry] to a given point.
   *
   * @param inputPt the coordinate to locate
   * @return the location of the nearest point
   */
  fun indexOf(inputPt: Coordinate): LinearLocation {
    return indexOfFromStart(inputPt, null)
  }

  /**
   * Find the nearest [LinearLocation] along the linear [Geometry]
   * to a given [Coordinate]
   * after the specified minimum [LinearLocation].
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
  fun indexOfAfter(inputPt: Coordinate, minIndex: LinearLocation?): LinearLocation {
    if (minIndex == null) return indexOf(inputPt)

    // sanity check for minLocation at or past end of line
    val endLoc = LinearLocation.getEndLocation(linearGeom)
    if (endLoc.compareTo(minIndex) <= 0)
      return endLoc

    val closestAfter = indexOfFromStart(inputPt, minIndex)
    /*
     * Return the minDistanceLocation found.
     * This will not be null, since it was initialized to minLocation
     */
    Assert.isTrue(closestAfter.compareTo(minIndex) >= 0,
        "computed location is before specified minimum location")
    return closestAfter
  }

  private fun indexOfFromStart(inputPt: Coordinate, minIndex: LinearLocation?): LinearLocation {
    var minDistance = Double.MAX_VALUE
    var minComponentIndex = 0
    var minSegmentIndex = 0
    var minFrac = -1.0

    val seg = LineSegment()
    val it = LinearIterator(linearGeom)
    while (it.hasNext()) {
      if (!it.isEndOfLine()) {
        seg.p0 = it.getSegmentStart()
        seg.p1 = it.getSegmentEnd()!!
        val segDistance = seg.distance(inputPt)
        val segFrac = seg.segmentFraction(inputPt)

        val candidateComponentIndex = it.getComponentIndex()
        val candidateSegmentIndex = it.getVertexIndex()
        if (segDistance < minDistance) {
          // ensure after minLocation, if any
          if (minIndex == null ||
              minIndex.compareLocationValues(
                  candidateComponentIndex, candidateSegmentIndex, segFrac)
              < 0
          ) {
            // otherwise, save this as new minimum
            minComponentIndex = candidateComponentIndex
            minSegmentIndex = candidateSegmentIndex
            minFrac = segFrac
            minDistance = segDistance
          }
        }
      }
      it.next()
    }
    if (minDistance == Double.MAX_VALUE) {
      // no minimum was found past minLocation, so return it
      return LinearLocation(minIndex!!)
    }
    // otherwise, return computed location
    val loc = LinearLocation(minComponentIndex, minSegmentIndex, minFrac)
    return loc
  }

  companion object {
    @JvmStatic
    fun indexOf(linearGeom: Geometry, inputPt: Coordinate): LinearLocation {
      val locater = LocationIndexOfPoint(linearGeom)
      return locater.indexOf(inputPt)
    }

    @JvmStatic
    fun indexOfAfter(linearGeom: Geometry, inputPt: Coordinate, minIndex: LinearLocation?): LinearLocation {
      val locater = LocationIndexOfPoint(linearGeom)
      return locater.indexOfAfter(inputPt, minIndex)
    }
  }
}
