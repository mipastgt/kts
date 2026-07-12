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
 * A noder which extracts chains of boundary segments
 * as [SegmentString]s from a polygonal coverage.
 * Boundary segments are those which are not duplicated in the input polygonal coverage.
 * Extracting chains of segments minimize the number of segment strings created,
 * which produces a more efficient topological graph structure.
 *
 *
 * This enables fast overlay of polygonal coverages in CoverageUnion.
 * Using this noder is faster than [SegmentExtractingNoder]
 * and [BoundarySegmentNoder].
 *
 *
 * No precision reduction is carried out.
 * If that is required, another noder must be used (such as a snap-rounding noder),
 * or the input must be precision-reduced beforehand.
 *
 * @author Martin Davis
 */
class BoundaryChainNoder : Noder {

  private var chainList: MutableList<SegmentString>? = null

  override fun computeNodes(segStrings: Collection<*>?) {
    val segSet = HashSet<Segment>()
    val boundaryChains = arrayOfNulls<BoundaryChainMap>(segStrings!!.size)
    addSegments(segStrings, segSet, boundaryChains)
    markBoundarySegments(segSet)
    chainList = extractChains(boundaryChains)
  }

  override fun getNodedSubstrings(): MutableCollection<*>? {
    return chainList
  }

  private class BoundaryChainMap(private val segString: SegmentString) {
    private val isBoundary: BooleanArray = BooleanArray(segString.size() - 1)

    fun setBoundarySegment(index: Int) {
      isBoundary[index] = true
    }

    fun createChains(chainList: MutableList<SegmentString>) {
      var endIndex = 0
      while (true) {
        val startIndex = findChainStart(endIndex)
        if (startIndex >= segString.size() - 1)
          break
        endIndex = findChainEnd(startIndex)
        val ss = createChain(segString, startIndex, endIndex)
        chainList.add(ss)
      }
    }

    private fun findChainStart(index: Int): Int {
      var i = index
      while (i < isBoundary.size && !isBoundary[i]) {
        i++
      }
      return i
    }

    private fun findChainEnd(index: Int): Int {
      var i = index
      i++
      while (i < isBoundary.size && isBoundary[i]) {
        i++
      }
      return i
    }

    companion object {
      private fun createChain(segString: SegmentString, startIndex: Int, endIndex: Int): SegmentString {
        val pts = arrayOfNulls<Coordinate>(endIndex - startIndex + 1)
        var ipts = 0
        for (i in startIndex until endIndex + 1) {
          pts[ipts++] = segString.getCoordinate(i).copy()
        }
        @Suppress("UNCHECKED_CAST")
        return BasicSegmentString(pts as Array<Coordinate>, segString.getData())
      }
    }
  }

  private class Segment(
    p0: Coordinate, p1: Coordinate,
    private val segMap: BoundaryChainMap, private val index: Int
  ) : LineSegment(p0, p1) {

    init {
      normalize()
    }

    fun markBoundary() {
      segMap.setBoundarySegment(index)
    }
  }

  companion object {
    private fun addSegments(
      segStrings: Collection<*>, segSet: HashSet<Segment>,
      boundaryChains: Array<BoundaryChainMap?>
    ) {
      var i = 0
      for (obj in segStrings) {
        val ss = obj as SegmentString
        val chainMap = BoundaryChainMap(ss)
        boundaryChains[i++] = chainMap
        addSegments(ss, chainMap, segSet)
      }
    }

    private fun addSegments(segString: SegmentString, chainMap: BoundaryChainMap, segSet: HashSet<Segment>) {
      for (i in 0 until segString.size() - 1) {
        val p0 = segString.getCoordinate(i)
        val p1 = segString.getCoordinate(i + 1)
        val seg = Segment(p0, p1, chainMap, i)
        if (segSet.contains(seg)) {
          segSet.remove(seg)
        } else {
          segSet.add(seg)
        }
      }
    }

    private fun markBoundarySegments(segSet: HashSet<Segment>) {
      for (seg in segSet) {
        seg.markBoundary()
      }
    }

    private fun extractChains(boundaryChains: Array<BoundaryChainMap?>): MutableList<SegmentString> {
      val chainList = ArrayList<SegmentString>()
      for (chainMap in boundaryChains) {
        chainMap!!.createChains(chainList)
      }
      return chainList
    }
  }
}
