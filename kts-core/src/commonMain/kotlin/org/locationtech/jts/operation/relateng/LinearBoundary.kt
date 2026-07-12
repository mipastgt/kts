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

import org.locationtech.jts.algorithm.BoundaryNodeRule
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.LineString

/**
 * Determines the boundary points of a linear geometry,
 * using a [BoundaryNodeRule].
 *
 * @author mdavis
 *
 */
class LinearBoundary(lines: List<LineString>, bnRule: BoundaryNodeRule) {

    private val vertexDegree: MutableMap<Coordinate, Int>
    private val hasBoundaryFlag: Boolean
    private val boundaryNodeRule: BoundaryNodeRule = bnRule

    init {
        //assert: dim(geom) == 1
        vertexDegree = computeBoundaryPoints(lines)
        hasBoundaryFlag = checkBoundary(vertexDegree)
    }

    private fun checkBoundary(vertexDegree: Map<Coordinate, Int>): Boolean {
        for (degree in vertexDegree.values) {
            if (boundaryNodeRule.isInBoundary(degree)) {
                return true
            }
        }
        return false
    }

    fun hasBoundary(): Boolean {
        return hasBoundaryFlag
    }

    fun isBoundary(pt: Coordinate): Boolean {
        if (!vertexDegree.containsKey(pt))
            return false
        val degree = vertexDegree[pt]!!
        return boundaryNodeRule.isInBoundary(degree)
    }

    companion object {
        private fun computeBoundaryPoints(lines: List<LineString>): MutableMap<Coordinate, Int> {
            val vertexDegree = HashMap<Coordinate, Int>()
            for (line in lines) {
                if (line.isEmpty())
                    continue
                addEndpoint(line.getCoordinateN(0), vertexDegree)
                addEndpoint(line.getCoordinateN(line.getNumPoints() - 1), vertexDegree)
            }
            return vertexDegree
        }

        private fun addEndpoint(p: Coordinate, degree: MutableMap<Coordinate, Int>) {
            var dim = 0
            if (degree.containsKey(p)) {
                dim = degree[p]!!
            }
            dim++
            degree[p] = dim
        }
    }
}
