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
package org.locationtech.jts.io.kml

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryCollection
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.LinearRing
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.Polygon
import org.locationtech.jts.io.OrdinateFormat
import org.locationtech.jts.util.StringUtil
import kotlin.jvm.JvmField
import kotlin.jvm.JvmStatic

/**
 * Writes a formatted string containing the KML representation of a JTS [Geometry].
 * The output is KML fragments which can be substituted wherever the KML *Geometry* abstract element
 * can be used.
 *
 * Output elements are indented to provide a nicely-formatted representation. An output line prefix
 * and maximum number of coordinates per line can be specified.
 *
 * The Z ordinate value output can be forced to be a specific value. The `extrude` and
 * `altitudeMode` modes can be set. If set, the corresponding sub-elements will be output.
 */
class KMLWriter {

    private var linePrefix: String? = null
    private var maxCoordinatesPerLine = 5
    private var zVal = Double.NaN
    private var extrude = false
    private var tesselate = false
    private var altitudeMode: String? = null
    private var numberFormatter: OrdinateFormat? = null

    /**
     * Sets a tag string which is prefixed to every emitted text line.
     * This can be used to indent the geometry text in a containing document.
     *
     * @param linePrefix the tag string
     */
    fun setLinePrefix(linePrefix: String?) {
        this.linePrefix = linePrefix
    }

    /**
     * Sets the maximum number of coordinates to output per line.
     *
     * @param maxCoordinatesPerLine the maximum number of coordinates to output
     */
    fun setMaximumCoordinatesPerLine(maxCoordinatesPerLine: Int) {
        var max = maxCoordinatesPerLine
        if (max <= 0) {
            max = 1
            return
        }
        this.maxCoordinatesPerLine = max
    }

    /**
     * Sets the Z value to be output for all coordinates.
     * This overrides any Z value present in the Geometry coordinates.
     *
     * @param zVal the Z value to output
     */
    fun setZ(zVal: Double) {
        this.zVal = zVal
    }

    /**
     * Sets the flag to be output in the `extrude` element.
     *
     * @param extrude the extrude flag to output
     */
    fun setExtrude(extrude: Boolean) {
        this.extrude = extrude
    }

    /**
     * Sets the flag to be output in the `tesselate` element.
     *
     * @param tesselate the tesselate flag to output
     */
    fun setTesselate(tesselate: Boolean) {
        this.tesselate = tesselate
    }

    /**
     * Sets the value output in the `altitudeMode` element.
     *
     * @param altitudeMode string representing the altitude mode
     */
    fun setAltitudeMode(altitudeMode: String?) {
        this.altitudeMode = altitudeMode
    }

    /**
     * Sets the maximum number of decimal places to output in ordinate values.
     * Useful for limiting output size.
     *
     * @param precision the number of decimal places to output
     */
    fun setPrecision(precision: Int) {
        if (precision >= 0) {
            numberFormatter = createFormatter(precision)
        }
    }

    /**
     * Writes a [Geometry] in KML format as a string.
     *
     * @param geom the geometry to write
     * @return a string containing the KML geometry representation
     */
    fun write(geom: Geometry): String {
        val buf = StringBuilder()
        write(geom, buf)
        return buf.toString()
    }

    /**
     * Appends the KML representation of a [Geometry] to a [StringBuilder].
     *
     * @param geometry the geometry to write
     * @param buf the buffer to write into
     */
    fun write(geometry: Geometry, buf: StringBuilder) {
        writeGeometry(geometry, 0, buf)
    }

    private fun writeGeometry(g: Geometry, level: Int, buf: StringBuilder) {
        val attributes = ""
        when (g) {
            is Point -> writePoint(g, attributes, level, buf)
            is LinearRing -> writeLinearRing(g, attributes, true, level, buf)
            is LineString -> writeLineString(g, attributes, level, buf)
            is Polygon -> writePolygon(g, attributes, level, buf)
            is GeometryCollection -> writeGeometryCollection(g, attributes, level, buf)
            else -> throw IllegalArgumentException("Geometry type not supported: " + g.getGeometryType())
        }
    }

    private fun startLine(text: String, level: Int, buf: StringBuilder) {
        if (linePrefix != null) {
            buf.append(linePrefix)
        }
        buf.append(StringUtil.spaces(INDENT_SIZE * level))
        buf.append(text)
    }

    private fun geometryTag(geometryName: String, attributes: String?): String {
        val buf = StringBuilder()
        buf.append("<")
        buf.append(geometryName)
        if (attributes != null && attributes.isNotEmpty()) {
            buf.append(" ")
            buf.append(attributes)
        }
        buf.append(">")
        return buf.toString()
    }

    private fun writeModifiers(level: Int, buf: StringBuilder) {
        if (extrude) {
            startLine("<extrude>1</extrude>\n", level, buf)
        }
        if (tesselate) {
            startLine("<tesselate>1</tesselate>\n", level, buf)
        }
        if (altitudeMode != null) {
            startLine("<altitudeMode>" + altitudeMode + "</altitudeMode>\n", level, buf)
        }
    }

    private fun writePoint(p: Point, attributes: String, level: Int, buf: StringBuilder) {
        // <Point><coordinates>...</coordinates></Point>
        startLine(geometryTag("Point", attributes) + "\n", level, buf)
        writeModifiers(level, buf)
        write(arrayOf(p.getCoordinate()!!), level + 1, buf)
        startLine("</Point>\n", level, buf)
    }

    private fun writeLineString(ls: LineString, attributes: String, level: Int, buf: StringBuilder) {
        // <LineString><coordinates>...</coordinates></LineString>
        startLine(geometryTag("LineString", attributes) + "\n", level, buf)
        writeModifiers(level, buf)
        write(ls.getCoordinates(), level + 1, buf)
        startLine("</LineString>\n", level, buf)
    }

