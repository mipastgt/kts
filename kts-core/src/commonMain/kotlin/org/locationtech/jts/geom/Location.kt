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
package org.locationtech.jts.geom

import kotlin.jvm.JvmStatic

/**
 *  Constants representing the different topological locations
 *  which can occur in a [Geometry].
 *  The constants are also used as the row and column indices
 *  of DE-9IM [IntersectionMatrix]es.
 *
 */
class Location {
  companion object {
    /**
     * The location value for the interior of a geometry.
     * Also, DE-9IM row index of the interior of the first geometry and column index of
     *  the interior of the second geometry.
     */
    const val INTERIOR = 0

    /**
     * The location value for the boundary of a geometry.
     * Also, DE-9IM row index of the boundary of the first geometry and column index of
     *  the boundary of the second geometry.
     */
    const val BOUNDARY = 1

    /**
     * The location value for the exterior of a geometry.
     * Also, DE-9IM row index of the exterior of the first geometry and column index of
     *  the exterior of the second geometry.
     */
    const val EXTERIOR = 2

    /**
     *  Used for uninitialized location values.
     */
    const val NONE = -1

    /**
     *  Converts the location value to a location symbol, for example, `EXTERIOR => 'e'`
     *  .
     *
     * @param  locationValue  either EXTERIOR, BOUNDARY, INTERIOR or NONE
     * @return                either 'e', 'b', 'i' or '-'
     */
    @JvmStatic
    fun toLocationSymbol(locationValue: Int): Char {
      when (locationValue) {
        EXTERIOR -> return 'e'
        BOUNDARY -> return 'b'
        INTERIOR -> return 'i'
        NONE -> return '-'
      }
      throw IllegalArgumentException("Unknown location value: " + locationValue)
    }
  }
}
