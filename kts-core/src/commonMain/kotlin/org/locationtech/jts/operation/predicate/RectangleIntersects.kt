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

package org.locationtech.jts.operation.predicate

import kotlin.jvm.JvmStatic

import org.locationtech.jts.algorithm.RectangleLineIntersector
import org.locationtech.jts.algorithm.locate.SimplePointInAreaLocator
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.CoordinateSequence
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.Polygon
import org.locationtech.jts.geom.util.LinearComponentExtracter
import org.locationtech.jts.geom.util.ShortCircuitedGeometryVisitor

/**
 * Implementation of the `intersects` spatial predicate
 * optimized for the case where one [Geometry] is a rectangle.
 * This class works for all
 * input geometries, including [org.locationtech.jts.geom.GeometryCollection]s.
 *
 *
 * As a further optimization,
 * this class can be used in batch style
 * to test many geometries
 * against a single rectangle.
 *
 */
class RectangleIntersects
/**
 * Create a new intersects computer for a rectangle.
 *
 * @param rectangle
 * a rectangular Polygon
 */
  (private val rectangle: Polygon) {

  private val rectEnv: Envelope = rectangle.getEnvelopeInternal()

  /**
   * Tests whether the given Geometry intersects
   * the query rectangle.
   *
   * @param geom the Geometry to test (may be of any type)
   * @return true if the geometry intersects the query rectangle
   */
  fun intersects(geom: Geometry): Boolean {
    if (!rectEnv.intersects(geom.getEnvelopeInternal()))
      return false

    /**
     * Test if rectangle envelope intersects any component envelope.
     * This handles Point components as well
     */
    val visitor = EnvelopeIntersectsVisitor(rectEnv)
    visitor.applyTo(geom)
    if (visitor.intersects())
      return true

    /**
     * Test if any rectangle vertex is contained in the target geometry
     */
    val ecpVisitor = GeometryContainsPointVisitor(rectangle)
    ecpVisitor.applyTo(geom)
    if (ecpVisitor.containsPoint())
      return true

    /**
     * Test if any target geometry line segment intersects the rectangle
     */
    val riVisitor = RectangleIntersectsSegmentVisitor(rectangle)
    riVisitor.applyTo(geom)
    if (riVisitor.intersects())
      return true

    return false
  }

  companion object {
    /**
     * Tests whether a rectangle intersects a given geometry.
     *
     * @param rectangle
     * a rectangular Polygon
     * @param b
     * a Geometry of any type
     * @return true if the geometries intersect
     */
    @JvmStatic
    fun intersects(rectangle: Polygon, b: Geometry): Boolean {
      val rp = RectangleIntersects(rectangle)
      return rp.intersects(b)
    }
  }
}

/**
 * Tests whether it can be concluded that a rectangle intersects a geometry,
 * based on the relationship of the envelope(s) of the geometry.
 *
 * @author Martin Davis
 */
internal class EnvelopeIntersectsVisitor(private val rectEnv: Envelope) : ShortCircuitedGeometryVisitor() {

  private var intersectsFlag = false

  /**
   * Reports whether it can be concluded that an intersection occurs,
   * or whether further testing is required.
   *
   * @return true if an intersection must occur
   * or false if no conclusion about intersection can be made
   */
  fun intersects(): Boolean {
    return intersectsFlag
  }

  override fun visit(element: Geometry) {
    val elementEnv = element.getEnvelopeInternal()

    // disjoint => no intersection
    if (!rectEnv.intersects(elementEnv)) {
      return
    }
    // rectangle contains target env => must intersect
    if (rectEnv.contains(elementEnv)) {
      intersectsFlag = true
      return
    }
    /*
     * Since the envelopes intersect and the test element is connected, if the
     * test envelope is completely bisected by an edge of the rectangle the
     * element and the rectangle must touch (This is basically an application of
     * the Jordan Curve Theorem). The alternative situation is that the test
     * envelope is "on a corner" of the rectangle envelope, i.e. is not
     * completely bisected. In this case it is not possible to make a conclusion
     * about the presence of an intersection.
     */
    if (elementEnv.getMinX() >= rectEnv.getMinX()
      && elementEnv.getMaxX() <= rectEnv.getMaxX()
    ) {
      intersectsFlag = true
      return
    }
    if (elementEnv.getMinY() >= rectEnv.getMinY()
      && elementEnv.getMaxY() <= rectEnv.getMaxY()
    ) {
      intersectsFlag = true
      return
    }
  }

