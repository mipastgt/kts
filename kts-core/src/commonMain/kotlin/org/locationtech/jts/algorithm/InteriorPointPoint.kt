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
import org.locationtech.jts.geom.Point

/**
 * Computes a point in the interior of an point geometry.
 * <h2>Algorithm</h2>
 * Find a point which is closest to the centroid of the geometry.
 *
 * @version 1.7
 */
class InteriorPointPoint(g: Geometry) {

  private val centroid: Coordinate? = g.getCentroid().getCoordinate()
  private var minDistance = Double.MAX_VALUE

  private var interiorPoint: Coordinate? = null

  init {
    add(g)
  }

  /**
   * Tests the point(s) defined by a Geometry for the best inside point.
   * If a Geometry is not of dimension 0 it is not tested.
   * @param geom the geometry to add
   */
  private fun add(geom: Geometry) {
    if (geom.isEmpty())
      return

    if (geom is Point) {
      add(geom.getCoordinate())
    } else if (geom is GeometryCollection) {
      for (i in 0 until geom.getNumGeometries()) {
        add(geom.getGeometryN(i))
      }
    }
  }

  private fun add(point: Coordinate?) {
    val dist = point!!.distance(centroid!!)
    if (dist < minDistance) {
      interiorPoint = Coordinate(point)
      minDistance = dist
    }
  }

  fun getInteriorPoint(): Coordinate? {
    return interiorPoint
  }

  companion object {
    /**
     * Computes an interior point for the
     * puntal components of a Geometry.
     *
     * @param geom the geometry to compute
     * @return the computed interior point,
     * or <code>null</code> if the geometry has no puntal components
     */
    @JvmStatic
    fun getInteriorPoint(geom: Geometry): Coordinate? {
      val intPt = InteriorPointPoint(geom)
      return intPt.getInteriorPoint()
    }
  }
}
