/*
 * Copyright (c) 2019 Martin Davis.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.locationtech.jts.operation.overlayng

import kotlin.jvm.JvmStatic

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryCollection
import org.locationtech.jts.geom.LinearRing
import org.locationtech.jts.geom.Polygon

/**
 * Computes a robust clipping envelope for a pair of polygonal geometries.
 * The envelope is computed to be large enough to include the full
 * length of all geometry line segments which intersect
 * a given target envelope.
 * This ensures that line segments which might intersect are
 * not perturbed when clipped using [RingClipper].
 *
 * @author Martin Davis
 */
class RobustClipEnvelopeComputer(private val targetEnv: Envelope) {

  private val clipEnv: Envelope = targetEnv.copy()

  fun getEnvelope(): Envelope {
    return clipEnv
  }

  fun add(g: Geometry?) {
    if (g == null || g.isEmpty())
      return

    if (g is Polygon)
      addPolygon(g)
    else if (g is GeometryCollection)
      addCollection(g)
  }

  private fun addCollection(gc: GeometryCollection) {
    for (i in 0 until gc.getNumGeometries()) {
      val g = gc.getGeometryN(i)
      add(g)
    }
  }

  private fun addPolygon(poly: Polygon) {
    val shell = poly.getExteriorRing()
    addPolygonRing(shell)

    for (i in 0 until poly.getNumInteriorRing()) {
      val hole = poly.getInteriorRingN(i)
      addPolygonRing(hole)
    }
  }

  /**
   * Adds a polygon ring to the graph. Empty rings are ignored.
   */
  private fun addPolygonRing(ring: LinearRing) {
    // don't add empty lines
    if (ring.isEmpty())
      return

    val seq = ring.getCoordinateSequence()
    for (i in 1 until seq.size()) {
      addSegment(seq.getCoordinate(i - 1), seq.getCoordinate(i))
    }
  }

  private fun addSegment(p1: Coordinate, p2: Coordinate) {
    if (intersectsSegment(targetEnv, p1, p2)) {
      clipEnv.expandToInclude(p1)
      clipEnv.expandToInclude(p2)
    }
  }

  companion object {
    @JvmStatic
    fun getEnvelope(a: Geometry?, b: Geometry?, targetEnv: Envelope): Envelope {
      val cec = RobustClipEnvelopeComputer(targetEnv)
      cec.add(a)
      cec.add(b)
      return cec.getEnvelope()
    }

    private fun intersectsSegment(env: Envelope, p1: Coordinate, p2: Coordinate): Boolean {
      /**
       * This is a crude test of whether segment intersects envelope.
       * It could be refined by checking exact intersection.
       * This could be based on the algorithm in the HotPixel.intersectsScaled method.
       */
      return env.intersects(p1, p2)
    }
  }
}
