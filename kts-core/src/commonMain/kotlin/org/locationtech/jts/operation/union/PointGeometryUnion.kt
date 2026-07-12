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

package org.locationtech.jts.operation.union

import kotlin.jvm.JvmStatic

import org.locationtech.jts.util.TreeSet

import org.locationtech.jts.algorithm.PointLocator
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.CoordinateArrays
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Location
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.Puntal
import org.locationtech.jts.geom.util.GeometryCombiner

/**
 * Computes the union of a [Puntal] geometry with
 * another arbitrary [Geometry].
 * Does not copy any component geometries.
 *
 * @author mbdavis
 *
 */
class PointGeometryUnion(pointGeom: Puntal, otherGeom: Geometry) {

    private val pointGeom: Geometry = pointGeom as Geometry
    private val otherGeom: Geometry = otherGeom
    private val geomFact: GeometryFactory = otherGeom.getFactory()

    fun union(): Geometry? {
        val locater = PointLocator()
        // use a set to eliminate duplicates, as required for union
        val exteriorCoords = TreeSet<Coordinate>()

        for (i in 0 until pointGeom.getNumGeometries()) {
            val point = pointGeom.getGeometryN(i) as Point
            val coord = point.getCoordinate()!!
            val loc = locater.locate(coord, otherGeom)
            if (loc == Location.EXTERIOR)
                exteriorCoords.add(coord)
        }

        // if no points are in exterior, return the other geom
        if (exteriorCoords.size == 0)
            return otherGeom

        // make a puntal geometry of appropriate size
        val coords = CoordinateArrays.toCoordinateArray(exteriorCoords)
        val ptComp: Geometry = if (coords.size == 1) {
            geomFact.createPoint(coords[0])
        } else {
            geomFact.createMultiPointFromCoords(coords)
        }

        // add point component to the other geometry
        return GeometryCombiner.combine(ptComp, otherGeom)
    }

    companion object {
        @JvmStatic
        fun union(pointGeom: Puntal, otherGeom: Geometry): Geometry? {
            val unioner = PointGeometryUnion(pointGeom, otherGeom)
            return unioner.union()
        }
    }
}
