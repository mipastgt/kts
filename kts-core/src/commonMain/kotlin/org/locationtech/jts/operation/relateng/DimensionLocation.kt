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

import org.locationtech.jts.geom.Dimension
import org.locationtech.jts.geom.Location

/**
 * Codes which combine a geometry dimension and a location
 * on the geometry.
 *
 * @author mdavis
 *
 */
internal object DimensionLocation {

    const val EXTERIOR = Location.EXTERIOR
    const val POINT_INTERIOR = 103
    const val LINE_INTERIOR = 110
    const val LINE_BOUNDARY = 111
    const val AREA_INTERIOR = 120
    const val AREA_BOUNDARY = 121

    fun locationArea(loc: Int): Int {
        when (loc) {
            Location.INTERIOR -> return AREA_INTERIOR
            Location.BOUNDARY -> return AREA_BOUNDARY
        }
        return EXTERIOR
    }

    fun locationLine(loc: Int): Int {
        when (loc) {
            Location.INTERIOR -> return LINE_INTERIOR
            Location.BOUNDARY -> return LINE_BOUNDARY
        }
        return EXTERIOR
    }

    fun locationPoint(loc: Int): Int {
        when (loc) {
            Location.INTERIOR -> return POINT_INTERIOR
        }
        return EXTERIOR
    }

    fun location(dimLoc: Int): Int {
        when (dimLoc) {
            POINT_INTERIOR,
            LINE_INTERIOR,
            AREA_INTERIOR -> return Location.INTERIOR
            LINE_BOUNDARY,
            AREA_BOUNDARY -> return Location.BOUNDARY
        }
        return Location.EXTERIOR
    }

    fun dimension(dimLoc: Int): Int {
        when (dimLoc) {
            POINT_INTERIOR -> return Dimension.P
            LINE_INTERIOR,
            LINE_BOUNDARY -> return Dimension.L
            AREA_INTERIOR,
            AREA_BOUNDARY -> return Dimension.A
        }
        return Dimension.FALSE
    }

    fun dimension(dimLoc: Int, exteriorDim: Int): Int {
        if (dimLoc == EXTERIOR)
            return exteriorDim
        return dimension(dimLoc)
    }
}
