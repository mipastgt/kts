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

package org.locationtech.jts.shape

import kotlin.jvm.JvmField
import kotlin.math.min

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LineSegment

abstract class GeometricShapeBuilder(protected val geomFactory: GeometryFactory) {

  @JvmField
  protected var extent: Envelope? = Envelope(0.0, 1.0, 0.0, 1.0)

  @JvmField
  protected var numPts = 0

  open fun setExtent(extent: Envelope) {
    this.extent = extent
  }

  open fun getExtent(): Envelope? {
    return extent
  }

  open fun getCentre(): Coordinate? {
    return extent!!.centre()
  }

  open fun getDiameter(): Double {
    return min(extent!!.getHeight(), extent!!.getWidth())
  }

  open fun getRadius(): Double {
    return getDiameter() / 2
  }

  open fun getSquareBaseLine(): LineSegment {
    val radius = getRadius()

    val centre = getCentre()!!
    val p0 = Coordinate(centre.x - radius, centre.y - radius)
    val p1 = Coordinate(centre.x + radius, centre.y - radius)
    return LineSegment(p0, p1)
  }

  open fun getSquareExtent(): Envelope {
    val radius = getRadius()

    val centre = getCentre()!!
    return Envelope(centre.x - radius, centre.x + radius,
        centre.y - radius, centre.y + radius)
  }

  /**
   * Sets the total number of points in the created [Geometry].
   * The created geometry will have no more than this number of points,
   * unless more are needed to create a valid geometry.
   */
  open fun setNumPoints(numPts: Int) {
    this.numPts = numPts
  }

  abstract fun getGeometry(): Geometry

  protected open fun createCoord(x: Double, y: Double): Coordinate {
    val pt = Coordinate(x, y)
    geomFactory.getPrecisionModel().makePrecise(pt)
    return pt
  }
}
