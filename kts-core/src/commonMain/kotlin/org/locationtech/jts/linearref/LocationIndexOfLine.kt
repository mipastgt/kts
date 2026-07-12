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

package org.locationtech.jts.linearref

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.LineString

/**
 * Determines the location of a subline along a linear [Geometry].
 * The location is reported as a pair of [LinearLocation]s.
 *
 * **Note:** Currently this algorithm is not guaranteed to
 * return the correct substring in some situations where
 * an endpoint of the test line occurs more than once in the input line.
 * (However, the common case of a ring is always handled correctly).
 */
internal class LocationIndexOfLine(private val linearGeom: Geometry) {

  fun indicesOf(subLine: Geometry): Array<LinearLocation> {
    val startPt = (subLine.getGeometryN(0) as LineString).getCoordinateN(0)
    val lastLine = subLine.getGeometryN(subLine.getNumGeometries() - 1) as LineString
    val endPt = lastLine.getCoordinateN(lastLine.getNumPoints() - 1)

    val locPt = LocationIndexOfPoint(linearGeom)
    val subLineLoc = arrayOfNulls<LinearLocation>(2)
    subLineLoc[0] = locPt.indexOf(startPt)

    // check for case where subline is zero length
    if (subLine.getLength() == 0.0) {
      subLineLoc[1] = subLineLoc[0]!!.copy()
    } else {
      subLineLoc[1] = locPt.indexOfAfter(endPt, subLineLoc[0])
    }
    @Suppress("UNCHECKED_CAST")
    return subLineLoc as Array<LinearLocation>
  }

  companion object {
    /**
     * MD - this algorithm has been extracted into a class
     * because it is intended to validate that the subline truly is a subline,
     * and also to use the internal vertex information to unambiguously locate the subline.
     */
    fun indicesOf(linearGeom: Geometry, subLine: Geometry): Array<LinearLocation> {
      val locater = LocationIndexOfLine(linearGeom)
      return locater.indicesOf(subLine)
    }
  }
}
