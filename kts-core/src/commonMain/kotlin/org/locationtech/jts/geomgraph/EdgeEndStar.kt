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

import kotlin.jvm.JvmField

import org.locationtech.jts.util.TreeMap

import org.locationtech.jts.algorithm.BoundaryNodeRule
import org.locationtech.jts.algorithm.locate.SimplePointInAreaLocator
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Location
import org.locationtech.jts.geom.Position
import org.locationtech.jts.geom.TopologyException
import org.locationtech.jts.util.Assert

/**
 * A EdgeEndStar is an ordered list of EdgeEnds around a node.
 * They are maintained in CCW order (starting with the positive x-axis) around the node
 * for efficient lookup and topology building.
 *
 */
abstract class EdgeEndStar {

  /**
   * A map which maintains the edges in sorted order around the node
   */
  @JvmField
  protected val edgeMap: MutableMap<EdgeEnd, EdgeEnd> = TreeMap()

  /**
   * A list of all outgoing edges in the result, in CCW order
   */
  protected var edgeList: MutableList<EdgeEnd>? = null

  /**
   * The location of the point for this star in Geometry i Areas
   */
  private val ptInAreaLocation = intArrayOf(Location.NONE, Location.NONE)

  /**
   * Insert a EdgeEnd into this EdgeEndStar
   * @param e EdgeEnd
   */
  abstract fun insert(e: EdgeEnd)

  /**
   * Insert an EdgeEnd into the map, and clear the edgeList cache,
   * since the list of edges has now changed
   * @param e EdgeEnd
   * @param obj Object
   */
  protected open fun insertEdgeEnd(e: EdgeEnd, obj: EdgeEnd) {
    edgeMap[e] = obj
    edgeList = null  // edge list has changed - clear the cache
  }

  /**
   * @return the coordinate for the node this star is based at
   */
  open fun getCoordinate(): Coordinate? {
    val it = iterator()
    if (!it.hasNext()) return null
    val e = it.next()
    return e.getCoordinate()
  }

  open fun getDegree(): Int {
    return edgeMap.size
  }

  /**
   * Iterator access to the ordered list of edges is optimized by
   * copying the map collection to a list.  (This assumes that
   * once an iterator is requested, it is likely that insertion into
   * the map is complete).
   *
   * @return access to ordered list of edges
   */
  open fun iterator(): MutableIterator<EdgeEnd> {
    return getEdges().iterator()
  }

  open fun getEdges(): MutableList<EdgeEnd> {
    if (edgeList == null) {
      edgeList = ArrayList(edgeMap.values)
    }
    return edgeList!!
  }

  open fun getNextCW(ee: EdgeEnd): EdgeEnd {
    getEdges()
    val i = edgeList!!.indexOf(ee)
    var iNextCW = i - 1
    if (i == 0)
      iNextCW = edgeList!!.size - 1
    return edgeList!![iNextCW]
  }

  open fun computeLabelling(geomGraph: Array<GeometryGraph>) {
    computeEdgeEndLabels(geomGraph[0].getBoundaryNodeRule())
    // Propagate side labels  around the edges in the star
    // for each parent Geometry
//Debug.print(this);
    propagateSideLabels(0)
//Debug.print(this);
//Debug.printIfWatch(this);
    propagateSideLabels(1)
//Debug.print(this);
//Debug.printIfWatch(this);

    /**
     * If there are edges that still have null labels for a geometry
     * this must be because there are no area edges for that geometry incident on this node.
     * In this case, to label the edge for that geometry we must test whether the
     * edge is in the interior of the geometry.
     * ...
     */
    val hasDimensionalCollapseEdge = booleanArrayOf(false, false)
    run {
      val it = iterator()
      while (it.hasNext()) {
        val e = it.next()
        val label = e.getLabel()!!
        for (geomi in 0..1) {
          if (label.isLine(geomi) && label.getLocation(geomi) == Location.BOUNDARY)
            hasDimensionalCollapseEdge[geomi] = true
        }
      }
    }
//Debug.print(this);
    val it = iterator()
    while (it.hasNext()) {
      val e = it.next()
      val label = e.getLabel()!!
//Debug.println(e);
      for (geomi in 0..1) {
        if (label.isAnyNull(geomi)) {
          var loc = Location.NONE
          if (hasDimensionalCollapseEdge[geomi]) {
            loc = Location.EXTERIOR
          } else {
            val p = e.getCoordinate()
            loc = getLocation(geomi, p, geomGraph)
          }
          label.setAllLocationsIfNull(geomi, loc)
        }
      }
//Debug.println(e);
    }
//Debug.print(this);
//Debug.printIfWatch(this);
  }

  private fun computeEdgeEndLabels(boundaryNodeRule: BoundaryNodeRule) {
    // Compute edge label for each EdgeEnd
    val it = iterator()
    while (it.hasNext()) {
      val ee = it.next()
      ee.computeLabel(boundaryNodeRule)
    }
  }

