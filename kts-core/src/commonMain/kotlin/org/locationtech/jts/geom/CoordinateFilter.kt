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
package org.locationtech.jts.geom

/**
 *  An interface for classes which use the values of the coordinates in a [Geometry].
 * Coordinate filters can be used to implement centroid and
 * envelope computation, and many other functions.
 * 
 * `CoordinateFilter` is
 * an example of the Gang-of-Four Visitor pattern.
 * 
 * **Note**: it is not recommended to use these filters to mutate the coordinates.
 * There is no guarantee that the coordinate is the actual object stored in the source geometry.
 * In particular, modified values may not be preserved if the source Geometry uses a non-default [CoordinateSequence].
 * If in-place mutation is required, use [CoordinateSequenceFilter].
 *
 * @see Geometry#apply(CoordinateFilter)
 * @see CoordinateSequenceFilter
 *
 */
interface CoordinateFilter {

  /**
   * Performs an operation with the provided `coord`.
   * Note that there is no guarantee that the input coordinate
   * is the actual object stored in the source geometry,
   * so changes to the coordinate object may not be persistent.
   *
   * @param  coord  a `Coordinate` to which the filter is applied.
   */
  fun filter(coord: Coordinate)
}
