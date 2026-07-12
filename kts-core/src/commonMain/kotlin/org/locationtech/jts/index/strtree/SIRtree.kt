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
import kotlin.math.max
import kotlin.math.min

/**
 * One-dimensional version of an STR-packed R-tree. SIR stands for
 * "Sort-Interval-Recursive". STR-packed R-trees are described in:
 * P. Rigaux, Michel Scholl and Agnes Voisard. Spatial Databases With
 * Application To GIS. Morgan Kaufmann, San Francisco, 2002.
 *
 * @see STRtree
 *
 * @version 1.7
 */
open class SIRtree : AbstractSTRtree {

  private val comparator: Comparator<Boundable> = Comparator { o1, o2 ->
    AbstractSTRtree.compareDoubles(
      (o1.getBounds() as Interval).getCentre(),
      (o2.getBounds() as Interval).getCentre()
    )
  }

  private val intersectsOp: IntersectsOp = object : IntersectsOp {
    override fun intersects(aBounds: Any?, bBounds: Any?): Boolean {
      return (aBounds as Interval).intersects(bBounds as Interval)
    }
  }

  /**
   * Constructs an SIRtree with the default node capacity.
   */
  constructor() : this(10)

  /**
   * Constructs an SIRtree with the given maximum number of child nodes that
   * a node may have
   */
  constructor(nodeCapacity: Int) : super(nodeCapacity)

  override fun createNode(level: Int): AbstractNode {
    return object : AbstractNode(level) {
      override fun computeBounds(): Any? {
        var bounds: Interval? = null
        val i = getChildBoundables().iterator()
        while (i.hasNext()) {
          val childBoundable = i.next()
          if (bounds == null) {
            bounds = Interval(childBoundable.getBounds() as Interval)
          } else {
            bounds.expandToInclude(childBoundable.getBounds() as Interval)
          }
        }
        return bounds
      }
    }
  }

  /**
   * Inserts an item having the given bounds into the tree.
   */
  fun insert(x1: Double, x2: Double, item: Any?) {
    super.insert(Interval(min(x1, x2), max(x1, x2)), item)
  }

  /**
   * Returns items whose bounds intersect the given value.
   */
  fun query(x: Double): MutableList<Any?> {
    return query(x, x)
  }

  /**
   * Returns items whose bounds intersect the given bounds.
   * @param x1 possibly equal to x2
   */
  fun query(x1: Double, x2: Double): MutableList<Any?> {
    return super.query(Interval(min(x1, x2), max(x1, x2)))
  }

  override fun getIntersectsOp(): IntersectsOp {
    return intersectsOp
  }

  override fun getComparator(): Comparator<Boundable> {
    return comparator
  }
}