    private fun writeLinearRing(
        lr: LinearRing,
        attributes: String?,
        writeModifiers: Boolean,
        level: Int,
        buf: StringBuilder,
    ) {
        // <LinearRing><coordinates>...</coordinates></LinearRing>
        startLine(geometryTag("LinearRing", attributes) + "\n", level, buf)
        if (writeModifiers) writeModifiers(level, buf)
        write(lr.getCoordinates(), level + 1, buf)
        startLine("</LinearRing>\n", level, buf)
    }

    private fun writePolygon(p: Polygon, attributes: String, level: Int, buf: StringBuilder) {
        startLine(geometryTag("Polygon", attributes) + "\n", level, buf)
        writeModifiers(level, buf)

        startLine("  <outerBoundaryIs>\n", level, buf)
        writeLinearRing(p.getExteriorRing(), null, false, level + 1, buf)
        startLine("  </outerBoundaryIs>\n", level, buf)

        for (t in 0 until p.getNumInteriorRing()) {
            startLine("  <innerBoundaryIs>\n", level, buf)
            writeLinearRing(p.getInteriorRingN(t), null, false, level + 1, buf)
            startLine("  </innerBoundaryIs>\n", level, buf)
        }

        startLine("</Polygon>\n", level, buf)
    }

    private fun writeGeometryCollection(gc: GeometryCollection, attributes: String, level: Int, buf: StringBuilder) {
        startLine("<MultiGeometry>\n", level, buf)
        for (t in 0 until gc.getNumGeometries()) {
            writeGeometry(gc.getGeometryN(t), level + 1, buf)
        }
        startLine("</MultiGeometry>\n", level, buf)
    }

    /**
     * Takes a list of coordinates and converts it to KML.
     * 2d and 3d aware. Terminates the coordinate output with a newline.
     *
     * @param coords array of coordinates
     */
    private fun write(coords: Array<Coordinate>, level: Int, buf: StringBuilder) {
        startLine("<coordinates>", level, buf)

        var isNewLine = false
        for (i in coords.indices) {
            if (i > 0) {
                buf.append(TUPLE_SEPARATOR)
            }

            if (isNewLine) {
                startLine("  ", level, buf)
                isNewLine = false
            }

            write(coords[i], buf)

            // break output lines to prevent them from getting too long
            if ((i + 1) % maxCoordinatesPerLine == 0 && i < coords.size - 1) {
                buf.append("\n")
                isNewLine = true
            }
        }
        buf.append("</coordinates>\n")
    }

    private fun write(p: Coordinate, buf: StringBuilder) {
        write(p.x, buf)
        buf.append(COORDINATE_SEPARATOR)
        write(p.y, buf)

        var z = p.getZ()
        // if altitude was specified directly, use it
        if (!zVal.isNaN()) {
            z = zVal
        }

        // only write if Z present
        // MD - is this right? Or should it always be written?
        if (!z.isNaN()) {
            buf.append(COORDINATE_SEPARATOR)
            write(z, buf)
        }
    }

    private fun write(num: Double, buf: StringBuilder) {
        val formatter = numberFormatter
        if (formatter != null) {
            buf.append(formatter.format(num))
        } else {
            buf.append(num)
        }
    }

    companion object {
        /** The KML standard value `clampToGround` for use in [setAltitudeMode]. */
        @JvmField
        var ALTITUDE_MODE_CLAMPTOGROUND: String = "clampToGround "

        /** The KML standard value `relativeToGround` for use in [setAltitudeMode]. */
        @JvmField
        var ALTITUDE_MODE_RELATIVETOGROUND: String = "relativeToGround  "

        /** The KML standard value `absolute` for use in [setAltitudeMode]. */
        @JvmField
        var ALTITUDE_MODE_ABSOLUTE: String = "absolute"

        private const val INDENT_SIZE = 2
        private const val COORDINATE_SEPARATOR = ","
        private const val TUPLE_SEPARATOR = " "

        /**
         * Writes a Geometry as KML to a string, using a specified Z value.
         *
         * @param geometry the geometry to write
         * @param z the Z value to use
         * @return a string containing the KML geometry representation
         */
        @JvmStatic
        fun writeGeometry(geometry: Geometry, z: Double): String {
            val writer = KMLWriter()
            writer.setZ(z)
            return writer.write(geometry)
        }

        /**
         * Writes a Geometry as KML to a string, using a specified Z value, precision, extrude flag,
         * and altitude mode code.
         *
         * @param geometry the geometry to write
         * @param z the Z value to use
         * @param precision the maximum number of decimal places to write
         * @param extrude the extrude flag to write
         * @param altitudeMode the altitude model code to write
         * @return a string containing the KML geometry representation
         */
        @JvmStatic
        fun writeGeometry(
            geometry: Geometry,
            z: Double,
            precision: Int,
            extrude: Boolean,
            altitudeMode: String?,
        ): String {
            val writer = KMLWriter()
            writer.setZ(z)
            writer.setPrecision(precision)
            writer.setExtrude(extrude)
            writer.setAltitudeMode(altitudeMode)
            return writer.write(geometry)
        }

        /**
         * Creates the number formatter used to write `double`s with a sufficient number of decimal
         * places, without scientific notation. Reuses core's [OrdinateFormat] (locale-independent
         * `.`-separated, `HALF_EVEN`, no exponent), which matches the former `DecimalFormat`
         * (`"0." + "#" * precision`) output.
         */
        private fun createFormatter(precision: Int): OrdinateFormat = OrdinateFormat.create(precision)
    }
}
