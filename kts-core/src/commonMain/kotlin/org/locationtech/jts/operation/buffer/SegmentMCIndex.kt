/*
 * Copyright (c) 2021 Martin Davis.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.locationtech.jts.operation.buffer

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.index.ItemVisitor
import org.locationtech.jts.index.chain.MonotoneChain
import org.locationtech.jts.index.chain.MonotoneChainBuilder
import org.locationtech.jts.index.chain.MonotoneChainSelectAction
import org.locationtech.jts.index.strtree.STRtree

/**
 * A spatial index over a segment sequence
 * using [MonotoneChain]s.
 *
 * @author mdavis
 *
 */
internal class SegmentMCIndex(segs: Array<Coordinate>) {
  private val index: STRtree = buildIndex(segs)

  private fun buildIndex(segs: Array<Coordinate>): STRtree {
    val index = STRtree()
    val segChains: List<MonotoneChain> = MonotoneChainBuilder.getChains(segs, segs)
    for (mc in segChains) {
      index.insert(mc.getEnvelope(), mc)
    }
    return index
  }

  fun query(env: Envelope, action: MonotoneChainSelectAction) {
    index.query(env, object : ItemVisitor {
      override fun visitItem(item: Any?) {
        val testChain = item as MonotoneChain
        testChain.select(env, action)
      }
    })
  }
}
