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

import kotlin.jvm.JvmStatic
import kotlin.math.abs

import org.locationtech.jts.algorithm.BoundaryNodeRule
import org.locationtech.jts.algorithm.LineIntersector
import org.locationtech.jts.algorithm.RobustLineIntersector
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.CoordinateArrays
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryCollection
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.MultiLineString
import org.locationtech.jts.geom.MultiPoint
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.Polygonal
import org.locationtech.jts.geom.util.LinearComponentExtracter
import org.locationtech.jts.noding.BasicSegmentString
import org.locationtech.jts.noding.MCIndexNoder
import org.locationtech.jts.noding.SegmentIntersector
import org.locationtech.jts.noding.SegmentString

/**
 * Tests whether a `Geometry` is simple as defined by the OGC SFS specification.
 *
 * @see BoundaryNodeRule
 *
 */
class IsSimpleOp {

    private val inputGeom: Geometry
    private val isClosedEndpointsInInterior: Boolean
    private var isFindAllLocations = false

    private var simple = false
    private var nonSimplePts: MutableList<Coordinate>? = null

    /**
     * Creates a simplicity checker using the default SFS Mod-2 Boundary Node Rule
     *
     * @param geom the geometry to test
     */
    constructor(geom: Geometry) : this(geom, BoundaryNodeRule.MOD2_BOUNDARY_RULE)

    /**
     * Creates a simplicity checker using a given [BoundaryNodeRule]
     *
     * @param geom the geometry to test
     * @param boundaryNodeRule the boundary node rule to use.
     */
    constructor(geom: Geometry, boundaryNodeRule: BoundaryNodeRule) {
        this.inputGeom = geom
        isClosedEndpointsInInterior = !boundaryNodeRule.isInBoundary(2)
    }

    /**
     * Sets whether all non-simple intersection points
     * will be found.
     *
     * @param isFindAll whether to find all non-simple points
     */
    fun setFindAllLocations(isFindAll: Boolean) {
        this.isFindAllLocations = isFindAll
    }

    /**
     * Tests whether the geometry is simple.
     *
     * @return true if the geometry is simple
     */
    fun isSimple(): Boolean {
        compute()
        return simple
    }

    /**
     * Gets the coordinate for an location where the geometry
     * fails to be simple.
     *
     * @return a coordinate for the location of the non-boundary self-intersection
     * or null if the geometry is simple
     */
    fun getNonSimpleLocation(): Coordinate? {
        compute()
        if (nonSimplePts!!.size == 0) return null
        return nonSimplePts!![0]
    }

    /**
     * Gets all non-simple intersection locations.
     *
     * @return a list of the coordinates of non-simple locations
     */
    fun getNonSimpleLocations(): MutableList<Coordinate> {
        compute()
        return nonSimplePts!!
    }

    private fun compute() {
        if (nonSimplePts != null) return
        nonSimplePts = ArrayList()
        simple = computeSimple(inputGeom)
    }

    private fun computeSimple(geom: Geometry): Boolean {
        if (geom.isEmpty()) return true
        if (geom is Point) return true
        if (geom is LineString) return isSimpleLinearGeometry(geom)
        if (geom is MultiLineString) return isSimpleLinearGeometry(geom)
        if (geom is MultiPoint) return isSimpleMultiPoint(geom)
        if (geom is Polygonal) return isSimplePolygonal(geom)
        if (geom is GeometryCollection) return isSimpleGeometryCollection(geom)
        // all other geometry types are simple by definition
        return true
    }

    private fun isSimpleMultiPoint(mp: MultiPoint): Boolean {
        if (mp.isEmpty()) return true
        var simpleResult = true
        val points = HashSet<Coordinate>()
        for (i in 0 until mp.getNumGeometries()) {
            val pt = mp.getGeometryN(i) as Point
            val p = pt.getCoordinate()
            if (points.contains(p)) {
                nonSimplePts!!.add(p!!)
                simpleResult = false
                if (!isFindAllLocations)
                    break
            } else
                points.add(p!!)
        }
        return simpleResult
    }

