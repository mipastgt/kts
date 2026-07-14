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

/**
 * Coordinate subclass supporting XY ordinates.
 * 
 * This data object is suitable for use with coordinate sequences with `dimension` = 2.
 * 
 * The [Coordinate.z] field is visible, but intended to be ignored.
 *
 * @since 1.16
 */
open class CoordinateXY : Coordinate {

  /** Default constructor */
  constructor() : super()

  /**
   * Constructs a CoordinateXY instance with the given ordinates.
   *
   * @param x the X ordinate
   * @param y the Y ordinate
   */
  constructor(x: Double, y: Double) : super(x, y, Coordinate.NULL_ORDINATE)

  /**
   * Constructs a CoordinateXY instance with the x and y ordinates of the given Coordinate.
   *
   * @param coord the Coordinate providing the ordinates
   */
  constructor(coord: Coordinate) : super(coord.x, coord.y)

  /**
   * Constructs a CoordinateXY instance with the x and y ordinates of the given CoordinateXY.
   *
   * @param coord the CoordinateXY providing the ordinates
   */
  constructor(coord: CoordinateXY) : super(coord.x, coord.y)

  /**
   * Creates a copy of this CoordinateXY.
   *
   * @return a copy of this CoordinateXY
   */
  override fun copy(): CoordinateXY {
    return CoordinateXY(this)
  }

  /**
   * Create a new Coordinate of the same type as this Coordinate, but with no values.
   *
   * @return a new Coordinate
   */
  override fun create(): Coordinate {
    return CoordinateXY()
  }

  /** The z-ordinate is not supported */
  override fun getZ(): Double {
    return NULL_ORDINATE
  }

  /** The z-ordinate is not supported */
  override fun setZ(z: Double) {
    throw IllegalArgumentException("CoordinateXY dimension 2 does not support z-ordinate")
  }

  override fun setCoordinate(other: Coordinate) {
    x = other.x
    y = other.y
    z = other.getZ()
  }

  override fun getOrdinate(ordinateIndex: Int): Double {
    when (ordinateIndex) {
      X -> return x
      Y -> return y
    }
    return Double.NaN
    // disable for now to avoid regression issues
    //throw new IllegalArgumentException("Invalid ordinate index: " + ordinateIndex);
  }

  override fun setOrdinate(ordinateIndex: Int, value: Double) {
    when (ordinateIndex) {
      X -> x = value
      Y -> y = value
      else -> throw IllegalArgumentException("Invalid ordinate index: " + ordinateIndex)
    }
  }

  override fun toString(): String {
    return "(" + x + ", " + y + ")"
  }

  companion object {

    /** Standard ordinate index value for X */
    const val X = 0

    /** Standard ordinate index value for Y */
    const val Y = 1

    /** CoordinateXY does not support Z values. */
    const val Z = -1

    /** CoordinateXY does not support M measures. */
    const val M = -1
  }
}
