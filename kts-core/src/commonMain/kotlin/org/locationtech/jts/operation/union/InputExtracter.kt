/*
 * Copyright (c) 2019 Martin Davis.
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

import org.locationtech.jts.geom.Dimension
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryCollection
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.GeometryFilter
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.Polygon
import org.locationtech.jts.util.Assert

/**
 * Extracts atomic elements from
 * input geometries or collections,
 * recording the dimension found.
 * Empty geometries are discarded since they
 * do not contribute to the result of [UnaryUnionOp].
 *
 * @author Martin Davis
 *
 */
internal class InputExtracter : GeometryFilter {

    private var geomFactory: GeometryFactory? = null
    private val polygons: MutableList<Polygon> = ArrayList()
    private val lines: MutableList<LineString> = ArrayList()
    private val points: MutableList<Point> = ArrayList()

    /**
     * The default dimension for an empty GeometryCollection
     */
    private var dimension = Dimension.FALSE

    /**
     * Tests whether there were any non-empty geometries extracted.
     *
     * @return true if there is a non-empty geometry present
     */
    fun isEmpty(): Boolean {
        return polygons.isEmpty() &&
            lines.isEmpty() &&
            points.isEmpty()
    }

    /**
     * Gets the maximum dimension extracted.
     *
     * @return the maximum extracted dimension
     */
    fun getDimension(): Int {
        return dimension
    }

    /**
     * Gets the geometry factory from the extracted geometry,
     * if there is one.
     * If an empty collection was extracted, will return `null`.
     *
     * @return a geometry factory, or null if one could not be determined
     */
    fun getFactory(): GeometryFactory? {
        return geomFactory
    }

    /**
     * Gets the extracted atomic geometries of the given dimension `dim`.
     *
     * @param dim the dimension of geometry to return
     * @return a list of the extracted geometries of dimension dim.
     */
    fun getExtract(dim: Int): MutableList<*> {
        when (dim) {
            0 -> return points
            1 -> return lines
            2 -> return polygons
        }
        Assert.shouldNeverReachHere("Invalid dimension: $dim")
        return points
    }

    private fun add(geoms: Collection<*>?) {
        for (geom in geoms!!) {
            add(geom as Geometry)
        }
    }

    private fun add(geom: Geometry) {
        if (geomFactory == null)
            geomFactory = geom.getFactory()

        geom.apply(this)
    }

    override fun filter(geom: Geometry) {
        recordDimension(geom.getDimension())

        if (geom is GeometryCollection) {
            return
        }
        /**
         * Don't keep empty geometries
         */
        if (geom.isEmpty())
            return

        if (geom is Polygon) {
            polygons.add(geom)
            return
        } else if (geom is LineString) {
            lines.add(geom)
            return
        } else if (geom is Point) {
            points.add(geom)
            return
        }
        Assert.shouldNeverReachHere("Unhandled geometry type: " + geom.getGeometryType())
    }

    private fun recordDimension(dim: Int) {
        if (dim > dimension)
            dimension = dim
    }

    companion object {
        /**
         * Extracts elements from a collection of geometries.
         *
         * @param geoms a collection of geometries
         * @return an extracter over the geometries
         */
        fun extract(geoms: Collection<*>?): InputExtracter {
            val extracter = InputExtracter()
            extracter.add(geoms)
            return extracter
        }

        /**
         * Extracts elements from a geometry.
         *
         * @param geom a geometry to extract from
         * @return an extracter over the geometry
         */
        fun extract(geom: Geometry): InputExtracter {
            val extracter = InputExtracter()
            extracter.add(geom)
            return extracter
        }
    }
}
