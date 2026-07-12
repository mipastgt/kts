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
import kotlin.math.max
import kotlin.math.min

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.Polygon
import org.locationtech.jts.geom.PrecisionModel
import org.locationtech.jts.util.Assert

/**
 * Utility methods for overlay processing.
 *
 * @author mdavis
 */
class OverlayUtil {

  companion object {

    /**
     * A null-handling wrapper for [PrecisionModel.isFloating]
     */
    @JvmStatic
    fun isFloating(pm: PrecisionModel?): Boolean {
      if (pm == null) return true
      return pm.isFloating()
    }

    /**
     * Computes a clipping envelope for overlay input geometries.
     *
     * @param opCode the overlay op code
     * @param inputGeom the input geometries
     * @param pm the precision model being used
     * @return an envelope for clipping and line limiting, or null if no clipping is performed
     */
    @JvmStatic
    fun clippingEnvelope(opCode: Int, inputGeom: InputGeometry, pm: PrecisionModel?): Envelope? {
      val resultEnv = resultEnvelope(opCode, inputGeom, pm)
      if (resultEnv == null)
        return null

      val clipEnv = RobustClipEnvelopeComputer.getEnvelope(
        inputGeom.getGeometry(0),
        inputGeom.getGeometry(1),
        resultEnv
      )

      val safeEnv = safeEnv(clipEnv, pm)
      return safeEnv
    }

    /**
     * Computes an envelope which covers the extent of the result of
     * a given overlay operation for given inputs.
     *
     * @return the result envelope, or null if the full extent
     */
    private fun resultEnvelope(opCode: Int, inputGeom: InputGeometry, pm: PrecisionModel?): Envelope? {
      var overlapEnv: Envelope? = null
      when (opCode) {
        OverlayNG.INTERSECTION -> {
          // use safe envelopes for intersection to ensure they contain rounded coordinates
          val envA = safeEnv(inputGeom.getEnvelope(0), pm)
          val envB = safeEnv(inputGeom.getEnvelope(1), pm)
          overlapEnv = envA.intersection(envB)
        }
        OverlayNG.DIFFERENCE -> {
          overlapEnv = safeEnv(inputGeom.getEnvelope(0), pm)
        }
      }
      // return null for UNION and SYMDIFFERENCE to indicate no clipping
      return overlapEnv
    }

    /**
     * Determines a safe geometry envelope for clipping,
     * taking into account the precision model being used.
     */
    private fun safeEnv(env: Envelope, pm: PrecisionModel?): Envelope {
      val envExpandDist = safeExpandDistance(env, pm)
      val safeEnv = env.copy()
      safeEnv.expandBy(envExpandDist)
      return safeEnv
    }

    private const val SAFE_ENV_BUFFER_FACTOR = 0.1

    private const val SAFE_ENV_GRID_FACTOR = 3

    private fun safeExpandDistance(env: Envelope, pm: PrecisionModel?): Double {
      val envExpandDist: Double
      if (isFloating(pm)) {
        // if PM is FLOAT then there is no scale factor, so add 10%
        var minSize = min(env.getHeight(), env.getWidth())
        // heuristic to ensure zero-width envelopes don't cause total clipping
        if (minSize <= 0.0) {
          minSize = max(env.getHeight(), env.getWidth())
        }
        envExpandDist = SAFE_ENV_BUFFER_FACTOR * minSize
      } else {
        // if PM is fixed, add a small multiple of the grid size
        val gridSize = 1.0 / pm!!.getScale()
        envExpandDist = SAFE_ENV_GRID_FACTOR * gridSize
      }
      return envExpandDist
    }

    /**
     * Tests if the result can be determined to be empty
     * based on simple properties of the input geometries.
     *
     * @param opCode the overlay operation
     * @return true if the overlay result is determined to be empty
     */
    @JvmStatic
    fun isEmptyResult(opCode: Int, a: Geometry?, b: Geometry?, pm: PrecisionModel?): Boolean {
      when (opCode) {
        OverlayNG.INTERSECTION -> {
          if (isEnvDisjoint(a, b, pm))
            return true
        }
        OverlayNG.DIFFERENCE -> {
          if (isEmpty(a))
            return true
        }
        OverlayNG.UNION, OverlayNG.SYMDIFFERENCE -> {
          if (isEmpty(a) && isEmpty(b))
            return true
        }
      }
      return false
    }

    private fun isEmpty(geom: Geometry?): Boolean {
      return geom == null || geom.isEmpty()
    }

    /**
     * Tests if the geometry envelopes are disjoint, or empty.
     *
     * @param a a geometry
     * @param b a geometry
     * @param pm the precision model being used
     * @return true if the geometry envelopes are disjoint or empty
     */
    @JvmStatic
    fun isEnvDisjoint(a: Geometry?, b: Geometry?, pm: PrecisionModel?): Boolean {
      if (isEmpty(a) || isEmpty(b)) return true
      if (isFloating(pm)) {
        return a!!.getEnvelopeInternal().disjoint(b!!.getEnvelopeInternal())
      }
      return isDisjoint(a!!.getEnvelopeInternal(), b!!.getEnvelopeInternal(), pm)
    }

    /**
     * Tests for disjoint envelopes adjusting for rounding
     * caused by a fixed precision model.
     */
    private fun isDisjoint(envA: Envelope, envB: Envelope, pm: PrecisionModel?): Boolean {
      if (pm!!.makePrecise(envB.getMinX()) > pm.makePrecise(envA.getMaxX())) return true
      if (pm.makePrecise(envB.getMaxX()) < pm.makePrecise(envA.getMinX())) return true
      if (pm.makePrecise(envB.getMinY()) > pm.makePrecise(envA.getMaxY())) return true
      if (pm.makePrecise(envB.getMaxY()) < pm.makePrecise(envA.getMinY())) return true
      return false
    }

    /**
     * Creates an empty result geometry of the appropriate dimension,
     * based on the given overlay operation and the dimensions of the inputs.
     *
     * @param dim the dimension of the empty geometry to create
     * @param geomFact the geometry factory being used for the operation
     * @return an empty atomic geometry of the appropriate dimension
     */
    @JvmStatic
    fun createEmptyResult(dim: Int, geomFact: GeometryFactory): Geometry {
      val result: Geometry = when (dim) {
        0 -> geomFact.createPoint()
        1 -> geomFact.createLineString()
        2 -> geomFact.createPolygon()
        -1 -> geomFact.createGeometryCollection()
        else -> {
          Assert.shouldNeverReachHere("Unable to determine overlay result geometry dimension")
          throw IllegalStateException()
        }
      }
      return result
    }

    /**
     * Computes the dimension of the result of
     * applying the given operation to inputs
     * with the given dimensions.
     *
     * @param opCode the overlay operation
     * @param dim0 dimension of the LH input
     * @param dim1 dimension of the RH input
     * @return the dimension of the result
     */
    @JvmStatic
    fun resultDimension(opCode: Int, dim0: Int, dim1: Int): Int {
      var resultDimension = -1
      when (opCode) {
        OverlayNG.INTERSECTION -> resultDimension = min(dim0, dim1)
        OverlayNG.UNION -> resultDimension = max(dim0, dim1)
        OverlayNG.DIFFERENCE -> resultDimension = dim0
        OverlayNG.SYMDIFFERENCE ->
          /**
           * This result is chosen because
           * SymDiff = Union( Diff(A, B), Diff(B, A) )
           * and Union has the dimension of the highest-dimension argument.
           */
          resultDimension = max(dim0, dim1)
      }
      return resultDimension
    }

    /**
     * Creates an overlay result geometry for homogeneous or mixed components.
     *
     * @param resultPolyList the list of result polygons (may be empty or null)
     * @param resultLineList the list of result lines (may be empty or null)
     * @param resultPointList the list of result points (may be empty or null)
     * @param geometryFactory the geometry factory to use
     * @return a geometry structured according to the overlay result semantics
     */
    @JvmStatic
    fun createResultGeometry(
      resultPolyList: List<Polygon>?,
      resultLineList: List<LineString>?,
      resultPointList: List<Point>?,
      geometryFactory: GeometryFactory
    ): Geometry {
      val geomList = ArrayList<Geometry>()

      // element geometries of the result are always in the order A,L,P
      if (resultPolyList != null) geomList.addAll(resultPolyList)
      if (resultLineList != null) geomList.addAll(resultLineList)
      if (resultPointList != null) geomList.addAll(resultPointList)

      // build the most specific geometry possible
      return geometryFactory.buildGeometry(geomList)
    }

    @JvmStatic
    fun toLines(graph: OverlayGraph, isOutputEdges: Boolean, geomFact: GeometryFactory): Geometry {
      val lines = ArrayList<LineString>()
      for (edge in graph.getEdges()) {
        val includeEdge = isOutputEdges || edge.isInResultArea()
        if (!includeEdge) continue
        val pts = edge.getCoordinatesOriented()
        val line = geomFact.createLineString(pts)
        line.setUserData(labelForResult(edge))
        lines.add(line)
      }
      return geomFact.buildGeometry(lines)
    }

    private fun labelForResult(edge: OverlayEdge): String {
      return edge.getLabel().toString(edge.isForward()) +
          (if (edge.isInResultArea()) " Res" else "")
    }

    /**
     * Round the key point if precision model is fixed.
     * Note: return value is only copied if rounding is performed.
     *
     * @param pt the Point to round
     * @return the rounded point coordinate, or null if empty
     */
    @JvmStatic
    fun round(pt: Point, pm: PrecisionModel?): Coordinate? {
      if (pt.isEmpty()) return null
      return round(pt.getCoordinate()!!, pm)
    }

    /**
     * Rounds a coordinate if precision model is fixed.
     * Note: return value is only copied if rounding is performed.
     *
     * @param p the coordinate to round
     * @return the rounded coordinate
     */
    @JvmStatic
    fun round(p: Coordinate, pm: PrecisionModel?): Coordinate {
      if (!isFloating(pm)) {
        val pRound = p.copy()
        pm!!.makePrecise(pRound)
        return pRound
      }
      return p
    }

    private const val AREA_HEURISTIC_TOLERANCE = 0.1

    /**
     * A heuristic check for overlay result correctness
     * comparing the areas of the input and result.
     *
     * @param geom0 input geometry 0
     * @param geom1 input geometry 1
     * @param opCode the overlay opcode
     * @param result the overlay result
     * @return true if the result area is consistent
     */
    @JvmStatic
    fun isResultAreaConsistent(geom0: Geometry?, geom1: Geometry?, opCode: Int, result: Geometry): Boolean {
      if (geom0 == null || geom1 == null)
        return true

      if (result.getDimension() < 2) return true

      val areaResult = result.getArea()
      val areaA = geom0.getArea()
      val areaB = geom1.getArea()

      var isConsistent = true
      when (opCode) {
        OverlayNG.INTERSECTION ->
          isConsistent = isLess(areaResult, areaA, AREA_HEURISTIC_TOLERANCE) &&
              isLess(areaResult, areaB, AREA_HEURISTIC_TOLERANCE)
        OverlayNG.DIFFERENCE ->
          isConsistent = isDifferenceAreaConsistent(areaA, areaB, areaResult, AREA_HEURISTIC_TOLERANCE)
        OverlayNG.SYMDIFFERENCE ->
          isConsistent = isLess(areaResult, areaA + areaB, AREA_HEURISTIC_TOLERANCE)
        OverlayNG.UNION ->
          isConsistent = isLess(areaA, areaResult, AREA_HEURISTIC_TOLERANCE) &&
              isLess(areaB, areaResult, AREA_HEURISTIC_TOLERANCE) &&
              isGreater(areaResult, areaA - areaB, AREA_HEURISTIC_TOLERANCE)
      }
      return isConsistent
    }

    /**
     * Tests if the area of a difference is greater than the minimum possible difference area.
     *
     * @return true if the difference area is consistent.
     */
    private fun isDifferenceAreaConsistent(areaA: Double, areaB: Double, areaResult: Double, tolFrac: Double): Boolean {
      if (!isLess(areaResult, areaA, tolFrac))
        return false
      val areaDiffMin = areaA - areaB - tolFrac * areaA
      return areaResult > areaDiffMin
    }

    private fun isLess(v1: Double, v2: Double, tol: Double): Boolean {
      return v1 <= v2 * (1 + tol)
    }

    private fun isGreater(v1: Double, v2: Double, tol: Double): Boolean {
      return v1 >= v2 * (1 - tol)
    }
  }
}
