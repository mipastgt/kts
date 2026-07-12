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

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryCollection
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.LinearRing
import org.locationtech.jts.geom.MultiPoint
import org.locationtech.jts.geom.MultiPolygon
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.Polygon

/**
 * Implements the algorithms required to compute the `isValid()` method
 * for [Geometry]s.
 * See the documentation for the various geometry types for a specification of validity.
 *
 * @version 1.7
 */
class IsValidOp(
    /**
     * The geometry being validated
     */
    private val inputGeometry: Geometry
) {
    /**
     * If the following condition is TRUE JTS will validate inverted shells and exverted holes
     * (the ESRI SDE model)
     */
    private var isInvertedRingValid = false

    private var validErr: TopologyValidationError? = null

    /**
     * Sets whether polygons using **Self-Touching Rings** to form
     * holes are reported as valid.
     *
     * @param isValid states whether geometry with this condition is valid
     */
    fun setSelfTouchingRingFormingHoleValid(isValid: Boolean) {
        isInvertedRingValid = isValid
    }

    /**
     * Tests the validity of the input geometry.
     *
     * @return true if the geometry is valid
     */
    fun isValid(): Boolean {
        return isValidGeometry(inputGeometry)
    }

    /**
     * Computes the validity of the geometry,
     * and if not valid returns the validation error for the geometry,
     * or null if the geometry is valid.
     *
     * @return the validation error, if the geometry is invalid
     * or null if the geometry is valid
     */
    fun getValidationError(): TopologyValidationError? {
        isValidGeometry(inputGeometry)
        return validErr
    }

    private fun logInvalid(code: Int, pt: Coordinate?) {
        validErr = TopologyValidationError(code, pt)
    }

    private fun hasInvalidError(): Boolean {
        return validErr != null
    }

    private fun isValidGeometry(g: Geometry): Boolean {
        validErr = null

        // empty geometries are always valid
        if (g.isEmpty()) return true

        if (g is Point) return isValid(g)
        if (g is MultiPoint) return isValid(g)
        if (g is LinearRing) return isValid(g)
        if (g is LineString) return isValid(g)
        if (g is Polygon) return isValid(g)
        if (g is MultiPolygon) return isValid(g)
        if (g is GeometryCollection) return isValid(g)

        // geometry type not known
        throw UnsupportedOperationException(g::class.simpleName)
    }

    /**
     * Tests validity of a Point.
     */
    private fun isValid(g: Point): Boolean {
        checkCoordinatesValid(g.getCoordinates())
        if (hasInvalidError()) return false
        return true
    }

    /**
     * Tests validity of a MultiPoint.
     */
    private fun isValid(g: MultiPoint): Boolean {
        checkCoordinatesValid(g.getCoordinates())
        if (hasInvalidError()) return false
        return true
    }

    /**
     * Tests validity of a LineString.
     * Almost anything goes for linestrings!
     */
    private fun isValid(g: LineString): Boolean {
        checkCoordinatesValid(g.getCoordinates())
        if (hasInvalidError()) return false
        checkPointSize(g, MIN_SIZE_LINESTRING)
        if (hasInvalidError()) return false
        return true
    }

    /**
     * Tests validity of a LinearRing.
     */
    private fun isValid(g: LinearRing): Boolean {
        checkCoordinatesValid(g.getCoordinates())
        if (hasInvalidError()) return false

        checkRingClosed(g)
        if (hasInvalidError()) return false

        checkRingPointSize(g)
        if (hasInvalidError()) return false

        checkRingSimple(g)
        return validErr == null
    }

    /**
     * Tests the validity of a polygon.
     * Sets the validErr flag.
     */
    private fun isValid(g: Polygon): Boolean {
        checkCoordinatesValid(g)
        if (hasInvalidError()) return false

        checkRingsClosed(g)
        if (hasInvalidError()) return false

        checkRingsPointSize(g)
        if (hasInvalidError()) return false

        val areaAnalyzer = PolygonTopologyAnalyzer(g, isInvertedRingValid)

        checkAreaIntersections(areaAnalyzer)
        if (hasInvalidError()) return false

        checkHolesInShell(g)
        if (hasInvalidError()) return false

        checkHolesNotNested(g)
        if (hasInvalidError()) return false

        checkInteriorConnected(areaAnalyzer)
        if (hasInvalidError()) return false

        return true
    }

    /**
     * Tests validity of a MultiPolygon.
     */
    private fun isValid(g: MultiPolygon): Boolean {
        for (i in 0 until g.getNumGeometries()) {
            val p = g.getGeometryN(i) as Polygon
            checkCoordinatesValid(p)
            if (hasInvalidError()) return false

            checkRingsClosed(p)
            if (hasInvalidError()) return false
            checkRingsPointSize(p)
            if (hasInvalidError()) return false
        }

        val areaAnalyzer = PolygonTopologyAnalyzer(g, isInvertedRingValid)

        checkAreaIntersections(areaAnalyzer)
        if (hasInvalidError()) return false

        for (i in 0 until g.getNumGeometries()) {
            val p = g.getGeometryN(i) as Polygon
            checkHolesInShell(p)
            if (hasInvalidError()) return false
        }
        for (i in 0 until g.getNumGeometries()) {
            val p = g.getGeometryN(i) as Polygon
            checkHolesNotNested(p)
            if (hasInvalidError()) return false
        }
        checkShellsNotNested(g)
        if (hasInvalidError()) return false

        checkInteriorConnected(areaAnalyzer)
        if (hasInvalidError()) return false

        return true
    }

    /**
     * Tests validity of a GeometryCollection.
     */
    private fun isValid(gc: GeometryCollection): Boolean {
        for (i in 0 until gc.getNumGeometries()) {
            if (!isValidGeometry(gc.getGeometryN(i)))
                return false
        }
        return true
    }

    private fun checkCoordinatesValid(coords: Array<Coordinate>) {
        for (i in coords.indices) {
            if (!isValid(coords[i])) {
                logInvalid(TopologyValidationError.INVALID_COORDINATE, coords[i])
                return
            }
        }
    }

    private fun checkCoordinatesValid(poly: Polygon) {
        checkCoordinatesValid(poly.getExteriorRing().getCoordinates())
        if (hasInvalidError()) return
        for (i in 0 until poly.getNumInteriorRing()) {
            checkCoordinatesValid(poly.getInteriorRingN(i).getCoordinates())
            if (hasInvalidError()) return
        }
    }

    private fun checkRingClosed(ring: LinearRing) {
        if (ring.isEmpty()) return
        if (!ring.isClosed()) {
            val pt = if (ring.getNumPoints() >= 1) ring.getCoordinateN(0) else null
            logInvalid(TopologyValidationError.RING_NOT_CLOSED, pt)
            return
        }
    }

    private fun checkRingsClosed(poly: Polygon) {
        checkRingClosed(poly.getExteriorRing())
        if (hasInvalidError()) return
        for (i in 0 until poly.getNumInteriorRing()) {
            checkRingClosed(poly.getInteriorRingN(i))
            if (hasInvalidError()) return
        }
    }

    private fun checkRingsPointSize(poly: Polygon) {
        checkRingPointSize(poly.getExteriorRing())
        if (hasInvalidError()) return
        for (i in 0 until poly.getNumInteriorRing()) {
            checkRingPointSize(poly.getInteriorRingN(i))
            if (hasInvalidError()) return
        }
    }

    private fun checkRingPointSize(ring: LinearRing) {
        if (ring.isEmpty()) return
        checkPointSize(ring, MIN_SIZE_RING)
    }

    /**
     * Check the number of non-repeated points is at least a given size.
     */
    private fun checkPointSize(line: LineString, minSize: Int) {
        if (!isNonRepeatedSizeAtLeast(line, minSize)) {
            val pt = if (line.getNumPoints() >= 1) line.getCoordinateN(0) else null
            logInvalid(TopologyValidationError.TOO_FEW_POINTS, pt)
        }
    }

    /**
     * Test if the number of non-repeated points in a line
     * is at least a given minimum size.
     *
     * @param line the line to test
     * @param minSize the minimum line size
     * @return true if the line has the required number of non-repeated points
     */
    private fun isNonRepeatedSizeAtLeast(line: LineString, minSize: Int): Boolean {
        var numPts = 0
        var prevPt: Coordinate? = null
        for (i in 0 until line.getNumPoints()) {
            if (numPts >= minSize) return true
            val pt = line.getCoordinateN(i)
            if (prevPt == null || !pt.equals2D(prevPt))
                numPts++
            prevPt = pt
        }
        return numPts >= minSize
    }

    private fun checkAreaIntersections(areaAnalyzer: PolygonTopologyAnalyzer) {
        if (areaAnalyzer.hasInvalidIntersection()) {
            logInvalid(
                areaAnalyzer.getInvalidCode(),
                areaAnalyzer.getInvalidLocation()
            )
            return
        }
    }

    /**
     * Check whether a ring self-intersects (except at its endpoints).
     *
     * @param ring the linear ring to check
     */
    private fun checkRingSimple(ring: LinearRing) {
        val intPt = PolygonTopologyAnalyzer.findSelfIntersection(ring)
        if (intPt != null) {
            logInvalid(
                TopologyValidationError.RING_SELF_INTERSECTION,
                intPt
            )
        }
    }

    /**
     * Tests that each hole is inside the polygon shell.
     *
     * @param poly the polygon to be tested for hole inclusion
     */
    private fun checkHolesInShell(poly: Polygon) {
        // skip test if no holes are present
        if (poly.getNumInteriorRing() <= 0) return

        val shell = poly.getExteriorRing()
        val isShellEmpty = shell.isEmpty()

        for (i in 0 until poly.getNumInteriorRing()) {
            val hole = poly.getInteriorRingN(i)
            if (hole.isEmpty()) continue

            val invalidPt: Coordinate? = if (isShellEmpty) {
                hole.getCoordinate()
            } else {
                findHoleOutsideShellPoint(hole, shell)
            }
            if (invalidPt != null) {
                logInvalid(
                    TopologyValidationError.HOLE_OUTSIDE_SHELL,
                    invalidPt
                )
                return
            }
        }
    }

    /**
     * Checks if a polygon hole lies inside its shell
     * and if not returns a point indicating this.
     *
     * @param hole the hole to test
     * @param shell the polygon shell to test against
     * @return a hole point outside the shell, or null if it is inside
     */
    private fun findHoleOutsideShellPoint(hole: LinearRing, shell: LinearRing): Coordinate? {
        val holePt0 = hole.getCoordinateN(0)
        /**
         * If hole envelope is not covered by shell, it must be outside
         */
        if (!shell.getEnvelopeInternal().covers(hole.getEnvelopeInternal()))
            return holePt0

        if (PolygonTopologyAnalyzer.isRingNested(hole, shell))
            return null
        return holePt0
    }

    /**
     * Checks if any polygon hole is nested inside another.
     *
     * @param poly the polygon with holes to test
     */
    private fun checkHolesNotNested(poly: Polygon) {
        // skip test if no holes are present
        if (poly.getNumInteriorRing() <= 0) return

        val nestedTester = IndexedNestedHoleTester(poly)
        if (nestedTester.isNested()) {
            logInvalid(
                TopologyValidationError.NESTED_HOLES,
                nestedTester.getNestedPoint()
            )
        }
    }

    /**
     * Checks that no element polygon is in the interior of another element polygon.
     */
    private fun checkShellsNotNested(mp: MultiPolygon) {
        // skip test if only one shell present
        if (mp.getNumGeometries() <= 1) return

        val nestedTester = IndexedNestedPolygonTester(mp)
        if (nestedTester.isNested()) {
            logInvalid(
                TopologyValidationError.NESTED_SHELLS,
                nestedTester.getNestedPoint()
            )
        }
    }

    private fun checkInteriorConnected(analyzer: PolygonTopologyAnalyzer) {
        if (analyzer.isInteriorDisconnected()) {
            logInvalid(
                TopologyValidationError.DISCONNECTED_INTERIOR,
                analyzer.getDisconnectionLocation()
            )
        }
    }

    companion object {
        private const val MIN_SIZE_LINESTRING = 2
        private const val MIN_SIZE_RING = 4

        /**
         * Tests whether a [Geometry] is valid.
         * @param geom the Geometry to test
         * @return true if the geometry is valid
         */
        @JvmStatic
        fun isValid(geom: Geometry): Boolean {
            val isValidOp = IsValidOp(geom)
            return isValidOp.isValid()
        }

        /**
         * Checks whether a coordinate is valid for processing.
         * Coordinates are valid if their x and y ordinates are in the
         * range of the floating point representation.
         *
         * @param coord the coordinate to validate
         * @return `true` if the coordinate is valid
         */
        @JvmStatic
        fun isValid(coord: Coordinate): Boolean {
            if (coord.x.isNaN()) return false
            if (coord.x.isInfinite()) return false
            if (coord.y.isNaN()) return false
            if (coord.y.isInfinite()) return false
            return true
        }
    }
}
