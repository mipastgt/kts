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

import kotlin.jvm.JvmField

import org.locationtech.jts.util.TreeMap
import org.locationtech.jts.util.TreeSet
import org.locationtech.jts.algorithm.BoundaryNodeRule
import org.locationtech.jts.algorithm.LineIntersector
import org.locationtech.jts.algorithm.RobustLineIntersector
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryCollection
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.LinearRing
import org.locationtech.jts.geom.MultiLineString
import org.locationtech.jts.geom.MultiPoint
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.Polygonal
import org.locationtech.jts.geom.util.LinearComponentExtracter
import org.locationtech.jts.geomgraph.Edge
import org.locationtech.jts.geomgraph.GeometryGraph

/**
 * Tests whether a `Geometry` is simple.
 *
 * @see BoundaryNodeRule
 *
 *
 * @deprecated Replaced by org.locationtech.jts.operation.valid.IsSimpleOp
 */
@Deprecated("Replaced by org.locationtech.jts.operation.valid.IsSimpleOp")
class IsSimpleOp {
  private var inputGeom: Geometry? = null
  private var isClosedEndpointsInInterior = true
  private var nonSimpleLocation: Coordinate? = null

  /**
   * Creates a simplicity checker using the default SFS Mod-2 Boundary Node Rule
   *
   * @deprecated use IsSimpleOp(Geometry)
   */
  @Deprecated("use IsSimpleOp(Geometry)")
  constructor()

  /**
   * Creates a simplicity checker using the default SFS Mod-2 Boundary Node Rule
   *
   * @param geom the geometry to test
   */
  constructor(geom: Geometry) {
    this.inputGeom = geom
  }

  /**
   * Creates a simplicity checker using a given [BoundaryNodeRule]
   *
   * @param geom the geometry to test
   * @param boundaryNodeRule the rule to use.
   */
  constructor(geom: Geometry, boundaryNodeRule: BoundaryNodeRule) {
    this.inputGeom = geom
    isClosedEndpointsInInterior = !boundaryNodeRule.isInBoundary(2)
  }

  /**
   * Tests whether the geometry is simple.
   *
   * @return true if the geometry is simple
   */
  fun isSimple(): Boolean {
    nonSimpleLocation = null
    return computeSimple(inputGeom!!)
  }

  private fun computeSimple(geom: Geometry): Boolean {
    nonSimpleLocation = null
    if (geom.isEmpty()) return true
    if (geom is LineString) return isSimpleLinearGeometry(geom)
    if (geom is MultiLineString) return isSimpleLinearGeometry(geom)
    if (geom is MultiPoint) return isSimpleMultiPoint(geom)
    if (geom is Polygonal) return isSimplePolygonal(geom)
    if (geom is GeometryCollection) return isSimpleGeometryCollection(geom)
    // all other geometry types are simple by definition
    return true
  }

  /**
   * Gets a coordinate for the location where the geometry
   * fails to be simple.
   *
   * @return a coordinate for the location of the non-boundary self-intersection
   * or null if the geometry is simple
   */
  fun getNonSimpleLocation(): Coordinate? {
    return nonSimpleLocation
  }

  /**
   * Reports whether a [LineString] is simple.
   *
   * @param geom the lineal geometry to test
   * @return true if the geometry is simple
   * @deprecated use isSimple()
   */
  @Deprecated("use isSimple()")
  fun isSimple(geom: LineString): Boolean {
    return isSimpleLinearGeometry(geom)
  }

  /**
   * Reports whether a [MultiLineString] geometry is simple.
   *
   * @param geom the lineal geometry to test
   * @return true if the geometry is simple
   * @deprecated use isSimple()
   */
  @Deprecated("use isSimple()")
  fun isSimple(geom: MultiLineString): Boolean {
    return isSimpleLinearGeometry(geom)
  }

  /**
   * A MultiPoint is simple if it has no repeated points
   * @deprecated use isSimple()
   */
  @Deprecated("use isSimple()")
  fun isSimple(mp: MultiPoint): Boolean {
    return isSimpleMultiPoint(mp)
  }

