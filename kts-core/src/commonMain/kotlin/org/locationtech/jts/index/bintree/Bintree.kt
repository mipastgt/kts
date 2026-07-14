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
package org.locationtech.jts.index.bintree

import kotlin.jvm.JvmStatic

/**
 * An `BinTree` (or "Binary Interval Tree")
 * is a 1-dimensional version of a quadtree.
 * It indexes 1-dimensional intervals (which may
 * be the projection of 2-D objects on an axis).
 * It supports range searching
 * (where the range may be a single point).
 *
 * This implementation does not require specifying the extent of the inserted
 * items beforehand.  It will automatically expand to accommodate any extent
 * of dataset.
 *
 */
class Bintree {

  private val root: Root = Root()
  /**
   * minExtent is the minimum extent of all items
   * inserted into the tree so far. It is used as a heuristic value
   * to construct non-zero extents for features with zero extent.
   */
  private var minExtent = 1.0

  fun depth(): Int {
    if (root != null) return root.depth()
    return 0
  }

  fun size(): Int {
    if (root != null) return root.size()
    return 0
  }

  /**
   * Compute the total number of nodes in the tree
   *
   * @return the number of nodes in the tree
   */
  fun nodeSize(): Int {
    if (root != null) return root.nodeSize()
    return 0
  }

  fun insert(itemInterval: Interval, item: Any?) {
    collectStats(itemInterval)
    val insertInterval = ensureExtent(itemInterval, minExtent)
    root.insert(insertInterval, item)
  }

  /**
   * Removes a single item from the tree.
   *
   * @param itemInterval the interval of the item to be removed
   * @param item the item to remove
   * @return `true` if the item was found (and thus removed)
   */
  fun remove(itemInterval: Interval, item: Any?): Boolean {
    val insertInterval = ensureExtent(itemInterval, minExtent)
    return root.remove(insertInterval, item)
  }

  fun iterator(): MutableIterator<Any?> {
    val foundItems = ArrayList<Any?>()
    root.addAllItems(foundItems)
    return foundItems.iterator()
  }

  fun query(x: Double): MutableList<Any?> {
    return query(Interval(x, x))
  }

  /**
   * Queries the tree to find all candidate items which
   * may overlap the query interval.
   * If the query interval is `null`, all items in the tree are found.
   *
   * min and max may be the same value
   */
  fun query(interval: Interval?): MutableList<Any?> {
    /**
     * the items that are matched are all items in intervals
     * which overlap the query interval
     */
    val foundItems = ArrayList<Any?>()
    query(interval, foundItems)
    return foundItems
  }

  /**
   * Adds items in the tree which potentially overlap the query interval
   * to the given collection.
   * If the query interval is `null`, add all items in the tree.
   *
   * @param interval a query interval, or null
   * @param foundItems the candidate items found
   */
  fun query(interval: Interval?, foundItems: MutableCollection<Any?>) {
    root.addAllItemsFromOverlapping(interval, foundItems)
  }

  private fun collectStats(interval: Interval) {
    val del = interval.getWidth()
    if (del < minExtent && del > 0.0)
      minExtent = del
  }

  companion object {
    /**
     * Ensure that the Interval for the inserted item has non-zero extents.
     * Use the current minExtent to pad it, if necessary
     */
    @JvmStatic
    fun ensureExtent(itemInterval: Interval, minExtent: Double): Interval {
      var min = itemInterval.getMin()
      var max = itemInterval.getMax()
      // has a non-zero extent
      if (min != max) return itemInterval

      // pad extent
      if (min == max) {
        min = min - minExtent / 2.0
        max = min + minExtent / 2.0
      }
      return Interval(min, max)
    }
  }
}