  override fun isDone(): Boolean {
    return intersectsFlag
  }
}

/**
 * A visitor which tests whether it can be
 * concluded that a geometry contains a vertex of
 * a query geometry.
 *
 * @author Martin Davis
 */
internal class GeometryContainsPointVisitor(rectangle: Polygon) : ShortCircuitedGeometryVisitor() {

  private val rectSeq: CoordinateSequence = rectangle.getExteriorRing().getCoordinateSequence()
  private val rectEnv: Envelope = rectangle.getEnvelopeInternal()
  private var containsPointFlag = false

  /**
   * Reports whether it can be concluded that a corner point of the rectangle is
   * contained in the geometry, or whether further testing is required.
   *
   * @return true if a corner point is contained
   * or false if no conclusion about intersection can be made
   */
  fun containsPoint(): Boolean {
    return containsPointFlag
  }

  override fun visit(geom: Geometry) {
    // if test geometry is not polygonal this check is not needed
    if (geom !is Polygon)
      return

    // skip if envelopes do not intersect
    val elementEnv = geom.getEnvelopeInternal()
    if (!rectEnv.intersects(elementEnv))
      return

    // test each corner of rectangle for inclusion
    val rectPt = Coordinate()
    for (i in 0 until 4) {
      rectSeq.getCoordinate(i, rectPt)
      if (!elementEnv.contains(rectPt))
        continue
      // check rect point in poly (rect is known not to touch polygon at this
      // point)
      if (SimplePointInAreaLocator.containsPointInPolygon(rectPt, geom)) {
        containsPointFlag = true
        return
      }
    }
  }

  override fun isDone(): Boolean {
    return containsPointFlag
  }
}

/**
 * A visitor to test for intersection between the query
 * rectangle and the line segments of the geometry.
 *
 * @author Martin Davis
 */
internal class RectangleIntersectsSegmentVisitor(rectangle: Polygon) : ShortCircuitedGeometryVisitor() {

  private val rectEnv: Envelope = rectangle.getEnvelopeInternal()
  private val rectIntersector: RectangleLineIntersector = RectangleLineIntersector(rectEnv)

  private var hasIntersection = false

  /**
   * Reports whether any segment intersection exists.
   *
   * @return true if a segment intersection exists
   * or false if no segment intersection exists
   */
  fun intersects(): Boolean {
    return hasIntersection
  }

  override fun visit(geom: Geometry) {
    /**
     * It may be the case that the rectangle and the
     * envelope of the geometry component are disjoint,
     * so it is worth checking this simple condition.
     */
    val elementEnv = geom.getEnvelopeInternal()
    if (!rectEnv.intersects(elementEnv))
      return

    // check segment intersections
    // get all lines from geometry component
    // (there may be more than one if it's a multi-ring polygon)
    val lines = LinearComponentExtracter.getLines(geom)
    checkIntersectionWithLineStrings(lines)
  }

  private fun checkIntersectionWithLineStrings(lines: List<*>) {
    for (obj in lines) {
      val testLine = obj as LineString
      checkIntersectionWithSegments(testLine)
      if (hasIntersection)
        return
    }
  }

  private fun checkIntersectionWithSegments(testLine: LineString) {
    val seq1 = testLine.getCoordinateSequence()
    val p0 = seq1.createCoordinate()
    val p1 = seq1.createCoordinate()
    for (j in 1 until seq1.size()) {
      seq1.getCoordinate(j - 1, p0)
      seq1.getCoordinate(j, p1)

      if (rectIntersector.intersects(p0, p1)) {
        hasIntersection = true
        return
      }
    }
  }

  override fun isDone(): Boolean {
    return hasIntersection
  }
}
