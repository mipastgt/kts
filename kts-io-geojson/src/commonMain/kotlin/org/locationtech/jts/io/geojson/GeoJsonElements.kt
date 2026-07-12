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

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.double

// Small typed accessors over the kotlinx-serialization JSON tree, mirroring the unchecked casts the
// upstream json.simple-based reader performed on java.util.Map / java.util.List. A missing member or
// a JSON `null` value reads as `null` (upstream: absent key / JSON null both yielded a Java null),
// so callers reproduce the original null-handling (e.g. null coordinates -> empty geometry).

/** The string value of member [name], or null if absent, JSON null, or not a JSON string. */
internal fun JsonObject.string(name: String): String? {
    val prim = this[name] as? JsonPrimitive ?: return null
    return if (prim.isString) prim.content else null
}

/** The array value of member [name], or null if absent, JSON null, or not a JSON array. */
internal fun JsonObject.array(name: String): JsonArray? = this[name] as? JsonArray

/** The object value of member [name], or null if absent, JSON null, or not a JSON object. */
internal fun JsonObject.`object`(name: String): JsonObject? = this[name] as? JsonObject

/** This element as a `double` ordinate. Throws if it is not a JSON number/primitive. */
internal fun JsonElement.asDouble(): Double = (this as JsonPrimitive).double
