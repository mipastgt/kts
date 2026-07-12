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
import kotlin.jvm.JvmField

import org.locationtech.jts.algorithm.LineIntersector
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.geom.IntersectionMatrix
import org.locationtech.jts.geom.Position
import org.locationtech.jts.geomgraph.index.MonotoneChainEdge

/**
 * @version 1.7
 */
open class Edge(pts: Array<Coordinate>, label: Label?) : GraphComponent(label) {

  @JvmField
  internal val pts: Array<Coordinate> = pts
  private var env: Envelope? = null

  @JvmField
  internal val eiList: EdgeIntersectionList = EdgeIntersectionList(this)
  private var name: String? = null
  private var mce: MonotoneChainEdge? = null
  private var isolated = true
  private val depth = Depth()
  private var depthDelta = 0   // the change in area depth from the R to L side of this edge

  constructor(pts: Array<Coordinate>) : this(pts, null)

  fun getNumPoints(): Int = pts.size
  fun setName(name: String?) { this.name = name }
  fun getCoordinates(): Array<Coordinate> = pts
  fun getCoordinate(i: Int): Coordinate {
    return pts[i]
  }

  override fun getCoordinate(): Coordinate? {
    if (pts.isNotEmpty()) return pts[0]
    return null
  }

  fun getEnvelope(): Envelope {
    // compute envelope lazily
    if (env == null) {
      env = Envelope()
      for (i in pts.indices) {
        env!!.expandToInclude(pts[i])
      }
    }
    return env!!
  }

  fun getDepth(): Depth = depth

  /**
   * The depthDelta is the change in depth as an edge is crossed from R to L
   * @return the change in depth as the edge is crossed from R to L
   */
  fun getDepthDelta(): Int = depthDelta
  fun setDepthDelta(depthDelta: Int) { this.depthDelta = depthDelta }

  fun getMaximumSegmentIndex(): Int {
    return pts.size - 1
  }

  fun getEdgeIntersectionList(): EdgeIntersectionList = eiList

  fun getMonotoneChainEdge(): MonotoneChainEdge {
    if (mce == null) mce = MonotoneChainEdge(this)
    return mce!!
  }

  fun isClosed(): Boolean {
    return pts[0] == pts[pts.size - 1]
  }

  /**
   * An Edge is collapsed if it is an Area edge and it consists of
   * two segments which are equal and opposite (eg a zero-width V).
   *
   * @return zero-width V area edge, consisting of two segments which are equal and of oppose orientation
   */
  fun isCollapsed(): Boolean {
    if (!label!!.isArea()) return false
    if (pts.size != 3) return false
    if (pts[0] == pts[2]) return true
    return false
  }

  fun getCollapsedEdge(): Edge {
    val newPts = arrayOf(pts[0], pts[1])
    return Edge(newPts, Label.toLineLabel(label!!))
  }

  fun setIsolated(isIsolated: Boolean) {
    this.isolated = isIsolated
  }

  override fun isIsolated(): Boolean {
    return isolated
  }

  /**
   * Adds EdgeIntersections for one or both
   * intersections found for a segment of an edge to the edge intersection list.
   * @param li Determining number of intersections to add
   * @param segmentIndex Segment index to add
   * @param geomIndex Geometry index to add
   */
  fun addIntersections(li: LineIntersector, segmentIndex: Int, geomIndex: Int) {
    for (i in 0 until li.getIntersectionNum()) {
      addIntersection(li, segmentIndex, geomIndex, i)
    }
  }

