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
 * Multiplatform stand-in for `java.io.IOException`. On the JVM this is a `typealias` to the real
 * `java.io.IOException`, so the JVM ABI, `throws` clauses, and Java `catch (IOException)` blocks are
 * unchanged. On other platforms it is a plain [Exception]. Only the JVM stream/file adapters ever
 * actually throw it; the common `String`/`ByteArray` parse path does not.
 */
expect open class IOException : Exception {
    constructor()
    constructor(message: String?)
}
