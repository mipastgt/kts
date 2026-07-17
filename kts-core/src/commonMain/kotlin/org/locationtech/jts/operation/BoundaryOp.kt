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
package org.locationtech.jts.operation

import kotlin.jvm.JvmStatic
import kotlin.jvm.JvmField

import org.locationtech.jts.util.TreeMap
import org.locationtech.jts.algorithm.BoundaryNodeRule
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.CoordinateArrays
import org.locationtech.jts.geom.Dimension
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.MultiLineString
import org.locationtech.jts.geom.MultiPoint
import org.locationtech.jts.geom.Point

/**
 * Computes the boundary of a [Geometry].
 * Allows specifying the [BoundaryNodeRule] to be used.
 * This operation will always return a [Geometry] of the appropriate
 * dimension for the boundary (even if the input geometry is empty).
 * The boundary of zero-dimensional geometries (Points) is
 * always the empty [GeometryCollection][org.locationtech.jts.geom.GeometryCollection].
 *
 * @author Martin Davis
 */
class BoundaryOp(private val geom: Geometry, private val bnRule: BoundaryNodeRule) {
  private val geomFact: GeometryFactory = geom.getFactory()

  private lateinit var endpointMap: MutableMap<Coordinate, Counter>

  /**
   * Creates a new instance for the given geometry.
   *
   * @param geom the input geometry
   */
  constructor(geom: Geometry) : this(geom, BoundaryNodeRule.MOD2_BOUNDARY_RULE)

  /**
   * Gets the computed boundary.
   *
   * @return the boundary geometry
   */
  fun getBoundary(): Geometry {
    if (geom is LineString) return boundaryLineString(geom)
    if (geom is MultiLineString) return boundaryMultiLineString(geom)
    return geom.getBoundary()
  }

  private fun getEmptyMultiPoint(): MultiPoint {
    return geomFact.createMultiPoint()
  }

  private fun boundaryMultiLineString(mLine: MultiLineString): Geometry {
    if (geom.isEmpty()) {
      return getEmptyMultiPoint()
    }

    val bdyPts = computeBoundaryCoordinates(mLine)

    // return Point or MultiPoint
    if (bdyPts.size == 1) {
      return geomFact.createPoint(bdyPts[0])
    }
    // this handles 0 points case as well
    return geomFact.createMultiPointFromCoords(bdyPts)
  }

  private fun computeBoundaryCoordinates(mLine: MultiLineString): Array<Coordinate> {
    val bdyPts = ArrayList<Coordinate>()
    endpointMap = TreeMap()
    for (i in 0 until mLine.getNumGeometries()) {
      val line = mLine.getGeometryN(i) as LineString
      if (line.getNumPoints() == 0) continue
      addEndpoint(line.getCoordinateN(0))
      addEndpoint(line.getCoordinateN(line.getNumPoints() - 1))
    }

    for (entry in endpointMap.entries) {
      val counter = entry.value
      val valence = counter.count
      if (bnRule.isInBoundary(valence)) {
        bdyPts.add(entry.key)
      }
    }

    return CoordinateArrays.toCoordinateArray(bdyPts)
  }

  private fun addEndpoint(pt: Coordinate) {
    var counter = endpointMap[pt]
    if (counter == null) {
      counter = Counter()
      endpointMap[pt] = counter
    }
    counter.count++
  }

  private fun boundaryLineString(line: LineString): Geometry {
    if (geom.isEmpty()) {
      return getEmptyMultiPoint()
    }

    if (line.isClosed()) {
      // check whether endpoints of valence 2 are on the boundary or not
      val closedEndpointOnBoundary = bnRule.isInBoundary(2)
      if (closedEndpointOnBoundary) {
        return line.getStartPoint()!!
      } else {
        return geomFact.createMultiPoint()
      }
    }
    return geomFact.createMultiPoint(
      arrayOf(
        line.getStartPoint()!!,
        line.getEndPoint()!!
      )
    )
  }

  companion object {
    /**
     * Computes a geometry representing the boundary of a geometry.
     *
     * @param g the input geometry
     * @return the computed boundary
     */
    @JvmStatic
    fun getBoundary(g: Geometry): Geometry {
      val bop = BoundaryOp(g)
      return bop.getBoundary()
    }

    /**
     * Computes a geometry representing the boundary of a geometry,
     * using an explicit [BoundaryNodeRule].
     *
     * @param g the input geometry
     * @param bnRule the Boundary Node Rule to use
     * @return the computed boundary
     */
    @JvmStatic
    fun getBoundary(g: Geometry, bnRule: BoundaryNodeRule): Geometry {
      val bop = BoundaryOp(g, bnRule)
      return bop.getBoundary()
    }

    /**
     * Tests if a geometry has a boundary (it is non-empty).
     *
     * @param geom the geometry providing the boundary
     * @param boundaryNodeRule the Boundary Node Rule to use
     * @return true if the boundary exists
     */
    @JvmStatic
    fun hasBoundary(geom: Geometry, boundaryNodeRule: BoundaryNodeRule): Boolean {
      // Note that this does not handle geometry collections with a non-empty linear element
      if (geom.isEmpty()) return false
      when (geom.getDimension()) {
        Dimension.P -> return false
        /**
         * Linear geometries might have an empty boundary due to boundary node rule.
         */
        Dimension.L -> {
          val boundary = getBoundary(geom, boundaryNodeRule)
          return !boundary.isEmpty()
        }
        Dimension.A -> return true
      }
      return true
    }
  }
}

/**
 * Stores an integer count, for use as a Map entry.
 *
 * @author Martin Davis
 */
internal class Counter {
  /**
   * The value of the count
   */
  @JvmField
  var count = 0
}
