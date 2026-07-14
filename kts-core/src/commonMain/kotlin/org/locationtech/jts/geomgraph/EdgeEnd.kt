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
package org.locationtech.jts.geomgraph

import kotlin.jvm.JvmField
import kotlin.math.atan2

import org.locationtech.jts.algorithm.BoundaryNodeRule
import org.locationtech.jts.algorithm.Orientation
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Quadrant
import org.locationtech.jts.util.Assert

/**
 * Models the end of an edge incident on a node.
 * EdgeEnds have a direction
 * determined by the direction of the ray from the initial
 * point to the next point.
 * EdgeEnds are comparable under the ordering
 * "a has a greater angle with the x-axis than b".
 * This ordering is used to sort EdgeEnds around a node.
 */
open class EdgeEnd : Comparable<Any?> {

  @JvmField
  protected var edge: Edge  // the parent edge of this edge end

  @JvmField
  protected var label: Label? = null

  private var node: Node? = null              // the node this edge end originates at
  private lateinit var p0: Coordinate         // points of initial line segment
  private lateinit var p1: Coordinate
  private var dx = 0.0                         // the direction vector for this edge from its starting point
  private var dy = 0.0
  private var quadrant = 0

  protected constructor(edge: Edge) {
    this.edge = edge
  }

  constructor(edge: Edge, p0: Coordinate, p1: Coordinate) : this(edge, p0, p1, null)

  constructor(edge: Edge, p0: Coordinate, p1: Coordinate, label: Label?) : this(edge) {
    init(p0, p1)
    this.label = label
  }

  protected fun init(p0: Coordinate, p1: Coordinate) {
    this.p0 = p0
    this.p1 = p1
    dx = p1.x - p0.x
    dy = p1.y - p0.y
    quadrant = Quadrant.quadrant(dx, dy)
    Assert.isTrue(!(dx == 0.0 && dy == 0.0), "EdgeEnd with identical endpoints found")
  }

  open fun getEdge(): Edge = edge
  open fun getLabel(): Label? = label
  open fun getCoordinate(): Coordinate = p0
  open fun getDirectedCoordinate(): Coordinate = p1
  open fun getQuadrant(): Int = quadrant
  open fun getDx(): Double = dx
  open fun getDy(): Double = dy

  open fun setNode(node: Node?) { this.node = node }
  open fun getNode(): Node? = node

  override fun compareTo(obj: Any?): Int {
    val e = obj as EdgeEnd
    return compareDirection(e)
  }

  /**
   * Implements the total order relation:
   *
   * a has a greater angle with the positive x-axis than b
   *
   * Using the obvious algorithm of simply computing the angle is not robust,
   * since the angle calculation is obviously susceptible to roundoff.
   * A robust algorithm is:
   * - first compare the quadrant.  If the quadrants
   * are different, it it trivial to determine which vector is "greater".
   * - if the vectors lie in the same quadrant, the computeOrientation function
   * can be used to decide the relative orientation of the vectors.
   *
   * @param e EdgeEnd
   * @return direction comparison
   */
  open fun compareDirection(e: EdgeEnd): Int {
    if (dx == e.dx && dy == e.dy)
      return 0
    // if the rays are in different quadrants, determining the ordering is trivial
    if (quadrant > e.quadrant) return 1
    if (quadrant < e.quadrant) return -1
    // vectors are in the same quadrant - check relative orientation of direction vectors
    // this is > e if it is CCW of e
    return Orientation.index(e.p0, e.p1, p1)
  }

  open fun computeLabel(boundaryNodeRule: BoundaryNodeRule) {
    // subclasses should override this if they are using labels
  }

  override fun toString(): String {
    val angle = atan2(dy, dx)
    val className = this::class.simpleName ?: ""
    val lastDotPos = className.lastIndexOf('.')
    val name = className.substring(lastDotPos + 1)
    return "  $name: $p0 - $p1 $quadrant:$angle   $label"
  }
}
