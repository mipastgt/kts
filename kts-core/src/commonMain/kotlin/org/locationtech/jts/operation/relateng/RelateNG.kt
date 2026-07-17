/*
 * Copyright (c) 2024 Martin Davis.
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

import kotlin.jvm.JvmStatic

import org.locationtech.jts.algorithm.BoundaryNodeRule
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Dimension
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryCollectionIterator
import org.locationtech.jts.geom.IntersectionMatrix
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.LinearRing
import org.locationtech.jts.geom.Location
import org.locationtech.jts.geom.Polygon
import org.locationtech.jts.noding.MCIndexSegmentSetMutualIntersector

/**
 * Computes the value of topological predicates between two geometries based on the
 * Dimensionally-Extended 9-Intersection Model (DE-9IM).
 * Standard and custom topological predicates are provided by [RelatePredicate].
 *
 * @author Martin Davis
 *
 */
class RelateNG private constructor(inputA: Geometry, isPrepared: Boolean, bnRule: BoundaryNodeRule) {

    private val boundaryNodeRule: BoundaryNodeRule = bnRule
    private val geomA: RelateGeometry = RelateGeometry(inputA, isPrepared, boundaryNodeRule)
    private var edgeMutualInt: MCIndexSegmentSetMutualIntersector? = null

    private constructor(inputA: Geometry, isPrepared: Boolean) :
        this(inputA, isPrepared, BoundaryNodeRule.OGC_SFS_BOUNDARY_RULE)

    /**
     * Computes the DE-9IM matrix for the topological relationship to a geometry.
     *
     * @param b the B geometry to test against
     * @return the DE-9IM matrix
     */
    fun evaluate(b: Geometry): IntersectionMatrix {
        val rel = RelateMatrixPredicate()
        evaluate(b, rel)
        return rel.getIM()
    }

    /**
     * Tests whether the topological relationship to a geometry
     * matches a DE-9IM matrix pattern.
     *
     * @param b the B geometry to test against
     * @param imPattern the DE-9IM pattern to match
     * @return true if the geometries' topological relationship matches the DE-9IM pattern
     */
    fun evaluate(b: Geometry, imPattern: String): Boolean {
        return evaluate(b, RelatePredicate.matches(imPattern))
    }

    /**
     * Tests whether the topological relationship to a geometry
     * satisfies a topology predicate.
     *
     * @param b the B geometry to test against
     * @param predicate the topological predicate
     * @return true if the predicate is satisfied
     */
    fun evaluate(b: Geometry, predicate: TopologyPredicate): Boolean {
        //-- fast envelope checks
        if (!hasRequiredEnvelopeInteraction(b, predicate)) {
            return false
        }

        val geomB = RelateGeometry(b, boundaryNodeRule)

        if (geomA.isEmpty() && geomB.isEmpty()) {
            //TODO: what if predicate is disjoint?  Perhaps use result on disjoint envs?
            return finishValue(predicate)
        }
        val dimA = geomA.getDimensionReal()
        val dimB = geomB.getDimensionReal()

        //-- check if predicate is determined by dimension or envelope
        predicate.init(dimA, dimB)
        if (predicate.isKnown())
            return finishValue(predicate)

        predicate.init(geomA.getEnvelope(), geomB.getEnvelope())
        if (predicate.isKnown())
            return finishValue(predicate)

        val topoComputer = TopologyComputer(predicate, geomA, geomB)

        //-- optimized P/P evaluation
        if (dimA == Dimension.P && dimB == Dimension.P) {
            computePP(geomB, topoComputer)
            topoComputer.finish()
            return topoComputer.getResult()
        }

        //-- test points against (potentially) indexed geometry first
        computeAtPoints(geomB, RelateGeometry.GEOM_B, geomA, topoComputer)
        if (topoComputer.isResultKnown()) {
            return topoComputer.getResult()
        }
        computeAtPoints(geomA, RelateGeometry.GEOM_A, geomB, topoComputer)
        if (topoComputer.isResultKnown()) {
            return topoComputer.getResult()
        }

        if (geomA.hasEdges() && geomB.hasEdges()) {
            computeAtEdges(geomB, topoComputer)
        }

        //-- after all processing, set remaining unknown values in IM
        topoComputer.finish()
        return topoComputer.getResult()
    }

