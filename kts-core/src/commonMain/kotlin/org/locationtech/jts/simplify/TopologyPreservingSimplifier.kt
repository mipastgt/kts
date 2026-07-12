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

package org.locationtech.jts.simplify

import kotlin.jvm.JvmStatic

import org.locationtech.jts.geom.CoordinateSequence
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryComponentFilter
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.LinearRing
import org.locationtech.jts.geom.util.GeometryTransformer

/**
 * Simplifies a geometry and ensures that
 * the result is a valid geometry having the
 * same dimension and number of components as the input,
 * and with the components having the same topological relationship.
 *
 * @author Martin Davis
 * @see DouglasPeuckerSimplifier
 */
class TopologyPreservingSimplifier(private val inputGeom: Geometry) {

  private val lineSimplifier = TaggedLinesSimplifier()
  private lateinit var linestringMap: MutableMap<LineString, TaggedLineString>

  /**
   * Sets the distance tolerance for the simplification.
   * The tolerance value must be non-negative.
   *
   * @param distanceTolerance the approximation tolerance to use
   */
  fun setDistanceTolerance(distanceTolerance: Double) {
    if (distanceTolerance < 0.0)
      throw IllegalArgumentException("Tolerance must be non-negative")
    lineSimplifier.setDistanceTolerance(distanceTolerance)
  }

  fun getResultGeometry(): Geometry {
    // empty input produces an empty result
    if (inputGeom.isEmpty()) return inputGeom.copy()

    linestringMap = HashMap()
    inputGeom.apply(LineStringMapBuilderFilter(this))
    lineSimplifier.simplify(linestringMap.values)
    val result = LineStringTransformer(linestringMap).transform(inputGeom)
    return result!!
  }

  internal class LineStringTransformer(
      private val linestringMap: Map<LineString, TaggedLineString>
  ) : GeometryTransformer() {

    override fun transformCoordinates(coords: CoordinateSequence, parent: Geometry?): CoordinateSequence? {
      if (coords.size() == 0) return null
      // for linear components (including rings), simplify the linestring
      if (parent is LineString) {
        val taggedLine = linestringMap.get(parent)
        return createCoordinateSequence(taggedLine!!.getResultCoordinates())
      }
      // for anything else (e.g. points) just copy the coordinates
      return super.transformCoordinates(coords, parent)
    }
  }

  /**
   * A filter to add linear geometries to the linestring map
   * with the appropriate minimum size constraint.
   *
   * @author Martin Davis
   */
  internal class LineStringMapBuilderFilter(private val tps: TopologyPreservingSimplifier) : GeometryComponentFilter {

    /**
     * Filters linear geometries.
     */
    override fun filter(geom: Geometry) {
      if (geom is LineString) {
        val line = geom
        // skip empty geometries
        if (line.isEmpty()) return

        val minSize = if (line.isClosed()) 4 else 2
        val isRing = line is LinearRing
        val taggedLine = TaggedLineString(line, minSize, isRing)
        tps.linestringMap.put(line, taggedLine)
      }
    }
  }

  companion object {
    @JvmStatic
    fun simplify(geom: Geometry, distanceTolerance: Double): Geometry {
      val tss = TopologyPreservingSimplifier(geom)
      tss.setDistanceTolerance(distanceTolerance)
      return tss.getResultGeometry()
    }
  }
}
