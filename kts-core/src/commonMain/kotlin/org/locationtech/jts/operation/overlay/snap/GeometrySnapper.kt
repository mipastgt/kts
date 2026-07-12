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
package org.locationtech.jts.operation.overlay.snap

import kotlin.jvm.JvmStatic
import kotlin.math.min

import org.locationtech.jts.util.TreeSet

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.CoordinateSequence
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.Polygonal
import org.locationtech.jts.geom.PrecisionModel
import org.locationtech.jts.geom.util.GeometryTransformer

/**
 * Snaps the vertices and segments of a [Geometry]
 * to another Geometry's vertices.
 * A snap distance tolerance is used to control where snapping is performed.
 *
 * @author Martin Davis
 * @version 1.7
 */
class GeometrySnapper(private val srcGeom: Geometry) {

  /**
   * Snaps the vertices in the component [org.locationtech.jts.geom.LineString]s
   * of the source geometry
   * to the vertices of the given snap geometry.
   *
   * @param snapGeom a geometry to snap the source to
   * @return a new snapped Geometry
   */
  fun snapTo(snapGeom: Geometry, snapTolerance: Double): Geometry {
    val snapPts = extractTargetCoordinates(snapGeom)

    val snapTrans = SnapTransformer(snapTolerance, snapPts)
    return snapTrans.transform(srcGeom)!!
  }

  /**
   * Snaps the vertices in the component [org.locationtech.jts.geom.LineString]s
   * of the source geometry
   * to the vertices of the same geometry.
   * Allows optionally cleaning the result to ensure it is
   * topologically valid
   * (which fixes issues such as topology collapses in polygonal inputs).
   *
   * @param snapTolerance the snapping tolerance
   * @param cleanResult whether the result should be made valid
   * @return a new snapped Geometry
   */
  fun snapToSelf(snapTolerance: Double, cleanResult: Boolean): Geometry {
    val snapPts = extractTargetCoordinates(srcGeom)

    val snapTrans = SnapTransformer(snapTolerance, snapPts, true)
    val snappedGeom = snapTrans.transform(srcGeom)!!
    var result: Geometry = snappedGeom
    if (cleanResult && result is Polygonal) {
      // TODO: use better cleaning approach
      result = snappedGeom.buffer(0.0)
    }
    return result
  }

  private fun extractTargetCoordinates(g: Geometry): Array<Coordinate> {
    // TODO: should do this more efficiently.  Use CoordSeq filter to get points, KDTree for uniqueness & queries
    val ptSet: MutableSet<Coordinate> = TreeSet()
    val pts = g.getCoordinates()
    for (i in pts.indices) {
      ptSet.add(pts[i])
    }
    return ptSet.toTypedArray()
  }

  /**
   * Computes the snap tolerance based on the input geometries.
   */
  private fun computeSnapTolerance(ringPts: Array<Coordinate>): Double {
    val minSegLen = computeMinimumSegmentLength(ringPts)
    // use a small percentage of this to be safe
    val snapTol = minSegLen / 10
    return snapTol
  }

  private fun computeMinimumSegmentLength(pts: Array<Coordinate>): Double {
    var minSegLen = Double.MAX_VALUE
    for (i in 0 until pts.size - 1) {
      val segLen = pts[i].distance(pts[i + 1])
      if (segLen < minSegLen)
        minSegLen = segLen
    }
    return minSegLen
  }

  companion object {
    private const val SNAP_PRECISION_FACTOR = 1e-9

    /**
     * Estimates the snap tolerance for a Geometry, taking into account its precision model.
     *
     * @param g a Geometry
     * @return the estimated snap tolerance
     */
    @JvmStatic
    fun computeOverlaySnapTolerance(g: Geometry): Double {
      var snapTolerance = computeSizeBasedSnapTolerance(g)

      /**
       * Overlay is carried out in the precision model
       * of the two inputs.
       * If this precision model is of type FIXED, then the snap tolerance
       * must reflect the precision grid size.
       */
      val pm = g.getPrecisionModel()
      if (pm.getType() == PrecisionModel.FIXED) {
        val fixedSnapTol = (1 / pm.getScale()) * 2 / 1.415
        if (fixedSnapTol > snapTolerance)
          snapTolerance = fixedSnapTol
      }
      return snapTolerance
    }

    @JvmStatic
    fun computeSizeBasedSnapTolerance(g: Geometry): Double {
      val env = g.getEnvelopeInternal()
      val minDimension = min(env.getHeight(), env.getWidth())
      val snapTol = minDimension * SNAP_PRECISION_FACTOR
      return snapTol
    }

    @JvmStatic
    fun computeOverlaySnapTolerance(g0: Geometry, g1: Geometry): Double {
      return min(computeOverlaySnapTolerance(g0), computeOverlaySnapTolerance(g1))
    }

    /**
     * Snaps two geometries together with a given tolerance.
     *
     * @param g0 a geometry to snap
     * @param g1 a geometry to snap
     * @param snapTolerance the tolerance to use
     * @return the snapped geometries
     */
    @JvmStatic
    fun snap(g0: Geometry, g1: Geometry, snapTolerance: Double): Array<Geometry> {
      val snapGeom = arrayOfNulls<Geometry>(2)
      val snapper0 = GeometrySnapper(g0)
      snapGeom[0] = snapper0.snapTo(g1, snapTolerance)

      /**
       * Snap the second geometry to the snapped first geometry
       * (this strategy minimizes the number of possible different points in the result)
       */
      val snapper1 = GeometrySnapper(g1)
      snapGeom[1] = snapper1.snapTo(snapGeom[0]!!, snapTolerance)

//    System.out.println(snap[0]);
//    System.out.println(snap[1]);
      @Suppress("UNCHECKED_CAST")
      return snapGeom as Array<Geometry>
    }

    /**
     * Snaps a geometry to itself.
     * Allows optionally cleaning the result to ensure it is
     * topologically valid
     * (which fixes issues such as topology collapses in polygonal inputs).
     *
     * @param geom the geometry to snap
     * @param snapTolerance the snapping tolerance
     * @param cleanResult whether the result should be made valid
     * @return a new snapped Geometry
     */
    @JvmStatic
    fun snapToSelf(geom: Geometry, snapTolerance: Double, cleanResult: Boolean): Geometry {
      val snapper0 = GeometrySnapper(geom)
      return snapper0.snapToSelf(snapTolerance, cleanResult)
    }
  }
}

internal class SnapTransformer : GeometryTransformer {
  private val snapTolerance: Double
  private val snapPts: Array<Coordinate>
  private var isSelfSnap = false

  constructor(snapTolerance: Double, snapPts: Array<Coordinate>) {
    this.snapTolerance = snapTolerance
    this.snapPts = snapPts
  }

  constructor(snapTolerance: Double, snapPts: Array<Coordinate>, isSelfSnap: Boolean) {
    this.snapTolerance = snapTolerance
    this.snapPts = snapPts
    this.isSelfSnap = isSelfSnap
  }

  override fun transformCoordinates(coords: CoordinateSequence, parent: Geometry?): CoordinateSequence? {
    val srcPts = coords.toCoordinateArray()
    val newPts = snapLine(srcPts, snapPts)
    return factory!!.getCoordinateSequenceFactory().create(newPts)
  }

  private fun snapLine(srcPts: Array<Coordinate>, snapPts: Array<Coordinate>): Array<Coordinate> {
    val snapper = LineStringSnapper(srcPts, snapTolerance)
    snapper.setAllowSnappingToSourceVertices(isSelfSnap)
    return snapper.snapTo(snapPts)
  }
}
