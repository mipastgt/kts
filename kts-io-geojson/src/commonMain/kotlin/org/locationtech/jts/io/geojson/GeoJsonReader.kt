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

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import org.locationtech.jts.geom.CoordinateSequence
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LinearRing
import org.locationtech.jts.geom.Polygon
import org.locationtech.jts.geom.PrecisionModel
import org.locationtech.jts.geom.impl.CoordinateArraySequence
import org.locationtech.jts.io.ParseException

/**
 * Reads a GeoJSON Geometry from a JSON fragment into a [Geometry].
 *
 * The current GeoJSON specification is
 * [RFC 7946](https://tools.ietf.org/html/rfc7946).
 * An older specification is on the GeoJSON web site:
 * [geojson-spec.html](http://geojson.org/geojson-spec.html).
 *
 * The reader does not require a particular orientation for polygon rings.
 *
 * The reader reads empty or null coordinate arrays as empty geometries.
 *
 * It is the caller's responsibility to ensure that the supplied
 * [PrecisionModel] matches the precision of the incoming data. If a lower
 * precision for the data is required, a subsequent process must be run on the
 * data to reduce its precision.
 *
 * @author Martin Davis
 * @author Paul Howells, Vivid Solutions.
 */
class GeoJsonReader {

    private var gf: GeometryFactory? = null

    /**
     * The default constructor uses the SRID from the Geojson CRS and the
     * default `PrecisionModel` to create a `GeometryFactory`. If there is no CRS
     * specified then the default CRS is a geographic coordinate reference system,
     * using the WGS84 datum, and with longitude and latitude units of decimal
     * degrees (SRID = 4326)
     */
    constructor() {
        // do nothing
    }

    /**
     * This constructor accepts a `GeometryFactory` that is used
     * to create the output geometries and to override the GeoJson CRS.
     *
     * @param geometryFactory a GeometryFactory
     */
    constructor(geometryFactory: GeometryFactory) {
        this.gf = geometryFactory
    }

    /**
     * Reads a GeoJson Geometry from a [String] into a single [Geometry].
     *
     * @param json The GeoJson String to parse
     * @return the resulting JTS Geometry
     *
     * @throws ParseException throws a ParseException if the JSON string cannot be parsed
     */
    @Throws(ParseException::class)
    fun read(json: String): Geometry {
        val root = try {
            Json.parseToJsonElement(json)
        } catch (e: SerializationException) {
            throw ParseException(e)
        }

        val geometryMap = root as? JsonObject
            ?: throw ParseException("Could not parse Geometry from Json string.")

        val geometryFactory = this.gf ?: getGeometryFactory(geometryMap)

        return create(geometryMap, geometryFactory)
    }

    private fun create(geometryMap: JsonObject, geometryFactory: GeometryFactory): Geometry {
        val type = geometryMap.string(GeoJsonConstants.NAME_TYPE)
            ?: throw ParseException(
                "Could not parse Geometry from Json string.  No 'type' property found."
            )

        return when (type) {
            GeoJsonConstants.NAME_POINT -> createPoint(geometryMap, geometryFactory)
            GeoJsonConstants.NAME_LINESTRING -> createLineString(geometryMap, geometryFactory)
            GeoJsonConstants.NAME_POLYGON -> createPolygon(geometryMap, geometryFactory)
            GeoJsonConstants.NAME_MULTIPOINT -> createMultiPoint(geometryMap, geometryFactory)
            GeoJsonConstants.NAME_MULTILINESTRING -> createMultiLineString(geometryMap, geometryFactory)
            GeoJsonConstants.NAME_MULTIPOLYGON -> createMultiPolygon(geometryMap, geometryFactory)
            GeoJsonConstants.NAME_GEOMETRYCOLLECTION -> createGeometryCollection(geometryMap, geometryFactory)
            GeoJsonConstants.NAME_FEATURE -> createFeature(geometryMap, geometryFactory)
            GeoJsonConstants.NAME_FEATURECOLLECTION -> createFeatureCollection(geometryMap, geometryFactory)
            else -> throw ParseException(
                "Could not parse Geometry from GeoJson string.  Unsupported 'type':$type"
            )
        }
    }

