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

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.CoordinateSequence
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.LinearRing
import org.locationtech.jts.geom.MultiPolygon
import org.locationtech.jts.geom.Polygon
import org.locationtech.jts.geom.util.GeometryTransformer

/**
 * Simplifies a [Geometry] using the Visvalingam-Whyatt area-based algorithm.
 *
 * @version 1.7
 */
class VWSimplifier(private val inputGeom: Geometry) {

  private var distanceTolerance = 0.0
  private var isEnsureValidTopology = true

  /**
   * Sets the distance tolerance for the simplification.
   * The tolerance value must be non-negative.
   */
  fun setDistanceTolerance(distanceTolerance: Double) {
    if (distanceTolerance < 0.0)
      throw IllegalArgumentException("Tolerance must be non-negative")
    this.distanceTolerance = distanceTolerance
  }

  /**
   * Controls whether simplified polygons will be "fixed" to have valid
   * topology.
   *
   * The default is to fix polygon topology.
   */
  fun setEnsureValid(isEnsureValidTopology: Boolean) {
    this.isEnsureValidTopology = isEnsureValidTopology
  }

  /**
   * Gets the simplified geometry.
   *
   * @return the simplified geometry
   */
  fun getResultGeometry(): Geometry {
    // empty input produces an empty result
    if (inputGeom.isEmpty())
      return inputGeom.copy()

    return VWTransformer(isEnsureValidTopology, distanceTolerance).transform(inputGeom)!!
  }

  class VWTransformer(
      private val isEnsureValidTopology: Boolean,
      private val distanceTolerance: Double
  ) : GeometryTransformer() {

    override fun transformCoordinates(coords: CoordinateSequence, parent: Geometry?): CoordinateSequence? {
      val inputPts = coords.toCoordinateArray()
      val newPts: Array<Coordinate>
      if (inputPts.isEmpty()) {
        newPts = arrayOf()
      } else {
        newPts = VWLineSimplifier.simplify(inputPts, distanceTolerance)
      }
      return factory!!.getCoordinateSequenceFactory().create(newPts)
    }

    /**
     * Simplifies a polygon, fixing it if required.
     */
    override fun transformPolygon(geom: Polygon, parent: Geometry?): Geometry? {
      // empty geometries are simply removed
      if (geom.isEmpty())
        return null
      val rawGeom = super.transformPolygon(geom, parent)
      // don't try and correct if the parent is going to do this
      if (parent is MultiPolygon) {
        return rawGeom
      }
      return createValidArea(rawGeom!!)
    }

    /**
     * Simplifies a LinearRing. If the simplification results in a degenerate
     * ring, remove the component.
     *
     * @return null if the simplification results in a degenerate ring
     */
    override fun transformLinearRing(geom: LinearRing, parent: Geometry?): Geometry? {
      val removeDegenerateRings = parent is Polygon
      val simpResult = super.transformLinearRing(geom, parent)
      if (removeDegenerateRings && simpResult !is LinearRing)
        return null
      return simpResult
    }

    /**
     * Simplifies a MultiPolygon, fixing it if required.
     */
    override fun transformMultiPolygon(geom: MultiPolygon, parent: Geometry?): Geometry? {
      val rawGeom = super.transformMultiPolygon(geom, parent)
      return createValidArea(rawGeom!!)
    }

    /**
     * Creates a valid area geometry from one that possibly has bad topology
     * (i.e. self-intersections).
     *
     * @param rawAreaGeom an area geometry possibly containing self-intersections
     * @return a valid area geometry
     */
    private fun createValidArea(rawAreaGeom: Geometry): Geometry {
      // if geometry is invalid then make it valid
      if (isEnsureValidTopology && !rawAreaGeom.isValid())
        return rawAreaGeom.buffer(0.0)
      return rawAreaGeom
    }
  }

  companion object {
    /**
     * Simplifies a geometry using a given tolerance.
     *
     * @param geom geometry to simplify
     * @param distanceTolerance the tolerance to use
     * @return a simplified version of the geometry
     */
    @JvmStatic
    fun simplify(geom: Geometry, distanceTolerance: Double): Geometry {
      val simp = VWSimplifier(geom)
      simp.setDistanceTolerance(distanceTolerance)
      return simp.getResultGeometry()
    }
  }
}
