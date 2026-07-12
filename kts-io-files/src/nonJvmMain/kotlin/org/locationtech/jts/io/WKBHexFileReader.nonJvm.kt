/*
 * Copyright (c) 2016 Martin Davis.
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
import org.locationtech.jts.io.WKBReader

/**
 * Native implementation of [WKBHexFileReader]. The file is read from a filesystem path via
 * kotlinx-io (`SystemFileSystem`); the parsing engine is shared with every platform.
 */
actual class WKBHexFileReader private constructor(private val engine: WkbHexFileReaderEngine) {

    actual constructor(filename: String, wkbReader: WKBReader) :
        this(WkbHexFileReaderEngine(wkbReader) { readFileText(filename) })

    actual fun setLimit(limit: Int) {
        engine.limit = limit
    }

    actual fun setOffset(offset: Int) {
        engine.offset = offset
    }

    @Throws(ParseException::class)
    actual fun read(): List<Geometry> = engine.read()
}
