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

import org.locationtech.jts.geom.Location
import org.locationtech.jts.geom.Position
import org.locationtech.jts.geom.Quadrant
import org.locationtech.jts.geom.TopologyException
import org.locationtech.jts.util.Assert

/**
 * A DirectedEdgeStar is an ordered list of **outgoing** DirectedEdges around a node.
 * It supports labelling the edges as well as linking the edges to form both
 * MaximalEdgeRings and MinimalEdgeRings.
 *
 * @version 1.7
 */
open class DirectedEdgeStar : EdgeEndStar() {

  /**
   * A list of all outgoing edges in the result, in CCW order
   */
  private var resultAreaEdgeList: MutableList<DirectedEdge>? = null
  private var label: Label? = null

  /**
   * Insert a directed edge in the list
   */
  override fun insert(e: EdgeEnd) {
    val de = e as DirectedEdge
    insertEdgeEnd(de, de)
  }

  open fun getLabel(): Label? = label

  open fun getOutgoingDegree(): Int {
    var degree = 0
    val it = iterator()
    while (it.hasNext()) {
      val de = it.next() as DirectedEdge
      if (de.isInResult()) degree++
    }
    return degree
  }

  open fun getOutgoingDegree(er: EdgeRing): Int {
    var degree = 0
    val it = iterator()
    while (it.hasNext()) {
      val de = it.next() as DirectedEdge
      if (de.getEdgeRing() === er) degree++
    }
    return degree
  }

  open fun getRightmostEdge(): DirectedEdge? {
    val edges = getEdges()
    val size = edges.size
    if (size < 1) return null
    val de0 = edges[0] as DirectedEdge
    if (size == 1) return de0
    val deLast = edges[size - 1] as DirectedEdge

    val quad0 = de0.getQuadrant()
    val quad1 = deLast.getQuadrant()
    if (Quadrant.isNorthern(quad0) && Quadrant.isNorthern(quad1))
      return de0
    else if (!Quadrant.isNorthern(quad0) && !Quadrant.isNorthern(quad1))
      return deLast
    else {
      // edges are in different hemispheres - make sure we return one that is non-horizontal
      //Assert.isTrue(de0.getDy() != 0, "should never return horizontal edge!");
      if (de0.getDy() != 0.0)
        return de0
      else if (deLast.getDy() != 0.0)
        return deLast
    }
    Assert.shouldNeverReachHere("found two horizontal edges incident on node")
    return null
  }

  /**
   * Compute the labelling for all dirEdges in this star, as well
   * as the overall labelling
   */
  override fun computeLabelling(geomGraph: Array<GeometryGraph>) {
//Debug.print(this);
    super.computeLabelling(geomGraph)

    // determine the overall labelling for this DirectedEdgeStar
    // (i.e. for the node it is based at)
    label = Label(Location.NONE)
    val it = iterator()
    while (it.hasNext()) {
      val ee = it.next()
      val e = ee.getEdge()
      val eLabel = e.getLabel()!!
      for (i in 0..1) {
        val eLoc = eLabel.getLocation(i)
        if (eLoc == Location.INTERIOR || eLoc == Location.BOUNDARY)
          label!!.setLocation(i, Location.INTERIOR)
      }
    }
//Debug.print(this);
  }

  /**
   * For each dirEdge in the star,
   * merge the label from the sym dirEdge into the label
   */
  open fun mergeSymLabels() {
    val it = iterator()
    while (it.hasNext()) {
      val de = it.next() as DirectedEdge
      val label = de.getLabel()!!
      label.merge(de.getSym()!!.getLabel()!!)
    }
  }

  /**
   * Update incomplete dirEdge labels from the labelling for the node.
   *
   * @param nodeLabel Label to apply
   */
  open fun updateLabelling(nodeLabel: Label) {
    val it = iterator()
    while (it.hasNext()) {
      val de = it.next() as DirectedEdge
      val label = de.getLabel()!!
      label.setAllLocationsIfNull(0, nodeLabel.getLocation(0))
      label.setAllLocationsIfNull(1, nodeLabel.getLocation(1))
    }
  }

