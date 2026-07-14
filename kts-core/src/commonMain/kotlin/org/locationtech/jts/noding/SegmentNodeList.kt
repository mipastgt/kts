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
package org.locationtech.jts.noding

import org.locationtech.jts.util.TreeMap

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.CoordinateList
import org.locationtech.jts.util.Assert

/**
 * A list of the [SegmentNode]s present along a noded [SegmentString].
 *
 */
class SegmentNodeList(private val edge: NodedSegmentString) {
  private val nodeMap: TreeMap<SegmentNode, SegmentNode> = TreeMap()

  /**
   * Gets the number of nodes in the list
   *
   * @return the size of the list
   */
  fun size(): Int {
    return nodeMap.size
  }

  fun getEdge(): NodedSegmentString = edge

  /**
   * Adds an intersection into the list, if it isn't already there.
   * The input segmentIndex and dist are expected to be normalized.
   *
   * @return the SegmentIntersection found or added
   */
  fun add(intPt: Coordinate, segmentIndex: Int): SegmentNode {
    val eiNew = SegmentNode(edge, intPt, segmentIndex, edge.getSegmentOctant(segmentIndex))
    val ei = nodeMap[eiNew]
    if (ei != null) {
      // debugging sanity check
      Assert.isTrue(ei.coord.equals2D(intPt), "Found equal nodes with different coordinates")
      //      if (! ei.coord.equals2D(intPt))
      //        Debug.println("Found equal nodes with different coordinates");

      return ei
    }
    // node does not exist, so create it
    nodeMap.put(eiNew, eiNew)
    return eiNew
  }

  /**
   * returns an iterator of SegmentNodes
   */
  fun iterator(): MutableIterator<SegmentNode> = nodeMap.values.iterator()

  /**
   * Adds nodes for the first and last points of the edge
   */
  private fun addEndpoints() {
    val maxSegIndex = edge.size() - 1
    add(edge.getCoordinate(0), 0)
    add(edge.getCoordinate(maxSegIndex), maxSegIndex)
  }

  /**
   * Adds nodes for any collapsed edge pairs.
   * Collapsed edge pairs can be caused by inserted nodes, or they can be
   * pre-existing in the edge vertex list.
   * In order to provide the correct fully noded semantics,
   * the vertex at the base of a collapsed pair must also be added as a node.
   */
  private fun addCollapsedNodes() {
    val collapsedVertexIndexes = ArrayList<Int>()

    findCollapsesFromInsertedNodes(collapsedVertexIndexes)
    findCollapsesFromExistingVertices(collapsedVertexIndexes)

    // node the collapses
    for (vertexIndex in collapsedVertexIndexes) {
      add(edge.getCoordinate(vertexIndex), vertexIndex)
    }
  }

  /**
   * Adds nodes for any collapsed edge pairs
   * which are pre-existing in the vertex list.
   */
  private fun findCollapsesFromExistingVertices(collapsedVertexIndexes: MutableList<Int>) {
    for (i in 0 until edge.size() - 2) {
      val p0 = edge.getCoordinate(i)
      val p2 = edge.getCoordinate(i + 2)
      if (p0.equals2D(p2)) {
        // add base of collapse as node
        collapsedVertexIndexes.add(i + 1)
      }
    }
  }

  /**
   * Adds nodes for any collapsed edge pairs caused by inserted nodes
   * Collapsed edge pairs occur when the same coordinate is inserted as a node
   * both before and after an existing edge vertex.
   * To provide the correct fully noded semantics,
   * the vertex must be added as a node as well.
   */
  private fun findCollapsesFromInsertedNodes(collapsedVertexIndexes: MutableList<Int>) {
    val collapsedVertexIndex = IntArray(1)
    val it = iterator()
    // there should always be at least two entries in the list, since the endpoints are nodes
    var eiPrev = it.next()
    while (it.hasNext()) {
      val ei = it.next()
      val isCollapsed = findCollapseIndex(eiPrev, ei, collapsedVertexIndex)
      if (isCollapsed)
        collapsedVertexIndexes.add(collapsedVertexIndex[0])

      eiPrev = ei
    }
  }

  private fun findCollapseIndex(ei0: SegmentNode, ei1: SegmentNode, collapsedVertexIndex: IntArray): Boolean {
    // only looking for equal nodes
    if (!ei0.coord.equals2D(ei1.coord)) return false

    var numVerticesBetween = ei1.segmentIndex - ei0.segmentIndex
    if (!ei1.isInterior()) {
      numVerticesBetween--
    }

    // if there is a single vertex between the two equal nodes, this is a collapse
    if (numVerticesBetween == 1) {
      collapsedVertexIndex[0] = ei0.segmentIndex + 1
      return true
    }
    return false
  }

  /**
   * Creates new edges for all the edges that the intersections in this
   * list split the parent edge into.
   * Adds the edges to the provided argument list
   * (this is so a single list can be used to accumulate all split edges
   * for a set of [SegmentString]s).
   */
  fun addSplitEdges(edgeList: MutableCollection<in NodedSegmentString>) {
    // ensure that the list has entries for the first and last point of the edge
    addEndpoints()
    addCollapsedNodes()

    val it = iterator()
    // there should always be at least two entries in the list, since the endpoints are nodes
    var eiPrev = it.next()
    while (it.hasNext()) {
      val ei = it.next()
      val newEdge = createSplitEdge(eiPrev, ei)
      /*
      if (newEdge.size() < 2)
        throw new RuntimeException("created single point edge: " + newEdge.toString());
      */
      edgeList.add(newEdge)
      eiPrev = ei
    }
    //checkSplitEdgesCorrectness(testingSplitEdges);
  }

