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
 * Models an OGC SFS <code>LinearRing</code>.
 * A <code>LinearRing</code> is a {@link LineString} which is both closed and simple.
 * <p>
 * A ring must have either 0 or 3 or more points.
 * The first and last points must be equal (in 2D).
 * If these conditions are not met, the constructors throw
 * an {@link IllegalArgumentException}.
 *
 * @version 1.7
 */
open class LinearRing : LineString {

  /**
   * Constructs a <code>LinearRing</code> with the given points.
   *
   * @throws IllegalArgumentException if the ring is not closed, or has too few points
   *
   * @deprecated Use GeometryFactory instead
   */
  constructor(
    points: Array<Coordinate>?, precisionModel: PrecisionModel,
    SRID: Int
  ) : this(points, GeometryFactory(precisionModel, SRID)) {
    validateConstruction()
  }

  /**
   * This method is ONLY used to avoid deprecation warnings.
   * @throws IllegalArgumentException if the ring is not closed, or has too few points
   */
  private constructor(points: Array<Coordinate>?, factory: GeometryFactory) : this(factory.getCoordinateSequenceFactory().create(points), factory)

  /**
   * Constructs a <code>LinearRing</code> with the vertices
   * specified by the given {@link CoordinateSequence}.
   *
   * @throws IllegalArgumentException if the ring is not closed, or has too few points
   *
   */
  constructor(points: CoordinateSequence?, factory: GeometryFactory) : super(points, factory) {
    validateConstruction()
  }

  private fun validateConstruction() {
    if (!isEmpty() && !super.isClosed()) {
      throw IllegalArgumentException("Points of LinearRing do not form a closed linestring")
    }
    if (getCoordinateSequence().size() >= 1 && getCoordinateSequence().size() < MINIMUM_VALID_SIZE) {
      throw IllegalArgumentException(
        "Invalid number of points in LinearRing (found "
          + getCoordinateSequence().size() + " - must be 0 or >= " + MINIMUM_VALID_SIZE + ")"
      )
    }
  }

  /**
   * Returns <code>Dimension.FALSE</code>, since by definition LinearRings do
   * not have a boundary.
   *
   * @return Dimension.FALSE
   */
  override fun getBoundaryDimension(): Int {
    return Dimension.FALSE
  }

  /**
   * Tests whether this ring is closed.
   * Empty rings are closed by definition.
   *
   * @return true if this ring is closed
   */
  override fun isClosed(): Boolean {
    if (isEmpty()) {
      // empty LinearRings are closed by definition
      return true
    }
    return super.isClosed()
  }

  override fun getGeometryType(): String {
    return Geometry.TYPENAME_LINEARRING
  }

  override fun getTypeCode(): Int {
    return Geometry.TYPECODE_LINEARRING
  }

  override fun copyInternal(): LinearRing {
    return LinearRing(points.copy(), factory)
  }

  override fun reverse(): LinearRing {
    return super.reverse() as LinearRing
  }

  override fun reverseInternal(): LinearRing {
    val seq = points.copy()
    CoordinateSequences.reverse(seq)
    return getFactory().createLinearRing(seq)
  }

  companion object {
    /**
     * The minimum number of vertices allowed in a valid non-empty ring.
     * Empty rings with 0 vertices are also valid.
     */
    const val MINIMUM_VALID_SIZE = 3

  }
}