    /**
     * Computes simplicity for polygonal geometries.
     * Polygonal geometries are simple if and only if
     * all of their component rings are simple.
     *
     * @param geom a Polygonal geometry
     * @return true if the geometry is simple
     */
    private fun isSimplePolygonal(geom: Geometry): Boolean {
        var simpleResult = true
        val rings = LinearComponentExtracter.getLines(geom)
        for (o in rings) {
            val ring = o as Geometry
            if (!isSimpleLinearGeometry(ring)) {
                simpleResult = false
                if (!isFindAllLocations)
                    break
            }
        }
        return simpleResult
    }

    /**
     * Semantics for GeometryCollection is
     * simple iff all components are simple.
     *
     * @param geom a geometry collection
     * @return true if the geometry is simple
     */
    private fun isSimpleGeometryCollection(geom: Geometry): Boolean {
        var simpleResult = true
        for (i in 0 until geom.getNumGeometries()) {
            val comp = geom.getGeometryN(i)
            if (!computeSimple(comp)) {
                simpleResult = false
                if (!isFindAllLocations)
                    break
            }
        }
        return simpleResult
    }

    private fun isSimpleLinearGeometry(geom: Geometry): Boolean {
        if (geom.isEmpty()) return true
        val segStrings = extractSegmentStrings(geom)
        val segInt = NonSimpleIntersectionFinder(isClosedEndpointsInInterior, isFindAllLocations, nonSimplePts!!)
        val noder = MCIndexNoder()
        noder.setSegmentIntersector(segInt)
        noder.computeNodes(segStrings)
        if (segInt.hasIntersection()) {
            return false
        }
        return true
    }

    companion object {
        /**
         * Tests whether a geometry is simple.
         *
         * @param geom the geometry to test
         * @return true if the geometry is simple
         */
        @JvmStatic
        fun isSimple(geom: Geometry): Boolean {
            val op = IsSimpleOp(geom)
            return op.isSimple()
        }

        /**
         * Gets a non-simple location in a geometry, if any.
         *
         * @param geom the input geometry
         * @return a non-simple location, or null if the geometry is simple
         */
        @JvmStatic
        fun getNonSimpleLocation(geom: Geometry): Coordinate? {
            val op = IsSimpleOp(geom)
            return op.getNonSimpleLocation()
        }

        private fun extractSegmentStrings(geom: Geometry): MutableList<SegmentString> {
            val segStrings = ArrayList<SegmentString>()
            for (i in 0 until geom.getNumGeometries()) {
                val line = geom.getGeometryN(i) as LineString
                val trimPts = trimRepeatedPoints(line.getCoordinates())
                if (trimPts != null) {
                    val ss: SegmentString = BasicSegmentString(trimPts, null)
                    segStrings.add(ss)
                }
            }
            return segStrings
        }

        private fun trimRepeatedPoints(pts: Array<Coordinate>): Array<Coordinate>? {
            if (pts.size <= 2)
                return pts

            val len = pts.size
            val hasRepeatedStart = pts[0].equals2D(pts[1])
            val hasRepeatedEnd = pts[len - 1].equals2D(pts[len - 2])
            if (!hasRepeatedStart && !hasRepeatedEnd)
                return pts

            //-- trim ends
            var startIndex = 0
            val startPt = pts[0]
            while (startIndex < len - 1 && startPt.equals2D(pts[startIndex + 1])) {
                startIndex++
            }
            var endIndex = len - 1
            val endPt = pts[endIndex]
            while (endIndex > 0 && endPt.equals2D(pts[endIndex - 1])) {
                endIndex--
            }
            //-- are all points identical?
            if (endIndex - startIndex < 1) {
                return null
            }
            val trimPts = CoordinateArrays.extract(pts, startIndex, endIndex)
            return trimPts
        }
    }

