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
package org.locationtech.jts.planargraph

import org.locationtech.jts.geom.Coordinate

/**
 * Represents a directed graph which is embeddable in a planar surface.
 *
 */
abstract class PlanarGraph {
  protected val edges: MutableSet<Edge> = HashSet()
  protected val dirEdges: MutableSet<DirectedEdge> = HashSet()
  protected val nodeMap: NodeMap = NodeMap()

  /**
   * Returns the [Node] at the given location,
   * or null if no [Node] was there.
   *
   * @param pt the location to query
   * @return the node found
   * or `null` if this graph contains no node at the location
   */
  open fun findNode(pt: Coordinate): Node? {
    return nodeMap.find(pt)
  }

  /**
   * Adds a node to the map, replacing any that is already at that location.
   * Only subclasses can add Nodes, to ensure Nodes are of the right type.
   *
   * @param node the node to add
   */
  protected open fun add(node: Node) {
    nodeMap.add(node)
  }

  /**
   * Adds the Edge and its DirectedEdges with this PlanarGraph.
   * Assumes that the Edge has already been created with its associated DirectEdges.
   * Only subclasses can add Edges, to ensure the edges added are of the right class.
   */
  protected open fun add(edge: Edge) {
    edges.add(edge)
    add(edge.getDirEdge(0))
    add(edge.getDirEdge(1))
  }

  /**
   * Adds the Edge to this PlanarGraph; only subclasses can add DirectedEdges,
   * to ensure the edges added are of the right class.
   */
  protected open fun add(dirEdge: DirectedEdge) {
    dirEdges.add(dirEdge)
  }

  /**
   * Returns an Iterator over the Nodes in this PlanarGraph.
   */
  open fun nodeIterator(): MutableIterator<Node> {
    return nodeMap.iterator()
  }

  /**
   * Tests whether this graph contains the given [Edge]
   *
   * @param e the edge to query
   * @return `true` if the graph contains the edge
   */
  open fun contains(e: Edge): Boolean {
    return edges.contains(e)
  }

  /**
   * Tests whether this graph contains the given [DirectedEdge]
   *
   * @param de the directed edge to query
   * @return `true` if the graph contains the directed edge
   */
  open fun contains(de: DirectedEdge): Boolean {
    return dirEdges.contains(de)
  }

  open fun getNodes(): MutableCollection<Node> {
    return nodeMap.values()
  }

  /**
   * Returns an Iterator over the DirectedEdges in this PlanarGraph, in the order in which they
   * were added.
   *
   * @see add
   */
  open fun dirEdgeIterator(): MutableIterator<DirectedEdge> {
    return dirEdges.iterator()
  }

  /**
   * Returns an Iterator over the Edges in this PlanarGraph, in the order in which they
   * were added.
   *
   * @see add
   */
  open fun edgeIterator(): MutableIterator<Edge> {
    return edges.iterator()
  }

  /**
   * Returns the Edges that have been added to this PlanarGraph
   * @see add
   */
  open fun getEdges(): MutableCollection<Edge> {
    return edges
  }

  /**
   * Removes an [Edge] and its associated [DirectedEdge]s
   * from their from-Nodes and from the graph.
   */
  open fun remove(edge: Edge) {
    remove(edge.getDirEdge(0))
    remove(edge.getDirEdge(1))
    edges.remove(edge)
    edge.remove()
  }

  /**
   * Removes a [DirectedEdge] from its from-[Node] and from this graph.
   */
  open fun remove(de: DirectedEdge) {
    val sym = de.getSym()
    if (sym != null) sym.setSym(null)

    de.getFromNode().remove(de)
    de.remove()
    dirEdges.remove(de)
  }

  /**
   * Removes a node from the graph, along with any associated DirectedEdges and
   * Edges.
   */
  open fun remove(node: Node) {
    // unhook all directed edges
    val outEdges = node.getOutEdges().getEdges()
    for (de in outEdges) {
      val sym = de.getSym()
      // remove the diredge that points to this node
      if (sym != null) remove(sym)
      // remove this diredge from the graph collection
      dirEdges.remove(de)

      val edge = de.getEdge()
      if (edge != null) {
        edges.remove(edge)
      }
    }
    // remove the node from the graph
    nodeMap.remove(node.getCoordinate()!!)
    node.remove()
  }

  /**
   * Returns all Nodes with the given number of Edges around it.
   */
  open fun findNodesOfDegree(degree: Int): MutableList<Node> {
    val nodesFound = ArrayList<Node>()
    val i = nodeIterator()
    while (i.hasNext()) {
      val node = i.next()
      if (node.getDegree() == degree)
        nodesFound.add(node)
    }
    return nodesFound
  }
}