  private fun getResultAreaEdges(): MutableList<DirectedEdge> {
//print(System.out);
    resultAreaEdgeList?.let { return it }
    val list = ArrayList<DirectedEdge>()
    resultAreaEdgeList = list
    val it = iterator()
    while (it.hasNext()) {
      val de = it.next() as DirectedEdge
      if (de.isInResult() || de.getSym()!!.isInResult())
        list.add(de)
    }
    return list
  }

  /**
   * Traverse the star of DirectedEdges, linking the included edges together.
   * To link two dirEdges, the `next` pointer for an incoming dirEdge
   * is set to the next outgoing edge.
   *
   *
   * DirEdges are only linked if:
   *
   *  * they belong to an area (i.e. they have sides)
   *  * they are marked as being in the result
   *
   *
   *
   * Edges are linked in CCW order (the order they are stored).
   * This means that rings have their face on the Right
   * (in other words,
   * the topological location of the face is given by the RHS label of the DirectedEdge)
   *
   *
   * PRECONDITION: No pair of dirEdges are both marked as being in the result
   */
  open fun linkResultDirectedEdges() {
    // make sure edges are copied to resultAreaEdges list
    getResultAreaEdges()
    // find first area edge (if any) to start linking at
    var firstOut: DirectedEdge? = null
    var incoming: DirectedEdge? = null
    var state = SCANNING_FOR_INCOMING
    // link edges in CCW order
    for (i in resultAreaEdgeList!!.indices) {
      val nextOut = resultAreaEdgeList!![i]
      val nextIn = nextOut.getSym()!!

      // skip de's that we're not interested in
      if (!nextOut.getLabel()!!.isArea()) continue

      // record first outgoing edge, in order to link the last incoming edge
      if (firstOut == null && nextOut.isInResult()) firstOut = nextOut
      // assert: sym.isInResult() == false, since pairs of dirEdges should have been removed already

      when (state) {
        SCANNING_FOR_INCOMING -> {
          if (!nextIn.isInResult()) continue
          incoming = nextIn
          state = LINKING_TO_OUTGOING
        }
        LINKING_TO_OUTGOING -> {
          if (!nextOut.isInResult()) continue
          incoming!!.setNext(nextOut)
          state = SCANNING_FOR_INCOMING
        }
      }
    }
//Debug.print(this);
    if (state == LINKING_TO_OUTGOING) {
//Debug.print(firstOut == null, this);
      if (firstOut == null)
        throw TopologyException("no outgoing dirEdge found", getCoordinate()!!)
      //Assert.isTrue(firstOut != null, "no outgoing dirEdge found (at " + getCoordinate() );
      Assert.isTrue(firstOut.isInResult(), "unable to link last incoming dirEdge")
      incoming!!.setNext(firstOut)
    }
  }

  open fun linkMinimalDirectedEdges(er: EdgeRing) {
    // find first area edge (if any) to start linking at
    var firstOut: DirectedEdge? = null
    var incoming: DirectedEdge? = null
    var state = SCANNING_FOR_INCOMING
    // link edges in CW order
    for (i in resultAreaEdgeList!!.indices.reversed()) {
      val nextOut = resultAreaEdgeList!![i]
      val nextIn = nextOut.getSym()!!

      // record first outgoing edge, in order to link the last incoming edge
      if (firstOut == null && nextOut.getEdgeRing() === er) firstOut = nextOut

      when (state) {
        SCANNING_FOR_INCOMING -> {
          if (nextIn.getEdgeRing() !== er) continue
          incoming = nextIn
          state = LINKING_TO_OUTGOING
        }
        LINKING_TO_OUTGOING -> {
          if (nextOut.getEdgeRing() !== er) continue
          incoming!!.setNextMin(nextOut)
          state = SCANNING_FOR_INCOMING
        }
      }
    }
//print(System.out);
    if (state == LINKING_TO_OUTGOING) {
      Assert.isTrue(firstOut != null, "found null for first outgoing dirEdge")
      Assert.isTrue(firstOut!!.getEdgeRing() === er, "unable to link last incoming dirEdge")
      incoming!!.setNextMin(firstOut)
    }
  }

