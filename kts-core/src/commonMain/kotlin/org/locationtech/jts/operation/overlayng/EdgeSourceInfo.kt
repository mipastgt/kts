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

import org.locationtech.jts.geom.Dimension

/**
 * Records topological information about an
 * edge representing a piece of linework (lineString or polygon ring)
 * from a single source geometry.
 * This information is carried through the noding process
 * (which may result in many noded edges sharing the same information object).
 * It is then used to populate the topology info fields
 * in [Edge]s (possibly via merging).
 * That information is used to construct the topology graph [OverlayLabel]s.
 *
 * @author mdavis
 */
class EdgeSourceInfo {
  private var index: Int
  private var dim = -999
  private var hole = false
  private var depthDelta = 0

  constructor(index: Int, depthDelta: Int, isHole: Boolean) {
    this.index = index
    this.dim = Dimension.A
    this.depthDelta = depthDelta
    this.hole = isHole
  }

  constructor(index: Int) {
    this.index = index
    this.dim = Dimension.L
  }

  fun getIndex(): Int {
    return index
  }

  fun getDimension(): Int {
    return dim
  }

  fun getDepthDelta(): Int {
    return depthDelta
  }

  fun isHole(): Boolean {
    return hole
  }

  override fun toString(): String {
    return Edge.infoString(index, dim, hole, depthDelta)
  }
}
