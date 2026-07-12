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
package org.locationtech.jts.geom.impl

import kotlin.jvm.JvmField
import kotlin.math.max


import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.CoordinateSequence
import org.locationtech.jts.geom.CoordinateSequenceFactory
import org.locationtech.jts.geom.Coordinates

/**
 * Builds packed array coordinate sequences.
 * The array data type can be either
 * `double` or `float`,
 * and defaults to `double`.
 */
class PackedCoordinateSequenceFactory(
  /**
   * Gets the type of packed coordinate sequence this factory builds, either
   * [PackedCoordinateSequenceFactory.FLOAT] or
   * [PackedCoordinateSequenceFactory.DOUBLE]
   *
   * @return the type of packed array built
   */
  val type: Int
) : CoordinateSequenceFactory {

  /**
   * Creates a new PackedCoordinateSequenceFactory
   * of type DOUBLE.
   */
  constructor() : this(DOUBLE)

  /**
   * @see CoordinateSequenceFactory.create
   */
  override fun create(coordinates: Array<Coordinate>?): CoordinateSequence {
    var dimension = DEFAULT_DIMENSION
    var measures = DEFAULT_MEASURES
    if (coordinates != null && coordinates.isNotEmpty() && coordinates[0] != null) {
      val first = coordinates[0]
      dimension = Coordinates.dimension(first)
      measures = Coordinates.measures(first)
    }
    return if (type == DOUBLE) {
      PackedCoordinateSequence.Double(coordinates, dimension, measures)
    } else {
      PackedCoordinateSequence.Float(coordinates, dimension, measures)
    }
  }

  /**
   * @see CoordinateSequenceFactory.create
   */
  override fun create(coordSeq: CoordinateSequence?): CoordinateSequence {
    val dimension = coordSeq!!.getDimension()
    val measures = coordSeq.getMeasures()
    return if (type == DOUBLE) {
      PackedCoordinateSequence.Double(coordSeq.toCoordinateArray(), dimension, measures)
    } else {
      PackedCoordinateSequence.Float(coordSeq.toCoordinateArray(), dimension, measures)
    }
  }

  /**
   * Creates a packed coordinate sequence of type [DOUBLE]
   * from the provided array
   * using the given coordinate dimension and a measure count of 0.
   *
   * @param packedCoordinates the array containing coordinate values
   * @param dimension the coordinate dimension
   * @return a packed coordinate sequence of type [DOUBLE]
   */
  fun create(packedCoordinates: DoubleArray, dimension: Int): CoordinateSequence {
    return create(packedCoordinates, dimension, DEFAULT_MEASURES)
  }

  /**
   * Creates a packed coordinate sequence of type [DOUBLE]
   * from the provided array
   * using the given coordinate dimension and measure count.
   *
   * @param packedCoordinates the array containing coordinate values
   * @param dimension the coordinate dimension
   * @param measures the coordinate measure count
   * @return a packed coordinate sequence of type [DOUBLE]
   */
  fun create(packedCoordinates: DoubleArray, dimension: Int, measures: Int): CoordinateSequence {
    return if (type == DOUBLE) {
      PackedCoordinateSequence.Double(packedCoordinates, dimension, measures)
    } else {
      PackedCoordinateSequence.Float(packedCoordinates, dimension, measures)
    }
  }

  /**
   * Creates a packed coordinate sequence of type [FLOAT]
   * from the provided array.
   *
   * @param packedCoordinates the array containing coordinate values
   * @param dimension the coordinate dimension
   * @return a packed coordinate sequence of type [FLOAT]
   */
  fun create(packedCoordinates: FloatArray, dimension: Int): CoordinateSequence {
    return create(packedCoordinates, dimension, max(DEFAULT_MEASURES, dimension - 3))
  }

  /**
   * Creates a packed coordinate sequence of type [FLOAT]
   * from the provided array.
   *
   * @param packedCoordinates the array containing coordinate values
   * @param dimension the coordinate dimension
   * @param measures the coordinate measure count
   * @return a packed coordinate sequence of type [FLOAT]
   */
  fun create(packedCoordinates: FloatArray, dimension: Int, measures: Int): CoordinateSequence {
    return if (type == DOUBLE) {
      PackedCoordinateSequence.Double(packedCoordinates, dimension, measures)
    } else {
      PackedCoordinateSequence.Float(packedCoordinates, dimension, measures)
    }
  }

  /**
   * @see org.locationtech.jts.geom.CoordinateSequenceFactory.create
   */
  override fun create(size: Int, dimension: Int): CoordinateSequence {
    return if (type == DOUBLE) {
      PackedCoordinateSequence.Double(
          size, dimension, max(DEFAULT_MEASURES, dimension - 3))
    } else {
      PackedCoordinateSequence.Float(
          size, dimension, max(DEFAULT_MEASURES, dimension - 3))
    }
  }

  /**
   * @see org.locationtech.jts.geom.CoordinateSequenceFactory.create
   */
  override fun create(size: Int, dimension: Int, measures: Int): CoordinateSequence {
    return if (type == DOUBLE) {
      PackedCoordinateSequence.Double(size, dimension, measures)
    } else {
      PackedCoordinateSequence.Float(size, dimension, measures)
    }
  }

  companion object {

    /**
     * Type code for arrays of type `double`.
     */
    const val DOUBLE = 0

    /**
     * Type code for arrays of type `float`.
     */
    const val FLOAT = 1

    /**
     * A factory using array type [DOUBLE]
     */
    @JvmField
    val DOUBLE_FACTORY = PackedCoordinateSequenceFactory(DOUBLE)

    /**
     * A factory using array type [FLOAT]
     */
    @JvmField
    val FLOAT_FACTORY = PackedCoordinateSequenceFactory(FLOAT)

    private const val DEFAULT_MEASURES = 0

    private const val DEFAULT_DIMENSION = 3
  }
}
