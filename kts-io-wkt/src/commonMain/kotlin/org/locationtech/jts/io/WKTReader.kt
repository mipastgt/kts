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
package org.locationtech.jts.io

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.CoordinateSequence
import org.locationtech.jts.geom.CoordinateSequenceFactory
import org.locationtech.jts.geom.CoordinateXY
import org.locationtech.jts.geom.CoordinateXYM
import org.locationtech.jts.geom.CoordinateXYZM
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryCollection
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.LinearRing
import org.locationtech.jts.geom.MultiLineString
import org.locationtech.jts.geom.MultiPoint
import org.locationtech.jts.geom.MultiPolygon
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.Polygon
import org.locationtech.jts.geom.PrecisionModel
import org.locationtech.jts.geom.impl.CoordinateArraySequenceFactory
import org.locationtech.jts.util.Assert

/**
 * Converts a geometry in Well-Known Text format to a [Geometry].
 *
 * `WKTReader` supports extracting [Geometry] objects from a [String] (and, on the JVM, a
 * `java.io.Reader` via the streaming `read(Reader)` extension). This allows it to function as a
 * parser to read [Geometry] objects from text blocks embedded in other data formats (e.g. XML).
 *
 * A `WKTReader` is parameterized by a [GeometryFactory], to allow it to create [Geometry] objects
 * of the appropriate implementation. In particular, the [GeometryFactory] determines the
 * [PrecisionModel] and SRID that is used.
 *
 * The `WKTReader` converts all input numbers to the precise internal representation.
 *
 * As of version 1.15, JTS can read (but not write) WKT syntax which specifies coordinate dimension
 * Z, M or ZM as modifiers (e.g. POINT Z) or in the name of the geometry type (e.g. LINESTRINGZM).
 *
 * Notes:
 * - Keywords are case-insensitive.
 * - The reader supports non-standard "LINEARRING" tags.
 * - The reader uses `String.toDouble` to perform the conversion of ASCII numbers to floating point.
 *   This means it supports the Java syntax for floating point literals (including scientific
 *   notation).
 *
 * @version 1.7
 * @see WKTWriter
 */
class WKTReader(private var geometryFactory: GeometryFactory) {

    private val csFactory: CoordinateSequenceFactory = geometryFactory.getCoordinateSequenceFactory()
    private val precisionModel: PrecisionModel = geometryFactory.getPrecisionModel()

    private var isAllowOldJtsCoordinateSyntax = ALLOW_OLD_JTS_COORDINATE_SYNTAX
    private var isAllowOldJtsMultipointSyntax = ALLOW_OLD_JTS_MULTIPOINT_SYNTAX
    private var isFixStructure = false

    /**
     * Creates a reader that creates objects using the default [GeometryFactory].
     */
    constructor() : this(GeometryFactory())

    /**
     * Sets a flag indicating, that coordinates may have 3 ordinate values even though no Z or M
     * ordinate indicator is present. The default value is [ALLOW_OLD_JTS_COORDINATE_SYNTAX].
     */
    fun setIsOldJtsCoordinateSyntaxAllowed(value: Boolean) {
        isAllowOldJtsCoordinateSyntax = value
    }

    /**
     * Sets a flag indicating, that point coordinates in a MultiPoint geometry must not be enclosed
     * in paren. The default value is [ALLOW_OLD_JTS_MULTIPOINT_SYNTAX].
     */
    fun setIsOldJtsMultiPointSyntaxAllowed(value: Boolean) {
        isAllowOldJtsMultipointSyntax = value
    }

    /**
     * Sets a flag indicating that the structure of input geometry should be fixed so that the
     * geometry can be constructed without error.
     *
     * @see LinearRing.MINIMUM_VALID_SIZE
     */
    fun setFixStructure(isFixStructure: Boolean) {
        this.isFixStructure = isFixStructure
    }

