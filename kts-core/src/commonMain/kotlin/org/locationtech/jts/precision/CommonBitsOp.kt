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

import org.locationtech.jts.geom.Geometry

/**
 * Provides versions of Geometry spatial functions which use
 * common bit removal to reduce the likelihood of robustness problems.
 *
 * @version 1.7
 */
class CommonBitsOp(private val returnToOriginalPrecision: Boolean) {

  private lateinit var cbr: CommonBitsRemover

  /**
   * Creates a new instance of class, which reshifts result [Geometry]s.
   */
  constructor() : this(true)

  /**
   * Computes the set-theoretic intersection of two [Geometry]s, using enhanced precision.
   */
  fun intersection(geom0: Geometry, geom1: Geometry): Geometry {
    val geom = removeCommonBits(geom0, geom1)
    return computeResultPrecision(geom[0].intersection(geom[1]))
  }

  /**
   * Computes the set-theoretic union of two [Geometry]s, using enhanced precision.
   */
  fun union(geom0: Geometry, geom1: Geometry): Geometry {
    val geom = removeCommonBits(geom0, geom1)
    return computeResultPrecision(geom[0].union(geom[1]))
  }

  /**
   * Computes the set-theoretic difference of two [Geometry]s, using enhanced precision.
   */
  fun difference(geom0: Geometry, geom1: Geometry): Geometry {
    val geom = removeCommonBits(geom0, geom1)
    return computeResultPrecision(geom[0].difference(geom[1]))
  }

  /**
   * Computes the set-theoretic symmetric difference of two geometries,
   * using enhanced precision.
   */
  fun symDifference(geom0: Geometry, geom1: Geometry): Geometry {
    val geom = removeCommonBits(geom0, geom1)
    return computeResultPrecision(geom[0].symDifference(geom[1]))
  }

  /**
   * Computes the buffer a geometry,
   * using enhanced precision.
   */
  fun buffer(geom0: Geometry, distance: Double): Geometry {
    val geom = removeCommonBits(geom0)
    return computeResultPrecision(geom.buffer(distance))
  }

  /**
   * If required, returning the result to the original precision if required.
   */
  private fun computeResultPrecision(result: Geometry): Geometry {
    if (returnToOriginalPrecision)
      cbr.addCommonBits(result)
    return result
  }

  /**
   * Computes a copy of the input [Geometry] with the calculated common bits
   * removed from each coordinate.
   */
  private fun removeCommonBits(geom0: Geometry): Geometry {
    cbr = CommonBitsRemover()
    cbr.add(geom0)
    val geom = cbr.removeCommonBits(geom0.copy())
    return geom
  }

  /**
   * Computes a copy of each input [Geometry]s with the calculated common bits
   * removed from each coordinate.
   */
  private fun removeCommonBits(geom0: Geometry, geom1: Geometry): Array<Geometry> {
    cbr = CommonBitsRemover()
    cbr.add(geom0)
    cbr.add(geom1)
    val geom = arrayOfNulls<Geometry>(2)
    geom[0] = cbr.removeCommonBits(geom0.copy())
    geom[1] = cbr.removeCommonBits(geom1.copy())
    @Suppress("UNCHECKED_CAST")
    return geom as Array<Geometry>
  }
}
