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

import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.Polygon
import org.locationtech.jts.geom.Polygonal
import org.locationtech.jts.noding.SegmentIntersectionDetector
import org.locationtech.jts.noding.SegmentStringUtil

/**
 * A base class containing the logic for computes the `contains`
 * and `covers` spatial relationship predicates
 * for a [PreparedPolygon] relative to all other [Geometry] classes.
 * Uses short-circuit tests and indexing to improve performance.
 *
 * @author Martin Davis
 *
 */
abstract class AbstractPreparedPolygonContains(prepPoly: PreparedPolygon) : PreparedPolygonPredicate(prepPoly) {
  /**
   * This flag controls a difference between contains and covers.
   *
   * For contains the value is true.
   * For covers the value is false.
   */
  protected var requireSomePointInInterior = true

  // information about geometric situation
  private var hasSegmentIntersection = false
  private var hasProperIntersection = false
  private var hasNonProperIntersection = false

  /**
   * Evaluate the `contains` or `covers` relationship
   * for the given geometry.
   *
   * @param geom the test geometry
   * @return true if the test geometry is contained
   */
  protected fun eval(geom: Geometry): Boolean {
    if (geom.getDimension() == 0) {
      return evalPoints(geom)
    }

    /**
     * Do point-in-poly tests first, since they are cheaper and may result
     * in a quick negative result.
     *
     * If a point of any test components does not lie in target, result is false
     */
    val isAllInTargetArea = isAllTestComponentsInTarget(geom)
    if (!isAllInTargetArea) return false

    /**
     * Check if there is any intersection between the line segments
     * in target and test.
     */
    val properIntersectionImpliesNotContained = isProperIntersectionImpliesNotContainedSituation(geom)

    // find all intersection types which exist
    findAndClassifyIntersections(geom)

    if (properIntersectionImpliesNotContained && hasProperIntersection)
      return false

    /*
     * If all intersections are proper
     * (i.e. no non-proper intersections occur)
     * we can conclude that the test geometry is not contained in the target area.
     */
    if (hasSegmentIntersection && !hasNonProperIntersection)
      return false

    /*
     * If there is a segment intersection and the situation is not one
     * of the ones above, the only choice is to compute the full topological
     * relationship.
     */
    if (hasSegmentIntersection) {
      return fullTopologicalPredicate(geom)
    }

    /*
     * This tests for the case where a ring of the target lies inside
     * a test polygon - which implies the exterior of the Target
     * intersects the interior of the Test, and hence the result is false
     */
    if (geom is Polygonal) {
      // TODO: generalize this to handle GeometryCollections
      val isTargetInTestArea = isAnyTargetComponentInAreaTest(geom, prepPoly.getRepresentativePoints())
      if (isTargetInTestArea) return false
    }
    return true
  }

  /**
   * Evaluation optimized for Point geometries.
   * This provides about a 2x performance increase, and less memory usage.
   *
   * @param geom a Point or MultiPoint geometry
   * @return the value of the predicate being evaluated
   */
  private fun evalPoints(geom: Geometry): Boolean {
    /**
     * Do point-in-poly tests first, since they are cheaper and may result
     * in a quick negative result.
     *
     * If a point of any test components does not lie in target, result is false
     */
    val isAllInTargetArea = isAllTestPointsInTarget(geom)
    if (!isAllInTargetArea) return false

    /*
     * If the test geometry consists of only Points,
     * then it is now sufficient to test if any of those
     * points lie in the interior of the target geometry.
     */
    if (requireSomePointInInterior) {
      val isAnyInTargetInterior = isAnyTestPointInTargetInterior(geom)
      return isAnyInTargetInterior
    }
    return true
  }

  private fun isProperIntersectionImpliesNotContainedSituation(testGeom: Geometry): Boolean {
    /*
     * If the test geometry is polygonal we have the A/A situation.
     */
    if (testGeom is Polygonal) return true
    /*
     * A single shell with no holes allows concluding that
     * a proper intersection implies not contained
     */
    if (isSingleShell(prepPoly.getGeometry())) return true
    return false
  }

  /**
   * Tests whether a geometry consists of a single polygon with no holes.
   *
   * @return true if the geometry is a single polygon with no holes
   */
  private fun isSingleShell(geom: Geometry): Boolean {
    // handles single-element MultiPolygons, as well as Polygons
    if (geom.getNumGeometries() != 1) return false

    val poly = geom.getGeometryN(0) as Polygon
    val numHoles = poly.getNumInteriorRing()
    if (numHoles == 0) return true
    return false
  }

  private fun findAndClassifyIntersections(geom: Geometry) {
    val lineSegStr = SegmentStringUtil.extractSegmentStrings(geom)

    val intDetector = SegmentIntersectionDetector()
    intDetector.setFindAllIntersectionTypes(true)
    prepPoly.getIntersectionFinder().intersects(lineSegStr, intDetector)

    hasSegmentIntersection = intDetector.hasIntersection()
    hasProperIntersection = intDetector.hasProperIntersection()
    hasNonProperIntersection = intDetector.hasNonProperIntersection()
  }

  /**
   * Computes the full topological predicate.
   * Used when short-circuit tests are not conclusive.
   *
   * @param geom the test geometry
   * @return true if this prepared polygon has the relationship with the test geometry
   */
  protected abstract fun fullTopologicalPredicate(geom: Geometry): Boolean

}
