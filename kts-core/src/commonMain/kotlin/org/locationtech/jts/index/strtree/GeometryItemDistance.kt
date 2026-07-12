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
package org.locationtech.jts.index.strtree

import org.locationtech.jts.geom.Geometry

/**
 * An [ItemDistance] function for
 * items which are [Geometry]s,
 * using the [Geometry.distance] method.
 *
 * @author Martin Davis
 */
class GeometryItemDistance : ItemDistance {
  /**
   * Computes the distance between two [Geometry] items,
   * using the [Geometry.distance] method.
   *
   * @param item1 an item which is a Geometry
   * @param item2 an item which is a Geometry
   * @return the distance between the geometries
   * @throws ClassCastException if either item is not a Geometry
   */
  override fun distance(item1: ItemBoundable, item2: ItemBoundable): Double {
    if (item1 === item2) return Double.MAX_VALUE
    val g1 = item1.getItem() as Geometry
    val g2 = item2.getItem() as Geometry
    return g1.distance(g2)
  }
}
