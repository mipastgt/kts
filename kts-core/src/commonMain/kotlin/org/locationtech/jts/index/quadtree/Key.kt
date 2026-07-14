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
package org.locationtech.jts.index.quadtree

import kotlin.jvm.JvmStatic
import kotlin.math.floor

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Envelope

/**
 * A Key is a unique identifier for a node in a quadtree.
 * It contains a lower-left point and a level number. The level number
 * is the power of two for the size of the node envelope
 *
 */
class Key(itemEnv: Envelope) {

  // the fields which make up the key
  private val pt = Coordinate()
  private var level = 0
  // auxiliary data which is derived from the key for use in computation
  private var env: Envelope? = null

  init {
    computeKey(itemEnv)
  }

  fun getPoint(): Coordinate {
    return pt
  }

  fun getLevel(): Int {
    return level
  }

  fun getEnvelope(): Envelope {
    return env!!
  }

  fun getCentre(): Coordinate {
    return Coordinate(
      (env!!.getMinX() + env!!.getMaxX()) / 2,
      (env!!.getMinY() + env!!.getMaxY()) / 2
    )
  }

  /**
   * return a square envelope containing the argument envelope,
   * whose extent is a power of two and which is based at a power of 2
   */
  fun computeKey(itemEnv: Envelope) {
    level = computeQuadLevel(itemEnv)
    env = Envelope()
    computeKey(level, itemEnv)
    // MD - would be nice to have a non-iterative form of this algorithm
    while (!env!!.contains(itemEnv)) {
      level += 1
      computeKey(level, itemEnv)
    }
  }

  private fun computeKey(level: Int, itemEnv: Envelope) {
    val quadSize = DoubleBits.powerOf2(level)
    pt.x = floor(itemEnv.getMinX() / quadSize) * quadSize
    pt.y = floor(itemEnv.getMinY() / quadSize) * quadSize
    env!!.init(pt.x, pt.x + quadSize, pt.y, pt.y + quadSize)
  }

  companion object {
    @JvmStatic
    fun computeQuadLevel(env: Envelope): Int {
      val dx = env.getWidth()
      val dy = env.getHeight()
      val dMax = if (dx > dy) dx else dy
      val level = DoubleBits.exponent(dMax) + 1
      return level
    }
  }
}
