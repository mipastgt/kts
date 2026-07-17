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

import org.locationtech.jts.index.quadtree.IntervalSize
import org.locationtech.jts.util.Assert

/**
 * The root node of a single [Bintree].
 * It is centred at the origin,
 * and does not have a defined extent.
 *
 */
class Root : NodeBase() {

  /**
   * Insert an item into the tree this is the root of.
   */
  fun insert(itemInterval: Interval, item: Any?) {
    val index = NodeBase.getSubnodeIndex(itemInterval, origin)
    // if index is -1, itemEnv must contain the origin.
    if (index == -1) {
      add(item)
      return
    }
    /**
     * the item must be contained in one interval, so insert it into the
     * tree for that interval (which may not yet exist)
     */
    val node = subnode[index]
    /*
     *  If the subnode doesn't exist or this item is not contained in it,
     *  have to expand the tree upward to contain the item.
     */
    if (node == null || !node.getInterval().contains(itemInterval)) {
      val largerNode = Node.createExpanded(node, itemInterval)
      subnode[index] = largerNode
    }
    /*
     * At this point we have a subnode which exists and must contain
     * contains the env for the item.  Insert the item into the tree.
     */
    insertContained(subnode[index]!!, itemInterval, item)
  }

  /**
   * insert an item which is known to be contained in the tree rooted at
   * the given Node.  Lower levels of the tree will be created
   * if necessary to hold the item.
   */
  private fun insertContained(tree: Node, itemInterval: Interval, item: Any?) {
    Assert.isTrue(tree.getInterval().contains(itemInterval))
    /**
     * Do NOT create a new node for zero-area intervals - this would lead
     * to infinite recursion. Instead, use a heuristic of simply returning
     * the smallest existing node containing the query
     */
    val isZeroArea = IntervalSize.isZeroWidth(itemInterval.getMin(), itemInterval.getMax())
    val node: NodeBase
    if (isZeroArea)
      node = tree.find(itemInterval)
    else
      node = tree.getNode(itemInterval)
    node.add(item)
  }

  /**
   * The root node matches all searches
   */
  override fun isSearchMatch(interval: Interval): Boolean {
    return true
  }

  companion object {
    // the singleton root node is centred at the origin.
    private const val origin = 0.0
  }
}
