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
package org.locationtech.jts.geom

/**
 *  Iterates over all [Geometry]s in a [Geometry],
 *  (which may be either a collection or an atomic geometry).
 *  The iteration sequence follows a pre-order, depth-first traversal of the
 *  structure of the `GeometryCollection`
 *  (which may be nested). The original `Geometry` object is
 *  returned as well (as the first object), as are all sub-collections and atomic elements.
 *  It is  simple to ignore the intermediate `GeometryCollection` objects if they are not
 *  needed.
 *
 */
class GeometryCollectionIterator
/**
 *  Constructs an iterator over the given `Geometry`.
 *
 * @param  parent  the geometry over which to iterate; also, the first
 *      element returned by the iterator.
 */
(
  /**
   *  The `Geometry` being iterated over.
   */
  private val parent: Geometry
) : MutableIterator<Any?> {

  /**
   *  Indicates whether or not the first element
   *  (the root `GeometryCollection`) has been returned.
   */
  private var atStart = true

  /**
   *  The number of `Geometry`s in the the `GeometryCollection`.
   */
  private var max = parent.getNumGeometries()

  /**
   *  The index of the `Geometry` that will be returned when `next`
   *  is called.
   */
  private var index = 0

  /**
   *  The iterator over a nested `Geometry`, or `null`
   *  if this `GeometryCollectionIterator` is not currently iterating
   *  over a nested `GeometryCollection`.
   */
  private var subcollectionIterator: GeometryCollectionIterator? = null

  /**
   * Tests whether any geometry elements remain to be returned.
   *
   * @return true if more geometry elements remain
   */
  override fun hasNext(): Boolean {
    if (atStart) {
      return true
    }
    val subcollectionIterator = subcollectionIterator
    if (subcollectionIterator != null) {
      if (subcollectionIterator.hasNext()) {
        return true
      }
      this.subcollectionIterator = null
    }
    if (index >= max) {
      return false
    }
    return true
  }

  /**
   * Gets the next geometry in the iteration sequence.
   *
   * @return the next geometry in the iteration
   */
  override fun next(): Any? {
    // the parent GeometryCollection is the first object returned
    if (atStart) {
      atStart = false
      if (isAtomic(parent))
        index++
      return parent
    }
    val subcollectionIterator = subcollectionIterator
    if (subcollectionIterator != null) {
      if (subcollectionIterator.hasNext()) {
        return subcollectionIterator.next()
      } else {
        this.subcollectionIterator = null
      }
    }
    if (index >= max) {
      throw NoSuchElementException()
    }
    val obj = parent.getGeometryN(index++)
    if (obj is GeometryCollection) {
      val sub = GeometryCollectionIterator(obj)
      this.subcollectionIterator = sub
      // there will always be at least one element in the sub-collection
      return sub.next()
    }
    return obj
  }

  /**
   * Removal is not supported.
   *
   * @throws  UnsupportedOperationException  This method is not implemented.
   */
  override fun remove() {
    throw UnsupportedOperationException(this::class.simpleName)
  }

  companion object {
    private fun isAtomic(geom: Geometry): Boolean {
      return geom !is GeometryCollection
    }
  }
}
