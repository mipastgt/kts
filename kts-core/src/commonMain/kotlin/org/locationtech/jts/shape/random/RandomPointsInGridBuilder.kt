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

package org.locationtech.jts.shape.random
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.MultiPoint
import org.locationtech.jts.math.MathUtil
import org.locationtech.jts.shape.GeometricShapeBuilder

/**
 * Creates random point sets
 * where the points are constrained to lie in the cells of a grid.
 *
 * @author mbdavis
 */
open class RandomPointsInGridBuilder : GeometricShapeBuilder {

  private var isConstrainedToCircle = false
  private var gutterFraction = 0.0

  /**
   * Create a builder which will create shapes using the default
   * [GeometryFactory].
   */
  constructor() : super(GeometryFactory())

  /**
   * Create a builder which will create shapes using the given
   * [GeometryFactory].
   *
   * @param geomFact the factory to use
   */
  constructor(geomFact: GeometryFactory) : super(geomFact)

  /**
   * Sets whether generated points are constrained to lie
   * within a circle contained within each grid cell.
   * This provides greater separation between points
   * in adjacent cells.
   *
   * The default is to not be constrained to a circle.
   */
  fun setConstrainedToCircle(isConstrainedToCircle: Boolean) {
    this.isConstrainedToCircle = isConstrainedToCircle
  }

  /**
   * Sets the fraction of the grid cell side which will be treated as
   * a gutter, in which no points will be created.
   * The provided value is clamped to the range [0.0, 1.0].
   */
  fun setGutterFraction(gutterFraction: Double) {
    this.gutterFraction = gutterFraction
  }

  /**
   * Gets the [MultiPoint] containing the generated point
   *
   * @return a MultiPoint
   */
  override fun getGeometry(): Geometry {
    var nCells = sqrt(numPts.toDouble()).toInt()
    // ensure that at least numPts points are generated
    if (nCells * nCells < numPts)
      nCells += 1

    val gridDX = getExtent()!!.getWidth() / nCells
    val gridDY = getExtent()!!.getHeight() / nCells

    val gutterFrac = MathUtil.clamp(gutterFraction, 0.0, 1.0)
    val gutterOffsetX = gridDX * gutterFrac / 2
    val gutterOffsetY = gridDY * gutterFrac / 2
    val cellFrac = 1.0 - gutterFrac
    val cellDX = cellFrac * gridDX
    val cellDY = cellFrac * gridDY

    val pts = arrayOfNulls<Coordinate>(nCells * nCells)
    var index = 0
    for (i in 0 until nCells) {
      for (j in 0 until nCells) {
        val orgX = getExtent()!!.getMinX() + i * gridDX + gutterOffsetX
        val orgY = getExtent()!!.getMinY() + j * gridDY + gutterOffsetY
        pts[index++] = randomPointInCell(orgX, orgY, cellDX, cellDY)
      }
    }
    @Suppress("UNCHECKED_CAST")
    return geomFactory.createMultiPointFromCoords(pts as Array<Coordinate>)
  }

  private fun randomPointInCell(orgX: Double, orgY: Double, xLen: Double, yLen: Double): Coordinate {
    if (isConstrainedToCircle) {
      return randomPointInCircle(
          orgX,
          orgY,
          xLen, yLen)
    }
    return randomPointInGridCell(orgX, orgY, xLen, yLen)
  }

  private fun randomPointInGridCell(orgX: Double, orgY: Double, xLen: Double, yLen: Double): Coordinate {
    val x = orgX + xLen * Random.nextDouble()
    val y = orgY + yLen * Random.nextDouble()
    return createCoord(x, y)
  }

  companion object {
    private fun randomPointInCircle(orgX: Double, orgY: Double, width: Double, height: Double): Coordinate {
      val centreX = orgX + width / 2
      val centreY = orgY + height / 2

      val rndAng = 2 * PI * Random.nextDouble()
      val rndRadius = Random.nextDouble()
      // use square root of radius, since area is proportional to square of radius
      val rndRadius2 = sqrt(rndRadius)
      val rndX = width / 2 * rndRadius2 * cos(rndAng)
      val rndY = height / 2 * rndRadius2 * sin(rndAng)

      val x0 = centreX + rndX
      val y0 = centreY + rndY
      return Coordinate(x0, y0)
    }
  }
}
