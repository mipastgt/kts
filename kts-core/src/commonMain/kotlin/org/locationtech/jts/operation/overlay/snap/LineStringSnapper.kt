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
package org.locationtech.jts.operation.overlay.snap

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.CoordinateList
import org.locationtech.jts.geom.LineSegment
import org.locationtech.jts.geom.LineString

/**
 * Snaps the vertices and segments of a [LineString]
 * to a set of target snap vertices.
 * A snap distance tolerance is used to control where snapping is performed.
 *
 *
 * The implementation handles empty geometry and empty snap vertex sets.
 *
 * @author Martin Davis
 * @version 1.7
 */
class LineStringSnapper {

  private var snapTolerance = 0.0

  private val srcPts: Array<Coordinate>
  private val seg = LineSegment() // for reuse during snapping
  private var allowSnappingToSourceVertices = false
  private var isClosed = false

  /**
   * Creates a new snapper using the points in the given [LineString]
   * as source snap points.
   *
   * @param srcLine a LineString to snap (may be empty)
   * @param snapTolerance the snap tolerance to use
   */
  constructor(srcLine: LineString, snapTolerance: Double) :
    this(srcLine.getCoordinates(), snapTolerance)

  /**
   * Creates a new snapper using the given points
   * as source points to be snapped.
   *
   * @param srcPts the points to snap
   * @param snapTolerance the snap tolerance to use
   */
  constructor(srcPts: Array<Coordinate>, snapTolerance: Double) {
    this.srcPts = srcPts
    isClosed = isClosed(srcPts)
    this.snapTolerance = snapTolerance
  }

  fun setAllowSnappingToSourceVertices(allowSnappingToSourceVertices: Boolean) {
    this.allowSnappingToSourceVertices = allowSnappingToSourceVertices
  }

  /**
   * Snaps the vertices and segments of the source LineString
   * to the given set of snap vertices.
   *
   * @param snapPts the vertices to snap to
   * @return a list of the snapped points
   */
  fun snapTo(snapPts: Array<Coordinate>): Array<Coordinate> {
    val coordList = CoordinateList(srcPts)

    snapVertices(coordList, snapPts)
    snapSegments(coordList, snapPts)

    val newPts = coordList.toCoordinateArray()
    return newPts
  }

  /**
   * Snap source vertices to vertices in the target.
   *
   * @param srcCoords the points to snap
   * @param snapPts the points to snap to
   */
  private fun snapVertices(srcCoords: CoordinateList, snapPts: Array<Coordinate>) {
    // try snapping vertices
    // if src is a ring then don't snap final vertex
    val end = if (isClosed) srcCoords.size - 1 else srcCoords.size
    for (i in 0 until end) {
      val srcPt = srcCoords[i]
      val snapVert = findSnapForVertex(srcPt, snapPts)
      if (snapVert != null) {
        // update src with snap pt
        srcCoords.set(i, Coordinate(snapVert))
        // keep final closing point in synch (rings only)
        if (i == 0 && isClosed)
          srcCoords.set(srcCoords.size - 1, Coordinate(snapVert))
      }
    }
  }

  private fun findSnapForVertex(pt: Coordinate, snapPts: Array<Coordinate>): Coordinate? {
    for (i in snapPts.indices) {
      // if point is already equal to a src pt, don't snap
      if (pt.equals2D(snapPts[i]))
        return null
      if (pt.distance(snapPts[i]) < snapTolerance)
        return snapPts[i]
    }
    return null
  }

  /**
   * Snap segments of the source to nearby snap vertices.
   * Source segments are "cracked" at a snap vertex.
   * A single input segment may be snapped several times
   * to different snap vertices.
   *
   *
   * For each distinct snap vertex, at most one source segment
   * is snapped to.  This prevents "cracking" multiple segments
   * at the same point, which would likely cause
   * topology collapse when being used on polygonal linework.
   *
   * @param srcCoords the coordinates of the source linestring to be snapped
   * @param snapPts the target snap vertices
   */
  private fun snapSegments(srcCoords: CoordinateList, snapPts: Array<Coordinate>) {
    // guard against empty input
    if (snapPts.isEmpty()) return

    var distinctPtCount = snapPts.size

    // check for duplicate snap pts when they are sourced from a linear ring.
    // TODO: Need to do this better - need to check *all* snap points for dups (using a Set?)
    if (snapPts[0].equals2D(snapPts[snapPts.size - 1]))
      distinctPtCount = snapPts.size - 1

    for (i in 0 until distinctPtCount) {
      val snapPt = snapPts[i]
      val index = findSegmentIndexToSnap(snapPt, srcCoords)
      /**
       * If a segment to snap to was found, "crack" it at the snap pt.
       * The new pt is inserted immediately into the src segment list,
       * so that subsequent snapping will take place on the modified segments.
       * Duplicate points are not added.
       */
      if (index >= 0) {
        srcCoords.add(index + 1, Coordinate(snapPt), false)
      }
    }
  }

  /**
   * Finds a src segment which snaps to (is close to) the given snap point.
   *
   *
   * Only a single segment is selected for snapping.
   * This prevents multiple segments snapping to the same snap vertex,
   * which would almost certainly cause invalid geometry
   * to be created.
   *
   *
   * Also, if the snap vertex occurs as a vertex in the src coordinate list,
   * no snapping is performed.
   *
   * @param snapPt the point to snap to
   * @param srcCoords the source segment coordinates
   * @return the index of the snapped segment
   * or -1 if no segment snaps to the snap point
   */
  private fun findSegmentIndexToSnap(snapPt: Coordinate, srcCoords: CoordinateList): Int {
    var minDist = Double.MAX_VALUE
    var snapIndex = -1
    for (i in 0 until srcCoords.size - 1) {
      seg.p0 = srcCoords[i]
      seg.p1 = srcCoords[i + 1]

      /**
       * Check if the snap pt is equal to one of the segment endpoints.
       *
       * If the snap pt is already in the src list, don't snap at all.
       */
      if (seg.p0.equals2D(snapPt) || seg.p1.equals2D(snapPt)) {
        if (allowSnappingToSourceVertices)
          continue
        else
          return -1
      }

      val dist = seg.distance(snapPt)
      if (dist < snapTolerance && dist < minDist) {
        minDist = dist
        snapIndex = i
      }
    }
    return snapIndex
  }

  companion object {
    private fun isClosed(pts: Array<Coordinate>): Boolean {
      if (pts.size <= 1) return false
      return pts[0].equals2D(pts[pts.size - 1])
    }
  }
}
