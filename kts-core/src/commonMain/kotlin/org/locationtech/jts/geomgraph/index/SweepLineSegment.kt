/*
 * Copyright (c) 2016 Vivid Solutions.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.locationtech.jts.geomgraph.index

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geomgraph.Edge

class SweepLineSegment(private val edge: Edge, private val ptIndex: Int) {

  private val pts: Array<Coordinate> = edge.getCoordinates()

  fun getMinX(): Double {
    val x1 = pts[ptIndex].x
    val x2 = pts[ptIndex + 1].x
    return if (x1 < x2) x1 else x2
  }

  fun getMaxX(): Double {
    val x1 = pts[ptIndex].x
    val x2 = pts[ptIndex + 1].x
    return if (x1 > x2) x1 else x2
  }

  fun computeIntersections(ss: SweepLineSegment, si: SegmentIntersector) {
    si.addIntersections(edge, ptIndex, ss.edge, ss.ptIndex)
  }
}
