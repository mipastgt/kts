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

/**
 * DoubleBits manipulates Double numbers
 * by using bit manipulation and bit-field extraction.
 *
 */
class DoubleBits(private val x: Double) {

  private var xBits: Long = x.toBits()

  fun getDouble(): Double {
    return Double.fromBits(xBits)
  }

  /**
   * Determines the exponent for the number
   */
  fun biasedExponent(): Int {
    val signExp = (xBits shr 52).toInt()
    val exp = signExp and 0x07ff
    return exp
  }

  /**
   * Determines the exponent for the number
   */
  fun getExponent(): Int {
    return biasedExponent() - EXPONENT_BIAS
  }

  fun zeroLowerBits(nBits: Int) {
    val invMask = (1L shl nBits) - 1L
    val mask = invMask.inv()
    xBits = xBits and mask
  }

  fun getBit(i: Int): Int {
    val mask = (1L shl i)
    return if ((xBits and mask) != 0L) 1 else 0
  }

  /**
   * This computes the number of common most-significant bits in the mantissa.
   *
   * @param db
   * @return the number of common most-significant mantissa bits
   */
  fun numCommonMantissaBits(db: DoubleBits): Int {
    for (i in 0 until 52) {
      if (getBit(i) != db.getBit(i)) return i
    }
    return 52
  }

  /**
   * A representation of the Double bits formatted for easy readability
   */
  override fun toString(): String {
    val numStr = xBits.toULong().toString(2)
    // 64 zeroes!
    val zero64 = "0000000000000000000000000000000000000000000000000000000000000000"
    val padStr = zero64 + numStr
    val bitStr = padStr.substring(padStr.length - 64)
    val str = bitStr.substring(0, 1) + "  " +
      bitStr.substring(1, 12) + "(" + getExponent() + ") " +
      bitStr.substring(12) +
      " [ " + x + " ]"
    return str
  }

  companion object {
    const val EXPONENT_BIAS = 1023

    @JvmStatic
    fun powerOf2(exp: Int): Double {
      if (exp > 1023 || exp < -1022) throw IllegalArgumentException("Exponent out of bounds")
      val expBias = (exp + EXPONENT_BIAS).toLong()
      val bits = expBias shl 52
      return Double.fromBits(bits)
    }

    @JvmStatic
    fun exponent(d: Double): Int {
      val db = DoubleBits(d)
      return db.getExponent()
    }

    @JvmStatic
    fun truncateToPowerOfTwo(d: Double): Double {
      val db = DoubleBits(d)
      db.zeroLowerBits(52)
      return db.getDouble()
    }

    @JvmStatic
    fun toBinaryString(d: Double): String {
      val db = DoubleBits(d)
      return db.toString()
    }

    @JvmStatic
    fun maximumCommonMantissa(d1: Double, d2: Double): Double {
      if (d1 == 0.0 || d2 == 0.0) return 0.0

      val db1 = DoubleBits(d1)
      val db2 = DoubleBits(d2)

      if (db1.getExponent() != db2.getExponent()) return 0.0

      val maxCommon = db1.numCommonMantissaBits(db2)
      db1.zeroLowerBits(64 - (12 + maxCommon))
      return db1.getDouble()
    }
  }
}