    private fun createFeatureCollection(
        geometryMap: JsonObject,
        geometryFactory: GeometryFactory,
    ): Geometry {
        try {
            val features = geometryMap.array(GeoJsonConstants.NAME_FEATURES)!!
            val geometries = Array(features.size) { i ->
                createFeature(features[i] as JsonObject, geometryFactory)
            }
            return geometryFactory.createGeometryCollection(geometries)
        } catch (e: RuntimeException) {
            throw ParseException("Could not parse FeatureCollection from GeoJson string.", e)
        }
    }

    private fun createFeature(geometryMap: JsonObject, geometryFactory: GeometryFactory): Geometry {
        try {
            val innerGeometryMap = geometryMap.`object`(GeoJsonConstants.NAME_GEOMETRY)!!
            return create(innerGeometryMap, geometryFactory)
        } catch (e: RuntimeException) {
            throw ParseException("Could not parse Feature from GeoJson string.", e)
        }
    }

    private fun createGeometryCollection(
        geometryMap: JsonObject,
        geometryFactory: GeometryFactory,
    ): Geometry {
        try {
            val geometriesList = geometryMap.array(GeoJsonConstants.NAME_GEOMETRIES)!!
            val geometries = Array(geometriesList.size) { i ->
                create(geometriesList[i] as JsonObject, geometryFactory)
            }
            return geometryFactory.createGeometryCollection(geometries)
        } catch (e: RuntimeException) {
            throw ParseException("Could not parse GeometryCollection from GeoJson string.", e)
        }
    }

    private fun createMultiPolygon(
        geometryMap: JsonObject,
        geometryFactory: GeometryFactory,
    ): Geometry {
        try {
            val polygonsList = geometryMap.array(GeoJsonConstants.NAME_COORDINATES)!!
            val polygons = arrayOfNulls<Polygon>(polygonsList.size)

            var p = 0
            for (polygonElement in polygonsList) {
                val ringsList = polygonElement.jsonArray

                val rings = ArrayList<CoordinateSequence>()
                for (ringElement in ringsList) {
                    rings.add(createCoordinateSequence(ringElement.jsonArray))
                }

                if (rings.isEmpty()) {
                    continue
                }

                val outer = geometryFactory.createLinearRing(rings[0])
                var inner: Array<LinearRing>? = null
                if (rings.size > 1) {
                    inner = Array(rings.size - 1) { i -> geometryFactory.createLinearRing(rings[i + 1]) }
                }

                polygons[p] = geometryFactory.createPolygon(outer, inner)
                ++p
            }

            @Suppress("UNCHECKED_CAST")
            return geometryFactory.createMultiPolygon(polygons as Array<Polygon>)
        } catch (e: RuntimeException) {
            throw ParseException("Could not parse MultiPolygon from GeoJson string.", e)
        }
    }

    private fun createMultiLineString(
        geometryMap: JsonObject,
        geometryFactory: GeometryFactory,
    ): Geometry {
        try {
            val linesList = geometryMap.array(GeoJsonConstants.NAME_COORDINATES)!!
            val lineStrings = Array(linesList.size) { i ->
                geometryFactory.createLineString(createCoordinateSequence(linesList[i].jsonArray))
            }
            return geometryFactory.createMultiLineString(lineStrings)
        } catch (e: RuntimeException) {
            throw ParseException("Could not parse MultiLineString from GeoJson string.", e)
        }
    }

    private fun createMultiPoint(
        geometryMap: JsonObject,
        geometryFactory: GeometryFactory,
    ): Geometry {
        try {
            val coordinatesList = geometryMap.array(GeoJsonConstants.NAME_COORDINATES)
            val coordinates = createCoordinateSequence(coordinatesList)
            return geometryFactory.createMultiPoint(coordinates)
        } catch (e: RuntimeException) {
            throw ParseException("Could not parse MultiPoint from GeoJson string.", e)
        }
    }