  /**
   * Add an EdgeIntersection for intersection intIndex.
   * An intersection that falls exactly on a vertex of the edge is normalized
   * to use the higher of the two possible segmentIndexes
   *
   * @param li Determining number of intersections to add
   * @param segmentIndex Segment index to add
   * @param geomIndex Geometry index to add
   * @param intIndex intIndex is 0 or 1
   */
  fun addIntersection(li: LineIntersector, segmentIndex: Int, geomIndex: Int, intIndex: Int) {
    val intPt = Coordinate(li.getIntersection(intIndex))
    var normalizedSegmentIndex = segmentIndex
    var dist = li.getEdgeDistance(geomIndex, intIndex)
//Debug.println("edge intpt: " + intPt + " dist: " + dist);
    // normalize the intersection point location
    val nextSegIndex = normalizedSegmentIndex + 1
    if (nextSegIndex < pts.size) {
      val nextPt = pts[nextSegIndex]
//Debug.println("next pt: " + nextPt);

      // Normalize segment index if intPt falls on vertex
      // The check for point equality is 2D only - Z values are ignored
      if (intPt.equals2D(nextPt)) {
//Debug.println("normalized distance");
        normalizedSegmentIndex = nextSegIndex
        dist = 0.0
      }
    }
    /**
     * Add the intersection point to edge intersection list.
     */
    val ei = eiList.add(intPt, normalizedSegmentIndex, dist)
//ei.print(System.out);
  }

  /**
   * Update the IM with the contribution for this component.
   * A component only contributes if it has a labelling for both parent geometries
   */
  public override fun computeIM(im: IntersectionMatrix) {
    updateIM(label!!, im)
  }

  /**
   * equals is defined to be:
   *
   *
   * e1 equals e2
   * **iff**
   * the coordinates of e1 are the same or the reverse of the coordinates in e2
   */
  override fun equals(o: Any?): Boolean {
    if (o !is Edge) return false
    val e = o

    if (pts.size != e.pts.size) return false

    var isEqualForward = true
    var isEqualReverse = true
    var iRev = pts.size
    for (i in pts.indices) {
      if (!pts[i].equals2D(e.pts[i])) {
        isEqualForward = false
      }
      if (!pts[i].equals2D(e.pts[--iRev])) {
        isEqualReverse = false
      }
      if (!isEqualForward && !isEqualReverse) return false
    }
    return true
  }

  /* (non-Javadoc)
   */
  override fun hashCode(): Int {
    val prime = 31
    var result = 1
    result = prime * result + pts.size
    if (pts.isNotEmpty()) {
      var p0 = pts[0]
      var p1 = pts[pts.size - 1]
      if (p0.compareTo(p1) == 1) {
        p0 = pts[pts.size - 1]
        p1 = pts[0]
      }
      result = prime * result + p0.hashCode()
      result = prime * result + p1.hashCode()
    }
    return result
  }

  /**
   * Check if coordinate sequences of the Edges are identical.
   *
   * @param e Edge
   * @return true if the coordinate sequences of the Edges are identical
   */
  fun isPointwiseEqual(e: Edge): Boolean {
    if (pts.size != e.pts.size) return false

    for (i in pts.indices) {
      if (!pts[i].equals2D(e.pts[i])) {
        return false
      }
    }
    return true
  }

  override fun toString(): String {
    val builder = StringBuilder()
    builder.append("edge $name: ")
    builder.append("LINESTRING (")
    for (i in pts.indices) {
      if (i > 0) builder.append(",")
      builder.append(pts[i].x.toString() + " " + pts[i].y)
    }
    builder.append(")  $label $depthDelta")
    return builder.toString()
  }

  companion object {
    /**
     * Updates an IM from the label for an edge.
     * Handles edges from both L and A geometries.
     * @param label Label defining position
     * @param im intersection matrix
     */
    @JvmStatic
    fun updateIM(label: Label, im: IntersectionMatrix) {
      im.setAtLeastIfValid(label.getLocation(0, Position.ON), label.getLocation(1, Position.ON), 1)
      if (label.isArea()) {
        im.setAtLeastIfValid(label.getLocation(0, Position.LEFT), label.getLocation(1, Position.LEFT), 2)
        im.setAtLeastIfValid(label.getLocation(0, Position.RIGHT), label.getLocation(1, Position.RIGHT), 2)
      }
    }
  }
}
