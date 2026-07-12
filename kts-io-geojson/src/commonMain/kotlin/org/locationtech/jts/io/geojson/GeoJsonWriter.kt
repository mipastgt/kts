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
package org.locationtech.jts.io.geojson

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonUnquotedLiteral
import org.locationtech.jts.geom.CoordinateSequence
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryCollection
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.MultiLineString
import org.locationtech.jts.geom.MultiPoint
import org.locationtech.jts.geom.MultiPolygon
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.Polygon
import kotlin.jvm.JvmOverloads
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.pow

/**
 * Writes [Geometry]s as JSON fragments in GeoJSON format.
 *
 * The current GeoJSON specification is
 * [RFC 7946](https://tools.ietf.org/html/rfc7946).
 *
 * The GeoJSON specification states that polygons should be emitted using
 * the counter-clockwise shell orientation. This is not enforced by this writer.
 *
 * The GeoJSON specification does not state how to represent empty geometries of specific type.
 * The writer emits empty typed geometries using an empty array for the `coordinates` property.
 *
 * @author Martin Davis
 * @author Paul Howells, Vivid Solutions
 *
 * @constructor Constructs a GeoJsonWriter instance specifying the number of decimals to
 *   use when encoding floating point numbers.
 * @param decimals the number of decimal places to output
 */
@OptIn(ExperimentalSerializationApi::class)
class GeoJsonWriter @JvmOverloads constructor(decimals: Int = 8) {

    private val scale: Double = 10.0.pow(decimals)

    /**
     * Sets whether the GeoJSON `crs` property should be output.
     * The value of the property is taken from geometry SRID.
     */
    var isEncodeCRS: Boolean = true

    /**
     * Sets whether the GeoJSON should be output following counter-clockwise orientation aka Right
     * Hand Rule defined in RFC7946. See
     * [RFC 7946 Specification](https://tools.ietf.org/html/rfc7946#section-3.1.6) for more context.
     */
    var isForceCCW: Boolean = false

    /**
     * Writes a [Geometry] in GeoJson format to a String.
     *
     * @param geometry the geometry to write
     * @return String GeoJson Encoded Geometry
     */
    fun write(geometry: Geometry): String {
        val obj = create(geometry, isEncodeCRS)
        return jsonFormat.encodeToString(JsonObject.serializer(), obj)
    }

    private fun create(geometry: Geometry, encodeCRS: Boolean): JsonObject {
        val result = LinkedHashMap<String, JsonElement>()
        result[GeoJsonConstants.NAME_TYPE] = JsonPrimitive(geometry.getGeometryType())

        when (geometry) {
            is Point -> {
                val coordinateSequence = geometry.getCoordinateSequence()
                val jsonString = if (coordinateSequence.size() == 0) {
                    JSON_ARRAY_EMPTY
                } else {
                    getJsonString(coordinateSequence)
                }
                result[GeoJsonConstants.NAME_COORDINATES] = JsonUnquotedLiteral(jsonString)
            }

            is LineString -> {
                val coordinateSequence = geometry.getCoordinateSequence()
                val jsonString = if (coordinateSequence.size() == 0) {
                    JSON_ARRAY_EMPTY
                } else {
                    getJsonString(coordinateSequence)
                }
                result[GeoJsonConstants.NAME_COORDINATES] = JsonUnquotedLiteral(jsonString)
            }

            is Polygon -> {
                val polygon = if (isForceCCW) {
                    OrientationTransformer.transformCCW(geometry)
                } else {
                    geometry
                }
                result[GeoJsonConstants.NAME_COORDINATES] = makeJsonAware(polygon)
            }

            is MultiPoint -> {
                result[GeoJsonConstants.NAME_COORDINATES] = makeJsonAware(geometry)
            }

            is MultiLineString -> {
                result[GeoJsonConstants.NAME_COORDINATES] = makeJsonAware(geometry)
            }

            is MultiPolygon -> {
                val multiPolygon = if (isForceCCW) {
                    OrientationTransformer.transformCCW(geometry) as MultiPolygon
                } else {
                    geometry
                }
                result[GeoJsonConstants.NAME_COORDINATES] = makeJsonAware(multiPolygon)
            }

            is GeometryCollection -> {
                val geometries = ArrayList<JsonElement>(geometry.getNumGeometries())
                for (i in 0 until geometry.getNumGeometries()) {
                    geometries.add(create(geometry.getGeometryN(i), false))
                }
                result[GeoJsonConstants.NAME_GEOMETRIES] = JsonArray(geometries)
            }

            else -> throw IllegalArgumentException(
                "Unable to encode geometry " + geometry.getGeometryType()
            )
        }

        if (encodeCRS) {
            result[GeoJsonConstants.NAME_CRS] = createCRS(geometry.getSRID())
        }

        return JsonObject(result)
    }

