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
package org.locationtech.jts.coverage

import kotlin.jvm.JvmStatic

import org.locationtech.jts.util.PriorityQueue

import org.locationtech.jts.algorithm.Area
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.CoordinateArrays
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.index.VertexSequencePackedRtree
import org.locationtech.jts.index.strtree.STRtree
import org.locationtech.jts.simplify.LinkedLine

/**
 * Computes a Topology-Preserving Visvalingam-Whyatt simplification
 * of a set of input lines.
 *
 * @author mdavis
 */
class TPVWSimplifier(private val edges: Array<Edge>) {

  private var cornerArea: CornerArea? = null
  private var removableSizeFactor = 1.0

  fun setRemovableRingSizeFactor(removableSizeFactor: Double) {
    this.removableSizeFactor = removableSizeFactor
  }

  fun setCornerArea(cornerArea: CornerArea) {
    this.cornerArea = cornerArea
  }

  private fun simplify() {
    val edgeIndex = EdgeIndex()
    add(edges, edgeIndex)

    for (i in edges.indices) {
      val edge = edges[i]
      edge.simplify(cornerArea!!, edgeIndex)
    }
  }

  private fun add(edges: Array<Edge>, edgeIndex: EdgeIndex) {
    for (edge in edges) {
      //-- don't include removed edges in index
      edge.updateRemoved(removableSizeFactor)
      if (!edge.isRemoved()) {
        //-- avoid fluffing up removed edges
        edge.init()
        edgeIndex.add(edge)
      }
    }
  }

