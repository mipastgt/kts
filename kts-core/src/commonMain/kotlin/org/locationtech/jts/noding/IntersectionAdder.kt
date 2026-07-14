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

import kotlin.jvm.JvmStatic
import kotlin.jvm.JvmField
import kotlin.math.abs

import org.locationtech.jts.algorithm.LineIntersector
import org.locationtech.jts.geom.Coordinate

/**
 * Computes the possible intersections between two line segments in [NodedSegmentString]s
 * and adds them to each string
 * using [NodedSegmentString.addIntersection].
 *
 */
class IntersectionAdder(li: LineIntersector) : SegmentIntersector {

  /**
   * These variables keep track of what types of intersections were
   * found during ALL edges that have been intersected.
   */
  private var hasIntersectionFound = false
  private var hasProper = false
  private var hasProperInterior = false
  private var hasInterior = false

  // the proper intersection point found
  private var properIntersectionPoint: Coordinate? = null

  private val li: LineIntersector = li
  private var isSelfIntersection = false
  //private boolean intersectionFound;
  @JvmField var numIntersections = 0
  @JvmField var numInteriorIntersections = 0
  @JvmField var numProperIntersections = 0

  // testing only
  @JvmField var numTests = 0

  fun getLineIntersector(): LineIntersector = li

  /**
   * @return the proper intersection point, or `null` if none was found
   */
  fun getProperIntersectionPoint(): Coordinate? = properIntersectionPoint

  fun hasIntersection(): Boolean = hasIntersectionFound

  /**
   * A proper intersection is an intersection which is interior to at least two
   * line segments.  Note that a proper intersection is not necessarily
   * in the interior of the entire Geometry, since another edge may have
   * an endpoint equal to the intersection, which according to SFS semantics
   * can result in the point being on the Boundary of the Geometry.
   */
  fun hasProperIntersection(): Boolean = hasProper

  /**
   * A proper interior intersection is a proper intersection which is **not**
   * contained in the set of boundary nodes set for this SegmentIntersector.
   */
  fun hasProperInteriorIntersection(): Boolean = hasProperInterior

  /**
   * An interior intersection is an intersection which is
   * in the interior of some segment.
   */
  fun hasInteriorIntersection(): Boolean = hasInterior

  /**
   * A trivial intersection is an apparent self-intersection which in fact
   * is simply the point shared by adjacent line segments.
   * Note that closed edges require a special check for the point shared by the beginning
   * and end segments.
   */
  private fun isTrivialIntersection(e0: SegmentString, segIndex0: Int, e1: SegmentString, segIndex1: Int): Boolean {
    if (e0 === e1) {
      if (li.getIntersectionNum() == 1) {
        if (isAdjacentSegments(segIndex0, segIndex1))
          return true
        if (e0.isClosed()) {
          val maxSegIndex = e0.size() - 1
          if ((segIndex0 == 0 && segIndex1 == maxSegIndex)
            || (segIndex1 == 0 && segIndex0 == maxSegIndex)
          ) {
            return true
          }
        }
      }
    }
    return false
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
    if (e0 === e1 && segIndex0 == segIndex1) return
    numTests++
    val p00 = e0.getCoordinate(segIndex0)
    val p01 = e0.getCoordinate(segIndex0 + 1)
    val p10 = e1.getCoordinate(segIndex1)
    val p11 = e1.getCoordinate(segIndex1 + 1)

    li.computeIntersection(p00, p01, p10, p11)
    //if (li.hasIntersection() && li.isProper()) Debug.println(li);
    if (li.hasIntersection()) {
      //intersectionFound = true;
      numIntersections++
      if (li.isInteriorIntersection()) {
        numInteriorIntersections++
        hasInterior = true
        //System.out.println(li);
      }
      // if the segments are adjacent they have at least one trivial intersection,
      // the shared endpoint.  Don't bother adding it if it is the
      // only intersection.
      if (!isTrivialIntersection(e0, segIndex0, e1, segIndex1)) {
        hasIntersectionFound = true
        (e0 as NodedSegmentString).addIntersections(li, segIndex0, 0)
        (e1 as NodedSegmentString).addIntersections(li, segIndex1, 1)
        if (li.isProper()) {
          numProperIntersections++
          //Debug.println(li.toString());  Debug.println(li.getIntersection(0));
          //properIntersectionPoint = (Coordinate) li.getIntersection(0).clone();
          hasProper = true
          hasProperInterior = true
        }
      }
    }
  }

  /**
   * Always process all intersections
   *
   * @return false always
   */
  override fun isDone(): Boolean = false

  companion object {
    @JvmStatic
    fun isAdjacentSegments(i1: Int, i2: Int): Boolean {
      return abs(i1 - i2) == 1
    }
  }
}
