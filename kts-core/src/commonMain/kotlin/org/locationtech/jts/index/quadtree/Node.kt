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
import org.locationtech.jts.util.Assert

/**
 * Represents a node of a [Quadtree].  Nodes contain
 * items which have a spatial extent corresponding to the node's position
 * in the quadtree.
 *
 * @version 1.7
 */
class Node(private val env: Envelope, private val level: Int) : NodeBase() {

  private val centrex: Double = (env.getMinX() + env.getMaxX()) / 2
  private val centrey: Double = (env.getMinY() + env.getMaxY()) / 2

  fun getEnvelope(): Envelope {
    return env
  }

  override fun isSearchMatch(searchEnv: Envelope?): Boolean {
    if (searchEnv == null) return false
    return env.intersects(searchEnv)
  }

  /**
   * Returns the subquad containing the envelope `searchEnv`.
   * Creates the subquad if it does not already exist.
   *
   * @return the subquad containing the search envelope
   */
  fun getNode(searchEnv: Envelope): Node {
    val subnodeIndex = getSubnodeIndex(searchEnv, centrex, centrey)
    // if subquadIndex is -1 searchEnv is not contained in a subquad
    return if (subnodeIndex != -1) {
      // create the quad if it does not exist
      val node = getSubnode(subnodeIndex)
      // recursively search the found/created quad
      node.getNode(searchEnv)
    } else {
      this
    }
  }

  /**
   * Returns the smallest <i>existing</i>
   * node containing the envelope.
   */
  fun find(searchEnv: Envelope): NodeBase {
    val subnodeIndex = getSubnodeIndex(searchEnv, centrex, centrey)
    if (subnodeIndex == -1) return this
    if (subnode[subnodeIndex] != null) {
      // query lies in subquad, so search it
      val node = subnode[subnodeIndex]!!
      return node.find(searchEnv)
    }
    // no existing subquad, so return this one anyway
    return this
  }

  fun insertNode(node: Node) {
    Assert.isTrue(env.contains(node.env))
    val index = getSubnodeIndex(node.env, centrex, centrey)
    if (node.level == level - 1) {
      subnode[index] = node
    } else {
      // the quad is not a direct child, so make a new child quad to contain it
      // and recursively insert the quad
      val childNode = createSubnode(index)
      childNode.insertNode(node)
      subnode[index] = childNode
    }
  }

  /**
   * get the subquad for the index.
   * If it doesn't exist, create it
   */
  private fun getSubnode(index: Int): Node {
    if (subnode[index] == null) {
      subnode[index] = createSubnode(index)
    }
    return subnode[index]!!
  }

  private fun createSubnode(index: Int): Node {
    // create a new subquad in the appropriate quadrant

    var minx = 0.0
    var maxx = 0.0
    var miny = 0.0
    var maxy = 0.0

    when (index) {
      0 -> {
        minx = env.getMinX()
        maxx = centrex
        miny = env.getMinY()
        maxy = centrey
      }
      1 -> {
        minx = centrex
        maxx = env.getMaxX()
        miny = env.getMinY()
        maxy = centrey
      }
      2 -> {
        minx = env.getMinX()
        maxx = centrex
        miny = centrey
        maxy = env.getMaxY()
      }
      3 -> {
        minx = centrex
        maxx = env.getMaxX()
        miny = centrey
        maxy = env.getMaxY()
      }
    }
    val sqEnv = Envelope(minx, maxx, miny, maxy)
    val node = Node(sqEnv, level - 1)
    return node
  }

  fun getLevel(): Int {
    return level
  }

  companion object {
    @JvmStatic
    fun createNode(env: Envelope): Node {
      val key = Key(env)
      val node = Node(key.getEnvelope(), key.getLevel())
      return node
    }

    @JvmStatic
    fun createExpanded(node: Node?, addEnv: Envelope): Node {
      val expandEnv = Envelope(addEnv)
      if (node != null) expandEnv.expandToInclude(node.env)

      val largerNode = createNode(expandEnv)
      if (node != null) largerNode.insertNode(node)
      return largerNode
    }
  }
}
