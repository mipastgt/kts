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

package org.locationtech.jts.geom.util

import kotlin.jvm.JvmStatic

import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryCollection
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.Polygonal

/**
 * Combines [Geometry]s
 * to produce a [GeometryCollection] of the most appropriate type.
 * Input geometries which are already collections
 * will have their elements extracted first.
 * No validation of the result geometry is performed.
 * (The only case where invalidity is possible is where [Polygonal] geometries
 * are combined and result in a self-intersection).
 *
 * @author mbdavis
 * @see GeometryFactory.buildGeometry
 */
open class GeometryCombiner
/**
 * Creates a new combiner for a collection of geometries
 *
 * @param geoms the geometries to combine
 */
(geoms: Collection<*>) {

  private val geomFactory: GeometryFactory? = extractFactory(geoms)
  private var skipEmpty = false
  private val inputGeoms: Collection<*> = geoms

  /**
   * Computes the combination of the input geometries
   * to produce the most appropriate [Geometry] or [GeometryCollection]
   *
   * @return a Geometry which is the combination of the inputs
   */
  fun combine(): Geometry? {
    val elems = ArrayList<Geometry>()
    for (i in inputGeoms) {
      val g = i as Geometry
      extractElements(g, elems)
    }

    if (elems.size == 0) {
      if (geomFactory != null) {
        // return an empty GC
        return geomFactory.createGeometryCollection()
      }
      return null
    }
    // return the "simplest possible" geometry
    return geomFactory!!.buildGeometry(elems)
  }

  private fun extractElements(geom: Geometry?, elems: MutableList<Geometry>) {
    if (geom == null)
      return

    for (i in 0 until geom.getNumGeometries()) {
      val elemGeom = geom.getGeometryN(i)
      if (skipEmpty && elemGeom.isEmpty())
        continue
      elems.add(elemGeom)
    }
  }

  companion object {
    /**
     * Combines a collection of geometries.
     *
     * @param geoms the geometries to combine
     * @return the combined geometry
     */
    @JvmStatic
    fun combine(geoms: Collection<*>): Geometry? {
      val combiner = GeometryCombiner(geoms)
      return combiner.combine()
    }

    /**
     * Combines two geometries.
     *
     * @param g0 a geometry to combine
     * @param g1 a geometry to combine
     * @return the combined geometry
     */
    @JvmStatic
    fun combine(g0: Geometry, g1: Geometry): Geometry? {
      val combiner = GeometryCombiner(createList(g0, g1))
      return combiner.combine()
    }

    /**
     * Combines three geometries.
     *
     * @param g0 a geometry to combine
     * @param g1 a geometry to combine
     * @param g2 a geometry to combine
     * @return the combined geometry
     */
    @JvmStatic
    fun combine(g0: Geometry, g1: Geometry, g2: Geometry): Geometry? {
      val combiner = GeometryCombiner(createList(g0, g1, g2))
      return combiner.combine()
    }

    /**
     * Creates a list from two items
     *
     * @return a List containing the two items
     */
    private fun createList(obj0: Any?, obj1: Any?): MutableList<Any?> {
      val list = ArrayList<Any?>()
      list.add(obj0)
      list.add(obj1)
      return list
    }

    /**
     * Creates a list from three items
     *
     * @return a List containing the items
     */
    private fun createList(obj0: Any?, obj1: Any?, obj2: Any?): MutableList<Any?> {
      val list = ArrayList<Any?>()
      list.add(obj0)
      list.add(obj1)
      list.add(obj2)
      return list
    }

    /**
     * Extracts the GeometryFactory used by the geometries in a collection
     *
     * @return a GeometryFactory
     */
    @JvmStatic
    fun extractFactory(geoms: Collection<*>): GeometryFactory? {
      if (geoms.isEmpty())
        return null
      return (geoms.iterator().next() as Geometry).getFactory()
    }
  }
}
