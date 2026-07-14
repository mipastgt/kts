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
package org.locationtech.jts.util

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.CoordinateFilter

/**
 *  A [CoordinateFilter] that counts the total number of coordinates
 *  in a `Geometry`.
 *
 */
class CoordinateCountFilter : CoordinateFilter {
  private var n = 0

  /**
   *  Returns the result of the filtering.
   *
   * @return    the number of points found by this `CoordinateCountFilter`
   */
  fun getCount(): Int {
    return n
  }

  override fun filter(coord: Coordinate) {
    n++
  }
}
