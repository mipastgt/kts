/*
 * Copyright (c) 2022 Martin Davis.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.locationtech.jts.coverage

import kotlin.jvm.JvmStatic

import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.index.strtree.STRtree

/**
 * Validates a polygonal coverage, and returns the locations of
 * invalid polygon boundary segments if found.
 *
 * @author Martin Davis
 */
class CoverageValidator(private val coverage: Array<Geometry>) {

  private var gapWidth = 0.0

  /**
   * Sets the maximum gap width, if narrow gaps are to be detected.
   *
   * @param gapWidth the maximum width of gaps to detect
   */
  fun setGapWidth(gapWidth: Double) {
    this.gapWidth = gapWidth
  }

  /**
   * Validates the polygonal coverage.
   *
   * @return an array of nulls or linear geometries
   */
  fun validate(): Array<Geometry?> {
    val index = STRtree()
    for (geom in coverage) {
      index.insert(geom.getEnvelopeInternal(), geom)
    }
    val invalidLines = arrayOfNulls<Geometry>(coverage.size)
    for (i in coverage.indices) {
      val geom = coverage[i]
      invalidLines[i] = validate(geom, index)
    }
    return invalidLines
  }

  private fun validate(targetGeom: Geometry, index: STRtree): Geometry? {
    val queryEnv = targetGeom.getEnvelopeInternal()
    queryEnv.expandBy(gapWidth)
    val nearGeomList = index.query(queryEnv)
    //-- the target geometry is returned in the query, so must be removed from the set
    nearGeomList.remove(targetGeom)

    val nearGeoms = GeometryFactory.toGeometryArray(nearGeomList)
    val result = CoveragePolygonValidator.validate(targetGeom, nearGeoms!!, gapWidth)
    return if (result.isEmpty()) null else result
  }

  companion object {
    /**
     * Tests whether a polygonal coverage is valid.
     *
     * @param coverage an array of polygons forming a coverage
     * @return true if the coverage is valid
     */
    @JvmStatic
    fun isValid(coverage: Array<Geometry>): Boolean {
      val v = CoverageValidator(coverage)
      return !hasInvalidResult(v.validate())
    }

    /**
     * Tests if some element of an array of geometries is a coverage invalidity
     * indicator.
     *
     * @param validateResult an array produced by a polygonal coverage validation
     * @return true if the result has at least one invalid indicator
     */
    @JvmStatic
    fun hasInvalidResult(validateResult: Array<Geometry?>): Boolean {
      for (geom in validateResult) {
        if (geom != null)
          return true
      }
      return false
    }

    /**
     * Validates that a set of polygons forms a valid polygonal coverage,
     * and returns linear geometries indicating the locations of invalidities, if any.
     *
     * @param coverage an array of polygons forming a coverage
     * @return an array of linear geometries indicating coverage errors, or nulls
     */
    @JvmStatic
    fun validate(coverage: Array<Geometry>): Array<Geometry?> {
      val v = CoverageValidator(coverage)
      return v.validate()
    }

    /**
     * Validates that a set of polygons forms a valid polygonal coverage
     * and contains no gaps narrower than a specified width.
     *
     * @param coverage an array of polygons forming a coverage
     * @param gapWidth the maximum width of invalid gaps
     * @return an array of linear geometries indicating coverage errors, or nulls
     */
    @JvmStatic
    fun validate(coverage: Array<Geometry>, gapWidth: Double): Array<Geometry?> {
      val v = CoverageValidator(coverage)
      v.setGapWidth(gapWidth)
      return v.validate()
    }
  }
}
