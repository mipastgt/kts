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

import kotlin.jvm.JvmStatic

import org.locationtech.jts.geom.Location
import org.locationtech.jts.geom.Position

/**
 * A Depth object records the topological depth of the sides
 * of an Edge for up to two Geometries.
 * @version 1.7
 */
class Depth {

  private val depth = Array(2) { IntArray(3) }

  init {
    // initialize depth array to a sentinel value
    for (i in 0..1) {
      for (j in 0..2) {
        depth[i][j] = NULL_VALUE
      }
    }
  }

  fun getDepth(geomIndex: Int, posIndex: Int): Int {
    return depth[geomIndex][posIndex]
  }

  fun setDepth(geomIndex: Int, posIndex: Int, depthValue: Int) {
    depth[geomIndex][posIndex] = depthValue
  }

  fun getLocation(geomIndex: Int, posIndex: Int): Int {
    if (depth[geomIndex][posIndex] <= 0) return Location.EXTERIOR
    return Location.INTERIOR
  }

  fun add(geomIndex: Int, posIndex: Int, location: Int) {
    if (location == Location.INTERIOR)
      depth[geomIndex][posIndex]++
  }

  /**
   * A Depth object is null (has never been initialized) if all depths are null.
   *
   * @return True if depth is null (has never been initialized)
   */
  fun isNull(): Boolean {
    for (i in 0..1) {
      for (j in 0..2) {
        if (depth[i][j] != NULL_VALUE)
          return false
      }
    }
    return true
  }

  fun isNull(geomIndex: Int): Boolean {
    return depth[geomIndex][1] == NULL_VALUE
  }

  fun isNull(geomIndex: Int, posIndex: Int): Boolean {
    return depth[geomIndex][posIndex] == NULL_VALUE
  }

  fun add(lbl: Label) {
    for (i in 0..1) {
      for (j in 1..2) {
        val loc = lbl.getLocation(i, j)
        if (loc == Location.EXTERIOR || loc == Location.INTERIOR) {
          // initialize depth if it is null, otherwise add this location value
          if (isNull(i, j)) {
            depth[i][j] = depthAtLocation(loc)
          } else
            depth[i][j] += depthAtLocation(loc)
        }
      }
    }
  }

  fun getDelta(geomIndex: Int): Int {
    return depth[geomIndex][Position.RIGHT] - depth[geomIndex][Position.LEFT]
  }

  /**
   * Normalize the depths for each geometry, if they are non-null.
   * A normalized depth
   * has depth values in the set { 0, 1 }.
   * Normalizing the depths
   * involves reducing the depths by the same amount so that at least
   * one of them is 0.  If the remaining value is &gt; 0, it is set to 1.
   */
  fun normalize() {
    for (i in 0..1) {
      if (!isNull(i)) {
        var minDepth = depth[i][1]
        if (depth[i][2] < minDepth)
          minDepth = depth[i][2]

        if (minDepth < 0) minDepth = 0
        for (j in 1..2) {
          var newValue = 0
          if (depth[i][j] > minDepth)
            newValue = 1
          depth[i][j] = newValue
        }
      }
    }
  }

  override fun toString(): String {
    return "A: " + depth[0][1] + "," + depth[0][2] +
      " B: " + depth[1][1] + "," + depth[1][2]
  }

  companion object {
    private const val NULL_VALUE = -1

    @JvmStatic
    fun depthAtLocation(location: Int): Int {
      if (location == Location.EXTERIOR) return 0
      if (location == Location.INTERIOR) return 1
      return NULL_VALUE
    }
  }
}
