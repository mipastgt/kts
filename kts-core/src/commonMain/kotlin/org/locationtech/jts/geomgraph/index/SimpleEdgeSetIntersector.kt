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
package org.locationtech.jts.geomgraph.index

import org.locationtech.jts.geomgraph.Edge

/**
 * Finds all intersections in one or two sets of edges,
 * using the straightforward method of
 * comparing all segments.
 * This algorithm is too slow for production use, but is useful for testing purposes.
 * @version 1.7
 */
class SimpleEdgeSetIntersector : EdgeSetIntersector() {
  // statistics information
  private var nOverlaps = 0

  override fun computeIntersections(edges: List<*>, si: SegmentIntersector, testAllSegments: Boolean) {
    nOverlaps = 0

    val i0 = edges.iterator()
    while (i0.hasNext()) {
      val edge0 = i0.next() as Edge
      val i1 = edges.iterator()
      while (i1.hasNext()) {
        val edge1 = i1.next() as Edge
        if (testAllSegments || edge0 !== edge1)
          computeIntersects(edge0, edge1, si)
      }
    }
  }

  override fun computeIntersections(edges0: List<*>, edges1: List<*>, si: SegmentIntersector) {
    nOverlaps = 0

    val i0 = edges0.iterator()
    while (i0.hasNext()) {
      val edge0 = i0.next() as Edge
      val i1 = edges1.iterator()
      while (i1.hasNext()) {
        val edge1 = i1.next() as Edge
        computeIntersects(edge0, edge1, si)
      }
    }
  }

  /**
   * Performs a brute-force comparison of every segment in each Edge.
   * This has n^2 performance, and is about 100 times slower than using
   * monotone chains.
   */
  private fun computeIntersects(e0: Edge, e1: Edge, si: SegmentIntersector) {
    val pts0 = e0.getCoordinates()
    val pts1 = e1.getCoordinates()
    for (i0 in 0 until pts0.size - 1) {
      for (i1 in 0 until pts1.size - 1) {
        si.addIntersections(e0, i0, e1, i1)
      }
    }
  }
}
