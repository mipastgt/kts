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
package org.locationtech.jts.operation.union

import kotlin.jvm.JvmStatic

import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Puntal

/**
 * Unions a `Collection` of [Geometry]s or a single Geometry
 * (which may be a [org.locationtech.jts.geom.GeometryCollection]) together.
 * By using this special-purpose operation over a collection of geometries
 * it is possible to take advantage of various optimizations to improve performance.
 * Heterogeneous [org.locationtech.jts.geom.GeometryCollection]s are fully supported.
 *
 *
 * The result obeys the following contract:
 *
 *  * Unioning a set of [org.locationtech.jts.geom.Polygon]s has the effect of
 * merging the areas (i.e. the same effect as
 * iteratively unioning all individual polygons together).
 *
 *  * Unioning a set of [org.locationtech.jts.geom.LineString]s has the effect of **noding**
 * and **dissolving** the input linework.
 *
 *  * Unioning a set of [org.locationtech.jts.geom.Point]s has the effect of merging
 * all identical points (producing a set with no duplicates).
 *
 *
 * `UnaryUnion` always operates on the individual components of MultiGeometries.
 * So it is possible to use it to "clean" invalid self-intersecting MultiPolygons
 * (although the polygon components must all still be individually valid.)
 *
 * @author mbdavis
 *
 */
class UnaryUnionOp {

    private var geomFact: GeometryFactory? = null

    private lateinit var extracter: InputExtracter
    private var unionFunction: UnionStrategy = CascadedPolygonUnion.CLASSIC_UNION

    /**
     * Constructs a unary union operation for a `Collection`
     * of [Geometry]s.
     *
     * @param geoms a collection of geometries
     * @param geomFact the geometry factory to use if the collection is empty
     */
    constructor(geoms: Collection<*>?, geomFact: GeometryFactory) {
        this.geomFact = geomFact
        extract(geoms)
    }

    /**
     * Constructs a unary union operation for a `Collection`
     * of [Geometry]s, using the [GeometryFactory]
     * of the input geometries.
     *
     * @param geoms a collection of geometries
     */
    constructor(geoms: Collection<*>?) {
        extract(geoms)
    }

    /**
     * Constructs a unary union operation for a [Geometry]
     * (which may be a [org.locationtech.jts.geom.GeometryCollection]).
     * @param geom
     */
    constructor(geom: Geometry) {
        extract(geom)
    }

    fun setUnionFunction(unionFun: UnionStrategy) {
        this.unionFunction = unionFun
    }

    private fun extract(geoms: Collection<*>?) {
        extracter = InputExtracter.extract(geoms)
    }

    private fun extract(geom: Geometry) {
        extracter = InputExtracter.extract(geom)
    }

    /**
     * Gets the union of the input geometries.
     *
     *
     * The result of empty input is determined as follows:
     *
     *  1. If the input is empty and a dimension can be
     * determined (i.e. an empty geometry is present),
     * an empty atomic geometry of that dimension is returned.
     *  1. If no input geometries were provided but a [GeometryFactory] was provided,
     * an empty [org.locationtech.jts.geom.GeometryCollection] is returned.
     *  1. Otherwise, the return value is `null`.
     *
     *
     * @return a Geometry containing the union,
     * or an empty atomic geometry, or an empty GEOMETRYCOLLECTION,
     * or `null` if no GeometryFactory was provided
     */
    fun union(): Geometry? {
        if (geomFact == null)
            geomFact = extracter.getFactory()

        // Case 3
        if (geomFact == null) {
            return null
        }

        // Case 1 & 2
        if (extracter.isEmpty()) {
            return geomFact!!.createEmpty(extracter.getDimension())
        }
        val points = extracter.getExtract(0)
        val lines = extracter.getExtract(1)
        val polygons = extracter.getExtract(2)

        /**
         * For points and lines, only a single union operation is
         * required, since the OGC model allows self-intersecting
         * MultiPoint and MultiLineStrings.
         * This is not the case for polygons, so Cascaded Union is required.
         */
        var unionPoints: Geometry? = null
        if (points.size > 0) {
            val ptGeom = geomFact!!.buildGeometry(points)
            unionPoints = unionNoOpt(ptGeom)
        }

        var unionLines: Geometry? = null
        if (lines.size > 0) {
            val lineGeom = geomFact!!.buildGeometry(lines)
            unionLines = unionNoOpt(lineGeom)
        }

        var unionPolygons: Geometry? = null
        if (polygons.size > 0) {
            unionPolygons = CascadedPolygonUnion.union(polygons, unionFunction)
        }

        /**
         * Performing two unions is somewhat inefficient,
         * but is mitigated by unioning lines and points first
         */
        val unionLA = unionWithNull(unionLines, unionPolygons)
        val union: Geometry? =
            if (unionPoints == null)
                unionLA
            else if (unionLA == null)
                unionPoints
            else
                PointGeometryUnion.union(unionPoints as Puntal, unionLA)

        if (union == null)
            return geomFact!!.createGeometryCollection()

        return union
    }

    /**
     * Computes the union of two geometries,
     * either of both of which may be null.
     *
     * @param g0 a Geometry
     * @param g1 a Geometry
     * @return the union of the input(s)
     * or null if both inputs are null
     */
    private fun unionWithNull(g0: Geometry?, g1: Geometry?): Geometry? {
        if (g0 == null && g1 == null)
            return null

        if (g1 == null)
            return g0
        if (g0 == null)
            return g1

        return g0.union(g1)
    }

    /**
     * Computes a unary union with no extra optimization,
     * and no short-circuiting.
     * Due to the way the overlay operations
     * are implemented, this is still efficient in the case of linear
     * and puntal geometries.
     * Uses robust version of overlay operation
     * to ensure identical behaviour to the `union(Geometry)` operation.
     *
     * @param g0 a geometry
     * @return the union of the input geometry
     */
    private fun unionNoOpt(g0: Geometry): Geometry {
        val empty = geomFact!!.createPoint()
        return unionFunction.union(g0, empty)
    }

    companion object {
        /**
         * Computes the geometric union of a [Collection]
         * of [Geometry]s.
         *
         * @param geoms a collection of geometries
         * @return the union of the geometries,
         * or `null` if the input is empty
         */
        @JvmStatic
        fun union(geoms: Collection<*>?): Geometry? {
            val op = UnaryUnionOp(geoms)
            return op.union()
        }

        /**
         * Computes the geometric union of a [Collection]
         * of [Geometry]s.
         *
         * If no input geometries were provided but a [GeometryFactory] was provided,
         * an empty [org.locationtech.jts.geom.GeometryCollection] is returned.
         *
         * @param geoms a collection of geometries
         * @param geomFact the geometry factory to use if the collection is empty
         * @return the union of the geometries,
         * or an empty GEOMETRYCOLLECTION
         */
        @JvmStatic
        fun union(geoms: Collection<*>?, geomFact: GeometryFactory): Geometry? {
            val op = UnaryUnionOp(geoms, geomFact)
            return op.union()
        }

        /**
         * Constructs a unary union operation for a [Geometry]
         * (which may be a [org.locationtech.jts.geom.GeometryCollection]).
         *
         * @param geom a geometry to union
         * @return the union of the elements of the geometry
         * or an empty GEOMETRYCOLLECTION
         */
        @JvmStatic
        fun union(geom: Geometry): Geometry {
            val op = UnaryUnionOp(geom)
            return op.union()!!
        }
    }
}
