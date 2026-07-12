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
import org.locationtech.jts.geom.CoordinateSequence
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.LinearRing
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.Polygon
import org.locationtech.jts.io.ParseException
import org.locationtech.jts.io.gml2.GMLHandler.Handler
import org.locationtech.jts.util.StringUtil

/**
 * A parse strategy associated with a GML element when the element begins; it is used at the end of
 * the element to create an object of value (either a `kotlin`/`java` value or a JTS Geometry).
 */
internal fun interface ParseStrategy {
    /**
     * @param arg value to interpret
     * @param gf GeometryFactory
     * @return the interpreted value
     */
    @Throws(ParseException::class)
    fun parse(arg: Handler, gf: GeometryFactory): Any?
}

/**
 * Container for GML2 Geometry parsing strategies which can be represented in JTS.
 *
 * @author David Zwiers, Vivid Solutions.
 */
internal object GeometryStrategies {

    private val strategies = loadStrategies()

    private fun loadStrategies(): HashMap<String, ParseStrategy> {
        val strats = HashMap<String, ParseStrategy>()

        // point
        strats[GMLConstants.GML_POINT.lowercase()] = ParseStrategy { arg, gf ->
            // one child, either a coord or a coordinate sequence
            if (arg.children!!.size != 1) {
                throw ParseException("Cannot create a point without exactly one coordinate")
            }

            val srid = getSrid(arg.attrs, gf.getSRID())

            val c = arg.children!![0]
            val p: Point = if (c is Coordinate) {
                gf.createPoint(c)
            } else {
                gf.createPoint(c as CoordinateSequence)
            }
            if (p.getSRID() != srid) {
                p.setSRID(srid)
            }
            p
        }

        // linestring
        strats[GMLConstants.GML_LINESTRING.lowercase()] = ParseStrategy { arg, gf ->
            if (arg.children!!.size < 1) {
                throw ParseException("Cannot create a linestring without atleast two coordinates or one coordinate sequence")
            }

            val srid = getSrid(arg.attrs, gf.getSRID())

            val ls: LineString
            if (arg.children!!.size == 1) {
                // coord set
                try {
                    val cs = arg.children!![0] as CoordinateSequence
                    ls = gf.createLineString(cs)
                } catch (e: ClassCastException) {
                    throw ParseException("Cannot create a linestring without atleast two coordinates or one coordinate sequence", e)
                }
            } else {
                try {
                    val coords = Array(arg.children!!.size) { arg.children!![it] as Coordinate }
                    ls = gf.createLineString(coords)
                } catch (e: ClassCastException) {
                    throw ParseException("Cannot create a linestring without atleast two coordinates or one coordinate sequence", e)
                }
            }

            if (ls.getSRID() != srid) {
                ls.setSRID(srid)
            }
            ls
        }

        // linearring
        strats[GMLConstants.GML_LINEARRING.lowercase()] = ParseStrategy { arg, gf ->
            if (arg.children!!.size != 1 && arg.children!!.size < 4) {
                throw ParseException("Cannot create a linear ring without atleast four coordinates or one coordinate sequence")
            }

            val srid = getSrid(arg.attrs, gf.getSRID())

            val ls: LinearRing
            if (arg.children!!.size == 1) {
                try {
                    val cs = arg.children!![0] as CoordinateSequence
                    ls = gf.createLinearRing(cs)
                } catch (e: ClassCastException) {
                    throw ParseException("Cannot create a linear ring without atleast four coordinates or one coordinate sequence", e)
                }
            } else {
                try {
                    val coords = Array(arg.children!!.size) { arg.children!![it] as Coordinate }
                    ls = gf.createLinearRing(coords)
                } catch (e: ClassCastException) {
                    throw ParseException("Cannot create a linear ring without atleast four coordinates or one coordinate sequence", e)
                }
            }

            if (ls.getSRID() != srid) {
                ls.setSRID(srid)
            }
            ls
        }

        // polygon
        strats[GMLConstants.GML_POLYGON.lowercase()] = ParseStrategy { arg, gf ->
            if (arg.children!!.size < 1) {
                throw ParseException("Cannot create a polygon without atleast one linear ring")
            }

            val srid = getSrid(arg.attrs, gf.getSRID())

            val outer = arg.children!![0] as LinearRing // will be the first
            val children = arg.children!!
            val inner: Array<LinearRing>? =
                if (children.size > 1) Array(children.size - 1) { children[it + 1] as LinearRing } else null

            val p = gf.createPolygon(outer, inner)

            if (p.getSRID() != srid) {
                p.setSRID(srid)
            }
            p
        }

        // box
        strats[GMLConstants.GML_BOX.lowercase()] = ParseStrategy { arg, gf ->
            if (arg.children!!.size < 1 || arg.children!!.size > 2) {
                throw ParseException("Cannot create a box without either two coords or one coordinate sequence")
            }

            val box: Envelope
            if (arg.children!!.size == 1) {
                val cs = arg.children!![0] as CoordinateSequence
                box = cs.expandEnvelope(Envelope())
            } else {
                box = Envelope(arg.children!![0] as Coordinate, arg.children!![1] as Coordinate)
            }
            box
        }

        // multi-point
        strats[GMLConstants.GML_MULTI_POINT.lowercase()] = ParseStrategy { arg, gf ->
            if (arg.children!!.size < 1) {
                throw ParseException("Cannot create a multi-point without atleast one point")
            }

            val srid = getSrid(arg.attrs, gf.getSRID())

            val pts = Array(arg.children!!.size) { arg.children!![it] as Point }
            val mp = gf.createMultiPoint(pts)

            if (mp.getSRID() != srid) {
                mp.setSRID(srid)
            }
            mp
        }

        // multi-linestring
        strats[GMLConstants.GML_MULTI_LINESTRING.lowercase()] = ParseStrategy { arg, gf ->
            if (arg.children!!.size < 1) {
                throw ParseException("Cannot create a multi-linestring without atleast one linestring")
            }

            val srid = getSrid(arg.attrs, gf.getSRID())

            val lns = Array(arg.children!!.size) { arg.children!![it] as LineString }
            val mp = gf.createMultiLineString(lns)

            if (mp.getSRID() != srid) {
                mp.setSRID(srid)
            }
            mp
        }

        // multi-poly
        strats[GMLConstants.GML_MULTI_POLYGON.lowercase()] = ParseStrategy { arg, gf ->
            if (arg.children!!.size < 1) {
                throw ParseException("Cannot create a multi-polygon without atleast one polygon")
            }

            val srid = getSrid(arg.attrs, gf.getSRID())

            val plys = Array(arg.children!!.size) { arg.children!![it] as Polygon }
            val mp = gf.createMultiPolygon(plys)

            if (mp.getSRID() != srid) {
                mp.setSRID(srid)
            }
            mp
        }

        // multi-geom
        strats[GMLConstants.GML_MULTI_GEOMETRY.lowercase()] = ParseStrategy { arg, gf ->
            if (arg.children!!.size < 1) {
                throw ParseException("Cannot create a multi-polygon without atleast one geometry")
            }

            val geoms = Array(arg.children!!.size) { arg.children!![it] as Geometry }
            gf.createGeometryCollection(geoms)
        }

        // coordinates
        strats[GMLConstants.GML_COORDINATES.lowercase()] = ParseStrategy { arg, gf ->
            if (arg.text == null || arg.text!!.length == 0) {
                throw ParseException("Cannot create a coordinate sequence without text to parse")
            }

            var decimal = "."
            var coordSeperator = ","
            var toupleSeperator = " "

            // get overrides from coordinates attributes
            val attrs = arg.attrs!!
            if (attrs.getIndex("decimal") >= 0) {
                decimal = attrs.getValue("decimal")!!
            } else if (attrs.getIndex(GMLConstants.GML_NAMESPACE, "decimal") >= 0) {
                decimal = attrs.getValue(GMLConstants.GML_NAMESPACE, "decimal")!!
            }

            if (attrs.getIndex("cs") >= 0) {
                coordSeperator = attrs.getValue("cs")!!
            } else if (attrs.getIndex(GMLConstants.GML_NAMESPACE, "cs") >= 0) {
                coordSeperator = attrs.getValue(GMLConstants.GML_NAMESPACE, "cs")!!
            }

            if (attrs.getIndex("ts") >= 0) {
                toupleSeperator = attrs.getValue("ts")!!
            } else if (attrs.getIndex(GMLConstants.GML_NAMESPACE, "ts") >= 0) {
                toupleSeperator = attrs.getValue(GMLConstants.GML_NAMESPACE, "ts")!!
            }

            // now to start parse
            var t = arg.text.toString()
            t = t.replace(Regex("\\s"), " ")
            // Remove spaces after commas, for when they are used as separators (default).
            // This prevents coordinates being split by the tuple separator
            t = t.replace(Regex("\\s*,\\s*"), ",")

            val touplesRaw = Regex(escapeSeparator(toupleSeperator)).split(t.trim())

            if (touplesRaw.isEmpty()) {
                throw ParseException("Cannot create a coordinate sequence without a touple to parse")
            }

            val touples = touplesRaw.filter { it.trim() != "" }
            val numNonNullTouples = touples.size

            if (numNonNullTouples == 0) {
                throw ParseException("Cannot create a coordinate sequence without a non-null touple to parse")
            }

            var dim = StringUtil.split(touples[0], coordSeperator).size
            val cs = gf.getCoordinateSequenceFactory().create(numNonNullTouples, dim)
            dim = cs.getDimension() // max dim

            val replaceDec = "." != decimal
            val coordPattern = Regex(escapeSeparator(coordSeperator))
            val decimalPattern = if (replaceDec) Regex(decimal) else null

            for (i in 0 until numNonNullTouples) {
                // for each touple, split, parse, add
                val coords = coordPattern.split(touples[i])

                var dimIndex = 0
                var j = 0
                while (j < coords.size && j < dim) {
                    val cj = coords[j]
                    if (cj.trim() != "") {
                        val ordinate = (if (replaceDec) cj.replace(decimalPattern!!, ".") else cj).toDouble()
                        cs.setOrdinate(i, dimIndex++, ordinate)
                    }
                    j++
                }
                // fill remaining dim
                while (dimIndex < dim) {
                    cs.setOrdinate(i, dimIndex++, Double.NaN)
                }
            }

            cs
        }

        // coord
        strats[GMLConstants.GML_COORD.lowercase()] = ParseStrategy { arg, gf ->
            if (arg.children!!.size < 1) {
                throw ParseException("Cannot create a coordinate without atleast one axis")
            }
            if (arg.children!!.size > 3) {
                throw ParseException("Cannot create a coordinate with more than 3 axis")
            }

            val axis = Array(arg.children!!.size) { arg.children!![it] as Double }
            val c = Coordinate()
            c.x = axis[0]
            if (axis.size > 1) {
                c.y = axis[1]
            }
            if (axis.size > 2) {
                c.setZ(axis[2])
            }
            c
        }

        val coordChild = ParseStrategy { arg, _ ->
            if (arg.text == null) {
                null
            } else {
                arg.text.toString().toDouble()
            }
        }

        // coord-x / coord-y / coord-z
        strats[GMLConstants.GML_COORD_X.lowercase()] = coordChild
        strats[GMLConstants.GML_COORD_Y.lowercase()] = coordChild
        strats[GMLConstants.GML_COORD_Z.lowercase()] = coordChild

        val member = ParseStrategy { arg, _ ->
            if (arg.children!!.size != 1) {
                throw ParseException("Geometry Members may only contain one geometry.")
            }
            // type checking will occur in the parent geom collection.
            arg.children!![0]
        }
        // outerBoundary / innerBoundary - linear ring member
        strats[GMLConstants.GML_OUTER_BOUNDARY_IS.lowercase()] = member
        strats[GMLConstants.GML_INNER_BOUNDARY_IS.lowercase()] = member
        // point / line string / polygon member
        strats[GMLConstants.GML_POINT_MEMBER.lowercase()] = member
        strats[GMLConstants.GML_LINESTRING_MEMBER.lowercase()] = member
        strats[GMLConstants.GML_POLYGON_MEMBER.lowercase()] = member

        return strats
    }

