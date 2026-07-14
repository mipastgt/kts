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
package org.locationtech.jts.algorithm

import kotlin.jvm.JvmStatic

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.CoordinateSequence
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryCollection
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.LinearRing
import org.locationtech.jts.geom.Polygon
import org.locationtech.jts.util.Assert

/**
 * Computes a point in the interior of an areal geometry.
 * The point will lie in the geometry interior
 * in all except certain pathological cases.
 *
 */
class InteriorPointArea(g: Geometry) {

  private var interiorPoint: Coordinate? = null
  private var maxWidth = -1.0

  init {
    process(g)
  }

  /**
   * Gets the computed interior point.
   *
   * @return the coordinate of an interior point
   *  or `null` if the input geometry is empty
   */
  fun getInteriorPoint(): Coordinate? {
    return interiorPoint
  }

  /**
   * Processes a geometry to determine
   * the best interior point for
   * all component polygons.
   *
   * @param geom the geometry to process
   */
  private fun process(geom: Geometry) {
    if (geom.isEmpty())
      return

    if (geom is Polygon) {
      processPolygon(geom)
    } else if (geom is GeometryCollection) {
      for (i in 0 until geom.getNumGeometries()) {
        process(geom.getGeometryN(i))
      }
    }
  }

  /**
   * Computes an interior point of a component Polygon
   * and updates current best interior point
   * if appropriate.
   *
   * @param polygon the polygon to process
   */
  private fun processPolygon(polygon: Polygon) {
    val intPtPoly = InteriorPointPolygon(polygon)
    intPtPoly.process()
    val width = intPtPoly.getWidth()
    if (width > maxWidth) {
      maxWidth = width
      interiorPoint = intPtPoly.getInteriorPoint()
    }
  }

  /**
   * Computes an interior point in a single [Polygon],
   * as well as the width of the scan-line section it occurs in
   * to allow choosing the widest section occurrence.
   *
   * @author mdavis
   *
   */
  private class InteriorPointPolygon(private val polygon: Polygon) {
    private val interiorPointY: Double = ScanLineYOrdinateFinder.getScanLineY(polygon)
    private var interiorSectionWidth = 0.0
    private var interiorPoint: Coordinate? = null

    /**
     * Gets the computed interior point.
     *
     * @return the interior point coordinate,
     *  or `null` if the input geometry is empty
     */
    fun getInteriorPoint(): Coordinate? {
      return interiorPoint
    }

    /**
     * Gets the width of the scanline section containing the interior point.
     *
     * @return the width
     */
    fun getWidth(): Double {
      return interiorSectionWidth
    }

    /**
     * Compute the interior point.
     */
    fun process() {
      /**
       * This results in returning a null Coordinate
       */
      if (polygon.isEmpty()) return

      /**
       * set default interior point in case polygon has zero area
       */
      interiorPoint = Coordinate(polygon.getCoordinate()!!)

      val crossings = ArrayList<Double>()
      scanRing(polygon.getExteriorRing(), crossings)
      for (i in 0 until polygon.getNumInteriorRing()) {
        scanRing(polygon.getInteriorRingN(i), crossings)
      }
      findBestMidpoint(crossings)
    }

    private fun scanRing(ring: LinearRing, crossings: MutableList<Double>) {
      // skip rings which don't cross scan line
      if (!intersectsHorizontalLine(ring.getEnvelopeInternal(), interiorPointY))
        return

      val seq = ring.getCoordinateSequence()
      for (i in 1 until seq.size()) {
        val ptPrev = seq.getCoordinate(i - 1)
        val pt = seq.getCoordinate(i)
        addEdgeCrossing(ptPrev, pt, interiorPointY, crossings)
      }
    }

    private fun addEdgeCrossing(p0: Coordinate, p1: Coordinate, scanY: Double, crossings: MutableList<Double>) {
      // skip non-crossing segments
      if (!intersectsHorizontalLine(p0, p1, scanY))
        return
      if (!isEdgeCrossingCounted(p0, p1, scanY))
        return

      // edge intersects scan line, so add a crossing
      val xInt = intersection(p0, p1, scanY)
      crossings.add(xInt)
    }