    private fun createCRS(srid: Int): JsonObject {
        val props = LinkedHashMap<String, JsonElement>()
        props[GeoJsonConstants.NAME_NAME] = JsonPrimitive(EPSG_PREFIX + srid)

        val result = LinkedHashMap<String, JsonElement>()
        result[GeoJsonConstants.NAME_TYPE] = JsonPrimitive(GeoJsonConstants.NAME_NAME)
        result[GeoJsonConstants.NAME_PROPERTIES] = JsonObject(props)

        return JsonObject(result)
    }

    private fun makeJsonAware(poly: Polygon): JsonArray {
        val result = ArrayList<JsonElement>()

        result.add(
            JsonUnquotedLiteral(getJsonString(poly.getExteriorRing().getCoordinateSequence()))
        )
        for (i in 0 until poly.getNumInteriorRing()) {
            result.add(
                JsonUnquotedLiteral(getJsonString(poly.getInteriorRingN(i).getCoordinateSequence()))
            )
        }

        return JsonArray(result)
    }

    private fun makeJsonAware(geometryCollection: GeometryCollection): JsonArray {
        val list = ArrayList<JsonElement>(geometryCollection.getNumGeometries())
        for (i in 0 until geometryCollection.getNumGeometries()) {
            val geometry = geometryCollection.getGeometryN(i)

            when (geometry) {
                is Polygon -> list.add(makeJsonAware(geometry))
                is LineString ->
                    list.add(JsonUnquotedLiteral(getJsonString(geometry.getCoordinateSequence())))
                is Point ->
                    list.add(JsonUnquotedLiteral(getJsonString(geometry.getCoordinateSequence())))
            }
        }

        return JsonArray(list)
    }

    private fun getJsonString(coordinateSequence: CoordinateSequence): String {
        val result = StringBuilder()

        if (coordinateSequence.size() > 1) {
            result.append("[")
        }
        for (i in 0 until coordinateSequence.size()) {
            if (i > 0) {
                result.append(",")
            }
            result.append("[")
            result.append(formatOrdinate(coordinateSequence.getOrdinate(i, CoordinateSequence.X)))
            result.append(",")
            result.append(formatOrdinate(coordinateSequence.getOrdinate(i, CoordinateSequence.Y)))

            if (coordinateSequence.getDimension() > 2) {
                val z = coordinateSequence.getOrdinate(i, CoordinateSequence.Z)
                if (!z.isNaN()) {
                    result.append(",")
                    result.append(formatOrdinate(z))
                }
            }

            result.append("]")
        }

        if (coordinateSequence.size() > 1) {
            result.append("]")
        }

        return result.toString()
    }

    private fun formatOrdinate(ordinate: Double): String {
        var x = ordinate

        return if (abs(x) >= 10.0.pow(-3) && x < 10.0.pow(7)) {
            x = floor(x * scale + 0.5) / scale
            val lx = x.toLong()
            if (lx.toDouble() == x) {
                lx.toString()
            } else {
                x.toString()
            }
        } else {
            x.toString()
        }
    }

    companion object {
        private const val JSON_ARRAY_EMPTY = "[]"

        /**
         * The prefix for EPSG codes in the `crs` property.
         */
        const val EPSG_PREFIX: String = "EPSG:"

        // Compact output (no spaces), keys in insertion order — matching the historical
        // org.json.simple `JSONObject.writeJSONString` byte layout.
        private val jsonFormat = Json
    }
}
