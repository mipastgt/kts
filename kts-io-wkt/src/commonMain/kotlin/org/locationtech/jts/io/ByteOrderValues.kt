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

import kotlin.jvm.JvmStatic

/**
 * Methods to read and write primitive datatypes from/to byte sequences, allowing the byte order to
 * be specified.
 *
 * Similar to the standard Java `ByteBuffer` class.
 */
object ByteOrderValues {
    const val BIG_ENDIAN = 1
    const val LITTLE_ENDIAN = 2

    @JvmStatic
    fun getInt(buf: ByteArray, byteOrder: Int): Int {
        return if (byteOrder == BIG_ENDIAN) {
            ((buf[0].toInt() and 0xff) shl 24) or
                ((buf[1].toInt() and 0xff) shl 16) or
                ((buf[2].toInt() and 0xff) shl 8) or
                (buf[3].toInt() and 0xff)
        } else { // LITTLE_ENDIAN
            ((buf[3].toInt() and 0xff) shl 24) or
                ((buf[2].toInt() and 0xff) shl 16) or
                ((buf[1].toInt() and 0xff) shl 8) or
                (buf[0].toInt() and 0xff)
        }
    }

    @JvmStatic
    fun putInt(intValue: Int, buf: ByteArray, byteOrder: Int) {
        if (byteOrder == BIG_ENDIAN) {
            buf[0] = (intValue shr 24).toByte()
            buf[1] = (intValue shr 16).toByte()
            buf[2] = (intValue shr 8).toByte()
            buf[3] = intValue.toByte()
        } else { // LITTLE_ENDIAN
            buf[0] = intValue.toByte()
            buf[1] = (intValue shr 8).toByte()
            buf[2] = (intValue shr 16).toByte()
            buf[3] = (intValue shr 24).toByte()
        }
    }

    @JvmStatic
    fun getLong(buf: ByteArray, byteOrder: Int): Long {
        return if (byteOrder == BIG_ENDIAN) {
            (buf[0].toLong() and 0xff) shl 56 or
                ((buf[1].toLong() and 0xff) shl 48) or
                ((buf[2].toLong() and 0xff) shl 40) or
                ((buf[3].toLong() and 0xff) shl 32) or
                ((buf[4].toLong() and 0xff) shl 24) or
                ((buf[5].toLong() and 0xff) shl 16) or
                ((buf[6].toLong() and 0xff) shl 8) or
                (buf[7].toLong() and 0xff)
        } else { // LITTLE_ENDIAN
            (buf[7].toLong() and 0xff) shl 56 or
                ((buf[6].toLong() and 0xff) shl 48) or
                ((buf[5].toLong() and 0xff) shl 40) or
                ((buf[4].toLong() and 0xff) shl 32) or
                ((buf[3].toLong() and 0xff) shl 24) or
                ((buf[2].toLong() and 0xff) shl 16) or
                ((buf[1].toLong() and 0xff) shl 8) or
                (buf[0].toLong() and 0xff)
        }
    }

    @JvmStatic
    fun putLong(longValue: Long, buf: ByteArray, byteOrder: Int) {
        if (byteOrder == BIG_ENDIAN) {
            buf[0] = (longValue shr 56).toByte()
            buf[1] = (longValue shr 48).toByte()
            buf[2] = (longValue shr 40).toByte()
            buf[3] = (longValue shr 32).toByte()
            buf[4] = (longValue shr 24).toByte()
            buf[5] = (longValue shr 16).toByte()
            buf[6] = (longValue shr 8).toByte()
            buf[7] = longValue.toByte()
        } else { // LITTLE_ENDIAN
            buf[0] = longValue.toByte()
            buf[1] = (longValue shr 8).toByte()
            buf[2] = (longValue shr 16).toByte()
            buf[3] = (longValue shr 24).toByte()
            buf[4] = (longValue shr 32).toByte()
            buf[5] = (longValue shr 40).toByte()
            buf[6] = (longValue shr 48).toByte()
            buf[7] = (longValue shr 56).toByte()
        }
    }

    @JvmStatic
    fun getDouble(buf: ByteArray, byteOrder: Int): Double {
        val longVal = getLong(buf, byteOrder)
        return Double.fromBits(longVal)
    }

    @JvmStatic
    fun putDouble(doubleValue: Double, buf: ByteArray, byteOrder: Int) {
        // .toBits() (not .toRawBits()) matches java.lang.Double.doubleToLongBits — both
        // normalize NaN to the canonical 0x7ff8000000000000L.
        val longVal = doubleValue.toBits()
        putLong(longVal, buf, byteOrder)
    }
}
