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

import org.locationtech.jts.algorithm.Area
import org.locationtech.jts.algorithm.Orientation

/**
 * Represents a polygon with linear edges, which may include holes.
 * The outer boundary (shell)
 * and inner boundaries (holes) of the polygon are represented by {@link LinearRing}s.
 *
 * @version 1.7
 */
open class Polygon : Geometry, Polygonal {

  /**
   *  The exterior boundary,
   * or <code>null</code> if this <code>Polygon</code>
   *  is empty.
   */
  protected lateinit var shell: LinearRing

  /**
   * The interior boundaries, if any.
   * This instance var is never null.
   * If there are no holes, the array is of zero length.
   */
  protected lateinit var holes: Array<LinearRing>

  /**
   *  Constructs a <code>Polygon</code> with the given exterior boundary.
   *
   * @deprecated Use GeometryFactory instead
   */
  constructor(shell: LinearRing?, precisionModel: PrecisionModel, SRID: Int) : this(shell, arrayOf<LinearRing>(), GeometryFactory(precisionModel, SRID))

  /**
   *  Constructs a <code>Polygon</code> with the given exterior boundary and
   *  interior boundaries.
   *
   * @deprecated Use GeometryFactory instead
   */
  constructor(shell: LinearRing?, holes: Array<LinearRing>?, precisionModel: PrecisionModel, SRID: Int) : this(shell, holes, GeometryFactory(precisionModel, SRID))

  /**
   *  Constructs a <code>Polygon</code> with the given exterior boundary and
   *  interior boundaries.
   */
  constructor(shell: LinearRing?, holes: Array<LinearRing>?, factory: GeometryFactory) : super(factory) {
    var shell = shell
    var holes = holes
    if (shell == null) {
      shell = getFactory().createLinearRing()
    }
    if (holes == null) {
      holes = arrayOf<LinearRing>()
    }
    if (Geometry.hasNullElements(holes)) {
      throw IllegalArgumentException("holes must not contain null elements")
    }
    if (shell.isEmpty() && Geometry.hasNonEmptyElements(holes)) {
      throw IllegalArgumentException("shell is empty but holes are not")
    }
    this.shell = shell
    this.holes = holes
  }

  override fun getCoordinate(): Coordinate? {
    return shell.getCoordinate()
  }

  override fun getCoordinates(): Array<Coordinate> {
    if (isEmpty()) {
      return arrayOf<Coordinate>()
    }
    val coordinates = arrayOfNulls<Coordinate>(getNumPoints())
    var k = -1
    val shellCoordinates = shell.getCoordinates()
    for (x in 0 until shellCoordinates.size) {
      k++
      coordinates[k] = shellCoordinates[x]
    }
    for (i in 0 until holes.size) {
      val childCoordinates = holes[i].getCoordinates()
      for (j in 0 until childCoordinates.size) {
        k++
        coordinates[k] = childCoordinates[j]
      }
    }
    @Suppress("UNCHECKED_CAST")
    return coordinates as Array<Coordinate>
  }

  override fun getNumPoints(): Int {
    var numPoints = shell.getNumPoints()
    for (i in 0 until holes.size) {
      numPoints += holes[i].getNumPoints()
    }
    return numPoints
  }

  override fun getDimension(): Int {
    return 2
  }

  override fun getBoundaryDimension(): Int {
    return 1
  }

  override fun isEmpty(): Boolean {
    return shell.isEmpty()
  }

  override fun isRectangle(): Boolean {
    if (getNumInteriorRing() != 0) return false
    if (shell == null) return false
    if (shell.getNumPoints() != 5) return false

    val seq = shell.getCoordinateSequence()

    // check vertices have correct values
    val env = getEnvelopeInternal()
    for (i in 0 until 5) {
      val x = seq.getX(i)
      if (!(x == env.getMinX() || x == env.getMaxX())) return false
      val y = seq.getY(i)
      if (!(y == env.getMinY() || y == env.getMaxY())) return false
    }

    // check vertices are in right order
    var prevX = seq.getX(0)
    var prevY = seq.getY(0)
    for (i in 1..4) {
      val x = seq.getX(i)
      val y = seq.getY(i)
      val xChanged = x != prevX
      val yChanged = y != prevY
      if (xChanged == yChanged)
        return false
      prevX = x
      prevY = y
    }
    return true
  }

  open fun getExteriorRing(): LinearRing {
    return shell
  }

  open fun getNumInteriorRing(): Int {
    return holes.size
  }

  open fun getInteriorRingN(n: Int): LinearRing {
    return holes[n]
  }

  override fun getGeometryType(): String {
    return Geometry.TYPENAME_POLYGON
  }

  /**
   *  Returns the area of this <code>Polygon</code>
   *
   * @return the area of the polygon
   */
  override fun getArea(): Double {
    var area = 0.0
    area += Area.ofRing(shell.getCoordinateSequence())
    for (i in 0 until holes.size) {
      area -= Area.ofRing(holes[i].getCoordinateSequence())
    }
    return area
  }

  /**
   *  Returns the perimeter of this <code>Polygon</code>
   *
   * @return the perimeter of the polygon
   */
  override fun getLength(): Double {
    var len = 0.0
    len += shell.getLength()
    for (i in 0 until holes.size) {
      len += holes[i].getLength()
    }
    return len
  }

