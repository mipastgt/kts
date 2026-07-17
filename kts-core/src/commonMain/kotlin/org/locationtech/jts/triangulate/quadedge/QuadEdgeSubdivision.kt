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
import kotlin.math.max


import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.CoordinateList
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LineSegment
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.Polygon
import org.locationtech.jts.geom.Triangle
import org.locationtech.jts.io.WKTWriter

/**
 * A class that contains the [QuadEdge]s representing a planar
 * subdivision that models a triangulation.
 * The subdivision is constructed using the
 * quadedge algebra defined in the class [QuadEdge].
 * All metric calculations
 * are done in the [Vertex] class.
 * In addition to a triangulation, subdivisions
 * support extraction of Voronoi diagrams.
 * This is easily accomplished, since the Voronoi diagram is the dual
 * of the Delaunay triangulation.
 *
 * Subdivisions can be provided with a tolerance value. Inserted vertices which
 * are closer than this value to vertices already in the subdivision will be
 * ignored. Using a suitable tolerance value can prevent robustness failures
 * from happening during Delaunay triangulation.
 *
 * Subdivisions maintain a **frame** triangle around the client-created
 * edges. The frame is used to provide a bounded "container" for all edges
 * within a TIN. Normally the frame edges, frame connecting edges, and frame
 * triangles are not included in client processing.
 *
 * @author David Skea
 * @author Martin Davis
 *
 * @constructor Creates a new instance of a quad-edge subdivision based on a frame triangle
 * that encloses a supplied bounding box. A new super-bounding box that
 * contains the triangle is computed and stored.
 *
 * @param env the bounding box to surround
 * @param tolerance the tolerance value for determining if two sites are equal
 */
class QuadEdgeSubdivision(env: Envelope, tolerance: Double) {

    // used for edge extraction to ensure edge uniqueness
    private var visitedKey = 0
    //	private Set quadEdges = new HashSet();
    private val quadEdges: MutableList<QuadEdge> = ArrayList()
    private var startingEdge: QuadEdge
    private val tolerance: Double
    private val edgeCoincidenceTolerance: Double
    private lateinit var frameVertex: Array<Vertex>
    private lateinit var frameEnv: Envelope
    private var locator: QuadEdgeLocator? = null

    init {
        // currentSubdiv = this;
        this.tolerance = tolerance
        edgeCoincidenceTolerance = tolerance / EDGE_COINCIDENCE_TOL_FACTOR

        createFrame(env)

        startingEdge = initSubdiv()
        locator = LastFoundQuadEdgeLocator(this)
    }

    /**
     * Creates a triangular frame which contains the vertices to be triangulated.
     *
     * The frame must be large enough so that its vertices are not in the circumcircle
     * of any constructed triangle.
     * This ensures that the vertices of the frame do not prevent the convex hull
     * of the input vertices from forming edges of the triangulation.
     * This is done by using a heuristic size
     * of the frame.  However, it may be that this is not fully robust,
     * for input points which contain very narry triangles.
     *
     * @param env the envelope of the input points
     */
    private fun createFrame(env: Envelope) {
        val deltaX = env.getWidth()
        val deltaY = env.getHeight()
        val frameSize = max(deltaX, deltaY) * FRAME_SIZE_FACTOR

        frameVertex = arrayOf(
            Vertex(
                (env.getMaxX() + env.getMinX()) / 2.0,
                env.getMaxY() + frameSize
            ),
            Vertex(env.getMinX() - frameSize, env.getMinY() - frameSize),
            Vertex(env.getMaxX() + frameSize, env.getMinY() - frameSize)
        )

        frameEnv = Envelope(
            frameVertex[0].getCoordinate(),
            frameVertex[1].getCoordinate()
        )
        frameEnv.expandToInclude(frameVertex[2].getCoordinate())
    }

