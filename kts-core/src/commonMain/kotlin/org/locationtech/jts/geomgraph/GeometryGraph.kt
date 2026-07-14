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
package org.locationtech.jts.geomgraph

import kotlin.jvm.JvmStatic

import org.locationtech.jts.algorithm.BoundaryNodeRule
import org.locationtech.jts.algorithm.LineIntersector
import org.locationtech.jts.algorithm.Orientation
import org.locationtech.jts.algorithm.PointLocator
import org.locationtech.jts.algorithm.locate.IndexedPointInAreaLocator
import org.locationtech.jts.algorithm.locate.PointOnGeometryLocator
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.CoordinateArrays
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryCollection
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.LinearRing
import org.locationtech.jts.geom.Location
import org.locationtech.jts.geom.MultiLineString
import org.locationtech.jts.geom.MultiPoint
import org.locationtech.jts.geom.MultiPolygon
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.Polygon
import org.locationtech.jts.geom.Polygonal
import org.locationtech.jts.geom.Position
import org.locationtech.jts.geomgraph.index.EdgeSetIntersector
import org.locationtech.jts.geomgraph.index.SegmentIntersector
import org.locationtech.jts.geomgraph.index.SimpleMCSweepLineIntersector
import org.locationtech.jts.util.Assert

/**
 * A GeometryGraph is a graph that models a given Geometry
 */
