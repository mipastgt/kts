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
import kotlin.jvm.JvmField

/**
 * The base class for nodes in a [Bintree].
 *
 */
abstract class NodeBase {

  private val items: MutableList<Any?> = ArrayList()

  /**
   * subnodes are numbered as follows:
   *
   *  0 | 1
   */
  @JvmField
  protected val subnode: Array<Node?> = arrayOfNulls(2)

  fun getItems(): MutableList<Any?> = items

  fun add(item: Any?) {
    items.add(item)
  }

  fun addAllItems(items: MutableList<Any?>): MutableList<Any?> {
    items.addAll(this.items)
    for (i in 0 until 2) {
      if (subnode[i] != null) {
        subnode[i]!!.addAllItems(items)
      }
    }
    return items
  }

  protected abstract fun isSearchMatch(interval: Interval): Boolean

  /**
   * Adds items in the tree which potentially overlap the query interval
   * to the given collection.
   * If the query interval is `null`, add all items in the tree.
   *
   * @param interval a query interval, or null
   * @param resultItems the candidate items found
   */
  fun addAllItemsFromOverlapping(interval: Interval?, resultItems: MutableCollection<Any?>) {
    if (interval != null && !isSearchMatch(interval))
      return

    // some of these may not actually overlap - this is allowed by the bintree contract
    resultItems.addAll(items)

    if (subnode[0] != null) subnode[0]!!.addAllItemsFromOverlapping(interval, resultItems)
    if (subnode[1] != null) subnode[1]!!.addAllItemsFromOverlapping(interval, resultItems)
  }

  /**
   * Removes a single item from this subtree.
   *
   * @param itemInterval the envelope containing the item
   * @param item the item to remove
   * @return `true` if the item was found and removed
   */
  fun remove(itemInterval: Interval, item: Any?): Boolean {
    // use interval to restrict nodes scanned
    if (!isSearchMatch(itemInterval))
      return false

    var found = false
    for (i in 0 until 2) {
      val sub = subnode[i]
      if (sub != null) {
        found = sub.remove(itemInterval, item)
        if (found) {
          // trim subtree if empty
          if (sub.isPrunable())
            subnode[i] = null
          break
        }
      }
    }
    // if item was found lower down, don't need to search for it here
    if (found) return found
    // otherwise, try and remove the item from the list of items in this node
    found = items.remove(item)
    return found
  }

  fun isPrunable(): Boolean {
    return !(hasChildren() || hasItems())
  }

  fun hasChildren(): Boolean {
    for (i in 0 until 2) {
      if (subnode[i] != null)
        return true
    }
    return false
  }

  fun hasItems(): Boolean = !items.isEmpty()

  internal fun depth(): Int {
    var maxSubDepth = 0
    for (i in 0 until 2) {
      val sub = subnode[i]
      if (sub != null) {
        val sqd = sub.depth()
        if (sqd > maxSubDepth)
          maxSubDepth = sqd
      }
    }
    return maxSubDepth + 1
  }

  internal fun size(): Int {
    var subSize = 0
    for (i in 0 until 2) {
      val sub = subnode[i]
      if (sub != null) {
        subSize += sub.size()
      }
    }
    return subSize + items.size
  }

  internal fun nodeSize(): Int {
    var subSize = 0
    for (i in 0 until 2) {
      val sub = subnode[i]
      if (sub != null) {
        subSize += sub.nodeSize()
      }
    }
    return subSize + 1
  }

  companion object {
    /**
     * Returns the index of the subnode that wholely contains the given interval.
     * If none does, returns -1.
     */
    @JvmStatic
    fun getSubnodeIndex(interval: Interval, centre: Double): Int {
      var subnodeIndex = -1
      if (interval.min >= centre) subnodeIndex = 1
      if (interval.max <= centre) subnodeIndex = 0
      return subnodeIndex
    }
  }
}