    private fun initSubdiv(): QuadEdge {
        // build initial subdivision from frame
        val ea = makeEdge(frameVertex[0], frameVertex[1])
        val eb = makeEdge(frameVertex[1], frameVertex[2])
        QuadEdge.splice(ea.sym(), eb)
        val ec = makeEdge(frameVertex[2], frameVertex[0])
        QuadEdge.splice(eb.sym(), ec)
        QuadEdge.splice(ec.sym(), ea)
        return ea
    }

    /**
     * Gets the vertex-equality tolerance value
     * used in this subdivision
     *
     * @return the tolerance value
     */
    fun getTolerance(): Double {
        return tolerance
    }

    /**
     * Gets the envelope of the Subdivision (including the frame).
     *
     * @return the envelope
     */
    fun getEnvelope(): Envelope {
        return Envelope(frameEnv)
    }

    /**
     * Gets the collection of base [QuadEdge]s (one for every pair of
     * vertices which is connected).
     *
     * @return a collection of QuadEdges
     */
    fun getEdges(): MutableCollection<QuadEdge> {
        return quadEdges
    }

    /**
     * Sets the [QuadEdgeLocator] to use for locating containing triangles
     * in this subdivision.
     *
     * @param locator a QuadEdgeLocator
     */
    fun setLocator(locator: QuadEdgeLocator) {
        this.locator = locator
    }

    /**
     * Creates a new quadedge, recording it in the edges list.
     *
     * @param o
     * @param d
     * @return a new quadedge
     */
    fun makeEdge(o: Vertex, d: Vertex): QuadEdge {
        val q = QuadEdge.makeEdge(o, d)
        quadEdges.add(q)
        return q
    }

    /**
     * Creates a new QuadEdge connecting the destination of a to the origin of b,
     * in such a way that all three have the same left face after the connection
     * is complete. The quadedge is recorded in the edges list.
     *
     * @param a
     * @param b
     * @return a quadedge
     */
    fun connect(a: QuadEdge, b: QuadEdge): QuadEdge {
        val q = QuadEdge.connect(a, b)
        quadEdges.add(q)
        return q
    }

    /**
     * Deletes a quadedge from the subdivision. Linked quadedges are updated to
     * reflect the deletion.
     *
     * @param e the quadedge to delete
     */
    fun delete(e: QuadEdge) {
        QuadEdge.splice(e, e.oPrev())
        QuadEdge.splice(e.sym(), e.sym().oPrev())

        val eSym = e.sym()
        val eRot = e.rot()
        val eRotSym = e.rot().sym()

        // this is inefficient on an ArrayList, but this method should be called infrequently
        quadEdges.remove(e)
        quadEdges.remove(eSym)
        quadEdges.remove(eRot)
        quadEdges.remove(eRotSym)

        e.delete()
        eSym.delete()
        eRot.delete()
        eRotSym.delete()
    }

    /**
     * Locates an edge of a triangle which contains a location
     * specified by a Vertex v.
     * The edge returned has the
     * property that either v is on e, or e is an edge of a triangle containing v.
     * The search starts from startEdge amd proceeds on the general direction of v.
     *
     * This locate algorithm relies on the subdivision being Delaunay. For
     * non-Delaunay subdivisions, this may loop for ever.
     *
     * @param v the location to search for
     * @param startEdge an edge of the subdivision to start searching at
     * @return a QuadEdge which contains v, or is on the edge of a triangle containing v
     * @throws LocateFailureException
     *           if the location algorithm fails to converge in a reasonable
     *           number of iterations
     */
    fun locateFromEdge(v: Vertex, startEdge: QuadEdge): QuadEdge {
        var iter = 0
        val maxIter = quadEdges.size

        var e = startEdge

        while (true) {
            iter++

            /*
             * So far it has always been the case that failure to locate indicates an
             * invalid subdivision. So just fail completely. (An alternative would be
             * to perform an exhaustive search for the containing triangle, but this
             * would mask errors in the subdivision topology)
             *
             * This can also happen if two vertices are located very close together,
             * since the orientation predicates may experience precision failures.
             */
            if (iter > maxIter) {
                throw LocateFailureException(e.toLineSegment())
            }

            if ((v.equals(e.orig())) || (v.equals(e.dest()))) {
                break
            } else if (v.rightOf(e)) {
                e = e.sym()
            } else if (!v.rightOf(e.oNext())) {
                e = e.oNext()
            } else if (!v.rightOf(e.dPrev())) {
                e = e.dPrev()
            } else {
                // on edge or in triangle containing edge
                break
            }
        }
        // System.out.println("Locate count: " + iter);
        return e
    }

