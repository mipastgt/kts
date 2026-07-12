/*
 * Copyright (c) 2021 Martin Davis.
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

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.CoordinateArrays
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryCollection
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.LinearRing
import org.locationtech.jts.geom.MultiLineString
import org.locationtech.jts.geom.MultiPoint
import org.locationtech.jts.geom.MultiPolygon
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.Polygon
import org.locationtech.jts.geom.prep.PreparedGeometry
import org.locationtech.jts.geom.prep.PreparedGeometryFactory
import org.locationtech.jts.operation.buffer.BufferOp
import org.locationtech.jts.operation.overlayng.OverlayNG
import org.locationtech.jts.operation.overlayng.OverlayNGRobust

/**
 * Fixes a geometry to be a valid geometry, while preserving as much as
 * possible of the shape and location of the input.
 * Validity is determined according to [Geometry.isValid].
 *
 * @author Martin Davis
 *
 * @see Geometry.isValid
 */
open class GeometryFixer(private val geom: Geometry) {

  private val factory: GeometryFactory = geom.getFactory()

  private var isKeepCollapsed = false

  private var isKeepMulti = DEFAULT_KEEP_MULTI

  /**
   * Sets whether collapsed geometries are converted to empty,
   * (which will be removed from collections),
   * or to a valid geometry of lower dimension.
   * The default is to convert collapses to empty geometries.
   *
   * @param isKeepCollapsed whether collapses should be converted to a lower dimension geometry
   */
  fun setKeepCollapsed(isKeepCollapsed: Boolean) {
    this.isKeepCollapsed = isKeepCollapsed
  }

  /**
   * Sets whether fixed {@code MULTI} geometries that consist of
   * only one item should still be returned as {@code MULTI} geometries.
   *
   * The default is to keep {@code MULTI} geometries.
   *
   * @param isKeepMulti flag whether to keep {@code MULTI} geometries.
   */
  fun setKeepMulti(isKeepMulti: Boolean) {
    this.isKeepMulti = isKeepMulti
  }

  /**
   * Gets the fixed geometry.
   *
   * @return the fixed geometry
   */
  fun getResult(): Geometry {
    /*
     *  Truly empty geometries are simply copied.
     *  Geometry collections with elements are evaluated on a per-element basis.
     */
    if (geom.getNumGeometries() == 0) {
      return geom.copy()
    }

    if (geom is Point)              return fixPoint(geom)
    //  LinearRing must come before LineString
    if (geom is LinearRing)         return fixLinearRing(geom)
    if (geom is LineString)         return fixLineString(geom)
    if (geom is Polygon)            return fixPolygon(geom)
    if (geom is MultiPoint)         return fixMultiPoint(geom)
    if (geom is MultiLineString)    return fixMultiLineString(geom)
    if (geom is MultiPolygon)       return fixMultiPolygon(geom)
    if (geom is GeometryCollection) return fixCollection(geom)
    throw UnsupportedOperationException(geom::class.simpleName)
  }

  private fun fixPoint(geom: Point): Point {
    val pt = fixPointElement(geom)
    if (pt == null)
      return factory.createPoint()
    return pt
  }

  private fun fixPointElement(geom: Point): Point? {
    if (geom.isEmpty() || !isValidPoint(geom)) {
      return null
    }
    return geom.copy() as Point
  }

  private fun fixMultiPoint(geom: MultiPoint): Geometry {
    val pts = ArrayList<Point>()
    for (i in 0 until geom.getNumGeometries()) {
      val pt = geom.getGeometryN(i) as Point
      if (pt.isEmpty()) continue
      val fixPt = fixPointElement(pt)
      if (fixPt != null) {
        pts.add(fixPt)
      }
    }

    if (!this.isKeepMulti && pts.size == 1)
      return pts[0]

    return factory.createMultiPoint(GeometryFactory.toPointArray(pts))
  }

  private fun fixLinearRing(geom: LinearRing): Geometry {
    val fix = fixLinearRingElement(geom)
    if (fix == null)
      return factory.createLinearRing()
    return fix
  }

