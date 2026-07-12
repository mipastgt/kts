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

import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryCollection
import org.locationtech.jts.geom.GeometryFilter
import org.locationtech.jts.geom.Point

/**
 * Extracts all the 0-dimensional ([Point]) components from a [Geometry].
 *
 * @version 1.7
 * @see GeometryExtracter
 */
class PointExtracter(private val pts: MutableList<in Point>) : GeometryFilter {

  override fun filter(geom: Geometry) {
    if (geom is Point) pts.add(geom)
  }

  companion object {
    /**
     * Extracts the [Point] elements from a single [Geometry]
     * and adds them to the provided [List].
     *
     * @param geom the geometry from which to extract
     * @param list the list to add the extracted elements to
     */
    @JvmStatic
    fun getPoints(geom: Geometry, list: MutableList<in Point>): MutableList<in Point> {
      if (geom is Point) {
        list.add(geom)
      } else if (geom is GeometryCollection) {
        geom.apply(PointExtracter(list))
      }
      // skip non-Polygonal elemental geometries

      return list
    }

    /**
     * Extracts the [Point] elements from a single [Geometry]
     * and returns them in a [List].
     *
     * @param geom the geometry from which to extract
     */
    @JvmStatic
    fun getPoints(geom: Geometry): List<Point> {
      if (geom is Point) {
        return listOf(geom)
      }
      val list = ArrayList<Point>()
      getPoints(geom, list)
      return list
    }
  }
}