    /**
     * Finds a quadedge of a triangle containing a location
     * specified by a [Vertex], if one exists.
     *
     * @param v the vertex to locate
     * @return a quadedge on the edge of a triangle which touches or contains the location
     * or null if no such triangle exists
     */
    fun locate(v: Vertex): QuadEdge {
        return locator!!.locate(v)
    }

    /**
     * Finds a quadedge of a triangle containing a location
     * specified by a [Coordinate], if one exists.
     *
     * @param p the Coordinate to locate
     * @return a quadedge on the edge of a triangle which touches or contains the location
     * or null if no such triangle exists
     */
    fun locate(p: Coordinate): QuadEdge {
        return locator!!.locate(Vertex(p))
    }

    /**
     * Locates the edge between the given vertices, if it exists in the
     * subdivision.
     *
     * @param p0 a coordinate
     * @param p1 another coordinate
     * @return the edge joining the coordinates, if present
     * or null if no such edge exists
     */
    fun locate(p0: Coordinate, p1: Coordinate): QuadEdge? {
        // find an edge containing one of the points
        val e = locator!!.locate(Vertex(p0))
        if (e == null)
            return null

        // normalize so that p0 is origin of base edge
        var base = e
        if (e.dest().getCoordinate().equals2D(p0))
            base = e.sym()
        // check all edges around origin of base edge
        var locEdge = base
        do {
            if (locEdge.dest().getCoordinate().equals2D(p1))
                return locEdge
            locEdge = locEdge.oNext()
        } while (locEdge !== base)
        return null
    }

    /**
     * Inserts a new site into the Subdivision, connecting it to the vertices of
     * the containing triangle (or quadrilateral, if the split point falls on an
     * existing edge).
     *
     * This method does NOT maintain the Delaunay condition. If desired, this must
     * be checked and enforced by the caller.
     *
     * This method does NOT check if the inserted vertex falls on an edge. This
     * must be checked by the caller, since this situation may cause erroneous
     * triangulation
     *
     * @param v the vertex to insert
     * @return a new quad edge terminating in v
     */
    fun insertSite(v: Vertex): QuadEdge {
        var e = locate(v)

        if ((v.equals(e.orig(), tolerance)) || (v.equals(e.dest(), tolerance))) {
            return e // point already in subdivision.
        }

        // Connect the new point to the vertices of the containing
        // triangle (or quadrilateral, if the new point fell on an
        // existing edge.)
        var base = makeEdge(e.orig(), v)
        QuadEdge.splice(base, e)
        val startEdge = base
        do {
            base = connect(e, base.sym())
            e = base.oPrev()
        } while (e.lNext() !== startEdge)

        return startEdge
    }

    /**
     * Tests whether a QuadEdge is an edge incident on a frame triangle vertex.
     *
     * @param e the edge to test
     * @return true if the edge is connected to the frame triangle
     */
    fun isFrameEdge(e: QuadEdge): Boolean {
        if (isFrameVertex(e.orig()) || isFrameVertex(e.dest()))
            return true
        return false
    }

