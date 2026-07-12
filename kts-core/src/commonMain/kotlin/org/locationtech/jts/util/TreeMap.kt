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
 * A minimal sorted map ordered by the natural ordering of its keys (the keys
 * must implement [Comparable]). It replaces `java.util.TreeMap` for the multiplatform
 * build, which has no `kotlin` common-stdlib equivalent.
 *
 * Only the subset of behaviour used within JTS is provided. In particular, as with
 * `java.util.TreeMap`, key identity is determined by the natural ordering
 * (`compareTo == 0`), **not** by `equals`/`hashCode`; and iteration of `keys`,
 * `values` and `entries` is in ascending key order.
 *
 * The keys/values/entries views are ordered snapshots; structural modification must be
 * done through the map itself.
 */
class TreeMap<K, V> : MutableMap<K, V> {

  private val entryList = ArrayList<MutableMap.MutableEntry<K, V>>()

  @Suppress("UNCHECKED_CAST")
  private fun compareKeys(a: K, b: K): Int {
    return (a as Comparable<Any?>).compareTo(b)
  }

  /**
   * Binary search for the given key.
   * @return the index of the key if present, otherwise `-(insertionPoint) - 1`
   */
  private fun indexOfKey(key: K): Int {
    var lo = 0
    var hi = entryList.size - 1
    while (lo <= hi) {
      val mid = (lo + hi) ushr 1
      val c = compareKeys(entryList[mid].key, key)
      when {
        c < 0 -> lo = mid + 1
        c > 0 -> hi = mid - 1
        else -> return mid
      }
    }
    return -(lo + 1)
  }

  override val size: Int
    get() = entryList.size

  override fun isEmpty(): Boolean {
    return entryList.isEmpty()
  }

  override fun containsKey(key: K): Boolean {
    return indexOfKey(key) >= 0
  }

  override fun containsValue(value: V): Boolean {
    return entryList.any { it.value == value }
  }

  override fun get(key: K): V? {
    val i = indexOfKey(key)
    return if (i >= 0) entryList[i].value else null
  }

  override fun put(key: K, value: V): V? {
    val i = indexOfKey(key)
    if (i >= 0) {
      return entryList[i].setValue(value)
    }
    entryList.add(-(i + 1), Entry(key, value))
    return null
  }

  override fun remove(key: K): V? {
    val i = indexOfKey(key)
    if (i < 0) return null
    return entryList.removeAt(i).value
  }

  override fun putAll(from: Map<out K, V>) {
    for (entry in from) {
      put(entry.key, entry.value)
    }
  }

  override fun clear() {
    entryList.clear()
  }

  override val keys: MutableSet<K>
    get() = entryList.mapTo(LinkedHashSet(entryList.size)) { it.key }

  override val values: MutableCollection<V>
    get() = entryList.mapTo(ArrayList(entryList.size)) { it.value }

  override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
    get() = LinkedHashSet(entryList)

  private class Entry<K, V>(
    override val key: K,
    private var currentValue: V
  ) : MutableMap.MutableEntry<K, V> {
    override val value: V
      get() = currentValue

    override fun setValue(newValue: V): V {
      val old = currentValue
      currentValue = newValue
      return old
    }
  }
}
