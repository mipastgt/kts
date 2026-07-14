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

import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.CoordinateSequence
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
import org.locationtech.jts.util.Assert

/**
 * A class which supports creating new [Geometry]s
 * which are modifications of existing ones,
 * maintaining the same type structure.
 *
 * @see GeometryTransformer
 * @see Geometry.isValid
 *
 */
open class GeometryEditor {
  /**
   * The factory used to create the modified Geometry.
   * If `null` the GeometryFactory of the input is used.
   */
  private var factory: GeometryFactory? = null
  private var isUserDataCopied = false

  /**
   * Creates a new GeometryEditor object which will create
   * edited [Geometry]s with the same [GeometryFactory] as the input Geometry.
   */
  constructor()

  /**
   * Creates a new GeometryEditor object which will create
   * edited [Geometry]s with the given [GeometryFactory].
   *
   * @param factory the GeometryFactory to create  edited Geometrys with
   */
  constructor(factory: GeometryFactory) {
    this.factory = factory
  }

  /**
   * Sets whether the User Data is copied to the edit result.
   * Only the object reference is copied.
   *
   * @param isUserDataCopied true if the input user data should be copied.
   */
  fun setCopyUserData(isUserDataCopied: Boolean) {
    this.isUserDataCopied = isUserDataCopied
  }

  /**
   * Edit the input [Geometry] with the given edit operation.
   * Clients can create subclasses of [GeometryEditorOperation] or
   * [CoordinateOperation] to perform required modifications.
   *
   * @param geometry the Geometry to edit
   * @param operation the edit operation to carry out
   * @return a new [Geometry] which is the result of the editing (which may be empty)
   */
  fun edit(geometry: Geometry?, operation: GeometryEditorOperation): Geometry? {
    // nothing to do
    if (geometry == null) return null

    val result = editInternal(geometry, operation)
    if (isUserDataCopied) {
      result!!.setUserData(geometry.getUserData())
    }
    return result
  }

  private fun editInternal(geometry: Geometry, operation: GeometryEditorOperation): Geometry? {
    // if client did not supply a GeometryFactory, use the one from the input Geometry
    if (factory == null)
      factory = geometry.getFactory()

    if (geometry is GeometryCollection) {
      return editGeometryCollection(geometry, operation)
    }

    if (geometry is Polygon) {
      return editPolygon(geometry, operation)
    }

    if (geometry is Point) {
      return operation.edit(geometry, factory!!)
    }

    if (geometry is LineString) {
      return operation.edit(geometry, factory!!)
    }

    Assert.shouldNeverReachHere("Unsupported Geometry class: " + geometry::class.simpleName)
    return null
  }

  private fun editPolygon(polygon: Polygon, operation: GeometryEditorOperation): Polygon {
    var newPolygon = operation.edit(polygon, factory!!) as Polygon?
    // create one if needed
    if (newPolygon == null)
      newPolygon = factory!!.createPolygon()
    if (newPolygon.isEmpty()) {
      //RemoveSelectedPlugIn relies on this behaviour. [Jon Aquino]
      return newPolygon
    }

    val shell = edit(newPolygon.getExteriorRing(), operation) as LinearRing?
    if (shell == null || shell.isEmpty()) {
      //RemoveSelectedPlugIn relies on this behaviour. [Jon Aquino]
      return factory!!.createPolygon()
    }

    val holes = ArrayList<LinearRing>()
    for (i in 0 until newPolygon.getNumInteriorRing()) {
      val hole = edit(newPolygon.getInteriorRingN(i), operation) as LinearRing?
      if (hole == null || hole.isEmpty()) {
        continue
      }
      holes.add(hole)
    }

    return factory!!.createPolygon(shell, holes.toTypedArray())
  }

