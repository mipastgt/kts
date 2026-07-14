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
 * `Geometry` classes support the concept of applying
 * a `GeometryComponentFilter`
 * filter to a geometry.
 * The filter is applied to every component of a geometry,
 * as well as to the geometry itself.
 * (For instance, in a [Polygon],
 * all the [LinearRing] components for the shell and holes are visited,
 * as well as the polygon itself.
 * In order to process only atomic components,
 * the [filter] method code must
 * explicitly handle only [LineString]s, [LinearRing]s and [Point]s.
 * 
 * A `GeometryComponentFilter` filter can either
 * record information about the `Geometry`
 * or change the `Geometry` in some way.
 * 
 * `GeometryComponentFilter`
 * is an example of the Gang-of-Four Visitor pattern.
 *
 */
interface GeometryComponentFilter {

  /**
   * Performs an operation with or on a geometry component.
   *
   * @param geom a component of the geometry to which the filter is applied.
   */
  fun filter(geom: Geometry)
}
