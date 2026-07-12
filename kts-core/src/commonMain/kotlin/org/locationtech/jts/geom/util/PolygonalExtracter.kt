/*
 * Copyright (c) 2023 Martin Davis.
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
import org.locationtech.jts.geom.MultiPolygon
import org.locationtech.jts.geom.Polygon

/**
 * Extracts the [Polygon] and [MultiPolygon] elements from a [Geometry].
 */
class PolygonalExtracter {
  companion object {
    /**
     * Extracts the [Polygon] and [MultiPolygon] elements from a [Geometry]
     * and adds them to the provided list.
     *
     * @param geom the geometry from which to extract
     * @param list the list to add the extracted elements to
     */
    @JvmStatic
    fun getPolygonals(geom: Geometry, list: MutableList<Geometry>): MutableList<Geometry> {
      if (geom is Polygon || geom is MultiPolygon) {
        list.add(geom)
      } else if (geom is GeometryCollection) {
        for (i in 0 until geom.getNumGeometries()) {
          getPolygonals(geom.getGeometryN(i), list)
        }
      }
      // skip non-Polygonal elemental geometries
      return list
    }

    /**
     * Extracts the [Polygon] and [MultiPolygon] elements from a [Geometry]
     * and returns them in a list.
     *
     * @param geom the geometry from which to extract
     */
    @JvmStatic
    fun getPolygonals(geom: Geometry): MutableList<Geometry> {
      return getPolygonals(geom, ArrayList<Geometry>())
    }
  }
}
