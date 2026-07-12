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
 * @version 1.7
 */
abstract class Geometry(
  /**
   * The {@link GeometryFactory} used to create this Geometry
   */
  @JvmField protected val factory: GeometryFactory
) : Comparable<Any?> {

  /**
   *  The bounding box of this <code>Geometry</code>.
   */
  @JvmField
  protected var envelope: Envelope? = null

  /**
   *  The ID of the Spatial Reference System used by this <code>Geometry</code>
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
   * @return the name of this <code>Geometry</code>s actual class
   */
  abstract fun getGeometryType(): String

  /**
   *  Returns the ID of the Spatial Reference System used by the <code>Geometry</code>.
   *
   * @return    the ID of the coordinate space in which the <code>Geometry</code>
   *      is defined.
   */
  open fun getSRID(): Int {
    return SRID
  }

  /**
   *  Sets the ID of the Spatial Reference System used by the <code>Geometry</code>.
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
   * @return the user data object, or <code>null</code> if none set
   */
  open fun getUserData(): Any? {
    return userData
  }

  /**
   * Returns the number of {@link Geometry}s in a {@link GeometryCollection}
   * (or 1, if the geometry is not a collection).
   *
   * @return the number of geometries contained in this geometry
   */
  open fun getNumGeometries(): Int {
    return 1
  }

  /**
   * Returns an element {@link Geometry} from a {@link GeometryCollection}
   * (or <code>this</code>, if the geometry is not a collection).
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
   *  Returns the <code>PrecisionModel</code> used by the <code>Geometry</code>.
   *
   * @return    the specification of the grid of allowable points, for this
   *      <code>Geometry</code> and all other <code>Geometry</code>s
   */
  open fun getPrecisionModel(): PrecisionModel {
    return factory.getPrecisionModel()
  }

  /**
   *  Returns a vertex of this geometry
   *  (usually, but not necessarily, the first one),
   *  or <code>null</code> if the geometry is empty.
   *
   * @return a coordinate which is a vertex of this <code>Geometry</code>.
   * @return null if this Geometry is empty
   */
  abstract fun getCoordinate(): Coordinate?

  /**
   *  Returns an array containing the values of all the vertices for
   *  this geometry.
   *
   * @return    the vertices of this <code>Geometry</code>
   */
  abstract fun getCoordinates(): Array<Coordinate>

  /**
   *  Returns the count of this <code>Geometry</code>s vertices.
   *
   * @return    the number of vertices in this <code>Geometry</code>
   */
  abstract fun getNumPoints(): Int

  /**
   * Tests whether this {@link Geometry} is simple.
   *
   * @return <code>true</code> if this <code>Geometry</code> is simple
   * @see #isValid
   */
  open fun isSimple(): Boolean {
    val op = IsSimpleOp(this)
    return op.isSimple()
  }

  /**
   * Tests whether this <code>Geometry</code>
   * is topologically valid, according to the OGC SFS specification.
   *
   * @return <code>true</code> if this <code>Geometry</code> is valid
   *
   * @see IsValidOp
   */
  open fun isValid(): Boolean {
    return IsValidOp.isValid(this)
  }

  /**
   * Tests whether the set of points covered by this <code>Geometry</code> is
   * empty.
   *
   * @return <code>true</code> if this <code>Geometry</code> does not cover any points
   */
  abstract fun isEmpty(): Boolean

  /**
   * Returns the minimum distance between this <code>Geometry</code>
   * and another <code>Geometry</code>.
   *
   * @param  g the <code>Geometry</code> from which to compute the distance
   * @return the distance between the geometries
   * @return 0 if either input geometry is empty
   * @throws IllegalArgumentException if g is null
   */
  open fun distance(g: Geometry): Double {
    return DistanceOp.distance(this, g)
  }

  /**
   * Tests whether the distance from this <code>Geometry</code>
   * to another is less than or equal to a specified value.
   *
   * @param geom the Geometry to check the distance to
   * @param distance the distance value to compare
   * @return <code>true</code> if the geometries are less than <code>distance</code> apart.
   */
  open fun isWithinDistance(geom: Geometry, distance: Double): Boolean {
    return DistanceOp.isWithinDistance(this, geom, distance)
  }

  /**
   * Tests whether this is a rectangular {@link Polygon}.
   *
   * @return true if the geometry is a rectangle.
   */
  open fun isRectangle(): Boolean {
    // Polygon overrides to check for actual rectangle
    return false
  }

  /**
   *  Returns the area of this <code>Geometry</code>.
   *
   * @return the area of the Geometry
   */
  open fun getArea(): Double {
    return 0.0
  }

  /**
   *  Returns the length of this <code>Geometry</code>.
   *
   * @return the length of the Geometry
   */
  open fun getLength(): Double {
    return 0.0
  }

  /**
   * Computes the centroid of this <code>Geometry</code>.
   *
   * @return a {@link Point} which is the centroid of this Geometry
   */
  open fun getCentroid(): Point {
    if (isEmpty())
      return factory.createPoint()
    val centPt = Centroid.getCentroid(this)
    return createPointFromInternalCoord(centPt, this)
  }

  /**
   * Computes an interior point of this <code>Geometry</code>.
   *
   * @return a {@link Point} which is in the interior of this Geometry
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
   * if this <code>Geometry</code>  is empty.
   *
   * @return    the closure of the combinatorial boundary of this <code>Geometry</code>
   */
  abstract fun getBoundary(): Geometry

  /**
   *  Returns the dimension of this <code>Geometry</code>s inherent boundary.
   *
   * @return    the dimension of the boundary of the class implementing this
   *      interface.
   */
  abstract fun getBoundaryDimension(): Int

  /**
   *  Gets a Geometry representing the envelope (bounding box) of
   *  this <code>Geometry</code>.
   *
   * @return a Geometry representing the envelope of this Geometry
   *
   * @see GeometryFactory#toGeometry(Envelope)
   */
  open fun getEnvelope(): Geometry {
    return getFactory().toGeometry(getEnvelopeInternal())
  }

  /**
   * Gets an {@link Envelope} containing
   * the minimum and maximum x and y values in this <code>Geometry</code>.
   *
   * @return the envelope of this <code>Geometry</code>.
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
   * @param  g  the <code>Geometry</code> with which to compare this <code>Geometry</code>
   * @return        <code>true</code> if the two <code>Geometry</code>s are
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
   * @param  g  the <code>Geometry</code> with which to compare this <code>Geometry</code>
   * @return        <code>true</code> if the two <code>Geometry</code>s touch;
   *      Returns <code>false</code> if both <code>Geometry</code>s are points
   */
  open fun touches(g: Geometry): Boolean {
    return GeometryRelate.touches(this, g)
  }

  /**
   * Tests whether this geometry intersects the argument geometry.
   *
   * @param  g  the <code>Geometry</code> with which to compare this <code>Geometry</code>
   * @return        <code>true</code> if the two <code>Geometry</code>s intersect
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
   * @param  g  the <code>Geometry</code> with which to compare this <code>Geometry</code>
   * @return        <code>true</code> if the two <code>Geometry</code>s cross.
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
   * @param  g  the <code>Geometry</code> with which to compare this <code>Geometry</code>
   * @return        <code>true</code> if this <code>Geometry</code> is within
   *      <code>g</code>
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
   * @param  g  the <code>Geometry</code> with which to compare this <code>Geometry</code>
   * @return        <code>true</code> if this <code>Geometry</code> contains <code>g</code>
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
   * @param  g  the <code>Geometry</code> with which to compare this <code>Geometry</code>
   * @return        <code>true</code> if the two <code>Geometry</code>s overlap.
   */
  open fun overlaps(g: Geometry): Boolean {
    return GeometryRelate.overlaps(this, g)
  }

  /**
   * Tests whether this geometry covers the
   * argument geometry.
   *
   * @param  g  the <code>Geometry</code> with which to compare this <code>Geometry</code>
   * @return        <code>true</code> if this <code>Geometry</code> covers <code>g</code>
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
   * @param  g  the <code>Geometry</code> with which to compare this <code>Geometry</code>
   * @return        <code>true</code> if this <code>Geometry</code> is covered by <code>g</code>
   *
   * @see Geometry#within
   * @see Geometry#covers
   */
  open fun coveredBy(g: Geometry): Boolean {
    return GeometryRelate.coveredBy(this, g)
  }

  /**
   * Tests whether the elements in the DE-9IM
   * {@link IntersectionMatrix} for the two <code>Geometry</code>s match the elements in <code>intersectionPattern</code>.
   *
   * @param  g                the <code>Geometry</code> with which to compare
   *      this <code>Geometry</code>
   * @param  intersectionPattern  the pattern against which to check the
   *      intersection matrix for the two <code>Geometry</code>s
   * @return                      <code>true</code> if the DE-9IM intersection
   *      matrix for the two <code>Geometry</code>s match <code>intersectionPattern</code>
   * @see IntersectionMatrix
   */
  open fun relate(g: Geometry, intersectionPattern: String): Boolean {
    return GeometryRelate.relate(this, g, intersectionPattern)
  }

  /**
   *  Returns the DE-9IM {@link IntersectionMatrix} for the two <code>Geometry</code>s.
   *
   * @param  g  the <code>Geometry</code> with which to compare this <code>Geometry</code>
   * @return        an {@link IntersectionMatrix} describing the intersections of the interiors,
   *      boundaries and exteriors of the two <code>Geometry</code>s
   */
  open fun relate(g: Geometry): IntersectionMatrix {
    return GeometryRelate.relate(this, g)
  }

  /**
   * Tests whether this geometry is
   * topologically equal to the argument geometry.
   *
   * @param  g  the <code>Geometry</code> with which to compare this <code>Geometry</code>
   * @return true if the two <code>Geometry</code>s are topologically equal
   *
   * @see #equalsTopo(Geometry)
   */
  open fun equals(g: Geometry?): Boolean {
    if (g == null) return false
    return equalsTopo(g)
  }

  /**
   * Tests whether this geometry is topologically equal to the argument geometry
   * as defined by the SFS <code>equals</code> predicate.
   *
   * @param g the <code>Geometry</code> with which to compare this <code>Geometry</code>
   * @return <code>true</code> if the two <code>Geometry</code>s are topologically equal
   *
   * @see #equalsExact(Geometry)
   */
  open fun equalsTopo(g: Geometry): Boolean {
    return GeometryRelate.equalsTopo(this, g)
  }

  /**
   * Tests whether this geometry is structurally and numerically equal
   * to a given <code>Object</code>.
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
   *  Returns the Well-known Text representation of this <code>Geometry</code>.
   *
   * @return    the Well-known Text representation of this <code>Geometry</code>
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
   *  Computes the smallest convex <code>Polygon</code> that contains all the
   *  points in the <code>Geometry</code>.
   *
   * @return    the minimum-area convex polygon containing this <code>Geometry</code>'
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
   * Computes a <code>Geometry</code> representing the point-set which is
   * common to both this <code>Geometry</code> and the <code>other</code> Geometry.
   *
   * @param  other the <code>Geometry</code> with which to compute the intersection
   * @return a Geometry representing the point-set common to the two <code>Geometry</code>s
   * @throws TopologyException if a robustness error occurs
   * @throws IllegalArgumentException if the argument is a non-empty heterogeneous <code>GeometryCollection</code>
   */
  open fun intersection(other: Geometry): Geometry {
    return GeometryOverlay.intersection(this, other)
  }

  /**
   * Computes a <code>Geometry</code> representing the point-set
   * which is contained in both this
   * <code>Geometry</code> and the <code>other</code> Geometry.
   *
   * @param other
   *          the <code>Geometry</code> with which to compute the union
   * @return a point-set combining the points of this <code>Geometry</code> and the
   *         points of <code>other</code>
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
   * Computes a <code>Geometry</code> representing the closure of the point-set
   * of the points contained in this <code>Geometry</code> that are not contained in
   * the <code>other</code> Geometry.
   *
   * @param  other  the <code>Geometry</code> with which to compute the
   *      difference
   * @return a Geometry representing the point-set difference of this <code>Geometry</code> with
   *      <code>other</code>
   * @throws TopologyException if a robustness error occurs
   * @throws IllegalArgumentException if either input is a non-empty GeometryCollection
   */
  open fun difference(other: Geometry): Geometry {
    return GeometryOverlay.difference(this, other)
  }

  /**
   * Computes a <code>Geometry</code> representing the closure of the point-set
   * which is the union of the points in this <code>Geometry</code> which are not
   * contained in the <code>other</code> Geometry,
   * with the points in the <code>other</code> Geometry not contained in this
   * <code>Geometry</code>.
   *
   * @param  other the <code>Geometry</code> with which to compute the symmetric
   *      difference
   * @return a Geometry representing the point-set symmetric difference of this <code>Geometry</code>
   *      with <code>other</code>
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
   * Returns true if the two <code>Geometry</code>s are exactly equal,
   * up to a specified distance tolerance.
   *
   * @param other the <code>Geometry</code> with which to compare this <code>Geometry</code>
   * @param tolerance distance at or below which two <code>Coordinate</code>s
   *   are considered equal
   * @return <code>true</code> if this and the other <code>Geometry</code>
   *   have identical structure and point values, up to the distance tolerance.
   *
   * @see #equalsExact(Geometry)
   * @see #normalize()
   */
  abstract fun equalsExact(other: Geometry, tolerance: Double): Boolean

  /**
   * Returns true if the two <code>Geometry</code>s are exactly equal.
   *
   * @param  other  the <code>Geometry</code> with which to compare this <code>Geometry</code>
   * @return <code>true</code> if this and the other <code>Geometry</code>
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
   *  Performs an operation with or on this <code>Geometry</code>'s
   *  coordinates.
   *
   * @param  filter  the filter to apply to this <code>Geometry</code>'s
   *      coordinates
   */
  abstract fun apply(filter: CoordinateFilter)

  /**
   *  Performs an operation on the coordinates in this <code>Geometry</code>'s
   *  {@link CoordinateSequence}s.
   *
   * @param  filter  the filter to apply
   */
  abstract fun apply(filter: CoordinateSequenceFilter)

  /**
   *  Performs an operation with or on this <code>Geometry</code> and its
   *  subelement <code>Geometry</code>s (if any).
   *
   * @param  filter  the filter to apply to this <code>Geometry</code> (and
   *      its children, if it is a <code>GeometryCollection</code>).
   */
  abstract fun apply(filter: GeometryFilter)

  /**
   *  Performs an operation with or on this Geometry and its
   *  component Geometry's.
   *
   * @param  filter  the filter to apply to this <code>Geometry</code>.
   */
  abstract fun apply(filter: GeometryComponentFilter)

  /**
   * Creates and returns a full copy of this {@link Geometry} object.
   *
   * @return a clone of this instance
   * @deprecated
   */
  open fun clone(): Any {
    return copy()
  }

  /**
   * Creates a deep copy of this {@link Geometry} object.
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
   *  Converts this <code>Geometry</code> to <b>normal form</b> (or <b>
   *  canonical form</b> ).
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
   *  Returns whether this <code>Geometry</code> is greater than, equal to,
   *  or less than another <code>Geometry</code>.
   *
   * @param  o  a <code>Geometry</code> with which to compare this <code>Geometry</code>
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
   *  Returns whether this <code>Geometry</code> is greater than, equal to,
   *  or less than another <code>Geometry</code>,
   * using the given {@link CoordinateSequenceComparator}.
   *
   * @param  o  a <code>Geometry</code> with which to compare this <code>Geometry</code>
   * @param comp a <code>CoordinateSequenceComparator</code>
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
   *  Returns whether the two <code>Geometry</code>s are equal, from the point
   *  of view of the <code>equalsExact</code> method.
   *
   * @param  other  the <code>Geometry</code> with which to compare this <code>Geometry</code>
   *      for equality
   * @return        <code>true</code> if the classes of the two <code>Geometry</code>
   *      s are considered to be equal by the <code>equalsExact</code> method.
   */
  protected open fun isEquivalentClass(other: Geometry): Boolean {
    return this::class == other::class
  }

  /**
   * Tests whether this is an instance of a general {@link GeometryCollection},
   * rather than a homogeneous subclass.
   *
   * @return true if this is a heterogeneous GeometryCollection
   */
  protected open fun isGeometryCollection(): Boolean {
    return getTypeCode() == TYPECODE_GEOMETRYCOLLECTION
  }

  /**
   *  Returns the minimum and maximum x and y values in this <code>Geometry</code>
   *  , or a null <code>Envelope</code> if this <code>Geometry</code> is empty.
   *
   * @return    this <code>Geometry</code>s bounding box
   */
  protected abstract fun computeEnvelopeInternal(): Envelope

  /**
   *  Returns whether this <code>Geometry</code> is greater than, equal to,
   *  or less than another <code>Geometry</code> having the same class.
   *
   * @param  o  a <code>Geometry</code> having the same class as this <code>Geometry</code>
   * @return    a positive number, 0, or a negative number
   */
  protected abstract fun compareToSameClass(o: Any?): Int

  /**
   *  Returns whether this <code>Geometry</code> is greater than, equal to,
   *  or less than another <code>Geometry</code> of the same class.
   * using the given {@link CoordinateSequenceComparator}.
   *
   * @param  o  a <code>Geometry</code> having the same class as this <code>Geometry</code>
   * @param comp a <code>CoordinateSequenceComparator</code>
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
   *  Returns the first non-zero result of <code>compareTo</code> encountered as
   *  the two <code>Collection</code>s are iterated over.
   *
   * @param  a  a <code>Collection</code> of <code>Comparable</code>s
   * @param  b  a <code>Collection</code> of <code>Comparable</code>s
   * @return    the first non-zero <code>compareTo</code> result, if any;
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
     * Returns true if the array contains any non-empty <code>Geometry</code>s.
     *
     * @param  geometries  an array of <code>Geometry</code>s; no elements may be
     *      <code>null</code>
     * @return             <code>true</code> if any of the <code>Geometry</code>s
     *      <code>isEmpty</code> methods return <code>false</code>
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
     *  Returns true if the array contains any <code>null</code> elements.
     *
     * @param  array  an array to validate
     * @return        <code>true</code> if any of <code>array</code>s elements are
     *      <code>null</code>
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
     *  Throws an exception if <code>g</code>'s type is a <code>GeometryCollection</code>.
     *
     * @param  g the <code>Geometry</code> to check
     * @throws  IllegalArgumentException  if <code>g</code> is a <code>GeometryCollection</code>
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
