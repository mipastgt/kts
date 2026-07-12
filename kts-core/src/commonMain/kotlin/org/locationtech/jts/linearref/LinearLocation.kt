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

package org.locationtech.jts.linearref

import kotlin.jvm.JvmStatic

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.LineSegment
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.MultiLineString

/**
 * Represents a location along a [LineString] or [MultiLineString].
 * The referenced geometry is not maintained within
 * this location, but must be provided for operations which require it.
 * Various methods are provided to manipulate the location value
 * and query the geometry it references.
 */
class LinearLocation : Comparable<Any?> {

  private var componentIndex = 0
  private var segmentIndex = 0
  private var segmentFraction = 0.0

  /**
   * Creates a location referring to the start of a linear geometry
   */
  constructor()

  constructor(segmentIndex: Int, segmentFraction: Double) : this(0, segmentIndex, segmentFraction)

  constructor(componentIndex: Int, segmentIndex: Int, segmentFraction: Double) {
    this.componentIndex = componentIndex
    this.segmentIndex = segmentIndex
    this.segmentFraction = segmentFraction
    normalize()
  }

  private constructor(componentIndex: Int, segmentIndex: Int, segmentFraction: Double, doNormalize: Boolean) {
    this.componentIndex = componentIndex
    this.segmentIndex = segmentIndex
    this.segmentFraction = segmentFraction
    if (doNormalize)
      normalize()
  }

  /**
   * Creates a new location equal to a given one.
   *
   * @param loc a LinearLocation
   */
  constructor(loc: LinearLocation) {
    this.componentIndex = loc.componentIndex
    this.segmentIndex = loc.segmentIndex
    this.segmentFraction = loc.segmentFraction
  }

  /**
   * Ensures the individual values are locally valid.
   * Does **not** ensure that the indexes are valid for
   * a particular linear geometry.
   *
   * @see clamp
   */
  private fun normalize() {
    if (segmentFraction < 0.0) {
      segmentFraction = 0.0
    }
    if (segmentFraction > 1.0) {
      segmentFraction = 1.0
    }

    if (componentIndex < 0) {
      componentIndex = 0
      segmentIndex = 0
      segmentFraction = 0.0
    }
    if (segmentIndex < 0) {
      segmentIndex = 0
      segmentFraction = 0.0
    }
    if (segmentFraction == 1.0) {
      segmentFraction = 0.0
      segmentIndex += 1
    }
  }

  /**
   * Ensures the indexes are valid for a given linear [Geometry].
   *
   * @param linear a linear geometry
   */
  fun clamp(linear: Geometry) {
    if (componentIndex >= linear.getNumGeometries()) {
      setToEnd(linear)
      return
    }
    if (segmentIndex >= linear.getNumPoints()) {
      val line = linear.getGeometryN(componentIndex) as LineString
      segmentIndex = numSegments(line)
      segmentFraction = 1.0
    }
  }

  /**
   * Snaps the value of this location to
   * the nearest vertex on the given linear [Geometry],
   * if the vertex is closer than `minDistance`.
   *
   * @param linearGeom a linear geometry
   * @param minDistance the minimum allowable distance to a vertex
   */
  fun snapToVertex(linearGeom: Geometry, minDistance: Double) {
    if (segmentFraction <= 0.0 || segmentFraction >= 1.0)
      return
    val segLen = getSegmentLength(linearGeom)
    val lenToStart = segmentFraction * segLen
    val lenToEnd = segLen - lenToStart
    if (lenToStart <= lenToEnd && lenToStart < minDistance) {
      segmentFraction = 0.0
    } else if (lenToEnd <= lenToStart && lenToEnd < minDistance) {
      segmentFraction = 1.0
    }
  }

  /**
   * Gets the length of the segment in the given
   * Geometry containing this location.
   *
   * @param linearGeom a linear geometry
   * @return the length of the segment
   */
  fun getSegmentLength(linearGeom: Geometry): Double {
    val lineComp = linearGeom.getGeometryN(componentIndex) as LineString

    // ensure segment index is valid
    var segIndex = segmentIndex
    if (segmentIndex >= numSegments(lineComp))
      segIndex = lineComp.getNumPoints() - 2

    val p0 = lineComp.getCoordinateN(segIndex)
    val p1 = lineComp.getCoordinateN(segIndex + 1)
    return p0.distance(p1)
  }

  /**
   * Sets the value of this location to
   * refer to the end of a linear geometry.
   *
   * @param linear the linear geometry to use to set the end
   */
  fun setToEnd(linear: Geometry) {
    componentIndex = linear.getNumGeometries() - 1
    val lastLine = linear.getGeometryN(componentIndex) as LineString
    segmentIndex = numSegments(lastLine)
    segmentFraction = 0.0
  }

