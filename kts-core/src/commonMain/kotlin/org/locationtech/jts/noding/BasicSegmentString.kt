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

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.impl.CoordinateArraySequence
import org.locationtech.jts.io.WKTWriter

/**
 * Represents a read-only list of contiguous line segments.
 * This can be used for detection of intersections or nodes.
 * [SegmentString]s can carry a context object, which is useful
 * for preserving topological or parentage information.
 *
 *
 * If adding nodes is required use [NodedSegmentString].
 *
 * @version 1.7
 * @see NodedSegmentString
 */
open class BasicSegmentString
/**
 * Creates a new segment string from a list of vertices.
 *
 * @param pts the vertices of the segment string
 * @param data the user-defined data of this segment string (may be null)
 */
  (
  private val pts: Array<Coordinate>,
  private var data: Any?
) : SegmentString {

  /**
   * Gets the user-defined data for this segment string.
   *
   * @return the user-defined data
   */
  override fun getData(): Any? = data

  /**
   * Sets the user-defined data for this segment string.
   *
   * @param data an Object containing user-defined data
   */
  override fun setData(data: Any?) {
    this.data = data
  }

  override fun size(): Int = pts.size
  override fun getCoordinate(i: Int): Coordinate = pts[i]
  override fun getCoordinates(): Array<Coordinate> = pts

  override fun isClosed(): Boolean {
    return pts[0] == pts[pts.size - 1]
  }

  /**
   * Gets the octant of the segment starting at vertex `index`.
   *
   * @param index the index of the vertex starting the segment.  Must not be
   * the last index in the vertex list
   * @return the octant of the segment at the vertex
   */
  open fun getSegmentOctant(index: Int): Int {
    if (index == pts.size - 1) return -1
    return Octant.octant(getCoordinate(index), getCoordinate(index + 1))
  }

  override fun toString(): String {
    return WKTWriter.toLineString(CoordinateArraySequence(pts))
  }
}
