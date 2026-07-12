/*
 * Copyright (c) 2022 Martin Davis.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.locationtech.jts.coverage
import kotlin.math.PI

import org.locationtech.jts.algorithm.Angle
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Triangle
import org.locationtech.jts.math.MathUtil

/**
 * Computes the effective area of corners,
 * taking into account the smoothing weight.
 *
 * @author Martin Davis
 */
class CornerArea {

  private var smoothWeight = DEFAULT_SMOOTH_WEIGHT

  constructor()

  /**
   * Creates a new corner area computer.
   *
   * @param smoothWeight the weight for smoothing corners.  In range [0..1].
   */
  constructor(smoothWeight: Double) {
    this.smoothWeight = smoothWeight
  }

  fun area(pp: Coordinate, p: Coordinate, pn: Coordinate): Double {
    val area = Triangle.area(pp, p, pn)
    val ang = angleNorm(pp, p, pn)
    //-- rescale to [-1 .. 1], with 1 being narrow and -1 being flat
    val angBias = 1.0 - 2.0 * ang
    //-- reduce area for narrower corners, to make them more likely to be removed
    val areaWeighted = (1 - smoothWeight * angBias) * area
    return areaWeighted
  }

  companion object {
    const val DEFAULT_SMOOTH_WEIGHT = 0.0

    private fun angleNorm(pp: Coordinate, p: Coordinate, pn: Coordinate): Double {
      val angNorm = Angle.angleBetween(pp, p, pn) / 2 / PI
      return MathUtil.clamp(angNorm, 0.0, 1.0)
    }
  }
}
