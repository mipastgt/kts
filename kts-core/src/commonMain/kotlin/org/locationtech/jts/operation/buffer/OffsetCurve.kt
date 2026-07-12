/*
 * Copyright (c) 2021 Martin Davis.
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
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.CoordinateArrays
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LineSegment
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.LinearRing
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.Polygon
import org.locationtech.jts.geom.util.GeometryMapper
import org.locationtech.jts.index.chain.MonotoneChain
import org.locationtech.jts.index.chain.MonotoneChainSelectAction
import org.locationtech.jts.util.Assert

/**
 * Computes an offset curve from a geometry.
 * An offset curve is a linear geometry which is offset a given distance
 * from the input.
 *
 * @author Martin Davis
 *
 */
class OffsetCurve {

  private val inputGeom: Geometry
  private val distance: Double
  private var isJoined = false

  private val bufferParams: BufferParameters
  private val matchDistance: Double
  private val geomFactory: GeometryFactory

  /**
   * Creates a new instance for computing an offset curve for a geometry at a given distance.
   * with default quadrant segments and join style.
   *
   * @param geom the geometry to offset
   * @param distance the offset distance (positive for left, negative for right)
   */
  constructor(geom: Geometry, distance: Double) : this(geom, distance, null)

  /**
   * Creates a new instance for computing an offset curve for a geometry at a given distance.
   * setting the quadrant segments and join style and mitre limit
   * via [BufferParameters].
   *
   * @param geom the geometry to offset
   * @param distance the offset distance (positive for left, negative for right)
   * @param bufParams the buffer parameters to use
   */
  constructor(geom: Geometry, distance: Double, bufParams: BufferParameters?) {
    this.inputGeom = geom
    this.distance = distance

    matchDistance = abs(distance) / MATCH_DISTANCE_FACTOR
    geomFactory = inputGeom.getFactory()

    //-- make new buffer params since the end cap style must be the default
    this.bufferParams = BufferParameters()
    if (bufParams != null) {
      /**
       * Prevent using a very small QuadSegs value, to avoid
       * offset curve artifacts near the end caps.
       */
      var quadSegs = bufParams.getQuadrantSegments()
      if (quadSegs < MIN_QUADRANT_SEGMENTS) {
        quadSegs = MIN_QUADRANT_SEGMENTS
      }
      bufferParams.setQuadrantSegments(quadSegs)
      bufferParams.setJoinStyle(bufParams.getJoinStyle())
      bufferParams.setMitreLimit(bufParams.getMitreLimit())
    }
  }

  /**
   * Computes a single curve line for each input linear component,
   * by joining curve sections in order along the raw offset curve.
   * The default mode is to compute separate curve sections.
   *
   * @param isJoined true if joined mode should be used.
   */
  fun setJoined(isJoined: Boolean) {
    this.isJoined = isJoined
  }

  /**
   * Gets the computed offset curve lines.
   *
   * @return the offset curve geometry
   */
  fun getCurve(): Geometry {
    return GeometryMapper.flatMap(inputGeom, 1, object : GeometryMapper.MapOp {

      override fun map(geom: Geometry): Geometry? {
        if (geom is Point) return null
        if (geom is Polygon) {
          return toLineString(geom.buffer(distance).getBoundary())
        }
        return computeCurve(geom as LineString, distance)
      }

      /**
       * Force LinearRings to be LineStrings.
       *
       * @param geom a geometry which may be a LinearRing
       * @return a geometry which will be a LineString or MultiLineString
       */
      fun toLineString(geom: Geometry): Geometry {
        if (geom is LinearRing) {
          return geom.getFactory().createLineString(geom.getCoordinateSequence())
        }
        return geom
      }
    })
  }

  private fun computeCurve(lineGeom: LineString, distance: Double): Geometry {
    //-- first handle simple cases
    //-- empty or single-point line
    if (lineGeom.getNumPoints() < 2 || lineGeom.getLength() == 0.0) {
      return geomFactory.createLineString()
    }
    //-- zero offset distance
    if (distance == 0.0) {
      return lineGeom.copy()
    }
    //-- two-point line
    if (lineGeom.getNumPoints() == 2) {
      return offsetSegment(lineGeom.getCoordinates(), distance)
    }

    val sections = computeSections(lineGeom, distance)

    val offsetCurve: Geometry = if (isJoined) {
      OffsetCurveSection.toLine(sections, geomFactory)
    } else {
      OffsetCurveSection.toGeometry(sections, geomFactory)
    }
    return offsetCurve
  }

