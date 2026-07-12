/*
 * Copyright (c) 2021 Martin Davis.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.locationtech.jts.triangulate.tri

import kotlin.jvm.JvmField

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.io.WKTWriter

/**
 * Represents an edge in a [Tri],
 * to be used as a key for looking up Tris
 * while building a triangulation.
 * The edge value is normalized to allow lookup
 * of adjacent triangles.
 *
 * @author mdavis
 */
internal class TriEdge(a: Coordinate, b: Coordinate) {
  @JvmField
  var p0: Coordinate = a
  @JvmField
  var p1: Coordinate = b

  init {
    normalize()
  }

  private fun normalize() {
    if (p0.compareTo(p1) < 0) {
      val tmp = p0
      p0 = p1
      p1 = tmp
    }
  }

  override fun hashCode(): Int {
    var result = 17
    result = 37 * result + Coordinate.hashCode(p0.x)
    result = 37 * result + Coordinate.hashCode(p1.x)
    result = 37 * result + Coordinate.hashCode(p0.y)
    result = 37 * result + Coordinate.hashCode(p1.y)
    return result
  }

  override fun equals(arg: Any?): Boolean {
    if (arg !is TriEdge)
      return false
    val other = arg
    if (p0.equals(other.p0) && p1.equals(other.p1))
      return true
    return false
  }

  override fun toString(): String {
    return WKTWriter.toLineString(arrayOf(p0, p1))
  }
}
