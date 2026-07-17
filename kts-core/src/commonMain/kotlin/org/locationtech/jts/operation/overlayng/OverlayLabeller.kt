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


import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Location
import org.locationtech.jts.geom.Position
import org.locationtech.jts.geom.TopologyException
import org.locationtech.jts.io.WKTWriter
import org.locationtech.jts.util.Assert

/**
 * Implements the logic to compute the full labeling
 * for the edges in an [OverlayGraph].
 *
 * @author mdavis
 */
class OverlayLabeller(private val graph: OverlayGraph, private val inputGeometry: InputGeometry) {

  private val edges: Collection<OverlayEdge> = graph.getEdges()

  /**
   * Computes the topological labelling for the edges in the graph.
   */
  fun computeLabelling() {
    val nodes = graph.getNodeEdges()

    labelAreaNodeEdges(nodes)
    labelConnectedLinearEdges()

    /*
     * At this point collapsed edges labeled with location UNKNOWN
     * must be disconnected from the area edges of the parent.
     */
    labelCollapsedEdges()
    labelConnectedLinearEdges()

    labelDisconnectedEdges()
  }

  /**
   * Labels edges around nodes based on the arrangement
   * of incident area boundary edges.
   *
   * @param nodes the nodes to label
   */
  private fun labelAreaNodeEdges(nodes: Collection<OverlayEdge>) {
    for (nodeEdge in nodes) {
      propagateAreaLocations(nodeEdge, 0)
      if (inputGeometry.hasEdges(1)) {
        propagateAreaLocations(nodeEdge, 1)
      }
    }
  }

  /**
   * Scans around a node CCW, propagating the side labels
   * for a given area geometry to all edges (and their sym)
   * with unknown locations for that geometry.
   *
   * @param geomIndex the geometry to propagate locations for
   */
  fun propagateAreaLocations(nodeEdge: OverlayEdge, geomIndex: Int) {
    /*
     * Only propagate for area geometries
     */
    if (!inputGeometry.isArea(geomIndex)) return
    /*
     * No need to propagate if node has only one edge.
     * This handles dangling edges created by overlap limiting.
     */
    if (nodeEdge.degree() == 1) return

    val eStart = findPropagationStartEdge(nodeEdge, geomIndex)
    // no labelled edge found, so nothing to propagate
    if (eStart == null)
      return

    // initialize currLoc to location of L side
    var currLoc = eStart.getLocation(geomIndex, Position.LEFT)
    var e = eStart.oNextOE()

    do {
      val label = e.getLabel()
      if (!label.isBoundary(geomIndex)) {
        /*
         * If this is not a Boundary edge for this input area,
         * its location is now known relative to this input area
         */
        label.setLocationLine(geomIndex, currLoc)
      } else {
        // must be a boundary edge
        Assert.isTrue(label.hasSides(geomIndex))
        /**
         *  This is a boundary edge for the input area geom.
         *  Update the current location from its labels.
         *  Also check for topological consistency.
         */
        val locRight = e.getLocation(geomIndex, Position.RIGHT)
        if (locRight != currLoc) {
          throw TopologyException("side location conflict: arg " + geomIndex, e.getCoordinate())
        }
        val locLeft = e.getLocation(geomIndex, Position.LEFT)
        if (locLeft == Location.NONE) {
          Assert.shouldNeverReachHere("found single null side at " + e)
        }
        currLoc = locLeft
      }
      e = e.oNextOE()
    } while (e !== eStart)
  }

  private fun labelCollapsedEdges() {
    for (edge in edges) {
      if (edge.getLabel().isLineLocationUnknown(0)) {
        labelCollapsedEdge(edge, 0)
      }
      if (edge.getLabel().isLineLocationUnknown(1)) {
        labelCollapsedEdge(edge, 1)
      }
    }
  }

  private fun labelCollapsedEdge(edge: OverlayEdge, geomIndex: Int) {
    val label = edge.getLabel()
    if (!label.isCollapse(geomIndex)) return
    /*
     * This must be a collapsed edge which is disconnected
     * from any area edges (e.g. a fully collapsed shell or hole).
     * It can be labeled according to its parent source ring role.
     */
    label.setLocationCollapse(geomIndex)
  }