    /**
     * Tests whether a QuadEdge is an edge on the border of the frame facets and
     * the internal facets. E.g. an edge which does not itself touch a frame
     * vertex, but which touches an edge which does.
     *
     * @param e the edge to test
     * @return true if the edge is on the border of the frame
     */
    fun isFrameBorderEdge(e: QuadEdge): Boolean {
        // MD debugging
        val leftTri = arrayOfNulls<QuadEdge>(3)
        getTriangleEdges(e, leftTri)
        // System.out.println(new QuadEdgeTriangle(leftTri).toString());
        val rightTri = arrayOfNulls<QuadEdge>(3)
        getTriangleEdges(e.sym(), rightTri)
        // System.out.println(new QuadEdgeTriangle(rightTri).toString());

        // check other vertex of triangle to left of edge
        val vLeftTriOther = e.lNext().dest()
        if (isFrameVertex(vLeftTriOther))
            return true
        // check other vertex of triangle to right of edge
        val vRightTriOther = e.sym().lNext().dest()
        if (isFrameVertex(vRightTriOther))
            return true

        return false
    }

    /**
     * Tests whether a vertex is a vertex of the outer triangle.
     *
     * @param v the vertex to test
     * @return true if the vertex is an outer triangle vertex
     */
    fun isFrameVertex(v: Vertex): Boolean {
        if (v.equals(frameVertex[0]))
            return true
        if (v.equals(frameVertex[1]))
            return true
        if (v.equals(frameVertex[2]))
            return true
        return false
    }

    private val seg = LineSegment()

    /**
     * Tests whether a [Coordinate] lies on a [QuadEdge], up to a
     * tolerance determined by the subdivision tolerance.
     *
     * @param e a QuadEdge
     * @param p a point
     * @return true if the vertex lies on the edge
     */
    fun isOnEdge(e: QuadEdge, p: Coordinate): Boolean {
        seg.setCoordinates(e.orig().getCoordinate(), e.dest().getCoordinate())
        val dist = seg.distance(p)
        // heuristic (hack?)
        return dist < edgeCoincidenceTolerance
    }

    /**
     * Tests whether a [Vertex] is the start or end vertex of a
     * [QuadEdge], up to the subdivision tolerance distance.
     *
     * @param e
     * @param v
     * @return true if the vertex is a endpoint of the edge
     */
    fun isVertexOfEdge(e: QuadEdge, v: Vertex): Boolean {
        if ((v.equals(e.orig(), tolerance)) || (v.equals(e.dest(), tolerance))) {
            return true
        }
        return false
    }

    /**
     * Gets the unique [Vertex]es in the subdivision,
     * including the frame vertices if desired.
     *
     * @param includeFrame true if the frame vertices should be included
     * @return a collection of the subdivision vertices
     *
     * @see .getVertexUniqueEdges
     */
    fun getVertices(includeFrame: Boolean): MutableCollection<Vertex> {
        val vertices = HashSet<Vertex>()
        for (qe in quadEdges) {
            val v = qe.orig()
            //System.out.println(v);
            if (includeFrame || !isFrameVertex(v))
                vertices.add(v)

            /**
             * Inspect the sym edge as well, since it is
             * possible that a vertex is only at the
             * dest of all tracked quadedges.
             */
            val vd = qe.dest()
            //System.out.println(vd);
            if (includeFrame || !isFrameVertex(vd))
                vertices.add(vd)
        }
        return vertices
    }

    /**
     * Gets a collection of [QuadEdge]s whose origin
     * vertices are a unique set which includes
     * all vertices in the subdivision.
     * The frame vertices can be included if required.
     *
     * This is useful for algorithms which require traversing the
     * subdivision starting at all vertices.
     * Returning a quadedge for each vertex
     * is more efficient than
     * the alternative of finding the actual vertices
     * using [getVertices] and then locating
     * quadedges attached to them.
     *
     * @param includeFrame true if the frame vertices should be included
     * @return a collection of QuadEdge with the vertices of the subdivision as their origins
     */
    fun getVertexUniqueEdges(includeFrame: Boolean): MutableList<QuadEdge> {
        val edges = ArrayList<QuadEdge>()
        val visitedVertices = HashSet<Vertex>()
        for (qe in quadEdges) {
            val v = qe.orig()
            //System.out.println(v);
            if (!visitedVertices.contains(v)) {
                visitedVertices.add(v)
                if (includeFrame || !isFrameVertex(v)) {
                    edges.add(qe)
                }
            }

            /**
             * Inspect the sym edge as well, since it is
             * possible that a vertex is only at the
             * dest of all tracked quadedges.
             */
            val qd = qe.sym()
            val vd = qd.orig()
            //System.out.println(vd);
            if (!visitedVertices.contains(vd)) {
                visitedVertices.add(vd)
                if (includeFrame || !isFrameVertex(vd)) {
                    edges.add(qd)
                }
            }
        }
        return edges
    }

