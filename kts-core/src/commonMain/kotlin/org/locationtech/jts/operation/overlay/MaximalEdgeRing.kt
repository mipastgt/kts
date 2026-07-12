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
package org.locationtech.jts.operation.overlay

import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geomgraph.DirectedEdge
import org.locationtech.jts.geomgraph.DirectedEdgeStar
import org.locationtech.jts.geomgraph.EdgeRing

/**
 * A ring of [DirectedEdge]s which may contain nodes of degree &gt; 2.
 * A `MaximalEdgeRing` may represent two different spatial entities:
 *
 *  * a single polygon possibly containing inversions (if the ring is oriented CW)
 *  * a single hole possibly containing exversions (if the ring is oriented CCW)
 *
 * If the MaximalEdgeRing represents a polygon,
 * the interior of the polygon is strongly connected.
 *
 *
 * These are the form of rings used to define polygons under some spatial data models.
 * However, under the OGC SFS model, [MinimalEdgeRing]s are required.
 * A MaximalEdgeRing can be converted to a list of MinimalEdgeRings using the
 * [buildMinimalRings] method.
 *
 * @version 1.7
 * @see org.locationtech.jts.operation.overlay.MinimalEdgeRing
 */
class MaximalEdgeRing(start: DirectedEdge, geometryFactory: GeometryFactory) :
  EdgeRing(start, geometryFactory) {

  override fun getNext(de: DirectedEdge): DirectedEdge? {
    return de.getNext()
  }

  override fun setEdgeRing(de: DirectedEdge, er: EdgeRing) {
    de.setEdgeRing(er)
  }

  /**
   * For all nodes in this EdgeRing,
   * link the DirectedEdges at the node to form minimalEdgeRings
   */
  fun linkDirectedEdgesForMinimalEdgeRings() {
    var de: DirectedEdge? = startDe
    do {
      val node = de!!.getNode()!!
      (node.getEdges() as DirectedEdgeStar).linkMinimalDirectedEdges(this)
      de = de.getNext()
    } while (de !== startDe)
  }

  fun buildMinimalRings(): MutableList<EdgeRing> {
    val minEdgeRings: MutableList<EdgeRing> = ArrayList()
    var de: DirectedEdge? = startDe
    do {
      if (de!!.getMinEdgeRing() == null) {
        val minEr: EdgeRing = MinimalEdgeRing(de, geometryFactory)
        minEdgeRings.add(minEr)
      }
      de = de.getNext()
    } while (de !== startDe)
    return minEdgeRings
  }
}
