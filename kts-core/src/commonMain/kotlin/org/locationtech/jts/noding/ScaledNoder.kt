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

package org.locationtech.jts.noding
import kotlin.math.roundToLong

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.CoordinateArrays

/**
 * Wraps a [Noder] and transforms its input
 * into the integer domain.
 * This is intended for use with Snap-Rounding noders,
 * which typically are only intended to work in the integer domain.
 * Offsets can be provided to increase the number of digits of available precision.
 *
 *
 * Clients should be aware that rescaling can involve loss of precision,
 * which can cause zero-length line segments to be created.
 * These in turn can cause problems when used to build a planar graph.
 * This situation should be checked for and collapsed segments removed if necessary.
 *
 * @version 1.7
 */
class ScaledNoder(
  private val noder: Noder,
  private val scaleFactor: Double,
  private val offsetX: Double,
  private val offsetY: Double
) : Noder {
  private var isScaled = false

  constructor(noder: Noder, scaleFactor: Double) : this(noder, scaleFactor, 0.0, 0.0)

  init {
    // no need to scale if input precision is already integral
    isScaled = !isIntegerPrecision()
  }

  fun isIntegerPrecision(): Boolean = scaleFactor == 1.0

  override fun getNodedSubstrings(): MutableCollection<*> {
    val splitSS = noder.getNodedSubstrings()!!
    if (isScaled) rescale(splitSS)
    return splitSS
  }

  override fun computeNodes(inputSegStrings: Collection<*>?) {
    var intSegStrings: Collection<*>? = inputSegStrings
    if (isScaled)
      intSegStrings = scale(inputSegStrings!!)
    noder.computeNodes(intSegStrings)
  }

  private fun scale(segStrings: Collection<*>): MutableCollection<*> {
    val nodedSegmentStrings = ArrayList<SegmentString>(segStrings.size)
    for (obj in segStrings) {
      val ss = obj as SegmentString
      nodedSegmentStrings.add(NodedSegmentString(scale(ss.getCoordinates()), ss.getData()))
    }
    return nodedSegmentStrings
  }

  private fun scale(pts: Array<Coordinate>): Array<Coordinate> {
    val roundPts = Array(pts.size) { i ->
      Coordinate(
        ((pts[i].x - offsetX) * scaleFactor).roundToLong().toDouble(),
        ((pts[i].y - offsetY) * scaleFactor).roundToLong().toDouble(),
        pts[i].getZ()
      )
    }
    val roundPtsNoDup = CoordinateArrays.removeRepeatedPoints(roundPts)
    return roundPtsNoDup
  }

  //private double scale(double val) { return (double) (val * scaleFactor).roundToLong(); }

  private fun rescale(segStrings: Collection<*>) {
    for (obj in segStrings) {
      val ss = obj as SegmentString
      rescale(ss.getCoordinates())
    }
  }

  private fun rescale(pts: Array<Coordinate>) {
    for (i in pts.indices) {
      pts[i].x = pts[i].x / scaleFactor + offsetX
      pts[i].y = pts[i].y / scaleFactor + offsetY
    }
  }
}
