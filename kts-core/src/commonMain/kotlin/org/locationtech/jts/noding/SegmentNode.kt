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
package org.locationtech.jts.noding

import kotlin.jvm.JvmField

import org.locationtech.jts.geom.Coordinate

/**
 * Represents an intersection point between two [SegmentString]s.
 *
 * @version 1.7
 */
class SegmentNode(
  private val segString: NodedSegmentString,
  coord: Coordinate,
  @JvmField val segmentIndex: Int,   // the index of the containing line segment in the parent edge
  private val segmentOctant: Int
) : Comparable<Any?> {
  @JvmField val coord: Coordinate = coord.copy()   // the point of intersection
  private val interior: Boolean = !coord.equals2D(segString.getCoordinate(segmentIndex))

  /**
   * Gets the [Coordinate] giving the location of this node.
   *
   * @return the coordinate of the node
   */
  fun getCoordinate(): Coordinate {
    return coord
  }

  fun isInterior(): Boolean {
    return interior
  }

  fun isEndPoint(maxSegmentIndex: Int): Boolean {
    if (segmentIndex == 0 && !interior) return true
    if (segmentIndex == maxSegmentIndex) return true
    return false
  }

  /**
   * @return -1 this SegmentNode is located before the argument location;
   * 0 this SegmentNode is at the argument location;
   * 1 this SegmentNode is located after the argument location
   */
  override fun compareTo(obj: Any?): Int {
    val other = obj as SegmentNode

    if (segmentIndex < other.segmentIndex) return -1
    if (segmentIndex > other.segmentIndex) return 1

    if (coord.equals2D(other.coord)) return 0

    // an exterior node is the segment start point, so always sorts first
    // this guards against a robustness problem where the octants are not reliable
    if (!interior) return -1
    if (!other.interior) return 1

    return SegmentPointComparator.compare(segmentOctant, coord, other.coord)
    //return segment.compareNodePosition(this, other);
  }

  override fun toString(): String {
    return segmentIndex.toString() + ":" + coord.toString()
  }
}
