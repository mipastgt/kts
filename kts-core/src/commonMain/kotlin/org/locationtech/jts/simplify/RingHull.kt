/*
 * Copyright (c) 2022 Martin Davis.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.locationtech.jts.simplify

import org.locationtech.jts.util.PriorityQueue

import org.locationtech.jts.algorithm.Orientation
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.CoordinateArrays
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.LinearRing
import org.locationtech.jts.geom.Polygon
import org.locationtech.jts.geom.Triangle
import org.locationtech.jts.index.VertexSequencePackedRtree

/**
 * Computes the outer or inner hull of a ring.
 *
 * @author Martin Davis
 */
internal class RingHull(ring: LinearRing, isOuter: Boolean) {

  private val inputRing: LinearRing = ring
  private var targetVertexNum = -1
  private var targetAreaDelta = -1.0

  /**
   * The ring vertices are oriented so that
   * for corners which are to be kept
   * the vertices forming the corner are in CW orientation.
   */
  private lateinit var vertexRing: LinkedRing
  private var areaDelta = 0.0

  /**
   * Indexing vertices improves corner intersection testing performance.
   */
  private lateinit var vertexIndex: VertexSequencePackedRtree

  private lateinit var cornerQueue: PriorityQueue<Corner>

  init {
    init(ring.getCoordinates(), isOuter)
  }

  fun setMinVertexNum(minVertexNum: Int) {
    targetVertexNum = minVertexNum
  }

  fun setMaxAreaDelta(maxAreaDelta: Double) {
    targetAreaDelta = maxAreaDelta
  }

  fun getEnvelope(): Envelope {
    return inputRing.getEnvelopeInternal()
  }

  fun getVertexIndex(): VertexSequencePackedRtree {
    return vertexIndex
  }

  fun getHull(hullIndex: RingHullIndex?): LinearRing {
    compute(hullIndex)
    val hullPts = vertexRing.getCoordinates()
    return inputRing.getFactory().createLinearRing(hullPts)
  }

  private fun init(ringArg: Array<Coordinate>, isOuter: Boolean) {
    var ring = ringArg
    /**
     * Ensure ring is oriented according to outer/inner:
     * - outer, CW
     * - inner: CCW
     */
    val orientCW = isOuter
    if (orientCW == Orientation.isCCW(ring)) {
      ring = ring.copyOf()
      CoordinateArrays.reverse(ring)
    }

    vertexRing = LinkedRing(ring)
    vertexIndex = VertexSequencePackedRtree(ring)
    //-- remove duplicate final vertex
    vertexIndex.remove(ring.size - 1)

    cornerQueue = PriorityQueue()
    for (i in 0 until vertexRing.size()) {
      addCorner(i, cornerQueue)
    }
  }

  private fun addCorner(i: Int, cornerQueue: PriorityQueue<Corner>) {
    //-- convex corners are left untouched
    if (isConvex(vertexRing, i))
      return
    //-- corner is concave or flat - both can be removed
    val corner = Corner(i,
        vertexRing.prev(i),
        vertexRing.next(i),
        area(vertexRing, i))
    cornerQueue.add(corner)
  }

  fun compute(hullIndex: RingHullIndex?) {
    while (!cornerQueue.isEmpty()
        && vertexRing.size() > 3) {
      val corner = cornerQueue.poll()!!
      //-- a corner may no longer be valid due to removal of adjacent corners
      if (corner.isRemoved(vertexRing))
        continue
      if (isAtTarget(corner))
        return
      /**
       * Corner is concave or flat - remove it if possible.
       */
      if (isRemovable(corner, hullIndex)) {
        removeCorner(corner, cornerQueue)
      }
    }
  }

  private fun isAtTarget(corner: Corner): Boolean {
    if (targetVertexNum >= 0) {
      return vertexRing.size() < targetVertexNum
    }
    if (targetAreaDelta >= 0) {
      //-- include candidate corder to avoid overshooting target
      return areaDelta + corner.getArea() > targetAreaDelta
    }
    //-- no target set
    return true
  }

  /**
   * Removes a corner by removing the apex vertex from the ring.
   */
  private fun removeCorner(corner: Corner, cornerQueue: PriorityQueue<Corner>) {
    val index = corner.getIndex()
    val prev = vertexRing.prev(index)
    val next = vertexRing.next(index)
    vertexRing.remove(index)
    vertexIndex.remove(index)
    areaDelta += corner.getArea()

    //-- potentially add the new corners created
    addCorner(prev, cornerQueue)
    addCorner(next, cornerQueue)
  }

