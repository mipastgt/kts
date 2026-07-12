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

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryCollection
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.LinearRing
import org.locationtech.jts.geom.MultiLineString
import org.locationtech.jts.geom.MultiPoint
import org.locationtech.jts.geom.MultiPolygon
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.Polygon

/**
 * Writes [Geometry]s as XML fragments in GML2 format.
 * Allows specifying the XML prefix, namespace and srsName of the emitted GML.
 * Also allows adding custom root elements to support GML extensions such as KML.
 *
 * This class does not rely on any external XML libraries.
 *
 * @author David Zwiers, Vivid Solutions
 * @author Martin Davis
 */
class GMLWriter {

    private var startingIndentIndex = 0
    private var maxCoordinatesPerLine = 10
    private var emitNamespace = false
    private var isRootTag = false

    private var prefix: String? = GMLConstants.GML_PREFIX
    private var namespace: String? = GMLConstants.GML_NAMESPACE
    private var srsName: String? = null

    private var customElements: Array<String>? = null

    /**
     * Creates a writer which outputs GML with default settings.
     */
    constructor()

    /**
     * Creates a writer which may emit the GML namespace prefix declaration in the geometry root
     * element.
     *
     * @param emitNamespace true if the GML namespace prefix declaration should be written in the
     *     geometry root element
     */
    constructor(emitNamespace: Boolean) {
        this.setNamespace(emitNamespace)
    }

    /**
     * Specifies the namespace prefix to write on each GML tag.
     * A null or blank prefix may be used to indicate no prefix.
     *
     * @param prefix the namespace prefix to use (`null` or blank if none)
     */
    fun setPrefix(prefix: String?) {
        this.prefix = prefix
    }

    /**
     * Sets the value of the `srsName` attribute to be written into the root geometry tag.
     *
     * @param srsName the srsName attribute value
     */
    fun setSrsName(srsName: String?) {
        this.srsName = srsName
    }

    /**
     * Determines whether a GML namespace declaration will be written in the opening tag of
     * geometries.
     *
     * @param emitNamespace true if the GML namespace prefix declaration should be written in the
     *     root geometry element
     */
    fun setNamespace(emitNamespace: Boolean) {
        this.emitNamespace = emitNamespace
    }

    /**
     * Specifies a list of custom elements which are written after the opening tag of the root
     * element.
     *
     * @param customElements a list of the custom element strings, or null if none
     */
    fun setCustomElements(customElements: Array<String>?) {
        this.customElements = customElements
    }

    /**
     * Sets the starting column index for pretty printing.
     */
    fun setStartingIndentIndex(indent: Int) {
        var i = indent
        if (i < 0) i = 0
        startingIndentIndex = i
    }

    /**
     * Sets the number of coordinates printed per line.
     */
    fun setMaxCoordinatesPerLine(num: Int) {
        if (num < 1) {
            throw IndexOutOfBoundsException("Invalid coordinate count per line, must be > 0")
        }
        maxCoordinatesPerLine = num
    }

    /**
     * Writes a [Geometry] in GML2 format to a String.
     *
     * @param geom the geometry to encode
     * @return String GML2 Encoded Geometry
     */
    fun write(geom: Geometry): String {
        val buf = StringBuilder()
        write(geom, buf, startingIndentIndex)
        return buf.toString()
    }

    private fun write(geom: Geometry, buf: StringBuilder, level: Int) {
        isRootTag = true
        when (geom) {
            is Point -> writePoint(geom, buf, level)
            is LineString -> writeLineString(geom, buf, level)
            is Polygon -> writePolygon(geom, buf, level)
            is MultiPoint -> writeMultiPoint(geom, buf, level)
            is MultiLineString -> writeMultiLineString(geom, buf, level)
            is MultiPolygon -> writeMultiPolygon(geom, buf, level)
            is GeometryCollection -> writeGeometryCollection(geom, buf, startingIndentIndex)
            else -> throw IllegalArgumentException("Unhandled geometry type: " + geom.getGeometryType())
        }
    }

