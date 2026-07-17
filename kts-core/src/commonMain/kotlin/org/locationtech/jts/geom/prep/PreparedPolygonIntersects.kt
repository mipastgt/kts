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
import org.locationtech.jts.noding.SegmentStringUtil

/**
 * Computes the `intersects` spatial relationship predicate for
 * [PreparedPolygon]s relative to all other [Geometry] classes. Uses
 * short-circuit tests and indexing to improve performance.
 *
 * @author Martin Davis
 *
 */
class PreparedPolygonIntersects(prepPoly: PreparedPolygon) : PreparedPolygonPredicate(prepPoly) {

  /**
   * Tests whether this PreparedPolygon intersects a given geometry.
   *
   * @param geom
   *          the test geometry
   * @return true if the test geometry intersects
   */
  fun intersects(geom: Geometry): Boolean {
    /**
     * Do point-in-poly tests first, since they are cheaper and may result in a
     * quick positive result.
     *
     * If a point of any test components lie in target, result is true
     */
    val isInPrepGeomArea = isAnyTestComponentInTarget(geom)
    if (isInPrepGeomArea)
      return true
    /*
     * If input contains only points, then at
     * this point it is known that none of them are contained in the target
     */
    if (geom.getDimension() == 0)
      return false
    /**
     * If any segments intersect, result is true
     */
    val lineSegStr = SegmentStringUtil.extractSegmentStrings(geom)
    // only request intersection finder if there are segments
    // (i.e. NOT for point inputs)
    if (lineSegStr.size > 0) {
      val segsIntersect = prepPoly.getIntersectionFinder().intersects(lineSegStr)
      if (segsIntersect)
        return true
    }

    /*
     * If the test has dimension = 2 as well, it is necessary to test for proper
     * inclusion of the target. Since no segments intersect, it is sufficient to
     * test representative points.
     */
    if (geom.getDimension() == 2) {
      // TODO: generalize this to handle GeometryCollections
      val isPrepGeomInArea = isAnyTargetComponentInAreaTest(geom, prepPoly.getRepresentativePoints())
      if (isPrepGeomInArea)
        return true
    }

    return false
  }

  companion object {
    /**
     * Computes the intersects predicate between a [PreparedPolygon] and a
     * [Geometry].
     *
     * @param prep
     *          the prepared polygon
     * @param geom
     *          a test geometry
     * @return true if the polygon intersects the geometry
     */
    @JvmStatic
    fun intersects(prep: PreparedPolygon, geom: Geometry): Boolean {
      val polyInt = PreparedPolygonIntersects(prep)
      return polyInt.intersects(geom)
    }
  }
}
