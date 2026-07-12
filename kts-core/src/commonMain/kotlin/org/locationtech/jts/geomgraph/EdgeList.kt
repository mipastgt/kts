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

import org.locationtech.jts.util.TreeMap

import org.locationtech.jts.noding.OrientedCoordinateArray

/**
 * A EdgeList is a list of Edges.  It supports locating edges
 * that are pointwise equals to a target edge.
 * @version 1.7
 */
class EdgeList {

  private val edges: MutableList<Edge> = ArrayList()

  /**
   * An index of the edges, for fast lookup.
   */
  private val ocaMap: MutableMap<OrientedCoordinateArray, Edge> = TreeMap()

  /**
   * Insert an edge unless it is already in the list
   *
   * @param e Edge
   */
  fun add(e: Edge) {
    edges.add(e)
    val oca = OrientedCoordinateArray(e.getCoordinates())
    ocaMap[oca] = e
  }

  fun addAll(edgeColl: Collection<*>) {
    val i = edgeColl.iterator()
    while (i.hasNext()) {
      add(i.next() as Edge)
    }
  }

  fun getEdges(): MutableList<Edge> = edges

  /**
   * If there is an edge equal to e already in the list, return it.
   * Otherwise return null.
   * @param e Edge
   * @return  equal edge, if there is one already in the list
   * null otherwise
   */
  fun findEqualEdge(e: Edge): Edge? {
    val oca = OrientedCoordinateArray(e.getCoordinates())
    // will return null if no edge matches
    val matchEdge = ocaMap[oca]
    return matchEdge
  }

  fun iterator(): MutableIterator<Edge> = edges.iterator()

  fun get(i: Int): Edge = edges[i]

  /**
   * If the edge e is already in the list, return its index.
   * @param e Edge
   * @return  index, if e is already in the list
   * -1 otherwise
   */
  fun findEdgeIndex(e: Edge): Int {
    for (i in edges.indices) {
      if (edges[i] == e) return i
    }
    return -1
  }

}
