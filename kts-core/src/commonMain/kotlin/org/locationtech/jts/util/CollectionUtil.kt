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

import kotlin.jvm.JvmStatic

/**
 * Utilities for processing {@link Collection}s.
 *
 * @version 1.7
 */
class CollectionUtil {

  interface Function {
    fun execute(obj: Any?): Any?
  }

  companion object {
    /**
     * Executes a function on each item in a {@link Collection}
     * and returns the results in a new {@link List}
     *
     * @param coll the collection to process
     * @param func the Function to execute
     * @return a list of the transformed objects
     */
    @JvmStatic
    fun transform(coll: Collection<*>, func: Function): List<Any?> {
      val result = ArrayList<Any?>()
      val i = coll.iterator()
      while (i.hasNext()) {
        result.add(func.execute(i.next()))
      }
      return result
    }

    /**
     * Executes a function on each item in a Collection but does
     * not accumulate the result
     *
     * @param coll the collection to process
     * @param func the Function to execute
     */
    @JvmStatic
    fun apply(coll: Collection<*>, func: Function) {
      val i = coll.iterator()
      while (i.hasNext()) {
        func.execute(i.next())
      }
    }

    /**
     * Executes a {@link Function} on each item in a Collection
     * and collects all the entries for which the result
     * of the function is equal to {@link Boolean} <tt>true</tt>.
     *
     * @param collection the collection to process
     * @param func the Function to execute
     * @return a list of objects for which the function was true
     */
    @JvmStatic
    fun select(collection: Collection<*>, func: Function): List<Any?> {
      val result = ArrayList<Any?>()
      val i = collection.iterator()
      while (i.hasNext()) {
        val item = i.next()
        if (true == func.execute(item)) {
          result.add(item)
        }
      }
      return result
    }
  }
}