    /**
     * Reads a Well-Known Text representation of a [Geometry] from a [String].
     *
     * @param wellKnownText one or more &lt;Geometry Tagged Text&gt; strings (see the OpenGIS Simple
     *     Features Specification) separated by whitespace
     * @return a [Geometry] specified by `wellKnownText`
     * @throws ParseException if a parsing problem occurs
     */
    @Throws(ParseException::class)
    fun read(wellKnownText: String): Geometry {
        return read(StringWktCharStream(wellKnownText))
    }

    /**
     * Reads a single geometry from a [WktCharStream], consuming one geometry and leaving the source
     * positioned at the next (streaming). Shared entry point for the common `read(String)`, the JVM
     * streaming `read(Reader)` overload, and the streaming file readers in `jts-io-files`.
     *
     * @throws ParseException if a parsing problem occurs
     */
    @Throws(ParseException::class)
    fun read(source: WktCharStream): Geometry {
        val tokenizer = WktStreamTokenizer(source)
        try {
            return readGeometryTaggedText(tokenizer)
        } catch (e: IOException) {
            throw ParseException(e.toString())
        }
    }

    private fun getCoordinate(tokenizer: WktStreamTokenizer, ordinateFlags: Set<Ordinate>, tryParen: Boolean): Coordinate {
        var opened = false
        if (tryParen && isOpenerNext(tokenizer)) {
            tokenizer.nextToken()
            opened = true
        }

        // create a sequence for one coordinate
        val offsetM = if (ordinateFlags.contains(Ordinate.Z)) 1 else 0
        val coord = createCoordinate(ordinateFlags)
        coord.setOrdinate(CoordinateSequence.X, precisionModel.makePrecise(getNextNumber(tokenizer)))
        coord.setOrdinate(CoordinateSequence.Y, precisionModel.makePrecise(getNextNumber(tokenizer)))

        // additionally read other vertices
        if (ordinateFlags.contains(Ordinate.Z)) {
            coord.setOrdinate(CoordinateSequence.Z, getNextNumber(tokenizer))
        }
        if (ordinateFlags.contains(Ordinate.M)) {
            coord.setOrdinate(CoordinateSequence.Z + offsetM, getNextNumber(tokenizer))
        }

        if (ordinateFlags.size == 2 && this.isAllowOldJtsCoordinateSyntax && isNumberNext(tokenizer)) {
            coord.setOrdinate(CoordinateSequence.Z, getNextNumber(tokenizer))
        }

        // read close token if it was opened here
        if (opened) {
            getNextCloser(tokenizer)
        }

        return coord
    }

    private fun createCoordinate(ordinateFlags: Set<Ordinate>): Coordinate {
        val hasZ = ordinateFlags.contains(Ordinate.Z)
        val hasM = ordinateFlags.contains(Ordinate.M)
        if (hasZ && hasM) return CoordinateXYZM()
        if (hasM) return CoordinateXYM()
        if (hasZ || this.isAllowOldJtsCoordinateSyntax) return Coordinate()
        return CoordinateXY()
    }

    private fun getCoordinateSequence(tokenizer: WktStreamTokenizer, ordinateFlags: Set<Ordinate>, minSize: Int, isRing: Boolean): CoordinateSequence {
        if (getNextEmptyOrOpener(tokenizer) == WKTConstants.EMPTY) {
            return createCoordinateSequenceEmpty(ordinateFlags)
        }

        val coordinates = ArrayList<Coordinate>()
        do {
            coordinates.add(getCoordinate(tokenizer, ordinateFlags, false))
        } while (getNextCloserOrComma(tokenizer) == COMMA)

        if (isFixStructure) {
            fixStructure(coordinates, minSize, isRing)
        }
        val coordArray = coordinates.toTypedArray()
        return csFactory.create(coordArray)
    }

