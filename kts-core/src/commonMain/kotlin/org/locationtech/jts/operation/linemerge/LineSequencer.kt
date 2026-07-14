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
package org.locationtech.jts.operation.linemerge

import kotlin.jvm.JvmStatic

import org.locationtech.jts.util.TreeSet

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryComponentFilter
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.MultiLineString
import org.locationtech.jts.planargraph.DirectedEdge
import org.locationtech.jts.planargraph.GraphComponent
import org.locationtech.jts.planargraph.Node
import org.locationtech.jts.planargraph.Subgraph
import org.locationtech.jts.planargraph.algorithm.ConnectedSubgraphFinder
import org.locationtech.jts.util.Assert

/**
 * Builds a sequence from a set of LineStrings so that
 * they are ordered end to end.
 *
 */
class LineSequencer {

  private val graph = LineMergeGraph()
  // initialize with default, in case no lines are input
  private var factory = GeometryFactory()
  private var lineCount = 0

  private var isRun = false
  private var sequencedGeometry: Geometry? = null
  private var isSequenceable = false

  /**
   * Adds a [Collection] of [Geometry]s to be sequenced.
   *
   * @param geometries a Collection of geometries to add
   */
  fun add(geometries: Collection<*>) {
    val i = geometries.iterator()
    while (i.hasNext()) {
      val geometry = i.next() as Geometry
      add(geometry)
    }
  }

  /**
   * Adds a [Geometry] to be sequenced.
   *
   * @param geometry the geometry to add
   */
  fun add(geometry: Geometry) {
    geometry.apply(object : GeometryComponentFilter {
      override fun filter(component: Geometry) {
        if (component is LineString) {
          addLine(component)
        }
      }
    })
  }

  private fun addLine(lineString: LineString) {
    graph.addEdge(lineString)
    lineCount++
  }

  /**
   * Tests whether the arrangement of linestrings has a valid
   * sequence.
   *
   * @return `true` if a valid sequence exists.
   */
  fun isSequenceable(): Boolean {
    computeSequence()
    return isSequenceable
  }

  /**
   * Returns the [LineString] or [MultiLineString]
   * built by the sequencing process, if one exists.
   *
   * @return the sequenced linestrings,
   * or `null` if a valid sequence does not exist
   */
  fun getSequencedLineStrings(): Geometry? {
    computeSequence()
    return sequencedGeometry
  }

  private fun computeSequence() {
    if (isRun) {
      return
    }
    isRun = true

    val sequences = findSequences()
    if (sequences == null)
      return

    val sequencedGeometry = buildSequencedGeometry(sequences)
    this.sequencedGeometry = sequencedGeometry
    isSequenceable = true

    val finalLineCount = sequencedGeometry.getNumGeometries()
    Assert.isTrue(lineCount == finalLineCount, "Lines were missing from result")
    Assert.isTrue(
      sequencedGeometry is LineString || sequencedGeometry is MultiLineString,
      "Result is not lineal"
    )
  }

  private fun findSequences(): MutableList<MutableList<DirectedEdge>>? {
    val sequences = ArrayList<MutableList<DirectedEdge>>()
    val csFinder = ConnectedSubgraphFinder(graph)
    val subgraphs = csFinder.getConnectedSubgraphs()
    val i = subgraphs.iterator()
    while (i.hasNext()) {
      val subgraph = i.next()
      if (hasSequence(subgraph)) {
        val seq = findSequence(subgraph)
        sequences.add(seq)
      } else {
        // if any subgraph cannot be sequenced, abort
        return null
      }
    }
    return sequences
  }

  /**
   * Tests whether a complete unique path exists in a graph
   * using Euler's Theorem.
   *
   * @param graph the subgraph containing the edges
   * @return `true` if a sequence exists
   */
  private fun hasSequence(graph: Subgraph): Boolean {
    var oddDegreeCount = 0
    val i = graph.nodeIterator()
    while (i.hasNext()) {
      val node = i.next()
      if (node.getDegree() % 2 == 1)
        oddDegreeCount++
    }
    return oddDegreeCount <= 2
  }

