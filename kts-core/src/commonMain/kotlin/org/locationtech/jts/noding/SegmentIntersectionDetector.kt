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
import org.locationtech.jts.algorithm.RobustLineIntersector
import org.locationtech.jts.geom.Coordinate

/**
 * Detects and records an intersection between two [SegmentString]s,
 * if one exists.  Only a single intersection is recorded.
 * This strategy can be configured to search for **proper intersections**.
 * In this case, the presence of *any* kind of intersection will still be recorded,
 * but searching will continue until either a proper intersection has been found
 * or no intersections are detected.
 *
 * @version 1.7
 */
class SegmentIntersectionDetector
/**
 * Creates an intersection finder using a given LineIntersector.
 *
 * @param li the LineIntersector to use
 */
  (li: LineIntersector) : SegmentIntersector {
  private val li: LineIntersector = li
  private var findProper = false
  private var findAllTypes = false

  private var intersectionFound = false
  private var properIntersectionFound = false
  private var nonProperIntersectionFound = false

  private var intPt: Coordinate? = null
  private var intSegments: Array<Coordinate>? = null

  /**
   * Creates an intersection finder using a [RobustLineIntersector].
   */
  constructor() : this(RobustLineIntersector())

  /**
   * Sets whether processing must continue until a proper intersection is found.
   *
   * @param findProper true if processing should continue until a proper intersection is found
   */
  fun setFindProper(findProper: Boolean) {
    this.findProper = findProper
  }

  /**
   * Sets whether processing can terminate once any intersection is found.
   *
   * @param findAllTypes true if processing can terminate once any intersection is found.
   */
  fun setFindAllIntersectionTypes(findAllTypes: Boolean) {
    this.findAllTypes = findAllTypes
  }

  /**
   * Tests whether an intersection was found.
   *
   * @return true if an intersection was found
   */
  fun hasIntersection(): Boolean {
    return intersectionFound
  }

  /**
   * Tests whether a proper intersection was found.
   *
   * @return true if a proper intersection was found
   */
  fun hasProperIntersection(): Boolean {
    return properIntersectionFound
  }

  /**
   * Tests whether a non-proper intersection was found.
   *
   * @return true if a non-proper intersection was found
   */
  fun hasNonProperIntersection(): Boolean {
    return nonProperIntersectionFound
  }

  /**
   * Gets the computed location of the intersection.
   * Due to round-off, the location may not be exact.
   *
   * @return the coordinate for the intersection location
   */
  fun getIntersection(): Coordinate? {
    return intPt
  }

  /**
   * Gets the endpoints of the intersecting segments.
   *
   * @return an array of the segment endpoints (p00, p01, p10, p11)
   */
  fun getIntersectionSegments(): Array<Coordinate>? {
    return intSegments
  }

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
    //  if (li.hasIntersection() && li.isProper()) Debug.println(li);

    if (li.hasIntersection()) {
      // System.out.println(li);

      // record intersection info
      intersectionFound = true

      val isProper = li.isProper()
      if (isProper)
        properIntersectionFound = true
      if (!isProper)
        nonProperIntersectionFound = true

      /**
       * If this is the kind of intersection we are searching for
       * OR no location has yet been recorded
       * save the location data
       */
      var saveLocation = true
      if (findProper && !isProper) saveLocation = false

      if (intPt == null || saveLocation) {

        // record intersection location (approximate)
        intPt = li.getIntersection(0)

        // record intersecting segments
        intSegments = arrayOf(p00, p01, p10, p11)
      }
    }
  }

  /**
   * Tests whether processing can terminate,
   * because all required information has been obtained
   * (e.g. an intersection of the desired type has been detected).
   *
   * @return true if processing can terminate
   */
  override fun isDone(): Boolean {
    /**
     * If finding all types, we can stop
     * when both possible types have been found.
     */
    if (findAllTypes) {
      return properIntersectionFound && nonProperIntersectionFound
    }

    /**
     * If searching for a proper intersection, only stop if one is found
     */
    if (findProper) {
      return properIntersectionFound
    }
    return intersectionFound
  }
}