    private fun hasRequiredEnvelopeInteraction(b: Geometry, predicate: TopologyPredicate): Boolean {
        val envB = b.getEnvelopeInternal()
        var isInteracts = false
        if (predicate.requireCovers(RelateGeometry.GEOM_A)) {
            if (!geomA.getEnvelope().covers(envB)) {
                return false
            }
            isInteracts = true
        } else if (predicate.requireCovers(RelateGeometry.GEOM_B)) {
            if (!envB.covers(geomA.getEnvelope())) {
                return false
            }
            isInteracts = true
        }
        if (!isInteracts &&
            predicate.requireInteraction() &&
            !geomA.getEnvelope().intersects(envB)
        ) {
            return false
        }
        return true
    }

    private fun finishValue(predicate: TopologyPredicate): Boolean {
        predicate.finish()
        return predicate.value()
    }

    /**
     * An optimized algorithm for evaluating P/P cases.
     * It tests one point set against the other.
     */
    private fun computePP(geomB: RelateGeometry, topoComputer: TopologyComputer) {
        val ptsA = geomA.getUniquePoints()
        //TODO: only query points in interaction extent?
        val ptsB = geomB.getUniquePoints()

        var numBinA = 0
        for (ptB in ptsB) {
            if (ptsA.contains(ptB)) {
                numBinA++
                topoComputer.addPointOnPointInterior(ptB)
            } else {
                topoComputer.addPointOnPointExterior(RelateGeometry.GEOM_B, ptB)
            }
            if (topoComputer.isResultKnown()) {
                return
            }
        }
        /*
         * If number of matched B points is less than size of A,
         * there must be at least one A point in the exterior of B
         */
        if (numBinA < ptsA.size) {
            //TODO: determine actual exterior point?
            topoComputer.addPointOnPointExterior(RelateGeometry.GEOM_A, null)
        }
    }

    private fun computeAtPoints(
        geom: RelateGeometry,
        isA: Boolean,
        geomTarget: RelateGeometry,
        topoComputer: TopologyComputer
    ) {
        var isResultKnown = computePoints(geom, isA, geomTarget, topoComputer)
        if (isResultKnown)
            return

        /**
         * Performance optimization: only check points against target
         * if it has areas OR if the predicate requires checking for
         * exterior interaction.
         */
        val checkDisjointPoints = geomTarget.hasDimension(Dimension.A) ||
            topoComputer.isExteriorCheckRequired(isA)
        if (!checkDisjointPoints)
            return

        isResultKnown = computeLineEnds(geom, isA, geomTarget, topoComputer)
        if (isResultKnown)
            return

        computeAreaVertex(geom, isA, geomTarget, topoComputer)
    }

    private fun computePoints(
        geom: RelateGeometry,
        isA: Boolean,
        geomTarget: RelateGeometry,
        topoComputer: TopologyComputer
    ): Boolean {
        if (!geom.hasDimension(Dimension.P)) {
            return false
        }

        val points = geom.getEffectivePoints()
        for (point in points) {
            //TODO: exit when all possible target locations (E,I,B) have been found?
            if (point.isEmpty())
                continue

            val pt = point.getCoordinate()
            computePoint(isA, pt!!, geomTarget, topoComputer)
            if (topoComputer.isResultKnown()) {
                return true
            }
        }
        return false
    }

    private fun computePoint(isA: Boolean, pt: Coordinate, geomTarget: RelateGeometry, topoComputer: TopologyComputer) {
        val locDimTarget = geomTarget.locateWithDim(pt)
        val locTarget = DimensionLocation.location(locDimTarget)
        val dimTarget = DimensionLocation.dimension(locDimTarget, topoComputer.getDimension(!isA))
        topoComputer.addPointOnGeometry(isA, locTarget, dimTarget, pt)
    }