    private fun fixStructure(coords: MutableList<Coordinate>, minSize: Int, isRing: Boolean) {
        if (coords.size == 0) return
        if (isRing && !isClosed(coords)) {
            coords.add(coords[0].copy())
        }
        while (coords.size < minSize) {
            coords.add(coords[coords.size - 1].copy())
        }
    }

    private fun isClosed(coords: List<Coordinate>): Boolean {
        if (coords.size == 0) return true
        if (coords.size == 1 || !coords[0].equals2D(coords[coords.size - 1])) {
            return false
        }
        return true
    }

    private fun createCoordinateSequenceEmpty(ordinateFlags: Set<Ordinate>): CoordinateSequence {
        return csFactory.create(0, toDimension(ordinateFlags), if (ordinateFlags.contains(Ordinate.M)) 1 else 0)
    }

    private fun getCoordinateSequenceOldMultiPoint(tokenizer: WktStreamTokenizer, ordinateFlags: Set<Ordinate>): CoordinateSequence {
        val coordinates = ArrayList<Coordinate>()
        do {
            coordinates.add(getCoordinate(tokenizer, ordinateFlags, true))
        } while (getNextCloserOrComma(tokenizer) == COMMA)

        val coordArray = coordinates.toTypedArray()
        return csFactory.create(coordArray)
    }

    /**
     * Computes the required dimension based on the given ordinate values.
     * It is assumed that X and Y are included.
     */
    private fun toDimension(ordinateFlags: Set<Ordinate>): Int {
        var dimension = 2
        if (ordinateFlags.contains(Ordinate.Z)) dimension++
        if (ordinateFlags.contains(Ordinate.M)) dimension++

        if (dimension == 2 && this.isAllowOldJtsCoordinateSyntax) dimension++

        return dimension
    }

    private fun isNumberNext(tokenizer: WktStreamTokenizer): Boolean {
        val type = tokenizer.nextToken()
        tokenizer.pushBack()
        return type == WktStreamTokenizer.TT_WORD
    }

    private fun isOpenerNext(tokenizer: WktStreamTokenizer): Boolean {
        val type = tokenizer.nextToken()
        tokenizer.pushBack()
        return type == '('.code
    }

    private fun getNextNumber(tokenizer: WktStreamTokenizer): Double {
        val type = tokenizer.nextToken()
        if (type == WktStreamTokenizer.TT_WORD) {
            val sval = tokenizer.sval!!
            return if (sval.equals(NAN_SYMBOL, ignoreCase = true)) {
                Double.NaN
            } else {
                try {
                    sval.toDouble()
                } catch (ex: NumberFormatException) {
                    throw parseErrorWithLine(tokenizer, "Invalid number: $sval")
                }
            }
        }
        throw parseErrorExpected(tokenizer, "number")
    }

    private fun getNextEmptyOrOpener(tokenizer: WktStreamTokenizer): String {
        var nextWord = getNextWord(tokenizer)
        if (nextWord.equals(WKTConstants.Z, ignoreCase = true)) {
            nextWord = getNextWord(tokenizer)
        } else if (nextWord.equals(WKTConstants.M, ignoreCase = true)) {
            nextWord = getNextWord(tokenizer)
        } else if (nextWord.equals(WKTConstants.ZM, ignoreCase = true)) {
            nextWord = getNextWord(tokenizer)
        }
        if (nextWord == WKTConstants.EMPTY || nextWord == L_PAREN) {
            return nextWord
        }
        throw parseErrorExpected(tokenizer, WKTConstants.EMPTY + " or " + L_PAREN)
    }

    private fun getNextOrdinateFlags(tokenizer: WktStreamTokenizer): MutableSet<Ordinate> {
        val result = mutableSetOf(Ordinate.X, Ordinate.Y)

        val nextWord = lookAheadWord(tokenizer).uppercase()
        if (nextWord.equals(WKTConstants.Z, ignoreCase = true)) {
            tokenizer.nextToken()
            result.add(Ordinate.Z)
        } else if (nextWord.equals(WKTConstants.M, ignoreCase = true)) {
            tokenizer.nextToken()
            result.add(Ordinate.M)
        } else if (nextWord.equals(WKTConstants.ZM, ignoreCase = true)) {
            tokenizer.nextToken()
            result.add(Ordinate.Z)
            result.add(Ordinate.M)
        }
        return result
    }

