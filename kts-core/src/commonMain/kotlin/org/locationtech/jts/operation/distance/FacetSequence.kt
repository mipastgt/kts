/*
 * Copyright (c) 2016 Martin Davis.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.locationtech.jts.operation.distance

import org.locationtech.jts.algorithm.Distance
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.CoordinateSequence
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.LineSegment

/**
 * Represents a sequence of facets (points or line segments)
 * of a [Geometry]
 * specified by a subsequence of a [CoordinateSequence].
 *
 * @author Martin Davis
 */
class FacetSequence
/**
 * Creates a new sequence of facets based on a [CoordinateSequence]
 * contained in the given [Geometry].
 *
 * @param geom the geometry containing the facets
 * @param pts the sequence containing the facet points
 * @param start the index of the start point
 * @param end the index of the end point + 1
 */
  (
  private val geom: Geometry?,
  private val pts: CoordinateSequence,
  private val start: Int,
  private val end: Int
) {

  /**
   * Creates a new sequence of facets based on a [CoordinateSequence].
   *
   * @param pts the sequence containing the facet points
   * @param start the index of the start point
   * @param end the index of the end point + 1
   */
  constructor(pts: CoordinateSequence, start: Int, end: Int) : this(null, pts, start, end)

  /**
   * Creates a new sequence for a single point from a [CoordinateSequence].
   *
   * @param pts the sequence containing the facet point
   * @param start the index of the point
   */
  constructor(pts: CoordinateSequence, start: Int) : this(null, pts, start, start + 1)

  fun getEnvelope(): Envelope {
    val env = Envelope()
    for (i in start until end) {
      env.expandToInclude(pts.getX(i), pts.getY(i))
    }
    return env
  }

  fun size(): Int {
    return end - start
  }

  fun getCoordinate(index: Int): Coordinate {
    return pts.getCoordinate(start + index)
  }

  fun isPoint(): Boolean {
    return end - start == 1
  }

  /**
   * Computes the distance between this and another
   * `FacetSequence`.
   *
   * @param facetSeq the sequence to compute the distance to
   * @return the minimum distance between the sequences
   */
  fun distance(facetSeq: FacetSequence): Double {
    val isPoint = isPoint()
    val isPointOther = facetSeq.isPoint()
    val distance: Double

    if (isPoint && isPointOther) {
      val pt = pts.getCoordinate(start)
      val seqPt = facetSeq.pts.getCoordinate(facetSeq.start)
      distance = pt.distance(seqPt)
    } else if (isPoint) {
      val pt = pts.getCoordinate(start)
      distance = computeDistancePointLine(pt, facetSeq, null)
    } else if (isPointOther) {
      val seqPt = facetSeq.pts.getCoordinate(facetSeq.start)
      distance = computeDistancePointLine(seqPt, this, null)
    } else {
      distance = computeDistanceLineLine(facetSeq, null)
    }
    return distance
  }

  /**
   * Computes the locations of the nearest points between this sequence
   * and another sequence.
   * The locations are presented in the same order as the input sequences.
   *
   * @return a pair of [GeometryLocation]s for the nearest points
   */
  fun nearestLocations(facetSeq: FacetSequence): Array<GeometryLocation?> {
    val isPoint = isPoint()
    val isPointOther = facetSeq.isPoint()
    val locs = arrayOfNulls<GeometryLocation>(2)

    if (isPoint && isPointOther) {
      val pt = pts.getCoordinate(start)
      val seqPt = facetSeq.pts.getCoordinate(facetSeq.start)
      locs[0] = GeometryLocation(geom!!, start, Coordinate(pt))
      locs[1] = GeometryLocation(facetSeq.geom!!, facetSeq.start, Coordinate(seqPt))
    } else if (isPoint) {
      val pt = pts.getCoordinate(start)
      computeDistancePointLine(pt, facetSeq, locs)
    } else if (isPointOther) {
      val seqPt = facetSeq.pts.getCoordinate(facetSeq.start)
      computeDistancePointLine(seqPt, this, locs)
      // unflip the locations
      val tmp = locs[0]
      locs[0] = locs[1]
      locs[1] = tmp
    } else {
      computeDistanceLineLine(facetSeq, locs)
    }
    return locs
  }

  private fun computeDistanceLineLine(facetSeq: FacetSequence, locs: Array<GeometryLocation?>?): Double {
    // both linear - compute minimum segment-segment distance
    var minDistance = Double.MAX_VALUE

    for (i in start until end - 1) {
      val p0 = pts.getCoordinate(i)
      val p1 = pts.getCoordinate(i + 1)
      for (j in facetSeq.start until facetSeq.end - 1) {
        val q0 = facetSeq.pts.getCoordinate(j)
        val q1 = facetSeq.pts.getCoordinate(j + 1)

        val dist = Distance.segmentToSegment(p0, p1, q0, q1)
        if (dist < minDistance) {
          minDistance = dist
          if (locs != null) updateNearestLocationsLineLine(i, p0, p1, facetSeq, j, q0, q1, locs)
          if (minDistance <= 0.0) return minDistance
        }
      }
    }
    return minDistance
  }

  private fun updateNearestLocationsLineLine(
    i: Int, p0: Coordinate, p1: Coordinate, facetSeq: FacetSequence, j: Int,
    q0: Coordinate, q1: Coordinate, locs: Array<GeometryLocation?>
  ) {
    val seg0 = LineSegment(p0, p1)
    val seg1 = LineSegment(q0, q1)
    val closestPt = seg0.closestPoints(seg1)
    locs[0] = GeometryLocation(geom!!, i, Coordinate(closestPt[0]))
    locs[1] = GeometryLocation(facetSeq.geom!!, j, Coordinate(closestPt[1]))
  }

  private fun computeDistancePointLine(pt: Coordinate, facetSeq: FacetSequence, locs: Array<GeometryLocation?>?): Double {
    var minDistance = Double.MAX_VALUE

    for (i in facetSeq.start until facetSeq.end - 1) {
      val q0 = facetSeq.pts.getCoordinate(i)
      val q1 = facetSeq.pts.getCoordinate(i + 1)
      val dist = Distance.pointToSegment(pt, q0, q1)
      if (dist < minDistance) {
        minDistance = dist
        if (locs != null) updateNearestLocationsPointLine(pt, facetSeq, i, q0, q1, locs)
        if (minDistance <= 0.0) return minDistance
      }
    }
    return minDistance
  }

  private fun updateNearestLocationsPointLine(
    pt: Coordinate,
    facetSeq: FacetSequence, i: Int, q0: Coordinate, q1: Coordinate,
    locs: Array<GeometryLocation?>
  ) {
    locs[0] = GeometryLocation(geom!!, start, Coordinate(pt))
    val seg = LineSegment(q0, q1)
    val segClosestPoint = seg.closestPoint(pt)
    locs[1] = GeometryLocation(facetSeq.geom!!, i, Coordinate(segClosestPoint))
  }

  override fun toString(): String {
    val buf = StringBuilder()
    buf.append("LINESTRING ( ")
    val p = Coordinate()
    for (i in start until end) {
      if (i > start)
        buf.append(", ")
      pts.getCoordinate(i, p)
      buf.append(p.x.toString() + " " + p.y)
    }
    buf.append(" )")
    return buf.toString()
  }
}
