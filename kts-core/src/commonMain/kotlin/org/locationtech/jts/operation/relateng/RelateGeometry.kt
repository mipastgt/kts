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

import org.locationtech.jts.algorithm.BoundaryNodeRule
import org.locationtech.jts.algorithm.Orientation
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.CoordinateArrays
import org.locationtech.jts.geom.Dimension
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryCollection
import org.locationtech.jts.geom.GeometryCollectionIterator
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.LinearRing
import org.locationtech.jts.geom.MultiLineString
import org.locationtech.jts.geom.MultiPoint
import org.locationtech.jts.geom.MultiPolygon
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.Polygon
import org.locationtech.jts.geom.util.ComponentCoordinateExtracter
import org.locationtech.jts.geom.util.PointExtracter

class RelateGeometry(input: Geometry, isPrepared: Boolean, bnRule: BoundaryNodeRule) {

    private val geom: Geometry = input
    private val prepared: Boolean = isPrepared

    private val geomEnv: Envelope = input.getEnvelopeInternal()
    private var geomDim: Int = Dimension.FALSE
    private var uniquePoints: MutableSet<Coordinate>? = null
    private val boundaryNodeRule: BoundaryNodeRule = bnRule
    private var locator: RelatePointLocator? = null
    private var elementId = 0
    private var hasPoints = false
    private var hasLines = false
    private var hasAreas = false
    private var isLineZeroLen = false
    private var isGeomEmpty = false

    init {
        //-- cache geometry metadata
        isGeomEmpty = geom.isEmpty()
        geomDim = input.getDimension()
        analyzeDimensions()
        isLineZeroLen = isZeroLengthLine(geom)
    }

    constructor(input: Geometry) : this(input, false, BoundaryNodeRule.OGC_SFS_BOUNDARY_RULE)

    constructor(input: Geometry, bnRule: BoundaryNodeRule) : this(input, false, bnRule)

    private fun isZeroLengthLine(geom: Geometry): Boolean {
        // avoid expensive zero-length calculation if not linear
        if (getDimension() != Dimension.L)
            return false
        return isZeroLength(geom)
    }

    private fun analyzeDimensions() {
        if (isGeomEmpty) {
            return
        }
        if (geom is Point || geom is MultiPoint) {
            hasPoints = true
            geomDim = Dimension.P
            return
        }
        if (geom is LineString || geom is MultiLineString) {
            hasLines = true
            geomDim = Dimension.L
            return
        }
        if (geom is Polygon || geom is MultiPolygon) {
            hasAreas = true
            geomDim = Dimension.A
            return
        }
        //-- analyze a (possibly mixed type) collection
        val geomi = GeometryCollectionIterator(geom)
        while (geomi.hasNext()) {
            val elem = geomi.next() as Geometry
            if (elem.isEmpty())
                continue
            if (elem is Point) {
                hasPoints = true
                if (geomDim < Dimension.P) geomDim = Dimension.P
            }
            if (elem is LineString) {
                hasLines = true
                if (geomDim < Dimension.L) geomDim = Dimension.L
            }
            if (elem is Polygon) {
                hasAreas = true
                if (geomDim < Dimension.A) geomDim = Dimension.A
            }
        }
    }

    fun getGeometry(): Geometry {
        return geom
    }

    fun isPrepared(): Boolean {
        return prepared
    }

    fun getEnvelope(): Envelope {
        return geomEnv
    }

    fun getDimension(): Int {
        return geomDim
    }

    fun hasDimension(dim: Int): Boolean {
        when (dim) {
            Dimension.P -> return hasPoints
            Dimension.L -> return hasLines
            Dimension.A -> return hasAreas
        }
        return false
    }

    /**
     * Gets the actual non-empty dimension of the geometry.
     * Zero-length LineStrings are treated as Points.
     *
     * @return the real (non-empty) dimension
     */
    fun getDimensionReal(): Int {
        if (isGeomEmpty) return Dimension.FALSE
        if (getDimension() == 1 && isLineZeroLen)
            return Dimension.P
        if (hasAreas) return Dimension.A
        if (hasLines) return Dimension.L
        return Dimension.P
    }

