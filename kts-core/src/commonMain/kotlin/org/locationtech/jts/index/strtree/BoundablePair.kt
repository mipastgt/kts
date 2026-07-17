/*
 * Copyright (c) 2016 Martin Davis.
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

import org.locationtech.jts.util.PriorityQueue
import org.locationtech.jts.geom.Envelope

/**
 * A pair of [Boundable]s, whose leaf items
 * support a distance metric between them.
 *
 * @author Martin Davis
 */
internal class BoundablePair(
  private val boundable1: Boundable,
  private val boundable2: Boundable,
  private val itemDistance: ItemDistance
) : Comparable<Any?> {

  private val distance: Double = distance()

  /**
   * Gets one of the member [Boundable]s in the pair
   * (indexed by [0, 1]).
   *
   * @param i the index of the member to return (0 or 1)
   * @return the chosen member
   */
  fun getBoundable(i: Int): Boundable {
    if (i == 0) return boundable1
    return boundable2
  }

  /**
   * Computes the maximum distance between any
   * two items in the pair of nodes.
   *
   * @return the maximum distance between items in the pair
   */
  fun maximumDistance(): Double {
    return EnvelopeDistance.maximumDistance(
      boundable1.getBounds() as Envelope,
      boundable2.getBounds() as Envelope
    )
  }

  /**
   * Computes the distance between the [Boundable]s in this pair.
   */
  private fun distance(): Double {
    // if items, compute exact distance
    if (isLeaves()) {
      return itemDistance.distance(boundable1 as ItemBoundable, boundable2 as ItemBoundable)
    }
    // otherwise compute distance between bounds of boundables
    return (boundable1.getBounds() as Envelope).distance(boundable2.getBounds() as Envelope)
  }

  /**
   * Gets the minimum possible distance between the Boundables in
   * this pair.
   *
   * @return the exact or lower bound distance for this pair
   */
  fun getDistance(): Double {
    return distance
  }

  /**
   * Compares two pairs based on their minimum distances
   */
  override fun compareTo(o: Any?): Int {
    val nd = o as BoundablePair
    if (distance < nd.distance) return -1
    if (distance > nd.distance) return 1
    return 0
  }

  /**
   * Tests if both elements of the pair are leaf nodes
   *
   * @return true if both pair elements are leaf nodes
   */
  fun isLeaves(): Boolean {
    return !(isComposite(boundable1) || isComposite(boundable2))
  }

  /**
   * For a pair which is not a leaf
   * (i.e. has at least one composite boundable)
   * computes a list of new pairs
   * from the expansion of the larger boundable
   * with distance less than minDistance
   * and adds them to a priority queue.
   *
   * @param priQ the priority queue to add the new pairs to
   * @param minDistance the limit on the distance between added pairs
   */
  fun expandToQueue(priQ: PriorityQueue<BoundablePair>, minDistance: Double) {
    val isComp1 = isComposite(boundable1)
    val isComp2 = isComposite(boundable2)

    /*
     * HEURISTIC: If both boundable are composite,
     * choose the one with largest area to expand.
     * Otherwise, simply expand whichever is composite.
     */
    if (isComp1 && isComp2) {
      if (area(boundable1) > area(boundable2)) {
        expand(boundable1, boundable2, false, priQ, minDistance)
        return
      } else {
        expand(boundable2, boundable1, true, priQ, minDistance)
        return
      }
    } else if (isComp1) {
      expand(boundable1, boundable2, false, priQ, minDistance)
      return
    } else if (isComp2) {
      expand(boundable2, boundable1, true, priQ, minDistance)
      return
    }

    throw IllegalArgumentException("neither boundable is composite")
  }

  private fun expand(
    bndComposite: Boundable,
    bndOther: Boundable,
    isFlipped: Boolean,
    priQ: PriorityQueue<BoundablePair>,
    minDistance: Double
  ) {
    val children = (bndComposite as AbstractNode).getChildBoundables()
    val i = children.iterator()
    while (i.hasNext()) {
      val child = i.next()
      val bp: BoundablePair = if (isFlipped) {
        BoundablePair(bndOther, child, itemDistance)
      } else {
        BoundablePair(child, bndOther, itemDistance)
      }
      // only add to queue if this pair might contain the closest points
      if (bp.getDistance() < minDistance) {
        priQ.add(bp)
      }
    }
  }

  companion object {
    @JvmStatic
    fun isComposite(item: Any?): Boolean {
      return (item is AbstractNode)
    }

    private fun area(b: Boundable): Double {
      return (b.getBounds() as Envelope).getArea()
    }
  }
}
