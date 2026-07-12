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
package org.locationtech.jts.operation.valid

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.LinearRing
import org.locationtech.jts.geom.Polygon
import org.locationtech.jts.index.SpatialIndex
import org.locationtech.jts.index.strtree.STRtree

/**
 * Tests whether any holes of a Polygon are
 * nested inside another hole, using a spatial
 * index to speed up the comparisons.
 *
 * @version 1.7
 */
internal class IndexedNestedHoleTester(private val polygon: Polygon) {

    private lateinit var index: SpatialIndex
    private var nestedPt: Coordinate? = null

    init {
        loadIndex()
    }

    private fun loadIndex() {
        index = STRtree()

        for (i in 0 until polygon.getNumInteriorRing()) {
            val hole = polygon.getInteriorRingN(i)
            val env = hole.getEnvelopeInternal()
            index.insert(env, hole)
        }
    }

    /**
     * Gets a point on a nested hole, if one exists.
     *
     * @return a point on a nested hole, or null if none are nested
     */
    fun getNestedPoint(): Coordinate? {
        return nestedPt
    }

    /**
     * Tests if any hole is nested (contained) within another hole.
     * This is invalid.
     * The nested point will be set to reflect this.
     * @return true if some hole is nested
     */
    fun isNested(): Boolean {
        for (i in 0 until polygon.getNumInteriorRing()) {
            val hole = polygon.getInteriorRingN(i)

            @Suppress("UNCHECKED_CAST")
            val results = index.query(hole.getEnvelopeInternal()) as List<LinearRing>
            for (testHole in results) {
                if (hole === testHole)
                    continue

                /**
                 * Hole is not fully covered by test hole, so cannot be nested
                 */
                if (!testHole.getEnvelopeInternal().covers(hole.getEnvelopeInternal()))
                    continue

                if (PolygonTopologyAnalyzer.isRingNested(hole, testHole)) {
                    //TODO: find a hole point known to be inside
                    nestedPt = hole.getCoordinateN(0)
                    return true
                }
            }
        }
        return false
    }
}