    private fun lookAheadWord(tokenizer: WktStreamTokenizer): String {
        val nextWord = getNextWord(tokenizer)
        tokenizer.pushBack()
        return nextWord
    }

    private fun getNextCloserOrComma(tokenizer: WktStreamTokenizer): String {
        val nextWord = getNextWord(tokenizer)
        if (nextWord == COMMA || nextWord == R_PAREN) {
            return nextWord
        }
        throw parseErrorExpected(tokenizer, COMMA + " or " + R_PAREN)
    }

    private fun getNextCloser(tokenizer: WktStreamTokenizer): String {
        val nextWord = getNextWord(tokenizer)
        if (nextWord == R_PAREN) {
            return nextWord
        }
        throw parseErrorExpected(tokenizer, R_PAREN)
    }

    private fun getNextWord(tokenizer: WktStreamTokenizer): String {
        val type = tokenizer.nextToken()
        when (type) {
            WktStreamTokenizer.TT_WORD -> {
                val word = tokenizer.sval!!
                return if (word.equals(WKTConstants.EMPTY, ignoreCase = true)) WKTConstants.EMPTY else word
            }
            '('.code -> return L_PAREN
            ')'.code -> return R_PAREN
            ','.code -> return COMMA
        }
        throw parseErrorExpected(tokenizer, "word")
    }

    private fun parseErrorExpected(tokenizer: WktStreamTokenizer, expected: String): ParseException {
        // throws Asserts for tokens that should never be seen
        if (tokenizer.ttype == WktStreamTokenizer.TT_NUMBER) {
            Assert.shouldNeverReachHere("Unexpected NUMBER token")
        }
        if (tokenizer.ttype == WktStreamTokenizer.TT_EOL) {
            Assert.shouldNeverReachHere("Unexpected EOL token")
        }

        val tokenStr = tokenString(tokenizer)
        return parseErrorWithLine(tokenizer, "Expected $expected but found $tokenStr")
    }

    private fun parseErrorWithLine(tokenizer: WktStreamTokenizer, msg: String): ParseException {
        return ParseException(msg + " (line " + tokenizer.lineno() + ")")
    }

    private fun tokenString(tokenizer: WktStreamTokenizer): String {
        return when (tokenizer.ttype) {
            WktStreamTokenizer.TT_NUMBER -> "<NUMBER>"
            WktStreamTokenizer.TT_EOL -> "End-of-Line"
            WktStreamTokenizer.TT_EOF -> "End-of-Stream"
            WktStreamTokenizer.TT_WORD -> "'" + tokenizer.sval + "'"
            else -> "'" + tokenizer.ttype.toChar() + "'"
        }
    }

    private fun readGeometryTaggedText(tokenizer: WktStreamTokenizer): Geometry {
        val ordinateFlags = mutableSetOf(Ordinate.X, Ordinate.Y)
        val type = getNextWord(tokenizer).uppercase()
        if (type.endsWith(WKTConstants.ZM)) {
            ordinateFlags.add(Ordinate.Z)
            ordinateFlags.add(Ordinate.M)
        } else if (type.endsWith(WKTConstants.Z)) {
            ordinateFlags.add(Ordinate.Z)
        } else if (type.endsWith(WKTConstants.M)) {
            ordinateFlags.add(Ordinate.M)
        }
        return readGeometryTaggedText(tokenizer, type, ordinateFlags)
    }

