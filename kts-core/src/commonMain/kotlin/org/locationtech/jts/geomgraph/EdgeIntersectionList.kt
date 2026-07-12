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

import org.locationtech.jts.util.TreeMap

import org.locationtech.jts.geom.Coordinate

/**
 * A list of edge intersections along an [Edge].
 * Implements splitting an edge with intersections
 * into multiple resultant edges.
 *
 * @version 1.7
 */
class EdgeIntersectionList(private val edge: Edge) {

  // a Map <EdgeIntersection, EdgeIntersection>
  private val nodeMap: MutableMap<EdgeIntersection, EdgeIntersection> = TreeMap()

  /**
   * Adds an intersection into the list, if it isn't already there.
   * The input segmentIndex and dist are expected to be normalized.
   *
   * @param intPt Point of intersection
   * @param segmentIndex Index of the containing line segment in the parent edge
   * @param dist Edge distance of this point along the containing line segment
   *
   * @return the EdgeIntersection found or added
   */
  fun add(intPt: Coordinate, segmentIndex: Int, dist: Double): EdgeIntersection {
    val eiNew = EdgeIntersection(intPt, segmentIndex, dist)
    val ei = nodeMap[eiNew]
    if (ei != null) {
      return ei
    }
    nodeMap[eiNew] = eiNew
    return eiNew
  }

  /**
   * Returns an iterator of [EdgeIntersection]s
   *
   * @return an Iterator of EdgeIntersections
   */
  fun iterator(): MutableIterator<EdgeIntersection> = nodeMap.values.iterator()

  /**
   * Tests if the given point is an edge intersection
   *
   * @param pt the point to test
   * @return true if the point is an intersection
   */
  fun isIntersection(pt: Coordinate): Boolean {
    val it = iterator()
    while (it.hasNext()) {
      val ei = it.next()
      if (ei.coord == pt)
        return true
    }
    return false
  }

  /**
   * Adds entries for the first and last points of the edge to the list
   */
  fun addEndpoints() {
    val maxSegIndex = edge.pts.size - 1
    add(edge.pts[0], 0, 0.0)
    add(edge.pts[maxSegIndex], maxSegIndex, 0.0)
  }

  /**
   * Creates new edges for all the edges that the intersections in this
   * list split the parent edge into.
   * Adds the edges to the input list (this is so a single list
   * can be used to accumulate all split edges for a Geometry).
   *
   * @param edgeList a list of EdgeIntersections
   */
  fun addSplitEdges(edgeList: MutableList<Edge>) {
    // ensure that the list has entries for the first and last point of the edge
    addEndpoints()

    val it = iterator()
    // there should always be at least two entries in the list
    var eiPrev = it.next()
    while (it.hasNext()) {
      val ei = it.next()
      val newEdge = createSplitEdge(eiPrev, ei)
      edgeList.add(newEdge)

      eiPrev = ei
    }
  }

  /**
   * Create a new "split edge" with the section of points between
   * (and including) the two intersections.
   * The label for the new edge is the same as the label for the parent edge.
   */
  private fun createSplitEdge(ei0: EdgeIntersection, ei1: EdgeIntersection): Edge {
//Debug.print("\ncreateSplitEdge"); Debug.print(ei0); Debug.print(ei1);
    var npts = ei1.segmentIndex - ei0.segmentIndex + 2

    val lastSegStartPt = edge.pts[ei1.segmentIndex]
    // if the last intersection point is not equal to the its segment start pt,
    // add it to the points list as well.
    // (This check is needed because the distance metric is not totally reliable!)
    // The check for point equality is 2D only - Z values are ignored
    val useIntPt1 = ei1.dist > 0.0 || !ei1.coord.equals2D(lastSegStartPt)
    if (!useIntPt1) {
      npts--
    }

    @Suppress("UNCHECKED_CAST")
    val pts = arrayOfNulls<Coordinate>(npts) as Array<Coordinate>
    var ipt = 0
    pts[ipt++] = Coordinate(ei0.coord)
    for (i in ei0.segmentIndex + 1..ei1.segmentIndex) {
      pts[ipt++] = edge.pts[i]
    }
    if (useIntPt1) pts[ipt] = ei1.coord
    return Edge(pts, Label(edge.getLabel()!!))
  }

}