  /**
   * Checks the correctness of the set of split edges corresponding to this edge.
   *
   * @param splitEdges the split edges for this edge (in order)
   */
  private fun checkSplitEdgesCorrectness(splitEdges: MutableList<Any?>) {
    val edgePts = edge.getCoordinates()

    // check that first and last points of split edges are same as endpoints of edge
    val split0 = splitEdges[0] as SegmentString
    val pt0 = split0.getCoordinate(0)
    if (!pt0.equals2D(edgePts[0]))
      throw RuntimeException("bad split edge start point at $pt0")

    val splitn = splitEdges[splitEdges.size - 1] as SegmentString
    val splitnPts = splitn.getCoordinates()
    val ptn = splitnPts[splitnPts.size - 1]
    if (!ptn.equals2D(edgePts[edgePts.size - 1]))
      throw RuntimeException("bad split edge end point at $ptn")
  }

  /**
   * Create a new "split edge" with the section of points between
   * (and including) the two intersections.
   * The label for the new edge is the same as the label for the parent edge.
   */
  private fun createSplitEdge(ei0: SegmentNode, ei1: SegmentNode): NodedSegmentString {
    val pts = createSplitEdgePts(ei0, ei1)
    return NodedSegmentString(pts, edge.getData())
  }

  /**
   * Extracts the points for a split edge running between two nodes.
   * The extracted points should contain no duplicate points.
   * There should always be at least two points extracted
   * (which will be the given nodes).
   *
   * @param ei0 the start node of the split edge
   * @param ei1 the end node of the split edge
   * @return the points for the split edge
   */
  private fun createSplitEdgePts(ei0: SegmentNode, ei1: SegmentNode): Array<Coordinate> {
    //Debug.println("\ncreateSplitEdge"); Debug.print(ei0); Debug.print(ei1);
    var npts = ei1.segmentIndex - ei0.segmentIndex + 2

    // if only two points in split edge they must be the node points
    if (npts == 2) return arrayOf(Coordinate(ei0.coord), Coordinate(ei1.coord))

    val lastSegStartPt = edge.getCoordinate(ei1.segmentIndex)
    /**
     * If the last intersection point is not equal to the its segment start pt,
     * add it to the points list as well.
     * This check is needed because the distance metric is not totally reliable!
     *
     * Also ensure that the created edge always has at least 2 points.
     *
     * The check for point equality is 2D only - Z values are ignored
     */
    val useIntPt1 = ei1.isInterior() || !ei1.coord.equals2D(lastSegStartPt)
    if (!useIntPt1) {
      npts--
    }

    @Suppress("UNCHECKED_CAST")
    val pts = arrayOfNulls<Coordinate>(npts) as Array<Coordinate>
    var ipt = 0
    pts[ipt++] = ei0.coord.copy()
    for (i in ei0.segmentIndex + 1..ei1.segmentIndex) {
      pts[ipt++] = edge.getCoordinate(i)
    }
    if (useIntPt1) pts[ipt] = ei1.coord.copy()
    return pts
  }

  /**
   * Gets the list of coordinates for the fully noded segment string,
   * including all original segment string vertices and vertices
   * introduced by nodes in this list.
   * Repeated coordinates are collapsed.
   *
   * @return an array of Coordinates
   */
  fun getSplitCoordinates(): Array<Coordinate> {
    val coordList = CoordinateList()
    // ensure that the list has entries for the first and last point of the edge
    addEndpoints()

    val it = iterator()
    // there should always be at least two entries in the list, since the endpoints are nodes
    var eiPrev = it.next()
    while (it.hasNext()) {
      val ei = it.next()
      addEdgeCoordinates(eiPrev, ei, coordList)
      eiPrev = ei
    }
    return coordList.toCoordinateArray()
  }

  private fun addEdgeCoordinates(ei0: SegmentNode, ei1: SegmentNode, coordList: CoordinateList) {
    val pts = createSplitEdgePts(ei0, ei1)
    coordList.add(pts, false)
  }

}

// INCOMPLETE!
internal class NodeVertexIterator(private val nodeList: SegmentNodeList) : MutableIterator<Any?> {
  private val edge: NodedSegmentString = nodeList.getEdge()
  private val nodeIt: MutableIterator<SegmentNode> = nodeList.iterator()
  private var currNode: SegmentNode? = null
  private var nextNode: SegmentNode? = null
  private var currSegIndex = 0

  init {
    readNextNode()
  }

  override fun hasNext(): Boolean {
    if (nextNode == null) return false
    return true
  }

  override fun next(): Any? {
    if (currNode == null) {
      currNode = nextNode
      currSegIndex = currNode!!.segmentIndex
      readNextNode()
      return currNode
    }
    // check for trying to read too far
    if (nextNode == null) return null

    if (nextNode!!.segmentIndex == currNode!!.segmentIndex) {
      currNode = nextNode
      currSegIndex = currNode!!.segmentIndex
      readNextNode()
      return currNode
    }

    if (nextNode!!.segmentIndex > currNode!!.segmentIndex) {
    }
    return null
  }

  private fun readNextNode() {
    if (nodeIt.hasNext())
      nextNode = nodeIt.next()
    else
      nextNode = null
  }

  /**
   *  Not implemented.
   *
   * @throws  UnsupportedOperationException  This method is not implemented.
   */
  override fun remove() {
    throw UnsupportedOperationException(this::class.simpleName)
  }
}
