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

import org.locationtech.jts.util.Assert

/**
 * A node of an [AbstractSTRtree]. A node is one of:
 * <ul>
 * <li>empty
 * <li>an <i>interior node</i> containing child [AbstractNode]s
 * <li>a <i>leaf node</i> containing data items ([ItemBoundable]s).
 * </ul>
 * A node stores the bounds of its children, and its level within the index tree.
 *
 * @version 1.7
 */
abstract class AbstractNode(private val level: Int) : Boundable {

  private var childBoundables: MutableList<Boundable> = ArrayList()
  private var bounds: Any? = null

  /**
   * Default constructor required for serialization.
   */
  constructor() : this(0)

  /**
   * Returns either child [AbstractNode]s, or if this is a leaf node, real data (wrapped
   * in [ItemBoundable]s).
   *
   * @return a list of the children
   */
  fun getChildBoundables(): MutableList<Boundable> {
    return childBoundables
  }

  /**
   * Returns a representation of space that encloses this Boundable.
   *
   * @return an Envelope (for STRtrees), an Interval (for SIRtrees), or other
   *         object (for other subclasses of AbstractSTRtree)
   * @see AbstractSTRtree.IntersectsOp
   */
  protected abstract fun computeBounds(): Any?

  /**
   * Gets the bounds of this node
   *
   * @return the object representing bounds in this index
   */
  override fun getBounds(): Any? {
    if (bounds == null) {
      bounds = computeBounds()
    }
    return bounds
  }

  /**
   * Returns 0 if this node is a leaf, 1 if a parent of a leaf, and so on; the
   * root node will have the highest level
   *
   * @return the node level
   */
  fun getLevel(): Int {
    return level
  }

  /**
   * Gets the count of the [Boundable]s at this node.
   *
   * @return the count of boundables at this node
   */
  fun size(): Int {
    return childBoundables.size
  }

  /**
   * Tests whether there are any [Boundable]s at this node.
   *
   * @return true if there are boundables at this node
   */
  fun isEmpty(): Boolean {
    return childBoundables.isEmpty()
  }

  /**
   * Adds either an AbstractNode, or if this is a leaf node, a data object
   * (wrapped in an ItemBoundable)
   *
   * @param childBoundable the child to add
   */
  fun addChildBoundable(childBoundable: Boundable) {
    Assert.isTrue(bounds == null)
    childBoundables.add(childBoundable)
  }

  internal fun setChildBoundables(childBoundables: MutableList<Boundable>) {
    this.childBoundables = childBoundables
  }

  companion object {
  }
}
