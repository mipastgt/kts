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

import org.locationtech.jts.geom.Coordinate

/**
 * A graph comprised of [HalfEdge]s.
 * It supports tracking the vertices in the graph
 * via edges incident on them,
 * to allow efficient lookup of edges and vertices.
 *
 * This class may be subclassed to use a
 * different subclass of HalfEdge,
 * by overriding [createEdge].
 * If additional logic is required to initialize
 * edges then [addEdge]
 * can be overridden as well.
 *
 * @author Martin Davis
 */
open class EdgeGraph {
    private val vertexMap: MutableMap<Coordinate, HalfEdge> = HashMap()

    /**
     * Creates a single HalfEdge.
     * Override to use a different HalfEdge subclass.
     *
     * @param orig the origin location
     * @return a new HalfEdge with the given origin
     */
    protected open fun createEdge(orig: Coordinate): HalfEdge {
        return HalfEdge(orig)
    }

    /**
     * Creates a HalfEge pair, using the HalfEdge type of the graph subclass.
     *
     * @param p0
     * @param p1
     * @return
     */
    private fun create(p0: Coordinate, p1: Coordinate): HalfEdge {
        val e0 = createEdge(p0)
        val e1 = createEdge(p1)
        e0.link(e1)
        return e0
    }

    /**
     * Adds an edge between the coordinates orig and dest
     * to this graph.
     * Only valid edges can be added (in particular, zero-length segments cannot be added)
     *
     * @param orig the edge origin location
     * @param dest the edge destination location.
     * @return the created edge
     * or null if the edge was invalid and not added
     *
     * @see isValidEdge
     */
    open fun addEdge(orig: Coordinate, dest: Coordinate): HalfEdge? {
        if (!isValidEdge(orig, dest)) return null

        /**
         * Attempt to find the edge already in the graph.
         * Return it if found.
         * Otherwise, use a found edge with same origin (if any) to construct new edge.
         */
        val eAdj = vertexMap[orig]
        var eSame: HalfEdge? = null
        if (eAdj != null) {
            eSame = eAdj.find(dest)
        }
        if (eSame != null) {
            return eSame
        }

        val e = insert(orig, dest, eAdj)
        return e
    }

    /**
     * Inserts an edge not already present into the graph.
     *
     * @param orig the edge origin location
     * @param dest the edge destination location
     * @param eAdj an existing edge with same orig (if any)
     * @return the created edge
     */
    private fun insert(orig: Coordinate, dest: Coordinate, eAdj: HalfEdge?): HalfEdge {
        // edge does not exist, so create it and insert in graph
        val e = create(orig, dest)
        if (eAdj != null) {
            eAdj.insert(e)
        } else {
            // add halfedges to to map
            vertexMap.put(orig, e)
        }

        val eAdjDest = vertexMap[dest]
        if (eAdjDest != null) {
            eAdjDest.insert(e.sym())
        } else {
            vertexMap.put(dest, e.sym())
        }
        return e
    }

    /**
     * Gets all [HalfEdge]s in the graph.
     * Both edges of edge pairs are included.
     *
     * @return a collection of the graph edges
     */
    open fun getVertexEdges(): MutableCollection<HalfEdge> {
        return vertexMap.values
    }

    /**
     * Finds an edge in this graph with the given origin
     * and destination, if one exists.
     *
     * @param orig the origin location
     * @param dest the destination location.
     * @return an edge with the given orig and dest, or null if none exists
     */
    open fun findEdge(orig: Coordinate, dest: Coordinate): HalfEdge? {
        val e = vertexMap[orig]
        if (e == null) return null
        return e.find(dest)
    }

    companion object {
        /**
         * Tests if the given coordinates form a valid edge (with non-zero length).
         *
         * @param orig the start coordinate
         * @param dest the end coordinate
         * @return true if the edge formed is valid
         */
        @JvmStatic
        fun isValidEdge(orig: Coordinate, dest: Coordinate): Boolean {
            val cmp = dest.compareTo(orig)
            return cmp != 0
        }
    }
}
