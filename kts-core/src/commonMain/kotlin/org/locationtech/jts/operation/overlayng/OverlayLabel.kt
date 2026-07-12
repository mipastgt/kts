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

import org.locationtech.jts.geom.Location
import org.locationtech.jts.geom.Position

/**
 * A structure recording the topological situation
 * for an edge in a topology graph
 * used during overlay processing.
 * A label contains the topological [Location]s for
 * one or two input geometries to an overlay operation.
 * An input geometry may be either a Line or an Area.
 *
 * @author Martin Davis
 */
class OverlayLabel {

  private var aDim = DIM_NOT_PART
  private var aIsHole = false
  private var aLocLeft = LOC_UNKNOWN
  private var aLocRight = LOC_UNKNOWN
  private var aLocLine = LOC_UNKNOWN

  private var bDim = DIM_NOT_PART
  private var bIsHole = false
  private var bLocLeft = LOC_UNKNOWN
  private var bLocRight = LOC_UNKNOWN
  private var bLocLine = LOC_UNKNOWN

  /**
   * Creates a label for an Area edge.
   */
  constructor(index: Int, locLeft: Int, locRight: Int, isHole: Boolean) {
    initBoundary(index, locLeft, locRight, isHole)
  }

  /**
   * Creates a label for a Line edge.
   */
  constructor(index: Int) {
    initLine(index)
  }

  /**
   * Creates an uninitialized label.
   */
  constructor()

  /**
   * Creates a label which is a copy of another label.
   */
  constructor(lbl: OverlayLabel) {
    this.aLocLeft = lbl.aLocLeft
    this.aLocRight = lbl.aLocRight
    this.aLocLine = lbl.aLocLine
    this.aDim = lbl.aDim
    this.aIsHole = lbl.aIsHole

    this.bLocLeft = lbl.bLocLeft
    this.bLocRight = lbl.bLocRight
    this.bLocLine = lbl.bLocLine
    this.bDim = lbl.bDim
    this.bIsHole = lbl.bIsHole
  }

  /**
   * Gets the effective dimension of the given input geometry.
   */
  fun dimension(index: Int): Int {
    if (index == 0) return aDim
    return bDim
  }

  /**
   * Initializes the label for an input geometry which is an Area boundary.
   */
  fun initBoundary(index: Int, locLeft: Int, locRight: Int, isHole: Boolean) {
    if (index == 0) {
      aDim = DIM_BOUNDARY
      aIsHole = isHole
      aLocLeft = locLeft
      aLocRight = locRight
      aLocLine = Location.INTERIOR
    } else {
      bDim = DIM_BOUNDARY
      bIsHole = isHole
      bLocLeft = locLeft
      bLocRight = locRight
      bLocLine = Location.INTERIOR
    }
  }

  /**
   * Initializes the label for an edge which is the collapse of
   * part of the boundary of an Area input geometry.
   */
  fun initCollapse(index: Int, isHole: Boolean) {
    if (index == 0) {
      aDim = DIM_COLLAPSE
      aIsHole = isHole
    } else {
      bDim = DIM_COLLAPSE
      bIsHole = isHole
    }
  }

  /**
   * Initializes the label for an input geometry which is a Line.
   */
  fun initLine(index: Int) {
    if (index == 0) {
      aDim = DIM_LINE
      aLocLine = LOC_UNKNOWN
    } else {
      bDim = DIM_LINE
      bLocLine = LOC_UNKNOWN
    }
  }

  /**
   * Initializes the label for an edge which is not part of an input geometry.
   */
  fun initNotPart(index: Int) {
    // this assumes locations are initialized to UNKNOWN
    if (index == 0) {
      aDim = DIM_NOT_PART
    } else {
      bDim = DIM_NOT_PART
    }
  }

  /**
   * Sets the line location.
   */
  fun setLocationLine(index: Int, loc: Int) {
    if (index == 0) {
      aLocLine = loc
    } else {
      bLocLine = loc
    }
  }

  /**
   * Sets the location of all postions for a given input.
   */
  fun setLocationAll(index: Int, loc: Int) {
    if (index == 0) {
      aLocLine = loc
      aLocLeft = loc
      aLocRight = loc
    } else {
      bLocLine = loc
      bLocLeft = loc
      bLocRight = loc
    }
  }

  /**
   * Sets the location for a collapsed edge (the Line position)
   * for an input geometry, depending on the ring role recorded in the label.
   */
  fun setLocationCollapse(index: Int) {
    val loc = if (isHole(index)) Location.INTERIOR else Location.EXTERIOR
    if (index == 0) {
      aLocLine = loc
    } else {
      bLocLine = loc
    }
  }

