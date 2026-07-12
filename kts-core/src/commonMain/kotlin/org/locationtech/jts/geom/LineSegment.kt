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
package org.locationtech.jts.geom

import kotlin.jvm.JvmStatic
import kotlin.jvm.JvmField
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min


import org.locationtech.jts.algorithm.Distance
import org.locationtech.jts.algorithm.Intersection
import org.locationtech.jts.algorithm.LineIntersector
import org.locationtech.jts.algorithm.Orientation
import org.locationtech.jts.algorithm.RobustLineIntersector
import org.locationtech.jts.io.WKTConstants

/**
 * Represents a line segment defined by two {@link Coordinate}s.
 * Provides methods to compute various geometric properties
 * and relationships of line segments.
 * <p>
 * This class is designed to be easily mutable (to the extent of
 * having its contained points public).
 * This supports a common pattern of reusing a single LineSegment
 * object as a way of computing segment properties on the
 * segments defined by arrays or lists of {@link Coordinate}s.
 *
 * @version 1.7
 */
open class LineSegment(
  @JvmField var p0: Coordinate,
  @JvmField var p1: Coordinate
) : Comparable<Any?> {

  constructor(x0: Double, y0: Double, x1: Double, y1: Double) : this(Coordinate(x0, y0), Coordinate(x1, y1))

  constructor(ls: LineSegment) : this(ls.p0, ls.p1)

  constructor() : this(Coordinate(), Coordinate())

  open fun getCoordinate(i: Int): Coordinate {
    if (i == 0) return p0
    return p1
  }

  open fun setCoordinates(ls: LineSegment) {
    setCoordinates(ls.p0, ls.p1)
  }

  open fun setCoordinates(p0: Coordinate, p1: Coordinate) {
    this.p0.x = p0.x
    this.p0.y = p0.y
    this.p1.x = p1.x
    this.p1.y = p1.y
  }

  /**
   * Gets the minimum X ordinate.
   * @return the minimum X ordinate
   */
  open fun minX(): Double {
    return min(p0.x, p1.x)
  }

  /**
   * Gets the maximum X ordinate.
   * @return the maximum X ordinate
   */
  open fun maxX(): Double {
    return max(p0.x, p1.x)
  }

  /**
   * Gets the minimum Y ordinate.
   * @return the minimum Y ordinate
   */
  open fun minY(): Double {
    return min(p0.y, p1.y)
  }

  /**
   * Gets the maximum Y ordinate.
   * @return the maximum Y ordinate
   */
  open fun maxY(): Double {
    return max(p0.y, p1.y)
  }

  /**
   * Computes the length of the line segment.
   * @return the length of the line segment
   */
  open fun getLength(): Double {
    return p0.distance(p1)
  }

  /**
   * Tests whether the segment is horizontal.
   *
   * @return <code>true</code> if the segment is horizontal
   */
  open fun isHorizontal(): Boolean {
    return p0.y == p1.y
  }

  /**
   * Tests whether the segment is vertical.
   *
   * @return <code>true</code> if the segment is vertical
   */
  open fun isVertical(): Boolean {
    return p0.x == p1.x
  }

  /**
   * Determines the orientation of a LineSegment relative to this segment.
   * The concept of orientation is specified as follows:
   * Given two line segments A and L,
   * <ul>
   * <li>A is to the left of a segment L if A lies wholly in the
   * closed half-plane lying to the left of L
   * <li>A is to the right of a segment L if A lies wholly in the
   * closed half-plane lying to the right of L
   * <li>otherwise, A has indeterminate orientation relative to L. This
   * happens if A is collinear with L or if A crosses the line determined by L.
   * </ul>
   *
   * @param seg the LineSegment to compare
   *
   * @return 1 if <code>seg</code> is to the left of this segment
   * @return -1 if <code>seg</code> is to the right of this segment
   * @return 0 if <code>seg</code> is collinear to or crosses this segment
   */
  open fun orientationIndex(seg: LineSegment): Int {
    val orient0 = Orientation.index(p0, p1, seg.p0)
    val orient1 = Orientation.index(p0, p1, seg.p1)
    // this handles the case where the points are L or collinear
    if (orient0 >= 0 && orient1 >= 0)
      return max(orient0, orient1)
    // this handles the case where the points are R or collinear
    if (orient0 <= 0 && orient1 <= 0)
      return min(orient0, orient1)
    // points lie on opposite sides ==> indeterminate orientation
    return 0
  }

  /**
   * Determines the orientation index of a {@link Coordinate} relative to this segment.
   * The orientation index is as defined in {@link Orientation#index(Coordinate, Coordinate, Coordinate)}.
   *
   * @param p the coordinate to compare
   *
   * @return 1 (LEFT) if <code>p</code> is to the left of this segment
   * @return -1 (RIGHT) if <code>p</code> is to the right of this segment
   * @return 0 (COLLINEAR) if <code>p</code> is collinear with this segment
   *
   * @see Orientation#index(Coordinate, Coordinate, Coordinate)
   */
  open fun orientationIndex(p: Coordinate): Int {
    return Orientation.index(p0, p1, p)
  }

  /**
   * Reverses the direction of the line segment.
   */
  open fun reverse() {
    val temp = p0
    p0 = p1
    p1 = temp
  }

  /**
   * Puts the line segment into a normalized form.
   * This is useful for using line segments in maps and indexes when
   * topological equality rather than exact equality is desired.
   * A segment in normalized form has the first point smaller
   * than the second (according to the standard ordering on {@link Coordinate}).
   */
  open fun normalize() {
    if (p1.compareTo(p0) < 0) reverse()
  }

  /**
   * Computes the angle that the vector defined by this segment
   * makes with the X-axis.
   * The angle will be in the range [ -PI, PI ] radians.
   *
   * @return the angle this segment makes with the X-axis (in radians)
   */
  open fun angle(): Double {
    return atan2(p1.y - p0.y, p1.x - p0.x)
  }

  /**
   * Computes the midpoint of the segment
   *
   * @return the midpoint of the segment
   */
  open fun midPoint(): Coordinate {
    return midPoint(p0, p1)
  }

  /**
   * Computes the distance between this line segment and another segment.
   *
   * @return the distance to the other segment
   */
  open fun distance(ls: LineSegment): Double {
    return Distance.segmentToSegment(p0, p1, ls.p0, ls.p1)
  }

  /**
   * Computes the distance between this line segment and a given point.
   *
   * @return the distance from this segment to the given point
   */
  open fun distance(p: Coordinate): Double {
    return Distance.pointToSegment(p, p0, p1)
  }

  /**
   * Computes the perpendicular distance between the (infinite) line defined
   * by this line segment and a point.
   * If the segment has zero length this returns the distance between
   * the segment and the point.
   *
   * @param p the point to compute the distance to
   * @return the perpendicular distance between the line and point
   */
  open fun distancePerpendicular(p: Coordinate): Double {
    if (p0.equals2D(p1))
      return p0.distance(p)
    return Distance.pointToLinePerpendicular(p, p0, p1)
  }

  /**
   * Computes the oriented perpendicular distance between the (infinite) line
   * defined by this line segment and a point.
   * The oriented distance is positive if the point on the left of the line,
   * and negative if it is on the right.
   * If the segment has zero length this returns the distance between
   * the segment and the point.
   *
   * @param p the point to compute the distance to
   * @return the oriented perpendicular distance between the line and point
   */
  open fun distancePerpendicularOriented(p: Coordinate): Double {
    if (p0.equals2D(p1))
      return p0.distance(p)
    val dist = distancePerpendicular(p)
    if (orientationIndex(p) < 0)
      return -dist
    return dist
  }

  /**
   * Computes the {@link Coordinate} that lies a given
   * fraction along the line defined by this segment.
   * A fraction of <code>0.0</code> returns the start point of the segment;
   * a fraction of <code>1.0</code> returns the end point of the segment.
   * If the fraction is &lt; 0.0 or &gt; 1.0 the point returned
   * will lie before the start or beyond the end of the segment.
   *
   * @param segmentLengthFraction the fraction of the segment length along the line
   * @return the point at that distance
   */
  open fun pointAlong(segmentLengthFraction: Double): Coordinate {
    val coord = p0.create()
    coord.x = p0.x + segmentLengthFraction * (p1.x - p0.x)
    coord.y = p0.y + segmentLengthFraction * (p1.y - p0.y)
    return coord
  }

  /**
   * Computes the {@link Coordinate} that lies a given
   * fraction along the line defined by this segment and offset from
   * the segment by a given distance.
   * A fraction of <code>0.0</code> offsets from the start point of the segment;
   * a fraction of <code>1.0</code> offsets from the end point of the segment.
   * The computed point is offset to the left of the line if the offset distance is
   * positive, to the right if negative.
   *
   * @param segmentLengthFraction the fraction of the segment length along the line
   * @param offsetDistance the distance the point is offset from the segment
   *    (positive is to the left, negative is to the right)
   * @return the point at that distance and offset
   *
   * @throws IllegalStateException if the segment has zero length
   */
  open fun pointAlongOffset(segmentLengthFraction: Double, offsetDistance: Double): Coordinate {
    // the point on the segment line
    val segx = p0.x + segmentLengthFraction * (p1.x - p0.x)
    val segy = p0.y + segmentLengthFraction * (p1.y - p0.y)

    val dx = p1.x - p0.x
    val dy = p1.y - p0.y
    val len = hypot(dx, dy)
    var ux = 0.0
    var uy = 0.0
    if (offsetDistance != 0.0) {
      if (len <= 0.0)
        throw IllegalStateException("Cannot compute offset from zero-length line segment")

      // u is the vector that is the length of the offset, in the direction of the segment
      ux = offsetDistance * dx / len
      uy = offsetDistance * dy / len
    }

    // the offset point is the seg point plus the offset vector rotated 90 degrees CCW
    val offsetx = segx - uy
    val offsety = segy + ux

    val coord = p0.create()
    coord.setX(offsetx)
    coord.setY(offsety)
    return coord
  }

  /**
   * Computes the Projection Factor for the projection of the point p
   * onto this LineSegment.  The Projection Factor is the constant r
   * by which the vector for this segment must be multiplied to
   * equal the vector for the projection of <tt>p</tt> on the line
   * defined by this segment.
   * <p>
   * The projection factor will lie in the range <tt>(-inf, +inf)</tt>,
   * or be <code>NaN</code> if the line segment has zero length..
   *
   * @param p the point to compute the factor for
   * @return the projection factor for the point
   */
  open fun projectionFactor(p: Coordinate): Double {
    if (p.equals(p0)) return 0.0
    if (p.equals(p1)) return 1.0
    // Otherwise, use comp.graphics.algorithms Frequently Asked Questions method
    /*     	      AC dot AB
                   r = ---------
                         ||AB||^2
                r has the following meaning:
                r=0 P = A
                r=1 P = B
                r<0 P is on the backward extension of AB
                r>1 P is on the forward extension of AB
                0<r<1 P is interior to AB
        */
    val dx = p1.x - p0.x
    val dy = p1.y - p0.y
    val len = dx * dx + dy * dy

    // handle zero-length segments
    if (len <= 0.0) return Double.NaN

    val r = ((p.x - p0.x) * dx + (p.y - p0.y) * dy) / len
    return r
  }

  /**
   * Computes the fraction of distance (in <tt>[0.0, 1.0]</tt>)
   * that the projection of a point occurs along this line segment.
   * If the point is beyond either ends of the line segment,
   * the closest fractional value (<tt>0.0</tt> or <tt>1.0</tt>) is returned.
   * <p>
   * Essentially, this is the {@link #projectionFactor} clamped to
   * the range <tt>[0.0, 1.0]</tt>.
   * If the segment has zero length, 1.0 is returned.
   *
   * @param inputPt the point
   * @return the fraction along the line segment the projection of the point occurs
   */
  open fun segmentFraction(
    inputPt: Coordinate
  ): Double {
    var segFrac = projectionFactor(inputPt)
    if (segFrac < 0.0)
      segFrac = 0.0
    else if (segFrac > 1.0 || segFrac.isNaN())
      segFrac = 1.0
    return segFrac
  }

  /**
   * Compute the projection of a point onto the line determined
   * by this line segment.
   * <p>
   * Note that the projected point
   * may lie outside the line segment.  If this is the case,
   * the projection factor will lie outside the range [0.0, 1.0].
   */
  open fun project(p: Coordinate): Coordinate {
    if (p.equals(p0) || p.equals(p1)) return p.copy()

    val r = projectionFactor(p)
    return project(p, r)
  }

  private fun project(p: Coordinate, projectionFactor: Double): Coordinate {
    val coord = p.copy()
    coord.x = p0.x + projectionFactor * (p1.x - p0.x)
    coord.y = p0.y + projectionFactor * (p1.y - p0.y)
    return coord
  }

  /**
   * Project a line segment onto this line segment and return the resulting
   * line segment.  The returned line segment will be a subset of
   * the target line line segment.  This subset may be null, if
   * the segments are oriented in such a way that there is no projection.
   * <p>
   * Note that the returned line may have zero length (i.e. the same endpoints).
   * This can happen for instance if the lines are perpendicular to one another.
   *
   * @param seg the line segment to project
   * @return the projected line segment, or <code>null</code> if there is no overlap
   */
  open fun project(seg: LineSegment): LineSegment? {
    val pf0 = projectionFactor(seg.p0)
    val pf1 = projectionFactor(seg.p1)
    // check if segment projects at all
    if (pf0 >= 1.0 && pf1 >= 1.0) return null
    if (pf0 <= 0.0 && pf1 <= 0.0) return null

    var newp0 = project(seg.p0, pf0)
    if (pf0 < 0.0) newp0 = p0
    if (pf0 > 1.0) newp0 = p1

    var newp1 = project(seg.p1, pf1)
    if (pf1 < 0.0) newp1 = p0
    if (pf1 > 1.0) newp1 = p1

    return LineSegment(newp0, newp1)
  }

  /**
   * Computes the {@link LineSegment} that is offset from
   * the segment by a given distance.
   * The computed segment is offset to the left of the line if the offset distance is
   * positive, to the right if negative.
   *
   * @param offsetDistance the distance the point is offset from the segment
   *    (positive is to the left, negative is to the right)
   * @return a line segment offset by the specified distance
   *
   * @throws IllegalStateException if the segment has zero length
   */
  open fun offset(offsetDistance: Double): LineSegment {
    val offset0 = pointAlongOffset(0.0, offsetDistance)
    val offset1 = pointAlongOffset(1.0, offsetDistance)
    return LineSegment(offset0, offset1)
  }

  /**
   * Computes the reflection of a point in the line defined
   * by this line segment.
   *
   * @param p the point to reflect
   * @return the reflected point
   */
  open fun reflect(p: Coordinate): Coordinate {
    // general line equation
    val A = p1.getY() - p0.getY()
    val B = p0.getX() - p1.getX()
    val C = p0.getY() * (p1.getX() - p0.getX()) - p0.getX() * (p1.getY() - p0.getY())

    // compute reflected point
    val A2plusB2 = A * A + B * B
    val A2subB2 = A * A - B * B

    val x = p.getX()
    val y = p.getY()
    val rx = (-A2subB2 * x - 2 * A * B * y - 2 * A * C) / A2plusB2
    val ry = (A2subB2 * y - 2 * A * B * x - 2 * B * C) / A2plusB2

    val coord = p.copy()
    coord.setX(rx)
    coord.setY(ry)
    return coord
  }

  /**
   * Computes the closest point on this line segment to another point.
   * @param p the point to find the closest point to
   * @return a Coordinate which is the closest point on the line segment to the point p
   */
  open fun closestPoint(p: Coordinate): Coordinate {
    val factor = projectionFactor(p)
    if (factor > 0 && factor < 1) {
      return project(p, factor)
    }
    val dist0 = p0.distance(p)
    val dist1 = p1.distance(p)
    if (dist0 < dist1)
      return p0
    return p1
  }

  /**
   * Computes the closest points on two line segments.
   *
   * @param line the segment to find the closest point to
   * @return a pair of Coordinates which are the closest points on the line segments
   */
  open fun closestPoints(line: LineSegment): Array<Coordinate> {
    // test for intersection
    val intPt = intersection(line)
    if (intPt != null) {
      return arrayOf(intPt, intPt)
    }

    /**
     *  if no intersection closest pair contains at least one endpoint.
     * Test each endpoint in turn.
     */
    @Suppress("UNCHECKED_CAST")
    val closestPt = arrayOfNulls<Coordinate>(2) as Array<Coordinate>
    var minDistance = Double.MAX_VALUE
    var dist: Double

    val close00 = closestPoint(line.p0)
    minDistance = close00.distance(line.p0)
    closestPt[0] = close00
    closestPt[1] = line.p0

    val close01 = closestPoint(line.p1)
    dist = close01.distance(line.p1)
    if (dist < minDistance) {
      minDistance = dist
      closestPt[0] = close01
      closestPt[1] = line.p1
    }

    val close10 = line.closestPoint(p0)
    dist = close10.distance(p0)
    if (dist < minDistance) {
      minDistance = dist
      closestPt[0] = p0
      closestPt[1] = close10
    }

    val close11 = line.closestPoint(p1)
    dist = close11.distance(p1)
    if (dist < minDistance) {
      minDistance = dist
      closestPt[0] = p1
      closestPt[1] = close11
    }

    return closestPt
  }

  /**
   * Computes an intersection point between two line segments, if there is one.
   * There may be 0, 1 or many intersection points between two segments.
   * If there are 0, null is returned. If there is 1 or more,
   * exactly one of them is returned
   * (chosen at the discretion of the algorithm).
   * If more information is required about the details of the intersection,
   * the {@link RobustLineIntersector} class should be used.
   *
   * @param line a line segment
   * @return an intersection point, or <code>null</code> if there is none
   *
   * @see RobustLineIntersector
   */
  open fun intersection(line: LineSegment): Coordinate? {
    val li = RobustLineIntersector()
    li.computeIntersection(p0, p1, line.p0, line.p1)
    if (li.hasIntersection())
      return li.getIntersection(0)
    return null
  }

  /**
   * Computes the intersection point of the lines of infinite extent defined
   * by two line segments (if there is one).
   * There may be 0, 1 or an infinite number of intersection points
   * between two lines.
   * If there is a unique intersection point, it is returned.
   * Otherwise, <tt>null</tt> is returned.
   * If more information is required about the details of the intersection,
   * the {@link RobustLineIntersector} class should be used.
   *
   * @param line a line segment defining an straight line with infinite extent
   * @return an intersection point,
   * or <code>null</code> if there is no point of intersection
   * or an infinite number of intersection points
   *
   * @see RobustLineIntersector
   */
  open fun lineIntersection(line: LineSegment): Coordinate? {
    val intPt = Intersection.intersection(p0, p1, line.p0, line.p1)
    return intPt
  }

  /**
   * Creates a LineString with the same coordinates as this segment
   *
   * @param geomFactory the geometry factory to use
   * @return a LineString with the same geometry as this segment
   */
  open fun toGeometry(geomFactory: GeometryFactory): LineString {
    return geomFactory.createLineString(arrayOf(p0, p1))
  }

  /**
   *  Returns <code>true</code> if <code>other</code> has the same values for
   *  its points.
   *
   * @param  o  a <code>LineSegment</code> with which to do the comparison.
   * @return        <code>true</code> if <code>other</code> is a <code>LineSegment</code>
   *      with the same values for the x and y ordinates.
   */
  override fun equals(o: Any?): Boolean {
    if (o !is LineSegment) {
      return false
    }
    val other = o
    return p0.equals(other.p0) && p1.equals(other.p1)
  }

  /**
   * Gets a hashcode for this object.
   *
   * @return a hashcode for this object
   */
  override fun hashCode(): Int {
    var hash = 17
    hash = hash * 29 + p0.x.hashCode()
    hash = hash * 29 + p0.y.hashCode()
    hash = hash * 29 + p1.x.hashCode()
    hash = hash * 29 + p1.y.hashCode()
    return hash
  }

  open fun OLDhashCode(): Int {
    var bits0 = p0.x.toBits()
    bits0 = bits0 xor (p0.y.toBits() * 31)
    val hash0 = (bits0.toInt() xor (bits0 shr 32).toInt())

    var bits1 = p1.x.toBits()
    bits1 = bits1 xor (p1.y.toBits() * 31)
    val hash1 = (bits1.toInt() xor (bits1 shr 32).toInt())

    // XOR is supposed to be a good way to combine hashcodes
    return hash0 xor hash1
  }

  /**
   *  Compares this object with the specified object for order.
   *  Uses the standard lexicographic ordering for the points in the LineSegment.
   *
   * @param  o  the <code>LineSegment</code> with which this <code>LineSegment</code>
   *      is being compared
   * @return    a negative integer, zero, or a positive integer as this <code>LineSegment</code>
   *      is less than, equal to, or greater than the specified <code>LineSegment</code>
   */
  override fun compareTo(o: Any?): Int {
    val other = o as LineSegment
    val comp0 = p0.compareTo(other.p0)
    if (comp0 != 0) return comp0
    return p1.compareTo(other.p1)
  }

  /**
   *  Returns <code>true</code> if <code>other</code> is
   *  topologically equal to this LineSegment (e.g. irrespective
   *  of orientation).
   *
   * @param  other  a <code>LineSegment</code> with which to do the comparison.
   * @return        <code>true</code> if <code>other</code> is a <code>LineSegment</code>
   *      with the same values for the x and y ordinates.
   */
  open fun equalsTopo(other: LineSegment): Boolean {
    return (p0.equals(other.p0) && p1.equals(other.p1) ||
      p0.equals(other.p1) && p1.equals(other.p0))
  }

  override fun toString(): String {
    return (WKTConstants.LINESTRING + " (" +
      p0.x + " " + p0.y +
      ", " +
      p1.x + " " + p1.y + ")")
  }

  companion object {

    /**
     * Computes the midpoint of a segment
     *
     * @return the midpoint of the segment
     */
    @JvmStatic
    fun midPoint(p0: Coordinate, p1: Coordinate): Coordinate {
      return Coordinate(
        (p0.x + p1.x) / 2,
        (p0.y + p1.y) / 2
      )
    }
  }
}
