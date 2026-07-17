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

package org.locationtech.jts.dissolve

import kotlin.jvm.JvmStatic


import org.locationtech.jts.edgegraph.HalfEdge
import org.locationtech.jts.edgegraph.MarkHalfEdge
import org.locationtech.jts.geom.CoordinateList
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryComponentFilter
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LineString

/**
 * Dissolves the linear components
 * from a collection of [Geometry]s
 * into a set of maximal-length [LineString]s
 * in which every unique segment appears once only.
 * The output linestrings run between node vertices
 * of the input, which are vertices which have
 * either degree 1, or degree 3 or greater.
 *
 * Use cases for dissolving linear components
 * include generalization
 * (in particular, simplifying polygonal coverages),
 * and visualization
 * (in particular, avoiding symbology conflicts when
 * depicting shared polygon boundaries).
 *
 * This class does **not** node the input lines.
 * If there are line segments crossing in the input,
 * they will still cross in the output.
 *
 * @author Martin Davis
 */
class LineDissolver {

    private var result: Geometry? = null
    private var factory: GeometryFactory? = null
    private val graph: DissolveEdgeGraph = DissolveEdgeGraph()
    private val lines: MutableList<Geometry> = ArrayList()

    /**
     * Adds a [Geometry] to be dissolved.
     * Any number of geometries may be added by calling this method multiple times.
     * Any type of Geometry may be added.  The constituent linework will be
     * extracted to be dissolved.
     *
     * @param geometry geometry to be line-merged
     */
    fun add(geometry: Geometry) {
        geometry.apply(object : GeometryComponentFilter {
            override fun filter(geom: Geometry) {
                if (geom is LineString) {
                    add(geom)
                }
            }
        })
    }

    /**
     * Adds a collection of Geometries to be processed. May be called multiple times.
     * Any dimension of Geometry may be added; the constituent linework will be
     * extracted.
     *
     * @param geometries the geometries to be line-merged
     */
    fun add(geometries: Collection<*>) {
        for (o in geometries) {
            val geometry = o as Geometry
            add(geometry)
        }
    }

    private fun add(lineString: LineString) {
        if (factory == null) {
            this.factory = lineString.getFactory()
        }
        val seq = lineString.getCoordinateSequence()
        var doneStart = false
        for (i in 1 until seq.size()) {
            val e = graph.addEdge(seq.getCoordinate(i - 1), seq.getCoordinate(i)) as DissolveHalfEdge?
            // skip zero-length edges
            if (e == null) continue
            /*
             * Record source initial segments, so that they can be reflected in output when needed
             * (i.e. during formation of isolated rings)
             */
            if (!doneStart) {
                e.setStart()
                doneStart = true
            }
        }
    }

    /**
     * Gets the dissolved result as a MultiLineString.
     *
     * @return the dissolved lines
     */
    fun getResult(): Geometry {
        if (result == null)
            computeResult()
        return result!!
    }

    private fun computeResult() {
        val edges = graph.getVertexEdges()
        for (e in edges) {
            if (MarkHalfEdge.isMarked(e)) continue
            process(e)
        }
        result = factory!!.buildGeometry(lines)
    }

    private val nodeEdgeStack = ArrayDeque<HalfEdge>()

    private fun process(e: HalfEdge) {
        var eNode = e.prevNode()
        // if edge is in a ring, just process this edge
        if (eNode == null)
            eNode = e
        stackEdges(eNode)
        // extract lines from node edges in stack
        buildLines()
    }

    /**
     * For each edge in stack
     * (which must originate at a node)
     * extracts the line it initiates.
     */
    private fun buildLines() {
        while (!nodeEdgeStack.isEmpty()) {
            val e = nodeEdgeStack.removeLast()
            if (MarkHalfEdge.isMarked(e))
                continue
            buildLine(e)
        }
    }

    private var ringStartEdge: DissolveHalfEdge? = null

    /**
     * Updates the tracked ringStartEdge
     * if the given edge has a lower origin
     * (using the standard [org.locationtech.jts.geom.Coordinate] ordering).
     *
     * Identifying the lowest starting node meets two goals:
     *
     *  * It ensures that isolated input rings are created using the original node and orientation
     *  * For isolated rings formed from multiple input linestrings,
     * it provides a canonical node and orientation for the output
     * (rather than essentially random, and thus hard to test).
     *
     * @param edge
     */
    private fun updateRingStartEdge(edge: DissolveHalfEdge) {
        var e = edge
        if (!e.isStart()) {
            e = e.sym() as DissolveHalfEdge
            if (!e.isStart()) return
        }
        // here e is known to be a start edge
        if (ringStartEdge == null) {
            ringStartEdge = e
            return
        }
        if (e.orig().compareTo(ringStartEdge!!.orig()) < 0) {
            ringStartEdge = e
        }
    }

    /**
     * Builds a line starting from the given edge.
     * The start edge origin is a node (valence = 1 or >= 3),
     * unless it is part of a pure ring.
     * A pure ring has no other incident lines.
     * In this case the start edge may occur anywhere on the ring.
     *
     * The line is built up to the next node encountered,
     * or until the start edge is re-encountered
     * (which happens if the edges form a ring).
     *
     * @param eStart
     */
    private fun buildLine(eStart: HalfEdge) {
        val line = CoordinateList()
        var e = eStart as DissolveHalfEdge
        ringStartEdge = null

        MarkHalfEdge.markBoth(e)
        line.add(e.orig().copy(), false)
        // scan along the path until a node is found (if one exists)
        while (e.sym().degree() == 2) {
            updateRingStartEdge(e)
            val eNext = e.next() as DissolveHalfEdge
            // check if edges form a ring - if so, we're done
            if (eNext === eStart) {
                buildRing(ringStartEdge!!)
                return
            }
            // add point to line, and move to next edge
            line.add(eNext.orig().copy(), false)
            e = eNext
            MarkHalfEdge.markBoth(e)
        }
        // add final node
        line.add(e.dest().clone(), false)

        // queue up the final node edges
        stackEdges(e.sym())
        // store the scanned line
        addLine(line)
    }

    private fun buildRing(eStartRing: HalfEdge) {
        val line = CoordinateList()
        var e = eStartRing

        line.add(e.orig().copy(), false)
        // scan along the path until a node is found (if one exists)
        while (e.sym().degree() == 2) {
            val eNext = e.next()
            // check if edges form a ring - if so, we're done
            if (eNext === eStartRing)
                break

            // add point to line, and move to next edge
            line.add(eNext.orig().copy(), false)
            e = eNext
        }
        // add final node
        line.add(e.dest().copy(), false)

        // store the scanned line
        addLine(line)
    }

    private fun addLine(line: CoordinateList) {
        lines.add(factory!!.createLineString(line.toCoordinateArray()))
    }

    /**
     * Adds edges around this node to the stack.
     *
     * @param node
     */
    private fun stackEdges(node: HalfEdge) {
        var e = node
        do {
            if (!MarkHalfEdge.isMarked(e))
                nodeEdgeStack.add(e)
            e = e.oNext()
        } while (e !== node)
    }

    companion object {
        /**
         * Dissolves the linear components in a geometry.
         *
         * @param g the geometry to dissolve
         * @return the dissolved lines
         */
        @JvmStatic
        fun dissolve(g: Geometry): Geometry {
            val d = LineDissolver()
            d.add(g)
            return d.getResult()
        }
    }
}
