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
package org.locationtech.jts.operation.buffer.validate

import org.locationtech.jts.geom.Coordinate

/**
 * Contains a pair of points and the distance between them.
 * Provides methods to update with a new point pair with
 * either maximum or minimum distance.
 */
class PointPairDistance {

  private val pt = arrayOf(Coordinate(), Coordinate())
  private var distance = Double.NaN
  private var isNull = true

  fun initialize() {
    isNull = true
  }

  fun initialize(p0: Coordinate, p1: Coordinate) {
    pt[0].setCoordinate(p0)
    pt[1].setCoordinate(p1)
    distance = p0.distance(p1)
    isNull = false
  }

  /**
   * Initializes the points, avoiding recomputing the distance.
   * @param p0
   * @param p1
   * @param distance the distance between p0 and p1
   */
  private fun initialize(p0: Coordinate, p1: Coordinate, distance: Double) {
    pt[0].setCoordinate(p0)
    pt[1].setCoordinate(p1)
    this.distance = distance
    isNull = false
  }

  fun getDistance(): Double {
    return distance
  }

  fun getCoordinates(): Array<Coordinate> {
    return pt
  }

  fun getCoordinate(i: Int): Coordinate {
    return pt[i]
  }

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
}
