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
import org.locationtech.jts.geom.GeometryFilter
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.LinearRing
import org.locationtech.jts.geom.MultiLineString
import org.locationtech.jts.geom.MultiPoint
import org.locationtech.jts.geom.MultiPolygon
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.Polygon
import kotlin.reflect.KClass

/**
 * Extracts the components of a given type from a [Geometry].
 *
 */
open class GeometryExtracter : GeometryFilter {

  private val geometryType: String?
  private val comps: MutableList<Any?>

  /**
   * Constructs a filter with a list in which to store the elements found.
   *
   * @param clz the class of the components to extract (null means all types)
   * @param comps the list to extract into
   */
  constructor(clz: KClass<*>?, comps: MutableList<Any?>) {
    this.geometryType = toGeometryType(clz)
    this.comps = comps
  }

  /**
   * Constructs a filter with a list in which to store the elements found.
   *
   * @param geometryType Geometry type to extract (null means all types)
   * @param comps the list to extract into
   */
  constructor(geometryType: String?, comps: MutableList<Any?>) {
    this.geometryType = geometryType
    this.comps = comps
  }

  override fun filter(geom: Geometry) {
    if (geometryType == null || isOfType(geom, geometryType))
      comps.add(geom)
  }

  companion object {
    /**
     * Extracts the components of type `clz` from a [Geometry]
     * and adds them to the provided [List].
     *
     * @param geom the geometry from which to extract
     * @param list the list to add the extracted elements to
     */
    @JvmStatic
    fun extract(geom: Geometry, clz: KClass<*>?, list: MutableList<Any?>): MutableList<Any?> {
      return extract(geom, toGeometryType(clz), list)
    }

    private fun toGeometryType(clz: KClass<*>?): String? {
      if (clz == null)
        return null
      else if (clz == Point::class)
        return Geometry.TYPENAME_POINT
      else if (clz == LineString::class)
        return Geometry.TYPENAME_LINESTRING
      else if (clz == LinearRing::class)
        return Geometry.TYPENAME_LINEARRING
      else if (clz == Polygon::class)
        return Geometry.TYPENAME_POLYGON
      else if (clz == MultiPoint::class)
        return Geometry.TYPENAME_MULTIPOINT
      else if (clz == MultiLineString::class)
        return Geometry.TYPENAME_MULTILINESTRING
      else if (clz == MultiPolygon::class)
        return Geometry.TYPENAME_MULTIPOLYGON
      else if (clz == GeometryCollection::class)
        return Geometry.TYPENAME_GEOMETRYCOLLECTION
      throw RuntimeException("Unsupported class")
    }

    /**
     * Extracts the components of `geometryType` from a [Geometry]
     * and adds them to the provided [List].
     *
     * @param geom the geometry from which to extract
     * @param geometryType Geometry type to extract (null means all types)
     * @param list the list to add the extracted elements to
     */
    @JvmStatic
    fun extract(geom: Geometry, geometryType: String?, list: MutableList<Any?>): MutableList<Any?> {
      if (geom.getGeometryType() === geometryType) {
        list.add(geom)
      } else if (geom is GeometryCollection) {
        geom.apply(GeometryExtracter(geometryType, list))
      }
      // skip non-LineString elemental geometries

      return list
    }

    /**
     * Extracts the components of type `clz` from a [Geometry]
     * and returns them in a [List].
     *
     * @param geom the geometry from which to extract
     */
    @JvmStatic
    fun extract(geom: Geometry, clz: KClass<*>?): MutableList<Any?> {
      return extract(geom, clz, ArrayList())
    }

    @JvmStatic
    fun extract(geom: Geometry, geometryType: String?): MutableList<Any?> {
      return extract(geom, geometryType, ArrayList())
    }

    @JvmStatic
    protected fun isOfType(geom: Geometry, geometryType: String?): Boolean {
      if (geom.getGeometryType() === geometryType) return true
      if (geometryType === Geometry.TYPENAME_LINESTRING &&
          geom.getGeometryType() === Geometry.TYPENAME_LINEARRING) return true
      return false
    }
  }
}
