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
package org.locationtech.jts.index.strtree

import kotlin.jvm.JvmStatic
import kotlin.jvm.JvmField

import org.locationtech.jts.index.ItemVisitor
import org.locationtech.jts.util.Assert

/**
 * Base class for STRtree and SIRtree. STR-packed R-trees are described in:
 * P. Rigaux, Michel Scholl and Agnes Voisard. *Spatial Databases With
 * Application To GIS.* Morgan Kaufmann, San Francisco, 2002.
 *
 * @see STRtree
 * @see SIRtree
 *
 */
abstract class AbstractSTRtree {

  /**
   * A test for intersection between two bounds, necessary because subclasses
   * of AbstractSTRtree have different implementations of bounds.
   */
  protected interface IntersectsOp {
    /**
     * For STRtrees, the bounds will be Envelopes; for SIRtrees, Intervals;
     * for other subclasses of AbstractSTRtree, some other class.
     * @param aBounds the bounds of one spatial object
     * @param bBounds the bounds of another spatial object
     * @return whether the two bounds intersect
     */
    fun intersects(aBounds: Any?, bBounds: Any?): Boolean
  }

  @JvmField
  protected var root: AbstractNode? = null

  private var built = false
  /**
   * Set to `null` when index is built, to avoid retaining memory.
   */
  private var itemBoundables: ArrayList<Boundable>? = ArrayList()

  private var nodeCapacity: Int = 0

  /**
   * Constructs an AbstractSTRtree with the specified maximum number of child
   * nodes that a node may have
   *
   * @param nodeCapacity the maximum number of child nodes in a node
   */
  constructor(nodeCapacity: Int) {
    Assert.isTrue(nodeCapacity > 1, "Node capacity must be greater than 1")
    this.nodeCapacity = nodeCapacity
  }

  /**
   * Constructs an AbstractSTRtree with the default node capacity.
   */
  constructor() : this(DEFAULT_NODE_CAPACITY)

  /**
   * Constructs an AbstractSTRtree with the specified maximum number of child
   * nodes that a node may have, and the root node
   */
  constructor(nodeCapacity: Int, root: AbstractNode) : this(nodeCapacity) {
    built = true
    this.root = root
    this.itemBoundables = null
  }

  /**
   * Constructs an AbstractSTRtree with the specified maximum number of child
   * nodes that a node may have, and all leaf nodes in the tree
   */
  constructor(nodeCapacity: Int, itemBoundables: ArrayList<Boundable>) : this(nodeCapacity) {
    this.itemBoundables = itemBoundables
  }

  /**
   * Creates parent nodes, grandparent nodes, and so forth up to the root
   * node, for the data that has been inserted into the tree.
   */
  fun build() {
    if (built) return
    root = if (itemBoundables!!.isEmpty()) {
      createNode(0)
    } else {
      createHigherLevels(itemBoundables!!, -1)
    }
    // the item list is no longer needed
    itemBoundables = null
    built = true
  }

  protected abstract fun createNode(level: Int): AbstractNode

  /**
   * Sorts the childBoundables then divides them into groups of size M, where
   * M is the node capacity.
   */
  protected open fun createParentBoundables(childBoundables: MutableList<Boundable>, newLevel: Int): MutableList<Boundable> {
    Assert.isTrue(!childBoundables.isEmpty())
    val parentBoundables = ArrayList<Boundable>()
    parentBoundables.add(createNode(newLevel))
    val sortedChildBoundables = ArrayList(childBoundables)
    sortedChildBoundables.sortWith(getComparator())
    val i = sortedChildBoundables.iterator()
    while (i.hasNext()) {
      val childBoundable = i.next()
      if (lastNode(parentBoundables).getChildBoundables().size == getNodeCapacity()) {
        parentBoundables.add(createNode(newLevel))
      }
      lastNode(parentBoundables).addChildBoundable(childBoundable)
    }
    return parentBoundables
  }

  protected fun lastNode(nodes: MutableList<Boundable>): AbstractNode {
    return nodes[nodes.size - 1] as AbstractNode
  }

