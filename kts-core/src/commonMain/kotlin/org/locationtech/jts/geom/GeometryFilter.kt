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
 *  `GeometryCollection` classes support the concept of
 *  applying a `GeometryFilter` to the `Geometry`.
 *  The filter is applied to every element `Geometry`.
 *  A `GeometryFilter` can either record information about the `Geometry`
 *  or change the `Geometry` in some way.
 *  `GeometryFilter`
 *  is an example of the Gang-of-Four Visitor pattern.
 *
 */
interface GeometryFilter {

  /**
   *  Performs an operation with or on `geom`.
   *
   * @param  geom  a `Geometry` to which the filter is applied.
   */
  fun filter(geom: Geometry)
}
