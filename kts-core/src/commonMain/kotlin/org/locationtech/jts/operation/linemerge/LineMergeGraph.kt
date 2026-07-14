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
package org.locationtech.jts.operation.linemerge

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.CoordinateArrays
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.planargraph.DirectedEdge
import org.locationtech.jts.planargraph.Edge
import org.locationtech.jts.planargraph.Node
import org.locationtech.jts.planargraph.PlanarGraph

/**
 * A planar graph of edges that is analyzed to sew the edges together.
 *
 */
class LineMergeGraph : PlanarGraph() {
  /**
   * Adds an Edge, DirectedEdges, and Nodes for the given LineString representation
   * of an edge.
   * Empty lines or lines with all coordinates equal are not added.
   *
   * @param lineString the linestring to add to the graph
   */
  fun addEdge(lineString: LineString) {
    if (lineString.isEmpty()) {
      return
    }

    val coordinates = CoordinateArrays.removeRepeatedPoints(lineString.getCoordinates())

    // don't add lines with all coordinates equal
    if (coordinates.size <= 1) return

    val startCoordinate = coordinates[0]
    val endCoordinate = coordinates[coordinates.size - 1]
    val startNode = getNode(startCoordinate)
    val endNode = getNode(endCoordinate)
    val directedEdge0: DirectedEdge = LineMergeDirectedEdge(
      startNode, endNode,
      coordinates[1], true
    )
    val directedEdge1: DirectedEdge = LineMergeDirectedEdge(
      endNode, startNode,
      coordinates[coordinates.size - 2], false
    )
    val edge: Edge = LineMergeEdge(lineString)
    edge.setDirectedEdges(directedEdge0, directedEdge1)
    add(edge)
  }

  private fun getNode(coordinate: Coordinate): Node {
    var node = findNode(coordinate)
    if (node == null) {
      node = Node(coordinate)
      add(node)
    }

    return node
  }
}
