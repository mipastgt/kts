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
package org.locationtech.jts.algorithm.locate

import kotlin.jvm.JvmStatic

import org.locationtech.jts.algorithm.PointLocation
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryCollection
import org.locationtech.jts.geom.GeometryCollectionIterator
import org.locationtech.jts.geom.LinearRing
import org.locationtech.jts.geom.Location
import org.locationtech.jts.geom.Polygon

/**
 * Computes the location of points
 * relative to a [org.locationtech.jts.geom.Polygonal] [Geometry],
 * using a simple `O(n)` algorithm.
 *
 * The algorithm used reports
 * if a point lies in the interior, exterior,
 * or exactly on the boundary of the Geometry.
 *
 * This algorithm is suitable for use in cases where
 * only a few points will be tested.
 * If many points will be tested,
 * [IndexedPointInAreaLocator] may provide better performance.
 *
 */
class SimplePointInAreaLocator(private val geom: Geometry) : PointOnGeometryLocator {

  /**
   * Determines the [Location] of a point in an areal [Geometry].
   *
   * @param p the point to test
   * @return the Location of the point in the geometry
   */
  override fun locate(p: Coordinate): Int {
    return locate(p, geom)
  }

  companion object {
    /**
     * Determines the [Location] of a point in an areal [Geometry].
     *
     * @param p the point to test
     * @param geom the areal geometry to test
     * @return the Location of the point in the geometry
     */
    @JvmStatic
    fun locate(p: Coordinate, geom: Geometry): Int {
      if (geom.isEmpty()) return Location.EXTERIOR
      /**
       * Do a fast check against the geometry envelope first
       */
      if (!geom.getEnvelopeInternal().intersects(p))
        return Location.EXTERIOR

      return locateInGeometry(p, geom)
    }

    /**
     * Determines whether a point is contained in a [Geometry],
     * or lies on its boundary.
     *
     * @param p the point to test
     * @param geom the geometry to test
     * @return true if the point lies in or on the geometry
     */
    @JvmStatic
    fun isContained(p: Coordinate, geom: Geometry): Boolean {
      return Location.EXTERIOR != locate(p, geom)
    }

    private fun locateInGeometry(p: Coordinate, geom: Geometry): Int {
      if (geom is Polygon) {
        return locatePointInPolygon(p, geom)
      }

      if (geom is GeometryCollection) {
        val geomi = GeometryCollectionIterator(geom)
        while (geomi.hasNext()) {
          val g2 = geomi.next() as Geometry
          if (g2 !== geom) {
            val loc = locateInGeometry(p, g2)
            if (loc != Location.EXTERIOR) return loc
          }
        }
      }
      return Location.EXTERIOR
    }

    /**
     * Determines the [Location] of a point in a [Polygon].
     *
     * This method is provided for backwards compatibility only.
     * Use [locate] instead.
     *
     * @param p the point to test
     * @param poly the geometry to test
     * @return the Location of the point in the polygon
     */
    @JvmStatic
    fun locatePointInPolygon(p: Coordinate, poly: Polygon): Int {
      if (poly.isEmpty()) return Location.EXTERIOR
      val shell = poly.getExteriorRing()
      val shellLoc = locatePointInRing(p, shell)
      if (shellLoc != Location.INTERIOR) return shellLoc

      // now test if the point lies in or on the holes
      for (i in 0 until poly.getNumInteriorRing()) {
        val hole = poly.getInteriorRingN(i)
        val holeLoc = locatePointInRing(p, hole)
        if (holeLoc == Location.BOUNDARY) return Location.BOUNDARY
        if (holeLoc == Location.INTERIOR) return Location.EXTERIOR
        // if in EXTERIOR of this hole keep checking the other ones
      }
      // If not in any hole must be inside polygon
      return Location.INTERIOR
    }

    /**
     * Determines whether a point lies in a [Polygon].
     * If the point lies on the polygon boundary it is
     * considered to be inside.
     *
     * @param p the point to test
     * @param poly the geometry to test
     * @return true if the point lies in or on the polygon
     */
    @JvmStatic
    fun containsPointInPolygon(p: Coordinate, poly: Polygon): Boolean {
      return Location.EXTERIOR != locatePointInPolygon(p, poly)
    }

    /**
     * Determines whether a point lies in a LinearRing,
     * using the ring envelope to short-circuit if possible.
     *
     * @param p the point to test
     * @param ring a linear ring
     * @return true if the point lies inside the ring
     */
    private fun locatePointInRing(p: Coordinate, ring: LinearRing): Int {
      // short-circuit if point is not in ring envelope
      if (!ring.getEnvelopeInternal().intersects(p))
        return Location.EXTERIOR
      return PointLocation.locateInRing(p, ring.getCoordinates())
    }
  }
}