  /**
   * Tests whether at least one of the sources is a Line.
   */
  fun isLine(): Boolean {
    return aDim == DIM_LINE || bDim == DIM_LINE
  }

  /**
   * Tests whether a source is a Line.
   */
  fun isLine(index: Int): Boolean {
    if (index == 0) {
      return aDim == DIM_LINE
    }
    return bDim == DIM_LINE
  }

  /**
   * Tests whether an edge is linear (a Line or a Collapse) in an input geometry.
   */
  fun isLinear(index: Int): Boolean {
    if (index == 0) {
      return aDim == DIM_LINE || aDim == DIM_COLLAPSE
    }
    return bDim == DIM_LINE || bDim == DIM_COLLAPSE
  }

  /**
   * Tests whether a the source of a label is known.
   */
  fun isKnown(index: Int): Boolean {
    if (index == 0) {
      return aDim != DIM_UNKNOWN
    }
    return bDim != DIM_UNKNOWN
  }

  /**
   * Tests whether a label is for an edge which is not part
   * of a given input geometry.
   */
  fun isNotPart(index: Int): Boolean {
    if (index == 0) {
      return aDim == DIM_NOT_PART
    }
    return bDim == DIM_NOT_PART
  }

  /**
   * Tests if a label is for an edge which is in the boundary of either source geometry.
   */
  fun isBoundaryEither(): Boolean {
    return aDim == DIM_BOUNDARY || bDim == DIM_BOUNDARY
  }

  /**
   * Tests if a label is for an edge which is in the boundary of both source geometries.
   */
  fun isBoundaryBoth(): Boolean {
    return aDim == DIM_BOUNDARY && bDim == DIM_BOUNDARY
  }

  /**
   * Tests if the label is a collapsed edge of one area
   * and is a (non-collapsed) boundary edge of the other area.
   */
  fun isBoundaryCollapse(): Boolean {
    if (isLine()) return false
    return !isBoundaryBoth()
  }

  /**
   * Tests if a label is for an edge where two
   * area touch along their boundary.
   */
  fun isBoundaryTouch(): Boolean {
    return isBoundaryBoth() &&
        getLocation(0, Position.RIGHT, true) != getLocation(1, Position.RIGHT, true)
  }

  /**
   * Tests if a label is for an edge which is in the boundary of a source geometry.
   */
  fun isBoundary(index: Int): Boolean {
    if (index == 0) {
      return aDim == DIM_BOUNDARY
    }
    return bDim == DIM_BOUNDARY
  }

  /**
   * Tests whether a label is for an edge which is a boundary of one geometry
   * and not part of the other.
   */
  fun isBoundarySingleton(): Boolean {
    if (aDim == DIM_BOUNDARY && bDim == DIM_NOT_PART) return true
    if (bDim == DIM_BOUNDARY && aDim == DIM_NOT_PART) return true
    return false
  }

  /**
   * Tests if the line location for a source is unknown.
   */
  fun isLineLocationUnknown(index: Int): Boolean {
    return if (index == 0) {
      aLocLine == LOC_UNKNOWN
    } else {
      bLocLine == LOC_UNKNOWN
    }
  }

  /**
   * Tests if a line edge is inside a source geometry
   * (i.e. it has location [Location.INTERIOR]).
   */
  fun isLineInArea(index: Int): Boolean {
    if (index == 0) {
      return aLocLine == Location.INTERIOR
    }
    return bLocLine == Location.INTERIOR
  }

  /**
   * Tests if the ring role of an edge is a hole.
   */
  fun isHole(index: Int): Boolean {
    return if (index == 0) {
      aIsHole
    } else {
      bIsHole
    }
  }

  /**
   * Tests if an edge is a Collapse for a source geometry.
   */
  fun isCollapse(index: Int): Boolean {
    return dimension(index) == DIM_COLLAPSE
  }

  /**
   * Tests if a label is a Collapse has location [Location.INTERIOR],
   * to at least one source geometry.
   */
  fun isInteriorCollapse(): Boolean {
    if (aDim == DIM_COLLAPSE && aLocLine == Location.INTERIOR) return true
    if (bDim == DIM_COLLAPSE && bLocLine == Location.INTERIOR) return true
    return false
  }

  /**
   * Tests if a label is a Collapse
   * and NotPart with location [Location.INTERIOR] for the other geometry.
   */
  fun isCollapseAndNotPartInterior(): Boolean {
    if (aDim == DIM_COLLAPSE && bDim == DIM_NOT_PART && bLocLine == Location.INTERIOR) return true
    if (bDim == DIM_COLLAPSE && aDim == DIM_NOT_PART && aLocLine == Location.INTERIOR) return true
    return false
  }

