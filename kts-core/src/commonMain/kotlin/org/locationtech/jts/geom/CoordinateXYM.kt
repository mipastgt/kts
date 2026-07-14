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

import kotlin.jvm.JvmField

/**
 * Coordinate subclass supporting XYM ordinates.
 * 
 * This data object is suitable for use with coordinate sequences with `dimension` = 3 and `measures` = 1.
 * 
 * The [Coordinate.z] field is visible, but intended to be ignored.
 *
 * @since 1.16
 */
open class CoordinateXYM : Coordinate {

  /** The m-measure. */
  @JvmField
  protected var m: Double = 0.0

  /** Default constructor */
  constructor() : super() {
    this.m = 0.0
  }

  /**
   * Constructs a CoordinateXYM instance with the given ordinates and measure.
   *
   * @param x the X ordinate
   * @param y the Y ordinate
   * @param m the M measure value
   */
  constructor(x: Double, y: Double, m: Double) : super(x, y, Coordinate.NULL_ORDINATE) {
    this.m = m
  }

  /**
   * Constructs a CoordinateXYM instance with the x and y ordinates of the given Coordinate.
   *
   * @param coord the coordinate providing the ordinates
   */
  constructor(coord: Coordinate) : super(coord.x, coord.y) {
    m = getM()
  }

  /**
   * Constructs a CoordinateXY instance with the x and y ordinates of the given CoordinateXYM.
   *
   * @param coord the coordinate providing the ordinates
   */
  constructor(coord: CoordinateXYM) : super(coord.x, coord.y) {
    m = coord.m
  }

  /**
   * Creates a copy of this CoordinateXYM.
   *
   * @return a copy of this CoordinateXYM
   */
  override fun copy(): CoordinateXYM {
    return CoordinateXYM(this)
  }

  /**
   * Create a new Coordinate of the same type as this Coordinate, but with no values.
   *
   * @return a new Coordinate
   */
  override fun create(): Coordinate {
    return CoordinateXYM()
  }

  /** The m-measure, if available. */
  override fun getM(): Double {
    return m
  }

  override fun setM(m: Double) {
    this.m = m
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
    m = other.getM()
  }

  override fun getOrdinate(ordinateIndex: Int): Double {
    when (ordinateIndex) {
      X -> return x
      Y -> return y
      M -> return m
    }
    throw IllegalArgumentException("Invalid ordinate index: " + ordinateIndex)
  }

  override fun setOrdinate(ordinateIndex: Int, value: Double) {
    when (ordinateIndex) {
      X -> x = value
      Y -> y = value
      M -> m = value
      else -> throw IllegalArgumentException("Invalid ordinate index: " + ordinateIndex)
    }
  }

  override fun toString(): String {
    return "(" + x + ", " + y + " m=" + getM() + ")"
  }

  companion object {

    /** Standard ordinate index value for X */
    const val X = 0

    /** Standard ordinate index value for Y */
    const val Y = 1

    /** CoordinateXYM does not support Z values. */
    const val Z = -1

    /**
     * Standard ordinate index value for M in XYM sequences.
     *
     * This constant assumes XYM coordinate sequence definition.  Check this assumption using
     * [CoordinateSequence.getDimension] and [CoordinateSequence.getMeasures] before use.
     */
    const val M = 2
  }
}
