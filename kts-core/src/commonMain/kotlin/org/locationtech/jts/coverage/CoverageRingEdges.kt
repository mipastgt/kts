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

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.CoordinateArrays
import org.locationtech.jts.geom.CoordinateList
import org.locationtech.jts.geom.CoordinateSequence
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LineSegment
import org.locationtech.jts.geom.LinearRing
import org.locationtech.jts.geom.MultiPolygon
import org.locationtech.jts.geom.Polygon

/**
 * Models a polygonal coverage as a set of unique [CoverageEdge]s,
 * linked to the parent rings in the coverage polygons.
 *
 * @author Martin Davis
 */
class CoverageRingEdges(private val coverage: Array<Geometry>) {

  private val ringEdgesMap: MutableMap<LinearRing, List<CoverageEdge>> = HashMap()
  private val edges: MutableList<CoverageEdge> = ArrayList()

  init {
    build()
  }

  fun getEdges(): List<CoverageEdge> {
    return edges
  }

  private fun build() {
    val nodes = findMultiRingNodes(coverage)
    val boundarySegs = CoverageBoundarySegmentFinder.findBoundarySegments(coverage)
    nodes.addAll(findBoundaryNodes(boundarySegs))
    val uniqueEdgeMap: HashMap<LineSegment, CoverageEdge> = HashMap()
    for (i in coverage.indices) {
      //-- geom is a Polygon or MultiPolygon
      val geom = coverage[i]
      val indexLargest = findLargestPolygonIndex(geom)
      for (ipoly in 0 until geom.getNumGeometries()) {
        val poly = geom.getGeometryN(ipoly) as Polygon

        //-- skip empty elements. Missing elements are copied in result
        if (poly.isEmpty())
          continue

        //-- largest polygon is the primary one, which is never removed
        val isPrimary = ipoly == indexLargest

        //-- extract shell
        val shell = poly.getExteriorRing()
        addRingEdges(i, shell, isPrimary, nodes, boundarySegs, uniqueEdgeMap)
        //-- extract holes
        for (ihole in 0 until poly.getNumInteriorRing()) {
          val hole = poly.getInteriorRingN(ihole)
          //-- skip empty holes. Missing rings are copied in result
          if (hole.isEmpty())
            continue
          //-- holes are never primary
          addRingEdges(i, hole, false, nodes, boundarySegs, uniqueEdgeMap)
        }
      }
    }
  }

  private fun findLargestPolygonIndex(geom: Geometry): Int {
    if (geom is Polygon)
      return 0
    var indexLargest = -1
    var areaLargest = -1.0
    for (ipoly in 0 until geom.getNumGeometries()) {
      val poly = geom.getGeometryN(ipoly) as Polygon
      val area = poly.getArea()
      if (area > areaLargest) {
        areaLargest = area
        indexLargest = ipoly
      }
    }
    return indexLargest
  }

  private fun addRingEdges(index: Int, ring: LinearRing, isPrimary: Boolean, nodes: MutableSet<Coordinate>, boundarySegs: Set<LineSegment>,
                           uniqueEdgeMap: HashMap<LineSegment, CoverageEdge>) {
    addBoundaryInnerNodes(ring, boundarySegs, nodes)
    val ringEdges = extractRingEdges(index, ring, isPrimary, uniqueEdgeMap, nodes)
    if (ringEdges != null)
      ringEdgesMap.put(ring, ringEdges)
  }

  /**
   * Detects nodes occurring at vertices which are between a boundary segment
   * and an inner (shared) segment.
   */
  private fun addBoundaryInnerNodes(ring: LinearRing, boundarySegs: Set<LineSegment>, nodes: MutableSet<Coordinate>) {
    val seq = ring.getCoordinateSequence()
    val isBdyLast = CoverageBoundarySegmentFinder.isBoundarySegment(boundarySegs, seq, seq.size() - 2)
    var isBdyPrev = isBdyLast
    for (i in 0 until seq.size() - 1) {
      val isBdy = CoverageBoundarySegmentFinder.isBoundarySegment(boundarySegs, seq, i)
      if (isBdy != isBdyPrev) {
        val nodePt = seq.getCoordinate(i)
        nodes.add(nodePt)
      }
      isBdyPrev = isBdy
    }
  }