  /**
   * There can be edges which have unknown location
   * but are connected to a linear edge with known location.
   * In this case linear location is propagated to the connected edges.
   */
  private fun labelConnectedLinearEdges() {
    //TODO: can these be merged to avoid two scans?
    propagateLinearLocations(0)
    if (inputGeometry.hasEdges(1)) {
      propagateLinearLocations(1)
    }
  }

  /**
   * Performs a breadth-first graph traversal to find and label
   * connected linear edges.
   *
   * @param geomIndex the index of the input geometry to label
   */
  private fun propagateLinearLocations(geomIndex: Int) {
    // find located linear edges
    val linearEdges = findLinearEdgesWithLocation(edges, geomIndex)
    if (linearEdges.size <= 0) return

    val edgeStack: ArrayDeque<OverlayEdge> = ArrayDeque(linearEdges)
    val isInputLine = inputGeometry.isLine(geomIndex)
    // traverse connected linear edges, labeling unknown ones
    while (!edgeStack.isEmpty()) {
      val lineEdge = edgeStack.removeFirst()
      // for any edges around origin with unknown location for this geomIndex,
      // add those edges to stack to continue traversal
      propagateLinearLocationAtNode(lineEdge, geomIndex, isInputLine, edgeStack)
    }
  }

  /**
   * At this point there may still be edges which have unknown location
   * relative to an input geometry.
   */
  private fun labelDisconnectedEdges() {
    for (edge in edges) {
      if (edge.getLabel().isLineLocationUnknown(0)) {
        labelDisconnectedEdge(edge, 0)
      }
      if (edge.getLabel().isLineLocationUnknown(1)) {
        labelDisconnectedEdge(edge, 1)
      }
    }
  }

  /**
   * Determines the location of an edge relative to a target input geometry.
   *
   * @param edge the edge to label
   * @param geomIndex the input geometry to label against
   */
  private fun labelDisconnectedEdge(edge: OverlayEdge, geomIndex: Int) {
    val label = edge.getLabel()

    /*
     * if target geom is not an area then
     * edge must be EXTERIOR, since to be
     * INTERIOR it would have been labelled
     * when it was created.
     */
    if (!inputGeometry.isArea(geomIndex)) {
      label.setLocationAll(geomIndex, Location.EXTERIOR)
      return
    }

    /**
     * Locate edge in input area using a Point-In-Poly check.
     */
    val edgeLoc = locateEdgeBothEnds(geomIndex, edge)
    label.setLocationAll(geomIndex, edgeLoc)
  }

  /**
   * Determines the [Location] for an edge within an Area geometry
   * via point-in-polygon location.
   *
   * @param geomIndex the parent geometry index
   * @param edge the edge to locate
   * @return the location of the edge
   */
  private fun locateEdge(geomIndex: Int, edge: OverlayEdge): Int {
    val loc = inputGeometry.locatePointInArea(geomIndex, edge.orig())
    val edgeLoc = if (loc != Location.EXTERIOR) Location.INTERIOR else Location.EXTERIOR
    return edgeLoc
  }

  /**
   * Determines the [Location] for an edge within an Area geometry
   * via point-in-polygon location,
   * by checking that both endpoints are interior to the target geometry.
   *
   * @param geomIndex the parent geometry index
   * @param edge the edge to locate
   * @return the location of the edge
   */
  private fun locateEdgeBothEnds(geomIndex: Int, edge: OverlayEdge): Int {
    /*
     * To improve the robustness of the point location,
     * check both ends of the edge.
     * Edge is only labelled INTERIOR if both ends are.
     */
    val locOrig = inputGeometry.locatePointInArea(geomIndex, edge.orig())
    val locDest = inputGeometry.locatePointInArea(geomIndex, edge.dest())
    val isInt = locOrig != Location.EXTERIOR && locDest != Location.EXTERIOR
    val edgeLoc = if (isInt) Location.INTERIOR else Location.EXTERIOR
    return edgeLoc
  }