    fun hasEdges(): Boolean {
        return hasLines || hasAreas
    }

    private fun getLocator(): RelatePointLocator {
        if (locator == null)
            locator = RelatePointLocator(geom, prepared, boundaryNodeRule)
        return locator!!
    }

    fun isNodeInArea(nodePt: Coordinate, parentPolygonal: Geometry?): Boolean {
        val loc = getLocator().locateNodeWithDim(nodePt, parentPolygonal)
        return loc == DimensionLocation.AREA_INTERIOR
    }

    fun locateLineEndWithDim(p: Coordinate): Int {
        return getLocator().locateLineEndWithDim(p)
    }

    /**
     * Locates a vertex of a polygon.
     *
     * @param pt the polygon vertex
     * @return the location of the vertex
     */
    fun locateAreaVertex(pt: Coordinate): Int {
        /**
         * Can pass a null polygon, because the point is an exact vertex,
         * which will be detected as being on the boundary of its polygon
         */
        return locateNode(pt, null)
    }

    fun locateNode(pt: Coordinate, parentPolygonal: Geometry?): Int {
        return getLocator().locateNode(pt, parentPolygonal)
    }

    fun locateWithDim(pt: Coordinate): Int {
        val loc = getLocator().locateWithDim(pt)
        return loc
    }

    /**
     * Indicates whether the geometry requires self-noding
     * for correct evaluation of specific spatial predicates.
     *
     * @return true if self-noding is required for this geometry
     */
    fun isSelfNodingRequired(): Boolean {
        if (geom is Point ||
            geom is MultiPoint ||
            geom is Polygon ||
            geom is MultiPolygon
        )
            return false
        //-- a GC with a single polygon does not need noding
        if (hasAreas && geom.getNumGeometries() == 1)
            return false
        return true
    }

    /**
     * Tests whether the geometry has polygonal topology.
     *
     * @return true if the geometry has polygonal topology
     */
    fun isPolygonal(): Boolean {
        //TODO: also true for a GC containing one polygonal element (and possibly some lower-dimension elements)
        return geom is Polygon || geom is MultiPolygon
    }

    fun isEmpty(): Boolean {
        return isGeomEmpty
    }

    fun hasBoundary(): Boolean {
        return getLocator().hasBoundary()
    }

    fun getUniquePoints(): MutableSet<Coordinate> {
        //-- will be re-used in prepared mode
        if (uniquePoints == null) {
            uniquePoints = createUniquePoints()
        }
        return uniquePoints!!
    }

    private fun createUniquePoints(): MutableSet<Coordinate> {
        //-- only called on P geometries
        @Suppress("UNCHECKED_CAST")
        val pts = ComponentCoordinateExtracter.getCoordinates(geom) as List<Coordinate>
        val set = HashSet<Coordinate>()
        set.addAll(pts)
        return set
    }

    fun getEffectivePoints(): List<Point> {
        @Suppress("UNCHECKED_CAST")
        val ptListAll = PointExtracter.getPoints(geom) as List<Point>

        if (getDimensionReal() <= Dimension.P)
            return ptListAll

        //-- only return Points not covered by another element
        val ptList = ArrayList<Point>()
        for (p in ptListAll) {
            if (p.isEmpty())
                continue
            val locDim = locateWithDim(p.getCoordinate()!!)
            if (DimensionLocation.dimension(locDim) == Dimension.P) {
                ptList.add(p)
            }
        }
        return ptList
    }

    /**
     * Extract RelateSegmentStrings from the geometry which
     * intersect a given envelope.
     * If the envelope is null all edges are extracted.
     *
     * @param env the envelope to extract around (may be null)
     * @return a list of RelateSegmentStrings
     */
    internal fun extractSegmentStrings(isA: Boolean, env: Envelope?): MutableList<RelateSegmentString> {
        val segStrings = ArrayList<RelateSegmentString>()
        extractSegmentStrings(isA, env, geom, segStrings)
        return segStrings
    }

