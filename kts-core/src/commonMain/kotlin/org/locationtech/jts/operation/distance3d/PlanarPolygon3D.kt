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

import org.locationtech.jts.algorithm.RayCrossingCounter
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.CoordinateSequence
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.Location
import org.locationtech.jts.geom.Polygon
import org.locationtech.jts.math.Plane3D
import org.locationtech.jts.math.Vector3D

/**
 * Models a polygon lying in a plane in 3-dimensional Cartesian space.
 * The polygon representation is supplied
 * by a [Polygon],
 * containing coordinates with XYZ ordinates.
 * 3D polygons are assumed to lie in a single plane.
 * The plane best fitting the polygon coordinates is
 * computed and is represented by a [Plane3D].
 *
 * @author mdavis
 */
class PlanarPolygon3D(private val poly: Polygon) {

  private val plane: Plane3D = findBestFitPlane(poly)
  private val facingPlane: Int = plane.closestAxisPlane()

  /**
   * Finds a best-fit plane for the polygon,
   * by sampling a few points from the exterior ring.
   *
   *
   * The algorithm used is Newell's algorithm:
   * - a base point for the plane is determined from the average of all vertices
   * - the normal vector is determined by
   * computing the area of the projections on each of the axis planes
   *
   * @param poly the polygon to determine the plane for
   * @return the best-fit plane
   */
  private fun findBestFitPlane(poly: Polygon): Plane3D {
    val seq = poly.getExteriorRing().getCoordinateSequence()
    val basePt = averagePoint(seq)
    val normal = averageNormal(seq)
    return Plane3D(normal, basePt)
  }

  /**
   * Computes an average normal vector from a list of polygon coordinates.
   * Uses Newell's method, which is based
   * on the fact that the vector with components
   * equal to the areas of the projection of the polygon onto
   * the Cartesian axis planes is normal.
   *
   * @param seq the sequence of coordinates for the polygon
   * @return a normal vector
   */
  private fun averageNormal(seq: CoordinateSequence): Vector3D {
    val n = seq.size()
    val sum = Coordinate(0.0, 0.0, 0.0)
    val p1 = Coordinate(0.0, 0.0, 0.0)
    val p2 = Coordinate(0.0, 0.0, 0.0)
    for (i in 0 until n - 1) {
      seq.getCoordinate(i, p1)
      seq.getCoordinate(i + 1, p2)
      sum.x += (p1.y - p2.y) * (p1.getZ() + p2.getZ())
      sum.y += (p1.getZ() - p2.getZ()) * (p1.x + p2.x)
      sum.setZ(sum.getZ() + (p1.x - p2.x) * (p1.y + p2.y))
    }
    sum.x /= n
    sum.y /= n
    sum.setZ(sum.getZ() / n)
    val norm = Vector3D.create(sum).normalize()
    return norm
  }

  /**
   * Computes a point which is the average of all coordinates
   * in a sequence.
   * If the sequence lies in a single plane,
   * the computed point also lies in the plane.
   *
   * @param seq a coordinate sequence
   * @return a Coordinate with averaged ordinates
   */
  private fun averagePoint(seq: CoordinateSequence): Coordinate {
    val a = Coordinate(0.0, 0.0, 0.0)
    val n = seq.size()
    for (i in 0 until n) {
      a.x += seq.getOrdinate(i, CoordinateSequence.X)
      a.y += seq.getOrdinate(i, CoordinateSequence.Y)
      a.setZ(a.getZ() + seq.getOrdinate(i, CoordinateSequence.Z))
    }
    a.x /= n
    a.y /= n
    a.setZ(a.getZ() / n)
    return a
  }

  fun getPlane(): Plane3D {
    return plane
  }

  fun getPolygon(): Polygon {
    return poly
  }

  fun intersects(intPt: Coordinate): Boolean {
    if (Location.EXTERIOR == locate(intPt, poly.getExteriorRing()))
      return false

    for (i in 0 until poly.getNumInteriorRing()) {
      if (Location.INTERIOR == locate(intPt, poly.getInteriorRingN(i)))
        return false
    }
    return true
  }

  private fun locate(pt: Coordinate, ring: LineString): Int {
    val seq = ring.getCoordinateSequence()
    val seqProj = project(seq, facingPlane)
    val ptProj = project(pt, facingPlane)
    return RayCrossingCounter.locatePointInRing(ptProj, seqProj)
  }

  fun intersects(pt: Coordinate, ring: LineString): Boolean {
    val seq = ring.getCoordinateSequence()
    val seqProj = project(seq, facingPlane)
    val ptProj = project(pt, facingPlane)
    return Location.EXTERIOR != RayCrossingCounter.locatePointInRing(ptProj, seqProj)
  }

  companion object {
    private fun project(seq: CoordinateSequence, facingPlane: Int): CoordinateSequence {
      return when (facingPlane) {
        Plane3D.XY_PLANE -> AxisPlaneCoordinateSequence.projectToXY(seq)
        Plane3D.XZ_PLANE -> AxisPlaneCoordinateSequence.projectToXZ(seq)
        else -> AxisPlaneCoordinateSequence.projectToYZ(seq)
      }
    }

    private fun project(p: Coordinate, facingPlane: Int): Coordinate {
      return when (facingPlane) {
        Plane3D.XY_PLANE -> Coordinate(p.x, p.y)
        Plane3D.XZ_PLANE -> Coordinate(p.x, p.getZ())
        // Plane3D.YZ
        else -> Coordinate(p.y, p.getZ())
      }
    }
  }
}
