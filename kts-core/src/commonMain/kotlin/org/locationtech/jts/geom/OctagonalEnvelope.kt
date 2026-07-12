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
package org.locationtech.jts.geom

import kotlin.jvm.JvmStatic
import kotlin.math.sqrt

/**
 * A bounding container for a {@link Geometry} which is in the shape of a general octagon.
 * The OctagonalEnvelope of a geometric object
 * is a geometry which is a tight bound
 * along the (up to) four extremal rectilinear parallels
 * and along the (up to) four extremal diagonal parallels.
 * Depending on the shape of the contained
 * geometry, the octagon may be degenerate to any extreme
 * (e.g. it may be a rectangle, a line, or a point).
 */
open class OctagonalEnvelope {

  // initialize in the null state
  private var minX = Double.NaN
  private var maxX = 0.0
  private var minY = 0.0
  private var maxY = 0.0
  private var minA = 0.0
  private var maxA = 0.0
  private var minB = 0.0
  private var maxB = 0.0

  /**
   * Creates a new null bounding octagon
   */
  constructor()

  /**
   * Creates a new null bounding octagon bounding a {@link Coordinate}
   *
   * @param p the coordinate to bound
   */
  constructor(p: Coordinate) {
    expandToInclude(p)
  }

  /**
   * Creates a new null bounding octagon bounding a pair of {@link Coordinate}s
   *
   * @param p0 a coordinate to bound
   * @param p1 a coordinate to bound
   */
  constructor(p0: Coordinate, p1: Coordinate) {
    expandToInclude(p0)
    expandToInclude(p1)
  }

  /**
   * Creates a new null bounding octagon bounding an {@link Envelope}
   */
  constructor(env: Envelope) {
    expandToInclude(env)
  }

  /**
   * Creates a new null bounding octagon bounding an {@link OctagonalEnvelope}
   * (the copy constructor).
   */
  constructor(oct: OctagonalEnvelope) {
    expandToInclude(oct)
  }

  /**
   * Creates a new null bounding octagon bounding a {@link Geometry}
   */
  constructor(geom: Geometry) {
    expandToInclude(geom)
  }

  fun getMinX(): Double {
    return minX
  }

  fun getMaxX(): Double {
    return maxX
  }

  fun getMinY(): Double {
    return minY
  }

  fun getMaxY(): Double {
    return maxY
  }

  fun getMinA(): Double {
    return minA
  }

  fun getMaxA(): Double {
    return maxA
  }

  fun getMinB(): Double {
    return minB
  }

  fun getMaxB(): Double {
    return maxB
  }

  fun isNull(): Boolean {
    return minX.isNaN()
  }

  /**
   *  Sets the value of this object to the null value
   */
  fun setToNull() {
    minX = Double.NaN
  }

  fun expandToInclude(g: Geometry) {
    g.apply(BoundingOctagonComponentFilter(this))
  }

  fun expandToInclude(seq: CoordinateSequence): OctagonalEnvelope {
    for (i in 0 until seq.size()) {
      val x = seq.getX(i)
      val y = seq.getY(i)
      expandToInclude(x, y)
    }
    return this
  }

  fun expandToInclude(oct: OctagonalEnvelope): OctagonalEnvelope {
    if (oct.isNull()) return this

    if (isNull()) {
      minX = oct.minX
      maxX = oct.maxX
      minY = oct.minY
      maxY = oct.maxY
      minA = oct.minA
      maxA = oct.maxA
      minB = oct.minB
      maxB = oct.maxB
      return this
    }
    if (oct.minX < minX) minX = oct.minX
    if (oct.maxX > maxX) maxX = oct.maxX
    if (oct.minY < minY) minY = oct.minY
    if (oct.maxY > maxY) maxY = oct.maxY
    if (oct.minA < minA) minA = oct.minA
    if (oct.maxA > maxA) maxA = oct.maxA
    if (oct.minB < minB) minB = oct.minB
    if (oct.maxB > maxB) maxB = oct.maxB
    return this
  }

  fun expandToInclude(p: Coordinate): OctagonalEnvelope {
    expandToInclude(p.x, p.y)
    return this
  }

  fun expandToInclude(env: Envelope): OctagonalEnvelope {
    expandToInclude(env.getMinX(), env.getMinY())
    expandToInclude(env.getMinX(), env.getMaxY())
    expandToInclude(env.getMaxX(), env.getMinY())
    expandToInclude(env.getMaxX(), env.getMaxY())
    return this
  }

  fun expandToInclude(x: Double, y: Double): OctagonalEnvelope {
    val A = computeA(x, y)
    val B = computeB(x, y)

    if (isNull()) {
      minX = x
      maxX = x
      minY = y
      maxY = y
      minA = A
      maxA = A
      minB = B
      maxB = B
    } else {
      if (x < minX) minX = x
      if (x > maxX) maxX = x
      if (y < minY) minY = y
      if (y > maxY) maxY = y
      if (A < minA) minA = A
      if (A > maxA) maxA = A
      if (B < minB) minB = B
      if (B > maxB) maxB = B
    }
    return this
  }