  private fun isRemovable(corner: Corner, hullIndex: RingHullIndex?): Boolean {
    val cornerEnv = corner.envelope(vertexRing)
    if (hasIntersectingVertex(corner, cornerEnv, this))
      return false
    //-- no other rings to check
    if (hullIndex == null)
      return true
    //-- check other rings for intersections
    for (hull in hullIndex.query(cornerEnv)) {
      //-- this hull was already checked above
      if (hull === this)
        continue
      if (hasIntersectingVertex(corner, cornerEnv, hull))
        return false
    }
    return true
  }

  /**
   * Tests if any vertices in a hull intersect the corner triangle.
   */
  private fun hasIntersectingVertex(corner: Corner, cornerEnv: Envelope,
                                    hull: RingHull): Boolean {
    val result = hull.query(cornerEnv)
    for (i in result.indices) {
      val index = result[i]
      //-- skip vertices of corner
      if (hull === this && corner.isVertex(index))
        continue

      val v = hull.getCoordinate(index)
      //--- does corner triangle contain vertex?
      if (corner.intersects(v, vertexRing))
        return true
    }
    return false
  }

  private fun getCoordinate(index: Int): Coordinate {
    return vertexRing.getCoordinate(index)
  }

  private fun query(cornerEnv: Envelope): IntArray {
    return vertexIndex.query(cornerEnv)
  }

  fun queryHull(queryEnv: Envelope, pts: MutableList<Coordinate>) {
    val result = vertexIndex.query(queryEnv)

    for (i in result.indices) {
      val index = result[i]
      //-- skip if already removed
      if (!vertexRing.hasCoordinate(index))
        continue
      val v = vertexRing.getCoordinate(index)
      pts.add(v)
    }
  }

  fun toGeometry(): Polygon {
    val fact = GeometryFactory()
    val coords = vertexRing.getCoordinates()
    return fact.createPolygon(fact.createLinearRing(coords))
  }

  private class Corner(i: Int, private val prev: Int, private val next: Int, private val area: Double) : Comparable<Corner> {
    private val index: Int = i

    fun isVertex(index: Int): Boolean {
      return index == this.index
          || index == prev
          || index == next
    }

    fun getIndex(): Int {
      return index
    }

    fun getArea(): Double {
      return area
    }

    /**
     * Orders corners by increasing area
     */
    override fun compareTo(o: Corner): Int {
      return area.compareTo(o.area)
    }

    fun envelope(ring: LinkedRing): Envelope {
      val pp = ring.getCoordinate(prev)
      val p = ring.getCoordinate(index)
      val pn = ring.getCoordinate(next)
      val env = Envelope(pp, pn)
      env.expandToInclude(p)
      return env
    }

    fun intersects(v: Coordinate, ring: LinkedRing): Boolean {
      val pp = ring.getCoordinate(prev)
      val p = ring.getCoordinate(index)
      val pn = ring.getCoordinate(next)
      return Triangle.intersects(pp, p, pn, v)
    }

    fun isRemoved(ring: LinkedRing): Boolean {
      return ring.prev(index) != prev || ring.next(index) != next
    }

    fun toLineString(ring: LinkedRing): LineString {
      val pp = ring.getCoordinate(prev)
      val p = ring.getCoordinate(index)
      val pn = ring.getCoordinate(next)
      return GeometryFactory().createLineString(
          arrayOf(safeCoord(pp), safeCoord(p), safeCoord(pn)))
    }

    companion object {
      private fun safeCoord(p: Coordinate?): Coordinate {
        if (p == null) return Coordinate(Double.NaN, Double.NaN)
        return p
      }
    }
  }

  companion object {
    fun isConvex(vertexRing: LinkedRing, index: Int): Boolean {
      val pp = vertexRing.prevCoordinate(index)
      val p = vertexRing.getCoordinate(index)
      val pn = vertexRing.nextCoordinate(index)
      return Orientation.CLOCKWISE == Orientation.index(pp, p, pn)
    }

    fun area(vertexRing: LinkedRing, index: Int): Double {
      val pp = vertexRing.prevCoordinate(index)
      val p = vertexRing.getCoordinate(index)
      val pn = vertexRing.nextCoordinate(index)
      return Triangle.area(pp, p, pn)
    }
  }
}
