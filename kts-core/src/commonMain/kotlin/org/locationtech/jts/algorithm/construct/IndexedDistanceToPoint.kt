/*
 * Copyright (c) 2023 Martin Davis.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.locationtech.jts.algorithm.construct

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.Location
import org.locationtech.jts.geom.Point
import org.locationtech.jts.operation.distance.IndexedFacetDistance

/**
 * Computes the distance between a point and a geometry
 * (which may be a collection containing any type of geometry).
 * Also computes the pair of points containing the input
 * point and the nearest point on the geometry.
 *
 * @author mdavis
 */
internal class IndexedDistanceToPoint(private val targetGeometry: Geometry) {

  private var facetDistance: IndexedFacetDistance? = null
  private var ptLocater: IndexedPointInPolygonsLocator? = null

  private fun init() {
    if (facetDistance != null)
      return
    facetDistance = IndexedFacetDistance(targetGeometry)
    ptLocater = IndexedPointInPolygonsLocator(targetGeometry)
  }

  /**
   * Computes the distance from a point to the geometry.
   *
   * @param pt the input point
   * @return the distance to the geometry
   */
  fun distance(pt: Point): Double {
    init()
    //-- distance is 0 if point is inside a target polygon
    if (isInArea(pt)) {
      return 0.0
    }
    return facetDistance!!.distance(pt)
  }

  private fun isInArea(pt: Point): Boolean {
    return Location.EXTERIOR != ptLocater!!.locate(pt.getCoordinate()!!)
  }

  /**
   * Gets the nearest locations between the geometry and a point.
   * The first location lies on the geometry,
   * and the second location is the provided point.
   *
   * @param pt the point to compute the nearest location for
   * @return a pair of locations
   */
  fun nearestPoints(pt: Point): Array<Coordinate> {
    init()
    if (isInArea(pt)) {
      val p = pt.getCoordinate()!!
      return arrayOf(p.copy(), p.copy())
    }
    return facetDistance!!.nearestPoints(pt)
  }
}
