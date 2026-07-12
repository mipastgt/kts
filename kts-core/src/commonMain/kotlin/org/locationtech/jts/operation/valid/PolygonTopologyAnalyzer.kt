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

import org.locationtech.jts.algorithm.Orientation
import org.locationtech.jts.algorithm.PointLocation
import org.locationtech.jts.algorithm.PolygonNodeTopology
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.CoordinateArrays
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.LinearRing
import org.locationtech.jts.geom.Location
import org.locationtech.jts.geom.Polygon
import org.locationtech.jts.noding.BasicSegmentString
import org.locationtech.jts.noding.MCIndexNoder
import org.locationtech.jts.noding.SegmentString

/**
 * Analyzes the topology of polygonal geometry
 * to determine whether it is valid.
 *
 * @author mdavis
 *
 */
internal class PolygonTopologyAnalyzer(geom: Geometry, private val isInvertedRingValid: Boolean) {

    private var intFinder: PolygonIntersectionAnalyzer? = null
    private var polyRings: MutableList<PolygonRing>? = null
    private var disconnectionPt: Coordinate? = null

    init {
        analyze(geom)
    }

    fun hasInvalidIntersection(): Boolean {
        return intFinder!!.isInvalid()
    }

    fun getInvalidCode(): Int {
        return intFinder!!.getInvalidCode()
    }

    fun getInvalidLocation(): Coordinate? {
        return intFinder!!.getInvalidLocation()
    }

    /**
     * Tests whether the interior of the polygonal geometry is
     * disconnected.
     *
     * @return true if the interior is disconnected
     */
    fun isInteriorDisconnected(): Boolean {
        /**
         * May already be set by a double-touching hole
         */
        if (disconnectionPt != null) {
            return true
        }
        if (isInvertedRingValid) {
            checkInteriorDisconnectedBySelfTouch()
            if (disconnectionPt != null) {
                return true
            }
        }
        checkInteriorDisconnectedByHoleCycle()
        if (disconnectionPt != null) {
            return true
        }
        return false
    }

    /**
     * Gets a location where the polyonal interior is disconnected.
     * [isInteriorDisconnected] must be called first.
     *
     * @return the location of an interior disconnection, or null
     */
    fun getDisconnectionLocation(): Coordinate? {
        return disconnectionPt
    }

    /**
     * Tests whether any polygon with holes has a disconnected interior
     * by virtue of the holes (and possibly shell) forming a hole cycle.
     */
    fun checkInteriorDisconnectedByHoleCycle() {
        /**
         * PolyRings will be null for empty, no hole or LinearRing inputs
         */
        if (polyRings != null) {
            disconnectionPt = PolygonRing.findHoleCycleLocation(polyRings!!)
        }
    }

    /**
     * Tests if an area interior is disconnected by a self-touching ring.
     */
    fun checkInteriorDisconnectedBySelfTouch() {
        if (polyRings != null) {
            disconnectionPt = PolygonRing.findInteriorSelfNode(polyRings!!)
        }
    }

    private fun analyze(geom: Geometry) {
        if (geom.isEmpty())
            return
        val segStrings = createSegmentStrings(geom, isInvertedRingValid)
        polyRings = getPolygonRings(segStrings)
        val intFinderLocal = analyzeIntersections(segStrings)
        intFinder = intFinderLocal

        if (intFinderLocal.hasDoubleTouch()) {
            disconnectionPt = intFinderLocal.getDoubleTouchLocation()
            return
        }
    }

    private fun analyzeIntersections(segStrings: MutableList<SegmentString>): PolygonIntersectionAnalyzer {
        val segInt = PolygonIntersectionAnalyzer(isInvertedRingValid)
        val noder = MCIndexNoder()
        noder.setSegmentIntersector(segInt)
        noder.computeNodes(segStrings)
        return segInt
    }

