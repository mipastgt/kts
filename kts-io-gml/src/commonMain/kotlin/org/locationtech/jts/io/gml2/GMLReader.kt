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
package org.locationtech.jts.io.gml2

import nl.adaptivity.xmlutil.EventType
import nl.adaptivity.xmlutil.XmlException
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.XmlUtilInternal
import nl.adaptivity.xmlutil.core.KtXmlReader
import nl.adaptivity.xmlutil.core.impl.multiplatform.StringReader
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryCollection
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel
import org.locationtech.jts.io.ParseException

/**
 * Reads a GML2 geometry from an XML fragment into a [Geometry].
 *
 * The reader ignores namespace prefixes. The XML is parsed with a pull parser (xmlutil `KtXmlReader`
 * in *relaxed* mode, which — like the original SAX reader with namespace processing disabled —
 * tolerates the undeclared `gml:` prefixes commonly used in GML fragments), so this reader is
 * multiplatform.
 *
 * It is the caller's responsibility to ensure that the supplied [PrecisionModel] matches the
 * precision of the incoming data.
 *
 * @author David Zwiers, Vivid Solutions.
 * @see GMLHandler
 */
class GMLReader {

    /**
     * Reads a GML2 Geometry from a `String` into a single [Geometry].
     *
     * If a collection of geometries is found, a [GeometryCollection] is returned.
     *
     * @param gml the GML String to parse
     * @param geometryFactory when null, a default will be used.
     * @return the resulting JTS Geometry
     * @throws ParseException if a parsing problem occurs
     */
    // KtXmlReader in relaxed mode is the only way to accept the undeclared `gml:` prefixes GML
    // fragments use (see the compat doc); the relaxed constructor + multiplatform StringReader are
    // marked internal to xmlutil, so we opt in.
    @OptIn(XmlUtilInternal::class)
    @Throws(ParseException::class)
    fun read(gml: String, geometryFactory: GeometryFactory?): Geometry {
        val gf = geometryFactory ?: GeometryFactory()
        val gh = GMLHandler(gf)
        try {
            val reader = KtXmlReader(StringReader(gml), relaxed = true)
            while (reader.hasNext()) {
                when (reader.next()) {
                    EventType.START_ELEMENT -> gh.startElement(reader.localName, buildAttributes(reader))
                    EventType.TEXT, EventType.CDSECT -> gh.characters(reader.text)
                    EventType.IGNORABLE_WHITESPACE -> gh.characters(" ")
                    EventType.END_ELEMENT -> gh.endElement()
                    else -> {}
                }
            }
        } catch (e: XmlException) {
            throw ParseException(e.toString())
        }

        return gh.getGeometry()
    }

    private fun buildAttributes(reader: XmlReader): GmlAttributes {
        val n = reader.attributeCount
        val qNames = ArrayList<String>(n)
        val values = ArrayList<String>(n)
        for (i in 0 until n) {
            val prefix = reader.getAttributePrefix(i)
            val localName = reader.getAttributeLocalName(i)
            val qName = if (prefix.isEmpty()) localName else "$prefix:$localName"
            qNames.add(qName)
            values.add(reader.getAttributeValue(i))
        }
        return GmlAttributes(qNames, values)
    }
}
