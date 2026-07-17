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

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.CoordinateList
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.Location

/**
 * Finds and builds overlay result lines from the overlay graph.
 *
 * @author Martin Davis
 */
class LineBuilder(
  inputGeom: InputGeometry,
  private val graph: OverlayGraph,
  private val hasResultArea: Boolean,
  private val opCode: Int,
  private val geometryFactory: GeometryFactory
) {

  private val inputAreaIndex: Int = inputGeom.getAreaIndex()

  /**
   * Indicates whether intersections are allowed to produce
   * heterogeneous results including proper boundary touches.
   */
  private var isAllowMixedResult = !OverlayNG.STRICT_MODE_DEFAULT

  /**
   * Allow lines created by area topology collapses
   * to appear in the result.
   */
  private var isAllowCollapseLines = !OverlayNG.STRICT_MODE_DEFAULT

  private val lines: MutableList<LineString> = ArrayList()

  fun setStrictMode(isStrictResultMode: Boolean) {
    isAllowCollapseLines = !isStrictResultMode
    isAllowMixedResult = !isStrictResultMode
  }

  fun getLines(): MutableList<LineString> {
    markResultLines()
    addResultLines()
    return lines
  }

  private fun markResultLines() {
    val edges = graph.getEdges()
    for (edge in edges) {
      /*
       * If the edge linework is already marked as in the result,
       * it is not included as a line.
       */
      if (edge.isInResultEither())
        continue
      if (isResultLine(edge.getLabel())) {
        edge.markInResultLine()
      }
    }
  }

  /**
   * Checks if the topology indicated by an edge label
   * determines that this edge should be part of a result line.
   *
   * @param lbl the label for an edge
   * @return true if the edge should be included in the result
   */
  private fun isResultLine(lbl: OverlayLabel): Boolean {
    /*
     * Omit edge which is a boundary of a single geometry
     * (i.e. not a collapse or line edge as well).
     */
    if (lbl.isBoundarySingleton()) return false

    /*
     * Omit edge which is a collapse along a boundary.
     */
    if (!isAllowCollapseLines && lbl.isBoundaryCollapse()) return false

    /*
     * Omit edge which is a collapse interior to its parent area.
     */
    if (lbl.isInteriorCollapse()) return false

    /*
     * For ops other than Intersection, omit a line edge
     * if it is interior to the other area.
     */
    if (opCode != OverlayNG.INTERSECTION) {
      /*
       * Omit collapsed edge in other area interior.
       */
      if (lbl.isCollapseAndNotPartInterior()) return false

      /*
       * If there is a result area, omit line edge inside it.
       */
      if (hasResultArea && lbl.isLineInArea(inputAreaIndex))
        return false
    }

    /*
     * Include line edge formed by touching area boundaries,
     * if enabled.
     */
    if (isAllowMixedResult &&
      opCode == OverlayNG.INTERSECTION && lbl.isBoundaryTouch()
    ) {
      return true
    }

    /**
     * Finally, determine included line edge
     * according to overlay op boolean logic.
     */
    val aLoc = effectiveLocation(lbl, 0)
    val bLoc = effectiveLocation(lbl, 1)
    val isInResult = OverlayNG.isResultOfOp(opCode, aLoc, bLoc)
    return isInResult
  }

  private fun addResultLines() {
    val edges = graph.getEdges()
    for (edge in edges) {
      if (!edge.isInResultLine()) continue
      if (edge.isVisited()) continue

      lines.add(toLine(edge))
      edge.markVisitedBoth()
    }
  }

  private fun toLine(edge: OverlayEdge): LineString {
    val isForward = edge.isForward()
    val pts = CoordinateList()
    pts.add(edge.orig(), false)
    edge.addCoordinates(pts)

    val ptsOut = pts.toCoordinateArray(isForward)
    val line = geometryFactory.createLineString(ptsOut)
    return line
  }

  //-----------------------------------------------
  //----  Maximal line extraction logic
  //-----------------------------------------------
  /**
   * NOT USED currently.
   * Instead the raw noded edges are output.
   */
  private fun addResultLinesMerged() {
    addResultLinesForNodes()
    addResultLinesRings()
  }

  private fun addResultLinesForNodes() {
    val edges = graph.getEdges()
    for (edge in edges) {
      if (!edge.isInResultLine()) continue
      if (edge.isVisited()) continue

      /*
       * Choose line start point as a node.
       * Nodes in the line graph are degree-1 or degree >= 3 edges.
       */
      if (degreeOfLines(edge) != 2) {
        lines.add(buildLine(edge))
      }
    }
  }

  /**
   * Adds lines which form rings (i.e. have only degree-2 vertices).
   */
  private fun addResultLinesRings() {
    val edges = graph.getEdges()
    for (edge in edges) {
      if (!edge.isInResultLine()) continue
      if (edge.isVisited()) continue

      lines.add(buildLine(edge))
    }
  }

  /**
   * Traverses edges from edgeStart which
   * lie in a single line (have degree = 2).
   *
   * @param node the start edge
   * @return the built line
   */
  private fun buildLine(node: OverlayEdge): LineString {
    val pts = CoordinateList()
    pts.add(node.orig(), false)

    val isForward = node.isForward()

    var e: OverlayEdge? = node
    do {
      val curr = e!!
      curr.markVisitedBoth()
      curr.addCoordinates(pts)

      // end line if next vertex is a node
      if (degreeOfLines(curr.symOE()) != 2) {
        break
      }
      e = nextLineEdgeUnvisited(curr.symOE())
      // e will be null if next edge has been visited, which indicates a ring
    } while (e != null)

    val ptsOut = pts.toCoordinateArray(isForward)

    val line = geometryFactory.createLineString(ptsOut)
    return line
  }

  companion object {
    /**
     * Determines the effective location for a line,
     * for the purpose of overlay operation evaluation.
     *
     * @param lbl label of line
     * @param geomIndex index of input geometry
     *
     * @return the effective location of the line
     */
    private fun effectiveLocation(lbl: OverlayLabel, geomIndex: Int): Int {
      if (lbl.isCollapse(geomIndex))
        return Location.INTERIOR
      if (lbl.isLine(geomIndex))
        return Location.INTERIOR
      return lbl.getLineLocation(geomIndex)
    }

    /**
     * Finds the next edge around a node which forms
     * part of a result line.
     *
     * @param node a line edge originating at the node to be scanned
     * @return the next line edge, or null if there is none
     */
    private fun nextLineEdgeUnvisited(node: OverlayEdge): OverlayEdge? {
      var e = node
      do {
        e = e.oNextOE()
        if (e.isVisited()) continue
        if (e.isInResultLine()) {
          return e
        }
      } while (e !== node)
      return null
    }

    /**
     * Computes the degree of the line edges incident on a node
     * @param node node to compute degree for
     * @return degree of the node line edges
     */
    private fun degreeOfLines(node: OverlayEdge): Int {
      var degree = 0
      var e = node
      do {
        if (e.isInResultLine()) {
          degree++
        }
        e = e.oNextOE()
      } while (e !== node)
      return degree
    }
  }
}
