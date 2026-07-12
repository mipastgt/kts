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
package org.locationtech.jts.geomgraph

import kotlin.jvm.JvmField

import org.locationtech.jts.geom.Coordinate

/**
 * Represents a point on an
 * edge which intersects with another edge.
 *
 *
 * The intersection may either be a single point, or a line segment
 * (in which case this point is the start of the line segment)
 * The intersection point must be precise.
 *
 * @version 1.7
 */
class EdgeIntersection
/**
 * EdgeIntersection.
 *
 * @param coord Point of intersection
 * @param segmentIndex Index of the containing line segment in the parent edge
 * @param dist Edge distance of this point along the containing line segment
 */
  (coord: Coordinate, segmentIndex: Int, dist: Double) : Comparable<Any?> {

  /** Point of intersection  */
  @JvmField
  var coord: Coordinate = Coordinate(coord)

  /** Index of the containing line segment in the parent edge  */
  @JvmField
  var segmentIndex: Int = segmentIndex

  /** Edge distance of this point along the containing line segment  */
  @JvmField
  var dist: Double = dist

  fun getCoordinate(): Coordinate = coord

  fun getSegmentIndex(): Int = segmentIndex

  fun getDistance(): Double = dist

  override fun compareTo(obj: Any?): Int {
    val other = obj as EdgeIntersection
    return compare(other.segmentIndex, other.dist)
  }

  /**
   * Comparison with segment and distance.
   *
   * @param segmentIndex index of the containing line segment
   * @param dist dge distance of this point along the containing line segment
   * @return `1` this EdgeIntersection is located before the argument location,
   * `0` this EdgeIntersection is at the argument location,
   * `1` this EdgeIntersection is located after the argument location
   */
  fun compare(segmentIndex: Int, dist: Double): Int {
    if (this.segmentIndex < segmentIndex) return -1
    if (this.segmentIndex > segmentIndex) return 1
    if (this.dist < dist) return -1
    if (this.dist > dist) return 1
    return 0
  }

  fun isEndPoint(maxSegmentIndex: Int): Boolean {
    if (segmentIndex == 0 && dist == 0.0) return true
    if (segmentIndex == maxSegmentIndex) return true
    return false
  }

  override fun toString(): String {
    return "$coord seg # = $segmentIndex dist = $dist"
  }
}
