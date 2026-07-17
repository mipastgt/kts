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
import kotlin.jvm.JvmField
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.pow

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.CoordinateFilter
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.PrecisionModel
import org.locationtech.jts.io.OrdinateFormat
import org.locationtech.jts.math.MathUtil

/**
 * Functions for computing precision model scale factors
 * that ensure robust geometry operations.
 *
 * @author Martin Davis
 */
class PrecisionUtil {

  companion object {
    /**
     * A number of digits of precision which leaves some computational "headroom"
     * to ensure robust evaluation of certain double-precision floating point geometric operations.
     *
     * This value should be less than the maximum decimal precision of double-precision values (16).
     */
    @JvmField
    var MAX_ROBUST_DP_DIGITS = 14

    /**
     * Determines a precision model to
     * use for robust overlay operations.
     *
     * @param a a geometry
     * @param b a geometry
     * @return a suitable precision model for overlay
     */
    @JvmStatic
    fun robustPM(a: Geometry, b: Geometry?): PrecisionModel {
      val scale = robustScale(a, b)
      return PrecisionModel(scale)
    }

    /**
     * Computes a safe scale factor for a numeric value.
     *
     * @param value a numeric value
     * @return a safe scale factor for the value
     */
    @JvmStatic
    fun safeScale(value: Double): Double {
      return precisionScale(value, MAX_ROBUST_DP_DIGITS)
    }

    /**
     * Computes a safe scale factor for a geometry.
     *
     * @param geom a geometry
     * @return a safe scale factor for the geometry ordinates
     */
    @JvmStatic
    fun safeScale(geom: Geometry): Double {
      return safeScale(maxBoundMagnitude(geom.getEnvelopeInternal()))
    }

    /**
     * Computes a safe scale factor for two geometries.
     *
     * @param a a geometry
     * @param b a geometry (which may be null)
     * @return a safe scale factor for the geometry ordinates
     */
    @JvmStatic
    fun safeScale(a: Geometry, b: Geometry?): Double {
      var maxBnd = maxBoundMagnitude(a.getEnvelopeInternal())
      if (b != null) {
        val maxBndB = maxBoundMagnitude(b.getEnvelopeInternal())
        maxBnd = max(maxBnd, maxBndB)
      }
      val scale = safeScale(maxBnd)
      return scale
    }

    /**
     * Determines the maximum magnitude (absolute value) of the bounds of an
     * of an envelope.
     *
     * @param env an envelope
     * @return the value of the maximum bound magnitude
     */
    private fun maxBoundMagnitude(env: Envelope): Double {
      return MathUtil.max(
        abs(env.getMaxX()),
        abs(env.getMaxY()),
        abs(env.getMinX()),
        abs(env.getMinY())
      )
    }

    /**
     * Computes the scale factor which will
     * produce a given number of digits of precision (significant digits)
     * when used to round the given number.
     *
     * @param value a number to be rounded
     * @param precisionDigits the number of digits of precision required
     * @return scale factor which provides the required number of digits of precision
     */
    private fun precisionScale(value: Double, precisionDigits: Int): Double {
      // the smallest power of 10 greater than the value
      val magnitude = (ln(value) / ln(10.0) + 1.0).toInt()
      val precDigits = precisionDigits - magnitude

      val scaleFactor = (10.0).pow(precDigits.toDouble())
      return scaleFactor
    }

    /**
     * Computes the inherent scale of a number.
     *
     * @param value a number
     * @return the inherent scale factor of the number
     */
    @JvmStatic
    fun inherentScale(value: Double): Double {
      val numDec = numberOfDecimals(value)
      val scaleFactor = (10.0).pow(numDec.toDouble())
      return scaleFactor
    }

    /**
     * Computes the inherent scale of a geometry.
     *
     * @param geom geometry
     * @return inherent scale of a geometry
     */
    @JvmStatic
    fun inherentScale(geom: Geometry): Double {
      val scaleFilter = InherentScaleFilter()
      geom.apply(scaleFilter)
      return scaleFilter.getScale()
    }

    /**
     * Computes the inherent scale of two geometries.
     *
     * @param a a geometry
     * @param b a geometry
     * @return the inherent scale factor of the two geometries
     */
    @JvmStatic
    fun inherentScale(a: Geometry, b: Geometry?): Double {
      var scale = inherentScale(a)
      if (b != null) {
        val scaleB = inherentScale(b)
        scale = max(scale, scaleB)
      }
      return scale
    }

    /**
     * Determines the
     * number of decimal places represented in a double-precision
     * number (as determined by Java).
     *
     * @param value a numeric value
     * @return the number of decimal places in the value
     */
    private fun numberOfDecimals(value: Double): Int {
      /**
       * Ensure that scientific notation is NOT used
       * (it would skew the number of fraction digits)
       */
      val s = OrdinateFormat.DEFAULT.format(value)
      if (s.endsWith(".0"))
        return 0
      val len = s.length
      val decIndex = s.indexOf('.')
      if (decIndex <= 0)
        return 0
      return len - decIndex - 1
    }

    /**
     * Applies the inherent scale calculation
     * to every ordinate in a geometry.
     *
     * @author Martin Davis
     */
    private class InherentScaleFilter : CoordinateFilter {

      private var scale = 0.0

      fun getScale(): Double {
        return scale
      }

      override fun filter(coord: Coordinate) {
        updateScaleMax(coord.getX())
        updateScaleMax(coord.getY())
      }

      private fun updateScaleMax(value: Double) {
        val scaleVal = inherentScale(value)
        if (scaleVal > scale) {
          scale = scaleVal
        }
      }
    }

    /**
     * Determines a precision model to
     * use for robust overlay operations for one geometry.
     *
     * @param a a geometry
     * @return a suitable precision model for overlay
     */
    @JvmStatic
    fun robustPM(a: Geometry): PrecisionModel {
      val scale = robustScale(a)
      return PrecisionModel(scale)
    }

    /**
     * Determines a scale factor which maximizes
     * the digits of precision and is
     * safe to use for overlay operations.
     *
     * @param a a geometry
     * @param b a geometry
     * @return a scale factor for use in overlay operations
     */
    @JvmStatic
    fun robustScale(a: Geometry, b: Geometry?): Double {
      val inherentScale = inherentScale(a, b)
      val safeScale = safeScale(a, b)
      return robustScale(inherentScale, safeScale)
    }

    /**
     * Determines a scale factor which maximizes
     * the digits of precision and is
     * safe to use for overlay operations.
     *
     * @param a a geometry
     * @return a scale factor for use in overlay operations
     */
    @JvmStatic
    fun robustScale(a: Geometry): Double {
      val inherentScale = inherentScale(a)
      val safeScale = safeScale(a)
      return robustScale(inherentScale, safeScale)
    }

    private fun robustScale(inherentScale: Double, safeScale: Double): Double {
      /*
       * Use safe scale if lower,
       * since it is important to preserve some precision for robustness
       */
      if (inherentScale <= safeScale) {
        return inherentScale
      }
      return safeScale
    }
  }
}
