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
import kotlin.math.max

import org.locationtech.jts.algorithm.Orientation
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.LineSegment
import org.locationtech.jts.geom.Position
import org.locationtech.jts.geomgraph.DirectedEdge

/**
 * Locates a subgraph inside a set of subgraphs,
 * in order to determine the outside depth of the subgraph.
 * The input subgraphs are assumed to have had depths
 * already calculated for their edges.
 *
 */
class SubgraphDepthLocater internal constructor(private val subgraphs: List<BufferSubgraph>) {
  private val seg = LineSegment()

  fun getDepth(p: Coordinate): Int {
    val stabbedSegments = findStabbedSegments(p)
    // if no segments on stabbing line subgraph must be outside all others.
    if (stabbedSegments.size == 0) return 0
    val ds = stabbedSegments.min()
    return ds.leftDepth
  }

  /**
   * Finds all non-horizontal segments intersecting the stabbing line.
   * The stabbing line is the ray to the right of stabbingRayLeftPt.
   *
   * @param stabbingRayLeftPt the left-hand origin of the stabbing line
   * @return a List of [DepthSegment]s intersecting the stabbing line
   */
  private fun findStabbedSegments(stabbingRayLeftPt: Coordinate): MutableList<DepthSegment> {
    val stabbedSegments = ArrayList<DepthSegment>()
    for (bsg in subgraphs) {
      // optimization - don't bother checking subgraphs which the ray does not intersect
      val env = bsg.getEnvelope()
      if (stabbingRayLeftPt.y < env.getMinY() || stabbingRayLeftPt.y > env.getMaxY()) continue

      findStabbedSegments(stabbingRayLeftPt, bsg.getDirectedEdges(), stabbedSegments)
    }
    return stabbedSegments
  }

  /**
   * Finds all non-horizontal segments intersecting the stabbing line
   * in the list of dirEdges.
   */
  private fun findStabbedSegments(
    stabbingRayLeftPt: Coordinate,
    dirEdges: List<DirectedEdge>,
    stabbedSegments: MutableList<DepthSegment>
  ) {
    /**
     * Check all forward DirectedEdges only.  This is still general,
     * because each Edge has a forward DirectedEdge.
     */
    val i = dirEdges.iterator()
    while (i.hasNext()) {
      val de = i.next()
      if (!de.isForward()) continue
      findStabbedSegments(stabbingRayLeftPt, de, stabbedSegments)
    }
  }

  /**
   * Finds all non-horizontal segments intersecting the stabbing line
   * in the input dirEdge.
   */
  private fun findStabbedSegments(
    stabbingRayLeftPt: Coordinate,
    dirEdge: DirectedEdge,
    stabbedSegments: MutableList<DepthSegment>
  ) {
    val pts = dirEdge.getEdge().getCoordinates()
    for (i in 0 until pts.size - 1) {
      seg.p0 = pts[i]
      seg.p1 = pts[i + 1]
      // ensure segment always points upwards
      if (seg.p0.y > seg.p1.y) seg.reverse()

      // skip segment if it is left of the stabbing line
      val maxx = max(seg.p0.x, seg.p1.x)
      if (maxx < stabbingRayLeftPt.x) continue

      // skip horizontal segments (there will be a non-horizontal one carrying the same depth info
      if (seg.isHorizontal()) continue

      // skip if segment is above or below stabbing line
      if (stabbingRayLeftPt.y < seg.p0.y || stabbingRayLeftPt.y > seg.p1.y) continue

      // skip if stabbing ray is right of the segment
      if (Orientation.index(seg.p0, seg.p1, stabbingRayLeftPt) == Orientation.RIGHT) continue

      // stabbing line cuts this segment, so record it
      var depth = dirEdge.getDepth(Position.LEFT)
      // if segment direction was flipped, use RHS depth instead
      if (seg.p0 != pts[i]) depth = dirEdge.getDepth(Position.RIGHT)
      val ds = DepthSegment(seg, depth)
      stabbedSegments.add(ds)
    }
  }

  /**
   * A segment from a directed edge which has been assigned a depth value
   * for its sides.
   */
  class DepthSegment(seg: LineSegment, depth: Int) : Comparable<Any?> {
    private val upwardSeg: LineSegment
    val leftDepth: Int

    init {
      // Assert: input seg is upward (p0.y <= p1.y)
      upwardSeg = LineSegment(seg)
      this.leftDepth = depth
    }

    fun isUpward(): Boolean {
      return upwardSeg.p0.y <= upwardSeg.p1.y
    }

    /**
     * A comparison operation
     * which orders segments left to right.
     *
     * @param obj a DepthSegment
     * @return the comparison value
     */
    override fun compareTo(obj: Any?): Int {
      val other = obj as DepthSegment

      /*
       * If segment envelopes do not overlap, then
       * can use standard segment lexicographic ordering.
       */
      if (upwardSeg.minX() >= other.upwardSeg.maxX() ||
        upwardSeg.maxX() <= other.upwardSeg.minX() ||
        upwardSeg.minY() >= other.upwardSeg.maxY() ||
        upwardSeg.maxY() <= other.upwardSeg.minY()
      ) {
        return upwardSeg.compareTo(other.upwardSeg)
      }

      /**
       * Otherwise if envelopes overlap, use relative segment orientation.
       *
       * Collinear segments should be evaluated by previous logic
       */
      var orientIndex = upwardSeg.orientationIndex(other.upwardSeg)
      if (orientIndex != 0) return orientIndex

      /*
       * If comparison between this and other is indeterminate,
       * try the opposite call order.
       * The sign of the result needs to be flipped.
       */
      orientIndex = -1 * other.upwardSeg.orientationIndex(upwardSeg)
      if (orientIndex != 0) return orientIndex

      /*
       * If segment envelopes overlap and they are collinear,
       * since segments do not cross they must be equal.
       */
      // assert: segments are equal
      return 0
    }

    fun OLDcompareTo(obj: Any?): Int {
      val other = obj as DepthSegment

      // fast check if segments are trivially ordered along X
      if (upwardSeg.minX() > other.upwardSeg.maxX()) return 1
      if (upwardSeg.maxX() < other.upwardSeg.minX()) return -1

      /**
       * try and compute a determinate orientation for the segments.
       * Test returns 1 if other is left of this (i.e. this > other)
       */
      var orientIndex = upwardSeg.orientationIndex(other.upwardSeg)
      if (orientIndex != 0) return orientIndex

      /*
       * If comparison between this and other is indeterminate,
       * try the opposite call order.
       * The sign of the result needs to be flipped.
       */
      orientIndex = -1 * other.upwardSeg.orientationIndex(upwardSeg)
      if (orientIndex != 0) return orientIndex

      // otherwise, use standard lexicographic segment ordering
      return upwardSeg.compareTo(other.upwardSeg)
    }

    override fun toString(): String {
      return upwardSeg.toString()
    }
  }
}
