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

/**
 * Allows reading a stream of primitive datatypes from an underlying [InStream], with the
 * representation being in either common byte ordering.
 */
class ByteOrderDataInStream(private var stream: InStream?) {

    private var byteOrder = ByteOrderValues.BIG_ENDIAN

    // buffers to hold primitive datatypes
    private val buf1 = ByteArray(1)
    private val buf4 = ByteArray(4)
    private val buf8 = ByteArray(8)
    private var bufLast: ByteArray? = null

    private var count: Long = 0

    constructor() : this(null)

    /**
     * Allows a single ByteOrderDataInStream to be reused on multiple InStreams.
     */
    fun setInStream(stream: InStream?) {
        this.stream = stream
    }

    /**
     * Sets the ordering on the stream using the codes in [ByteOrderValues].
     *
     * @param byteOrder the byte order code
     */
    fun setOrder(byteOrder: Int) {
        this.byteOrder = byteOrder
    }

    /**
     * Gets the number of bytes read from the stream.
     */
    fun getCount(): Long = count

    /**
     * Gets the data item that was last read from the stream.
     */
    fun getData(): ByteArray? = bufLast

    /**
     * Reads a byte value.
     *
     * @throws IOException if an I/O error occurred
     * @throws ParseException if not enough data could be read
     */
    @Throws(IOException::class, ParseException::class)
    fun readByte(): Byte {
        read(buf1)
        return buf1[0]
    }

    /**
     * Reads an int value.
     *
     * @throws IOException if an I/O error occurred
     * @throws ParseException if not enough data could be read
     */
    @Throws(IOException::class, ParseException::class)
    fun readInt(): Int {
        read(buf4)
        return ByteOrderValues.getInt(buf4, byteOrder)
    }

    /**
     * Reads a long value.
     *
     * @throws IOException if an I/O error occurred
     * @throws ParseException if not enough data could be read
     */
    @Throws(IOException::class, ParseException::class)
    fun readLong(): Long {
        read(buf8)
        return ByteOrderValues.getLong(buf8, byteOrder)
    }

    /**
     * Reads a double value.
     *
     * @throws IOException if an I/O error occurred
     * @throws ParseException if not enough data could be read
     */
    @Throws(IOException::class, ParseException::class)
    fun readDouble(): Double {
        read(buf8)
        return ByteOrderValues.getDouble(buf8, byteOrder)
    }

    @Throws(IOException::class, ParseException::class)
    private fun read(buf: ByteArray) {
        val num = stream!!.read(buf)
        if (num < buf.size) {
            throw ParseException("Attempt to read past end of input")
        }
        bufLast = buf
        count += num.toLong()
    }
}