  fun markResultAreaEdges(overlayOpCode: Int) {
    for (edge in edges) {
      markInResultArea(edge, overlayOpCode)
    }
  }

  /**
   * Marks an edge which forms part of the boundary of the result area.
   *
   * @param e the edge to mark
   * @param overlayOpCode the overlay operation
   */
  fun markInResultArea(e: OverlayEdge, overlayOpCode: Int) {
    val label = e.getLabel()
    if (label.isBoundaryEither() &&
      OverlayNG.isResultOfOp(
        overlayOpCode,
        label.getLocationBoundaryOrLine(0, Position.RIGHT, e.isForward()),
        label.getLocationBoundaryOrLine(1, Position.RIGHT, e.isForward())
      )
    ) {
      e.markInResultArea()
    }
  }

  /**
   * Unmarks result area edges where the sym edge
   * is also marked as in the result.
   */
  fun unmarkDuplicateEdgesFromResultArea() {
    for (edge in edges) {
      if (edge.isInResultAreaBoth()) {
        edge.unmarkFromResultAreaBoth()
      }
    }
  }

  companion object {
    /**
     * Finds a boundary edge for this geom originating at the given
     * node, if one exists.
     *
     * @param nodeEdge an edge for this node
     * @param geomIndex the parent geometry index
     * @return a boundary edge, or null if no boundary edge exists
     */
    private fun findPropagationStartEdge(nodeEdge: OverlayEdge, geomIndex: Int): OverlayEdge? {
      var eStart = nodeEdge
      do {
        val label = eStart.getLabel()
        if (label.isBoundary(geomIndex)) {
          Assert.isTrue(label.hasSides(geomIndex))
          return eStart
        }
        eStart = eStart.oNext() as OverlayEdge
      } while (eStart !== nodeEdge)
      return null
    }

    private fun propagateLinearLocationAtNode(
      eNode: OverlayEdge,
      geomIndex: Int,
      isInputLine: Boolean,
      edgeStack: ArrayDeque<OverlayEdge>
    ) {
      val lineLoc = eNode.getLabel().getLineLocation(geomIndex)
      /*
       * If the parent geom is a Line
       * then only propagate EXTERIOR locations.
       */
      if (isInputLine && lineLoc != Location.EXTERIOR) return

      var e = eNode.oNextOE()
      do {
        val label = e.getLabel()
        if (label.isLineLocationUnknown(geomIndex)) {
          /*
           * If edge is not a boundary edge,
           * its location is now known for this area
           */
          label.setLocationLine(geomIndex, lineLoc)

          /*
           * Add sym edge to stack for graph traversal
           * (Don't add e itself, since e origin node has now been scanned)
           */
          edgeStack.addFirst(e.symOE())
        }
        e = e.oNextOE()
      } while (e !== eNode)
    }

    /**
     * Finds all OverlayEdges which are linear
     * (i.e. line or collapsed) and have a known location
     * for the given input geometry.
     *
     * @param geomIndex the index of the input geometry
     * @return list of linear edges with known location
     */
    private fun findLinearEdgesWithLocation(
      edges: Collection<OverlayEdge>,
      geomIndex: Int
    ): MutableList<OverlayEdge> {
      val linearEdges = ArrayList<OverlayEdge>()
      for (edge in edges) {
        val lbl = edge.getLabel()
        // keep if linear with known location
        if (lbl.isLinear(geomIndex) && !lbl.isLineLocationUnknown(geomIndex)) {
          linearEdges.add(edge)
        }
      }
      return linearEdges
    }

    @JvmStatic
    fun toString(nodeEdge: OverlayEdge): String {
      val orig = nodeEdge.orig()
      val sb = StringBuilder()
      sb.append("Node( " + WKTWriter.format(orig) + " )" + "\n")
      var e = nodeEdge
      do {
        sb.append("  -> " + e)
        if (e.isResultLinked()) {
          sb.append(" Link: ")
          sb.append(e.nextResult())
        }
        sb.append("\n")
        e = e.oNextOE()
      } while (e !== nodeEdge)
      return sb.toString()
    }
  }
}
