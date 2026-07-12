/*
 * Copyright (c) 2021 Martin Davis.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.locationtech.jts.index

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.math.MathUtil
import org.locationtech.jts.util.IntArrayList

/**
 * A semi-static spatial index for points which occur
 * in a spatially-coherent sequence.
 * In particular, this is suitable for indexing the vertices
 * of a LineString or Polygon ring.
 *
 * The index is constructed in a batch fashion on a given sequence of coordinates.
 * Coordinates can be removed via the [remove] method.
 *
 * The input coordinate array is read-only,
 * and is not changed when vertices are removed.
 *
 * @author Martin Davis
 */
class VertexSequencePackedRtree(pts: Array<Coordinate>) {

  private val items: Array<Coordinate> = pts
  private var levelOffset: IntArray = IntArray(0)
  private val nodeCapacity = NODE_CAPACITY
  private var bounds: Array<Envelope?> = arrayOfNulls(0)
  private val isRemoved: BooleanArray = BooleanArray(pts.size)

  init {
    build()
  }

  fun getBounds(): Array<Envelope?> {
    return bounds.copyOf()
  }

  private fun build() {
    levelOffset = computeLevelOffsets()
    bounds = createBounds()
  }

  /**
   * Computes the level offsets.
   * This is the position in the `bounds` array of each level.
   *
   * @return the level offsets
   */
  private fun computeLevelOffsets(): IntArray {
    val offsets = IntArrayList()
    offsets.add(0)
    var levelSize = items.size
    var currOffset = 0
    do {
      levelSize = levelNodeCount(levelSize)
      currOffset += levelSize
      offsets.add(currOffset)
    } while (levelSize > 1)
    return offsets.toArray()
  }

  private fun levelNodeCount(numNodes: Int): Int {
    return MathUtil.ceil(numNodes, nodeCapacity)
  }

  private fun createBounds(): Array<Envelope?> {
    val boundsSize = levelOffset[levelOffset.size - 1] + 1
    val bounds = arrayOfNulls<Envelope>(boundsSize)
    fillItemBounds(bounds)

    for (lvl in 1 until levelOffset.size) {
      fillLevelBounds(lvl, bounds)
    }
    return bounds
  }

  private fun fillLevelBounds(lvl: Int, bounds: Array<Envelope?>) {
    val levelStart = levelOffset[lvl - 1]
    val levelEnd = levelOffset[lvl]
    var nodeStart = levelStart
    var levelBoundIndex = levelOffset[lvl]
    do {
      val nodeEnd = MathUtil.clampMax(nodeStart + nodeCapacity, levelEnd)
      bounds[levelBoundIndex++] = computeNodeEnvelope(bounds, nodeStart, nodeEnd)
      nodeStart = nodeEnd
    } while (nodeStart < levelEnd)
  }

  private fun fillItemBounds(bounds: Array<Envelope?>) {
    var nodeStart = 0
    var boundIndex = 0
    do {
      val nodeEnd = MathUtil.clampMax(nodeStart + nodeCapacity, items.size)
      bounds[boundIndex++] = computeItemEnvelope(items, nodeStart, nodeEnd)
      nodeStart = nodeEnd
    } while (nodeStart < items.size)
  }

  //------------------------

  /**
   * Queries the index to find all items which intersect an extent.
   * The query result is a list of the indices of input coordinates
   * which intersect the extent.
   *
   * @param queryEnv the query extent
   * @return an array of the indices of the input coordinates
   */
  fun query(queryEnv: Envelope): IntArray {
    val resultList = IntArrayList()
    val level = levelOffset.size - 1
    queryNode(queryEnv, level, 0, resultList)
    val result = resultList.toArray()
    return result
  }

  private fun queryNode(queryEnv: Envelope, level: Int, nodeIndex: Int, resultList: IntArrayList) {
    val boundsIndex = levelOffset[level] + nodeIndex
    val nodeEnv = bounds[boundsIndex]
    //--- node is empty
    if (nodeEnv == null)
      return
    if (!queryEnv.intersects(nodeEnv))
      return

    val childNodeIndex = nodeIndex * nodeCapacity
    if (level == 0) {
      queryItemRange(queryEnv, childNodeIndex, resultList)
    } else {
      queryNodeRange(queryEnv, level - 1, childNodeIndex, resultList)
    }
  }

  private fun queryNodeRange(queryEnv: Envelope, level: Int, nodeStartIndex: Int, resultList: IntArrayList) {
    val levelMax = levelSize(level)
    for (i in 0 until nodeCapacity) {
      val index = nodeStartIndex + i
      if (index >= levelMax)
        return
      queryNode(queryEnv, level, index, resultList)
    }
  }

  private fun levelSize(level: Int): Int {
    return levelOffset[level + 1] - levelOffset[level]
  }

  private fun queryItemRange(queryEnv: Envelope, itemIndex: Int, resultList: IntArrayList) {
    for (i in 0 until nodeCapacity) {
      val index = itemIndex + i
      if (index >= items.size)
        return
      val p = items[index]
      if (!isRemoved[index] &&
          queryEnv.contains(p))
        resultList.add(index)
    }
  }

  //------------------------

  /**
   * Removes the input item at the given index from the spatial index.
   * This does not change the underlying coordinate array.
   *
   * @param index the index of the item in the input
   */
  fun remove(index: Int) {
    isRemoved[index] = true

    //--- prune the item parent node if all its items are removed
    val nodeIndex = index / nodeCapacity
    if (!isItemsNodeEmpty(nodeIndex))
      return

    bounds[nodeIndex] = null

    if (levelOffset.size <= 2)
      return

    //-- prune the node parent if all children removed
    val nodeLevelIndex = nodeIndex / nodeCapacity
    if (!isNodeEmpty(1, nodeLevelIndex))
      return
    val nodeIndex1 = levelOffset[1] + nodeLevelIndex
    bounds[nodeIndex1] = null

    //TODO: propagate removal up the tree nodes?
  }

  private fun isNodeEmpty(level: Int, index: Int): Boolean {
    val start = index * nodeCapacity
    val end = MathUtil.clampMax(start + nodeCapacity, levelOffset[level])
    for (i in start until end) {
      if (bounds[i] != null) return false
    }
    return true
  }

  private fun isItemsNodeEmpty(nodeIndex: Int): Boolean {
    val start = nodeIndex * nodeCapacity
    val end = MathUtil.clampMax(start + nodeCapacity, items.size)
    for (i in start until end) {
      if (!isRemoved[i]) return false
    }
    return true
  }

  companion object {
    /**
     * Number of items/nodes in a parent node.
     * Determined empirically.  Performance is not too sensitive to this.
     */
    private const val NODE_CAPACITY = 16

    private fun computeNodeEnvelope(bounds: Array<Envelope?>, start: Int, end: Int): Envelope {
      val env = Envelope()
      for (i in start until end) {
        env.expandToInclude(bounds[i]!!)
      }
      return env
    }

    private fun computeItemEnvelope(items: Array<Coordinate>, start: Int, end: Int): Envelope {
      val env = Envelope()
      for (i in start until end) {
        env.expandToInclude(items[i])
      }
      return env
    }
  }
}
