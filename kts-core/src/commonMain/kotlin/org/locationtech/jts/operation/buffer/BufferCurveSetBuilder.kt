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
import kotlin.math.min

import org.locationtech.jts.algorithm.Distance
import org.locationtech.jts.algorithm.Orientation
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.CoordinateArrays
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryCollection
import org.locationtech.jts.geom.LineSegment
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.LinearRing
import org.locationtech.jts.geom.Location
import org.locationtech.jts.geom.MultiLineString
import org.locationtech.jts.geom.MultiPoint
import org.locationtech.jts.geom.MultiPolygon
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.Polygon
import org.locationtech.jts.geom.Position
import org.locationtech.jts.geom.PrecisionModel
import org.locationtech.jts.geom.Triangle
import org.locationtech.jts.geomgraph.Label
import org.locationtech.jts.noding.NodedSegmentString
import org.locationtech.jts.noding.SegmentString

/**
 * Creates all the raw offset curves for a buffer of a [Geometry].
 * Raw curves need to be noded together and polygonized to form the final buffer area.
 *
 * @version 1.7
 */
class BufferCurveSetBuilder(
  private val inputGeom: Geometry,
  private val distance: Double,
  precisionModel: PrecisionModel,
  bufParams: BufferParameters
) {

  private val curveBuilder: OffsetCurveBuilder = OffsetCurveBuilder(precisionModel, bufParams)

  private val curveList: MutableList<SegmentString> = ArrayList()

  private var isInvertOrientation = false

  /**
   * Sets whether the offset curve is generated
   * using the inverted orientation of input rings.
   *
   * @param isInvertOrientation true if input ring orientation should be inverted
   */
  internal fun setInvertOrientation(isInvertOrientation: Boolean) {
    this.isInvertOrientation = isInvertOrientation
  }

  /**
   * Computes orientation of a ring using a signed-area orientation test.
   *
   * @param coord the ring coordinates
   * @return true if the ring is CCW
   */
  private fun isRingCCW(coord: Array<Coordinate>): Boolean {
    val isCCW = Orientation.isCCWArea(coord)
    //--- invert orientation if required
    if (isInvertOrientation) return !isCCW
    return isCCW
  }

  /**
   * Computes the set of raw offset curves for the buffer.
   * Each offset curve has an attached [Label] indicating
   * its left and right location.
   *
   * @return a Collection of SegmentStrings representing the raw buffer curves
   */
  fun getCurves(): MutableList<SegmentString> {
    add(inputGeom)
    return curveList
  }

  /**
   * Creates a [SegmentString] for a coordinate list which is a raw offset curve,
   * and adds it to the list of buffer curves.
   */
  private fun addCurve(coord: Array<Coordinate>?, leftLoc: Int, rightLoc: Int) {
    // don't add null or trivial curves
    if (coord == null || coord.size < 2) return
    // add the edge for a coordinate list which is a raw offset curve
    val e: SegmentString = NodedSegmentString(coord, Label(0, Location.BOUNDARY, leftLoc, rightLoc))
    curveList.add(e)
  }

  private fun add(g: Geometry) {
    if (g.isEmpty()) return

    if (g is Polygon) addPolygon(g)
    // LineString also handles LinearRings
    else if (g is LineString) addLineString(g)
    else if (g is Point) addPoint(g)
    else if (g is MultiPoint) addCollection(g)
    else if (g is MultiLineString) addCollection(g)
    else if (g is MultiPolygon) addCollection(g)
    else if (g is GeometryCollection) addCollection(g)
    else throw UnsupportedOperationException(g::class.simpleName)
  }

  private fun addCollection(gc: GeometryCollection) {
    for (i in 0 until gc.getNumGeometries()) {
      val g = gc.getGeometryN(i)
      add(g)
    }
  }

  /**
   * Add a Point to the graph.
   */
  private fun addPoint(p: Point) {
    // a zero or negative width buffer of a point is empty
    if (distance <= 0.0) return
    val coord = p.getCoordinates()
    // skip if coordinate is invalid
    if (coord.size >= 1 && !coord[0].isValid()) return
    val curve = curveBuilder.getLineCurve(coord, distance)
    addCurve(curve, Location.EXTERIOR, Location.INTERIOR)
  }

  private fun addLineString(line: LineString) {
    if (curveBuilder.isLineOffsetEmpty(distance)) return

    val coord = clean(line.getCoordinates())

    /**
     * Rings (closed lines) are generated with a continuous curve,
     * with no end arcs.
     */
    if (CoordinateArrays.isRing(coord) && !curveBuilder.getBufferParameters().isSingleSided()) {
      addRingBothSides(coord, distance)
    } else {
      val curve = curveBuilder.getLineCurve(coord, distance)
      addCurve(curve, Location.EXTERIOR, Location.INTERIOR)
    }
  }

  private fun addPolygon(p: Polygon) {
    var offsetDistance = distance
    var offsetSide = Position.LEFT
    if (distance < 0.0) {
      offsetDistance = -distance
      offsetSide = Position.RIGHT
    }

    val shell = p.getExteriorRing()
    val shellCoord = clean(shell.getCoordinates())
    // optimization - don't bother computing buffer
    // if the polygon would be completely eroded
    if (distance < 0.0 && isErodedCompletely(shell, distance)) return
    // don't attempt to buffer a polygon with too few distinct vertices
    if (distance <= 0.0 && shellCoord.size < 3) return

    addRingSide(
      shellCoord,
      offsetDistance,
      offsetSide,
      Location.EXTERIOR,
      Location.INTERIOR
    )

    for (i in 0 until p.getNumInteriorRing()) {
      val hole = p.getInteriorRingN(i)
      val holeCoord = clean(hole.getCoordinates())

      // optimization - don't bother computing buffer for this hole
      // if the hole would be completely covered
      if (distance > 0.0 && isErodedCompletely(hole, -distance)) continue

      // Holes are topologically labelled opposite to the shell, since
      // the interior of the polygon lies on their opposite side
      // (on the left, if the hole is oriented CCW)
      addRingSide(
        holeCoord,
        offsetDistance,
        Position.opposite(offsetSide),
        Location.INTERIOR,
        Location.EXTERIOR
      )
    }
  }

  private fun addRingBothSides(coord: Array<Coordinate>, distance: Double) {
    addRingSide(
      coord, distance,
      Position.LEFT,
      Location.EXTERIOR, Location.INTERIOR
    )
    /* Add the opposite side of the ring
    */
    addRingSide(
      coord, distance,
      Position.RIGHT,
      Location.INTERIOR, Location.EXTERIOR
    )
  }

  /**
   * Adds an offset curve for one side of a ring.
   */
  private fun addRingSide(coord: Array<Coordinate>, offsetDistance: Double, side: Int, cwLeftLoc: Int, cwRightLoc: Int) {
    // don't bother adding ring if it is "flat" and will disappear in the output
    if (offsetDistance == 0.0 && coord.size < LinearRing.MINIMUM_VALID_SIZE) return

    var leftLoc = cwLeftLoc
    var rightLoc = cwRightLoc
    var localSide = side
    val isCCW = isRingCCW(coord)
    if (coord.size >= LinearRing.MINIMUM_VALID_SIZE && isCCW) {
      leftLoc = cwRightLoc
      rightLoc = cwLeftLoc
      localSide = Position.opposite(side)
    }
    val curve = curveBuilder.getRingCurve(coord, localSide, offsetDistance)

    /**
     * If the offset curve has inverted completely it will produce
     * an unwanted artifact in the result, so skip it.
     */
    if (isRingCurveInverted(coord, offsetDistance, curve)) {
      return
    }

    addCurve(curve, leftLoc, rightLoc)
  }

  companion object {
    /**
     * Keeps only valid coordinates, and removes repeated points.
     */
    private fun clean(coords: Array<Coordinate>): Array<Coordinate> {
      return CoordinateArrays.removeRepeatedOrInvalidPoints(coords)
    }

    private const val MAX_INVERTED_RING_SIZE = 9
    private const val INVERTED_CURVE_VERTEX_FACTOR = 4
    private const val NEARNESS_FACTOR = 0.99

    /**
     * Tests whether the offset curve for a ring is fully inverted.
     */
    private fun isRingCurveInverted(inputRing: Array<Coordinate>, distance: Double, curveRing: Array<Coordinate>?): Boolean {
      if (distance == 0.0) return false
      /**
       * Only proper rings can invert.
       */
      if (inputRing.size <= 3) return false
      /**
       * Heuristic based on low chance that a ring with many vertices will invert.
       */
      if (inputRing.size >= MAX_INVERTED_RING_SIZE) return false

      /**
       * Don't check curves which are much larger than the input.
       */
      if (curveRing!!.size > INVERTED_CURVE_VERTEX_FACTOR * inputRing.size) return false

      /**
       * If curve contains points which are on the buffer,
       * it is not inverted and can be included in the raw curves.
       */
      if (hasPointOnBuffer(inputRing, distance, curveRing)) return false

      //-- curve is inverted, so discard it
      return true
    }

    /**
     * Tests if there are points on the raw offset curve which may
     * lie on the final buffer curve.
     */
    private fun hasPointOnBuffer(inputRing: Array<Coordinate>, distance: Double, curveRing: Array<Coordinate>): Boolean {
      val distTol = NEARNESS_FACTOR * abs(distance)

      for (i in 0 until curveRing.size - 1) {
        val v = curveRing[i]

        //-- check curve vertices
        val dist = Distance.pointToSegmentString(v, inputRing)
        if (dist > distTol) {
          return true
        }

        //-- check curve segment midpoints
        val iNext = if (i < curveRing.size - 1) i + 1 else 0
        val vnext = curveRing[iNext]
        val midPt = LineSegment.midPoint(v, vnext)

        val distMid = Distance.pointToSegmentString(midPt, inputRing)
        if (distMid > distTol) {
          return true
        }
      }
      return false
    }

    /**
     * Tests whether a ring buffer is eroded completely (is empty)
     * based on simple heuristics.
     */
    private fun isErodedCompletely(ring: LinearRing, bufferDistance: Double): Boolean {
      val ringCoord = ring.getCoordinates()
      // degenerate ring has no area
      if (ringCoord.size < 4) return bufferDistance < 0

      // important test to eliminate inverted triangle bug
      // also optimizes erosion test for triangles
      if (ringCoord.size == 4) return isTriangleErodedCompletely(ringCoord, bufferDistance)

      // if envelope is narrower than twice the buffer distance, ring is eroded
      val env = ring.getEnvelopeInternal()
      val envMinDimension = min(env.getHeight(), env.getWidth())
      if (bufferDistance < 0.0 && 2 * abs(bufferDistance) > envMinDimension) return true

      return false
    }

    /**
     * Tests whether a triangular ring would be eroded completely by the given
     * buffer distance.
     */
    private fun isTriangleErodedCompletely(triangleCoord: Array<Coordinate>, bufferDistance: Double): Boolean {
      val tri = Triangle(triangleCoord[0], triangleCoord[1], triangleCoord[2])
      val inCentre = tri.inCentre()
      val distToCentre = Distance.pointToSegment(inCentre, tri.p0, tri.p1)
      return distToCentre < abs(bufferDistance)
    }
  }
}
