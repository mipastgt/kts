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
import kotlin.math.ln

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LineSegment
import org.locationtech.jts.geom.LinearRing
import org.locationtech.jts.geom.Polygon
import org.locationtech.jts.shape.GeometricShapeBuilder

class SierpinskiCarpetBuilder(geomFactory: GeometryFactory) : GeometricShapeBuilder(geomFactory) {

  override fun getGeometry(): Geometry {
    val level = recursionLevelForSize(numPts)
    val baseLine = getSquareBaseLine()
    val origin = baseLine.getCoordinate(0)
    val holes = getHoles(level, origin.x, origin.y, getDiameter())
    val shell = (geomFactory.toGeometry(getSquareExtent()) as Polygon).getExteriorRing()
    return geomFactory.createPolygon(
        shell, holes)
  }

  private fun getHoles(n: Int, originX: Double, originY: Double, width: Double): Array<LinearRing> {
    val holeList: MutableList<Any?> = ArrayList()

    addHoles(n, originX, originY, width, holeList)

    return GeometryFactory.toLinearRingArray(holeList)
  }

  private fun addHoles(n: Int, originX: Double, originY: Double, width: Double, holeList: MutableList<Any?>) {
    if (n < 0) return
    val n2 = n - 1
    val widthThird = width / 3.0
    addHoles(n2, originX, originY, widthThird, holeList)
    addHoles(n2, originX + widthThird, originY, widthThird, holeList)
    addHoles(n2, originX + 2 * widthThird, originY, widthThird, holeList)

    addHoles(n2, originX, originY + widthThird, widthThird, holeList)
    addHoles(n2, originX + 2 * widthThird, originY + widthThird, widthThird, holeList)

    addHoles(n2, originX, originY + 2 * widthThird, widthThird, holeList)
    addHoles(n2, originX + widthThird, originY + 2 * widthThird, widthThird, holeList)
    addHoles(n2, originX + 2 * widthThird, originY + 2 * widthThird, widthThird, holeList)

    // add the centre hole
    holeList.add(createSquareHole(originX + widthThird, originY + widthThird, widthThird))
  }

  private fun createSquareHole(x: Double, y: Double, width: Double): LinearRing {
    val pts = arrayOf(
        Coordinate(x, y),
        Coordinate(x + width, y),
        Coordinate(x + width, y + width),
        Coordinate(x, y + width),
        Coordinate(x, y)
    )
    return geomFactory.createLinearRing(pts)
  }

  companion object {
    @JvmStatic
    fun recursionLevelForSize(numPts: Int): Int {
      val pow4 = (numPts / 3).toDouble()
      val exp = ln(pow4) / ln(4.0)
      return exp.toInt()
    }
  }
}
