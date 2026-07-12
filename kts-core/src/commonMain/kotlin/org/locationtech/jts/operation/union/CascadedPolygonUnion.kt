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
import kotlin.jvm.JvmField

import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Polygon
import org.locationtech.jts.geom.Polygonal
import org.locationtech.jts.geom.TopologyException
import org.locationtech.jts.geom.util.PolygonExtracter
import org.locationtech.jts.index.strtree.STRtree
import org.locationtech.jts.operation.overlay.snap.SnapIfNeededOverlayOp
import org.locationtech.jts.operation.overlayng.OverlayNG
import org.locationtech.jts.operation.overlayng.OverlayNGRobust

/**
 * Provides an efficient method of unioning a collection of
 * [Polygonal] geometries.
 * The geometries are indexed using a spatial index,
 * and unioned recursively in index order.
 * For geometries with a high degree of overlap,
 * this has the effect of reducing the number of vertices
 * early in the process, which increases speed
 * and robustness.
 *
 *
 * This algorithm is faster and more robust than
 * the simple iterated approach of
 * repeatedly unioning each polygon to a result geometry.
 *
 * @author Martin Davis
 *
 */
class CascadedPolygonUnion {

    private var inputPolys: MutableCollection<*>?
    private var geomFactory: GeometryFactory? = null
    private var unionFun: UnionStrategy

    private var countRemainder = 0
    private var countInput = 0

    /**
     * Creates a new instance to union
     * the given collection of [Geometry]s.
     *
     * @param polys a collection of [Polygonal] [Geometry]s
     */
    constructor(polys: MutableCollection<*>?) : this(polys, CLASSIC_UNION)

    /**
     * Creates a new instance to union
     * the given collection of [Geometry]s.
     *
     * @param polys a collection of [Polygonal] [Geometry]s
     */
    constructor(polys: MutableCollection<*>?, unionFun: UnionStrategy) {
        this.inputPolys = polys
        this.unionFun = unionFun
        // guard against null input
        if (inputPolys == null)
            inputPolys = ArrayList<Any?>()
        this.countInput = inputPolys!!.size
        this.countRemainder = countInput
    }

    /**
     * Computes the union of the input geometries.
     *
     *
     * This method discards the input geometries as they are processed.
     * In many input cases this reduces the memory retained
     * as the operation proceeds.
     * Optimal memory usage is achieved
     * by disposing of the original input collection
     * before calling this method.
     *
     * @return the union of the input geometries
     * or null if no input geometries were provided
     * @throws IllegalStateException if this method is called more than once
     */
    fun union(): Geometry? {
        if (inputPolys == null)
            throw IllegalStateException("union() method cannot be called twice")
        if (inputPolys!!.isEmpty())
            return null
        geomFactory = (inputPolys!!.iterator().next() as Geometry).getFactory()

        /**
         * A spatial index to organize the collection
         * into groups of close geometries.
         * This makes unioning more efficient, since vertices are more likely
         * to be eliminated on each round.
         */
        val index = STRtree(STRTREE_NODE_CAPACITY)
        val i = inputPolys!!.iterator()
        while (i.hasNext()) {
            val item = i.next() as Geometry
            index.insert(item.getEnvelopeInternal(), item)
        }
        // To avoiding holding memory remove references to the input geometries,
        inputPolys = null

        val itemTree = index.itemsTree()
        val unionAll = unionTree(itemTree)
        return unionAll
    }

    private fun unionTree(geomTree: List<*>?): Geometry? {
        /**
         * Recursively unions all subtrees in the list into single geometries.
         * The result is a list of Geometrys only
         */
        val geoms = reduceToGeometries(geomTree)
        val union = binaryUnion(geoms)

        return union
    }

    /**
     * Unions a list of geometries
     * by treating the list as a flattened binary tree,
     * and performing a cascaded union on the tree.
     */
    private fun binaryUnion(geoms: List<Geometry?>): Geometry? {
        return binaryUnion(geoms, 0, geoms.size)
    }

    /**
     * Unions a section of a list using a recursive binary union on each half
     * of the section.
     *
     * @param geoms the list of geometries containing the section to union
     * @param start the start index of the section
     * @param end the index after the end of the section
     * @return the union of the list section
     */
    private fun binaryUnion(geoms: List<Geometry?>, start: Int, end: Int): Geometry? {
        if (end - start <= 1) {
            val g0 = getGeometry(geoms, start)
            return unionSafe(g0, null)
        } else if (end - start == 2) {
            return unionSafe(getGeometry(geoms, start), getGeometry(geoms, start + 1))
        } else {
            // recurse on both halves of the list
            val mid = (end + start) / 2
            val g0 = binaryUnion(geoms, start, mid)
            val g1 = binaryUnion(geoms, mid, end)
            return unionSafe(g0, g1)
        }
    }