    /**
     * Escapes a separator string so it can be used as a regular expression: backslashes and dots are
     * escaped. Reproduces the original `Pattern.compile` preparation.
     */
    private fun escapeSeparator(sep: String): String {
        var ts = sep
        if (ts.contains('\\')) ts = ts.replace("\\", "\\\\")
        if (ts.contains('.')) ts = ts.replace(".", "\\.")
        return ts
    }

    fun getSrid(attrs: GmlAttributes?, defaultValue: Int): Int {
        var srs: String? = null
        if (attrs!!.getIndex(GMLConstants.GML_ATTR_SRSNAME) >= 0) {
            srs = attrs.getValue(GMLConstants.GML_ATTR_SRSNAME)
        } else if (attrs.getIndex(GMLConstants.GML_NAMESPACE, GMLConstants.GML_ATTR_SRSNAME) >= 0) {
            srs = attrs.getValue(GMLConstants.GML_NAMESPACE, GMLConstants.GML_ATTR_SRSNAME)
        }

        if (srs != null) {
            srs = srs.trim()
            if (srs != "") {
                try {
                    return srs.toInt()
                } catch (e: NumberFormatException) {
                    val srsNum = extractIntSuffix(srs)
                    if (srsNum != null) {
                        try {
                            return srsNum.toInt()
                        } catch (e2: NumberFormatException) {
                            // ignore
                        }
                    }
                }
            }
        }

        return defaultValue
    }

    private val PATT_SUFFIX_INT = Regex("(\\d+)$")

    fun extractIntSuffix(s: String): String? {
        val matcher = PATT_SUFFIX_INT.find(s)
        return matcher?.groupValues?.get(1)
    }

    /**
     * The ParseStrategy which should be employed.
     *
     * @param uri Not currently used, included for future work
     * @param localName Used to look up an appropriate parse strategy
     * @return The ParseStrategy which should be employed
     */
    fun findStrategy(uri: String?, localName: String?): ParseStrategy? {
        return if (localName == null) null else strategies[localName.lowercase()]
    }
}
