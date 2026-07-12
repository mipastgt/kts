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

import kotlin.jvm.JvmStatic

import org.locationtech.jts.geom.Coordinate

/**
 * A node in a [PlanarGraph] is a location where 0 or more [Edge]s
 * meet. A node is connected to each of its incident Edges via an outgoing
 * DirectedEdge. Some clients using a `PlanarGraph` may want to
 * subclass `Node` to add their own application-specific
 * data and methods.
 *
 * @version 1.7
 */
open class Node(
  /** The location of this Node */
  protected var pt: Coordinate?,
  /** The collection of DirectedEdges that leave this Node */
  protected val deStar: DirectedEdgeStar
) : GraphComponent() {

  /**
   * Constructs a Node with the given location.
   */
  constructor(pt: Coordinate?) : this(pt, DirectedEdgeStar())

  /**
   * Returns the location of this Node.
   */
  open fun getCoordinate(): Coordinate? {
    return pt
  }

  /**
   * Adds an outgoing DirectedEdge to this Node.
   */
  open fun addOutEdge(de: DirectedEdge) {
    deStar.add(de)
  }

  /**
   * Returns the collection of DirectedEdges that leave this Node.
   */
  open fun getOutEdges(): DirectedEdgeStar {
    return deStar
  }

  /**
   * Returns the number of edges around this Node.
   */
  open fun getDegree(): Int {
    return deStar.getDegree()
  }

  /**
   * Returns the zero-based index of the given Edge, after sorting in ascending order
   * by angle with the positive x-axis.
   */
  open fun getIndex(edge: Edge): Int {
    return deStar.getIndex(edge)
  }

  /**
   * Removes a [DirectedEdge] incident on this node.
   * Does not change the state of the directed edge.
   */
  open fun remove(de: DirectedEdge) {
    deStar.remove(de)
  }

  /**
   * Removes this node from its containing graph.
   */
  internal fun remove() {
    pt = null
  }

  /**
   * Tests whether this node has been removed from its containing graph
   *
   * @return `true` if this node is removed
   */
  override fun isRemoved(): Boolean {
    return pt == null
  }

  companion object {
    /**
     * Returns all Edges that connect the two nodes (which are assumed to be different).
     */
    @JvmStatic
    fun getEdgesBetween(node0: Node, node1: Node): MutableCollection<*> {
      val edges0 = DirectedEdge.toEdges(node0.getOutEdges().getEdges())
      val commonEdges = HashSet<Any?>(edges0)
      val edges1 = DirectedEdge.toEdges(node1.getOutEdges().getEdges())
      commonEdges.retainAll(edges1)
      return commonEdges
    }
  }
}
