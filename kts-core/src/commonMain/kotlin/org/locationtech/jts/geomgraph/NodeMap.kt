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

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Location

/**
 * A map of nodes, indexed by the coordinate of the node
 */
class NodeMap(private val nodeFact: NodeFactory) {

  //Map nodeMap = new HashMap();
  private val nodeMap: MutableMap<Coordinate, Node> = TreeMap()

  /**
   * Factory function - subclasses can override to create their own types of nodes
   */
  /*
  protected Node createNode(Coordinate coord)
  {
    return new Node(coord);
  }
  */
  /**
   * This method expects that a node has a coordinate value.
   * @param coord Coordinate
   * @return node for the provided coord
   */
  fun addNode(coord: Coordinate): Node {
    var node = nodeMap[coord]
    if (node == null) {
      node = nodeFact.createNode(coord)
      nodeMap[coord] = node
    }
    return node
  }

  fun addNode(n: Node): Node {
    val node = nodeMap[n.getCoordinate()!!]
    if (node == null) {
      nodeMap[n.getCoordinate()!!] = n
      return n
    }
    node.mergeLabel(n)
    return node
  }

  /**
   * Adds a node for the start point of this EdgeEnd
   * (if one does not already exist in this map).
   * Adds the EdgeEnd to the (possibly new) node.
   *
   * @param e EdgeEnd
   */
  fun add(e: EdgeEnd) {
    val p = e.getCoordinate()
    val n = addNode(p)
    n.add(e)
  }

  /**
   * Find coordinate.
   *
   * @param coord Coordinate to find
   * @return the node if found; null otherwise
   */
  fun find(coord: Coordinate): Node? = nodeMap[coord]

  fun iterator(): MutableIterator<Node> {
    return nodeMap.values.iterator()
  }

  fun values(): MutableCollection<Node> {
    return nodeMap.values
  }

  fun getBoundaryNodes(geomIndex: Int): MutableCollection<Node> {
    val bdyNodes: MutableCollection<Node> = ArrayList()
    val i = iterator()
    while (i.hasNext()) {
      val node = i.next()
      if (node.getLabel()!!.getLocation(geomIndex) == Location.BOUNDARY)
        bdyNodes.add(node)
    }
    return bdyNodes
  }

}
