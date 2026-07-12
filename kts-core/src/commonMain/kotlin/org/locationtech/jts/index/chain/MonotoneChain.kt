/*
 * Copyright (c) 2016 Vivid Solutions, and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.locationtech.jts.index.chain
import kotlin.math.max
import kotlin.math.min

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.geom.LineSegment

/**
 * Monotone Chains are a way of partitioning the segments of a linestring to
 * allow for fast searching of intersections.
 *
 * This implementation of MonotoneChains uses the concept of internal iterators
 * ([MonotoneChainSelectAction] and [MonotoneChainOverlapAction])
 * to return the results for queries.
 *
 * @version 1.7
 */
class MonotoneChain(
    private val pts: Array<Coordinate>,
    private val start: Int,
    private val end: Int,
    private val context: Any?  // user-defined information
) {

  private var env: Envelope? = null
  private var id = 0 // useful for optimizing chain comparisons

  /**
   * Sets the id of this chain.
   *
   * @param id an id value
   */
  fun setId(id: Int) {
    this.id = id
  }

  /**
   * Sets the overlap distance used in overlap tests
   * with other chains.
   *
   * @param distance the distance to buffer overlap tests by
   */
  fun setOverlapDistance(distance: Double) {
    //this.overlapDistance = distance;
  }

  /**
   * Gets the id of this chain.
   *
   * @return the id value
   */
  fun getId(): Int = id

  /**
   * Gets the user-defined context data value.
   *
   * @return a data value
   */
  fun getContext(): Any? = context

  /**
   * Gets the envelope of the chain.
   *
   * @return the envelope of the chain
   */
  fun getEnvelope(): Envelope {
    return getEnvelope(0.0)
  }

  /**
   * Gets the envelope for this chain,
   * expanded by a given distance.
   *
   * @param expansionDistance distance to expand the envelope by
   * @return the expanded envelope of the chain
   */
  fun getEnvelope(expansionDistance: Double): Envelope {
    var e = env
    if (e == null) {
      /**
       * The monotonicity property allows fast envelope determination
       */
      val p0 = pts[start]
      val p1 = pts[end]
      e = Envelope(p0, p1)
      if (expansionDistance > 0.0)
        e.expandBy(expansionDistance)
      env = e
    }
    return e
  }

  /**
   * Gets the index of the start of the monotone chain
   * in the underlying array of points.
   *
   * @return the start index of the chain
   */
  fun getStartIndex(): Int = start

  /**
   * Gets the index of the end of the monotone chain
   * in the underlying array of points.
   *
   * @return the end index of the chain
   */
  fun getEndIndex(): Int = end

  /**
   * Gets the line segment starting at `index`
   *
   * @param index index of segment
   * @param ls line segment to extract into
   */
  fun getLineSegment(index: Int, ls: LineSegment) {
    ls.p0 = pts[index]
    ls.p1 = pts[index + 1]
  }

  /**
   * Return the subsequence of coordinates forming this chain.
   * Allocates a new array to hold the Coordinates
   */
  fun getCoordinates(): Array<Coordinate> {
    return Array(end - start + 1) { i -> pts[start + i] }
  }

  /**
   * Determine all the line segments in the chain whose envelopes overlap
   * the searchEnvelope, and process them.
   *
   * @param searchEnv the search envelope
   * @param mcs the select action to execute on selected segments
   */
  fun select(searchEnv: Envelope, mcs: MonotoneChainSelectAction) {
    computeSelect(searchEnv, start, end, mcs)
  }

  private fun computeSelect(
    searchEnv: Envelope,
    start0: Int, end0: Int,
    mcs: MonotoneChainSelectAction
  ) {
    val p0 = pts[start0]
    val p1 = pts[end0]

    // terminating condition for the recursion
    if (end0 - start0 == 1) {
      mcs.select(this, start0)
      return
    }
    // nothing to do if the envelopes don't overlap
    if (!searchEnv.intersects(p0, p1))
      return

    // the chains overlap, so split each in half and iterate  (binary search)
    val mid = (start0 + end0) / 2

    // Assert: mid != start or end (since we checked above for end - start <= 1)
    // check terminating conditions before recursing
    if (start0 < mid) {
      computeSelect(searchEnv, start0, mid, mcs)
    }
    if (mid < end0) {
      computeSelect(searchEnv, mid, end0, mcs)
    }
  }

  /**
   * Determines the line segments in two chains which may overlap,
   * and passes them to an overlap action.
   *
   * @param mc the chain to compare to
   * @param mco the overlap action to execute on overlapping segments
   */
  fun computeOverlaps(mc: MonotoneChain, mco: MonotoneChainOverlapAction) {
    computeOverlaps(start, end, mc, mc.start, mc.end, 0.0, mco)
  }

  /**
   * Determines the line segments in two chains which may overlap,
   * using an overlap distance tolerance,
   * and passes them to an overlap action.
   *
   * @param mc the chain to compare to
   * @param overlapTolerance the distance tolerance for the overlap test
   * @param mco the overlap action to execute on selected segments
   */
  fun computeOverlaps(mc: MonotoneChain, overlapTolerance: Double, mco: MonotoneChainOverlapAction) {
    computeOverlaps(start, end, mc, mc.start, mc.end, overlapTolerance, mco)
  }

  /**
   * Uses an efficient mutual binary search strategy
   * to determine which pairs of chain segments
   * may overlap, and calls the given overlap action on them.
   *
   * @param start0 the start index of this chain section
   * @param end0 the end index of this chain section
   * @param mc the target monotone chain
   * @param start1 the start index of the target chain section
   * @param end1 the end index of the target chain section
   * @param overlapTolerance the overlap tolerance distance (may be 0)
   * @param mco the overlap action to execute on selected segments
   */
  private fun computeOverlaps(
    start0: Int, end0: Int,
    mc: MonotoneChain,
    start1: Int, end1: Int,
    overlapTolerance: Double,
    mco: MonotoneChainOverlapAction
  ) {
    // terminating condition for the recursion
    if (end0 - start0 == 1 && end1 - start1 == 1) {
      mco.overlap(this, start0, mc, start1)
      return
    }
    // nothing to do if the envelopes of these subchains don't overlap
    if (!overlaps(start0, end0, mc, start1, end1, overlapTolerance)) return

    // the chains overlap, so split each in half and iterate  (binary search)
    val mid0 = (start0 + end0) / 2
    val mid1 = (start1 + end1) / 2

    // Assert: mid != start or end (since we checked above for end - start <= 1)
    // check terminating conditions before recursing
    if (start0 < mid0) {
      if (start1 < mid1) computeOverlaps(start0, mid0, mc, start1, mid1, overlapTolerance, mco)
      if (mid1 < end1) computeOverlaps(start0, mid0, mc, mid1, end1, overlapTolerance, mco)
    }
    if (mid0 < end0) {
      if (start1 < mid1) computeOverlaps(mid0, end0, mc, start1, mid1, overlapTolerance, mco)
      if (mid1 < end1) computeOverlaps(mid0, end0, mc, mid1, end1, overlapTolerance, mco)
    }
  }

  /**
   * Tests whether the envelope of a section of the chain
   * overlaps (intersects) the envelope of a section of another target chain.
   *
   * @return true if the section envelopes overlap
   */
  private fun overlaps(
    start0: Int, end0: Int,
    mc: MonotoneChain,
    start1: Int, end1: Int,
    overlapTolerance: Double
  ): Boolean {
    if (overlapTolerance > 0.0) {
      return overlaps(pts[start0], pts[end0], mc.pts[start1], mc.pts[end1], overlapTolerance)
    }
    return Envelope.intersects(pts[start0], pts[end0], mc.pts[start1], mc.pts[end1])
  }

  private fun overlaps(p1: Coordinate, p2: Coordinate, q1: Coordinate, q2: Coordinate, overlapTolerance: Double): Boolean {
    var minq = min(q1.x, q2.x)
    var maxq = max(q1.x, q2.x)
    var minp = min(p1.x, p2.x)
    var maxp = max(p1.x, p2.x)

    if (minp > maxq + overlapTolerance)
      return false
    if (maxp < minq - overlapTolerance)
      return false

    minq = min(q1.y, q2.y)
    maxq = max(q1.y, q2.y)
    minp = min(p1.y, p2.y)
    maxp = max(p1.y, p2.y)

    if (minp > maxq + overlapTolerance)
      return false
    if (maxp < minq - overlapTolerance)
      return false
    return true
  }
}
