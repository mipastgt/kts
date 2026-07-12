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
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryCollection
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.MultiLineString
import org.locationtech.jts.geom.MultiPoint
import org.locationtech.jts.geom.MultiPolygon
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.Polygon
import org.locationtech.jts.util.Assert
import kotlin.jvm.JvmStatic

/**
 * Writes a [Geometry] into Well-Known Binary format. Supports use of an [OutStream], which allows
 * easy use with arbitrary byte stream sinks.
 *
 * The WKB format is specified in the OGC *Simple Features for SQL specification* (section 3.3.2.6).
 *
 * This implementation supports the **Extended WKB** standard. Extended WKB allows writing
 * 3-dimensional coordinates and the geometry SRID value. The presence of 3D coordinates is indicated
 * by setting the high bit of the `wkbType` word. The presence of a SRID is indicated by setting the
 * third bit of the `wkbType` word. EWKB format is upward-compatible with the original SFS WKB format.
 *
 * This class supports reuse of a single instance to write multiple geometries. This class is not
 * thread-safe; each thread should create its own instance.
 *
 * @see WKBReader
 */
class WKBWriter(
    private val outputDimension: Int,
    private val byteOrder: Int,
    private var includeSRID: Boolean
) {

    private var outputOrdinates: MutableSet<Ordinate>
    private val byteArrayOutStream = ByteArrayOutStream()
    // holds output data values
    private val buf = ByteArray(8)

    init {
        if (outputDimension < 2 || outputDimension > 4) {
            throw IllegalArgumentException("Output dimension must be 2 to 4")
        }

        this.outputOrdinates = mutableSetOf(Ordinate.X, Ordinate.Y)
        if (outputDimension > 2) {
            outputOrdinates.add(Ordinate.Z)
        }
        if (outputDimension > 3) {
            outputOrdinates.add(Ordinate.M)
        }
    }

    /**
     * Creates a writer that writes [Geometry]s with output dimension = 2 and BIG_ENDIAN byte order.
     */
    constructor() : this(2, ByteOrderValues.BIG_ENDIAN, false)

    /**
     * Creates a writer that writes [Geometry]s with the given dimension (2 or 3) for output
     * coordinates and [ByteOrderValues.BIG_ENDIAN] byte order.
     */
    constructor(outputDimension: Int) : this(outputDimension, ByteOrderValues.BIG_ENDIAN, false)

    /**
     * Creates a writer that writes [Geometry]s with the given dimension (2 or 3) for output
     * coordinates and [ByteOrderValues.BIG_ENDIAN] byte order. Also takes a flag to control whether
     * srid information will be written.
     */
    constructor(outputDimension: Int, includeSRID: Boolean) :
        this(outputDimension, ByteOrderValues.BIG_ENDIAN, includeSRID)

    /**
     * Creates a writer that writes [Geometry]s with the given dimension (2 or 3) for output
     * coordinates and byte order.
     */
    constructor(outputDimension: Int, byteOrder: Int) : this(outputDimension, byteOrder, false)

    /**
     * Sets the [Ordinate] that are to be written. Possible members are X, Y, Z and M. Values of X
     * and Y are always assumed and not particularly checked for.
     *
     * @param outputOrdinates A set of [Ordinate] values
     */
    fun setOutputOrdinates(outputOrdinates: Set<Ordinate>) {
        this.outputOrdinates.remove(Ordinate.Z)
        this.outputOrdinates.remove(Ordinate.M)

        if (this.outputDimension == 3) {
            if (outputOrdinates.contains(Ordinate.Z)) {
                this.outputOrdinates.add(Ordinate.Z)
            } else if (outputOrdinates.contains(Ordinate.M)) {
                this.outputOrdinates.add(Ordinate.M)
            }
        }
        if (this.outputDimension == 4) {
            if (outputOrdinates.contains(Ordinate.Z)) {
                this.outputOrdinates.add(Ordinate.Z)
            }
            if (outputOrdinates.contains(Ordinate.M)) {
                this.outputOrdinates.add(Ordinate.M)
            }
        }
    }

    /**
     * Gets a bit-pattern defining which ordinates should be written.
     *
     * @return an ordinate bit-pattern
     */
    fun getOutputOrdinates(): MutableSet<Ordinate> = outputOrdinates

    /**
     * Writes a [Geometry] into a byte array.
     *
     * @param geom the geometry to write
     * @return the byte array containing the WKB
     */
    fun write(geom: Geometry): ByteArray {
        try {
            byteArrayOutStream.reset()
            write(geom, byteArrayOutStream)
        } catch (ex: IOException) {
            throw RuntimeException("Unexpected IO exception: " + ex.message)
        }
        return byteArrayOutStream.toByteArray()
    }

    /**
     * Writes a [Geometry] to an [OutStream].
     *
     * @param geom the geometry to write
     * @param os the out stream to write to
     * @throws IOException if an I/O error occurs
     */
    @Throws(IOException::class)
    fun write(geom: Geometry, os: OutStream) {
        when (geom) {
            is Point -> writePoint(geom, os)
            // LinearRings will be written as LineStrings
            is LineString -> writeLineString(geom, os)
            is Polygon -> writePolygon(geom, os)
            is MultiPoint -> writeGeometryCollection(WKBConstants.wkbMultiPoint, geom, os)
            is MultiLineString -> writeGeometryCollection(WKBConstants.wkbMultiLineString, geom, os)
            is MultiPolygon -> writeGeometryCollection(WKBConstants.wkbMultiPolygon, geom, os)
            is GeometryCollection -> writeGeometryCollection(WKBConstants.wkbGeometryCollection, geom, os)
            else -> Assert.shouldNeverReachHere("Unknown Geometry type")
        }
    }

    @Throws(IOException::class)
    private fun writePoint(pt: Point, os: OutStream) {
        writeByteOrder(os)
        writeGeometryType(WKBConstants.wkbPoint, pt, os)
        if (pt.getCoordinateSequence().size() == 0) {
            // write empty point as NaNs (extension to OGC standard)
            writeNaNs(outputDimension, os)
        } else {
            writeCoordinateSequence(pt.getCoordinateSequence(), false, os)
        }
    }

    @Throws(IOException::class)
    private fun writeLineString(line: LineString, os: OutStream) {
        writeByteOrder(os)
        writeGeometryType(WKBConstants.wkbLineString, line, os)
        writeCoordinateSequence(line.getCoordinateSequence(), true, os)
    }

    @Throws(IOException::class)
    private fun writePolygon(poly: Polygon, os: OutStream) {
        writeByteOrder(os)
        writeGeometryType(WKBConstants.wkbPolygon, poly, os)
        //--- write empty polygons with no rings (OCG extension)
        if (poly.isEmpty()) {
            writeInt(0, os)
            return
        }
        writeInt(poly.getNumInteriorRing() + 1, os)
        writeCoordinateSequence(poly.getExteriorRing().getCoordinateSequence(), true, os)
        for (i in 0 until poly.getNumInteriorRing()) {
            writeCoordinateSequence(poly.getInteriorRingN(i).getCoordinateSequence(), true, os)
        }
    }

    @Throws(IOException::class)
    private fun writeGeometryCollection(geometryType: Int, gc: GeometryCollection, os: OutStream) {
        writeByteOrder(os)
        writeGeometryType(geometryType, gc, os)
        writeInt(gc.getNumGeometries(), os)
        val originalIncludeSRID = this.includeSRID
        this.includeSRID = false
        for (i in 0 until gc.getNumGeometries()) {
            write(gc.getGeometryN(i), os)
        }
        this.includeSRID = originalIncludeSRID
    }

    @Throws(IOException::class)
    private fun writeByteOrder(os: OutStream) {
        buf[0] = if (byteOrder == ByteOrderValues.LITTLE_ENDIAN) {
            WKBConstants.wkbNDR.toByte()
        } else {
            WKBConstants.wkbXDR.toByte()
        }
        os.write(buf, 1)
    }

    @Throws(IOException::class)
    private fun writeGeometryType(geometryType: Int, g: Geometry, os: OutStream) {
        var ordinals = 0
        if (outputOrdinates.contains(Ordinate.Z)) {
            ordinals = ordinals or 0x80000000.toInt()
        }

        if (outputOrdinates.contains(Ordinate.M)) {
            ordinals = ordinals or 0x40000000
        }

        val flag3D = if (outputDimension > 2) ordinals else 0
        var typeInt = geometryType or flag3D
        typeInt = typeInt or (if (includeSRID) 0x20000000 else 0)
        writeInt(typeInt, os)
        if (includeSRID) {
            writeInt(g.getSRID(), os)
        }
    }

    @Throws(IOException::class)
    private fun writeInt(intValue: Int, os: OutStream) {
        ByteOrderValues.putInt(intValue, buf, byteOrder)
        os.write(buf, 4)
    }

    @Throws(IOException::class)
    private fun writeCoordinateSequence(seq: CoordinateSequence, writeSize: Boolean, os: OutStream) {
        if (writeSize) {
            writeInt(seq.size(), os)
        }

        for (i in 0 until seq.size()) {
            writeCoordinate(seq, i, os)
        }
    }

    @Throws(IOException::class)
    private fun writeCoordinate(seq: CoordinateSequence, index: Int, os: OutStream) {
        ByteOrderValues.putDouble(seq.getX(index), buf, byteOrder)
        os.write(buf, 8)
        ByteOrderValues.putDouble(seq.getY(index), buf, byteOrder)
        os.write(buf, 8)

        // only write 3rd dim if caller has requested it for this writer
        if (outputDimension >= 3) {
            // if 3rd dim is requested, only write it if the CoordinateSequence provides it
            val ordVal = seq.getOrdinate(index, 2)
            ByteOrderValues.putDouble(ordVal, buf, byteOrder)
            os.write(buf, 8)
        }
        // only write 4th dim if caller has requested it for this writer
        if (outputDimension == 4) {
            // if 4th dim is requested, only write it if the CoordinateSequence provides it
            val ordVal = seq.getOrdinate(index, 3)
            ByteOrderValues.putDouble(ordVal, buf, byteOrder)
            os.write(buf, 8)
        }
    }

    @Throws(IOException::class)
    private fun writeNaNs(numNaNs: Int, os: OutStream) {
        for (i in 0 until numNaNs) {
            ByteOrderValues.putDouble(Double.NaN, buf, byteOrder)
            os.write(buf, 8)
        }
    }

    companion object {
        /**
         * Converts a byte array to a hexadecimal string.
         *
         * @param bytes a byte array
         * @return a string of hexadecimal digits
         */
        @Deprecated("Use toHex", ReplaceWith("WKBWriter.toHex(bytes)"))
        @JvmStatic
        fun bytesToHex(bytes: ByteArray): String {
            return toHex(bytes)
        }

        /**
         * Converts a byte array to a hexadecimal string.
         *
         * @param bytes a byte array
         * @return a string of hexadecimal digits
         */
        @JvmStatic
        fun toHex(bytes: ByteArray): String {
            val buf = StringBuilder()
            for (i in bytes.indices) {
                val b = bytes[i]
                buf.append(toHexDigit((b.toInt() shr 4) and 0x0F))
                buf.append(toHexDigit(b.toInt() and 0x0F))
            }
            return buf.toString()
        }

        private fun toHexDigit(n: Int): Char {
            if (n < 0 || n > 15) {
                throw IllegalArgumentException("Nibble value out of range: $n")
            }
            if (n <= 9) {
                return '0' + n
            }
            return 'A' + (n - 10)
        }
    }
}
