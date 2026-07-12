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

import kotlin.jvm.JvmStatic

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryCollection
import org.locationtech.jts.geom.LineSegment
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.Polygon

/**
 * Computes the Euclidean distance (L2 metric) from a Point to a Geometry.
 * Also computes two points which are separated by the distance.
 */
class DistanceToPointFinder {

  companion object {
    @JvmStatic
    fun computeDistance(geom: Geometry, pt: Coordinate, ptDist: PointPairDistance) {
      if (geom is LineString) {
        computeDistance(geom, pt, ptDist)
      } else if (geom is Polygon) {
        computeDistance(geom, pt, ptDist)
      } else if (geom is GeometryCollection) {
        for (i in 0 until geom.getNumGeometries()) {
          val g = geom.getGeometryN(i)
          computeDistance(g, pt, ptDist)
        }
      } else { // assume geom is Point
        ptDist.setMinimum(geom.getCoordinate()!!, pt)
      }
    }

    @JvmStatic
    fun computeDistance(line: LineString, pt: Coordinate, ptDist: PointPairDistance) {
      val coords = line.getCoordinates()
      val tempSegment = LineSegment()
      for (i in 0 until coords.size - 1) {
        tempSegment.setCoordinates(coords[i], coords[i + 1])
        // this is somewhat inefficient - could do better
        val closestPt = tempSegment.closestPoint(pt)
        ptDist.setMinimum(closestPt, pt)
      }
    }

    @JvmStatic
    fun computeDistance(segment: LineSegment, pt: Coordinate, ptDist: PointPairDistance) {
      val closestPt = segment.closestPoint(pt)
      ptDist.setMinimum(closestPt, pt)
    }

    @JvmStatic
    fun computeDistance(poly: Polygon, pt: Coordinate, ptDist: PointPairDistance) {
      computeDistance(poly.getExteriorRing(), pt, ptDist)
      for (i in 0 until poly.getNumInteriorRing()) {
        computeDistance(poly.getInteriorRingN(i), pt, ptDist)
      }
    }
  }
}
