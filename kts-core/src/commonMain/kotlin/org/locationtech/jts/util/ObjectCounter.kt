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
 * Counts occurrences of objects.
 *
 * @author Martin Davis
 *
 */
class ObjectCounter {

  private val counts: MutableMap<Any?, Counter> = HashMap()

  fun add(o: Any?) {
    val counter = counts.get(o)
    if (counter == null)
      counts.put(o, Counter(1))
    else
      counter.increment()
  }

  // TODO: add remove(Object o)

  fun count(o: Any?): Int {
    val counter = counts.get(o)
    if (counter == null)
      return 0
    else
      return counter.count()
  }

  private class Counter {
    var count = 0

    constructor()

    constructor(count: Int) {
      this.count = count
    }

    fun count(): Int {
      return count
    }

    fun increment() {
      count++
    }
  }
}
