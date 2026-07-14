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

import kotlin.jvm.JvmStatic
import kotlin.math.abs

import org.locationtech.jts.algorithm.Distance
import org.locationtech.jts.algorithm.Orientation
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.CoordinateArrays
import org.locationtech.jts.geom.CoordinateList

/**
 * Simplifies a buffer input line to
 * remove concavities with shallow depth.
 * 
 * A key aspect of the simplification is that it
 * affects inside (concave or inward) corners only.
 * Convex (outward) corners are preserved, since they
 * are required to ensure that the generated buffer curve
 * lies at the correct distance from the input geometry.
 *
 * @author Martin Davis
 *
 */
class BufferInputLineSimplifier(private val inputLine: Array<Coordinate>) {

  private var distanceTol = 0.0
  private val isRing: Boolean = CoordinateArrays.isRing(inputLine)
  private var isDeleted: BooleanArray? = null
  private var angleOrientation = Orientation.COUNTERCLOCKWISE

  /**
   * Simplify the input coordinate list.
   * If the distance tolerance is positive,
   * concavities on the LEFT side of the line are simplified.
   * If the supplied distance tolerance is negative,
   * concavities on the RIGHT side of the line are simplified.
   *
   * @param distanceTol simplification distance tolerance to use
   * @return the simplified coordinate list
   */
  fun simplify(distanceTol: Double): Array<Coordinate> {
    this.distanceTol = abs(distanceTol)
    angleOrientation = Orientation.COUNTERCLOCKWISE
    if (distanceTol < 0) angleOrientation = Orientation.CLOCKWISE

    // rely on fact that boolean array is filled with false values
    isDeleted = BooleanArray(inputLine.size)

    var isChanged: Boolean
    do {
      isChanged = deleteShallowConcavities()
    } while (isChanged)

    return collapseLine()
  }

  /**
   * Uses a sliding window containing 3 vertices to detect shallow angles
   * in which the middle vertex can be deleted, since it does not
   * affect the shape of the resulting buffer in a significant way.
   *
   * @return true if any vertices were deleted
   */
  private fun deleteShallowConcavities(): Boolean {
    /**
     * Do not simplify end line segments of lines.
     * This ensures that end caps are generated consistently.
     */
    var index = if (isRing) 0 else 1

    var midIndex = nextIndex(index)
    var lastIndex = nextIndex(midIndex)

    var isChanged = false
    while (lastIndex < inputLine.size) {
      // test triple for shallow concavity
      var isMiddleVertexDeleted = false
      if (isDeletable(index, midIndex, lastIndex, distanceTol)) {
        isDeleted!![midIndex] = true
        isMiddleVertexDeleted = true
        isChanged = true
      }
      // move simplification window forward
      index = if (isMiddleVertexDeleted) lastIndex else midIndex

      midIndex = nextIndex(index)
      lastIndex = nextIndex(midIndex)
    }
    return isChanged
  }

  /**
   * Finds the next non-deleted index, or the end of the point array if none
   * @param index
   * @return the next non-deleted index, if any
   * or inputLine.length if there are no more non-deleted indices
   */
  private fun nextIndex(index: Int): Int {
    var next = index + 1
    while (next < inputLine.size && isDeleted!![next]) next++
    return next
  }

  private fun collapseLine(): Array<Coordinate> {
    val coordList = CoordinateList()
    for (i in inputLine.indices) {
      if (!isDeleted!![i]) coordList.add(inputLine[i])
    }
    return coordList.toCoordinateArray()
  }

  private fun isDeletable(i0: Int, i1: Int, i2: Int, distanceTol: Double): Boolean {
    val p0 = inputLine[i0]
    val p1 = inputLine[i1]
    val p2 = inputLine[i2]

    if (!isConcave(p0, p1, p2)) return false
    if (!isShallow(p0, p1, p2, distanceTol)) return false

    return isShallowSampled(p0, p1, i0, i2, distanceTol)
  }

  /**
   * Checks for shallowness over a sample of points in the given section.
   * This helps prevents the simplification from incrementally
   * "skipping" over points which are in fact non-shallow.
   */
  private fun isShallowSampled(p0: Coordinate, p2: Coordinate, i0: Int, i2: Int, distanceTol: Double): Boolean {
    // check every n'th point to see if it is within tolerance
    var inc = (i2 - i0) / NUM_PTS_TO_CHECK
    if (inc <= 0) inc = 1

    var i = i0
    while (i < i2) {
      if (!isShallow(p0, inputLine[i], p2, distanceTol)) return false
      i += inc
    }
    return true
  }

  private fun isConcave(p0: Coordinate, p1: Coordinate, p2: Coordinate): Boolean {
    val orientation = Orientation.index(p0, p1, p2)
    val concave = (orientation == angleOrientation)
    return concave
  }

  companion object {
    /**
     * Simplify the input coordinate list.
     *
     * @param inputLine the coordinate list to simplify
     * @param distanceTol simplification distance tolerance to use
     * @return the simplified coordinate list
     */
    @JvmStatic
    fun simplify(inputLine: Array<Coordinate>, distanceTol: Double): Array<Coordinate> {
      val simp = BufferInputLineSimplifier(inputLine)
      return simp.simplify(distanceTol)
    }

    private const val DELETE = 1

    private const val NUM_PTS_TO_CHECK = 10

    private fun isShallow(p0: Coordinate, p1: Coordinate, p2: Coordinate, distanceTol: Double): Boolean {
      val dist = Distance.pointToSegment(p1, p0, p2)
      return dist < distanceTol
    }
  }
}
