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
import kotlin.math.max

import org.locationtech.jts.algorithm.ConvexHull
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.index.kdtree.KdTree
import org.locationtech.jts.triangulate.quadedge.LastFoundQuadEdgeLocator
import org.locationtech.jts.triangulate.quadedge.QuadEdgeSubdivision
import org.locationtech.jts.triangulate.quadedge.Vertex

/**
 * Computes a Conforming Delaunay Triangulation over a set of sites and a set of
 * linear constraints.
 *
 * A conforming Delaunay triangulation is a true Delaunay triangulation. In it
 * each constraint segment is present as a union of one or more triangulation
 * edges. Constraint segments may be subdivided into two or more triangulation
 * edges by the insertion of additional sites. The additional sites are called
 * Steiner points, and are necessary to allow the segments to be faithfully
 * reflected in the triangulation while maintaining the Delaunay property.
 * Another way of stating this is that in a conforming Delaunay triangulation
 * every constraint segment will be the union of a subset of the triangulation
 * edges (up to tolerance).
 *
 * A Conforming Delaunay triangulation is distinct from a Constrained Delaunay triangulation.
 * A Constrained Delaunay triangulation is not necessarily fully Delaunay,
 * and it contains the constraint segments exactly as edges of the triangulation.
 *
 * @author David Skea
 * @author Martin Davis
 */
/**
 * Creates a Conforming Delaunay Triangulation based on the given
 * unconstrained initial vertices. The initial vertex set should not contain
 * any vertices which appear in the constraint set.
 *
 * @param initialVertices a collection of [ConstraintVertex]
 * @param tolerance the distance tolerance below which points are considered identical
 */
class ConformingDelaunayTriangulator(initialVertices: Collection<*>, private val tolerance: Double) {

    private val initialVertices: MutableList<Vertex> // List<Vertex>
    private lateinit var segVertices: MutableList<Vertex> // List<Vertex>

    // MD - using a Set doesn't seem to be much faster
    // private Set segments = new HashSet();
    private var segments: MutableList<Segment> = ArrayList() // List<Segment>
    private var subdiv: QuadEdgeSubdivision? = null
    private lateinit var incDel: IncrementalDelaunayTriangulator
    private var convexHull: Geometry? = null
    private var splitFinder: ConstraintSplitPointFinder = NonEncroachingSplitPointFinder()
    private val kdt: KdTree = KdTree(tolerance)
    private var vertexFactory: ConstraintVertexFactory? = null

    // allPointsEnv expanded by a small buffer
    private lateinit var computeAreaEnv: Envelope
    // records the last split point computed, for error reporting
    private var splitPt: Coordinate? = null

    init {
        @Suppress("UNCHECKED_CAST")
        this.initialVertices = ArrayList(initialVertices as Collection<Vertex>)
    }

    /**
     * Sets the constraints to be conformed to by the computed triangulation.
     * The constraints must not contain duplicate segments (up to orientation).
     * The unique set of vertices (as [ConstraintVertex]es)
     * forming the constraints must also be supplied.
     * Supplying it explicitly allows the ConstraintVertexes to be initialized
     * appropriately (e.g. with external data), and avoids re-computing the unique set
     * if it is already available.
     *
     * @param segments a list of the constraint [Segment]s
     * @param segVertices the set of unique [ConstraintVertex]es referenced by the segments
     */
    fun setConstraints(segments: MutableList<Segment>, segVertices: MutableList<Vertex>) {
        this.segments = segments
        this.segVertices = segVertices
    }

    /**
     * Sets the [ConstraintSplitPointFinder] to be
     * used during constraint enforcement.
     * Different splitting strategies may be appropriate
     * for special situations.
     *
     * @param splitFinder the ConstraintSplitPointFinder to be used
     */
    fun setSplitPointFinder(splitFinder: ConstraintSplitPointFinder) {
        this.splitFinder = splitFinder
    }

    /**
     * Gets the tolerance value used to construct the triangulation.
     *
     * @return a tolerance value
     */
    fun getTolerance(): Double {
        return tolerance
    }

    /**
     * Gets the `ConstraintVertexFactory` used to create new constraint vertices at split points.
     *
     * @return a new constraint vertex
     */
    fun getVertexFactory(): ConstraintVertexFactory? {
        return vertexFactory
    }

    /**
     * Sets a custom [ConstraintVertexFactory] to be used
     * to allow vertices carrying extra information to be created.
     *
     * @param vertexFactory the ConstraintVertexFactory to be used
     */
    fun setVertexFactory(vertexFactory: ConstraintVertexFactory) {
        this.vertexFactory = vertexFactory
    }

