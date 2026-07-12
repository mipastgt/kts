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
package org.locationtech.jts.geomgraph

import org.locationtech.jts.geom.Location
import org.locationtech.jts.geom.Position

/**
 * A TopologyLocation is the labelling of a
 * GraphComponent's topological relationship to a single Geometry.
 *
 *
 * If the parent component is an area edge, each side and the edge itself
 * have a topological location.  These locations are named
 *
 *  * ON: on the edge
 *  * LEFT: left-hand side of the edge
 *  * RIGHT: right-hand side
 *
 * If the parent component is a line edge or node, there is a single
 * topological relationship attribute, ON.
 *
 *
 * The possible values of a topological location are
 * {Location.NONE, Location.EXTERIOR, Location.BOUNDARY, Location.INTERIOR}
 *
 *
 * The labelling is stored in an array location[j] where
 * where j has the values ON, LEFT, RIGHT
 * @version 1.7
 */
class TopologyLocation {

  internal var location: IntArray = IntArray(0)

  constructor(location: IntArray) {
    init(location.size)
  }

  /**
   * Constructs a TopologyLocation specifying how points on, to the left of, and to the
   * right of some GraphComponent relate to some Geometry. Possible values for the
   * parameters are Location.NULL, Location.EXTERIOR, Location.BOUNDARY,
   * and Location.INTERIOR.
   * @see Location
   * @param on on position
   * @param left left position
   * @param right right position
   */
  constructor(on: Int, left: Int, right: Int) {
    init(3)
    location[Position.ON] = on
    location[Position.LEFT] = left
    location[Position.RIGHT] = right
  }

  constructor(on: Int) {
    init(1)
    location[Position.ON] = on
  }

  constructor(gl: TopologyLocation) {
    init(gl.location.size)
    for (i in location.indices) {
      location[i] = gl.location[i]
    }
  }

  private fun init(size: Int) {
    location = IntArray(size)
    setAllLocations(Location.NONE)
  }

  fun get(posIndex: Int): Int {
    if (posIndex < location.size) return location[posIndex]
    return Location.NONE
  }

  /**
   * @return true if all locations are NULL
   */
  fun isNull(): Boolean {
    for (i in location.indices) {
      if (location[i] != Location.NONE) return false
    }
    return true
  }

  /**
   * @return true if any locations are NULL
   */
  fun isAnyNull(): Boolean {
    for (i in location.indices) {
      if (location[i] == Location.NONE) return true
    }
    return false
  }

  fun isEqualOnSide(le: TopologyLocation, locIndex: Int): Boolean {
    return location[locIndex] == le.location[locIndex]
  }

  fun isArea(): Boolean = location.size > 1
  fun isLine(): Boolean = location.size == 1

  fun flip() {
    if (location.size <= 1) return
    val temp = location[Position.LEFT]
    location[Position.LEFT] = location[Position.RIGHT]
    location[Position.RIGHT] = temp
  }

  fun setAllLocations(locValue: Int) {
    for (i in location.indices) {
      location[i] = locValue
    }
  }

  fun setAllLocationsIfNull(locValue: Int) {
    for (i in location.indices) {
      if (location[i] == Location.NONE) location[i] = locValue
    }
  }

  fun setLocation(locIndex: Int, locValue: Int) {
    location[locIndex] = locValue
  }

  fun setLocation(locValue: Int) {
    setLocation(Position.ON, locValue)
  }

  fun getLocations(): IntArray = location

  fun setLocations(on: Int, left: Int, right: Int) {
    location[Position.ON] = on
    location[Position.LEFT] = left
    location[Position.RIGHT] = right
  }

  fun allPositionsEqual(loc: Int): Boolean {
    for (i in location.indices) {
      if (location[i] != loc) return false
    }
    return true
  }

  /**
   * merge updates only the NULL attributes of this object
   * with the attributes of another.
   *
   * @param gl Topology location
   */
  fun merge(gl: TopologyLocation) {
    // if the src is an Area label & and the dest is not, increase the dest to be an Area
    if (gl.location.size > location.size) {
      val newLoc = IntArray(3)
      newLoc[Position.ON] = location[Position.ON]
      newLoc[Position.LEFT] = Location.NONE
      newLoc[Position.RIGHT] = Location.NONE
      location = newLoc
    }
    for (i in location.indices) {
      if (location[i] == Location.NONE && i < gl.location.size)
        location[i] = gl.location[i]
    }
  }

  override fun toString(): String {
    val buf = StringBuilder()
    if (location.size > 1) buf.append(Location.toLocationSymbol(location[Position.LEFT]))
    buf.append(Location.toLocationSymbol(location[Position.ON]))
    if (location.size > 1) buf.append(Location.toLocationSymbol(location[Position.RIGHT]))
    return buf.toString()
  }
}
