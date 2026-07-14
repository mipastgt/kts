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
package org.locationtech.jts.algorithm

import kotlin.jvm.JvmStatic
import kotlin.jvm.JvmField

import org.locationtech.jts.geom.Coordinate

/**
 * Represents a homogeneous coordinate in a 2-D coordinate space.
 * In JTS [HCoordinate]s are used as a clean way
 * of computing intersections between line segments.
 *
 * @author David Skea
 */
class HCoordinate {

  @JvmField
  var x = 0.0

  @JvmField
  var y = 0.0

  @JvmField
  var w = 0.0

  constructor() {
    x = 0.0
    y = 0.0
    w = 1.0
  }

  constructor(x: Double, y: Double, w: Double) {
    this.x = x
    this.y = y
    this.w = w
  }

  constructor(x: Double, y: Double) {
    this.x = x
    this.y = y
    w = 1.0
  }

  constructor(p: Coordinate) {
    x = p.x
    y = p.y
    w = 1.0
  }

  constructor(p1: HCoordinate, p2: HCoordinate) {
    x = p1.y * p2.w - p2.y * p1.w
    y = p2.x * p1.w - p1.x * p2.w
    w = p1.x * p2.y - p2.x * p1.y
  }

  /**
   * Constructs a homogeneous coordinate which is the intersection of the lines
   * define by the homogenous coordinates represented by two
   * [Coordinate]s.
   *
   * @param p1
   * @param p2
   */
  constructor(p1: Coordinate, p2: Coordinate) {
    // optimization when it is known that w = 1
    x = p1.y - p2.y
    y = p2.x - p1.x
    w = p1.x * p2.y - p2.x * p1.y
  }

  constructor(p1: Coordinate, p2: Coordinate, q1: Coordinate, q2: Coordinate) {
    // unrolled computation
    val px = p1.y - p2.y
    val py = p2.x - p1.x
    val pw = p1.x * p2.y - p2.x * p1.y

    val qx = q1.y - q2.y
    val qy = q2.x - q1.x
    val qw = q1.x * q2.y - q2.x * q1.y

    x = py * qw - qy * pw
    y = qx * pw - px * qw
    w = px * qy - qx * py
  }

  @Throws(NotRepresentableException::class)
  fun getX(): Double {
    val a = x / w
    if ((a.isNaN()) || (a.isInfinite())) {
      throw NotRepresentableException()
    }
    return a
  }

  @Throws(NotRepresentableException::class)
  fun getY(): Double {
    val a = y / w
    if ((a.isNaN()) || (a.isInfinite())) {
      throw NotRepresentableException()
    }
    return a
  }

  @Throws(NotRepresentableException::class)
  fun getCoordinate(): Coordinate {
    val p = Coordinate()
    p.x = getX()
    p.y = getY()
    return p
  }

  companion object {
    /**
     * Computes the (approximate) intersection point between two line segments
     * using homogeneous coordinates.
     *
     * @deprecated use [Intersection.intersection]
     */
    @JvmStatic
    @Throws(NotRepresentableException::class)
    fun intersection(
      p1: Coordinate, p2: Coordinate,
      q1: Coordinate, q2: Coordinate
    ): Coordinate {
      // unrolled computation
      val px = p1.y - p2.y
      val py = p2.x - p1.x
      val pw = p1.x * p2.y - p2.x * p1.y

      val qx = q1.y - q2.y
      val qy = q2.x - q1.x
      val qw = q1.x * q2.y - q2.x * q1.y

      val x = py * qw - qy * pw
      val y = qx * pw - px * qw
      val w = px * qy - qx * py

      val xInt = x / w
      val yInt = y / w

      if ((xInt.isNaN()) || (xInt.isInfinite() ||
            yInt.isNaN()) || (yInt.isInfinite())) {
        throw NotRepresentableException()
      }

      return Coordinate(xInt, yInt)
    }
  }
}
