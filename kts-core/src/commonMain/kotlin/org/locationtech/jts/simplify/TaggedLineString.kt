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

package org.locationtech.jts.simplify

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.LineSegment
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.LinearRing

/**
 * Represents a [LineString] which can be modified to a simplified shape.
 *
 */
internal class TaggedLineString(parentLine: LineString, minimumSize: Int, isRing: Boolean) {

  private val parentLine: LineString = parentLine
  private lateinit var segs: Array<TaggedLineSegment>
  private val resultSegs: MutableList<LineSegment> = ArrayList()
  private val minimumSize: Int = minimumSize
  private val ring: Boolean = isRing

  init {
    initSegs()
  }

  fun isRing(): Boolean {
    return ring
  }

  fun getMinimumSize(): Int = minimumSize
  fun getParent(): LineString = parentLine
  fun getParentCoordinates(): Array<Coordinate> = parentLine.getCoordinates()
  fun getResultCoordinates(): Array<Coordinate> = extractCoordinates(resultSegs)

  fun getCoordinate(i: Int): Coordinate {
    return parentLine.getCoordinateN(i)
  }

  fun size(): Int {
    return parentLine.getNumPoints()
  }

  fun getComponentPoint(): Coordinate {
    return getParentCoordinates()[1]
  }

  fun getResultSize(): Int {
    val resultSegsSize = resultSegs.size
    return if (resultSegsSize == 0) 0 else resultSegsSize + 1
  }

  fun getSegment(i: Int): TaggedLineSegment = segs[i]

  /**
   * Gets a segment of the result list.
   * Negative indexes can be used to retrieve from the end of the list.
   * @param i the segment index to retrieve
   * @return the result segment
   */
  fun getResultSegment(i: Int): LineSegment {
    var index = i
    if (i < 0) {
      index = resultSegs.size + i
    }
    return resultSegs.get(index)
  }

  private fun initSegs() {
    val pts = parentLine.getCoordinates()
    val s = arrayOfNulls<TaggedLineSegment>(pts.size - 1)
    for (i in 0 until pts.size - 1) {
      val seg = TaggedLineSegment(pts[i], pts[i + 1], parentLine, i)
      s[i] = seg
    }
    @Suppress("UNCHECKED_CAST")
    segs = s as Array<TaggedLineSegment>
  }

  fun getSegments(): Array<TaggedLineSegment> = segs

  /**
   * Add a simplified segment to the result.
   * This assumes simplified segments are computed in the order
   * they occur in the line.
   *
   * @param seg the result segment to add
   */
  fun addToResult(seg: LineSegment) {
    resultSegs.add(seg)
  }

  fun asLineString(): LineString {
    return parentLine.getFactory().createLineString(extractCoordinates(resultSegs))
  }

  fun asLinearRing(): LinearRing {
    return parentLine.getFactory().createLinearRing(extractCoordinates(resultSegs))
  }

  fun removeRingEndpoint(): LineSegment {
    val firstSeg = resultSegs.get(0)
    val lastSeg = resultSegs.get(resultSegs.size - 1)

    firstSeg.p0 = lastSeg.p0
    resultSegs.removeAt(resultSegs.size - 1)
    return firstSeg
  }

  companion object {
    private fun extractCoordinates(segs: List<LineSegment>): Array<Coordinate> {
      val pts = arrayOfNulls<Coordinate>(segs.size + 1)
      var seg: LineSegment? = null
      for (i in segs.indices) {
        seg = segs.get(i)
        pts[i] = seg.p0
      }
      // add last point
      pts[pts.size - 1] = seg!!.p1
      @Suppress("UNCHECKED_CAST")
      return pts as Array<Coordinate>
    }
  }
}