  /**
   * Gets the component index for this location.
   *
   * @return the component index
   */
  fun getComponentIndex(): Int = componentIndex

  /**
   * Gets the segment index for this location
   *
   * @return the segment index
   */
  fun getSegmentIndex(): Int = segmentIndex

  /**
   * Gets the segment fraction for this location
   *
   * @return the segment fraction
   */
  fun getSegmentFraction(): Double = segmentFraction

  /**
   * Tests whether this location refers to a vertex
   *
   * @return true if the location is a vertex
   */
  fun isVertex(): Boolean {
    return segmentFraction <= 0.0 || segmentFraction >= 1.0
  }

  /**
   * Gets the [Coordinate] along the
   * given linear [Geometry] which is
   * referenced by this location.
   *
   * @param linearGeom the linear geometry referenced by this location
   * @return the `Coordinate` at the location
   */
  fun getCoordinate(linearGeom: Geometry): Coordinate {
    val lineComp = linearGeom.getGeometryN(componentIndex) as LineString
    val p0 = lineComp.getCoordinateN(segmentIndex)
    if (segmentIndex >= numSegments(lineComp))
      return p0
    val p1 = lineComp.getCoordinateN(segmentIndex + 1)
    return pointAlongSegmentByFraction(p0, p1, segmentFraction)
  }

  /**
   * Gets a [LineSegment] representing the segment of the
   * given linear [Geometry] which contains this location.
   *
   * @param linearGeom a linear geometry
   * @return the `LineSegment` containing the location
   */
  fun getSegment(linearGeom: Geometry): LineSegment {
    val lineComp = linearGeom.getGeometryN(componentIndex) as LineString
    val p0 = lineComp.getCoordinateN(segmentIndex)
    // check for endpoint - return last segment of the line if so
    if (segmentIndex >= numSegments(lineComp)) {
      val prev = lineComp.getCoordinateN(lineComp.getNumPoints() - 2)
      return LineSegment(prev, p0)
    }
    val p1 = lineComp.getCoordinateN(segmentIndex + 1)
    return LineSegment(p0, p1)
  }

  /**
   * Tests whether this location refers to a valid
   * location on the given linear [Geometry].
   *
   * @param linearGeom a linear geometry
   * @return true if this location is valid
   */
  fun isValid(linearGeom: Geometry): Boolean {
    if (componentIndex < 0 || componentIndex >= linearGeom.getNumGeometries())
      return false

    val lineComp = linearGeom.getGeometryN(componentIndex) as LineString
    if (segmentIndex < 0 || segmentIndex > lineComp.getNumPoints())
      return false
    if (segmentIndex == lineComp.getNumPoints() && segmentFraction != 0.0)
      return false

    if (segmentFraction < 0.0 || segmentFraction > 1.0)
      return false
    return true
  }

  /**
   *  Compares this object with the specified object for order.
   *
   * @param  o  the `LineStringLocation` with which this `Coordinate`
   *      is being compared
   * @return    a negative integer, zero, or a positive integer as this `LineStringLocation`
   *      is less than, equal to, or greater than the specified `LineStringLocation`
   */
  override fun compareTo(o: Any?): Int {
    val other = o as LinearLocation
    // compare component indices
    if (componentIndex < other.componentIndex) return -1
    if (componentIndex > other.componentIndex) return 1
    // compare segments
    if (segmentIndex < other.segmentIndex) return -1
    if (segmentIndex > other.segmentIndex) return 1
    // same segment, so compare segment fraction
    if (segmentFraction < other.segmentFraction) return -1
    if (segmentFraction > other.segmentFraction) return 1
    // same location
    return 0
  }

  /**
   *  Compares this object with the specified index values for order.
   *
   * @param componentIndex1 a component index
   * @param segmentIndex1 a segment index
   * @param segmentFraction1 a segment fraction
   * @return    a negative integer, zero, or a positive integer as this `LineStringLocation`
   *      is less than, equal to, or greater than the specified locationValues
   */
  fun compareLocationValues(componentIndex1: Int, segmentIndex1: Int, segmentFraction1: Double): Int {
    // compare component indices
    if (componentIndex < componentIndex1) return -1
    if (componentIndex > componentIndex1) return 1
    // compare segments
    if (segmentIndex < segmentIndex1) return -1
    if (segmentIndex > segmentIndex1) return 1
    // same segment, so compare segment fraction
    if (segmentFraction < segmentFraction1) return -1
    if (segmentFraction > segmentFraction1) return 1
    // same location
    return 0
  }