  /**
   * Gets the line location for a source geometry.
   */
  fun getLineLocation(index: Int): Int {
    return if (index == 0) {
      aLocLine
    } else {
      bLocLine
    }
  }

  /**
   * Tests if a line is in the interior of a source geometry.
   */
  fun isLineInterior(index: Int): Boolean {
    if (index == 0) {
      return aLocLine == Location.INTERIOR
    }
    return bLocLine == Location.INTERIOR
  }

  /**
   * Gets the location for a [Position] of an edge of a source
   * for an edge with given orientation.
   */
  fun getLocation(index: Int, position: Int, isForward: Boolean): Int {
    if (index == 0) {
      when (position) {
        Position.LEFT -> return if (isForward) aLocLeft else aLocRight
        Position.RIGHT -> return if (isForward) aLocRight else aLocLeft
        Position.ON -> return aLocLine
      }
    }
    // index == 1
    when (position) {
      Position.LEFT -> return if (isForward) bLocLeft else bLocRight
      Position.RIGHT -> return if (isForward) bLocRight else bLocLeft
      Position.ON -> return bLocLine
    }
    return LOC_UNKNOWN
  }

  /**
   * Gets the location for this label for either
   * a Boundary or a Line edge.
   */
  fun getLocationBoundaryOrLine(index: Int, position: Int, isForward: Boolean): Int {
    if (isBoundary(index)) {
      return getLocation(index, position, isForward)
    }
    return getLineLocation(index)
  }

  /**
   * Gets the linear location for the given source.
   */
  fun getLocation(index: Int): Int {
    if (index == 0) {
      return aLocLine
    }
    return bLocLine
  }

  /**
   * Tests whether this label has side position information
   * for a source geometry.
   */
  fun hasSides(index: Int): Boolean {
    if (index == 0) {
      return aLocLeft != LOC_UNKNOWN || aLocRight != LOC_UNKNOWN
    }
    return bLocLeft != LOC_UNKNOWN || bLocRight != LOC_UNKNOWN
  }

  /**
   * Creates a copy of this label.
   */
  fun copy(): OverlayLabel {
    return OverlayLabel(this)
  }

  override fun toString(): String {
    return toString(true)
  }

  fun toString(isForward: Boolean): String {
    val buf = StringBuilder()
    buf.append("A:")
    buf.append(locationString(0, isForward))
    buf.append("/B:")
    buf.append(locationString(1, isForward))
    return buf.toString()
  }

  private fun locationString(index: Int, isForward: Boolean): String {
    val buf = StringBuilder()
    if (isBoundary(index)) {
      buf.append(Location.toLocationSymbol(getLocation(index, Position.LEFT, isForward)))
      buf.append(Location.toLocationSymbol(getLocation(index, Position.RIGHT, isForward)))
    } else {
      // is a linear edge
      buf.append(Location.toLocationSymbol(if (index == 0) aLocLine else bLocLine))
    }
    if (isKnown(index))
      buf.append(dimensionSymbol(if (index == 0) aDim else bDim))
    if (isCollapse(index)) {
      buf.append(ringRoleSymbol(if (index == 0) aIsHole else bIsHole))
    }
    return buf.toString()
  }

  companion object {
    private const val SYM_UNKNOWN = '#'
    private const val SYM_BOUNDARY = 'B'
    private const val SYM_COLLAPSE = 'C'
    private const val SYM_LINE = 'L'

    /**
     * The dimension of an input geometry which is not known
     */
    const val DIM_UNKNOWN = -1

    /**
     * The dimension of an edge which is not part of a specified input geometry.
     */
    const val DIM_NOT_PART = DIM_UNKNOWN

    /**
     * The dimension of an edge which is a line.
     */
    const val DIM_LINE = 1

    /**
     * The dimension for an edge which is part of an input Area geometry boundary.
     */
    const val DIM_BOUNDARY = 2

    /**
     * The dimension for an edge which is a collapsed part of an input Area geometry boundary.
     */
    const val DIM_COLLAPSE = 3

    /**
     * Indicates that the location is currently unknown
     */
    const val LOC_UNKNOWN = Location.NONE

    /**
     * Gets a symbol for the a ring role (Shell or Hole).
     */
    @JvmStatic
    fun ringRoleSymbol(isHole: Boolean): Char {
      return if (isHole) 'h' else 's'
    }

    /**
     * Gets the symbol for the dimension code of an edge.
     */
    @JvmStatic
    fun dimensionSymbol(dim: Int): Char {
      when (dim) {
        DIM_LINE -> return SYM_LINE
        DIM_COLLAPSE -> return SYM_COLLAPSE
        DIM_BOUNDARY -> return SYM_BOUNDARY
      }
      return SYM_UNKNOWN
    }
  }
}
