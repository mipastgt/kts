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
import org.locationtech.jts.geom.GeometryFactory

/**
 * Validates that a collection of [SegmentString]s is correctly noded.
 * Throws an appropriate exception if an noding error is found.
 *
 * @version 1.7
 */
class NodingValidator(private val segStrings: Collection<*>) {

  private val li: LineIntersector = RobustLineIntersector()

  fun checkValid() {
    // MD - is this call required?  Or could it be done in the Interior Intersection code?
    checkEndPtVertexIntersections()
    checkInteriorIntersections()
    checkCollapses()
  }

  /**
   * Checks if a segment string contains a segment pattern a-b-a (which implies a self-intersection)
   */
  private fun checkCollapses() {
    for (ss in segStrings) {
      checkCollapses(ss as SegmentString)
    }
  }

  private fun checkCollapses(ss: SegmentString) {
    val pts = ss.getCoordinates()
    for (i in 0 until pts.size - 2) {
      checkCollapse(pts[i], pts[i + 1], pts[i + 2])
    }
  }

  private fun checkCollapse(p0: Coordinate, p1: Coordinate, p2: Coordinate) {
    if (p0 == p2)
      throw RuntimeException(
        "found non-noded collapse at "
          + fact.createLineString(arrayOf(p0, p1, p2))
      )
  }

  /**
   * Checks all pairs of segments for intersections at an interior point of a segment
   */
  private fun checkInteriorIntersections() {
    for (ss0 in segStrings) {
      for (ss1 in segStrings) {
        checkInteriorIntersections(ss0 as SegmentString, ss1 as SegmentString)
      }
    }
  }

  private fun checkInteriorIntersections(ss0: SegmentString, ss1: SegmentString) {
    val pts0 = ss0.getCoordinates()
    val pts1 = ss1.getCoordinates()
    for (i0 in 0 until pts0.size - 1) {
      for (i1 in 0 until pts1.size - 1) {
        checkInteriorIntersections(ss0, i0, ss1, i1)
      }
    }
  }

  private fun checkInteriorIntersections(e0: SegmentString, segIndex0: Int, e1: SegmentString, segIndex1: Int) {
    if (e0 === e1 && segIndex0 == segIndex1) return
    //numTests++;
    val p00 = e0.getCoordinate(segIndex0)
    val p01 = e0.getCoordinate(segIndex0 + 1)
    val p10 = e1.getCoordinate(segIndex1)
    val p11 = e1.getCoordinate(segIndex1 + 1)

    li.computeIntersection(p00, p01, p10, p11)
    if (li.hasIntersection()) {

      if (li.isProper()
        || hasInteriorIntersection(li, p00, p01)
        || hasInteriorIntersection(li, p10, p11)
      ) {
        throw RuntimeException(
          "found non-noded intersection at "
            + p00 + "-" + p01
            + " and "
            + p10 + "-" + p11
        )
      }
    }
  }

  /**
   * @return true if there is an intersection point which is not an endpoint of the segment p0-p1
   */
  private fun hasInteriorIntersection(li: LineIntersector, p0: Coordinate, p1: Coordinate): Boolean {
    for (i in 0 until li.getIntersectionNum()) {
      val intPt = li.getIntersection(i)
      if (!(intPt == p0 || intPt == p1))
        return true
    }
    return false
  }

  /**
   * Checks for intersections between an endpoint of a segment string
   * and an interior vertex of another segment string
   */
  private fun checkEndPtVertexIntersections() {
    for (ss in segStrings) {
      val pts = (ss as SegmentString).getCoordinates()
      checkEndPtVertexIntersections(pts[0], segStrings)
      checkEndPtVertexIntersections(pts[pts.size - 1], segStrings)
    }
  }

  private fun checkEndPtVertexIntersections(testPt: Coordinate, segStrings: Collection<*>) {
    for (ssObj in segStrings) {
      val ss = ssObj as SegmentString
      val pts = ss.getCoordinates()
      for (j in 1 until pts.size - 1) {
        if (pts[j] == testPt)
          throw RuntimeException("found endpt/interior pt intersection at index $j :pt $testPt")
      }
    }
  }

  companion object {
    private val fact = GeometryFactory()
  }
}
