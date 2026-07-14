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

package org.locationtech.jts.index

/**
 * Builds an array of all visited items.
 *
 */
class ArrayListVisitor : ItemVisitor {

  private val items = ArrayList<Any?>()

  /**
   * Visits an item.
   *
   * @param item the item to visit
   */
  override fun visitItem(item: Any?) {
    items.add(item)
  }

  /**
   * Gets the array of visited items.
   *
   * @return the array of items
   */
  fun getItems(): ArrayList<Any?> = items
}
