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
package org.locationtech.jts.io

import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.io.ParseException
import org.locationtech.jts.io.WKTReader
import java.io.File
import java.io.Reader

/**
 * JVM implementation of [WKTFileReader]. Preserves the historical `java.io.File` and
 * `java.io.Reader` constructors (the latter also serving classpath-resource streams), reading the
 * file content with `java.io` so behaviour is byte-identical to the former `jts-io-wkt` reader.
 */
actual class WKTFileReader private constructor(private val engine: WktFileReaderEngine) {

    /**
     * Creates a new `WKTFileReader` given the `File` to read from
     * and a `WKTReader` to use to parse the geometries.
     *
     * @param file the `File` to read from
     * @param wktReader the geometry reader to use
     */
    constructor(file: File, wktReader: WKTReader) :
        this(WktFileReaderEngine(wktReader) { file.readText() })

    // The by-name constructor reads via kotlinx-io on every platform (UTF-8), so the same file
    // access is exercised on the JVM as on native. The File/Reader constructors below use java.io.
    actual constructor(filename: String, wktReader: WKTReader) :
        this(WktFileReaderEngine(wktReader) { readFileText(filename) })

    /**
     * Creates a new `WKTFileReader`, given a `Reader` to read from.
     *
     * @param reader the reader to read from
     * @param wktReader the geometry reader to use
     */
    constructor(reader: Reader, wktReader: WKTReader) :
        this(WktFileReaderEngine(wktReader) { reader.use { it.readText() } })

    actual fun setLimit(limit: Int) {
        engine.limit = limit
    }

    actual fun setStrictParsing(isStrict: Boolean) {
        engine.isStrictParsing = isStrict
    }

    actual fun setOffset(offset: Int) {
        engine.offset = offset
    }

    @Throws(ParseException::class)
    actual fun read(): List<Geometry> = engine.read()
}
