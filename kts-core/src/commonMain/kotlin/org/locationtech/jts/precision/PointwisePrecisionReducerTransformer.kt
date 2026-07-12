/*
 * Copyright (c) 2021 Martin Davis.
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
import org.locationtech.jts.geom.CoordinateSequence
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.PrecisionModel
import org.locationtech.jts.geom.util.GeometryTransformer

/**
 * A transformer to reduce the precision of a geometry pointwise.
 *
 * @author mdavis
 */
internal class PointwisePrecisionReducerTransformer(
    private val targetPM: PrecisionModel
) : GeometryTransformer() {

  override fun transformCoordinates(
      coordinates: CoordinateSequence, parent: Geometry?): CoordinateSequence? {
    if (coordinates.size() == 0)
      return null

    val coordsReduce = reducePointwise(coordinates)
    return factory!!.getCoordinateSequenceFactory().create(coordsReduce)
  }

  private fun reducePointwise(coordinates: CoordinateSequence): Array<Coordinate> {
    val coordReduce = arrayOfNulls<Coordinate>(coordinates.size())
    // copy coordinates and reduce
    for (i in 0 until coordinates.size()) {
      val coord = coordinates.getCoordinate(i).copy()
      targetPM.makePrecise(coord)
      coordReduce[i] = coord
    }
    @Suppress("UNCHECKED_CAST")
    return coordReduce as Array<Coordinate>
  }

  companion object {
    fun reduce(geom: Geometry, targetPM: PrecisionModel): Geometry {
      val trans = PointwisePrecisionReducerTransformer(targetPM)
      return trans.transform(geom)!!
    }
  }
}
