/*
 * Copyright (c) 2022 Martin Davis.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.locationtech.jts.coverage

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.Triangle
import org.locationtech.jts.simplify.LinkedLine

internal class Corner(private val edge: LinkedLine, i: Int, private val area: Double) : Comparable<Corner> {
  private val index: Int = i
  private val prev: Int = edge.prev(i)
  private val next: Int = edge.next(i)

  fun isVertex(index: Int): Boolean {
    return index == this.index
        || index == prev
        || index == next
  }

  fun getIndex(): Int {
    return index
  }

  fun getCoordinate(): Coordinate {
    return edge.getCoordinate(index)
  }

  fun getArea(): Double {
    return area
  }

  fun prev(): Coordinate {
    return edge.getCoordinate(prev)
  }

  fun next(): Coordinate {
    return edge.getCoordinate(next)
  }

  /**
   * Orders corners by increasing area.
   */
  override fun compareTo(o: Corner): Int {
    val comp = area.compareTo(o.area)
    if (comp != 0)
      return comp
    //-- ensure equal-area corners have a deterministic ordering
    return index.compareTo(o.index)
  }

  fun envelope(): Envelope {
    val pp = edge.getCoordinate(prev)
    val p = edge.getCoordinate(index)
    val pn = edge.getCoordinate(next)
    val env = Envelope(pp, pn)
    env.expandToInclude(p)
    return env
  }

  fun isVertex(v: Coordinate): Boolean {
    if (v.equals2D(edge.getCoordinate(prev))) return true
    if (v.equals2D(edge.getCoordinate(index))) return true
    if (v.equals2D(edge.getCoordinate(next))) return true
    return false
  }

  fun isBaseline(p0: Coordinate, p1: Coordinate): Boolean {
    val prev = prev()
    val next = next()
    if (prev.equals2D(p0) && next.equals2D(p1)) return true
    if (prev.equals2D(p1) && next.equals2D(p0)) return true
    return false
  }

  fun intersects(v: Coordinate): Boolean {
    val pp = edge.getCoordinate(prev)
    val p = edge.getCoordinate(index)
    val pn = edge.getCoordinate(next)
    return Triangle.intersects(pp, p, pn, v)
  }

  fun isRemoved(): Boolean {
    return edge.prev(index) != prev || edge.next(index) != next
  }

  fun toLineString(): LineString {
    val pp = edge.getCoordinate(prev)
    val p = edge.getCoordinate(index)
    val pn = edge.getCoordinate(next)
    return GeometryFactory().createLineString(
        arrayOf(safeCoord(pp), safeCoord(p), safeCoord(pn)))
  }

  override fun toString(): String {
    return toLineString().toString()
  }

  companion object {
    private fun safeCoord(p: Coordinate?): Coordinate {
      if (p == null) return Coordinate(Double.NaN, Double.NaN)
      return p
    }
  }
}
