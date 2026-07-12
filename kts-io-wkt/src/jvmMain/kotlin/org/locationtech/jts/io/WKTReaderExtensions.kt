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
@file:JvmName("WKTReaderExtensions")

package org.locationtech.jts.io

import org.locationtech.jts.geom.Geometry
import java.io.Reader

/** A JVM streaming [WktCharStream] over a `java.io.Reader`. */
private class ReaderWktCharStream(private val reader: Reader) : WktCharStream {
    override fun read(): Int = reader.read()
}

/**
 * Reads a single Well-Known Text [Geometry] from a [Reader], consuming one geometry and leaving the
 * reader positioned at the next (streaming). JVM-only convenience preserving the historical
 * `WKTReader.read(Reader)` behaviour. From Java, call `WKTReaderExtensions.read(wktReader, reader)`.
 *
 * @throws ParseException if a parsing problem occurs
 */
@Throws(ParseException::class)
fun WKTReader.read(reader: Reader): Geometry = read(ReaderWktCharStream(reader))