    private fun writePoint(p: Point, buf: StringBuilder, level: Int) {
        startLine(level, buf)
        startGeomTag(GMLConstants.GML_POINT, p, buf)

        write(arrayOf(p.getCoordinate()!!), buf, level + 1)

        startLine(level, buf)
        endGeomTag(GMLConstants.GML_POINT, buf)
    }

    private fun writeLineString(ls: LineString, buf: StringBuilder, level: Int) {
        startLine(level, buf)
        startGeomTag(GMLConstants.GML_LINESTRING, ls, buf)

        write(ls.getCoordinates(), buf, level + 1)

        startLine(level, buf)
        endGeomTag(GMLConstants.GML_LINESTRING, buf)
    }

    private fun writeLinearRing(lr: LinearRing, buf: StringBuilder, level: Int) {
        startLine(level, buf)
        startGeomTag(GMLConstants.GML_LINEARRING, lr, buf)

        write(lr.getCoordinates(), buf, level + 1)

        startLine(level, buf)
        endGeomTag(GMLConstants.GML_LINEARRING, buf)
    }

    private fun writePolygon(p: Polygon, buf: StringBuilder, level: Int) {
        startLine(level, buf)
        startGeomTag(GMLConstants.GML_POLYGON, p, buf)

        startLine(level + 1, buf)
        startGeomTag(GMLConstants.GML_OUTER_BOUNDARY_IS, null, buf)

        writeLinearRing(p.getExteriorRing(), buf, level + 2)

        startLine(level + 1, buf)
        endGeomTag(GMLConstants.GML_OUTER_BOUNDARY_IS, buf)

        for (t in 0 until p.getNumInteriorRing()) {
            startLine(level + 1, buf)
            startGeomTag(GMLConstants.GML_INNER_BOUNDARY_IS, null, buf)

            writeLinearRing(p.getInteriorRingN(t), buf, level + 2)

            startLine(level + 1, buf)
            endGeomTag(GMLConstants.GML_INNER_BOUNDARY_IS, buf)
        }

        startLine(level, buf)
        endGeomTag(GMLConstants.GML_POLYGON, buf)
    }

    private fun writeMultiPoint(mp: MultiPoint, buf: StringBuilder, level: Int) {
        startLine(level, buf)
        startGeomTag(GMLConstants.GML_MULTI_POINT, mp, buf)

        for (t in 0 until mp.getNumGeometries()) {
            startLine(level + 1, buf)
            startGeomTag(GMLConstants.GML_POINT_MEMBER, null, buf)

            writePoint(mp.getGeometryN(t) as Point, buf, level + 2)

            startLine(level + 1, buf)
            endGeomTag(GMLConstants.GML_POINT_MEMBER, buf)
        }
        startLine(level, buf)
        endGeomTag(GMLConstants.GML_MULTI_POINT, buf)
    }

    private fun writeMultiLineString(mls: MultiLineString, buf: StringBuilder, level: Int) {
        startLine(level, buf)
        startGeomTag(GMLConstants.GML_MULTI_LINESTRING, mls, buf)

        for (t in 0 until mls.getNumGeometries()) {
            startLine(level + 1, buf)
            startGeomTag(GMLConstants.GML_LINESTRING_MEMBER, null, buf)

            writeLineString(mls.getGeometryN(t) as LineString, buf, level + 2)

            startLine(level + 1, buf)
            endGeomTag(GMLConstants.GML_LINESTRING_MEMBER, buf)
        }
        startLine(level, buf)
        endGeomTag(GMLConstants.GML_MULTI_LINESTRING, buf)
    }

    private fun writeMultiPolygon(mp: MultiPolygon, buf: StringBuilder, level: Int) {
        startLine(level, buf)
        startGeomTag(GMLConstants.GML_MULTI_POLYGON, mp, buf)

        for (t in 0 until mp.getNumGeometries()) {
            startLine(level + 1, buf)
            startGeomTag(GMLConstants.GML_POLYGON_MEMBER, null, buf)

            writePolygon(mp.getGeometryN(t) as Polygon, buf, level + 2)

            startLine(level + 1, buf)
            endGeomTag(GMLConstants.GML_POLYGON_MEMBER, buf)
        }
        startLine(level, buf)
        endGeomTag(GMLConstants.GML_MULTI_POLYGON, buf)
    }

