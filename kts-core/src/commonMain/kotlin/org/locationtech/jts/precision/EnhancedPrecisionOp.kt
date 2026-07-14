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
package org.locationtech.jts.precision

import kotlin.jvm.JvmStatic

import org.locationtech.jts.geom.Geometry

/**
 * Provides versions of Geometry spatial functions which use
 * enhanced precision techniques to reduce the likelihood of robustness problems.
 *
 */
class EnhancedPrecisionOp {
  companion object {
    /**
     * Computes the set-theoretic intersection of two [Geometry]s, using enhanced precision.
     */
    @JvmStatic
    fun intersection(geom0: Geometry, geom1: Geometry): Geometry {
      val originalEx: RuntimeException
      try {
        val result = geom0.intersection(geom1)
        return result
      } catch (ex: RuntimeException) {
        originalEx = ex
      }
      /*
       * If we are here, the original op encountered a precision problem
       * (or some other problem).  Retry the operation with
       * enhanced precision to see if it succeeds
       */
      try {
        val cbo = CommonBitsOp(true)
        val resultEP = cbo.intersection(geom0, geom1)
        // check that result is a valid geometry after the reshift to original precision
        if (!resultEP.isValid())
          throw originalEx
        return resultEP
      } catch (ex2: RuntimeException) {
        throw originalEx
      }
    }

    /**
     * Computes the set-theoretic union of two [Geometry]s, using enhanced precision.
     */
    @JvmStatic
    fun union(geom0: Geometry, geom1: Geometry): Geometry {
      val originalEx: RuntimeException
      try {
        val result = geom0.union(geom1)
        return result
      } catch (ex: RuntimeException) {
        originalEx = ex
      }
      try {
        val cbo = CommonBitsOp(true)
        val resultEP = cbo.union(geom0, geom1)
        if (!resultEP.isValid())
          throw originalEx
        return resultEP
      } catch (ex2: RuntimeException) {
        throw originalEx
      }
    }

    /**
     * Computes the set-theoretic difference of two [Geometry]s, using enhanced precision.
     */
    @JvmStatic
    fun difference(geom0: Geometry, geom1: Geometry): Geometry {
      val originalEx: RuntimeException
      try {
        val result = geom0.difference(geom1)
        return result
      } catch (ex: RuntimeException) {
        originalEx = ex
      }
      try {
        val cbo = CommonBitsOp(true)
        val resultEP = cbo.difference(geom0, geom1)
        if (!resultEP.isValid())
          throw originalEx
        return resultEP
      } catch (ex2: RuntimeException) {
        throw originalEx
      }
    }

    /**
     * Computes the set-theoretic symmetric difference of two [Geometry]s, using enhanced precision.
     */
    @JvmStatic
    fun symDifference(geom0: Geometry, geom1: Geometry): Geometry {
      val originalEx: RuntimeException
      try {
        val result = geom0.symDifference(geom1)
        return result
      } catch (ex: RuntimeException) {
        originalEx = ex
      }
      try {
        val cbo = CommonBitsOp(true)
        val resultEP = cbo.symDifference(geom0, geom1)
        if (!resultEP.isValid())
          throw originalEx
        return resultEP
      } catch (ex2: RuntimeException) {
        throw originalEx
      }
    }

    /**
     * Computes the buffer of a [Geometry], using enhanced precision.
     */
    @JvmStatic
    fun buffer(geom: Geometry, distance: Double): Geometry {
      val originalEx: RuntimeException
      try {
        val result = geom.buffer(distance)
        return result
      } catch (ex: RuntimeException) {
        originalEx = ex
      }
      try {
        val cbo = CommonBitsOp(true)
        val resultEP = cbo.buffer(geom, distance)
        if (!resultEP.isValid())
          throw originalEx
        return resultEP
      } catch (ex2: RuntimeException) {
        throw originalEx
      }
    }
  }
}
