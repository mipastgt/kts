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

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.CoordinateSequence
import org.locationtech.jts.geom.CoordinateSequenceFilter
import org.locationtech.jts.geom.CoordinateSequences
import org.locationtech.jts.geom.Geometry

/**
 * Counts the number of rings containing each vertex.
 *
 * @author mdavis
 */
internal class VertexRingCounter(private val vertexRingCount: MutableMap<Coordinate, Int>) : CoordinateSequenceFilter {

  override fun filter(seq: CoordinateSequence, i: Int) {
    //-- for rings don't double-count duplicate endpoint
    if (CoordinateSequences.isRing(seq) && i == 0)
      return
    val v = seq.getCoordinate(i)
    var count = if (vertexRingCount.containsKey(v)) vertexRingCount[v]!! else 0
    count++
    vertexRingCount.put(v, count)
  }

  override fun isDone(): Boolean {
    return false
  }

  override fun isGeometryChanged(): Boolean {
    return false
  }

  companion object {
    fun count(geoms: Array<Geometry>): MutableMap<Coordinate, Int> {
      val vertexRingCount: MutableMap<Coordinate, Int> = HashMap()
      val counter = VertexRingCounter(vertexRingCount)
      for (geom in geoms) {
        geom.apply(counter)
      }
      return vertexRingCount
    }
  }
}