    private fun createPolygon(
        geometryMap: JsonObject,
        geometryFactory: GeometryFactory,
    ): Geometry {
        try {
            val ringsList = geometryMap.array(GeoJsonConstants.NAME_COORDINATES)

            if (ringsList == null || ringsList.isEmpty()) {
                return geometryFactory.createPolygon()
            }

            val rings = ArrayList<CoordinateSequence>()
            for (ringElement in ringsList) {
                rings.add(createCoordinateSequence(ringElement.jsonArray))
            }

            val outer = geometryFactory.createLinearRing(rings[0])
            var inner: Array<LinearRing>? = null
            if (rings.size > 1) {
                inner = Array(rings.size - 1) { i -> geometryFactory.createLinearRing(rings[i + 1]) }
            }

            return geometryFactory.createPolygon(outer, inner)
        } catch (e: RuntimeException) {
            throw ParseException("Could not parse Polygon from GeoJson string.", e)
        }
    }

    private fun createLineString(
        geometryMap: JsonObject,
        geometryFactory: GeometryFactory,
    ): Geometry {
        try {
            val coordinatesList = geometryMap.array(GeoJsonConstants.NAME_COORDINATES)
            val coordinates = createCoordinateSequence(coordinatesList)
            return geometryFactory.createLineString(coordinates)
        } catch (e: RuntimeException) {
            throw ParseException("Could not parse LineString from GeoJson string.", e)
        }
    }

    private fun createPoint(
        geometryMap: JsonObject,
        geometryFactory: GeometryFactory,
    ): Geometry {
        try {
            val coordinateList = geometryMap.array(GeoJsonConstants.NAME_COORDINATES)
            val coordinate = createCoordinate(coordinateList)
            return geometryFactory.createPoint(coordinate)
        } catch (e: RuntimeException) {
            throw ParseException("Could not parse Point from GeoJson string.", e)
        }
    }

    private fun getGeometryFactory(geometryMap: JsonObject): GeometryFactory {
        val crsMap = geometryMap.`object`(GeoJsonConstants.NAME_CRS)
        var srid: Int? = null

        if (crsMap != null) {
            try {
                val propertiesMap = crsMap.`object`(GeoJsonConstants.NAME_PROPERTIES)!!
                val name = propertiesMap.string(GeoJsonConstants.NAME_NAME)!!
                val split = name.split(":")
                val epsg = split[1]
                srid = epsg.toInt()
            } catch (e: RuntimeException) {
                throw ParseException("Could not parse SRID from Geojson 'crs' object.", e)
            }
        }

        // The default CRS is a geographic coordinate reference system, using the WGS84 datum, and
        // with longitude and latitude units of decimal degrees. SRID 4326
        val sridValue = srid ?: 4326

        return GeometryFactory(PrecisionModel(), sridValue)
    }

    private fun createCoordinateSequence(coordinates: JsonArray?): CoordinateSequence {
        val coords = coordinates ?: JsonArray(emptyList())

        val result: CoordinateSequence = CoordinateArraySequence(coords.size)

        for (i in coords.indices) {
            val ordinates = coords[i].jsonArray

            if (ordinates.size > 0) {
                result.setOrdinate(i, 0, ordinates[0].asDouble())
            }
            if (ordinates.size > 1) {
                result.setOrdinate(i, 1, ordinates[1].asDouble())
            }
            if (ordinates.size > 2) {
                result.setOrdinate(i, 2, ordinates[2].asDouble())
            }
        }

        return result
    }

    private fun createCoordinate(ordinates: JsonArray?): CoordinateSequence {
        if (ordinates == null || ordinates.isEmpty()) {
            return CoordinateArraySequence(0)
        }

        val result: CoordinateSequence = CoordinateArraySequence(1)

        if (ordinates.size > 0) {
            result.setOrdinate(0, 0, ordinates[0].asDouble())
        }
        if (ordinates.size > 1) {
            result.setOrdinate(0, 1, ordinates[1].asDouble())
        }
        if (ordinates.size > 2) {
            result.setOrdinate(0, 2, ordinates[2].asDouble())
        }

        return result
    }
}
