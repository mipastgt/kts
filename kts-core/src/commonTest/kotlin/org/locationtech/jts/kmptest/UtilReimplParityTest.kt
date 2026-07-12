/*
 * Copyright (c) 2026 mpMediaSoft.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.locationtech.jts.kmptest

import org.locationtech.jts.index.quadtree.DoubleBits
import org.locationtech.jts.precision.CommonBits
import org.locationtech.jts.util.PriorityQueue
import org.locationtech.jts.util.Random
import org.locationtech.jts.util.TreeMap
import org.locationtech.jts.util.TreeSet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Native-runtime parity for the pure-Kotlin reimplementations of JVM library types that JTS relies
 * on: [Random] (a faithful port of `java.util.Random`'s 48-bit LCG), the comparison-identity
 * [TreeMap]/[TreeSet], the binary-heap [PriorityQueue], and the [DoubleBits]/[CommonBits] bit
 * manipulation (which reimplements `Long.toBinaryString` for negatives). All run on every target.
 */
class UtilReimplParityTest {

  // -- util.Random: must reproduce java.util.Random exactly on every platform ------------------

  /** Independent re-derivation of `java.util.Random` to check the port without golden constants. */
  private class RefRandom(seed: Long) {
    private val mask = (1L shl 48) - 1
    private var s = (seed xor 0x5DEECE66DL) and mask
    fun next(bits: Int): Int {
      s = (s * 0x5DEECE66DL + 0xBL) and mask
      return (s ushr (48 - bits)).toInt()
    }
    fun nextInt(bound: Int): Int {
      if ((bound and -bound) == bound) { // power of two
        return ((bound.toLong() * next(31).toLong()) shr 31).toInt()
      }
      var bits: Int
      var v: Int
      do {
        bits = next(31)
        v = bits % bound
      } while (bits - v + (bound - 1) < 0)
      return v
    }
  }

  @Test
  fun randomMatchesJavaUtilRandomAlgorithm() {
    for (seed in longArrayOf(0L, 1L, 13L, 42L, -7L, 123456789L)) {
      val actual = Random(seed)
      val ref = RefRandom(seed)
      for (bound in intArrayOf(1, 2, 3, 5, 7, 8, 10, 64, 100, 1000, 999)) {
        repeat(50) {
          assertEquals(
            ref.nextInt(bound),
            actual.nextInt(bound),
            "util.Random diverged for seed=$seed bound=$bound",
          )
        }
      }
    }
  }

  // -- util.TreeMap: sorted iteration + comparison-based key identity --------------------------

  @Test
  fun treeMapOrdersAndOverwritesByComparison() {
    val m = TreeMap<Int, String>()
    m.put(3, "c")
    m.put(1, "a")
    m.put(2, "b")
    assertEquals("a", m.put(1, "A")) // returns previous value
    assertEquals(3, m.size)
    assertEquals(listOf(1, 2, 3), m.keys.toList())
    assertEquals("A", m[1])
    assertTrue(m.containsKey(2))
    assertEquals("b", m.remove(2))
    assertEquals(listOf(1, 3), m.keys.toList())
    assertEquals(listOf("A", "c"), m.values.toList())
    assertNull(m.remove(99))
  }

  // -- util.TreeSet: sorted iteration + higher/lower navigable ops -----------------------------

  @Test
  fun treeSetOrdersDedupesAndNavigates() {
    val s = TreeSet<Int>()
    s.addAll(listOf(5, 3, 8, 1, 3))
    assertEquals(4, s.size) // 3 deduped
    assertEquals(listOf(1, 3, 5, 8), s.iterator().asSequence().toList())
    assertTrue(s.contains(5))
    assertFalse(s.contains(4))
    assertEquals(5, s.higher(3))
    assertEquals(1, s.lower(3))
    assertEquals(5, s.higher(4)) // absent element
    assertEquals(3, s.lower(4))
    assertNull(s.higher(8)) // nothing above max
    assertNull(s.lower(1)) // nothing below min
  }

  // -- util.PriorityQueue: min-heap poll order -------------------------------------------------

  @Test
  fun priorityQueuePollsInAscendingOrder() {
    val q = PriorityQueue<Int>()
    listOf(5, 1, 3, 2, 4, 3).forEach { q.add(it) }
    assertEquals(6, q.size())
    assertEquals(1, q.peek())
    val polled = buildList { while (!q.isEmpty()) add(q.poll()!!) }
    assertEquals(listOf(1, 2, 3, 3, 4, 5), polled)
    assertTrue(q.isEmpty())
    assertNull(q.poll())
  }

  // -- util.DoubleBits: exponent / powerOf2 / truncate -----------------------------------------

  @Test
  fun doubleBitsNumericOps() {
    assertEquals(0, DoubleBits.exponent(1.0))
    assertEquals(1, DoubleBits.exponent(2.0))
    assertEquals(-1, DoubleBits.exponent(0.5))
    assertEquals(10, DoubleBits.exponent(1024.0))

    assertEquals(8.0, DoubleBits.powerOf2(3))
    assertEquals(0.25, DoubleBits.powerOf2(-2))

    assertEquals(4.0, DoubleBits.truncateToPowerOfTwo(6.0))
    assertEquals(1.0, DoubleBits.truncateToPowerOfTwo(1.5))
    assertEquals(0.0625, DoubleBits.truncateToPowerOfTwo(0.1))
  }

  @Test
  fun doubleBitsBinaryStringHandlesSignBit() {
    // Exercises the reimplemented unsigned-binary formatting (xBits.toULong().toString(2)):
    // the sign bit is the first character of the rendered string.
    assertEquals('0', DoubleBits.toBinaryString(2.0).first())
    assertEquals('1', DoubleBits.toBinaryString(-2.0).first())
  }

  // -- util.CommonBits: common leading mantissa bits -------------------------------------------

  @Test
  fun commonBitsOfIdenticalValueIsThatValue() {
    val cb = CommonBits()
    cb.add(1234.5)
    cb.add(1234.5)
    assertEquals(1234.5, cb.getCommon())
  }
}
