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

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.CoordinateSequence
import org.locationtech.jts.geom.CoordinateSequenceFilter
import org.locationtech.jts.geom.Envelope
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.math.MathUtil

/**
 * A simple elevation model used to populate missing Z values
 * in overlay results.
 *
 * @author Martin Davis
 */
class ElevationModel
/**
 * Creates a new elevation model covering an extent by a grid of given dimensions.
 *
 * @param extent the XY extent to cover
 * @param numCellX the number of grid cells in the X dimension
 * @param numCellY the number of grid cells in the Y dimension
 */
(private val extent: Envelope, numCellXArg: Int, numCellYArg: Int) {

  private var numCellX = numCellXArg
  private var numCellY = numCellYArg
  private var cellSizeX = extent.getWidth() / numCellXArg
  private var cellSizeY = extent.getHeight() / numCellYArg
  private val cells: Array<Array<ElevationCell?>>
  private var isInitialized = false
  private var hasZValue = false
  private var averageZ = Double.NaN

  init {
    if (cellSizeX <= 0.0) {
      this.numCellX = 1
    }
    if (cellSizeY <= 0.0) {
      this.numCellY = 1
    }
    cells = Array(numCellXArg) { arrayOfNulls<ElevationCell>(numCellYArg) }
  }

  /**
   * Updates the model using the Z values of a given geometry.
   *
   * @param geom the geometry to scan for Z values
   */
  fun add(geom: Geometry) {
    geom.apply(object : CoordinateSequenceFilter {

      private var hasZ = true

      override fun filter(seq: CoordinateSequence, i: Int) {
        if (!seq.hasZ()) {
          hasZ = false
          return
        }
        val z = seq.getOrdinate(i, Coordinate.Z)
        add(
          seq.getOrdinate(i, Coordinate.X),
          seq.getOrdinate(i, Coordinate.Y),
          z
        )
      }

      override fun isDone(): Boolean {
        // no need to scan if no Z present
        return !hasZ
      }

      override fun isGeometryChanged(): Boolean {
        return false
      }
    })
  }

  protected fun add(x: Double, y: Double, z: Double) {
    if (z.isNaN())
      return
    hasZValue = true
    val cell = getCell(x, y, true)!!
    cell.add(z)
  }

  private fun init() {
    isInitialized = true
    var numCells = 0
    var sumZ = 0.0

    for (i in cells.indices) {
      for (j in cells[0].indices) {
        val cell = cells[i][j]
        if (cell != null) {
          cell.compute()
          numCells++
          sumZ += cell.getZ()
        }
      }
    }
    averageZ = Double.NaN
    if (numCells > 0) {
      averageZ = sumZ / numCells
    }
  }

  /**
   * Gets the model Z value at a given location.
   *
   * @param x the x ordinate of the location
   * @param y the y ordinate of the location
   * @return the computed model Z value
   */
  fun getZ(x: Double, y: Double): Double {
    if (!isInitialized)
      init()
    val cell = getCell(x, y, false)
    if (cell == null)
      return averageZ
    return cell.getZ()
  }

  /**
   * Computes Z values for any missing Z values in a geometry,
   * using the computed model.
   *
   * @param geom the geometry to populate Z values for
   */
  fun populateZ(geom: Geometry) {
    // short-circuit if no Zs are present in model
    if (!hasZValue)
      return

    if (!isInitialized)
      init()

    geom.apply(object : CoordinateSequenceFilter {

      private var done = false

      override fun filter(seq: CoordinateSequence, i: Int) {
        if (!seq.hasZ()) {
          // if no Z then short-circuit evaluation
          done = true
          return
        }
        // if Z not populated then assign using model
        if (seq.getZ(i).isNaN()) {
          val z = getZ(
            seq.getOrdinate(i, Coordinate.X),
            seq.getOrdinate(i, Coordinate.Y)
          )
          seq.setOrdinate(i, Coordinate.Z, z)
        }
      }

      override fun isDone(): Boolean {
        return done
      }

      override fun isGeometryChanged(): Boolean {
        // geometry extent is not changed
        return false
      }
    })
  }

  private fun getCell(x: Double, y: Double, isCreateIfMissing: Boolean): ElevationCell? {
    var ix = 0
    if (numCellX > 1) {
      ix = ((x - extent.getMinX()) / cellSizeX).toInt()
      ix = MathUtil.clamp(ix, 0, numCellX - 1)
    }
    var iy = 0
    if (numCellY > 1) {
      iy = ((y - extent.getMinY()) / cellSizeY).toInt()
      iy = MathUtil.clamp(iy, 0, numCellY - 1)
    }
    var cell = cells[ix][iy]
    if (isCreateIfMissing && cell == null) {
      cell = ElevationCell()
      cells[ix][iy] = cell
    }
    return cell
  }

  class ElevationCell {

    private var numZ = 0
    private var sumZ = 0.0
    private var avgZ = 0.0

    fun add(z: Double) {
      numZ++
      sumZ += z
    }

    fun compute() {
      avgZ = Double.NaN
      if (numZ > 0)
        avgZ = sumZ / numZ
    }

    fun getZ(): Double {
      return avgZ
    }
  }

  companion object {
    private const val DEFAULT_CELL_NUM = 3

    /**
     * Creates an elevation model from two geometries (which may be null).
     *
     * @param geom1 an input geometry
     * @param geom2 an input geometry, or null
     * @return the elevation model computed from the geometries
     */
    @JvmStatic
    fun create(geom1: Geometry, geom2: Geometry?): ElevationModel {
      val extent = geom1.getEnvelopeInternal().copy()
      if (geom2 != null) {
        extent.expandToInclude(geom2.getEnvelopeInternal())
      }
      val model = ElevationModel(extent, DEFAULT_CELL_NUM, DEFAULT_CELL_NUM)
      model.add(geom1)
      if (geom2 != null) model.add(geom2)
      return model
    }
  }
}
