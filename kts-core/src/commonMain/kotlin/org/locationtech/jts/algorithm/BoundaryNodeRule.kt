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
package org.locationtech.jts.algorithm

import kotlin.jvm.JvmField

/**
 * An interface for rules which determine whether node points
 * which are in boundaries of [org.locationtech.jts.geom.Lineal] geometry components
 * are in the boundary of the parent geometry collection.
 * The SFS specifies a single kind of boundary node rule,
 * the [Mod2BoundaryNodeRule] rule.
 * However, other kinds of Boundary Node Rules are appropriate
 * in specific situations (for instance, linear network topology
 * usually follows the [EndPointBoundaryNodeRule].)
 * Some JTS operations allow the BoundaryNodeRule to be specified,
 * and respect the supplied rule when computing the results of the operation.
 * <p>
 * This interface and its subclasses follow the `Strategy` design pattern.
 *
 * @author Martin Davis
 * @version 1.7
 */
interface BoundaryNodeRule {

  /**
   * Tests whether a point that lies in `boundaryCount`
   * geometry component boundaries is considered to form part of the boundary
   * of the parent geometry.
   *
   * @param boundaryCount the number of component boundaries that this point occurs in
   * @return true if points in this number of boundaries lie in the parent boundary
   */
  fun isInBoundary(boundaryCount: Int): Boolean

  /**
   * A [BoundaryNodeRule] specifies that points are in the
   * boundary of a lineal geometry iff
   * the point lies on the boundary of an odd number
   * of components.
   * Under this rule [org.locationtech.jts.geom.LinearRing]s and closed
   * [org.locationtech.jts.geom.LineString]s have an empty boundary.
   * <p>
   * This is the rule specified by the <i>OGC SFS</i>,
   * and is the default rule used in JTS.
   *
   * @author Martin Davis
   * @version 1.7
   */
  class Mod2BoundaryNodeRule : BoundaryNodeRule {
    override fun isInBoundary(boundaryCount: Int): Boolean {
      // the "Mod-2 Rule"
      return boundaryCount % 2 == 1
    }

    override fun toString(): String {
      return "Mod2 Boundary Node Rule"
    }
  }

  /**
   * A [BoundaryNodeRule] which specifies that any points which are endpoints
   * of lineal components are in the boundary of the
   * parent geometry.
   * This corresponds to the "intuitive" topological definition
   * of boundary.
   * Under this rule [org.locationtech.jts.geom.LinearRing]s have a non-empty boundary
   * (the common endpoint of the underlying LineString).
   *
   * @author Martin Davis
   * @version 1.7
   */
  class EndPointBoundaryNodeRule : BoundaryNodeRule {
    override fun isInBoundary(boundaryCount: Int): Boolean {
      return boundaryCount > 0
    }

    override fun toString(): String {
      return "EndPoint Boundary Node Rule"
    }
  }

  /**
   * A [BoundaryNodeRule] which determines that only
   * endpoints with valency greater than 1 are on the boundary.
   * This corresponds to the boundary of a [org.locationtech.jts.geom.MultiLineString]
   * being all the "attached" endpoints, but not
   * the "unattached" ones.
   *
   * @author Martin Davis
   * @version 1.7
   */
  class MultiValentEndPointBoundaryNodeRule : BoundaryNodeRule {
    override fun isInBoundary(boundaryCount: Int): Boolean {
      return boundaryCount > 1
    }

    override fun toString(): String {
      return "MultiValent EndPoint Boundary Node Rule"
    }
  }

  /**
   * A [BoundaryNodeRule] which determines that only
   * endpoints with valency of exactly 1 are on the boundary.
   * This corresponds to the boundary of a [org.locationtech.jts.geom.MultiLineString]
   * being all the "unattached" endpoints.
   *
   * @author Martin Davis
   * @version 1.7
   */
  class MonoValentEndPointBoundaryNodeRule : BoundaryNodeRule {
    override fun isInBoundary(boundaryCount: Int): Boolean {
      return boundaryCount == 1
    }

    override fun toString(): String {
      return "MonoValent EndPoint Boundary Node Rule"
    }
  }

  companion object {
    /**
     * The Mod-2 Boundary Node Rule (which is the rule specified in the OGC SFS).
     * @see Mod2BoundaryNodeRule
     */
    @JvmField
    val MOD2_BOUNDARY_RULE: BoundaryNodeRule = Mod2BoundaryNodeRule()

    /**
     * The Endpoint Boundary Node Rule.
     * @see EndPointBoundaryNodeRule
     */
    @JvmField
    val ENDPOINT_BOUNDARY_RULE: BoundaryNodeRule = EndPointBoundaryNodeRule()

    /**
     * The MultiValent Endpoint Boundary Node Rule.
     * @see MultiValentEndPointBoundaryNodeRule
     */
    @JvmField
    val MULTIVALENT_ENDPOINT_BOUNDARY_RULE: BoundaryNodeRule = MultiValentEndPointBoundaryNodeRule()

    /**
     * The Monovalent Endpoint Boundary Node Rule.
     * @see MonoValentEndPointBoundaryNodeRule
     */
    @JvmField
    val MONOVALENT_ENDPOINT_BOUNDARY_RULE: BoundaryNodeRule = MonoValentEndPointBoundaryNodeRule()

    /**
     * The Boundary Node Rule specified by the OGC Simple Features Specification,
     * which is the same as the Mod-2 rule.
     * @see Mod2BoundaryNodeRule
     */
    @JvmField
    val OGC_SFS_BOUNDARY_RULE: BoundaryNodeRule = MOD2_BOUNDARY_RULE
  }
}
