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
package org.locationtech.jts.operation.polygonize

import kotlin.jvm.JvmStatic


import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.CoordinateArrays
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.planargraph.DirectedEdge
import org.locationtech.jts.planargraph.Edge
import org.locationtech.jts.planargraph.Node
import org.locationtech.jts.planargraph.PlanarGraph
import org.locationtech.jts.util.Assert

/**
 * Represents a planar graph of edges that can be used to compute a
 * polygonization, and implements the algorithms to compute the
 * [EdgeRing]s formed by the graph.
 *
 * @version 1.7
 */
class PolygonizeGraph
/**
 * Create a new polygonization graph.
 */
(private val factory: GeometryFactory) : PlanarGraph() {

  /**
   * Add a [LineString] forming an edge of the polygon graph.
   * @param line the line to add
   */
  fun addEdge(line: LineString) {
    if (line.isEmpty()) {
      return
    }
    val linePts = CoordinateArrays.removeRepeatedPoints(line.getCoordinates())

    if (linePts.size < 2) {
      return
    }

    val startPt = linePts[0]
    val endPt = linePts[linePts.size - 1]

    val nStart = getNode(startPt)
    val nEnd = getNode(endPt)

    val de0: DirectedEdge = PolygonizeDirectedEdge(nStart, nEnd, linePts[1], true)
    val de1: DirectedEdge = PolygonizeDirectedEdge(nEnd, nStart, linePts[linePts.size - 2], false)
    val edge: Edge = PolygonizeEdge(line)
    edge.setDirectedEdges(de0, de1)
    add(edge)
  }

  private fun getNode(pt: Coordinate): Node {
    var node = findNode(pt)
    if (node == null) {
      node = Node(pt)
      // ensure node is only added once to graph
      add(node)
    }
    return node
  }

  private fun computeNextCWEdges() {
    // set the next pointers for the edges around each node
    val iNode = nodeIterator()
    while (iNode.hasNext()) {
      val node = iNode.next()
      computeNextCWEdges(node)
    }
  }

  /**
   * Convert the maximal edge rings found by the initial graph traversal
   * into the minimal edge rings required by JTS polygon topology rules.
   *
   * @param ringEdges the list of start edges for the edgeRings to convert.
   */
  private fun convertMaximalToMinimalEdgeRings(ringEdges: List<PolygonizeDirectedEdge>) {
    for (de in ringEdges) {
      val label = de.getLabel()
      val intNodes = findIntersectionNodes(de, label)

      if (intNodes == null) continue
      // flip the next pointers on the intersection nodes to create minimal edge rings
      for (node in intNodes) {
        computeNextCCWEdges(node, label)
      }
    }
  }

  /**
   * Computes the minimal EdgeRings formed by the edges in this graph.
   * @return a list of the [EdgeRing]s found by the polygonization process.
   */
  fun getEdgeRings(): MutableList<EdgeRing> {
    // maybe could optimize this
    computeNextCWEdges()
    // clear labels of all edges in graph
    label(dirEdges, -1)
    @Suppress("UNCHECKED_CAST")
    val maximalRings = findLabeledEdgeRings(dirEdges as Collection<PolygonizeDirectedEdge>)
    convertMaximalToMinimalEdgeRings(maximalRings)

    // find all edgerings (which will now be minimal ones, as required)
    val edgeRingList = ArrayList<EdgeRing>()
    for (obj in dirEdges) {
      val de = obj as PolygonizeDirectedEdge
      if (de.isMarked()) continue
      if (de.isInRing()) continue

      val er = findEdgeRing(de)
      edgeRingList.add(er)
    }
    return edgeRingList
  }

  /**
   * Finds and removes all cut edges from the graph.
   * @return a list of the [LineString]s forming the removed cut edges
   */
  fun deleteCutEdges(): MutableList<LineString> {
    computeNextCWEdges()
    // label the current set of edgerings
    @Suppress("UNCHECKED_CAST")
    findLabeledEdgeRings(dirEdges as Collection<PolygonizeDirectedEdge>)

    /**
     * Cut Edges are edges where both dirEdges have the same label.
     * Delete them, and record them
     */
    val cutLines = ArrayList<LineString>()
    for (obj in dirEdges) {
      val de = obj as PolygonizeDirectedEdge
      if (de.isMarked()) continue

      val sym = de.getSym() as PolygonizeDirectedEdge

      if (de.getLabel() == sym.getLabel()) {
        de.setMarked(true)
        sym.setMarked(true)

        // save the line as a cut edge
        val e = de.getEdge() as PolygonizeEdge
        cutLines.add(e.getLine())
      }
    }
    return cutLines
  }

  private fun findEdgeRing(startDE: PolygonizeDirectedEdge): EdgeRing {
    val er = EdgeRing(factory)
    er.build(startDE)
    return er
  }

  /**
   * Marks all edges from the graph which are "dangles".
   *
   * @return a List containing the [LineString]s that formed dangles
   */
  fun deleteDangles(): MutableList<LineString> {
    val nodesToRemove = findNodesOfDegree(1)
    val dangleLines = ArrayList<LineString>()

    val nodeStack = ArrayDeque<Node>()
    for (n in nodesToRemove) {
      nodeStack.addLast(n)
    }

    while (!nodeStack.isEmpty()) {
      val node = nodeStack.removeLast()

      deleteAllEdges(node)
      val nodeOutEdges = node.getOutEdges().getEdges()
      for (obj in nodeOutEdges) {
        val de = obj as PolygonizeDirectedEdge
        // delete this edge and its sym
        de.setMarked(true)
        val sym = de.getSym() as PolygonizeDirectedEdge?
        if (sym != null)
          sym.setMarked(true)

        // save the line as a dangle
        val e = de.getEdge() as PolygonizeEdge
        dangleLines.add(e.getLine())

        val toNode = de.getToNode()
        // add the toNode to the list to be processed, if it is now a dangle
        if (getDegreeNonDeleted(toNode) == 1)
          nodeStack.addLast(toNode)
      }
    }
    return dangleLines
  }

  companion object {
    private fun getDegreeNonDeleted(node: Node): Int {
      val edges = node.getOutEdges().getEdges()
      var degree = 0
      for (obj in edges) {
        val de = obj as PolygonizeDirectedEdge
        if (!de.isMarked()) degree++
      }
      return degree
    }

    private fun getDegree(node: Node, label: Long): Int {
      val edges = node.getOutEdges().getEdges()
      var degree = 0
      for (obj in edges) {
        val de = obj as PolygonizeDirectedEdge
        if (de.getLabel() == label) degree++
      }
      return degree
    }

    /**
     * Deletes all edges at a node
     */
    @JvmStatic
    fun deleteAllEdges(node: Node) {
      val edges = node.getOutEdges().getEdges()
      for (obj in edges) {
        val de = obj as PolygonizeDirectedEdge
        de.setMarked(true)
        val sym = de.getSym() as PolygonizeDirectedEdge?
        if (sym != null)
          sym.setMarked(true)
      }
    }

    /**
     * Finds all nodes in a maximal edgering which are self-intersection nodes
     *
     * @return the list of intersection nodes found,
     * or `null` if no intersection nodes were found
     */
    private fun findIntersectionNodes(startDE: PolygonizeDirectedEdge, label: Long): MutableList<Node>? {
      var de: PolygonizeDirectedEdge? = startDE
      var intNodes: MutableList<Node>? = null
      do {
        val d = de!!
        val node = d.getFromNode()
        if (getDegree(node, label) > 1) {
          if (intNodes == null)
            intNodes = ArrayList()
          intNodes.add(node)
        }

        de = d.getNext()
        Assert.isTrue(de != null, "found null DE in ring")
        Assert.isTrue(de === startDE || !de!!.isInRing(), "found DE already in ring")
      } while (de !== startDE)

      return intNodes
    }

    /**
     * Finds and labels all edgerings in the graph.
     *
     * @param dirEdges a List of the DirectedEdges in the graph
     * @return a List of DirectedEdges, one for each edge ring found
     */
    private fun findLabeledEdgeRings(dirEdges: Collection<PolygonizeDirectedEdge>): MutableList<PolygonizeDirectedEdge> {
      val edgeRingStarts = ArrayList<PolygonizeDirectedEdge>()
      // label the edge rings formed
      var currLabel: Long = 1
      for (de in dirEdges) {
        if (de.isMarked()) continue
        if (de.getLabel() >= 0) continue

        edgeRingStarts.add(de)
        val edges = EdgeRing.findDirEdgesInRing(de)

        label(edges, currLabel)
        currLabel++
      }
      return edgeRingStarts
    }

    private fun label(dirEdges: Collection<*>, label: Long) {
      for (obj in dirEdges) {
        val de = obj as PolygonizeDirectedEdge
        de.setLabel(label)
      }
    }

    private fun computeNextCWEdges(node: Node) {
      val deStar = node.getOutEdges()
      var startDE: PolygonizeDirectedEdge? = null
      var prevDE: PolygonizeDirectedEdge? = null

      // the edges are stored in CCW order around the star
      for (obj in deStar.getEdges()) {
        val outDE = obj as PolygonizeDirectedEdge
        if (outDE.isMarked()) continue

        if (startDE == null)
          startDE = outDE
        if (prevDE != null) {
          val sym = prevDE.getSym() as PolygonizeDirectedEdge
          sym.setNext(outDE)
        }
        prevDE = outDE
      }
      if (prevDE != null) {
        val sym = prevDE.getSym() as PolygonizeDirectedEdge
        sym.setNext(startDE)
      }
    }

    /**
     * Computes the next edge pointers going CCW around the given node, for the
     * given edgering label.
     */
    private fun computeNextCCWEdges(node: Node, label: Long) {
      val deStar = node.getOutEdges()
      var firstOutDE: PolygonizeDirectedEdge? = null
      var prevInDE: PolygonizeDirectedEdge? = null

      // the edges are stored in CCW order around the star
      val edges = deStar.getEdges()
      for (i in edges.size - 1 downTo 0) {
        val de = edges[i] as PolygonizeDirectedEdge
        val sym = de.getSym() as PolygonizeDirectedEdge

        var outDE: PolygonizeDirectedEdge? = null
        if (de.getLabel() == label) outDE = de
        var inDE: PolygonizeDirectedEdge? = null
        if (sym.getLabel() == label) inDE = sym

        if (outDE == null && inDE == null) continue // this edge is not in edgering

        if (inDE != null) {
          prevInDE = inDE
        }

        if (outDE != null) {
          if (prevInDE != null) {
            prevInDE.setNext(outDE)
            prevInDE = null
          }
          if (firstOutDE == null)
            firstOutDE = outDE
        }
      }
      if (prevInDE != null) {
        Assert.isTrue(firstOutDE != null)
        prevInDE.setNext(firstOutDE)
      }
    }
  }
}
