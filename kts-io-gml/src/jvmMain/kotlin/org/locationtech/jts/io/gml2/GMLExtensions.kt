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
@file:JvmName("GMLExtensions")

package org.locationtech.jts.io.gml2

import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.io.ParseException
import java.io.IOException
import java.io.Reader
import java.io.Writer

/**
 * Reads a GML2 Geometry from a [Reader] into a single [Geometry]. JVM-only convenience preserving
 * the historical `GMLReader.read(Reader, GeometryFactory)` member (a common class cannot take a
 * `java.io.Reader`). From Java, call `GMLExtensions.read(gmlReader, reader, geometryFactory)`.
 *
 * @throws IOException if reading the input fails
 * @throws ParseException if a parsing problem occurs
 */
@Throws(IOException::class, ParseException::class)
fun GMLReader.read(reader: Reader, geometryFactory: GeometryFactory?): Geometry =
    read(reader.readText(), geometryFactory)

/**
 * Writes a [Geometry] in GML2 format to a [Writer]. JVM-only convenience preserving the historical
 * `GMLWriter.write(Geometry, Writer)` member. From Java, call
 * `GMLExtensions.write(gmlWriter, geometry, writer)`.
 *
 * @throws IOException if writing fails
 */
@Throws(IOException::class)
fun GMLWriter.write(geom: Geometry, writer: Writer) {
    writer.write(write(geom))
    writer.flush()
}