  open fun linkAllDirectedEdges() {
    getEdges()
    // find first area edge (if any) to start linking at
    var prevOut: DirectedEdge? = null
    var firstIn: DirectedEdge? = null
    // link edges in CW order
    for (i in edgeList!!.indices.reversed()) {
      val nextOut = edgeList!![i] as DirectedEdge
      val nextIn = nextOut.getSym()!!
      if (firstIn == null) firstIn = nextIn
      if (prevOut != null) nextIn.setNext(prevOut)
      // record outgoing edge, in order to link the last incoming edge
      prevOut = nextOut
    }
    firstIn!!.setNext(prevOut)
//Debug.print(this);
  }

  /**
   * Traverse the star of edges, maintaining the current location in the result
   * area at this node (if any).
   * If any L edges are found in the interior of the result, mark them as covered.
   */
  open fun findCoveredLineEdges() {
//Debug.print("findCoveredLineEdges");
//Debug.print(this);
    // Since edges are stored in CCW order around the node,
    // as we move around the ring we move from the right to the left side of the edge

    /**
     * Find first DirectedEdge of result area (if any).
     * The interior of the result is on the RHS of the edge,
     * so the start location will be:
     * - INTERIOR if the edge is outgoing
     * - EXTERIOR if the edge is incoming
     */
    var startLoc = Location.NONE
    run {
      val it = iterator()
      while (it.hasNext()) {
        val nextOut = it.next() as DirectedEdge
        val nextIn = nextOut.getSym()!!
        if (!nextOut.isLineEdge()) {
          if (nextOut.isInResult()) {
            startLoc = Location.INTERIOR
            break
          }
          if (nextIn.isInResult()) {
            startLoc = Location.EXTERIOR
            break
          }
        }
      }
    }
    // no A edges found, so can't determine if L edges are covered or not
    if (startLoc == Location.NONE) return

    /**
     * move around ring, keeping track of the current location
     * (Interior or Exterior) for the result area.
     * If L edges are found, mark them as covered if they are in the interior
     */
    var currLoc = startLoc
    val it = iterator()
    while (it.hasNext()) {
      val nextOut = it.next() as DirectedEdge
      val nextIn = nextOut.getSym()!!
      if (nextOut.isLineEdge()) {
        nextOut.getEdge().setCovered(currLoc == Location.INTERIOR)
//Debug.println(nextOut);
      } else {  // edge is an Area edge
        if (nextOut.isInResult())
          currLoc = Location.EXTERIOR
        if (nextIn.isInResult())
          currLoc = Location.INTERIOR
      }
    }
  }

  open fun computeDepths(de: DirectedEdge) {
    val edgeIndex = findIndex(de)
    val startDepth = de.getDepth(Position.LEFT)
    val targetLastDepth = de.getDepth(Position.RIGHT)
    // compute the depths from this edge up to the end of the edge array
    val nextDepth = computeDepths(edgeIndex + 1, edgeList!!.size, startDepth)
    // compute the depths for the initial part of the array
    val lastDepth = computeDepths(0, edgeIndex, nextDepth)
//Debug.print(lastDepth != targetLastDepth, this);
//Debug.print(lastDepth != targetLastDepth, "mismatch: " + lastDepth + " / " + targetLastDepth);
    if (lastDepth != targetLastDepth)
      throw TopologyException("depth mismatch at " + de.getCoordinate())
    //Assert.isTrue(lastDepth == targetLastDepth, "depth mismatch at " + de.getCoordinate());
  }

  /**
   * Compute the DirectedEdge depths for a subsequence of the edge array.
   *
   * @return the last depth assigned (from the R side of the last edge visited)
   */
  private fun computeDepths(startIndex: Int, endIndex: Int, startDepth: Int): Int {
    var currDepth = startDepth
    for (i in startIndex until endIndex) {
      val nextDe = edgeList!![i] as DirectedEdge
      nextDe.setEdgeDepths(Position.RIGHT, currDepth)
      currDepth = nextDe.getDepth(Position.LEFT)
    }
    return currDepth
  }

  companion object {
    private const val SCANNING_FOR_INCOMING = 1
    private const val LINKING_TO_OUTGOING = 2
  }
}
