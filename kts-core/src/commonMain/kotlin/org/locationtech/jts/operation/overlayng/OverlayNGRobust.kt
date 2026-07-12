/*
 * Copyright (c) 2020 Martin Davis.
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
import kotlin.math.abs
import kotlin.math.max

import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel
import org.locationtech.jts.geom.TopologyException
import org.locationtech.jts.noding.snap.SnappingNoder
import org.locationtech.jts.operation.union.UnaryUnionOp
import org.locationtech.jts.operation.union.UnionStrategy

/**
 * Performs an overlay operation using [OverlayNG],
 * providing full robustness by using a series of
 * increasingly robust (but slower) noding strategies.
 *
 * @author Martin Davis
 *
 * @see OverlayNG
 */
class OverlayNGRobust {

  companion object {
    /**
     * Computes the unary union of a geometry using robust computation.
     *
     * @param geom the geometry to union
     * @return the union result
     *
     * @see UnaryUnionOp
     */
    @JvmStatic
    fun union(geom: Geometry): Geometry {
      val op = UnaryUnionOp(geom)
      op.setUnionFunction(OVERLAY_UNION)
      return op.union()!!
    }

    /**
     * Computes the unary union of a collection of geometries using robust computation.
     *
     * @param geoms the collection of geometries to union
     * @return the union result
     *
     * @see UnaryUnionOp
     */
    @JvmStatic
    fun union(geoms: Collection<Geometry>): Geometry {
      val op = UnaryUnionOp(geoms)
      op.setUnionFunction(OVERLAY_UNION)
      return op.union()!!
    }

    /**
     * Computes the unary union of a collection of geometries using robust computation.
     *
     * @param geoms the collection of geometries to union
     * @param geomFact the geometry factory to use
     * @return the union of the geometries
     */
    @JvmStatic
    fun union(geoms: Collection<Geometry>, geomFact: GeometryFactory): Geometry {
      val op = UnaryUnionOp(geoms, geomFact)
      op.setUnionFunction(OVERLAY_UNION)
      return op.union()!!
    }

    private val OVERLAY_UNION: UnionStrategy = object : UnionStrategy {
      override fun union(g0: Geometry, g1: Geometry): Geometry {
        return overlay(g0, g1, OverlayNG.UNION)
      }

      override fun isFloatingPrecision(): Boolean {
        return true
      }
    }

    /**
     * Overlay two geometries, using heuristics to ensure
     * computation completes correctly.
     *
     * @param geom0 a geometry
     * @param geom1 a geometry
     * @param opCode the overlay operation code (from [OverlayNG])
     * @return the overlay result geometry
     *
     * @see OverlayNG
     */
    @JvmStatic
    fun overlay(geom0: Geometry, geom1: Geometry, opCode: Int): Geometry {
      var result: Geometry?
      val exOriginal: RuntimeException

      /**
       * First try overlay with a FLOAT noder, which is fast and causes least
       * change to geometry coordinates
       */
      try {
        result = OverlayNG.overlay(geom0, geom1, opCode)
        return result
      } catch (ex: RuntimeException) {
        /**
         * Capture original exception,
         * so it can be rethrown if the remaining strategies all fail.
         */
        exOriginal = ex
      }

      /**
       * On failure retry using snapping noding with a "safe" tolerance.
       */
      result = overlaySnapTries(geom0, geom1, opCode)
      if (result != null)
        return result

      /**
       * On failure retry using snap-rounding with a heuristic scale factor (grid size).
       */
      result = overlaySR(geom0, geom1, opCode)
      if (result != null)
        return result

      /**
       * Just can't get overlay to work, so throw original error.
       */
      throw exOriginal
    }

    private const val NUM_SNAP_TRIES = 5

    /**
     * Attempt overlay using snapping with repeated tries with increasing snap tolerances.
     *
     * @return the computed overlay result, or null if the overlay fails
     */
    private fun overlaySnapTries(geom0: Geometry, geom1: Geometry, opCode: Int): Geometry? {
      var result: Geometry?
      var snapTol = snapTolerance(geom0, geom1)

      for (i in 0 until NUM_SNAP_TRIES) {

        result = overlaySnapping(geom0, geom1, opCode, snapTol)
        if (result != null) return result

        /**
         * Now try snapping each input individually,
         * and then doing the overlay.
         */
        result = overlaySnapBoth(geom0, geom1, opCode, snapTol)
        if (result != null) return result

        // increase the snap tolerance and try again
        snapTol = snapTol * 10
      }
      // failed to compute overlay
      return null
    }

    /**
     * Attempt overlay using a [SnappingNoder].
     *
     * @return the computed overlay result, or null if the overlay fails
     */
    private fun overlaySnapping(geom0: Geometry, geom1: Geometry, opCode: Int, snapTol: Double): Geometry? {
      try {
        return overlaySnapTol(geom0, geom1, opCode, snapTol)
      } catch (ex: TopologyException) {
        //---- ignore exception, return null result to indicate failure
      }
      return null
    }

    /**
     * Attempt overlay with first snapping each geometry individually.
     *
     * @return the computed overlay result, or null if the overlay fails
     */
    private fun overlaySnapBoth(geom0: Geometry, geom1: Geometry, opCode: Int, snapTol: Double): Geometry? {
      try {
        val snap0 = snapSelf(geom0, snapTol)
        val snap1 = snapSelf(geom1, snapTol)

        return overlaySnapTol(snap0, snap1, opCode, snapTol)
      } catch (ex: TopologyException) {
        //---- ignore exception, return null result to indicate failure
      }
      return null
    }

    /**
     * Self-snaps a geometry by running a union operation with it as the only input.
     *
     * @param geom geometry to self-snap
     * @param snapTol snap tolerance
     * @return the snapped geometry (homogeneous)
     */
    private fun snapSelf(geom: Geometry, snapTol: Double): Geometry {
      val ov = OverlayNG(geom, null)
      val snapNoder = SnappingNoder(snapTol)
      ov.setNoder(snapNoder)
      /**
       * Ensure the result is not mixed-dimension,
       * since it will be used in further overlay computation.
       */
      ov.setStrictMode(true)
      return ov.getResult()
    }

    private fun overlaySnapTol(geom0: Geometry, geom1: Geometry, opCode: Int, snapTol: Double): Geometry {
      val snapNoder = SnappingNoder(snapTol)
      return OverlayNG.overlay(geom0, geom1, opCode, snapNoder)
    }

    //============================================

    /**
     * A factor for a snapping tolerance distance which
     * should allow noding to be computed robustly.
     */
    private const val SNAP_TOL_FACTOR = 1e12

    /**
     * Computes a heuristic snap tolerance distance
     * for overlaying a pair of geometries using a [SnappingNoder].
     *
     * @return the snap tolerance
     */
    private fun snapTolerance(geom0: Geometry, geom1: Geometry): Double {
      val tol0 = snapTolerance(geom0)
      val tol1 = snapTolerance(geom1)
      val snapTol = max(tol0, tol1)
      return snapTol
    }

    private fun snapTolerance(geom: Geometry): Double {
      val magnitude = ordinateMagnitude(geom)
      return magnitude / SNAP_TOL_FACTOR
    }

    /**
     * Computes the largest magnitude of the ordinates of a geometry,
     * based on the geometry envelope.
     *
     * @param geom a geometry
     * @return the magnitude of the largest ordinate
     */
    private fun ordinateMagnitude(geom: Geometry?): Double {
      if (geom == null || geom.isEmpty()) return 0.0
      val env = geom.getEnvelopeInternal()
      val magMax = max(
        abs(env.getMaxX()), abs(env.getMaxY())
      )
      val magMin = max(
        abs(env.getMinX()), abs(env.getMinY())
      )
      return max(magMax, magMin)
    }

    /**
     * Attempt Overlay using Snap-Rounding with an automatically-determined
     * scale factor.
     *
     * @return the computed overlay result, or null if the overlay fails
     */
    private fun overlaySR(geom0: Geometry, geom1: Geometry, opCode: Int): Geometry? {
      val result: Geometry
      try {
        val scaleSafe = PrecisionUtil.safeScale(geom0, geom1)
        val pmSafe = PrecisionModel(scaleSafe)
        result = OverlayNG.overlay(geom0, geom1, opCode, pmSafe)
        return result
      } catch (ex: TopologyException) {
        //---- ignore exception, return null result to indicate failure
      }
      return null
    }
  }
}
