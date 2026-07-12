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
package org.locationtech.jts.triangulate.polygon

import kotlin.jvm.JvmStatic

import org.locationtech.jts.util.TreeSet

import org.locationtech.jts.algorithm.LineIntersector
import org.locationtech.jts.algorithm.Orientation
import org.locationtech.jts.algorithm.PolygonNodeTopology
import org.locationtech.jts.algorithm.RobustLineIntersector
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.CoordinateArrays
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.LinearRing
import org.locationtech.jts.geom.Polygon
import org.locationtech.jts.noding.BasicSegmentString
import org.locationtech.jts.noding.MCIndexSegmentSetMutualIntersector
import org.locationtech.jts.noding.SegmentIntersector
import org.locationtech.jts.noding.SegmentSetMutualIntersector
import org.locationtech.jts.noding.SegmentString

/**
 * Transforms a polygon with holes into a single self-touching (invalid) ring
 * by joining holes to the exterior shell or to another hole
 * with out-and-back line segments.
 * The holes are added in order of their envelopes (leftmost/lowest first).
 * As the result shell develops, a hole may be added to what was
 * originally another hole.
 *
 * There is no attempt to optimize the quality of the join lines.
 * In particular, holes may be joined by lines longer than is optimal.
 * However, holes which touch the shell or other holes are joined at the touch point.
 *
 * The class does not require the input polygon to have normal
 * orientation (shell CW and rings CCW).
 * The output ring is always CW.
 */
/**
 * Creates a new hole joiner.
 *
 * @param polygon the polygon to join
 */
class PolygonHoleJoiner(private val inputPolygon: Polygon) {

    //-- normalized, sorted and noded polygon rings
    private lateinit var shellRing: Array<Coordinate>
    private lateinit var holeRings: Array<Array<Coordinate>>

    //-- indicates whether a hole should be testing for touching
    private lateinit var isHoleTouchingHint: BooleanArray

    private lateinit var joinedRing: MutableList<Coordinate>
    // a sorted and searchable version of the joinedRing
    private lateinit var joinedPts: TreeSet<Coordinate>
    private lateinit var boundaryIntersector: SegmentSetMutualIntersector

    /**
     * Computes the joined ring.
     *
     * @return the points in the joined ring
     */
    fun compute(): Array<Coordinate> {
        extractOrientedRings(inputPolygon)
        if (holeRings.size > 0)
            nodeRings()
        joinedRing = copyToList(shellRing)
        if (holeRings.size > 0)
            joinHoles()
        return CoordinateArrays.toCoordinateArray(joinedRing)
    }

    private fun extractOrientedRings(polygon: Polygon) {
        shellRing = extractOrientedRing(polygon.getExteriorRing(), true)
        val holes = sortHoles(polygon)
        holeRings = Array(holes.size) { i -> extractOrientedRing(holes[i], false) }
    }

    private fun nodeRings() {
        val noder = PolygonNoder(shellRing, holeRings)
        noder.node()
        if (noder.isShellNoded()) {
            shellRing = noder.getNodedShell()
        }
        for (i in holeRings.indices) {
            if (noder.isHoleNoded(i)) {
                holeRings[i] = noder.getNodedHole(i)
            }
        }
        isHoleTouchingHint = noder.getHolesTouching()
    }

    private fun joinHoles() {
        boundaryIntersector = createBoundaryIntersector(shellRing, holeRings)

        joinedPts = TreeSet()
        joinedPts.addAll(joinedRing)

        for (i in holeRings.indices) {
            joinHole(i, holeRings[i])
        }
    }

    private fun joinHole(index: Int, holeCoords: Array<Coordinate>) {
        //-- check if hole is touching
        if (isHoleTouchingHint[index]) {
            val isTouching = joinTouchingHole(holeCoords)
            if (isTouching)
                return
        }
        joinNonTouchingHole(holeCoords)
    }

