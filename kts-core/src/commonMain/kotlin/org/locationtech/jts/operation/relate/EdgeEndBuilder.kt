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
package org.locationtech.jts.operation.relate

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geomgraph.Edge
import org.locationtech.jts.geomgraph.EdgeEnd
import org.locationtech.jts.geomgraph.EdgeIntersection
import org.locationtech.jts.geomgraph.Label

/**
 * Computes the [EdgeEnd]s which arise from a noded [Edge].
 *
 */
internal class EdgeEndBuilder {

  fun computeEdgeEnds(edges: Iterator<*>): MutableList<EdgeEnd> {
    val l: MutableList<EdgeEnd> = ArrayList()
    val i = edges
    while (i.hasNext()) {
      val e = i.next() as Edge
      computeEdgeEnds(e, l)
    }
    return l
  }

  /**
   * Creates stub edges for all the intersections in this
   * Edge (if any) and inserts them into the graph.
   */
  fun computeEdgeEnds(edge: Edge, l: MutableList<EdgeEnd>) {
    val eiList = edge.getEdgeIntersectionList()
//Debug.print(eiList);
    // ensure that the list has entries for the first and last point of the edge
    eiList.addEndpoints()

    val it = eiList.iterator()
    var eiPrev: EdgeIntersection?
    var eiCurr: EdgeIntersection? = null
    // no intersections, so there is nothing to do
    if (!it.hasNext()) return
    var eiNext: EdgeIntersection? = it.next()
    do {
      eiPrev = eiCurr
      eiCurr = eiNext
      eiNext = null
      if (it.hasNext()) eiNext = it.next()

      if (eiCurr != null) {
        createEdgeEndForPrev(edge, l, eiCurr, eiPrev)
        createEdgeEndForNext(edge, l, eiCurr, eiNext)
      }
    } while (eiCurr != null)
  }

  /**
   * Create a EdgeStub for the edge before the intersection eiCurr.
   * The previous intersection is provided
   * in case it is the endpoint for the stub edge.
   * Otherwise, the previous point from the parent edge will be the endpoint.
   *
   * eiCurr will always be an EdgeIntersection, but eiPrev may be null.
   */
  private fun createEdgeEndForPrev(
    edge: Edge,
    l: MutableList<EdgeEnd>,
    eiCurr: EdgeIntersection,
    eiPrev: EdgeIntersection?
  ) {
    var iPrev = eiCurr.segmentIndex
    if (eiCurr.dist == 0.0) {
      // if at the start of the edge there is no previous edge
      if (iPrev == 0) return
      iPrev--
    }
    var pPrev = edge.getCoordinate(iPrev)
    // if prev intersection is past the previous vertex, use it instead
    if (eiPrev != null && eiPrev.segmentIndex >= iPrev)
      pPrev = eiPrev.coord

    val label = Label(edge.getLabel()!!)
    // since edgeStub is oriented opposite to it's parent edge, have to flip sides for edge label
    label.flip()
    val e = EdgeEnd(edge, eiCurr.coord, pPrev, label)
//e.print(System.out);  System.out.println();
    l.add(e)
  }

  /**
   * Create a StubEdge for the edge after the intersection eiCurr.
   * The next intersection is provided
   * in case it is the endpoint for the stub edge.
   * Otherwise, the next point from the parent edge will be the endpoint.
   *
   * eiCurr will always be an EdgeIntersection, but eiNext may be null.
   */
  private fun createEdgeEndForNext(
    edge: Edge,
    l: MutableList<EdgeEnd>,
    eiCurr: EdgeIntersection,
    eiNext: EdgeIntersection?
  ) {
    val iNext = eiCurr.segmentIndex + 1
    // if there is no next edge there is nothing to do
    if (iNext >= edge.getNumPoints() && eiNext == null) return

    var pNext = edge.getCoordinate(iNext)

    // if the next intersection is in the same segment as the current, use it as the endpoint
    if (eiNext != null && eiNext.segmentIndex == eiCurr.segmentIndex)
      pNext = eiNext.coord

    val e = EdgeEnd(edge, eiCurr.coord, pNext, Label(edge.getLabel()!!))
//Debug.println(e);
    l.add(e)
  }
}
