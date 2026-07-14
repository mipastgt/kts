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
package org.locationtech.jts.geom.util

import kotlin.jvm.JvmStatic

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryComponentFilter
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.Point

/**
 * Extracts a representative [Coordinate]
 * from each connected component of a [Geometry].
 *
 */
open class ComponentCoordinateExtracter(private val coords: MutableList<in Coordinate>) : GeometryComponentFilter {

  override fun filter(geom: Geometry) {
    if (geom.isEmpty()) return
    // add coordinates from connected components
    if (geom is LineString || geom is Point) coords.add(geom.getCoordinate()!!)
  }

  companion object {
    /**
     * Extracts a representative [Coordinate]
     * from each connected component in a geometry.
     *
     * @param geom the Geometry from which to extract
     * @return a list of representative Coordinates
     */
    @JvmStatic
    fun getCoordinates(geom: Geometry): List<Coordinate> {
      val coords = ArrayList<Coordinate>()
      geom.apply(ComponentCoordinateExtracter(coords))
      return coords
    }
  }
}
