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
package org.locationtech.jts.index.intervalrtree

import org.locationtech.jts.index.ItemVisitor

class IntervalRTreeLeafNode(min: Double, max: Double, private val item: Any?) : IntervalRTreeNode() {

  init {
    this.min = min
    this.max = max
  }

  override fun query(queryMin: Double, queryMax: Double, visitor: ItemVisitor) {
    if (!intersects(queryMin, queryMax))
      return

    visitor.visitItem(item)
  }
}
