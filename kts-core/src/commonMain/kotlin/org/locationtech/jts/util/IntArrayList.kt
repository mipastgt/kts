/*
 * Copyright (c) 2019 Martin Davis.
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
import kotlin.math.max

/**
 * An extendable array of primitive <code>int</code> values.
 *
 * @author Martin Davis
 *
 */
class IntArrayList
/**
 * Constructs an empty list with the specified initial capacity
 *
 * @param initialCapacity the initial capacity of the list
 */
(initialCapacity: Int) {
  private var data: IntArray = IntArray(initialCapacity)
  private var size = 0

  /**
   * Constructs an empty list.
   */
  constructor() : this(10)

  /**
   * Returns the number of values in this list.
   *
   * @return the number of values in the list
   */
  fun size(): Int {
    return size
  }

  /**
   * Increases the capacity of this list instance, if necessary,
   * to ensure that it can hold at least the number of elements
   * specified by the capacity argument.
   *
   * @param capacity the desired capacity
   */
  fun ensureCapacity(capacity: Int) {
    if (capacity <= data.size) return
    val newLength = max(capacity, data.size * 2)
    //System.out.println("IntArrayList: copying " + size + " ints to new array of length " + capacity);
    data = data.copyOf(newLength)
  }

  /**
   * Adds a value to the end of this list.
   *
   * @param value the value to add
   */
  fun add(value: Int) {
    ensureCapacity(size + 1)
    data[size] = value
    ++size
  }

  /**
   * Adds all values in an array to the end of this list.
   *
   * @param values an array of values
   */
  fun addAll(values: IntArray?) {
    if (values == null) return
    if (values.size == 0) return
    ensureCapacity(size + values.size)
    values.copyInto(data, destinationOffset = size, startIndex = 0, endIndex = values.size)
    size += values.size
  }

  /**
   * Returns a int array containing a copy of
   * the values in this list.
   *
   * @return an array containing the values in this list
   */
  fun toArray(): IntArray {
    val array = IntArray(size)
    data.copyInto(array, destinationOffset = 0, startIndex = 0, endIndex = size)
    return array
  }
}