    private class NonSimpleIntersectionFinder(
        private val isClosedEndpointsInInterior: Boolean,
        private val isFindAll: Boolean,
        private val intersectionPts: MutableList<Coordinate>
    ) : SegmentIntersector {

        val li: LineIntersector = RobustLineIntersector()

        /**
         * Tests whether an intersection was found.
         *
         * @return true if an intersection was found
         */
        fun hasIntersection(): Boolean {
            return intersectionPts.size > 0
        }

        override fun processIntersections(ss0: SegmentString, segIndex0: Int, ss1: SegmentString, segIndex1: Int) {

            // don't test a segment with itself
            val isSameSegString = ss0 === ss1
            val isSameSegment = isSameSegString && segIndex0 == segIndex1
            if (isSameSegment) return

            val hasInt = findIntersection(ss0, segIndex0, ss1, segIndex1)

            if (hasInt) {
                // found an intersection!
                intersectionPts.add(li.getIntersection(0))
            }
        }

        private fun findIntersection(ss0: SegmentString, segIndex0: Int, ss1: SegmentString, segIndex1: Int): Boolean {

            val p00 = ss0.getCoordinate(segIndex0)
            val p01 = ss0.getCoordinate(segIndex0 + 1)
            val p10 = ss1.getCoordinate(segIndex1)
            val p11 = ss1.getCoordinate(segIndex1 + 1)

            li.computeIntersection(p00, p01, p10, p11)
            if (!li.hasIntersection()) return false

            /**
             * Check for an intersection in the interior of a segment.
             */
            val hasInteriorInt = li.isInteriorIntersection()
            if (hasInteriorInt) return true

            /**
             * Check for equal segments (which will produce two intersection points).
             */
            val hasEqualSegments = li.getIntersectionNum() >= 2
            if (hasEqualSegments) return true

            /**
             * Following tests assume non-adjacent segments.
             */
            val isSameSegString = ss0 === ss1
            val isAdjacentSegment = isSameSegString && abs(segIndex1 - segIndex0) <= 1
            if (isAdjacentSegment) return false

            /**
             * At this point there is a single intersection point
             * which is a vertex in each segString.
             * Classify them as endpoints or interior
             */
            val isIntersectionEndpt0 = isIntersectionEndpoint(ss0, segIndex0, li, 0)
            val isIntersectionEndpt1 = isIntersectionEndpoint(ss1, segIndex1, li, 1)

            val hasInteriorVertexInt = !(isIntersectionEndpt0 && isIntersectionEndpt1)
            if (hasInteriorVertexInt) return true

            /**
             * Both intersection vertices must be endpoints.
             * Final check is if one or both of them is interior due
             * to being endpoint of a closed ring.
             */
            if (isClosedEndpointsInInterior && !isSameSegString) {
                val hasInteriorEndpointInt = ss0.isClosed() || ss1.isClosed()
                if (hasInteriorEndpointInt) return true
            }
            return false
        }

        override fun isDone(): Boolean {
            if (isFindAll) return false
            return intersectionPts.size > 0
        }

        companion object {
            /**
             * Tests whether an intersection vertex is an endpoint of a segment string.
             *
             * @param ss the segmentString
             * @param ssIndex index of segment in segmentString
             * @param li the line intersector
             * @param liSegmentIndex index of segment in intersector
             * @return true if the intersection vertex is an endpoint
             */
            private fun isIntersectionEndpoint(
                ss: SegmentString,
                ssIndex: Int,
                li: LineIntersector,
                liSegmentIndex: Int
            ): Boolean {
                val vertexIndex = intersectionVertexIndex(li, liSegmentIndex)
                /**
                 * If the vertex is the first one of the segment, check if it is the start endpoint.
                 * Otherwise check if it is the end endpoint.
                 */
                if (vertexIndex == 0) {
                    return ssIndex == 0
                } else {
                    return ssIndex + 2 == ss.size()
                }
            }

            /**
             * Finds the vertex index in a segment of an intersection
             * which is known to be a vertex.
             *
             * @param li the line intersector
             * @param segmentIndex the intersection segment index
             * @return the vertex index (0 or 1) in the segment vertex of the intersection point
             */
            private fun intersectionVertexIndex(li: LineIntersector, segmentIndex: Int): Int {
                val intPt = li.getIntersection(0)
                val endPt0 = li.getEndpoint(segmentIndex, 0)
                return if (intPt.equals2D(endPt0!!)) 0 else 1
            }
        }
    }
}
