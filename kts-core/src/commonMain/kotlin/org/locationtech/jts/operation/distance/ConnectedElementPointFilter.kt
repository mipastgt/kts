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
package org.locationtech.jts.operation.distance

import kotlin.jvm.JvmStatic

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFilter
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.Polygon

/**
 * Extracts a single point
 * from each connected element in a Geometry
 * (e.g. a polygon, linestring or point)
 * and returns them in a list
 *
 */
class ConnectedElementPointFilter private constructor(
  private val pts: MutableList<Coordinate?>
) : GeometryFilter {

  override fun filter(geom: Geometry) {
    if (geom is Point
      || geom is LineString
      || geom is Polygon
    )
      pts.add(geom.getCoordinate())
  }

  companion object {
    /**
     * Returns a list containing a Coordinate from each Polygon, LineString, and Point
     * found inside the specified geometry. Thus, if the specified geometry is
     * not a GeometryCollection, an empty list will be returned.
     */
    @JvmStatic
    fun getCoordinates(geom: Geometry): MutableList<Coordinate?> {
      val pts = ArrayList<Coordinate?>()
      geom.apply(ConnectedElementPointFilter(pts))
      return pts
    }
  }
}
