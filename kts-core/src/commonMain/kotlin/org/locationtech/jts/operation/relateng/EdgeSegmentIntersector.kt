/*
 * Copyright (c) 2022 Martin Davis.
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

import org.locationtech.jts.algorithm.RobustLineIntersector
import org.locationtech.jts.noding.SegmentIntersector
import org.locationtech.jts.noding.SegmentString

/**
 * Tests segments of [RelateSegmentString]s
 * and if they intersect adds the intersection(s)
 * to the [TopologyComputer].
 *
 * @author Martin Davis
 *
 */
internal class EdgeSegmentIntersector(topoBuilder: TopologyComputer) : SegmentIntersector {

    private val li = RobustLineIntersector()
    private val topoComputer: TopologyComputer = topoBuilder

    override fun isDone(): Boolean {
        return topoComputer.isResultKnown()
    }

    override fun processIntersections(ss0: SegmentString, segIndex0: Int, ss1: SegmentString, segIndex1: Int) {
        // don't intersect a segment with itself
        if (ss0 === ss1 && segIndex0 == segIndex1) return

        val rss0 = ss0 as RelateSegmentString
        val rss1 = ss1 as RelateSegmentString
        //TODO: move this ordering logic to TopologyBuilder
        if (rss0.isA()) {
            addIntersections(rss0, segIndex0, rss1, segIndex1)
        } else {
            addIntersections(rss1, segIndex1, rss0, segIndex0)
        }
    }

    private fun addIntersections(ssA: RelateSegmentString, segIndexA: Int, ssB: RelateSegmentString, segIndexB: Int) {

        val a0 = ssA.getCoordinate(segIndexA)
        val a1 = ssA.getCoordinate(segIndexA + 1)
        val b0 = ssB.getCoordinate(segIndexB)
        val b1 = ssB.getCoordinate(segIndexB + 1)

        li.computeIntersection(a0, a1, b0, b1)

        if (!li.hasIntersection())
            return

        for (i in 0 until li.getIntersectionNum()) {
            val intPt = li.getIntersection(i)
            /**
             * Ensure endpoint intersections are added once only, for their canonical segments.
             */
            if (li.isProper() ||
                (
                    ssA.isContainingSegment(segIndexA, intPt) &&
                        ssB.isContainingSegment(segIndexB, intPt)
                    )
            ) {
                val nsa = ssA.createNodeSection(segIndexA, intPt)
                val nsb = ssB.createNodeSection(segIndexB, intPt)
                topoComputer.addIntersection(nsa, nsb)
            }
        }
    }
}
