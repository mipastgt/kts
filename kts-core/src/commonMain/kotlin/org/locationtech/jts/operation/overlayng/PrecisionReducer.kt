/*
 * Copyright (c) 2019 Martin Davis.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.locationtech.jts.operation.overlayng

import kotlin.jvm.JvmStatic

import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.PrecisionModel
import org.locationtech.jts.geom.TopologyException

/**
 * Functions to reduce the precision of a geometry
 * by rounding it to a given precision model.
 *
 * @see org.locationtech.jts.precision.GeometryPrecisionReducer
 *
 * @author Martin Davis
 */
class PrecisionReducer private constructor() {

  companion object {
    /**
     * Reduces the precision of a geometry by rounding and snapping it to the
     * supplied [PrecisionModel].
     * The input geometry must be polygonal or linear.
     *
     * @param geom the geometry to reduce
     * @param pm the precision model to use
     * @return the precision-reduced geometry
     *
     * @throws IllegalArgumentException if the reduction fails due to invalid input geometry is invalid
     */
    @JvmStatic
    fun reducePrecision(geom: Geometry, pm: PrecisionModel): Geometry {
      val ov = OverlayNG(geom, pm)
      /*
       * Ensure reducing a area only produces polygonal result.
       * (I.e. collapse lines are not output)
       */
      if (geom.getDimension() == 2) {
        ov.setAreaResultOnly(true)
      }
      try {
        val reduced = ov.getResult()
        return reduced
      } catch (ex: TopologyException) {
        throw IllegalArgumentException("Reduction failed, possible invalid input")
      }
    }
  }
}