    /**
     * Gets the [QuadEdgeSubdivision] which represents the triangulation.
     *
     * @return a subdivision
     */
    fun getSubdivision(): QuadEdgeSubdivision? {
        return subdiv
    }

    /**
     * Gets the [KdTree] which contains the vertices of the triangulation.
     *
     * @return a KdTree
     */
    fun getKDT(): KdTree {
        return kdt
    }

    /**
     * Gets the sites (vertices) used to initialize the triangulation.
     *
     * @return a List of Vertex
     */
    fun getInitialVertices(): MutableList<Vertex> {
        return initialVertices
    }

    /**
     * Gets the [Segment]s which represent the constraints.
     *
     * @return a collection of Segments
     */
    fun getConstraintSegments(): MutableCollection<Segment> {
        return segments
    }

    /**
     * Gets the convex hull of all the sites in the triangulation,
     * including constraint vertices.
     * Only valid after the constraints have been enforced.
     *
     * @return the convex hull of the sites
     */
    fun getConvexHull(): Geometry? {
        return convexHull
    }

    // ==================================================================

    private fun computeBoundingBox() {
        val vertexEnv = computeVertexEnvelope(initialVertices)
        val segEnv = computeVertexEnvelope(segVertices)

        val allPointsEnv = Envelope(vertexEnv)
        allPointsEnv.expandToInclude(segEnv)

        val deltaX = allPointsEnv.getWidth() * 0.2
        val deltaY = allPointsEnv.getHeight() * 0.2

        val delta = max(deltaX, deltaY)

        computeAreaEnv = Envelope(allPointsEnv)
        computeAreaEnv.expandBy(delta)
    }

    private fun computeConvexHull() {
        val fact = GeometryFactory()
        val coords = getPointArray()
        val hull = ConvexHull(coords, fact)
        convexHull = hull.getConvexHull()
    }

    private fun getPointArray(): Array<Coordinate> {
        val pts = arrayOfNulls<Coordinate>(
            initialVertices.size + segVertices.size
        )
        var index = 0
        for (v in initialVertices) {
            pts[index++] = v.getCoordinate()
        }
        for (v in segVertices) {
            pts[index++] = v.getCoordinate()
        }
        @Suppress("UNCHECKED_CAST")
        return pts as Array<Coordinate>
    }

    private fun createVertex(p: Coordinate): ConstraintVertex {
        val v = if (vertexFactory != null)
            vertexFactory!!.createVertex(p, null)
        else
            ConstraintVertex(p)
        return v
    }

    /**
     * Creates a vertex on a constraint segment
     *
     * @param p the location of the vertex to create
     * @param seg the constraint segment it lies on
     * @return the new constraint vertex
     */
    private fun createVertex(p: Coordinate, seg: Segment): ConstraintVertex {
        val v = if (vertexFactory != null)
            vertexFactory!!.createVertex(p, seg)
        else
            ConstraintVertex(p)
        v.setOnConstraint(true)
        return v
    }

    /**
     * Inserts all sites in a collection
     *
     * @param vertices a collection of ConstraintVertex
     */
    private fun insertSites(vertices: Collection<*>) {
        for (o in vertices) {
            val v = o as ConstraintVertex
            insertSite(v)
        }
    }

    private fun insertSite(v: ConstraintVertex): ConstraintVertex {
        val kdnode = kdt.insert(v.getCoordinate(), v)
        if (!kdnode.isRepeated()) {
            incDel.insertSite(v)
        } else {
            val snappedV = kdnode.getData() as ConstraintVertex
            snappedV.merge(v)
            return snappedV
        }
        return v
    }

    /**
     * Inserts a site into the triangulation, maintaining the conformal Delaunay property.
     * This can be used to further refine the triangulation if required
     * (e.g. to approximate the medial axis of the constraints,
     * or to improve the grading of the triangulation).
     *
     * @param p the location of the site to insert
     */
    fun insertSite(p: Coordinate) {
        insertSite(createVertex(p))
    }

    // ==================================================================

    /**
     * Computes the Delaunay triangulation of the initial sites.
     */
    fun formInitialDelaunay() {
        computeBoundingBox()
        val sd = QuadEdgeSubdivision(computeAreaEnv, tolerance)
        subdiv = sd
        sd.setLocator(LastFoundQuadEdgeLocator(sd))
        incDel = IncrementalDelaunayTriangulator(sd)
        insertSites(initialVertices)
    }

    // ==================================================================

