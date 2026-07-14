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
package org.locationtech.jts.operation.overlay.validate

import kotlin.jvm.JvmStatic
import kotlin.math.min

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.Location
import org.locationtech.jts.operation.overlay.OverlayOp
import org.locationtech.jts.operation.overlay.snap.GeometrySnapper

/**
 * Validates that the result of an overlay operation is
 * geometrically correct, within a determined tolerance.
 * Uses fuzzy point location to find points which are
 * definitely in either the interior or exterior of the result
 * geometry, and compares these results with the expected ones.
 *
 * @author Martin Davis
 * @see OverlayOp
 */
class OverlayResultValidator(a: Geometry, b: Geometry, result: Geometry) {

  /**
   * The tolerance to use needs to depend on the size of the geometries.
   * It should not be more precise than double-precision can support.
   */
  private val boundaryDistanceTolerance = computeBoundaryDistanceTolerance(a, b)
  private val geom: Array<Geometry> = arrayOf(a, b, result)
  private val locFinder: Array<FuzzyPointLocator> = arrayOf(
    FuzzyPointLocator(geom[0], boundaryDistanceTolerance),
    FuzzyPointLocator(geom[1], boundaryDistanceTolerance),
    FuzzyPointLocator(geom[2], boundaryDistanceTolerance)
  )
  private val location = IntArray(3)
  private var invalidLocation: Coordinate? = null

  private val testCoords: MutableList<Coordinate> = ArrayList()

  fun isValid(overlayOp: Int): Boolean {
    addTestPts(geom[0])
    addTestPts(geom[1])
    val isValid = checkValid(overlayOp)
    return isValid
  }

  fun getInvalidLocation(): Coordinate? = invalidLocation

  private fun addTestPts(g: Geometry) {
    val ptGen = OffsetPointGenerator(g)
    testCoords.addAll(ptGen.getPoints(5 * boundaryDistanceTolerance))
  }

  private fun checkValid(overlayOp: Int): Boolean {
    for (i in testCoords.indices) {
      val pt = testCoords[i]
      if (!checkValid(overlayOp, pt)) {
        invalidLocation = pt
        return false
      }
    }
    return true
  }

  private fun checkValid(overlayOp: Int, pt: Coordinate): Boolean {
    location[0] = locFinder[0].getLocation(pt)
    location[1] = locFinder[1].getLocation(pt)
    location[2] = locFinder[2].getLocation(pt)

    /**
     * If any location is on the Boundary, can't deduce anything, so just return true
     */
    if (hasLocation(location, Location.BOUNDARY))
      return true

    return isValidResult(overlayOp, location)
  }

  private fun isValidResult(overlayOp: Int, location: IntArray): Boolean {
    val expectedInterior = OverlayOp.isResultOfOp(location[0], location[1], overlayOp)

    val resultInInterior = (location[2] == Location.INTERIOR)
    // MD use simpler: boolean isValid = (expectedInterior == resultInInterior);
    val isValid = !(expectedInterior xor resultInInterior)

    return isValid
  }

  companion object {
    private const val TOLERANCE = 0.000001

    @JvmStatic
    fun isValid(a: Geometry, b: Geometry, overlayOp: Int, result: Geometry): Boolean {
      val validator = OverlayResultValidator(a, b, result)
      return validator.isValid(overlayOp)
    }

    private fun computeBoundaryDistanceTolerance(g0: Geometry, g1: Geometry): Double {
      return min(
        GeometrySnapper.computeSizeBasedSnapTolerance(g0),
        GeometrySnapper.computeSizeBasedSnapTolerance(g1)
      )
    }

    private fun hasLocation(location: IntArray, loc: Int): Boolean {
      for (i in 0..2) {
        if (location[i] == loc)
          return true
      }
      return false
    }
  }
}
