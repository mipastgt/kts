/*
 * Copyright (c) 2019 Martin Davis.
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
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

import org.locationtech.jts.algorithm.Angle
import org.locationtech.jts.algorithm.Orientation
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.CoordinateList
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LineSegment
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.Polygon

/**
 * Creates a buffer polygon with a varying buffer distance
 * at each vertex along a line.
 * Vertex distances may be zero.
 * <p>
 * Only single linestrings are supported as input, since buffer widths
 * are typically specified individually for each line.
 *
 * @author Martin Davis
 *
 */
class VariableBuffer(line: Geometry, private val distance: DoubleArray) {

  private val line: LineString = line as LineString
  private val geomFactory: GeometryFactory = line.getFactory()
  private val quadrantSegs = BufferParameters.DEFAULT_QUADRANT_SEGMENTS

  init {
    if (distance.size != this.line.getNumPoints()) {
      throw IllegalArgumentException("Number of distances is not equal to number of vertices")
    }
  }

  /**
   * Computes the variable buffer polygon.
   *
   * @return a buffer polygon
   */
  fun getResult(): Geometry {
    val parts = ArrayList<Geometry>()

    val pts = line.getCoordinates()
    // construct segment buffers
    for (i in 1 until pts.size) {
      val dist0 = distance[i - 1]
      val dist1 = distance[i]
      if (dist0 > 0 || dist1 > 0) {
        val poly = segmentBuffer(pts[i - 1], pts[i], dist0, dist1)
        if (poly != null) parts.add(poly)
      }
    }

    val partsGeom = geomFactory.createGeometryCollection(GeometryFactory.toGeometryArray(parts))
    val buffer = partsGeom.union()

    //-- ensure an empty polygon is returned if needed
    if (buffer.isEmpty()) {
      return geomFactory.createPolygon()
    }
    return buffer
  }

  /**
   * Computes a variable buffer polygon for a single segment,
   * with the given endpoints and buffer distances.
   *
   * @return the segment buffer, or null if void
   */
  private fun segmentBuffer(p0: Coordinate, p1: Coordinate, dist0: Double, dist1: Double): Polygon? {
    /**
     * Skip buffer polygon if both distances are zero
     */
    if (dist0 <= 0 && dist1 <= 0) return null

    /**
     * Generation algorithm requires increasing distance, so flip if needed
     */
    if (dist0 > dist1) {
      return segmentBufferOriented(p1, p0, dist1, dist0)
    }
    return segmentBufferOriented(p0, p1, dist0, dist1)
  }

  private fun segmentBufferOriented(p0: Coordinate, p1: Coordinate, dist0: Double, dist1: Double): Polygon? {
    //-- Assert: dist0 <= dist1

    //-- forward tangent line
    val tangent = outerTangent(p0, dist0, p1, dist1)

    //-- if tangent is null then compute a buffer for largest circle
    if (tangent == null) {
      var center = p0
      var dist = dist0
      if (dist1 > dist0) {
        center = p1
        dist = dist1
      }
      return circle(center, dist)
    }

    //-- reverse tangent line on other side of segment
    val tangentReflect = reflect(tangent, p0, p1, dist0)

    val coords = CoordinateList()
    //-- end cap
    addCap(p1, dist1, tangent.p1, tangentReflect.p1, coords)
    //-- start cap
    addCap(p0, dist0, tangentReflect.p0, tangent.p0, coords)

    coords.closeRing()

    val pts = coords.toCoordinateArray()
    val polygon = geomFactory.createPolygon(pts)
    return polygon
  }

  private fun reflect(seg: LineSegment, p0: Coordinate, p1: Coordinate, dist0: Double): LineSegment {
    val line = LineSegment(p0, p1)
    var r0 = line.reflect(seg.p0)
    val r1 = line.reflect(seg.p1)
    //-- avoid numeric jitter if first distance is zero (second dist must be > 0)
    if (dist0 == 0.0) r0 = p0.copy()
    return LineSegment(r0, r1)
  }

  /**
   * Returns a circular polygon.
   *
   * @param center the circle center point
   * @param radius the radius
   * @return a polygon, or null if the radius is 0
   */
  private fun circle(center: Coordinate, radius: Double): Polygon? {
    if (radius <= 0) return null
    val nPts = 4 * quadrantSegs
    val pts = arrayOfNulls<Coordinate>(nPts + 1)
    val angInc = PI / 2 / quadrantSegs
    for (i in 0 until nPts) {
      pts[i] = projectPolar(center, radius, i * angInc)
    }
    pts[pts.size - 1] = pts[0]!!.copy()
    @Suppress("UNCHECKED_CAST")
    return geomFactory.createPolygon(pts as Array<Coordinate>)
  }

