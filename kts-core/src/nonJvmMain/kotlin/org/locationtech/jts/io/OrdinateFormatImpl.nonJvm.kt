/*
 * Copyright (c) 2019 Martin Davis.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.locationtech.jts.io

/**
 * Native backend for [OrdinateFormat]. Pure-Kotlin (no `java.text`) reproduction of
 * `DecimalFormat("0")` with `maximumFractionDigits`, HALF_EVEN rounding, a period decimal
 * separator, no grouping and no scientific notation.
 *
 * It derives the shortest round-trip decimal digits from [Double.toString] and re-expands any
 * scientific notation into plain decimal form. For the default formatter (`maxFractionDigits`
 * = 325) no rounding ever occurs, so the output is the exact shortest-decimal form — identical
 * to the JVM `DecimalFormat` backend.
 *
 * KNOWN LIMITATION (Stage 2B follow-up, to be pinned down by a native test): on the *rounding*
 * path (`create(n)` with a small `n` that forces truncation), this rounds the shortest decimal,
 * whereas `DecimalFormat` rounds the exact binary value; the two can differ in the last place
 * for half-way doubles (e.g. `0.015`, whose exact value is `0.01499…`). Exact parity there
 * would require expanding the exact decimal value of the double.
 */
internal actual class OrdinateFormatImpl actual constructor(private val maximumFractionDigits: Int) {
  actual fun format(ord: Double): String = formatFixed(ord, maximumFractionDigits)
}

private fun formatFixed(value: Double, maxFractionDigits: Int): String {
  if (value == 0.0) return "0"
  val negative = value < 0.0
  val s = (if (negative) -value else value).toString()

  // Split mantissa / exponent (Double.toString may use scientific notation).
  var eIdx = s.indexOf('E')
  if (eIdx < 0) eIdx = s.indexOf('e')
  val mantissa: String
  val exp: Int
  if (eIdx >= 0) {
    mantissa = s.substring(0, eIdx)
    exp = s.substring(eIdx + 1).toInt()
  } else {
    mantissa = s
    exp = 0
  }

  // Split integer / fraction parts of the mantissa.
  val dotIdx = mantissa.indexOf('.')
  val intPart: String
  val fracPart: String
  if (dotIdx >= 0) {
    intPart = mantissa.substring(0, dotIdx)
    fracPart = mantissa.substring(dotIdx + 1)
  } else {
    intPart = mantissa
    fracPart = ""
  }

  // value == digits * 10^(-scale)
  var digits = intPart + fracPart
  var scale = fracPart.length - exp

  // Round off excess fraction digits (HALF_EVEN) when scale exceeds the maximum.
  if (scale > maxFractionDigits) {
    val drop = scale - maxFractionDigits
    digits = roundHalfEven(digits, drop)
    scale = maxFractionDigits
  }

  val result = buildPlain(digits, scale)
  return if (negative && result != "0") "-$result" else result
}

/** Builds a plain decimal string for `digits * 10^(-scale)`, stripping trailing fraction zeros. */
private fun buildPlain(digits: String, scale: Int): String {
  if (scale <= 0) {
    val intp = stripLeadingZeros(digits)
    val sb = StringBuilder(intp)
    repeat(-scale) { sb.append('0') }
    return sb.toString()
  }
  // Left-pad so there is at least one integer digit.
  var d = digits
  if (d.length <= scale) {
    d = "0".repeat(scale - d.length + 1) + d
  }
  val pointPos = d.length - scale
  val intp = stripLeadingZeros(d.substring(0, pointPos)).ifEmpty { "0" }
  val frac = d.substring(pointPos).trimEnd('0')
  return if (frac.isEmpty()) intp else "$intp.$frac"
}

private fun stripLeadingZeros(s: String): String {
  var i = 0
  while (i < s.length - 1 && s[i] == '0') i++
  return s.substring(i)
}

/**
 * Rounds off the last [drop] digits of [digits] using HALF_EVEN, returning the kept digit
 * string (its length may grow by one on a carry, e.g. "99" -> "100").
 */
private fun roundHalfEven(digits: String, drop: Int): String {
  if (drop <= 0) return digits
  val keepLen = if (drop >= digits.length) 0 else digits.length - drop
  val kept = digits.substring(0, keepLen)

  val roundDigit = digits[keepLen]
  val roundUp: Boolean = when {
    roundDigit < '5' -> false
    roundDigit > '5' -> true
    else -> {
      // exactly '5' in this place: round up if any following digit is non-zero, else HALF_EVEN
      var nonZeroRest = false
      for (i in keepLen + 1 until digits.length) {
        if (digits[i] != '0') { nonZeroRest = true; break }
      }
      if (nonZeroRest) true
      else {
        val lastKept = if (keepLen == 0) '0' else kept[kept.length - 1]
        ((lastKept - '0') % 2) == 1 // round up only if last kept digit is odd
      }
    }
  }
  if (!roundUp) return if (kept.isEmpty()) "0" else kept
  return incrementDigits(kept)
}

/** Increments a decimal digit string by one, handling carry (e.g. "" -> "1", "99" -> "100"). */
private fun incrementDigits(s: String): String {
  if (s.isEmpty()) return "1"
  val arr = s.toCharArray()
  var i = arr.size - 1
  while (i >= 0) {
    if (arr[i] == '9') {
      arr[i] = '0'
      i--
    } else {
      arr[i] = arr[i] + 1
      return arr.concatToString()
    }
  }
  return "1" + arr.concatToString()
}
