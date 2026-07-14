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
 * A framework to visit sets of edge-connected [QuadEdgeTriangle]s in breadth-first order
 *
 * @author Martin Davis
 */
class EdgeConnectedTriangleTraversal {
    private val triQueue = ArrayDeque<QuadEdgeTriangle>()

    fun init(tri: QuadEdgeTriangle) {
        triQueue.addLast(tri)
    }

    /**
     * Called to initialize the traversal queue with a given set of [QuadEdgeTriangle]s
     *
     * @param tris a collection of QuadEdgeTriangle
     */
    fun init(tris: Collection<QuadEdgeTriangle>) {
        triQueue.addAll(tris)
    }

    /**
     * Subclasses can call this method to add a triangle to the end of the queue. This is useful for
     * initializing the queue to a chosen set of triangles.
     *
     * @param tri a triangle
     */
    /*
     * protected void addLast(QuadEdgeTriangle tri) { triQueue.addLast(tri); }
     */

    /**
     * Subclasses call this method to perform the visiting process.
     */
    fun visitAll(visitor: TraversalVisitor) {
        while (!triQueue.isEmpty()) {
            val tri = triQueue.removeFirst()
            process(tri, visitor)
        }
    }

    private fun process(currTri: QuadEdgeTriangle, visitor: TraversalVisitor) {
        currTri.getNeighbours()
        for (i in 0..2) {
            val neighTri = currTri.getEdge(i).sym().getData() as QuadEdgeTriangle?
            if (neighTri == null)
                continue
            if (visitor.visit(currTri, i, neighTri))
                triQueue.addLast(neighTri)
        }
    }
}
