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

import org.locationtech.jts.algorithm.Orientation
import org.locationtech.jts.algorithm.locate.IndexedPointInAreaLocator
import org.locationtech.jts.algorithm.locate.PointOnGeometryLocator
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.CoordinateList
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LinearRing
import org.locationtech.jts.geom.Location
import org.locationtech.jts.geom.Polygon
import org.locationtech.jts.geom.TopologyException

class OverlayEdgeRing(start: OverlayEdge, geometryFactory: GeometryFactory) {

  private val startEdge: OverlayEdge = start
  private var ring: LinearRing? = null
  private var hole = false
  private val ringPts: Array<Coordinate>
  private var locator: IndexedPointInAreaLocator? = null
  private var shell: OverlayEdgeRing? = null
  private val holes: MutableList<OverlayEdgeRing> = ArrayList() // a list of EdgeRings which are holes in this EdgeRing

  init {
    ringPts = computeRingPts(start)
    computeRing(ringPts, geometryFactory)
  }

  fun getRing(): LinearRing {
    return ring!!
  }

  private fun getEnvelope(): Envelope {
    return ring!!.getEnvelopeInternal()
  }

  /**
   * Tests whether this ring is a hole.
   * @return `true` if this ring is a hole
   */
  fun isHole(): Boolean {
    return hole
  }

  /**
   * Sets the containing shell ring of a ring that has been determined to be a hole.
   *
   * @param shell the shell ring
   */
  fun setShell(shell: OverlayEdgeRing?) {
    this.shell = shell
    if (shell != null) shell.addHole(this)
  }

  /**
   * Tests whether this ring has a shell assigned to it.
   *
   * @return true if the ring has a shell
   */
  fun hasShell(): Boolean {
    return shell != null
  }

  /**
   * Gets the shell for this ring.  The shell is the ring itself if it is not a hole, otherwise its parent shell.
   *
   * @return the shell for this ring
   */
  fun getShell(): OverlayEdgeRing? {
    if (isHole()) return shell
    return this
  }

  fun addHole(ring: OverlayEdgeRing) {
    holes.add(ring)
  }

  private fun computeRingPts(start: OverlayEdge): Array<Coordinate> {
    var edge = start
    val pts = CoordinateList()
    do {
      if (edge.getEdgeRing() === this)
        throw TopologyException("Edge visited twice during ring-building at " + edge.getCoordinate(), edge.getCoordinate())

      // only valid for polygonal output
      //Assert.isTrue(edge.getLabel().isBoundaryEither());

      edge.addCoordinates(pts)
      edge.setEdgeRing(this)
      if (edge.nextResult() == null)
        throw TopologyException("Found null edge in ring", edge.dest())

      edge = edge.nextResult()!!
    } while (edge !== start)
    pts.closeRing()
    return pts.toCoordinateArray()
  }

  private fun computeRing(ringPts: Array<Coordinate>, geometryFactory: GeometryFactory) {
    if (ring != null) return // don't compute more than once
    ring = geometryFactory.createLinearRing(ringPts)
    hole = Orientation.isCCW(ring!!.getCoordinates())
  }

  /**
   * Computes the list of coordinates which are contained in this ring.
   * The coordinates are computed once only and cached.
   *
   * @return an array of the [Coordinate]s in this ring
   */
  private fun getCoordinates(): Array<Coordinate> {
    return ringPts
  }

  /**
   * Finds the innermost enclosing shell OverlayEdgeRing
   * containing this OverlayEdgeRing, if any.
   *
   * @return containing EdgeRing or null if no containing EdgeRing is found
   */
  fun findEdgeRingContaining(erList: List<OverlayEdgeRing>): OverlayEdgeRing? {
    var minContainingRing: OverlayEdgeRing? = null

    for (edgeRing in erList) {
      if (edgeRing.contains(this)) {
        if (minContainingRing == null ||
          minContainingRing.getEnvelope().contains(edgeRing.getEnvelope())
        ) {
          minContainingRing = edgeRing
        }
      }
    }
    return minContainingRing
  }

  private fun getLocator(): PointOnGeometryLocator {
    if (locator == null) {
      locator = IndexedPointInAreaLocator(getRing())
    }
    return locator!!
  }

  fun locate(pt: Coordinate): Int {
    /**
     * Use an indexed point-in-polygon for performance
     */
    return getLocator().locate(pt)
  }

  /**
   * Tests if an edgeRing is properly contained in this ring.
   * Relies on property that edgeRings never overlap (although they may
   * touch at single vertices).
   *
   * @param ring ring to test
   * @return true if ring is properly contained
   */
  private fun contains(ring: OverlayEdgeRing): Boolean {
    // the test envelope must be properly contained
    // (guards against testing rings against themselves)
    val env = getEnvelope()
    val testEnv = ring.getEnvelope()
    if (!env.containsProperly(testEnv))
      return false
    return isPointInOrOut(ring)
  }

  private fun isPointInOrOut(ring: OverlayEdgeRing): Boolean {
    // in most cases only one or two points will be checked
    for (pt in ring.getCoordinates()) {
      val loc = locate(pt)
      if (loc == Location.INTERIOR) {
        return true
      }
      if (loc == Location.EXTERIOR) {
        return false
      }
      // pt is on BOUNDARY, so keep checking for a determining location
    }
    return false
  }

  fun getCoordinate(): Coordinate {
    return ringPts[0]
  }

  /**
   * Computes the [Polygon] formed by this ring and any contained holes.
   *
   * @return the [Polygon] formed by this ring and its holes.
   */
  fun toPolygon(factory: GeometryFactory): Polygon {
    val holeLR = Array(holes.size) { i -> holes[i].getRing() }
    val poly = factory.createPolygon(ring, holeLR)
    return poly
  }

  fun getEdge(): OverlayEdge {
    return startEdge
  }
}
