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
package org.locationtech.jts.operation.overlay.validate
import kotlin.math.hypot

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.util.LinearComponentExtracter

/**
 * Generates points offset by a given distance
 * from both sides of the midpoint of
 * all segments in a [Geometry].
 * Can be used to generate probe points for
 * determining whether a polygonal overlay result
 * is incorrect.
 *
 * @author Martin Davis
 * @version 1.7
 */
class OffsetPointGenerator(private val g: Geometry) {

  private var doLeft = true
  private var doRight = true

  /**
   * Set the sides on which to generate offset points.
   */
  fun setSidesToGenerate(doLeft: Boolean, doRight: Boolean) {
    this.doLeft = doLeft
    this.doRight = doRight
  }

  /**
   * Gets the computed offset points.
   *
   * @return List&lt;Coordinate&gt;
   */
  fun getPoints(offsetDistance: Double): MutableList<Coordinate> {
    val offsetPts: MutableList<Coordinate> = ArrayList()
    val lines = LinearComponentExtracter.getLines(g)
    val i = lines.iterator()
    while (i.hasNext()) {
      val line = i.next() as LineString
      extractPoints(line, offsetDistance, offsetPts)
    }
    //System.out.println(toMultiPoint(offsetPts));
    return offsetPts
  }

  private fun extractPoints(line: LineString, offsetDistance: Double, offsetPts: MutableList<Coordinate>) {
    val pts = line.getCoordinates()
    for (i in 0 until pts.size - 1) {
      computeOffsetPoints(pts[i], pts[i + 1], offsetDistance, offsetPts)
    }
  }

  /**
   * Generates the two points which are offset from the
   * midpoint of the segment `(p0, p1)` by the
   * `offsetDistance`.
   *
   * @param p0 the first point of the segment to offset from
   * @param p1 the second point of the segment to offset from
   */
  private fun computeOffsetPoints(p0: Coordinate, p1: Coordinate, offsetDistance: Double, offsetPts: MutableList<Coordinate>) {
    val dx = p1.x - p0.x
    val dy = p1.y - p0.y
    val len = hypot(dx, dy)
    // u is the vector that is the length of the offset, in the direction of the segment
    val ux = offsetDistance * dx / len
    val uy = offsetDistance * dy / len

    val midX = (p1.x + p0.x) / 2
    val midY = (p1.y + p0.y) / 2

    if (doLeft) {
      val offsetLeft = Coordinate(midX - uy, midY + ux)
      offsetPts.add(offsetLeft)
    }

    if (doRight) {
      val offsetRight = Coordinate(midX + uy, midY - ux)
      offsetPts.add(offsetRight)
    }
  }
}
