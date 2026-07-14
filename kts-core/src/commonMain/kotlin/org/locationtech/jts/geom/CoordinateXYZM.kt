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
 * Coordinate subclass supporting XYZM ordinates.
 * 
 * This data object is suitable for use with coordinate sequences with `dimension` = 4 and `measures` = 1.
 *
 * @since 1.16
 */
open class CoordinateXYZM : Coordinate {

  /** The m-measure. */
  private var m: Double = 0.0

  /** Default constructor */
  constructor() : super() {
    this.m = 0.0
  }

  /**
   * Constructs a CoordinateXYZM instance with the given ordinates and measure.
   *
   * @param x the X ordinate
   * @param y the Y ordinate
   * @param z the Z ordinate
   * @param m the M measure value
   */
  constructor(x: Double, y: Double, z: Double, m: Double) : super(x, y, z) {
    this.m = m
  }

  /**
   * Constructs a CoordinateXYZM instance with the ordinates of the given Coordinate.
   *
   * @param coord the coordinate providing the ordinates
   */
  constructor(coord: Coordinate) : super(coord) {
    m = getM()
  }

  /**
   * Constructs a CoordinateXYZM instance with the ordinates of the given CoordinateXYZM.
   *
   * @param coord the coordinate providing the ordinates
   */
  constructor(coord: CoordinateXYZM) : super(coord) {
    m = coord.m
  }

  /**
   * Creates a copy of this CoordinateXYZM.
   *
   * @return a copy of this CoordinateXYZM
   */
  override fun copy(): CoordinateXYZM {
    return CoordinateXYZM(this)
  }

  /**
   * Create a new Coordinate of the same type as this Coordinate, but with no values.
   *
   * @return a new Coordinate
   */
  override fun create(): Coordinate {
    return CoordinateXYZM()
  }

  /** The m-measure, if available. */
  override fun getM(): Double {
    return m
  }

  override fun setM(m: Double) {
    this.m = m
  }

  override fun getOrdinate(ordinateIndex: Int): Double {
    when (ordinateIndex) {
      X -> return x
      Y -> return y
      Z -> return getZ() // sure to delegate to subclass rather than offer direct field access
      M -> return getM() // sure to delegate to subclass rather than offer direct field access
    }
    throw IllegalArgumentException("Invalid ordinate index: " + ordinateIndex)
  }

  override fun setCoordinate(other: Coordinate) {
    x = other.x
    y = other.y
    z = other.getZ()
    m = other.getM()
  }

  override fun setOrdinate(ordinateIndex: Int, value: Double) {
    when (ordinateIndex) {
      X -> x = value
      Y -> y = value
      Z -> z = value
      M -> m = value
      else -> throw IllegalArgumentException("Invalid ordinate index: " + ordinateIndex)
    }
  }

  override fun toString(): String {
    return "(" + x + ", " + y + ", " + getZ() + " m=" + getM() + ")"
  }

  companion object {
  }
}