  /**
   * Computes the boundary of this geometry
   *
   * @return a lineal geometry (which may be empty)
   * @see Geometry#getBoundary
   */
  override fun getBoundary(): Geometry {
    if (isEmpty()) {
      return getFactory().createMultiLineString()
    }
    val rings = Array(holes.size + 1) { i -> if (i == 0) shell else holes[i - 1] }
    // create LineString or MultiLineString as appropriate
    if (rings.size <= 1)
      return getFactory().createLinearRing(rings[0].getCoordinateSequence())
    @Suppress("UNCHECKED_CAST")
    return getFactory().createMultiLineString(rings as Array<LineString>)
  }

  override fun computeEnvelopeInternal(): Envelope {
    return shell.getEnvelopeInternal()
  }

  override fun equalsExact(other: Geometry, tolerance: Double): Boolean {
    if (!isEquivalentClass(other)) {
      return false
    }
    val otherPolygon = other as Polygon
    val thisShell: Geometry = shell
    val otherPolygonShell: Geometry = otherPolygon.shell
    if (!thisShell.equalsExact(otherPolygonShell, tolerance)) {
      return false
    }
    if (holes.size != otherPolygon.holes.size) {
      return false
    }
    for (i in 0 until holes.size) {
      if (!holes[i].equalsExact(otherPolygon.holes[i], tolerance)) {
        return false
      }
    }
    return true
  }

  override fun apply(filter: CoordinateFilter) {
    shell.apply(filter)
    for (i in 0 until holes.size) {
      holes[i].apply(filter)
    }
  }

  override fun apply(filter: CoordinateSequenceFilter) {
    shell.apply(filter)
    if (!filter.isDone()) {
      for (i in 0 until holes.size) {
        holes[i].apply(filter)
        if (filter.isDone())
          break
      }
    }
    if (filter.isGeometryChanged())
      geometryChanged()
  }

  override fun apply(filter: GeometryFilter) {
    filter.filter(this)
  }

  override fun apply(filter: GeometryComponentFilter) {
    filter.filter(this)
    shell.apply(filter)
    for (i in 0 until holes.size) {
      holes[i].apply(filter)
    }
  }

  /**
   * Creates and returns a full copy of this {@link Polygon} object.
   * (including all coordinates contained by it).
   *
   * @return a clone of this instance
   * @deprecated
   */
  public override fun clone(): Any {
    return copy()
  }

  override fun copyInternal(): Polygon {
    val shellCopy = shell.copy() as LinearRing
    val holeCopies = Array(this.holes.size) { i -> holes[i].copy() as LinearRing }
    return Polygon(shellCopy, holeCopies, factory)
  }

  override fun convexHull(): Geometry {
    return getExteriorRing().convexHull()
  }

  override fun normalize() {
    shell = normalized(shell, true)
    for (i in 0 until holes.size) {
      holes[i] = normalized(holes[i], false)
    }
    holes.sort()
  }

  override fun compareToSameClass(o: Any?): Int {
    val poly = o as Polygon

    val thisShell = shell
    val otherShell = poly.shell
    val shellComp = thisShell.compareToSameClassWith(otherShell)
    if (shellComp != 0) return shellComp

    val nHole1 = getNumInteriorRing()
    val nHole2 = poly.getNumInteriorRing()
    var i = 0
    while (i < nHole1 && i < nHole2) {
      val thisHole = getInteriorRingN(i)
      val otherHole = poly.getInteriorRingN(i)
      val holeComp = thisHole.compareToSameClassWith(otherHole)
      if (holeComp != 0) return holeComp
      i++
    }
    if (i < nHole1) return 1
    if (i < nHole2) return -1
    return 0
  }

  override fun compareToSameClass(o: Any?, comp: CoordinateSequenceComparator): Int {
    val poly = o as Polygon

    val thisShell = shell
    val otherShell = poly.shell
    val shellComp = thisShell.compareToSameClassWith(otherShell, comp)
    if (shellComp != 0) return shellComp

    val nHole1 = getNumInteriorRing()
    val nHole2 = poly.getNumInteriorRing()
    var i = 0
    while (i < nHole1 && i < nHole2) {
      val thisHole = getInteriorRingN(i)
      val otherHole = poly.getInteriorRingN(i)
      val holeComp = thisHole.compareToSameClassWith(otherHole, comp)
      if (holeComp != 0) return holeComp
      i++
    }
    if (i < nHole1) return 1
    if (i < nHole2) return -1
    return 0
  }

  override fun getTypeCode(): Int {
    return Geometry.TYPECODE_POLYGON
  }

  private fun normalized(ring: LinearRing, clockwise: Boolean): LinearRing {
    val res = ring.copy() as LinearRing
    normalize(res, clockwise)
    return res
  }

  private fun normalize(ring: LinearRing, clockwise: Boolean) {
    if (ring.isEmpty()) {
      return
    }

    val seq = ring.getCoordinateSequence()
    val minCoordinateIndex = CoordinateSequences.minCoordinateIndex(seq, 0, seq.size() - 2)
    CoordinateSequences.scroll(seq, minCoordinateIndex, true)
    if (Orientation.isCCW(seq) == clockwise)
      CoordinateSequences.reverse(seq)
  }

  override fun reverse(): Polygon {
    return super.reverse() as Polygon
  }

  override fun reverseInternal(): Polygon {
    val shell = getExteriorRing().reverse() as LinearRing
    val holes = Array(getNumInteriorRing()) { i -> getInteriorRingN(i).reverse() as LinearRing }

    return getFactory().createPolygon(shell, holes)
  }

  companion object {
  }
}
