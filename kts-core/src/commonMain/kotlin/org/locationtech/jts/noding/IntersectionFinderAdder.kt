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
package org.locationtech.jts.noding

import org.locationtech.jts.algorithm.LineIntersector
import org.locationtech.jts.geom.Coordinate

/**
 * Finds **interior** intersections between line segments in [NodedSegmentString]s,
 * and adds them as nodes
 * using [NodedSegmentString.addIntersection].
 *
 *
 * This class is used primarily for Snap-Rounding.
 * For general-purpose noding, use [IntersectionAdder].
 *
 * @see IntersectionAdder
 */
@Deprecated("see InteriorIntersectionFinderAdder")
class IntersectionFinderAdder
/**
 * Creates an intersection finder which finds all proper intersections
 *
 * @param li the LineIntersector to use
 */
  (li: LineIntersector) : SegmentIntersector {
  private val li: LineIntersector = li
  private val interiorIntersections: MutableList<Coordinate> = ArrayList()

  fun getInteriorIntersections(): MutableList<Coordinate> = interiorIntersections

  /**
   * This method is called by clients
   * of the [SegmentIntersector] class to process
   * intersections for two segments of the [SegmentString]s being intersected.
   * Note that some clients (such as `MonotoneChain`s) may optimize away
   * this call for segment pairs which they have determined do not intersect
   * (e.g. by an disjoint envelope test).
   */
  override fun processIntersections(
    e0: SegmentString, segIndex0: Int,
    e1: SegmentString, segIndex1: Int
  ) {
    // don't bother intersecting a segment with itself
    if (e0 === e1 && segIndex0 == segIndex1) return

    val p00 = e0.getCoordinate(segIndex0)
    val p01 = e0.getCoordinate(segIndex0 + 1)
    val p10 = e1.getCoordinate(segIndex1)
    val p11 = e1.getCoordinate(segIndex1 + 1)

    li.computeIntersection(p00, p01, p10, p11)
    //if (li.hasIntersection() && li.isProper()) Debug.println(li);

    if (li.hasIntersection()) {
      if (li.isInteriorIntersection()) {
        for (intIndex in 0 until li.getIntersectionNum()) {
          interiorIntersections.add(li.getIntersection(intIndex))
        }
        (e0 as NodedSegmentString).addIntersections(li, segIndex0, 0)
        (e1 as NodedSegmentString).addIntersections(li, segIndex1, 1)
      }
    }
  }

  /**
   * Always process all intersections
   *
   * @return false always
   */
  override fun isDone(): Boolean = false
}
