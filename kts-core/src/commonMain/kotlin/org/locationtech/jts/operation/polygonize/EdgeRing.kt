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
package org.locationtech.jts.operation.polygonize

import kotlin.jvm.JvmStatic

import org.locationtech.jts.algorithm.Orientation
import org.locationtech.jts.algorithm.locate.IndexedPointInAreaLocator
import org.locationtech.jts.algorithm.locate.PointOnGeometryLocator
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.CoordinateList
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.LinearRing
import org.locationtech.jts.geom.Location
import org.locationtech.jts.geom.Polygon
import org.locationtech.jts.geom.impl.CoordinateArraySequence
import org.locationtech.jts.io.WKTWriter
import org.locationtech.jts.planargraph.DirectedEdge
import org.locationtech.jts.util.Assert

/**
 * Represents a ring of [PolygonizeDirectedEdge]s which form
 * a ring of a polygon.  The ring may be either an outer shell or a hole.
 *
 * @version 1.7
 */
class EdgeRing(private val factory: GeometryFactory) {

  private val deList: MutableList<PolygonizeDirectedEdge> = ArrayList()

  // cache the following data for efficiency
  private var ring: LinearRing? = null
  private var locator: IndexedPointInAreaLocator? = null

  private var ringPts: Array<Coordinate>? = null
  private var holes: MutableList<LinearRing>? = null
  private var shell: EdgeRing? = null
  private var hole = false
  private var valid = false
  private var processed = false
  private var includedSet = false
  private var included = false

  fun build(startDE: PolygonizeDirectedEdge) {
    var de: PolygonizeDirectedEdge? = startDE
    do {
      val d = de!!
      add(d)
      d.setRing(this)
      de = d.getNext()
      Assert.isTrue(de != null, "found null DE in ring")
      Assert.isTrue(de === startDE || !de!!.isInRing(), "found DE already in ring")
    } while (de !== startDE)
  }

  /**
   * Adds a [DirectedEdge] which is known to form part of this ring.
   * @param de the [DirectedEdge] to add.
   */
  private fun add(de: DirectedEdge) {
    deList.add(de as PolygonizeDirectedEdge)
  }

  fun getEdges(): MutableList<PolygonizeDirectedEdge> {
    return deList
  }

  /**
   * Tests whether this ring is a hole.
   * @return `true` if this ring is a hole
   */
  fun isHole(): Boolean {
    return hole
  }

  /**
   * Computes whether this ring is a hole.
   */
  fun computeHole() {
    val ring = getRing()!!
    hole = Orientation.isCCW(ring.getCoordinates())
  }

  /**
   * Adds a hole to the polygon formed by this ring.
   * @param hole the [LinearRing] forming the hole.
   */
  fun addHole(hole: LinearRing) {
    if (holes == null)
      holes = ArrayList()
    holes!!.add(hole)
  }

  /**
   * Adds a hole to the polygon formed by this ring.
   * @param holeER the [LinearRing] forming the hole.
   */
  fun addHole(holeER: EdgeRing) {
    holeER.setShell(this)
    val hole = holeER.getRing()!!
    if (holes == null)
      holes = ArrayList()
    holes!!.add(hole)
  }

  /**
   * Computes the [Polygon] formed by this ring and any contained holes.
   *
   * @return the [Polygon] formed by this ring and its holes.
   */
  fun getPolygon(): Polygon {
    var holeLR: Array<LinearRing>? = null
    val holes = this.holes
    if (holes != null) {
      holeLR = Array(holes.size) { i -> holes[i] }
    }
    val poly = factory.createPolygon(ring, holeLR)
    return poly
  }

  /**
   * Tests if the [LinearRing] ring formed by this edge ring is topologically valid.
   *
   * @return true if the ring is valid
   */
  fun isValid(): Boolean {
    return valid
  }

  /**
   * Computes the validity of the ring.
   * Must be called prior to calling [isValid].
   */
  fun computeValid() {
    getCoordinates()
    if (ringPts!!.size <= 3) {
      valid = false
      return
    }
    valid = getRing()!!.isValid()
  }

  fun isIncludedSet(): Boolean {
    return includedSet
  }

  fun isIncluded(): Boolean {
    return included
  }

  fun setIncluded(isIncluded: Boolean) {
    this.included = isIncluded
    this.includedSet = true
  }

