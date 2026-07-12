/*
 * Copyright (c) 2019 Martin Davis.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.locationtech.jts.operation.overlayng

import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Point

/**
 * Extracts Point resultants from an overlay graph
 * created by an Intersection operation
 * between non-Point inputs.
 *
 * @author Martin Davis
 *
 * @see OverlayPoints
 */
class IntersectionPointBuilder(
  private val graph: OverlayGraph,
  private val geometryFactory: GeometryFactory
) {

  private val points: MutableList<Point> = ArrayList()

  /**
   * Controls whether lines created by area topology collapses
   * to participate in the result computation.
   * True provides the original JTS semantics.
   */
  private var isAllowCollapseLines = !OverlayNG.STRICT_MODE_DEFAULT

  fun setStrictMode(isStrictMode: Boolean) {
    isAllowCollapseLines = !isStrictMode
  }

  fun getPoints(): MutableList<Point> {
    addResultPoints()
    return points
  }

  private fun addResultPoints() {
    for (nodeEdge in graph.getNodeEdges()) {
      if (isResultPoint(nodeEdge)) {
        val pt = geometryFactory.createPoint(nodeEdge.getCoordinate().copy())
        points.add(pt)
      }
    }
  }

  /**
   * Tests if a node is a result point.
   * This is the case if the node is incident on edges from both
   * inputs, and none of the edges are themselves in the result.
   *
   * @param nodeEdge an edge originating at the node
   * @return true if this node is a result point
   */
  private fun isResultPoint(nodeEdge: OverlayEdge): Boolean {
    var isEdgeOfA = false
    var isEdgeOfB = false

    var edge = nodeEdge
    do {
      if (edge.isInResult()) return false
      val label = edge.getLabel()
      isEdgeOfA = isEdgeOfA or isEdgeOf(label, 0)
      isEdgeOfB = isEdgeOfB or isEdgeOf(label, 1)
      edge = edge.oNext() as OverlayEdge
    } while (edge !== nodeEdge)
    val isNodeInBoth = isEdgeOfA && isEdgeOfB
    return isNodeInBoth
  }

  private fun isEdgeOf(label: OverlayLabel, i: Int): Boolean {
    if (!isAllowCollapseLines && label.isBoundaryCollapse())
      return false
    return label.isBoundary(i) || label.isLine(i)
  }
}
