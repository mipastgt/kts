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
package org.locationtech.jts.precision

import kotlin.jvm.JvmStatic

/**
 * Determines the maximum number of common most-significant
 * bits in the mantissa of one or numbers.
 * Can be used to compute the double-precision number which
 * is represented by the common bits.
 * If there are no common bits, the number computed is 0.0.
 *
 */
class CommonBits {

  private var isFirst = true
  private var commonMantissaBitsCount = 53
  private var commonBits: Long = 0
  private var commonSignExp: Long = 0

  fun add(num: Double) {
    val numBits = num.toBits()
    if (isFirst) {
      commonBits = numBits
      commonSignExp = signExpBits(commonBits)
      isFirst = false
      return
    }

    val numSignExp = signExpBits(numBits)
    if (numSignExp != commonSignExp) {
      commonBits = 0
      return
    }

//    System.out.println(toString(commonBits));
//    System.out.println(toString(numBits));
    commonMantissaBitsCount = numCommonMostSigMantissaBits(commonBits, numBits)
    commonBits = zeroLowerBits(commonBits, 64 - (12 + commonMantissaBitsCount))
//    System.out.println(toString(commonBits));
  }

  fun getCommon(): Double {
    return Double.fromBits(commonBits)
  }

  /**
   * A representation of the Double bits formatted for easy readability
   */
  fun toString(bits: Long): String {
    val x = Double.fromBits(bits)
    val numStr = bits.toULong().toString(2)
    val padStr = "0000000000000000000000000000000000000000000000000000000000000000$numStr"
    val bitStr = padStr.substring(padStr.length - 64)
    val str = (bitStr.substring(0, 1) + "  "
        + bitStr.substring(1, 12) + "(exp) "
        + bitStr.substring(12)
        + " [ " + x + " ]")
    return str
  }

  companion object {
    /**
     * Computes the bit pattern for the sign and exponent of a
     * double-precision number.
     *
     * @return the bit pattern for the sign and exponent
     */
    @JvmStatic
    fun signExpBits(num: Long): Long {
      return num shr 52
    }

    /**
     * This computes the number of common most-significant bits in the mantissas
     * of two double-precision numbers.
     * It does not count the hidden bit, which is always 1.
     * It does not determine whether the numbers have the same exponent - if they do
     * not, the value computed by this function is meaningless.
     *
     * @param num1 the first number
     * @param num2 the second number
     * @return the number of common most-significant mantissa bits
     */
    @JvmStatic
    fun numCommonMostSigMantissaBits(num1: Long, num2: Long): Int {
      var count = 0
      for (i in 52 downTo 0) {
        if (getBit(num1, i) != getBit(num2, i))
          return count
        count++
      }
      return 52
    }

    /**
     * Zeroes the lower n bits of a bitstring.
     *
     * @param bits the bitstring to alter
     * @return the zeroed bitstring
     */
    @JvmStatic
    fun zeroLowerBits(bits: Long, nBits: Int): Long {
      val invMask = (1L shl nBits) - 1L
      val mask = invMask.inv()
      val zeroed = bits and mask
      return zeroed
    }

    /**
     * Extracts the i'th bit of a bitstring.
     *
     * @param bits the bitstring to extract from
     * @param i the bit to extract
     * @return the value of the extracted bit
     */
    @JvmStatic
    fun getBit(bits: Long, i: Int): Int {
      val mask = (1L shl i)
      return if ((bits and mask) != 0L) 1 else 0
    }
  }
}
