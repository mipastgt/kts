/*
 * Copyright (c) 2016 Martin Davis.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.locationtech.jts.math

import kotlin.jvm.JvmStatic

import org.locationtech.jts.geom.Coordinate
import kotlin.math.sqrt

/**
 * Represents a vector in 3-dimensional Cartesian space.
 *
 * @author mdavis
 *
 */
class Vector3D
/**
 * Creates a vector with the givne components.
 *
 * @param x the X component
 * @param y the Y component
 * @param z the Z component
 */
(private val x: Double, private val y: Double, private val z: Double) {

  /**
   * Creates a new 3D vector from a [Coordinate]. The coordinate should have
   * the X,Y and Z ordinates specified.
   *
   * @param v the Coordinate to copy
   */
  constructor(v: Coordinate) : this(v.x, v.y, v.getZ())

  /**
   * Creates a new vector with the direction and magnitude
   * of the difference between the
   * `to` and `from` [Coordinate]s.
   *
   * @param from the origin Coordinate
   * @param to the destination Coordinate
   */
  constructor(from: Coordinate, to: Coordinate) : this(to.x - from.x, to.y - from.y, to.getZ() - from.getZ())

  /**
   * Gets the X component of this vector.
   *
   * @return the value of the X component
   */
  fun getX(): Double {
    return x
  }

  /**
   * Gets the Y component of this vector.
   *
   * @return the value of the Y component
   */
  fun getY(): Double {
    return y
  }

  /**
   * Gets the Z component of this vector.
   *
   * @return the value of the Z component
   */
  fun getZ(): Double {
    return z
  }

  /**
   * Computes a vector which is the sum
   * of this vector and the given vector.
   *
   * @param v the vector to add
   * @return the sum of this and `v`
   */
  fun add(v: Vector3D): Vector3D {
    return create(x + v.x, y + v.y, z + v.z)
  }

  /**
   * Computes a vector which is the difference
   * of this vector and the given vector.
   *
   * @param v the vector to subtract
   * @return the difference of this and `v`
   */
  fun subtract(v: Vector3D): Vector3D {
    return create(x - v.x, y - v.y, z - v.z)
  }

  /**
   * Creates a new vector which has the same direction
   * and with length equals to the length of this vector
   * divided by the scalar value `d`.
   *
   * @param d the scalar divisor
   * @return a new vector with divided length
   */
  fun divide(d: Double): Vector3D {
    return create(x / d, y / d, z / d)
  }

  /**
   * Computes the dot-product of two vectors
   *
   * @param v a vector
   * @return the dot product of the vectors
   */
  fun dot(v: Vector3D): Double {
    return x * v.x + y * v.y + z * v.z
  }

  /**
   * Computes the length of this vector.
   *
   * @return the length of the vector
   */
  fun length(): Double {
    return sqrt(x * x + y * y + z * z)
  }

  /**
   * Computes a vector having identical direction
   * but normalized to have length 1.
   *
   * @return a new normalized vector
   */
  fun normalize(): Vector3D {
    val length = length()
    if (length > 0.0)
      return divide(length())
    return create(0.0, 0.0, 0.0)
  }

  /**
   * Gets a string representation of this vector
   *
   * @return a string representing this vector
   */
  override fun toString(): String {
    return "[$x, $y, $z]"
  }

  /**
   * Tests if a vector `o` has the same values for the components.
   *
   * @param o a `Vector3D` with which to do the comparison.
   * @return true if `other` is a `Vector3D` with the same values
   *         for the x and y components.
   */
  override fun equals(o: Any?): Boolean {
    if (o !is Vector3D) {
      return false
    }
    val v = o
    return x == v.x && y == v.y && z == v.z
  }

  /**
   * Gets a hashcode for this vector.
   *
   * @return a hashcode for this vector
   */
  override fun hashCode(): Int {
    // Algorithm from Effective Java by Joshua Bloch
    var result = 17
    result = 37 * result + Coordinate.hashCode(x)
    result = 37 * result + Coordinate.hashCode(y)
    result = 37 * result + Coordinate.hashCode(z)
    return result
  }

  companion object {
    /**
     * Computes the dot product of the 3D vectors AB and CD.
     *
     * @param A the start point of the first vector
     * @param B the end point of the first vector
     * @param C the start point of the second vector
     * @param D the end point of the second vector
     * @return the dot product
     */
    @JvmStatic
    fun dot(A: Coordinate, B: Coordinate, C: Coordinate, D: Coordinate): Double {
      val ABx = B.x - A.x
      val ABy = B.y - A.y
      val ABz = B.getZ() - A.getZ()
      val CDx = D.x - C.x
      val CDy = D.y - C.y
      val CDz = D.getZ() - C.getZ()
      return ABx * CDx + ABy * CDy + ABz * CDz
    }

    /**
     * Creates a new vector with given X, Y and Z components.
     *
     * @param x the X component
     * @param y the Y component
     * @param z the Z component
     * @return a new vector
     */
    @JvmStatic
    fun create(x: Double, y: Double, z: Double): Vector3D {
      return Vector3D(x, y, z)
    }

    /**
     * Creates a vector from a 3D [Coordinate].
     * The coordinate should have the
     * X,Y and Z ordinates specified.
     *
     * @param coord the Coordinate to copy
     * @return a new vector
     */
    @JvmStatic
    fun create(coord: Coordinate): Vector3D {
      return Vector3D(coord)
    }

    /**
     * Computes the 3D dot-product of two [Coordinate]s.
     *
     * @param v1 the first vector
     * @param v2 the second vector
     * @return the dot product of the vectors
     */
    @JvmStatic
    fun dot(v1: Coordinate, v2: Coordinate): Double {
      return v1.x * v2.x + v1.y * v2.y + v1.getZ() * v2.getZ()
    }

    /**
     * Computes the length of a vector.
     *
     * @param v a coordinate representing a 3D vector
     * @return the length of the vector
     */
    @JvmStatic
    fun length(v: Coordinate): Double {
      return sqrt(v.x * v.x + v.y * v.y + v.getZ() * v.getZ())
    }

    /**
     * Computes a vector having identical direction
     * but normalized to have length 1.
     *
     * @param v a coordinate representing a 3D vector
     * @return a coordinate representing the normalized vector
     */
    @JvmStatic
    fun normalize(v: Coordinate): Coordinate {
      val len = length(v)
      return Coordinate(v.x / len, v.y / len, v.getZ() / len)
    }
  }
}
