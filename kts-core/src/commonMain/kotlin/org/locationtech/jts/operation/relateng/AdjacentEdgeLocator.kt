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

import org.locationtech.jts.algorithm.PointLocation
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Dimension
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryCollection
import org.locationtech.jts.geom.LinearRing
import org.locationtech.jts.geom.Location
import org.locationtech.jts.geom.Polygon

/**
 * Determines the location for a point which is known to lie
 * on at least one edge of a set of polygons.
 *
 * @author mdavis
 *
 */
class AdjacentEdgeLocator(geom: Geometry) {

    private var ringList: MutableList<Array<Coordinate>>? = null

    init {
        initRings(geom)
    }

    fun locate(p: Coordinate): Int {
        val sections = NodeSections(p)
        for (ring in ringList!!) {
            addSections(p, ring, sections)
        }
        val node = sections.createNode()
        //node.finish(false, false);
        return if (node.hasExteriorEdge(true)) Location.BOUNDARY else Location.INTERIOR
    }

    private fun addSections(p: Coordinate, ring: Array<Coordinate>, sections: NodeSections) {
        for (i in 0 until ring.size - 1) {
            val p0 = ring[i]
            val pnext = ring[i + 1]

            if (p.equals2D(pnext)) {
                //-- segment final point is assigned to next segment
                continue
            } else if (p.equals2D(p0)) {
                val iprev = if (i > 0) i - 1 else ring.size - 2
                val pprev = ring[iprev]
                sections.addNodeSection(createSection(p, pprev, pnext))
            } else if (PointLocation.isOnSegment(p, p0, pnext)) {
                sections.addNodeSection(createSection(p, p0, pnext))
            }
        }
    }

    private fun createSection(p: Coordinate, prev: Coordinate, next: Coordinate): NodeSection {
        if (prev.distance(p) == 0.0 || next.distance(p) == 0.0) {
            println("Found zero-length section segment")
        }
        val ns = NodeSection(true, Dimension.A, 1, 0, null, false, prev, p, next)
        return ns
    }

    private fun initRings(geom: Geometry) {
        if (geom.isEmpty())
            return
        ringList = ArrayList()
        addRings(geom, ringList!!)
    }

    private fun addRings(geom: Geometry, ringList2: MutableList<Array<Coordinate>>) {
        if (geom is Polygon) {
            val shell = geom.getExteriorRing()
            addRing(shell, true)
            for (i in 0 until geom.getNumInteriorRing()) {
                val hole = geom.getInteriorRingN(i)
                addRing(hole, false)
            }
        } else if (geom is GeometryCollection) {
            //-- recurse through collections
            for (i in 0 until geom.getNumGeometries()) {
                addRings(geom.getGeometryN(i), ringList!!)
            }
        }
    }

    private fun addRing(ring: LinearRing, requireCW: Boolean) {
        //TODO: remove repeated points?
        val pts = RelateGeometry.orient(ring.getCoordinates(), requireCW)
        ringList!!.add(pts)
    }
}
