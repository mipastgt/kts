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
 * A list of {@link Coordinate}s, which may
 * be set to prevent repeated coordinates from occurring in the list.
 *
 *
 * @version 1.7
 */
class CoordinateList private constructor(
  private val list: ArrayList<Coordinate>
) : MutableList<Coordinate> by list {

  /**
   * Constructs a new list without any coordinates
   */
  constructor() : this(ArrayList())

  /**
   * Constructs a new list from an array of Coordinates, allowing repeated points.
   * (I.e. this constructor produces a {@link CoordinateList} with exactly the same set of points
   * as the input array.)
   *
   * @param coord the initial coordinates
   */
  constructor(coord: Array<Coordinate>) : this(ArrayList(coord.size)) {
    add(coord, true)
  }

  /**
   * Constructs a new list from an array of Coordinates,
   * allowing caller to specify if repeated points are to be removed.
   *
   * @param coord the array of coordinates to load into the list
   * @param allowRepeated if <code>false</code>, repeated points are removed
   */
  constructor(coord: Array<Coordinate>, allowRepeated: Boolean) : this(ArrayList(coord.size)) {
    add(coord, allowRepeated)
  }

  fun getCoordinate(i: Int): Coordinate {
    return get(i)
  }

  /**
   * Adds a section of an array of coordinates to the list.
   * @param coord The coordinates
   * @param allowRepeated if set to false, repeated coordinates are collapsed
   * @param start the index to start from
   * @param end the index to add up to but not including
   * @return true (as by general collection contract)
   */
  fun add(coord: Array<Coordinate>, allowRepeated: Boolean, start: Int, end: Int): Boolean {
    var inc = 1
    if (start > end) inc = -1

    var i = start
    while (i != end) {
      add(coord[i], allowRepeated)
      i += inc
    }
    return true
  }

  /**
   * Adds an array of coordinates to the list.
   * @param coord The coordinates
   * @param allowRepeated if set to false, repeated coordinates are collapsed
   * @param direction if false, the array is added in reverse order
   * @return true (as by general collection contract)
   */
  fun add(coord: Array<Coordinate>, allowRepeated: Boolean, direction: Boolean): Boolean {
    if (direction) {
      for (i in 0 until coord.size) {
        add(coord[i], allowRepeated)
      }
    } else {
      for (i in coord.size - 1 downTo 0) {
        add(coord[i], allowRepeated)
      }
    }
    return true
  }

  /**
   * Adds an array of coordinates to the list.
   * @param coord The coordinates
   * @param allowRepeated if set to false, repeated coordinates are collapsed
   * @return true (as by general collection contract)
   */
  fun add(coord: Array<Coordinate>, allowRepeated: Boolean): Boolean {
    add(coord, allowRepeated, true)
    return true
  }

  /**
   * Adds a coordinate to the list.
   * @param obj The coordinate to add
   * @param allowRepeated if set to false, repeated coordinates are collapsed
   * @return true (as by general collection contract)
   */
  fun add(obj: Any?, allowRepeated: Boolean): Boolean {
    add(obj as Coordinate, allowRepeated)
    return true
  }

  /**
   * Adds a coordinate to the end of the list.
   *
   * @param coord The coordinates
   * @param allowRepeated if set to false, repeated coordinates are collapsed
   */
  fun add(coord: Coordinate, allowRepeated: Boolean) {
    // don't add duplicate coordinates
    if (!allowRepeated) {
      if (size >= 1) {
        val last = get(size - 1)
        if (last.equals2D(coord)) return
      }
    }
    list.add(coord)
  }

  /**
   * Inserts the specified coordinate at the specified position in this list.
   *
   * @param i the position at which to insert
   * @param coord the coordinate to insert
   * @param allowRepeated if set to false, repeated coordinates are collapsed
   */
  fun add(i: Int, coord: Coordinate, allowRepeated: Boolean) {
    // don't add duplicate coordinates
    if (!allowRepeated) {
      val size = size
      if (size > 0) {
        if (i > 0) {
          val prev = get(i - 1)
          if (prev.equals2D(coord)) return
        }
        if (i < size) {
          val next = get(i)
          if (next.equals2D(coord)) return
        }
      }
    }
    list.add(i, coord)
  }

  /** Add an array of coordinates
   * @param coll The coordinates
   * @param allowRepeated if set to false, repeated coordinates are collapsed
   * @return true (as by general collection contract)
   */
  fun addAll(coll: Collection<Coordinate>, allowRepeated: Boolean): Boolean {
    var isChanged = false
    val i = coll.iterator()
    while (i.hasNext()) {
      add(i.next(), allowRepeated)
      isChanged = true
    }
    return isChanged
  }

  /**
   * Ensure this coordList is a ring, by adding the start point if necessary
   */
  fun closeRing() {
    if (size > 0) {
      val duplicate = get(0).copy()
      add(duplicate, false)
    }
  }

  /** Returns the Coordinates in this collection.
   *
   * @return the coordinates
   */
  fun toCoordinateArray(): Array<Coordinate> {
    return toTypedArray()
  }

  /**
   * Creates an array containing the coordinates in this list,
   * oriented in the given direction (forward or reverse).
   *
   * @param isForward true if the direction is forward, false for reverse
   * @return an oriented array of coordinates
   */
  fun toCoordinateArray(isForward: Boolean): Array<Coordinate> {
    if (isForward) {
      return toTypedArray()
    }
    // construct reversed array
    val size = size
    val pts = arrayOfNulls<Coordinate>(size)
    for (i in 0 until size) {
      pts[i] = get(size - i - 1)
    }
    @Suppress("UNCHECKED_CAST")
    return pts as Array<Coordinate>
  }

  companion object {
  }
}
