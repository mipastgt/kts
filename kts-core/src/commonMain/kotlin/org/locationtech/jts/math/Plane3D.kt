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

package org.locationtech.jts.math

import org.locationtech.jts.geom.Coordinate
import kotlin.math.abs

/**
 * Models a plane in 3-dimensional Cartesian space.
 *
 * @author mdavis
 *
 */
class Plane3D(private val normal: Vector3D, private val basePt: Coordinate) {

  /**
   * Computes the oriented distance from a point to the plane.
   * The distance is:
   * 
   * - **positive** if the point lies above the plane (relative to the plane normal)
   * - **zero** if the point is on the plane
   * - **negative** if the point lies below the plane (relative to the plane normal)
   * 
   *
   * @param p the point to compute the distance for
   * @return the oriented distance to the plane
   */
  fun orientedDistance(p: Coordinate): Double {
    val pb = Vector3D(p, basePt)
    val pbdDotNormal = pb.dot(normal)
    if (pbdDotNormal.isNaN())
      throw IllegalArgumentException("3D Coordinate has NaN ordinate")
    val d = pbdDotNormal / normal.length()
    return d
  }

  /**
   * Computes the axis plane that this plane lies closest to.
   * 
   * Geometries lying in this plane undergo least distortion
   * (and have maximum area)
   * when projected to the closest axis plane.
   * This provides optimal conditioning for
   * computing a Point-in-Polygon test.
   *
   * @return the index of the closest axis plane.
   */
  fun closestAxisPlane(): Int {
    val xmag = abs(normal.getX())
    val ymag = abs(normal.getY())
    val zmag = abs(normal.getZ())
    if (xmag > ymag) {
      return if (xmag > zmag)
        YZ_PLANE
      else
        XY_PLANE
    } else if (zmag > ymag) {
      return XY_PLANE
    }
    // y >= z
    return XZ_PLANE
  }

  companion object {
    /**
     * Enums for the 3 coordinate planes
     */
    const val XY_PLANE = 1
    const val YZ_PLANE = 2
    const val XZ_PLANE = 3
  }
}
