/*
 * Copyright (c) 2024 Martin Davis.
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

import org.locationtech.jts.algorithm.PolygonNodeTopology
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Dimension
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.io.WKTWriter

/**
 * Represents a computed node along with the incident edges on either side of
 * it (if they exist).
 *
 * @author Martin Davis
 *
 */
class NodeSection(
    private val isGeomA: Boolean,
    private val dim: Int,
    private val id: Int,
    private val ringId: Int,
    private val poly: Geometry?,
    private val nodeAtVertex: Boolean,
    private val v0: Coordinate?,
    private val nodePt: Coordinate,
    private val v1: Coordinate?
) : Comparable<NodeSection> {

    fun getVertex(i: Int): Coordinate? {
        return if (i == 0) v0 else v1
    }

    fun nodePt(): Coordinate {
        return nodePt
    }

    fun dimension(): Int {
        return dim
    }

    fun id(): Int {
        return id
    }

    fun ringId(): Int {
        return ringId
    }

    /**
     * Gets the polygon this section is part of.
     * Will be null if section is not on a polygon boundary.
     *
     * @return the associated polygon, or null
     */
    fun getPolygonal(): Geometry? {
        return poly
    }

    fun isShell(): Boolean {
        return ringId == 0
    }

    fun isArea(): Boolean {
        return dim == Dimension.A
    }

    fun isA(): Boolean {
        return isGeomA
    }

    fun isSameGeometry(ns: NodeSection): Boolean {
        return isA() == ns.isA()
    }

    fun isSamePolygon(ns: NodeSection): Boolean {
        return isA() == ns.isA() && id() == ns.id()
    }

    fun isNodeAtVertex(): Boolean {
        return nodeAtVertex
    }

    fun isProper(): Boolean {
        return !nodeAtVertex
    }

    override fun toString(): String {
        val geomName = RelateGeometry.name(isGeomA)
        val atVertexInd = if (nodeAtVertex) "-V-" else "---"
        val polyId = if (id >= 0) "[" + id + ":" + ringId + "]" else ""
        return "$geomName$dim$polyId: ${edgeRep(v0, nodePt)} $atVertexInd ${edgeRep(nodePt, v1)}"
    }

    private fun edgeRep(p0: Coordinate?, p1: Coordinate?): String {
        if (p0 == null || p1 == null)
            return "null"
        return WKTWriter.toLineString(p0, p1)
    }

    /**
     * Compare node sections by parent geometry, dimension, element id and ring id,
     * and edge vertices.
     * Sections are assumed to be at the same node point.
     */
    override fun compareTo(o: NodeSection): Int {
        // Assert: nodePt.equals2D(o.nodePt())

        // sort A before B
        if (isGeomA != o.isGeomA) {
            if (isGeomA) return -1
            return 1
        }
        //-- sort on dimensions
        val compDim = dim.compareTo(o.dim)
        if (compDim != 0) return compDim

        //-- sort on id and ring id
        val compId = id.compareTo(o.id)
        if (compId != 0) return compId

        val compRingId = ringId.compareTo(o.ringId)
        if (compRingId != 0) return compRingId

        //-- sort on edge coordinates
        val compV0 = compareWithNull(v0, o.v0)
        if (compV0 != 0) return compV0

        return compareWithNull(v1, o.v1)
    }

    /**
     * Compares sections by the angle the entering edge makes with the positive X axis.
     */
    class EdgeAngleComparator : Comparator<NodeSection> {

        override fun compare(ns1: NodeSection, ns2: NodeSection): Int {
            return PolygonNodeTopology.compareAngle(ns1.nodePt, ns1.getVertex(0)!!, ns2.getVertex(0)!!)
        }
    }

    companion object {
        fun isAreaArea(a: NodeSection, b: NodeSection): Boolean {
            return a.dimension() == Dimension.A && b.dimension() == Dimension.A
        }

        fun isProper(a: NodeSection, b: NodeSection): Boolean {
            return a.isProper() && b.isProper()
        }

        private fun compareWithNull(v0: Coordinate?, v1: Coordinate?): Int {
            if (v0 == null) {
                if (v1 == null)
                    return 0
                //-- null is lower than non-null
                return -1
            }
            // v0 is non-null
            if (v1 == null)
                return 1
            return v0.compareTo(v1)
        }
    }
}