  private fun fixLinearRingElement(geom: LinearRing): Geometry? {
    if (geom.isEmpty()) return null
    val pts = geom.getCoordinates()
    val ptsFix = fixCoordinates(pts)
    if (this.isKeepCollapsed) {
      if (ptsFix.size == 1) {
        return factory.createPoint(ptsFix[0])
      }
      if (ptsFix.size > 1 && ptsFix.size <= 3) {
        return factory.createLineString(ptsFix)
      }
    }
    //--- too short to be a valid ring
    if (ptsFix.size <= 3) {
      return null
    }

    val ring = factory.createLinearRing(ptsFix)
    //--- convert invalid ring to LineString
    if (!ring.isValid()) {
      return factory.createLineString(ptsFix)
    }
    return ring
  }

  private fun fixLineString(geom: LineString): Geometry {
    val fix = fixLineStringElement(geom)
    if (fix == null)
      return factory.createLineString()
    return fix
  }

  private fun fixLineStringElement(geom: LineString): Geometry? {
    if (geom.isEmpty()) return null
    val pts = geom.getCoordinates()
    val ptsFix = fixCoordinates(pts)
    if (this.isKeepCollapsed && ptsFix.size == 1) {
      return factory.createPoint(ptsFix[0])
    }
    if (ptsFix.size <= 1) {
      return null
    }
    return factory.createLineString(ptsFix)
  }

  private fun fixMultiLineString(geom: MultiLineString): Geometry {
    val fixed = ArrayList<Geometry>()
    var isMixed = false
    for (i in 0 until geom.getNumGeometries()) {
      val line = geom.getGeometryN(i) as LineString
      if (line.isEmpty()) continue

      val fix = fixLineStringElement(line)
      if (fix == null) continue

      if (fix !is LineString) {
        isMixed = true
      }
      fixed.add(fix)
    }

    if (fixed.size == 1) {
      if (!this.isKeepMulti || fixed[0] !is LineString)
        return fixed[0]
    }

    if (isMixed) {
      return factory.createGeometryCollection(GeometryFactory.toGeometryArray(fixed))
    }

    return factory.createMultiLineString(GeometryFactory.toLineStringArray(fixed))
  }

  private fun fixPolygon(geom: Polygon): Geometry {
    val fix = fixPolygonElement(geom)
    if (fix == null)
      return factory.createPolygon()
    return fix
  }

  private fun fixPolygonElement(geom: Polygon): Geometry? {
    val shell = geom.getExteriorRing()
    val fixShell = fixRing(shell)
    if (fixShell.isEmpty()) {
      if (this.isKeepCollapsed) {
        return fixLineString(shell)
      }
      //--- if not allowing collapses then return empty polygon
      return null
    }
    //--- if no holes then done
    if (geom.getNumInteriorRing() == 0) {
      return fixShell
    }

    //--- fix holes, classify, and construct shell-true holes
    val holesFixed = fixHoles(geom)
    val holes = ArrayList<Geometry>()
    val shells = ArrayList<Geometry>()
    classifyHoles(fixShell, holesFixed, holes, shells)
    val polyWithHoles = difference(fixShell, holes)
    if (shells.size == 0) {
      return polyWithHoles
    }

    //--- if some holes converted to shells, union all shells
    shells.add(polyWithHoles)
    val result = union(shells)
    return result
  }

  private fun fixHoles(geom: Polygon): MutableList<Geometry> {
    val holes = ArrayList<Geometry>()
    for (i in 0 until geom.getNumInteriorRing()) {
      val holeRep = fixRing(geom.getInteriorRingN(i))
      if (holeRep != null) {
        holes.add(holeRep)
      }
    }
    return holes
  }

  private fun classifyHoles(
    shell: Geometry,
    holesFixed: MutableList<Geometry>,
    holes: MutableList<Geometry>,
    shells: MutableList<Geometry>
  ) {
    val shellPrep = PreparedGeometryFactory.prepare(shell)
    for (hole in holesFixed) {
      if (shellPrep.intersects(hole)) {
        holes.add(hole)
      } else {
        shells.add(hole)
      }
    }
  }

