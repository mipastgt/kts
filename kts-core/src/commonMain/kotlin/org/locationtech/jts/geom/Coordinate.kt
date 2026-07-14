/*
 * Copyright (c) 2018 Vivid Solutions
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
import kotlin.math.hypot
import kotlin.math.sqrt

import org.locationtech.jts.util.Assert
import org.locationtech.jts.util.NumberUtil

/**
 * A lightweight class used to store coordinates on the 2-dimensional Cartesian plane.
 * 
 * It is distinct from [Point], which is a subclass of [Geometry].
 * Unlike objects of type [Point] (which contain additional
 * information such as an envelope, a precision model, and spatial reference
 * system information), a `Coordinate` only contains ordinate values
 * and accessor methods. 
 * 
 * `Coordinate`s are two-dimensional points, with an additional Z-ordinate.
 * If an Z-ordinate value is not specified or not defined,
 * constructed coordinates have a Z-ordinate of `NaN`
 * (which is also the value of `NULL_ORDINATE`).
 * The standard comparison functions ignore the Z-ordinate.
 * Apart from the basic accessor functions, JTS supports
 * only specific operations involving the Z-ordinate.
 * 
 * Implementations may optionally support Z-ordinate and M-measure values
 * as appropriate for a [CoordinateSequence].
 * Use of [getZ] and [getM]
 * accessors, or [getOrdinate] are recommended.
 *
 */
open class Coordinate
/**
 *  Constructs a `Coordinate` at (x,y,z).
 *
 * @param  x  the x-ordinate
 * @param  y  the y-ordinate
 * @param  z  the z-ordinate
 */
