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
package org.locationtech.jts.io

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Exercises the `jts-io-files` readers end-to-end on the JVM, including the by-name constructor
 * (which reads via kotlinx-io on every platform) and the streaming multi-geometry engine.
 */
class FileReaderTest {

    private fun tempFile(suffix: String, content: String): String {
        val f = File.createTempFile("jts-io-files-test", suffix)
        f.deleteOnExit()
        f.writeText(content)
        return f.absolutePath
    }

    @Test
    fun readWktMultipleGeometries() {
        val path = tempFile(
            ".wkt",
            "POINT (1 2)\n" +
                "  LINESTRING (0 0, 1 1, 2 2)  \n" +
                "\n" +
                "POLYGON ((0 0, 10 0, 10 10, 0 10, 0 0))\n",
        )
        val geoms = WKTFileReader(path, WKTReader()).read()
        assertEquals(3, geoms.size)
        assertEquals("Point", geoms[0].getGeometryType())
        assertEquals("LineString", geoms[1].getGeometryType())
        assertEquals("Polygon", geoms[2].getGeometryType())
    }

    @Test
    fun readWktOffsetAndLimit() {
        val path = tempFile(".wkt", "POINT (0 0)\nPOINT (1 1)\nPOINT (2 2)\nPOINT (3 3)\n")
        val reader = WKTFileReader(path, WKTReader())
        reader.setOffset(1)
        reader.setLimit(2)
        val geoms = reader.read()
        assertEquals(2, geoms.size)
        // offset skips the first geometry; both survivors are the 2nd and 3rd points
        assertEquals(1.0, geoms[0].getCoordinate()!!.x, 0.0)
        assertEquals(2.0, geoms[1].getCoordinate()!!.x, 0.0)
    }

    @Test
    fun readWktNonStrictReturnsPartialResult() {
        val path = tempFile(".wkt", "POINT (0 0)\nNOT-A-GEOMETRY\n")
        val reader = WKTFileReader(path, WKTReader())
        reader.setStrictParsing(false)
        val geoms = reader.read()
        assertEquals(1, geoms.size)
    }

    @Test
    fun readWkbHexMultipleGeometries() {
        // Little-endian WKB for POINT (1 2) and POINT (3 4).
        val path = tempFile(
            ".wkb",
            "0101000000000000000000F03F0000000000000040\n" +
                "\n" +
                "010100000000000000000008400000000000001040\n",
        )
        val geoms = WKBHexFileReader(path, WKBReader()).read()
        assertEquals(2, geoms.size)
        assertEquals(1.0, geoms[0].getCoordinate()!!.x, 0.0)
        assertEquals(2.0, geoms[0].getCoordinate()!!.y, 0.0)
        assertEquals(3.0, geoms[1].getCoordinate()!!.x, 0.0)
        assertTrue(geoms.all { it.getGeometryType() == "Point" })
    }
}
