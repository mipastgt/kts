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
import org.locationtech.jts.geom.GeometryComponentFilter
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.LinearRing

/**
 * Extracts all the 1-dimensional ([LineString]) components from a [Geometry].
 * For polygonal geometries, this will extract all the component [LinearRing]s.
 * If desired, `LinearRing`s can be forced to be returned as `LineString`s.
 *
 * @version 1.7
 */
open class LinearComponentExtracter : GeometryComponentFilter {

  private val lines: MutableCollection<in LineString>
  private var isForcedToLineString = false

  /**
   * Constructs a LineExtracterFilter with a list in which to store LineStrings found.
   */
  constructor(lines: MutableCollection<in LineString>) {
    this.lines = lines
  }

  /**
   * Constructs a LineExtracterFilter with a list in which to store LineStrings found.
   */
  constructor(lines: MutableCollection<in LineString>, isForcedToLineString: Boolean) {
    this.lines = lines
    this.isForcedToLineString = isForcedToLineString
  }

  /**
   * Indicates that LinearRing components should be
   * converted to pure LineStrings.
   *
   * @param isForcedToLineString true if LinearRings should be converted to LineStrings
   */
  fun setForceToLineString(isForcedToLineString: Boolean) {
    this.isForcedToLineString = isForcedToLineString
  }

  override fun filter(geom: Geometry) {
    if (isForcedToLineString && geom is LinearRing) {
      val line = geom.getFactory().createLineString(geom.getCoordinateSequence())
      lines.add(line)
      return
    }
    // if not being forced, and this is a linear component
    if (geom is LineString) lines.add(geom)

    // else this is not a linear component, so skip it
  }

  companion object {
    /**
     * Extracts the linear components from a collection of [Geometry]s
     * and adds them to the provided [Collection].
     *
     * @return the collection of linear components (LineStrings or LinearRings)
     */
    @JvmStatic
    fun getLines(geoms: Collection<*>, lines: MutableCollection<in LineString>): MutableCollection<in LineString> {
      for (o in geoms) {
        val g = o as Geometry
        getLines(g, lines)
      }
      return lines
    }

    /**
     * Extracts the linear components from a collection of [Geometry]s
     * and adds them to the provided [Collection].
     *
     * @return the collection of linear components (LineStrings or LinearRings)
     */
    @JvmStatic
    fun getLines(geoms: Collection<*>, lines: MutableCollection<in LineString>, forceToLineString: Boolean): MutableCollection<in LineString> {
      for (o in geoms) {
        val g = o as Geometry
        getLines(g, lines, forceToLineString)
      }
      return lines
    }

    /**
     * Extracts the linear components from a single [Geometry]
     * and adds them to the provided [Collection].
     *
     * @return the Collection of linear components (LineStrings or LinearRings)
     */
    @JvmStatic
    fun getLines(geom: Geometry, lines: MutableCollection<in LineString>): MutableCollection<in LineString> {
      if (geom is LineString) {
        lines.add(geom)
      } else {
        geom.apply(LinearComponentExtracter(lines))
      }
      return lines
    }

    /**
     * Extracts the linear components from a single [Geometry]
     * and adds them to the provided [Collection].
     *
     * @return the Collection of linear components (LineStrings or LinearRings)
     */
    @JvmStatic
    fun getLines(geom: Geometry, lines: MutableCollection<in LineString>, forceToLineString: Boolean): MutableCollection<in LineString> {
      geom.apply(LinearComponentExtracter(lines, forceToLineString))
      return lines
    }

    /**
     * Extracts the linear components from a single geometry.
     *
     * @return the list of linear components
     */
    @JvmStatic
    fun getLines(geom: Geometry): List<LineString> {
      return getLines(geom, false)
    }

    /**
     * Extracts the linear components from a single geometry.
     *
     * @param forceToLineString true if LinearRings should be converted to LineStrings
     * @return the list of linear components
     */
    @JvmStatic
    fun getLines(geom: Geometry, forceToLineString: Boolean): List<LineString> {
      val lines = ArrayList<LineString>()
      geom.apply(LinearComponentExtracter(lines, forceToLineString))
      return lines
    }

    /**
     * Extracts the linear components from a single [Geometry]
     * and returns them as either a [LineString] or [org.locationtech.jts.geom.MultiLineString].
     *
     * @return a linear geometry
     */
    @JvmStatic
    fun getGeometry(geom: Geometry): Geometry {
      return geom.getFactory().buildGeometry(getLines(geom))
    }

    /**
     * Extracts the linear components from a single [Geometry]
     * and returns them as either a [LineString] or [org.locationtech.jts.geom.MultiLineString].
     *
     * @param forceToLineString true if LinearRings should be converted to LineStrings
     * @return a linear geometry
     */
    @JvmStatic
    fun getGeometry(geom: Geometry, forceToLineString: Boolean): Geometry {
      return geom.getFactory().buildGeometry(getLines(geom, forceToLineString))
    }
  }
}
