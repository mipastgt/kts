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
package org.locationtech.jts.geom.prep

import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryCollection

/**
 * An interface for classes which prepare [Geometry]s
 * in order to optimize the performance
 * of repeated calls to specific geometric operations.
 *
 * Subclasses are intended to be thread-safe, to allow `PreparedGeometry`
 * to be used in a multi-threaded context.
 *
 * @author Martin Davis
 *
 */
interface PreparedGeometry {

  /**
   * Gets the original [Geometry] which has been prepared.
   *
   * @return the base geometry
   */
  fun getGeometry(): Geometry

  /**
   * Tests whether the base [Geometry] contains a given geometry.
   *
   * @param geom the Geometry to test
   * @return true if this Geometry contains the given Geometry
   *
   * @see Geometry.contains
   */
  fun contains(geom: Geometry): Boolean

  /**
   * Tests whether the base [Geometry] properly contains a given geometry.
   *
   * @param geom the Geometry to test
   * @return true if this Geometry properly contains the given Geometry
   *
   * @see Geometry.contains
   */
  fun containsProperly(geom: Geometry): Boolean

  /**
   * Tests whether the base [Geometry] is covered by a given geometry.
   *
   * @param geom the Geometry to test
   * @return true if this Geometry is covered by the given Geometry
   *
   * @see Geometry.coveredBy
   */
  fun coveredBy(geom: Geometry): Boolean

  /**
   * Tests whether the base [Geometry] covers a given geometry.
   *
   * @param geom the Geometry to test
   * @return true if this Geometry covers the given Geometry
   *
   * @see Geometry.covers
   */
  fun covers(geom: Geometry): Boolean

  /**
   * Tests whether the base [Geometry] crosses a given geometry.
   *
   * @param geom the Geometry to test
   * @return true if this Geometry crosses the given Geometry
   *
   * @see Geometry.crosses
   */
  fun crosses(geom: Geometry): Boolean

  /**
   * Tests whether the base [Geometry] is disjoint from a given geometry.
   * This method supports [GeometryCollection]s as input
   *
   * @param geom the Geometry to test
   * @return true if this Geometry is disjoint from the given Geometry
   *
   * @see Geometry.disjoint
   */
  fun disjoint(geom: Geometry): Boolean

  /**
   * Tests whether the base [Geometry] intersects a given geometry.
   * This method supports [GeometryCollection]s as input
   *
   * @param geom the Geometry to test
   * @return true if this Geometry intersects the given Geometry
   *
   * @see Geometry.intersects
   */
  fun intersects(geom: Geometry): Boolean

  /**
   * Tests whether the base [Geometry] overlaps a given geometry.
   *
   * @param geom the Geometry to test
   * @return true if this Geometry overlaps the given Geometry
   *
   * @see Geometry.overlaps
   */
  fun overlaps(geom: Geometry): Boolean

  /**
   * Tests whether the base [Geometry] touches a given geometry.
   *
   * @param geom the Geometry to test
   * @return true if this Geometry touches the given Geometry
   *
   * @see Geometry.touches
   */
  fun touches(geom: Geometry): Boolean

  /**
   * Tests whether the base [Geometry] is within a given geometry.
   *
   * @param geom the Geometry to test
   * @return true if this Geometry is within the given Geometry
   *
   * @see Geometry.within
   */
  fun within(geom: Geometry): Boolean

}