  private fun computeSections(lineGeom: LineString, distance: Double): MutableList<OffsetCurveSection> {
    val rawCurve = rawOffset(lineGeom, distance, bufferParams)
    val sections = ArrayList<OffsetCurveSection>()
    if (rawCurve == null || rawCurve.size == 0) {
      return sections
    }

    /**
     * Note: If the raw offset curve has no
     * narrow concave angles or self-intersections it could be returned as is.
     */

    val bufferPoly = getBufferOriented(lineGeom, distance, bufferParams)

    //-- first extract offset curve sections from shell
    val shell = bufferPoly.getExteriorRing().getCoordinates()
    computeCurveSections(shell, rawCurve, sections)

    //-- extract offset curve sections from holes
    for (i in 0 until bufferPoly.getNumInteriorRing()) {
      val hole = bufferPoly.getInteriorRingN(i).getCoordinates()
      computeCurveSections(hole, rawCurve, sections)
    }
    return sections
  }

  private fun offsetSegment(pts: Array<Coordinate>, distance: Double): LineString {
    val offsetSeg = LineSegment(pts[0], pts[1]).offset(distance)
    return geomFactory.createLineString(arrayOf(offsetSeg.p0, offsetSeg.p1))
  }

  private fun computeCurveSections(bufferRingPts: Array<Coordinate>, rawCurve: Array<Coordinate>, sections: MutableList<OffsetCurveSection>) {
    val rawPosition = DoubleArray(bufferRingPts.size - 1)
    for (i in rawPosition.indices) {
      rawPosition[i] = NOT_IN_CURVE
    }
    val bufferSegIndex = SegmentMCIndex(bufferRingPts)
    var bufferFirstIndex = -1
    var minRawPosition = -1.0
    for (i in 0 until rawCurve.size - 1) {
      val minBufferIndexForSeg = matchSegments(rawCurve[i], rawCurve[i + 1], i, bufferSegIndex, bufferRingPts, rawPosition)
      if (minBufferIndexForSeg >= 0) {
        val pos = rawPosition[minBufferIndexForSeg]
        if (bufferFirstIndex < 0 || pos < minRawPosition) {
          minRawPosition = pos
          bufferFirstIndex = minBufferIndexForSeg
        }
      }
    }
    //-- no matching sections found in this buffer ring
    if (bufferFirstIndex < 0) return
    extractSections(bufferRingPts, rawPosition, bufferFirstIndex, sections)
  }

  /**
   * Matches the segments in a buffer ring to the raw offset curve
   * to obtain their match positions (if any).
   *
   * @return the index of the minimum matched buffer segment
   */
  private fun matchSegments(
    raw0: Coordinate,
    raw1: Coordinate,
    rawCurveIndex: Int,
    bufferSegIndex: SegmentMCIndex,
    bufferPts: Array<Coordinate>,
    rawCurvePos: DoubleArray
  ): Int {
    val matchEnv = Envelope(raw0, raw1)
    matchEnv.expandBy(matchDistance)
    val matchAction = MatchCurveSegmentAction(raw0, raw1, rawCurveIndex, matchDistance, bufferPts, rawCurvePos)
    bufferSegIndex.query(matchEnv, matchAction)
    return matchAction.getBufferMinIndex()
  }

