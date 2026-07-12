/*
 * Copyright (c) 2022 Martin Davis.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.locationtech.jts.coverage

import kotlin.jvm.JvmStatic

import org.locationtech.jts.algorithm.construct.MaximumInscribedCircle
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.LinearRing
import org.locationtech.jts.geom.Polygon
import org.locationtech.jts.geom.util.PolygonExtracter

/**
 * Finds gaps in a polygonal coverage.
 *
 * @author mdavis
 */
class CoverageGapFinder(private val coverage: Array<Geometry>) {

  /**
   * Finds gaps in the coverage.
   *
   * @param gapWidth the maximum width of gap to detect
   * @return a geometry indicating the locations of gaps
   */
  fun findGaps(gapWidth: Double): Geometry {
    val union = CoverageUnion.union(coverage)
    @Suppress("UNCHECKED_CAST")
    val polygons = PolygonExtracter.getPolygons(union) as List<Polygon>

    val gapLines: MutableList<LineString> = ArrayList()
    for (poly in polygons) {
      for (i in 0 until poly.getNumInteriorRing()) {
        val hole = poly.getInteriorRingN(i)
        if (isGap(hole, gapWidth)) {
          gapLines.add(copyLine(hole))
        }
      }
    }
    return union!!.getFactory().buildGeometry(gapLines)
  }

  private fun copyLine(hole: LinearRing): LineString {
    val pts = hole.getCoordinates()
    return hole.getFactory().createLineString(pts)
  }

  private fun isGap(hole: LinearRing, gapWidth: Double): Boolean {
    val holePoly = hole.getFactory().createPolygon(hole)
    //-- guard against bad input
    if (gapWidth <= 0.0)
      return false

    val tolerance = gapWidth / 100
    val line = MaximumInscribedCircle.getRadiusLine(holePoly, tolerance)
    val width = line.getLength() * 2
    return width <= gapWidth
  }

  companion object {
    /**
     * Finds gaps in a polygonal coverage.
     *
     * @param coverage a set of polygons forming a polygonal coverage
     * @param gapWidth the maximum width of gap to detect
     * @return a geometry indicating the locations of gaps
     */
    @JvmStatic
    fun findGaps(coverage: Array<Geometry>, gapWidth: Double): Geometry {
      val finder = CoverageGapFinder(coverage)
      return finder.findGaps(gapWidth)
    }
  }
}
