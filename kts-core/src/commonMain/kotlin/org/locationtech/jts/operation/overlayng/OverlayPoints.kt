/*
 * Copyright (c) 2019 Martin Davis.
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

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.CoordinateSequence
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryComponentFilter
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.PrecisionModel

/**
 * Performs an overlay operation on inputs which are both point geometries.
 *
 * @author Martin Davis
 */
class OverlayPoints(
  private val opCode: Int,
  private val geom0: Geometry,
  private val geom1: Geometry,
  private val pm: PrecisionModel?
) {

  private val geometryFactory: GeometryFactory = geom0.getFactory()
  private var resultList: ArrayList<Point>? = null

  /**
   * Gets the result of the overlay.
   *
   * @return the overlay result
   */
  fun getResult(): Geometry {
    val map0 = buildPointMap(geom0)
    val map1 = buildPointMap(geom1)

    val resultList = ArrayList<Point>()
    this.resultList = resultList
    when (opCode) {
      OverlayNG.INTERSECTION -> computeIntersection(map0, map1, resultList)
      OverlayNG.UNION -> computeUnion(map0, map1, resultList)
      OverlayNG.DIFFERENCE -> computeDifference(map0, map1, resultList)
      OverlayNG.SYMDIFFERENCE -> {
        computeDifference(map0, map1, resultList)
        computeDifference(map1, map0, resultList)
      }
    }
    if (resultList.isEmpty())
      return OverlayUtil.createEmptyResult(0, geometryFactory)

    return geometryFactory.buildGeometry(resultList)
  }

  private fun computeIntersection(map0: Map<Coordinate, Point>, map1: Map<Coordinate, Point>, resultList: ArrayList<Point>) {
    for (entry in map0.entries) {
      if (map1.containsKey(entry.key)) {
        resultList.add(copyPoint(entry.value))
      }
    }
  }

  private fun computeDifference(map0: Map<Coordinate, Point>, map1: Map<Coordinate, Point>, resultList: ArrayList<Point>) {
    for (entry in map0.entries) {
      if (!map1.containsKey(entry.key)) {
        resultList.add(copyPoint(entry.value))
      }
    }
  }

  private fun computeUnion(map0: Map<Coordinate, Point>, map1: Map<Coordinate, Point>, resultList: ArrayList<Point>) {
    // copy all A points
    for (p in map0.values) {
      resultList.add(copyPoint(p))
    }

    for (entry in map1.entries) {
      if (!map0.containsKey(entry.key)) {
        resultList.add(copyPoint(entry.value))
      }
    }
  }

  private fun copyPoint(pt: Point): Point {
    // if pm is floating, the point coordinate is not changed
    if (OverlayUtil.isFloating(pm))
      return pt.copy() as Point

    // pm is fixed.  Round off X&Y ordinates, copy other ordinates unchanged
    val seq = pt.getCoordinateSequence()
    val seq2 = seq.copy()
    seq2.setOrdinate(0, CoordinateSequence.X, pm!!.makePrecise(seq.getX(0)))
    seq2.setOrdinate(0, CoordinateSequence.Y, pm.makePrecise(seq.getY(0)))
    return geometryFactory.createPoint(seq2)
  }

  private fun buildPointMap(geoms: Geometry): HashMap<Coordinate, Point> {
    val map = HashMap<Coordinate, Point>()
    geoms.apply(object : GeometryComponentFilter {

      override fun filter(geom: Geometry) {
        if (geom !is Point)
          return
        if (geom.isEmpty())
          return

        val pt = geom
        val p = roundCoord(pt, pm)
        /*
         * Only add first occurrence of a point.
         * This provides the merging semantics of overlay
         */
        if (!map.containsKey(p))
          map[p] = pt
      }
    })

    return map
  }

  companion object {
    /**
     * Performs an overlay operation on inputs which are both point geometries.
     *
     * @param opCode the code for the desired overlay operation
     * @param geom0 the first geometry argument
     * @param geom1 the second geometry argument
     * @param pm the precision model to use
     * @return the result of the overlay operation
     */
    @JvmStatic
    fun overlay(opCode: Int, geom0: Geometry, geom1: Geometry, pm: PrecisionModel?): Geometry {
      val overlay = OverlayPoints(opCode, geom0, geom1, pm)
      return overlay.getResult()
    }

    /**
     * Round the key point if precision model is fixed.
     * Note: return value is only copied if rounding is performed.
     */
    fun roundCoord(pt: Point, pm: PrecisionModel?): Coordinate {
      val p = pt.getCoordinate()!!
      if (OverlayUtil.isFloating(pm))
        return p
      val p2 = p.copy()
      pm!!.makePrecise(p2)
      return p2
    }
  }
}
