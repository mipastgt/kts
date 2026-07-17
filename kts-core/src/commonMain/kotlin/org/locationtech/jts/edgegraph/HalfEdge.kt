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

package org.locationtech.jts.edgegraph

import kotlin.jvm.JvmStatic

import org.locationtech.jts.algorithm.Orientation
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Quadrant
import org.locationtech.jts.io.WKTWriter
import org.locationtech.jts.util.Assert

/**
 * Represents a directed component of an edge in an [EdgeGraph].
 * HalfEdges link vertices whose locations are defined by [Coordinate]s.
 * HalfEdges start at an **origin** vertex,
 * and terminate at a **destination** vertex.
 * HalfEdges always occur in symmetric pairs, with the [sym] method
 * giving access to the oppositely-oriented component.
 * HalfEdges and the methods on them form an edge algebra,
 * which can be used to traverse and query the topology
 * of the graph formed by the edges.
 *
 * To support graphs where the edges are sequences of coordinates
 * each edge may also have a direction point supplied.
 * This is used to determine the ordering
 * of the edges around the origin.
 * HalfEdges with the same origin are ordered
 * so that the ring of edges formed by them is oriented CCW.
 *
 * By design HalfEdges carry minimal information
 * about the actual usage of the graph they represent.
 * They can be subclassed to carry more information if required.
 *
 * HalfEdges form a complete and consistent data structure by themselves,
 * but an [EdgeGraph] is useful to allow retrieving edges
 * by vertex and edge location, as well as ensuring
 * edges are created and linked appropriately.
 *
 * @author Martin Davis
 */
open class HalfEdge
/**
 * Creates a half-edge originating from a given coordinate.
 *
 * @param orig the origin coordinate
 */
