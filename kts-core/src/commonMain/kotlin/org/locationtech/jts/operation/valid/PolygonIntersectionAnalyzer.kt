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
import kotlin.math.abs

import org.locationtech.jts.algorithm.LineIntersector
import org.locationtech.jts.algorithm.PolygonNodeTopology
import org.locationtech.jts.algorithm.RobustLineIntersector
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.noding.SegmentIntersector
import org.locationtech.jts.noding.SegmentString

/**
 * Finds and analyzes intersections in and between polygons,
 * to determine if they are valid.
 *
 * @author mdavis
 */
internal class PolygonIntersectionAnalyzer(private val isInvertedRingValid: Boolean) : SegmentIntersector {

    private val li: LineIntersector = RobustLineIntersector()
    private var invalidCode = NO_INVALID_INTERSECTION
    private var invalidLocation: Coordinate? = null

    private var hasDoubleTouchFlag = false
    private var doubleTouchLocation: Coordinate? = null

    override fun isDone(): Boolean {
        return isInvalid() || hasDoubleTouchFlag
    }

    fun isInvalid(): Boolean {
        return invalidCode >= 0
    }

    fun getInvalidCode(): Int {
        return invalidCode
    }

    fun getInvalidLocation(): Coordinate? {
        return invalidLocation
    }

    fun hasDoubleTouch(): Boolean {
        return hasDoubleTouchFlag
    }

    fun getDoubleTouchLocation(): Coordinate? {
        return doubleTouchLocation
    }

    override fun processIntersections(ss0: SegmentString, segIndex0: Int, ss1: SegmentString, segIndex1: Int) {
        // don't test a segment with itself
        val isSameSegString = ss0 === ss1
        val isSameSegment = isSameSegString && segIndex0 == segIndex1
        if (isSameSegment) return

        val code = findInvalidIntersection(ss0, segIndex0, ss1, segIndex1)
        /*
         * Ensure that invalidCode is only set once,
         * since the short-circuiting in [SegmentIntersector] is not guaranteed
         * to happen immediately.
         */
        if (code != NO_INVALID_INTERSECTION) {
            invalidCode = code
            invalidLocation = li.getIntersection(0)
        }
    }

    private fun findInvalidIntersection(
        ss0: SegmentString,
        segIndex0: Int,
        ss1: SegmentString,
        segIndex1: Int
    ): Int {
        val p00 = ss0.getCoordinate(segIndex0)
        val p01 = ss0.getCoordinate(segIndex0 + 1)
        val p10 = ss1.getCoordinate(segIndex1)
        val p11 = ss1.getCoordinate(segIndex1 + 1)

        li.computeIntersection(p00, p01, p10, p11)

        if (!li.hasIntersection()) {
            return NO_INVALID_INTERSECTION
        }

        val isSameSegString = ss0 === ss1

        /*
         * Check for an intersection in the interior of both segments.
         * Collinear intersections by definition contain an interior intersection.
         */
        if (li.isProper() || li.getIntersectionNum() >= 2) {
            return TopologyValidationError.SELF_INTERSECTION
        }

        /**
         * Now know there is exactly one intersection,
         * at a vertex of at least one segment.
         */
        val intPt = li.getIntersection(0)

        /**
         * If segments are adjacent the intersection must be their common endpoint.
         * (since they are not collinear).
         * This is valid.
         */
        val isAdjacentSegments = isSameSegString && isAdjacentInRing(ss0, segIndex0, segIndex1)
        // Assert: intersection is an endpoint of both segs
        if (isAdjacentSegments) return NO_INVALID_INTERSECTION

        /*
         * Under OGC semantics, rings cannot self-intersect.
         * So the intersection is invalid.
         *
         * The return of RING_SELF_INTERSECTION is to match the previous IsValid semantics.
         */
        if (isSameSegString && !isInvertedRingValid) {
            return TopologyValidationError.RING_SELF_INTERSECTION
        }

        /*
         * Optimization: don't analyze intPts at the endpoint of a segment.
         */
        if (intPt.equals2D(p01) || intPt.equals2D(p11))
            return NO_INVALID_INTERSECTION

        /**
         * Check topology of a vertex intersection.
         * The ring(s) must not cross.
         */
        var e00 = p00
        var e01 = p01
        if (intPt.equals2D(p00)) {
            e00 = prevCoordinateInRing(ss0, segIndex0)
            e01 = p01
        }
        var e10 = p10
        var e11 = p11
        if (intPt.equals2D(p10)) {
            e10 = prevCoordinateInRing(ss1, segIndex1)
            e11 = p11
        }
        val hasCrossing = PolygonNodeTopology.isCrossing(intPt, e00, e01, e10, e11)
        if (hasCrossing) {
            return TopologyValidationError.SELF_INTERSECTION
        }

        /*
         * If allowing inverted rings, record a self-touch to support later checking
         * that it does not disconnect the interior.
         */
        if (isSameSegString && isInvertedRingValid) {
            addSelfTouch(ss0, intPt, e00, e01, e10, e11)
        }

        /**
         * If the rings are in the same polygon
         * then record the touch to support connected interior checking.
         */
        val isDoubleTouch = addDoubleTouch(ss0, ss1, intPt)
        if (isDoubleTouch && !isSameSegString) {
            hasDoubleTouchFlag = true
            doubleTouchLocation = intPt
        }

        return NO_INVALID_INTERSECTION
    }

    private fun addDoubleTouch(ss0: SegmentString, ss1: SegmentString, intPt: Coordinate): Boolean {
        return PolygonRing.addTouch(ss0.getData() as PolygonRing?, ss1.getData() as PolygonRing?, intPt)
    }

    private fun addSelfTouch(
        ss: SegmentString,
        intPt: Coordinate,
        e00: Coordinate,
        e01: Coordinate,
        e10: Coordinate,
        e11: Coordinate
    ) {
        val polyRing = ss.getData() as PolygonRing?
            ?: throw IllegalStateException("SegmentString missing PolygonRing data when checking self-touches")
        polyRing.addSelfTouch(intPt, e00, e01, e10, e11)
    }

    companion object {
        private const val NO_INVALID_INTERSECTION = -1

        /**
         * For a segment string for a ring, gets the coordinate
         * previous to the given index (wrapping if the index is 0)
         *
         * @param ringSS the ring segment string
         * @param segIndex the segment index
         * @return the coordinate previous to the given segment
         */
        private fun prevCoordinateInRing(ringSS: SegmentString, segIndex: Int): Coordinate {
            var prevIndex = segIndex - 1
            if (prevIndex < 0) {
                prevIndex = ringSS.size() - 2
            }
            return ringSS.getCoordinate(prevIndex)
        }

        /**
         * Tests if two segments in a closed [SegmentString] are adjacent.
         * This handles determining adjacency across the start/end of the ring.
         *
         * @param ringSS the segment string
         * @param segIndex0 a segment index
         * @param segIndex1 a segment index
         * @return true if the segments are adjacent
         */
        private fun isAdjacentInRing(ringSS: SegmentString, segIndex0: Int, segIndex1: Int): Boolean {
            val delta = abs(segIndex1 - segIndex0)
            if (delta <= 1) return true
            /*
             * A string with N vertices has maximum segment index of N-2.
             * If the delta is at least N-2, the segments must be
             * at the start and end of the string and thus adjacent.
             */
            if (delta >= ringSS.size() - 2) return true
            return false
        }
    }
}
