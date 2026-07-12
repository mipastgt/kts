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
package org.locationtech.jts.geomgraph.index

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.geomgraph.Edge

/**
 * MonotoneChains are a way of partitioning the segments of an edge to
 * allow for fast searching of intersections.
 * They have the following properties:
 *
 *  1. the segments within a monotone chain will never intersect each other
 *  1. the envelope of any contiguous subset of the segments in a monotone chain
 * is simply the envelope of the endpoints of the subset.
 *
 * Property 1 means that there is no need to test pairs of segments from within
 * the same monotone chain for intersection.
 * Property 2 allows
 * binary search to be used to find the intersection points of two monotone chains.
 * For many types of real-world data, these properties eliminate a large number of
 * segment comparisons, producing substantial speed gains.
 * @version 1.7
 */
class MonotoneChainEdge(private val e: Edge) {

  private val pts: Array<Coordinate> = e.getCoordinates() // cache a reference to the coord array, for efficiency
  // the lists of start/end indexes of the monotone chains.
  // Includes the end point of the edge as a sentinel
  private val startIndex: IntArray

  init {
    val mcb = MonotoneChainIndexer()
    startIndex = mcb.getChainStartIndices(pts)
  }

  fun getCoordinates(): Array<Coordinate> = pts
  fun getStartIndexes(): IntArray = startIndex

  fun getMinX(chainIndex: Int): Double {
    val x1 = pts[startIndex[chainIndex]].x
    val x2 = pts[startIndex[chainIndex + 1]].x
    return if (x1 < x2) x1 else x2
  }

  fun getMaxX(chainIndex: Int): Double {
    val x1 = pts[startIndex[chainIndex]].x
    val x2 = pts[startIndex[chainIndex + 1]].x
    return if (x1 > x2) x1 else x2
  }

  fun computeIntersects(mce: MonotoneChainEdge, si: SegmentIntersector) {
    for (i in 0 until startIndex.size - 1) {
      for (j in 0 until mce.startIndex.size - 1) {
        computeIntersectsForChain(i, mce, j, si)
      }
    }
  }

  fun computeIntersectsForChain(
    chainIndex0: Int,
    mce: MonotoneChainEdge,
    chainIndex1: Int,
    si: SegmentIntersector
  ) {
    computeIntersectsForChain(
      startIndex[chainIndex0], startIndex[chainIndex0 + 1],
      mce,
      mce.startIndex[chainIndex1], mce.startIndex[chainIndex1 + 1],
      si
    )
  }

  private fun computeIntersectsForChain(
    start0: Int, end0: Int,
    mce: MonotoneChainEdge,
    start1: Int, end1: Int,
    ei: SegmentIntersector
  ) {
//Debug.println("computeIntersectsForChain:" + p00 + p01 + p10 + p11);

    // terminating condition for the recursion
    if (end0 - start0 == 1 && end1 - start1 == 1) {
      ei.addIntersections(e, start0, mce.e, start1)
      return
    }
    // nothing to do if the envelopes of these chains don't overlap
    if (!overlaps(start0, end0, mce, start1, end1)) return

    // the chains overlap, so split each in half and iterate  (binary search)
    val mid0 = (start0 + end0) / 2
    val mid1 = (start1 + end1) / 2

    // Assert: mid != start or end (since we checked above for end - start <= 1)
    // check terminating conditions before recursing
    if (start0 < mid0) {
      if (start1 < mid1) computeIntersectsForChain(start0, mid0, mce, start1, mid1, ei)
      if (mid1 < end1) computeIntersectsForChain(start0, mid0, mce, mid1, end1, ei)
    }
    if (mid0 < end0) {
      if (start1 < mid1) computeIntersectsForChain(mid0, end0, mce, start1, mid1, ei)
      if (mid1 < end1) computeIntersectsForChain(mid0, end0, mce, mid1, end1, ei)
    }
  }

  /**
   * Tests whether the envelopes of two chain sections overlap (intersect).
   *
   * @param start0
   * @param end0
   * @param mce
   * @param start1
   * @param end1
   * @return true if the section envelopes overlap
   */
  private fun overlaps(
    start0: Int, end0: Int,
    mce: MonotoneChainEdge,
    start1: Int, end1: Int
  ): Boolean {
    return Envelope.intersects(pts[start0], pts[end0], mce.pts[start1], mce.pts[end1])
  }
}