  /**
   * Creates the levels higher than the given level
   *
   * @param boundablesOfALevel the level to build on
   * @param level the level of the Boundables, or -1 if the boundables are item
   *            boundables (that is, below level 0)
   * @return the root, which may be a ParentNode or a LeafNode
   */
  private fun createHigherLevels(boundablesOfALevel: MutableList<Boundable>, level: Int): AbstractNode {
    Assert.isTrue(!boundablesOfALevel.isEmpty())
    val parentBoundables = createParentBoundables(boundablesOfALevel, level + 1)
    if (parentBoundables.size == 1) {
      return parentBoundables[0] as AbstractNode
    }
    return createHigherLevels(parentBoundables, level + 1)
  }

  /**
   * Gets the root node of the tree.
   *
   * @return the root node
   */
  open fun getRoot(): AbstractNode {
    build()
    return root!!
  }

  /**
   * Returns the maximum number of child nodes that a node may have.
   *
   * @return the node capacity
   */
  fun getNodeCapacity(): Int {
    return nodeCapacity
  }

  /**
   * Tests whether the index contains any items.
   *
   * @return true if the index does not contain any items
   */
  fun isEmpty(): Boolean {
    if (!built) return itemBoundables!!.isEmpty()
    return root!!.isEmpty()
  }

  protected open fun size(): Int {
    if (isEmpty()) {
      return 0
    }
    build()
    return size(root!!)
  }

  protected fun size(node: AbstractNode): Int {
    var size = 0
    val i = node.getChildBoundables().iterator()
    while (i.hasNext()) {
      val childBoundable = i.next()
      if (childBoundable is AbstractNode) {
        size += size(childBoundable)
      } else if (childBoundable is ItemBoundable) {
        size += 1
      }
    }
    return size
  }

  protected open fun depth(): Int {
    if (isEmpty()) {
      return 0
    }
    build()
    return depth(root!!)
  }

  protected fun depth(node: AbstractNode): Int {
    var maxChildDepth = 0
    val i = node.getChildBoundables().iterator()
    while (i.hasNext()) {
      val childBoundable = i.next()
      if (childBoundable is AbstractNode) {
        val childDepth = depth(childBoundable)
        if (childDepth > maxChildDepth) maxChildDepth = childDepth
      }
    }
    return maxChildDepth + 1
  }

  protected fun insert(bounds: Any?, item: Any?) {
    Assert.isTrue(!built, "Cannot insert items into an STR packed R-tree after it has been built.")
    itemBoundables!!.add(ItemBoundable(bounds, item))
  }

  /**
   *  Also builds the tree, if necessary.
   */
  protected fun query(searchBounds: Any?): MutableList<Any?> {
    build()
    val matches = ArrayList<Any?>()
    if (isEmpty()) {
      return matches
    }
    if (getIntersectsOp().intersects(root!!.getBounds(), searchBounds)) {
      queryInternal(searchBounds, root!!, matches)
    }
    return matches
  }

  /**
   *  Also builds the tree, if necessary.
   */
  protected fun query(searchBounds: Any?, visitor: ItemVisitor) {
    build()
    if (isEmpty()) {
      // nothing in tree, so return
      return
    }
    if (getIntersectsOp().intersects(root!!.getBounds(), searchBounds)) {
      queryInternal(searchBounds, root!!, visitor)
    }
  }

  /**
   * @return a test for intersection between two bounds
   * @see IntersectsOp
   */
  protected abstract fun getIntersectsOp(): IntersectsOp

  private fun queryInternal(searchBounds: Any?, node: AbstractNode, matches: MutableList<Any?>) {
    val childBoundables = node.getChildBoundables()
    for (i in 0 until childBoundables.size) {
      val childBoundable = childBoundables[i]
      if (!getIntersectsOp().intersects(childBoundable.getBounds(), searchBounds)) {
        continue
      }
      if (childBoundable is AbstractNode) {
        queryInternal(searchBounds, childBoundable, matches)
      } else if (childBoundable is ItemBoundable) {
        matches.add(childBoundable.getItem())
      } else {
        Assert.shouldNeverReachHere()
      }
    }
  }

  private fun queryInternal(searchBounds: Any?, node: AbstractNode, visitor: ItemVisitor) {
    val childBoundables = node.getChildBoundables()
    for (i in 0 until childBoundables.size) {
      val childBoundable = childBoundables[i]
      if (!getIntersectsOp().intersects(childBoundable.getBounds(), searchBounds)) {
        continue
      }
      if (childBoundable is AbstractNode) {
        queryInternal(searchBounds, childBoundable, visitor)
      } else if (childBoundable is ItemBoundable) {
        visitor.visitItem(childBoundable.getItem())
      } else {
        Assert.shouldNeverReachHere()
      }
    }
  }