    companion object {
        /**
         * Tests whether a ring is nested inside another ring.
         *
         * @param test the ring to test
         * @param target the ring to test against
         * @return true if the test ring lies inside the target ring
         */
        fun isRingNested(test: LinearRing, target: LinearRing): Boolean {
            val p0 = test.getCoordinateN(0)
            val targetPts = target.getCoordinates()
            val loc = PointLocation.locateInRing(p0, targetPts)
            if (loc == Location.EXTERIOR) return false
            if (loc == Location.INTERIOR) return true

            /**
             * The start point is on the boundary of the ring.
             * Use the topology at the node to check if the segment
             * is inside or outside the ring.
             */
            val p1 = findNonEqualVertex(test, p0)
            return isIncidentSegmentInRing(p0, p1, targetPts)
        }

        private fun findNonEqualVertex(ring: LinearRing, p: Coordinate): Coordinate {
            var i = 1
            var next = ring.getCoordinateN(i)
            while (next.equals2D(p) && i < ring.getNumPoints() - 1) {
                i += 1
                next = ring.getCoordinateN(i)
            }
            return next
        }

        /**
         * Tests whether a touching segment is interior to a ring.
         *
         * @param p0 the touching vertex of the segment
         * @param p1 the second vertex of the segment
         * @param ringPts the points of the ring
         * @return true if the segment is inside the ring.
         */
        private fun isIncidentSegmentInRing(p0: Coordinate, p1: Coordinate, ringPts: Array<Coordinate>): Boolean {
            val index = intersectingSegIndex(ringPts, p0)
            if (index < 0) {
                throw IllegalArgumentException("Segment vertex does not intersect ring")
            }
            var rPrev = findRingVertexPrev(ringPts, index, p0)
            var rNext = findRingVertexNext(ringPts, index, p0)
            /**
             * If ring orientation is not normalized, flip the corner orientation
             */
            val isInteriorOnRight = !Orientation.isCCW(ringPts)
            if (!isInteriorOnRight) {
                val temp = rPrev
                rPrev = rNext
                rNext = temp
            }
            return PolygonNodeTopology.isInteriorSegment(p0, rPrev, rNext, p1)
        }

        /**
         * Finds the ring vertex previous to a node point on a ring.
         * Repeated points are skipped over.
         */
        private fun findRingVertexPrev(ringPts: Array<Coordinate>, index: Int, node: Coordinate): Coordinate {
            var iPrev = index
            var prev = ringPts[iPrev]
            while (node.equals2D(prev)) {
                iPrev = ringIndexPrev(ringPts, iPrev)
                prev = ringPts[iPrev]
            }
            return prev
        }

        /**
         * Finds the ring vertex next from a node point on a ring.
         * Repeated points are skipped over.
         */
        private fun findRingVertexNext(ringPts: Array<Coordinate>, index: Int, node: Coordinate): Coordinate {
            //-- safe, since index is always the start of a ring segment
            var iNext = index + 1
            var next = ringPts[iNext]
            while (node.equals2D(next)) {
                iNext = ringIndexNext(ringPts, iNext)
                next = ringPts[iNext]
            }
            return next
        }

        private fun ringIndexPrev(ringPts: Array<Coordinate>, index: Int): Int {
            if (index == 0)
                return ringPts.size - 2
            return index - 1
        }

        private fun ringIndexNext(ringPts: Array<Coordinate>, index: Int): Int {
            if (index >= ringPts.size - 2)
                return 0
            return index + 1
        }

        /**
         * Computes the index of the segment which intersects a given point.
         * @param ringPts the ring points
         * @param pt the intersection point
         * @return the intersection segment index, or -1 if no intersection is found
         */
        private fun intersectingSegIndex(ringPts: Array<Coordinate>, pt: Coordinate): Int {
            for (i in 0 until ringPts.size - 1) {
                if (PointLocation.isOnSegment(pt, ringPts[i], ringPts[i + 1])) {
                    //-- check if pt is the start point of the next segment
                    if (pt.equals2D(ringPts[i + 1])) {
                        return i + 1
                    }
                    return i
                }
            }
            return -1
        }

        /**
         * Finds a self-intersection (if any) in a [LinearRing].
         *
         * @param ring the ring to analyze
         * @return a self-intersection point if one exists, or null
         */
        fun findSelfIntersection(ring: LinearRing): Coordinate? {
            val ata = PolygonTopologyAnalyzer(ring, false)
            if (ata.hasInvalidIntersection())
                return ata.getInvalidLocation()
            return null
        }

        private fun createSegmentStrings(geom: Geometry, isInvertedRingValid: Boolean): MutableList<SegmentString> {
            val segStrings = ArrayList<SegmentString>()
            if (geom is LinearRing) {
                segStrings.add(createSegString(geom, null))
                return segStrings
            }
            for (i in 0 until geom.getNumGeometries()) {
                val poly = geom.getGeometryN(i) as Polygon
                if (poly.isEmpty()) continue
                val hasHoles = poly.getNumInteriorRing() > 0

                //--- polygons with no holes do not need connected interior analysis
                var shellRing: PolygonRing? = null
                if (hasHoles || isInvertedRingValid) {
                    shellRing = PolygonRing(poly.getExteriorRing())
                }
                segStrings.add(createSegString(poly.getExteriorRing(), shellRing))

                for (j in 0 until poly.getNumInteriorRing()) {
                    val hole = poly.getInteriorRingN(j)
                    if (hole.isEmpty()) continue
                    val holeRing = PolygonRing(hole, j, shellRing!!)
                    segStrings.add(createSegString(hole, holeRing))
                }
            }
            return segStrings
        }

        private fun getPolygonRings(segStrings: List<SegmentString>): MutableList<PolygonRing>? {
            var polyRings: MutableList<PolygonRing>? = null
            for (ss in segStrings) {
                val polyRing = ss.getData() as PolygonRing?
                if (polyRing != null) {
                    if (polyRings == null) {
                        polyRings = ArrayList()
                    }
                    polyRings.add(polyRing)
                }
            }
            return polyRings
        }

        private fun createSegString(ring: LinearRing, polyRing: PolygonRing?): SegmentString {
            var pts = ring.getCoordinates()

            //--- repeated points must be removed for accurate intersection detection
            if (CoordinateArrays.hasRepeatedPoints(pts)) {
                pts = CoordinateArrays.removeRepeatedPoints(pts)
            }

            val ss: SegmentString = BasicSegmentString(pts, polyRing)
            return ss
        }
    }
}
