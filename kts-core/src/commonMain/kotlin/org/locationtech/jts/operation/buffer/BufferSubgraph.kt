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

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.geom.Position
import org.locationtech.jts.geom.TopologyException
import org.locationtech.jts.geomgraph.DirectedEdge
import org.locationtech.jts.geomgraph.DirectedEdgeStar
import org.locationtech.jts.geomgraph.Node

/**
 * A connected subset of the graph of
 * [DirectedEdge]s and [Node]s.
 * Its edges will generate either
 * 
 * -  a single polygon in the complete buffer, with zero or more holes, or
 * -  one or more connected holes
 * 
 *
 *
 */
internal class BufferSubgraph : Comparable<Any?> {
  private val finder: RightmostEdgeFinder = RightmostEdgeFinder()
  private val dirEdgeList: MutableList<DirectedEdge> = ArrayList()
  private val nodes: MutableList<Node> = ArrayList()
  private var rightMostCoord: Coordinate? = null
  private var env: Envelope? = null

  fun getDirectedEdges(): MutableList<DirectedEdge> {
    return dirEdgeList
  }

  fun getNodes(): MutableList<Node> {
    return nodes
  }

  /**
   * Computes the envelope of the edges in the subgraph.
   * The envelope is cached after being computed.
   *
   * @return the envelope of the graph.
   */
  fun getEnvelope(): Envelope {
    if (env == null) {
      val edgeEnv = Envelope()
      val it = dirEdgeList.iterator()
      while (it.hasNext()) {
        val dirEdge = it.next()
        val pts = dirEdge.getEdge().getCoordinates()
        for (i in 0 until pts.size - 1) {
          edgeEnv.expandToInclude(pts[i])
        }
      }
      env = edgeEnv
    }
    return env!!
  }

  /**
   * Gets the rightmost coordinate in the edges of the subgraph
   */
  fun getRightmostCoordinate(): Coordinate? {
    return rightMostCoord
  }

  /**
   * Creates the subgraph consisting of all edges reachable from this node.
   * Finds the edges in the graph and the rightmost coordinate.
   *
   * @param node a node to start the graph traversal from
   */
  fun create(node: Node) {
    addReachable(node)
    finder.findEdge(dirEdgeList)
    rightMostCoord = finder.getCoordinate()
  }

  /**
   * Adds all nodes and edges reachable from this node to the subgraph.
   * Uses an explicit stack to avoid a large depth of recursion.
   *
   * @param startNode a node known to be in the subgraph
   */
  private fun addReachable(startNode: Node) {
    val nodeStack = ArrayDeque<Node>()
    nodeStack.add(startNode)
    while (!nodeStack.isEmpty()) {
      val node = nodeStack.removeLast()
      add(node, nodeStack)
    }
  }

  /**
   * Adds the argument node and all its out edges to the subgraph
   * @param node the node to add
   * @param nodeStack the current set of nodes being traversed
   */
  private fun add(node: Node, nodeStack: ArrayDeque<Node>) {
    node.setVisited(true)
    nodes.add(node)
    val i = (node.getEdges() as DirectedEdgeStar).iterator()
    while (i.hasNext()) {
      val de = i.next() as DirectedEdge
      dirEdgeList.add(de)
      val sym = de.getSym()!!
      val symNode = sym.getNode()!!
      /*
       * NOTE: this is a depth-first traversal of the graph.
       * This will cause a large depth of recursion.
       * It might be better to do a breadth-first traversal.
       */
      if (!symNode.isVisited()) nodeStack.addLast(symNode)
    }
  }

  private fun clearVisitedEdges() {
    val it = dirEdgeList.iterator()
    while (it.hasNext()) {
      val de = it.next()
      de.setVisited(false)
    }
  }