    /**
     * Reduces a tree of geometries to a list of geometries
     * by recursively unioning the subtrees in the list.
     *
     * @param geomTree a tree-structured list of geometries
     * @return a list of Geometrys
     */
    private fun reduceToGeometries(geomTree: List<*>?): MutableList<Geometry?> {
        val geoms = ArrayList<Geometry?>()
        val i = geomTree!!.iterator()
        while (i.hasNext()) {
            val o = i.next()
            var geom: Geometry? = null
            if (o is List<*>) {
                geom = unionTree(o)
            } else if (o is Geometry) {
                geom = o
            }
            geoms.add(geom)
        }
        return geoms
    }

    /**
     * Computes the union of two geometries,
     * either or both of which may be null.
     *
     * @param g0 a Geometry
     * @param g1 a Geometry
     * @return the union of the input(s)
     * or null if both inputs are null
     */
    private fun unionSafe(g0: Geometry?, g1: Geometry?): Geometry? {
        if (g0 == null && g1 == null)
            return null

        if (g0 == null)
            return g1!!.copy()
        if (g1 == null)
            return g0.copy()

        countRemainder--

        val union = unionActual(g0, g1)

        return union
    }

    /**
     * Encapsulates the actual unioning of two polygonal geometries.
     *
     * @param g0
     * @param g1
     * @return
     */
    private fun unionActual(g0: Geometry, g1: Geometry): Geometry {
        val union = unionFun.union(g0, g1)
        val unionPoly = restrictToPolygons(union)
        return unionPoly
    }

    companion object {
        /**
         * A union strategy that uses the classic JTS [SnapIfNeededOverlayOp],
         * with a robustness fallback to OverlayNG.
         */
        @JvmField
        internal val CLASSIC_UNION: UnionStrategy = object : UnionStrategy {
            override fun union(g0: Geometry, g1: Geometry): Geometry {
                return try {
                    SnapIfNeededOverlayOp.union(g0, g1)
                } catch (ex: TopologyException) {
                    OverlayNGRobust.overlay(g0, g1, OverlayNG.UNION)
                }
            }

            override fun isFloatingPrecision(): Boolean {
                return true
            }
        }

        /**
         * Computes the union of
         * a collection of [Polygonal] [Geometry]s.
         *
         * @param polys a collection of [Polygonal] [Geometry]s
         */
        @JvmStatic
        fun union(polys: MutableCollection<*>?): Geometry? {
            val op = CascadedPolygonUnion(polys)
            return op.union()
        }

        /**
         * Computes the union of
         * a collection of [Polygonal] [Geometry]s.
         *
         * @param polys a collection of [Polygonal] [Geometry]s
         */
        @JvmStatic
        fun union(polys: MutableCollection<*>?, unionFun: UnionStrategy): Geometry? {
            val op = CascadedPolygonUnion(polys, unionFun)
            return op.union()
        }

        /**
         * The effectiveness of the index is somewhat sensitive
         * to the node capacity.
         * Testing indicates that a smaller capacity is better.
         * For an STRtree, 4 is probably a good number (since
         * this produces 2x2 "squares").
         */
        private const val STRTREE_NODE_CAPACITY = 4

        /**
         * Gets the element at a given list index, or
         * null if the index is out of range.
         *
         * @param list
         * @param index
         * @return the geometry at the given index
         * or null if the index is out of range
         */
        private fun getGeometry(list: List<Geometry?>, index: Int): Geometry? {
            if (index >= list.size) return null
            return list[index]
        }

        /**
         * Computes a [Geometry] containing only [Polygonal] components.
         * Extracts the [Polygon]s from the input
         * and returns them as an appropriate [Polygonal] geometry.
         *
         *
         * If the input is already `Polygonal`, it is returned unchanged.
         *
         *
         * A particular use case is to filter out non-polygonal components
         * returned from an overlay operation.
         *
         * @param g the geometry to filter
         * @return a Polygonal geometry
         */
        private fun restrictToPolygons(g: Geometry): Geometry {
            if (g is Polygonal) {
                return g
            }
            val polygons = PolygonExtracter.getPolygons(g)
            if (polygons.size == 1)
                return polygons[0] as Polygon
            return g.getFactory().createMultiPolygon(GeometryFactory.toPolygonArray(polygons))
        }
    }
}
