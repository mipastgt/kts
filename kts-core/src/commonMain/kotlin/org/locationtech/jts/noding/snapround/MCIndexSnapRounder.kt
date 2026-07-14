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
package org.locationtech.jts.noding.snapround

import org.locationtech.jts.algorithm.LineIntersector
import org.locationtech.jts.algorithm.RobustLineIntersector
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.PrecisionModel
import org.locationtech.jts.noding.InteriorIntersectionFinderAdder
import org.locationtech.jts.noding.MCIndexNoder
import org.locationtech.jts.noding.NodedSegmentString
import org.locationtech.jts.noding.Noder
import org.locationtech.jts.noding.SegmentString

/**
 * Uses Snap Rounding to compute a rounded,
 * fully noded arrangement from a set of [SegmentString]s.
 *
 * @deprecated Not robust. Use [SnapRoundingNoder] instead.
 *
 */
@Deprecated("Not robust. Use SnapRoundingNoder instead.")
class MCIndexSnapRounder(private val pm: PrecisionModel) : Noder {
  private val li: LineIntersector = RobustLineIntersector()
  private val scaleFactor: Double
  private var noder: MCIndexNoder? = null
  private var pointSnapper: MCIndexPointSnapper? = null
  private var nodedSegStrings: Collection<*>? = null

  init {
    li.setPrecisionModel(pm)
    scaleFactor = pm.getScale()
  }

  override fun getNodedSubstrings(): MutableCollection<*> {
    return NodedSegmentString.getNodedSubstrings(nodedSegStrings)
  }

  override fun computeNodes(inputSegmentStrings: Collection<*>?) {
    this.nodedSegStrings = inputSegmentStrings
    noder = MCIndexNoder()
    pointSnapper = MCIndexPointSnapper(noder!!.getIndex())
    snapRound(inputSegmentStrings, li)
  }

  private fun snapRound(segStrings: Collection<*>?, li: LineIntersector) {
    val intersections = findInteriorIntersections(segStrings, li)
    computeIntersectionSnaps(intersections)
    computeVertexSnaps(segStrings)
  }

  /**
   * Computes all interior intersections in the collection of [SegmentString]s,
   * and returns their [Coordinate]s.
   *
   * Does NOT node the segStrings.
   *
   * @return a list of Coordinates for the intersections
   */
  private fun findInteriorIntersections(segStrings: Collection<*>?, li: LineIntersector): MutableList<Coordinate> {
    val intFinderAdder = InteriorIntersectionFinderAdder(li)
    noder!!.setSegmentIntersector(intFinderAdder)
    noder!!.computeNodes(segStrings)
    return intFinderAdder.getInteriorIntersections()
  }

  /**
   * Snaps segments to nodes created by segment intersections.
   */
  private fun computeIntersectionSnaps(snapPts: MutableList<Coordinate>) {
    val it = snapPts.iterator()
    while (it.hasNext()) {
      val snapPt = it.next()
      val hotPixel = HotPixel(snapPt, scaleFactor)
      pointSnapper!!.snap(hotPixel)
    }
  }

  /**
   * Snaps segments to all vertices.
   *
   * @param edges the list of segment strings to snap together
   */
  fun computeVertexSnaps(edges: Collection<*>?) {
    val i0 = edges!!.iterator()
    while (i0.hasNext()) {
      val edge0 = i0.next() as NodedSegmentString
      computeVertexSnaps(edge0)
    }
  }

  /**
   * Snaps segments to the vertices of a Segment String.
   */
  private fun computeVertexSnaps(e: NodedSegmentString) {
    val pts0 = e.getCoordinates()
    for (i in pts0.indices) {
      val hotPixel = HotPixel(pts0[i], scaleFactor)
      val isNodeAdded = pointSnapper!!.snap(hotPixel, e, i)
      // if a node is created for a vertex, that vertex must be noded too
      if (isNodeAdded) {
        e.addIntersection(pts0[i], i)
      }
    }
  }
}
