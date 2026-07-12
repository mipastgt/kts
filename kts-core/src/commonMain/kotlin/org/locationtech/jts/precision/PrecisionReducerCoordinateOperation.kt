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
package org.locationtech.jts.precision

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.CoordinateList
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.LinearRing
import org.locationtech.jts.geom.PrecisionModel
import org.locationtech.jts.geom.util.GeometryEditor

class PrecisionReducerCoordinateOperation(
    private val targetPM: PrecisionModel,
    private val removeCollapsed: Boolean
) : GeometryEditor.CoordinateOperation() {

  override fun edit(coordinates: Array<Coordinate>, geom: Geometry): Array<Coordinate>? {
    if (coordinates.isEmpty())
      return null

    val reducedCoords = arrayOfNulls<Coordinate>(coordinates.size)
    // copy coordinates and reduce
    for (i in coordinates.indices) {
      val coord = Coordinate(coordinates[i])
      targetPM.makePrecise(coord)
      reducedCoords[i] = coord
    }
    @Suppress("UNCHECKED_CAST")
    val reducedCoordsNonNull = reducedCoords as Array<Coordinate>
    // remove repeated points, to simplify returned geometry as much as possible
    val noRepeatedCoordList = CoordinateList(reducedCoordsNonNull, false)
    val noRepeatedCoords = noRepeatedCoordList.toCoordinateArray()

    /**
     * Check to see if the removal of repeated points collapsed the coordinate
     * List to an invalid length for the type of the parent geometry.
     */
    var minLength = 0
    if (geom is LineString)
      minLength = 2
    if (geom is LinearRing)
      minLength = 4

    var collapsedCoords: Array<Coordinate>? = reducedCoordsNonNull
    if (removeCollapsed)
      collapsedCoords = null

    // return null or original length coordinate array
    if (noRepeatedCoords.size < minLength) {
      return collapsedCoords
    }

    // ok to return shorter coordinate array
    return noRepeatedCoords
  }
}