    /**
     * Gets all primary quadedges in the subdivision.
     * A primary edge is a [QuadEdge]
     * which occupies the 0'th position in its array of associated quadedges.
     * These provide the unique geometric edges of the triangulation.
     *
     * @param includeFrame true if the frame edges are to be included
     * @return a List of QuadEdges
     */
    fun getPrimaryEdges(includeFrame: Boolean): MutableList<QuadEdge> {
        visitedKey++

        val edges = ArrayList<QuadEdge>()
        val edgeStack = ArrayDeque<QuadEdge>()
        edgeStack.addLast(startingEdge)

        val visitedEdges = HashSet<QuadEdge>()

        while (!edgeStack.isEmpty()) {
            val edge = edgeStack.removeLast()
            if (!visitedEdges.contains(edge)) {
                val priQE = edge.getPrimary()

                if (includeFrame || !isFrameEdge(priQE))
                    edges.add(priQE)

                edgeStack.addLast(edge.oNext())
                edgeStack.addLast(edge.sym().oNext())

                visitedEdges.add(edge)
                visitedEdges.add(edge.sym())
            }
        }
        return edges
    }

    /**
     * Gets the edges which touch frame vertices. The returned edges are oriented so
     * that their origin is a frame vertex.
     *
     * @return the edges which touch the frame
     */
    fun getFrameEdges(): MutableList<QuadEdge> {
        val edges = getPrimaryEdges(true)
        val frameEdges = ArrayList<QuadEdge>()
        for (e in edges) {
            if (isFrameEdge(e)) {
                val fe = if (isFrameVertex(e.orig())) e else e.sym()
                frameEdges.add(fe)
            }
        }
        return frameEdges
    }

    /**
     * A TriangleVisitor which computes and sets the
     * circumcentre as the origin of the dual
     * edges originating in each triangle.
     *
     * @author mbdavis
     */
    private class TriangleCircumcentreVisitor : TriangleVisitor {
        override fun visit(triEdges: Array<QuadEdge>) {
            val a = triEdges[0].orig().getCoordinate()
            val b = triEdges[1].orig().getCoordinate()
            val c = triEdges[2].orig().getCoordinate()

            // TODO: choose the most accurate circumcentre based on the edges
            val cc = Triangle.circumcentreDD(a, b, c)
            val ccVertex = Vertex(cc)
            // save the circumcentre as the origin for the dual edges originating in this triangle
            for (i in 0..2) {
                triEdges[i].rot().setOrig(ccVertex)
            }
        }
    }

    /*****************************************************************************
     * Visitors
     ****************************************************************************/

    fun visitTriangles(
        triVisitor: TriangleVisitor,
        includeFrame: Boolean
    ) {
        visitedKey++

        // visited flag is used to record visited edges of triangles
        // setVisitedAll(false);
        val edgeStack = ArrayDeque<QuadEdge>()
        edgeStack.addLast(startingEdge)

        val visitedEdges = HashSet<QuadEdge>()

        while (!edgeStack.isEmpty()) {
            val edge = edgeStack.removeLast()
            if (!visitedEdges.contains(edge)) {
                val tri = fetchTriangleToVisit(
                    edge, edgeStack,
                    includeFrame, visitedEdges
                )
                if (tri != null) {
                    @Suppress("UNCHECKED_CAST")
                    triVisitor.visit(tri as Array<QuadEdge>)
                }
            }
        }
    }

