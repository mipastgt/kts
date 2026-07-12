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

package org.locationtech.jts.algorithm.distance

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.io.WKTWriter

/**
 * Contains a pair of points and the distance between them.
 * Provides methods to update with a new point pair with
 * either maximum or minimum distance.
 */
class PointPairDistance {

  private val pt = arrayOf(Coordinate(), Coordinate())
  private var distance = Double.NaN
  private var isNull = true

  /**
   * Initializes this instance.
   */
  fun initialize() {
    isNull = true
  }

  /**
   * Initializes the points, computing the distance between them.
   * @param p0 the 1st point
   * @param p1 the 2nd point
   */
  fun initialize(p0: Coordinate, p1: Coordinate) {
    initialize(p0, p1, p0.distance(p1))
  }

  /**
   * Initializes the points, avoiding recomputing the distance.
   * @param p0 the 1st point
   * @param p1 the 2nd point
   * @param distance the distance between p0 and p1
   */
  internal fun initialize(p0: Coordinate, p1: Coordinate, distance: Double) {
    pt[0].setCoordinate(p0)
    pt[1].setCoordinate(p1)
    this.distance = distance
    isNull = false
  }

  /**
   * Gets the distance between the paired points
   * @return the distance between the paired points
   */
  fun getDistance(): Double = distance

  /**
   * Gets the paired points
   * @return the paired points
   */
  fun getCoordinates(): Array<Coordinate> = pt

  /**
   * Gets one of the paired points
   * @param i the index of the paired point (0 or 1)
   * @return A point
   */
  fun getCoordinate(i: Int): Coordinate = pt[i]

  fun setMaximum(ptDist: PointPairDistance) {
    setMaximum(ptDist.pt[0], ptDist.pt[1])
  }

  fun setMaximum(p0: Coordinate, p1: Coordinate) {
    if (isNull) {
      initialize(p0, p1)
      return
    }
    val dist = p0.distance(p1)
    if (dist > distance) initialize(p0, p1, dist)
  }

  fun setMinimum(ptDist: PointPairDistance) {
    setMinimum(ptDist.pt[0], ptDist.pt[1])
  }

  fun setMinimum(p0: Coordinate, p1: Coordinate) {
    if (isNull) {
      initialize(p0, p1)
      return
    }
    val dist = p0.distance(p1)
    if (dist < distance) initialize(p0, p1, dist)
  }

  override fun toString(): String {
    return WKTWriter.toLineString(pt[0], pt[1])
  }
}
