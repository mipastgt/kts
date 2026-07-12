/*
 * Copyright (c) 2019 Martin Davis.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.locationtech.jts.operation.overlayng

import kotlin.jvm.JvmStatic

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.io.OrdinateFormat

/**
 * A key for sorting and comparing edges in a noded arrangement.
 * Relies on the fact that in a correctly noded arrangement
 * edges are identical (up to direction)
 * if they have their first segment in common.
 *
 * @author mdavis
 */
class EdgeKey internal constructor(edge: Edge) : Comparable<EdgeKey> {

  private var p0x = 0.0
  private var p0y = 0.0
  private var p1x = 0.0
  private var p1y = 0.0

  init {
    initPoints(edge)
  }

  private fun initPoints(edge: Edge) {
    val direction = edge.direction()
    if (direction) {
      init(edge.getCoordinate(0), edge.getCoordinate(1))
    } else {
      val len = edge.size()
      init(edge.getCoordinate(len - 1), edge.getCoordinate(len - 2))
    }
  }

  private fun init(p0: Coordinate, p1: Coordinate) {
    p0x = p0.getX()
    p0y = p0.getY()
    p1x = p1.getX()
    p1y = p1.getY()
  }

  override fun compareTo(ek: EdgeKey): Int {
    if (p0x < ek.p0x) return -1
    if (p0x > ek.p0x) return 1
    if (p0y < ek.p0y) return -1
    if (p0y > ek.p0y) return 1
    // first points are equal, compare second
    if (p1x < ek.p1x) return -1
    if (p1x > ek.p1x) return 1
    if (p1y < ek.p1y) return -1
    if (p1y > ek.p1y) return 1
    return 0
  }

  override fun equals(o: Any?): Boolean {
    if (o !is EdgeKey) {
      return false
    }
    return p0x == o.p0x &&
        p0y == o.p0y &&
        p1x == o.p1x &&
        p1y == o.p1y
  }

  /**
   * Gets a hashcode for this object.
   *
   * @return a hashcode for this object
   */
  override fun hashCode(): Int {
    //Algorithm from Effective Java by Joshua Bloch
    var result = 17
    result = 37 * result + hashCode(p0x)
    result = 37 * result + hashCode(p0y)
    result = 37 * result + hashCode(p1x)
    result = 37 * result + hashCode(p1y)
    return result
  }

  override fun toString(): String {
    return "EdgeKey(" + format(p0x, p0y) + ", " + format(p1x, p1y) + ")"
  }

  private fun format(x: Double, y: Double): String {
    return OrdinateFormat.DEFAULT.format(x) + " " + OrdinateFormat.DEFAULT.format(y)
  }

  companion object {
    @JvmStatic
    fun create(edge: Edge): EdgeKey {
      return EdgeKey(edge)
    }

    /**
     * Computes a hash code for a double value, using the algorithm from
     * Joshua Bloch's book *Effective Java"*
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