    /**
     * The quadedges forming a single triangle.
     * Only one visitor is allowed to be active at a
     * time, so this is safe.
     */
    private val triEdges = arrayOfNulls<QuadEdge>(3)

    /**
     * Stores the edges for a visited triangle. Also pushes sym (neighbour) edges
     * on stack to visit later.
     *
     * @param edge
     * @param edgeStack
     * @param includeFrame
     * @return the visited triangle edges
     * or null if the triangle should not be visited (for instance, if it is
     *         outer)
     */
    private fun fetchTriangleToVisit(
        edge: QuadEdge, edgeStack: ArrayDeque<QuadEdge>,
        includeFrame: Boolean, visitedEdges: MutableSet<QuadEdge>
    ): Array<QuadEdge?>? {
        var curr = edge
        var edgeCount = 0
        var isFrame = false
        do {
            triEdges[edgeCount] = curr

            if (isFrameEdge(curr))
                isFrame = true

            // push sym edges to visit next
            val sym = curr.sym()
            if (!visitedEdges.contains(sym))
                edgeStack.addLast(sym)

            // mark this edge as visited
            visitedEdges.add(curr)

            edgeCount++
            curr = curr.lNext()
        } while (curr !== edge)

        if (isFrame && !includeFrame)
            return null
        return triEdges
    }

    /**
     * Gets a list of the triangles
     * in the subdivision, specified as
     * an array of the primary quadedges around the triangle.
     *
     * @param includeFrame true if the frame triangles should be included
     * @return a List of QuadEdge[3] arrays
     */
    fun getTriangleEdges(includeFrame: Boolean): MutableList<Array<QuadEdge>> {
        val visitor = TriangleEdgesListVisitor()
        visitTriangles(visitor, includeFrame)
        return visitor.getTriangleEdges()
    }

    private class TriangleEdgesListVisitor : TriangleVisitor {
        private val triList = ArrayList<Array<QuadEdge>>()

        override fun visit(triEdges: Array<QuadEdge>) {
            triList.add(arrayOf(triEdges[0], triEdges[1], triEdges[2]))
        }

        fun getTriangleEdges(): MutableList<Array<QuadEdge>> {
            return triList
        }
    }

    /**
     * Gets a list of the triangles in the subdivision,
     * specified as an array of the triangle [Vertex]es.
     *
     * @param includeFrame true if the frame triangles should be included
     * @return a List of Vertex[3] arrays
     */
    fun getTriangleVertices(includeFrame: Boolean): MutableList<Array<Vertex>> {
        val visitor = TriangleVertexListVisitor()
        visitTriangles(visitor, includeFrame)
        return visitor.getTriangleVertices()
    }

    private class TriangleVertexListVisitor : TriangleVisitor {
        private val triList = ArrayList<Array<Vertex>>()

        override fun visit(triEdges: Array<QuadEdge>) {
            triList.add(
                arrayOf(
                    triEdges[0].orig(), triEdges[1].orig(),
                    triEdges[2].orig()
                )
            )
        }

        fun getTriangleVertices(): MutableList<Array<Vertex>> {
            return triList
        }
    }

    /**
     * Gets the coordinates for each triangle in the subdivision as an array.
     *
     * @param includeFrame true if the frame triangles should be included
     * @return a list of Coordinate[4] representing each triangle
     */
    fun getTriangleCoordinates(includeFrame: Boolean): MutableList<Array<Coordinate>> {
        val visitor = TriangleCoordinatesVisitor()
        visitTriangles(visitor, includeFrame)
        return visitor.getTriangles()
    }

    private class TriangleCoordinatesVisitor : TriangleVisitor {
        private val coordList = CoordinateList()

        private val triCoords = ArrayList<Array<Coordinate>>()