  /**
   * Extracts the [CoverageEdge]s for a ring.
   *
   * @return null if the ring has too few distinct vertices
   */
  private fun extractRingEdges(index: Int, ring: LinearRing,
                               isPrimary: Boolean, uniqueEdgeMap: HashMap<LineSegment, CoverageEdge>,
                               nodes: Set<Coordinate>): List<CoverageEdge>? {
    val ringEdges: MutableList<CoverageEdge> = ArrayList()

    var pts = ring.getCoordinates()
    pts = CoordinateArrays.removeRepeatedPoints(pts)
    //-- if compacted ring is too short, don't process it
    if (pts.size < 3)
      return null

    val first = findNextNodeIndex(pts, -1, nodes)
    if (first < 0) {
      //-- ring does not contain a node, so edge is entire ring
      val edge = createEdge(pts, -1, -1, index, isPrimary, uniqueEdgeMap)
      ringEdges.add(edge)
    } else {
      var start = first
      var end = start
      //-- two-node edges are always primary
      var isEdgePrimary = true
      do {
        end = findNextNodeIndex(pts, start, nodes)
        //-- a single-node ring is only retained if specified
        if (end == start) {
          isEdgePrimary = isPrimary
        }
        val edge = createEdge(pts, start, end, index, isEdgePrimary, uniqueEdgeMap)
        ringEdges.add(edge)
        start = end
      } while (end != first)
    }
    return ringEdges
  }

  /**
   * Creates or updates an edge for the given ring or ring section.
   *
   * @return the CoverageEdge for the ring or portion of ring
   */
  private fun createEdge(ring: Array<Coordinate>, start: Int, end: Int, index: Int, isPrimary: Boolean, uniqueEdgeMap: HashMap<LineSegment, CoverageEdge>): CoverageEdge {
    val edge: CoverageEdge
    val edgeKey = if (end == start) CoverageEdge.key(ring) else CoverageEdge.key(ring, start, end)
    if (uniqueEdgeMap.containsKey(edgeKey)) {
      edge = uniqueEdgeMap.get(edgeKey)!!
      //-- update shared attributes
      edge.setPrimary(isPrimary)
    } else {
      edge = if (start < 0) {
        CoverageEdge.createEdge(ring, isPrimary)
      } else {
        CoverageEdge.createEdge(ring, start, end, isPrimary)
      }
      uniqueEdgeMap.put(edgeKey, edge)
      edges.add(edge)
    }
    edge.addIndex(index)
    edge.incRingCount()
    return edge
  }

  private fun findNextNodeIndex(ring: Array<Coordinate>, start: Int, nodes: Set<Coordinate>): Int {
    var index = start
    var isScanned0 = false
    do {
      index = next(index, ring)
      if (index == 0) {
        if (start < 0 && isScanned0)
          return -1
        isScanned0 = true
      }
      val pt = ring[index]
      if (nodes.contains(pt)) {
        return index
      }
    } while (index != start)
    return -1
  }

  /**
   * Finds nodes in a coverage at vertices which are shared by 3 or more rings.
   *
   * @return the set of nodes contained in 3 or more rings
   */
  private fun findMultiRingNodes(coverage: Array<Geometry>): MutableSet<Coordinate> {
    val vertexRingCount = VertexRingCounter.count(coverage)
    val nodes: MutableSet<Coordinate> = HashSet()
    for (v in vertexRingCount.keys) {
      if (vertexRingCount[v]!! >= 3) {
        nodes.add(v)
      }
    }
    return nodes
  }

  /**
   * Finds nodes occurring between boundary segments.
   *
   * @return a set of vertices which are nodes where two rings touch
   */
  private fun findBoundaryNodes(boundarySegments: Set<LineSegment>): Set<Coordinate> {
    val counter: MutableMap<Coordinate, Int> = HashMap()
    for (seg in boundarySegments) {
      counter.put(seg.p0, (counter[seg.p0] ?: 0) + 1)
      counter.put(seg.p1, (counter[seg.p1] ?: 0) + 1)
    }
    val result: MutableSet<Coordinate> = HashSet()
    for ((key, value) in counter) {
      if (value > 2)
        result.add(key)
    }
    return result
  }