  /**
   * Adds a semi-circular cap CCW around the point p.
   */
  private fun addCap(p: Coordinate, r: Double, t1: Coordinate, t2: Coordinate, coords: CoordinateList) {
    //-- if radius is zero just copy the vertex
    if (r == 0.0) {
      coords.add(p.copy(), false)
      return
    }

    coords.add(t1, false)

    var angStart = Angle.angle(p, t1)
    val angEnd = Angle.angle(p, t2)
    if (angStart < angEnd) angStart += 2 * PI

    val indexStart = capAngleIndex(angStart)
    val indexEnd = capAngleIndex(angEnd)

    val capSegLen = r * 2 * sin(PI / 4 / quadrantSegs)
    val minSegLen = capSegLen / MIN_CAP_SEG_LEN_FACTOR

    for (i in indexStart downTo indexEnd) {
      //-- use negative increment to create points CW
      val ang = capAngle(i)
      val capPt = projectPolar(p, r, ang)

      var isCapPointHighQuality = true
      /**
       * Due to the fixed locations of the cap points,
       * a start or end cap point might create
       * a "reversed" segment to the next tangent point.
       */
      if (i == indexStart && Orientation.CLOCKWISE != Orientation.index(p, t1, capPt)) {
        isCapPointHighQuality = false
      } else if (i == indexEnd && Orientation.COUNTERCLOCKWISE != Orientation.index(p, t2, capPt)) {
        isCapPointHighQuality = false
      }

      /**
       * Remove short segments between the cap and the tangent segments.
       */
      if (capPt.distance(t1) < minSegLen) {
        isCapPointHighQuality = false
      } else if (capPt.distance(t2) < minSegLen) {
        isCapPointHighQuality = false
      }

      if (isCapPointHighQuality) {
        coords.add(capPt, false)
      }
    }

    coords.add(t2, false)
  }

  /**
   * Computes the actual angle for a cap angle index.
   */
  private fun capAngle(index: Int): Double {
    val capSegAng = PI / 2 / quadrantSegs
    return index * capSegAng
  }

  /**
   * Computes the canonical cap point index for a given angle.
   */
  private fun capAngleIndex(ang: Double): Int {
    val capSegAng = PI / 2 / quadrantSegs
    val index = (ang / capSegAng).toInt()
    return index
  }

