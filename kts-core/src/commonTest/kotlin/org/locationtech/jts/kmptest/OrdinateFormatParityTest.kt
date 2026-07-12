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

import org.locationtech.jts.io.OrdinateFormat
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Native-runtime parity for the [OrdinateFormat] number-formatting seam. Runs on every target
 * (jvm/native/js/wasm): the JVM `actual` uses `java.text.DecimalFormat`; the non-JVM `actual` is a
 * hand-rolled shortest-digit formatter. These assertions passing on all platforms proves the two
 * backends produce identical output for the paths JTS actually uses.
 *
 * The DEFAULT formatter (maxFractionDigits = 325) never rounds, so it must be byte-exact on every
 * platform. Only the rounding path (`create(n)` with small n) has a documented, out-of-scope 1-ulp
 * divergence for exactly-half-way doubles such as `create(2).format(0.015)`; those are deliberately
 * NOT asserted here. Clearly-unambiguous rounding cases are.
 */
class OrdinateFormatParityTest {

  private fun fmt(d: Double): String = OrdinateFormat.DEFAULT.format(d)

  @Test
  fun defaultFormatsIntegersWithoutDecimals() {
    assertEquals("0", fmt(0.0))
    assertEquals("1", fmt(1.0))
    assertEquals("2", fmt(2.0))
    assertEquals("100", fmt(100.0))
    assertEquals("123456789", fmt(123456789.0))
    assertEquals("-1", fmt(-1.0))
  }

  @Test
  fun defaultFormatsFractionsAsShortestDecimal() {
    assertEquals("0.1", fmt(0.1))
    assertEquals("0.5", fmt(0.5))
    assertEquals("-0.25", fmt(-0.25))
    assertEquals("1.5", fmt(1.5))
    assertEquals("-1.5", fmt(-1.5))
    assertEquals("0.015", fmt(0.015)) // DEFAULT never rounds -> exact shortest form on all platforms
    assertEquals("1234.5678", fmt(1234.5678))
    assertEquals("0.3333333333333333", fmt(1.0 / 3.0))
  }

  @Test
  fun defaultNeverUsesScientificNotation() {
    assertEquals("100000000000000000000", fmt(1.0e20))
    assertEquals("0.0000001", fmt(1.0e-7))
  }

  @Test
  fun specialValues() {
    assertEquals("NaN", fmt(Double.NaN))
    assertEquals("Inf", fmt(Double.POSITIVE_INFINITY))
    assertEquals("-Inf", fmt(Double.NEGATIVE_INFINITY))
  }

  @Test
  fun roundingPathUnambiguousCases() {
    // Rounding digit is clearly <5 or >5 (not an exact half-way double), so DecimalFormat's
    // exact-binary rounding and the shortest-decimal rounding agree on every platform.
    assertEquals("3.1", OrdinateFormat.create(1).format(3.14))
    assertEquals("3.8", OrdinateFormat.create(1).format(3.77))
    assertEquals("3", OrdinateFormat.create(0).format(3.2))
    assertEquals("4", OrdinateFormat.create(0).format(3.7))
    assertEquals("1.235", OrdinateFormat.create(3).format(1.23456))
  }
}
