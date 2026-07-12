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
 * A sorted collection of [DirectedEdge]s which leave a [Node]
 * in a [PlanarGraph].
 *
 * @version 1.7
 */
class DirectedEdgeStar {

  /**
   * The underlying list of outgoing DirectedEdges
   */
  private val outEdges: MutableList<DirectedEdge> = ArrayList()
  private var sorted = false

  /**
   * Adds a new member to this DirectedEdgeStar.
   */
  fun add(de: DirectedEdge) {
    outEdges.add(de)
    sorted = false
  }

  /**
   * Drops a member of this DirectedEdgeStar.
   */
  fun remove(de: DirectedEdge) {
    outEdges.remove(de)
  }

  /**
   * Returns an Iterator over the DirectedEdges, in ascending order by angle with the positive x-axis.
   */
  fun iterator(): MutableIterator<DirectedEdge> {
    sortEdges()
    return outEdges.iterator()
  }

  /**
   * Returns the number of edges around the Node associated with this DirectedEdgeStar.
   */
  fun getDegree(): Int {
    return outEdges.size
  }

  /**
   * Returns the coordinate for the node at which this star is based
   */
  fun getCoordinate(): Coordinate? {
    val it = iterator()
    if (!it.hasNext()) return null
    val e = it.next()
    return e.getCoordinate()
  }

  /**
   * Returns the DirectedEdges, in ascending order by angle with the positive x-axis.
   */
  fun getEdges(): MutableList<DirectedEdge> {
    sortEdges()
    return outEdges
  }

  private fun sortEdges() {
    if (!sorted) {
      outEdges.sort()
      sorted = true
    }
  }

  /**
   * Returns the zero-based index of the given Edge, after sorting in ascending order
   * by angle with the positive x-axis.
   */
  fun getIndex(edge: Edge): Int {
    sortEdges()
    for (i in outEdges.indices) {
      val de = outEdges[i]
      if (de.getEdge() === edge)
        return i
    }
    return -1
  }

  /**
   * Returns the zero-based index of the given DirectedEdge, after sorting in ascending order
   * by angle with the positive x-axis.
   */
  fun getIndex(dirEdge: DirectedEdge): Int {
    sortEdges()
    for (i in outEdges.indices) {
      val de = outEdges[i]
      if (de === dirEdge)
        return i
    }
    return -1
  }

  /**
   * Returns value of i modulo the number of edges in this DirectedEdgeStar
   * (i.e. the remainder when i is divided by the number of edges)
   *
   * @param i an integer (positive, negative or zero)
   */
  fun getIndex(i: Int): Int {
    var modi = i % outEdges.size
    if (modi < 0) modi += outEdges.size
    return modi
  }

  /**
   * Returns the [DirectedEdge] on the left-hand (CCW)
   * side of the given [DirectedEdge]
   * (which must be a member of this DirectedEdgeStar).
   */
  fun getNextEdge(dirEdge: DirectedEdge): DirectedEdge {
    val i = getIndex(dirEdge)
    return outEdges[getIndex(i + 1)]
  }

  /**
   * Returns the [DirectedEdge] on the right-hand (CW)
   * side of the given [DirectedEdge]
   * (which must be a member of this DirectedEdgeStar).
   */
  fun getNextCWEdge(dirEdge: DirectedEdge): DirectedEdge {
    val i = getIndex(dirEdge)
    return outEdges[getIndex(i - 1)]
  }
}
