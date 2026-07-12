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
import kotlin.math.abs

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryCollection
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.Polygon

/**
 * Computes the centroid of a [Geometry] of any dimension.
 * For collections the centroid is computed for the collection of
 * non-empty elements of highest dimension.
 * The centroid of an empty geometry is {@code null}.
 *
 * @see InteriorPoint
 *
 * @version 1.7
 */
class Centroid(geom: Geometry) {

  private var areaBasePt: Coordinate? = null // the point all triangles are based at
  private val triangleCent3 = Coordinate() // temporary variable to hold centroid of triangle
  private var areasum2 = 0.0 /* Partial area sum */
  private val cg3 = Coordinate() // partial centroid sum

  // data for linear centroid computation, if needed
  private val lineCentSum = Coordinate()
  private var totalLength = 0.0

  private var ptCount = 0
  private val ptCentSum = Coordinate()

  init {
    areaBasePt = null
    add(geom)
  }

  /**
   * Adds a Geometry to the centroid total.
   *
   * @param geom the geometry to add
   */
  private fun add(geom: Geometry) {
    if (geom.isEmpty())
      return
    if (geom is Point) {
      addPoint(geom.getCoordinate()!!)
    } else if (geom is LineString) {
      addLineSegments(geom.getCoordinates())
    } else if (geom is Polygon) {
      add(geom)
    } else if (geom is GeometryCollection) {
      for (i in 0 until geom.getNumGeometries()) {
        add(geom.getGeometryN(i))
      }
    }
  }

  /**
   * Gets the computed centroid.
   *
   * @return the computed centroid, or null if the input is empty
   */
  fun getCentroid(): Coordinate? {
    /*
     * The centroid is computed from the highest dimension components present in the input.
     */
    val cent = Coordinate()
    if (abs(areasum2) > 0.0) {
      /*
       * Input contains areal geometry
       */
      cent.x = cg3.x / 3 / areasum2
      cent.y = cg3.y / 3 / areasum2
    } else if (totalLength > 0.0) {
      /*
       * Input contains lineal geometry
       */
      cent.x = lineCentSum.x / totalLength
      cent.y = lineCentSum.y / totalLength
    } else if (ptCount > 0) {
      /*
       * Input contains puntal geometry only
       */
      cent.x = ptCentSum.x / ptCount
      cent.y = ptCentSum.y / ptCount
    } else {
      return null
    }
    return cent
  }

  private fun setAreaBasePoint(basePt: Coordinate) {
    this.areaBasePt = basePt
  }

  private fun add(poly: Polygon) {
    addShell(poly.getExteriorRing().getCoordinates())
    for (i in 0 until poly.getNumInteriorRing()) {
      addHole(poly.getInteriorRingN(i).getCoordinates())
    }
  }

  private fun addShell(pts: Array<Coordinate>) {
    if (pts.isNotEmpty())
      setAreaBasePoint(pts[0])
    val isPositiveArea = !Orientation.isCCW(pts)
    for (i in 0 until pts.size - 1) {
      addTriangle(areaBasePt!!, pts[i], pts[i + 1], isPositiveArea)
    }
    addLineSegments(pts)
  }

  private fun addHole(pts: Array<Coordinate>) {
    val isPositiveArea = Orientation.isCCW(pts)
    for (i in 0 until pts.size - 1) {
      addTriangle(areaBasePt!!, pts[i], pts[i + 1], isPositiveArea)
    }
    addLineSegments(pts)
  }

  private fun addTriangle(p0: Coordinate, p1: Coordinate, p2: Coordinate, isPositiveArea: Boolean) {
    val sign = if (isPositiveArea) 1.0 else -1.0
    centroid3(p0, p1, p2, triangleCent3)
    val area2 = area2(p0, p1, p2)
    cg3.x += sign * area2 * triangleCent3.x
    cg3.y += sign * area2 * triangleCent3.y
    areasum2 += sign * area2
  }

  /**
   * Adds the line segments defined by an array of coordinates
   * to the linear centroid accumulators.
   *
   * @param pts an array of [Coordinate]s
   */
  private fun addLineSegments(pts: Array<Coordinate>) {
    var lineLen = 0.0
    for (i in 0 until pts.size - 1) {
      val segmentLen = pts[i].distance(pts[i + 1])
      if (segmentLen == 0.0)
        continue

      lineLen += segmentLen

      val midx = (pts[i].x + pts[i + 1].x) / 2
      lineCentSum.x += segmentLen * midx
      val midy = (pts[i].y + pts[i + 1].y) / 2
      lineCentSum.y += segmentLen * midy
    }
    totalLength += lineLen
    if (lineLen == 0.0 && pts.isNotEmpty())
      addPoint(pts[0])
  }

  /**
   * Adds a point to the point centroid accumulator.
   * @param pt a [Coordinate]
   */
  private fun addPoint(pt: Coordinate) {
    ptCount += 1
    ptCentSum.x += pt.x
    ptCentSum.y += pt.y
  }

  companion object {
    /**
     * Computes the centroid point of a geometry.
     *
     * @param geom the geometry to use
     * @return the centroid point, or null if the geometry is empty
     */
    @JvmStatic
    fun getCentroid(geom: Geometry): Coordinate? {
      val cent = Centroid(geom)
      return cent.getCentroid()
    }

    /**
     * Computes three times the centroid of the triangle p1-p2-p3.
     * The factor of 3 is
     * left in to permit division to be avoided until later.
     */
    private fun centroid3(p1: Coordinate, p2: Coordinate, p3: Coordinate, c: Coordinate) {
      c.x = p1.x + p2.x + p3.x
      c.y = p1.y + p2.y + p3.y
    }

    /**
     * Returns twice the signed area of the triangle p1-p2-p3.
     * The area is positive if the triangle is oriented CCW, and negative if CW.
     */
    private fun area2(p1: Coordinate, p2: Coordinate, p3: Coordinate): Double {
      return (p2.x - p1.x) * (p3.y - p1.y) -
          (p3.x - p1.x) * (p2.y - p1.y)
    }
  }
}
