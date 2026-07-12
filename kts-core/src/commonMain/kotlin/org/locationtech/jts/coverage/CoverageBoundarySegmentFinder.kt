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

import org.locationtech.jts.geom.CoordinateSequence
import org.locationtech.jts.geom.CoordinateSequenceFilter
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.LineSegment

/**
 * Finds coverage segments which occur in only a single coverage element.
 *
 * @author mdavis
 */
internal class CoverageBoundarySegmentFinder(private val boundarySegs: MutableSet<LineSegment>) : CoordinateSequenceFilter {

  override fun filter(seq: CoordinateSequence, i: Int) {
    //-- final point does not start a segment
    if (i >= seq.size() - 1)
      return
    val seg = createSegment(seq, i)
    /**
     * Records segments with an odd number of occurrences.
     */
    if (boundarySegs.contains(seg)) {
      boundarySegs.remove(seg)
    } else {
      boundarySegs.add(seg)
    }
  }

  override fun isDone(): Boolean {
    return false
  }

  override fun isGeometryChanged(): Boolean {
    return false
  }

  companion object {
    fun findBoundarySegments(geoms: Array<Geometry>): MutableSet<LineSegment> {
      val segs: MutableSet<LineSegment> = HashSet()
      val finder = CoverageBoundarySegmentFinder(segs)
      for (geom in geoms) {
        geom.apply(finder)
      }
      return segs
    }

    fun isBoundarySegment(boundarySegs: Set<LineSegment>, seq: CoordinateSequence, i: Int): Boolean {
      val seg = createSegment(seq, i)
      return boundarySegs.contains(seg)
    }

    private fun createSegment(seq: CoordinateSequence, i: Int): LineSegment {
      val seg = LineSegment(seq.getCoordinate(i), seq.getCoordinate(i + 1))
      seg.normalize()
      return seg
    }
  }
}
