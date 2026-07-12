/*
 * Copyright (c) 2023 Martin Davis.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.locationtech.jts.operation.relateng

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Dimension
import org.locationtech.jts.geom.Location
import org.locationtech.jts.geom.Position
import org.locationtech.jts.io.WKTWriter

internal class RelateNode(private val nodePt: Coordinate) {

    /**
     * A list of the edges around the node in CCW order,
     * ordered by their CCW angle with the positive X-axis.
     */
    private val edges = ArrayList<RelateEdge>()

    fun getCoordinate(): Coordinate {
        return nodePt
    }

    fun getEdges(): List<RelateEdge> {
        return edges
    }

    fun addEdges(nss: List<NodeSection>) {
        for (ns in nss) {
            addEdges(ns)
        }
    }

    fun addEdges(ns: NodeSection) {
        when (ns.dimension()) {
            Dimension.L -> {
                addLineEdge(ns.isA(), ns.getVertex(0))
                addLineEdge(ns.isA(), ns.getVertex(1))
            }
            Dimension.A -> {
                //-- assumes node edges have CW orientation (as per JTS norm)
                //-- entering edge - interior on L
                val e0 = addAreaEdge(ns.isA(), ns.getVertex(0), false)
                //-- exiting edge - interior on R
                val e1 = addAreaEdge(ns.isA(), ns.getVertex(1), true)

                val index0 = if (e0 == null) -1 else edges.indexOf(e0)
                val index1 = if (e1 == null) -1 else edges.indexOf(e1)
                updateEdgesInArea(ns.isA(), index0, index1)
                updateIfAreaPrev(ns.isA(), index0)
                updateIfAreaNext(ns.isA(), index1)
            }
        }
    }

    private fun updateEdgesInArea(isA: Boolean, indexFrom: Int, indexTo: Int) {
        var index = nextIndex(edges, indexFrom)
        while (index != indexTo) {
            val edge = edges[index]
            edge.setAreaInterior(isA)
            index = nextIndex(edges, index)
        }
    }

    private fun updateIfAreaPrev(isA: Boolean, index: Int) {
        val indexPrev = prevIndex(edges, index)
        val edgePrev = edges[indexPrev]
        if (edgePrev.isInterior(isA, Position.LEFT)) {
            val edge = edges[index]
            edge.setAreaInterior(isA)
        }
    }

    private fun updateIfAreaNext(isA: Boolean, index: Int) {
        val indexNext = nextIndex(edges, index)
        val edgeNext = edges[indexNext]
        if (edgeNext.isInterior(isA, Position.RIGHT)) {
            val edge = edges[index]
            edge.setAreaInterior(isA)
        }
    }

    private fun addLineEdge(isA: Boolean, dirPt: Coordinate?): RelateEdge? {
        return addEdge(isA, dirPt, Dimension.L, false)
    }

    private fun addAreaEdge(isA: Boolean, dirPt: Coordinate?, isForward: Boolean): RelateEdge? {
        return addEdge(isA, dirPt, Dimension.A, isForward)
    }

    /**
     * Adds or merges an edge to the node.
     *
     * @return the created or merged edge for this point
     */
    private fun addEdge(isA: Boolean, dirPt: Coordinate?, dim: Int, isForward: Boolean): RelateEdge? {
        //-- check for well-formed edge - skip null or zero-len input
        if (dirPt == null)
            return null
        if (nodePt.equals2D(dirPt))
            return null

        var insertIndex = -1
        for (i in 0 until edges.size) {
            val e = edges[i]
            val comp = e.compareToEdge(dirPt)
            if (comp == 0) {
                e.merge(isA, dirPt, dim, isForward)
                return e
            }
            if (comp == 1) {
                //-- found further edge, so insert a new edge at this position
                insertIndex = i
                break
            }
        }
        //-- add a new edge
        val e = RelateEdge.create(this, dirPt, isA, dim, isForward)
        if (insertIndex < 0) {
            //-- add edge at end of list
            edges.add(e)
        } else {
            //-- add edge before higher edge found
            edges.add(insertIndex, e)
        }
        return e
    }

    /**
     * Computes the final topology for the edges around this node.
     *
     * @param isAreaInteriorA true if the node is in the interior of A
     * @param isAreaInteriorB true if the node is in the interior of B
     */
    fun finish(isAreaInteriorA: Boolean, isAreaInteriorB: Boolean) {
        finishNode(RelateGeometry.GEOM_A, isAreaInteriorA)
        finishNode(RelateGeometry.GEOM_B, isAreaInteriorB)
    }

    private fun finishNode(isA: Boolean, isAreaInterior: Boolean) {
        if (isAreaInterior) {
            RelateEdge.setAreaInterior(edges, isA)
        } else {
            val startIndex = RelateEdge.findKnownEdgeIndex(edges, isA)
            //-- only interacting nodes are finished, so this should never happen
            propagateSideLocations(isA, startIndex)
        }
    }

    private fun propagateSideLocations(isA: Boolean, startIndex: Int) {
        var currLoc = edges[startIndex].location(isA, Position.LEFT)
        //-- edges are stored in CCW order
        var index = nextIndex(edges, startIndex)
        while (index != startIndex) {
            val e = edges[index]
            e.setUnknownLocations(isA, currLoc)
            currLoc = e.location(isA, Position.LEFT)
            index = nextIndex(edges, index)
        }
    }

    override fun toString(): String {
        val buf = StringBuilder()
        buf.append("Node[" + WKTWriter.toPoint(nodePt) + "]:")
        buf.append("\n")
        for (e in edges) {
            buf.append(e.toString())
            buf.append("\n")
        }
        return buf.toString()
    }

    fun hasExteriorEdge(isA: Boolean): Boolean {
        for (e in edges) {
            if (Location.EXTERIOR == e.location(isA, Position.LEFT) ||
                Location.EXTERIOR == e.location(isA, Position.RIGHT)
            ) {
                return true
            }
        }
        return false
    }

    companion object {
        private fun prevIndex(list: List<RelateEdge>, index: Int): Int {
            if (index > 0)
                return index - 1
            //-- index == 0
            return list.size - 1
        }

        private fun nextIndex(list: List<RelateEdge>, i: Int): Int {
            if (i >= list.size - 1) {
                return 0
            }
            return i + 1
        }
    }
}
