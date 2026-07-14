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
package org.locationtech.jts.operation.valid

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryCollection
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.MultiPoint
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.Polygon

/**
 * Implements the appropriate checks for repeated points
 * (consecutive identical coordinates) as defined in the
 * JTS spec.
 *
 */
class RepeatedPointTester {

    // save the repeated coord found (if any)
    private var repeatedCoord: Coordinate? = null

    fun getCoordinate(): Coordinate? {
        return repeatedCoord
    }

    fun hasRepeatedPoint(g: Geometry): Boolean {
        if (g.isEmpty()) return false
        if (g is Point) return false
        else if (g is MultiPoint) return false
        // LineString also handles LinearRings
        else if (g is LineString) return hasRepeatedPoint(g.getCoordinates())
        else if (g is Polygon) return hasRepeatedPoint(g)
        else if (g is GeometryCollection) return hasRepeatedPoint(g)
        else throw UnsupportedOperationException(g::class.simpleName)
    }

    fun hasRepeatedPoint(coord: Array<Coordinate>): Boolean {
        for (i in 1 until coord.size) {
            if (coord[i - 1] == coord[i]) {
                repeatedCoord = coord[i]
                return true
            }
        }
        return false
    }

    private fun hasRepeatedPoint(p: Polygon): Boolean {
        if (hasRepeatedPoint(p.getExteriorRing().getCoordinates())) return true
        for (i in 0 until p.getNumInteriorRing()) {
            if (hasRepeatedPoint(p.getInteriorRingN(i).getCoordinates())) return true
        }
        return false
    }

    private fun hasRepeatedPoint(gc: GeometryCollection): Boolean {
        for (i in 0 until gc.getNumGeometries()) {
            val g = gc.getGeometryN(i)
            if (hasRepeatedPoint(g)) return true
        }
        return false
    }
}
