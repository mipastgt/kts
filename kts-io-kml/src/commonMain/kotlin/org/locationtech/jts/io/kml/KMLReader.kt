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

import nl.adaptivity.xmlutil.EventType
import nl.adaptivity.xmlutil.XmlException
import nl.adaptivity.xmlutil.XmlReader
import nl.adaptivity.xmlutil.xmlStreaming
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.LinearRing
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.Polygon
import org.locationtech.jts.io.ParseException

/**
 * Constructs a [Geometry] object from the OGC KML representation.
 * Works only with KML geometry elements and may also parse attributes within these elements.
 *
 * The KML is parsed with a pull parser (xmlutil `XmlReader`), replacing the JDK StAX
 * (`javax.xml.stream`) used by the original Java implementation, so this reader is multiplatform.
 */
class KMLReader private constructor(
    private val geometryFactory: GeometryFactory,
    private val attributeNames: Set<String>,
) {
    /**
     * Creates a reader that creates objects using the default [GeometryFactory].
     */
    constructor() : this(GeometryFactory(), emptySet())

    /**
     * Creates a reader that creates objects using the given [GeometryFactory].
     *
     * @param geometryFactory the factory used to create `Geometry`s.
     */
    constructor(geometryFactory: GeometryFactory) : this(geometryFactory, emptySet())

    /**
     * Creates a reader that creates objects using the default [GeometryFactory].
     *
     * @param attributeNames names of attributes that should be parsed (i.e. extrude, altitudeMode,
     *     tesselate, etc).
     */
    constructor(attributeNames: Collection<String>?) : this(GeometryFactory(), attributeNames)

    /**
     * Creates a reader that creates objects using the given [GeometryFactory].
     *
     * @param geometryFactory the factory used to create `Geometry`s.
     * @param attributeNames names of attributes that should be parsed (i.e. extrude, altitudeMode,
     *     tesselate, etc).
     */
    constructor(geometryFactory: GeometryFactory, attributeNames: Collection<String>?) :
        this(geometryFactory, attributeNames?.toHashSet() ?: emptySet())

    /**
     * Reads a KML representation of a [Geometry] from a [String].
     * If any attribute names were specified during [KMLReader] construction, they will be stored as
     * a `Map` in [Geometry.setUserData].
     *
     * @param kmlGeometryString string that specifies kml representation of geometry
     * @return a `Geometry` specified by `kmlGeometryString`
     * @throws ParseException if a parsing problem occurs
     */
    @Throws(ParseException::class)
    fun read(kmlGeometryString: String): Geometry {
        try {
            return parseKML(StaxLikeReader(xmlStreaming.newReader(kmlGeometryString)))
        } catch (e: XmlException) {
            throw ParseException(e.toString())
        }
    }

    private fun parseKMLCoordinates(reader: StaxLikeReader): Array<Coordinate> {
        var coordinates = reader.getElementText()

        if (coordinates.isEmpty()) {
            raiseParseError("Empty coordinates")
        }
        coordinates = whitespaceRegex.replace(coordinates.trim(), " ")

        val parsedOrdinates = doubleArrayOf(Double.NaN, Double.NaN, Double.NaN)
        val coordinateList = ArrayList<Coordinate>()

        var spaceIdx = coordinates.indexOf(' ')
        var currentIdx = 0

        while (currentIdx < coordinates.length) {
            if (spaceIdx == -1) {
                spaceIdx = coordinates.length
            }

            val coordinate = coordinates.substring(currentIdx, spaceIdx)

            val yOrdinateComma = coordinate.indexOf(',')
            if (yOrdinateComma == -1 || yOrdinateComma == coordinate.length - 1 || yOrdinateComma == 0) {
                raiseParseError("Invalid coordinate format")
            }

            parsedOrdinates[0] = coordinate.substring(0, yOrdinateComma).toDouble()

            val zOrdinateComma = coordinate.indexOf(',', yOrdinateComma + 1)
            if (zOrdinateComma == -1) {
                parsedOrdinates[1] = coordinate.substring(yOrdinateComma + 1).toDouble()
            } else {
                parsedOrdinates[1] = coordinate.substring(yOrdinateComma + 1, zOrdinateComma).toDouble()
                parsedOrdinates[2] = coordinate.substring(zOrdinateComma + 1).toDouble()
            }

            val crd = Coordinate(parsedOrdinates[0], parsedOrdinates[1], parsedOrdinates[2])
            geometryFactory.getPrecisionModel().makePrecise(crd)

            coordinateList.add(crd)
            currentIdx = spaceIdx + 1
            spaceIdx = coordinates.indexOf(' ', currentIdx)
            parsedOrdinates[2] = Double.NaN
            parsedOrdinates[1] = Double.NaN
            parsedOrdinates[0] = Double.NaN
        }

        return coordinateList.toTypedArray()
    }

    private fun parseKMLCoordinatesAndAttributes(reader: StaxLikeReader, objectNodeName: String): KMLCoordinatesAndAttributes {
        var coordinates: Array<Coordinate>? = null
        var attributes: MutableMap<String, String>? = null

        while (reader.hasNext() && !(reader.isEndElement() && reader.getLocalName() == objectNodeName)) {
            if (reader.isStartElement()) {
                val elementName = reader.getLocalName()

                if (elementName == COORDINATES) {
                    coordinates = parseKMLCoordinates(reader)
                } else if (attributeNames.contains(elementName)) {
                    if (attributes == null) {
                        attributes = HashMap()
                    }

                    attributes.put(elementName, reader.getElementText())
                }
            }

            reader.next()
        }

        if (coordinates == null) {
            raiseParseError(NO_ELEMENT_ERROR, COORDINATES, objectNodeName)
        }

        return KMLCoordinatesAndAttributes(coordinates!!, attributes)
    }

    private fun parseKMLPoint(reader: StaxLikeReader): Geometry {
        val kmlCoordinatesAndAttributes = parseKMLCoordinatesAndAttributes(reader, POINT)

        val point = geometryFactory.createPoint(kmlCoordinatesAndAttributes.coordinates[0])
        point.setUserData(kmlCoordinatesAndAttributes.attributes)

        return point
    }

    private fun parseKMLLineString(reader: StaxLikeReader): Geometry {
        val kmlCoordinatesAndAttributes = parseKMLCoordinatesAndAttributes(reader, LINESTRING)

        val lineString = geometryFactory.createLineString(kmlCoordinatesAndAttributes.coordinates)
        lineString.setUserData(kmlCoordinatesAndAttributes.attributes)

        return lineString
    }

    private fun parseKMLPolygon(reader: StaxLikeReader): Geometry {
        var shell: LinearRing? = null
        var holes: ArrayList<LinearRing>? = null
        var attributes: MutableMap<String, String>? = null

        while (reader.hasNext() && !(reader.isEndElement() && reader.getLocalName() == POLYGON)) {
            if (reader.isStartElement()) {
                val elementName = reader.getLocalName()

                if (elementName == OUTER_BOUNDARY_IS) {
                    moveToElement(reader, COORDINATES, OUTER_BOUNDARY_IS)
                    shell = geometryFactory.createLinearRing(parseKMLCoordinates(reader))
                } else if (elementName == INNER_BOUNDARY_IS) {
                    moveToElement(reader, COORDINATES, INNER_BOUNDARY_IS)

                    if (holes == null) {
                        holes = ArrayList()
                    }
                    holes.add(geometryFactory.createLinearRing(parseKMLCoordinates(reader)))
                } else if (attributeNames.contains(elementName)) {
                    if (attributes == null) {
                        attributes = HashMap()
                    }

                    attributes.put(elementName, reader.getElementText())
                }
            }

            reader.next()
        }

        if (shell == null) {
            raiseParseError("No outer boundary for Polygon")
        }

        val polygon = geometryFactory.createPolygon(shell, holes?.toTypedArray())
        polygon.setUserData(attributes)

        return polygon
    }

    private fun parseKMLMultiGeometry(reader: StaxLikeReader): Geometry? {
        val geometries = ArrayList<Geometry>()
        var firstParsedType: String? = null
        var allTypesAreSame = true

        while (reader.hasNext()) {
            if (reader.isStartElement()) {
                val elementName = reader.getLocalName()
                when (elementName) {
                    POINT, LINESTRING, POLYGON, MULTIGEOMETRY -> {
                        val geometry = parseKML(reader)

                        if (firstParsedType == null) {
                            firstParsedType = geometry.getGeometryType()
                        } else if (firstParsedType != geometry.getGeometryType()) {
                            allTypesAreSame = false
                        }

                        geometries.add(geometry)
                    }
                }
            }

            reader.next()
        }

        if (geometries.isEmpty()) {
            return null
        }

        if (geometries.size == 1) {
            return geometries[0]
        }

        if (allTypesAreSame) {
            return when (firstParsedType) {
                POINT -> geometryFactory.createMultiPoint(Array(geometries.size) { geometries[it] as Point })
                LINESTRING -> geometryFactory.createMultiLineString(Array(geometries.size) { geometries[it] as LineString })
                POLYGON -> geometryFactory.createMultiPolygon(Array(geometries.size) { geometries[it] as Polygon })
                else -> geometryFactory.createGeometryCollection(geometries.toTypedArray())
            }
        } else {
            return geometryFactory.createGeometryCollection(geometries.toTypedArray())
        }
    }

    private fun parseKML(reader: StaxLikeReader): Geometry {
        var hasElement = false

        while (reader.hasNext()) {
            if (reader.isStartElement()) {
                hasElement = true
                break
            }

            reader.next()
        }

        if (!hasElement) {
            raiseParseError("Invalid KML format")
        }

        val elementName = reader.getLocalName()
        when (elementName) {
            POINT -> return parseKMLPoint(reader)
            LINESTRING -> return parseKMLLineString(reader)
            POLYGON -> return parseKMLPolygon(reader)
            MULTIGEOMETRY -> {
                reader.next()
                return parseKMLMultiGeometry(reader)!!
            }
        }

        raiseParseError("Unknown KML geometry type %s", elementName)
        throw IllegalStateException() // unreachable: raiseParseError always throws
    }

    private fun moveToElement(reader: StaxLikeReader, elementName: String, endElementName: String) {
        var elementFound = false

        while (reader.hasNext() && !(reader.isEndElement() && reader.getLocalName() == endElementName)) {
            if (reader.isStartElement() && reader.getLocalName() == elementName) {
                elementFound = true
                break
            }

            reader.next()
        }

        if (!elementFound) {
            raiseParseError(NO_ELEMENT_ERROR, elementName, endElementName)
        }
    }

    @Throws(ParseException::class)
    private fun raiseParseError(template: String, vararg parameters: Any?) {
        throw ParseException(formatMessage(template, parameters))
    }

    private class KMLCoordinatesAndAttributes(
        val coordinates: Array<Coordinate>,
        val attributes: Map<String, String>?,
    )

    companion object {
        private val whitespaceRegex = Regex("\\s+")

        private const val POINT = "Point"
        private const val LINESTRING = "LineString"
        private const val POLYGON = "Polygon"
        private const val MULTIGEOMETRY = "MultiGeometry"

        private const val COORDINATES = "coordinates"
        private const val OUTER_BOUNDARY_IS = "outerBoundaryIs"
        private const val INNER_BOUNDARY_IS = "innerBoundaryIs"

        private const val NO_ELEMENT_ERROR = "No element %s found in %s"

        /** Substitutes `%s` placeholders in [template] with [parameters] in order (common replacement for `String.format`). */
        private fun formatMessage(template: String, parameters: Array<out Any?>): String {
            val sb = StringBuilder()
            var paramIndex = 0
            var i = 0
            while (i < template.length) {
                val c = template[i]
                if (c == '%' && i + 1 < template.length && template[i + 1] == 's') {
                    sb.append(if (paramIndex < parameters.size) parameters[paramIndex].toString() else "%s")
                    paramIndex++
                    i += 2
                } else {
                    sb.append(c)
                    i++
                }
            }
            return sb.toString()
        }
    }
}

