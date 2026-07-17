/*
 * Copyright (c) 2023 Martin Davis.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.locationtech.jts.operation.relateng

import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.index.chain.MonotoneChain
import org.locationtech.jts.index.chain.MonotoneChainBuilder
import org.locationtech.jts.index.chain.MonotoneChainOverlapAction
import org.locationtech.jts.index.hprtree.HPRtree
import org.locationtech.jts.noding.SegmentString

internal class EdgeSetIntersector(
    edgesA: List<RelateSegmentString>,
    edgesB: List<RelateSegmentString>,
    env: Envelope?
) {

    private val index = HPRtree()
    private val envelope: Envelope? = env
    private val monoChains = ArrayList<MonotoneChain>()
    private var idCounter = 0

    init {
        addEdges(edgesA)
        addEdges(edgesB)
        // build index to ensure thread-safety
        index.build()
    }

    private fun addEdges(segStrings: Collection<RelateSegmentString>) {
        for (ss in segStrings) {
            addToIndex(ss)
        }
    }

    private fun addToIndex(segStr: SegmentString) {
        val segChains = MonotoneChainBuilder.getChains(segStr.getCoordinates(), segStr)
        for (mc in segChains) {
            if (envelope == null || envelope.intersects(mc.getEnvelope())) {
                mc.setId(idCounter++)
                index.insert(mc.getEnvelope(), mc)
                monoChains.add(mc)
            }
        }
    }

    fun process(intersector: EdgeSegmentIntersector) {
        val overlapAction: MonotoneChainOverlapAction = EdgeSegmentOverlapAction(intersector)

        for (queryChain in monoChains) {
            @Suppress("UNCHECKED_CAST")
            val overlapChains = index.query(queryChain.getEnvelope()) as List<MonotoneChain>
            for (testChain in overlapChains) {
                /*
                 * following test makes sure we only compare each pair of chains once
                 * and that we don't compare a chain to itself
                 */
                if (testChain.getId() <= queryChain.getId())
                    continue

                testChain.computeOverlaps(queryChain, overlapAction)
                if (intersector.isDone())
                    return
            }
        }
    }
}