        override fun visit(triEdges: Array<QuadEdge>) {
            coordList.clear()
            for (i in 0..2) {
                val v = triEdges[i].orig()
                coordList.add(v.getCoordinate())
            }
            if (coordList.size > 0) {
                coordList.closeRing()
                val pts = coordList.toCoordinateArray()
                if (pts.size != 4) {
                    //checkTriangleSize(pts);
                    return
                }

                triCoords.add(pts)
            }
        }

        private fun checkTriangleSize(pts: Array<Coordinate>) {
            var loc = ""
            if (pts.size >= 2)
                loc = WKTWriter.toLineString(pts[0], pts[1])
            else {
                if (pts.size >= 1)
                    loc = WKTWriter.toPoint(pts[0])
            }
            // Assert.isTrue(pts.length == 4, "Too few points for visited triangle at " + loc);
        }

        fun getTriangles(): MutableList<Array<Coordinate>> {
            return triCoords
        }
    }

    /**
     * Gets the geometry for the edges in the subdivision as a [org.locationtech.jts.geom.MultiLineString]
     * containing 2-point lines.
     *
     * @param geomFact the GeometryFactory to use
     * @return a MultiLineString
     */
    fun getEdges(geomFact: GeometryFactory): Geometry {
        val quadEdges = getPrimaryEdges(false)
        val edges = arrayOfNulls<LineString>(quadEdges.size)
        var i = 0
        for (qe in quadEdges) {
            edges[i++] = geomFact.createLineString(
                arrayOf(
                    qe.orig().getCoordinate(), qe.dest().getCoordinate()
                )
            )
        }
        @Suppress("UNCHECKED_CAST")
        return geomFact.createMultiLineString(edges as Array<LineString>)
    }

    /**
     * Gets the geometry for the triangles in a triangulated subdivision as a [org.locationtech.jts.geom.GeometryCollection]
     * of triangular [Polygon]s.
     *
     * @param geomFact the GeometryFactory to use
     * @return a GeometryCollection of triangular Polygons
     */
    fun getTriangles(geomFact: GeometryFactory): Geometry {
        val triPtsList = getTriangleCoordinates(false)
        val tris = arrayOfNulls<Polygon>(triPtsList.size)
        var i = 0
        for (triPt in triPtsList) {
            tris[i++] = geomFact
                .createPolygon(geomFact.createLinearRing(triPt))
        }
        @Suppress("UNCHECKED_CAST")
        return geomFact.createGeometryCollection(tris as Array<Geometry>)
    }

    /**
     * Gets the geometry for the triangles in a triangulated subdivision as a [org.locationtech.jts.geom.GeometryCollection]
     * of triangular [Polygon]s, optionally including the frame triangles.
     *
     * @param includeFrame true if the frame triangles should be included
     * @param geomFact the GeometryFactory to use
     * @return a GeometryCollection of triangular Polygons
     */
    fun getTriangles(includeFrame: Boolean, geomFact: GeometryFactory): Geometry {
        val triPtsList = getTriangleCoordinates(includeFrame)
        val tris = arrayOfNulls<Polygon>(triPtsList.size)
        var i = 0
        for (triPt in triPtsList) {
            tris[i++] = geomFact.createPolygon(geomFact.createLinearRing(triPt))
        }
        @Suppress("UNCHECKED_CAST")
        return geomFact.createGeometryCollection(tris as Array<Geometry>)
    }

    /**
     * Gets the cells in the Voronoi diagram for this triangulation.
     * The cells are returned as a [org.locationtech.jts.geom.GeometryCollection] of [Polygon]s
     *
     * The userData of each polygon is set to be the [Coordinate]
     * of the cell site.  This allows easily associating external
     * data associated with the sites to the cells.
     *
     * @param geomFact a geometry factory
     * @return a GeometryCollection of Polygons
     */
    fun getVoronoiDiagram(geomFact: GeometryFactory): Geometry {
        val vorCells = getVoronoiCellPolygons(geomFact)
        return geomFact.createGeometryCollection(GeometryFactory.toGeometryArray(vorCells))
    }

