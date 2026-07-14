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

package org.locationtech.jts.simplify

import org.locationtech.jts.algorithm.LineIntersector
import org.locationtech.jts.algorithm.Orientation
import org.locationtech.jts.algorithm.RobustLineIntersector
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.CoordinateArrays
import org.locationtech.jts.geom.LineSegment

/**
 * Simplifies a TaggedLineString, preserving topology
 * (in the sense that no new intersections are introduced).
 * Uses the recursive Douglas-Peucker algorithm.
 *
 * @author Martin Davis
 */
internal class TaggedLineStringSimplifier(
    private val inputIndex: LineSegmentIndex,
    private val outputIndex: LineSegmentIndex,
    private val jumpChecker: ComponentJumpChecker
) {
  private val li: LineIntersector = RobustLineIntersector()
  private lateinit var line: TaggedLineString
  private lateinit var linePts: Array<Coordinate>

  /**
   * Simplifies the given [TaggedLineString]
   * using the distance tolerance specified.
   *
   * @param line the linestring to simplify
   * @param distanceTolerance the simplification distance tolerance
   */
  fun simplify(line: TaggedLineString, distanceTolerance: Double) {
    this.line = line
    linePts = line.getParentCoordinates()
    simplifySection(0, linePts.size - 1, 0, distanceTolerance)

    if (line.isRing() && CoordinateArrays.isRing(linePts)) {
      simplifyRingEndpoint(distanceTolerance)
    }
  }

  private fun simplifySection(i: Int, j: Int, depth: Int, distanceTolerance: Double) {
    var depthVar = depth
    depthVar += 1
    //-- if section has only one segment just keep the segment
    if ((i + 1) == j) {
      val newSeg = line.getSegment(i)
      line.addToResult(newSeg)
      //-- do not add segment to output index, since it is unchanged
      //-- leave the segment in the input index, for efficiency
      return
    }

    var isValidToSimplify = true

    /**
     * Following logic ensures that there is enough points in the output line.
     */
    if (line.getResultSize() < line.getMinimumSize()) {
      val worstCaseSize = depthVar + 1
      if (worstCaseSize < line.getMinimumSize())
        isValidToSimplify = false
    }

    val distance = DoubleArray(1)
    val furthestPtIndex = findFurthestPoint(linePts, i, j, distance)

    // flattening must be less than distanceTolerance
    if (distance[0] > distanceTolerance) {
      isValidToSimplify = false
    }

    if (isValidToSimplify) {
      // test if flattened section would cause intersection or jump
      val flatSeg = LineSegment()
      flatSeg.p0 = linePts[i]
      flatSeg.p1 = linePts[j]
      isValidToSimplify = isTopologyValid(line, i, j, flatSeg)
    }

    if (isValidToSimplify) {
      val newSeg = flatten(i, j)
      line.addToResult(newSeg)
      return
    }
    simplifySection(i, furthestPtIndex, depthVar, distanceTolerance)
    simplifySection(furthestPtIndex, j, depthVar, distanceTolerance)
  }

  /**
   * Simplifies the result segments on either side of a ring endpoint
   * (which was not processed by the initial simplification).
   */
  private fun simplifyRingEndpoint(distanceTolerance: Double) {
    if (line.getResultSize() > line.getMinimumSize()) {
      val firstSeg = line.getResultSegment(0)
      val lastSeg = line.getResultSegment(-1)

      val simpSeg = LineSegment(lastSeg.p0, firstSeg.p1)
      //-- the excluded segments are the ones containing the endpoint
      val endPt = firstSeg.p0
      if (simpSeg.distance(endPt) <= distanceTolerance
          && isTopologyValid(line, firstSeg, lastSeg, simpSeg)) {
        //-- don't know if segments are original or new, so remove from all indexes
        inputIndex.remove(firstSeg)
        inputIndex.remove(lastSeg)
        outputIndex.remove(firstSeg)
        outputIndex.remove(lastSeg)

        val flatSeg = line.removeRingEndpoint()
        outputIndex.add(flatSeg)
      }
    }
  }

  private fun findFurthestPoint(pts: Array<Coordinate>, i: Int, j: Int, maxDistance: DoubleArray): Int {
    val seg = LineSegment()
    seg.p0 = pts[i]
    seg.p1 = pts[j]
    var maxDist = -1.0
    var maxIndex = i
    for (k in i + 1 until j) {
      val midPt = pts[k]
      val distance = seg.distance(midPt)
      if (distance > maxDist) {
        maxDist = distance
        maxIndex = k
      }
    }
    maxDistance[0] = maxDist
    return maxIndex
  }

  /**
   * Flattens a section of the line between
   * indexes `start` and `end`,
   * replacing them with a line between the endpoints.
   *
   * @return the new segment created
   */
  private fun flatten(start: Int, end: Int): LineSegment {
    // make a new segment for the simplified geometry
    val p0 = linePts[start]
    val p1 = linePts[end]
    val newSeg = LineSegment(p0, p1)
    // update the input and output indexes
    outputIndex.add(newSeg)
    remove(line, start, end)

    return newSeg
  }

  private fun isTopologyValid(line: TaggedLineString,
                             sectionStart: Int, sectionEnd: Int,
                             flatSeg: LineSegment): Boolean {
    if (hasOutputIntersection(flatSeg))
      return false
    if (hasInputIntersection(line, sectionStart, sectionEnd, flatSeg))
      return false
    if (jumpChecker.hasJump(line, sectionStart, sectionEnd, flatSeg))
      return false
    return true
  }

  private fun isTopologyValid(line: TaggedLineString, seg1: LineSegment, seg2: LineSegment,
                             flatSeg: LineSegment): Boolean {
    //-- if segments are already flat, topology is unchanged and so is valid
    if (isCollinear(seg1.p0, flatSeg))
      return true
    if (hasOutputIntersection(flatSeg))
      return false
    if (hasInputIntersection(flatSeg))
      return false
    if (jumpChecker.hasJump(line, seg1, seg2, flatSeg))
      return false
    return true
  }

  private fun isCollinear(pt: Coordinate, seg: LineSegment): Boolean {
    return Orientation.COLLINEAR == seg.orientationIndex(pt)
  }

  private fun hasOutputIntersection(flatSeg: LineSegment): Boolean {
    val querySegs = outputIndex.query(flatSeg)
    for (o in querySegs) {
      val querySeg = o as LineSegment
      if (hasInvalidIntersection(querySeg, flatSeg)) {
        return true
      }
    }
    return false
  }

  private fun hasInputIntersection(flatSeg: LineSegment): Boolean {
    return hasInputIntersection(null, -1, -1, flatSeg)
  }

  private fun hasInputIntersection(line: TaggedLineString?,
                                  excludeStart: Int, excludeEnd: Int,
                                  flatSeg: LineSegment): Boolean {
    val querySegs = inputIndex.query(flatSeg)
    for (o in querySegs) {
      val querySeg = o as TaggedLineSegment
      if (hasInvalidIntersection(querySeg, flatSeg)) {
        /**
         * Ignore the intersection if the intersecting segment is part of the section being collapsed
         * to the candidate segment
         */
        if (line != null
            && isInLineSection(line, excludeStart, excludeEnd, querySeg))
          continue
        return true
      }
    }
    return false
  }

  private fun hasInvalidIntersection(seg0: LineSegment, seg1: LineSegment): Boolean {
    //-- segments must not be equal
    if (seg0.equalsTopo(seg1))
      return true
    li.computeIntersection(seg0.p0, seg0.p1, seg1.p0, seg1.p1)
    return li.isInteriorIntersection()
  }

  /**
   * Remove the segs in the section of the line
   */
  private fun remove(line: TaggedLineString,
                    start: Int, end: Int) {
    for (i in start until end) {
      val seg = line.getSegment(i)
      inputIndex.remove(seg)
    }
  }

  companion object {
    /**
     * Tests whether a segment is in a section of a TaggedLineString.
     */
    private fun isInLineSection(
        line: TaggedLineString,
        excludeStart: Int, excludeEnd: Int,
        seg: TaggedLineSegment): Boolean {
      //-- test segment is not in this line
      if (seg.getParent() !== line.getParent())
        return false
      val segIndex = seg.getIndex()
      if (excludeStart <= excludeEnd) {
        //-- section is contiguous
        if (segIndex >= excludeStart && segIndex < excludeEnd)
          return true
      } else {
        //-- section wraps around the end of a ring
        if (segIndex >= excludeStart || segIndex <= excludeEnd)
          return true
      }
      return false
    }
  }
}
