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
package org.locationtech.jts.operation.buffer

import org.locationtech.jts.algorithm.Orientation
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Position
import org.locationtech.jts.geomgraph.DirectedEdge
import org.locationtech.jts.geomgraph.DirectedEdgeStar
import org.locationtech.jts.util.Assert

/**
 * A RightmostEdgeFinder find the DirectedEdge in a list which has the highest coordinate,
 * and which is oriented L to R at that point. (I.e. the right side is on the RHS of the edge.)
 *
 */
internal class RightmostEdgeFinder {

  private var minIndex = -1
  private var minCoord: Coordinate? = null
  private var minDe: DirectedEdge? = null
  private var orientedDe: DirectedEdge? = null

  fun getEdge(): DirectedEdge? {
    return orientedDe
  }

  fun getCoordinate(): Coordinate? {
    return minCoord
  }

  fun findEdge(dirEdgeList: List<DirectedEdge>) {
    /**
     * Check all forward DirectedEdges only.  This is still general,
     * because each edge has a forward DirectedEdge.
     */
    val i = dirEdgeList.iterator()
    while (i.hasNext()) {
      val de = i.next()
      if (!de.isForward()) continue
      checkForRightmostCoordinate(de)
    }

    /**
     * If the rightmost point is a node, we need to identify which of
     * the incident edges is rightmost.
     */
    Assert.isTrue(minIndex != 0 || minCoord == minDe!!.getCoordinate(), "inconsistency in rightmost processing")
    if (minIndex == 0) {
      findRightmostEdgeAtNode()
    } else {
      findRightmostEdgeAtVertex()
    }
    /**
     * now check that the extreme side is the R side.
     * If not, use the sym instead.
     */
    orientedDe = minDe
    val rightmostSide = getRightmostSide(minDe!!, minIndex)
    if (rightmostSide == Position.LEFT) {
      orientedDe = minDe!!.getSym()
    }
  }

  private fun findRightmostEdgeAtNode() {
    val node = minDe!!.getNode()
    val star = node!!.getEdges() as DirectedEdgeStar
    minDe = star.getRightmostEdge()
    // the DirectedEdge returned by the previous call is not
    // necessarily in the forward direction. Use the sym edge if it isn't.
    if (!minDe!!.isForward()) {
      minDe = minDe!!.getSym()
      minIndex = minDe!!.getEdge().getCoordinates().size - 1
    }
  }

  private fun findRightmostEdgeAtVertex() {
    /**
     * The rightmost point is an interior vertex, so it has a segment on either side of it.
     * If these segments are both above or below the rightmost point, we need to
     * determine their relative orientation to decide which is rightmost.
     */
    val pts = minDe!!.getEdge().getCoordinates()
    Assert.isTrue(minIndex > 0 && minIndex < pts.size, "rightmost point expected to be interior vertex of edge")
    val pPrev = pts[minIndex - 1]
    val pNext = pts[minIndex + 1]
    val orientation = Orientation.index(minCoord!!, pNext, pPrev)
    var usePrev = false
    // both segments are below min point
    if (pPrev.y < minCoord!!.y && pNext.y < minCoord!!.y && orientation == Orientation.COUNTERCLOCKWISE) {
      usePrev = true
    } else if (pPrev.y > minCoord!!.y && pNext.y > minCoord!!.y && orientation == Orientation.CLOCKWISE) {
      usePrev = true
    }
    // if both segments are on the same side, do nothing - either is safe
    // to select as a rightmost segment
    if (usePrev) {
      minIndex = minIndex - 1
    }
  }

  private fun checkForRightmostCoordinate(de: DirectedEdge) {
    val coord = de.getEdge().getCoordinates()
    for (i in 0 until coord.size - 1) {
      // only check vertices which are the start or end point of a non-horizontal segment
      if (minCoord == null || coord[i].x > minCoord!!.x) {
        minDe = de
        minIndex = i
        minCoord = coord[i]
      }
    }
  }

  private fun getRightmostSide(de: DirectedEdge, index: Int): Int {
    var side = getRightmostSideOfSegment(de, index)
    if (side < 0) side = getRightmostSideOfSegment(de, index - 1)
    if (side < 0) {
      // reaching here can indicate that segment is horizontal
      // testing only
      minCoord = null
      checkForRightmostCoordinate(de)
    }
    return side
  }

  private fun getRightmostSideOfSegment(de: DirectedEdge, i: Int): Int {
    val e = de.getEdge()
    val coord = e.getCoordinates()

    if (i < 0 || i + 1 >= coord.size) return -1
    if (coord[i].y == coord[i + 1].y) return -1 // indicates edge is parallel to x-axis

    var pos = Position.LEFT
    if (coord[i].y < coord[i + 1].y) pos = Position.RIGHT
    return pos
  }
}
