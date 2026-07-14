/*
 * Copyright (c) 2016 Martin Davis.
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
import org.locationtech.jts.geom.GeometryFilter

/**
 * Computes an interior point of a `[Geometry]`.
 * An interior point is guaranteed to lie in the interior of the Geometry,
 * if it possible to calculate such a point exactly.
 * The interior point of an empty geometry is `null`.
 *
 * @see Centroid
 */
class InteriorPoint {

  companion object {
    /**
     * Computes a location of an interior point in a [Geometry].
     * Handles all geometry types.
     *
     * @param geom a geometry in which to find an interior point
     * @return the location of an interior point,
     *  or `null` if the input is empty
     */
    @JvmStatic
    fun getInteriorPoint(geom: Geometry): Coordinate? {
      if (geom.isEmpty())
        return null

      val interiorPt: Coordinate?
      val dim = dimensionNonEmpty(geom)
      // this should not happen, but just in case...
      if (dim < 0) {
        return null
      }
      if (dim == 0) {
        interiorPt = InteriorPointPoint.getInteriorPoint(geom)
      } else if (dim == 1) {
        interiorPt = InteriorPointLine.getInteriorPoint(geom)
      } else {
        interiorPt = InteriorPointArea.getInteriorPoint(geom)
      }
      return interiorPt
    }

    private fun dimensionNonEmpty(geom: Geometry): Int {
      val dimFilter = DimensionNonEmptyFilter()
      geom.apply(dimFilter)
      return dimFilter.getDimension()
    }
  }

  private class DimensionNonEmptyFilter : GeometryFilter {
    private var dim = -1

    fun getDimension(): Int {
      return dim
    }

    override fun filter(elem: Geometry) {
      if (elem is GeometryCollection)
        return
      if (!elem.isEmpty()) {
        val elemDim = elem.getDimension()
        if (elemDim > dim) dim = elemDim
      }
    }
  }
}