  companion object {
    private const val MIN_CAP_SEG_LEN_FACTOR = 4

    /**
     * Creates a buffer polygon along a line with the buffer distance interpolated
     * between a start distance and an end distance.
     *
     * @param line the line to buffer
     * @param startDistance the buffer width at the start of the line
     * @param endDistance the buffer width at the end of the line
     * @return the variable-distance buffer polygon
     */
    @JvmStatic
    fun buffer(line: Geometry, startDistance: Double, endDistance: Double): Geometry {
      val distance = interpolate(line as LineString, startDistance, endDistance)
      val vb = VariableBuffer(line, distance)
      return vb.getResult()
    }

    /**
     * Creates a buffer polygon along a line with the buffer distance interpolated
     * between a start distance, a middle distance and an end distance.
     *
     * @param line the line to buffer
     * @param startDistance the buffer width at the start of the line
     * @param midDistance the buffer width at the middle vertex of the line
     * @param endDistance the buffer width at the end of the line
     * @return the variable-distance buffer polygon
     */
    @JvmStatic
    fun buffer(line: Geometry, startDistance: Double, midDistance: Double, endDistance: Double): Geometry {
      val distance = interpolate(line as LineString, startDistance, midDistance, endDistance)
      val vb = VariableBuffer(line, distance)
      return vb.getResult()
    }

    /**
     * Creates a buffer polygon along a line with the distance specified
     * at each vertex.
     *
     * @param line the line to buffer
     * @param distance the buffer distance for each vertex of the line
     * @return the variable-distance buffer polygon
     */
    @JvmStatic
    fun buffer(line: Geometry, distance: DoubleArray): Geometry {
      val vb = VariableBuffer(line, distance)
      return vb.getResult()
    }

    /**
     * Computes a list of values for the points along a line by
     * interpolating between values for the start and end point.
     *
     * @return the array of interpolated values
     */
    private fun interpolate(line: LineString, startValue: Double, endValue: Double): DoubleArray {
      val startV = abs(startValue)
      val endV = abs(endValue)
      val values = DoubleArray(line.getNumPoints())
      values[0] = startV
      values[values.size - 1] = endV

      val totalLen = line.getLength()
      val pts = line.getCoordinates()
      var currLen = 0.0
      for (i in 1 until values.size - 1) {
        val segLen = pts[i].distance(pts[i - 1])
        currLen += segLen
        val lenFrac = currLen / totalLen
        val delta = lenFrac * (endV - startV)
        values[i] = startV + delta
      }
      return values
    }

    /**
     * Computes a list of values for the points along a line by
     * interpolating between values for the start, middle and end points.
     *
     * @return the array of interpolated values
     */
    private fun interpolate(line: LineString, startValue: Double, midValue: Double, endValue: Double): DoubleArray {
      val startV = abs(startValue)
      val midV = abs(midValue)
      val endV = abs(endValue)

      val values = DoubleArray(line.getNumPoints())
      values[0] = startV
      values[values.size - 1] = endV

      val pts = line.getCoordinates()
      val lineLen = line.getLength()
      val midIndex = indexAtLength(pts, lineLen / 2)

      val delMidStart = midV - startV
      val delEndMid = endV - midV

      val lenSM = length(pts, 0, midIndex)
      var currLen = 0.0
      for (i in 1..midIndex) {
        val segLen = pts[i].distance(pts[i - 1])
        currLen += segLen
        val lenFrac = currLen / lenSM
        val value = startV + lenFrac * delMidStart
        values[i] = value
      }

      val lenME = length(pts, midIndex, pts.size - 1)
      currLen = 0.0
      for (i in midIndex + 1 until values.size - 1) {
        val segLen = pts[i].distance(pts[i - 1])
        currLen += segLen
        val lenFrac = currLen / lenME
        val value = midV + lenFrac * delEndMid
        values[i] = value
      }
      return values
    }

    private fun indexAtLength(pts: Array<Coordinate>, targetLen: Double): Int {
      var len = 0.0
      for (i in 1 until pts.size) {
        len += pts[i].distance(pts[i - 1])
        if (len > targetLen) return i
      }
      return pts.size - 1
    }

    private fun length(pts: Array<Coordinate>, i1: Int, i2: Int): Double {
      var len = 0.0
      for (i in i1 + 1..i2) {
        len += pts[i].distance(pts[i - 1])
      }
      return len
    }

    /**
     * Computes the two circumference points defining the outer tangent line
     * between two circles.
     *
     * @return the outer tangent line segment, or null if none exists
     */
    private fun outerTangent(c1: Coordinate, r1: Double, c2: Coordinate, r2: Double): LineSegment? {
      /**
       * If distances are inverted then flip to compute and flip result back.
       */
      if (r1 > r2) {
        val seg = outerTangent(c2, r2, c1, r1)!!
        return LineSegment(seg.p1, seg.p0)
      }
      val x1 = c1.getX()
      val y1 = c1.getY()
      val x2 = c2.getX()
      val y2 = c2.getY()
      val a3 = -atan2(y2 - y1, x2 - x1)

      val dr = r2 - r1
      val d = sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1))

      val a2 = asin(dr / d)
      // check if no tangent exists
      if (a2.isNaN()) return null

      val a1 = a3 - a2

      val aa = PI / 2 - a1
      val x3 = x1 + r1 * cos(aa)
      val y3 = y1 + r1 * sin(aa)
      val x4 = x2 + r2 * cos(aa)
      val y4 = y2 + r2 * sin(aa)

      return LineSegment(x3, y3, x4, y4)
    }

    private fun projectPolar(p: Coordinate, r: Double, ang: Double): Coordinate {
      val x = p.getX() + r * snapTrig(cos(ang))
      val y = p.getY() + r * snapTrig(sin(ang))
      return Coordinate(x, y)
    }

    private const val SNAP_TRIG_TOL = 1e-6

    /**
     * Snap trig values to integer values for better consistency.
     */
    private fun snapTrig(x: Double): Double {
      if (x > (1 - SNAP_TRIG_TOL)) return 1.0
      if (x < (-1 + SNAP_TRIG_TOL)) return -1.0
      if (abs(x) < SNAP_TRIG_TOL) return 0.0
      return x
    }
  }
}
