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
package org.locationtech.jts.planargraph

import kotlin.jvm.JvmStatic
import kotlin.math.atan2

import org.locationtech.jts.algorithm.Orientation
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Quadrant

/**
 * Represents a directed edge in a [PlanarGraph]. A DirectedEdge may or
 * may not have a reference to a parent [Edge] (some applications of
 * planar graphs may not require explicit Edge objects to be created). Usually
 * a client using a `PlanarGraph` will subclass `DirectedEdge`
 * to add its own application-specific data and methods.
 *
 * @version 1.7
 *
 * @param from the node this DirectedEdge leaves
 * @param to the node this DirectedEdge goes to
 * @param directionPt specifies this DirectedEdge's direction vector
 *   (determined by the vector from the `from` node to `directionPt`)
 * @param edgeDirection whether this DirectedEdge's direction is the same as or
 *   opposite to that of the parent Edge (if any)
 */
open class DirectedEdge(
  private val from: Node,
  private val to: Node,
  directionPt: Coordinate,
  private val edgeDirection: Boolean
) : GraphComponent(), Comparable<Any?> {

  private var parentEdge: Edge? = null
  private var sym: DirectedEdge? = null // optional
  private val p0: Coordinate = from.getCoordinate()!!
  private val p1: Coordinate = directionPt
  private val quadrant: Int
  private val angle: Double

  init {
    val dx = p1.x - p0.x
    val dy = p1.y - p0.y
    quadrant = Quadrant.quadrant(dx, dy)
    angle = atan2(dy, dx)
    //Assert.isTrue(! (dx == 0 && dy == 0), "EdgeEnd with identical endpoints found");
  }

  /**
   * Returns this DirectedEdge's parent Edge, or null if it has none.
   */
  open fun getEdge(): Edge? {
    return parentEdge
  }

  /**
   * Associates this DirectedEdge with an Edge (possibly null, indicating no associated
   * Edge).
   */
  open fun setEdge(parentEdge: Edge?) {
    this.parentEdge = parentEdge
  }

  /**
   * Returns 0, 1, 2, or 3, indicating the quadrant in which this DirectedEdge's
   * orientation lies.
   */
  open fun getQuadrant(): Int {
    return quadrant
  }

  /**
   * Returns a point to which an imaginary line is drawn from the from-node to
   * specify this DirectedEdge's orientation.
   */
  open fun getDirectionPt(): Coordinate {
    return p1
  }

  /**
   * Returns whether the direction of the parent Edge (if any) is the same as that
   * of this Directed Edge.
   */
  open fun getEdgeDirection(): Boolean {
    return edgeDirection
  }

  /**
   * Returns the node from which this DirectedEdge leaves.
   */
  open fun getFromNode(): Node {
    return from
  }

  /**
   * Returns the node to which this DirectedEdge goes.
   */
  open fun getToNode(): Node {
    return to
  }

  /**
   * Returns the coordinate of the from-node.
   */
  open fun getCoordinate(): Coordinate? {
    return from.getCoordinate()
  }

  /**
   * Returns the angle that the start of this DirectedEdge makes with the
   * positive x-axis, in radians.
   */
  open fun getAngle(): Double {
    return angle
  }

  /**
   * Returns the symmetric DirectedEdge -- the other DirectedEdge associated with
   * this DirectedEdge's parent Edge.
   */
  open fun getSym(): DirectedEdge? {
    return sym
  }

  /**
   * Sets this DirectedEdge's symmetric DirectedEdge, which runs in the opposite
   * direction.
   */
  open fun setSym(sym: DirectedEdge?) {
    this.sym = sym
  }

  /**
   * Removes this directed edge from its containing graph.
   */
  internal fun remove() {
    this.sym = null
    this.parentEdge = null
  }

  /**
   * Tests whether this directed edge has been removed from its containing graph
   *
   * @return `true` if this directed edge is removed
   */
  override fun isRemoved(): Boolean {
    return parentEdge == null
  }

  /**
   * Returns 1 if this DirectedEdge has a greater angle with the
   * positive x-axis than b", 0 if the DirectedEdges are collinear, and -1 otherwise.
   *
   * Using the obvious algorithm of simply computing the angle is not robust,
   * since the angle calculation is susceptible to roundoff. A robust algorithm
   * is:
   *
   *  * first compare the quadrants. If the quadrants are different, it it
   * trivial to determine which vector is "greater".
   *  * if the vectors lie in the same quadrant, the robust
   * [Orientation.index] function can be used to decide the relative orientation
   * of the vectors.
   */
  override fun compareTo(obj: Any?): Int {
    val de = obj as DirectedEdge
    return compareDirection(de)
  }

  /**
   * Returns 1 if this DirectedEdge has a greater angle with the
   * positive x-axis than b", 0 if the DirectedEdges are collinear, and -1 otherwise.
   *
   * Using the obvious algorithm of simply computing the angle is not robust,
   * since the angle calculation is susceptible to roundoff. A robust algorithm
   * is:
   *
   *  * first compare the quadrants. If the quadrants are different, it it
   * trivial to determine which vector is "greater".
   *  * if the vectors lie in the same quadrant, the robust
   * [Orientation.index] function can be used to decide the relative orientation
   * of the vectors.
   */
  open fun compareDirection(e: DirectedEdge): Int {
    // if the rays are in different quadrants, determining the ordering is trivial
    if (quadrant > e.quadrant) return 1
    if (quadrant < e.quadrant) return -1
    // vectors are in the same quadrant - check relative orientation of direction vectors
    // this is > e if it is CCW of e
    return Orientation.index(e.p0, e.p1, p1)
  }

  companion object {
    /**
     * Returns a List containing the parent Edge (possibly null) for each of the given
     * DirectedEdges.
     */
    @JvmStatic
    fun toEdges(dirEdges: Collection<*>): MutableList<Any?> {
      val edges = ArrayList<Any?>()
      val i = dirEdges.iterator()
      while (i.hasNext()) {
        edges.add((i.next() as DirectedEdge).parentEdge)
      }
      return edges
    }
  }
}
