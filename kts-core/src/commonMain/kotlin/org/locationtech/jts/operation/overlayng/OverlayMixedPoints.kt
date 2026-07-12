/*
 * Copyright (c) 2020 Martin Davis.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.locationtech.jts.operation.overlayng

import kotlin.jvm.JvmStatic

import org.locationtech.jts.algorithm.locate.IndexedPointInAreaLocator
import org.locationtech.jts.algorithm.locate.PointOnGeometryLocator
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.CoordinateFilter
import org.locationtech.jts.geom.CoordinateList
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.Location
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.Polygon
import org.locationtech.jts.geom.PrecisionModel
import org.locationtech.jts.util.Assert

/**
 * Computes an overlay where one input is Point(s) and one is not.
 * This class supports overlay being used as an efficient way
 * to find points within or outside a polygon.
 *
 * @author Martin Davis
 */
class OverlayMixedPoints(
  private val opCode: Int,
  geom0: Geometry,
  geom1: Geometry,
  private val pm: PrecisionModel?
) {

  private val geometryFactory: GeometryFactory = geom0.getFactory()
  private val geomPoint: Geometry
  private val geomNonPointInput: Geometry
  private val isPointRHS: Boolean
  private val resultDim: Int = OverlayUtil.resultDimension(opCode, geom0.getDimension(), geom1.getDimension())

  private lateinit var geomNonPoint: Geometry
  private var geomNonPointDim = 0
  private lateinit var locator: PointOnGeometryLocator

  init {
    // name the dimensional geometries
    if (geom0.getDimension() == 0) {
      this.geomPoint = geom0
      this.geomNonPointInput = geom1
      this.isPointRHS = false
    } else {
      this.geomPoint = geom1
      this.geomNonPointInput = geom0
      this.isPointRHS = true
    }
  }

  fun getResult(): Geometry {
    // reduce precision of non-point input, if required
    geomNonPoint = prepareNonPoint(geomNonPointInput)
    geomNonPointDim = geomNonPoint.getDimension()
    locator = createLocator(geomNonPoint)

    val coords = extractCoordinates(geomPoint, pm)

    return when (opCode) {
      OverlayNG.INTERSECTION -> computeIntersection(coords)
      // UNION and SYMDIFFERENCE have same output
      OverlayNG.UNION, OverlayNG.SYMDIFFERENCE -> computeUnion(coords)
      OverlayNG.DIFFERENCE -> computeDifference(coords)
      else -> {
        Assert.shouldNeverReachHere("Unknown overlay op code")
        throw IllegalStateException()
      }
    }
  }

  private fun createLocator(geomNonPoint: Geometry): PointOnGeometryLocator {
    return if (geomNonPointDim == 2) {
      IndexedPointInAreaLocator(geomNonPoint)
    } else {
      IndexedPointOnLineLocator(geomNonPoint)
    }
  }

  private fun prepareNonPoint(geomInput: Geometry): Geometry {
    // if non-point not in output no need to node it
    if (resultDim == 0) {
      return geomInput
    }

    // Node and round the non-point geometry for output
    val geomPrep = OverlayNG.union(geomNonPointInput, pm)
    return geomPrep
  }

  private fun computeIntersection(coords: Array<Coordinate>): Geometry {
    return createPointResult(findPoints(true, coords))
  }

  private fun computeUnion(coords: Array<Coordinate>): Geometry {
    val resultPointList = findPoints(false, coords)
    var resultLineList: List<LineString>? = null
    if (geomNonPointDim == 1) {
      resultLineList = extractLines(geomNonPoint)
    }
    var resultPolyList: List<Polygon>? = null
    if (geomNonPointDim == 2) {
      resultPolyList = extractPolygons(geomNonPoint)
    }

    return OverlayUtil.createResultGeometry(resultPolyList, resultLineList, resultPointList, geometryFactory)
  }

  private fun computeDifference(coords: Array<Coordinate>): Geometry {
    if (isPointRHS) {
      return copyNonPoint()
    }
    return createPointResult(findPoints(false, coords))
  }

  private fun createPointResult(points: List<Point>): Geometry {
    if (points.size == 0) {
      return geometryFactory.createEmpty(0)
    } else if (points.size == 1) {
      return points[0]
    }
    val pointsArray = GeometryFactory.toPointArray(points)
    return geometryFactory.createMultiPoint(pointsArray)
  }

  private fun findPoints(isCovered: Boolean, coords: Array<Coordinate>): List<Point> {
    val resultCoords = HashSet<Coordinate>()
    // keep only points contained
    for (coord in coords) {
      if (hasLocation(isCovered, coord)) {
        // copy coordinate to avoid aliasing
        resultCoords.add(coord.copy())
      }
    }
    return createPoints(resultCoords)
  }

  private fun createPoints(coords: Set<Coordinate>): List<Point> {
    val points = ArrayList<Point>()
    for (coord in coords) {
      val point = geometryFactory.createPoint(coord)
      points.add(point)
    }
    return points
  }

  private fun hasLocation(isCovered: Boolean, coord: Coordinate): Boolean {
    val isExterior = Location.EXTERIOR == locator.locate(coord)
    if (isCovered) {
      return !isExterior
    }
    return isExterior
  }

  /**
   * Copy the non-point input geometry if not
   * already done by precision reduction process.
   *
   * @return a copy of the non-point geometry
   */
  private fun copyNonPoint(): Geometry {
    if (geomNonPointInput !== geomNonPoint)
      return geomNonPoint
    return geomNonPoint.copy()
  }

  companion object {
    @JvmStatic
    fun overlay(opCode: Int, geom0: Geometry, geom1: Geometry, pm: PrecisionModel?): Geometry {
      val overlay = OverlayMixedPoints(opCode, geom0, geom1, pm)
      return overlay.getResult()
    }

    private fun extractCoordinates(points: Geometry, pm: PrecisionModel?): Array<Coordinate> {
      val coords = CoordinateList()
      points.apply(object : CoordinateFilter {
        override fun filter(coord: Coordinate) {
          val p = OverlayUtil.round(coord, pm)
          coords.add(p, false)
        }
      })
      return coords.toCoordinateArray()
    }

    private fun extractPolygons(geom: Geometry): List<Polygon> {
      val list = ArrayList<Polygon>()
      for (i in 0 until geom.getNumGeometries()) {
        val poly = geom.getGeometryN(i) as Polygon
        if (!poly.isEmpty()) {
          list.add(poly)
        }
      }
      return list
    }

    private fun extractLines(geom: Geometry): List<LineString> {
      val list = ArrayList<LineString>()
      for (i in 0 until geom.getNumGeometries()) {
        val line = geom.getGeometryN(i) as LineString
        if (!line.isEmpty()) {
          list.add(line)
        }
      }
      return list
    }
  }
}
