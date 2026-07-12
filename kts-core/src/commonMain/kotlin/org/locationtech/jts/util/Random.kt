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
package org.locationtech.jts.util

/**
 * A faithful reimplementation of the 48-bit linear congruential generator used by
 * `java.util.Random`. It exists so that seeded sequences match the classic Java behaviour
 * exactly, which is required for JTS to produce reproducible output identical to upstream
 * (Java) JTS: a fixed seed is used to shuffle points before KD-tree insertion.
 * `kotlin.random.Random` uses a different algorithm and would produce a different sequence
 * for the same seed.
 *
 * Only the subset of the API used within JTS is provided.
 */
internal class Random(seed: Long) {

  private var seed: Long = (seed xor MULTIPLIER) and MASK

  /**
   * Generates the next pseudorandom number, matching `java.util.Random.next(int)`.
   */
  private fun next(bits: Int): Int {
    seed = (seed * MULTIPLIER + INCREMENT) and MASK
    return (seed ushr (48 - bits)).toInt()
  }

  /**
   * Returns a pseudorandom, uniformly distributed `Int` value between 0 (inclusive)
   * and the specified `bound` (exclusive), matching `java.util.Random.nextInt(int)`.
   */
  fun nextInt(bound: Int): Int {
    require(bound > 0) { "bound must be positive" }

    var r = next(31)
    val m = bound - 1
    if (bound and m == 0) {
      // bound is a power of two
      r = ((bound.toLong() * r) shr 31).toInt()
    } else {
      var u = r
      while (true) {
        r = u % bound
        if (u - r + m >= 0) break
        u = next(31)
      }
    }
    return r
  }

  companion object {
    private const val MULTIPLIER = 0x5DEECE66DL
    private const val INCREMENT = 0xBL
    private const val MASK = (1L shl 48) - 1
  }
}
