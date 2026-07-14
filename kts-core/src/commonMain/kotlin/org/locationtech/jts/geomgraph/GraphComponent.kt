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
import org.locationtech.jts.util.Assert

/**
 * A GraphComponent is the parent class for the objects'
 * that form a graph.  Each GraphComponent can carry a
 * Label.
 */
abstract class GraphComponent {

  @JvmField
  protected var label: Label? = null

  /**
   * isInResult indicates if this component has already been included in the result
   */
  private var inResult = false
  private var covered = false
  private var coveredSet = false
  private var visited = false

  constructor()

  constructor(label: Label?) {
    this.label = label
  }

  open fun getLabel(): Label? = label
  open fun setLabel(label: Label?) { this.label = label }
  open fun setInResult(isInResult: Boolean) { this.inResult = isInResult }
  open fun isInResult(): Boolean = inResult
  open fun setCovered(isCovered: Boolean) {
    this.covered = isCovered
    this.coveredSet = true
  }
  open fun isCovered(): Boolean = covered
  open fun isCoveredSet(): Boolean = coveredSet
  open fun isVisited(): Boolean = visited
  open fun setVisited(isVisited: Boolean) { this.visited = isVisited }

  /**
   * @return a coordinate in this component (or null, if there are none)
   */
  abstract fun getCoordinate(): Coordinate?

  /**
   * Compute the contribution to an IM for this component.
   *
   * @param im Intersection matrix
   */
  protected abstract fun computeIM(im: IntersectionMatrix)

  /**
   * An isolated component is one that does not intersect or touch any other
   * component.  This is the case if the label has valid locations for
   * only a single Geometry.
   *
   * @return true if this component is isolated
   */
  abstract fun isIsolated(): Boolean

  /**
   * Update the IM with the contribution for this component.
   * A component only contributes if it has a labelling for both parent geometries
   * @param im Intersection matrix
   */
  open fun updateIM(im: IntersectionMatrix) {
    Assert.isTrue(label!!.getGeometryCount() >= 2, "found partial label")
    computeIM(im)
  }
}
