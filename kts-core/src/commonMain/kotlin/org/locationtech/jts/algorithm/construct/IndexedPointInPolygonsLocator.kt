/*
 * Copyright (c) 2023 Martin Davis.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.locationtech.jts.algorithm.construct

import org.locationtech.jts.algorithm.locate.IndexedPointInAreaLocator
import org.locationtech.jts.algorithm.locate.PointOnGeometryLocator
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.Location
import org.locationtech.jts.geom.util.PolygonalExtracter
import org.locationtech.jts.index.strtree.STRtree

/**
 * Determines the location of a point in the polygonal elements of a geometry.
 * Uses spatial indexing to provide efficient performance.
 *
 * @author mdavis
 */
internal class IndexedPointInPolygonsLocator(private val geom: Geometry) : PointOnGeometryLocator {

  private var index: STRtree? = null

  private fun init() {
    if (index != null)
      return
    val polys = PolygonalExtracter.getPolygonals(geom)
    val idx = STRtree()
    for (i in 0 until polys.size) {
      val poly = polys[i]
      idx.insert(poly.getEnvelopeInternal(), IndexedPointInAreaLocator(poly))
    }
    index = idx
  }

  override fun locate(p: Coordinate): Int {
    init()

    val results = index!!.query(Envelope(p))
    for (o in results) {
      val ptLocater = o as IndexedPointInAreaLocator
      val loc = ptLocater.locate(p)
      if (loc != Location.EXTERIOR)
        return loc
    }
    return Location.EXTERIOR
  }
}
