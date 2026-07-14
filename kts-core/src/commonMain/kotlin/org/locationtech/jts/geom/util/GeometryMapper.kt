/*
 * Copyright (c) 2016 Martin Davis.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.locationtech.jts.geom.util

import kotlin.jvm.JvmStatic

import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryCollection

/**
 * Methods to map various collections
 * of [Geometry]s
 * via defined mapping functions.
 *
 * @author Martin Davis
 *
 */
open class GeometryMapper {
  /**
   * An interface for geometry functions that map a geometry input to a geometry output.
   * The output may be `null` if there is no valid output value for
   * the given input value.
   *
   * @author Martin Davis
   *
   */
  interface MapOp {
    /**
     * Maps a geometry value into another value.
     *
     * @param geom the input geometry
     * @return a result geometry
     */
    fun map(geom: Geometry): Geometry?
  }

  companion object {
    /**
     * Maps the members of a [Geometry]
     * (which may be atomic or composite)
     * into another `Geometry` of most specific type.
     * `null` results are skipped.
     * In the case of hierarchical [GeometryCollection]s,
     * only the first level of members are mapped.
     *
     * @param geom the input atomic or composite geometry
     * @param op the mapping operation
     * @return a result collection or geometry of most specific type
     */
    @JvmStatic
    fun map(geom: Geometry, op: MapOp): Geometry {
      val mapped = ArrayList<Geometry>()
      for (i in 0 until geom.getNumGeometries()) {
        val g = op.map(geom.getGeometryN(i))
        if (g != null)
          mapped.add(g)
      }
      return geom.getFactory().buildGeometry(mapped)
    }

    @JvmStatic
    fun map(geoms: Collection<*>, op: MapOp): Collection<*> {
      val mapped = ArrayList<Geometry>()
      for (i in geoms) {
        val g = i as Geometry
        val gr = op.map(g)
        if (gr != null)
          mapped.add(gr)
      }
      return mapped
    }

    /**
     * Maps the atomic elements of a [Geometry]
     * (which may be atomic or composite)
     * using a [MapOp] mapping operation
     * into an atomic `Geometry` or a flat collection
     * of the most specific type.
     * `null` and empty values returned from the mapping operation
     * are discarded.
     *
     * @param geom the geometry to map
     * @param emptyDim the dimension of empty geometry to create
     * @param op the mapping operation
     * @return the mapped result
     */
    @JvmStatic
    fun flatMap(geom: Geometry, emptyDim: Int, op: MapOp): Geometry {
      val mapped = ArrayList<Geometry>()
      flatMap(geom, op, mapped)

      if (mapped.size == 0) {
        return geom.getFactory().createEmpty(emptyDim)
      }
      if (mapped.size == 1)
        return mapped[0]
      return geom.getFactory().buildGeometry(mapped)
    }

    private fun flatMap(geom: Geometry, op: MapOp, mapped: MutableList<Geometry>) {
      for (i in 0 until geom.getNumGeometries()) {
        val g = geom.getGeometryN(i)
        if (g is GeometryCollection) {
          flatMap(g, op, mapped)
        } else {
          val res = op.map(g)
          if (res != null && !res.isEmpty()) {
            addFlat(res, mapped)
          }
        }
      }
    }

    private fun addFlat(geom: Geometry, geomList: MutableList<Geometry>) {
      if (geom.isEmpty()) return
      if (geom is GeometryCollection) {
        for (i in 0 until geom.getNumGeometries()) {
          addFlat(geom.getGeometryN(i), geomList)
        }
      } else {
        geomList.add(geom)
      }
    }
  }
}
