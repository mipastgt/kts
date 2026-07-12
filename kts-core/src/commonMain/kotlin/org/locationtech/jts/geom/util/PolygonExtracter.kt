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
import org.locationtech.jts.geom.Polygon

/**
 * Extracts all the [Polygon] elements from a [Geometry].
 *
 * @version 1.7
 * @see GeometryExtracter
 */
class PolygonExtracter(private val comps: MutableList<in Polygon>) : GeometryFilter {

  override fun filter(geom: Geometry) {
    if (geom is Polygon) comps.add(geom)
  }

  companion object {
    /**
     * Extracts the [Polygon] elements from a single [Geometry]
     * and adds them to the provided [List].
     *
     * @param geom the geometry from which to extract
     * @param list the list to add the extracted elements to
     */
    @JvmStatic
    fun getPolygons(geom: Geometry?, list: MutableList<in Polygon>): MutableList<in Polygon> {
      if (geom is Polygon) {
        list.add(geom)
      } else if (geom is GeometryCollection) {
        geom.apply(PolygonExtracter(list))
      }
      // skip non-Polygonal elemental geometries

      return list
    }

    /**
     * Extracts the [Polygon] elements from a single [Geometry]
     * and returns them in a [List].
     *
     * @param geom the geometry from which to extract
     */
    @JvmStatic
    fun getPolygons(geom: Geometry?): List<Polygon> {
      val list = ArrayList<Polygon>()
      getPolygons(geom, list)
      return list
    }
  }
}