    /**
     * Joins a hole to the shell only if the hole touches the shell.
     * Otherwise, reports the hole is non-touching.
     *
     * @param holeCoords the hole to join
     * @return true if the hole was touching, false if not
     */
    private fun joinTouchingHole(holeCoords: Array<Coordinate>): Boolean {
        val holeTouchIndex = findHoleTouchIndex(holeCoords)

        //-- hole does not touch
        if (holeTouchIndex < 0)
            return false

        /**
         * Find shell corner which contains the hole,
         * by finding corner which has a hole segment at the join pt in interior
         */
        val joinPt = holeCoords[holeTouchIndex]
        val holeSegPt = holeCoords[prev(holeTouchIndex, holeCoords.size)]

        val joinIndex = findJoinIndex(joinPt, holeSegPt)
        addJoinedHole(joinIndex, holeCoords, holeTouchIndex)
        return true
    }

    /**
     * Finds the vertex index of a hole where it touches the
     * current shell (if it does).
     * If a hole does touch, it must touch at a single vertex
     * (otherwise, the polygon is invalid).
     *
     * @param holeCoords the hole
     * @return the index of the touching vertex, or -1 if no touch
     */
    private fun findHoleTouchIndex(holeCoords: Array<Coordinate>): Int {
        for (i in holeCoords.indices) {
            if (joinedPts.contains(holeCoords[i]))
                return i
        }
        return -1
    }

    /**
     * Joins a single non-touching hole to the current joined ring.
     *
     * @param holeCoords the hole to join
     */
    private fun joinNonTouchingHole(holeCoords: Array<Coordinate>) {
        val holeJoinIndex = findLowestLeftVertexIndex(holeCoords)
        val holeJoinCoord = holeCoords[holeJoinIndex]
        val joinCoord = findJoinableVertex(holeJoinCoord)
        val joinIndex = findJoinIndex(joinCoord, holeJoinCoord)
        addJoinedHole(joinIndex, holeCoords, holeJoinIndex)
    }

    /**
     * Finds a shell vertex that is joinable to the hole join vertex.
     * One must always exist, since the hole join vertex is on the left
     * of the hole, and thus must always have at least one shell vertex visible to it.
     *
     * There is no attempt to optimize the selection of shell vertex
     * to join to (e.g. by choosing one with shortest distance).
     *
     * @param holeJoinCoord the hole join vertex
     * @return the shell vertex to join to
     */
    private fun findJoinableVertex(holeJoinCoord: Coordinate): Coordinate {
        //-- find highest shell vertex in half-plane left of hole pt
        var candidate = joinedPts.higher(holeJoinCoord)
        while (candidate!!.x == holeJoinCoord.x) {
            candidate = joinedPts.higher(candidate)
        }
        //-- drop back to last vertex with same X as hole
        candidate = joinedPts.lower(candidate!!)

        //-- find rightmost joinable shell vertex
        while (intersectsBoundary(holeJoinCoord, candidate!!)) {
            candidate = joinedPts.lower(candidate)
            //Assert: candidate is not null, since a joinable candidate always exists
            if (candidate == null) {
                throw IllegalStateException("Unable to find joinable vertex")
            }
        }
        return candidate!!
    }

    /**
     * Gets the join ring vertex index that the hole is joined after.
     * A vertex can occur multiple times in the join ring, so it is necessary
     * to choose the one which forms a corner having the
     * join line in the ring interior.
     *
     * @param joinCoord the join ring vertex
     * @param holeJoinCoord the hole join vertex
     * @return the join ring vertex index to join after
     */
    private fun findJoinIndex(joinCoord: Coordinate, holeJoinCoord: Coordinate): Int {
        //-- linear scan is slow but only done once per hole
        for (i in 0 until joinedRing.size - 1) {
            if (joinCoord.equals2D(joinedRing[i])) {
                if (isLineInterior(joinedRing, i, holeJoinCoord)) {
                    return i
                }
            }
        }
        throw IllegalStateException("Unable to find shell join index with interior join line")
    }

