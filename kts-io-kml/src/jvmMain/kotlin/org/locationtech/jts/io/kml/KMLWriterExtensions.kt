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
@file:JvmName("KMLWriterExtensions")

package org.locationtech.jts.io.kml

import org.locationtech.jts.geom.Geometry
import java.io.IOException
import java.io.Writer

/**
 * Writes the KML representation of a [Geometry] to a [Writer]. JVM-only convenience preserving the
 * historical `KMLWriter.write(Geometry, Writer)` member (a common class cannot take a
 * `java.io.Writer`). From Java, call `KMLWriterExtensions.write(kmlWriter, geometry, writer)`.
 *
 * @throws IOException if an I/O error occurred
 */
@Throws(IOException::class)
fun KMLWriter.write(geometry: Geometry, writer: Writer) {
    writer.write(write(geometry))
}
