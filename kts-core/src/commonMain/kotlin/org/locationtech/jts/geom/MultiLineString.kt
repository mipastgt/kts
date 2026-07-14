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

import org.locationtech.jts.operation.BoundaryOp

/**
 * Models a collection of [LineString]s.
 * 
 * Any collection of LineStrings is a valid MultiLineString.
 *
 */
open class MultiLineString : GeometryCollection, Lineal {

  /**
   *  Constructs a `MultiLineString`.
   *
   * @deprecated Use GeometryFactory instead
   */
  @Suppress("UNCHECKED_CAST")
  constructor(lineStrings: Array<LineString>?, precisionModel: PrecisionModel, SRID: Int) : super(lineStrings as Array<Geometry>?, GeometryFactory(precisionModel, SRID))

  /**
   * @param lineStrings
   *            the `LineString`s for this `MultiLineString`,
   *            or `null` or an empty array to create the empty
   *            geometry.
   */
  @Suppress("UNCHECKED_CAST")
  constructor(lineStrings: Array<LineString>?, factory: GeometryFactory) : super(lineStrings as Array<Geometry>?, factory)

  override fun getDimension(): Int {
    return 1
  }

  override fun hasDimension(dim: Int): Boolean {
    return dim == 1
  }

  override fun getBoundaryDimension(): Int {
    if (isClosed()) {
      return Dimension.FALSE
    }
    return 0
  }

  override fun getGeometryType(): String {
    return Geometry.TYPENAME_MULTILINESTRING
  }

  fun isClosed(): Boolean {
    if (isEmpty()) {
      return false
    }
    for (i in 0 until geometries.size) {
      if (!(geometries[i] as LineString).isClosed()) {
        return false
      }
    }
    return true
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
   * Creates a [MultiLineString] in the reverse
   * order to this object.
   *
   * @return a [MultiLineString] in the reverse order
   */
  override fun reverse(): MultiLineString {
    return super.reverse() as MultiLineString
  }

  override fun reverseInternal(): MultiLineString {
    val lineStrings = Array(this.geometries.size) { i -> this.geometries[i].reverse() as LineString }
    return MultiLineString(lineStrings, factory)
  }

  override fun copyInternal(): MultiLineString {
    val lineStrings = Array(this.geometries.size) { i -> this.geometries[i].copy() as LineString }
    return MultiLineString(lineStrings, factory)
  }

  override fun equalsExact(other: Geometry, tolerance: Double): Boolean {
    if (!isEquivalentClass(other)) {
      return false
    }
    return super.equalsExact(other, tolerance)
  }

  override fun getTypeCode(): Int {
    return Geometry.TYPECODE_MULTILINESTRING
  }

  companion object {
  }
}
