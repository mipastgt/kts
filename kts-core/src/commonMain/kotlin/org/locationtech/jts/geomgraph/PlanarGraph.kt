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

/**
 */

import org.locationtech.jts.algorithm.Orientation
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Location
import org.locationtech.jts.geom.Quadrant

/**
 * The computation of the `IntersectionMatrix` relies on the use of a structure
 * called a "topology graph".  The topology graph contains nodes and edges
 * corresponding to the nodes and line segments of a `Geometry`. Each
 * node and edge in the graph is labeled with its topological location relative to
 * the source geometry.
 *
 *
 * Note that there is no requirement that points of self-intersection be a vertex.
 * Thus to obtain a correct topology graph, `Geometry`s must be
 * self-noded before constructing their graphs.
 *
 *
 * Two fundamental operations are supported by topology graphs:
 *
 *  * Computing the intersections between all the edges and nodes of a single graph
 *  * Computing the intersections between the edges and nodes of two different graphs
 *
 *
 */
open class PlanarGraph(nodeFact: NodeFactory) {

  protected val edges: MutableList<Edge> = ArrayList()
  protected val nodes: NodeMap = NodeMap(nodeFact)
  protected val edgeEndList: MutableList<EdgeEnd> = ArrayList()

  constructor() : this(NodeFactory())

  open fun getEdgeIterator(): MutableIterator<Edge> = edges.iterator()
  open fun getEdgeEnds(): MutableList<EdgeEnd> = edgeEndList

  open fun isBoundaryNode(geomIndex: Int, coord: Coordinate): Boolean {
    val node = nodes.find(coord) ?: return false
    val label = node.getLabel()
    if (label != null && label.getLocation(geomIndex) == Location.BOUNDARY) return true
    return false
  }

  protected open fun insertEdge(e: Edge) {
    edges.add(e)
  }

  open fun add(e: EdgeEnd) {
    nodes.add(e)
    edgeEndList.add(e)
  }

  open fun getNodeIterator(): MutableIterator<Node> = nodes.iterator()
  open fun getNodes(): MutableCollection<Node> = nodes.values()
  open fun addNode(node: Node): Node = nodes.addNode(node)
  open fun addNode(coord: Coordinate): Node = nodes.addNode(coord)

  /**
   * Find coordinate.
   *
   * @param coord Coordinate to find
   * @return the node if found; null otherwise
   */
  open fun find(coord: Coordinate): Node? = nodes.find(coord)

  /**
   * Add a set of edges to the graph.  For each edge two DirectedEdges
   * will be created.  DirectedEdges are NOT linked by this method.
   *
   * @param edgesToAdd Set of edges to add to the graph
   */
  open fun addEdges(edgesToAdd: List<*>) {
    // create all the nodes for the edges
    val it = edgesToAdd.iterator()
    while (it.hasNext()) {
      val e = it.next() as Edge
      edges.add(e)

      val de1 = DirectedEdge(e, true)
      val de2 = DirectedEdge(e, false)
      de1.setSym(de2)
      de2.setSym(de1)

      add(de1)
      add(de2)
    }
  }

  /**
   * Link the DirectedEdges at the nodes of the graph.
   * This allows clients to link only a subset of nodes in the graph, for
   * efficiency (because they know that only a subset is of interest).
   */
  open fun linkResultDirectedEdges() {
    val nodeit = nodes.iterator()
    while (nodeit.hasNext()) {
      val node = nodeit.next()
      (node.getEdges() as DirectedEdgeStar).linkResultDirectedEdges()
    }
  }

  /**
   * Link the DirectedEdges at the nodes of the graph.
   * This allows clients to link only a subset of nodes in the graph, for
   * efficiency (because they know that only a subset is of interest).
   */
  open fun linkAllDirectedEdges() {
    val nodeit = nodes.iterator()
    while (nodeit.hasNext()) {
      val node = nodeit.next()
      (node.getEdges() as DirectedEdgeStar).linkAllDirectedEdges()
    }
  }

  /**
   * Returns the EdgeEnd which has edge e as its base edge
   * (MD 18 Feb 2002 - this should return a pair of edges)
   *
   * @param e Edge
   * @return the edge, if found
   * `null` if the edge was not found
   */
  open fun findEdgeEnd(e: Edge): EdgeEnd? {
    val i = getEdgeEnds().iterator()
    while (i.hasNext()) {
      val ee = i.next()
      if (ee.getEdge() === e)
        return ee
    }
    return null
  }

  /**
   * Returns the edge whose first two coordinates are p0 and p1
   *
   * @param p0 first coordinate to match
   * @param p1 second coordinate to match
   * @return the edge, if found
   * `null` if the edge was not found
   */
  open fun findEdge(p0: Coordinate, p1: Coordinate): Edge? {
    for (i in edges.indices) {
      val e = edges[i]
      val eCoord = e.getCoordinates()
      if (p0 == eCoord[0] && p1 == eCoord[1])
        return e
    }
    return null
  }

  /**
   * Returns the edge which starts at p0 and whose first segment is
   * parallel to p1
   *
   * @param p0 Starting coordinate
   * @param p1 Coordinate used to establish direction
   * @return matching edge, if found
   * `null` if the edge was not found
   */
  open fun findEdgeInSameDirection(p0: Coordinate, p1: Coordinate): Edge? {
    for (i in edges.indices) {
      val e = edges[i]

      val eCoord = e.getCoordinates()
      if (matchInSameDirection(p0, p1, eCoord[0], eCoord[1]))
        return e

      if (matchInSameDirection(p0, p1, eCoord[eCoord.size - 1], eCoord[eCoord.size - 2]))
        return e
    }
    return null
  }

  /**
   * The coordinate pairs match if they define line segments lying in the same direction.
   * E.g. the segments are parallel and in the same quadrant
   * (as opposed to parallel and opposite!).
   */
  private fun matchInSameDirection(p0: Coordinate, p1: Coordinate, ep0: Coordinate, ep1: Coordinate): Boolean {
    if (p0 != ep0)
      return false

    if (Orientation.index(p0, p1, ep1) == Orientation.COLLINEAR &&
      Quadrant.quadrant(p0, p1) == Quadrant.quadrant(ep0, ep1))
      return true
    return false
  }

  companion object {
    /**
     * For nodes in the Collection, link the DirectedEdges at the node that are in the result.
     * This allows clients to link only a subset of nodes in the graph, for
     * efficiency (because they know that only a subset is of interest).
     *
     * @param nodes Collection of nodes
     */
    @JvmStatic
    fun linkResultDirectedEdges(nodes: Collection<*>) {
      val nodeit = nodes.iterator()
      while (nodeit.hasNext()) {
        val node = nodeit.next() as Node
        (node.getEdges() as DirectedEdgeStar).linkResultDirectedEdges()
      }
    }
  }
}
