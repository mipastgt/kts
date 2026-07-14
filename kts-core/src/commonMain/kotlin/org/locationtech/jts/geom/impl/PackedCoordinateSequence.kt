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
import org.locationtech.jts.geom.CoordinateSequences
import org.locationtech.jts.geom.CoordinateXY
import org.locationtech.jts.geom.CoordinateXYM
import org.locationtech.jts.geom.CoordinateXYZM
import org.locationtech.jts.geom.Envelope

/**
 * A [CoordinateSequence] implementation based on a packed arrays.
 * In this implementation, [Coordinate]s returned by #toArray and #get are copies
 * of the internal values.
 * To change the actual values, use the provided setters.
 *
 * For efficiency, created Coordinate arrays
 * are cached using a soft reference.
 * The cache is cleared each time the coordinate sequence contents are
 * modified through a setter method.
 *
 */
abstract class PackedCoordinateSequence protected constructor(
  dimension: Int,
  measures: Int
) : CoordinateSequence {
  /**
   * The dimensions of the coordinates held in the packed array
   */
  @JvmField
  protected var dimension: Int

  /**
   * The number of measures of the coordinates held in the packed array.
   */
  @JvmField
  protected var measures: Int

  init {
    if (dimension - measures < 2) {
      throw IllegalArgumentException("Must have at least 2 spatial dimensions")
    }
    this.dimension = dimension
    this.measures = measures
  }

  /**
   * A soft reference to the Coordinate[] representation of this sequence.
   * Makes repeated coordinate array accesses more efficient.
   */
  @JvmField
  protected var coordRef: Array<Coordinate>? = null

  /**
   * @see CoordinateSequence.getDimension
   */
  override fun getDimension(): Int {
    return this.dimension
  }

  /**
   * @see CoordinateSequence.getMeasures
   */
  override fun getMeasures(): Int {
    return this.measures
  }

  /**
   * @see CoordinateSequence.getCoordinate
   */
  override fun getCoordinate(i: Int): Coordinate {
    val coords = getCachedCoords()
    return if (coords != null)
      coords[i]
    else
      getCoordinateInternal(i)
  }

  /**
   * @see CoordinateSequence.getCoordinate
   */
  override fun getCoordinateCopy(i: Int): Coordinate {
    return getCoordinateInternal(i)
  }

  /**
   * @see CoordinateSequence.getCoordinate
   */
  override fun getCoordinate(i: Int, coord: Coordinate) {
    coord.x = getOrdinate(i, 0)
    coord.y = getOrdinate(i, 1)
    if (hasZ()) {
      coord.setZ(getZ(i))
    }
    if (hasM()) {
      coord.setM(getM(i))
    }
  }

  /**
   * @see CoordinateSequence.toCoordinateArray
   */
  override fun toCoordinateArray(): Array<Coordinate> {
    var coords = getCachedCoords()
    // testing - never cache
    if (coords != null)
      return coords

    coords = Array(size()) { i -> getCoordinateInternal(i) }
    coordRef = coords

    return coords
  }

  private fun getCachedCoords(): Array<Coordinate>? {
    return coordRef
  }

  /**
   * @see CoordinateSequence.getX
   */
  override fun getX(index: Int): kotlin.Double {
    return getOrdinate(index, 0)
  }

  /**
   * @see CoordinateSequence.getY
   */
  override fun getY(index: Int): kotlin.Double {
    return getOrdinate(index, 1)
  }

  /**
   * @see CoordinateSequence.getOrdinate
   */
  abstract override fun getOrdinate(index: Int, ordinateIndex: Int): kotlin.Double

  /**
   * Sets the first ordinate of a coordinate in this sequence.
   *
   * @param index  the coordinate index
   * @param value  the new ordinate value
   */
  fun setX(index: Int, value: kotlin.Double) {
    coordRef = null
    setOrdinate(index, 0, value)
  }

  /**
   * Sets the second ordinate of a coordinate in this sequence.
   *
   * @param index  the coordinate index
   * @param value  the new ordinate value
   */
  fun setY(index: Int, value: kotlin.Double) {
    coordRef = null
    setOrdinate(index, 1, value)
  }

  override fun toString(): String {
    return CoordinateSequences.toString(this)
  }

  protected fun readResolve(): Any {
    coordRef = null
    return this
  }

  /**
   * Returns a Coordinate representation of the specified coordinate, by always
   * building a new Coordinate object
   *
   * @param index  the coordinate index
   * @return  the [Coordinate] at the given index
   */
  protected abstract fun getCoordinateInternal(index: Int): Coordinate

  /**
   * @see CoordinateSequence.clone
   */
  @Deprecated("")
  abstract override fun clone(): Any

  abstract override fun copy(): PackedCoordinateSequence

  /**
   * Sets the ordinate of a coordinate in this sequence.
   * <br>
   * Warning: for performance reasons the ordinate index is not checked
   * - if it is over dimensions you may not get an exception but a meaningless value.
   *
   * @param index
   *          the coordinate index
   * @param ordinate
   *          the ordinate index in the coordinate, 0 based, smaller than the
   *          number of dimensions
   * @param value
   *          the new ordinate value
   */
  abstract override fun setOrdinate(index: Int, ordinate: Int, value: kotlin.Double)

  /**
   * Packed coordinate sequence implementation based on doubles
   */
  class Double : PackedCoordinateSequence {
    /**
     * The packed coordinate array
     */
    @JvmField
    var coords: DoubleArray

    /**
     * Builds a new packed coordinate sequence
     *
     * @param coords  an array of `double` values that contains the ordinate values of the sequence
     * @param dimension the total number of ordinates that make up a [Coordinate] in this sequence.
     * @param measures the number of measure-ordinates each [Coordinate] in this sequence has.
     */
    constructor(coords: DoubleArray, dimension: Int, measures: Int) : super(dimension, measures) {
      if (coords.size % dimension != 0) {
        throw IllegalArgumentException("Packed array does not contain " +
            "an integral number of coordinates")
      }
      this.coords = coords
    }

    /**
     * Builds a new packed coordinate sequence out of a float coordinate array
     *
     * @param coords  an array of `float` values that contains the ordinate values of the sequence
     * @param dimension the total number of ordinates that make up a [Coordinate] in this sequence.
     * @param measures the number of measure-ordinates each [Coordinate] in this sequence has.
     */
    constructor(coords: FloatArray, dimension: Int, measures: Int) : super(dimension, measures) {
      this.coords = DoubleArray(coords.size)
      for (i in coords.indices) {
        this.coords[i] = coords[i].toDouble()
      }
    }

    /**
     * Builds a new packed coordinate sequence out of a coordinate array
     *
     * @param coordinates an array of [Coordinate]s
     * @param dimension the total number of ordinates that make up a [Coordinate] in this sequence.
     */
    constructor(coordinates: Array<Coordinate>?, dimension: Int) :
      this(coordinates, dimension, max(0, dimension - 3))

    /**
     * Builds a new packed coordinate sequence out of a coordinate array
     *
     * @param coordinates an array of [Coordinate]s
     * @param dimension the total number of ordinates that make up a [Coordinate] in this sequence.
     * @param measures the number of measure-ordinates each [Coordinate] in this sequence has.
     */
    constructor(coordinates: Array<Coordinate>?, dimension: Int, measures: Int) : super(dimension, measures) {
      val coordinates = coordinates ?: arrayOf()

      coords = DoubleArray(coordinates.size * this.dimension)
      for (i in coordinates.indices) {
        val offset = i * dimension
        coords[offset] = coordinates[i].x
        coords[offset + 1] = coordinates[i].y
        if (dimension >= 3)
          coords[offset + 2] = coordinates[i].getOrdinate(2) // Z or M
        if (dimension >= 4)
          coords[offset + 3] = coordinates[i].getOrdinate(3) // M
      }
    }

    /**
     * Builds a new packed coordinate sequence out of a coordinate array
     *
     * @param coordinates an array of [Coordinate]s
     */
    constructor(coordinates: Array<Coordinate>?) : this(coordinates, 3, 0)

    /**
     * Builds a new empty packed coordinate sequence of a given size and dimension
     *
     * @param size the number of coordinates in this sequence
     * @param dimension the total number of ordinates that make up a [Coordinate] in this sequence.
     * @param measures the number of measure-ordinates each [Coordinate] in this sequence has.
     */
    constructor(size: Int, dimension: Int, measures: Int) : super(dimension, measures) {
      coords = DoubleArray(size * this.dimension)
    }

    /**
     * @see PackedCoordinateSequence.getCoordinate
     */
    override fun getCoordinateInternal(i: Int): Coordinate {
      val x = coords[i * dimension]
      val y = coords[i * dimension + 1]
      if (dimension == 2 && measures == 0) {
        return CoordinateXY(x, y)
      } else if (dimension == 3 && measures == 0) {
        val z = coords[i * dimension + 2]
        return Coordinate(x, y, z)
      } else if (dimension == 3 && measures == 1) {
        val m = coords[i * dimension + 2]
        return CoordinateXYM(x, y, m)
      } else if (dimension == 4) {
        val z = coords[i * dimension + 2]
        val m = coords[i * dimension + 3]
        return CoordinateXYZM(x, y, z, m)
      }
      return Coordinate(x, y)
    }

    /**
     * Gets the underlying array containing the coordinate values.
     *
     * @return the array of coordinate values
     */
    fun getRawCoordinates(): DoubleArray {
      return coords
    }

    /**
     * @see CoordinateSequence.size
     */
    override fun size(): Int {
      return coords.size / dimension
    }

    /**
     * @see PackedCoordinateSequence.clone
     */
    @Deprecated("")
    public override fun clone(): Any {
      return copy()
    }

    /**
     * @see PackedCoordinateSequence.size
     */
    override fun copy(): Double {
      val clone = coords.copyOf(coords.size)
      return Double(clone, dimension, measures)
    }

    /**
     * @see PackedCoordinateSequence.getOrdinate
     *      Beware, for performance reasons the ordinate index is not checked, if
     *      it's over dimensions you may not get an exception but a meaningless
     *      value.
     */
    override fun getOrdinate(index: Int, ordinate: Int): kotlin.Double {
      return coords[index * dimension + ordinate]
    }

    /**
     * @see PackedCoordinateSequence.setOrdinate
     */
    override fun setOrdinate(index: Int, ordinate: Int, value: kotlin.Double) {
      coordRef = null
      coords[index * dimension + ordinate] = value
    }

    /**
     * @see CoordinateSequence.expandEnvelope
     */
    override fun expandEnvelope(env: Envelope): Envelope {
      var i = 0
      while (i < coords.size) {
        // added to make static code analysis happy
        if (i + 1 < coords.size) {
          env.expandToInclude(coords[i], coords[i + 1])
        }
        i += dimension
      }
      return env
    }

    companion object {
    }
  }

  /**
   * Packed coordinate sequence implementation based on floats
   */
  class Float : PackedCoordinateSequence {
    /**
     * The packed coordinate array
     */
    @JvmField
    var coords: FloatArray

    /**
     * Constructs a packed coordinate sequence from an array of `float`s
     *
     * @param coords  an array of `float` values that contains the ordinate values of the sequence
     * @param dimension the total number of ordinates that make up a [Coordinate] in this sequence.
     * @param measures the number of measure-ordinates each [Coordinate] in this sequence has.
     */
    constructor(coords: FloatArray, dimension: Int, measures: Int) : super(dimension, measures) {
      if (coords.size % dimension != 0) {
        throw IllegalArgumentException("Packed array does not contain " +
            "an integral number of coordinates")
      }
      this.coords = coords
    }

    /**
     * Constructs a packed coordinate sequence from an array of `double`s
     *
     * @param coords  an array of `double` values that contains the ordinate values of the sequence
     * @param dimension the total number of ordinates that make up a [Coordinate] in this sequence.
     * @param measures the number of measure-ordinates each [Coordinate] in this sequence has.
     */
    constructor(coords: DoubleArray, dimension: Int, measures: Int) : super(dimension, measures) {
      this.coords = FloatArray(coords.size)

      for (i in coords.indices) {
        this.coords[i] = coords[i].toFloat()
      }
    }

    /**
     * Builds a new packed coordinate sequence out of a coordinate array
     *
     * @param coordinates an array of [Coordinate]s
     * @param dimension the total number of ordinates that make up a [Coordinate] in this sequence.
     */
    constructor(coordinates: Array<Coordinate>?, dimension: Int) :
      this(coordinates, dimension, max(0, dimension - 3))

    /**
     * Constructs a packed coordinate sequence out of a coordinate array
     *
     * @param coordinates an array of [Coordinate]s
     * @param dimension the total number of ordinates that make up a [Coordinate] in this sequence.
     * @param measures the number of measure-ordinates each [Coordinate] in this sequence has.
     */
    constructor(coordinates: Array<Coordinate>?, dimension: Int, measures: Int) : super(dimension, measures) {
      val coordinates = coordinates ?: arrayOf()

      coords = FloatArray(coordinates.size * dimension)
      for (i in coordinates.indices) {
        val offset = i * dimension
        coords[offset] = coordinates[i].x.toFloat()
        coords[offset + 1] = coordinates[i].y.toFloat()
        if (dimension >= 3)
          coords[offset + 2] = coordinates[i].getOrdinate(2).toFloat() // Z or M
        if (dimension >= 4)
          coords[offset + 3] = coordinates[i].getOrdinate(3).toFloat() // M
      }
    }

    /**
     * Constructs an empty packed coordinate sequence of a given size and dimension
     *
     * @param size the number of coordinates in this sequence
     * @param dimension the total number of ordinates that make up a [Coordinate] in this sequence.
     * @param measures the number of measure-ordinates each [Coordinate] in this sequence has.
     */
    constructor(size: Int, dimension: Int, measures: Int) : super(dimension, measures) {
      coords = FloatArray(size * this.dimension)
    }

    /**
     * @see PackedCoordinateSequence.getCoordinate
     */
    override fun getCoordinateInternal(i: Int): Coordinate {
      val x = coords[i * dimension].toDouble()
      val y = coords[i * dimension + 1].toDouble()
      if (dimension == 2 && measures == 0) {
        return CoordinateXY(x, y)
      } else if (dimension == 3 && measures == 0) {
        val z = coords[i * dimension + 2].toDouble()
        return Coordinate(x, y, z)
      } else if (dimension == 3 && measures == 1) {
        val m = coords[i * dimension + 2].toDouble()
        return CoordinateXYM(x, y, m)
      } else if (dimension == 4) {
        val z = coords[i * dimension + 2].toDouble()
        val m = coords[i * dimension + 3].toFloat()
        return CoordinateXYZM(x, y, z, m.toDouble())
      }
      return Coordinate(x, y)
    }

    /**
     * Gets the underlying array containing the coordinate values.
     *
     * @return the array of coordinate values
     */
    fun getRawCoordinates(): FloatArray {
      return coords
    }

    /**
     * @see CoordinateSequence.size
     */
    override fun size(): Int {
      return coords.size / dimension
    }

    /**
     * @see PackedCoordinateSequence.clone
     */
    @Deprecated("")
    public override fun clone(): Any {
      return copy()
    }

    /**
     * @see PackedCoordinateSequence.copy
     */
    override fun copy(): Float {
      val clone = coords.copyOf(coords.size)
      return Float(clone, dimension, measures)
    }

    /**
     * @see PackedCoordinateSequence.getOrdinate
     *      For performance reasons the ordinate index is not checked.
     *      If it is larger than the dimension a meaningless
     *      value may be returned.
     */
    override fun getOrdinate(index: Int, ordinate: Int): kotlin.Double {
      return coords[index * dimension + ordinate].toDouble()
    }

    /**
     * @see PackedCoordinateSequence.setOrdinate
     */
    override fun setOrdinate(index: Int, ordinate: Int, value: kotlin.Double) {
      coordRef = null
      coords[index * dimension + ordinate] = value.toFloat()
    }

    /**
     * @see CoordinateSequence.expandEnvelope
     */
    override fun expandEnvelope(env: Envelope): Envelope {
      var i = 0
      while (i < coords.size) {
        // added to make static code analysis happy
        if (i + 1 < coords.size) {
          env.expandToInclude(coords[i].toDouble(), coords[i + 1].toDouble())
        }
        i += dimension
      }
      return env
    }

    companion object {
    }
  }

  companion object {
  }
}
