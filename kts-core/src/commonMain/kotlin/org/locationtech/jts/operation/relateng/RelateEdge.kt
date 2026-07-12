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

import org.locationtech.jts.algorithm.PolygonNodeTopology
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Dimension
import org.locationtech.jts.geom.Location
import org.locationtech.jts.geom.Position
import org.locationtech.jts.io.WKTWriter
import org.locationtech.jts.util.Assert

internal class RelateEdge private constructor(private val node: RelateNode, private val dirPt: Coordinate) {

    private var aDim = DIM_UNKNOWN
    private var aLocLeft = LOC_UNKNOWN
    private var aLocRight = LOC_UNKNOWN
    private var aLocLine = LOC_UNKNOWN

    private var bDim = DIM_UNKNOWN
    private var bLocLeft = LOC_UNKNOWN
    private var bLocRight = LOC_UNKNOWN
    private var bLocLine = LOC_UNKNOWN

    constructor(node: RelateNode, pt: Coordinate, isA: Boolean, isForward: Boolean) : this(node, pt) {
        setLocationsArea(isA, isForward)
    }

    constructor(node: RelateNode, pt: Coordinate, isA: Boolean) : this(node, pt) {
        setLocationsLine(isA)
    }

    constructor(node: RelateNode, pt: Coordinate, isA: Boolean, locLeft: Int, locRight: Int, locLine: Int) : this(node, pt) {
        setLocations(isA, locLeft, locRight, locLine)
    }

    private fun setLocations(isA: Boolean, locLeft: Int, locRight: Int, locLine: Int) {
        if (isA) {
            aDim = 2
            aLocLeft = locLeft
            aLocRight = locRight
            aLocLine = locLine
        } else {
            bDim = 2
            bLocLeft = locLeft
            bLocRight = locRight
            bLocLine = locLine
        }
    }

    private fun setLocationsLine(isA: Boolean) {
        if (isA) {
            aDim = 1
            aLocLeft = Location.EXTERIOR
            aLocRight = Location.EXTERIOR
            aLocLine = Location.INTERIOR
        } else {
            bDim = 1
            bLocLeft = Location.EXTERIOR
            bLocRight = Location.EXTERIOR
            bLocLine = Location.INTERIOR
        }
    }

    private fun setLocationsArea(isA: Boolean, isForward: Boolean) {
        val locLeft = if (isForward) Location.EXTERIOR else Location.INTERIOR
        val locRight = if (isForward) Location.INTERIOR else Location.EXTERIOR
        if (isA) {
            aDim = 2
            aLocLeft = locLeft
            aLocRight = locRight
            aLocLine = Location.BOUNDARY
        } else {
            bDim = 2
            bLocLeft = locLeft
            bLocRight = locRight
            bLocLine = Location.BOUNDARY
        }
    }

    fun compareToEdge(edgeDirPt: Coordinate): Int {
        return PolygonNodeTopology.compareAngle(node.getCoordinate(), this.dirPt, edgeDirPt)
    }

    fun merge(isA: Boolean, dirPt: Coordinate, dim: Int, isForward: Boolean) {
        var locEdge = Location.INTERIOR
        var locLeft = Location.EXTERIOR
        var locRight = Location.EXTERIOR
        if (dim == Dimension.A) {
            locEdge = Location.BOUNDARY
            locLeft = if (isForward) Location.EXTERIOR else Location.INTERIOR
            locRight = if (isForward) Location.INTERIOR else Location.EXTERIOR
        }

        if (!isKnown(isA)) {
            setDimension(isA, dim)
            setOn(isA, locEdge)
            setLeft(isA, locLeft)
            setRight(isA, locRight)
            return
        }

        // Assert: node-dirpt is collinear with node-pt
        mergeDimEdgeLoc(isA, locEdge)
        mergeSideLocation(isA, Position.LEFT, locLeft)
        mergeSideLocation(isA, Position.RIGHT, locRight)
    }

    /**
     * Area edges override Line edges.
     *
     * @param isA
     * @param locEdge
     */
    private fun mergeDimEdgeLoc(isA: Boolean, locEdge: Int) {
        //TODO: this logic needs work - ie handling A edges marked as Interior
        val dim = if (locEdge == Location.BOUNDARY) Dimension.A else Dimension.L
        if (dim == Dimension.A && dimension(isA) == Dimension.L) {
            setDimension(isA, dim)
            setOn(isA, Location.BOUNDARY)
        }
    }

    private fun mergeSideLocation(isA: Boolean, pos: Int, loc: Int) {
        val currLoc = location(isA, pos)
        //-- INTERIOR takes precedence over EXTERIOR
        if (currLoc != Location.INTERIOR) {
            setLocation(isA, pos, loc)
        }
    }

    private fun setDimension(isA: Boolean, dimension: Int) {
        if (isA) {
            aDim = dimension
        } else {
            bDim = dimension
        }
    }