  fun expandBy(distance: Double) {
    if (isNull()) return

    val diagonalDistance = SQRT2 * distance

    minX -= distance
    maxX += distance
    minY -= distance
    maxY += distance
    minA -= diagonalDistance
    maxA += diagonalDistance
    minB -= diagonalDistance
    maxB += diagonalDistance

    if (!isValid())
      setToNull()
  }

  /**
   * Tests if the extremal values for this octagon are valid.
   *
   * @return <code>true</code> if this object has valid values
   */
  private fun isValid(): Boolean {
    if (isNull()) return true
    return minX <= maxX &&
      minY <= maxY &&
      minA <= maxA &&
      minB <= maxB
  }

  fun intersects(other: OctagonalEnvelope): Boolean {
    if (isNull() || other.isNull()) {
      return false
    }

    if (minX > other.maxX) return false
    if (maxX < other.minX) return false
    if (minY > other.maxY) return false
    if (maxY < other.minY) return false
    if (minA > other.maxA) return false
    if (maxA < other.minA) return false
    if (minB > other.maxB) return false
    if (maxB < other.minB) return false
    return true
  }

  fun intersects(p: Coordinate): Boolean {
    if (minX > p.x) return false
    if (maxX < p.x) return false
    if (minY > p.y) return false
    if (maxY < p.y) return false

    val A = computeA(p.x, p.y)
    val B = computeB(p.x, p.y)
    if (minA > A) return false
    if (maxA < A) return false
    if (minB > B) return false
    if (maxB < B) return false
    return true
  }

  fun contains(other: OctagonalEnvelope): Boolean {
    if (isNull() || other.isNull()) {
      return false
    }

    return other.minX >= minX &&
      other.maxX <= maxX &&
      other.minY >= minY &&
      other.maxY <= maxY &&
      other.minA >= minA &&
      other.maxA <= maxA &&
      other.minB >= minB &&
      other.maxB <= maxB
  }

  fun toGeometry(geomFactory: GeometryFactory): Geometry {
    if (isNull()) {
      return geomFactory.createPoint()
    }

    val px00 = Coordinate(minX, minA - minX)
    val px01 = Coordinate(minX, minX - minB)

    val px10 = Coordinate(maxX, maxX - maxB)
    val px11 = Coordinate(maxX, maxA - maxX)

    val py00 = Coordinate(minA - minY, minY)
    val py01 = Coordinate(minY + maxB, minY)

    val py10 = Coordinate(maxY + minB, maxY)
    val py11 = Coordinate(maxA - maxY, maxY)

    val pm = geomFactory.getPrecisionModel()
    pm.makePrecise(px00)
    pm.makePrecise(px01)
    pm.makePrecise(px10)
    pm.makePrecise(px11)
    pm.makePrecise(py00)
    pm.makePrecise(py01)
    pm.makePrecise(py10)
    pm.makePrecise(py11)

    val coordList = CoordinateList()
    coordList.add(px00, false)
    coordList.add(px01, false)
    coordList.add(py10, false)
    coordList.add(py11, false)
    coordList.add(px11, false)
    coordList.add(px10, false)
    coordList.add(py01, false)
    coordList.add(py00, false)

    if (coordList.size == 1) {
      return geomFactory.createPoint(px00)
    }
    if (coordList.size == 2) {
      val pts = coordList.toCoordinateArray()
      return geomFactory.createLineString(pts)
    }
    // must be a polygon, so add closing point
    coordList.add(px00, false)
    val pts = coordList.toCoordinateArray()
    return geomFactory.createPolygon(geomFactory.createLinearRing(pts))
  }

  private class BoundingOctagonComponentFilter(val oe: OctagonalEnvelope) : GeometryComponentFilter {
    override fun filter(geom: Geometry) {
      if (geom is LineString) {
        oe.expandToInclude(geom.getCoordinateSequence())
      } else if (geom is Point) {
        oe.expandToInclude(geom.getCoordinateSequence())
      }
    }
  }

  companion object {
    /**
     * Gets the octagonal envelope of a geometry
     * @param geom the geometry
     * @return the octagonal envelope of the geometry
     */
    @JvmStatic
    fun octagonalEnvelope(geom: Geometry): Geometry {
      return (OctagonalEnvelope(geom)).toGeometry(geom.getFactory())
    }

    private fun computeA(x: Double, y: Double): Double {
      return x + y
    }

    private fun computeB(x: Double, y: Double): Double {
      return x - y
    }

    private val SQRT2 = sqrt(2.0)
  }
}
