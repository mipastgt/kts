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

import org.locationtech.jts.algorithm.locate.PointOnGeometryLocator
import org.locationtech.jts.algorithm.locate.SimplePointInAreaLocator
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.Location
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.util.ComponentCoordinateExtracter

/**
 * A base class for predicate operations on [PreparedPolygon]s.
 *
 * @author mbdavis
 *
 */
abstract class PreparedPolygonPredicate(protected val prepPoly: PreparedPolygon) {

  private val targetPointLocator: PointOnGeometryLocator = prepPoly.getPointLocator()

  /**
   * Tests whether all components of the test Geometry
   * are contained in the target geometry.
   * Handles both linear and point components.
   *
   * @param testGeom a geometry to test
   * @return true if all components of the argument are contained in the target geometry
   */
  protected fun isAllTestComponentsInTarget(testGeom: Geometry): Boolean {
    val coords = ComponentCoordinateExtracter.getCoordinates(testGeom)
    for (o in coords) {
      val p = o as Coordinate
      val loc = targetPointLocator.locate(p)
      if (loc == Location.EXTERIOR)
        return false
    }
    return true
  }

  /**
   * Tests whether all components of the test Geometry
   * are contained in the interior of the target geometry.
   * Handles both linear and point components.
   *
   * @param testGeom a geometry to test
   * @return true if all components of the argument are contained in the target geometry interior
   */
  protected fun isAllTestComponentsInTargetInterior(testGeom: Geometry): Boolean {
    val coords = ComponentCoordinateExtracter.getCoordinates(testGeom)
    for (o in coords) {
      val p = o as Coordinate
      val loc = targetPointLocator.locate(p)
      if (loc != Location.INTERIOR)
        return false
    }
    return true
  }

  /**
   * Tests whether any component of the test Geometry intersects
   * the area of the target geometry.
   * Handles test geometries with both linear and point components.
   *
   * @param testGeom a geometry to test
   * @return true if any component of the argument intersects the prepared area geometry
   */
  protected fun isAnyTestComponentInTarget(testGeom: Geometry): Boolean {
    val coords = ComponentCoordinateExtracter.getCoordinates(testGeom)
    for (o in coords) {
      val p = o as Coordinate
      val loc = targetPointLocator.locate(p)
      if (loc != Location.EXTERIOR)
        return true
    }
    return false
  }

  /**
   * Tests whether all points of the test Pointal geometry
   * are contained in the target geometry.
   *
   * @param testGeom a Pointal geometry to test
   * @return true if all points of the argument are contained in the target geometry
   */
  protected fun isAllTestPointsInTarget(testGeom: Geometry): Boolean {
    for (i in 0 until testGeom.getNumGeometries()) {
      val pt = testGeom.getGeometryN(i) as Point
      val p = pt.getCoordinate()
      val loc = targetPointLocator.locate(p!!)
      if (loc == Location.EXTERIOR)
        return false
    }
    return true
  }

  /**
   * Tests whether any point of the test Geometry intersects
   * the interior of the target geometry.
   *
   * @param testGeom a geometry to test
   * @return true if any point of the argument intersects the prepared area geometry interior
   */
  protected fun isAnyTestPointInTargetInterior(testGeom: Geometry): Boolean {
    for (i in 0 until testGeom.getNumGeometries()) {
      val pt = testGeom.getGeometryN(i) as Point
      val p = pt.getCoordinate()
      val loc = targetPointLocator.locate(p!!)
      if (loc == Location.INTERIOR)
        return true
    }
    return false
  }

  /**
   * Tests whether any component of the target geometry
   * intersects the test geometry (which must be an areal geometry)
   *
   * @param testGeom the test geometry
   * @param targetRepPts the representative points of the target geometry
   * @return true if any component intersects the areal test geometry
   */
  protected fun isAnyTargetComponentInAreaTest(testGeom: Geometry, targetRepPts: List<Coordinate>): Boolean {
    val piaLoc: PointOnGeometryLocator = SimplePointInAreaLocator(testGeom)
    for (p in targetRepPts) {
      val loc = piaLoc.locate(p)
      if (loc != Location.EXTERIOR)
        return true
    }
    return false
  }

}
