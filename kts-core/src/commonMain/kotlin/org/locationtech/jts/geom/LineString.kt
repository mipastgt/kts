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

import org.locationtech.jts.algorithm.Length
import org.locationtech.jts.operation.BoundaryOp

/**
 *  Models an OGC-style <code>LineString</code>.
 *  A LineString consists of a sequence of two or more vertices,
 *  along with all points along the linearly-interpolated curves
 *  (line segments) between each
 *  pair of consecutive vertices.
 *
 * @version 1.7
 */
open class LineString : Geometry, Lineal {

  /**
   *  The points of this <code>LineString</code>.
   */
  protected lateinit var points: CoordinateSequence

  /**
   *  Constructs a <code>LineString</code> with the given points.
   *
   * @throws IllegalArgumentException if too few points are provided
   */
  /** @deprecated Use GeometryFactory instead */
  constructor(points: Array<Coordinate>?, precisionModel: PrecisionModel, SRID: Int) : super(GeometryFactory(precisionModel, SRID)) {
    init(getFactory().getCoordinateSequenceFactory().create(points))
  }

  /**
   * Constructs a <code>LineString</code> with the given points.
   *
   * @throws IllegalArgumentException if too few points are provided
   */
  constructor(points: CoordinateSequence?, factory: GeometryFactory) : super(factory) {
    init(points)
  }

  private fun init(points: CoordinateSequence?) {
    var points = points
    if (points == null) {
      points = getFactory().getCoordinateSequenceFactory().create(arrayOf<Coordinate>())
    }
    if (points.size() > 0 && points.size() < MINIMUM_VALID_SIZE) {
      throw IllegalArgumentException(
        "Invalid number of points in LineString (found "
          + points.size() + " - must be 0 or >= " + MINIMUM_VALID_SIZE + ")"
      )
    }
    this.points = points
  }

  override fun getCoordinates(): Array<Coordinate> {
    return points.toCoordinateArray()
  }

  open fun getCoordinateSequence(): CoordinateSequence {
    return points
  }

  open fun getCoordinateN(n: Int): Coordinate {
    return points.getCoordinate(n)
  }

  override fun getCoordinate(): Coordinate? {
    if (isEmpty()) return null
    return points.getCoordinate(0)
  }

  override fun getDimension(): Int {
    return 1
  }

  override fun getBoundaryDimension(): Int {
    if (isClosed()) {
      return Dimension.FALSE
    }
    return 0
  }

  override fun isEmpty(): Boolean {
    return points.size() == 0
  }

  override fun getNumPoints(): Int {
    return points.size()
  }

  open fun getPointN(n: Int): Point {
    return getFactory().createPoint(points.getCoordinate(n))
  }

  open fun getStartPoint(): Point? {
    if (isEmpty()) {
      return null
    }
    return getPointN(0)
  }

  open fun getEndPoint(): Point? {
    if (isEmpty()) {
      return null
    }
    return getPointN(getNumPoints() - 1)
  }

  open fun isClosed(): Boolean {
    if (isEmpty()) {
      return false
    }
    return getCoordinateN(0).equals2D(getCoordinateN(getNumPoints() - 1))
  }

  open fun isRing(): Boolean {
    return isClosed() && isSimple()
  }

  override fun getGeometryType(): String {
    return Geometry.TYPENAME_LINESTRING
  }

  /**
   *  Returns the length of this <code>LineString</code>
   *
   * @return the length of the linestring
   */
  override fun getLength(): Double {
    return Length.ofLine(points)
  }

  /**
   * Gets the boundary of this geometry.
   * The boundary of a lineal geometry is always a zero-dimensional geometry (which may be empty).
   *
   * @return the boundary geometry
   * @see Geometry#getBoundary
   */
  override fun getBoundary(): Geometry {
    return (BoundaryOp(this)).getBoundary()
  }

  /**
   * Creates a {@link LineString} whose coordinates are in the reverse
   * order of this objects
   *
   * @return a {@link LineString} with coordinates in the reverse order
   */
  override fun reverse(): LineString {
    return super.reverse() as LineString
  }

