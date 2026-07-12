/*
 * Copyright (c) 2019 Martin Davis.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.locationtech.jts.index.hprtree

import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.index.ArrayListVisitor
import org.locationtech.jts.index.ItemVisitor
import org.locationtech.jts.index.SpatialIndex
import org.locationtech.jts.util.IntArrayList

/**
 * A Hilbert-Packed R-tree.  This is a static R-tree
 * which is packed by using the Hilbert ordering
 * of the tree items.
 *
 * @see org.locationtech.jts.index.strtree.STRtree
 *
 * @author Martin Davis
 */
class HPRtree(private val nodeCapacity: Int) : SpatialIndex {

  private var itemsToLoad: MutableList<Item>? = ArrayList()

  private var numItems = 0

  private val totalExtent = Envelope()

  private var layerStartIndex: IntArray? = null

  private var nodeBounds: DoubleArray? = null

  private var itemBounds: DoubleArray? = null

  private var itemValues: Array<Any?>? = null

  private var isBuilt = false

  /**
   * Creates a new index with the default node capacity.
   */
  constructor() : this(DEFAULT_NODE_CAPACITY)

  /**
   * Gets the number of items in the index.
   *
   * @return the number of items
   */
  fun size(): Int {
    return numItems
  }

  override fun insert(itemEnv: Envelope, item: Any?) {
    if (isBuilt) {
      throw IllegalStateException("Cannot insert items after tree is built.")
    }
    numItems++
    itemsToLoad!!.add(Item(itemEnv, item))
    totalExtent.expandToInclude(itemEnv)
  }

  override fun query(searchEnv: Envelope?): MutableList<*> {
    build()

    val env = searchEnv!!
    if (!totalExtent.intersects(env)) return ArrayList<Any?>()

    val visitor = ArrayListVisitor()
    query(env, visitor)
    return visitor.getItems()
  }

  override fun query(searchEnv: Envelope?, visitor: ItemVisitor) {
    build()
    val env = searchEnv!!
    if (!totalExtent.intersects(env)) return
    if (layerStartIndex == null) {
      queryItems(0, env, visitor)
    } else {
      queryTopLayer(env, visitor)
    }
  }

  private fun queryTopLayer(searchEnv: Envelope, visitor: ItemVisitor) {
    val layerIndex = layerStartIndex!!.size - 2
    val layerSize = layerSize(layerIndex)
    // query each node in layer
    var i = 0
    while (i < layerSize) {
      queryNode(layerIndex, i, searchEnv, visitor)
      i += ENV_SIZE
    }
  }

  private fun queryNode(layerIndex: Int, nodeOffset: Int, searchEnv: Envelope, visitor: ItemVisitor) {
    val layerStart = layerStartIndex!![layerIndex]
    val nodeIndex = layerStart + nodeOffset
    if (!intersects(nodeBounds!!, nodeIndex, searchEnv)) return
    if (layerIndex == 0) {
      val childNodesOffset = nodeOffset / ENV_SIZE * nodeCapacity
      queryItems(childNodesOffset, searchEnv, visitor)
    } else {
      val childNodesOffset = nodeOffset * nodeCapacity
      queryNodeChildren(layerIndex - 1, childNodesOffset, searchEnv, visitor)
    }
  }

  private fun queryNodeChildren(layerIndex: Int, blockOffset: Int, searchEnv: Envelope, visitor: ItemVisitor) {
    val layerStart = layerStartIndex!![layerIndex]
    val layerEnd = layerStartIndex!![layerIndex + 1]
    for (i in 0 until nodeCapacity) {
      val nodeOffset = blockOffset + ENV_SIZE * i
      // don't query past layer end
      if (layerStart + nodeOffset >= layerEnd) break

      queryNode(layerIndex, nodeOffset, searchEnv, visitor)
    }
  }

  private fun queryItems(blockStart: Int, searchEnv: Envelope, visitor: ItemVisitor) {
    for (i in 0 until nodeCapacity) {
      val itemIndex = blockStart + i
      // don't query past end of items
      if (itemIndex >= numItems) break
      if (intersects(itemBounds!!, itemIndex * ENV_SIZE, searchEnv)) {
        visitor.visitItem(itemValues!![itemIndex])
      }
    }
  }

