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
import kotlin.math.max

import org.locationtech.jts.util.TreeSet

import org.locationtech.jts.util.Assert

/**
 * Models a collection of [Geometry]s of
 * arbitrary type and dimension.
 *
 *
 */
open class GeometryCollection : Geometry {

  /**
   *  Internal representation of this `GeometryCollection`.
   */
  protected lateinit var geometries: Array<Geometry>

  /** @deprecated Use GeometryFactory instead */
  constructor(geometries: Array<Geometry>?, precisionModel: PrecisionModel, SRID: Int) : this(geometries, GeometryFactory(precisionModel, SRID))

  /**
   * @param geometries
   *            the `Geometry`s for this `GeometryCollection`,
   *            or `null` or an empty array to create the empty
   *            geometry. Elements may be empty `Geometry`s,
   *            but not `null`s.
   */
  constructor(geometries: Array<Geometry>?, factory: GeometryFactory) : super(factory) {
    var geometries = geometries
    if (geometries == null) {
      geometries = arrayOf<Geometry>()
    }
    if (Geometry.hasNullElements(geometries)) {
      throw IllegalArgumentException("geometries must not contain null elements")
    }
    this.geometries = geometries
  }

  override fun getCoordinate(): Coordinate? {
    for (i in 0 until geometries.size) {
      if (!geometries[i].isEmpty()) {
        return geometries[i].getCoordinate()
      }
    }
    return null
  }

  /**
   * Collects all coordinates of all subgeometries into an Array.
   *
   * @return the collected coordinates
   */
  override fun getCoordinates(): Array<Coordinate> {
    val coordinates = arrayOfNulls<Coordinate>(getNumPoints())
    var k = -1
    for (i in 0 until geometries.size) {
      val childCoordinates = geometries[i].getCoordinates()
      for (j in 0 until childCoordinates.size) {
        k++
        coordinates[k] = childCoordinates[j]
      }
    }
    @Suppress("UNCHECKED_CAST")
    return coordinates as Array<Coordinate>
  }

  override fun isEmpty(): Boolean {
    for (i in 0 until geometries.size) {
      if (!geometries[i].isEmpty()) {
        return false
      }
    }
    return true
  }

  override fun getDimension(): Int {
    var dimension = Dimension.FALSE
    for (i in 0 until geometries.size) {
      dimension = max(dimension, geometries[i].getDimension())
    }
    return dimension
  }

  override fun hasDimension(dim: Int): Boolean {
    for (i in 0 until geometries.size) {
      if (geometries[i].hasDimension(dim))
        return true
    }
    return false
  }

  override fun getBoundaryDimension(): Int {
    var dimension = Dimension.FALSE
    for (i in 0 until geometries.size) {
      dimension = max(dimension, geometries[i].getBoundaryDimension())
    }
    return dimension
  }

  override fun getNumGeometries(): Int {
    return geometries.size
  }

  override fun getGeometryN(n: Int): Geometry {
    return geometries[n]
  }

  override fun getNumPoints(): Int {
    var numPoints = 0
    for (i in 0 until geometries.size) {
      numPoints += geometries[i].getNumPoints()
    }
    return numPoints
  }

  override fun getGeometryType(): String {
    return Geometry.TYPENAME_GEOMETRYCOLLECTION
  }

  override fun getBoundary(): Geometry {
    Geometry.checkNotGeometryCollection(this)
    Assert.shouldNeverReachHere()
    throw IllegalStateException()
  }

  /**
   *  Returns the area of this `GeometryCollection`
   *
   * @return the area of the polygon
   */
  override fun getArea(): Double {
    var area = 0.0
    for (i in 0 until geometries.size) {
      area += geometries[i].getArea()
    }
    return area
  }

  override fun getLength(): Double {
    var sum = 0.0
    for (i in 0 until geometries.size) {
      sum += geometries[i].getLength()
    }
    return sum
  }

  override fun equalsExact(other: Geometry, tolerance: Double): Boolean {
    if (!isEquivalentClass(other)) {
      return false
    }
    val otherCollection = other as GeometryCollection
    if (geometries.size != otherCollection.geometries.size) {
      return false
    }
    for (i in 0 until geometries.size) {
      if (!geometries[i].equalsExact(otherCollection.geometries[i], tolerance)) {
        return false
      }
    }
    return true
  }

  override fun apply(filter: CoordinateFilter) {
    for (i in 0 until geometries.size) {
      geometries[i].apply(filter)
    }
  }

  override fun apply(filter: CoordinateSequenceFilter) {
    if (geometries.size == 0)
      return
    for (i in 0 until geometries.size) {
      geometries[i].apply(filter)
      if (filter.isDone()) {
        break
      }
    }
    if (filter.isGeometryChanged())
      geometryChanged()
  }

  override fun apply(filter: GeometryFilter) {
    filter.filter(this)
    for (i in 0 until geometries.size) {
      geometries[i].apply(filter)
    }
  }

  override fun apply(filter: GeometryComponentFilter) {
    filter.filter(this)
    for (i in 0 until geometries.size) {
      geometries[i].apply(filter)
    }
  }

  /**
   * Creates and returns a full copy of this [GeometryCollection] object.
   * (including all coordinates contained by it).
   *
   * @return a clone of this instance
   * @deprecated
   */
  public override fun clone(): Any {
    return copy()
  }

  override fun copyInternal(): GeometryCollection {
    val geometries = arrayOfNulls<Geometry>(this.geometries.size)
    for (i in 0 until geometries.size) {
      geometries[i] = this.geometries[i].copy()
    }
    @Suppress("UNCHECKED_CAST")
    return GeometryCollection(geometries as Array<Geometry>, factory)
  }

  override fun normalize() {
    for (i in 0 until geometries.size) {
      geometries[i].normalize()
    }
    geometries.sort()
  }

  override fun computeEnvelopeInternal(): Envelope {
    val envelope = Envelope()
    for (i in 0 until geometries.size) {
      envelope.expandToInclude(geometries[i].getEnvelopeInternal())
    }
    return envelope
  }

  override fun compareToSameClass(o: Any?): Int {
    val theseElements = TreeSet<Any?>(geometries.asList())
    val otherElements = TreeSet<Any?>((o as GeometryCollection).geometries.asList())
    return compare(theseElements, otherElements)
  }

  override fun compareToSameClass(o: Any?, comp: CoordinateSequenceComparator): Int {
    val gc = o as GeometryCollection

    val n1 = getNumGeometries()
    val n2 = gc.getNumGeometries()
    var i = 0
    while (i < n1 && i < n2) {
      val thisGeom = getGeometryN(i)
      val otherGeom = gc.getGeometryN(i)
      val holeComp = thisGeom.compareToSameClassWith(otherGeom, comp)
      if (holeComp != 0) return holeComp
      i++
    }
    if (i < n1) return 1
    if (i < n2) return -1
    return 0
  }

  override fun getTypeCode(): Int {
    return Geometry.TYPECODE_GEOMETRYCOLLECTION
  }

  /**
   * Creates a [GeometryCollection] with
   * every component reversed.
   * The order of the components in the collection are not reversed.
   *
   * @return a [GeometryCollection] in the reverse order
   */
  override fun reverse(): GeometryCollection {
    return super.reverse() as GeometryCollection
  }

  override fun reverseInternal(): GeometryCollection {
    val geometries = arrayOfNulls<Geometry>(this.geometries.size)
    for (i in 0 until geometries.size) {
      geometries[i] = this.geometries[i].reverse()
    }
    @Suppress("UNCHECKED_CAST")
    return GeometryCollection(geometries as Array<Geometry>, factory)
  }

  companion object {
  }
}