  override fun reverseInternal(): LineString {
    val seq = points.copy()
    CoordinateSequences.reverse(seq)
    return getFactory().createLineString(seq)
  }

  /**
   *  Returns true if the given point is a vertex of this <code>LineString</code>.
   *
   * @param  pt  the <code>Coordinate</code> to check
   * @return     <code>true</code> if <code>pt</code> is one of this <code>LineString</code>
   *      's vertices
   */
  open fun isCoordinate(pt: Coordinate): Boolean {
    for (i in 0 until points.size()) {
      if (points.getCoordinate(i).equals(pt)) {
        return true
      }
    }
    return false
  }

  override fun computeEnvelopeInternal(): Envelope {
    if (isEmpty()) {
      return Envelope()
    }
    return points.expandEnvelope(Envelope())
  }

  override fun equalsExact(other: Geometry, tolerance: Double): Boolean {
    if (!isEquivalentClass(other)) {
      return false
    }
    val otherLineString = other as LineString
    if (points.size() != otherLineString.points.size()) {
      return false
    }
    for (i in 0 until points.size()) {
      if (!equal(points.getCoordinate(i), otherLineString.points.getCoordinate(i), tolerance)) {
        return false
      }
    }
    return true
  }

  override fun apply(filter: CoordinateFilter) {
    for (i in 0 until points.size()) {
      filter.filter(points.getCoordinate(i))
    }
  }

  override fun apply(filter: CoordinateSequenceFilter) {
    if (points.size() == 0)
      return
    for (i in 0 until points.size()) {
      filter.filter(points, i)
      if (filter.isDone())
        break
    }
    if (filter.isGeometryChanged())
      geometryChanged()
  }

  override fun apply(filter: GeometryFilter) {
    filter.filter(this)
  }

  override fun apply(filter: GeometryComponentFilter) {
    filter.filter(this)
  }

  /**
   * Creates and returns a full copy of this {@link LineString} object.
   * (including all coordinates contained by it).
   *
   * @return a clone of this instance
   * @deprecated
   */
  public override fun clone(): Any {
    return copy()
  }

  override fun copyInternal(): LineString {
    return LineString(points.copy(), factory)
  }

  /**
   * Normalizes a LineString.  A normalized linestring
   * has the first point which is not equal to it's reflected point
   * less than the reflected point.
   */
  override fun normalize() {
    for (i in 0 until points.size() / 2) {
      val j = points.size() - 1 - i
      // skip equal points on both ends
      if (!points.getCoordinate(i).equals(points.getCoordinate(j))) {
        if (points.getCoordinate(i).compareTo(points.getCoordinate(j)) > 0) {
          val copy = points.copy()
          CoordinateSequences.reverse(copy)
          points = copy
        }
        return
      }
    }
  }

  override fun isEquivalentClass(other: Geometry): Boolean {
    return other is LineString
  }

  override fun compareToSameClass(o: Any?): Int {
    val line = o as LineString
    // MD - optimized implementation
    var i = 0
    var j = 0
    while (i < points.size() && j < line.points.size()) {
      val comparison = points.getCoordinate(i).compareTo(line.points.getCoordinate(j))
      if (comparison != 0) {
        return comparison
      }
      i++
      j++
    }
    if (i < points.size()) {
      return 1
    }
    if (j < line.points.size()) {
      return -1
    }
    return 0
  }

  override fun compareToSameClass(o: Any?, comp: CoordinateSequenceComparator): Int {
    val line = o as LineString
    return comp.compare(this.points, line.points)
  }

  override fun getTypeCode(): Int {
    return Geometry.TYPECODE_LINESTRING
  }

  companion object {

    /**
     * The minimum number of vertices allowed in a valid non-empty linestring.
     * Empty linestrings with 0 vertices are also valid.
     */
    const val MINIMUM_VALID_SIZE = 2
  }
}
