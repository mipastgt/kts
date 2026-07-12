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

import org.locationtech.jts.geom.CoordinateSequence
import org.locationtech.jts.geom.CoordinateSequenceFactory
import org.locationtech.jts.geom.CoordinateSequences
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
import kotlin.jvm.JvmStatic

/**
 * Reads a [Geometry] from a byte stream in Well-Known Binary format.
 * Supports use of an [InStream], which allows easy use with arbitrary byte stream sources.
 *
 * This class reads the format describe in [WKBWriter]. It partially handles the **Extended WKB**
 * format used by PostGIS, by parsing and storing optional SRID values. If a SRID is not specified in
 * an element geometry, it is inherited from the parent's SRID. The default SRID value is 0.
 *
 * Although not defined in the WKB specification, empty points are handled if they are represented as
 * a Point with `NaN` X and Y ordinates.
 *
 * The reader repairs structurally-invalid input (specifically, LineStrings and LinearRings which
 * contain too few points have vertices added, and non-closed rings are closed).
 *
 * The reader handles most errors caused by malformed or malicious WKB data. It checks for obviously
 * excessive values of the fields `numElems`, `numRings`, and `numCoords`. It also checks that the
 * reader does not read beyond the end of the data supplied. A [ParseException] is thrown if this
 * situation is detected.
 *
 * This class is designed to support reuse of a single instance to read multiple geometries. This
 * class is not thread-safe; each thread should create its own instance.
 *
 * As of version 1.15, the reader can read geometries following the OGC 06-103r4 Simple Features
 * Access 1.2.1 specification, which aligns with the ISO 19125 standard. This format is used by
 * Spatialite and Geopackage.
 *
 * @see WKBWriter for a formal format specification
 */
class WKBReader(private val factory: GeometryFactory) {

    private val csFactory: CoordinateSequenceFactory = factory.getCoordinateSequenceFactory()
    private val precisionModel: PrecisionModel = factory.getPrecisionModel()

    // default dimension - will be set on read
    private var inputDimension = 2

    /**
     * true if structurally invalid input should be reported rather than repaired.
     * At some point this could be made client-controllable.
     */
    private val isStrict = false
    private val dis = ByteOrderDataInStream()
    private var ordValues: DoubleArray? = null

    private var maxNumFieldValue = 0

    constructor() : this(GeometryFactory())

    /**
     * Reads a single [Geometry] in WKB format from a byte array.
     *
     * @param bytes the byte array to read from
     * @return the geometry read
     * @throws ParseException if the WKB is ill-formed
     */
    @Throws(ParseException::class)
    fun read(bytes: ByteArray): Geometry {
        // possibly reuse the ByteArrayInStream?
        // don't throw IOExceptions, since we are not doing any I/O
        try {
            return read(ByteArrayInStream(bytes), bytes.size / 8)
        } catch (ex: IOException) {
            throw RuntimeException("Unexpected IOException caught: " + ex.message)
        }
    }

    /**
     * Reads a [Geometry] in binary WKB format from an [InStream].
     *
     * @param is the stream to read from
     * @return the Geometry read
     * @throws IOException if the underlying stream creates an error
     * @throws ParseException if the WKB is ill-formed
     */
    @Throws(IOException::class, ParseException::class)
    fun read(`is`: InStream): Geometry {
        // can't tell size of InStream, but MAX_VALUE should be safe
        return read(`is`, Int.MAX_VALUE)
    }

    @Throws(IOException::class, ParseException::class)
    private fun read(`is`: InStream, maxCoordNum: Int): Geometry {
        /*
         * This puts an upper bound on the allowed value in coordNum fields.
         * It avoids OOM exceptions due to malformed input.
         */
        this.maxNumFieldValue = maxCoordNum
        dis.setInStream(`is`)
        return readGeometry(0)
    }

    @Throws(IOException::class, ParseException::class)
    private fun readNumField(fieldName: String): Int {
        // num field is unsigned int, but Java has only signed int
        val num = dis.readInt()
        if (num < 0 || num > maxNumFieldValue) {
            throw ParseException("$fieldName value is too large")
        }
        return num
    }