    /**
     * Finds the midpoint of the widest interior section.
     *
     * @param crossings the list of scan-line crossing X ordinates
     */
    private fun findBestMidpoint(crossings: MutableList<Double>) {
      // zero-area polygons will have no crossings
      if (crossings.size == 0) return

      // TODO: is there a better way to verify the crossings are correct?
      Assert.isTrue(0 == crossings.size % 2, "Interior Point robustness failure: odd number of scanline crossings")

      crossings.sort()
      /*
       * Entries in crossings list are expected to occur in pairs representing a
       * section of the scan line interior to the polygon (which may be zero-length)
       */
      var i = 0
      while (i < crossings.size) {
        val x1 = crossings[i]
        // crossings count must be even so this should be safe
        val x2 = crossings[i + 1]

        val width = x2 - x1
        if (width > interiorSectionWidth) {
          interiorSectionWidth = width
          val interiorPointX = avg(x1, x2)
          interiorPoint = Coordinate(interiorPointX, interiorPointY)
        }
        i += 2
      }
    }

    companion object {
      /**
       * Tests if an edge intersection contributes to the crossing count.
       *
       * @return true if the edge crossing is counted
       */
      private fun isEdgeCrossingCounted(p0: Coordinate, p1: Coordinate, scanY: Double): Boolean {
        val y0 = p0.getY()
        val y1 = p1.getY()
        // skip horizontal lines
        if (y0 == y1)
          return false
        // handle cases where vertices lie on scan-line
        // downward segment does not include start point
        if (y0 == scanY && y1 < scanY)
          return false
        // upward segment does not include endpoint
        if (y1 == scanY && y0 < scanY)
          return false
        return true
      }

      /**
       * Computes the intersection of a segment with a horizontal line.
       *
       * @return the X ordinate of the intersection
       */
      private fun intersection(p0: Coordinate, p1: Coordinate, Y: Double): Double {
        val x0 = p0.getX()
        val x1 = p1.getX()

        if (x0 == x1)
          return x0

        // Assert: segDX is non-zero, due to previous equality test
        val segDX = x1 - x0
        val segDY = p1.getY() - p0.getY()
        val m = segDY / segDX
        val x = x0 + ((Y - p0.getY()) / m)
        return x
      }

      /**
       * Tests if an envelope intersects a horizontal line.
       *
       * @return true if the envelope and line intersect
       */
      private fun intersectsHorizontalLine(env: Envelope, y: Double): Boolean {
        if (y < env.getMinY())
          return false
        if (y > env.getMaxY())
          return false
        return true
      }

      /**
       * Tests if a line segment intersects a horizontal line.
       *
       * @return true if the segment and line intersect
       */
      private fun intersectsHorizontalLine(p0: Coordinate, p1: Coordinate, y: Double): Boolean {
        // both ends above?
        if (p0.getY() > y && p1.getY() > y)
          return false
        // both ends below?
        if (p0.getY() < y && p1.getY() < y)
          return false
        // segment must intersect line
        return true
      }
    }
  }

  /**
   * Finds a safe scan line Y ordinate by projecting
   * the polygon segments to the Y axis and finding the
   * Y-axis interval which contains the centre of the Y extent.
   *
   * @author mdavis
   *
   */
  private class ScanLineYOrdinateFinder(private val poly: Polygon) {

    private var centreY = 0.0
    private var hiY = Double.MAX_VALUE
    private var loY = -Double.MAX_VALUE

    init {
      // initialize using extremal values
      hiY = poly.getEnvelopeInternal().getMaxY()
      loY = poly.getEnvelopeInternal().getMinY()
      centreY = avg(loY, hiY)
    }

    fun getScanLineY(): Double {
      process(poly.getExteriorRing())
      for (i in 0 until poly.getNumInteriorRing()) {
        process(poly.getInteriorRingN(i))
      }
      val scanLineY = avg(hiY, loY)
      return scanLineY
    }

    private fun process(line: LineString) {
      val seq = line.getCoordinateSequence()
      for (i in 0 until seq.size()) {
        val y = seq.getY(i)
        updateInterval(y)
      }
    }

    private fun updateInterval(y: Double) {
      if (y <= centreY) {
        if (y > loY)
          loY = y
      } else if (y > centreY) {
        if (y < hiY) {
          hiY = y
        }
      }
    }

    companion object {
      @JvmStatic
      fun getScanLineY(poly: Polygon): Double {
        val finder = ScanLineYOrdinateFinder(poly)
        return finder.getScanLineY()
      }
    }
  }

  companion object {
    /**
     * Computes an interior point for the
     * polygonal components of a Geometry.
     *
     * @param geom the geometry to compute
     * @return the computed interior point,
     * or `null` if the geometry has no polygonal components
     */
    @JvmStatic
    fun getInteriorPoint(geom: Geometry): Coordinate? {
      val intPt = InteriorPointArea(geom)
      return intPt.getInteriorPoint()
    }

    private fun avg(a: Double, b: Double): Double {
      return (a + b) / 2.0
    }
  }
}
