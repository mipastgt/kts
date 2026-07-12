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
package org.locationtech.jts.operation.overlay.snap

import kotlin.jvm.JvmStatic

import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.TopologyException
import org.locationtech.jts.operation.overlay.OverlayOp

/**
 * Performs an overlay operation using snapping and enhanced precision
 * to improve the robustness of the result.
 * This class only uses snapping
 * if an error is detected when running the standard JTS overlay code.
 * Errors detected include thrown exceptions
 * (in particular, [TopologyException])
 * and invalid overlay computations.
 *
 * @author Martin Davis
 * @version 1.7
 */
class SnapIfNeededOverlayOp(g1: Geometry, g2: Geometry) {

  private val geom: Array<Geometry> = arrayOf(g1, g2)

  fun getResultGeometry(opCode: Int): Geometry {
    var result: Geometry? = null
    var isSuccess = false
    var savedException: RuntimeException? = null
    try {
      // try basic operation with input geometries
      result = OverlayOp.overlayOp(geom[0], geom[1], opCode)
      val isValid = true
      // not needed if noding validation is used
//      boolean isValid = OverlayResultValidator.isValid(geom[0], geom[1], OverlayOp.INTERSECTION, result);
      if (isValid)
        isSuccess = true
    } catch (ex: RuntimeException) {
      savedException = ex
      // ignore this exception, since the operation will be rerun
//    	System.out.println(ex.getMessage());
    }
    if (!isSuccess) {
      // this may still throw an exception
      // if so, throw the original exception since it has the input coordinates
      try {
        result = SnapOverlayOp.overlayOp(geom[0], geom[1], opCode)
      } catch (ex: RuntimeException) {
        throw savedException!!
      }
    }
    return result!!
  }

  companion object {
    @JvmStatic
    fun overlayOp(g0: Geometry, g1: Geometry, opCode: Int): Geometry {
      val op = SnapIfNeededOverlayOp(g0, g1)
      return op.getResultGeometry(opCode)
    }

    @JvmStatic
    fun intersection(g0: Geometry, g1: Geometry): Geometry {
      return overlayOp(g0, g1, OverlayOp.INTERSECTION)
    }

    @JvmStatic
    fun union(g0: Geometry, g1: Geometry): Geometry {
      return overlayOp(g0, g1, OverlayOp.UNION)
    }

    @JvmStatic
    fun difference(g0: Geometry, g1: Geometry): Geometry {
      return overlayOp(g0, g1, OverlayOp.DIFFERENCE)
    }

    @JvmStatic
    fun symDifference(g0: Geometry, g1: Geometry): Geometry {
      return overlayOp(g0, g1, OverlayOp.SYMDIFFERENCE)
    }
  }
}
