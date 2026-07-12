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
package org.locationtech.jts.operation.buffer.validate
import kotlin.math.abs

import org.locationtech.jts.algorithm.distance.DiscreteHausdorffDistance
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryCollection
import org.locationtech.jts.geom.MultiPolygon
import org.locationtech.jts.geom.Polygon
import org.locationtech.jts.geom.util.LinearComponentExtracter
import org.locationtech.jts.geom.util.PolygonExtracter
import org.locationtech.jts.io.WKTWriter
import org.locationtech.jts.operation.distance.DistanceOp

/**
 * Validates that a given buffer curve lies an appropriate distance
 * from the input generating it.
 * Useful only for round buffers (cap and join).
 * Can be used for either positive or negative distances.
 *
 * @author mbdavis
 *
 */
class BufferDistanceValidator(
  private val input: Geometry,
  private val bufDistance: Double,
  private val result: Geometry
) {

  private var minValidDistance = 0.0
  private var maxValidDistance = 0.0

  private var minDistanceFound = 0.0
  private var maxDistanceFound = 0.0

  private var valid = true
  private var errMsg: String? = null
  private var errorLocation: Coordinate? = null
  private var errorIndicator: Geometry? = null

  fun isValid(): Boolean {
    val posDistance = abs(bufDistance)
    val distDelta = MAX_DISTANCE_DIFF_FRAC * posDistance
    minValidDistance = posDistance - distDelta
    maxValidDistance = posDistance + distDelta

    // can't use this test if either is empty
    if (input.isEmpty() || result.isEmpty()) return true

    if (bufDistance > 0.0) {
      checkPositiveValid()
    } else {
      checkNegativeValid()
    }
    return valid
  }

  fun getErrorMessage(): String? {
    return errMsg
  }

  fun getErrorLocation(): Coordinate? {
    return errorLocation
  }

  /**
   * Gets a geometry which indicates the location and nature of a validation failure.
   *
   * @return a geometric error indicator
   * or null if no error was found
   */
  fun getErrorIndicator(): Geometry? {
    return errorIndicator
  }

  private fun checkPositiveValid() {
    val bufCurve = result.getBoundary()
    checkMinimumDistance(input, bufCurve, minValidDistance)
    if (!valid) return

    checkMaximumDistance(input, bufCurve, maxValidDistance)
  }

  private fun checkNegativeValid() {
    // Assert: only polygonal inputs can be checked for negative buffers
    if (!(input is Polygon || input is MultiPolygon || input is GeometryCollection)) {
      return
    }
    val inputCurve = getPolygonLines(input)
    checkMinimumDistance(inputCurve, result, minValidDistance)
    if (!valid) return

    checkMaximumDistance(inputCurve, result, maxValidDistance)
  }

  private fun getPolygonLines(g: Geometry): Geometry {
    val lines = ArrayList<Any?>()
    val lineExtracter = LinearComponentExtracter(lines)
    val polys = PolygonExtracter.getPolygons(g)
    val i = polys.iterator()
    while (i.hasNext()) {
      val poly = i.next() as Polygon
      poly.apply(lineExtracter)
    }
    return g.getFactory().buildGeometry(lines)
  }

  /**
   * Checks that two geometries are at least a minimum distance apart.
   */
  private fun checkMinimumDistance(g1: Geometry, g2: Geometry, minDist: Double) {
    val distOp = DistanceOp(g1, g2, minDist)
    minDistanceFound = distOp.distance()

    if (minDistanceFound < minDist) {
      valid = false
      val pts = distOp.nearestPoints()
      errorLocation = distOp.nearestPoints()[1]
      errorIndicator = g1.getFactory().createLineString(pts)
      errMsg = "Distance between buffer curve and input is too small " +
        "(" + minDistanceFound +
        " at " + WKTWriter.toLineString(pts[0], pts[1]) + " )"
    }
  }

  /**
   * Checks that the furthest distance from the buffer curve to the input
   * is less than the given maximum distance.
   */
  private fun checkMaximumDistance(input: Geometry, bufCurve: Geometry, maxDist: Double) {
    val haus = DiscreteHausdorffDistance(bufCurve, input)
    haus.setDensifyFraction(0.25)
    maxDistanceFound = haus.orientedDistance()

    if (maxDistanceFound > maxDist) {
      valid = false
      val pts = haus.getCoordinates()
      errorLocation = pts[1]
      errorIndicator = input.getFactory().createLineString(pts)
      errMsg = "Distance between buffer curve and input is too large " +
        "(" + maxDistanceFound +
        " at " + WKTWriter.toLineString(pts[0], pts[1]) + ")"
    }
  }

  companion object {
    private const val VERBOSE = false

    /**
     * Maximum allowable fraction of buffer distance the
     * actual distance can differ by.
     * 1% sometimes causes an error - 1.2% should be safe.
     */
    private const val MAX_DISTANCE_DIFF_FRAC = .012
  }
}
