/*
 * Copyright (c) 2020 Martin Davis
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.locationtech.jts.operation.overlayng

import org.locationtech.jts.algorithm.PointLocator
import org.locationtech.jts.algorithm.locate.PointOnGeometryLocator
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry

/**
 * Locates points on a linear geometry,
 * using a spatial index to provide good performance.
 *
 * @author mdavis
 */
class IndexedPointOnLineLocator(private val inputGeom: Geometry) : PointOnGeometryLocator {

  override fun locate(p: Coordinate): Int {
    // TODO: optimize this with a segment index
    val locator = PointLocator()
    return locator.locate(p, inputGeom)
  }
}