  private fun findSequence(graph: Subgraph): MutableList<DirectedEdge> {
    GraphComponent.setVisited(graph.edgeIterator(), false)

    val startNode = findLowestDegreeNode(graph)
    val startDE = startNode!!.getOutEdges().iterator().next()
    val startDESym = startDE.getSym()

    val seq = ArrayDeque<DirectedEdge>()
    val lit = seq.listIterator()
    addReverseSubpath(startDESym!!, lit, false)
    while (lit.hasPrevious()) {
      val prev = lit.previous()
      val unvisitedOutDE = findUnvisitedBestOrientedDE(prev.getFromNode())
      if (unvisitedOutDE != null)
        addReverseSubpath(unvisitedOutDE.getSym()!!, lit, true)
    }

    /**
     * At this point, we have a valid sequence of graph DirectedEdges, but it
     * is not necessarily appropriately oriented relative to the underlying
     * geometry.
     */
    val orientedSeq = orient(seq)
    return orientedSeq
  }

  private fun addReverseSubpath(deArg: DirectedEdge, lit: MutableListIterator<DirectedEdge>, expectedClosed: Boolean) {
    // trace an unvisited path *backwards* from this de
    var de = deArg
    val endNode = de.getToNode()

    var fromNode: Node? = null
    while (true) {
      lit.add(de.getSym()!!)
      de.getEdge()!!.setVisited(true)
      fromNode = de.getFromNode()
      val unvisitedOutDE = findUnvisitedBestOrientedDE(fromNode)
      // this must terminate, since we are continually marking edges as visited
      if (unvisitedOutDE == null)
        break
      de = unvisitedOutDE.getSym()!!
    }
    if (expectedClosed) {
      // the path should end at the toNode of this de, otherwise we have an error
      Assert.isTrue(fromNode === endNode, "path not contiguous")
    }
  }

  /**
   * Computes a version of the sequence which is optimally
   * oriented relative to the underlying geometry.
   *
   * @param seq a List of DirectedEdges
   * @return a List of DirectedEdges oriented appropriately
   */
  private fun orient(seq: MutableList<DirectedEdge>): MutableList<DirectedEdge> {
    val startEdge = seq[0]
    val endEdge = seq[seq.size - 1]
    val startNode = startEdge.getFromNode()
    val endNode = endEdge.getToNode()

    var flipSeq = false
    val hasDegree1Node = startNode.getDegree() == 1 || endNode.getDegree() == 1

    if (hasDegree1Node) {
      var hasObviousStartNode = false

      // test end edge before start edge, to make result stable
      // (ie. if both are good starts, pick the actual start
      if (endEdge.getToNode().getDegree() == 1 && !endEdge.getEdgeDirection()) {
        hasObviousStartNode = true
        flipSeq = true
      }
      if (startEdge.getFromNode().getDegree() == 1 && startEdge.getEdgeDirection()) {
        hasObviousStartNode = true
        flipSeq = false
      }

      // since there is no obvious start node, use any node of degree 1
      if (!hasObviousStartNode) {
        // check if the start node should actually be the end node
        if (startEdge.getFromNode().getDegree() == 1)
          flipSeq = true
        // if the end node is of degree 1, it is properly the end node
      }
    }

    // if there is no degree 1 node, just use the sequence as is

    if (flipSeq)
      return reverse(seq)
    return seq
  }

  /**
   * Reverse the sequence.
   * This requires reversing the order of the dirEdges, and flipping
   * each dirEdge as well
   *
   * @param seq a List of DirectedEdges, in sequential order
   * @return the reversed sequence
   */
  private fun reverse(seq: List<DirectedEdge>): MutableList<DirectedEdge> {
    val newSeq = ArrayDeque<DirectedEdge>()
    for (de in seq) {
      newSeq.addFirst(de.getSym()!!)
    }
    return newSeq
  }

