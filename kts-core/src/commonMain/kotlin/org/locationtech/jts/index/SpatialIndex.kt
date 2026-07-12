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

import org.locationtech.jts.geom.Envelope

/**
 * The basic operations supported by classes
 * implementing spatial index algorithms.
 * <p>
 * A spatial index typically provides a primary filter for range rectangle queries.
 * A secondary filter is required to test for exact intersection.
 * The secondary filter may consist of other kinds of tests,
 * such as testing other spatial relationships.
 *
 * @version 1.7
 */
interface SpatialIndex {
  /**
   * Adds a spatial item with an extent specified by the given [Envelope] to the index
   */
  fun insert(itemEnv: Envelope, item: Any?)

  /**
   * Queries the index for all items whose extents intersect the given search [Envelope]
   * Note that some kinds of indexes may also return objects which do not in fact
   * intersect the query envelope.
   *
   * @param searchEnv the envelope to query for
   * @return a list of the items found by the query
   */
  fun query(searchEnv: Envelope?): MutableList<*>

  /**
   * Queries the index for all items whose extents intersect the given search [Envelope],
   * and applies an [ItemVisitor] to them.
   * Note that some kinds of indexes may also return objects which do not in fact
   * intersect the query envelope.
   *
   * @param searchEnv the envelope to query for
   * @param visitor a visitor object to apply to the items found
   */
  fun query(searchEnv: Envelope?, visitor: ItemVisitor)

  /**
   * Removes a single item from the tree.
   *
   * @param itemEnv the Envelope of the item to remove
   * @param item the item to remove
   * @return `true` if the item was found
   */
  fun remove(itemEnv: Envelope, item: Any?): Boolean
}
