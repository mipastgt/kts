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

import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Polygon
import org.locationtech.jts.geom.util.PolygonExtracter
import org.locationtech.jts.triangulate.tri.Tri

/**
 * Computes a triangulation of each polygon in a [Geometry].
 * A polygon triangulation is a non-overlapping set of triangles which
 * cover the polygon and have the same vertices as the polygon.
 * The priority is on performance rather than triangulation quality,
 * so that the output may contain many narrow triangles.
 *
 * Holes are handled by joining them to the shell to form a
 * (self-touching) polygon shell with no holes.
 * Although invalid, this can be triangulated effectively.
 *
 * For better-quality triangulation use [ConstrainedDelaunayTriangulator].
 *
 * @see ConstrainedDelaunayTriangulator
 *
 * @author Martin Davis
 *
 * @constructor Constructs a new triangulator.
 *
 * @param inputGeom the input geometry
 */
class PolygonTriangulator(private val inputGeom: Geometry) {

    private val geomFact: GeometryFactory = inputGeom.getFactory()
    private var triList: MutableList<Tri>? = null

    /**
     * Gets the triangulation as a [org.locationtech.jts.geom.GeometryCollection] of triangular [Polygon]s.
     *
     * @return a collection of the result triangle polygons
     */
    fun getResult(): Geometry {
        compute()
        return Tri.toGeometry(triList!!, geomFact)
    }

    /**
     * Gets the triangulation as a list of [Tri]s.
     *
     * @return the list of Tris in the triangulation
     */
    fun getTriangles(): MutableList<Tri> {
        compute()
        return triList!!
    }

    private fun compute() {
        val polys = PolygonExtracter.getPolygons(inputGeom)
        val triList = ArrayList<Tri>()
        this.triList = triList
        for (o in polys) {
            val poly = o as Polygon
            if (poly.isEmpty()) continue
            val polyTriList = triangulatePolygon(poly)
            triList.addAll(polyTriList)
        }
    }

    /**
     * Computes the triangulation of a single polygon
     *
     * @return GeometryCollection of triangular polygons
     */
    private fun triangulatePolygon(poly: Polygon): List<Tri> {
        val polyShell = PolygonHoleJoiner.join(poly)

        val triList = PolygonEarClipper.triangulate(polyShell)
        //Tri.validate(triList);

        return triList
    }

    companion object {
        /**
         * Computes a triangulation of each polygon in a geometry.
         *
         * @param geom a geometry containing polygons
         * @return a GeometryCollection containing the triangle polygons
         */
        @JvmStatic
        fun triangulate(geom: Geometry): Geometry {
            val triangulator = PolygonTriangulator(geom)
            return triangulator.getResult()
        }
    }
}
