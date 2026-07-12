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
import org.locationtech.jts.io.WktCharStream

/**
 * A [WktCharStream] over in-memory file content, adding whitespace-skipping end-of-input detection.
 * Reproduces `WKTFileReader.isAtEndOfFile`: skip leading whitespace, report EOF, and otherwise leave
 * the stream positioned at the first non-whitespace character (equivalent to the original's
 * mark/read/reset push-back).
 */
internal class WktContentCharStream(private val text: String) : WktCharStream {
    private var pos = 0

    override fun read(): Int = if (pos < text.length) text[pos++].code else -1

    fun isAtEndOfFile(): Boolean {
        while (pos < text.length && text[pos].isWhitespace()) pos++
        return pos >= text.length
    }
}

/**
 * The platform-agnostic engine backing [WKTFileReader]. Reads a sequence of WKT geometries from the
 * file content supplied by [contentSupplier] (invoked lazily on [read], so construction never does
 * I/O — matching the historical Java behaviour). The geometry-boundary streaming reuses
 * [WKTReader.read] over a shared [WktContentCharStream].
 */
internal class WktFileReaderEngine(
    private val wktReader: WKTReader,
    private val contentSupplier: () -> String,
) {
    var limit: Int = -1
    var offset: Int = 0
    var isStrictParsing: Boolean = true

    @Throws(ParseException::class)
    fun read(): List<Geometry> {
        val geoms = ArrayList<Geometry>()
        try {
            readAll(contentSupplier(), geoms)
        } catch (ex: ParseException) {
            // throw if strict or error is on first geometry
            if (isStrictParsing || geoms.size == 0) throw ex
        }
        return geoms
    }

    private fun readAll(content: String, geoms: MutableList<Geometry>) {
        val stream = WktContentCharStream(content)
        var count = 0
        while (!stream.isAtEndOfFile() && !isAtLimit(geoms)) {
            val g = wktReader.read(stream)
            if (count >= offset) geoms.add(g)
            count++
        }
    }

    private fun isAtLimit(geoms: List<Geometry>): Boolean {
        if (limit < 0) return false
        return geoms.size >= limit
    }
}