    /**
     * Tests if a line between a ring corner vertex and a given point
     * is interior to the ring corner.
     *
     * @param ring a ring of points
     * @param ringIndex the index of a ring vertex
     * @param linePt the point to be joined to the ring
     * @return true if the line to the point is interior to the ring corner
     */
    private fun isLineInterior(
        ring: List<Coordinate>, ringIndex: Int,
        linePt: Coordinate
    ): Boolean {
        val nodePt = ring[ringIndex]
        val shell0 = ring[prev(ringIndex, ring.size)]
        val shell1 = ring[next(ringIndex, ring.size)]
        return PolygonNodeTopology.isInteriorSegment(nodePt, shell0, shell1, linePt)
    }

    /**
     * Add hole vertices at proper position in shell vertex list.
     * This code assumes that if hole touches (shell or other hole),
     * it touches at a node.  This requires an initial noding step.
     * In this case, the code avoids duplicating join vertices.
     *
     * Also adds hole points to ordered coordinates.
     *
     * @param joinIndex index of join vertex in shell
     * @param holeCoords the vertices of the hole to be inserted
     * @param holeJoinIndex index of join vertex in hole
     */
    private fun addJoinedHole(joinIndex: Int, holeCoords: Array<Coordinate>, holeJoinIndex: Int) {
        val joinPt = joinedRing[joinIndex]
        val holeJoinPt = holeCoords[holeJoinIndex]

        //-- check for touching (zero-length) join to avoid inserting duplicate vertices
        val isVertexTouch = joinPt.equals2D(holeJoinPt)
        val addJoinPt = if (isVertexTouch) null else joinPt

        //-- create new section of vertices to insert in shell
        val newSection = createHoleSection(holeCoords, holeJoinIndex, addJoinPt)

        //-- add section after shell join vertex
        val addIndex = joinIndex + 1
        joinedRing.addAll(addIndex, newSection)
        joinedPts.addAll(newSection)
    }

    /**
     * Creates the new section of vertices for ad added hole,
     * including any required vertices from the shell at the join point,
     * and ensuring join vertices are not duplicated.
     *
     * @param holeCoords the hole vertices
     * @param holeJoinIndex the index of the join vertex
     * @param joinPt the shell join vertex
     * @return a list of new vertices to be added
     */
    private fun createHoleSection(
        holeCoords: Array<Coordinate>, holeJoinIndex: Int,
        joinPt: Coordinate?
    ): MutableList<Coordinate> {
        val section = ArrayList<Coordinate>()

        val isNonTouchingHole = joinPt != null
        /**
         * Add all hole vertices, including duplicate at hole join vertex
         * Except if hole DOES touch, join vertex is already in shell ring
         */
        if (isNonTouchingHole)
            section.add(holeCoords[holeJoinIndex].copy())

        val holeSize = holeCoords.size - 1
        var index = holeJoinIndex
        for (i in 0 until holeSize) {
            index = (index + 1) % holeSize
            section.add(holeCoords[index].copy())
        }
        /**
         * Add duplicate shell vertex at end of the return join line.
         * Except if hole DOES touch, join line is zero-length so do not need dup vertex
         */
        if (isNonTouchingHole) {
            section.add(joinPt!!.copy())
        }

        return section
    }

    /**
     * Tests whether the interior of a line segment intersects the polygon boundary.
     * If so, the line is not a valid join line.
     *
     * @param p0 a segment vertex
     * @param p1 the other segment vertex
     * @return true if the segment interior intersects a polygon boundary segment
     */
    private fun intersectsBoundary(p0: Coordinate, p1: Coordinate): Boolean {
        val segString: SegmentString = BasicSegmentString(
            arrayOf(p0, p1), null
        )
        val segStrings = ArrayList<SegmentString>()
        segStrings.add(segString)

        val segInt = InteriorIntersectionDetector()
        boundaryIntersector.process(segStrings, segInt)
        return segInt.hasIntersection()
    }

    /**
     * Detects if a segment has an interior intersection with another segment.
     */
    private class InteriorIntersectionDetector : SegmentIntersector {

        private val li: LineIntersector = RobustLineIntersector()
        private var hasIntersection = false

        fun hasIntersection(): Boolean {
            return hasIntersection
        }

