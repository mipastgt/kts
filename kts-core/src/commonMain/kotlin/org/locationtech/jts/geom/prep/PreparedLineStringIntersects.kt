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

import org.locationtech.jts.algorithm.PointLocator
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.util.ComponentCoordinateExtracter
import org.locationtech.jts.noding.SegmentStringUtil

/**
 * Computes the <tt>intersects</tt> spatial relationship predicate
 * for a target [PreparedLineString] relative to other [Geometry] classes.
 * Uses short-circuit tests and indexing to improve performance.
 *
 * @author Martin Davis
 *
 */
class PreparedLineStringIntersects(private val prepLine: PreparedLineString) {

  /**
   * Tests whether this geometry intersects a given geometry.
   *
   * @param geom the test geometry
   * @return true if the test geometry intersects
   */
  fun intersects(geom: Geometry): Boolean {
    /**
     * If any segments intersect, obviously intersects = true
     */
    val lineSegStr = SegmentStringUtil.extractSegmentStrings(geom)
    // only request intersection finder if there are segments (ie NOT for point inputs)
    if (lineSegStr.size > 0) {
      val segsIntersect = prepLine.getIntersectionFinder().intersects(lineSegStr)
      if (segsIntersect)
        return true
    }

    /**
     * For L/A case, need to check for proper inclusion of the target in the test
     */
    if (geom.getDimension() == 2 &&
        prepLine.isAnyTargetComponentInTest(geom)) return true

    /**
     * For L/P case, need to check if any points lie on line(s)
     */
    if (geom.hasDimension(0))
      return isAnyTestPointInTarget(geom)

    return false
  }

  /**
   * Tests whether any representative point of the test Geometry intersects
   * the target geometry.
   * Only handles test geometries which are Puntal (dimension 0)
   *
   * @param testGeom a Puntal geometry to test
   * @return true if any point of the argument intersects the prepared geometry
   */
  protected fun isAnyTestPointInTarget(testGeom: Geometry): Boolean {
    /**
     * This could be optimized by using the segment index on the lineal target.
     * However, it seems like the L/P case would be pretty rare in practice.
     */
    val locator = PointLocator()
    val coords = ComponentCoordinateExtracter.getCoordinates(testGeom)
    for (o in coords) {
      val p = o as Coordinate
      if (locator.intersects(p, prepLine.getGeometry()))
        return true
    }
    return false
  }

  companion object {
    /**
     * Computes the intersects predicate between a [PreparedLineString]
     * and a [Geometry].
     *
     * @param prep the prepared linestring
     * @param geom a test geometry
     * @return true if the linestring intersects the geometry
     */
    @JvmStatic
    fun intersects(prep: PreparedLineString, geom: Geometry): Boolean {
      val op = PreparedLineStringIntersects(prep)
      return op.intersects(geom)
    }
  }
}
