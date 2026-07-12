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

package org.locationtech.jts.triangulate

import org.locationtech.jts.algorithm.Orientation
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.triangulate.quadedge.QuadEdge
import org.locationtech.jts.triangulate.quadedge.QuadEdgeSubdivision
import org.locationtech.jts.triangulate.quadedge.Vertex

/**
 * Computes a Delaunay Triangulation of a set of [Vertex]es, using an
 * incremental insertion algorithm.
 *
 * @author Martin Davis
 * @version 1.0
 */
/**
 * Creates a new triangulator using the given [QuadEdgeSubdivision].
 * The triangulator uses the tolerance of the supplied subdivision.
 *
 * @param subdiv a subdivision in which to build the TIN
 */
class IncrementalDelaunayTriangulator(private val subdiv: QuadEdgeSubdivision) {
    private var isUsingTolerance = false
    private var isForceConvex = true

    init {
        isUsingTolerance = subdiv.getTolerance() > 0.0
    }

    /**
     * Sets whether the triangulation is forced to have a convex boundary. Because
     * of the use of a finite-size frame, this condition requires special logic to
     * enforce. The default is true, since this is a requirement for some uses of
     * Delaunay Triangulations (such as Concave Hull generation). However, forcing
     * the triangulation boundary to be convex may cause the overall frame
     * triangulation to be non-Delaunay. This can cause a problem for Voronoi
     * generation, so the logic can be disabled via this method.
     *
     * @param isForceConvex true if the triangulation boundary is forced to be
     *                      convex
     */
    fun forceConvex(isForceConvex: Boolean) {
        this.isForceConvex = isForceConvex
    }

    /**
     * Inserts all sites in a collection. The inserted vertices **MUST** be
     * unique up to the provided tolerance value. (i.e. no two vertices should be
     * closer than the provided tolerance value). They do not have to be rounded
     * to the tolerance grid, however.
     *
     * @param vertices a Collection of Vertex
     *
     * @throws org.locationtech.jts.triangulate.quadedge.LocateFailureException if the location algorithm fails to converge in a reasonable number of iterations
     */
    fun insertSites(vertices: Collection<*>) {
        for (o in vertices) {
            val v = o as Vertex
            insertSite(v)
        }
    }

    /**
     * Inserts a new point into a subdivision representing a Delaunay
     * triangulation, and fixes the affected edges so that the result is still a
     * Delaunay triangulation.
     *
     * @return a quadedge containing the inserted vertex
     */
    fun insertSite(v: Vertex): QuadEdge {

        /**
         * This code is based on Guibas and Stolfi (1985), with minor modifications
         * and a bug fix from Dani Lischinski (Graphic Gems 1993). (The modification
         * I believe is the test for the inserted site falling exactly on an
         * existing edge. Without this test zero-width triangles have been observed
         * to be created)
         */
        var e = subdiv.locate(v)

        if (subdiv.isVertexOfEdge(e, v)) {
            // point is already in subdivision.
            return e
        } else if (subdiv.isOnEdge(e, v.getCoordinate())) {
            // the point lies exactly on an edge, so delete the edge
            // (it will be replaced by a pair of edges which have the point as a vertex)
            e = e.oPrev()
            subdiv.delete(e.oNext())
        }

        /**
         * Connect the new point to the vertices of the containing triangle
         * (or quadrilateral, if the new point fell on an existing edge.)
         */
        var base = subdiv.makeEdge(e.orig(), v)
        QuadEdge.splice(base, e)
        val startEdge = base
        do {
            base = subdiv.connect(e, base.sym())
            e = base.oPrev()
        } while (e.lNext() !== startEdge)

        /**
         * Examine suspect edges to ensure that the Delaunay condition is satisfied.
         * If it is not, flip the edge and continue scanning.
         *
         * Since the frame is not infinitely far away,
         * edges which touch the frame or are adjacent to it require special logic
         * to ensure the inner triangulation maintains a convex boundary.
         */
        do {
            //-- general case - flip if vertex is in circumcircle
            val t = e.oPrev()
            var doFlip = t.dest().rightOf(e) && v.isInCircle(e.orig(), t.dest(), e.dest())

            if (isForceConvex) {
                //-- special cases to ensure triangulation boundary is convex
                if (isConcaveBoundary(e)) {
                    //-- flip if the triangulation boundary is concave
                    doFlip = true
                } else if (isBetweenFrameAndInserted(e, v)) {
                    //-- don't flip if edge lies between the inserted vertex and a frame vertex
                    doFlip = false
                }
            }

            if (doFlip) {
                //-- flip the edge within its quadrilateral
                QuadEdge.swap(e)
                e = e.oPrev()
                continue
            }

            if (e.oNext() === startEdge) {
                return base // no more suspect edges.
            }
            //-- check next edge
            e = e.oNext().lPrev()
        } while (true)
    }

    /**
     * Tests if a edge touching a frame vertex
     * creates a concavity in the triangulation boundary.
     *
     * @param e the edge to test
     * @return true if the triangulation boundary is concave at the edge
     */
    private fun isConcaveBoundary(e: QuadEdge): Boolean {
        if (subdiv.isFrameVertex(e.dest())) {
            return isConcaveAtOrigin(e)
        }
        if (subdiv.isFrameVertex(e.orig())) {
            return isConcaveAtOrigin(e.sym())
        }
        return false
    }

    /**
     * Edges whose adjacent triangles contain
     * a frame vertex and the inserted vertex must not be flipped.
     *
     * @param e the edge to test
     * @param vInsert the inserted vertex
     * @return true if the edge is between the frame and inserted vertex
     */
    private fun isBetweenFrameAndInserted(e: QuadEdge, vInsert: Vertex): Boolean {
        val v1 = e.oNext().dest()
        val v2 = e.oPrev().dest()
        return (v1 === vInsert && subdiv.isFrameVertex(v2)) ||
            (v2 === vInsert && subdiv.isFrameVertex(v1))
    }

    companion object {
        /**
         * Tests if the quadrilateral surrounding an edge is concave at the edge origin.
         * Used to determine if the triangulation boundary has a concavity.
         * @param e
         * @return
         */
        private fun isConcaveAtOrigin(e: QuadEdge): Boolean {
            val p = e.orig().getCoordinate()
            val pp = e.oPrev().dest().getCoordinate()
            val pn = e.oNext().dest().getCoordinate()
            val isConcave = Orientation.COUNTERCLOCKWISE == Orientation.index(pp, pn, p)
            return isConcave
        }
    }
}
