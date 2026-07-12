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
package org.locationtech.jts.operation.distance3d

import kotlin.jvm.JvmStatic
import kotlin.math.abs

import org.locationtech.jts.algorithm.CGAlgorithms3D
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryCollection
import org.locationtech.jts.geom.LineSegment
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.Polygon
import org.locationtech.jts.operation.distance.GeometryLocation

/**
 * Find two points on two 3D [Geometry]s which lie within a given distance,
 * or else are the nearest points on the geometries (in which case this also
 * provides the distance between the geometries).
 *
 *
 * 3D geometries have vertex Z ordinates defined.
 * 3D [Polygon]s are assumed to lie in a single plane (which is enforced if not actually the case).
 * 3D [LineString]s and [Point]s may have any configuration.
 *
 *
 * The algorithms used are straightforward O(n^2) comparisons.
 *
 * @version 1.7
 */
class Distance3DOp
/**
 * Constructs a DistanceOp that computes the distance and nearest points
 * between the two specified geometries.
 *
 * @param g0 a Geometry
 * @param g1 a Geometry
 * @param terminateDistance the distance on which to terminate the search
 */
  (g0: Geometry?, g1: Geometry?, private val terminateDistance: Double) {

  // input
  private val geom: Array<Geometry?> = arrayOf(g0, g1)
  // working
  private var minDistanceLocation: Array<GeometryLocation?>? = null
  private var minDistance = Double.MAX_VALUE
  private var isDone = false

  /**
   * Constructs a DistanceOp that computes the distance and nearest points
   * between the two specified geometries.
   *
   * @param g0 a Geometry
   * @param g1 a Geometry
   */
  constructor(g0: Geometry?, g1: Geometry?) : this(g0, g1, 0.0)

  /**
   * Report the distance between the nearest points on the input geometries.
   *
   * @return the distance between the geometries, or 0 if either input geometry is empty
   * @throws IllegalArgumentException if either input geometry is null
   */
  fun distance(): Double {
    if (geom[0] == null || geom[1] == null)
      throw IllegalArgumentException("null geometries are not supported")
    if (geom[0]!!.isEmpty() || geom[1]!!.isEmpty())
      return 0.0

    computeMinDistance()
    return minDistance
  }

  /**
   * Report the coordinates of the nearest points in the input geometries. The
   * points are presented in the same order as the input Geometries.
   *
   * @return a pair of [Coordinate]s of the nearest points
   */
  fun nearestPoints(): Array<Coordinate> {
    computeMinDistance()
    val nearestPts = arrayOf(
      minDistanceLocation!![0]!!.getCoordinate(),
      minDistanceLocation!![1]!!.getCoordinate()
    )
    return nearestPts
  }

  /**
   * Report the locations of the nearest points in the input geometries. The
   * locations are presented in the same order as the input Geometries.
   *
   * @return a pair of [GeometryLocation]s for the nearest points
   */
  fun nearestLocations(): Array<GeometryLocation?> {
    computeMinDistance()
    return minDistanceLocation!!
  }

  private fun updateDistance(
    dist: Double,
    loc0: GeometryLocation, loc1: GeometryLocation,
    flip: Boolean
  ) {
    this.minDistance = dist
    val index = if (flip) 1 else 0
    minDistanceLocation!![index] = loc0
    minDistanceLocation!![1 - index] = loc1
    if (minDistance < terminateDistance)
      isDone = true
  }

  private fun computeMinDistance() {
    // only compute once
    if (minDistanceLocation != null)
      return
    minDistanceLocation = arrayOfNulls(2)

    val geomIndex = mostPolygonalIndex()
    val flip = geomIndex == 1
    computeMinDistanceMultiMulti(geom[geomIndex]!!, geom[1 - geomIndex]!!, flip)
  }

  /**
   * Finds the index of the "most polygonal" input geometry.
   * This optimizes the computation of the best-fit plane,
   * since it is cached only for the left-hand geometry.
   *
   * @return the index of the most polygonal geometry
   */
  private fun mostPolygonalIndex(): Int {
    val dim0 = geom[0]!!.getDimension()
    val dim1 = geom[1]!!.getDimension()
    if (dim0 >= 2 && dim1 >= 2) {
      if (geom[0]!!.getNumPoints() > geom[1]!!.getNumPoints())
        return 0
      return 1
    }
    // no more than one is dim 2
    if (dim0 >= 2) return 0
    if (dim1 >= 2) return 1
    // both dim <= 1 - don't flip
    return 0
  }

  private fun computeMinDistanceMultiMulti(g0: Geometry, g1: Geometry, flip: Boolean) {
    if (g0 is GeometryCollection) {
      val n = g0.getNumGeometries()
      for (i in 0 until n) {
        val g = g0.getGeometryN(i)
        computeMinDistanceMultiMulti(g, g1, flip)
        if (isDone) return
      }
    } else {
      // handle case of multigeom component being empty
      if (g0.isEmpty())
        return

      // compute planar polygon only once for efficiency
      if (g0 is Polygon) {
        computeMinDistanceOneMulti(polyPlane(g0), g1, flip)
      } else
        computeMinDistanceOneMulti(g0, g1, flip)
    }
  }

  private fun computeMinDistanceOneMulti(g0: Geometry, g1: Geometry, flip: Boolean) {
    if (g1 is GeometryCollection) {
      val n = g1.getNumGeometries()
      for (i in 0 until n) {
        val g = g1.getGeometryN(i)
        computeMinDistanceOneMulti(g0, g, flip)
        if (isDone) return
      }
    } else {
      computeMinDistance(g0, g1, flip)
    }
  }

  private fun computeMinDistanceOneMulti(poly: PlanarPolygon3D, geom: Geometry, flip: Boolean) {
    if (geom is GeometryCollection) {
      val n = geom.getNumGeometries()
      for (i in 0 until n) {
        val g = geom.getGeometryN(i)
        computeMinDistanceOneMulti(poly, g, flip)
        if (isDone) return
      }
    } else {
      if (geom is Point) {
        computeMinDistancePolygonPoint(poly, geom, flip)
        return
      }
      if (geom is LineString) {
        computeMinDistancePolygonLine(poly, geom, flip)
        return
      }
      if (geom is Polygon) {
        computeMinDistancePolygonPolygon(poly, geom, flip)
        return
      }
    }
  }

  private fun computeMinDistance(g0: Geometry, g1: Geometry, flip: Boolean) {
    if (g0 is Point) {
      if (g1 is Point) {
        computeMinDistancePointPoint(g0, g1, flip)
        return
      }
      if (g1 is LineString) {
        computeMinDistanceLinePoint(g1, g0, !flip)
        return
      }
      if (g1 is Polygon) {
        computeMinDistancePolygonPoint(polyPlane(g1), g0, !flip)
        return
      }
    }
    if (g0 is LineString) {
      if (g1 is Point) {
        computeMinDistanceLinePoint(g0, g1, flip)
        return
      }
      if (g1 is LineString) {
        computeMinDistanceLineLine(g0, g1, flip)
        return
      }
      if (g1 is Polygon) {
        computeMinDistancePolygonLine(polyPlane(g1), g0, !flip)
        return
      }
    }
    if (g0 is Polygon) {
      if (g1 is Point) {
        computeMinDistancePolygonPoint(polyPlane(g0), g1, flip)
        return
      }
      if (g1 is LineString) {
        computeMinDistancePolygonLine(polyPlane(g0), g1, flip)
        return
      }
      if (g1 is Polygon) {
        computeMinDistancePolygonPolygon(polyPlane(g0), g1, flip)
        return
      }
    }
  }

  /**
   * Computes distance between two polygons.
   */
  private fun computeMinDistancePolygonPolygon(poly0: PlanarPolygon3D, poly1: Polygon, flip: Boolean) {
    computeMinDistancePolygonRings(poly0, poly1, flip)
    if (isDone) return
    val polyPlane1 = PlanarPolygon3D(poly1)
    computeMinDistancePolygonRings(polyPlane1, poly0.getPolygon(), flip)
  }

  /**
   * Compute distance between a polygon and the rings of another.
   */
  private fun computeMinDistancePolygonRings(poly: PlanarPolygon3D, ringPoly: Polygon, flip: Boolean) {
    // compute shell ring
    computeMinDistancePolygonLine(poly, ringPoly.getExteriorRing(), flip)
    if (isDone) return
    // compute hole rings
    val nHole = ringPoly.getNumInteriorRing()
    for (i in 0 until nHole) {
      computeMinDistancePolygonLine(poly, ringPoly.getInteriorRingN(i), flip)
      if (isDone) return
    }
  }

  private fun computeMinDistancePolygonLine(poly: PlanarPolygon3D, line: LineString, flip: Boolean) {

    // first test if line intersects polygon
    val intPt = intersection(poly, line)
    if (intPt != null) {
      updateDistance(
        0.0,
        GeometryLocation(poly.getPolygon(), 0, intPt),
        GeometryLocation(line, 0, intPt),
        flip
      )
      return
    }

    // if no intersection, then compute line distance to polygon rings
    computeMinDistanceLineLine(poly.getPolygon().getExteriorRing(), line, flip)
    if (isDone) return
    val nHole = poly.getPolygon().getNumInteriorRing()
    for (i in 0 until nHole) {
      computeMinDistanceLineLine(poly.getPolygon().getInteriorRingN(i), line, flip)
      if (isDone) return
    }
  }

  private fun intersection(poly: PlanarPolygon3D, line: LineString): Coordinate? {
    val seq = line.getCoordinateSequence()
    if (seq.size() == 0)
      return null

    // start point of line
    val p0 = Coordinate()
    seq.getCoordinate(0, p0)
    var d0 = poly.getPlane().orientedDistance(p0)

    // for each segment in the line
    val p1 = Coordinate()
    for (i in 0 until seq.size() - 1) {
      seq.getCoordinate(i, p0)
      seq.getCoordinate(i + 1, p1)
      val d1 = poly.getPlane().orientedDistance(p1)

      /**
       * If the oriented distances of the segment endpoints have the same sign,
       * the segment does not cross the plane, and is skipped.
       */
      if (d0 * d1 > 0)
        continue

      /**
       * Compute segment-plane intersection point
       * which is then used for a point-in-polygon test.
       */
      val intPt = segmentPoint(p0, p1, d0, d1)
      // Coordinate intPt = polyPlane.intersection(p0, p1, s0, s1);
      if (poly.intersects(intPt)) {
        return intPt
      }

      // shift to next segment
      d0 = d1
    }
    return null
  }

  private fun computeMinDistancePolygonPoint(polyPlane: PlanarPolygon3D, point: Point, flip: Boolean) {
    val pt = point.getCoordinate()!!

    val shell = polyPlane.getPolygon().getExteriorRing()
    if (polyPlane.intersects(pt, shell)) {
      // point is either inside or in a hole

      val nHole = polyPlane.getPolygon().getNumInteriorRing()
      for (i in 0 until nHole) {
        val hole = polyPlane.getPolygon().getInteriorRingN(i)
        if (polyPlane.intersects(pt, hole)) {
          computeMinDistanceLinePoint(hole, point, flip)
          return
        }
      }
      // point is in interior of polygon
      // distance is distance to polygon plane
      val dist = abs(polyPlane.getPlane().orientedDistance(pt))
      updateDistance(
        dist,
        GeometryLocation(polyPlane.getPolygon(), 0, pt),
        GeometryLocation(point, 0, pt),
        flip
      )
    }
    // point is outside polygon, so compute distance to shell linework
    computeMinDistanceLinePoint(shell, point, flip)
  }

  private fun computeMinDistanceLineLine(line0: LineString, line1: LineString, flip: Boolean) {
    val coord0 = line0.getCoordinates()
    val coord1 = line1.getCoordinates()
    // brute force approach!
    for (i in 0 until coord0.size - 1) {
      for (j in 0 until coord1.size - 1) {
        val dist = CGAlgorithms3D.distanceSegmentSegment(
          coord0[i],
          coord0[i + 1], coord1[j], coord1[j + 1]
        )
        if (dist < minDistance) {
          minDistance = dist
          // TODO: compute closest pts in 3D
          val seg0 = LineSegment(coord0[i], coord0[i + 1])
          val seg1 = LineSegment(coord1[j], coord1[j + 1])
          val closestPt = seg0.closestPoints(seg1)
          updateDistance(
            dist,
            GeometryLocation(line0, i, closestPt[0]),
            GeometryLocation(line1, j, closestPt[1]),
            flip
          )
        }
        if (isDone) return
      }
    }
  }

  private fun computeMinDistanceLinePoint(line: LineString, point: Point, flip: Boolean) {
    val lineCoord = line.getCoordinates()
    val coord = point.getCoordinate()!!
    // brute force approach!
    for (i in 0 until lineCoord.size - 1) {
      val dist = CGAlgorithms3D.distancePointSegment(
        coord, lineCoord[i],
        lineCoord[i + 1]
      )
      if (dist < minDistance) {
        val seg = LineSegment(lineCoord[i], lineCoord[i + 1])
        val segClosestPoint = seg.closestPoint(coord)
        updateDistance(
          dist,
          GeometryLocation(line, i, segClosestPoint),
          GeometryLocation(point, 0, coord),
          flip
        )
      }
      if (isDone) return
    }
  }

  private fun computeMinDistancePointPoint(point0: Point, point1: Point, flip: Boolean) {
    val dist = CGAlgorithms3D.distance(
      point0.getCoordinate()!!,
      point1.getCoordinate()!!
    )
    if (dist < minDistance) {
      updateDistance(
        dist,
        GeometryLocation(point0, 0, point0.getCoordinate()!!),
        GeometryLocation(point1, 0, point1.getCoordinate()!!),
        flip
      )
    }
  }

  companion object {
    /**
     * Compute the distance between the nearest points of two geometries.
     *
     * @param g0 a [Geometry]
     * @param g1 another [Geometry]
     * @return the distance between the geometries
     */
    @JvmStatic
    fun distance(g0: Geometry, g1: Geometry): Double {
      val distOp = Distance3DOp(g0, g1)
      return distOp.distance()
    }

    /**
     * Test whether two geometries lie within a given distance of each other.
     *
     * @param g0 a [Geometry]
     * @param g1 another [Geometry]
     * @param distance the distance to test
     * @return true if g0.distance(g1) &lt;= distance
     */
    @JvmStatic
    fun isWithinDistance(g0: Geometry, g1: Geometry, distance: Double): Boolean {
      val distOp = Distance3DOp(g0, g1, distance)
      return distOp.distance() <= distance
    }

    /**
     * Compute the the nearest points of two geometries. The points are
     * presented in the same order as the input Geometries.
     *
     * @param g0 a [Geometry]
     * @param g1 another [Geometry]
     * @return the nearest points in the geometries
     */
    @JvmStatic
    fun nearestPoints(g0: Geometry, g1: Geometry): Array<Coordinate> {
      val distOp = Distance3DOp(g0, g1)
      return distOp.nearestPoints()
    }

    /**
     * Convenience method to create a Plane3DPolygon
     */
    private fun polyPlane(poly: Geometry): PlanarPolygon3D {
      return PlanarPolygon3D(poly as Polygon)
    }

    /**
     * Computes a point at a distance along a segment
     * specified by two relatively proportional values.
     * The fractional distance along the segment is d0/(d0+d1).
     */
    private fun segmentPoint(p0: Coordinate, p1: Coordinate, d0: Double, d1: Double): Coordinate {
      if (d0 <= 0) return Coordinate(p0)
      if (d1 <= 0) return Coordinate(p1)

      val f = abs(d0) / (abs(d0) + abs(d1))
      val intx = p0.x + f * (p1.x - p0.x)
      val inty = p0.y + f * (p1.y - p0.y)
      val intz = p0.getZ() + f * (p1.getZ() - p0.getZ())
      return Coordinate(intx, inty, intz)
    }
  }
}