    private fun computeLineEnds(
        geom: RelateGeometry,
        isA: Boolean,
        geomTarget: RelateGeometry,
        topoComputer: TopologyComputer
    ): Boolean {
        if (!geom.hasDimension(Dimension.L)) {
            return false
        }

        var hasExteriorIntersection = false
        val geomi = GeometryCollectionIterator(geom.getGeometry())
        while (geomi.hasNext()) {
            val elem = geomi.next() as Geometry
            if (elem.isEmpty())
                continue

            if (elem is LineString) {
                //-- once an intersection with target exterior is recorded, skip further known-exterior points
                if (hasExteriorIntersection &&
                    elem.getEnvelopeInternal().disjoint(geomTarget.getEnvelope())
                )
                    continue

                val e0 = elem.getCoordinateN(0)
                hasExteriorIntersection =
                    hasExteriorIntersection or computeLineEnd(geom, isA, e0, geomTarget, topoComputer)
                if (topoComputer.isResultKnown()) {
                    return true
                }

                if (!elem.isClosed()) {
                    val e1 = elem.getCoordinateN(elem.getNumPoints() - 1)
                    hasExteriorIntersection =
                        hasExteriorIntersection or computeLineEnd(geom, isA, e1, geomTarget, topoComputer)
                    if (topoComputer.isResultKnown()) {
                        return true
                    }
                }
                //TODO: break when all possible locations have been found?
            }
        }
        return false
    }

    /**
     * Compute the topology of a line endpoint.
     *
     * @return true if the line endpoint is in the exterior of the target
     */
    private fun computeLineEnd(
        geom: RelateGeometry,
        isA: Boolean,
        pt: Coordinate,
        geomTarget: RelateGeometry,
        topoComputer: TopologyComputer
    ): Boolean {
        val locDimLineEnd = geom.locateLineEndWithDim(pt)
        val dimLineEnd = DimensionLocation.dimension(locDimLineEnd, topoComputer.getDimension(isA))
        //-- skip line ends which are in a GC area
        if (dimLineEnd != Dimension.L)
            return false
        val locLineEnd = DimensionLocation.location(locDimLineEnd)

        val locDimTarget = geomTarget.locateWithDim(pt)
        val locTarget = DimensionLocation.location(locDimTarget)
        val dimTarget = DimensionLocation.dimension(locDimTarget, topoComputer.getDimension(!isA))
        topoComputer.addLineEndOnGeometry(isA, locLineEnd, locTarget, dimTarget, pt)
        return locTarget == Location.EXTERIOR
    }