  private fun isSimpleMultiPoint(mp: MultiPoint): Boolean {
    if (mp.isEmpty()) return true
    val points = TreeSet<Coordinate>()
    for (i in 0 until mp.getNumGeometries()) {
      val pt = mp.getGeometryN(i) as Point
      val p = pt.getCoordinate()!!
      if (points.contains(p)) {
        nonSimpleLocation = p
        return false
      }
      points.add(p)
    }
    return true
  }

  /**
   * Computes simplicity for polygonal geometries.
   * Polygonal geometries are simple if and only if
   * all of their component rings are simple.
   *
   * @param geom a Polygonal geometry
   * @return true if the geometry is simple
   */
  private fun isSimplePolygonal(geom: Geometry): Boolean {
    val rings = LinearComponentExtracter.getLines(geom)
    for (line in rings) {
      val ring = line as LinearRing
      if (!isSimpleLinearGeometry(ring)) return false
    }
    return true
  }

  /**
   * Semantics for GeometryCollection is
   * simple if all components are simple.
   *
   * @param geom
   * @return true if the geometry is simple
   */
  private fun isSimpleGeometryCollection(geom: Geometry): Boolean {
    for (i in 0 until geom.getNumGeometries()) {
      val comp = geom.getGeometryN(i)
      if (!computeSimple(comp)) return false
    }
    return true
  }

  private fun isSimpleLinearGeometry(geom: Geometry): Boolean {
    if (geom.isEmpty()) return true
    val graph = GeometryGraph(0, geom)
    val li: LineIntersector = RobustLineIntersector()
    val si = graph.computeSelfNodes(li, true)
    // if no self-intersection, must be simple
    if (!si.hasIntersection()) return true
    if (si.hasProperIntersection()) {
      nonSimpleLocation = si.getProperIntersectionPoint()
      return false
    }
    if (hasNonEndpointIntersection(graph)) return false
    if (isClosedEndpointsInInterior) {
      if (hasClosedEndpointIntersection(graph)) return false
    }
    return true
  }

  /**
   * For all edges, check if there are any intersections which are NOT at an endpoint.
   * The Geometry is not simple if there are intersections not at endpoints.
   */
  private fun hasNonEndpointIntersection(graph: GeometryGraph): Boolean {
    val i = graph.getEdgeIterator()
    while (i.hasNext()) {
      val e = i.next() as Edge
      val maxSegmentIndex = e.getMaximumSegmentIndex()
      val eiIt = e.getEdgeIntersectionList().iterator()
      while (eiIt.hasNext()) {
        val ei = eiIt.next()
        if (!ei.isEndPoint(maxSegmentIndex)) {
          nonSimpleLocation = ei.getCoordinate()
          return true
        }
      }
    }
    return false
  }

  private class EndpointInfo(pt: Coordinate) {
    @JvmField
    val pt: Coordinate = pt
    @JvmField
    var isClosed: Boolean = false
    @JvmField
    var degree: Int = 0

    fun getCoordinate(): Coordinate {
      return pt
    }

    fun addEndpoint(isClosed: Boolean) {
      degree++
      this.isClosed = this.isClosed or isClosed
    }
  }

  /**
   * Tests that no edge intersection is the endpoint of a closed line.
   */
  private fun hasClosedEndpointIntersection(graph: GeometryGraph): Boolean {
    val endPoints = TreeMap<Coordinate, EndpointInfo>()
    val i = graph.getEdgeIterator()
    while (i.hasNext()) {
      val e = i.next() as Edge
      val isClosed = e.isClosed()
      val p0 = e.getCoordinate(0)
      addEndpoint(endPoints, p0, isClosed)
      val p1 = e.getCoordinate(e.getNumPoints() - 1)
      addEndpoint(endPoints, p1, isClosed)
    }

    for (eiInfo in endPoints.values) {
      if (eiInfo.isClosed && eiInfo.degree != 2) {
        nonSimpleLocation = eiInfo.getCoordinate()
        return true
      }
    }
    return false
  }

  /**
   * Add an endpoint to the map, creating an entry for it if none exists
   */
  private fun addEndpoint(endPoints: MutableMap<Coordinate, EndpointInfo>, p: Coordinate, isClosed: Boolean) {
    var eiInfo = endPoints[p]
    if (eiInfo == null) {
      eiInfo = EndpointInfo(p)
      endPoints[p] = eiInfo
    }
    eiInfo.addEndpoint(isClosed)
  }
}
