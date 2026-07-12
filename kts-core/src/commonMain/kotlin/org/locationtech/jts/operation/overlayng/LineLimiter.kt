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
package org.locationtech.jts.operation.overlayng

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.CoordinateList
import org.locationtech.jts.geom.Envelope

/**
 * Limits the segments in a list of segments
 * to those which intersect an envelope.
 *
 * @author Martin Davis
 *
 * @see RingClipper
 */
class LineLimiter(private val limitEnv: Envelope) {

  private var ptList: CoordinateList? = null
  private var lastOutside: Coordinate? = null
  private var sections: MutableList<Array<Coordinate>>? = null

  /**
   * Limits a list of segments.
   *
   * @param pts the segment sequence to limit
   * @return the sections which intersect the limit envelope
   */
  fun limit(pts: Array<Coordinate>): MutableList<Array<Coordinate>> {
    lastOutside = null
    ptList = null
    sections = ArrayList()

    for (i in pts.indices) {
      val p = pts[i]
      if (limitEnv.intersects(p))
        addPoint(p)
      else {
        addOutside(p)
      }
    }
    // finish last section, if any
    finishSection()
    return sections!!
  }

  private fun addPoint(p: Coordinate?) {
    if (p == null) return
    startSection()
    ptList!!.add(p, false)
  }

  private fun addOutside(p: Coordinate) {
    val segIntersects = isLastSegmentIntersecting(p)
    if (!segIntersects) {
      finishSection()
    } else {
      addPoint(lastOutside)
      addPoint(p)
    }
    lastOutside = p
  }

  private fun isLastSegmentIntersecting(p: Coordinate): Boolean {
    if (lastOutside == null) {
      // last point must have been inside
      if (isSectionOpen())
        return true
      return false
    }
    return limitEnv.intersects(lastOutside!!, p)
  }

  private fun isSectionOpen(): Boolean {
    return ptList != null
  }

  private fun startSection() {
    if (ptList == null) {
      ptList = CoordinateList()
    }
    if (lastOutside != null) {
      ptList!!.add(lastOutside!!, false)
    }
    lastOutside = null
  }

  private fun finishSection() {
    if (ptList == null)
      return
    // finish off this section
    if (lastOutside != null) {
      ptList!!.add(lastOutside!!, false)
      lastOutside = null
    }

    val section = ptList!!.toCoordinateArray()
    sections!!.add(section)
    ptList = null
  }
}
