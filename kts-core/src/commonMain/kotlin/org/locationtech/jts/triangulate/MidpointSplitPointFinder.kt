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

/**
 * A simple split point finder which returns the midpoint of the split segment. This is a default
 * strategy only. Usually a more sophisticated strategy is required to prevent repeated splitting.
 * Other points which could be used are:
 *
 *  * The projection of the encroaching point on the segment
 *  * A point on the segment which will produce two segments which will not be further encroached
 *  * The point on the segment which is the same distance from an endpoint as the encroaching
 * point
 *
 * @author Martin Davis
 */
class MidpointSplitPointFinder : ConstraintSplitPointFinder {
    /**
     * Gets the midpoint of the split segment
     */
    override fun findSplitPoint(seg: Segment, encroachPt: Coordinate): Coordinate {
        val p0 = seg.getStart()
        val p1 = seg.getEnd()
        return Coordinate((p0.x + p1.x) / 2, (p0.y + p1.y) / 2)
    }
}