  private fun editGeometryCollection(
    collection: GeometryCollection,
    operation: GeometryEditorOperation
  ): GeometryCollection {
    // first edit the entire collection
    // MD - not sure why this is done - could just check original collection?
    val collectionForType = operation.edit(collection, factory!!) as GeometryCollection

    // edit the component geometries
    val geometries = ArrayList<Geometry>()
    for (i in 0 until collectionForType.getNumGeometries()) {
      val geometry = edit(collectionForType.getGeometryN(i), operation)
      if (geometry == null || geometry.isEmpty()) {
        continue
      }
      geometries.add(geometry)
    }

    if (collectionForType::class == MultiPoint::class) {
      return factory!!.createMultiPoint(Array(geometries.size) { geometries[it] as Point })
    }
    if (collectionForType::class == MultiLineString::class) {
      return factory!!.createMultiLineString(Array(geometries.size) { geometries[it] as LineString })
    }
    if (collectionForType::class == MultiPolygon::class) {
      return factory!!.createMultiPolygon(Array(geometries.size) { geometries[it] as Polygon })
    }
    return factory!!.createGeometryCollection(geometries.toTypedArray())
  }

  /**
   * A interface which specifies an edit operation for Geometries.
   *
   */
  interface GeometryEditorOperation {
    /**
     * Edits a Geometry by returning a new Geometry with a modification.
     * The returned geometry may be:
     * 
     * - the input geometry itself.
     * - `null` if the geometry is to be deleted.
     * 
     *
     * @param geometry the Geometry to modify
     * @param factory the factory with which to construct the modified Geometry
     * @return a new Geometry which is a modification of the input Geometry
     * @return null if the Geometry is to be deleted completely
     */
    fun edit(geometry: Geometry, factory: GeometryFactory): Geometry?
  }

  /**
   * A GeometryEditorOperation which does not modify
   * the input geometry.
   * This can be used for simple changes of
   * GeometryFactory (including PrecisionModel and SRID).
   *
   * @author mbdavis
   *
   */
  class NoOpGeometryOperation : GeometryEditorOperation {
    override fun edit(geometry: Geometry, factory: GeometryFactory): Geometry {
      return geometry
    }
  }

  /**
   * A [GeometryEditorOperation] which edits the coordinate list of a [Geometry].
   * Operates on Geometry subclasses which contains a single coordinate list.
   */
  abstract class CoordinateOperation : GeometryEditorOperation {
    final override fun edit(geometry: Geometry, factory: GeometryFactory): Geometry? {
      if (geometry is LinearRing) {
        return factory.createLinearRing(edit(geometry.getCoordinates(), geometry))
      }

      if (geometry is LineString) {
        return factory.createLineString(edit(geometry.getCoordinates(), geometry))
      }

      if (geometry is Point) {
        val newCoordinates = edit(geometry.getCoordinates(), geometry)

        return factory.createPoint(
            if (newCoordinates!!.isNotEmpty()) newCoordinates[0] else null)
      }

      return geometry
    }

    /**
     * Edits the array of [Coordinate]s from a [Geometry].
     *
     * @param coordinates the coordinate array to operate on
     * @param geometry the geometry containing the coordinate list
     * @return an edited coordinate array (which may be the same as the input)
     */
    abstract fun edit(coordinates: Array<Coordinate>, geometry: Geometry): Array<Coordinate>?
  }

  /**
   * A [GeometryEditorOperation] which edits the [CoordinateSequence]
   * of a [Geometry].
   * Operates on Geometry subclasses which contains a single coordinate list.
   */
  abstract class CoordinateSequenceOperation : GeometryEditorOperation {
    final override fun edit(geometry: Geometry, factory: GeometryFactory): Geometry? {
      if (geometry is LinearRing) {
        return factory.createLinearRing(edit(geometry.getCoordinateSequence(), geometry))
      }

      if (geometry is LineString) {
        return factory.createLineString(edit(geometry.getCoordinateSequence(), geometry))
      }

      if (geometry is Point) {
        return factory.createPoint(edit(geometry.getCoordinateSequence(), geometry))
      }

      return geometry
    }

    /**
     * Edits a [CoordinateSequence] from a [Geometry].
     *
     * @param coordSeq the coordinate array to operate on
     * @param geometry the geometry containing the coordinate list
     * @return an edited coordinate sequence (which may be the same as the input)
     */
    abstract fun edit(coordSeq: CoordinateSequence, geometry: Geometry): CoordinateSequence
  }
}