  class Edge(private val pts: Array<Coordinate>, private val distanceTolerance: Double,
             private val isFreeRing: Boolean, private val isRemovable: Boolean) {
    private lateinit var linkedLine: LinkedLine
    private val nPts: Int = pts.size
    private var vertexIndex: VertexSequencePackedRtree? = null
    private val envelope: Envelope = CoordinateArrays.envelope(pts)
    private var removed = false

    internal fun updateRemoved(removableSizeFactor: Double) {
      if (!isRemovable)
        return
      val areaTolerance = distanceTolerance * distanceTolerance
      removed = CoordinateArrays.isRing(pts)
          && Area.ofRing(pts) < removableSizeFactor * areaTolerance
    }

    internal fun init() {
      linkedLine = LinkedLine(pts)
    }

    fun getTolerance(): Double {
      return distanceTolerance
    }

    fun isRemoved(): Boolean {
      return removed
    }

    private fun getCoordinate(index: Int): Coordinate {
      return pts[index]
    }

    fun getCoordinates(): Array<Coordinate> {
      if (removed) {
        return arrayOf()
      }
      return linkedLine.getCoordinates()
    }

    fun getEnvelope(): Envelope {
      return envelope
    }

    fun size(): Int {
      return linkedLine.size()
    }

    internal fun simplify(cornerArea: CornerArea, edgeIndex: EdgeIndex) {
      if (removed) {
        return
      }
      //-- don't simplify
      if (distanceTolerance <= 0.0)
        return

      val areaTolerance = distanceTolerance * distanceTolerance
      val minEdgeSize = if (linkedLine.isRing()) MIN_RING_SIZE else MIN_EDGE_SIZE

      val cornerQueue = createQueue(areaTolerance, cornerArea)
      while (!cornerQueue.isEmpty()
          && size() > minEdgeSize) {
        val corner = cornerQueue.poll()!!
        //-- a corner may no longer be valid due to removal of adjacent corners
        if (corner.isRemoved())
          continue
        //-- done when all small corners are removed
        if (corner.getArea() > areaTolerance)
          break
        if (isRemovable(corner, edgeIndex)) {
          removeCorner(corner, areaTolerance, cornerArea, cornerQueue)
        }
      }
    }

    private fun createQueue(areaTolerance: Double, cornerArea: CornerArea): PriorityQueue<Corner> {
      val cornerQueue: PriorityQueue<Corner> = PriorityQueue()
      val minIndex = if (linkedLine.isRing() && isFreeRing) 0 else 1
      val maxIndex = nPts - 1
      for (i in minIndex until maxIndex) {
        addCorner(i, areaTolerance, cornerArea, cornerQueue)
      }
      return cornerQueue
    }

    private fun addCorner(i: Int, areaTolerance: Double, cornerArea: CornerArea, cornerQueue: PriorityQueue<Corner>) {
      //-- add if this vertex can be a corner
      if (isFreeRing || (i != 0 && i != nPts - 1)) {
        val area = area(i, cornerArea)
        if (area <= areaTolerance) {
          val corner = Corner(linkedLine, i, area)
          cornerQueue.add(corner)
        }
      }
    }

    private fun area(index: Int, cornerArea: CornerArea): Double {
      val pp = linkedLine.prevCoordinate(index)
      val p = linkedLine.getCoordinate(index)
      val pn = linkedLine.nextCoordinate(index)
      return cornerArea.area(pp, p, pn)
    }

    private fun isRemovable(corner: Corner, edgeIndex: EdgeIndex): Boolean {
      val cornerEnv = corner.envelope()
      //-- check nearby lines for violating intersections
      for (edge in edgeIndex.query(cornerEnv)) {
        if (hasIntersectingVertex(corner, cornerEnv, edge))
          return false
        //-- check if corner base equals line (2-pts)
        if (edge !== this && edge.size() == 2) {
          val linePts = edge.linkedLine.getCoordinates()
          if (corner.isBaseline(linePts[0], linePts[1]))
            return false
        }
      }
      return true
    }

    /**
     * Tests if any vertices in a line intersect the corner triangle.
     */
    private fun hasIntersectingVertex(corner: Corner, cornerEnv: Envelope,
                                      edge: Edge): Boolean {
      val result = edge.query(cornerEnv)
      for (index in result) {
        val v = edge.getCoordinate(index)
        // ok if corner touches another line - should only happen at endpoints
        if (corner.isVertex(v))
          continue

        //--- does corner triangle contain vertex?
        if (corner.intersects(v))
          return true
      }
      return false
    }

    private fun initIndex() {
      vertexIndex = VertexSequencePackedRtree(pts)
      //-- remove ring duplicate final vertex
      if (CoordinateArrays.isRing(pts)) {
        vertexIndex!!.remove(pts.size - 1)
      }
    }

    private fun query(cornerEnv: Envelope): IntArray {
      if (vertexIndex == null) {
        initIndex()
      }
      return vertexIndex!!.query(cornerEnv)
    }

    /**
     * Removes a corner by removing the apex vertex from the ring.
     */
    private fun removeCorner(corner: Corner, areaTolerance: Double, cornerArea: CornerArea, cornerQueue: PriorityQueue<Corner>) {
      val index = corner.getIndex()
      val prev = linkedLine.prev(index)
      val next = linkedLine.next(index)
      linkedLine.remove(index)
      vertexIndex!!.remove(index)

      //-- potentially add the new corners created
      addCorner(prev, areaTolerance, cornerArea, cornerQueue)
      addCorner(next, areaTolerance, cornerArea, cornerQueue)
    }

    override fun toString(): String {
      return linkedLine.toString()
    }

    companion object {
      private const val MIN_EDGE_SIZE = 2
      private const val MIN_RING_SIZE = 4
    }
  }

  internal class EdgeIndex {
    val index = STRtree()

    fun add(edge: Edge) {
      index.insert(edge.getEnvelope(), edge)
    }

    fun query(queryEnv: Envelope): MutableList<Edge> {
      @Suppress("UNCHECKED_CAST")
      return index.query(queryEnv) as MutableList<Edge>
    }
  }

  companion object {
    /**
     * Simplifies a set of edges.
     *
     * @param edges the edges to simplify
     * @param cornerArea the corner area computer
     * @param removableSizeFactor the size factor for removable rings
     */
    @JvmStatic
    fun simplify(edges: Array<Edge>,
                 cornerArea: CornerArea,
                 removableSizeFactor: Double) {
      val simp = TPVWSimplifier(edges)
      simp.setCornerArea(cornerArea)
      simp.setRemovableRingSizeFactor(removableSizeFactor)
      simp.simplify()
    }
  }
}
