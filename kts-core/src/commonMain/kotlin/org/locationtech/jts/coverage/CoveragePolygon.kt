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

import org.locationtech.jts.algorithm.locate.IndexedPointInAreaLocator
import org.locationtech.jts.algorithm.locate.PointOnGeometryLocator
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.geom.Location
import org.locationtech.jts.geom.Polygon

internal class CoveragePolygon(private val polygon: Polygon) {

  private val polyEnv: Envelope = polygon.getEnvelopeInternal()
  private var locator: IndexedPointInAreaLocator? = null

  fun intersectsEnv(env: Envelope): Boolean {
    //-- test intersection explicitly to avoid expensive null check
    return !(env.getMinX() > polyEnv.getMaxX()
        || env.getMaxX() < polyEnv.getMinX()
        || env.getMinY() > polyEnv.getMaxY()
        || env.getMaxY() < polyEnv.getMinY())
  }

  private fun intersectsEnv(p: Coordinate): Boolean {
    //-- test intersection explicitly to avoid expensive null check
    return !(p.x > polyEnv.getMaxX() ||
        p.x < polyEnv.getMinX() ||
        p.y > polyEnv.getMaxY() ||
        p.y < polyEnv.getMinY())
  }

  fun contains(p: Coordinate): Boolean {
    if (!intersectsEnv(p))
      return false
    val pia = getLocator()
    return Location.INTERIOR == pia.locate(p)
  }

  private fun getLocator(): PointOnGeometryLocator {
    if (locator == null) {
      locator = IndexedPointInAreaLocator(polygon)
    }
    return locator!!
  }
}