(
  /**
   * The x-ordinate.
   */
  @JvmField var x: Double,
  /**
   * The y-ordinate.
   */
  @JvmField var y: Double,
  /**
   * The z-ordinate.
   * 
   * Direct access to this field is discouraged; use [getZ].
   */
  @JvmField var z: Double
) : Comparable<Coordinate> {

  /**
   *  Constructs a `Coordinate` at (0,0,NaN).
   */
  constructor() : this(0.0, 0.0)

  /**
   *  Constructs a `Coordinate` having the same (x,y,z) values as
   *  `other`.
   *
   * @param  c  the `Coordinate` to copy.
   */
  constructor(c: Coordinate) : this(c.x, c.y, c.getZ())

  /**
   *  Constructs a `Coordinate` at (x,y,NaN).
   *
   * @param  x  the x-value
   * @param  y  the y-value
   */
  constructor(x: Double, y: Double) : this(x, y, NULL_ORDINATE)

  /**
   *  Sets this `Coordinate`s (x,y,z) values to that of `other`.
   *
   * @param  other  the `Coordinate` to copy
   */
  open fun setCoordinate(other: Coordinate) {
    x = other.x
    y = other.y
    z = other.getZ()
  }

  /**
   *  Retrieves the value of the X ordinate.
   *
   *  @return the value of the X ordinate
   */
  open fun getX(): Double {
    return x
  }

  /**
   * Sets the X ordinate value.
   *
   * @param x the value to set as X
   */
  open fun setX(x: Double) {
    this.x = x
  }

  /**
   *  Retrieves the value of the Y ordinate.
   *
   *  @return the value of the Y ordinate
   */
  open fun getY(): Double {
    return y
  }

  /**
   * Sets the Y ordinate value.
   *
   * @param y the value to set as Y
   */
  open fun setY(y: Double) {
    this.y = y
  }

  /**
   *  Retrieves the value of the Z ordinate, if present.
   *  If no Z value is present returns `NaN`.
   *
   *  @return the value of the Z ordinate, or `NaN`
   */
  open fun getZ(): Double {
    return z
  }

  /**
   * Sets the Z ordinate value.
   *
   * @param z the value to set as Z
   */
  open fun setZ(z: Double) {
    this.z = z
  }

  /**
   *  Retrieves the value of the measure, if present.
   *  If no measure value is present returns `NaN`.
   *
   *  @return the value of the measure, or `NaN`
   */
  open fun getM(): Double {
    return Double.NaN
  }

  /**
   * Sets the measure value, if supported.
   *
   * @param m the value to set as M
   */
  open fun setM(m: Double) {
    throw IllegalArgumentException("Invalid ordinate index: " + M)
  }

  /**
   * Gets the ordinate value for the given index.
   *
   * The base implementation supports values for the index are
   * [X], [Y], and [Z].
   *
   * @param ordinateIndex the ordinate index
   * @return the value of the ordinate
   * @throws IllegalArgumentException if the index is not valid
   */
  open fun getOrdinate(ordinateIndex: Int): Double {
    when (ordinateIndex) {
      X -> return x
      Y -> return y
      Z -> return getZ() // sure to delegate to subclass rather than offer direct field access
    }
    throw IllegalArgumentException("Invalid ordinate index: " + ordinateIndex)
  }

  /**
   * Sets the ordinate for the given index
   * to a given value.
   *
   * The base implementation supported values for the index are
   * [X], [Y], and [Z].
   *
   * @param ordinateIndex the ordinate index
   * @param value the value to set
   * @throws IllegalArgumentException if the index is not valid
   */
  open fun setOrdinate(ordinateIndex: Int, value: Double) {
    when (ordinateIndex) {
      X -> x = value
      Y -> y = value
      Z -> setZ(value) // delegate to subclass rather than offer direct field access
      else -> throw IllegalArgumentException("Invalid ordinate index: " + ordinateIndex)
    }
  }

  /**
   * Tests if the coordinate has valid X and Y ordinate values.
   * An ordinate value is valid iff it is finite.
   *
   * @return true if the coordinate is valid
   * @see Double#isFinite(double)
   */
  open fun isValid(): Boolean {
    if (!x.isFinite()) return false
    if (!y.isFinite()) return false
    return true
  }

  /**
   *  Returns whether the planar projections of the two `Coordinate`s
   *  are equal.
   *
   * @param  other  a `Coordinate` with which to do the 2D comparison.
   * @return        `true` if the x- and y-coordinates are equal; the
   *      z-coordinates do not have to be equal.
   */
  open fun equals2D(other: Coordinate): Boolean {
    if (x != other.x) {
      return false
    }
    if (y != other.y) {
      return false
    }
    return true
  }

  /**
   * Tests if another Coordinate has the same values for the X and Y ordinates,
   * within a specified tolerance value.
   * The Z ordinate is ignored.
   *
   * @param c a `Coordinate` with which to do the 2D comparison.
   * @param tolerance the tolerance value to use
   * @return true if `other` is a `Coordinate`
   *      with the same values for X and Y.
   */
  open fun equals2D(c: Coordinate, tolerance: Double): Boolean {
    if (!NumberUtil.equalsWithTolerance(this.x, c.x, tolerance)) {
      return false
    }
    if (!NumberUtil.equalsWithTolerance(this.y, c.y, tolerance)) {
      return false
    }
    return true
  }

  /**
   * Tests if another coordinate has the same values for the X, Y and Z ordinates.
   *
   * @param other a `Coordinate` with which to do the 3D comparison.
   * @return true if `other` is a `Coordinate`
   *      with the same values for X, Y and Z.
   */
  open fun equals3D(other: Coordinate): Boolean {
    return (x == other.x) && (y == other.y) &&
      ((getZ() == other.getZ()) ||
        (getZ().isNaN() && other.getZ().isNaN()))
  }

  /**
   * Tests if another coordinate has the same value for Z, within a tolerance.
   *
   * @param c a coordinate
   * @param tolerance the tolerance value
   * @return true if the Z ordinates are within the given tolerance
   */
  open fun equalInZ(c: Coordinate, tolerance: Double): Boolean {
    return NumberUtil.equalsWithTolerance(this.getZ(), c.getZ(), tolerance)
  }

  /**
   *  Returns `true` if `other` has the same values for
   *  the x and y ordinates.
   *  Since Coordinates are 2.5D, this routine ignores the z value when making the comparison.
   *
   * @param  other  a `Coordinate` with which to do the comparison.
   * @return        `true` if `other` is a `Coordinate`
   *      with the same values for the x and y ordinates.
   */
  override fun equals(other: Any?): Boolean {
    if (other !is Coordinate) {
      return false
    }
    return equals2D(other)
  }

  /**
   *  Compares this [Coordinate] with the specified [Coordinate] for order.
   *  This method ignores the z value when making the comparison.
   *  Returns:
   *  <UL>
   *    <LI> -1 : this.x < other.x || ((this.x == other.x) && (this.y < other.y))
   *    <LI> 0 : this.x == other.x && this.y = other.y
   *    <LI> 1 : this.x > other.x || ((this.x == other.x) && (this.y > other.y))
   *
   *  </UL>
   *  Note: This method assumes that ordinate values
   * are valid numbers.  NaN values are not handled correctly.
   *
   * @param  o  the `Coordinate` with which this `Coordinate`
   *      is being compared
   * @return    -1, zero, or 1 as this `Coordinate`
   *      is less than, equal to, or greater than the specified `Coordinate`
   */
  override fun compareTo(o: Coordinate): Int {
    val other = o

    if (x < other.x) return -1
    if (x > other.x) return 1
    if (y < other.y) return -1
    if (y > other.y) return 1
    return 0
  }

  /**
   *  Returns a `String` of the form <I>(x,y,z)</I> .
   *
   * @return    a `String` of the form <I>(x,y,z)</I>
   */
  override fun toString(): String {
    return "(" + x + ", " + y + ", " + getZ() + ")"
  }

  open fun clone(): Any {
    return copy()
  }

  /**
   * Creates a copy of this Coordinate.
   *
   * @return a copy of this coordinate.
   */
  open fun copy(): Coordinate {
    return Coordinate(this)
  }

  /**
   * Create a new Coordinate of the same type as this Coordinate, but with no values.
   *
   * @return a new Coordinate
   */
  open fun create(): Coordinate {
    return Coordinate()
  }

  /**
   * Computes the 2-dimensional Euclidean distance to another location.
   * The Z-ordinate is ignored.
   *
   * @param c a point
   * @return the 2-dimensional Euclidean distance between the locations
   */
  open fun distance(c: Coordinate): Double {
    val dx = x - c.x
    val dy = y - c.y
    return hypot(dx, dy)
  }

  /**
   * Computes the 3-dimensional Euclidean distance to another location.
   *
   * @param c a coordinate
   * @return the 3-dimensional Euclidean distance between the locations
   */
  open fun distance3D(c: Coordinate): Double {
    val dx = x - c.x
    val dy = y - c.y
    val dz = getZ() - c.getZ()
    return sqrt(dx * dx + dy * dy + dz * dz)
  }

  /**
   * Gets a hashcode for this coordinate.
   *
   * @return a hashcode for this coordinate
   */
  override fun hashCode(): Int {
    //Algorithm from Effective Java by Joshua Bloch [Jon Aquino]
    var result = 17
    result = 37 * result + hashCode(x)
    result = 37 * result + hashCode(y)
    return result
  }

  /**
   * Compares two [Coordinate]s, allowing for either a 2-dimensional
   * or 3-dimensional comparison, and handling NaN values correctly.
   */
  class DimensionalComparator : Comparator<Coordinate> {

    private var dimensionsToTest = 2

    /**
     * Creates a comparator for 2 dimensional coordinates.
     */
    constructor() : this(2)

    /**
     * Creates a comparator for 2 or 3 dimensional coordinates, depending
     * on the value provided.
     *
     * @param dimensionsToTest the number of dimensions to test
     */
    constructor(dimensionsToTest: Int) {
      if (dimensionsToTest != 2 && dimensionsToTest != 3)
        throw IllegalArgumentException("only 2 or 3 dimensions may be specified")
      this.dimensionsToTest = dimensionsToTest
    }

    /**
     * Compares two [Coordinate]s along to the number of
     * dimensions specified.
     *
     * @param c1 a [Coordinate]
     * @param c2 a {link Coordinate}
     * @return -1, 0, or 1 depending on whether o1 is less than,
     * equal to, or greater than 02
     *
     */
    override fun compare(c1: Coordinate, c2: Coordinate): Int {
      val compX = compare(c1.x, c2.x)
      if (compX != 0) return compX

      val compY = compare(c1.y, c2.y)
      if (compY != 0) return compY

      if (dimensionsToTest <= 2) return 0

      val compZ = compare(c1.getZ(), c2.getZ())
      return compZ
    }

    companion object {
      /**
       * Compare two `double`s, allowing for NaN values.
       * NaN is treated as being less than any valid number.
       *
       * @param a a `double`
       * @param b a `double`
       * @return -1, 0, or 1 depending on whether a is less than, equal to or greater than b
       */
      @JvmStatic
      fun compare(a: Double, b: Double): Int {
        if (a < b) return -1
        if (a > b) return 1

        if (a.isNaN()) {
          if (b.isNaN()) return 0
          return -1
        }

        if (b.isNaN()) return 1
        return 0
      }
    }
  }

  companion object {

    /**
     * The value used to indicate a null or missing ordinate value.
     * In particular, used for the value of ordinates for dimensions
     * greater than the defined dimension of a coordinate.
     */
    @JvmField
    val NULL_ORDINATE = Double.NaN

    /** Standard ordinate index value for, where X is 0 */
    const val X = 0

    /** Standard ordinate index value for, where Y is 1 */
    const val Y = 1

    /**
     * Standard ordinate index value for, where Z is 2.
     *
     * This constant assumes XYZM coordinate sequence definition, please check this assumption
     * using [CoordinateSequence.getDimension] and [CoordinateSequence.getMeasures]
     * before use.
     */
    const val Z = 2

    /**
     * Standard ordinate index value for, where M is 3.
     *
     * This constant assumes XYZM coordinate sequence definition, please check this assumption
     * using [CoordinateSequence.getDimension] and [CoordinateSequence.getMeasures]
     * before use.
     */
    const val M = 3

    /**
     * Computes a hash code for a double value, using the algorithm from
     * Joshua Bloch's book <i>Effective Java"</i>
     *
     * @param x the value to compute for
     * @return a hashcode for x
     */
    @JvmStatic
    fun hashCode(x: Double): Int {
      val f = x.toBits()
      return (f xor (f ushr 32)).toInt()
    }
  }
}