    private fun computeAreaVertex(
        geom: RelateGeometry,
        isA: Boolean,
        geomTarget: RelateGeometry,
        topoComputer: TopologyComputer
    ): Boolean {
        if (!geom.hasDimension(Dimension.A)) {
            return false
        }
        //-- evaluate for line and area targets only, since points are handled in the reverse direction
        if (geomTarget.getDimension() < Dimension.L)
            return false

        var hasExteriorIntersection = false
        val geomi = GeometryCollectionIterator(geom.getGeometry())
        while (geomi.hasNext()) {
            val elem = geomi.next() as Geometry
            if (elem.isEmpty())
                continue

            if (elem is Polygon) {
                //-- once an intersection with target exterior is recorded, skip further known-exterior points
                if (hasExteriorIntersection &&
                    elem.getEnvelopeInternal().disjoint(geomTarget.getEnvelope())
                )
                    continue

                hasExteriorIntersection =
                    hasExteriorIntersection or computeAreaVertex(geom, isA, elem.getExteriorRing(), geomTarget, topoComputer)
                if (topoComputer.isResultKnown()) {
                    return true
                }
                for (j in 0 until elem.getNumInteriorRing()) {
                    hasExteriorIntersection =
                        hasExteriorIntersection or computeAreaVertex(geom, isA, elem.getInteriorRingN(j), geomTarget, topoComputer)
                    if (topoComputer.isResultKnown()) {
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun computeAreaVertex(
        geom: RelateGeometry,
        isA: Boolean,
        ring: LinearRing,
        geomTarget: RelateGeometry,
        topoComputer: TopologyComputer
    ): Boolean {
        //TODO: use extremal (highest) point to ensure one is on boundary of polygon cluster
        val pt = ring.getCoordinate()!!

        val locArea = geom.locateAreaVertex(pt)
        val locDimTarget = geomTarget.locateWithDim(pt)
        val locTarget = DimensionLocation.location(locDimTarget)
        val dimTarget = DimensionLocation.dimension(locDimTarget, topoComputer.getDimension(!isA))
        topoComputer.addAreaVertex(isA, locArea, locTarget, dimTarget, pt)
        return locTarget == Location.EXTERIOR
    }

    private fun computeAtEdges(geomB: RelateGeometry, topoComputer: TopologyComputer) {
        val envInt = geomA.getEnvelope().intersection(geomB.getEnvelope())
        if (envInt.isNull())
            return

        val edgesB = geomB.extractSegmentStrings(RelateGeometry.GEOM_B, envInt)
        val intersector = EdgeSegmentIntersector(topoComputer)

        if (topoComputer.isSelfNodingRequired()) {
            computeEdgesAll(edgesB, envInt, intersector)
        } else {
            computeEdgesMutual(edgesB, envInt, intersector)
        }
        if (topoComputer.isResultKnown()) {
            return
        }

        topoComputer.evaluateNodes()
    }

    private fun computeEdgesAll(edgesB: List<RelateSegmentString>, envInt: Envelope, intersector: EdgeSegmentIntersector) {
        //TODO: find a way to reuse prepared index?
        val edgesA = geomA.extractSegmentStrings(RelateGeometry.GEOM_A, envInt)

        val edgeInt = EdgeSetIntersector(edgesA, edgesB, envInt)
        edgeInt.process(intersector)
    }

    private fun computeEdgesMutual(edgesB: List<RelateSegmentString>, envInt: Envelope, intersector: EdgeSegmentIntersector) {
        //-- in prepared mode the A edge index is reused
        if (edgeMutualInt == null) {
            val envExtract = if (geomA.isPrepared()) null else envInt
            val edgesA = geomA.extractSegmentStrings(RelateGeometry.GEOM_A, envExtract)
            edgeMutualInt = MCIndexSegmentSetMutualIntersector(edgesA, envExtract)
        }

        edgeMutualInt!!.process(edgesB, intersector)
    }

    companion object {
        /**
         * Tests whether the topological relationship between two geometries
         * satisfies a topological predicate.
         */
        @JvmStatic
        fun relate(a: Geometry, b: Geometry, pred: TopologyPredicate): Boolean {
            val rng = RelateNG(a, false)
            return rng.evaluate(b, pred)
        }

        /**
         * Tests whether the topological relationship between two geometries
         * satisfies a topological predicate, using a given [BoundaryNodeRule].
         */
        @JvmStatic
        fun relate(a: Geometry, b: Geometry, pred: TopologyPredicate, bnRule: BoundaryNodeRule): Boolean {
            val rng = RelateNG(a, false, bnRule)
            return rng.evaluate(b, pred)
        }

        /**
         * Tests whether the topological relationship to a geometry
         * matches a DE-9IM matrix pattern.
         */
        @JvmStatic
        fun relate(a: Geometry, b: Geometry, imPattern: String): Boolean {
            val rng = RelateNG(a, false)
            return rng.evaluate(b, imPattern)
        }

        /**
         * Computes the DE-9IM matrix
         * for the topological relationship between two geometries.
         */
        @JvmStatic
        fun relate(a: Geometry, b: Geometry): IntersectionMatrix {
            val rng = RelateNG(a, false)
            return rng.evaluate(b)
        }

        /**
         * Computes the DE-9IM matrix
         * for the topological relationship between two geometries.
         */
        @JvmStatic
        fun relate(a: Geometry, b: Geometry, bnRule: BoundaryNodeRule): IntersectionMatrix {
            val rng = RelateNG(a, false, bnRule)
            return rng.evaluate(b)
        }

        /**
         * Creates a prepared RelateNG instance to optimize the
         * evaluation of relationships against a single geometry.
         */
        @JvmStatic
        fun prepare(a: Geometry): RelateNG {
            return RelateNG(a, true)
        }

        /**
         * Creates a prepared RelateNG instance to optimize the
         * computation of predicates against a single geometry,
         * using a given [BoundaryNodeRule].
         */
        @JvmStatic
        fun prepare(a: Geometry, bnRule: BoundaryNodeRule): RelateNG {
            return RelateNG(a, true, bnRule)
        }
    }
}