  /**
   * Gets a tree structure (as a nested list)
   * corresponding to the structure of the items and nodes in this tree.
   *
   * @return a List of items and/or Lists
   */
  fun itemsTree(): MutableList<Any?> {
    build()

    val valuesTree = itemsTree(root!!)
    if (valuesTree == null) return ArrayList<Any?>()
    return valuesTree
  }

  private fun itemsTree(node: AbstractNode): MutableList<Any?>? {
    val valuesTreeForNode = ArrayList<Any?>()
    val i = node.getChildBoundables().iterator()
    while (i.hasNext()) {
      val childBoundable = i.next()
      if (childBoundable is AbstractNode) {
        val valuesTreeForChild = itemsTree(childBoundable)
        // only add if not null (which indicates an item somewhere in this tree
        if (valuesTreeForChild != null) valuesTreeForNode.add(valuesTreeForChild)
      } else if (childBoundable is ItemBoundable) {
        valuesTreeForNode.add(childBoundable.getItem())
      } else {
        Assert.shouldNeverReachHere()
      }
    }
    if (valuesTreeForNode.size <= 0) return null
    return valuesTreeForNode
  }

  /**
   * Removes an item from the tree.
   * (Builds the tree, if necessary.)
   */
  protected fun remove(searchBounds: Any?, item: Any?): Boolean {
    build()
    if (getIntersectsOp().intersects(root!!.getBounds(), searchBounds)) {
      return remove(searchBounds, root!!, item)
    }
    return false
  }

  private fun removeItem(node: AbstractNode, item: Any?): Boolean {
    var childToRemove: Boundable? = null
    val i = node.getChildBoundables().iterator()
    while (i.hasNext()) {
      val childBoundable = i.next()
      if (childBoundable is ItemBoundable) {
        if (childBoundable.getItem() === item) childToRemove = childBoundable
      }
    }
    if (childToRemove != null) {
      node.getChildBoundables().remove(childToRemove)
      return true
    }
    return false
  }

  private fun remove(searchBounds: Any?, node: AbstractNode, item: Any?): Boolean {
    // first try removing item from this node
    var found = removeItem(node, item)
    if (found) return true

    var childToPrune: AbstractNode? = null
    // next try removing item from lower nodes
    val i = node.getChildBoundables().iterator()
    while (i.hasNext()) {
      val childBoundable = i.next()
      if (!getIntersectsOp().intersects(childBoundable.getBounds(), searchBounds)) {
        continue
      }
      if (childBoundable is AbstractNode) {
        found = remove(searchBounds, childBoundable, item)
        // if found, record child for pruning and exit
        if (found) {
          childToPrune = childBoundable
          break
        }
      }
    }
    // prune child if possible
    if (childToPrune != null) {
      if (childToPrune.getChildBoundables().isEmpty()) {
        node.getChildBoundables().remove(childToPrune)
      }
    }
    return found
  }

  protected open fun boundablesAtLevel(level: Int): MutableList<Boundable> {
    val boundables = ArrayList<Boundable>()
    boundablesAtLevel(level, root!!, boundables)
    return boundables
  }

  /**
   * @param level -1 to get items
   */
  private fun boundablesAtLevel(level: Int, top: AbstractNode, boundables: MutableCollection<Boundable>) {
    Assert.isTrue(level > -2)
    if (top.getLevel() == level) {
      boundables.add(top)
      return
    }
    val i = top.getChildBoundables().iterator()
    while (i.hasNext()) {
      val boundable = i.next()
      if (boundable is AbstractNode) {
        boundablesAtLevel(level, boundable, boundables)
      } else {
        Assert.isTrue(boundable is ItemBoundable)
        if (level == -1) {
          boundables.add(boundable)
        }
      }
    }
    return
  }

  protected abstract fun getComparator(): Comparator<Boundable>

  fun getItemBoundables(): ArrayList<Boundable>? {
    return itemBoundables
  }

  companion object {

    private const val DEFAULT_NODE_CAPACITY = 10

    @JvmStatic
    fun compareDoubles(a: Double, b: Double): Int {
      return if (a > b) 1
      else if (a < b) -1
      else 0
    }
  }
}
