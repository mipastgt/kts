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
import kotlin.random.Random

import org.locationtech.jts.algorithm.locate.IndexedPointInAreaLocator
import org.locationtech.jts.algorithm.locate.PointOnGeometryLocator
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Location
import org.locationtech.jts.geom.Polygonal
import org.locationtech.jts.shape.GeometricShapeBuilder

/**
 * Creates random point sets contained in a
 * region defined by either a rectangular or a polygonal extent.
 *
 * @author mbdavis
 */
open class RandomPointsBuilder : GeometricShapeBuilder {

  protected var maskPoly: Geometry? = null
  private var extentLocator: PointOnGeometryLocator? = null

  /**
   * Create a shape factory which will create shapes using the default
   * [GeometryFactory].
   */
  constructor() : super(GeometryFactory())

  /**
   * Create a shape factory which will create shapes using the given
   * [GeometryFactory].
   *
   * @param geomFact the factory to use
   */
  constructor(geomFact: GeometryFactory) : super(geomFact)

  /**
   * Sets a polygonal mask.
   *
   * @throws IllegalArgumentException if the mask is not polygonal
   */
  fun setExtent(mask: Geometry) {
    if (mask !is Polygonal)
      throw IllegalArgumentException("Only polygonal extents are supported")
    this.maskPoly = mask
    setExtent(mask.getEnvelopeInternal())
    extentLocator = IndexedPointInAreaLocator(mask)
  }

  override fun getGeometry(): Geometry {
    val pts = arrayOfNulls<Coordinate>(numPts)
    var i = 0
    while (i < numPts) {
      val p = createRandomCoord(getExtent()!!)
      if (extentLocator != null && !isInExtent(p))
        continue
      pts[i++] = p
    }
    @Suppress("UNCHECKED_CAST")
    return geomFactory.createMultiPointFromCoords(pts as Array<Coordinate>)
  }

  protected fun isInExtent(p: Coordinate): Boolean {
    if (extentLocator != null)
      return extentLocator!!.locate(p) != Location.EXTERIOR
    return getExtent()!!.contains(p)
  }

  protected override fun createCoord(x: Double, y: Double): Coordinate {
    val pt = Coordinate(x, y)
    geomFactory.getPrecisionModel().makePrecise(pt)
    return pt
  }

  protected fun createRandomCoord(env: Envelope): Coordinate {
    val x = env.getMinX() + env.getWidth() * Random.nextDouble()
    val y = env.getMinY() + env.getHeight() * Random.nextDouble()
    return createCoord(x, y)
  }
}
