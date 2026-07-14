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
 * Represents an undirected edge of a [PlanarGraph]. An undirected edge
 * in fact simply acts as a central point of reference for two opposite
 * [DirectedEdge]s.
 *
 */
open class Edge : GraphComponent {

  /**
   * The two DirectedEdges associated with this Edge.
   * Index 0 is forward, 1 is reverse.
   */
  private var dirEdge: Array<DirectedEdge>? = null

  /**
   * Constructs an Edge whose DirectedEdges are not yet set. Be sure to call
   * [setDirectedEdges]
   */
  constructor()

  /**
   * Constructs an Edge initialized with the given DirectedEdges, and for each
   * DirectedEdge: sets the Edge, sets the symmetric DirectedEdge, and adds
   * this Edge to its from-Node.
   */
  constructor(de0: DirectedEdge, de1: DirectedEdge) {
    setDirectedEdges(de0, de1)
  }

  /**
   * Initializes this Edge's two DirectedEdges, and for each DirectedEdge: sets the
   * Edge, sets the symmetric DirectedEdge, and adds this Edge to its from-Node.
   */
  open fun setDirectedEdges(de0: DirectedEdge, de1: DirectedEdge) {
    dirEdge = arrayOf(de0, de1)
    de0.setEdge(this)
    de1.setEdge(this)
    de0.setSym(de1)
    de1.setSym(de0)
    de0.getFromNode()!!.addOutEdge(de0)
    de1.getFromNode()!!.addOutEdge(de1)
  }

  /**
   * Returns one of the DirectedEdges associated with this Edge.
   * @param i 0 or 1.  0 returns the forward directed edge, 1 returns the reverse
   */
  open fun getDirEdge(i: Int): DirectedEdge {
    return dirEdge!![i]
  }

  /**
   * Returns the [DirectedEdge] that starts from the given node, or null if the
   * node is not one of the two nodes associated with this Edge.
   */
  open fun getDirEdge(fromNode: Node): DirectedEdge? {
    if (dirEdge!![0].getFromNode() === fromNode) return dirEdge!![0]
    if (dirEdge!![1].getFromNode() === fromNode) return dirEdge!![1]
    // node not found
    return null
  }

  /**
   * If `node` is one of the two nodes associated with this Edge,
   * returns the other node; otherwise returns null.
   */
  open fun getOppositeNode(node: Node): Node? {
    if (dirEdge!![0].getFromNode() === node) return dirEdge!![0].getToNode()
    if (dirEdge!![1].getFromNode() === node) return dirEdge!![1].getToNode()
    // node not found
    return null
  }

  /**
   * Removes this edge from its containing graph.
   */
  internal fun remove() {
    this.dirEdge = null
  }

  /**
   * Tests whether this edge has been removed from its containing graph
   *
   * @return `true` if this edge is removed
   */
  override fun isRemoved(): Boolean {
    return dirEdge == null
  }
}
