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

import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

/**
 * JVM backend for [OrdinateFormat], using `java.text.DecimalFormat` (the reference
 * behavior of upstream JTS). `format` is synchronized because `DecimalFormat` is not
 * thread-safe.
 */
internal actual class OrdinateFormatImpl actual constructor(maximumFractionDigits: Int) {

  private val decimalFormat: DecimalFormat = createFormat(maximumFractionDigits)

  @Synchronized
  actual fun format(ord: Double): String = decimalFormat.format(ord)

  private companion object {
    private const val DECIMAL_PATTERN = "0"

    fun createFormat(maximumFractionDigits: Int): DecimalFormat {
      // ensure format uses standard WKT number format
      val nf = NumberFormat.getInstance(Locale.US)
      // This is expected to succeed for Locale.US
      val format = nf as? DecimalFormat
        ?: throw RuntimeException("Unable to create DecimalFormat for Locale.US")
      format.applyPattern(DECIMAL_PATTERN)
      format.maximumFractionDigits = maximumFractionDigits
      return format
    }
  }
}