  /**
   * Tests whether two locations
   * are on the same segment in the parent [Geometry].
   *
   * @param loc a location on the same geometry
   * @return true if the locations are on the same segment of the parent geometry
   */
  fun isOnSameSegment(loc: LinearLocation): Boolean {
    if (componentIndex != loc.componentIndex) return false
    if (segmentIndex == loc.segmentIndex) return true
    if (loc.segmentIndex - segmentIndex == 1
        && loc.segmentFraction == 0.0)
      return true
    if (segmentIndex - loc.segmentIndex == 1
        && segmentFraction == 0.0)
      return true
    return false
  }

  /**
   * Tests whether this location is an endpoint of
   * the linear component it refers to.
   *
   * @param linearGeom the linear geometry referenced by this location
   * @return true if the location is a component endpoint
   */
  fun isEndpoint(linearGeom: Geometry): Boolean {
    val lineComp = linearGeom.getGeometryN(componentIndex) as LineString
    // check for endpoint
    val nseg = numSegments(lineComp)
    return segmentIndex >= nseg
        || (segmentIndex == nseg - 1 && segmentFraction >= 1.0)
  }

  /**
   * Converts a linear location to the lowest equivalent location index.
   * The lowest index has the lowest possible component and segment indices.
   *
   * @param linearGeom the linear geometry referenced by this location
   * @return the lowest equivalent location
   */
  fun toLowest(linearGeom: Geometry): LinearLocation {
    // TODO: compute lowest component index
    val lineComp = linearGeom.getGeometryN(componentIndex) as LineString
    val nseg = numSegments(lineComp)
    // if not an endpoint can be returned directly
    if (segmentIndex < nseg) return this
    return LinearLocation(componentIndex, nseg - 1, 1.0, false)
  }

  /**
   * Copies this location
   *
   * @return a copy of this location
   */
  @Deprecated("")
  fun clone(): Any {
    return copy()
  }

  /**
   * Copies this location
   *
   * @return a copy of this location
   */
  fun copy(): LinearLocation {
    return LinearLocation(componentIndex, segmentIndex, segmentFraction)
  }

  override fun toString(): String {
    return ("LinearLoc["
        + componentIndex + ", "
        + segmentIndex + ", "
        + segmentFraction + "]")
  }

  companion object {
    /**
     * Gets a location which refers to the end of a linear [Geometry].
     * @param linear the linear geometry
     * @return a new `LinearLocation`
     */
    @JvmStatic
    fun getEndLocation(linear: Geometry): LinearLocation {
      // assert: linear is LineString or MultiLineString
      val loc = LinearLocation()
      loc.setToEnd(linear)
      return loc
    }

    /**
     * Computes the [Coordinate] of a point a given fraction
     * along the line segment `(p0, p1)`.
     * If the fraction is greater than 1.0 the last
     * point of the segment is returned.
     * If the fraction is less than or equal to 0.0 the first point
     * of the segment is returned.
     * The Z ordinate is interpolated from the Z-ordinates of the given points,
     * if they are specified.
     *
     * @param p0 the first point of the line segment
     * @param p1 the last point of the line segment
     * @param frac the length to the desired point
     * @return the `Coordinate` of the desired point
     */
    @JvmStatic
    fun pointAlongSegmentByFraction(p0: Coordinate, p1: Coordinate, frac: Double): Coordinate {
      if (frac <= 0.0) return p0
      if (frac >= 1.0) return p1

      val x = (p1.x - p0.x) * frac + p0.x
      val y = (p1.y - p0.y) * frac + p0.y
      // interpolate Z value. If either input Z is NaN, result z will be NaN as well.
      val z = (p1.getZ() - p0.getZ()) * frac + p0.getZ()
      return Coordinate(x, y, z)
    }

    /**
     *  Compares two sets of location values for order.
     *
     * @return    a negative integer, zero, or a positive integer
     *      as the first set of location values
     *      is less than, equal to, or greater than the second set of locationValues
     */
    @JvmStatic
    fun compareLocationValues(
        componentIndex0: Int, segmentIndex0: Int, segmentFraction0: Double,
        componentIndex1: Int, segmentIndex1: Int, segmentFraction1: Double): Int {
      // compare component indices
      if (componentIndex0 < componentIndex1) return -1
      if (componentIndex0 > componentIndex1) return 1
      // compare segments
      if (segmentIndex0 < segmentIndex1) return -1
      if (segmentIndex0 > segmentIndex1) return 1
      // same segment, so compare segment fraction
      if (segmentFraction0 < segmentFraction1) return -1
      if (segmentFraction0 > segmentFraction1) return 1
      // same location
      return 0
    }

    /**
     * Gets the count of the number of line segments
     * in a [LineString].  This is one less than the
     * number of coordinates.
     *
     * @param line a LineString
     * @return the number of segments
     */
    private fun numSegments(line: LineString): Int {
      val npts = line.getNumPoints()
      if (npts <= 1) return 0
      return npts - 1
    }
  }
}
