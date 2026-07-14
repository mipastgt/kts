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
package org.locationtech.jts.precision

import kotlin.jvm.JvmStatic

import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel
import org.locationtech.jts.geom.util.GeometryEditor

/**
 * Reduces the precision of a [Geometry]
 * according to the supplied [PrecisionModel],
 * ensuring that the result is valid (unless specified otherwise).
 *
 */
class GeometryPrecisionReducer(private val targetPM: PrecisionModel) {

  private var removeCollapsed = true
  private var changePrecisionModel = false
  private var isPointwise = false

  /**
   * Sets whether the reduction will result in collapsed components
   * being removed completely, or simply being collapsed to an (invalid)
   * Geometry of the same type.
   * The default is to remove collapsed components.
   *
   * @param removeCollapsed if `true` collapsed components will be removed
   */
  fun setRemoveCollapsedComponents(removeCollapsed: Boolean) {
    this.removeCollapsed = removeCollapsed
  }

  /**
   * Sets whether the [PrecisionModel] of the new reduced Geometry
   * will be changed to be the [PrecisionModel] supplied to
   * specify the precision reduction.
   *
   * The default is to **not** change the precision model
   *
   * @param changePrecisionModel if `true` the precision model of the created Geometry will be
   * the precisionModel supplied in the constructor.
   */
  fun setChangePrecisionModel(changePrecisionModel: Boolean) {
    this.changePrecisionModel = changePrecisionModel
  }

  /**
   * Sets whether the precision reduction will be done
   * in pointwise fashion only.
   *
   * @param isPointwise if reduction should be done pointwise only
   */
  fun setPointwise(isPointwise: Boolean) {
    this.isPointwise = isPointwise
  }

  /**
   * Reduces the precision of a geometry,
   * according to the specified strategy of this reducer.
   *
   * @param geom the geometry to reduce
   * @return the precision-reduced geometry
   * @throws IllegalArgumentException if the reduction fails due to invalid input geometry is invalid
   */
  fun reduce(geom: Geometry): Geometry {
    val reduced: Geometry
    if (isPointwise) {
      reduced = PointwisePrecisionReducerTransformer.reduce(geom, targetPM)
    } else {
      reduced = PrecisionReducerTransformer.reduce(geom, targetPM, removeCollapsed)
    }

    // TODO: incorporate this in the Transformer above
    if (changePrecisionModel) {
      return changePM(reduced, targetPM)
    }
    return reduced
  }

  /**
   * Duplicates a geometry to one that uses a different PrecisionModel,
   * without changing any coordinate values.
   *
   * @param geom the geometry to duplicate
   * @param newPM the precision model to use
   * @return the geometry value with a new precision model
   */
  private fun changePM(geom: Geometry, newPM: PrecisionModel): Geometry {
    val geomEditor = createEditor(geom.getFactory(), newPM)
    // this operation changes the PM for the entire geometry tree
    return geomEditor.edit(geom, GeometryEditor.NoOpGeometryOperation())!!
  }

  private fun createEditor(geomFactory: GeometryFactory, newPM: PrecisionModel): GeometryEditor {
    // no need to change if precision model is the same
    if (geomFactory.getPrecisionModel() === newPM)
      return GeometryEditor()
    // otherwise create a geometry editor which changes PrecisionModel
    val newFactory = createFactory(geomFactory, newPM)
    val geomEdit = GeometryEditor(newFactory)
    return geomEdit
  }

  private fun createFactory(inputFactory: GeometryFactory, pm: PrecisionModel): GeometryFactory {
    val newFactory = GeometryFactory(pm,
        inputFactory.getSRID(),
        inputFactory.getCoordinateSequenceFactory())
    return newFactory
  }

  companion object {
    /**
     * Reduces precision of a geometry, ensuring output geometry is valid.
     * Collapsed linear and polygonal components are removed.
     *
     * @param g the geometry to reduce
     * @param precModel the precision model to use
     * @return the reduced geometry
     * @throws IllegalArgumentException if the reduction fails due to invalid input geometry
     */
    @JvmStatic
    fun reduce(g: Geometry, precModel: PrecisionModel): Geometry {
      val reducer = GeometryPrecisionReducer(precModel)
      return reducer.reduce(g)
    }

    /**
     * Reduces precision of a geometry, ensuring output polygonal geometry is valid,
     * and preserving collapsed linear elements.
     *
     * @param geom the geometry to reduce
     * @param pm the precision model to use
     * @return the reduced geometry
     * @throws IllegalArgumentException if the reduction fails due to invalid input geometry
     */
    @JvmStatic
    fun reduceKeepCollapsed(geom: Geometry, pm: PrecisionModel): Geometry {
      val reducer = GeometryPrecisionReducer(pm)
      reducer.setRemoveCollapsedComponents(false)
      return reducer.reduce(geom)
    }

    /**
     * Reduce precision of a geometry in a pointwise way.
     * All input geometry elements are preserved in the output.
     *
     * @param g the geometry to reduce
     * @param precModel the precision model to use
     * @return the reduced geometry
     */
    @JvmStatic
    fun reducePointwise(g: Geometry, precModel: PrecisionModel): Geometry {
      val reducer = GeometryPrecisionReducer(precModel)
      reducer.setPointwise(true)
      return reducer.reduce(g)
    }
  }
}
