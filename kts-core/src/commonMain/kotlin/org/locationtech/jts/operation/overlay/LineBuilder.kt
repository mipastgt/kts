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

import org.locationtech.jts.algorithm.PointLocator
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geomgraph.DirectedEdge
import org.locationtech.jts.geomgraph.DirectedEdgeStar
import org.locationtech.jts.geomgraph.Edge
import org.locationtech.jts.util.Assert

/**
 * Forms JTS LineStrings out of a the graph of [DirectedEdge]s
 * created by an [OverlayOp].
 *
 */
class LineBuilder(
  private val op: OverlayOp,
  private val geometryFactory: GeometryFactory,
  private val ptLocator: PointLocator
) {

  private val lineEdgesList: MutableList<Edge> = ArrayList()
  private val resultLineList: MutableList<LineString> = ArrayList()

  /**
   * @return a list of the LineStrings in the result of the specified overlay operation
   */
  fun build(opCode: Int): MutableList<LineString> {
    findCoveredLineEdges()
    collectLines(opCode)
    //labelIsolatedLines(lineEdgesList);
    buildLines(opCode)
    return resultLineList
  }

  /**
   * Find and mark L edges which are "covered" by the result area (if any).
   * L edges at nodes which also have A edges can be checked by checking
   * their depth at that node.
   * L edges at nodes which do not have A edges can be checked by doing a
   * point-in-polygon test with the previously computed result areas.
   */
  private fun findCoveredLineEdges() {
    // first set covered for all L edges at nodes which have A edges too
    val nodeit = op.getGraph().getNodes().iterator()
    while (nodeit.hasNext()) {
      val node = nodeit.next()
//node.print(System.out);
      (node.getEdges() as DirectedEdgeStar).findCoveredLineEdges()
    }

    /**
     * For all L edges which weren't handled by the above,
     * use a point-in-poly test to determine whether they are covered
     */
    val it = op.getGraph().getEdgeEnds().iterator()
    while (it.hasNext()) {
      val de = it.next() as DirectedEdge
      val e = de.getEdge()
      if (de.isLineEdge() && !e.isCoveredSet()) {
        val isCovered = op.isCoveredByA(de.getCoordinate())
        e.setCovered(isCovered)
      }
    }
  }

  private fun collectLines(opCode: Int) {
    val it = op.getGraph().getEdgeEnds().iterator()
    while (it.hasNext()) {
      val de = it.next() as DirectedEdge
      collectLineEdge(de, opCode, lineEdgesList)
      collectBoundaryTouchEdge(de, opCode, lineEdgesList)
    }
  }

  /**
   * Collect line edges which are in the result.
   * Line edges are in the result if they are not part of
   * an area boundary, if they are in the result of the overlay operation,
   * and if they are not covered by a result area.
   *
   * @param de the directed edge to test
   * @param opCode the overlap operation
   * @param edges the list of included line edges
   */
  private fun collectLineEdge(de: DirectedEdge, opCode: Int, edges: MutableList<Edge>) {
    val label = de.getLabel()!!
    val e = de.getEdge()
    // include L edges which are in the result
    if (de.isLineEdge()) {
      if (!de.isVisited() && OverlayOp.isResultOfOp(label, opCode) && !e.isCovered()) {
//Debug.println("de: " + de.getLabel());
//Debug.println("edge: " + e.getLabel());

        edges.add(e)
        de.setVisitedEdge(true)
      }
    }
  }

  /**
   * Collect edges from Area inputs which should be in the result but
   * which have not been included in a result area.
   * This happens ONLY:
   *
   *  * during an intersection when the boundaries of two
   * areas touch in a line segment
   *  *  OR as a result of a dimensional collapse.
   *
   */
  private fun collectBoundaryTouchEdge(de: DirectedEdge, opCode: Int, edges: MutableList<Edge>) {
    val label = de.getLabel()!!
    if (de.isLineEdge()) return  // only interested in area edges
    if (de.isVisited()) return  // already processed
    if (de.isInteriorAreaEdge()) return  // added to handle dimensional collapses
    if (de.getEdge().isInResult()) return  // if the edge linework is already included, don't include it again

    // sanity check for labelling of result edgerings
    Assert.isTrue(!(de.isInResult() || de.getSym()!!.isInResult()) || !de.getEdge().isInResult())

    // include the linework if it's in the result of the operation
    if (OverlayOp.isResultOfOp(label, opCode) && opCode == OverlayOp.INTERSECTION) {
      edges.add(de.getEdge())
      de.setVisitedEdge(true)
    }
  }

  private fun buildLines(opCode: Int) {
    val it = lineEdgesList.iterator()
    while (it.hasNext()) {
      val e = it.next()
      // Label label = e.getLabel();
      val line = geometryFactory.createLineString(e.getCoordinates())
      resultLineList.add(line)
      e.setInResult(true)
    }
  }

  private fun labelIsolatedLines(edgesList: MutableList<Edge>) {
    val it = edgesList.iterator()
    while (it.hasNext()) {
      val e = it.next()
      val label = e.getLabel()!!
//n.print(System.out);
      if (e.isIsolated()) {
        if (label.isNull(0))
          labelIsolatedLine(e, 0)
        else
          labelIsolatedLine(e, 1)
      }
    }
  }

  /**
   * Label an isolated node with its relationship to the target geometry.
   */
  private fun labelIsolatedLine(e: Edge, targetIndex: Int) {
    val loc = ptLocator.locate(e.getCoordinate()!!, op.getArgGeometry(targetIndex))
    e.getLabel()!!.setLocation(targetIndex, loc)
  }
}