  private fun getLocator(): PointOnGeometryLocator {
    if (locator == null) {
      locator = IndexedPointInAreaLocator(getRing()!!)
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
   *
   * @param ring ring to test
   * @return true if ring is properly contained
   */
  private fun contains(ring: EdgeRing): Boolean {
    // the test envelope must be properly contained
    // (guards against testing rings against themselves)
    val env = getEnvelope()
    val testEnv = ring.getEnvelope()
    if (!env.containsProperly(testEnv))
      return false
    return isPointInOrOut(ring)
  }

  private fun isPointInOrOut(ring: EdgeRing): Boolean {
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

  /**
   * Computes the list of coordinates which are contained in this ring.
   * The coordinates are computed once only and cached.
   *
   * @return an array of the [Coordinate]s in this ring
   */
  private fun getCoordinates(): Array<Coordinate> {
    if (ringPts == null) {
      val coordList = CoordinateList()
      for (de in deList) {
        val edge = de.getEdge() as PolygonizeEdge
        addEdge(edge.getLine().getCoordinates(), de.getEdgeDirection(), coordList)
      }
      ringPts = coordList.toCoordinateArray()
    }
    return ringPts!!
  }

  /**
   * Gets the coordinates for this ring as a [LineString].
   *
   * @return a [LineString] containing the coordinates in this ring
   */
  fun getLineString(): LineString {
    getCoordinates()
    return factory.createLineString(ringPts!!)
  }

  /**
   * Returns this ring as a [LinearRing], or null if an Exception occurs while
   * creating it (such as a topology problem).
   */
  fun getRing(): LinearRing? {
    if (ring != null) return ring
    getCoordinates()
    try {
      ring = factory.createLinearRing(ringPts!!)
    } catch (ex: Exception) {
    }
    return ring
  }

  private fun getEnvelope(): Envelope {
    return getRing()!!.getEnvelopeInternal()
  }

  /**
   * Sets the containing shell ring of a ring that has been determined to be a hole.
   *
   * @param shell the shell ring
   */
  fun setShell(shell: EdgeRing?) {
    this.shell = shell
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
  fun getShell(): EdgeRing? {
    if (isHole()) return shell
    return this
  }

  /**
   * Tests whether this ring is an outer hole.
   *
   * @return true if the ring is an outer hole.
   */
  fun isOuterHole(): Boolean {
    if (!hole) return false
    return !hasShell()
  }

  /**
   * Tests whether this ring is an outer shell.
   *
   * @return true if the ring is an outer shell.
   */
  fun isOuterShell(): Boolean {
    return getOuterHole() != null
  }

  /**
   * Gets the outer hole of a shell, if it has one.
   *
   * @return the outer hole edge ring, or null
   */
  fun getOuterHole(): EdgeRing? {
    /*
     * Only shells can have outer holes
     */
    if (isHole()) return null
    /*
     * A shell is an outer shell if any edge is also in an outer hole.
     */
    for (i in deList.indices) {
      val de = deList[i]
      val adjRing = (de.getSym() as PolygonizeDirectedEdge).getRing()
      if (adjRing!!.isOuterHole()) return adjRing
    }
    return null
  }

  /**
   * Updates the included status for currently non-included shells
   * based on whether they are adjacent to an included shell.
   */
  fun updateIncluded() {
    if (isHole()) return
    for (i in deList.indices) {
      val de = deList[i]
      val adjShell = (de.getSym() as PolygonizeDirectedEdge).getRing()!!.getShell()

      if (adjShell != null && adjShell.isIncludedSet()) {
        // adjacent ring has been processed, so set included to inverse of adjacent included
        setIncluded(!adjShell.isIncluded())
        return
      }
    }
  }

  /**
   * Gets a string representation of this object.
   *
   * @return a string representing the object
   */
  override fun toString(): String {
    return WKTWriter.toLineString(CoordinateArraySequence(getCoordinates()))
  }

  /**
   * @return whether the ring has been processed
   */
  fun isProcessed(): Boolean {
    return processed
  }

  /**
   * @param isProcessed whether the ring has been processed
   */
  fun setProcessed(isProcessed: Boolean) {
    this.processed = isProcessed
  }

  /**
   * Compares EdgeRings based on their envelope,
   * using the standard lexicographic ordering.
   *
   * @author mbdavis
   */
  class EnvelopeComparator : Comparator<EdgeRing> {
    override fun compare(r0: EdgeRing, r1: EdgeRing): Int {
      return r0.getRing()!!.getEnvelope().compareTo(r1.getRing()!!.getEnvelope())
    }
  }

  /**
   * Compares EdgeRings based on the area of their envelopes.
   *
   * @author mdavis
   */
  class EnvelopeAreaComparator : Comparator<EdgeRing> {
    override fun compare(r0: EdgeRing, r1: EdgeRing): Int {
      return r0.getRing()!!.getEnvelope().getArea().compareTo(
        r1.getRing()!!.getEnvelope().getArea()
      )
    }
  }

  companion object {
    /**
     * Find the innermost enclosing shell EdgeRing containing the argument EdgeRing, if any.
     *
     * @return containing EdgeRing, or null if no containing EdgeRing is found
     */
    @JvmStatic
    fun findEdgeRingContaining(testEr: EdgeRing, erList: List<EdgeRing>): EdgeRing? {
      var minContainingRing: EdgeRing? = null
      for (edgeRing in erList) {
        if (edgeRing.contains(testEr)) {
          if (minContainingRing == null ||
            minContainingRing.getEnvelope().contains(edgeRing.getEnvelope())
          ) {
            minContainingRing = edgeRing
          }
        }
      }
      return minContainingRing
    }

    /**
     * Traverses a ring of DirectedEdges, accumulating them into a list.
     *
     * @param startDE the DirectedEdge to start traversing at
     * @return a List of DirectedEdges that form a ring
     */
    @JvmStatic
    fun findDirEdgesInRing(startDE: PolygonizeDirectedEdge): MutableList<PolygonizeDirectedEdge> {
      var de: PolygonizeDirectedEdge? = startDE
      val edges = ArrayList<PolygonizeDirectedEdge>()
      do {
        val d = de!!
        edges.add(d)
        de = d.getNext()
        Assert.isTrue(de != null, "found null DE in ring")
        Assert.isTrue(de === startDE || !de!!.isInRing(), "found DE already in ring")
      } while (de !== startDE)
      return edges
    }

    private fun addEdge(coords: Array<Coordinate>, isForward: Boolean, coordList: CoordinateList) {
      if (isForward) {
        for (i in coords.indices) {
          coordList.add(coords[i], false)
        }
      } else {
        for (i in coords.indices.reversed()) {
          coordList.add(coords[i], false)
        }
      }
    }
  }
}
