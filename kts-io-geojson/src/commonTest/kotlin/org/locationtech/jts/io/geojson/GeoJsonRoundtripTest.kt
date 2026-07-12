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
package org.locationtech.jts.io.geojson

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.io.ParseException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Multiplatform runtime validation of the GeoJSON reader/writer. Runs on every target (jvm, js,
 * wasmJs, native). Assertions compare geometries structurally rather than by exact JSON string,
 * because `GeoJsonWriter.formatOrdinate` renders ordinates via `Double.toString`, whose output for
 * whole numbers differs between the JVM and the JS/native backends. Exact-string parity with
 * upstream JTS is covered separately by the JVM-only `GeoJsonWriterTest`.
 */
class GeoJsonRoundtripTest {

    private val gf = GeometryFactory()

    private fun writeNoCRS(json: String): String {
        val geom = GeoJsonReader().read(json)
        val writer = GeoJsonWriter()
        writer.isEncodeCRS = false
        return writer.write(geom)
    }

    private fun roundtrip(json: String) {
        val g1 = GeoJsonReader().read(json)
        val g2 = GeoJsonReader().read(writeNoCRS(json))
        assertTrue(g1.equalsExact(g2), "roundtrip mismatch for: $json")
    }

    @Test
    fun roundtripAllGeometryTypes() {
        val samples = listOf(
            "{\"type\":\"Point\",\"coordinates\":[1,2]}",
            "{\"type\":\"Point\",\"coordinates\":[]}",
            "{\"type\":\"LineString\",\"coordinates\":[[1,2],[10,20],[100,200]]}",
            "{\"type\":\"LineString\",\"coordinates\":[]}",
            "{\"type\":\"Polygon\",\"coordinates\":[[[0,0],[100,0],[100,100],[0,100],[0,0]]]}",
            "{\"type\":\"Polygon\",\"coordinates\":[[[0,0],[100,0],[100,100],[0,100],[0,0]]," +
                "[[1,1],[1,10],[10,10],[10,1],[1,1]]]}",
            "{\"type\":\"MultiPoint\",\"coordinates\":[[0,0],[1,4],[100,200]]}",
            "{\"type\":\"MultiLineString\",\"coordinates\":[[[0,0],[1,10]],[[10,10],[20,30]]]}",
            "{\"type\":\"MultiPolygon\",\"coordinates\":[[[[0,0],[100,0],[100,100],[0,100],[0,0]]]," +
                "[[[200,200],[200,250],[250,250],[250,200],[200,200]]]]}",
            "{\"type\":\"GeometryCollection\",\"geometries\":[" +
                "{\"type\":\"Point\",\"coordinates\":[1,1]}," +
                "{\"type\":\"LineString\",\"coordinates\":[[0,0],[10,10]]}]}",
        )
        for (json in samples) {
            roundtrip(json)
        }
    }

    @Test
    fun readsCrsSrid() {
        val json = "{\"type\":\"Point\",\"coordinates\":[1,2]," +
            "\"crs\":{\"type\":\"name\",\"properties\":{\"name\":\"EPSG:1234\"}}}"
        val geom = GeoJsonReader().read(json)
        assertEquals(1234, geom.getSRID())
    }

    @Test
    fun writesGeometryFromFactory() {
        // Exercises the write path on a factory-built geometry (no exact-string assertion; see
        // the class comment). Whole-number ordinates round-trip through the reader unchanged.
        val point = gf.createPoint(Coordinate(3.0, 4.0))
        val writer = GeoJsonWriter()
        writer.isEncodeCRS = false
        val back = GeoJsonReader().read(writer.write(point))
        assertTrue(point.equalsExact(back))
    }

    @Test
    fun nonObjectJsonThrows() {
        assertFailsWith<ParseException> { GeoJsonReader().read("[]") }
    }

    @Test
    fun missingTypeThrows() {
        assertFailsWith<ParseException> { GeoJsonReader().read("{}") }
    }
}
