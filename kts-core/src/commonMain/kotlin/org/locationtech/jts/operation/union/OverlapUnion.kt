/*
 * Copyright (c) 2019 Martin Davis.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.locationtech.jts.operation.union

import kotlin.jvm.JvmStatic

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.CoordinateSequence
import org.locationtech.jts.geom.CoordinateSequenceFilter
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LineSegment
import org.locationtech.jts.geom.util.GeometryCombiner

/**
 * Unions MultiPolygons efficiently by
 * using full topological union only for polygons which may overlap,
 * and combining with the remaining polygons.
 * Polygons which may overlap are those which intersect the common extent of the inputs.
 * Polygons wholly outside this extent must be disjoint to the computed union.
 * They can thus be simply combined with the union result,
 * which is much more performant.
 *
 * @author mbdavis
 *
 */
@Deprecated("due to impairing performance")
class OverlapUnion {

    private val geomFactory: GeometryFactory

    private val g0: Geometry
    private val g1: Geometry

    private var isUnionSafe = false

    private var unionFun: UnionStrategy

    /**
     * Creates a new instance for unioning the given geometries.
     *
     * @param g0 a geometry to union
     * @param g1 a geometry to union
     */
    constructor(g0: Geometry, g1: Geometry) : this(g0, g1, CascadedPolygonUnion.CLASSIC_UNION)

    constructor(g0: Geometry, g1: Geometry, unionFun: UnionStrategy) {
        this.g0 = g0
        this.g1 = g1
        geomFactory = g0.getFactory()
        this.unionFun = unionFun
    }

    /**
     * Unions the input geometries,
     * using the more performant overlap union algorithm if possible.
     *
     * @return the union of the inputs
     */
    fun union(): Geometry? {
        val overlapEnv = overlapEnvelope(g0, g1)

        /**
         * If no overlap, can just combine the geometries
         */
        if (overlapEnv.isNull()) {
            val g0Copy = g0.copy()
            val g1Copy = g1.copy()
            return GeometryCombiner.combine(g0Copy, g1Copy)
        }

        val disjointPolys = ArrayList<Geometry>()

        val g0Overlap = extractByEnvelope(overlapEnv, g0, disjointPolys)
        val g1Overlap = extractByEnvelope(overlapEnv, g1, disjointPolys)

        val unionGeom = unionFull(g0Overlap, g1Overlap)

        val result: Geometry?
        isUnionSafe = isBorderSegmentsSame(unionGeom, overlapEnv)
        if (!isUnionSafe) {
            // overlap union changed border segments... need to do full union
            result = unionFull(g0, g1)
        } else {
            result = combine(unionGeom, disjointPolys)
        }
        return result
    }

    /**
     * Allows checking whether the optimized
     * or full union was performed.
     * Used for unit testing.
     *
     * @return true if the optimized union was performed
     */
    fun isUnionOptimized(): Boolean {
        return isUnionSafe
    }

    private fun combine(unionGeom: Geometry, disjointPolys: MutableList<Geometry>): Geometry? {
        if (disjointPolys.size <= 0)
            return unionGeom

        disjointPolys.add(unionGeom)
        val result = GeometryCombiner.combine(disjointPolys)
        return result
    }

    private fun extractByEnvelope(
        env: Envelope,
        geom: Geometry,
        disjointGeoms: MutableList<Geometry>
    ): Geometry {
        val intersectingGeoms = ArrayList<Geometry>()
        for (i in 0 until geom.getNumGeometries()) {
            val elem = geom.getGeometryN(i)
            if (elem.getEnvelopeInternal().intersects(env)) {
                intersectingGeoms.add(elem)
            } else {
                val copy = elem.copy()
                disjointGeoms.add(copy)
            }
        }
        return geomFactory.buildGeometry(intersectingGeoms)
    }

    private fun unionFull(geom0: Geometry, geom1: Geometry): Geometry {
        // if both are empty collections, just return a copy of one of them
        if (geom0.getNumGeometries() == 0 &&
            geom1.getNumGeometries() == 0
        )
            return geom0.copy()

        val union = unionFun.union(geom0, geom1)
        return union
    }

    private fun isBorderSegmentsSame(result: Geometry, env: Envelope): Boolean {
        val segsBefore = extractBorderSegments(g0, g1, env)

        val segsAfter = ArrayList<LineSegment>()
        extractBorderSegments(result, env, segsAfter)

        return isEqual(segsBefore, segsAfter)
    }

    private fun isEqual(segs0: List<LineSegment>, segs1: List<LineSegment>): Boolean {
        if (segs0.size != segs1.size)
            return false

        val segIndex = HashSet<LineSegment>(segs0)

        for (seg in segs1) {
            if (!segIndex.contains(seg)) {
                return false
            }
        }
        return true
    }

    private fun extractBorderSegments(geom0: Geometry, geom1: Geometry?, env: Envelope): List<LineSegment> {
        val segs = ArrayList<LineSegment>()
        extractBorderSegments(geom0, env, segs)
        if (geom1 != null)
            extractBorderSegments(geom1, env, segs)
        return segs
    }

    companion object {
        /**
         * Union a pair of geometries,
         * using the more performant overlap union algorithm if possible.
         *
         * @param g0 a geometry to union
         * @param g1 a geometry to union
         * @param unionFun
         * @return the union of the inputs
         */
        @JvmStatic
        fun union(g0: Geometry, g1: Geometry, unionFun: UnionStrategy): Geometry? {
            val union = OverlapUnion(g0, g1, unionFun)
            return union.union()
        }

        private fun overlapEnvelope(g0: Geometry, g1: Geometry): Envelope {
            val g0Env = g0.getEnvelopeInternal()
            val g1Env = g1.getEnvelopeInternal()
            val overlapEnv = g0Env.intersection(g1Env)
            return overlapEnv
        }

        private fun intersects(env: Envelope, p0: Coordinate, p1: Coordinate): Boolean {
            return env.intersects(p0) || env.intersects(p1)
        }

        private fun containsProperly(env: Envelope, p0: Coordinate, p1: Coordinate): Boolean {
            return containsProperly(env, p0) && containsProperly(env, p1)
        }

        private fun containsProperly(env: Envelope, p: Coordinate): Boolean {
            if (env.isNull()) return false
            return p.getX() > env.getMinX() &&
                p.getX() < env.getMaxX() &&
                p.getY() > env.getMinY() &&
                p.getY() < env.getMaxY()
        }

        private fun extractBorderSegments(geom: Geometry, env: Envelope, segs: MutableList<LineSegment>) {
            geom.apply(object : CoordinateSequenceFilter {

                override fun filter(seq: CoordinateSequence, i: Int) {
                    if (i <= 0) return

                    // extract LineSegment
                    val p0 = seq.getCoordinate(i - 1)
                    val p1 = seq.getCoordinate(i)
                    val isBorder = intersects(env, p0, p1) && !containsProperly(env, p0, p1)
                    if (isBorder) {
                        val seg = LineSegment(p0, p1)
                        segs.add(seg)
                    }
                }

                override fun isDone(): Boolean {
                    return false
                }

                override fun isGeometryChanged(): Boolean {
                    return false
                }
            })
        }
    }
}