open class GeometryGraph(
  private val argIndex: Int,  // the index of this geometry as an argument to a spatial function (used for labelling)
  private val parentGeom: Geometry?,
  private val boundaryNodeRule: BoundaryNodeRule
) : PlanarGraph() {

  /**
   * The lineEdgeMap is a map of the linestring components of the
   * parentGeometry to the edges which are derived from them.
   * This is used to efficiently perform findEdge queries
   */
  private val lineEdgeMap: MutableMap<LineString, Edge> = HashMap()

  /**
   * If this flag is true, the Boundary Determination Rule will used when deciding
   * whether nodes are in the boundary or not
   */
  private var useBoundaryDeterminationRule = true
  private var boundaryNodes: MutableCollection<Node>? = null
  private var tooFewPoints = false
  private var invalidPoint: Coordinate? = null

  private var areaPtLocator: PointOnGeometryLocator? = null
  // for use if geometry is not Polygonal
  private val ptLocator = PointLocator()

  init {
    if (parentGeom != null) {
//      precisionModel = parentGeom.getPrecisionModel();
//      SRID = parentGeom.getSRID();
      add(parentGeom)
    }
  }

  constructor(argIndex: Int, parentGeom: Geometry?) :
    this(argIndex, parentGeom, BoundaryNodeRule.OGC_SFS_BOUNDARY_RULE)

  private fun createEdgeSetIntersector(): EdgeSetIntersector {
    // various options for computing intersections, from slowest to fastest

    //private EdgeSetIntersector esi = new SimpleEdgeSetIntersector();
    //private EdgeSetIntersector esi = new MonotoneChainIntersector();
    //private EdgeSetIntersector esi = new NonReversingChainIntersector();
    //private EdgeSetIntersector esi = new SimpleSweepLineIntersector();
    //private EdgeSetIntersector esi = new MCSweepLineIntersector();

    //return new SimpleEdgeSetIntersector();
    return SimpleMCSweepLineIntersector()
  }

  fun hasTooFewPoints(): Boolean = tooFewPoints

  fun getInvalidPoint(): Coordinate? = invalidPoint

  fun getGeometry(): Geometry? = parentGeom

  fun getBoundaryNodeRule(): BoundaryNodeRule = boundaryNodeRule

  fun getBoundaryNodes(): MutableCollection<Node> {
    if (boundaryNodes == null)
      boundaryNodes = nodes.getBoundaryNodes(argIndex)
    return boundaryNodes!!
  }

  fun getBoundaryPoints(): Array<Coordinate> {
    val coll = getBoundaryNodes()
    @Suppress("UNCHECKED_CAST")
    val pts = arrayOfNulls<Coordinate>(coll.size) as Array<Coordinate>
    var i = 0
    for (node in coll) {
      pts[i++] = node.getCoordinate()!!.copy()
    }
    return pts
  }

  fun findEdge(line: LineString): Edge? {
    return lineEdgeMap[line]
  }

  fun computeSplitEdges(edgelist: MutableList<Edge>) {
    for (e in edges) {
      e.eiList.addSplitEdges(edgelist)
    }
  }

  private fun add(g: Geometry) {
    if (g.isEmpty()) return

    // check if this Geometry should obey the Boundary Determination Rule
    // all collections except MultiPolygons obey the rule
    if (g is MultiPolygon)
      useBoundaryDeterminationRule = false

    if (g is Polygon) addPolygon(g)
    // LineString also handles LinearRings
    else if (g is LineString) addLineString(g)
    else if (g is Point) addPoint(g)
    else if (g is MultiPoint) addCollection(g)
    else if (g is MultiLineString) addCollection(g)
    else if (g is MultiPolygon) addCollection(g)
    else if (g is GeometryCollection) addCollection(g)
    else throw UnsupportedOperationException(g::class.simpleName)
  }

  private fun addCollection(gc: GeometryCollection) {
    for (i in 0 until gc.getNumGeometries()) {
      add(gc.getGeometryN(i)!!)
    }
  }

  /**
   * Add a Point to the graph.
   */
  private fun addPoint(p: Point) {
    val coord = p.getCoordinate()
    insertPoint(argIndex, coord!!, Location.INTERIOR)
  }

  /**
   * Adds a polygon ring to the graph.
   * Empty rings are ignored.
   *
   * The left and right topological location arguments assume that the ring is oriented CW.
   * If the ring is in the opposite orientation,
   * the left and right locations must be interchanged.
   */
  private fun addPolygonRing(lr: LinearRing, cwLeft: Int, cwRight: Int) {
    // don't bother adding empty holes
    if (lr.isEmpty()) return

    val coord = CoordinateArrays.removeRepeatedPoints(lr.getCoordinates())

    if (coord.size < 4) {
      tooFewPoints = true
      invalidPoint = coord[0]
      return
    }

    var left = cwLeft
    var right = cwRight
    if (Orientation.isCCW(coord)) {
      left = cwRight
      right = cwLeft
    }
    val e = Edge(coord, Label(argIndex, Location.BOUNDARY, left, right))
    lineEdgeMap[lr] = e

    insertEdge(e)
    // insert the endpoint as a node, to mark that it is on the boundary
    insertPoint(argIndex, coord[0], Location.BOUNDARY)
  }

  private fun addPolygon(p: Polygon) {
    addPolygonRing(
      p.getExteriorRing(),
      Location.EXTERIOR,
      Location.INTERIOR
    )

    for (i in 0 until p.getNumInteriorRing()) {
      val hole = p.getInteriorRingN(i)

      // Holes are topologically labelled opposite to the shell, since
      // the interior of the polygon lies on their opposite side
      // (on the left, if the hole is oriented CW)
      addPolygonRing(
        hole,
        Location.INTERIOR,
        Location.EXTERIOR
      )
    }
  }

  private fun addLineString(line: LineString) {
    val coord = CoordinateArrays.removeRepeatedPoints(line.getCoordinates())

    if (coord.size < 2) {
      tooFewPoints = true
      invalidPoint = coord[0]
      return
    }

    // add the edge for the LineString
    // line edges do not have locations for their left and right sides
    val e = Edge(coord, Label(argIndex, Location.INTERIOR))
    lineEdgeMap[line] = e
    insertEdge(e)
    /*
     * Add the boundary points of the LineString, if any.
     * Even if the LineString is closed, add both points as if they were endpoints.
     * This allows for the case that the node already exists and is a boundary point.
     */
    Assert.isTrue(coord.size >= 2, "found LineString with single point")
    insertBoundaryPoint(argIndex, coord[0])
    insertBoundaryPoint(argIndex, coord[coord.size - 1])
  }

  /**
   * Add an Edge computed externally.  The label on the Edge is assumed
   * to be correct.
   *
   * @param e Edge
   */
  fun addEdge(e: Edge) {
    insertEdge(e)
    val coord = e.getCoordinates()
    // insert the endpoint as a node, to mark that it is on the boundary
    insertPoint(argIndex, coord[0], Location.BOUNDARY)
    insertPoint(argIndex, coord[coord.size - 1], Location.BOUNDARY)
  }

  /**
   * Add a point computed externally.  The point is assumed to be a
   * Point Geometry part, which has a location of INTERIOR.
   *
   * @param pt Coordinate
   */
  fun addPoint(pt: Coordinate) {
    insertPoint(argIndex, pt, Location.INTERIOR)
  }

  /**
   * Compute self-nodes, taking advantage of the Geometry type to
   * minimize the number of intersection tests.  (E.g. rings are
   * not tested for self-intersection, since they are assumed to be valid).
   *
   * @param li the LineIntersector to use
   * @param computeRingSelfNodes if `false`, intersection checks are optimized to not test rings for self-intersection
   * @return the computed SegmentIntersector containing information about the intersections found
   */
  fun computeSelfNodes(li: LineIntersector, computeRingSelfNodes: Boolean): SegmentIntersector {
    val si = SegmentIntersector(li, true, false)
    val esi = createEdgeSetIntersector()
    // optimize intersection search for valid Polygons and LinearRings
    val isRings = parentGeom is LinearRing ||
      parentGeom is Polygon ||
      parentGeom is MultiPolygon
    val computeAllSegments = computeRingSelfNodes || !isRings
    esi.computeIntersections(edges, si, computeAllSegments)

    //System.out.println("SegmentIntersector # tests = " + si.numTests);
    addSelfIntersectionNodes(argIndex)
    return si
  }

  fun computeEdgeIntersections(
    g: GeometryGraph,
    li: LineIntersector,
    includeProper: Boolean
  ): SegmentIntersector {
    val si = SegmentIntersector(li, includeProper, true)
    si.setBoundaryNodes(this.getBoundaryNodes(), g.getBoundaryNodes())

    val esi = createEdgeSetIntersector()
    esi.computeIntersections(edges, g.edges, si)
/*
for (Iterator i = g.edges.iterator(); i.hasNext();) {
Edge e = (Edge) i.next();
Debug.print(e.getEdgeIntersectionList());
}
*/
    return si
  }

  private fun insertPoint(argIndex: Int, coord: Coordinate, onLocation: Int) {
    val n = nodes.addNode(coord)
    val lbl = n.getLabel()
    if (lbl == null) {
      n.setLabel(Label(argIndex, onLocation))
    } else
      lbl.setLocation(argIndex, onLocation)
  }

  /**
   * Adds candidate boundary points using the current [BoundaryNodeRule].
   * This is used to add the boundary
   * points of dim-1 geometries (Curves/MultiCurves).
   */
  private fun insertBoundaryPoint(argIndex: Int, coord: Coordinate) {
    val n = nodes.addNode(coord)
    // nodes always have labels
    val lbl = n.getLabel()!!
    // the new point to insert is on a boundary
    var boundaryCount = 1
    // determine the current location for the point (if any)
    var loc = Location.NONE
    loc = lbl.getLocation(argIndex, Position.ON)
    if (loc == Location.BOUNDARY) boundaryCount++

    // determine the boundary status of the point according to the Boundary Determination Rule
    val newLoc = determineBoundary(boundaryNodeRule, boundaryCount)
    lbl.setLocation(argIndex, newLoc)
  }

  private fun addSelfIntersectionNodes(argIndex: Int) {
    for (e in edges) {
      val eLoc = e.getLabel()!!.getLocation(argIndex)
      val eiIt = e.eiList.iterator()
      while (eiIt.hasNext()) {
        val ei = eiIt.next()
        addSelfIntersectionNode(argIndex, ei.coord, eLoc)
      }
    }
  }

  /**
   * Add a node for a self-intersection.
   * If the node is a potential boundary node (e.g. came from an edge which
   * is a boundary) then insert it as a potential boundary node.
   * Otherwise, just add it as a regular node.
   */
  private fun addSelfIntersectionNode(argIndex: Int, coord: Coordinate, loc: Int) {
    // if this node is already a boundary node, don't change it
    if (isBoundaryNode(argIndex, coord)) return
    if (loc == Location.BOUNDARY && useBoundaryDeterminationRule)
      insertBoundaryPoint(argIndex, coord)
    else
      insertPoint(argIndex, coord, loc)
  }

  // MD - experimental for now
  /**
   * Determines the [Location] of the given [Coordinate]
   * in this geometry.
   *
   * @param pt the point to test
   * @return the location of the point in the geometry
   */
  fun locate(pt: Coordinate): Int {
    if (parentGeom is Polygonal && (parentGeom as Geometry).getNumGeometries() > 50) {
      // lazily init point locator
      if (areaPtLocator == null) {
        areaPtLocator = IndexedPointInAreaLocator(parentGeom as Geometry)
      }
      return areaPtLocator!!.locate(pt)
    }
    return ptLocator.locate(pt, parentGeom!!)
  }

  companion object {
    /**
     * Determine boundary
     *
     * @param boundaryNodeRule Boundary node rule
     * @param boundaryCount the number of component boundaries that this point occurs in
     * @return boundary or interior
     */
    @JvmStatic
    fun determineBoundary(boundaryNodeRule: BoundaryNodeRule, boundaryCount: Int): Int {
      return if (boundaryNodeRule.isInBoundary(boundaryCount))
        Location.BOUNDARY else Location.INTERIOR
    }
  }
}
