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

package org.locationtech.jts.triangulate

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.LineSegment

/**
 * Models a constraint segment which can be split in two in various ways,
 * according to certain geometric constraints.
 *
 * @author Martin Davis
 */
class SplitSegment(private val seg: LineSegment) {
    private val segLen: Double = seg.getLength()
    private var splitPt: Coordinate? = null
    private var minimumLen = 0.0

    fun setMinimumLength(minLen: Double) {
        minimumLen = minLen
    }

    fun getSplitPoint(): Coordinate? {
        return splitPt
    }

    fun splitAt(length: Double, endPt: Coordinate) {
        val actualLen = getConstrainedLength(length)
        val frac = actualLen / segLen
        splitPt = if (endPt.equals2D(seg.p0))
            seg.pointAlong(frac)
        else
            pointAlongReverse(seg, frac)
    }

    fun splitAt(pt: Coordinate) {
        // check that given pt doesn't violate min length
        val minFrac = minimumLen / segLen
        if (pt.distance(seg.p0) < minimumLen) {
            splitPt = seg.pointAlong(minFrac)
            return
        }
        if (pt.distance(seg.p1) < minimumLen) {
            splitPt = pointAlongReverse(seg, minFrac)
            return
        }
        // passes minimum distance check - use provided point as split pt
        splitPt = pt
    }

    private fun getConstrainedLength(len: Double): Double {
        if (len < minimumLen)
            return minimumLen
        return len
    }

    companion object {
        /**
         * Computes the [Coordinate] that lies a given fraction along the line defined by the
         * reverse of the given segment. A fraction of `0.0` returns the end point of the
         * segment; a fraction of `1.0` returns the start point of the segment.
         *
         * @param seg the LineSegment
         * @param segmentLengthFraction the fraction of the segment length along the line
         * @return the point at that distance
         */
        private fun pointAlongReverse(seg: LineSegment, segmentLengthFraction: Double): Coordinate {
            val coord = Coordinate()
            coord.x = seg.p1.x - segmentLengthFraction * (seg.p1.x - seg.p0.x)
            coord.y = seg.p1.y - segmentLengthFraction * (seg.p1.y - seg.p0.y)
            return coord
        }
    }
}