  /**
   * An action to match a raw offset curve segment
   * to segments in a buffer ring
   * and record the matched segment locations(s) along the raw curve.
   *
   * @author Martin Davis
   */
  private class MatchCurveSegmentAction(
    private val raw0: Coordinate,
    private val raw1: Coordinate,
    private val rawCurveIndex: Int,
    private val matchDistance: Double,
    private val bufferRingPts: Array<Coordinate>,
    private val rawCurveLoc: DoubleArray
  ) : MonotoneChainSelectAction() {
    private val rawLen: Double = raw0.distance(raw1)
    private var minRawLocation = -1.0
    private var bufferRingMinIndex = -1

    fun getBufferMinIndex(): Int {
      return bufferRingMinIndex
    }

    override fun select(mc: MonotoneChain, segIndex: Int) {
      /**
       * Generally buffer segments are no longer than raw curve segments,
       * since the final buffer line likely has node points added.
       */
      val frac = segmentMatchFrac(bufferRingPts[segIndex], bufferRingPts[segIndex + 1], raw0, raw1, matchDistance)
      //-- no match
      if (frac < 0) return

      //-- location is used to sort segments along raw curve
      val location = rawCurveIndex + frac
      rawCurveLoc[segIndex] = location
      //-- buffer seg index at lowest raw location is the curve start
      if (minRawLocation < 0 || location < minRawLocation) {
        minRawLocation = location
        bufferRingMinIndex = segIndex
      }
    }

    private fun segmentMatchFrac(
      buf0: Coordinate,
      buf1: Coordinate,
      raw0: Coordinate,
      raw1: Coordinate,
      matchDistance: Double
    ): Double {
      if (!isMatch(buf0, buf1, raw0, raw1, matchDistance)) return -1.0

      //-- matched - determine location as fraction along raw segment
      val seg = LineSegment(raw0, raw1)
      return seg.segmentFraction(buf0)
    }

    private fun isMatch(buf0: Coordinate, buf1: Coordinate, raw0: Coordinate, raw1: Coordinate, matchDistance: Double): Boolean {
      val bufSegLen = buf0.distance(buf1)
      if (rawLen <= bufSegLen) {
        if (matchDistance < Distance.pointToSegment(raw0, buf0, buf1)) return false
        if (matchDistance < Distance.pointToSegment(raw1, buf0, buf1)) return false
      } else {
        if (matchDistance < Distance.pointToSegment(buf0, raw0, raw1)) return false
        if (matchDistance < Distance.pointToSegment(buf1, raw0, raw1)) return false
      }
      return true
    }
  }

  /**
   * This is only called when there is at least one ring segment matched.
   */
  private fun extractSections(ringPts: Array<Coordinate>, rawCurveLoc: DoubleArray, startIndex: Int, sections: MutableList<OffsetCurveSection>) {
    var sectionStart = startIndex
    var sectionCount = 0
    var sectionEnd: Int
    do {
      sectionEnd = findSectionEnd(rawCurveLoc, sectionStart, startIndex)
      val location = rawCurveLoc[sectionStart]
      val lastIndex = prev(sectionEnd, rawCurveLoc.size)
      val lastLoc = rawCurveLoc[lastIndex]
      val section = OffsetCurveSection.create(ringPts, sectionStart, sectionEnd, location, lastLoc)
      sections.add(section)
      sectionStart = findSectionStart(rawCurveLoc, sectionEnd)

      //-- check for an abnormal state
      if (sectionCount++ > ringPts.size) {
        Assert.shouldNeverReachHere("Too many sections for ring - probable bug")
      }
    } while (sectionStart != startIndex && sectionEnd != startIndex)
  }

  private fun findSectionStart(loc: DoubleArray, end: Int): Int {
    var start = end
    do {
      val next = next(start, loc.size)
      //-- skip ahead if segment is not in raw curve
      if (loc[start] == NOT_IN_CURVE) {
        start = next
        continue
      }
      val prev = prev(start, loc.size)
      //-- if prev segment is not in raw curve then have found a start
      if (loc[prev] == NOT_IN_CURVE) {
        return start
      }
      if (isJoined) {
        /**
         *  Start section at next gap in raw curve.
         */
        val locDelta = abs(loc[start] - loc[prev])
        if (locDelta > 1) return start
      }
      start = next
    } while (start != end)
    return start
  }

  private fun findSectionEnd(loc: DoubleArray, start: Int, firstStartIndex: Int): Int {
    // assert: pos[start] is IN CURVE
    var end = start
    var next: Int
    do {
      next = next(end, loc.size)
      if (loc[next] == NOT_IN_CURVE) return next
      if (isJoined) {
        /**
         *  End section at gap in raw curve.
         */
        val locDelta = abs(loc[next] - loc[end])
        if (locDelta > 1) return next
      }
      end = next
    } while (end != start && end != firstStartIndex)
    return end
  }

