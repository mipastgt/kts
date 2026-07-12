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

/**
 * Reads a sequence of [Geometry]s in WKB-hex format from a text file.
 * Each WKB-hex geometry must be on a single line.
 * The geometries in the file may be separated by any amount of whitespace and newlines.
 *
 * This is the multiplatform (`jts-io-files`) successor to the JVM-only `WKBHexFileReader` that
 * formerly lived in `jts-io-wkt`. On the JVM the historical `java.io.File`/`java.io.Reader`
 * constructors are preserved; on all platforms a filesystem path can be read via kotlinx-io.
 *
 * @author Martin Davis
 */
expect class WKBHexFileReader {
    /**
     * Creates a new `WKBHexFileReader`, given the name of the file to read from.
     *
     * @param filename the name of the file to read from
     * @param wkbReader the geometry reader to use
     */
    constructor(filename: String, wkbReader: org.locationtech.jts.io.WKBReader)

    /**
     * Sets the maximum number of geometries to read.
     *
     * @param limit the maximum number of geometries to read
     */
    fun setLimit(limit: Int)

    /**
     * Sets the number of geometries to skip before storing.
     *
     * @param offset the number of geometries to skip
     */
    fun setOffset(offset: Int)

    /**
     * Reads a sequence of geometries.
     * If an offset is specified, geometries read up to the offset count are skipped.
     * If a limit is specified, no more than `limit` geometries are read.
     *
     * @return the list of geometries read
     * @throws ParseException if an error occurred reading a geometry
     */
    @Throws(ParseException::class)
    fun read(): List<Geometry>
}
