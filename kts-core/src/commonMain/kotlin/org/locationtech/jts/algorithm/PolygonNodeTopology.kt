/*
 * Copyright (c) 2021 Martin Davis.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.locationtech.jts.algorithm

import kotlin.jvm.JvmStatic

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Quadrant

/**
 * Functions to compute topological information
 * about nodes (ring intersections) in polygonal geometry.
 *
 * @author mdavis
 *
 */
class PolygonNodeTopology {
  companion object {
    /**
     * Check if four segments at a node cross.
     *
     * @param nodePt the node location
     * @param a0 the previous segment endpoint in a ring
     * @param a1 the next segment endpoint in a ring
     * @param b0 the previous segment endpoint in the other ring
     * @param b1 the next segment endpoint in the other ring
     * @return true if the rings cross at the node
     */
    @JvmStatic
    fun isCrossing(nodePt: Coordinate, a0: Coordinate, a1: Coordinate, b0: Coordinate, b1: Coordinate): Boolean {
      var aLo = a0
      var aHi = a1
      if (isAngleGreater(nodePt, aLo, aHi)) {
        aLo = a1
        aHi = a0
      }

      /**
       * Find positions of b0 and b1.
       * The edges cross if the positions are different.
       * If any edge is collinear they are reported as not crossing
       */
      val compBetween0 = compareBetween(nodePt, b0, aLo, aHi)
      if (compBetween0 == 0) return false
      val compBetween1 = compareBetween(nodePt, b1, aLo, aHi)
      if (compBetween1 == 0) return false

      return compBetween0 != compBetween1
    }

    /**
     * Tests whether an segment node-b lies in the interior or exterior
     * of a corner of a ring formed by the two segments a0-node-a1.
     *
     * @param nodePt the node location
     * @param a0 the first vertex of the corner
     * @param a1 the second vertex of the corner
     * @param b the other vertex of the test segment
     * @return true if the segment is interior to the ring corner
     */
    @JvmStatic
    fun isInteriorSegment(nodePt: Coordinate, a0: Coordinate, a1: Coordinate, b: Coordinate): Boolean {
      var aLo = a0
      var aHi = a1
      var isInteriorBetween = true
      if (isAngleGreater(nodePt, aLo, aHi)) {
        aLo = a1
        aHi = a0
        isInteriorBetween = false
      }
      val isBetween = isBetween(nodePt, b, aLo, aHi)
      val isInterior = (isBetween && isInteriorBetween) ||
          (!isBetween && !isInteriorBetween)
      return isInterior
    }

    /**
     * Tests if an edge p is between edges e0 and e1,
     * where the edges all originate at a common origin.
     *
     * @param origin the origin
     * @param p the destination point of edge p
     * @param e0 the destination point of edge e0
     * @param e1 the destination point of edge e1
     * @return true if p is between e0 and e1
     */
    private fun isBetween(origin: Coordinate, p: Coordinate, e0: Coordinate, e1: Coordinate): Boolean {
      val isGreater0 = isAngleGreater(origin, p, e0)
      if (!isGreater0) return false
      val isGreater1 = isAngleGreater(origin, p, e1)
      return !isGreater1
    }

    /**
     * Compares whether an edge p is between or outside the edges e0 and e1,
     * where the edges all originate at a common origin.
     *
     * @param origin the origin
     * @param p the destination point of edge p
     * @param e0 the destination point of edge e0
     * @param e1 the destination point of edge e1
     * @return a negative integer, zero or positive integer as the vector P lies outside, collinear with, or inside the vectors E0 and E1
     */
    private fun compareBetween(origin: Coordinate, p: Coordinate, e0: Coordinate, e1: Coordinate): Int {
      val comp0 = compareAngle(origin, p, e0)
      if (comp0 == 0) return 0
      val comp1 = compareAngle(origin, p, e1)
      if (comp1 == 0) return 0
      if (comp0 > 0 && comp1 < 0) return 1
      return -1
    }

    /**
     * Tests if the angle with the origin of a vector P is greater than that of the
     * vector Q.
     *
     * @param origin the origin of the vectors
     * @param p the endpoint of the vector P
     * @param q the endpoint of the vector Q
     * @return true if vector P has angle greater than Q
     */
    private fun isAngleGreater(origin: Coordinate, p: Coordinate, q: Coordinate): Boolean {
      val quadrantP = quadrant(origin, p)
      val quadrantQ = quadrant(origin, q)

      /**
       * If the vectors are in different quadrants,
       * that determines the ordering
       */
      if (quadrantP > quadrantQ) return true
      if (quadrantP < quadrantQ) return false

      //--- vectors are in the same quadrant
      // Check relative orientation of vectors
      // P > Q if it is CCW of Q
      val orient = Orientation.index(origin, q, p)
      return orient == Orientation.COUNTERCLOCKWISE
    }

    /**
     * Compares the angles of two vectors
     * relative to the positive X-axis at their origin.
     * Angles increase CCW from the X-axis.
     *
     * @param origin the origin of the vectors
     * @param p the endpoint of the vector P
     * @param q the endpoint of the vector Q
     * @return a negative integer, zero, or a positive integer as this vector P has angle less than, equal to, or greater than vector Q
     */
    @JvmStatic
    fun compareAngle(origin: Coordinate, p: Coordinate, q: Coordinate): Int {
      val quadrantP = quadrant(origin, p)
      val quadrantQ = quadrant(origin, q)

      /**
       * If the vectors are in different quadrants,
       * that determines the ordering
       */
      if (quadrantP > quadrantQ) return 1
      if (quadrantP < quadrantQ) return -1

      //--- vectors are in the same quadrant
      // Check relative orientation of vectors
      // P > Q if it is CCW of Q
      val orient = Orientation.index(origin, q, p)
      return when (orient) {
        Orientation.COUNTERCLOCKWISE -> 1
        Orientation.CLOCKWISE -> -1
        else -> 0
      }
    }

    private fun quadrant(origin: Coordinate, p: Coordinate): Int {
      val dx = p.getX() - origin.getX()
      val dy = p.getY() - origin.getY()
      return Quadrant.quadrant(dx, dy)
    }
  }
}