    @Throws(IOException::class, ParseException::class)
    private fun readGeometry(srid: Int): Geometry {
        var sriD = srid

        // determine byte order
        val byteOrderWKB = dis.readByte()

        // always set byte order, since it may change from geometry to geometry
        if (byteOrderWKB.toInt() == WKBConstants.wkbNDR) {
            dis.setOrder(ByteOrderValues.LITTLE_ENDIAN)
        } else if (byteOrderWKB.toInt() == WKBConstants.wkbXDR) {
            dis.setOrder(ByteOrderValues.BIG_ENDIAN)
        } else if (isStrict) {
            throw ParseException("Unknown geometry byte order (not NDR or XDR): $byteOrderWKB")
        }
        // if not strict and not XDR or NDR, then we just use the dis default set at the
        // start of the geometry (if a multi-geometry). This allows WBKReader to work
        // with Spatialite native BLOB WKB, as well as other WKB variants that might just
        // specify endian-ness at the start of the multigeometry.

        val typeInt = dis.readInt()

        /*
         * To get geometry type mask out EWKB flag bits, and use only low 3 digits of type word.
         * This supports both EWKB and ISO/OGC.
         */
        val geometryType = (typeInt and 0xffff) % 1000

        // handle 3D and 4D WKB geometries
        // geometries with Z coordinates have the 0x80 flag (postgis EWKB)
        // or are in the 1000 range (Z) or in the 3000 range (ZM) of geometry type (ISO/OGC 06-103r4)
        val hasZ = (typeInt and 0x80000000.toInt()) != 0 ||
            (typeInt and 0xffff) / 1000 == 1 || (typeInt and 0xffff) / 1000 == 3
        // geometries with M coordinates have the 0x40 flag (postgis EWKB)
        // or are in the 1000 range (M) or in the 3000 range (ZM) of geometry type (ISO/OGC 06-103r4)
        val hasM = (typeInt and 0x40000000) != 0 ||
            (typeInt and 0xffff) / 1000 == 2 || (typeInt and 0xffff) / 1000 == 3
        inputDimension = 2 + (if (hasZ) 1 else 0) + (if (hasM) 1 else 0)

        val ordinateFlags = mutableSetOf(Ordinate.X, Ordinate.Y)
        if (hasZ) {
            ordinateFlags.add(Ordinate.Z)
        }
        if (hasM) {
            ordinateFlags.add(Ordinate.M)
        }

        // determine if SRIDs are present (EWKB only)
        val hasSRID = (typeInt and 0x20000000) != 0
        if (hasSRID) {
            sriD = dis.readInt()
        }

        // only allocate ordValues buffer if necessary
        val ord = ordValues
        if (ord == null || ord.size < inputDimension) {
            ordValues = DoubleArray(inputDimension)
        }

        val geom: Geometry = when (geometryType) {
            WKBConstants.wkbPoint -> readPoint(ordinateFlags)
            WKBConstants.wkbLineString -> readLineString(ordinateFlags)
            WKBConstants.wkbPolygon -> readPolygon(ordinateFlags)
            WKBConstants.wkbMultiPoint -> readMultiPoint(sriD)
            WKBConstants.wkbMultiLineString -> readMultiLineString(sriD)
            WKBConstants.wkbMultiPolygon -> readMultiPolygon(sriD)
            WKBConstants.wkbGeometryCollection -> readGeometryCollection(sriD)
            else -> throw ParseException("Unknown WKB type $geometryType")
        }
        setSRID(geom, sriD)
        return geom
    }

    /**
     * Sets the SRID, if it was specified in the WKB.
     *
     * @param g the geometry to update
     * @return the geometry with an updated SRID value, if required
     */
    private fun setSRID(g: Geometry, SRID: Int): Geometry {
        if (SRID != 0) {
            g.setSRID(SRID)
        }
        return g
    }

    @Throws(IOException::class, ParseException::class)
    private fun readPoint(ordinateFlags: Set<Ordinate>): Point {
        val pts = readCoordinateSequence(1, ordinateFlags)
        // If X and Y are NaN create a empty point
        if (pts.getX(0).isNaN() || pts.getY(0).isNaN()) {
            return factory.createPoint()
        }
        return factory.createPoint(pts)
    }

    @Throws(IOException::class, ParseException::class)
    private fun readLineString(ordinateFlags: Set<Ordinate>): LineString {
        val size = readNumField(FIELD_NUMCOORDS)
        val pts = readCoordinateSequenceLineString(size, ordinateFlags)
        return factory.createLineString(pts)
    }

    @Throws(IOException::class, ParseException::class)
    private fun readLinearRing(ordinateFlags: Set<Ordinate>): LinearRing {
        val size = readNumField(FIELD_NUMCOORDS)
        val pts = readCoordinateSequenceRing(size, ordinateFlags)
        return factory.createLinearRing(pts)
    }

    @Throws(IOException::class, ParseException::class)
    private fun readPolygon(ordinateFlags: Set<Ordinate>): Polygon {
        val numRings = readNumField(FIELD_NUMRINGS)

        // empty polygon
        if (numRings <= 0) {
            return factory.createPolygon()
        }

        val shell = readLinearRing(ordinateFlags)
        // holes are read after the shell, in order (null when there are none, matching upstream)
        val holes: Array<LinearRing>? =
            if (numRings > 1) Array(numRings - 1) { readLinearRing(ordinateFlags) } else null
        return factory.createPolygon(shell, holes)
    }

