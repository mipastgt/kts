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
import org.locationtech.jts.geom.Polygonal
import org.locationtech.jts.noding.SegmentStringUtil

/**
 * Computes the `containsProperly` spatial relationship predicate
 * for [PreparedPolygon]s relative to all other [Geometry] classes.
 * Uses short-circuit tests and indexing to improve performance.
 *
 * @author Martin Davis
 */
class PreparedPolygonContainsProperly(prepPoly: PreparedPolygon) : PreparedPolygonPredicate(prepPoly) {

  /**
   * Tests whether this PreparedPolygon containsProperly a given geometry.
   *
   * @param geom the test geometry
   * @return true if the test geometry is contained properly
   */
  fun containsProperly(geom: Geometry): Boolean {
    /**
     * Do point-in-poly tests first, since they are cheaper and may result
     * in a quick negative result.
     *
     * If a point of any test components does not lie in the target interior, result is false
     */
    val isAllInPrepGeomAreaInterior = isAllTestComponentsInTargetInterior(geom)
    if (!isAllInPrepGeomAreaInterior) return false

    /**
     * If any segments intersect, result is false.
     */
    val lineSegStr = SegmentStringUtil.extractSegmentStrings(geom)
    val segsIntersect = prepPoly.getIntersectionFinder().intersects(lineSegStr)
    if (segsIntersect)
      return false

    /**
     * Given that no segments intersect, if any vertex of the target
     * is contained in some test component.
     * the test is NOT properly contained.
     */
    if (geom is Polygonal) {
      // TODO: generalize this to handle GeometryCollections
      val isTargetGeomInTestArea = isAnyTargetComponentInAreaTest(geom, prepPoly.getRepresentativePoints())
      if (isTargetGeomInTestArea) return false
    }

    return true
  }

  companion object {
    /**
     * Computes the `containsProperly` predicate between a [PreparedPolygon]
     * and a [Geometry].
     *
     * @param prep the prepared polygon
     * @param geom a test geometry
     * @return true if the polygon properly contains the geometry
     */
    @JvmStatic
    fun containsProperly(prep: PreparedPolygon, geom: Geometry): Boolean {
      val polyInt = PreparedPolygonContainsProperly(prep)
      return polyInt.containsProperly(geom)
    }
  }
}
