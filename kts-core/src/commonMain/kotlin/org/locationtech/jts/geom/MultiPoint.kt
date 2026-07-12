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

/**
 * Models a collection of {@link Point}s.
 * <p>
 * Any collection of Points is a valid MultiPoint.
 *
 * @version 1.7
 */
open class MultiPoint : GeometryCollection, Puntal {

  /**
   *  Constructs a <code>MultiPoint</code>.
   *
   * @deprecated Use GeometryFactory instead
   */
  @Suppress("UNCHECKED_CAST")
  constructor(points: Array<Point>?, precisionModel: PrecisionModel, SRID: Int) : super(points as Array<Geometry>?, GeometryFactory(precisionModel, SRID))

  /**
   * @param  points          the <code>Point</code>s for this <code>MultiPoint</code>
   *      , or <code>null</code> or an empty array to create the empty geometry.
   */
  @Suppress("UNCHECKED_CAST")
  constructor(points: Array<Point>?, factory: GeometryFactory) : super(points as Array<Geometry>?, factory)

  override fun getDimension(): Int {
    return 0
  }

  override fun hasDimension(dim: Int): Boolean {
    return dim == 0
  }

  override fun getBoundaryDimension(): Int {
    return Dimension.FALSE
  }

  override fun getGeometryType(): String {
    return Geometry.TYPENAME_MULTIPOINT
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

  override fun reverse(): MultiPoint {
    return super.reverse() as MultiPoint
  }

  override fun reverseInternal(): MultiPoint {
    val points = Array(this.geometries.size) { i -> this.geometries[i].copy() as Point }
    return MultiPoint(points, factory)
  }

  override fun equalsExact(other: Geometry, tolerance: Double): Boolean {
    if (!isEquivalentClass(other)) {
      return false
    }
    return super.equalsExact(other, tolerance)
  }

  /**
   *  Returns the <code>Coordinate</code> at the given position.
   *
   * @param  n  the index of the <code>Coordinate</code> to retrieve, beginning
   *      at 0
   * @return    the <code>n</code>th <code>Coordinate</code>
   */
  protected fun getCoordinate(n: Int): Coordinate? {
    return (geometries[n] as Point).getCoordinate()
  }

  override fun copyInternal(): MultiPoint {
    val points = Array(this.geometries.size) { i -> this.geometries[i].copy() as Point }
    return MultiPoint(points, factory)
  }

  override fun getTypeCode(): Int {
    return Geometry.TYPECODE_MULTIPOINT
  }

  companion object {
  }
}
