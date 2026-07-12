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

import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.io.ParseException

/**
 * A read-only view of the attributes of an element, matching the subset of the SAX `Attributes` API
 * the GML strategies use. Namespaces are ignored (as the original SAX-based reader disabled them):
 * attributes are looked up by qualified name, and the namespace-qualified overloads never match.
 */
internal class GmlAttributes(
    private val qNames: List<String>,
    private val values: List<String>,
) {
    fun getIndex(qName: String): Int = qNames.indexOf(qName)

    fun getValue(qName: String): String? {
        val i = getIndex(qName)
        return if (i >= 0) values[i] else null
    }

    // Namespaces are disabled, so the (uri, localName) form never matches (as with the original
    // SAXParserFactory.setNamespaceAware(false) behaviour).
    fun getIndex(uri: String, localName: String): Int = -1

    fun getValue(uri: String, localName: String): String? = null
}

/**
 * Builds [Geometry]s from GML2-formatted geometries. Originally a SAX `DefaultHandler`; it is now
 * fed element/character events by [GMLReader] (driven by an xmlutil pull parser) so it can be
 * multiplatform. Namespaces and prefixes are ignored.
 *
 * @author David Zwiers, Vivid Solutions.
 */
class GMLHandler(private val gf: GeometryFactory) {

    /**
     * Logs the parse activity within a given element until its termination. At that time a new
     * object of value is created and passed to the parent. An object of value is typically either a
     * `kotlin`/`java` value or a JTS Geometry. Not intended for use outside this distribution.
     */
    internal class Handler(val strategy: ParseStrategy?, val attrs: GmlAttributes?) {

        var text: StringBuilder? = null

        /** Caches text for the future. */
        fun addText(str: String) {
            if (text == null) {
                text = StringBuilder()
            }
            text!!.append(str)
        }

        var children: MutableList<Any?>? = null

        /** Stores a value for the future. */
        fun keep(obj: Any?) {
            if (children == null) {
                children = ArrayList()
            }
            children!!.add(obj)
        }

        @Throws(ParseException::class)
        fun create(gf: GeometryFactory): Any? = strategy!!.parse(this, gf)
    }

    private val stack = ArrayDeque<Handler>()

    init {
        stack.addLast(Handler(null, null))
    }

    /**
     * Tests whether this handler has completed parsing a geometry.
     * If so, [getGeometry] can be called to get the value of the parsed geometry.
     *
     * @return if the parsing of the geometry is complete
     */
    fun isGeometryComplete(): Boolean {
        if (stack.size > 1) {
            return false
        }
        // top level node on stack needs to have at least one child
        val h = stack.last()
        if (h.children!!.size < 1) {
            return false
        }
        return true
    }

    /**
     * Gets the geometry parsed by this handler.
     * This method should only be called AFTER the parse has completed.
     *
     * @return the parsed Geometry, or a GeometryCollection if more than one geometry was parsed
     * @throws IllegalStateException if called before the parse is complete
     */
    fun getGeometry(): Geometry {
        if (stack.size == 1) {
            val h = stack.last()
            val children = h.children!!
            if (children.size == 1) {
                return children[0] as Geometry
            }
            return gf.createGeometryCollection(Array(children.size) { children[it] as Geometry })
        }
        throw IllegalStateException(
            "Parse did not complete as expected, there are ${stack.size} elements on the Stack",
        )
    }

    // Parsing methods, fed by GMLReader.

    internal fun characters(text: String) {
        if (stack.isNotEmpty()) {
            stack.last().addText(text)
        }
    }

    @Throws(ParseException::class)
    internal fun endElement() {
        val thisAction = stack.removeLast()
        stack.last().keep(thisAction.create(gf))
    }

    internal fun startElement(localName: String, attributes: GmlAttributes) {
        // The original stripped the namespace prefix from the qName; the pull parser already reports
        // the prefix-stripped local name.
        val ps = GeometryStrategies.findStrategy(null, localName)
        stack.addLast(Handler(ps, attributes))
    }
}
