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

import org.locationtech.jts.util.Assert

/**
 * Represents a single point.
 *
 * A <code>Point</code> is topologically valid if and only if:
 * <ul>
 * <li>the coordinate which defines it (if any) is a valid coordinate
 * (i.e. does not have an <code>NaN</code> X or Y ordinate)
 * </ul>
 *
 * @version 1.7
 */
open class Point : Geometry, Puntal {
  /**
   *  The <code>Coordinate</code> wrapped by this <code>Point</code>.
   */
  private lateinit var coordinates: CoordinateSequence

  /**
   *  Constructs a <code>Point</code> with the given coordinate.
   *
   * @param  coordinate      the coordinate on which to base this <code>Point</code>
   *      , or <code>null</code> to create the empty geometry.
   * @param  precisionModel  the specification of the grid of allowable points
   *      for this <code>Point</code>
   * @param  SRID            the ID of the Spatial Reference System used by this
   *      <code>Point</code>
   * @deprecated Use GeometryFactory instead
   */
  constructor(coordinate: Coordinate?, precisionModel: PrecisionModel, SRID: Int) : super(GeometryFactory(precisionModel, SRID)) {
    init(
      getFactory().getCoordinateSequenceFactory().create(
        if (coordinate != null) arrayOf(coordinate) else arrayOf<Coordinate>()
      )
    )
  }

  /**
   * @param  coordinates      contains the single coordinate on which to base this <code>Point</code>
   *      , or <code>null</code> to create the empty geometry.
   */
  constructor(coordinates: CoordinateSequence?, factory: GeometryFactory) : super(factory) {
    init(coordinates)
  }

  private fun init(coordinates: CoordinateSequence?) {
    var coordinates = coordinates
    if (coordinates == null) {
      coordinates = getFactory().getCoordinateSequenceFactory().create(arrayOf<Coordinate>())
    }
    Assert.isTrue(coordinates.size() <= 1)
    this.coordinates = coordinates
  }

  override fun getCoordinates(): Array<Coordinate> {
    return if (isEmpty()) arrayOf<Coordinate>() else arrayOf<Coordinate>(getCoordinate()!!)
  }

  override fun getNumPoints(): Int {
    return if (isEmpty()) 0 else 1
  }

  override fun isEmpty(): Boolean {
    return coordinates.size() == 0
  }

  override fun isSimple(): Boolean {
    return true
  }

  override fun getDimension(): Int {
    return 0
  }

  override fun getBoundaryDimension(): Int {
    return Dimension.FALSE
  }

  fun getX(): Double {
    if (getCoordinate() == null) {
      throw IllegalStateException("getX called on empty Point")
    }
    return getCoordinate()!!.x
  }

  fun getY(): Double {
    if (getCoordinate() == null) {
      throw IllegalStateException("getY called on empty Point")
    }
    return getCoordinate()!!.y
  }

  override fun getCoordinate(): Coordinate? {
    return if (coordinates.size() != 0) coordinates.getCoordinate(0) else null
  }

  override fun getGeometryType(): String {
    return Geometry.TYPENAME_POINT
  }

  /**
   * Gets the boundary of this geometry.
   * Zero-dimensional geometries have no boundary by definition,
   * so an empty GeometryCollection is returned.
   *
   * @return an empty GeometryCollection
   * @see Geometry#getBoundary
   */
  override fun getBoundary(): Geometry {
    return getFactory().createGeometryCollection()
  }

  override fun computeEnvelopeInternal(): Envelope {
    if (isEmpty()) {
      return Envelope()
    }
    val env = Envelope()
    env.expandToInclude(coordinates.getX(0), coordinates.getY(0))
    return env
  }

  override fun equalsExact(other: Geometry, tolerance: Double): Boolean {
    if (!isEquivalentClass(other)) {
      return false
    }
    if (isEmpty() && other.isEmpty()) {
      return true
    }
    if (isEmpty() != other.isEmpty()) {
      return false
    }
    return equal((other as Point).getCoordinate()!!, this.getCoordinate()!!, tolerance)
  }

  override fun apply(filter: CoordinateFilter) {
    if (isEmpty()) {
      return
    }
    filter.filter(getCoordinate()!!)
  }

  override fun apply(filter: CoordinateSequenceFilter) {
    if (isEmpty())
      return
    filter.filter(coordinates, 0)
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
   * Creates and returns a full copy of this {@link Point} object.
   * (including all coordinates contained by it).
   *
   * @return a clone of this instance
   * @deprecated
   */
  public override fun clone(): Any {
    return copy()
  }

  override fun copyInternal(): Point {
    return Point(coordinates.copy(), factory)
  }

  override fun reverse(): Point {
    return super.reverse() as Point
  }

  override fun reverseInternal(): Point {
    return getFactory().createPoint(coordinates.copy())
  }

  override fun normalize() {
    // a Point is always in normalized form
  }

  override fun compareToSameClass(other: Any?): Int {
    val point = other as Point
    return getCoordinate()!!.compareTo(point.getCoordinate()!!)
  }

  override fun compareToSameClass(other: Any?, comp: CoordinateSequenceComparator): Int {
    val point = other as Point
    return comp.compare(this.coordinates, point.coordinates)
  }

  override fun getTypeCode(): Int {
    return Geometry.TYPECODE_POINT
  }

  fun getCoordinateSequence(): CoordinateSequence {
    return coordinates
  }

  companion object {
  }
}