  fun computeDepth(outsideDepth: Int) {
    clearVisitedEdges()
    // find an outside edge to assign depth to
    val de = finder.getEdge()!!
    // right side of line returned by finder is on the outside
    de.setEdgeDepths(Position.RIGHT, outsideDepth)
    copySymDepths(de)

    computeDepths(de)
  }

  /**
   * Compute depths for all dirEdges via breadth-first traversal of nodes in graph
   * @param startEdge edge to start processing with
   */
  private fun computeDepths(startEdge: DirectedEdge) {
    val nodesVisited = HashSet<Node>()
    val nodeQueue = ArrayDeque<Node>()

    val startNode = startEdge.getNode()!!
    nodeQueue.addLast(startNode)
    nodesVisited.add(startNode)
    startEdge.setVisited(true)

    while (!nodeQueue.isEmpty()) {
      val n = nodeQueue.removeFirst()
      nodesVisited.add(n)
      // compute depths around node, starting at this edge since it has depths assigned
      computeNodeDepth(n)

      // add all adjacent nodes to process queue,
      // unless the node has been visited already
      val i = (n.getEdges() as DirectedEdgeStar).iterator()
      while (i.hasNext()) {
        val de = i.next() as DirectedEdge
        val sym = de.getSym()!!
        if (sym.isVisited()) continue
        val adjNode = sym.getNode()!!
        if (!nodesVisited.contains(adjNode)) {
          nodeQueue.addLast(adjNode)
          nodesVisited.add(adjNode)
        }
      }
    }
  }

  private fun computeNodeDepth(n: Node) {
    // find a visited dirEdge to start at
    var startEdge: DirectedEdge? = null
    val i = (n.getEdges() as DirectedEdgeStar).iterator()
    while (i.hasNext()) {
      val de = i.next() as DirectedEdge
      if (de.isVisited() || de.getSym()!!.isVisited()) {
        startEdge = de
        break
      }
    }

    // only compute string append if assertion would fail
    if (startEdge == null)
      throw TopologyException("unable to find edge to compute depths at " + n.getCoordinate())

    (n.getEdges() as DirectedEdgeStar).computeDepths(startEdge)

    // copy depths to sym edges
    val i2 = (n.getEdges() as DirectedEdgeStar).iterator()
    while (i2.hasNext()) {
      val de = i2.next() as DirectedEdge
      de.setVisited(true)
      copySymDepths(de)
    }
  }

  private fun copySymDepths(de: DirectedEdge) {
    val sym = de.getSym()!!
    sym.setDepth(Position.LEFT, de.getDepth(Position.RIGHT))
    sym.setDepth(Position.RIGHT, de.getDepth(Position.LEFT))
  }

  /**
   * Find all edges whose depths indicates that they are in the result area(s).
   * Since we want polygon shells to be
   * oriented CW, choose dirEdges with the interior of the result on the RHS.
   * Mark them as being in the result.
   * Interior Area edges are the result of dimensional collapses.
   * They do not form part of the result area boundary.
   */
  fun findResultEdges() {
    val it = dirEdgeList.iterator()
    while (it.hasNext()) {
      val de = it.next()
      /*
       * Select edges which have an interior depth on the RHS
       * and an exterior depth on the LHS.
       * Note that because of weird rounding effects there may be
       * edges which have negative depths!  Negative depths
       * count as "outside".
       */
      if (de.getDepth(Position.RIGHT) >= 1 &&
        de.getDepth(Position.LEFT) <= 0 &&
        !de.isInteriorAreaEdge()
      ) {
        de.setInResult(true)
      }
    }
  }

  /**
   * BufferSubgraphs are compared on the x-value of their rightmost Coordinate.
   */
  override fun compareTo(o: Any?): Int {
    val graph = o as BufferSubgraph
    if (this.rightMostCoord!!.x < graph.rightMostCoord!!.x) {
      return -1
    }
    if (this.rightMostCoord!!.x > graph.rightMostCoord!!.x) {
      return 1
    }
    return 0
  }
}
