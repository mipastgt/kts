/*
 * Copyright (c) 2019 Martin Davis.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.locationtech.jts.operation.overlayng

import kotlin.jvm.JvmStatic

import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel
import org.locationtech.jts.operation.union.UnaryUnionOp
import org.locationtech.jts.operation.union.UnionStrategy

/**
 * Unions a geometry or collection of geometries in an
 * efficient way, using [OverlayNG]
 * to ensure robust computation.
 *
 * @author Martin Davis
 * @see OverlayNGRobust
 */
class UnaryUnionNG private constructor() {

  companion object {
    /**
     * Unions a geometry (which is often a collection)
     * using a given precision model.
     *
     * @param geom the geometry to union
     * @param pm the precision model to use
     * @return the union of the geometry
     */
    @JvmStatic
    fun union(geom: Geometry, pm: PrecisionModel): Geometry? {
      val op = UnaryUnionOp(geom)
      op.setUnionFunction(createUnionStrategy(pm))
      return op.union()
    }

    /**
     * Unions a collection of geometries
     * using a given precision model.
     *
     * @param geoms the collection of geometries to union
     * @param pm the precision model to use
     * @return the union of the geometries
     */
    @JvmStatic
    fun union(geoms: Collection<Geometry>, pm: PrecisionModel): Geometry? {
      val op = UnaryUnionOp(geoms)
      op.setUnionFunction(createUnionStrategy(pm))
      return op.union()
    }

    /**
     * Unions a collection of geometries
     * using a given precision model.
     *
     * @param geoms the collection of geometries to union
     * @param geomFact the geometry factory to use
     * @param pm the precision model to use
     * @return the union of the geometries
     */
    @JvmStatic
    fun union(geoms: Collection<Geometry>, geomFact: GeometryFactory, pm: PrecisionModel): Geometry? {
      val op = UnaryUnionOp(geoms, geomFact)
      op.setUnionFunction(createUnionStrategy(pm))
      return op.union()
    }

    private fun createUnionStrategy(pm: PrecisionModel): UnionStrategy {
      val unionSRFun: UnionStrategy = object : UnionStrategy {

        override fun union(g0: Geometry, g1: Geometry): Geometry {
          return OverlayNG.overlay(g0, g1, OverlayNG.UNION, pm)
        }

        override fun isFloatingPrecision(): Boolean {
          return OverlayUtil.isFloating(pm)
        }
      }
      return unionSRFun
    }
  }
}
