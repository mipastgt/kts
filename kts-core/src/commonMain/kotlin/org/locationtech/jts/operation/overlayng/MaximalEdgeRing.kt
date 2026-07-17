/*
 * Copyright (c) 2020 Martin Davis
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
import org.locationtech.jts.geom.CoordinateList
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.TopologyException
import org.locationtech.jts.io.WKTWriter
import org.locationtech.jts.util.Assert

class MaximalEdgeRing(e: OverlayEdge) {

  private val startEdge: OverlayEdge = e

  init {
    attachEdges(e)
  }

  private fun attachEdges(startEdge: OverlayEdge) {
    var edge: OverlayEdge? = startEdge
    do {
      if (edge == null)
        throw TopologyException("Ring edge is null")
      if (edge.getEdgeRingMax() === this)
        throw TopologyException("Ring edge visited twice at " + edge.getCoordinate(), edge.getCoordinate())
      if (edge.nextResultMax() == null) {
        throw TopologyException("Ring edge missing at", edge.dest())
      }
      edge.setEdgeRingMax(this)
      edge = edge.nextResultMax()
    } while (edge !== startEdge)
  }

  fun buildMinimalRings(geometryFactory: GeometryFactory): MutableList<OverlayEdgeRing> {
    linkMinimalRings()

    val minEdgeRings = ArrayList<OverlayEdgeRing>()
    var e: OverlayEdge? = startEdge
    do {
      val curr = e!!
      if (curr.getEdgeRing() == null) {
        val minEr = OverlayEdgeRing(curr, geometryFactory)
        minEdgeRings.add(minEr)
      }
      e = curr.nextResultMax()
    } while (e !== startEdge)
    return minEdgeRings
  }

  private fun linkMinimalRings() {
    var e: OverlayEdge? = startEdge
    do {
      val curr = e!!
      linkMinRingEdgesAtNode(curr, this)
      e = curr.nextResultMax()
    } while (e !== startEdge)
  }

  override fun toString(): String {
    val pts = getCoordinates()
    return WKTWriter.toLineString(pts)
  }

  private fun getCoordinates(): Array<Coordinate> {
    val coords = CoordinateList()
    var edge = startEdge
    do {
      coords.add(edge.orig())
      if (edge.nextResultMax() == null) {
        break
      }
      edge = edge.nextResultMax()!!
    } while (edge !== startEdge)
    // add last coordinate
    coords.add(edge.dest())
    return coords.toCoordinateArray()
  }

  companion object {
    private const val STATE_FIND_INCOMING = 1
    private const val STATE_LINK_OUTGOING = 2

    /**
     * Traverses the star of edges originating at a node
     * and links consecutive result edges together
     * into **maximal** edge rings.
     *
     * PRECONDITIONS:
     * - This edge is in the result
     * - This edge is not yet linked
     * - The edge and its sym are NOT both marked as being in the result
     */
    @JvmStatic
    fun linkResultAreaMaxRingAtNode(nodeEdge: OverlayEdge) {
      Assert.isTrue(nodeEdge.isInResultArea(), "Attempt to link non-result edge")

      /**
       * Since the node edge is an out-edge,
       * make it the last edge to be linked
       * by starting at the next edge.
       * The node edge cannot be an in-edge as well,
       * but the next one may be the first in-edge.
       */
      val endOut = nodeEdge.oNextOE()
      var currOut = endOut
      var state = STATE_FIND_INCOMING
      var currResultIn: OverlayEdge? = null
      do {
        /*
         * If an edge is linked this node has already been processed
         * so can skip further processing
         */
        if (currResultIn != null && currResultIn.isResultMaxLinked())
          return

        when (state) {
          STATE_FIND_INCOMING -> {
            val currIn = currOut.symOE()
            if (currIn.isInResultArea()) {
              currResultIn = currIn
              state = STATE_LINK_OUTGOING
            }
          }
          STATE_LINK_OUTGOING -> {
            if (currOut.isInResultArea()) {
              // link the in edge to the out edge
              currResultIn!!.setNextResultMax(currOut)
              state = STATE_FIND_INCOMING
            }
          }
        }
        currOut = currOut.oNextOE()
      } while (currOut !== endOut)
      if (state == STATE_LINK_OUTGOING) {
        throw TopologyException("no outgoing edge found", nodeEdge.getCoordinate())
      }
    }

    /**
     * Links the edges of a [MaximalEdgeRing] around this node
     * into minimal edge rings ([OverlayEdgeRing]s).
     *
     * @param nodeEdge an edge originating at this node
     * @param maxRing the maximal ring to link
     */
    private fun linkMinRingEdgesAtNode(nodeEdge: OverlayEdge, maxRing: MaximalEdgeRing) {
      /**
       * The node edge is an out-edge,
       * so it is the first edge linked
       * with the next CCW in-edge
       */
      val endOut = nodeEdge
      var currMaxRingOut: OverlayEdge? = endOut
      var currOut = endOut.oNextOE()
      do {
        if (isAlreadyLinked(currOut.symOE(), maxRing))
          return

        currMaxRingOut = if (currMaxRingOut == null) {
          selectMaxOutEdge(currOut, maxRing)
        } else {
          linkMaxInEdge(currOut, currMaxRingOut, maxRing)
        }
        currOut = currOut.oNextOE()
      } while (currOut !== endOut)
      if (currMaxRingOut != null) {
        throw TopologyException("Unmatched edge found during min-ring linking", nodeEdge.getCoordinate())
      }
    }

    /**
     * Tests if an edge of the maximal edge ring is already linked into
     * a minimal [OverlayEdgeRing].
     *
     * @param edge an edge of a maximal edgering
     * @param maxRing the maximal edgering
     * @return true if the edge has already been linked into a minimal edgering.
     */
    private fun isAlreadyLinked(edge: OverlayEdge, maxRing: MaximalEdgeRing): Boolean {
      val isLinked = edge.getEdgeRingMax() === maxRing && edge.isResultLinked()
      return isLinked
    }

    private fun selectMaxOutEdge(currOut: OverlayEdge, maxEdgeRing: MaximalEdgeRing): OverlayEdge? {
      // select if currOut edge is part of this max ring
      if (currOut.getEdgeRingMax() === maxEdgeRing)
        return currOut
      // otherwise skip this edge
      return null
    }

    private fun linkMaxInEdge(
      currOut: OverlayEdge,
      currMaxRingOut: OverlayEdge,
      maxEdgeRing: MaximalEdgeRing
    ): OverlayEdge? {
      val currIn = currOut.symOE()
      // currIn is not in this max-edgering, so keep looking
      if (currIn.getEdgeRingMax() !== maxEdgeRing)
        return currMaxRingOut

      currIn.setNextResult(currMaxRingOut)
      // return null to indicate to scan for the next max-ring out-edge
      return null
    }
  }
}