  /**
   * Subtracts a list of polygonal geometries from a polygonal geometry.
   *
   * @param shell polygonal geometry for shell
   * @param holes polygonal geometries to subtract
   * @return the result geometry
   */
  private fun difference(shell: Geometry, holes: MutableList<Geometry>?): Geometry {
    if (holes == null || holes.size == 0)
      return shell
    val holesUnion = union(holes)
    return OverlayNGRobust.overlay(shell, holesUnion, OverlayNG.DIFFERENCE)
  }

  /**
   * Unions a list of polygonal geometries.
   * Optimizes case of zero or one input geometries.
   * Requires that the inputs are net new objects.
   *
   * @param polys the polygonal geometries to union
   * @return the union of the inputs
   */
  private fun union(polys: MutableList<Geometry>): Geometry {
    if (polys.size == 0) return factory.createPolygon()
    if (polys.size == 1) {
      return polys[0]
    }
    // TODO: replace with holes.union() once OverlayNG is the default
    return OverlayNGRobust.union(polys)
  }

  private fun fixRing(ring: LinearRing): Geometry {
    //-- always execute fix, since it may remove repeated/invalid coords etc
    // TODO: would it be faster to check ring validity first?
    val poly = factory.createPolygon(ring)
    return BufferOp.bufferByZero(poly, true)
  }

  private fun fixMultiPolygon(geom: MultiPolygon): Geometry {
    val polys = ArrayList<Geometry>()
    for (i in 0 until geom.getNumGeometries()) {
      val poly = geom.getGeometryN(i) as Polygon
      val polyFix = fixPolygonElement(poly)
      if (polyFix != null && !polyFix.isEmpty()) {
        polys.add(polyFix)
      }
    }
    if (polys.size == 0) {
      return factory.createMultiPolygon()
    }
    // TODO: replace with polys.union() once OverlayNG is the default
    var result = union(polys)

    if (this.isKeepMulti && result is Polygon)
      result = factory.createMultiPolygon(arrayOf(result as Polygon))

    return result
  }

  private fun fixCollection(geom: GeometryCollection): Geometry {
    val geomRep = Array(geom.getNumGeometries()) { i ->
      fix(geom.getGeometryN(i), this.isKeepCollapsed, this.isKeepMulti)
    }
    return factory.createGeometryCollection(geomRep)
  }

  companion object {
    private const val DEFAULT_KEEP_MULTI = true

    /**
     * Fixes a geometry to be valid.
     *
     * @param geom the geometry to be fixed
     * @return the valid fixed geometry
     */
    @JvmStatic
    fun fix(geom: Geometry): Geometry {
      return fix(geom, DEFAULT_KEEP_MULTI)
    }

    /**
     * Fixes a geometry to be valid, allowing to set a flag controlling how
     * single item results from fixed {@code MULTI} geometries should be
     * returned.
     *
     * @param geom the geometry to be fixed
     * @param isKeepMulti a flag indicating if {@code MULTI} geometries should not be converted to single instance types
     *                    if they consist of only one item.
     * @return the valid fixed geometry
     */
    @JvmStatic
    fun fix(geom: Geometry, isKeepMulti: Boolean): Geometry {
      val fix = GeometryFixer(geom)
      fix.setKeepMulti(isKeepMulti)
      return fix.getResult()
    }

    private fun isValidPoint(pt: Point): Boolean {
      val p = pt.getCoordinate()
      return p!!.isValid()
    }

    /**
     * Returns a clean copy of the input coordinate array.
     *
     * @param pts coordinates to clean
     * @return an array of clean coordinates
     */
    private fun fixCoordinates(pts: Array<Coordinate>): Array<Coordinate> {
      val ptsClean = CoordinateArrays.removeRepeatedOrInvalidPoints(pts)
      return CoordinateArrays.copyDeep(ptsClean)
    }

    private fun fix(geom: Geometry, isKeepCollapsed: Boolean, isKeepMulti: Boolean): Geometry {
      val fix = GeometryFixer(geom)
      fix.setKeepCollapsed(isKeepCollapsed)
      fix.setKeepMulti(isKeepMulti)
      return fix.getResult()
    }
  }
}