    private fun writeGeometryCollection(gc: GeometryCollection, buf: StringBuilder, level: Int) {
        startLine(level, buf)
        startGeomTag(GMLConstants.GML_MULTI_GEOMETRY, gc, buf)

        for (t in 0 until gc.getNumGeometries()) {
            startLine(level + 1, buf)
            startGeomTag(GMLConstants.GML_GEOMETRY_MEMBER, null, buf)

            write(gc.getGeometryN(t), buf, level + 2)

            startLine(level + 1, buf)
            endGeomTag(GMLConstants.GML_GEOMETRY_MEMBER, buf)
        }
        startLine(level, buf)
        endGeomTag(GMLConstants.GML_MULTI_GEOMETRY, buf)
    }

    /**
     * Takes a list of coordinates and converts it to GML. 2d and 3d aware.
     *
     * @param coords array of coordinates
     */
    private fun write(coords: Array<Coordinate>, buf: StringBuilder, level: Int) {
        startLine(level, buf)
        startGeomTag(GMLConstants.GML_COORDINATES, null, buf)

        var dim = 2

        if (coords.isNotEmpty()) {
            if (!coords[0].getZ().isNaN()) {
                dim = 3
            }
        }

        var isNewLine = true
        for (i in coords.indices) {
            if (isNewLine) {
                startLine(level + 1, buf)
                isNewLine = false
            }
            if (dim == 2) {
                buf.append(coords[i].x)
                buf.append(COORDINATE_SEPARATOR)
                buf.append(coords[i].y)
            } else if (dim == 3) {
                buf.append(coords[i].x)
                buf.append(COORDINATE_SEPARATOR)
                buf.append(coords[i].y)
                buf.append(COORDINATE_SEPARATOR)
                buf.append(coords[i].getZ())
            }
            buf.append(TUPLE_SEPARATOR)

            // break output lines to prevent them from getting too long
            if ((i + 1) % maxCoordinatesPerLine == 0 && i < coords.size - 1) {
                buf.append("\n")
                isNewLine = true
            }
        }
        if (!isNewLine) {
            buf.append("\n")
        }

        startLine(level, buf)
        endGeomTag(GMLConstants.GML_COORDINATES, buf)
    }

    private fun startLine(level: Int, buf: StringBuilder) {
        for (i in 0 until level) {
            buf.append(INDENT)
        }
    }

    private fun startGeomTag(geometryName: String, g: Geometry?, buf: StringBuilder) {
        buf.append("<" + (if (prefix == null || "" == prefix) "" else prefix + ":"))
        buf.append(geometryName)
        writeAttributes(g, buf)
        buf.append(">\n")
        writeCustomElements(g, buf)
        isRootTag = false
    }

    private fun writeAttributes(geom: Geometry?, buf: StringBuilder) {
        if (geom == null) return
        if (!isRootTag) return

        if (emitNamespace) {
            buf.append(
                " xmlns" +
                    (if (prefix == null || "" == prefix) "" else ":" + prefix) +
                    "='" + namespace + "'",
            )
        }
        val srs = srsName
        if (srs != null && srs.isNotEmpty()) {
            buf.append(" " + GMLConstants.GML_ATTR_SRSNAME + "='" + srs + "'")
        }
    }

    private fun writeCustomElements(geom: Geometry?, buf: StringBuilder) {
        if (geom == null) return
        if (!isRootTag) return
        val elements = customElements ?: return

        for (i in elements.indices) {
            buf.append(elements[i])
            buf.append("\n")
        }
    }

    private fun endGeomTag(geometryName: String, buf: StringBuilder) {
        buf.append("</" + prefix())
        buf.append(geometryName)
        buf.append(">\n")
    }

    private fun prefix(): String {
        val p = prefix
        if (p == null || p.isEmpty()) {
            return ""
        }
        return p + ":"
    }

    companion object {
        private const val INDENT = "  "
        private const val COORDINATE_SEPARATOR = ","
        private const val TUPLE_SEPARATOR = " "
    }
}
