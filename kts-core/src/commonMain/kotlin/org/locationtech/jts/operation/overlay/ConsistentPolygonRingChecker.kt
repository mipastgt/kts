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

import org.locationtech.jts.geom.Position
import org.locationtech.jts.geom.TopologyException
import org.locationtech.jts.geomgraph.DirectedEdge
import org.locationtech.jts.geomgraph.DirectedEdgeStar
import org.locationtech.jts.geomgraph.GeometryGraph
import org.locationtech.jts.geomgraph.PlanarGraph

/**
 * Tests whether the polygon rings in a [GeometryGraph]
 * are consistent.
 * Used for checking if Topology errors are present after noding.
 *
 * @author Martin Davis
 * @version 1.7
 */
class ConsistentPolygonRingChecker(private val graph: PlanarGraph) {

  fun checkAll() {
    check(OverlayOp.INTERSECTION)
    check(OverlayOp.DIFFERENCE)
    check(OverlayOp.UNION)
    check(OverlayOp.SYMDIFFERENCE)
  }

  /**
   * Tests whether the result geometry is consistent
   *
   * @throws TopologyException if inconsistent topology is found
   */
  fun check(opCode: Int) {
    val nodeit = graph.getNodeIterator()
    while (nodeit.hasNext()) {
      val node = nodeit.next()
      testLinkResultDirectedEdges(node.getEdges() as DirectedEdgeStar, opCode)
    }
  }

  private fun getPotentialResultAreaEdges(deStar: DirectedEdgeStar, opCode: Int): MutableList<DirectedEdge> {
//print(System.out);
    val resultAreaEdgeList: MutableList<DirectedEdge> = ArrayList()
    val it = deStar.iterator()
    while (it.hasNext()) {
      val de = it.next() as DirectedEdge
      if (isPotentialResultAreaEdge(de, opCode) || isPotentialResultAreaEdge(de.getSym()!!, opCode))
        resultAreaEdgeList.add(de)
    }
    return resultAreaEdgeList
  }

  private fun isPotentialResultAreaEdge(de: DirectedEdge, opCode: Int): Boolean {
    // mark all dirEdges with the appropriate label
    val label = de.getLabel()!!
    if (label.isArea() &&
      !de.isInteriorAreaEdge() &&
      OverlayOp.isResultOfOp(
        label.getLocation(0, Position.RIGHT),
        label.getLocation(1, Position.RIGHT),
        opCode
      )
    ) {
      return true
//Debug.print("in result "); Debug.println(de);
    }
    return false
  }

  private fun testLinkResultDirectedEdges(deStar: DirectedEdgeStar, opCode: Int) {
    // make sure edges are copied to resultAreaEdges list
    val ringEdges = getPotentialResultAreaEdges(deStar, opCode)
    // find first area edge (if any) to start linking at
    var firstOut: DirectedEdge? = null
    var incoming: DirectedEdge? = null
    var state = SCANNING_FOR_INCOMING
    // link edges in CCW order
    for (i in ringEdges.indices) {
      val nextOut = ringEdges[i]
      val nextIn = nextOut.getSym()!!

      // skip de's that we're not interested in
      if (!nextOut.getLabel()!!.isArea()) continue

      // record first outgoing edge, in order to link the last incoming edge
      if (firstOut == null && isPotentialResultAreaEdge(nextOut, opCode))
        firstOut = nextOut
      // assert: sym.isInResult() == false, since pairs of dirEdges should have been removed already

      when (state) {
        SCANNING_FOR_INCOMING -> {
          if (!isPotentialResultAreaEdge(nextIn, opCode)) continue
          incoming = nextIn
          state = LINKING_TO_OUTGOING
        }
        LINKING_TO_OUTGOING -> {
          if (!isPotentialResultAreaEdge(nextOut, opCode)) continue
          //incoming.setNext(nextOut);
          state = SCANNING_FOR_INCOMING
        }
      }
    }
//Debug.print(this);
    if (state == LINKING_TO_OUTGOING) {
//Debug.print(firstOut == null, this);
      if (firstOut == null)
        throw TopologyException("no outgoing dirEdge found", deStar.getCoordinate()!!)
    }
  }

  companion object {
    private const val SCANNING_FOR_INCOMING = 1
    private const val LINKING_TO_OUTGOING = 2
  }
}
