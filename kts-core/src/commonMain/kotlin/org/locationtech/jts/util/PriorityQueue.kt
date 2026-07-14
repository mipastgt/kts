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
package org.locationtech.jts.util

/**
 * A priority queue over a set of [Comparable] objects.
 *
 * @author Martin Davis
 */
class PriorityQueue<E> {
  private var size: Int // Number of elements in queue
  private var items: ArrayList<Any?> // The queue binary heap array

  /**
   * Creates a new empty priority queue
   */
  init {
    size = 0
    items = ArrayList()
    // create space for sentinel
    items.add(null)
  }

  /**
   * Insert into the priority queue.
   * Duplicates are allowed.
   * @param x the item to insert.
   */
  fun add(x: E) {
    @Suppress("UNCHECKED_CAST")
    val xc = x as Comparable<Any?>
    // increase the size of the items heap to create a hole for the new item
    items.add(null)

    // Insert item at end of heap and then re-establish ordering
    size += 1
    var hole = size
    // set the item as a sentinel at the base of the heap
    items.set(0, x)

    // move the item up from the hole position to its correct place
    while (xc.compareTo(items.get(hole / 2)) < 0) {
      items.set(hole, items.get(hole / 2))
      hole /= 2
    }
    // insert the new item in the correct place
    items.set(hole, x)
  }

  /**
   * Establish heap from an arbitrary arrangement of items.
   */
  /*
   private void buildHeap( ) {
   for( int i = currentSize / 2; i > 0; i-- )
   reorder( i );
   }
   */

  /**
   * Test if the priority queue is logically empty.
   * @return true if empty, false otherwise.
   */
  fun isEmpty(): Boolean {
    return size == 0
  }

  /**
   * Returns size.
   * @return current size.
   */
  fun size(): Int {
    return size
  }

  /**
   * Make the priority queue logically empty.
   */
  fun clear() {
    size = 0
    items.clear()
  }

  /**
   * Remove the smallest item from the priority queue.
   * @return the smallest item, or null if empty
   */
  fun poll(): E? {
    if (isEmpty())
      return null
    @Suppress("UNCHECKED_CAST")
    val minItem = items.get(1) as E?
    items.set(1, items.get(size))
    size -= 1
    reorder(1)

    return minItem
  }

  fun peek(): E? {
    if (isEmpty())
      return null
    @Suppress("UNCHECKED_CAST")
    val minItem = items.get(1) as E?
    return minItem
  }

  /**
   * Internal method to percolate down in the heap.
   *
   * @param hole the index at which the percolate begins.
   */
  private fun reorder(hole: Int) {
    var hole = hole
    var child: Int
    val tmp = items.get(hole)

    while (hole * 2 <= size) {
      child = hole * 2
      if (child != size
        && (items.get(child + 1) as Comparable<Any?>).compareTo(items.get(child)) < 0)
        child++
      if ((items.get(child) as Comparable<Any?>).compareTo(tmp) < 0)
        items.set(hole, items.get(child))
      else
        break
      hole = child
    }
    items.set(hole, tmp)
  }
}
