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
package org.locationtech.jts.triangulate.polygon

import org.locationtech.jts.algorithm.LineIntersector
import org.locationtech.jts.algorithm.RobustLineIntersector
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.noding.MCIndexNoder
import org.locationtech.jts.noding.NodedSegmentString
import org.locationtech.jts.noding.SegmentIntersector
import org.locationtech.jts.noding.SegmentString

/**
 * Adds node vertices to the rings of a polygon
 * where holes touch the shell or each other.
 * The structure of the polygon is preserved.
 *
 * This does not fix invalid polygon topology
 * (such as self-touching or crossing rings).
 * Invalid input remains invalid after noding,
 * and does not trigger an error.
 */
class PolygonNoder(shellRing: Array<Coordinate>, holeRings: Array<Array<Coordinate>>) {

    private val isHoleTouching: BooleanArray = BooleanArray(holeRings.size)
    private val nodedRings: MutableList<NodedSegmentString> =
        createNodedSegmentStrings(shellRing, holeRings)

    fun node() {
        val nodeAdder: SegmentIntersector = NodeAdder(isHoleTouching)
        val noder = MCIndexNoder(nodeAdder)
        noder.computeNodes(nodedRings)
    }

    fun isShellNoded(): Boolean {
        return nodedRings[0].hasNodes()
    }

    fun isHoleNoded(i: Int): Boolean {
        return nodedRings[i + 1].hasNodes()
    }

    fun getNodedShell(): Array<Coordinate> {
        return nodedRings[0].getNodedCoordinates()
    }

    fun getNodedHole(i: Int): Array<Coordinate> {
        return nodedRings[i + 1].getNodedCoordinates()
    }

    fun getHolesTouching(): BooleanArray {
        return isHoleTouching
    }

    /**
     * A [SegmentIntersector] that added node vertices
     * to [NodedSegmentString]s where a segment touches another
     * segment in its interior.
     *
     * @author mdavis
     */
    private class NodeAdder(private val isHoleTouching: BooleanArray) : SegmentIntersector {

        private val li: LineIntersector = RobustLineIntersector()

        override fun processIntersections(
            ss0: SegmentString, segIndex0: Int, ss1: SegmentString, segIndex1: Int
        ) {
            //-- input is assumed valid, so rings do not self-intersect
            if (ss0 === ss1)
                return

            val p00 = ss0.getCoordinate(segIndex0)
            val p01 = ss0.getCoordinate(segIndex0 + 1)
            val p10 = ss1.getCoordinate(segIndex1)
            val p11 = ss1.getCoordinate(segIndex1 + 1)

            li.computeIntersection(p00, p01, p10, p11)
            /*
             * There should never be 2 intersection points, since
             * that would imply collinear segments, and an invalid polygon
             */
            if (li.getIntersectionNum() == 1) {
                addTouch(ss0)
                addTouch(ss1)
                val intPt = li.getIntersection(0)
                if (li.isInteriorIntersection(0)) {
                    (ss0 as NodedSegmentString).addIntersectionNode(intPt, segIndex0)
                } else if (li.isInteriorIntersection(1)) {
                    (ss1 as NodedSegmentString).addIntersectionNode(intPt, segIndex1)
                }
            }
        }

        private fun addTouch(ss: SegmentString) {
            val holeIndex = ss.getData() as Int
            if (holeIndex >= 0) {
                isHoleTouching[holeIndex] = true
            }
        }

        override fun isDone(): Boolean {
            return false
        }
    }

    companion object {
        fun createNodedSegmentStrings(
            shellRing: Array<Coordinate>, holeRings: Array<Array<Coordinate>>
        ): MutableList<NodedSegmentString> {
            val segStr = ArrayList<NodedSegmentString>()
            segStr.add(createNodedSegString(shellRing, -1))
            for (i in holeRings.indices) {
                segStr.add(createNodedSegString(holeRings[i], i))
            }
            return segStr
        }

        private fun createNodedSegString(ringPts: Array<Coordinate>, i: Int): NodedSegmentString {
            return NodedSegmentString(ringPts, i)
        }
    }
}
