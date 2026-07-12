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
import org.locationtech.jts.geom.LineString

/**
 * Extracts all the [LineString] elements from a [Geometry].
 *
 * @version 1.7
 * @see GeometryExtracter
 */
class LineStringExtracter(private val comps: MutableList<in LineString>) : GeometryFilter {

  override fun filter(geom: Geometry) {
    if (geom is LineString) comps.add(geom)
  }

  companion object {
    /**
     * Extracts the [LineString] elements from a single [Geometry]
     * and adds them to the provided [List].
     *
     * @param geom the geometry from which to extract
     * @param lines the list to add the extracted LineStrings to
     * @return the list argument
     */
    @JvmStatic
    fun getLines(geom: Geometry, lines: MutableList<in LineString>): MutableList<in LineString> {
      if (geom is LineString) {
        lines.add(geom)
      } else if (geom is GeometryCollection) {
        geom.apply(LineStringExtracter(lines))
      }
      // skip non-LineString elemental geometries

      return lines
    }

    /**
     * Extracts the [LineString] elements from a single [Geometry]
     * and returns them in a [List].
     *
     * @param geom the geometry from which to extract
     * @return a list containing the linear elements
     */
    @JvmStatic
    fun getLines(geom: Geometry): List<LineString> {
      val lines = ArrayList<LineString>()
      getLines(geom, lines)
      return lines
    }

    /**
     * Extracts the [LineString] elements from a single [Geometry]
     * and returns them as either a [LineString] or [org.locationtech.jts.geom.MultiLineString].
     *
     * @param geom the geometry from which to extract
     * @return a linear geometry
     */
    @JvmStatic
    fun getGeometry(geom: Geometry): Geometry {
      return geom.getFactory().buildGeometry(getLines(geom))
    }
  }
}
