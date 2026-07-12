/*
 * Copyright (c) 2022 Martin Davis.
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

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.LineSegment

/**
 * A noder which extracts boundary line segments
 * as [SegmentString]s.
 * Boundary segments are those which are not duplicated in the input.
 * It is appropriate for use with valid polygonal coverages.
 *
 *
 * No precision reduction is carried out.
 * If that is required, another noder must be used (such as a snap-rounding noder),
 * or the input must be precision-reduced beforehand.
 *
 * @author Martin Davis
 */
class BoundarySegmentNoder : Noder {

  private var segList: MutableList<SegmentString>? = null

  override fun computeNodes(segStrings: Collection<*>?) {
    val segSet = HashSet<Segment>()
    addSegments(segStrings!!, segSet)
    segList = extractSegments(segSet)
  }

  override fun getNodedSubstrings(): MutableCollection<*>? {
    return segList
  }

  class Segment(
    p0: Coordinate, p1: Coordinate,
    private val segStr: SegmentString, private val index: Int
  ) : LineSegment(p0, p1) {

    init {
      normalize()
    }

    fun getSegmentString(): SegmentString {
      return segStr
    }

    fun getIndex(): Int {
      return index
    }
  }

  companion object {
    private fun addSegments(segStrings: Collection<*>, segSet: HashSet<Segment>) {
      for (obj in segStrings) {
        val ss = obj as SegmentString
        addSegments(ss, segSet)
      }
    }

    private fun addSegments(segString: SegmentString, segSet: HashSet<Segment>) {
      for (i in 0 until segString.size() - 1) {
        val p0 = segString.getCoordinate(i)
        val p1 = segString.getCoordinate(i + 1)
        val seg = Segment(p0, p1, segString, i)
        if (segSet.contains(seg)) {
          segSet.remove(seg)
        } else {
          segSet.add(seg)
        }
      }
    }

    private fun extractSegments(segSet: HashSet<Segment>): MutableList<SegmentString> {
      val segList = ArrayList<SegmentString>()
      for (seg in segSet) {
        val ss = seg.getSegmentString()
        val i = seg.getIndex()
        val p0 = ss.getCoordinate(i)
        val p1 = ss.getCoordinate(i + 1)
        val segStr: SegmentString = BasicSegmentString(arrayOf(p0, p1), ss.getData())
        segList.add(segStr)
      }
      return segList
    }
  }
}
