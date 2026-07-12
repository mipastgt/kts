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

package org.locationtech.jts.triangulate

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.io.WKTWriter

/**
 * Indicates a failure during constraint enforcement.
 *
 * @author Martin Davis
 * @version 1.0
 */
class ConstraintEnforcementException : RuntimeException {

    private var pt: Coordinate? = null

    /**
     * Creates a new instance with a given message.
     *
     * @param msg a string
     */
    constructor(msg: String) : super(msg)

    /**
     * Creates a new instance with a given message and approximate location.
     *
     * @param msg a string
     * @param pt the location of the error
     */
    constructor(msg: String, pt: Coordinate) : super(msgWithCoord(msg, pt)) {
        this.pt = Coordinate(pt)
    }

    /**
     * Gets the approximate location of this error.
     *
     * @return a location
     */
    fun getCoordinate(): Coordinate? {
        return pt
    }

    companion object {

        private fun msgWithCoord(msg: String, pt: Coordinate?): String {
            if (pt != null)
                return msg + " [ " + WKTWriter.toPoint(pt) + " ]"
            return msg
        }
    }
}
