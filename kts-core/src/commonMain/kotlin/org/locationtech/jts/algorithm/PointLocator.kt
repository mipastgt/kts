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

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.CoordinateSequence
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryCollection
import org.locationtech.jts.geom.GeometryCollectionIterator
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.LinearRing
import org.locationtech.jts.geom.Location
import org.locationtech.jts.geom.MultiLineString
import org.locationtech.jts.geom.MultiPolygon
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.Polygon

/**
 * Computes the topological ([Location])
 * of a single point to a [Geometry].
 * A [BoundaryNodeRule] may be specified
 * to control the evaluation of whether the point lies on the boundary or not
 * The default rule is to use the the <i>SFS Boundary Determination Rule</i>
 *
 * Instances of this class are not reentrant.
 *
 * @version 1.7
 */
class PointLocator {
  // default is to use OGC SFS rule
  private var boundaryRule: BoundaryNodeRule = BoundaryNodeRule.OGC_SFS_BOUNDARY_RULE

  private var isIn = false // true if the point lies in or on any Geometry element
  private var numBoundaries = 0 // the number of sub-elements whose boundaries the point lies in

  constructor()

  constructor(boundaryRule: BoundaryNodeRule?) {
    if (boundaryRule == null)
      throw IllegalArgumentException("Rule must be non-null")
    this.boundaryRule = boundaryRule
  }

  /**
   * Convenience method to test a point for intersection with
   * a Geometry
   * @param p the coordinate to test
   * @param geom the Geometry to test
   * @return <code>true</code> if the point is in the interior or boundary of the Geometry
   */
  fun intersects(p: Coordinate, geom: Geometry): Boolean {
    return locate(p, geom) != Location.EXTERIOR
  }

  /**
   * Computes the topological relationship ([Location]) of a single point
   * to a Geometry.
   *
   * @return the [Location] of the point relative to the input Geometry
   */
  fun locate(p: Coordinate, geom: Geometry): Int {
    if (geom.isEmpty()) return Location.EXTERIOR

    if (geom is LineString) {
      return locateOnLineString(p, geom)
    } else if (geom is Polygon) {
      return locateInPolygon(p, geom)
    }

    isIn = false
    numBoundaries = 0
    computeLocation(p, geom)
    if (boundaryRule.isInBoundary(numBoundaries))
      return Location.BOUNDARY
    if (numBoundaries > 0 || isIn)
      return Location.INTERIOR

    return Location.EXTERIOR
  }

  private fun computeLocation(p: Coordinate, geom: Geometry) {
    if (geom.isEmpty())
      return

    if (geom is Point) {
      updateLocationInfo(locateOnPoint(p, geom))
    }
    if (geom is LineString) {
      updateLocationInfo(locateOnLineString(p, geom))
    } else if (geom is Polygon) {
      updateLocationInfo(locateInPolygon(p, geom))
    } else if (geom is MultiLineString) {
      for (i in 0 until geom.getNumGeometries()) {
        val l = geom.getGeometryN(i) as LineString
        updateLocationInfo(locateOnLineString(p, l))
      }
    } else if (geom is MultiPolygon) {
      for (i in 0 until geom.getNumGeometries()) {
        val poly = geom.getGeometryN(i) as Polygon
        updateLocationInfo(locateInPolygon(p, poly))
      }
    } else if (geom is GeometryCollection) {
      val geomi = GeometryCollectionIterator(geom)
      while (geomi.hasNext()) {
        val g2 = geomi.next() as Geometry
        if (g2 !== geom)
          computeLocation(p, g2)
      }
    }
  }

  private fun updateLocationInfo(loc: Int) {
    if (loc == Location.INTERIOR) isIn = true
    if (loc == Location.BOUNDARY) numBoundaries++
  }

  private fun locateOnPoint(p: Coordinate, pt: Point): Int {
    // no point in doing envelope test, since equality test is just as fast

    val ptCoord = pt.getCoordinate()
    if (ptCoord!!.equals2D(p))
      return Location.INTERIOR
    return Location.EXTERIOR
  }

  private fun locateOnLineString(p: Coordinate, l: LineString): Int {
    // bounding-box check
    if (!l.getEnvelopeInternal().intersects(p)) return Location.EXTERIOR

    val seq = l.getCoordinateSequence()
    if (p.equals(seq.getCoordinate(0)) ||
        p.equals(seq.getCoordinate(seq.size() - 1))) {
      val boundaryCount = if (l.isClosed()) 2 else 1
      val loc = if (boundaryRule.isInBoundary(boundaryCount)) Location.BOUNDARY else Location.INTERIOR
      return loc
    }
    if (PointLocation.isOnLine(p, seq)) {
      return Location.INTERIOR
    }
    return Location.EXTERIOR
  }

  private fun locateInPolygonRing(p: Coordinate, ring: LinearRing): Int {
    // bounding-box check
    if (!ring.getEnvelopeInternal().intersects(p)) return Location.EXTERIOR

    return PointLocation.locateInRing(p, ring.getCoordinates())
  }

  private fun locateInPolygon(p: Coordinate, poly: Polygon): Int {
    if (poly.isEmpty()) return Location.EXTERIOR

    val shell = poly.getExteriorRing()

    val shellLoc = locateInPolygonRing(p, shell)
    if (shellLoc == Location.EXTERIOR) return Location.EXTERIOR
    if (shellLoc == Location.BOUNDARY) return Location.BOUNDARY
    // now test if the point lies in or on the holes
    for (i in 0 until poly.getNumInteriorRing()) {
      val hole = poly.getInteriorRingN(i)
      val holeLoc = locateInPolygonRing(p, hole)
      if (holeLoc == Location.INTERIOR) return Location.EXTERIOR
      if (holeLoc == Location.BOUNDARY) return Location.BOUNDARY
    }
    return Location.INTERIOR
  }
}
