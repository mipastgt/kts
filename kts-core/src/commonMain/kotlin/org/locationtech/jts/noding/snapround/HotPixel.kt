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
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong

import org.locationtech.jts.algorithm.CGAlgorithmsDD
import org.locationtech.jts.algorithm.LineIntersector
import org.locationtech.jts.algorithm.RobustLineIntersector
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.io.WKTWriter

/**
 * Implements a "hot pixel" as used in the Snap Rounding algorithm.
 * A hot pixel is a square region centred
 * on the rounded valud of the coordinate given,
 * and of width equal to the size of the scale factor.
 * It is a partially open region, which contains
 * the interior of the tolerance square and
 * the boundary
 * **minus** the top and right segments.
 * This ensures that every point of the space lies in a unique hot pixel.
 * It also matches the rounding semantics for numbers.
 *
 *
 * The hot pixel operations are all computed in the integer domain
 * to avoid rounding problems.
 *
 *
 * Hot Pixels support being marked as nodes.
 * This is used to prevent introducing nodes at line vertices
 * which do not have other lines snapped to them.
 *
 * @version 1.7
 */
class HotPixel
/**
 * Creates a new hot pixel centered on a rounded point, using a given scale factor.
 * The scale factor must be strictly positive (non-zero).
 *
 * @param pt the coordinate at the centre of the pixel (already rounded)
 * @param scaleFactor the scaleFactor determining the pixel size.  Must be &gt; 0
 */
  (pt: Coordinate, private val scaleFactor: Double) {

  private val originalPt: Coordinate = pt

  /**
   * The scaled ordinates of the hot pixel point
   */
  private var hpx: Double
  private var hpy: Double

  /**
   * Indicates if this hot pixel must be a node in the output.
   */
  private var node = false

  init {
    if (scaleFactor <= 0)
      throw IllegalArgumentException("Scale factor must be non-zero")
    if (scaleFactor != 1.0) {
      hpx = scaleRound(pt.getX())
      hpy = scaleRound(pt.getY())
    } else {
      hpx = pt.getX()
      hpy = pt.getY()
    }
  }

  /**
   * Gets the coordinate this hot pixel is based at.
   *
   * @return the coordinate of the pixel
   */
  fun getCoordinate(): Coordinate = originalPt

  /**
   * Gets the scale factor for the precision grid for this pixel.
   *
   * @return the pixel scale factor
   */
  fun getScaleFactor(): Double {
    return scaleFactor
  }

  /**
   * Gets the width of the hot pixel in the original coordinate system.
   *
   * @return the width of the hot pixel tolerance square
   */
  fun getWidth(): Double {
    return 1.0 / scaleFactor
  }

  /**
   * Tests whether this pixel has been marked as a node.
   *
   * @return true if the pixel is marked as a node
   */
  fun isNode(): Boolean {
    return node
  }

  /**
   * Sets this pixel to be a node.
   */
  fun setToNode() {
    //System.out.println(this + " set to Node");
    node = true
  }

  private fun scaleRound(value: Double): Double {
    return (value * scaleFactor).roundToLong().toDouble()
  }

  /**
   * Scale without rounding.
   * This ensures intersections are checked against original
   * linework.
   * This is required to ensure that intersections are not missed
   * because the segment is moved by snapping.
   */
  private fun scale(value: Double): Double {
    return value * scaleFactor
  }

  /**
   * Tests whether a coordinate lies in (intersects) this hot pixel.
   *
   * @param p the coordinate to test
   * @return true if the coordinate intersects this hot pixel
   */
  fun intersects(p: Coordinate): Boolean {
    val x = scale(p.x)
    val y = scale(p.y)
    if (x >= hpx + TOLERANCE) return false
    // check Left side
    if (x < hpx - TOLERANCE) return false
    // check Top side
    if (y >= hpy + TOLERANCE) return false
    // check Bottom side
    if (y < hpy - TOLERANCE) return false
    return true
  }

  /**
   * Tests whether the line segment (p0-p1)
   * intersects this hot pixel.
   *
   * @param p0 the first coordinate of the line segment to test
   * @param p1 the second coordinate of the line segment to test
   * @return true if the line segment intersects this hot pixel
   */
  fun intersects(p0: Coordinate, p1: Coordinate): Boolean {
    if (scaleFactor == 1.0)
      return intersectsScaled(p0.x, p0.y, p1.x, p1.y)

    val sp0x = scale(p0.x)
    val sp0y = scale(p0.y)
    val sp1x = scale(p1.x)
    val sp1y = scale(p1.y)
    return intersectsScaled(sp0x, sp0y, sp1x, sp1y)
  }

  private fun intersectsScaled(
    p0x: Double, p0y: Double,
    p1x: Double, p1y: Double
  ): Boolean {
    // determine oriented segment pointing in positive X direction
    var px = p0x
    var py = p0y
    var qx = p1x
    var qy = p1y
    if (px > qx) {
      px = p1x
      py = p1y
      qx = p0x
      qy = p0y
    }
    /**
     * Report false if segment env does not intersect pixel env.
     * This check reflects the fact that the pixel Top and Right sides
     * are open (not part of the pixel).
     */
    // check Right side
    val maxx = hpx + TOLERANCE
    val segMinx = min(px, qx)
    if (segMinx >= maxx) return false
    // check Left side
    val minx = hpx - TOLERANCE
    val segMaxx = max(px, qx)
    if (segMaxx < minx) return false
    // check Top side
    val maxy = hpy + TOLERANCE
    val segMiny = min(py, qy)
    if (segMiny >= maxy) return false
    // check Bottom side
    val miny = hpy - TOLERANCE
    val segMaxy = max(py, qy)
    if (segMaxy < miny) return false

    /**
     * Vertical or horizontal segments must now intersect
     * the segment interior or Left or Bottom sides.
     */
    //---- check vertical segment
    if (px == qx) {
      return true
    }
    //---- check horizontal segment
    if (py == qy) {
      return true
    }

    /**
     * Now know segment is not horizontal or vertical.
     *
     * Compute orientation WRT each pixel corner.
     * If corner orientation == 0,
     * segment intersects the corner.
     * From the corner and whether segment is heading up or down,
     * can determine intersection or not.
     *
     * Otherwise, check whether segment crosses interior of pixel side
     * This is the case if the orientations for each corner of the side are different.
     */

    val orientUL = CGAlgorithmsDD.orientationIndex(px, py, qx, qy, minx, maxy)
    if (orientUL == 0) {
      // upward segment does not intersect pixel interior
      if (py < qy) return false
      // downward segment must intersect pixel interior
      return true
    }

    val orientUR = CGAlgorithmsDD.orientationIndex(px, py, qx, qy, maxx, maxy)
    if (orientUR == 0) {
      // downward segment does not intersect pixel interior
      if (py > qy) return false
      // upward segment must intersect pixel interior
      return true
    }
    //--- check crossing Top side
    if (orientUL != orientUR) {
      return true
    }

    val orientLL = CGAlgorithmsDD.orientationIndex(px, py, qx, qy, minx, miny)
    if (orientLL == 0) {
      // segment crossed LL corner, which is the only one in pixel interior
      return true
    }
    //--- check crossing Left side
    if (orientLL != orientUL) {
      return true
    }

    val orientLR = CGAlgorithmsDD.orientationIndex(px, py, qx, qy, maxx, miny)
    if (orientLR == 0) {
      // upward segment does not intersect pixel interior
      if (py < qy) return false
      // downward segment must intersect pixel interior
      return true
    }

    //--- check crossing Bottom side
    if (orientLL != orientLR) {
      return true
    }
    //--- check crossing Right side
    if (orientLR != orientUR) {
      return true
    }

    // segment does not intersect pixel
    return false
  }

  /**
   * Test whether a segment intersects
   * the closure of this hot pixel.
   * This is NOT the test used in the standard snap-rounding
   * algorithm, which uses the partially-open tolerance square
   * instead.
   * This method is provided for testing purposes only.
   *
   * @param p0 the start point of a line segment
   * @param p1 the end point of a line segment
   * @return `true` if the segment intersects the closure of the pixel's tolerance square
   */
  private fun intersectsPixelClosure(p0: Coordinate, p1: Coordinate): Boolean {
    val minx = hpx - TOLERANCE
    val maxx = hpx + TOLERANCE
    val miny = hpy - TOLERANCE
    val maxy = hpy + TOLERANCE

    @Suppress("UNCHECKED_CAST")
    val corner = arrayOfNulls<Coordinate>(4) as Array<Coordinate>
    corner[UPPER_RIGHT] = Coordinate(maxx, maxy)
    corner[UPPER_LEFT] = Coordinate(minx, maxy)
    corner[LOWER_LEFT] = Coordinate(minx, miny)
    corner[LOWER_RIGHT] = Coordinate(maxx, miny)

    val li: LineIntersector = RobustLineIntersector()
    li.computeIntersection(p0, p1, corner[0], corner[1])
    if (li.hasIntersection()) return true
    li.computeIntersection(p0, p1, corner[1], corner[2])
    if (li.hasIntersection()) return true
    li.computeIntersection(p0, p1, corner[2], corner[3])
    if (li.hasIntersection()) return true
    li.computeIntersection(p0, p1, corner[3], corner[0])
    if (li.hasIntersection()) return true

    return false
  }

  override fun toString(): String {
    return "HP(" + WKTWriter.format(originalPt) + ")"
  }

  companion object {
    // testing only
    //  public static int nTests = 0;

    private const val TOLERANCE = 0.5

    private const val UPPER_RIGHT = 0
    private const val UPPER_LEFT = 1
    private const val LOWER_LEFT = 2
    private const val LOWER_RIGHT = 3
  }
}
