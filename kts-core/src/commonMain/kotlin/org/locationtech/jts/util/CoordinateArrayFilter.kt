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
 *  A {@link CoordinateFilter} that creates an array containing every
 *  coordinate in a {@link Geometry}.
 *
 * @version 1.7
 */
class CoordinateArrayFilter
/**
 *  Constructs a <code>CoordinateArrayFilter</code>.
 *
 * @param  size  the number of points that the <code>CoordinateArrayFilter</code>
 *      will collect
 */
(size: Int) : CoordinateFilter {
  private var pts: Array<Coordinate?> = arrayOfNulls(size)
  private var n = 0

  /**
   *  Returns the gathered <code>Coordinate</code>s.
   *
   * @return    the <code>Coordinate</code>s collected by this <code>CoordinateArrayFilter</code>
   */
  fun getCoordinates(): Array<Coordinate?> {
    return pts
  }

  override fun filter(coord: Coordinate) {
    pts[n++] = coord
  }
}
