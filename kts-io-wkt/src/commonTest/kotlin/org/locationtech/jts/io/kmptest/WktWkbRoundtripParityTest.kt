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
package org.locationtech.jts.io.kmptest

import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.io.ByteOrderValues
import org.locationtech.jts.io.WKBReader
import org.locationtech.jts.io.WKBWriter
import org.locationtech.jts.io.WKTReader
import org.locationtech.jts.io.WKTWriter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Native-runtime parity for the WKT/WKB IO path. Runs on every target (jvm/native/js/wasm) and
 * exercises the hand-rolled `WktStreamTokenizer` (replacing `java.io.StreamTokenizer`), the WKT
 * parser, the WKB binary reader/writer with both byte orders, and the [org.locationtech.jts.io.OrdinateFormat]
 * seam via [WKTWriter]. Passing identically on all platforms proves the dep-free common parser
 * behaves the same off-JVM.
 */
class WktWkbRoundtripParityTest {

  private val wkts = listOf(
    "POINT (1 2)",
    "LINESTRING (0 0, 1 1, 2 2)",
    "POLYGON ((0 0, 4 0, 4 4, 0 4, 0 0))",
    "POLYGON ((0 0, 10 0, 10 10, 0 10, 0 0), (2 2, 2 4, 4 4, 4 2, 2 2))",
    "MULTIPOINT ((0 0), (1 1), (2 3))",
    "MULTILINESTRING ((0 0, 1 1), (2 2, 3 3))",
    "GEOMETRYCOLLECTION (POINT (1 1), LINESTRING (0 0, 2 2))",
  )

  private fun read(wkt: String): Geometry = WKTReader().read(wkt)

  @Test
  fun wktStringRoundtripIsStable() {
    // The canonical WKT of each input is itself, so read -> write reproduces it exactly. This
    // exercises the tokenizer, the parser, and the OrdinateFormat write path together.
    val writer = WKTWriter()
    for (wkt in wkts) {
      assertEquals(wkt, writer.write(read(wkt)), "WKT roundtrip differs for: $wkt")
    }
  }

  @Test
  fun wkbBinaryRoundtripPreservesGeometry() {
    val writer = WKBWriter()
    val reader = WKBReader()
    for (wkt in wkts) {
      val g = read(wkt)
      val back = reader.read(writer.write(g))
      assertTrue(g.equalsExact(back), "WKB roundtrip differs for: $wkt")
    }
  }

  @Test
  fun wkbHexRoundtripPreservesGeometry() {
    for (wkt in wkts) {
      val g = read(wkt)
      val hex = WKBWriter.toHex(WKBWriter().write(g))
      val back = WKBReader().read(WKBReader.hexToBytes(hex))
      assertTrue(g.equalsExact(back), "WKB hex roundtrip differs for: $wkt")
    }
  }

  @Test
  fun wkbBothByteOrdersRoundtrip() {
    val reader = WKBReader()
    for (byteOrder in intArrayOf(ByteOrderValues.BIG_ENDIAN, ByteOrderValues.LITTLE_ENDIAN)) {
      val writer = WKBWriter(2, byteOrder)
      for (wkt in wkts) {
        val g = read(wkt)
        assertTrue(
          g.equalsExact(reader.read(writer.write(g))),
          "WKB byteOrder=$byteOrder roundtrip differs for: $wkt",
        )
      }
    }
  }
}
