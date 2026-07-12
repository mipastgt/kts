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

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.IntersectionMatrix
import org.locationtech.jts.geom.Location

/**
 * @version 1.7
 */
open class Node(coord: Coordinate?, edges: EdgeEndStar?) : GraphComponent() {

  protected var coord: Coordinate? = coord // only non-null if this node is precise

  @JvmField
  protected var edges: EdgeEndStar? = edges

  init {
    label = Label(0, Location.NONE)
  }

  override fun getCoordinate(): Coordinate? = coord
  open fun getEdges(): EdgeEndStar? = edges

  /**
   * Tests whether any incident edge is flagged as
   * being in the result.
   * This test can be used to determine if the node is in the result,
   * since if any incident edge is in the result, the node must be in the result as well.
   *
   * @return `true` if any incident edge in the in the result
   */
  open fun isIncidentEdgeInResult(): Boolean {
    val it = getEdges()!!.getEdges().iterator()
    while (it.hasNext()) {
      val de = it.next() as DirectedEdge
      if (de.getEdge().isInResult())
        return true
    }
    return false
  }

  override fun isIsolated(): Boolean {
    return label!!.getGeometryCount() == 1
  }

  /**
   * Basic nodes do not compute IMs
   */
  override fun computeIM(im: IntersectionMatrix) {}

  /**
   * Add the edge to the list of edges at this node.
   *
   * @param e EdgeEnd
   */
  open fun add(e: EdgeEnd) {
    // Assert: start pt of e is equal to node point
    edges!!.insert(e)
    e.setNode(this)
  }

  open fun mergeLabel(n: Node) {
    mergeLabel(n.label!!)
  }

  /**
   * To merge labels for two nodes,
   * the merged location for each LabelElement is computed.
   * The location for the corresponding node LabelElement is set to the result,
   * as long as the location is non-null.
   *
   * @param label2 Label to merge
   */
  open fun mergeLabel(label2: Label) {
    for (i in 0..1) {
      val loc = computeMergedLocation(label2, i)
      val thisLoc = label!!.getLocation(i)
      if (thisLoc == Location.NONE) label!!.setLocation(i, loc)
    }
  }

  open fun setLabel(argIndex: Int, onLocation: Int) {
    if (label == null) {
      label = Label(argIndex, onLocation)
    } else
      label!!.setLocation(argIndex, onLocation)
  }

  /**
   * Updates the label of a node to BOUNDARY,
   * obeying the mod-2 boundaryDetermination rule.
   * @param argIndex location index
   */
  open fun setLabelBoundary(argIndex: Int) {
    if (label == null) return

    // determine the current location for the point (if any)
    var loc = Location.NONE
    if (label != null)
      loc = label!!.getLocation(argIndex)
    // flip the loc
    val newLoc: Int = when (loc) {
      Location.BOUNDARY -> Location.INTERIOR
      Location.INTERIOR -> Location.BOUNDARY
      else -> Location.BOUNDARY
    }
    label!!.setLocation(argIndex, newLoc)
  }

  /**
   * The location for a given eltIndex for a node will be one
   * of { null, INTERIOR, BOUNDARY }.
   * A node may be on both the boundary and the interior of a geometry;
   * in this case, the rule is that the node is considered to be in the boundary.
   * The merged location is the maximum of the two input values.
   */
  private fun computeMergedLocation(label2: Label, eltIndex: Int): Int {
    var loc = Location.NONE
    loc = label!!.getLocation(eltIndex)
    if (!label2.isNull(eltIndex)) {
      val nLoc = label2.getLocation(eltIndex)
      if (loc != Location.BOUNDARY) loc = nLoc
    }
    return loc
  }

  override fun toString(): String {
    return "Node(" + coord!!.x + ", " + coord!!.y + ")"
  }
}
