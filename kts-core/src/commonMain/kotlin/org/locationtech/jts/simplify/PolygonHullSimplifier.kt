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
package org.locationtech.jts.simplify

import kotlin.jvm.JvmStatic
import kotlin.math.abs
import kotlin.math.ceil

import org.locationtech.jts.algorithm.Area
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LinearRing
import org.locationtech.jts.geom.MultiPolygon
import org.locationtech.jts.geom.Polygon
import org.locationtech.jts.geom.Polygonal
import org.locationtech.jts.math.MathUtil

/**
 * Computes topology-preserving simplified hulls of polygonal geometry.
 *
 * @author Martin Davis
 */
class PolygonHullSimplifier(private val inputGeom: Geometry, private val isOuter: Boolean) {

  private var vertexNumFraction = -1.0
  private var areaDeltaRatio = -1.0
  private val geomFactory: GeometryFactory = inputGeom.getFactory()

  init {
    if (inputGeom !is Polygonal) {
      throw IllegalArgumentException("Input geometry must be  polygonal")
    }
  }

  /**
   * Sets the target fraction of input vertices
   * which are retained in the result.
   * The value should be in the range [0,1].
   */
  fun setVertexNumFraction(vertexNumFraction: Double) {
    val frac = MathUtil.clamp(vertexNumFraction, 0.0, 1.0)
    this.vertexNumFraction = frac
  }

  /**
   * Sets the target maximum ratio of the change in area of the result to the input area.
   */
  fun setAreaDeltaRatio(areaDeltaRatio: Double) {
    this.areaDeltaRatio = areaDeltaRatio
  }

  /**
   * Gets the result polygonal hull geometry.
   *
   * @return the polygonal geometry for the hull
   */
  fun getResult(): Geometry {
    //-- handle trivial parameter values
    if (vertexNumFraction == 1.0 || areaDeltaRatio == 0.0) {
      return inputGeom.copy()
    }

    if (inputGeom is MultiPolygon) {
      /**
       * Only outer hulls where there is more than one polygon
       * can potentially overlap.
       */
      val isOverlapPossible = isOuter && inputGeom.getNumGeometries() > 1
      if (isOverlapPossible) {
        return computeMultiPolygonAll(inputGeom)
      } else {
        return computeMultiPolygonEach(inputGeom)
      }
    } else if (inputGeom is Polygon) {
      return computePolygon(inputGeom)
    }
    throw IllegalArgumentException("Input geometry must be polygonal")
  }

  /**
   * Computes hulls for MultiPolygon elements for
   * the cases where hulls might overlap.
   */
  private fun computeMultiPolygonAll(multiPoly: MultiPolygon): Geometry {
    val hullIndex = RingHullIndex()
    val nPoly = multiPoly.getNumGeometries()
    val polyHulls = arrayOfNulls<MutableList<RingHull>>(nPoly)

    //-- prepare element polygon hulls and index
    for (i in 0 until multiPoly.getNumGeometries()) {
      val poly = multiPoly.getGeometryN(i) as Polygon
      val ringHulls = initPolygon(poly, hullIndex)
      polyHulls[i] = ringHulls
    }

    //-- compute hull polygons
    val polys: MutableList<Polygon> = ArrayList()
    for (i in 0 until multiPoly.getNumGeometries()) {
      val poly = multiPoly.getGeometryN(i) as Polygon
      val hull = polygonHull(poly, polyHulls[i]!!, hullIndex)
      polys.add(hull)
    }
    return geomFactory.createMultiPolygon(GeometryFactory.toPolygonArray(polys))
  }

  private fun computeMultiPolygonEach(multiPoly: MultiPolygon): Geometry {
    val polys: MutableList<Polygon> = ArrayList()
    for (i in 0 until multiPoly.getNumGeometries()) {
      val poly = multiPoly.getGeometryN(i) as Polygon
      val hull = computePolygon(poly)
      polys.add(hull)
    }
    return geomFactory.createMultiPolygon(GeometryFactory.toPolygonArray(polys))
  }

  private fun computePolygon(poly: Polygon): Polygon {
    var hullIndex: RingHullIndex? = null
    /**
     * For a single polygon overlaps are only possible for inner hulls
     * and where holes are present.
     */
    val isOverlapPossible = !isOuter && poly.getNumInteriorRing() > 0
    if (isOverlapPossible) hullIndex = RingHullIndex()
    val hulls = initPolygon(poly, hullIndex)
    val hull = polygonHull(poly, hulls, hullIndex)
    return hull
  }

