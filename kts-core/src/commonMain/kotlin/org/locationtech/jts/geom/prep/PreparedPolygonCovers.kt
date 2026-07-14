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
package org.locationtech.jts.geom.prep

import kotlin.jvm.JvmStatic

import org.locationtech.jts.geom.Geometry

/**
 * Computes the `covers` spatial relationship predicate
 * for a [PreparedPolygon] relative to all other [Geometry] classes.
 * Uses short-circuit tests and indexing to improve performance.
 *
 * @author Martin Davis
 *
 */
class PreparedPolygonCovers(prepPoly: PreparedPolygon) : AbstractPreparedPolygonContains(prepPoly) {

  init {
    requireSomePointInInterior = false
  }

  /**
   * Tests whether this PreparedPolygon `covers` a given geometry.
   *
   * @param geom the test geometry
   * @return true if the test geometry is covered
   */
  fun covers(geom: Geometry): Boolean {
    return eval(geom)
  }

  /**
   * Computes the full topological `covers` predicate.
   * Used when short-circuit tests are not conclusive.
   *
   * @param geom the test geometry
   * @return true if this prepared polygon covers the test geometry
   */
  override fun fullTopologicalPredicate(geom: Geometry): Boolean {
    val result = prepPoly.getGeometry().covers(geom)
    return result
  }

  companion object {
    /**
     * Computes the `covers` predicate between a [PreparedPolygon]
     * and a [Geometry].
     *
     * @param prep the prepared polygon
     * @param geom a test geometry
     * @return true if the polygon covers the geometry
     */
    @JvmStatic
    fun covers(prep: PreparedPolygon, geom: Geometry): Boolean {
      val polyInt = PreparedPolygonCovers(prep)
      return polyInt.covers(geom)
    }
  }
}
