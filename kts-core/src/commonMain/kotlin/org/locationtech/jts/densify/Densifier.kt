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
package org.locationtech.jts.densify

import kotlin.jvm.JvmStatic
import kotlin.math.ceil

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.CoordinateList
import org.locationtech.jts.geom.CoordinateSequence
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.LineSegment
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.MultiPolygon
import org.locationtech.jts.geom.Polygon
import org.locationtech.jts.geom.PrecisionModel
import org.locationtech.jts.geom.util.GeometryTransformer

/**
 * Densifies a [Geometry] by inserting extra vertices along the line segments
 * contained in the geometry.
 * All segments in the created densified geometry will be **no longer**
 * than the given distance tolerance
 * (that is, all segments in the output will have length less than or equal to
 * the distance tolerance).
 * The coordinates created during densification respect the input geometry's
 * [PrecisionModel].
 *
 * By default polygonal results are processed to ensure they are valid.
 * This processing is costly, and it is very rare for results to be invalid.
 * Validation processing can be disabled by calling the [setValidate] method.
 *
 * **Note:** At some future point this class will
 * offer a variety of densification strategies.
 *
 * @author Martin Davis
 *
 * @constructor Creates a new densifier instance.
 *
 * @param inputGeom
 */
class Densifier(private val inputGeom: Geometry) {

    private var distanceTolerance = 0.0

    /**
     * Indicates whether areas should be topologically validated.
     */
    private var isValidated = true

    /**
     * Sets the distance tolerance for the densification. All line segments
     * in the densified geometry will be no longer than the distance tolerance.
     * The distance tolerance must be positive.
     *
     * @param distanceTolerance the densification tolerance to use
     */
    fun setDistanceTolerance(distanceTolerance: Double) {
        if (distanceTolerance <= 0.0)
            throw IllegalArgumentException("Tolerance must be positive")
        this.distanceTolerance = distanceTolerance
    }

    /**
     * Sets whether polygonal results are processed to ensure they are valid.
     *
     * @param isValidated true if the results should be validated
     */
    fun setValidate(isValidated: Boolean) {
        this.isValidated = isValidated
    }

    /**
     * Gets the densified geometry.
     *
     * @return the densified geometry
     */
    fun getResultGeometry(): Geometry? {
        return DensifyTransformer(distanceTolerance, isValidated).transform(inputGeom)
    }

    internal class DensifyTransformer(
        private val distanceTolerance: Double,
        private val isValidated: Boolean
    ) : GeometryTransformer() {

        override fun transformCoordinates(
            coords: CoordinateSequence, parent: Geometry?
        ): CoordinateSequence? {
            val inputPts = coords.toCoordinateArray()
            var newPts = densifyPoints(inputPts, distanceTolerance, parent!!.getPrecisionModel())
            // prevent creation of invalid linestrings
            if (parent is LineString && newPts.size == 1) {
                newPts = arrayOf()
            }
            return factory!!.getCoordinateSequenceFactory().create(newPts)
        }

        override fun transformPolygon(geom: Polygon, parent: Geometry?): Geometry? {
            val roughGeom = super.transformPolygon(geom, parent)
            // don't try and correct if the parent is going to do this
            if (parent is MultiPolygon) {
                return roughGeom
            }
            return createValidArea(roughGeom!!)
        }

        override fun transformMultiPolygon(geom: MultiPolygon, parent: Geometry?): Geometry? {
            val roughGeom = super.transformMultiPolygon(geom, parent)
            return createValidArea(roughGeom!!)
        }

        /**
         * Creates a valid area geometry from one that possibly has bad topology
         * (i.e. self-intersections). Since buffer can handle invalid topology, but
         * always returns valid geometry, constructing a 0-width buffer "corrects"
         * the topology. Note this only works for area geometries, since buffer
         * always returns areas. This also may return empty geometries, if the input
         * has no actual area.
         *
         * @param roughAreaGeom
         *          an area geometry possibly containing self-intersections
         * @return a valid area geometry
         */
        private fun createValidArea(roughAreaGeom: Geometry): Geometry {
            // if valid no need to process to make valid
            if (!isValidated || roughAreaGeom.isValid()) return roughAreaGeom
            return roughAreaGeom.buffer(0.0)
        }
    }

    companion object {
        /**
         * Densifies a geometry using a given distance tolerance,
         * and respecting the input geometry's [PrecisionModel].
         *
         * @param geom the geometry to densify
         * @param distanceTolerance the distance tolerance to densify
         * @return the densified geometry
         */
        @JvmStatic
        fun densify(geom: Geometry, distanceTolerance: Double): Geometry? {
            val densifier = Densifier(geom)
            densifier.setDistanceTolerance(distanceTolerance)
            return densifier.getResultGeometry()
        }

        /**
         * Densifies a list of coordinates.
         *
         * @param pts the coordinate list
         * @param distanceTolerance the densify tolerance
         * @return the densified coordinate sequence
         */
        private fun densifyPoints(
            pts: Array<Coordinate>,
            distanceTolerance: Double, precModel: PrecisionModel
        ): Array<Coordinate> {
            val seg = LineSegment()
            val coordList = CoordinateList()
            for (i in 0 until pts.size - 1) {
                seg.p0 = pts[i]
                seg.p1 = pts[i + 1]
                coordList.add(seg.p0, false)
                val len = seg.getLength()

                // check if no densification is required
                if (len <= distanceTolerance)
                    continue

                // densify the segment
                val densifiedSegCount = ceil(len / distanceTolerance).toInt()
                val densifiedSegLen = len / densifiedSegCount
                for (j in 1 until densifiedSegCount) {
                    val segFract = (j * densifiedSegLen) / len
                    val p = seg.pointAlong(segFract)
                    if (!seg.p0.z.isNaN() && !seg.p1.z.isNaN()) {
                        p.setZ(seg.p0.z + segFract * (seg.p1.z - seg.p0.z))
                    }
                    precModel.makePrecise(p)
                    coordList.add(p, false)
                }
            }
            // this check handles empty sequences
            if (pts.size > 0)
                coordList.add(pts[pts.size - 1], false)
            return coordList.toCoordinateArray()
        }
    }
}