  /**
   * Create all ring hulls for the rings of a polygon,
   * so that all are in the hull index if required.
   *
   * @param poly the polygon being processed
   * @param hullIndex the hull index if present, or null
   * @return the list of ring hulls
   */
  private fun initPolygon(poly: Polygon, hullIndex: RingHullIndex?): MutableList<RingHull> {
    val hulls: MutableList<RingHull> = ArrayList()
    if (poly.isEmpty())
      return hulls

    var areaTotal = 0.0
    if (areaDeltaRatio >= 0) {
      areaTotal = ringArea(poly)
    }
    hulls.add(createRingHull(poly.getExteriorRing(), isOuter, areaTotal, hullIndex))
    for (i in 0 until poly.getNumInteriorRing()) {
      //Assert: interior ring is not empty
      hulls.add(createRingHull(poly.getInteriorRingN(i), !isOuter, areaTotal, hullIndex))
    }
    return hulls
  }

  private fun ringArea(poly: Polygon): Double {
    var area = Area.ofRing(poly.getExteriorRing().getCoordinateSequence())
    for (i in 0 until poly.getNumInteriorRing()) {
      area += Area.ofRing(poly.getInteriorRingN(i).getCoordinateSequence())
    }
    return area
  }

  private fun createRingHull(ring: LinearRing, isOuter: Boolean, areaTotal: Double, hullIndex: RingHullIndex?): RingHull {
    val ringHull = RingHull(ring, isOuter)
    if (vertexNumFraction >= 0) {
      val targetVertexCount = ceil(vertexNumFraction * (ring.getNumPoints() - 1)).toInt()
      ringHull.setMinVertexNum(targetVertexCount)
    } else if (areaDeltaRatio >= 0) {
      val ringArea = Area.ofRing(ring.getCoordinateSequence())
      val ringWeight = ringArea / areaTotal
      val maxAreaDelta = ringWeight * areaDeltaRatio * ringArea
      ringHull.setMaxAreaDelta(maxAreaDelta)
    }
    if (hullIndex != null) hullIndex.add(ringHull)
    return ringHull
  }

  private fun polygonHull(poly: Polygon, ringHulls: List<RingHull>, hullIndex: RingHullIndex?): Polygon {
    if (poly.isEmpty())
      return geomFactory.createPolygon()

    var ringIndex = 0
    val shellHull = ringHulls.get(ringIndex++).getHull(hullIndex)
    val holeHulls: MutableList<LinearRing> = ArrayList()
    for (i in 0 until poly.getNumInteriorRing()) {
      val hull = ringHulls.get(ringIndex++).getHull(hullIndex)
      //TODO: handle empty
      holeHulls.add(hull)
    }
    val resultHoles = GeometryFactory.toLinearRingArray(holeHulls)
    return geomFactory.createPolygon(shellHull, resultHoles)
  }

  companion object {
    /**
     * Computes a topology-preserving simplified hull of a polygonal geometry,
     * with hull shape determined by a target parameter
     * specifying the fraction of the input vertices retained in the result.
     *
     * @param geom the polygonal geometry to process
     * @param isOuter indicates whether to compute an outer or inner hull
     * @param vertexNumFraction the target fraction of number of input vertices in result
     * @return the hull geometry
     */
    @JvmStatic
    fun hull(geom: Geometry, isOuter: Boolean, vertexNumFraction: Double): Geometry {
      val hull = PolygonHullSimplifier(geom, isOuter)
      hull.setVertexNumFraction(abs(vertexNumFraction))
      return hull.getResult()
    }

    /**
     * Computes a topology-preserving simplified hull of a polygonal geometry,
     * with hull shape determined by a target parameter
     * specifying the ratio of maximum difference in area to original area.
     *
     * @param geom the polygonal geometry to process
     * @param isOuter indicates whether to compute an outer or inner hull
     * @param areaDeltaRatio the target ratio of area difference to original area
     * @return the hull geometry
     */
    @JvmStatic
    fun hullByAreaDelta(geom: Geometry, isOuter: Boolean, areaDeltaRatio: Double): Geometry {
      val hull = PolygonHullSimplifier(geom, isOuter)
      hull.setAreaDeltaRatio(abs(areaDeltaRatio))
      return hull.getResult()
    }
  }
}
