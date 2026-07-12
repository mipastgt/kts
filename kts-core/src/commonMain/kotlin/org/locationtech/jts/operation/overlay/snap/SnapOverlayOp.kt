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
import org.locationtech.jts.operation.overlay.OverlayOp
import org.locationtech.jts.precision.CommonBitsRemover

/**
 * Performs an overlay operation using snapping and enhanced precision
 * to improve the robustness of the result.
 * This class *always* uses snapping.
 * This is less performant than the standard JTS overlay code,
 * and may even introduce errors which were not present in the original data.
 * For this reason, this class should only be used
 * if the standard overlay code fails to produce a correct result.
 *
 * @author Martin Davis
 * @version 1.7
 */
class SnapOverlayOp(g1: Geometry, g2: Geometry) {

  private val geom: Array<Geometry> = arrayOf(g1, g2)
  private var snapTolerance = 0.0
  private var cbr: CommonBitsRemover? = null

  init {
    computeSnapTolerance()
  }

  private fun computeSnapTolerance() {
    snapTolerance = GeometrySnapper.computeOverlaySnapTolerance(geom[0], geom[1])

    // System.out.println("Snap tol = " + snapTolerance);
  }

  fun getResultGeometry(opCode: Int): Geometry {
//  	Geometry[] selfSnapGeom = new Geometry[] { selfSnap(geom[0]), selfSnap(geom[1])};
    val prepGeom = snap(geom)
    val result = OverlayOp.overlayOp(prepGeom[0], prepGeom[1], opCode)
    return prepareResult(result)
  }

  private fun selfSnap(geom: Geometry): Geometry {
    val snapper0 = GeometrySnapper(geom)
    val snapGeom = snapper0.snapTo(geom, snapTolerance)
    //System.out.println("Self-snapped: " + snapGeom);
    //System.out.println();
    return snapGeom
  }

  private fun snap(geom: Array<Geometry>): Array<Geometry> {
    val remGeom = removeCommonBits(geom)

    // MD - testing only
//  	Geometry[] remGeom = geom;

    val snapGeom = GeometrySnapper.snap(remGeom[0], remGeom[1], snapTolerance)
    // MD - may want to do this at some point, but it adds cycles
//    checkValid(snapGeom[0]);
//    checkValid(snapGeom[1]);

    return snapGeom
  }

  private fun prepareResult(geom: Geometry): Geometry {
    cbr!!.addCommonBits(geom)
    return geom
  }

  private fun removeCommonBits(geom: Array<Geometry>): Array<Geometry> {
    cbr = CommonBitsRemover()
    cbr!!.add(geom[0])
    cbr!!.add(geom[1])
    val remGeom = arrayOf(
      cbr!!.removeCommonBits(geom[0].copy()),
      cbr!!.removeCommonBits(geom[1].copy())
    )
    return remGeom
  }

  companion object {
    @JvmStatic
    fun overlayOp(g0: Geometry, g1: Geometry, opCode: Int): Geometry {
      val op = SnapOverlayOp(g0, g1)
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
