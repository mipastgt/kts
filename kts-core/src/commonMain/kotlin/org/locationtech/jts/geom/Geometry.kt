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


import org.locationtech.jts.algorithm.Centroid
import org.locationtech.jts.algorithm.ConvexHull
import org.locationtech.jts.algorithm.InteriorPoint
import org.locationtech.jts.io.WKTWriter
import org.locationtech.jts.operation.buffer.BufferOp
import org.locationtech.jts.operation.distance.DistanceOp
import org.locationtech.jts.operation.predicate.RectangleContains
import org.locationtech.jts.operation.predicate.RectangleIntersects
import org.locationtech.jts.operation.valid.IsSimpleOp
import org.locationtech.jts.operation.valid.IsValidOp
import org.locationtech.jts.util.Assert

/**
 * A representation of a planar, linear vector geometry.
 * (See the original Javadoc for the full contract description.)
 *
 */
abstract class Geometry(
  /**
   * The [GeometryFactory] used to create this Geometry
   */
  @JvmField protected val factory: GeometryFactory
) : Comparable<Any?> {

  /**
   *  The bounding box of this `Geometry`.
   */
  @JvmField
  protected var envelope: Envelope? = null

  /**
   *  The ID of the Spatial Reference System used by this `Geometry`
   */
  @JvmField
  protected var SRID: Int = factory.getSRID()

  /**
   * An object reference which can be used to carry ancillary data defined
   * by the client.
   */
  private var userData: Any? = null

  /**
   * Returns the name of this Geometry's actual class.
   *
   * @return the name of this `Geometry`s actual class
   */
  abstract fun getGeometryType(): String

  /**
   *  Returns the ID of the Spatial Reference System used by the `Geometry`.
   *
   * @return    the ID of the coordinate space in which the `Geometry`
   *      is defined.
   */
  open fun getSRID(): Int {
    return SRID
  }

  /**
   *  Sets the ID of the Spatial Reference System used by the `Geometry`.
   */
  open fun setSRID(SRID: Int) {
    this.SRID = SRID
  }

  /**
   * Gets the factory which contains the context in which this geometry was created.
   *
   * @return the factory for this geometry
   */
  open fun getFactory(): GeometryFactory {
    return factory
  }

  /**
   * Gets the user data object for this geometry, if any.
   *
   * @return the user data object, or `null` if none set
   */
  open fun getUserData(): Any? {
    return userData
  }

  /**
   * Returns the number of [Geometry]s in a [GeometryCollection]
   * (or 1, if the geometry is not a collection).
   *
   * @return the number of geometries contained in this geometry
   */
  open fun getNumGeometries(): Int {
    return 1
  }

  /**
   * Returns an element [Geometry] from a [GeometryCollection]
   * (or `this`, if the geometry is not a collection).
   *
   * @param n the index of the geometry element
   * @return the n'th geometry contained in this geometry
   */
  open fun getGeometryN(n: Int): Geometry {
    return this
  }

  /**
   * A simple scheme for applications to add their own custom data to a Geometry.
   *
   * @param userData an object, the semantics for which are defined by the
   * application using this Geometry
   */
  open fun setUserData(userData: Any?) {
    this.userData = userData
  }

  /**
   *  Returns the `PrecisionModel` used by the `Geometry`.
   *
   * @return    the specification of the grid of allowable points, for this
   *      `Geometry` and all other `Geometry`s
   */
  open fun getPrecisionModel(): PrecisionModel {
    return factory.getPrecisionModel()
  }

  /**
   *  Returns a vertex of this geometry
   *  (usually, but not necessarily, the first one),
   *  or `null` if the geometry is empty.
   *
   * @return a coordinate which is a vertex of this `Geometry`.
   * @return null if this Geometry is empty
   */
  abstract fun getCoordinate(): Coordinate?

  /**
   *  Returns an array containing the values of all the vertices for
   *  this geometry.
   *
   * @return    the vertices of this `Geometry`
   */
  abstract fun getCoordinates(): Array<Coordinate>

  /**
   *  Returns the count of this `Geometry`s vertices.
   *
   * @return    the number of vertices in this `Geometry`
   */
  abstract fun getNumPoints(): Int

  /**
   * Tests whether this [Geometry] is simple.
   *
   * @return `true` if this `Geometry` is simple
   * @see #isValid
   */
  open fun isSimple(): Boolean {
    val op = IsSimpleOp(this)
    return op.isSimple()
  }

  /**
   * Tests whether this `Geometry`
   * is topologically valid, according to the OGC SFS specification.
   *
   * @return `true` if this `Geometry` is valid
   *
   * @see IsValidOp
   */
  open fun isValid(): Boolean {
    return IsValidOp.isValid(this)
  }

  /**
   * Tests whether the set of points covered by this `Geometry` is
   * empty.
   *
   * @return `true` if this `Geometry` does not cover any points
   */
  abstract fun isEmpty(): Boolean

  /**
   * Returns the minimum distance between this `Geometry`
   * and another `Geometry`.
   *
   * @param  g the `Geometry` from which to compute the distance
   * @return the distance between the geometries
   * @return 0 if either input geometry is empty
   * @throws IllegalArgumentException if g is null
   */
  open fun distance(g: Geometry): Double {
    return DistanceOp.distance(this, g)
  }

  /**
   * Tests whether the distance from this `Geometry`
   * to another is less than or equal to a specified value.
   *
   * @param geom the Geometry to check the distance to
   * @param distance the distance value to compare
   * @return `true` if the geometries are less than `distance` apart.
   */
  open fun isWithinDistance(geom: Geometry, distance: Double): Boolean {
    return DistanceOp.isWithinDistance(this, geom, distance)
  }

  /**
   * Tests whether this is a rectangular [Polygon].
   *
   * @return true if the geometry is a rectangle.
   */
  open fun isRectangle(): Boolean {
    // Polygon overrides to check for actual rectangle
    return false
  }

  /**
   *  Returns the area of this `Geometry`.
   *
   * @return the area of the Geometry
   */
  open fun getArea(): Double {
    return 0.0
  }

  /**
   *  Returns the length of this `Geometry`.
   *
   * @return the length of the Geometry
   */
  open fun getLength(): Double {
    return 0.0
  }

  /**
   * Computes the centroid of this `Geometry`.
   *
   * @return a [Point] which is the centroid of this Geometry
   */
  open fun getCentroid(): Point {
    if (isEmpty())
      return factory.createPoint()
    val centPt = Centroid.getCentroid(this)
    return createPointFromInternalCoord(centPt, this)
  }

  /**
   * Computes an interior point of this `Geometry`.
   *
   * @return a [Point] which is in the interior of this Geometry
   */
  open fun getInteriorPoint(): Point {
    if (isEmpty()) return factory.createPoint()
    val pt = InteriorPoint.getInteriorPoint(this)
    return createPointFromInternalCoord(pt, this)
  }

  /**
   * Returns the dimension of this geometry.
   *
   * @return the topological dimension of this geometry.
   *
   * @see #hasDimension(int)
   */
  abstract fun getDimension(): Int

  /**
   * Tests whether an atomic geometry or any element of a collection
   * has the specified dimension.
   *
   * @param dim the dimension to test
   * @return true if the geometry has or contains an element with the dimension
   *
   * @see #getDimension()
   */
  open fun hasDimension(dim: Int): Boolean {
    return dim == getDimension()
  }

  /**
   * Returns the boundary, or an empty geometry of appropriate dimension
   * if this `Geometry`  is empty.
   *
   * @return    the closure of the combinatorial boundary of this `Geometry`
   */
  abstract fun getBoundary(): Geometry

  /**
   *  Returns the dimension of this `Geometry`s inherent boundary.
   *
   * @return    the dimension of the boundary of the class implementing this
   *      interface.
   */
  abstract fun getBoundaryDimension(): Int

  /**
   *  Gets a Geometry representing the envelope (bounding box) of
   *  this `Geometry`.
   *
   * @return a Geometry representing the envelope of this Geometry
   *
   * @see GeometryFactory#toGeometry(Envelope)
   */
  open fun getEnvelope(): Geometry {
    return getFactory().toGeometry(getEnvelopeInternal())
  }

  /**
   * Gets an [Envelope] containing
   * the minimum and maximum x and y values in this `Geometry`.
   *
   * @return the envelope of this `Geometry`.
   * @return an empty Envelope if this Geometry is empty
   */
  open fun getEnvelopeInternal(): Envelope {
    if (envelope == null) {
      envelope = computeEnvelopeInternal()
    }
    return Envelope(envelope!!)
  }

  /**
   * Notifies this geometry that its coordinates have been changed by an external
   * party.
   */
  open fun geometryChanged() {
    apply(geometryChangedFilter)
  }

  /**
   * Notifies this Geometry that its Coordinates have been changed by an external
   * party.
   *
   * @see #apply(GeometryComponentFilter)
   */
  protected open fun geometryChangedAction() {
    envelope = null
  }

  /**
   * Tests whether this geometry is disjoint from the argument geometry.
   *
   * @param  g  the `Geometry` with which to compare this `Geometry`
   * @return        `true` if the two `Geometry`s are
   *      disjoint
   *
   * @see Geometry#intersects
   */
  open fun disjoint(g: Geometry): Boolean {
    return !intersects(g)
  }

  /**
   * Tests whether this geometry touches the
   * argument geometry.
   *
   * @param  g  the `Geometry` with which to compare this `Geometry`
   * @return        `true` if the two `Geometry`s touch;
   *      Returns `false` if both `Geometry`s are points
   */
  open fun touches(g: Geometry): Boolean {
    return GeometryRelate.touches(this, g)
  }

  /**
   * Tests whether this geometry intersects the argument geometry.
   *
   * @param  g  the `Geometry` with which to compare this `Geometry`
   * @return        `true` if the two `Geometry`s intersect
   *
   * @see Geometry#disjoint
   */
  open fun intersects(g: Geometry): Boolean {

    // short-circuit envelope test
    if (!getEnvelopeInternal().intersects(g.getEnvelopeInternal()))
      return false

    // optimization for rectangle arguments
    if (isRectangle()) {
      return RectangleIntersects.intersects(this as Polygon, g)
    }
    if (g.isRectangle()) {
      return RectangleIntersects.intersects(g as Polygon, this)
    }

    return GeometryRelate.intersects(this, g)
  }

  /**
   * Tests whether this geometry crosses the
   * argument geometry.
   *
   * @param  g  the `Geometry` with which to compare this `Geometry`
   * @return        `true` if the two `Geometry`s cross.
   */
  open fun crosses(g: Geometry): Boolean {
    // short-circuit test
    if (!getEnvelopeInternal().intersects(g.getEnvelopeInternal()))
      return false
    return relate(g).isCrosses(getDimension(), g.getDimension())
  }

  /**
   * Tests whether this geometry is within the
   * specified geometry.
   *
   * @param  g  the `Geometry` with which to compare this `Geometry`
   * @return        `true` if this `Geometry` is within
   *      `g`
   *
   * @see Geometry#contains
   * @see Geometry#coveredBy
   */
  open fun within(g: Geometry): Boolean {
    return GeometryRelate.within(this, g)
  }

  /**
   * Tests whether this geometry contains the
   * argument geometry.
   *
   * @param  g  the `Geometry` with which to compare this `Geometry`
   * @return        `true` if this `Geometry` contains `g`
   *
   * @see Geometry#within
   * @see Geometry#covers
   */
  open fun contains(g: Geometry): Boolean {

    // optimization for rectangle arguments
    if (isRectangle()) {
      return RectangleContains.contains(this as Polygon, g)
    }
    // general case
    return GeometryRelate.contains(this, g)
  }

  /**
   * Tests whether this geometry overlaps the
   * specified geometry.
   *
   * @param  g  the `Geometry` with which to compare this `Geometry`
   * @return        `true` if the two `Geometry`s overlap.
   */
  open fun overlaps(g: Geometry): Boolean {
    return GeometryRelate.overlaps(this, g)
  }

  /**
   * Tests whether this geometry covers the
   * argument geometry.
   *
   * @param  g  the `Geometry` with which to compare this `Geometry`
   * @return        `true` if this `Geometry` covers `g`
   *
   * @see Geometry#contains
   * @see Geometry#coveredBy
   */
  open fun covers(g: Geometry): Boolean {
    return GeometryRelate.covers(this, g)
  }

  /**
   * Tests whether this geometry is covered by the
   * argument geometry.
   *
   * @param  g  the `Geometry` with which to compare this `Geometry`
   * @return        `true` if this `Geometry` is covered by `g`
   *
   * @see Geometry#within
   * @see Geometry#covers
   */
  open fun coveredBy(g: Geometry): Boolean {
    return GeometryRelate.coveredBy(this, g)
  }

  /**
   * Tests whether the elements in the DE-9IM
   * [IntersectionMatrix] for the two `Geometry`s match the elements in `intersectionPattern`.
   *
   * @param  g                the `Geometry` with which to compare
   *      this `Geometry`
   * @param  intersectionPattern  the pattern against which to check the
   *      intersection matrix for the two `Geometry`s
   * @return                      `true` if the DE-9IM intersection
   *      matrix for the two `Geometry`s match `intersectionPattern`
   * @see IntersectionMatrix
   */
  open fun relate(g: Geometry, intersectionPattern: String): Boolean {
    return GeometryRelate.relate(this, g, intersectionPattern)
  }

  /**
   *  Returns the DE-9IM [IntersectionMatrix] for the two `Geometry`s.
   *
   * @param  g  the `Geometry` with which to compare this `Geometry`
   * @return        an [IntersectionMatrix] describing the intersections of the interiors,
   *      boundaries and exteriors of the two `Geometry`s
   */
  open fun relate(g: Geometry): IntersectionMatrix {
    return GeometryRelate.relate(this, g)
  }

  /**
   * Tests whether this geometry is
   * topologically equal to the argument geometry.
   *
   * @param  g  the `Geometry` with which to compare this `Geometry`
   * @return true if the two `Geometry`s are topologically equal
   *
   * @see #equalsTopo(Geometry)
   */
  open fun equals(g: Geometry?): Boolean {
    if (g == null) return false
    return equalsTopo(g)
  }

  /**
   * Tests whether this geometry is topologically equal to the argument geometry
   * as defined by the SFS `equals` predicate.
   *
   * @param g the `Geometry` with which to compare this `Geometry`
   * @return `true` if the two `Geometry`s are topologically equal
   *
   * @see #equalsExact(Geometry)
   */
  open fun equalsTopo(g: Geometry): Boolean {
    return GeometryRelate.equalsTopo(this, g)
  }

  /**
   * Tests whether this geometry is structurally and numerically equal
   * to a given `Object`.
   *
   * @param o the Object to compare
   * @return true if this geometry is exactly equal to the argument
   *
   * @see #equalsExact(Geometry)
   * @see #hashCode()
   */
  override fun equals(o: Any?): Boolean {
    if (o !is Geometry) return false
    val g = o
    return equalsExact(g)
  }

  /**
   * Gets a hash code for the Geometry.
   *
   * @return an integer value suitable for use as a hashcode
   */
  override fun hashCode(): Int {
    return getEnvelopeInternal().hashCode()
  }

  override fun toString(): String {
    return toText()
  }

  /**
   *  Returns the Well-known Text representation of this `Geometry`.
   *
   * @return    the Well-known Text representation of this `Geometry`
   */
  open fun toText(): String {
    val writer = WKTWriter()
    return writer.write(this)
  }

  /**
   * Computes a buffer area around this geometry having the given width.
   *
   * @param distance
   *          the width of the buffer (may be positive, negative or 0)
   * @return a polygonal geometry representing the buffer region (which may be
   *         empty)
   *
   * @throws TopologyException
   *           if a robustness error occurs
   */
  open fun buffer(distance: Double): Geometry {
    return BufferOp.bufferOp(this, distance)
  }

  /**
   * Computes a buffer area around this geometry having the given width and with
   * a specified accuracy of approximation for circular arcs.
   *
   * @param distance
   *          the width of the buffer (may be positive, negative or 0)
   * @param quadrantSegments
   *          the number of line segments used to represent a quadrant of a
   *          circle
   * @return a polygonal geometry representing the buffer region (which may be
   *         empty)
   *
   * @throws TopologyException
   *           if a robustness error occurs
   */
  open fun buffer(distance: Double, quadrantSegments: Int): Geometry {
    return BufferOp.bufferOp(this, distance, quadrantSegments)
  }

  /**
   * Computes a buffer area around this geometry having the given
   * width and with a specified accuracy of approximation for circular arcs,
   * and using a specified end cap style.
   *
   * @param  distance  the width of the buffer (may be positive, negative or 0)
   * @param quadrantSegments the number of line segments used to represent a quadrant of a circle
   * @param endCapStyle the end cap style to use
   * @return a polygonal geometry representing the buffer region (which may be empty)
   *
   * @throws TopologyException if a robustness error occurs
   *
   * @see BufferOp
   */
  open fun buffer(distance: Double, quadrantSegments: Int, endCapStyle: Int): Geometry {
    return BufferOp.bufferOp(this, distance, quadrantSegments, endCapStyle)
  }

  /**
   *  Computes the smallest convex `Polygon` that contains all the
   *  points in the `Geometry`.
   *
   * @return    the minimum-area convex polygon containing this `Geometry`'
   *      s points
   */
  open fun convexHull(): Geometry {
    return (ConvexHull(this)).getConvexHull()
  }

  /**
   * Computes a new geometry which has all component coordinate sequences
   * in reverse order (opposite orientation) to this one.
   *
   * @return a reversed geometry
   */
  open fun reverse(): Geometry {

    val res = reverseInternal()
    if (this.envelope != null)
      res.envelope = this.envelope!!.copy()
    res.setSRID(getSRID())

    return res
  }

  protected abstract fun reverseInternal(): Geometry

  /**
   * Computes a `Geometry` representing the point-set which is
   * common to both this `Geometry` and the `other` Geometry.
   *
   * @param  other the `Geometry` with which to compute the intersection
   * @return a Geometry representing the point-set common to the two `Geometry`s
   * @throws TopologyException if a robustness error occurs
   * @throws IllegalArgumentException if the argument is a non-empty heterogeneous `GeometryCollection`
   */
  open fun intersection(other: Geometry): Geometry {
    return GeometryOverlay.intersection(this, other)
  }

  /**
   * Computes a `Geometry` representing the point-set
   * which is contained in both this
   * `Geometry` and the `other` Geometry.
   *
   * @param other
   *          the `Geometry` with which to compute the union
   * @return a point-set combining the points of this `Geometry` and the
   *         points of `other`
   * @throws TopologyException
   *           if a robustness error occurs
   * @throws IllegalArgumentException
   *           if either input is a non-empty GeometryCollection
   * @see LineMerger
   */
  open fun union(other: Geometry): Geometry {
    return GeometryOverlay.union(this, other)
  }

  /**
   * Computes a `Geometry` representing the closure of the point-set
   * of the points contained in this `Geometry` that are not contained in
   * the `other` Geometry.
   *
   * @param  other  the `Geometry` with which to compute the
   *      difference
   * @return a Geometry representing the point-set difference of this `Geometry` with
   *      `other`
   * @throws TopologyException if a robustness error occurs
   * @throws IllegalArgumentException if either input is a non-empty GeometryCollection
   */
  open fun difference(other: Geometry): Geometry {
    return GeometryOverlay.difference(this, other)
  }

  /**
   * Computes a `Geometry` representing the closure of the point-set
   * which is the union of the points in this `Geometry` which are not
   * contained in the `other` Geometry,
   * with the points in the `other` Geometry not contained in this
   * `Geometry`.
   *
   * @param  other the `Geometry` with which to compute the symmetric
   *      difference
   * @return a Geometry representing the point-set symmetric difference of this `Geometry`
   *      with `other`
   * @throws TopologyException if a robustness error occurs
   * @throws IllegalArgumentException if either input is a non-empty GeometryCollection
   */
  open fun symDifference(other: Geometry): Geometry {
    return GeometryOverlay.symDifference(this, other)
  }

  /**
   * Computes the union of all the elements of this geometry.
   *
   * @return the union geometry
   * @throws TopologyException if a robustness error occurs
   *
   * @see UnaryUnionOp
   */
  open fun union(): Geometry {
    return GeometryOverlay.union(this)
  }

  /**
   * Returns true if the two `Geometry`s are exactly equal,
   * up to a specified distance tolerance.
   *
   * @param other the `Geometry` with which to compare this `Geometry`
   * @param tolerance distance at or below which two `Coordinate`s
   *   are considered equal
   * @return `true` if this and the other `Geometry`
   *   have identical structure and point values, up to the distance tolerance.
   *
   * @see #equalsExact(Geometry)
   * @see #normalize()
   */
  abstract fun equalsExact(other: Geometry, tolerance: Double): Boolean

  /**
   * Returns true if the two `Geometry`s are exactly equal.
   *
   * @param  other  the `Geometry` with which to compare this `Geometry`
   * @return `true` if this and the other `Geometry`
   *      have identical structure and point values.
   *
   * @see #equalsExact(Geometry, double)
   * @see #normalize()
   */
  open fun equalsExact(other: Geometry): Boolean {
    return this === other || equalsExact(other, 0.0)
  }

  /**
   * Tests whether two geometries are exactly equal
   * in their normalized forms.
   *
   * @param g a Geometry
   * @return true if the input geometries are exactly equal in their normalized form
   */
  open fun equalsNorm(g: Geometry?): Boolean {
    if (g == null) return false
    return norm().equalsExact(g.norm())
  }

  /**
   *  Performs an operation with or on this `Geometry`'s
   *  coordinates.
   *
   * @param  filter  the filter to apply to this `Geometry`'s
   *      coordinates
   */
  abstract fun apply(filter: CoordinateFilter)

  /**
   *  Performs an operation on the coordinates in this `Geometry`'s
   *  [CoordinateSequence]s.
   *
   * @param  filter  the filter to apply
   */
  abstract fun apply(filter: CoordinateSequenceFilter)

  /**
   *  Performs an operation with or on this `Geometry` and its
   *  subelement `Geometry`s (if any).
   *
   * @param  filter  the filter to apply to this `Geometry` (and
   *      its children, if it is a `GeometryCollection`).
   */
  abstract fun apply(filter: GeometryFilter)

  /**
   *  Performs an operation with or on this Geometry and its
   *  component Geometry's.
   *
   * @param  filter  the filter to apply to this `Geometry`.
   */
  abstract fun apply(filter: GeometryComponentFilter)

  /**
   * Creates and returns a full copy of this [Geometry] object.
   *
   * @return a clone of this instance
   * @deprecated
   */
  open fun clone(): Any {
    return copy()
  }

  /**
   * Creates a deep copy of this [Geometry] object.
   *
   * @return a deep copy of this geometry
   */
  open fun copy(): Geometry {
    val copy = copyInternal()
    copy.envelope = if (envelope == null) null else envelope!!.copy()
    copy.SRID = this.SRID
    copy.userData = this.userData
    return copy
  }

  /**
   * An internal method to copy subclass-specific geometry data.
   *
   * @return a copy of the target geometry object.
   */
  protected abstract fun copyInternal(): Geometry

  /**
   *  Converts this `Geometry` to **normal form** (or **
   *  canonical form** ).
   */
  abstract fun normalize()

  /**
   * Creates a new Geometry which is a normalized
   * copy of this Geometry.
   *
   * @return a normalized copy of this geometry.
   * @see #normalize()
   */
  open fun norm(): Geometry {
    val copy = copy()
    copy.normalize()
    return copy
  }

  /**
   *  Returns whether this `Geometry` is greater than, equal to,
   *  or less than another `Geometry`.
   *
   * @param  o  a `Geometry` with which to compare this `Geometry`
   * @return    a positive number, 0, or a negative number
   */
  override fun compareTo(o: Any?): Int {
    val other = o as Geometry
    if (getTypeCode() != other.getTypeCode()) {
      return getTypeCode() - other.getTypeCode()
    }
    if (isEmpty() && other.isEmpty()) {
      return 0
    }
    if (isEmpty()) {
      return -1
    }
    if (other.isEmpty()) {
      return 1
    }
    return compareToSameClass(o)
  }

  /**
   *  Returns whether this `Geometry` is greater than, equal to,
   *  or less than another `Geometry`,
   * using the given [CoordinateSequenceComparator].
   *
   * @param  o  a `Geometry` with which to compare this `Geometry`
   * @param comp a `CoordinateSequenceComparator`
   *
   * @return    a positive number, 0, or a negative number
   */
  open fun compareTo(o: Any?, comp: CoordinateSequenceComparator): Int {
    val other = o as Geometry
    if (getTypeCode() != other.getTypeCode()) {
      return getTypeCode() - other.getTypeCode()
    }
    if (isEmpty() && other.isEmpty()) {
      return 0
    }
    if (isEmpty()) {
      return -1
    }
    if (other.isEmpty()) {
      return 1
    }
    return compareToSameClass(o, comp)
  }

  /**
   *  Returns whether the two `Geometry`s are equal, from the point
   *  of view of the `equalsExact` method.
   *
   * @param  other  the `Geometry` with which to compare this `Geometry`
   *      for equality
   * @return        `true` if the classes of the two `Geometry`
   *      s are considered to be equal by the `equalsExact` method.
   */
  protected open fun isEquivalentClass(other: Geometry): Boolean {
    return this::class == other::class
  }

  /**
   * Tests whether this is an instance of a general [GeometryCollection],
   * rather than a homogeneous subclass.
   *
   * @return true if this is a heterogeneous GeometryCollection
   */
  protected open fun isGeometryCollection(): Boolean {
    return getTypeCode() == TYPECODE_GEOMETRYCOLLECTION
  }

  /**
   *  Returns the minimum and maximum x and y values in this `Geometry`
   *  , or a null `Envelope` if this `Geometry` is empty.
   *
   * @return    this `Geometry`s bounding box
   */
  protected abstract fun computeEnvelopeInternal(): Envelope

  /**
   *  Returns whether this `Geometry` is greater than, equal to,
   *  or less than another `Geometry` having the same class.
   *
   * @param  o  a `Geometry` having the same class as this `Geometry`
   * @return    a positive number, 0, or a negative number
   */
  protected abstract fun compareToSameClass(o: Any?): Int

  /**
   *  Returns whether this `Geometry` is greater than, equal to,
   *  or less than another `Geometry` of the same class.
   * using the given [CoordinateSequenceComparator].
   *
   * @param  o  a `Geometry` having the same class as this `Geometry`
   * @param comp a `CoordinateSequenceComparator`
   * @return    a positive number, 0, or a negative number
   */
  protected abstract fun compareToSameClass(o: Any?, comp: CoordinateSequenceComparator): Int

  /**
   * Internal bridge: Kotlin `protected` (unlike Java same-package protected) cannot be
   * invoked cross-instance through a supertype reference. GeometryCollection needs to call
   * compareToSameClass on its component geometries, so this module-internal forwarder exposes it.
   */
  internal fun compareToSameClassWith(o: Any?, comp: CoordinateSequenceComparator): Int {
    return compareToSameClass(o, comp)
  }

  /** Internal bridge for the single-argument compareToSameClass (see the two-arg bridge above). */
  internal fun compareToSameClassWith(o: Any?): Int {
    return compareToSameClass(o)
  }

  /**
   * Internal bridge exposing the protected isGeometryCollection() to same-module helper
   * classes (GeometryOverlay, GeometryRelate) which are not subclasses and so, under Kotlin's
   * (non-package) protected rules, could not otherwise call it cross-instance.
   */
  internal fun isGeometryCollectionInternal(): Boolean {
    return isGeometryCollection()
  }

  /**
   *  Returns the first non-zero result of `compareTo` encountered as
   *  the two `Collection`s are iterated over.
   *
   * @param  a  a `Collection` of `Comparable`s
   * @param  b  a `Collection` of `Comparable`s
   * @return    the first non-zero `compareTo` result, if any;
   *      otherwise, zero
   */
  protected open fun compare(a: Collection<*>, b: Collection<*>): Int {
    val i = a.iterator()
    val j = b.iterator()
    while (i.hasNext() && j.hasNext()) {
      @Suppress("UNCHECKED_CAST")
      val aElement = i.next() as Comparable<Any?>
      val bElement = j.next()
      val comparison = aElement.compareTo(bElement)
      if (comparison != 0) {
        return comparison
      }
    }
    if (i.hasNext()) {
      return 1
    }
    if (j.hasNext()) {
      return -1
    }
    return 0
  }

  protected open fun equal(a: Coordinate, b: Coordinate, tolerance: Double): Boolean {
    if (tolerance == 0.0) {
      return a.equals(b)
    }
    return a.distance(b) <= tolerance
  }

  protected abstract fun getTypeCode(): Int

  private fun createPointFromInternalCoord(coord: Coordinate?, exemplar: Geometry): Point {
    // create empty point for null input
    if (coord == null)
      return exemplar.getFactory().createPoint()
    exemplar.getPrecisionModel().makePrecise(coord)
    return exemplar.getFactory().createPoint(coord)
  }

  companion object {

    @JvmField
    protected val TYPECODE_POINT = 0
    @JvmField
    protected val TYPECODE_MULTIPOINT = 1
    @JvmField
    protected val TYPECODE_LINESTRING = 2
    @JvmField
    protected val TYPECODE_LINEARRING = 3
    @JvmField
    protected val TYPECODE_MULTILINESTRING = 4
    @JvmField
    protected val TYPECODE_POLYGON = 5
    @JvmField
    protected val TYPECODE_MULTIPOLYGON = 6
    @JvmField
    protected val TYPECODE_GEOMETRYCOLLECTION = 7

    const val TYPENAME_POINT = "Point"
    const val TYPENAME_MULTIPOINT = "MultiPoint"
    const val TYPENAME_LINESTRING = "LineString"
    const val TYPENAME_LINEARRING = "LinearRing"
    const val TYPENAME_MULTILINESTRING = "MultiLineString"
    const val TYPENAME_POLYGON = "Polygon"
    const val TYPENAME_MULTIPOLYGON = "MultiPolygon"
    const val TYPENAME_GEOMETRYCOLLECTION = "GeometryCollection"

    private val geometryChangedFilter: GeometryComponentFilter = object : GeometryComponentFilter {
      override fun filter(geom: Geometry) {
        geom.geometryChangedAction()
      }
    }

    /**
     * Returns true if the array contains any non-empty `Geometry`s.
     *
     * @param  geometries  an array of `Geometry`s; no elements may be
     *      `null`
     * @return             `true` if any of the `Geometry`s
     *      `isEmpty` methods return `false`
     */
    @JvmStatic
    protected fun hasNonEmptyElements(geometries: Array<out Geometry>): Boolean {
      for (i in 0 until geometries.size) {
        if (!geometries[i].isEmpty()) {
          return true
        }
      }
      return false
    }

    /**
     *  Returns true if the array contains any `null` elements.
     *
     * @param  array  an array to validate
     * @return        `true` if any of `array`s elements are
     *      `null`
     */
    @JvmStatic
    protected fun hasNullElements(array: Array<out Any?>): Boolean {
      for (i in 0 until array.size) {
        if (array[i] == null) {
          return true
        }
      }
      return false
    }

    /**
     *  Throws an exception if `g`'s type is a `GeometryCollection`.
     *
     * @param  g the `Geometry` to check
     * @throws  IllegalArgumentException  if `g` is a `GeometryCollection`
     *      but not one of its subclasses
     */
    @JvmStatic
    fun checkNotGeometryCollection(g: Geometry) {
      if (g.isGeometryCollection()) {
        throw IllegalArgumentException("Operation does not support GeometryCollection arguments")
      }
    }
  }
}