  private fun layerSize(layerIndex: Int): Int {
    val layerStart = layerStartIndex!![layerIndex]
    val layerEnd = layerStartIndex!![layerIndex + 1]
    return layerEnd - layerStart
  }

  override fun remove(itemEnv: Envelope, item: Any?): Boolean {
    // TODO Auto-generated method stub
    return false
  }

  /**
   * Builds the index, if not already built.
   */
  fun build() {
    // skip if already built
    if (!isBuilt) {
      prepareIndex()
      prepareItems()
      this.isBuilt = true
    }
  }

  private fun prepareIndex() {
    // don't need to build an empty or very small tree
    if (itemsToLoad!!.size <= nodeCapacity) return

    sortItems()

    layerStartIndex = computeLayerIndices(numItems, nodeCapacity)
    // allocate storage
    val nodeCount = layerStartIndex!![layerStartIndex!!.size - 1] / 4
    nodeBounds = createBoundsArray(nodeCount)

    // compute tree nodes
    computeLeafNodes(layerStartIndex!![1])
    for (i in 1 until layerStartIndex!!.size - 1) {
      computeLayerNodes(i)
    }
  }

  private fun prepareItems() {
    // copy item contents out to arrays for querying
    var boundsIndex = 0
    var valueIndex = 0
    val itemBounds = DoubleArray(itemsToLoad!!.size * 4)
    val itemValues = arrayOfNulls<Any?>(itemsToLoad!!.size)
    for (item in itemsToLoad!!) {
      val envelope = item.getEnvelope()
      itemBounds[boundsIndex++] = envelope.getMinX()
      itemBounds[boundsIndex++] = envelope.getMinY()
      itemBounds[boundsIndex++] = envelope.getMaxX()
      itemBounds[boundsIndex++] = envelope.getMaxY()
      itemValues[valueIndex++] = item.getItem()
    }
    this.itemBounds = itemBounds
    this.itemValues = itemValues
    // and let GC free the original list
    itemsToLoad = null
  }

  private fun computeLayerNodes(layerIndex: Int) {
    val layerStart = layerStartIndex!![layerIndex]
    val childLayerStart = layerStartIndex!![layerIndex - 1]
    val layerSize = layerSize(layerIndex)
    val childLayerEnd = layerStart
    var i = 0
    while (i < layerSize) {
      val childStart = childLayerStart + nodeCapacity * i
      computeNodeBounds(layerStart + i, childStart, childLayerEnd)
      i += ENV_SIZE
    }
  }

  private fun computeNodeBounds(nodeIndex: Int, blockStart: Int, nodeMaxIndex: Int) {
    val nodeBounds = this.nodeBounds!!
    for (i in 0..nodeCapacity) {
      val index = blockStart + 4 * i
      if (index >= nodeMaxIndex) break
      updateNodeBounds(nodeIndex, nodeBounds[index], nodeBounds[index + 1], nodeBounds[index + 2], nodeBounds[index + 3])
    }
  }

  private fun computeLeafNodes(layerSize: Int) {
    var i = 0
    while (i < layerSize) {
      computeLeafNodeBounds(i, nodeCapacity * i / 4)
      i += ENV_SIZE
    }
  }

  private fun computeLeafNodeBounds(nodeIndex: Int, blockStart: Int) {
    for (i in 0..nodeCapacity) {
      val itemIndex = blockStart + i
      if (itemIndex >= itemsToLoad!!.size) break
      val env = itemsToLoad!![itemIndex].getEnvelope()
      updateNodeBounds(nodeIndex, env.getMinX(), env.getMinY(), env.getMaxX(), env.getMaxY())
    }
  }

  private fun updateNodeBounds(nodeIndex: Int, minX: Double, minY: Double, maxX: Double, maxY: Double) {
    val nodeBounds = this.nodeBounds!!
    if (minX < nodeBounds[nodeIndex]) nodeBounds[nodeIndex] = minX
    if (minY < nodeBounds[nodeIndex + 1]) nodeBounds[nodeIndex + 1] = minY
    if (maxX > nodeBounds[nodeIndex + 2]) nodeBounds[nodeIndex + 2] = maxX
    if (maxY > nodeBounds[nodeIndex + 3]) nodeBounds[nodeIndex + 3] = maxY
  }

