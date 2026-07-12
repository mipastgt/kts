/*
 * Copyright (c) 2016 Martin Davis.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.locationtech.jts.shape.fractal

import kotlin.jvm.JvmStatic
import kotlin.math.PI
import kotlin.math.ln
import kotlin.math.sin

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.CoordinateList
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LineSegment
import org.locationtech.jts.math.Vector2D
import org.locationtech.jts.shape.GeometricShapeBuilder

open class KochSnowflakeBuilder(geomFactory: GeometryFactory) : GeometricShapeBuilder(geomFactory) {

  private val coordList = CoordinateList()

  override fun getGeometry(): Geometry {
    val level = recursionLevelForSize(numPts)
    val baseLine = getSquareBaseLine()
    val pts = getBoundary(level, baseLine.getCoordinate(0), baseLine.getLength())
    return geomFactory.createPolygon(
        geomFactory.createLinearRing(pts), null)
  }

  private fun getBoundary(level: Int, origin: Coordinate, width: Double): Array<Coordinate> {
    var y = origin.y
    // for all levels beyond 0 need to vertically shift shape by height of one "arm" to centre it
    if (level > 0) {
      y += THIRD_HEIGHT * width
    }

    val p0 = Coordinate(origin.x, y)
    val p1 = Coordinate(origin.x + width / 2, y + width * HEIGHT_FACTOR)
    val p2 = Coordinate(origin.x + width, y)
    addSide(level, p0, p1)
    addSide(level, p1, p2)
    addSide(level, p2, p0)
    coordList.closeRing()
    return coordList.toCoordinateArray()
  }

  fun addSide(level: Int, p0: Coordinate, p1: Coordinate) {
    if (level == 0)
      addSegment(p0, p1)
    else {
      val base = Vector2D.create(p0, p1)
      val midPt = base.multiply(0.5).translate(p0)

      val heightVec = base.multiply(THIRD_HEIGHT)
      val offsetVec = heightVec.rotateByQuarterCircle(1)
      val offsetPt = offsetVec.translate(midPt)

      val n2 = level - 1
      val thirdPt = base.multiply(ONE_THIRD).translate(p0)
      val twoThirdPt = base.multiply(TWO_THIRDS).translate(p0)

      // construct sides recursively
      addSide(n2, p0, thirdPt)
      addSide(n2, thirdPt, offsetPt)
      addSide(n2, offsetPt, twoThirdPt)
      addSide(n2, twoThirdPt, p1)
    }
  }

  private fun addSegment(p0: Coordinate, p1: Coordinate) {
    coordList.add(p1)
  }

  companion object {
    @JvmStatic
    fun recursionLevelForSize(numPts: Int): Int {
      val pow4 = (numPts / 3).toDouble()
      val exp = ln(pow4) / ln(4.0)
      return exp.toInt()
    }

    /**
     * The height of an equilateral triangle of side one
     */
    private val HEIGHT_FACTOR = sin(PI / 3.0)
    private val ONE_THIRD = 1.0 / 3.0
    private val THIRD_HEIGHT = HEIGHT_FACTOR / 3.0
    private val TWO_THIRDS = 2.0 / 3.0
  }
}