    @Throws(IOException::class, ParseException::class)
    private fun readMultiPoint(SRID: Int): MultiPoint {
        val numGeom = readNumField(FIELD_NUMELEMS)
        val geoms = Array(numGeom) {
            val g = readGeometry(SRID)
            if (g !is Point) {
                throw ParseException(INVALID_GEOM_TYPE_MSG + "MultiPoint")
            }
            g
        }
        return factory.createMultiPoint(geoms)
    }

    @Throws(IOException::class, ParseException::class)
    private fun readMultiLineString(SRID: Int): MultiLineString {
        val numGeom = readNumField(FIELD_NUMELEMS)
        val geoms = Array(numGeom) {
            val g = readGeometry(SRID)
            if (g !is LineString) {
                throw ParseException(INVALID_GEOM_TYPE_MSG + "MultiLineString")
            }
            g
        }
        return factory.createMultiLineString(geoms)
    }

    @Throws(IOException::class, ParseException::class)
    private fun readMultiPolygon(SRID: Int): MultiPolygon {
        val numGeom = readNumField(FIELD_NUMELEMS)
        val geoms = Array(numGeom) {
            val g = readGeometry(SRID)
            if (g !is Polygon) {
                throw ParseException(INVALID_GEOM_TYPE_MSG + "MultiPolygon")
            }
            g
        }
        return factory.createMultiPolygon(geoms)
    }

    @Throws(IOException::class, ParseException::class)
    private fun readGeometryCollection(SRID: Int): GeometryCollection {
        val numGeom = readNumField(FIELD_NUMELEMS)
        val geoms = Array(numGeom) { readGeometry(SRID) }
        return factory.createGeometryCollection(geoms)
    }

    @Throws(IOException::class, ParseException::class)
    private fun readCoordinateSequence(size: Int, ordinateFlags: Set<Ordinate>): CoordinateSequence {
        val seq = csFactory.create(size, inputDimension, if (Ordinate.M in ordinateFlags) 1 else 0)
        var targetDim = seq.getDimension()
        if (targetDim > inputDimension) {
            targetDim = inputDimension
        }
        for (i in 0 until size) {
            readCoordinate()
            for (j in 0 until targetDim) {
                seq.setOrdinate(i, j, ordValues!![j])
            }
        }
        return seq
    }

    @Throws(IOException::class, ParseException::class)
    private fun readCoordinateSequenceLineString(size: Int, ordinateFlags: Set<Ordinate>): CoordinateSequence {
        val seq = readCoordinateSequence(size, ordinateFlags)
        if (isStrict) return seq
        if (seq.size() == 0 || seq.size() >= 2) return seq
        return CoordinateSequences.extend(csFactory, seq, 2)
    }

    @Throws(IOException::class, ParseException::class)
    private fun readCoordinateSequenceRing(size: Int, ordinateFlags: Set<Ordinate>): CoordinateSequence {
        val seq = readCoordinateSequence(size, ordinateFlags)
        if (isStrict) return seq
        if (CoordinateSequences.isRing(seq)) return seq
        return CoordinateSequences.ensureValidRing(csFactory, seq)
    }

    /**
     * Reads a coordinate value with the specified dimensionality.
     * Makes the X and Y ordinates precise according to the precision model in use.
     */
    @Throws(IOException::class, ParseException::class)
    private fun readCoordinate() {
        for (i in 0 until inputDimension) {
            if (i <= 1) {
                ordValues!![i] = precisionModel.makePrecise(dis.readDouble())
            } else {
                ordValues!![i] = dis.readDouble()
            }
        }
    }

    companion object {
        /**
         * Converts a hexadecimal string to a byte array.
         * The hexadecimal digit symbols are case-insensitive.
         *
         * @param hex a string containing hex digits
         * @return an array of bytes with the value of the hex string
         */
        @JvmStatic
        fun hexToBytes(hex: String): ByteArray {
            val byteLen = hex.length / 2
            val bytes = ByteArray(byteLen)

            for (i in 0 until hex.length / 2) {
                val i2 = 2 * i
                if (i2 + 1 > hex.length) {
                    throw IllegalArgumentException("Hex string has odd length")
                }

                val nib1 = hexToInt(hex[i2])
                val nib0 = hexToInt(hex[i2 + 1])
                val b = ((nib1 shl 4) + nib0).toByte()
                bytes[i] = b
            }
            return bytes
        }

        private fun hexToInt(hex: Char): Int {
            val nib = hex.digitToIntOrNull(16) ?: -1
            if (nib < 0) {
                throw IllegalArgumentException("Invalid hex digit: '$hex'")
            }
            return nib
        }

        private const val INVALID_GEOM_TYPE_MSG = "Invalid geometry type encountered in "
        private const val FIELD_NUMCOORDS = "numCoords"
        private const val FIELD_NUMRINGS = "numRings"
        private const val FIELD_NUMELEMS = "numElems"
    }
}
