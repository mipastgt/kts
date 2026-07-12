/*
 * Copyright (c) 2019 Martin Davis.
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

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LineSegment
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.shape.GeometricShapeBuilder

/**
 * Generates a [LineString] representing the Morton Curve
 * at a given level.
 *
 * @author Martin Davis
 * @see MortonCode
 */
open class MortonCurveBuilder(geomFactory: GeometryFactory) : GeometricShapeBuilder(geomFactory) {

  init {
    // use a null extent to indicate no transformation
    // (may be set by client)
    extent = null
  }

  /**
   * Sets the level of curve to generate.
   * The level must be in the range [0 - 16].
   * This determines the
   * number of points in the generated curve.
   *
   * @param level the level of the curve
   */
  fun setLevel(level: Int) {
    this.numPts = MortonCode.size(level)
  }

  override fun getGeometry(): Geometry {
    val level = MortonCode.level(numPts)
    val nPts = MortonCode.size(level)

    var scale = 1.0
    var baseX = 0.0
    var baseY = 0.0
    if (extent != null) {
      val baseLine = getSquareBaseLine()
      baseX = baseLine.minX()
      baseY = baseLine.minY()
      val width = baseLine.getLength()
      val maxOrdinate = MortonCode.maxOrdinate(level)
      scale = width / maxOrdinate
    }

    val pts = arrayOfNulls<Coordinate>(nPts)
    for (i in 0 until nPts) {
      val pt = MortonCode.decode(i)
      val x = transform(pt.getX(), scale, baseX)
      val y = transform(pt.getY(), scale, baseY)
      pts[i] = Coordinate(x, y)
    }
    @Suppress("UNCHECKED_CAST")
    return geomFactory.createLineString(pts as Array<Coordinate>)
  }

  companion object {
    private fun transform(value: Double, scale: Double, offset: Double): Double {
      return value * scale + offset
    }
  }
}