  /**
   * Builds a geometry ([LineString] or [MultiLineString] )
   * representing the sequence.
   *
   * @param sequences a List of Lists of DirectedEdges with
   *   LineMergeEdges as their parent edges.
   * @return the sequenced geometry, or `null` if no sequence exists
   */
  private fun buildSequencedGeometry(sequences: List<List<DirectedEdge>>): Geometry {
    val lines = ArrayList<LineString>()

    for (seq in sequences) {
      for (de in seq) {
        val e = de.getEdge() as LineMergeEdge
        val line = e.getLine()

        var lineToAdd = line
        if (!de.getEdgeDirection() && !line.isClosed())
          lineToAdd = reverse(line)

        lines.add(lineToAdd)
      }
    }
    if (lines.size == 0)
      return factory.createMultiLineString(arrayOf<LineString>())
    return factory.buildGeometry(lines)
  }

  companion object {
    @JvmStatic
    fun sequence(geom: Geometry): Geometry? {
      val sequencer = LineSequencer()
      sequencer.add(geom)
      return sequencer.getSequencedLineStrings()
    }

    /**
     * Tests whether a [Geometry] is sequenced correctly.
     *
     * @param geom the geometry to test
     * @return `true` if the geometry is sequenced or is not lineal
     */
    @JvmStatic
    fun isSequenced(geom: Geometry): Boolean {
      if (geom !is MultiLineString) {
        return true
      }

      val mls = geom
      // the nodes in all subgraphs which have been completely scanned
      val prevSubgraphNodes = TreeSet<Coordinate>()

      var lastNode: Coordinate? = null
      val currNodes = ArrayList<Coordinate>()
      for (i in 0 until mls.getNumGeometries()) {
        val line = mls.getGeometryN(i) as LineString
        val startNode = line.getCoordinateN(0)
        val endNode = line.getCoordinateN(line.getNumPoints() - 1)

        /**
         * If this linestring is connected to a previous subgraph, geom is not sequenced
         */
        if (prevSubgraphNodes.contains(startNode)) return false
        if (prevSubgraphNodes.contains(endNode)) return false

        if (lastNode != null) {
          if (!startNode.equals(lastNode)) {
            // start new connected sequence
            prevSubgraphNodes.addAll(currNodes)
            currNodes.clear()
          }
        }
        currNodes.add(startNode)
        currNodes.add(endNode)
        lastNode = endNode
      }
      return true
    }

    /**
     * Finds an [DirectedEdge] for an unvisited edge (if any),
     * choosing the dirEdge which preserves orientation, if possible.
     *
     * @param node the node to examine
     * @return the dirEdge found, or `null` if none were unvisited
     */
    private fun findUnvisitedBestOrientedDE(node: Node): DirectedEdge? {
      var wellOrientedDE: DirectedEdge? = null
      var unvisitedDE: DirectedEdge? = null
      val i = node.getOutEdges().iterator()
      while (i.hasNext()) {
        val de = i.next()
        if (!de.getEdge()!!.isVisited()) {
          unvisitedDE = de
          if (de.getEdgeDirection())
            wellOrientedDE = de
        }
      }
      if (wellOrientedDE != null)
        return wellOrientedDE
      return unvisitedDE
    }

    private fun findLowestDegreeNode(graph: Subgraph): Node? {
      var minDegree = Int.MAX_VALUE
      var minDegreeNode: Node? = null
      val i = graph.nodeIterator()
      while (i.hasNext()) {
        val node = i.next()
        if (minDegreeNode == null || node.getDegree() < minDegree) {
          minDegree = node.getDegree()
          minDegreeNode = node
        }
      }
      return minDegreeNode
    }

    private fun reverse(line: LineString): LineString {
      val pts = line.getCoordinates()
      val revPts = arrayOfNulls<Coordinate>(pts.size)
      val len = pts.size
      for (i in 0 until len) {
        revPts[len - 1 - i] = Coordinate(pts[i])
      }
      @Suppress("UNCHECKED_CAST")
      return line.getFactory().createLineString(revPts as Array<Coordinate>)
    }
  }
}
