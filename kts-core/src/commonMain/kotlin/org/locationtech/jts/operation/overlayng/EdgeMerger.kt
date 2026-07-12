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

import kotlin.jvm.JvmStatic

import org.locationtech.jts.util.Assert

/**
 * Performs merging on the noded edges of the input geometries.
 * Merging takes place on edges which are coincident
 * (i.e. have the same coordinate list, modulo direction).
 *
 * @author mdavis
 */
class EdgeMerger {

  companion object {
    @JvmStatic
    fun merge(edges: List<Edge>): MutableList<Edge> {
      // use a list to collect the final edges, to preserve order
      val mergedEdges = ArrayList<Edge>()
      val edgeMap = HashMap<EdgeKey, Edge>()

      for (edge in edges) {
        val edgeKey = EdgeKey.create(edge)
        val baseEdge = edgeMap[edgeKey]
        if (baseEdge == null) {
          // this is the first (and maybe only) edge for this line
          edgeMap[edgeKey] = edge
          mergedEdges.add(edge)
        } else {
          // found an existing edge

          // Assert: edges are identical (up to direction)
          // this is a fast (but incomplete) sanity check
          Assert.isTrue(
            baseEdge.size() == edge.size(),
            "Merge of edges of different sizes - probable noding error."
          )

          baseEdge.merge(edge)
        }
      }
      return mergedEdges
    }
  }
}
