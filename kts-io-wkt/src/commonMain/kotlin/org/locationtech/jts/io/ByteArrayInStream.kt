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
 * Allows an array of bytes to be used as an [InStream].
 * To optimize memory usage, instances can be reused with different byte arrays.
 */
class ByteArrayInStream(buffer: ByteArray) : InStream {
    /*
     * Implementation improvement suggested by Andrea Aime - Dec 15 2007
     */

    private var buffer: ByteArray = buffer
    private var position: Int = 0

    /**
     * Sets this stream to read from the given buffer.
     *
     * @param buffer the bytes to read
     */
    fun setBytes(buffer: ByteArray) {
        this.buffer = buffer
        this.position = 0
    }

    /**
     * Reads up to `buf.size` bytes from the stream into the given byte buffer.
     *
     * @param buf the buffer to place the read bytes into
     * @return the number of bytes read
     */
    override fun read(buf: ByteArray): Int {
        var numToRead = buf.size
        // don't try and copy past the end of the input
        if (position + numToRead > buffer.size) {
            numToRead = buffer.size - position
            buffer.copyInto(buf, 0, position, position + numToRead)
            // zero out the unread bytes
            for (i in numToRead until buf.size) {
                buf[i] = 0
            }
        } else {
            buffer.copyInto(buf, 0, position, position + numToRead)
        }
        position += numToRead
        return numToRead
    }
}
