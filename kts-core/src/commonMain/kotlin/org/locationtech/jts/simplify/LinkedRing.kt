/*
 * Copyright (c) 2021 Martin Davis.
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
import org.locationtech.jts.geom.CoordinateList

internal class LinkedRing(private val coord: Array<Coordinate>) {

  private val next: IntArray
  private val prev: IntArray
  private var size: Int = coord.size - 1

  init {
    next = createNextLinks(size)
    prev = createPrevLinks(size)
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
    return index >= 0 && index < prev.size
        && prev[index] != NO_COORD_INDEX
  }

  fun remove(index: Int) {
    val iprev = prev[index]
    val inext = next[index]
    next[iprev] = inext
    prev[inext] = iprev
    prev[index] = NO_COORD_INDEX
    next[index] = NO_COORD_INDEX
    size--
  }

  fun getCoordinates(): Array<Coordinate> {
    val coords = CoordinateList()
    for (i in 0 until coord.size - 1) {
      if (prev[i] != NO_COORD_INDEX) {
        coords.add(coord[i].copy(), false)
      }
    }
    coords.closeRing()
    return coords.toCoordinateArray()
  }

  companion object {
    private const val NO_COORD_INDEX = -1

    private fun createNextLinks(size: Int): IntArray {
      val next = IntArray(size)
      for (i in 0 until size) {
        next[i] = i + 1
      }
      next[size - 1] = 0
      return next
    }

    private fun createPrevLinks(size: Int): IntArray {
      val prev = IntArray(size)
      for (i in 0 until size) {
        prev[i] = i - 1
      }
      prev[0] = size - 1
      return prev
    }
  }
}
