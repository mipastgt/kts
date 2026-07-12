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

package org.locationtech.jts.simplify

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.LineSegment

/**
 * A [LineSegment] which is tagged with its location in a parent [Geometry].
 * Used to index the segments in a geometry and recover the segment locations
 * from the index.
 */
internal class TaggedLineSegment : LineSegment {
  private val parent: Geometry?
  private val index: Int

  constructor(p0: Coordinate, p1: Coordinate, parent: Geometry?, index: Int) : super(p0, p1) {
    this.parent = parent
    this.index = index
  }

  constructor(p0: Coordinate, p1: Coordinate) : this(p0, p1, null, -1)

  fun getParent(): Geometry? = parent
  fun getIndex(): Int = index
}
