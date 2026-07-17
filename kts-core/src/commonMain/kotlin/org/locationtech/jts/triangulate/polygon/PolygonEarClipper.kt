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

import org.locationtech.jts.algorithm.Angle
import org.locationtech.jts.algorithm.Orientation
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.CoordinateList
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Polygon
import org.locationtech.jts.geom.Triangle
import org.locationtech.jts.index.VertexSequencePackedRtree
import org.locationtech.jts.triangulate.tri.Tri

/**
 * Triangulates a polygon using the Ear-Clipping technique.
 * The polygon is provided as a closed list of contiguous vertices
 * defining its boundary.
 * The vertices must have clockwise orientation.
 *
 * The polygon boundary must not self-cross,
 * but may self-touch at points or along an edge.
 * It may contain repeated points, which are treated as a single vertex.
 * By default every vertex is triangulated,
 * including ones which are "flat" (the adjacent segments are collinear).
 * These can be removed by setting [setSkipFlatCorners]
 *
 * The polygon representation does not allow holes.
 * Polygons with holes can be triangulated by preparing them
 * with [PolygonHoleJoiner].
 *
 * @author Martin Davis
 */
internal class PolygonEarClipper
/**
 * Creates a new ear-clipper instance.
 *
 * @param polyShell the polygon vertices to process
 *
 * The polygon vertices are provided in CW orientation.
 * Thus for convex interior angles
 * the vertices forming the angle are in CW orientation.
 */