  /**
   * Gets the extents of the internal index nodes
   *
   * @return a list of the internal node extents
   */
  fun getBounds(): Array<Envelope> {
    val nodeBounds = this.nodeBounds!!
    val numNodes = nodeBounds.size / 4
    val bounds = arrayOfNulls<Envelope>(numNodes)
    // create from largest to smallest
    for (i in numNodes - 1 downTo 0) {
      val boundIndex = 4 * i
      bounds[i] = Envelope(
        nodeBounds[boundIndex], nodeBounds[boundIndex + 2],
        nodeBounds[boundIndex + 1], nodeBounds[boundIndex + 3]
      )
    }
    @Suppress("UNCHECKED_CAST")
    return bounds as Array<Envelope>
  }

  private fun sortItems() {
    val encoder = HilbertEncoder(HILBERT_LEVEL, totalExtent)
    val hilbertValues = IntArray(itemsToLoad!!.size)
    var pos = 0
    for (item in itemsToLoad!!) {
      hilbertValues[pos++] = encoder.encode(item.getEnvelope())
    }
    quickSortItemsIntoNodes(hilbertValues, 0, itemsToLoad!!.size - 1)
  }

  private fun quickSortItemsIntoNodes(values: IntArray, lo: Int, hi: Int) {
    // stop sorting when left/right pointers are within the same node
    // because queryItems just searches through them all sequentially
    if (lo / nodeCapacity < hi / nodeCapacity) {
      val pivot = hoarePartition(values, lo, hi)
      quickSortItemsIntoNodes(values, lo, pivot)
      quickSortItemsIntoNodes(values, pivot + 1, hi)
    }
  }

  private fun hoarePartition(values: IntArray, lo: Int, hi: Int): Int {
    val pivot = values[(lo + hi) shr 1]
    var i = lo - 1
    var j = hi + 1

    while (true) {
      do { i++ } while (values[i] < pivot)
      do { j-- } while (values[j] > pivot)
      if (i >= j) return j
      swapItems(values, i, j)
    }
  }

  private fun swapItems(values: IntArray, i: Int, j: Int) {
    val itemsToLoad = this.itemsToLoad!!
    val tmpItemp = itemsToLoad[i]
    itemsToLoad[i] = itemsToLoad[j]
    itemsToLoad[j] = tmpItemp

    val tmpValue = values[i]
    values[i] = values[j]
    values[j] = tmpValue
  }

  companion object {
    private const val ENV_SIZE = 4

    private const val HILBERT_LEVEL = 12

    private const val DEFAULT_NODE_CAPACITY = 16

    private fun intersects(bounds: DoubleArray, nodeIndex: Int, env: Envelope): Boolean {
      val isBeyond = (env.getMaxX() < bounds[nodeIndex]) ||
        (env.getMaxY() < bounds[nodeIndex + 1]) ||
        (env.getMinX() > bounds[nodeIndex + 2]) ||
        (env.getMinY() > bounds[nodeIndex + 3])
      return !isBeyond
    }

    private fun createBoundsArray(size: Int): DoubleArray {
      val a = DoubleArray(4 * size)
      for (i in 0 until size) {
        val index = 4 * i
        a[index] = Double.MAX_VALUE
        a[index + 1] = Double.MAX_VALUE
        a[index + 2] = -Double.MAX_VALUE
        a[index + 3] = -Double.MAX_VALUE
      }
      return a
    }

    private fun computeLayerIndices(itemSize: Int, nodeCapacity: Int): IntArray {
      val layerIndexList = IntArrayList()
      var layerSize = itemSize
      var index = 0
      do {
        layerIndexList.add(index)
        layerSize = numNodesToCover(layerSize, nodeCapacity)
        index += ENV_SIZE * layerSize
      } while (layerSize > 1)
      return layerIndexList.toArray()
    }

    /**
     * Computes the number of blocks (nodes) required to
     * cover a given number of children.
     *
     * @return the number of nodes needed to cover the children
     */
    private fun numNodesToCover(nChild: Int, nodeCapacity: Int): Int {
      val mult = nChild / nodeCapacity
      val total = mult * nodeCapacity
      if (total == nChild) return mult
      return mult + 1
    }
  }
}
