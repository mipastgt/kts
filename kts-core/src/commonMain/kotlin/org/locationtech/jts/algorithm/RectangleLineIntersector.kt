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
package org.locationtech.jts.algorithm

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Envelope

/**
 * Computes whether a rectangle intersects line segments.
 *
 * @author Martin Davis
 *
 */
class RectangleLineIntersector(private val rectEnv: Envelope) {
  // for intersection testing, don't need to set precision model
  private val li: LineIntersector = RobustLineIntersector()

  private val diagUp0: Coordinate
  private val diagUp1: Coordinate
  private val diagDown0: Coordinate
  private val diagDown1: Coordinate

  init {
    /*
     * Up and Down are the diagonal orientations
     * relative to the Left side of the rectangle.
     * Index 0 is the left side, 1 is the right side.
     */
    diagUp0 = Coordinate(rectEnv.getMinX(), rectEnv.getMinY())
    diagUp1 = Coordinate(rectEnv.getMaxX(), rectEnv.getMaxY())
    diagDown0 = Coordinate(rectEnv.getMinX(), rectEnv.getMaxY())
    diagDown1 = Coordinate(rectEnv.getMaxX(), rectEnv.getMinY())
  }

  /**
   * Tests whether the query rectangle intersects a
   * given line segment.
   *
   * @param p0 the first endpoint of the segment
   * @param p1 the second endpoint of the segment
   * @return true if the rectangle intersects the segment
   */
  fun intersects(p0: Coordinate, p1: Coordinate): Boolean {
    var p0 = p0
    var p1 = p1
    // TODO: confirm that checking envelopes first is faster

    /**
     * If the segment envelope is disjoint from the
     * rectangle envelope, there is no intersection
     */
    val segEnv = Envelope(p0, p1)
    if (!rectEnv.intersects(segEnv))
      return false

    /*
     * If either segment endpoint lies in the rectangle,
     * there is an intersection.
     */
    if (rectEnv.intersects(p0)) return true
    if (rectEnv.intersects(p1)) return true

    /*
     * Normalize segment.
     * This makes p0 less than p1,
     * so that the segment runs to the right,
     * or vertically upwards.
     */
    if (p0.compareTo(p1) > 0) {
      val tmp = p0
      p0 = p1
      p1 = tmp
    }
    /**
     * Compute angle of segment.
     * Since the segment is normalized to run left to right,
     * it is sufficient to simply test the Y ordinate.
     * "Upwards" means relative to the left end of the segment.
     */
    var isSegUpwards = false
    if (p1.y > p0.y)
      isSegUpwards = true

    /*
     * To distinguish disjoint vs crossing cases, it is sufficient
     * to test intersection with a single diagonal of the rectangle,
     * namely the one with slope "opposite" to the slope of the segment.
     */
    if (isSegUpwards) {
      li.computeIntersection(p0, p1, diagDown0, diagDown1)
    } else {
      li.computeIntersection(p0, p1, diagUp0, diagUp1)
    }
    if (li.hasIntersection())
      return true
    return false
  }
}
