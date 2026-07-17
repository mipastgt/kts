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

import org.locationtech.jts.algorithm.Orientation
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.triangulate.quadedge.TrianglePredicate
import org.locationtech.jts.triangulate.tri.Tri
import org.locationtech.jts.triangulate.tri.TriangulationBuilder

/**
 * Improves the quality of a triangulation of [Tri]s via
 * iterated Delaunay flipping.
 * This produces a Constrained Delaunay Triangulation
 * with the constraints being the boundary of the input triangulation.
 *
 * @author mdavis
 */
class TriDelaunayImprover private constructor(private val triList: List<Tri>) {

    private fun improve() {
        for (i in 0 until MAX_ITERATION) {
            val improveCount = improveScan(triList)
            //System.out.println("improve #" + i + " - count = " + improveCount);
            if (improveCount == 0) {
                return
            }
        }
    }

    /**
     * Improves a triangulation by examining pairs of adjacent triangles
     * (forming a quadrilateral) and testing if flipping the diagonal of
     * the quadrilateral would produce two new triangles with larger minimum
     * interior angles.
     *
     * @return the number of flips that were made
     */
    private fun improveScan(triList: List<Tri>): Int {
        var improveCount = 0
        for (i in 0 until triList.size - 1) {
            val tri = triList[i]
            for (j in 0..2) {
                //Tri neighb = tri.getAdjacent(j);
                //tri.validateAdjacent(j);
                if (improveNonDelaunay(tri, j)) {
                    // TODO: improve performance by only rescanning tris adjacent to flips?
                    improveCount++
                }
            }
        }
        return improveCount
    }

    /**
     * Does a flip of the common edge of two Tris if the Delaunay condition is not met.
     *
     * @param tri a Tri
     * @param index the edge index
     * @return true if the triangles were flipped
     */
    private fun improveNonDelaunay(tri: Tri?, index: Int): Boolean {
        if (tri == null) {
            return false
        }
        val tri1 = tri.getAdjacent(index)
        if (tri1 == null) {
            return false
        }
        //tri0.validate();
        //tri1.validate();

        val index1 = tri1.getIndex(tri)

        val adj0 = tri.getCoordinate(index)
        val adj1 = tri.getCoordinate(Tri.next(index))
        val opp0 = tri.getCoordinate(Tri.oppVertex(index))
        val opp1 = tri1.getCoordinate(Tri.oppVertex(index1))

        /*
         * The candidate new edge is opp0 - opp1.
         * Check if it is inside the quadrilateral formed by the two triangles.
         * This is the case if the quadrilateral is convex.
         */
        if (!isConvex(adj0, adj1, opp0, opp1)) {
            return false
        }

        /*
         * The candidate edge is inside the quadrilateral. Check to see if the flipping
         * criteria is met. The flipping criteria is to flip if the two triangles are
         * not Delaunay (i.e. one of the opposite vertices is in the circumcircle of the
         * other triangle).
         */
        if (!isDelaunay(adj0, adj1, opp0, opp1)) {
            tri.flip(index)
            return true
        }
        return false
    }

    companion object {
        /**
         * Improves the quality of a triangulation of [Tri]s via
         * iterated Delaunay flipping.
         * The Tris are assumed to be linked into a Triangulation
         * (e.g. via [TriangulationBuilder]).
         *
         * @param triList the list of Tris to flip.
         */
        @JvmStatic
        fun improve(triList: List<Tri>) {
            val improver = TriDelaunayImprover(triList)
            improver.improve()
        }

        private var MAX_ITERATION = 200

        /**
         * Tests if the quadrilateral formed by two adjacent triangles is convex.
         * opp0-adj0-adj1 and opp1-adj1-adj0 are the triangle corners
         * and hence are known to be convex.
         * The quadrilateral is convex if the other corners opp0-adj0-opp1
         * and opp1-adj1-opp0 have the same orientation (since at least one must be convex).
         *
         * @param adj0 adjacent edge vertex 0
         * @param adj1 adjacent edge vertex 1
         * @param opp0 corner vertex of triangle 0
         * @param opp1 corner vertex of triangle 1
         * @return true if the quadrilateral is convex
         */
        private fun isConvex(adj0: Coordinate, adj1: Coordinate, opp0: Coordinate, opp1: Coordinate): Boolean {
            val dir0 = Orientation.index(opp0, adj0, opp1)
            val dir1 = Orientation.index(opp1, adj1, opp0)
            val isConvex = dir0 == dir1
            return isConvex
        }

        /**
         * Tests if either of a pair of adjacent triangles satisfy the Delaunay condition.
         * The triangles are opp0-adj0-adj1 and opp1-adj1-adj0.
         * The Delaunay condition is not met if one opposite vertex
         * lies is in the circumcircle of the other triangle.
         *
         * @param adj0 adjacent edge vertex 0
         * @param adj1 adjacent edge vertex 1
         * @param opp0 corner vertex of triangle 0
         * @param opp1 corner vertex of triangle 1
         * @return true if the triangles are Delaunay
         */
        private fun isDelaunay(adj0: Coordinate, adj1: Coordinate, opp0: Coordinate, opp1: Coordinate): Boolean {
            if (isInCircle(adj0, adj1, opp0, opp1)) return false
            if (isInCircle(adj1, adj0, opp1, opp0)) return false
            return true
        }

        /**
         * Tests whether a point p is in the circumcircle of a triangle abc
         * (oriented clockwise).
         * @param a a vertex of the triangle
         * @param b a vertex of the triangle
         * @param c a vertex of the triangle
         * @param p the point
         *
         * @return true if the point is in the circumcircle
         */
        private fun isInCircle(a: Coordinate, b: Coordinate, c: Coordinate, p: Coordinate): Boolean {
            return TrianglePredicate.isInCircleRobust(a, c, b, p)
        }
    }
}
