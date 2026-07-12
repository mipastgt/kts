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
@file:JvmName("GeoJsonExtensions")

package org.locationtech.jts.io.geojson

import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.io.ParseException
import java.io.IOException
import java.io.Reader
import java.io.Writer

/**
 * Reads a GeoJson Geometry from a [Reader] into a single [Geometry]. JVM-only convenience preserving
 * the historical `GeoJsonReader.read(Reader)` member (a common class cannot take a `java.io.Reader`).
 * From Java, call `GeoJsonExtensions.read(geoJsonReader, reader)`.
 *
 * @throws ParseException if the JSON cannot be parsed as a Geometry
 */
@Throws(ParseException::class)
fun GeoJsonReader.read(reader: Reader): Geometry = read(reader.readText())

/**
 * Writes a [Geometry] in GeoJson format into a [Writer]. JVM-only convenience preserving the
 * historical `GeoJsonWriter.write(Geometry, Writer)` member. From Java, call
 * `GeoJsonExtensions.write(geoJsonWriter, geometry, writer)`.
 *
 * @throws IOException if writing fails
 */
@Throws(IOException::class)
fun GeoJsonWriter.write(geometry: Geometry, writer: Writer) {
    writer.write(write(geometry))
    writer.flush()
}