    private fun extractSegmentStrings(isA: Boolean, env: Envelope?, geom: Geometry, segStrings: MutableList<RelateSegmentString>) {
        //-- record if parent is MultiPolygon
        var parentPolygonal: MultiPolygon? = null
        if (geom is MultiPolygon) {
            parentPolygonal = geom
        }

        for (i in 0 until geom.getNumGeometries()) {
            val g = geom.getGeometryN(i)
            if (g is GeometryCollection) {
                extractSegmentStrings(isA, env, g, segStrings)
            } else {
                extractSegmentStringsFromAtomic(isA, g, parentPolygonal, env, segStrings)
            }
        }
    }

    private fun extractSegmentStringsFromAtomic(
        isA: Boolean,
        geom: Geometry,
        parentPolygonal: MultiPolygon?,
        env: Envelope?,
        segStrings: MutableList<RelateSegmentString>
    ) {
        if (geom.isEmpty())
            return
        val doExtract = env == null || env.intersects(geom.getEnvelopeInternal())
        if (!doExtract)
            return

        elementId++
        if (geom is LineString) {
            val ss = RelateSegmentString.createLine(geom.getCoordinates(), isA, elementId, this)
            segStrings.add(ss)
        } else if (geom is Polygon) {
            val parentPoly: Geometry = parentPolygonal ?: geom
            extractRingToSegmentString(isA, geom.getExteriorRing(), 0, env, parentPoly, segStrings)
            for (i in 0 until geom.getNumInteriorRing()) {
                extractRingToSegmentString(isA, geom.getInteriorRingN(i), i + 1, env, parentPoly, segStrings)
            }
        }
    }

    private fun extractRingToSegmentString(
        isA: Boolean,
        ring: LinearRing,
        ringId: Int,
        env: Envelope?,
        parentPoly: Geometry,
        segStrings: MutableList<RelateSegmentString>
    ) {
        if (ring.isEmpty())
            return
        if (env != null && !env.intersects(ring.getEnvelopeInternal()))
            return

        //-- orient the points if required
        val requireCW = ringId == 0
        val pts = orient(ring.getCoordinates(), requireCW)
        val ss = RelateSegmentString.createRing(pts, isA, elementId, ringId, parentPoly, this)
        segStrings.add(ss)
    }

    override fun toString(): String {
        return geom.toString()
    }

    companion object {
        const val GEOM_A = true
        const val GEOM_B = false

        fun name(isA: Boolean): String {
            return if (isA) "A" else "B"
        }

        /**
         * Tests if all geometry linear elements are zero-length.
         * For efficiency the test avoids computing actual length.
         */
        private fun isZeroLength(geom: Geometry): Boolean {
            val geomi = GeometryCollectionIterator(geom)
            while (geomi.hasNext()) {
                val elem = geomi.next() as Geometry
                if (elem is LineString) {
                    if (!isZeroLength(elem))
                        return false
                }
            }
            return true
        }

        private fun isZeroLength(line: LineString): Boolean {
            if (line.getNumPoints() >= 2) {
                val p0 = line.getCoordinateN(0)
                for (i in 0 until line.getNumPoints()) {
                    val pi = line.getCoordinateN(i)
                    //-- most non-zero-len lines will trigger this right away
                    if (!p0.equals2D(pi))
                        return false
                }
            }
            return true
        }

        fun orient(pts: Array<Coordinate>, orientCW: Boolean): Array<Coordinate> {
            var ptsLocal = pts
            val isFlipped = orientCW == Orientation.isCCW(pts)
            if (isFlipped) {
                ptsLocal = pts.copyOf()
                CoordinateArrays.reverse(ptsLocal)
            }
            return ptsLocal
        }
    }
}
