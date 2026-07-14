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
package org.locationtech.jts.operation.relate

import kotlin.jvm.JvmStatic

import org.locationtech.jts.algorithm.BoundaryNodeRule
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.IntersectionMatrix
import org.locationtech.jts.operation.GeometryGraphOperation

/**
 * Implements the SFS `relate()` generalized spatial predicate on two [Geometry]s.
 *
 *
 * The class supports specifying a custom [BoundaryNodeRule]
 * to be used during the relate computation.
 *
 *
 * If named spatial predicates are used on the result [IntersectionMatrix]
 * of the RelateOp, the result may or not be affected by the
 * choice of `BoundaryNodeRule`, depending on the exact nature of the pattern.
 * For instance, [IntersectionMatrix.isIntersects] is insensitive
 * to the choice of `BoundaryNodeRule`,
 * whereas [IntersectionMatrix.isTouches] is affected by the rule chosen.
 *
 *
 * **Note:** custom Boundary Node Rules do not (currently)
 * affect the results of other [Geometry] methods (such
 * as [Geometry.getBoundary].  The results of
 * these methods may not be consistent with the relationship computed by
 * a custom Boundary Node Rule.
 *
 */
class RelateOp : GeometryGraphOperation {

  private val relate: RelateComputer

  /**
   * Creates a new Relate operation, using the default (OGC SFS) Boundary Node Rule.
   *
   * @param g0 a Geometry to relate
   * @param g1 another Geometry to relate
   */
  constructor(g0: Geometry, g1: Geometry) : super(g0, g1) {
    relate = RelateComputer(arg)
  }

  /**
   * Creates a new Relate operation with a specified Boundary Node Rule.
   *
   * @param g0 a Geometry to relate
   * @param g1 another Geometry to relate
   * @param boundaryNodeRule the Boundary Node Rule to use
   */
  constructor(g0: Geometry, g1: Geometry, boundaryNodeRule: BoundaryNodeRule) :
    super(g0, g1, boundaryNodeRule) {
    relate = RelateComputer(arg)
  }

  /**
   * Gets the IntersectionMatrix for the spatial relationship
   * between the input geometries.
   *
   * @return the IntersectionMatrix for the spatial relationship between the input geometries
   */
  fun getIntersectionMatrix(): IntersectionMatrix {
    return relate.computeIM()
  }

  companion object {
    /**
     * Computes the [IntersectionMatrix] for the spatial relationship
     * between two [Geometry]s, using the default (OGC SFS) Boundary Node Rule
     *
     * @param a a Geometry to test
     * @param b a Geometry to test
     * @return the IntersectionMatrix for the spatial relationship between the geometries
     */
    @JvmStatic
    fun relate(a: Geometry, b: Geometry): IntersectionMatrix {
      val relOp = RelateOp(a, b)
      return relOp.getIntersectionMatrix()
    }

    /**
     * Computes the [IntersectionMatrix] for the spatial relationship
     * between two [Geometry]s using a specified Boundary Node Rule.
     *
     * @param a a Geometry to test
     * @param b a Geometry to test
     * @param boundaryNodeRule the Boundary Node Rule to use
     * @return the IntersectionMatrix for the spatial relationship between the input geometries
     */
    @JvmStatic
    fun relate(a: Geometry, b: Geometry, boundaryNodeRule: BoundaryNodeRule): IntersectionMatrix {
      val relOp = RelateOp(a, b, boundaryNodeRule)
      return relOp.getIntersectionMatrix()
    }
  }
}