    private fun readGeometryTaggedText(tokenizer: WktStreamTokenizer, type: String, ordinateFlagsIn: MutableSet<Ordinate>): Geometry {
        var ordinateFlags = ordinateFlagsIn
        if (ordinateFlags.size == 2) {
            ordinateFlags = getNextOrdinateFlags(tokenizer)
        }

        // if we can create a sequence with the required dimension everything is ok, otherwise
        // we need to take a different coordinate sequence factory.
        try {
            csFactory.create(0, toDimension(ordinateFlags), if (ordinateFlags.contains(Ordinate.M)) 1 else 0)
        } catch (e: Exception) {
            geometryFactory = GeometryFactory(
                geometryFactory.getPrecisionModel(),
                geometryFactory.getSRID(),
                csFactoryXYZM
            )
        }

        return when {
            isTypeName(tokenizer, type, WKTConstants.POINT) -> readPointText(tokenizer, ordinateFlags)
            isTypeName(tokenizer, type, WKTConstants.LINESTRING) -> readLineStringText(tokenizer, ordinateFlags)
            isTypeName(tokenizer, type, WKTConstants.LINEARRING) -> readLinearRingText(tokenizer, ordinateFlags)
            isTypeName(tokenizer, type, WKTConstants.POLYGON) -> readPolygonText(tokenizer, ordinateFlags)
            isTypeName(tokenizer, type, WKTConstants.MULTIPOINT) -> readMultiPointText(tokenizer, ordinateFlags)
            isTypeName(tokenizer, type, WKTConstants.MULTILINESTRING) -> readMultiLineStringText(tokenizer, ordinateFlags)
            isTypeName(tokenizer, type, WKTConstants.MULTIPOLYGON) -> readMultiPolygonText(tokenizer, ordinateFlags)
            isTypeName(tokenizer, type, WKTConstants.GEOMETRYCOLLECTION) -> readGeometryCollectionText(tokenizer, ordinateFlags)
            else -> throw parseErrorWithLine(tokenizer, "Unknown geometry type: $type")
        }
    }

    private fun isTypeName(tokenizer: WktStreamTokenizer, type: String, typeName: String): Boolean {
        if (!type.startsWith(typeName)) return false

        val modifiers = type.substring(typeName.length)
        val isValidMod = modifiers.length <= 2 &&
            (modifiers.length == 0 ||
                modifiers == WKTConstants.Z ||
                modifiers == WKTConstants.M ||
                modifiers == WKTConstants.ZM)
        if (!isValidMod) {
            throw parseErrorWithLine(tokenizer, "Invalid dimension modifiers: $type")
        }

        return true
    }

    private fun readPointText(tokenizer: WktStreamTokenizer, ordinateFlags: Set<Ordinate>): Point {
        return geometryFactory.createPoint(getCoordinateSequence(tokenizer, ordinateFlags, 1, false))
    }

    private fun readLineStringText(tokenizer: WktStreamTokenizer, ordinateFlags: Set<Ordinate>): LineString {
        return geometryFactory.createLineString(getCoordinateSequence(tokenizer, ordinateFlags, LineString.MINIMUM_VALID_SIZE, false))
    }

    private fun readLinearRingText(tokenizer: WktStreamTokenizer, ordinateFlags: Set<Ordinate>): LinearRing {
        return geometryFactory.createLinearRing(getCoordinateSequence(tokenizer, ordinateFlags, LinearRing.MINIMUM_VALID_SIZE, true))
    }

    private fun readMultiPointText(tokenizer: WktStreamTokenizer, ordinateFlags: Set<Ordinate>): MultiPoint {
        var nextToken = getNextEmptyOrOpener(tokenizer)
        if (nextToken == WKTConstants.EMPTY) {
            return geometryFactory.createMultiPoint(emptyArray<Point>())
        }

        // check for old-style JTS syntax (no parentheses surrounding Point coordinates)
        if (isAllowOldJtsMultipointSyntax) {
            val nextWord = lookAheadWord(tokenizer)
            if (nextWord != L_PAREN && nextWord != WKTConstants.EMPTY) {
                return geometryFactory.createMultiPoint(getCoordinateSequenceOldMultiPoint(tokenizer, ordinateFlags))
            }
        }

        val points = ArrayList<Point>()
        points.add(readPointText(tokenizer, ordinateFlags))
        nextToken = getNextCloserOrComma(tokenizer)
        while (nextToken == COMMA) {
            points.add(readPointText(tokenizer, ordinateFlags))
            nextToken = getNextCloserOrComma(tokenizer)
        }
        return geometryFactory.createMultiPoint(points.toTypedArray())
    }

