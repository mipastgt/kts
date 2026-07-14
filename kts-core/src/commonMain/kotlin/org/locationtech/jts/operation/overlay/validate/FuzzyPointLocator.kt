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
package org.locationtech.jts.operation.overlay.validate

import org.locationtech.jts.algorithm.PointLocator
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.GeometryFilter
import org.locationtech.jts.geom.LineSegment
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.Location
import org.locationtech.jts.geom.MultiLineString
import org.locationtech.jts.geom.Polygon

/**
 * Finds the most likely [Location] of a point relative to
 * the polygonal components of a geometry, using a tolerance value.
 * If a point is not clearly in the Interior or Exterior,
 * it is considered to be on the Boundary.
 *
 * @author Martin Davis
 */
class FuzzyPointLocator(private val g: Geometry, private val boundaryDistanceTolerance: Double) {

  private val linework: MultiLineString = extractLinework(g)
  private val ptLocator = PointLocator()
  private val seg = LineSegment()

  fun getLocation(pt: Coordinate): Int {
    if (isWithinToleranceOfBoundary(pt))
      return Location.BOUNDARY
    /*
    double dist = linework.distance(point);
    if (dist < tolerance)
      return Location.BOUNDARY;
     */

    // now we know point must be clearly inside or outside geometry, so return actual location value
    return ptLocator.locate(pt, g)
  }

  /**
   * Extracts linework for polygonal components.
   *
   * @param g the geometry from which to extract
   * @return a lineal geometry containing the extracted linework
   */
  private fun extractLinework(g: Geometry): MultiLineString {
    val extracter = PolygonalLineworkExtracter()
    g.apply(extracter)
    val linework = extracter.getLinework()
    val lines = GeometryFactory.toLineStringArray(linework)
    return g.getFactory().createMultiLineString(lines)
  }

  private fun isWithinToleranceOfBoundary(pt: Coordinate): Boolean {
    for (i in 0 until linework.getNumGeometries()) {
      val line = linework.getGeometryN(i) as LineString
      val seq = line.getCoordinateSequence()
      for (j in 0 until seq.size() - 1) {
        seq.getCoordinate(j, seg.p0)
        seq.getCoordinate(j + 1, seg.p1)
        val dist = seg.distance(pt)
        if (dist <= boundaryDistanceTolerance)
          return true
      }
    }
    return false
  }
}

/**
 * Extracts the LineStrings in the boundaries
 * of all the polygonal elements in the target [Geometry].
 *
 * @author Martin Davis
 */
internal class PolygonalLineworkExtracter : GeometryFilter {
  private val linework: MutableList<LineString> = ArrayList()

  /**
   * Filters out all linework for polygonal elements
   */
  override fun filter(geom: Geometry) {
    if (geom is Polygon) {
      linework.add(geom.getExteriorRing())
      for (i in 0 until geom.getNumInteriorRing()) {
        linework.add(geom.getInteriorRingN(i))
      }
    }
  }

  /**
   * Gets the list of polygonal linework.
   *
   * @return a List of LineStrings
   */
  fun getLinework(): MutableList<LineString> = linework
}
