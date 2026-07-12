/*
 * Copyright (c) 2026 mpMediaSoft.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.locationtech.jts.kmptest

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.io.WKTWriter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Representative end-to-end geometry operations, run on every target as a native-runtime safety
 * net over the algorithm code (area/length/centroid, predicates, overlay, buffer, hull, distance)
 * and the [WKTWriter] output path. Integer-coordinate results are exact; the trig-dependent buffer
 * area is checked within tolerance (absorbing any 1-ulp libm difference between platforms).
 */
class GeometryOpsParityTest {

  private val gf = GeometryFactory()

  private fun square(x0: Double, y0: Double, x1: Double, y1: Double): Geometry =
    gf.createPolygon(
      arrayOf(
        Coordinate(x0, y0),
        Coordinate(x1, y0),
        Coordinate(x1, y1),
        Coordinate(x0, y1),
        Coordinate(x0, y0),
      ),
    )

  @Test
  fun areaLengthCentroidType() {
    val unit = square(0.0, 0.0, 1.0, 1.0)
    assertEquals(1.0, unit.getArea())
    assertEquals(4.0, unit.getLength())
    assertEquals(0.5, unit.getCentroid().getX())
    assertEquals(0.5, unit.getCentroid().getY())
    assertEquals("Polygon", unit.getGeometryType())
    assertTrue(unit.isValid())
  }

  @Test
  fun predicates() {
    val unit = square(0.0, 0.0, 1.0, 1.0)
    val inside = gf.createPoint(Coordinate(0.5, 0.5))
    val outside = gf.createPoint(Coordinate(5.0, 5.0))
    assertTrue(unit.contains(inside))
    assertFalse(unit.contains(outside))
    assertTrue(unit.intersects(square(0.5, 0.5, 1.5, 1.5)))
    assertFalse(unit.intersects(square(2.0, 2.0, 3.0, 3.0)))
  }

  @Test
  fun distanceIsExact() {
    val a = gf.createPoint(Coordinate(0.0, 0.0))
    val b = gf.createPoint(Coordinate(3.0, 4.0))
    assertEquals(5.0, a.distance(b))
  }

  @Test
  fun overlayUnionAndIntersection() {
    // Two unit squares sharing the edge x = 1 -> combined area 2.
    val union = square(0.0, 0.0, 1.0, 1.0).union(square(1.0, 0.0, 2.0, 1.0))
    assertEquals(2.0, union.getArea())

    // [0,2]^2 intersect [1,3]^2 -> [1,2]^2, area 1.
    val inter = square(0.0, 0.0, 2.0, 2.0).intersection(square(1.0, 1.0, 3.0, 3.0))
    assertEquals(1.0, inter.getArea())
  }

  @Test
  fun convexHullOfSquarePlusInteriorPoint() {
    val pts = gf.createMultiPoint(
      arrayOf(
        Coordinate(0.0, 0.0),
        Coordinate(1.0, 0.0),
        Coordinate(1.0, 1.0),
        Coordinate(0.0, 1.0),
        Coordinate(0.5, 0.5), // interior -> not on hull
      ),
    )
    assertEquals(1.0, pts.convexHull().getArea())
  }

  @Test
  fun bufferAreaWithinTolerance() {
    // Default buffer of a point with 8 quadrant segments approximates a unit disc (area pi);
    // the 32-gon area is ~3.12145. Tolerance absorbs any platform trig (libm) rounding.
    val area = gf.createPoint(Coordinate(0.0, 0.0)).buffer(1.0).getArea()
    assertEquals(3.12145, area, 0.001)
  }

  @Test
  fun wktWriterOutput() {
    val w = WKTWriter()
    assertEquals("POINT (1 2)", w.write(gf.createPoint(Coordinate(1.0, 2.0))))
    assertEquals(
      "POLYGON ((0 0, 1 0, 1 1, 0 1, 0 0))",
      w.write(square(0.0, 0.0, 1.0, 1.0)),
    )
  }
}
