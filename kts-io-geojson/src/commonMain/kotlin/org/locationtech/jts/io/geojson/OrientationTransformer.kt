/*
 * Copyright (c) 2021 Martin Davis.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.locationtech.jts.io.geojson

import org.locationtech.jts.algorithm.Orientation
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LinearRing
import org.locationtech.jts.geom.MultiPolygon
import org.locationtech.jts.geom.Polygon

/**
 * Utilities to modify the ring orientation of polygonal geometries.
 */
object OrientationTransformer {

    /**
     * Transforms a geometry using the Right Hand Rule specifications defined
     * in the latest GeoJSON specification.
     * See [RFC-7946 Specification](https://tools.ietf.org/html/rfc7946#section-3.1.6) for more context.
     *
     * @param geometry to be transformed
     * @return Geometry under the Right Hand Rule specifications
     */
    fun transformCCW(geometry: Geometry): Geometry {
        return when (geometry) {
            is MultiPolygon -> {
                val polygons = ArrayList<Polygon>()
                for (i in 0 until geometry.getNumGeometries()) {
                    val polygon = geometry.getGeometryN(i)
                    polygons.add(transformCCW(polygon) as Polygon)
                }
                GeometryFactory().createMultiPolygon(polygons.toTypedArray())
            }

            is Polygon -> transformCCW(geometry)

            else -> geometry
        }
    }

    /**
     * Transforms a polygon using the Right Hand Rule specifications defined
     * in the latest GeoJSON specification.
     * See [RFC-7946 Specification](https://tools.ietf.org/html/rfc7946#section-3.1.6) for more context.
     *
     * @param polygon to be transformed
     * @return Polygon under the Right Hand Rule specifications
     */
    fun transformCCW(polygon: Polygon): Polygon {
        val exteriorRing = polygon.getExteriorRing()
        val exteriorRingEnforced = transformCCW(exteriorRing, true)

        val interiorRings = ArrayList<LinearRing>()
        for (i in 0 until polygon.getNumInteriorRing()) {
            interiorRings.add(transformCCW(polygon.getInteriorRingN(i), false))
        }

        return GeometryFactory(polygon.getPrecisionModel(), polygon.getSRID())
            .createPolygon(exteriorRingEnforced, interiorRings.toTypedArray())
    }

    /**
     * Transforms a polygon using the Right Hand Rule specifications defined
     * in the latest GeoJSON specification.
     * A linear ring MUST follow the right-hand rule with respect to the
     * area it bounds, i.e., exterior rings are counterclockwise, and
     * holes are clockwise.
     *
     * See [RFC 7946 Specification](https://tools.ietf.org/html/rfc7946#section-3.1.6) for more context.
     *
     * @param ring the LinearRing, a constraint specific to Polygons
     * @param isExteriorRing true if the LinearRing is the exterior polygon ring, the one that defines the boundary
     * @return LinearRing under the Right Hand Rule specifications
     */
    fun transformCCW(ring: LinearRing, isExteriorRing: Boolean): LinearRing {
        val isRingClockWise = !Orientation.isCCW(ring.getCoordinateSequence())

        val rightHandRuleRing: LinearRing = if (isExteriorRing) {
            if (isRingClockWise) ring.reverse() else ring.copy() as LinearRing
        } else {
            if (isRingClockWise) ring.copy() as LinearRing else ring.reverse()
        }
        return rightHandRuleRing
    }
}
