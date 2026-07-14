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
package org.locationtech.jts.precision

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.CoordinateFilter
import org.locationtech.jts.geom.CoordinateSequence
import org.locationtech.jts.geom.CoordinateSequenceFilter
import org.locationtech.jts.geom.Geometry

/**
 * Removes common most-significant mantissa bits
 * from one or more [Geometry]s.
 *
 */
class CommonBitsRemover {

  private var commonCoord: Coordinate? = null
  private val ccFilter = CommonCoordinateFilter()

  /**
   * Add a geometry to the set of geometries whose common bits are
   * being computed.  After this method has executed the
   * common coordinate reflects the common bits of all added
   * geometries.
   *
   * @param geom a Geometry to test for common bits
   */
  fun add(geom: Geometry) {
    geom.apply(ccFilter)
    commonCoord = ccFilter.getCommonCoordinate()
  }

  /**
   * The common bits of the Coordinates in the supplied Geometries.
   */
  fun getCommonCoordinate(): Coordinate? = commonCoord

  /**
   * Removes the common coordinate bits from a Geometry.
   * The coordinates of the Geometry are changed.
   *
   * @param geom the Geometry from which to remove the common coordinate bits
   * @return the shifted Geometry
   */
  fun removeCommonBits(geom: Geometry): Geometry {
    if (commonCoord!!.x == 0.0 && commonCoord!!.y == 0.0)
      return geom
    val invCoord = Coordinate(commonCoord!!)
    invCoord.x = -invCoord.x
    invCoord.y = -invCoord.y
    val trans = Translater(invCoord)
    geom.apply(trans)
    geom.geometryChanged()
    return geom
  }

  /**
   * Adds the common coordinate bits back into a Geometry.
   * The coordinates of the Geometry are changed.
   *
   * @param geom the Geometry to which to add the common coordinate bits
   */
  fun addCommonBits(geom: Geometry) {
    val trans = Translater(commonCoord)
    geom.apply(trans)
    geom.geometryChanged()
  }

  private class CommonCoordinateFilter : CoordinateFilter {
    private val commonBitsX = CommonBits()
    private val commonBitsY = CommonBits()

    override fun filter(coord: Coordinate) {
      commonBitsX.add(coord.x)
      commonBitsY.add(coord.y)
    }

    fun getCommonCoordinate(): Coordinate {
      return Coordinate(
          commonBitsX.getCommon(),
          commonBitsY.getCommon())
    }
  }

  private class Translater(var trans: Coordinate?) : CoordinateSequenceFilter {

    override fun filter(seq: CoordinateSequence, i: Int) {
      val xp = seq.getOrdinate(i, 0) + trans!!.x
      val yp = seq.getOrdinate(i, 1) + trans!!.y
      seq.setOrdinate(i, 0, xp)
      seq.setOrdinate(i, 1, yp)
    }

    override fun isDone(): Boolean {
      return false
    }

    override fun isGeometryChanged(): Boolean {
      return true
    }
  }
}
