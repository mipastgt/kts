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
package org.locationtech.jts.geomgraph

import kotlin.jvm.JvmStatic

import org.locationtech.jts.geom.Location
import org.locationtech.jts.geom.Position
import org.locationtech.jts.geom.TopologyException

/**
 */
open class DirectedEdge(edge: Edge, isForward: Boolean) : EdgeEnd(edge) {

  private var forward: Boolean = isForward
  private var inResult = false
  private var visited = false

  private var sym: DirectedEdge? = null // the symmetric edge
  private var next: DirectedEdge? = null  // the next edge in the edge ring for the polygon containing this edge
  private var nextMin: DirectedEdge? = null  // the next edge in the MinimalEdgeRing that contains this edge
  private var edgeRing: EdgeRing? = null  // the EdgeRing that this edge is part of
  private var minEdgeRing: EdgeRing? = null  // the MinimalEdgeRing that this edge is part of

  /**
   * The depth of each side (position) of this edge.
   * The 0 element of the array is never used.
   */
  private val depth = intArrayOf(0, -999, -999)

  init {
    if (isForward) {
      init(edge.getCoordinate(0), edge.getCoordinate(1))
    } else {
      val n = edge.getNumPoints() - 1
      init(edge.getCoordinate(n), edge.getCoordinate(n - 1))
    }
    computeDirectedLabel()
  }

  override fun getEdge(): Edge = edge
  open fun setInResult(isInResult: Boolean) { this.inResult = isInResult }
  open fun isInResult(): Boolean = inResult
  open fun isVisited(): Boolean = visited
  open fun setVisited(isVisited: Boolean) { this.visited = isVisited }
  open fun setEdgeRing(edgeRing: EdgeRing?) { this.edgeRing = edgeRing }
  open fun getEdgeRing(): EdgeRing? = edgeRing
  open fun setMinEdgeRing(minEdgeRing: EdgeRing?) { this.minEdgeRing = minEdgeRing }
  open fun getMinEdgeRing(): EdgeRing? = minEdgeRing
  open fun getDepth(position: Int): Int = depth[position]

  /**
   * Set depth for a position.
   *
   * You may also use [setEdgeDepths] to
   * update depth and opposite depth together.
   *
   * @param position Position to update
   * @param depthVal Depth at the provided position
   */
  open fun setDepth(position: Int, depthVal: Int) {
    if (depth[position] != -999) {
//      if (depth[position] != depthVal) {
//        Debug.print(this);
//      }
      if (depth[position] != depthVal)
        throw TopologyException("assigned depths do not match", getCoordinate())
      //Assert.isTrue(depth[position] == depthVal, "assigned depths do not match at " + getCoordinate());
    }
    depth[position] = depthVal
  }

  open fun getDepthDelta(): Int {
    var depthDelta = edge.getDepthDelta()
    if (!forward) depthDelta = -depthDelta
    return depthDelta
  }

  /**
   * Marks both DirectedEdges attached to a given Edge.
   *
   * This is used for edges corresponding to lines, which will only
   * appear oriented in a single direction in the result.
   *
   * @param isVisited True to mark edge as visited
   */
  open fun setVisitedEdge(isVisited: Boolean) {
    setVisited(isVisited)
    sym!!.setVisited(isVisited)
  }

  /**
   * Each Edge gives rise to a pair of symmetric DirectedEdges, in opposite
   * directions.
   * @return the DirectedEdge for the same Edge but in the opposite direction
   */
  open fun getSym(): DirectedEdge? = sym
  open fun isForward(): Boolean = forward
  open fun setSym(de: DirectedEdge?) {
    sym = de
  }
  open fun getNext(): DirectedEdge? = next
  open fun setNext(next: DirectedEdge?) { this.next = next }
  open fun getNextMin(): DirectedEdge? = nextMin
  open fun setNextMin(nextMin: DirectedEdge?) { this.nextMin = nextMin }

  /**
   * This edge is a line edge if
   *
   *  * at least one of the labels is a line label
   *  * any labels which are not line labels have all Locations = EXTERIOR
   *
   *
   * @return If edge is a line edge
   */
  open fun isLineEdge(): Boolean {
    val lbl = label!!
    val isLine = lbl.isLine(0) || lbl.isLine(1)
    val isExteriorIfArea0 =
      !lbl.isArea(0) || lbl.allPositionsEqual(0, Location.EXTERIOR)
    val isExteriorIfArea1 =
      !lbl.isArea(1) || lbl.allPositionsEqual(1, Location.EXTERIOR)

    return isLine && isExteriorIfArea0 && isExteriorIfArea1
  }

  /**
   * This is an interior Area edge if
   *
   *  * its label is an Area label for both Geometries
   *  * and for each Geometry both sides are in the interior.
   *
   *
   * @return true if this is an interior Area edge
   */
  open fun isInteriorAreaEdge(): Boolean {
    var isInteriorAreaEdge = true
    val lbl = label!!
    for (i in 0..1) {
      if (!(lbl.isArea(i) &&
          lbl.getLocation(i, Position.LEFT) == Location.INTERIOR &&
          lbl.getLocation(i, Position.RIGHT) == Location.INTERIOR)) {
        isInteriorAreaEdge = false
      }
    }
    return isInteriorAreaEdge
  }

  /**
   * Compute the label in the appropriate orientation for this DirEdge
   */
  private fun computeDirectedLabel() {
    label = Label(edge.getLabel()!!)
    if (!forward)
      label!!.flip()
  }

  /**
   * Set both edge depths.  One depth for a given side is provided.  The other is
   * computed depending on the Location transition and the depthDelta of the edge.
   *
   * @param position Position to update
   * @param depth Depth at the provided position
   */
  open fun setEdgeDepths(position: Int, depth: Int) {
    // get the depth transition delta from R to L for this directed Edge
    var depthDelta = getEdge().getDepthDelta()
    if (!forward) depthDelta = -depthDelta

    // if moving from L to R instead of R to L must change sign of delta
    var directionFactor = 1
    if (position == Position.LEFT)
      directionFactor = -1

    val oppositePos = Position.opposite(position)
    val delta = depthDelta * directionFactor
    //TESTINGint delta = depthDelta * DirectedEdge.depthFactor(loc, oppositeLoc);
    val oppositeDepth = depth + delta
    setDepth(position, depth)
    setDepth(oppositePos, oppositeDepth)
  }

  companion object {
    /**
     * Computes the factor for the change in depth when moving from one location to another.
     * E.g. if crossing from the [Location.INTERIOR] to the[Location.EXTERIOR]
     * the depth decreases, so the factor is -1.
     *
     * @param currLocation Current location
     * @param nextLocation Next location
     * @return change of depth moving from currLocation to nextLocation
     */
    @JvmStatic
    fun depthFactor(currLocation: Int, nextLocation: Int): Int {
      if (currLocation == Location.EXTERIOR && nextLocation == Location.INTERIOR)
        return 1
      else if (currLocation == Location.INTERIOR && nextLocation == Location.EXTERIOR)
        return -1
      return 0
    }
  }
}
