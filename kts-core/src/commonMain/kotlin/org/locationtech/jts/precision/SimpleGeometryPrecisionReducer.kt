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

import kotlin.jvm.JvmStatic

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.CoordinateList
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.LinearRing
import org.locationtech.jts.geom.PrecisionModel
import org.locationtech.jts.geom.util.GeometryEditor

/**
 * Reduces the precision of the coordinates of a [Geometry]
 * according to the supplied [PrecisionModel], without
 * attempting to preserve valid topology.
 *
 *
 * @deprecated use GeometryPrecisionReducer
 */
class SimpleGeometryPrecisionReducer(private val newPrecisionModel: PrecisionModel) {

  private var removeCollapsed = true
  private var changePrecisionModel = false

  /**
   * Sets whether the reduction will result in collapsed components
   * being removed completely, or simply being collapsed to an (invalid)
   * Geometry of the same type.
   *
   * @param removeCollapsed if `true` collapsed components will be removed
   */
  fun setRemoveCollapsedComponents(removeCollapsed: Boolean) {
    this.removeCollapsed = removeCollapsed
  }

  /**
   * Sets whether the [PrecisionModel] of the new reduced Geometry
   * will be changed to be the [PrecisionModel] supplied to
   * specify the precision reduction.
   *
   * @param changePrecisionModel if `true` the precision model of the created Geometry will be
   * the precisionModel supplied in the constructor.
   */
  fun setChangePrecisionModel(changePrecisionModel: Boolean) {
    this.changePrecisionModel = changePrecisionModel
  }

  fun reduce(geom: Geometry): Geometry {
    val geomEdit: GeometryEditor
    if (changePrecisionModel) {
      val newFactory = GeometryFactory(newPrecisionModel, geom.getFactory().getSRID())
      geomEdit = GeometryEditor(newFactory)
    } else {
      // don't change geometry factory
      geomEdit = GeometryEditor()
    }

    return geomEdit.edit(geom, PrecisionReducerCoordinateOperation())!!
  }

  private inner class PrecisionReducerCoordinateOperation : GeometryEditor.CoordinateOperation() {
    override fun edit(coordinates: Array<Coordinate>, geom: Geometry): Array<Coordinate>? {
      if (coordinates.isEmpty()) return null

      val reducedCoords = arrayOfNulls<Coordinate>(coordinates.size)
      // copy coordinates and reduce
      for (i in coordinates.indices) {
        val coord = Coordinate(coordinates[i])
        newPrecisionModel.makePrecise(coord)
        reducedCoords[i] = coord
      }
      @Suppress("UNCHECKED_CAST")
      val reducedCoordsNonNull = reducedCoords as Array<Coordinate>
      // remove repeated points, to simplify returned geometry as much as possible
      val noRepeatedCoordList = CoordinateList(reducedCoordsNonNull, false)
      val noRepeatedCoords = noRepeatedCoordList.toCoordinateArray()

      /**
       * Check to see if the removal of repeated points
       * collapsed the coordinate List to an invalid length
       * for the type of the parent geometry.
       */
      var minLength = 0
      if (geom is LineString) minLength = 2
      if (geom is LinearRing) minLength = 4

      var collapsedCoords: Array<Coordinate>? = reducedCoordsNonNull
      if (removeCollapsed) collapsedCoords = null

      // return null or orignal length coordinate array
      if (noRepeatedCoords.size < minLength) {
        return collapsedCoords
      }

      // ok to return shorter coordinate array
      return noRepeatedCoords
    }
  }

  companion object {
    /**
     * Convenience method for doing precision reduction on a single geometry,
     * with collapses removed and keeping the geometry precision model the same.
     *
     * @return the reduced geometry
     */
    @JvmStatic
    fun reduce(g: Geometry, precModel: PrecisionModel): Geometry {
      val reducer = SimpleGeometryPrecisionReducer(precModel)
      return reducer.reduce(g)
    }
  }
}
