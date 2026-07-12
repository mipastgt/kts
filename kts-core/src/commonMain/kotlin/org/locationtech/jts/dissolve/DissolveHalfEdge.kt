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

package org.locationtech.jts.dissolve

import org.locationtech.jts.edgegraph.MarkHalfEdge
import org.locationtech.jts.geom.Coordinate

/**
 * A HalfEdge which carries information
 * required to support [LineDissolver].
 *
 * @author Martin Davis
 */
internal class DissolveHalfEdge(orig: Coordinate) : MarkHalfEdge(orig) {
    private var start = false

    /**
     * Tests whether this edge is the starting segment
     * in a LineString being dissolved.
     *
     * @return true if this edge is a start segment
     */
    fun isStart(): Boolean {
        return start
    }

    /**
     * Sets this edge to be the start segment of an input LineString.
     */
    fun setStart() {
        start = true
    }
}
