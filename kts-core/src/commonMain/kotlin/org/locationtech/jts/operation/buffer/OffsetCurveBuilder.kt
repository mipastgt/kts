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
import kotlin.math.abs

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.CoordinateArrays
import org.locationtech.jts.geom.Position
import org.locationtech.jts.geom.PrecisionModel

/**
 * Computes the raw offset curve for a
 * single [org.locationtech.jts.geom.Geometry] component (ring, line or point).
 *
 * @version 1.7
 */
class OffsetCurveBuilder(
  private val precisionModel: PrecisionModel,
  private val bufParams: BufferParameters
) {
  private var distance = 0.0

  /**
   * Gets the buffer parameters being used to generate the curve.
   *
   * @return the buffer parameters being used
   */
  fun getBufferParameters(): BufferParameters {
    return bufParams
  }

  /**
   * This method handles single points as well as LineStrings.
   *
   * @param inputPts the vertices of the line to offset
   * @param distance the offset distance
   *
   * @return a Coordinate array representing the curve
   * or null if the curve is empty
   */
  fun getLineCurve(inputPts: Array<Coordinate>, distance: Double): Array<Coordinate>? {
    this.distance = distance

    if (isLineOffsetEmpty(distance)) return null

    val posDistance = abs(distance)
    val segGen = getSegGen(posDistance)
    if (inputPts.size <= 1) {
      computePointCurve(inputPts[0], segGen)
    } else {
      if (bufParams.isSingleSided()) {
        val isRightSide = distance < 0.0
        computeSingleSidedBufferCurve(inputPts, isRightSide, segGen)
      } else {
        computeLineBufferCurve(inputPts, segGen)
      }
    }

    val lineCoord = segGen.getCoordinates()
    return lineCoord
  }

  /**
   * Tests whether the offset curve for line or point geometries
   * at the given offset distance is empty (does not exist).
   *
   * @param distance the offset curve distance
   * @return true if the offset curve is empty
   */
  fun isLineOffsetEmpty(distance: Double): Boolean {
    // a zero width buffer of a line or point is empty
    if (distance == 0.0) return true
    // a negative width buffer of a line or point is empty,
    // except for single-sided buffers, where the sign indicates the side
    if (distance < 0.0 && !bufParams.isSingleSided()) return true
    return false
  }

  /**
   * This method handles the degenerate cases of single points and lines,
   * as well as valid rings.
   *
   * @return a Coordinate array representing the curve,
   * or null if the curve is empty
   */
  fun getRingCurve(inputPts: Array<Coordinate>, side: Int, distance: Double): Array<Coordinate>? {
    this.distance = distance
    if (inputPts.size <= 2) return getLineCurve(inputPts, distance)

    // optimize creating ring for for zero distance
    if (distance == 0.0) {
      return copyCoordinates(inputPts)
    }
    val segGen = getSegGen(distance)
    computeRingBufferCurve(inputPts, side, segGen)
    return segGen.getCoordinates()
  }

  fun getOffsetCurve(inputPts: Array<Coordinate>, distance: Double): Array<Coordinate>? {
    this.distance = distance

    // a zero width offset curve is empty
    if (distance == 0.0) return null

    val isRightSide = distance < 0.0
    val posDistance = abs(distance)
    val segGen = getSegGen(posDistance)
    if (inputPts.size <= 1) {
      computePointCurve(inputPts[0], segGen)
    } else {
      computeOffsetCurve(inputPts, isRightSide, segGen)
    }
    val curvePts = segGen.getCoordinates()
    // for right side line is traversed in reverse direction, so have to reverse generated line
    if (isRightSide) CoordinateArrays.reverse(curvePts)
    return curvePts
  }

  private fun getSegGen(distance: Double): OffsetSegmentGenerator {
    return OffsetSegmentGenerator(precisionModel, bufParams, distance)
  }

  /**
   * Computes the distance tolerance to use during input
   * line simplification.
   */
  private fun simplifyTolerance(bufDistance: Double): Double {
    return bufDistance * bufParams.getSimplifyFactor()
  }

  private fun computePointCurve(pt: Coordinate, segGen: OffsetSegmentGenerator) {
    when (bufParams.getEndCapStyle()) {
      BufferParameters.CAP_ROUND -> segGen.createCircle(pt)
      BufferParameters.CAP_SQUARE -> segGen.createSquare(pt)
      // otherwise curve is empty (e.g. for a butt cap);
    }
  }

  private fun computeLineBufferCurve(inputPts: Array<Coordinate>, segGen: OffsetSegmentGenerator) {
    val distTol = simplifyTolerance(distance)

    //--------- compute points for left side of line
    // Simplify the appropriate side of the line before generating
    val simp1 = BufferInputLineSimplifier.simplify(inputPts, distTol)

    val n1 = simp1.size - 1
    segGen.initSideSegments(simp1[0], simp1[1], Position.LEFT)
    for (i in 2..n1) {
      segGen.addNextSegment(simp1[i], true)
    }
    segGen.addLastSegment()
    // add line cap for end of line
    segGen.addLineEndCap(simp1[n1 - 1], simp1[n1])

    //---------- compute points for right side of line
    // Simplify the appropriate side of the line before generating
    val simp2 = BufferInputLineSimplifier.simplify(inputPts, -distTol)
    val n2 = simp2.size - 1

    // since we are traversing line in opposite order, offset position is still LEFT
    segGen.initSideSegments(simp2[n2], simp2[n2 - 1], Position.LEFT)
    for (i in n2 - 2 downTo 0) {
      segGen.addNextSegment(simp2[i], true)
    }
    segGen.addLastSegment()
    // add line cap for start of line
    segGen.addLineEndCap(simp2[1], simp2[0])

    segGen.closeRing()
  }

  private fun computeSingleSidedBufferCurve(inputPts: Array<Coordinate>, isRightSide: Boolean, segGen: OffsetSegmentGenerator) {
    val distTol = simplifyTolerance(distance)

    if (isRightSide) {
      // add original line
      segGen.addSegments(inputPts, true)

      //---------- compute points for right side of line
      val simp2 = BufferInputLineSimplifier.simplify(inputPts, -distTol)
      val n2 = simp2.size - 1

      // since we are traversing line in opposite order, offset position is still LEFT
      segGen.initSideSegments(simp2[n2], simp2[n2 - 1], Position.LEFT)
      segGen.addFirstSegment()
      for (i in n2 - 2 downTo 0) {
        segGen.addNextSegment(simp2[i], true)
      }
    } else {
      // add original line
      segGen.addSegments(inputPts, false)

      //--------- compute points for left side of line
      val simp1 = BufferInputLineSimplifier.simplify(inputPts, distTol)

      val n1 = simp1.size - 1
      segGen.initSideSegments(simp1[0], simp1[1], Position.LEFT)
      segGen.addFirstSegment()
      for (i in 2..n1) {
        segGen.addNextSegment(simp1[i], true)
      }
    }
    segGen.addLastSegment()
    segGen.closeRing()
  }

  private fun computeOffsetCurve(inputPts: Array<Coordinate>, isRightSide: Boolean, segGen: OffsetSegmentGenerator) {
    val distTol = simplifyTolerance(abs(distance))

    if (isRightSide) {
      //---------- compute points for right side of line
      val simp2 = BufferInputLineSimplifier.simplify(inputPts, -distTol)
      val n2 = simp2.size - 1

      // since we are traversing line in opposite order, offset position is still LEFT
      segGen.initSideSegments(simp2[n2], simp2[n2 - 1], Position.LEFT)
      segGen.addFirstSegment()
      for (i in n2 - 2 downTo 0) {
        segGen.addNextSegment(simp2[i], true)
      }
    } else {
      //--------- compute points for left side of line
      val simp1 = BufferInputLineSimplifier.simplify(inputPts, distTol)

      val n1 = simp1.size - 1
      segGen.initSideSegments(simp1[0], simp1[1], Position.LEFT)
      segGen.addFirstSegment()
      for (i in 2..n1) {
        segGen.addNextSegment(simp1[i], true)
      }
    }
    segGen.addLastSegment()
  }

  private fun computeRingBufferCurve(inputPts: Array<Coordinate>, side: Int, segGen: OffsetSegmentGenerator) {
    // simplify input line to improve performance
    var distTol = simplifyTolerance(distance)
    // ensure that correct side is simplified
    if (side == Position.RIGHT) distTol = -distTol
    val simp = BufferInputLineSimplifier.simplify(inputPts, distTol)

    val n = simp.size - 1
    segGen.initSideSegments(simp[n - 1], simp[0], side)
    for (i in 1..n) {
      val addStartPoint = i != 1
      segGen.addNextSegment(simp[i], addStartPoint)
    }
    segGen.closeRing()
  }

  companion object {
    private fun copyCoordinates(pts: Array<Coordinate>): Array<Coordinate> {
      val copy = arrayOfNulls<Coordinate>(pts.size)
      for (i in copy.indices) {
        copy[i] = pts[i].copy()
      }
      @Suppress("UNCHECKED_CAST")
      return copy as Array<Coordinate>
    }
  }
}
