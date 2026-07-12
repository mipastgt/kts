/*
 * Copyright (c) 2023 Martin Davis.
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
import kotlin.math.abs

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LineSegment
import org.locationtech.jts.geom.LinearRing
import org.locationtech.jts.geom.Polygon

class Rectangle {
  companion object {
    /**
     * Creates a rectangular [Polygon] from a base segment
     * defining the position and orientation of one side of the rectangle, and
     * three points defining the locations of the line segments
     * forming the opposite, left and right sides of the rectangle.
     * The base segment and side points must be presented so that the
     * rectangle has CW orientation.
     *
     * @param baseRightPt the right point of the base segment
     * @param baseLeftPt the left point of the base segment
     * @param oppositePt the point defining the opposite side
     * @param leftSidePt the point defining the left side
     * @param rightSidePt the point defining the right side
     * @param factory the geometry factory to use
     * @return the rectangular polygon
     */
    @JvmStatic
    fun createFromSidePts(
      baseRightPt: Coordinate, baseLeftPt: Coordinate,
      oppositePt: Coordinate,
      leftSidePt: Coordinate, rightSidePt: Coordinate,
      factory: GeometryFactory
    ): Polygon {
      //-- deltas for the base segment provide slope
      val dx = baseLeftPt.x - baseRightPt.x
      val dy = baseLeftPt.y - baseRightPt.y
      // Assert: dx and dy are not both zero

      val baseC = computeLineEquationC(dx, dy, baseRightPt)
      val oppC = computeLineEquationC(dx, dy, oppositePt)
      val leftC = computeLineEquationC(-dy, dx, leftSidePt)
      val rightC = computeLineEquationC(-dy, dx, rightSidePt)

      //-- compute lines along edges of rectangle
      val baseLine = createLineForStandardEquation(-dy, dx, baseC)
      val oppLine = createLineForStandardEquation(-dy, dx, oppC)
      val leftLine = createLineForStandardEquation(-dx, -dy, leftC)
      val rightLine = createLineForStandardEquation(-dx, -dy, rightC)

      /**
       * Corners of rectangle are the intersections of the
       * base and opposite, and left and right lines.
       * The rectangle is constructed with CW orientation.
       * The first side of the constructed rectangle contains the base segment.
       *
       * If a corner coincides with a input point
       * the exact value is used to avoid numerical inaccuracy.
       */
      val p0 = if (rightSidePt.equals2D(baseRightPt)) baseRightPt.copy()
        else baseLine.lineIntersection(rightLine)!!
      val p1 = if (leftSidePt.equals2D(baseLeftPt)) baseLeftPt.copy()
        else baseLine.lineIntersection(leftLine)!!
      val p2 = if (leftSidePt.equals2D(oppositePt)) oppositePt.copy()
        else oppLine.lineIntersection(leftLine)!!
      val p3 = if (rightSidePt.equals2D(oppositePt)) oppositePt.copy()
        else oppLine.lineIntersection(rightLine)!!

      val shell = factory.createLinearRing(
        arrayOf(p0, p1, p2, p3, p0.copy()))
      return factory.createPolygon(shell)
    }

    /**
     * Computes the constant C in the standard line equation Ax + By = C
     * from A and B and a point on the line.
     *
     * @param a the X coefficient
     * @param b the Y coefficient
     * @param p a point on the line
     * @return the constant C
     */
    private fun computeLineEquationC(a: Double, b: Double, p: Coordinate): Double {
      return a * p.y - b * p.x
    }

    private fun createLineForStandardEquation(a: Double, b: Double, c: Double): LineSegment {
      val p0: Coordinate
      val p1: Coordinate
      /*
      * Line equation is ax + by = c
      * Slope m = -a/b.
      * Y-intercept = c/b
      * X-intercept = c/a
      *
      * If slope is low, use constant X values; if high use Y values.
      * This handles lines that are vertical (b = 0, m = Inf )
      * and horizontal (a = 0, m = 0).
      */
      if (abs(b) > abs(a)) {
        //-- abs(m) < 1
        p0 = Coordinate(0.0, c / b)
        p1 = Coordinate(1.0, c / b - a / b)
      } else {
        //-- abs(m) >= 1
        p0 = Coordinate(c / a, 0.0)
        p1 = Coordinate(c / a - b / a, 1.0)
      }
      return LineSegment(p0, p1)
    }
  }
}