    /**
     * Enforces the supplied constraints into the triangulation.
     *
     * @throws ConstraintEnforcementException
     *           if the constraints cannot be enforced
     */
    fun enforceConstraints() {
        addConstraintVertices()
        // if (true) return;

        var count = 0
        var splits = 0
        do {
            splits = enforceGabriel(segments)

            count++
        } while (splits > 0 && count < MAX_SPLIT_ITER)
        if (count == MAX_SPLIT_ITER) {
            throw ConstraintEnforcementException(
                "Too many splitting iterations while enforcing constraints.  Last split point was at: ",
                splitPt!!
            )
        }
    }

    private fun addConstraintVertices() {
        computeConvexHull()
        // insert constraint vertices as sites
        insertSites(segVertices)
    }

    private fun enforceGabriel(segsToInsert: MutableCollection<Segment>): Int {
        val newSegments = ArrayList<Segment>()
        var splits = 0
        val segsToRemove = ArrayList<Segment>()

        /**
         * On each iteration must always scan all constraint (sub)segments, since
         * some constraints may be rebroken by Delaunay triangle flipping caused by
         * insertion of another constraint. However, this process must converge
         * eventually, with no splits remaining to find.
         */
        for (seg in segsToInsert) {
            // System.out.println(seg);

            val encroachPt = findNonGabrielPoint(seg)
            // no encroachment found - segment must already be in subdivision
            if (encroachPt == null)
                continue

            // compute split point
            splitPt = splitFinder.findSplitPoint(seg, encroachPt)
            val splitVertex = createVertex(splitPt!!, seg)

            /**
             * Check whether the inserted point still equals the split pt. This will
             * not be the case if the split pt was too close to an existing site. If
             * the point was snapped, the triangulation will not respect the inserted
             * constraint - this is a failure.
             */
            val insertedVertex = insertSite(splitVertex)

            // split segment and record the new halves
            val s1 = Segment(
                seg.getStartX(), seg.getStartY(), seg.getStartZ(),
                splitVertex.getX(), splitVertex.getY(), splitVertex.getZ(), seg.getData()
            )
            val s2 = Segment(
                splitVertex.getX(), splitVertex.getY(), splitVertex.getZ(),
                seg.getEndX(), seg.getEndY(), seg.getEndZ(), seg.getData()
            )
            newSegments.add(s1)
            newSegments.add(s2)
            segsToRemove.add(seg)

            splits = splits + 1
        }
        segsToInsert.removeAll(segsToRemove)
        segsToInsert.addAll(newSegments)

        return splits
    }

    /**
     * Given a set of points stored in the kd-tree and a line segment defined by
     * two points in this set, finds a [Coordinate] in the circumcircle of
     * the line segment, if one exists. This is called the Gabriel point - if none
     * exists then the segment is said to have the Gabriel condition. Uses the
     * heuristic of finding the non-Gabriel point closest to the midpoint of the
     * segment.
     *
     * @param seg the line segment
     * @return a point which is non-Gabriel
     * or null if no point is non-Gabriel
     */
    private fun findNonGabrielPoint(seg: Segment): Coordinate? {
        val p = seg.getStart()
        val q = seg.getEnd()
        // Find the mid point on the line and compute the radius of enclosing circle
        val midPt = Coordinate((p.x + q.x) / 2.0, (p.y + q.y) / 2.0)
        val segRadius = p.distance(midPt)

        // compute envelope of circumcircle
        val env = Envelope(midPt)
        env.expandBy(segRadius)
        // Find all points in envelope
        val result = kdt.query(env)

        // For each point found, test if it falls strictly in the circle
        // find closest point
        var closestNonGabriel: Coordinate? = null
        var minDist = Double.MAX_VALUE
        for (nextNode in result) {
            val testPt = nextNode.getCoordinate()
            // ignore segment endpoints
            if (testPt.equals2D(p) || testPt.equals2D(q))
                continue

            val testRadius = midPt.distance(testPt)
            if (testRadius < segRadius) {
                // double testDist = seg.distance(testPt);
                val testDist = testRadius
                if (closestNonGabriel == null || testDist < minDist) {
                    closestNonGabriel = testPt
                    minDist = testDist
                }
            }
        }
        return closestNonGabriel
    }

    companion object {
        private fun computeVertexEnvelope(vertices: Collection<*>): Envelope {
            val env = Envelope()
            for (o in vertices) {
                val v = o as Vertex
                env.expandToInclude(v.getCoordinate())
            }
            return env
        }

        private const val MAX_SPLIT_ITER = 99
    }
}
