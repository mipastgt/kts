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

/**
 * Nodes a set of [SegmentString]s by
 * performing a brute-force comparison of every segment to every other one.
 * This has n^2 performance, so is too slow for use on large numbers
 * of segments.
 *
 * @version 1.7
 */
class SimpleNoder : SinglePassNoder() {

  private var nodedSegStrings: Collection<*>? = null

  override fun getNodedSubstrings(): MutableCollection<*> {
    return NodedSegmentString.getNodedSubstrings(nodedSegStrings)
  }

  override fun computeNodes(inputSegStrings: Collection<*>?) {
    this.nodedSegStrings = inputSegStrings
    for (edge0 in inputSegStrings!!) {
      val e0 = edge0 as SegmentString
      for (edge1 in inputSegStrings) {
        val e1 = edge1 as SegmentString
        computeIntersects(e0, e1)
      }
    }
  }

  private fun computeIntersects(e0: SegmentString, e1: SegmentString) {
    val pts0: Array<Coordinate> = e0.getCoordinates()
    val pts1: Array<Coordinate> = e1.getCoordinates()
    for (i0 in 0 until pts0.size - 1) {
      for (i1 in 0 until pts1.size - 1) {
        segInt!!.processIntersections(e0, i0, e1, i1)
      }
    }
  }
}
