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
package org.locationtech.jts.index.chain

import kotlin.jvm.JvmStatic

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Quadrant

/**
 * Constructs [MonotoneChain]s
 * for sequences of [Coordinate]s.
 *
 */
class MonotoneChainBuilder {

  companion object {
    /**
     * Computes a list of the [MonotoneChain]s
     * for a list of coordinates.
     *
     * @param pts the list of points to compute chains for
     * @return a list of the monotone chains for the points
     */
    @JvmStatic
    fun getChains(pts: Array<Coordinate>): MutableList<MonotoneChain> {
      return getChains(pts, null)
    }

    /**
     * Computes a list of the [MonotoneChain]s
     * for a list of coordinates,
     * attaching a context data object to each.
     *
     * @param pts the list of points to compute chains for
     * @param context a data object to attach to each chain
     * @return a list of the monotone chains for the points
     */
    @JvmStatic
    fun getChains(pts: Array<Coordinate>, context: Any?): MutableList<MonotoneChain> {
      val mcList = ArrayList<MonotoneChain>()
      if (pts.size == 0)
        return mcList
      var chainStart = 0
      do {
        val chainEnd = findChainEnd(pts, chainStart)
        val mc = MonotoneChain(pts, chainStart, chainEnd, context)
        mcList.add(mc)
        chainStart = chainEnd
      } while (chainStart < pts.size - 1)
      return mcList
    }

    /**
     * Finds the index of the last point in a monotone chain
     * starting at a given point.
     * Repeated points (0-length segments) are included
     * in the monotone chain returned.
     *
     * @param pts the points to scan
     * @param start the index of the start of this chain
     * @return the index of the last point in the monotone chain
     * starting at `start`.
     */
    private fun findChainEnd(pts: Array<Coordinate>, start: Int): Int {
      var safeStart = start
      // skip any zero-length segments at the start of the sequence
      // (since they cannot be used to establish a quadrant)
      while (safeStart < pts.size - 1 && pts[safeStart].equals2D(pts[safeStart + 1])) {
        safeStart++
      }
      // check if there are NO non-zero-length segments
      if (safeStart >= pts.size - 1) {
        return pts.size - 1
      }
      // determine overall quadrant for chain (which is the starting quadrant)
      val chainQuad = Quadrant.quadrant(pts[safeStart], pts[safeStart + 1])
      var last = start + 1
      while (last < pts.size) {
        // skip zero-length segments, but include them in the chain
        if (!pts[last - 1].equals2D(pts[last])) {
          // compute quadrant for next possible segment in chain
          val quad = Quadrant.quadrant(pts[last - 1], pts[last])
          if (quad != chainQuad) break
        }
        last++
      }
      return last - 1
    }
  }
}
