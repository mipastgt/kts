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

package org.locationtech.jts.simplify

import kotlin.jvm.JvmField
import kotlin.math.abs

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.CoordinateList
import org.locationtech.jts.geom.Triangle

/**
 * Simplifies a linestring (sequence of points) using the
 * Visvalingam-Whyatt algorithm.
 *
 */
internal class VWLineSimplifier(private val pts: Array<Coordinate>, distanceTolerance: Double) {

  private val tolerance: Double = distanceTolerance * distanceTolerance

  fun simplify(): Array<Coordinate> {
    val vwLine = VWVertex.buildLine(pts)
    var minArea = tolerance
    do {
      minArea = simplifyVertex(vwLine)
    } while (minArea < tolerance)
    val simp = vwLine.getCoordinates()
    // ensure computed value is a valid line
    if (simp.size < 2) {
      return arrayOf(simp[0], Coordinate(simp[0]))
    }
    return simp
  }

  private fun simplifyVertex(vwLine: VWVertex): Double {
    /**
     * Scan vertices in line and remove the one with smallest effective area.
     */
    var curr: VWVertex? = vwLine
    var minArea = curr!!.getArea()
    var minVertex: VWVertex? = null
    while (curr != null) {
      val area = curr.getArea()
      if (area < minArea) {
        minArea = area
        minVertex = curr
      }
      curr = curr.getNext()
    }
    if (minVertex != null && minArea < tolerance) {
      minVertex.remove()
    }
    if (!vwLine.isLive()) return -1.0
    return minArea
  }

  class VWVertex(private val pt: Coordinate) {
    private var prev: VWVertex? = null
    private var next: VWVertex? = null
    private var area = MAX_AREA
    private var live = true

    fun setPrev(prev: VWVertex?) {
      this.prev = prev
    }

    fun setNext(next: VWVertex?) {
      this.next = next
    }

    // accessor for the enclosing class (Kotlin cannot see a nested class's private members)
    fun getNext(): VWVertex? = next

    fun updateArea() {
      if (prev == null || next == null) {
        area = MAX_AREA
        return
      }
      area = abs(Triangle.area(prev!!.pt, pt, next!!.pt))
    }

    fun getArea(): Double {
      return area
    }

    fun isLive(): Boolean {
      return live
    }

    fun remove(): VWVertex? {
      val tmpPrev = prev
      val tmpNext = next
      var result: VWVertex? = null
      if (prev != null) {
        prev!!.setNext(tmpNext)
        prev!!.updateArea()
        result = prev
      }
      if (next != null) {
        next!!.setPrev(tmpPrev)
        next!!.updateArea()
        if (result == null)
          result = next
      }
      live = false
      return result
    }

    fun getCoordinates(): Array<Coordinate> {
      val coords = CoordinateList()
      var curr: VWVertex? = this
      do {
        coords.add(curr!!.pt, false)
        curr = curr.next
      } while (curr != null)
      return coords.toCoordinateArray()
    }

    companion object {
      @JvmField
      var MAX_AREA = Double.MAX_VALUE

      fun buildLine(pts: Array<Coordinate>): VWVertex {
        var first: VWVertex? = null
        var prev: VWVertex? = null
        for (i in pts.indices) {
          val v = VWVertex(pts[i])
          if (first == null)
            first = v
          v.setPrev(prev)
          if (prev != null) {
            prev.setNext(v)
            prev.updateArea()
          }
          prev = v
        }
        return first!!
      }
    }
  }

  companion object {
    fun simplify(pts: Array<Coordinate>, distanceTolerance: Double): Array<Coordinate> {
      val simp = VWLineSimplifier(pts, distanceTolerance)
      return simp.simplify()
    }
  }
}