  private fun getLocation(geomIndex: Int, p: Coordinate, geom: Array<GeometryGraph>): Int {
    // compute location only on demand
    if (ptInAreaLocation[geomIndex] == Location.NONE) {
      ptInAreaLocation[geomIndex] = SimplePointInAreaLocator.locate(p, geom[geomIndex].getGeometry()!!)
    }
    return ptInAreaLocation[geomIndex]
  }

  open fun isAreaLabelsConsistent(geomGraph: GeometryGraph): Boolean {
    computeEdgeEndLabels(geomGraph.getBoundaryNodeRule())
    return checkAreaLabelsConsistent(0)
  }

  private fun checkAreaLabelsConsistent(geomIndex: Int): Boolean {
    // Since edges are stored in CCW order around the node,
    // As we move around the ring we move from the right to the left side of the edge
    val edges = getEdges()
    // if no edges, trivially consistent
    if (edges.size <= 0)
      return true
    // initialize startLoc to location of last L side (if any)
    val lastEdgeIndex = edges.size - 1
    val startLabel = edges[lastEdgeIndex].getLabel()!!
    val startLoc = startLabel.getLocation(geomIndex, Position.LEFT)
    Assert.isTrue(startLoc != Location.NONE, "Found unlabelled area edge")

    var currLoc = startLoc
    val it = iterator()
    while (it.hasNext()) {
      val e = it.next()
      val label = e.getLabel()!!
      // we assume that we are only checking a area
      Assert.isTrue(label.isArea(geomIndex), "Found non-area edge")
      val leftLoc = label.getLocation(geomIndex, Position.LEFT)
      val rightLoc = label.getLocation(geomIndex, Position.RIGHT)
//System.out.println(leftLoc + " " + rightLoc);
//Debug.print(this);
      // check that edge is really a boundary between inside and outside!
      if (leftLoc == rightLoc) {
        return false
      }
      // check side location conflict
      //Assert.isTrue(rightLoc == currLoc, "side location conflict " + locStr);
      if (rightLoc != currLoc) {
//Debug.print(this);
        return false
      }
      currLoc = leftLoc
    }
    return true
  }

  private fun propagateSideLabels(geomIndex: Int) {
    // Since edges are stored in CCW order around the node,
    // As we move around the ring we move from the right to the left side of the edge
    var startLoc = Location.NONE

    // initialize loc to location of last L side (if any)
//System.out.println("finding start location");
    run {
      val it = iterator()
      while (it.hasNext()) {
        val e = it.next()
        val label = e.getLabel()!!
        if (label.isArea(geomIndex) && label.getLocation(geomIndex, Position.LEFT) != Location.NONE)
          startLoc = label.getLocation(geomIndex, Position.LEFT)
      }
    }

    // no labelled sides found, so no labels to propagate
    if (startLoc == Location.NONE) return

    var currLoc = startLoc
    val it = iterator()
    while (it.hasNext()) {
      val e = it.next()
      val label = e.getLabel()!!
      // set null ON values to be in current location
      if (label.getLocation(geomIndex, Position.ON) == Location.NONE)
        label.setLocation(geomIndex, Position.ON, currLoc)
      // set side labels (if any)
      if (label.isArea(geomIndex)) {
        val leftLoc = label.getLocation(geomIndex, Position.LEFT)
        val rightLoc = label.getLocation(geomIndex, Position.RIGHT)
        // if there is a right location, that is the next location to propagate
        if (rightLoc != Location.NONE) {
//Debug.print(rightLoc != currLoc, this);
          if (rightLoc != currLoc)
            throw TopologyException("side location conflict", e.getCoordinate())
          if (leftLoc == Location.NONE) {
            Assert.shouldNeverReachHere("found single null side (at " + e.getCoordinate() + ")")
          }
          currLoc = leftLoc
        } else {
          /** RHS is null - LHS must be null too.
           * This must be an edge from the other geometry, which has no location
           * labelling for this geometry.  This edge must lie wholly inside or outside
           * the other geometry (which is determined by the current location).
           * Assign both sides to be the current location.
           */
          Assert.isTrue(
            label.getLocation(geomIndex, Position.LEFT) == Location.NONE,
            "found single null side"
          )
          label.setLocation(geomIndex, Position.RIGHT, currLoc)
          label.setLocation(geomIndex, Position.LEFT, currLoc)
        }
      }
    }
  }

  open fun findIndex(eSearch: EdgeEnd): Int {
    iterator()   // force edgelist to be computed
    for (i in edgeList!!.indices) {
      val e = edgeList!![i]
      if (e === eSearch) return i
    }
    return -1
  }

  override fun toString(): String {
    val buf = StringBuilder()
    buf.append("EdgeEndStar:   " + getCoordinate())
    buf.append("\n")
    val it = iterator()
    while (it.hasNext()) {
      val e = it.next()
      buf.append(e)
      buf.append("\n")
    }
    return buf.toString()
  }
}
