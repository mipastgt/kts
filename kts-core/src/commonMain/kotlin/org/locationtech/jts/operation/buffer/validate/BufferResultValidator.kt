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

import kotlin.jvm.JvmStatic

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.MultiPolygon
import org.locationtech.jts.geom.Polygon

/**
 * Validates that the result of a buffer operation
 * is geometrically correct, within a computed tolerance.
 *
 * @author Martin Davis
 */
class BufferResultValidator(
  private val input: Geometry,
  private val distance: Double,
  private val result: Geometry
) {

  private var valid = true
  private var errorMsg: String? = null
  private var errorLocation: Coordinate? = null
  private var errorIndicator: Geometry? = null

  fun isValid(): Boolean {
    checkPolygonal()
    if (!valid) return valid
    checkExpectedEmpty()
    if (!valid) return valid
    checkEnvelope()
    if (!valid) return valid
    checkArea()
    if (!valid) return valid
    checkDistance()
    return valid
  }

  fun getErrorMessage(): String? {
    return errorMsg
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

  private fun report(checkName: String) {
    if (!VERBOSE) return
  }

  private fun checkPolygonal() {
    if (!(result is Polygon || result is MultiPolygon)) valid = false
    errorMsg = "Result is not polygonal"
    errorIndicator = result
    report("Polygonal")
  }

  private fun checkExpectedEmpty() {
    // can't check areal features
    if (input.getDimension() >= 2) return
    // can't check positive distances
    if (distance > 0.0) return

    // at this point can expect an empty result
    if (!result.isEmpty()) {
      valid = false
      errorMsg = "Result is non-empty"
      errorIndicator = result
    }
    report("ExpectedEmpty")
  }

  private fun checkEnvelope() {
    if (distance < 0.0) return

    var padding = distance * MAX_ENV_DIFF_FRAC
    if (padding == 0.0) padding = 0.001

    val expectedEnv = Envelope(input.getEnvelopeInternal())
    expectedEnv.expandBy(distance)

    val bufEnv = Envelope(result.getEnvelopeInternal())
    bufEnv.expandBy(padding)

    if (!bufEnv.contains(expectedEnv)) {
      valid = false
      errorMsg = "Buffer envelope is incorrect"
      errorIndicator = input.getFactory().toGeometry(bufEnv)
    }
    report("Envelope")
  }

  private fun checkArea() {
    val inputArea = input.getArea()
    val resultArea = result.getArea()

    if (distance > 0.0 && inputArea > resultArea) {
      valid = false
      errorMsg = "Area of positive buffer is smaller than input"
      errorIndicator = result
    }
    if (distance < 0.0 && inputArea < resultArea) {
      valid = false
      errorMsg = "Area of negative buffer is larger than input"
      errorIndicator = result
    }
    report("Area")
  }

  private fun checkDistance() {
    val distValid = BufferDistanceValidator(input, distance, result)
    if (!distValid.isValid()) {
      valid = false
      errorMsg = distValid.getErrorMessage()
      errorLocation = distValid.getErrorLocation()
      errorIndicator = distValid.getErrorIndicator()
    }
    report("Distance")
  }

  companion object {
    private const val VERBOSE = false

    /**
     * Maximum allowable fraction of buffer distance the
     * actual distance can differ by.
     * 1% sometimes causes an error - 1.2% should be safe.
     */
    private const val MAX_ENV_DIFF_FRAC = .012

    @JvmStatic
    fun isValid(g: Geometry, distance: Double, result: Geometry): Boolean {
      val validator = BufferResultValidator(g, distance, result)
      if (validator.isValid()) return true
      return false
    }

    /**
     * Checks whether the geometry buffer is valid,
     * and returns an error message if not.
     *
     * @return an appropriate error message
     * or null if the buffer is valid
     */
    @JvmStatic
    fun isValidMsg(g: Geometry, distance: Double, result: Geometry): String? {
      val validator = BufferResultValidator(g, distance, result)
      if (!validator.isValid()) return validator.getErrorMessage()
      return null
    }
  }
}