/**
 * A minimal StAX-like adapter over an xmlutil [XmlReader], preserving the "current event" semantics
 * the KML parsing logic relies on (`isStartElement`/`isEndElement`/`getLocalName`/`getElementText`,
 * with the reader initially positioned *before* the first event). This lets the parser be a faithful
 * port of the original StAX (`XMLStreamReader`) code.
 */
internal class StaxLikeReader(private val reader: XmlReader) {
    private var current: EventType? = null

    fun hasNext(): Boolean = reader.hasNext()

    fun next() {
        current = reader.next()
    }

    fun isStartElement(): Boolean = current == EventType.START_ELEMENT

    fun isEndElement(): Boolean = current == EventType.END_ELEMENT

    fun getLocalName(): String = reader.localName

    /**
     * Equivalent to StAX `getElementText`: called on a START_ELEMENT, returns the concatenated text
     * content of the element and leaves the reader positioned on the matching END_ELEMENT.
     */
    fun getElementText(): String {
        val sb = StringBuilder()
        var ev = reader.next()
        while (ev != EventType.END_ELEMENT) {
            when (ev) {
                EventType.TEXT, EventType.CDSECT, EventType.ENTITY_REF -> sb.append(reader.text)
                else -> {}
            }
            ev = reader.next()
        }
        current = EventType.END_ELEMENT
        return sb.toString()
    }
}