(private val orig: Coordinate) {

    private lateinit var sym: HalfEdge
    private lateinit var next: HalfEdge

    /**
     * Links this edge with its sym (opposite) edge.
     * This must be done for each pair of edges created.
     *
     * @param sym the sym edge to link.
     */
    open fun link(sym: HalfEdge) {
        setSym(sym)
        sym.setSym(this)
        // set next ptrs for a single segment
        setNext(sym)
        sym.setNext(this)
    }

    /**
     * Gets the origin coordinate of this edge.
     *
     * @return the origin coordinate
     */
    open fun orig(): Coordinate {
        return orig
    }

    /**
     * Gets the destination coordinate of this edge.
     *
     * @return the destination coordinate
     */
    open fun dest(): Coordinate {
        return sym.orig
    }

    /**
     * The X component of the direction vector.
     *
     * @return the X component of the direction vector
     */
    private fun directionX(): Double {
        return directionPt().getX() - orig.getX()
    }

    /**
     * The Y component of the direction vector.
     *
     * @return the Y component of the direction vector
     */
    private fun directionY(): Double {
        return directionPt().getY() - orig.getY()
    }

    /**
     * Gets the direction point of this edge.
     * In the base case this is the dest coordinate
     * of the edge.
     * Subclasses may override to
     * allow a HalfEdge to represent an edge with more than two coordinates.
     *
     * @return the direction point for the edge
     */
    protected open fun directionPt(): Coordinate {
        // default is to assume edges have only 2 vertices
        // subclasses may override to provide an internal direction point
        return dest()
    }

    /**
     * Gets the symmetric pair edge of this edge.
     *
     * @return the symmetric pair edge
     */
    open fun sym(): HalfEdge {
        return sym
    }

    /**
     * Sets the symmetric (opposite) edge to this edge.
     *
     * @param e the sym edge to set
     */
    private fun setSym(e: HalfEdge) {
        sym = e
    }

    /**
     * Sets the next edge CCW around the destination vertex of this edge.
     *
     * @param e the next edge
     */
    private fun setNext(e: HalfEdge) {
        next = e
    }

    /**
     * Gets the next edge CCW around the
     * destination vertex of this edge,
     * originating at that vertex.
     * If the destination vertex has degree 1 then this is the **sym** edge.
     *
     * @return the next outgoing edge CCW around the destination vertex
     */
    open fun next(): HalfEdge {
        return next
    }

    /**
     * Gets the previous edge CW around the origin
     * vertex of this edge,
     * with that vertex being its destination.
     *
     * It is always true that `e.next().prev() == e`
     *
     * Note that this requires a scan of the origin edges,
     * so may not be efficient for some uses.
     *
     * @return the previous edge CW around the origin vertex
     */
    open fun prev(): HalfEdge {
        var curr = this
        var prev = this
        do {
            prev = curr
            curr = curr.oNext()
        } while (curr !== this)
        return prev.sym
    }

    /**
     * Gets the next edge CCW around the origin of this edge,
     * with the same origin.
     * If the origin vertex has degree 1 then this is the edge itself.
     *
     * `e.oNext()` is equal to `e.sym().next()`
     *
     * @return the next edge around the origin
     */
    open fun oNext(): HalfEdge {
        return sym.next
    }

    /**
     * Finds the edge starting at the origin of this edge
     * with the given dest vertex,
     * if any.
     *
     * @param dest the dest vertex to search for
     * @return the edge with the required dest vertex, if it exists,
     * or null
     */
    open fun find(dest: Coordinate): HalfEdge? {
        var oNext = this
        do {
            if (oNext.dest().equals2D(dest))
                return oNext
            oNext = oNext.oNext()
        } while (oNext !== this)
        return null
    }

    /**
     * Tests whether this edge has the given orig and dest vertices.
     *
     * @param p0 the origin vertex to test
     * @param p1 the destination vertex to test
     * @return true if the vertices are equal to the ones of this edge
     */
    open fun equals(p0: Coordinate, p1: Coordinate): Boolean {
        return orig.equals2D(p0) && sym.orig == p1
    }

    /**
     * Inserts an edge
     * into the ring of edges around the origin vertex of this edge,
     * ensuring that the edges remain ordered CCW.
     * The inserted edge must have the same origin as this edge.
     *
     * @param eAdd the edge to insert
     */
    open fun insert(eAdd: HalfEdge) {
        // If this is only edge at origin, insert it after this
        if (oNext() === this) {
            // set linkage so ring is correct
            insertAfter(eAdd)
            return
        }

        // Scan edges until insertion point is found
        val ePrev = insertionEdge(eAdd)
        ePrev!!.insertAfter(eAdd)
    }

    /**
     * Finds the insertion edge for a edge
     * being added to this origin,
     * ensuring that the star of edges
     * around the origin remains fully CCW.
     *
     * @param eAdd the edge being added
     * @return the edge to insert after
     */
    private fun insertionEdge(eAdd: HalfEdge): HalfEdge? {
        var ePrev = this
        do {
            val eNext = ePrev.oNext()
            /*
             * Case 1: General case,
             * with eNext higher than ePrev.
             *
             * Insert edge here if it lies between ePrev and eNext.
             */
            if (eNext.compareTo(ePrev) > 0 &&
                eAdd.compareTo(ePrev) >= 0 &&
                eAdd.compareTo(eNext) <= 0) {
                return ePrev
            }
            /*
             * Case 2: Origin-crossing case,
             * indicated by eNext <= ePrev.
             *
             * Insert edge here if it lies
             * in the gap between ePrev and eNext across the origin.
             */
            if (eNext.compareTo(ePrev) <= 0 &&
                (eAdd.compareTo(eNext) <= 0 || eAdd.compareTo(ePrev) >= 0)) {
                return ePrev
            }
            ePrev = eNext
        } while (ePrev !== this)
        Assert.shouldNeverReachHere()
        return null
    }

    /**
     * Insert an edge with the same origin after this one.
     * Assumes that the inserted edge is in the correct
     * position around the ring.
     *
     * @param e the edge to insert (with same origin)
     */
    private fun insertAfter(e: HalfEdge) {
        Assert.equals(orig, e.orig())
        val save = oNext()
        sym.setNext(e)
        e.sym().setNext(save)
    }

    /**
     * Tests whether the edges around the origin
     * are sorted correctly.
     * Note that edges must be strictly increasing,
     * which implies no two edges can have the same direction point.
     *
     * @return true if the origin edges are sorted correctly
     */
    open fun isEdgesSorted(): Boolean {
        // find lowest edge at origin
        val lowest = findLowest()
        var e = lowest
        // check that all edges are sorted
        do {
            val eNext = e.oNext()
            if (eNext === lowest) break
            val isSorted = eNext.compareTo(e) > 0
            if (!isSorted) {
                //int comp = eNext.compareTo(e);
                return false
            }
            e = eNext
        } while (e !== lowest)
        return true
    }

    /**
     * Finds the lowest edge around the origin,
     * using the standard edge ordering.
     *
     * @return the lowest edge around the origin
     */
    private fun findLowest(): HalfEdge {
        var lowest = this
        var e = this.oNext()
        do {
            if (e.compareTo(lowest) < 0)
                lowest = e
            e = e.oNext()
        } while (e !== this)
        return lowest
    }

    /**
     * Compares edges which originate at the same vertex
     * based on the angle they make at their origin vertex with the positive X-axis.
     * This allows sorting edges around their origin vertex in CCW order.
     */
    open fun compareTo(obj: Any?): Int {
        val e = obj as HalfEdge
        val comp = compareAngularDirection(e)
        return comp
    }

    /**
     * Implements the total order relation:
     *
     *    The angle of edge a is greater than the angle of edge b,
     *    where the angle of an edge is the angle made by
     *    the first segment of the edge with the positive x-axis
     *
     * When applied to a list of edges originating at the same point,
     * this produces a CCW ordering of the edges around the point.
     *
     * Using the obvious algorithm of computing the angle is not robust,
     * since the angle calculation is susceptible to roundoff error.
     * A robust algorithm is:
     *
     *  * First, compare the quadrants the edge vectors lie in.
     * If the quadrants are different,
     * it is trivial to determine which edge has a greater angle.
     *
     *  * if the vectors lie in the same quadrant, the
     * [Orientation.index] function
     * can be used to determine the relative orientation of the vectors.
     */
    open fun compareAngularDirection(e: HalfEdge): Int {
        val dx = directionX()
        val dy = directionY()
        val dx2 = e.directionX()
        val dy2 = e.directionY()

        // same vector
        if (dx == dx2 && dy == dy2)
            return 0

        val quadrant = Quadrant.quadrant(dx, dy)
        val quadrant2 = Quadrant.quadrant(dx2, dy2)

        /*
         * If the direction vectors are in different quadrants,
         * that determines the ordering
         */
        if (quadrant > quadrant2) return 1
        if (quadrant < quadrant2) return -1

        //--- vectors are in the same quadrant
        // Check relative orientation of direction vectors
        // this is > e if it is CCW of e
        val dir1 = directionPt()
        val dir2 = e.directionPt()
        return Orientation.index(e.orig, dir2, dir1)
    }

    /**
     * Provides a string representation of a HalfEdge.
     *
     * @return a string representation
     */
    override fun toString(): String {
        return "HE(" + orig.x + " " + orig.y +
            ", " +
            sym.orig.x + " " + sym.orig.y +
            ")"
    }

    /**
     * Provides a string representation of the edges around
     * the origin node of this edge.
     * Uses the subclass representation for each edge.
     *
     * @return a string showing the edges around the origin
     */
    open fun toStringNode(): String {
        val orig = orig()
        val dest = dest()
        val sb = StringBuilder()
        sb.append("Node( " + WKTWriter.format(orig) + " )" + "\n")
        var e = this
        do {
            sb.append("  -> $e")
            sb.append("\n")
            e = e.oNext()
        } while (e !== this)
        return sb.toString()
    }

    /**
     * Computes the degree of the origin vertex.
     * The degree is the number of edges
     * originating from the vertex.
     *
     * @return the degree of the origin vertex
     */
    open fun degree(): Int {
        var degree = 0
        var e = this
        do {
            degree++
            e = e.oNext()
        } while (e !== this)
        return degree
    }

    /**
     * Finds the first node previous to this edge, if any.
     * A node has degree `<> 2`.
     * If no such node exists (i.e. the edge is part of a ring)
     * then null is returned.
     *
     * @return an edge originating at the node prior to this edge, if any,
     *   or null if no node exists
     */
    open fun prevNode(): HalfEdge? {
        var e = this
        while (e.degree() == 2) {
            e = e.prev()
            if (e === this)
                return null
        }
        return e
    }

    companion object {
        /**
         * Creates a HalfEdge pair representing an edge
         * between two vertices located at coordinates p0 and p1.
         *
         * @param p0 a vertex coordinate
         * @param p1 a vertex coordinate
         * @return the HalfEdge with origin at p0
         */
        @JvmStatic
        fun create(p0: Coordinate, p1: Coordinate): HalfEdge {
            val e0 = HalfEdge(p0)
            val e1 = HalfEdge(p1)
            e0.link(e1)
            return e0
        }
    }
}
