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

/**
 * A subgraph of a [PlanarGraph].
 * A subgraph may contain any subset of [Edge]s
 * from the parent graph.
 */
open class Subgraph(protected val parentGraph: PlanarGraph) {
  protected val edges: MutableSet<Edge> = HashSet()
  protected val dirEdges: MutableList<DirectedEdge> = ArrayList()
  protected val nodeMap: NodeMap = NodeMap()

  /**
   * Gets the [PlanarGraph] which this subgraph
   * is part of.
   *
   * @return the parent PlanarGraph
   */
  open fun getParent(): PlanarGraph {
    return parentGraph
  }

  /**
   * Adds an [Edge] to the subgraph.
   * The associated [DirectedEdge]s and [Node]s
   * are also added.
   *
   * @param e the edge to add
   */
  open fun add(e: Edge) {
    if (edges.contains(e)) return

    edges.add(e)
    dirEdges.add(e.getDirEdge(0))
    dirEdges.add(e.getDirEdge(1))
    nodeMap.add(e.getDirEdge(0).getFromNode())
    nodeMap.add(e.getDirEdge(1).getFromNode())
  }

  /**
   * Returns an [Iterator] over the [DirectedEdge]s in this graph,
   * in the order in which they were added.
   *
   * @return an iterator over the directed edges
   *
   * @see add
   */
  open fun dirEdgeIterator(): MutableIterator<DirectedEdge> {
    return dirEdges.iterator()
  }

  /**
   * Returns an [Iterator] over the [Edge]s in this graph,
   * in the order in which they were added.
   *
   * @return an iterator over the edges
   *
   * @see add
   */
  open fun edgeIterator(): MutableIterator<Edge> {
    return edges.iterator()
  }

  /**
   * Returns an [Iterator] over the [Node]s in this graph.
   * @return an iterator over the nodes
   */
  open fun nodeIterator(): MutableIterator<Node> {
    return nodeMap.iterator()
  }

  /**
   * Tests whether an [Edge] is contained in this subgraph
   * @param e the edge to test
   * @return `true` if the edge is contained in this subgraph
   */
  open fun contains(e: Edge): Boolean {
    return edges.contains(e)
  }
}
