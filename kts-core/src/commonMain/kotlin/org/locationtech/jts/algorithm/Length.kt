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
package org.locationtech.jts.algorithm

import kotlin.jvm.JvmStatic
import kotlin.math.hypot

import org.locationtech.jts.geom.CoordinateSequence

/**
 * Functions for computing length.
 *
 * @author Martin Davis
 *
 */
class Length {
  companion object {
    /**
     * Computes the length of a linestring specified by a sequence of points.
     *
     * @param pts the points specifying the linestring
     * @return the length of the linestring
     */
    @JvmStatic
    fun ofLine(pts: CoordinateSequence): Double {
      // optimized for processing CoordinateSequences
      val n = pts.size()
      if (n <= 1)
        return 0.0

      var len = 0.0

      val p = pts.createCoordinate()
      pts.getCoordinate(0, p)
      var x0 = p.x
      var y0 = p.y

      for (i in 1 until n) {
        pts.getCoordinate(i, p)
        val x1 = p.x
        val y1 = p.y
        val dx = x1 - x0
        val dy = y1 - y0

        len += hypot(dx, dy)

        x0 = x1
        y0 = y1
      }
      return len
    }
  }
}