    private fun readPolygonText(tokenizer: WktStreamTokenizer, ordinateFlags: Set<Ordinate>): Polygon {
        var nextToken = getNextEmptyOrOpener(tokenizer)
        if (nextToken == WKTConstants.EMPTY) {
            return geometryFactory.createPolygon(createCoordinateSequenceEmpty(ordinateFlags))
        }
        val holes = ArrayList<LinearRing>()
        val shell = readLinearRingText(tokenizer, ordinateFlags)
        nextToken = getNextCloserOrComma(tokenizer)
        while (nextToken == COMMA) {
            holes.add(readLinearRingText(tokenizer, ordinateFlags))
            nextToken = getNextCloserOrComma(tokenizer)
        }
        return geometryFactory.createPolygon(shell, holes.toTypedArray())
    }

    private fun readMultiLineStringText(tokenizer: WktStreamTokenizer, ordinateFlags: Set<Ordinate>): MultiLineString {
        var nextToken = getNextEmptyOrOpener(tokenizer)
        if (nextToken == WKTConstants.EMPTY) {
            return geometryFactory.createMultiLineString()
        }

        val lineStrings = ArrayList<LineString>()
        do {
            lineStrings.add(readLineStringText(tokenizer, ordinateFlags))
            nextToken = getNextCloserOrComma(tokenizer)
        } while (nextToken == COMMA)

        return geometryFactory.createMultiLineString(lineStrings.toTypedArray())
    }

    private fun readMultiPolygonText(tokenizer: WktStreamTokenizer, ordinateFlags: Set<Ordinate>): MultiPolygon {
        var nextToken = getNextEmptyOrOpener(tokenizer)
        if (nextToken == WKTConstants.EMPTY) {
            return geometryFactory.createMultiPolygon()
        }
        val polygons = ArrayList<Polygon>()
        do {
            polygons.add(readPolygonText(tokenizer, ordinateFlags))
            nextToken = getNextCloserOrComma(tokenizer)
        } while (nextToken == COMMA)
        return geometryFactory.createMultiPolygon(polygons.toTypedArray())
    }

    private fun readGeometryCollectionText(tokenizer: WktStreamTokenizer, ordinateFlags: Set<Ordinate>): GeometryCollection {
        var nextToken = getNextEmptyOrOpener(tokenizer)
        if (nextToken == WKTConstants.EMPTY) {
            return geometryFactory.createGeometryCollection()
        }
        val geometries = ArrayList<Geometry>()
        do {
            geometries.add(readGeometryTaggedText(tokenizer))
            nextToken = getNextCloserOrComma(tokenizer)
        } while (nextToken == COMMA)

        return geometryFactory.createGeometryCollection(geometries.toTypedArray())
    }

    companion object {
        private const val COMMA = ","
        private const val L_PAREN = "("
        private const val R_PAREN = ")"
        private const val NAN_SYMBOL = "NaN"

        /** Flag indicating that the old notation of coordinates in JTS is supported. */
        private const val ALLOW_OLD_JTS_COORDINATE_SYNTAX = true

        /** Flag indicating that the old notation of MultiPoint coordinates in JTS is supported. */
        private const val ALLOW_OLD_JTS_MULTIPOINT_SYNTAX = true

        private val csFactoryXYZM: CoordinateSequenceFactory = CoordinateArraySequenceFactory.instance()
    }
}
