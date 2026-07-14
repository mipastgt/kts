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

import org.locationtech.jts.util.Assert

/**
 * A node of a [Bintree].
 *
 */
class Node(interval: Interval, level: Int) : NodeBase() {

  private val interval: Interval = interval
  private val centre: Double = (interval.getMin() + interval.getMax()) / 2
  private val level: Int = level

  fun getInterval(): Interval = interval

  override fun isSearchMatch(itemInterval: Interval): Boolean {
    return itemInterval.overlaps(interval)
  }

  /**
   * Returns the subnode containing the envelope.
   * Creates the node if
   * it does not already exist.
   */
  fun getNode(searchInterval: Interval): Node {
    val subnodeIndex = NodeBase.getSubnodeIndex(searchInterval, centre)
    // if index is -1 searchEnv is not contained in a subnode
    if (subnodeIndex != -1) {
      // create the node if it does not exist
      val node = getSubnode(subnodeIndex)
      // recursively search the found/created node
      return node.getNode(searchInterval)
    } else {
      return this
    }
  }

  /**
   * Returns the smallest *existing*
   * node containing the envelope.
   */
  fun find(searchInterval: Interval): NodeBase {
    val subnodeIndex = NodeBase.getSubnodeIndex(searchInterval, centre)
    if (subnodeIndex == -1)
      return this
    val node = subnode[subnodeIndex]
    if (node != null) {
      // query lies in subnode, so search it
      return node.find(searchInterval)
    }
    // no existing subnode, so return this one anyway
    return this
  }

  internal fun insert(node: Node) {
    Assert.isTrue(interval.contains(node.interval))
    val index = NodeBase.getSubnodeIndex(node.interval, centre)
    if (node.level == level - 1) {
      subnode[index] = node
    } else {
      // the node is not a direct child, so make a new child node to contain it
      // and recursively insert the node
      val childNode = createSubnode(index)
      childNode.insert(node)
      subnode[index] = childNode
    }
  }

  /**
   * get the subnode for the index.
   * If it doesn't exist, create it
   */
  private fun getSubnode(index: Int): Node {
    if (subnode[index] == null) {
      subnode[index] = createSubnode(index)
    }
    return subnode[index]!!
  }

  private fun createSubnode(index: Int): Node {
    // create a new subnode in the appropriate interval

    var min = 0.0
    var max = 0.0

    when (index) {
      0 -> {
        min = interval.getMin()
        max = centre
      }
      1 -> {
        min = centre
        max = interval.getMax()
      }
    }
    val subInt = Interval(min, max)
    val node = Node(subInt, level - 1)
    return node
  }

  companion object {
    @JvmStatic
    fun createNode(itemInterval: Interval): Node {
      val key = Key(itemInterval)
      val node = Node(key.getInterval(), key.getLevel())
      return node
    }

    @JvmStatic
    fun createExpanded(node: Node?, addInterval: Interval): Node {
      val expandInt = Interval(addInterval)
      if (node != null) expandInt.expandToInclude(node.interval)

      val largerNode = createNode(expandInt)
      if (node != null) largerNode.insert(node)
      return largerNode
    }
  }
}
