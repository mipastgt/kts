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

import kotlin.jvm.JvmStatic

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Quadrant
import org.locationtech.jts.util.IntArrayList

/**
 * MonotoneChains are a way of partitioning the segments of an edge to
 * allow for fast searching of intersections.
 * Specifically, a sequence of contiguous line segments
 * is a monotone chain if all the vectors defined by the oriented segments
 * lies in the same quadrant.
 *
 *
 * Monotone Chains have the following useful properties:
 *
 *  1. the segments within a monotone chain will never intersect each other
 *  1. the envelope of any contiguous subset of the segments in a monotone chain
 * is simply the envelope of the endpoints of the subset.
 *
 * Property 1 means that there is no need to test pairs of segments from within
 * the same monotone chain for intersection.
 * Property 2 allows
 * binary search to be used to find the intersection points of two monotone chains.
 * For many types of real-world data, these properties eliminate a large number of
 * segment comparisons, producing substantial speed gains.
 *
 *
 * Note that due to the efficient intersection test, there is no need to limit the size
 * of chains to obtain fast performance.
 *
 */
class MonotoneChainIndexer {

  fun getChainStartIndices(pts: Array<Coordinate>): IntArray {
    // find the startpoint (and endpoints) of all monotone chains in this edge
    var start = 0
    val startIndexList = IntArrayList(pts.size / 2)
    // use heuristic to size initial array
    //startIndexList.ensureCapacity(pts.length / 4);
    startIndexList.add(start)
    do {
      val last = findChainEnd(pts, start)
      startIndexList.add(last)
      start = last
    } while (start < pts.size - 1)
    // copy list to an array of ints, for efficiency
    return startIndexList.toArray()
  }

  fun OLDgetChainStartIndices(pts: Array<Coordinate>): IntArray {
    // find the startpoint (and endpoints) of all monotone chains in this edge
    var start = 0
    val startIndexList: MutableList<Any?> = ArrayList()
    startIndexList.add(start)
    do {
      val last = findChainEnd(pts, start)
      startIndexList.add(last)
      start = last
    } while (start < pts.size - 1)
    // copy list to an array of ints, for efficiency
    val startIndex = toIntArray(startIndexList)
    return startIndex
  }

  /**
   * @return the index of the last point in the monotone chain
   */
  private fun findChainEnd(pts: Array<Coordinate>, start: Int): Int {
    // determine quadrant for chain
    val chainQuad = Quadrant.quadrant(pts[start], pts[start + 1])
    var last = start + 1
    while (last < pts.size) {
      //if (last - start > 100) break;
      // compute quadrant for next possible segment in chain
      val quad = Quadrant.quadrant(pts[last - 1], pts[last])
      if (quad != chainQuad) break
      last++
    }
    return last - 1
  }

  companion object {
    @JvmStatic
    fun toIntArray(list: List<*>): IntArray {
      val array = IntArray(list.size)
      for (i in array.indices) {
        array[i] = list[i] as Int
      }
      return array
    }
  }
}
