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
package org.locationtech.jts.operation.buffer

import org.locationtech.jts.algorithm.LineIntersector
import org.locationtech.jts.algorithm.RobustLineIntersector
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.Location
import org.locationtech.jts.geom.Position
import org.locationtech.jts.geom.PrecisionModel
import org.locationtech.jts.geomgraph.Edge
import org.locationtech.jts.geomgraph.EdgeList
import org.locationtech.jts.geomgraph.Label
import org.locationtech.jts.geomgraph.Node
import org.locationtech.jts.geomgraph.PlanarGraph
import org.locationtech.jts.noding.FastNodingValidator
import org.locationtech.jts.noding.IntersectionAdder
import org.locationtech.jts.noding.MCIndexNoder
import org.locationtech.jts.noding.Noder
import org.locationtech.jts.noding.SegmentString
import org.locationtech.jts.operation.overlay.OverlayNodeFactory
import org.locationtech.jts.operation.overlay.PolygonBuilder

/**
 * Builds the buffer geometry for a given input geometry and precision model.
 *
 */
internal class BufferBuilder(private val bufParams: BufferParameters) {

  private var workingPrecisionModel: PrecisionModel? = null
  private var workingNoder: Noder? = null
  private lateinit var geomFact: GeometryFactory
  private var graph: PlanarGraph? = null
  private val edgeList = EdgeList()

  private var isInvertOrientation = false

  /**
   * Sets the precision model to use during the curve computation and noding,
   * if it is different to the precision model of the Geometry.
   *
   * @param pm the precision model to use
   */
  fun setWorkingPrecisionModel(pm: PrecisionModel) {
    workingPrecisionModel = pm
  }

  /**
   * Sets the [Noder] to use during noding.
   *
   * @param noder the noder to use
   */
  fun setNoder(noder: Noder) {
    workingNoder = noder
  }

  /**
   * Sets whether the offset curve is generated
   * using the inverted orientation of input rings.
   *
   * @param isInvertOrientation true if input ring orientation should be inverted
   */
  fun setInvertOrientation(isInvertOrientation: Boolean) {
    this.isInvertOrientation = isInvertOrientation
  }

  fun buffer(g: Geometry, distance: Double): Geometry {
    var precisionModel = workingPrecisionModel
    if (precisionModel == null) precisionModel = g.getPrecisionModel()

    // factory must be the same as the one used by the input
    geomFact = g.getFactory()

    val curveSetBuilder = BufferCurveSetBuilder(g, distance, precisionModel!!, bufParams)
    curveSetBuilder.setInvertOrientation(isInvertOrientation)

    val bufferSegStrList = curveSetBuilder.getCurves()

    // short-circuit test
    if (bufferSegStrList.size <= 0) {
      return createEmptyResultGeometry()
    }

    /**
     * Currently only zero-distance buffers are validated,
     * to avoid reducing performance for other buffers.
     */
    val isNodingValidated = distance == 0.0
    computeNodedEdges(bufferSegStrList, precisionModel, isNodingValidated)

    graph = PlanarGraph(OverlayNodeFactory())
    graph!!.addEdges(edgeList.getEdges())

    val subgraphList = createSubgraphs(graph!!)
    val polyBuilder = PolygonBuilder(geomFact)
    buildSubgraphs(subgraphList, polyBuilder)
    val resultPolyList = polyBuilder.getPolygons()

    // just in case...
    if (resultPolyList.size <= 0) {
      return createEmptyResultGeometry()
    }

    val resultGeom = geomFact.buildGeometry(resultPolyList)
    return resultGeom
  }

  private fun getNoder(precisionModel: PrecisionModel): Noder {
    if (workingNoder != null) return workingNoder!!

    // otherwise use a fast (but non-robust) noder
    val noder = MCIndexNoder()
    val li: LineIntersector = RobustLineIntersector()
    li.setPrecisionModel(precisionModel)
    noder.setSegmentIntersector(IntersectionAdder(li))
    return noder
  }

  private fun computeNodedEdges(bufferSegStrList: MutableList<SegmentString>, precisionModel: PrecisionModel, isNodingValidated: Boolean) {
    val noder = getNoder(precisionModel)
    noder.computeNodes(bufferSegStrList)
    val nodedSegStrings = noder.getNodedSubstrings()!!

    if (isNodingValidated) {
      val nv = FastNodingValidator(nodedSegStrings)
      nv.checkValid()
    }

    val i = nodedSegStrings.iterator()
    while (i.hasNext()) {
      val segStr = i.next() as SegmentString

      /**
       * Discard edges which have zero length,
       * since they carry no information and cause problems with topology building
       */
      val pts = segStr.getCoordinates()
      if (pts.size == 2 && pts[0].equals2D(pts[1])) continue

      val oldLabel = segStr.getData() as Label
      val edge = Edge(segStr.getCoordinates(), Label(oldLabel))
      insertUniqueEdge(edge)
    }
  }

