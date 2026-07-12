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

import kotlin.jvm.JvmStatic

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryCollection
import org.locationtech.jts.geom.LineString

/**
 * Computes a point in the interior of an linear geometry.
 * <h2>Algorithm</h2>
 * <ul>
 * <li>Find an interior vertex which is closest to
 * the centroid of the linestring.
 * <li>If there is no interior vertex, find the endpoint which is
 * closest to the centroid.
 * </ul>
 *
 * @version 1.7
 */
class InteriorPointLine(g: Geometry) {

  private val centroid: Coordinate? = g.getCentroid().getCoordinate()
  private var minDistance = Double.MAX_VALUE

  private var interiorPoint: Coordinate? = null

  init {
    addInterior(g)
    if (interiorPoint == null)
      addEndpoints(g)
  }

  fun getInteriorPoint(): Coordinate? {
    return interiorPoint
  }

  /**
   * Tests the interior vertices (if any)
   * defined by a linear Geometry for the best inside point.
   * If a Geometry is not of dimension 1 it is not tested.
   * @param geom the geometry to add
   */
  private fun addInterior(geom: Geometry) {
    if (geom.isEmpty())
      return

    if (geom is LineString) {
      addInterior(geom.getCoordinates())
    } else if (geom is GeometryCollection) {
      for (i in 0 until geom.getNumGeometries()) {
        addInterior(geom.getGeometryN(i))
      }
    }
  }

  private fun addInterior(pts: Array<Coordinate>) {
    for (i in 1 until pts.size - 1) {
      add(pts[i])
    }
  }

  /**
   * Tests the endpoint vertices
   * defined by a linear Geometry for the best inside point.
   * If a Geometry is not of dimension 1 it is not tested.
   * @param geom the geometry to add
   */
  private fun addEndpoints(geom: Geometry) {
    if (geom.isEmpty())
      return

    if (geom is LineString) {
      addEndpoints(geom.getCoordinates())
    } else if (geom is GeometryCollection) {
      for (i in 0 until geom.getNumGeometries()) {
        addEndpoints(geom.getGeometryN(i))
      }
    }
  }

  private fun addEndpoints(pts: Array<Coordinate>) {
    add(pts[0])
    add(pts[pts.size - 1])
  }

  private fun add(point: Coordinate) {
    val dist = point.distance(centroid!!)
    if (dist < minDistance) {
      interiorPoint = Coordinate(point)
      minDistance = dist
    }
  }

  companion object {
    /**
     * Computes an interior point for the
     * linear components of a Geometry.
     *
     * @param geom the geometry to compute
     * @return the computed interior point,
     * or <code>null</code> if the geometry has no linear components
     */
    @JvmStatic
    fun getInteriorPoint(geom: Geometry): Coordinate? {
      val intPt = InteriorPointLine(geom)
      return intPt.getInteriorPoint()
    }
  }
}
