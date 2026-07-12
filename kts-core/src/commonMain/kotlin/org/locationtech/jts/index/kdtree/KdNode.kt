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

package org.locationtech.jts.index.kdtree

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Envelope

/**
 * A node of a [KdTree], which represents one or more points in the same location.
 *
 * @author dskea
 */
class KdNode {

  private val p: Coordinate
  private val data: Any?
  private var left: KdNode? = null
  private var right: KdNode? = null
  private var count: Int = 1

  /**
   * Creates a new KdNode.
   *
   * @param _x coordinate of point
   * @param _y coordinate of point
   * @param data a data objects to associate with this node
   */
  constructor(_x: Double, _y: Double, data: Any?) {
    p = Coordinate(_x, _y)
    this.data = data
  }

  /**
   * Creates a new KdNode.
   *
   * @param p point location of new node
   * @param data a data objects to associate with this node
   */
  constructor(p: Coordinate, data: Any?) {
    this.p = Coordinate(p)
    this.data = data
  }

  /**
   * Returns the X coordinate of the node
   *
   * @return X coordinate of the node
   */
  fun getX(): Double {
    return p.x
  }

  /**
   * Returns the Y coordinate of the node
   *
   * @return Y coordinate of the node
   */
  fun getY(): Double {
    return p.y
  }

  /**
   * Gets the split value at a node, depending on
   * whether the node splits on X or Y.
   *
   * @param isSplitOnX whether the node splits on X or Y
   * @return the splitting value
   */
  fun splitValue(isSplitOnX: Boolean): Double {
    if (isSplitOnX) {
      return p.getX()
    }
    return p.getY()
  }

  /**
   * Returns the location of this node
   *
   * @return p location of this node
   */
  fun getCoordinate(): Coordinate {
    return p
  }

  /**
   * Gets the user data object associated with this node.
   * @return user data
   */
  fun getData(): Any? {
    return data
  }

  /**
   * Returns the left node of the tree
   *
   * @return left node
   */
  fun getLeft(): KdNode? {
    return left
  }

  /**
   * Returns the right node of the tree
   *
   * @return right node
   */
  fun getRight(): KdNode? {
    return right
  }

  // Increments counts of points at this location
  internal fun increment() {
    count = count + 1
  }

  /**
   * Returns the number of inserted points that are coincident at this location.
   *
   * @return number of inserted points that this node represents
   */
  fun getCount(): Int {
    return count
  }

  /**
   * Tests whether more than one point with this value have been inserted (up to the tolerance)
   *
   * @return true if more than one point have been inserted with this value
   */
  fun isRepeated(): Boolean {
    return count > 1
  }

  // Sets left node value
  internal fun setLeft(_left: KdNode?) {
    left = _left
  }

  // Sets right node value
  internal fun setRight(_right: KdNode?) {
    right = _right
  }

  /**
   * Tests whether the node's left subtree may contain values
   * in a given range envelope.
   *
   * @param isSplitOnX whether the node splits on  X or Y
   * @param env the range envelope
   * @return true if the left subtree is in range
   */
  internal fun isRangeOverLeft(isSplitOnX: Boolean, env: Envelope): Boolean {
    val envMin: Double
    if (isSplitOnX) {
      envMin = env.getMinX()
    } else {
      envMin = env.getMinY()
    }
    val splitValue = splitValue(isSplitOnX)
    val isInRange = envMin < splitValue
    return isInRange
  }

  /**
   * Tests whether the node's right subtree may contain values
   * in a given range envelope.
   *
   * @param isSplitOnX whether the node splits on  X or Y
   * @param env the range envelope
   * @return true if the right subtree is in range
   */
  internal fun isRangeOverRight(isSplitOnX: Boolean, env: Envelope): Boolean {
    val envMax: Double
    if (isSplitOnX) {
      envMax = env.getMaxX()
    } else {
      envMax = env.getMaxY()
    }
    val splitValue = splitValue(isSplitOnX)
    val isInRange = splitValue <= envMax
    return isInRange
  }

  /**
   * Tests whether a point is strictly to the left
   * of the splitting plane for this node.
   *
   * @param isSplitOnX whether the node splits on  X or Y
   * @param pt the query point
   * @return true if the point is strictly to the left.
   *
   * @see splitValue
   */
  internal fun isPointOnLeft(isSplitOnX: Boolean, pt: Coordinate): Boolean {
    val ptOrdinate: Double
    if (isSplitOnX) {
      ptOrdinate = pt.x
    } else {
      ptOrdinate = pt.y
    }
    val splitValue = splitValue(isSplitOnX)
    val isInRange = (ptOrdinate < splitValue)
    return isInRange
  }
}