    /**
     * Gets a List of [Polygon]s for the Voronoi cells
     * of this triangulation.
     *
     * The userData of each polygon is set to be the [Coordinate]
     * of the cell site.  This allows easily associating external
     * data associated with the sites to the cells.
     *
     * @param geomFact a geometry factory
     * @return a List of Polygons
     */
    fun getVoronoiCellPolygons(geomFact: GeometryFactory): MutableList<Polygon> {
        /*
         * Compute circumcentres of triangles as vertices for dual edges.
         * Precomputing the circumcentres is more efficient,
         * and more importantly ensures that the computed centres
         * are consistent across the Voronoi cells.
         */
        visitTriangles(TriangleCircumcentreVisitor(), true)

        val cells = ArrayList<Polygon>()
        val edges = getVertexUniqueEdges(false)
        for (qe in edges) {
            cells.add(getVoronoiCellPolygon(qe, geomFact))
        }
        return cells
    }

    /**
     * Gets the Voronoi cell around a site specified
     * by the origin of a QuadEdge.
     *
     * The userData of the polygon is set to be the [Coordinate]
     * of the site.  This allows attaching external
     * data associated with the site to this cell polygon.
     *
     * @param qe a quadedge originating at the cell site
     * @param geomFact a factory for building the polygon
     * @return a polygon indicating the cell extent
     */
    fun getVoronoiCellPolygon(qe: QuadEdge, geomFact: GeometryFactory): Polygon {
        val cellPts = ArrayList<Coordinate>()
        val startQE = qe
        var edge = qe
        do {
            //    	Coordinate cc = circumcentre(qe);
            // use previously computed circumcentre
            val cc = edge.rot().orig().getCoordinate()
            cellPts.add(cc)

            // move to next triangle CW around vertex
            edge = edge.oPrev()
        } while (edge !== startQE)

        val coordList = CoordinateList()
        coordList.addAll(cellPts, false)
        coordList.closeRing()

        if (coordList.size < 4) {
            //System.out.println(coordList);
            coordList.add(coordList.get(coordList.size - 1), true)
        }

        val pts = coordList.toCoordinateArray()
        val cellPoly = geomFact.createPolygon(geomFact.createLinearRing(pts))

        val v = startQE.orig()
        cellPoly.setUserData(v.getCoordinate())
        return cellPoly
    }

    /**
     * Tests whether a subdivision is a valid Delaunay Triangulation.
     * This is the case iff every edge is locally Delaunay, meaning that
     * the apex of one adjacent triangle is not inside the circumcircle
     * of the other adjacent triangle.
     *
     * @return true if the subdivision is Delaunay
     */
    fun isDelaunay(): Boolean {
        val edges = getPrimaryEdges(true)
        for (e in edges) {
            val a0 = e.oPrev().dest()
            val a1 = e.oNext().dest()
            val isDelaunay = !a1.isInCircle(e.orig(), a0, e.dest())
            if (!isDelaunay) {
                return false
            }
        }
        return true
    }

    companion object {
        /**
         * Gets the edges for the triangle to the left of the given [QuadEdge].
         *
         * @param startQE
         * @param triEdge
         *
         * @throws IllegalArgumentException
         *           if the edges do not form a triangle
         */
        @JvmStatic
        fun getTriangleEdges(startQE: QuadEdge, triEdge: Array<QuadEdge?>) {
            triEdge[0] = startQE
            triEdge[1] = triEdge[0]!!.lNext()
            triEdge[2] = triEdge[1]!!.lNext()
            if (triEdge[2]!!.lNext() !== triEdge[0])
                throw IllegalArgumentException("Edges do not form a triangle")
        }

        private const val EDGE_COINCIDENCE_TOL_FACTOR = 1000.0

        private const val FRAME_SIZE_FACTOR = 10.0
    }
}