        override fun processIntersections(
            ss0: SegmentString, segIndex0: Int, ss1: SegmentString, segIndex1: Int
        ) {
            val p00 = ss0.getCoordinate(segIndex0)
            val p01 = ss0.getCoordinate(segIndex0 + 1)
            val p10 = ss1.getCoordinate(segIndex1)
            val p11 = ss1.getCoordinate(segIndex1 + 1)

            li.computeIntersection(p00, p01, p10, p11)
            if (li.getIntersectionNum() == 0) {
                return
            } else if (li.getIntersectionNum() == 1) {
                if (li.isInteriorIntersection())
                    hasIntersection = true
            } else { // li.getIntersectionNum() >= 2 - must be collinear
                hasIntersection = true
            }
        }

        override fun isDone(): Boolean {
            return hasIntersection
        }
    }

    private class EnvelopeComparator : Comparator<Geometry> {
        override fun compare(g1: Geometry, g2: Geometry): Int {
            val e1 = g1.getEnvelopeInternal()
            val e2 = g2.getEnvelopeInternal()
            return e1.compareTo(e2)
        }
    }

    companion object {
        /**
         * Joins the shell and holes of a polygon
         * and returns the result as an (invalid) Polygon.
         *
         * @param polygon the polygon to join
         * @return the result polygon
         */
        @JvmStatic
        fun joinAsPolygon(polygon: Polygon): Polygon {
            return polygon.getFactory().createPolygon(join(polygon))
        }

        /**
         * Joins the shell and holes of a polygon
         * and returns the result as sequence of Coordinates.
         *
         * @param polygon the polygon to join
         * @return the result coordinates
         */
        @JvmStatic
        fun join(polygon: Polygon): Array<Coordinate> {
            val joiner = PolygonHoleJoiner(polygon)
            return joiner.compute()
        }

        private fun extractOrientedRing(ring: LinearRing, isCW: Boolean): Array<Coordinate> {
            val pts = ring.getCoordinates()
            val isRingCW = !Orientation.isCCW(pts)
            if (isCW == isRingCW)
                return pts
            //-- reverse a copy of the points
            val ptsRev = pts.copyOf()
            CoordinateArrays.reverse(ptsRev)
            return ptsRev
        }

        private fun copyToList(coords: Array<Coordinate>): MutableList<Coordinate> {
            val coordList = ArrayList<Coordinate>()
            for (p in coords) {
                coordList.add(p.copy())
            }
            return coordList
        }

        private fun prev(i: Int, size: Int): Int {
            val prev = i - 1
            if (prev < 0)
                return size - 2
            return prev
        }

        private fun next(i: Int, size: Int): Int {
            val next = i + 1
            if (next > size - 2)
                return 0
            return next
        }

        /**
         * Sort the hole rings by minimum X, minimum Y.
         *
         * @param poly polygon that contains the holes
         * @return a list of sorted hole rings
         */
        private fun sortHoles(poly: Polygon): List<LinearRing> {
            val holes = ArrayList<LinearRing>()
            for (i in 0 until poly.getNumInteriorRing()) {
                holes.add(poly.getInteriorRingN(i))
            }
            holes.sortWith(EnvelopeComparator())
            return holes
        }

        private fun findLowestLeftVertexIndex(coords: Array<Coordinate>): Int {
            var lowestLeftCoord: Coordinate? = null
            var lowestLeftIndex = -1
            for (i in 0 until coords.size - 1) {
                if (lowestLeftCoord == null || coords[i].compareTo(lowestLeftCoord) < 0) {
                    lowestLeftCoord = coords[i]
                    lowestLeftIndex = i
                }
            }
            return lowestLeftIndex
        }

        private fun createBoundaryIntersector(
            shellRing: Array<Coordinate>, holeRings: Array<Array<Coordinate>>
        ): SegmentSetMutualIntersector {
            val polySegStrings = ArrayList<SegmentString>()
            polySegStrings.add(BasicSegmentString(shellRing, null))
            for (hole in holeRings) {
                polySegStrings.add(BasicSegmentString(hole, null))
            }
            return MCIndexSegmentSetMutualIntersector(polySegStrings)
        }
    }
}
