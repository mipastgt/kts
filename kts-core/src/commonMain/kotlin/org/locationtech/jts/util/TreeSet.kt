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
 * A minimal sorted set ordered by the natural ordering of its elements (the elements
 * must implement [Comparable]). It replaces `java.util.TreeSet` for the multiplatform
 * build, which has no `kotlin` common-stdlib equivalent.
 *
 * Only the subset of behaviour used within JTS is provided. As with `java.util.TreeSet`,
 * element identity is determined by the natural ordering (`compareTo == 0`), **not** by
 * `equals`/`hashCode`, and iteration is in ascending order. The navigable [higher] and
 * [lower] queries are supported.
 */
class TreeSet<E>() : MutableSet<E> {

  private val elements = ArrayList<E>()

  constructor(c: Collection<E>) : this() {
    addAll(c)
  }

  @Suppress("UNCHECKED_CAST")
  private fun compareElements(a: E, b: E): Int {
    return (a as Comparable<Any?>).compareTo(b)
  }

  /**
   * Binary search for the given element.
   * @return the index of the element if present, otherwise `-(insertionPoint) - 1`
   */
  private fun indexOf(element: E): Int {
    var lo = 0
    var hi = elements.size - 1
    while (lo <= hi) {
      val mid = (lo + hi) ushr 1
      val c = compareElements(elements[mid], element)
      when {
        c < 0 -> lo = mid + 1
        c > 0 -> hi = mid - 1
        else -> return mid
      }
    }
    return -(lo + 1)
  }

  override val size: Int
    get() = elements.size

  override fun isEmpty(): Boolean {
    return elements.isEmpty()
  }

  override fun contains(element: E): Boolean {
    return indexOf(element) >= 0
  }

  override fun containsAll(elements: Collection<E>): Boolean {
    return elements.all { contains(it) }
  }

  override fun iterator(): MutableIterator<E> {
    return elements.iterator()
  }

  override fun add(element: E): Boolean {
    val i = indexOf(element)
    if (i >= 0) return false
    elements.add(-(i + 1), element)
    return true
  }

  override fun addAll(elements: Collection<E>): Boolean {
    var changed = false
    for (e in elements) {
      if (add(e)) changed = true
    }
    return changed
  }

  override fun remove(element: E): Boolean {
    val i = indexOf(element)
    if (i < 0) return false
    elements.removeAt(i)
    return true
  }

  override fun removeAll(elements: Collection<E>): Boolean {
    var changed = false
    for (e in elements) {
      if (remove(e)) changed = true
    }
    return changed
  }

  override fun retainAll(elements: Collection<E>): Boolean {
    val keep = TreeSet<E>()
    keep.addAll(elements)
    var changed = false
    val it = this.elements.iterator()
    while (it.hasNext()) {
      if (!keep.contains(it.next())) {
        it.remove()
        changed = true
      }
    }
    return changed
  }

  override fun clear() {
    elements.clear()
  }

  /**
   * Returns the least element strictly greater than the given element,
   * or `null` if there is no such element.
   */
  fun higher(element: E): E? {
    val i = indexOf(element)
    val next = if (i >= 0) i + 1 else -(i + 1)
    return if (next < elements.size) elements[next] else null
  }

  /**
   * Returns the greatest element strictly less than the given element,
   * or `null` if there is no such element.
   */
  fun lower(element: E): E? {
    val i = indexOf(element)
    val prev = if (i >= 0) i - 1 else -(i + 1) - 1
    return if (prev >= 0) elements[prev] else null
  }
}