(private val vertex: Array<Coordinate>) {

    private var isFlatCornersSkipped = false

    private val vertexNext: IntArray
    private var vertexSize: Int
    // first available vertex index
    private var vertexFirst: Int

    // indices for current corner
    private lateinit var cornerIndex: IntArray

    /**
     * Indexing vertices improves ear intersection testing performance.
     * The polyShell vertices are contiguous, so are suitable for an SPRtree.
     * Note that a KDtree cannot be used because the vertex indices must be stored
     * and duplicates must be stored.
     */
    private val vertexCoordIndex: VertexSequencePackedRtree

    init {
        // init working storage
        vertexSize = vertex.size - 1
        vertexNext = createNextLinks(vertexSize)
        vertexFirst = 0

        vertexCoordIndex = VertexSequencePackedRtree(vertex)
    }

    /**
     * Sets whether flat corners formed by collinear adjacent line segments
     * are included in the triangulation.
     * Skipping flat corners reduces the number of triangles in the output.
     * However, it produces a triangulation which does not include
     * all input vertices.  This may be undesirable for downstream processes
     * (such as computing a Constrained Delaunay Triangulation for
     * purposes of computing the medial axis).
     *
     * The default is to include all vertices in the result triangulation.
     * This still produces a valid triangulation, with no zero-area triangles.
     *
     * Note that repeated vertices are always skipped.
     *
     * @param isFlatCornersSkipped whether to skip collinear vertices
     */
    fun setSkipFlatCorners(isFlatCornersSkipped: Boolean) {
        this.isFlatCornersSkipped = isFlatCornersSkipped
    }

    fun compute(): MutableList<Tri> {
        val triList = ArrayList<Tri>()

        /**
         * Count scanned corners, to catch infinite loops
         * (which indicate an algorithm bug)
         */
        var cornerScanCount = 0

        initCornerIndex()
        @Suppress("UNCHECKED_CAST")
        val corner = arrayOfNulls<Coordinate>(3) as Array<Coordinate>
        fetchCorner(corner)

        /*
         * Scan continuously around vertex ring,
         * until all ears have been found.
         */
        while (true) {
            /*
             * Non-convex corner- remove if flat, or skip
             * (a concave corner will turn into a convex corner
             * after enough ears are removed)
             */
            if (!isConvex(corner)) {
                // remove the corner if it is invalid or flat (if required)
                val isCornerRemoved = isCornerInvalid(corner) ||
                    (isFlatCornersSkipped && isFlat(corner))
                if (isCornerRemoved) {
                    //System.out.println(WKTWriter.toLineString(corner));
                    removeCorner()
                }
                cornerScanCount++
                if (cornerScanCount > 2 * vertexSize) {
                    //System.out.println(toGeometry());
                    //System.out.println(WKTWriter.toLineString(corner));
                    throw IllegalStateException("Unable to find a convex corner")
                }
            } else if (isValidEar(cornerIndex[1], corner)) {
                /*
                 * Convex corner - check if it is a valid ear
                 */
                triList.add(Tri.create(corner))
                removeCorner()
                cornerScanCount = 0
            }
            if (cornerScanCount > 2 * vertexSize) {
                //System.out.println(toGeometry());
                throw IllegalStateException("Unable to find a valid ear")
            }

            //--- done when all corners are processed and removed
            if (vertexSize < 3) {
                return triList
            }

            /*
             * Skip to next corner.
             * This is done even after an ear is removed,
             * since that creates fewer skinny triangles.
             */
            nextCorner(corner)
        }
    }

    private fun isValidEar(cornerIndex: Int, corner: Array<Coordinate>): Boolean {
        val intApexIndex = findIntersectingVertex(cornerIndex, corner)
        //--- no intersections found
        if (intApexIndex == NO_VERTEX_INDEX)
            return true
        //--- check for duplicate corner apex vertex
        if (vertex[intApexIndex].equals2D(corner[1])) {
            //--- a duplicate corner vertex requires a full scan
            return isValidEarScan(cornerIndex, corner)
        }
        //-- vertex is contained in corner, so it is not a valid ear
        return false
    }

    /**
     * Finds a vertex contained in the corner triangle, if any.
     * Uses the vertex spatial index for efficiency.
     *
     * Also finds any vertex which is a duplicate of the corner apex vertex.
     * This requires a full scan of the vertices to confirm ear is valid.
     * This is usually a rare situation, so has little impact on performance.
     *
     * @param cornerIndex the index of the corner apex vertex
     * @param corner the corner vertices
     * @return the index of an intersecting or duplicate vertex, or [NO_VERTEX_INDEX] if none
     */
    private fun findIntersectingVertex(cornerIndex: Int, corner: Array<Coordinate>): Int {
        val cornerEnv = envelope(corner)
        val result = vertexCoordIndex.query(cornerEnv)

        var dupApexIndex = NO_VERTEX_INDEX
        //--- check for duplicate vertices
        for (i in result.indices) {
            val vertIndex = result[i]

            if (vertIndex == cornerIndex ||
                vertIndex == vertex.size - 1 ||
                isRemoved(vertIndex))
                continue

            val v = vertex[vertIndex]
            /*
             * If the vertex is equal to the corner apex, record it.
             * This can happen where the polygon ring self-touches,
             * usually due to hole joining.
             * This will require a full scan to check the incident segments.
             * So only report this if no properly intersecting vertex is found,
             * for efficiency.
             */
            if (v.equals2D(corner[1])) {
                dupApexIndex = vertIndex
            }
            //--- don't need to check other corner vertices
            else if (v.equals2D(corner[0]) || v.equals2D(corner[2])) {
                continue
            }
            //--- this is a properly intersecting vertex
            else if (Triangle.intersects(corner[0], corner[1], corner[2], v))
                return vertIndex
        }
        if (dupApexIndex != NO_VERTEX_INDEX) {
            return dupApexIndex
        }
        return NO_VERTEX_INDEX
    }

    /**
     * Scan all vertices in current ring to check if any are duplicates
     * of the corner apex vertex, and if so whether the corner ear
     * intersects the adjacent segments and thus is invalid.
     *
     * @param cornerIndex the index of the corner apex
     * @param corner the corner vertices
     * @return true if the corner ia a valid ear
     */
    private fun isValidEarScan(cornerIndex: Int, corner: Array<Coordinate>): Boolean {
        val cornerAngle = Angle.angleBetweenOriented(corner[0], corner[1], corner[2])

        var currIndex = nextIndex(vertexFirst)
        var prevIndex = vertexFirst
        var vPrev = vertex[prevIndex]
        for (i in 0 until vertexSize) {
            val v = vertex[currIndex]
            /*
             * Because of hole-joining vertices can occur more than once.
             * If vertex is same as corner[1],
             * check whether either adjacent edge lies inside the ear corner.
             * If so the ear is invalid.
             */
            if (currIndex != cornerIndex &&
                v.equals2D(corner[1])) {
                val vNext = vertex[nextIndex(currIndex)]

                //TODO: for robustness use segment orientation instead
                val aOut = Angle.angleBetweenOriented(corner[0], corner[1], vNext)
                val aIn = Angle.angleBetweenOriented(corner[0], corner[1], vPrev)
                if (aOut > 0 && aOut < cornerAngle) {
                    return false
                }
                if (aIn > 0 && aIn < cornerAngle) {
                    return false
                }
                if (aOut == 0.0 && aIn == cornerAngle) {
                    return false
                }
            }

            //--- move to next vertex
            vPrev = v
            prevIndex = currIndex
            currIndex = nextIndex(currIndex)
        }
        return true
    }

    /**
     * Remove the corner apex vertex and update the candidate corner location.
     */
    private fun removeCorner() {
        val cornerApexIndex = cornerIndex[1]
        if (vertexFirst == cornerApexIndex) {
            vertexFirst = vertexNext[cornerApexIndex]
        }
        vertexNext[cornerIndex[0]] = vertexNext[cornerApexIndex]
        vertexCoordIndex.remove(cornerApexIndex)
        vertexNext[cornerApexIndex] = NO_VERTEX_INDEX
        vertexSize--
        //-- adjust following corner indexes
        cornerIndex[1] = nextIndex(cornerIndex[0])
        cornerIndex[2] = nextIndex(cornerIndex[1])
    }

    private fun isRemoved(vertexIndex: Int): Boolean {
        return NO_VERTEX_INDEX == vertexNext[vertexIndex]
    }

    private fun initCornerIndex() {
        cornerIndex = IntArray(3)
        cornerIndex[0] = 0
        cornerIndex[1] = 1
        cornerIndex[2] = 2
    }

    /**
     * Fetch the corner vertices from the indices.
     *
     * @param cornerVertex an array for the corner vertices
     */
    private fun fetchCorner(cornerVertex: Array<Coordinate>) {
        cornerVertex[0] = vertex[cornerIndex[0]]
        cornerVertex[1] = vertex[cornerIndex[1]]
        cornerVertex[2] = vertex[cornerIndex[2]]
    }

    /**
     * Move to next corner.
     */
    private fun nextCorner(cornerVertex: Array<Coordinate>) {
        if (vertexSize < 3) {
            return
        }
        cornerIndex[0] = nextIndex(cornerIndex[0])
        cornerIndex[1] = nextIndex(cornerIndex[0])
        cornerIndex[2] = nextIndex(cornerIndex[1])
        fetchCorner(cornerVertex)
    }

    /**
     * Get the index of the next available shell coordinate starting from the given
     * index.
     *
     * @param index candidate position
     * @return index of the next available shell coordinate
     */
    private fun nextIndex(index: Int): Int {
        return vertexNext[index]
    }

    fun toGeometry(): Polygon {
        val fact = GeometryFactory()
        val coordList = CoordinateList()
        var index = vertexFirst
        for (i in 0 until vertexSize) {
            val v = vertex[index]
            index = nextIndex(index)
            // if (i < shellCoordAvailable.length && shellCoordAvailable.get(i))
            coordList.add(v, true)
        }
        coordList.closeRing()
        return fact.createPolygon(fact.createLinearRing(coordList.toCoordinateArray()))
    }

    companion object {
        private const val NO_VERTEX_INDEX = -1

        /**
         * Triangulates a polygon via ear-clipping.
         *
         * @param polyShell the vertices of the polygon
         * @return a list of the Tris
         */
        fun triangulate(polyShell: Array<Coordinate>): MutableList<Tri> {
            val clipper = PolygonEarClipper(polyShell)
            return clipper.compute()
        }

        private fun createNextLinks(size: Int): IntArray {
            val next = IntArray(size)
            for (i in 0 until size) {
                next[i] = i + 1
            }
            next[size - 1] = 0
            return next
        }

        private fun envelope(corner: Array<Coordinate>): Envelope {
            val cornerEnv = Envelope(corner[0], corner[1])
            cornerEnv.expandToInclude(corner[2])
            return cornerEnv
        }

        private fun isConvex(pts: Array<Coordinate>): Boolean {
            return Orientation.CLOCKWISE == Orientation.index(pts[0], pts[1], pts[2])
        }

        private fun isFlat(pts: Array<Coordinate>): Boolean {
            return Orientation.COLLINEAR == Orientation.index(pts[0], pts[1], pts[2])
        }

        /**
         * Detects if a corner has repeated points (AAB or ABB), or is collapsed (ABA).
         * @param pts the corner points
         * @return true if the corner is flat or collapsed
         */
        private fun isCornerInvalid(pts: Array<Coordinate>): Boolean {
            return pts[1].equals2D(pts[0]) || pts[1].equals2D(pts[2]) || pts[0].equals2D(pts[2])
        }
    }
}
