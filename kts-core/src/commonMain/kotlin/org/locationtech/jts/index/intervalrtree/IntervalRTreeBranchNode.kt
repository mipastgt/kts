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
import kotlin.math.max
import kotlin.math.min

import org.locationtech.jts.index.ItemVisitor

class IntervalRTreeBranchNode(n1: IntervalRTreeNode, n2: IntervalRTreeNode) : IntervalRTreeNode() {
  private val node1: IntervalRTreeNode = n1
  private val node2: IntervalRTreeNode = n2

  init {
    buildExtent(node1, node2)
  }

  private fun buildExtent(n1: IntervalRTreeNode, n2: IntervalRTreeNode) {
    min = min(n1.min, n2.min)
    max = max(n1.max, n2.max)
  }

  override fun query(queryMin: Double, queryMax: Double, visitor: ItemVisitor) {
    if (!intersects(queryMin, queryMax)) {
      return
    }
    node1.query(queryMin, queryMax, visitor)
    node2.query(queryMin, queryMax, visitor)
  }
}
