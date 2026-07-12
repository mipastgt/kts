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
 * A [HalfEdge] which supports
 * marking edges with a boolean flag.
 * Useful for algorithms which perform graph traversals.
 *
 * @author Martin Davis
 */
open class MarkHalfEdge
/**
 * Creates a new marked edge.
 *
 * @param orig the coordinate of the edge origin
 */
(orig: Coordinate) : HalfEdge(orig) {

    private var marked = false

    /**
     * Tests whether this edge is marked.
     *
     * @return true if this edge is marked
     */
    open fun isMarked(): Boolean {
        return marked
    }

    /**
     * Marks this edge.
     */
    open fun mark() {
        marked = true
    }

    /**
     * Sets the value of the mark on this edge.
     *
     * @param isMarked the mark value to set
     */
    open fun setMark(isMarked: Boolean) {
        this.marked = isMarked
    }

    companion object {
        /**
         * Tests whether the given edge is marked.
         *
         * @param e the edge to test
         * @return true if the edge is marked
         */
        @JvmStatic
        fun isMarked(e: HalfEdge): Boolean {
            return (e as MarkHalfEdge).isMarked()
        }

        /**
         * Marks the given edge.
         *
         * @param e the edge to mark
         */
        @JvmStatic
        fun mark(e: HalfEdge) {
            (e as MarkHalfEdge).mark()
        }

        /**
         * Sets the mark for the given edge to a boolean value.
         *
         * @param e the edge to set
         * @param isMarked the mark value
         */
        @JvmStatic
        fun setMark(e: HalfEdge, isMarked: Boolean) {
            (e as MarkHalfEdge).setMark(isMarked)
        }

        /**
         * Sets the mark for the given edge pair to a boolean value.
         *
         * @param e an edge of the pair to update
         * @param isMarked the mark value to set
         */
        @JvmStatic
        fun setMarkBoth(e: HalfEdge, isMarked: Boolean) {
            (e as MarkHalfEdge).setMark(isMarked)
            (e.sym() as MarkHalfEdge).setMark(isMarked)
        }

        /**
         * Marks the edges in a pair.
         *
         * @param e an edge of the pair to mark
         */
        @JvmStatic
        fun markBoth(e: HalfEdge) {
            (e as MarkHalfEdge).mark()
            (e.sym() as MarkHalfEdge).mark()
        }
    }
}
