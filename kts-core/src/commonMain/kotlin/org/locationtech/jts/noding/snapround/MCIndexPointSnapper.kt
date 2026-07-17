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
package org.locationtech.jts.noding.snapround

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.index.ItemVisitor
import org.locationtech.jts.index.SpatialIndex
import org.locationtech.jts.index.chain.MonotoneChain
import org.locationtech.jts.index.chain.MonotoneChainSelectAction
import org.locationtech.jts.noding.NodedSegmentString
import org.locationtech.jts.noding.SegmentString

/**
 * "Snaps" all [SegmentString]s in a [SpatialIndex] containing
 * [MonotoneChain]s to a given [HotPixel].
 *
 */
class MCIndexPointSnapper(private val index: SpatialIndex) {

  /**
   * Snaps (nodes) all interacting segments to this hot pixel.
   * The hot pixel may represent a vertex of an edge,
   * in which case this routine uses the optimization
   * of not noding the vertex itself
   *
   * @param hotPixel the hot pixel to snap to
   * @param parentEdge the edge containing the vertex, if applicable, or `null`
   * @param hotPixelVertexIndex the index of the hotPixel vertex, if applicable, or -1
   * @return `true` if a node was added for this pixel
   */
  fun snap(hotPixel: HotPixel, parentEdge: SegmentString?, hotPixelVertexIndex: Int): Boolean {
    val pixelEnv = getSafeEnvelope(hotPixel)
    val hotPixelSnapAction = HotPixelSnapAction(hotPixel, parentEdge, hotPixelVertexIndex)

    index.query(
      pixelEnv,
      object : ItemVisitor {
        override fun visitItem(item: Any?) {
          val testChain = item as MonotoneChain
          testChain.select(pixelEnv, hotPixelSnapAction)
        }
      }
    )
    return hotPixelSnapAction.isNodeAdded()
  }

  fun snap(hotPixel: HotPixel): Boolean {
    return snap(hotPixel, null, -1)
  }

  /**
   * Returns a "safe" envelope that is guaranteed to contain the hot pixel.
   *
   * @return an envelope which contains the hot pixel
   */
  fun getSafeEnvelope(hp: HotPixel): Envelope {
    val safeTolerance = SAFE_ENV_EXPANSION_FACTOR / hp.getScaleFactor()
    val safeEnv = Envelope(hp.getCoordinate())
    safeEnv.expandBy(safeTolerance)
    return safeEnv
  }

  class HotPixelSnapAction(
    private val hotPixel: HotPixel,
    private val parentEdge: SegmentString?,
    // is -1 if hotPixel is not a vertex
    private val hotPixelVertexIndex: Int
  ) : MonotoneChainSelectAction() {
    private var nodeAdded = false

    /**
     * Reports whether the HotPixel caused a
     * node to be added in any target segmentString (including its own).
     * @return true if a node was added in any target segmentString.
     */
    fun isNodeAdded(): Boolean {
      return nodeAdded
    }

    /**
     * Check if a segment of the monotone chain intersects
     * the hot pixel vertex and introduce a snap node if so.
     */
    override fun select(mc: MonotoneChain, startIndex: Int) {
      val ss = mc.getContext() as NodedSegmentString
      /*
       * Check to avoid snapping a hotPixel vertex to the its orginal vertex.
       */
      if (parentEdge != null && ss === parentEdge) {
        if (startIndex == hotPixelVertexIndex || startIndex + 1 == hotPixelVertexIndex) return
      }
      // records if this HotPixel caused any node to be added
      nodeAdded = nodeAdded or addSnappedNode(hotPixel, ss, startIndex)
    }

    /**
     * Adds a new node (equal to the snap pt) to the specified segment
     * if the segment passes through the hot pixel
     *
     * @return true if a node was added to the segment
     */
    fun addSnappedNode(hotPixel: HotPixel, segStr: NodedSegmentString, segIndex: Int): Boolean {
      val p0 = segStr.getCoordinate(segIndex)
      val p1 = segStr.getCoordinate(segIndex + 1)

      if (hotPixel.intersects(p0, p1)) {
        segStr.addIntersection(hotPixel.getCoordinate(), segIndex)
        return true
      }
      return false
    }
  }

  companion object {
    private const val SAFE_ENV_EXPANSION_FACTOR = 0.75
  }
}
