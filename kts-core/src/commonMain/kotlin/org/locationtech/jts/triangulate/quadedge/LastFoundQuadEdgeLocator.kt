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

/**
 * Locates [QuadEdge]s in a [QuadEdgeSubdivision],
 * optimizing the search by starting in the
 * locality of the last edge found.
 *
 * @author Martin Davis
 */
class LastFoundQuadEdgeLocator(private val subdiv: QuadEdgeSubdivision) : QuadEdgeLocator {
    private var lastEdge: QuadEdge? = null

    init {
        init()
    }

    private fun init() {
        lastEdge = findEdge()
    }

    private fun findEdge(): QuadEdge {
        val edges = subdiv.getEdges()
        // assume there is an edge - otherwise will get an exception
        return edges.iterator().next()
    }

    /**
     * Locates an edge e, such that either v is on e, or e is an edge of a triangle containing v.
     * The search starts from the last located edge and proceeds on the general direction of v.
     */
    override fun locate(v: Vertex): QuadEdge {
        if (!lastEdge!!.isLive()) {
            init()
        }

        val e = subdiv.locateFromEdge(v, lastEdge!!)
        lastEdge = e
        return e
    }
}
