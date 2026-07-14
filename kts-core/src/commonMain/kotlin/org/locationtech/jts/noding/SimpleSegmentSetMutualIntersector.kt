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

/**
 * Intersects two sets of [SegmentString]s using
 * brute-force comparison.
 *
 */
class SimpleSegmentSetMutualIntersector
/**
 * Constructs a new intersector for a given set of [SegmentString]s.
 *
 * @param segStrings the base segment strings to intersect
 */
  (private val baseSegStrings: Collection<*>) : SegmentSetMutualIntersector {

  /**
   * Calls [SegmentIntersector.processIntersections]
   * for all *candidate* intersections between
   * the given collection of SegmentStrings and the set of base segments.
   *
   * @param segStrings set of segments to intersect
   * @param segInt segment intersector to use
   */
  override fun process(segStrings: Collection<*>?, segInt: SegmentIntersector) {
    for (baseObj in baseSegStrings) {
      val baseSS = baseObj as SegmentString
      for (ssObj in segStrings!!) {
        val ss = ssObj as SegmentString
        intersect(baseSS, ss, segInt)
        if (segInt.isDone())
          return
      }
    }
  }

  /**
   * Processes all of the segment pairs in the given segment strings
   * using the given SegmentIntersector.
   *
   * @param ss0 a Segment string
   * @param ss1 a segment string
   * @param segInt the segment intersector to use
   */
  private fun intersect(ss0: SegmentString, ss1: SegmentString, segInt: SegmentIntersector) {
    val pts0 = ss0.getCoordinates()
    val pts1 = ss1.getCoordinates()
    for (i0 in 0 until pts0.size - 1) {
      for (i1 in 0 until pts1.size - 1) {
        segInt.processIntersections(ss0, i0, ss1, i1)
        if (segInt.isDone())
          return
      }
    }
  }
}