    fun setLocation(isA: Boolean, pos: Int, loc: Int) {
        when (pos) {
            Position.LEFT -> setLeft(isA, loc)
            Position.RIGHT -> setRight(isA, loc)
            Position.ON -> setOn(isA, loc)
        }
    }

    fun setAllLocations(isA: Boolean, loc: Int) {
        setLeft(isA, loc)
        setRight(isA, loc)
        setOn(isA, loc)
    }

    fun setUnknownLocations(isA: Boolean, loc: Int) {
        if (!isKnown(isA, Position.LEFT)) {
            setLocation(isA, Position.LEFT, loc)
        }
        if (!isKnown(isA, Position.RIGHT)) {
            setLocation(isA, Position.RIGHT, loc)
        }
        if (!isKnown(isA, Position.ON)) {
            setLocation(isA, Position.ON, loc)
        }
    }

    private fun setLeft(isA: Boolean, loc: Int) {
        if (isA) {
            aLocLeft = loc
        } else {
            bLocLeft = loc
        }
    }

    private fun setRight(isA: Boolean, loc: Int) {
        if (isA) {
            aLocRight = loc
        } else {
            bLocRight = loc
        }
    }

    private fun setOn(isA: Boolean, loc: Int) {
        if (isA) {
            aLocLine = loc
        } else {
            bLocLine = loc
        }
    }

    fun location(isA: Boolean, position: Int): Int {
        if (isA) {
            when (position) {
                Position.LEFT -> return aLocLeft
                Position.RIGHT -> return aLocRight
                Position.ON -> return aLocLine
            }
        } else {
            when (position) {
                Position.LEFT -> return bLocLeft
                Position.RIGHT -> return bLocRight
                Position.ON -> return bLocLine
            }
        }
        Assert.shouldNeverReachHere()
        return LOC_UNKNOWN
    }

    private fun dimension(isA: Boolean): Int {
        return if (isA) aDim else bDim
    }

    private fun isKnown(isA: Boolean): Boolean {
        if (isA)
            return aDim != DIM_UNKNOWN
        return bDim != DIM_UNKNOWN
    }

    private fun isKnown(isA: Boolean, pos: Int): Boolean {
        return location(isA, pos) != LOC_UNKNOWN
    }

    fun isInterior(isA: Boolean, position: Int): Boolean {
        return location(isA, position) == Location.INTERIOR
    }

    fun setDimLocations(isA: Boolean, dim: Int, loc: Int) {
        if (isA) {
            aDim = dim
            aLocLeft = loc
            aLocRight = loc
            aLocLine = loc
        } else {
            bDim = dim
            bLocLeft = loc
            bLocRight = loc
            bLocLine = loc
        }
    }

    fun setAreaInterior(isA: Boolean) {
        if (isA) {
            aLocLeft = Location.INTERIOR
            aLocRight = Location.INTERIOR
            aLocLine = Location.INTERIOR
        } else {
            bLocLeft = Location.INTERIOR
            bLocRight = Location.INTERIOR
            bLocLine = Location.INTERIOR
        }
    }

    override fun toString(): String {
        return WKTWriter.toLineString(node.getCoordinate(), dirPt) +
            " - " + labelString()
    }

    private fun labelString(): String {
        val buf = StringBuilder()
        buf.append("A:")
        buf.append(locationString(RelateGeometry.GEOM_A))
        buf.append("/B:")
        buf.append(locationString(RelateGeometry.GEOM_B))
        return buf.toString()
    }

    private fun locationString(isA: Boolean): String {
        val buf = StringBuilder()
        buf.append(Location.toLocationSymbol(location(isA, Position.LEFT)))
        buf.append(Location.toLocationSymbol(location(isA, Position.ON)))
        buf.append(Location.toLocationSymbol(location(isA, Position.RIGHT)))
        return buf.toString()
    }

    companion object {
        const val IS_FORWARD = true
        const val IS_REVERSE = false

        /**
         * The dimension of an input geometry which is not known
         */
        const val DIM_UNKNOWN = -1

        /**
         * Indicates that the location is currently unknown
         */
        private const val LOC_UNKNOWN = Location.NONE

        fun create(node: RelateNode, dirPt: Coordinate, isA: Boolean, dim: Int, isForward: Boolean): RelateEdge {
            if (dim == Dimension.A)
                //-- create an area edge
                return RelateEdge(node, dirPt, isA, isForward)
            //-- create line edge
            return RelateEdge(node, dirPt, isA)
        }

        fun findKnownEdgeIndex(edges: List<RelateEdge>, isA: Boolean): Int {
            for (i in 0 until edges.size) {
                val e = edges[i]
                if (e.isKnown(isA))
                    return i
            }
            return -1
        }

        fun setAreaInterior(edges: List<RelateEdge>, isA: Boolean) {
            for (e in edges) {
                e.setAreaInterior(isA)
            }
        }
    }
}
