/*
 * Copyright (c) 2016 Martin Davis.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.locationtech.jts.noding.snap
import kotlin.math.abs

import org.locationtech.jts.algorithm.Distance
import org.locationtech.jts.algorithm.LineIntersector
import org.locationtech.jts.algorithm.RobustLineIntersector
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.noding.NodedSegmentString
import org.locationtech.jts.noding.SegmentIntersector
import org.locationtech.jts.noding.SegmentString

/**
 * Finds intersections between line segments which are being snapped,
 * and adds them as nodes.
 *
 */
class SnappingIntersectionAdder
/**
 * Creates an intersector which finds intersections, snaps them,
 * and adds them as nodes.
 *
 * @param snapTolerance the snapping tolerance distance
 * @param snapPointIndex the snapPointIndex
 */
  (
  private val snapTolerance: Double,
  private val snapPointIndex: SnappingPointIndex
) : SegmentIntersector {
  private val li: LineIntersector = RobustLineIntersector()

  /**
   * This method is called by clients
   * of the [SegmentIntersector] class to process
   * intersections for two segments of the [SegmentString]s being intersected.
   * Note that some clients (such as `MonotoneChain`s) may optimize away
   * this call for segment pairs which they have determined do not intersect
   * (e.g. by an disjoint envelope test).
   */
  override fun processIntersections(
    seg0: SegmentString, segIndex0: Int,
    seg1: SegmentString, segIndex1: Int
  ) {
    // don't bother intersecting a segment with itself
    if (seg0 === seg1 && segIndex0 == segIndex1) return

    val p00 = seg0.getCoordinate(segIndex0)
    val p01 = seg0.getCoordinate(segIndex0 + 1)
    val p10 = seg1.getCoordinate(segIndex1)
    val p11 = seg1.getCoordinate(segIndex1 + 1)

    /*
     * Don't node intersections which are just
     * due to the shared vertex of adjacent segments.
     */
    if (!isAdjacent(seg0, segIndex0, seg1, segIndex1)) {
      li.computeIntersection(p00, p01, p10, p11)
      //if (li.hasIntersection() && li.isProper()) Debug.println(li);

      /*
       * Process single point intersections only.
       * Two-point (collinear) ones are handled by the near-vertex code
       */
      if (li.hasIntersection() && li.getIntersectionNum() == 1) {

        val intPt = li.getIntersection(0)
        val snapPt = snapPointIndex.snap(intPt)

        (seg0 as NodedSegmentString).addIntersection(snapPt, segIndex0)
        (seg1 as NodedSegmentString).addIntersection(snapPt, segIndex1)
      }
    }

    /*
     * The segments must also be snapped to the other segment endpoints.
     */
    processNearVertex(seg0, segIndex0, p00, seg1, segIndex1, p10, p11)
    processNearVertex(seg0, segIndex0, p01, seg1, segIndex1, p10, p11)
    processNearVertex(seg1, segIndex1, p10, seg0, segIndex0, p00, p01)
    processNearVertex(seg1, segIndex1, p11, seg0, segIndex0, p00, p01)
  }

  /**
   * If an endpoint of one segment is near
   * the *interior* of the other segment, add it as an intersection.
   * EXCEPT if the endpoint is also close to a segment endpoint
   * (since this can introduce "zigs" in the linework).
   */
  private fun processNearVertex(
    srcSS: SegmentString, srcIndex: Int, p: Coordinate,
    ss: SegmentString, segIndex: Int, p0: Coordinate, p1: Coordinate
  ) {
    /*
     * Don't add intersection if candidate vertex is near endpoints of segment.
     * This avoids creating "zig-zag" linework
     * (since the vertex could actually be outside the segment envelope).
     * Also, this should have already been snapped.
     */
    if (p.distance(p0) < snapTolerance) return
    if (p.distance(p1) < snapTolerance) return

    val distSeg = Distance.pointToSegment(p, p0, p1)
    if (distSeg < snapTolerance) {
      // add node to target segment
      (ss as NodedSegmentString).addIntersection(p, segIndex)
      // add node at vertex to source SS
      (srcSS as NodedSegmentString).addIntersection(p, srcIndex)
    }
  }

  /**
   * Always process all intersections
   *
   * @return false always
   */
  override fun isDone(): Boolean = false

  companion object {
    /**
     * Tests if segments are adjacent on the same SegmentString.
     * Closed segStrings require a check for the point shared by the beginning
     * and end segments.
     */
    private fun isAdjacent(ss0: SegmentString, segIndex0: Int, ss1: SegmentString, segIndex1: Int): Boolean {
      if (ss0 !== ss1) return false

      val adjacent = abs(segIndex0 - segIndex1) == 1
      if (adjacent)
        return true
      if (ss0.isClosed()) {
        val maxSegIndex = ss0.size() - 1
        if ((segIndex0 == 0 && segIndex1 == maxSegIndex)
          || (segIndex1 == 0 && segIndex0 == maxSegIndex)
        ) {
          return true
        }
      }
      return false
    }
  }
}
