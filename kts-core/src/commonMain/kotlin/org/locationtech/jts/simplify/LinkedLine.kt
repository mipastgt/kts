/*
 * Copyright (c) 2022 Martin Davis.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.locationtech.jts.simplify

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.CoordinateArrays
import org.locationtech.jts.geom.CoordinateList
import org.locationtech.jts.io.WKTWriter

class LinkedLine(private val coord: Array<Coordinate>) {

  private val ring: Boolean = CoordinateArrays.isRing(coord)
  private var size: Int = if (ring) coord.size - 1 else coord.size
  private val next: IntArray = createNextLinks(size)
  private val prev: IntArray = createPrevLinks(size)

  fun isRing(): Boolean {
    return ring
  }

  fun isCorner(i: Int): Boolean {
    if (!isRing()
        && (i == 0 || i == coord.size - 1))
      return false
    return true
  }

  private fun createNextLinks(size: Int): IntArray {
    val next = IntArray(size)
    for (i in 0 until size) {
      next[i] = i + 1
    }
    next[size - 1] = if (ring) 0 else NO_COORD_INDEX
    return next
  }

  private fun createPrevLinks(size: Int): IntArray {
    val prev = IntArray(size)
    for (i in 0 until size) {
      prev[i] = i - 1
    }
    prev[0] = if (ring) size - 1 else NO_COORD_INDEX
    return prev
  }

  fun size(): Int {
    return size
  }

  fun next(i: Int): Int {
    return next[i]
  }

  fun prev(i: Int): Int {
    return prev[i]
  }

  fun getCoordinate(index: Int): Coordinate {
    return coord[index]
  }

  fun prevCoordinate(index: Int): Coordinate {
    return coord[prev(index)]
  }

  fun nextCoordinate(index: Int): Coordinate {
    return coord[next(index)]
  }

  fun hasCoordinate(index: Int): Boolean {
    //-- if not a ring, endpoints are alway present
    if (!ring && (index == 0 || index == coord.size - 1))
      return true
    return index >= 0
        && index < prev.size
        && prev[index] != NO_COORD_INDEX
  }

  fun remove(index: Int) {
    val iprev = prev[index]
    val inext = next[index]
    if (iprev != NO_COORD_INDEX) next[iprev] = inext
    if (inext != NO_COORD_INDEX) prev[inext] = iprev
    prev[index] = NO_COORD_INDEX
    next[index] = NO_COORD_INDEX
    size--
  }

  fun getCoordinates(): Array<Coordinate> {
    val coords = CoordinateList()
    val len = if (ring) coord.size - 1 else coord.size
    for (i in 0 until len) {
      if (hasCoordinate(i)) {
        coords.add(coord[i].copy(), false)
      }
    }
    if (ring) {
      coords.closeRing()
    }
    return coords.toCoordinateArray()
  }

  override fun toString(): String {
    return WKTWriter.toLineString(getCoordinates())
  }

  companion object {
    private const val NO_COORD_INDEX = -1
  }
}