  companion object {
    /**
     * The nearness tolerance for matching the the raw offset linework and the buffer curve.
     */
    private const val MATCH_DISTANCE_FACTOR = 10000

    /**
     * A QuadSegs minimum value that will prevent generating
     * unwanted offset curve artifacts near end caps.
     */
    private const val MIN_QUADRANT_SEGMENTS = 8

    private const val NOT_IN_CURVE = -1.0

    /**
     * Computes the offset curve of a geometry at a given distance.
     *
     * @param geom a geometry
     * @param distance the offset distance (positive for left, negative for right)
     * @return the offset curve
     */
    @JvmStatic
    fun getCurve(geom: Geometry, distance: Double): Geometry {
      val oc = OffsetCurve(geom, distance)
      return oc.getCurve()
    }

    /**
     * Computes the offset curve of a geometry at a given distance,
     * with specified quadrant segments, join style and mitre limit.
     *
     * @param geom a geometry
     * @param distance the offset distance (positive for left, negative for right)
     * @param quadSegs the quadrant segments (-1 for default)
     * @param joinStyle the join style (-1 for default)
     * @param mitreLimit the mitre limit (-1 for default)
     * @return the offset curve
     */
    @JvmStatic
    fun getCurve(geom: Geometry, distance: Double, quadSegs: Int, joinStyle: Int, mitreLimit: Double): Geometry {
      val bufferParams = BufferParameters()
      if (quadSegs >= 0) bufferParams.setQuadrantSegments(quadSegs)
      if (joinStyle >= 0) bufferParams.setJoinStyle(joinStyle)
      if (mitreLimit >= 0) bufferParams.setMitreLimit(mitreLimit)
      val oc = OffsetCurve(geom, distance, bufferParams)
      return oc.getCurve()
    }

    /**
     * Computes the offset curve of a geometry at a given distance,
     * joining curve sections into a single line for each input line.
     *
     * @param geom a geometry
     * @param distance the offset distance (positive for left, negative for right)
     * @return the joined offset curve
     */
    @JvmStatic
    fun getCurveJoined(geom: Geometry, distance: Double): Geometry {
      val oc = OffsetCurve(geom, distance)
      oc.setJoined(true)
      return oc.getCurve()
    }

    /**
     * Gets the raw offset curve for a line at a given distance.
     *
     * @param line the line to offset
     * @param distance the offset distance (positive for left, negative for right)
     * @param bufParams the buffer parameters to use
     * @return the raw offset curve points
     */
    @JvmStatic
    fun rawOffset(line: LineString, distance: Double, bufParams: BufferParameters): Array<Coordinate>? {
      val pts = line.getCoordinates()
      val cleanPts = CoordinateArrays.removeRepeatedOrInvalidPoints(pts)
      val ocb = OffsetCurveBuilder(line.getFactory().getPrecisionModel(), bufParams)
      val rawPts = ocb.getOffsetCurve(cleanPts, distance)
      return rawPts
    }

    /**
     * Gets the raw offset curve for a line at a given distance,
     * with default buffer parameters.
     *
     * @param line the line to offset
     * @param distance the offset distance (positive for left, negative for right)
     * @return the raw offset curve points
     */
    @JvmStatic
    fun rawOffset(line: LineString, distance: Double): Array<Coordinate>? {
      return rawOffset(line, distance, BufferParameters())
    }

    private fun getBufferOriented(geom: LineString, distance: Double, bufParams: BufferParameters): Polygon {
      val buffer = BufferOp.bufferOp(geom, abs(distance), bufParams)
      var bufferPoly = extractMaxAreaPolygon(buffer)
      //-- for negative distances (Right of input) reverse buffer direction to match offset curve
      if (distance < 0) {
        bufferPoly = bufferPoly.reverse()
      }
      return bufferPoly
    }

    /**
     * Extracts the largest polygon by area from a geometry.
     *
     * @param geom a geometry
     * @return the polygon element of largest area
     */
    private fun extractMaxAreaPolygon(geom: Geometry): Polygon {
      if (geom.getNumGeometries() == 1) return geom as Polygon

      var maxArea = 0.0
      var maxPoly: Polygon? = null
      for (i in 0 until geom.getNumGeometries()) {
        val poly = geom.getGeometryN(i) as Polygon
        val area = poly.getArea()
        if (maxPoly == null || area > maxArea) {
          maxPoly = poly
          maxArea = area
        }
      }
      return maxPoly!!
    }

    private fun next(i: Int, size: Int): Int {
      val n = i + 1
      return if (n < size) n else 0
    }

    private fun prev(i: Int, size: Int): Int {
      val n = i - 1
      return if (n < 0) size - 1 else n
    }
  }
}
