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

import org.locationtech.jts.edgegraph.HalfEdge
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.CoordinateArrays
import org.locationtech.jts.geom.CoordinateList
import org.locationtech.jts.io.WKTWriter

class OverlayEdge(
  orig: Coordinate,
  private val dirPt: Coordinate,
  /**
   * `true` indicates direction is forward along segString
   * `false` is reverse direction
   * The label must be interpreted accordingly.
   */
  private val direction: Boolean,
  private val label: OverlayLabel,
  private val pts: Array<Coordinate>
) : HalfEdge(orig) {

  private var inResultArea = false
  private var inResultLine = false
  private var visited = false

  /**
   * Link to next edge in the result ring.
   * The origin of the edge is the dest of this edge.
   */
  private var nextResultEdge: OverlayEdge? = null

  private var edgeRing: OverlayEdgeRing? = null

  private var maxEdgeRing: MaximalEdgeRing? = null

  private var nextResultMaxEdge: OverlayEdge? = null

  fun isForward(): Boolean {
    return direction
  }

  override fun directionPt(): Coordinate {
    return dirPt
  }

  fun getLabel(): OverlayLabel {
    return label
  }

  fun getLocation(index: Int, position: Int): Int {
    return label.getLocation(index, position, direction)
  }

  fun getCoordinate(): Coordinate {
    return orig()
  }

  fun getCoordinates(): Array<Coordinate> {
    return pts
  }

  fun getCoordinatesOriented(): Array<Coordinate> {
    if (direction) {
      return pts
    }
    val copy = pts.copyOf()
    CoordinateArrays.reverse(copy)
    return copy
  }

  /**
   * Adds the coordinates of this edge to the given list,
   * in the direction of the edge.
   * Duplicate coordinates are removed
   * (which means that this is safe to use for a path
   * of connected edges in the topology graph).
   *
   * @param coords the coordinate list to add to
   */
  fun addCoordinates(coords: CoordinateList) {
    val isFirstEdge = coords.size > 0
    if (direction) {
      var startIndex = 1
      if (isFirstEdge) startIndex = 0
      for (i in startIndex until pts.size) {
        coords.add(pts[i], false)
      }
    } else { // is backward
      var startIndex = pts.size - 2
      if (isFirstEdge) startIndex = pts.size - 1
      for (i in startIndex downTo 0) {
        coords.add(pts[i], false)
      }
    }
  }

  /**
   * Gets the symmetric pair edge of this edge.
   *
   * @return the symmetric pair edge
   */
  fun symOE(): OverlayEdge {
    return sym() as OverlayEdge
  }

  /**
   * Gets the next edge CCW around the origin of this edge,
   * with the same origin.
   * If the origin vertex has degree 1 then this is the edge itself.
   *
   * @return the next edge around the origin
   */
  fun oNextOE(): OverlayEdge {
    return oNext() as OverlayEdge
  }

  fun isInResultArea(): Boolean {
    return inResultArea
  }

  fun isInResultAreaBoth(): Boolean {
    return inResultArea && symOE().inResultArea
  }

  fun unmarkFromResultAreaBoth() {
    inResultArea = false
    symOE().inResultArea = false
  }

  fun markInResultArea() {
    inResultArea = true
  }

  fun markInResultAreaBoth() {
    inResultArea = true
    symOE().inResultArea = true
  }

  fun isInResultLine(): Boolean {
    return inResultLine
  }

  fun markInResultLine() {
    inResultLine = true
    symOE().inResultLine = true
  }

  fun isInResult(): Boolean {
    return inResultArea || inResultLine
  }

  fun isInResultEither(): Boolean {
    return isInResult() || symOE().isInResult()
  }

  fun setNextResult(e: OverlayEdge?) {
    // Assert: e.orig() == this.dest();
    nextResultEdge = e
  }

  fun nextResult(): OverlayEdge? {
    return nextResultEdge
  }

  fun isResultLinked(): Boolean {
    return nextResultEdge != null
  }

  fun setNextResultMax(e: OverlayEdge?) {
    // Assert: e.orig() == this.dest();
    nextResultMaxEdge = e
  }

  fun nextResultMax(): OverlayEdge? {
    return nextResultMaxEdge
  }

  fun isResultMaxLinked(): Boolean {
    return nextResultMaxEdge != null
  }

  fun isVisited(): Boolean {
    return visited
  }

  private fun markVisited() {
    visited = true
  }

  fun markVisitedBoth() {
    markVisited()
    symOE().markVisited()
  }

  fun setEdgeRing(edgeRing: OverlayEdgeRing?) {
    this.edgeRing = edgeRing
  }

  fun getEdgeRing(): OverlayEdgeRing? {
    return edgeRing
  }

  fun getEdgeRingMax(): MaximalEdgeRing? {
    return maxEdgeRing
  }

  fun setEdgeRingMax(maximalEdgeRing: MaximalEdgeRing?) {
    maxEdgeRing = maximalEdgeRing
  }

  override fun toString(): String {
    val orig = orig()
    val dest = dest()
    val dirPtStr = if (pts.size > 2) ", " + WKTWriter.format(directionPt()) else ""

    return "OE( " + WKTWriter.format(orig) +
        dirPtStr +
        " .. " + WKTWriter.format(dest) +
        " ) " +
        label.toString(direction) +
        resultSymbol() +
        " / Sym: " + symOE().getLabel().toString(symOE().direction) +
        symOE().resultSymbol()
  }

  private fun resultSymbol(): String {
    if (inResultArea) return " resA"
    if (inResultLine) return " resL"
    return ""
  }

  companion object {
    /**
     * Creates a single OverlayEdge.
     *
     * @return a new edge based on the given coordinates and direction
     */
    @JvmStatic
    fun createEdge(pts: Array<Coordinate>, lbl: OverlayLabel, direction: Boolean): OverlayEdge {
      val origin: Coordinate
      val dirPt: Coordinate
      if (direction) {
        origin = pts[0]
        dirPt = pts[1]
      } else {
        val ilast = pts.size - 1
        origin = pts[ilast]
        dirPt = pts[ilast - 1]
      }
      return OverlayEdge(origin, dirPt, direction, lbl, pts)
    }

    @JvmStatic
    fun createEdgePair(pts: Array<Coordinate>, lbl: OverlayLabel): OverlayEdge {
      val e0 = createEdge(pts, lbl, true)
      val e1 = createEdge(pts, lbl, false)
      e0.link(e1)
      return e0
    }

    /**
     * Gets a [Comparator] which sorts by the origin Coordinates.
     *
     * @return a Comparator sorting by origin coordinate
     */
    @JvmStatic
    fun nodeComparator(): Comparator<OverlayEdge> {
      return object : Comparator<OverlayEdge> {
        override fun compare(e1: OverlayEdge, e2: OverlayEdge): Int {
          return e1.orig().compareTo(e2.orig())
        }
      }
    }
  }
}
