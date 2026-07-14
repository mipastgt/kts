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

import org.locationtech.jts.util.TreeMap

import org.locationtech.jts.geom.Coordinate

/**
 * A map of [Node]s, indexed by the coordinate of the node.
 *
 */
class NodeMap {

  private val nodeMap: MutableMap<Coordinate, Node> = TreeMap()

  /**
   * Adds a node to the map, replacing any that is already at that location.
   * @return the added node
   */
  fun add(n: Node): Node {
    nodeMap[n.getCoordinate()!!] = n
    return n
  }

  /**
   * Removes the Node at the given location, and returns it (or null if no Node was there).
   */
  fun remove(pt: Coordinate): Node? {
    return nodeMap.remove(pt)
  }

  /**
   * Returns the Node at the given location, or null if no Node was there.
   */
  fun find(coord: Coordinate): Node? {
    return nodeMap[coord]
  }

  /**
   * Returns an Iterator over the Nodes in this NodeMap, sorted in ascending order
   * by angle with the positive x-axis.
   */
  fun iterator(): MutableIterator<Node> {
    return nodeMap.values.iterator()
  }

  /**
   * Returns the Nodes in this NodeMap, sorted in ascending order
   * by angle with the positive x-axis.
   */
  fun values(): MutableCollection<Node> {
    return nodeMap.values
  }
}
