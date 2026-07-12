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
 * Computes the <tt>contains</tt> spatial relationship predicate
 * for a [PreparedPolygon] relative to all other [Geometry] classes.
 * Uses short-circuit tests and indexing to improve performance.
 *
 * @author Martin Davis
 *
 */
class PreparedPolygonContains(prepPoly: PreparedPolygon) : AbstractPreparedPolygonContains(prepPoly) {

  /**
   * Tests whether this PreparedPolygon <tt>contains</tt> a given geometry.
   *
   * @param geom the test geometry
   * @return true if the test geometry is contained
   */
  fun contains(geom: Geometry): Boolean {
    return eval(geom)
  }

  /**
   * Computes the full topological <tt>contains</tt> predicate.
   * Used when short-circuit tests are not conclusive.
   *
   * @param geom the test geometry
   * @return true if this prepared polygon contains the test geometry
   */
  override fun fullTopologicalPredicate(geom: Geometry): Boolean {
    val isContained = prepPoly.getGeometry().contains(geom)
    return isContained
  }

  companion object {
    /**
     * Computes the </tt>contains</tt> predicate between a [PreparedPolygon]
     * and a [Geometry].
     *
     * @param prep the prepared polygon
     * @param geom a test geometry
     * @return true if the polygon contains the geometry
     */
    @JvmStatic
    fun contains(prep: PreparedPolygon, geom: Geometry): Boolean {
      val polyInt = PreparedPolygonContains(prep)
      return polyInt.contains(geom)
    }
  }
}