  /**
   * Inserted edges are checked to see if an identical edge already exists.
   * If so, the edge is not inserted, but its label is merged
   * with the existing edge.
   */
  private fun insertUniqueEdge(e: Edge) {
    // fast lookup
    val existingEdge = edgeList.findEqualEdge(e)

    // If an identical edge already exists, simply update its label
    if (existingEdge != null) {
      val existingLabel = existingEdge.getLabel()!!

      var labelToMerge = e.getLabel()!!
      // check if new edge is in reverse direction to existing edge
      // if so, must flip the label before merging it
      if (!existingEdge.isPointwiseEqual(e)) {
        labelToMerge = Label(e.getLabel()!!)
        labelToMerge.flip()
      }
      existingLabel.merge(labelToMerge)

      // compute new depth delta of sum of edges
      val mergeDelta = depthDelta(labelToMerge)
      val existingDelta = existingEdge.getDepthDelta()
      val newDelta = existingDelta + mergeDelta
      existingEdge.setDepthDelta(newDelta)
    } else { // no matching existing edge was found
      // add this new edge to the list of edges in this graph
      edgeList.add(e)
      e.setDepthDelta(depthDelta(e.getLabel()!!))
    }
  }

  private fun createSubgraphs(graph: PlanarGraph): MutableList<BufferSubgraph> {
    val subgraphList = ArrayList<BufferSubgraph>()
    val i = graph.getNodes().iterator()
    while (i.hasNext()) {
      val node = i.next()
      if (!node.isVisited()) {
        val subgraph = BufferSubgraph()
        subgraph.create(node)
        subgraphList.add(subgraph)
      }
    }
    /**
     * Sort the subgraphs in descending order of their rightmost coordinate.
     * This ensures that when the Polygons for the subgraphs are built,
     * subgraphs for shells will have been built before the subgraphs for
     * any holes they contain.
     */
    subgraphList.sortWith(reverseOrder())
    return subgraphList
  }

  /**
   * Completes the building of the input subgraphs by depth-labelling them,
   * and adds them to the PolygonBuilder.
   * The subgraph list must be sorted in rightmost-coordinate order.
   */
  private fun buildSubgraphs(subgraphList: MutableList<BufferSubgraph>, polyBuilder: PolygonBuilder) {
    val processedGraphs = ArrayList<BufferSubgraph>()
    val i = subgraphList.iterator()
    while (i.hasNext()) {
      val subgraph = i.next()
      val p = subgraph.getRightmostCoordinate()
      val locater = SubgraphDepthLocater(processedGraphs)
      val outsideDepth = locater.getDepth(p!!)
      subgraph.computeDepth(outsideDepth)
      subgraph.findResultEdges()
      processedGraphs.add(subgraph)
      polyBuilder.add(subgraph.getDirectedEdges(), subgraph.getNodes())
    }
  }

  /**
   * Gets the standard result for an empty buffer.
   * Since buffer always returns a polygonal result,
   * this is chosen to be an empty polygon.
   *
   * @return the empty result geometry
   */
  private fun createEmptyResultGeometry(): Geometry {
    val emptyGeom = geomFact.createPolygon()
    return emptyGeom
  }

  companion object {
    /**
     * Compute the change in depth as an edge is crossed from R to L
     */
    private fun depthDelta(label: Label): Int {
      val lLoc = label.getLocation(0, Position.LEFT)
      val rLoc = label.getLocation(0, Position.RIGHT)
      if (lLoc == Location.INTERIOR && rLoc == Location.EXTERIOR) return 1
      else if (lLoc == Location.EXTERIOR && rLoc == Location.INTERIOR) return -1
      return 0
    }

    private fun convertSegStrings(it: Iterator<*>): Geometry {
      val fact = GeometryFactory()
      val lines = ArrayList<LineString>()
      while (it.hasNext()) {
        val ss = it.next() as SegmentString
        val line = fact.createLineString(ss.getCoordinates())
        lines.add(line)
      }
      return fact.buildGeometry(lines)
    }
  }
}
