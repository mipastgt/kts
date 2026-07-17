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

import org.locationtech.jts.algorithm.locate.IndexedPointInAreaLocator
import org.locationtech.jts.algorithm.locate.PointOnGeometryLocator
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.MultiPolygon
import org.locationtech.jts.geom.Polygon
import org.locationtech.jts.geom.Polygonal
import org.locationtech.jts.noding.FastSegmentSetIntersectionFinder
import org.locationtech.jts.noding.SegmentStringUtil
import org.locationtech.jts.operation.predicate.RectangleContains
import org.locationtech.jts.operation.predicate.RectangleIntersects

/**
 * A prepared version for [Polygonal] geometries.
 * This class supports both [Polygon]s and [MultiPolygon]s.
 *
 * Instances of this class are thread-safe and immutable.
 *
 * @author mbdavis
 *
 */
open class PreparedPolygon(poly: Polygonal) : BasicPreparedGeometry(poly as Geometry) {

  private val isRectangle: Boolean = getGeometry().isRectangle()
  // create these lazily, since they are expensive
  private var segIntFinder: FastSegmentSetIntersectionFinder? = null
  private var pia: PointOnGeometryLocator? = null

  /**
   * Gets the indexed intersection finder for this geometry.
   *
   * @return the intersection finder
   */
  fun getIntersectionFinder(): FastSegmentSetIntersectionFinder {
    /*
     * MD - Another option would be to use a simple scan for
     * segment testing for small geometries.
     * However, testing indicates that there is no particular advantage
     * to this approach.
     */
    if (segIntFinder == null)
      segIntFinder = FastSegmentSetIntersectionFinder(SegmentStringUtil.extractSegmentStrings(getGeometry()))
    return segIntFinder!!
  }

  fun getPointLocator(): PointOnGeometryLocator {
    if (pia == null)
      pia = IndexedPointInAreaLocator(getGeometry())

    return pia!!
  }

  override fun intersects(g: Geometry): Boolean {
    // envelope test
    if (!envelopesIntersect(g)) return false

    // optimization for rectangles
    if (isRectangle) {
      return RectangleIntersects.intersects(getGeometry() as Polygon, g)
    }

    return PreparedPolygonIntersects.intersects(this, g)
  }

  override fun contains(g: Geometry): Boolean {
    // short-circuit test
    if (!envelopeCovers(g))
      return false

    // optimization for rectangles
    if (isRectangle) {
      return RectangleContains.contains(getGeometry() as Polygon, g)
    }

    return PreparedPolygonContains.contains(this, g)
  }

  override fun containsProperly(g: Geometry): Boolean {
    // short-circuit test
    if (!envelopeCovers(g))
      return false
    return PreparedPolygonContainsProperly.containsProperly(this, g)
  }

  override fun covers(g: Geometry): Boolean {
    // short-circuit test
    if (!envelopeCovers(g))
      return false
    // optimization for rectangle arguments
    if (isRectangle) {
      return true
    }
    return PreparedPolygonCovers.covers(this, g)
  }
}