  /**
   * Recreates the polygon coverage from the current edge values.
   *
   * @return an array of polygonal geometries representing the coverage
   */
  fun buildCoverage(): Array<Geometry?> {
    val result = arrayOfNulls<Geometry>(coverage.size)
    for (i in coverage.indices) {
      result[i] = buildPolygonal(coverage[i])
    }
    return result
  }

  private fun buildPolygonal(geom: Geometry): Geometry? {
    return if (geom is MultiPolygon) {
      buildMultiPolygon(geom)
    } else {
      buildPolygon(geom as Polygon)
    }
  }

  private fun buildMultiPolygon(geom: MultiPolygon): Geometry {
    val polyList: MutableList<Polygon> = ArrayList()
    for (i in 0 until geom.getNumGeometries()) {
      val poly = buildPolygon(geom.getGeometryN(i) as Polygon)
      if (poly != null) {
        polyList.add(poly)
      }
    }
    if (polyList.size == 1) {
      return polyList.get(0)
    }
    val polys = GeometryFactory.toPolygonArray(polyList)
    return geom.getFactory().createMultiPolygon(polys)
  }

  /**
   * @return null if the polygon has been removed
   */
  private fun buildPolygon(polygon: Polygon): Polygon? {
    val shell = buildRing(polygon.getExteriorRing())
    if (shell == null) {
      return null
    }
    if (polygon.getNumInteriorRing() == 0) {
      return polygon.getFactory().createPolygon(shell)
    }
    val holeList: MutableList<LinearRing> = ArrayList()
    for (i in 0 until polygon.getNumInteriorRing()) {
      val hole = polygon.getInteriorRingN(i)
      val newHole = buildRing(hole)
      if (newHole != null) {
        holeList.add(newHole)
      }
    }
    val holes = GeometryFactory.toLinearRingArray(holeList)
    return polygon.getFactory().createPolygon(shell, holes)
  }

  private fun buildRing(ring: LinearRing): LinearRing? {
    val ringEdges = ringEdgesMap.get(ring)
    //-- if ring is not in map, must have been invalid.  Just copy original
    if (ringEdges == null)
      return ring.copy() as LinearRing

    val isRemoved = ringEdges.size == 1
        && ringEdges.get(0).getCoordinates().size == 0
    if (isRemoved)
      return null

    val ptsList = CoordinateList()
    for (i in ringEdges.indices) {
      val lastPt = if (ptsList.size > 0)
        ptsList.getCoordinate(ptsList.size - 1)
      else
        null
      val dir = isEdgeDirForward(ringEdges, i, lastPt)
      ptsList.add(ringEdges.get(i).getCoordinates(), false, dir)
    }
    val pts = ptsList.toCoordinateArray()
    return ring.getFactory().createLinearRing(pts)
  }

  private fun isEdgeDirForward(ringEdges: List<CoverageEdge>, index: Int, prevPt: Coordinate?): Boolean {
    val size = ringEdges.size
    if (size <= 1) return true
    if (index == 0) {
      //-- if only 2 edges, first one can keep orientation
      if (size == 2)
        return true
      val endPt0 = ringEdges.get(0).getEndCoordinate()
      return endPt0.equals2D(ringEdges.get(1).getStartCoordinate())
          || endPt0.equals2D(ringEdges.get(1).getEndCoordinate())
    }
    //-- previous point determines required orientation
    return prevPt!!.equals2D(ringEdges.get(index).getStartCoordinate())
  }

  companion object {
    /**
     * Create a new instance for a given coverage.
     *
     * @param coverage the set of polygonal geometries in the coverage
     * @return the edges of the coverage
     */
    @JvmStatic
    fun create(coverage: Array<Geometry>): CoverageRingEdges {
      val edges = CoverageRingEdges(coverage)
      return edges
    }

    private fun next(index: Int, ring: Array<Coordinate>): Int {
      var idx = index + 1
      if (idx >= ring.size - 1)
        idx = 0
      return idx
    }
  }
}
