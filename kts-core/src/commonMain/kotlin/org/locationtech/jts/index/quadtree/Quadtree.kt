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
package org.locationtech.jts.index.quadtree

import kotlin.jvm.JvmStatic

import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.index.ArrayListVisitor
import org.locationtech.jts.index.ItemVisitor
import org.locationtech.jts.index.SpatialIndex

/**
 * A Quadtree is a spatial index structure for efficient range querying
 * of items bounded by 2D rectangles.
 *
 */
class Quadtree : SpatialIndex {

  private val root: Root = Root()

  /**
   * minExtent is the minimum envelope extent of all items
   * inserted into the tree so far.
   */
  private var minExtent = 1.0

  /**
   * Returns the number of levels in the tree.
   */
  fun depth(): Int {
    return root.depth()
  }

  /**
   * Tests whether the index contains any items.
   *
   * @return true if the index does not contain any items
   */
  fun isEmpty(): Boolean {
    return root.isEmpty()
  }

  /**
   * Returns the number of items in the tree.
   *
   * @return the number of items in the tree
   */
  fun size(): Int {
    return root.size()
  }

  override fun insert(itemEnv: Envelope, item: Any?) {
    collectStats(itemEnv)
    val insertEnv = ensureExtent(itemEnv, minExtent)
    root.insert(insertEnv, item)
  }

  /**
   * Removes a single item from the tree.
   *
   * @param itemEnv the Envelope of the item to be removed
   * @param item the item to remove
   * @return `true` if the item was found (and thus removed)
   */
  override fun remove(itemEnv: Envelope, item: Any?): Boolean {
    val posEnv = ensureExtent(itemEnv, minExtent)
    return root.remove(posEnv, item)
  }

  /**
   * Queries the tree and returns items which may lie in the given search envelope.
   *
   * @param searchEnv the envelope of the desired query area.
   * @return a List of items which may intersect the search envelope
   */
  override fun query(searchEnv: Envelope?): MutableList<*> {
    /**
     * the items that are matched are the items in quads which
     * overlap the search envelope
     */
    val visitor = ArrayListVisitor()
    query(searchEnv, visitor)
    return visitor.getItems()
  }

  /**
   * Queries the tree and visits items which may lie in the given search envelope.
   *
   * @param searchEnv the envelope of the desired query area.
   * @param visitor a visitor object which is passed the visited items
   */
  override fun query(searchEnv: Envelope?, visitor: ItemVisitor) {
    /*
     * the items that are matched are the items in quads which
     * overlap the search envelope
     */
    root.visit(searchEnv, visitor)
  }

  /**
   * Return a list of all items in the Quadtree
   */
  fun queryAll(): MutableList<*> {
    val foundItems = ArrayList<Any?>()
    root.addAllItems(foundItems)
    return foundItems
  }

  private fun collectStats(itemEnv: Envelope) {
    val delX = itemEnv.getWidth()
    if (delX < minExtent && delX > 0.0) minExtent = delX

    val delY = itemEnv.getHeight()
    if (delY < minExtent && delY > 0.0) minExtent = delY
  }

  fun getRoot(): Root {
    return root
  }

  companion object {

    /**
     * Ensure that the envelope for the inserted item has non-zero extents.
     * Use the current minExtent to pad the envelope, if necessary
     */
    @JvmStatic
    fun ensureExtent(itemEnv: Envelope, minExtent: Double): Envelope {
      var minx = itemEnv.getMinX()
      var maxx = itemEnv.getMaxX()
      var miny = itemEnv.getMinY()
      var maxy = itemEnv.getMaxY()
      // has a non-zero extent
      if (minx != maxx && miny != maxy) return itemEnv

      // pad one or both extents
      if (minx == maxx) {
        minx -= minExtent / 2.0
        maxx += minExtent / 2.0
      }
      if (miny == maxy) {
        miny -= minExtent / 2.0
        maxy += minExtent / 2.0
      }
      return Envelope(minx, maxx, miny, maxy)
    }
  }
}
