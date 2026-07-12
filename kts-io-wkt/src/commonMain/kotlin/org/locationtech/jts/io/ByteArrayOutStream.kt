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
 * A growable in-memory [OutStream] accumulating bytes into a byte array. Common-Kotlin replacement
 * for the `java.io.ByteArrayOutputStream` + [org.locationtech.jts.io.OutStream] adapter that
 * [WKBWriter.write] used for the `ByteArray`-returning path.
 */
internal class ByteArrayOutStream : OutStream {
    private var buf = ByteArray(32)
    private var count = 0

    override fun write(buf: ByteArray, len: Int) {
        ensureCapacity(count + len)
        buf.copyInto(this.buf, count, 0, len)
        count += len
    }

    fun reset() {
        count = 0
    }

    fun toByteArray(): ByteArray = buf.copyOf(count)

    private fun ensureCapacity(minCapacity: Int) {
        if (minCapacity > buf.size) {
            var newLen = buf.size * 2
            while (newLen < minCapacity) {
                newLen *= 2
            }
            buf = buf.copyOf(newLen)
        }
    }
}
