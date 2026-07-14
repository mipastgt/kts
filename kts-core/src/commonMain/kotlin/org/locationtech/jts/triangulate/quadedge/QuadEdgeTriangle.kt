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

package org.locationtech.jts.triangulate.quadedge

import kotlin.jvm.JvmStatic

import org.locationtech.jts.algorithm.PointLocation
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LineSegment
import org.locationtech.jts.geom.LinearRing
import org.locationtech.jts.geom.Polygon

/**
 * Models a triangle formed from [QuadEdge]s in a [QuadEdgeSubdivision]
 * which forms a triangulation. The class provides methods to access the
 * topological and geometric properties of the triangle and its neighbours in
 * the triangulation. Triangle vertices are ordered in CCW orientation in the
 * structure.
 *
 * QuadEdgeTriangles support having an external data attribute attached to them.
 * Alternatively, this class can be subclassed and attributes can
 * be defined in the subclass.  Subclasses will need to define
 * their own `BuilderVisitor` class
 * and `createOn` method.
 *
 * @author Martin Davis
 */
class QuadEdgeTriangle
/**
 * Creates a new triangle from the given edges.
 *
 * @param edge an array of the edges of the triangle in CCW order
 */
(edge: Array<QuadEdge>) {

    private var edge: Array<QuadEdge>?
    private var data: Any? = null

    init {
        this.edge = edge.copyOf()
        // link the quadedges back to this triangle
        for (i in 0..2) {
            edge[i].setData(this)
        }
    }

    /**
     * Sets the external data value for this triangle.
     *
     * @param data an object containing external data
     */
    fun setData(data: Any?) {
        this.data = data
    }

    /**
     * Gets the external data value for this triangle.
     *
     * @return the data object
     */
    fun getData(): Any? {
        return data
    }

    fun kill() {
        edge = null
    }

    fun isLive(): Boolean {
        return edge != null
    }

    fun getEdges(): Array<QuadEdge>? {
        return edge
    }

    fun getEdge(i: Int): QuadEdge {
        return edge!![i]
    }

    fun getVertex(i: Int): Vertex {
        return edge!![i].orig()
    }

    /**
     * Gets the vertices for this triangle.
     *
     * @return a new array containing the triangle vertices
     */
    fun getVertices(): Array<Vertex?> {
        val vert = arrayOfNulls<Vertex>(3)
        for (i in 0..2) {
            vert[i] = getVertex(i)
        }
        return vert
    }

    fun getCoordinate(i: Int): Coordinate {
        return edge!![i].orig().getCoordinate()
    }

    /**
     * Gets the index for the given edge of this triangle
     *
     * @param e a QuadEdge
     * @return the index of the edge in this triangle
     * or -1 if the edge is not an edge of this triangle
     */
    fun getEdgeIndex(e: QuadEdge): Int {
        for (i in 0..2) {
            if (edge!![i] === e)
                return i
        }
        return -1
    }

    /**
     * Gets the index for the edge that starts at vertex v.
     *
     * @param v the vertex to find the edge for
     * @return the index of the edge starting at the vertex
     * or -1 if the vertex is not in the triangle
     */
    fun getEdgeIndex(v: Vertex): Int {
        for (i in 0..2) {
            if (edge!![i].orig() === v)
                return i
        }
        return -1
    }

    fun getEdgeSegment(i: Int, seg: LineSegment) {
        seg.p0 = edge!![i].orig().getCoordinate()
        val nexti = (i + 1) % 3
        seg.p1 = edge!![nexti].orig().getCoordinate()
    }

    fun getCoordinates(): Array<Coordinate> {
        val pts = arrayOfNulls<Coordinate>(4)
        for (i in 0..2) {
            pts[i] = edge!![i].orig().getCoordinate()
        }
        pts[3] = Coordinate(pts[0]!!)
        @Suppress("UNCHECKED_CAST")
        return pts as Array<Coordinate>
    }

    fun contains(pt: Coordinate): Boolean {
        val ring = getCoordinates()
        return PointLocation.isInRing(pt, ring)
    }

    fun getGeometry(fact: GeometryFactory): Polygon {
        val ring = fact.createLinearRing(getCoordinates())
        val tri = fact.createPolygon(ring)
        return tri
    }

    override fun toString(): String {
        return getGeometry(GeometryFactory()).toString()
    }

    /**
     * Tests whether this triangle is adjacent to the outside of the subdivision.
     *
     * @return true if the triangle is adjacent to the subdivision exterior
     */
    fun isBorder(): Boolean {
        for (i in 0..2) {
            if (getAdjacentTriangleAcrossEdge(i) == null)
                return true
        }
        return false
    }

    fun isBorder(i: Int): Boolean {
        return getAdjacentTriangleAcrossEdge(i) == null
    }

    fun getAdjacentTriangleAcrossEdge(edgeIndex: Int): QuadEdgeTriangle? {
        return getEdge(edgeIndex).sym().getData() as QuadEdgeTriangle?
    }

    fun getAdjacentTriangleEdgeIndex(i: Int): Int {
        return getAdjacentTriangleAcrossEdge(i)!!.getEdgeIndex(getEdge(i).sym())
    }

    /**
     * Gets the triangles which are adjacent (include) to a
     * given vertex of this triangle.
     *
     * @param vertexIndex the vertex to query
     * @return a list of the vertex-adjacent triangles
     */
    fun getTrianglesAdjacentToVertex(vertexIndex: Int): MutableList<QuadEdgeTriangle> {
        // Assert: isVertex
        val adjTris = ArrayList<QuadEdgeTriangle>()

        val start = getEdge(vertexIndex)
        var qe = start
        do {
            val adjTri = qe.getData() as QuadEdgeTriangle?
            if (adjTri != null) {
                adjTris.add(adjTri)
            }
            qe = qe.oNext()
        } while (qe !== start)

        return adjTris
    }

    /**
     * Gets the neighbours of this triangle. If there is no neighbour triangle,
     * the array element is `null`
     *
     * @return an array containing the 3 neighbours of this triangle
     */
    fun getNeighbours(): Array<QuadEdgeTriangle?> {
        val neigh = arrayOfNulls<QuadEdgeTriangle>(3)
        for (i in 0..2) {
            neigh[i] = getEdge(i).sym().getData() as QuadEdgeTriangle?
        }
        return neigh
    }

    private class QuadEdgeTriangleBuilderVisitor : TriangleVisitor {
        private val triangles = ArrayList<QuadEdgeTriangle>()

        override fun visit(triEdges: Array<QuadEdge>) {
            triangles.add(QuadEdgeTriangle(triEdges))
        }

        fun getTriangles(): MutableList<QuadEdgeTriangle> {
            return triangles
        }
    }

    companion object {
        /**
         * Creates [QuadEdgeTriangle]s for all facets of a
         * [QuadEdgeSubdivision] representing a triangulation.
         * The `data` attributes of the [QuadEdge]s in the subdivision
         * will be set to point to the triangle which contains that edge.
         * This allows tracing the neighbour triangles of any given triangle.
         *
         * @param subdiv the QuadEdgeSubdivision to create the triangles on.
         * @return a List of the created QuadEdgeTriangles
         */
        @JvmStatic
        fun createOn(subdiv: QuadEdgeSubdivision): MutableList<QuadEdgeTriangle> {
            val visitor = QuadEdgeTriangleBuilderVisitor()
            subdiv.visitTriangles(visitor, false)
            return visitor.getTriangles()
        }

        /**
         * Tests whether the point pt is contained in the triangle defined by 3
         * [Vertex]es.
         *
         * @param tri an array containing at least 3 Vertexes
         * @param pt the point to test
         * @return true if the point is contained in the triangle
         */
        @JvmStatic
        fun contains(tri: Array<Vertex>, pt: Coordinate): Boolean {
            val ring = arrayOf(
                tri[0].getCoordinate(),
                tri[1].getCoordinate(), tri[2].getCoordinate(), tri[0].getCoordinate()
            )
            return PointLocation.isInRing(pt, ring)
        }

        /**
         * Tests whether the point pt is contained in the triangle defined by 3
         * [QuadEdge]es.
         *
         * @param tri an array containing at least 3 QuadEdges
         * @param pt the point to test
         * @return true if the point is contained in the triangle
         */
        @JvmStatic
        fun contains(tri: Array<QuadEdge>, pt: Coordinate): Boolean {
            val ring = arrayOf(
                tri[0].orig().getCoordinate(),
                tri[1].orig().getCoordinate(), tri[2].orig().getCoordinate(),
                tri[0].orig().getCoordinate()
            )
            return PointLocation.isInRing(pt, ring)
        }

        @JvmStatic
        fun toPolygon(v: Array<Vertex>): Geometry {
            val ringPts = arrayOf(
                v[0].getCoordinate(),
                v[1].getCoordinate(), v[2].getCoordinate(), v[0].getCoordinate()
            )
            val fact = GeometryFactory()
            val ring = fact.createLinearRing(ringPts)
            val tri = fact.createPolygon(ring)
            return tri
        }

        @JvmStatic
        fun toPolygon(e: Array<QuadEdge>): Geometry {
            val ringPts = arrayOf(
                e[0].orig().getCoordinate(),
                e[1].orig().getCoordinate(), e[2].orig().getCoordinate(),
                e[0].orig().getCoordinate()
            )
            val fact = GeometryFactory()
            val ring = fact.createLinearRing(ringPts)
            val tri = fact.createPolygon(ring)
            return tri
        }

        /**
         * Finds the next index around the triangle. Index may be an edge or vertex
         * index.
         *
         * @param index
         * @return the next index
         */
        @JvmStatic
        fun nextIndex(index: Int): Int {
            return (index + 1) % 3
        }
    }
}
