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
 * The platform-agnostic engine backing [WKBHexFileReader]. Reads a sequence of WKB-hex geometries
 * (one per line) from the file content supplied by [contentSupplier] (invoked lazily on [read], so
 * construction never does I/O — matching the historical Java behaviour). Blank lines are skipped.
 */
internal class WkbHexFileReaderEngine(
    private val wkbReader: WKBReader,
    private val contentSupplier: () -> String,
) {
    var limit: Int = -1
    var offset: Int = 0

    @Throws(ParseException::class)
    fun read(): List<Geometry> {
        val geoms = ArrayList<Geometry>()
        var count = 0
        for (rawLine in contentSupplier().lineSequence()) {
            if (isAtLimit(geoms)) break
            val line = rawLine.trim()
            if (line.isEmpty()) continue
            val g = wkbReader.read(WKBReader.hexToBytes(line))
            if (count >= offset) geoms.add(g)
            count++
        }
        return geoms
    }

    private fun isAtLimit(geoms: List<Geometry>): Boolean {
        if (limit < 0) return false
        return geoms.size >= limit
    }
}
