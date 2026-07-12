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

import kotlin.jvm.JvmField

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

/**
 * A framework for processes which transform an input [Geometry] into
 * an output [Geometry], possibly changing its structure and type(s).
 * This class is a framework for implementing subclasses
 * which perform transformations on
 * various different Geometry subclasses.
 *
 * All <code>transformX</code> methods may return <code>null</code>,
 * to avoid creating empty or invalid geometry objects. This will be handled correctly
 * by the transformer.
 * The [transform] method itself will always
 * return a non-null Geometry object (but this may be empty).
 *
 * @version 1.7
 *
 * @see GeometryEditor
 */
open class GeometryTransformer {

  /**
   * Possible extensions:
   * getParent() method to return immediate parent e.g. of LinearRings in Polygons
   */

  private var inputGeom: Geometry? = null

  @JvmField
  protected var factory: GeometryFactory? = null

  // these could eventually be exposed to clients
  /**
   * <code>true</code> if empty geometries should not be included in the result
   */
  private val pruneEmptyGeometry = true

  /**
   * <code>true</code> if a homogenous collection result
   * from a [GeometryCollection] should still
   * be a general GeometryCollection
   */
  private val preserveGeometryCollectionType = true

  /**
   * <code>true</code> if the output from a collection argument should still be a collection
   */
  private val preserveCollections = false

  /**
   * <code>true</code> if the type of the input should be preserved
   */
  private val preserveType = false

  /**
   * Utility function to make input geometry available
   *
   * @return the input geometry
   */
  fun getInputGeometry(): Geometry? {
    return inputGeom
  }

  fun transform(inputGeom: Geometry): Geometry? {
    this.inputGeom = inputGeom
    this.factory = inputGeom.getFactory()

    if (inputGeom is Point)
      return transformPoint(inputGeom, null)
    if (inputGeom is MultiPoint)
      return transformMultiPoint(inputGeom, null)
    if (inputGeom is LinearRing)
      return transformLinearRing(inputGeom, null)
    if (inputGeom is LineString)
      return transformLineString(inputGeom, null)
    if (inputGeom is MultiLineString)
      return transformMultiLineString(inputGeom, null)
    if (inputGeom is Polygon)
      return transformPolygon(inputGeom, null)
    if (inputGeom is MultiPolygon)
      return transformMultiPolygon(inputGeom, null)
    if (inputGeom is GeometryCollection)
      return transformGeometryCollection(inputGeom, null)

    throw IllegalArgumentException("Unknown Geometry subtype: " + inputGeom::class.simpleName)
  }

  /**
   * Convenience method which provides standard way of
   * creating a [CoordinateSequence]
   *
   * @param coords the coordinate array to copy
   * @return a coordinate sequence for the array
   */
  protected fun createCoordinateSequence(coords: Array<Coordinate>): CoordinateSequence {
    return factory!!.getCoordinateSequenceFactory().create(coords)
  }

  /**
   * Convenience method which provides a standard way of copying [CoordinateSequence]s
   * @param seq the sequence to copy
   * @return a deep copy of the sequence
   */
  protected fun copy(seq: CoordinateSequence): CoordinateSequence {
    return seq.copy()
  }

  /**
   * Transforms a [CoordinateSequence].
   * This method should always return a valid coordinate list for
   * the desired result type.  (E.g. a coordinate list for a LineString
   * must have 0 or at least 2 points).
   * If this is not possible, return an empty sequence -
   * this will be pruned out.
   *
   * @param coords the coordinates to transform
   * @param parent the parent geometry
   * @return the transformed coordinates
   */
  protected open fun transformCoordinates(coords: CoordinateSequence, parent: Geometry?): CoordinateSequence? {
    return copy(coords)
  }

  protected open fun transformPoint(geom: Point, parent: Geometry?): Geometry? {
    return factory!!.createPoint(
        transformCoordinates(geom.getCoordinateSequence(), geom))
  }

  protected open fun transformMultiPoint(geom: MultiPoint, parent: Geometry?): Geometry? {
    val transGeomList = ArrayList<Geometry>()
    for (i in 0 until geom.getNumGeometries()) {
      val transformGeom = transformPoint(geom.getGeometryN(i) as Point, geom)
      if (transformGeom == null) continue
      if (transformGeom.isEmpty()) continue
      transGeomList.add(transformGeom)
    }
    if (transGeomList.isEmpty()) {
      return factory!!.createMultiPoint()
    }
    return factory!!.buildGeometry(transGeomList)
  }

  /**
   * Transforms a LinearRing.
   * The transformation of a LinearRing may result in a coordinate sequence
   * which does not form a structurally valid ring (i.e. a degenerate ring of 3 or fewer points).
   * In this case a LineString is returned.
   * Subclasses may wish to override this method and check for this situation
   * (e.g. a subclass may choose to eliminate degenerate linear rings)
   *
   * @param geom the ring to simplify
   * @param parent the parent geometry
   * @return a LinearRing if the transformation resulted in a structurally valid ring
   * @return a LineString if the transformation caused the LinearRing to collapse to 3 or fewer points
   */
  protected open fun transformLinearRing(geom: LinearRing, parent: Geometry?): Geometry? {
    val seq = transformCoordinates(geom.getCoordinateSequence(), geom)
    if (seq == null)
      return factory!!.createLinearRing(null as CoordinateSequence?)
    val seqSize = seq.size()
    // ensure a valid LinearRing
    if (seqSize > 0 && seqSize < 4 && !preserveType)
      return factory!!.createLineString(seq)
    return factory!!.createLinearRing(seq)
  }

  /**
   * Transforms a [LineString] geometry.
   *
   * @param geom
   * @param parent
   * @return
   */
  protected open fun transformLineString(geom: LineString, parent: Geometry?): Geometry? {
    // should check for 1-point sequences and downgrade them to points
    return factory!!.createLineString(
        transformCoordinates(geom.getCoordinateSequence(), geom))
  }

  protected open fun transformMultiLineString(geom: MultiLineString, parent: Geometry?): Geometry? {
    val transGeomList = ArrayList<Geometry>()
    for (i in 0 until geom.getNumGeometries()) {
      val transformGeom = transformLineString(geom.getGeometryN(i) as LineString, geom)
      if (transformGeom == null) continue
      if (transformGeom.isEmpty()) continue
      transGeomList.add(transformGeom)
    }
    if (transGeomList.isEmpty()) {
      return factory!!.createMultiLineString()
    }
    return factory!!.buildGeometry(transGeomList)
  }

  protected open fun transformPolygon(geom: Polygon, parent: Geometry?): Geometry? {
    var isAllValidLinearRings = true
    val shell = transformLinearRing(geom.getExteriorRing(), geom)

    // handle empty inputs, or inputs which are made empty
    val shellIsNullOrEmpty = shell == null || shell.isEmpty()
    if (geom.isEmpty() && shellIsNullOrEmpty) {
      return factory!!.createPolygon()
    }

    if (shellIsNullOrEmpty || shell !is LinearRing)
      isAllValidLinearRings = false

    val holes = ArrayList<Geometry>()
    for (i in 0 until geom.getNumInteriorRing()) {
      val hole = transformLinearRing(geom.getInteriorRingN(i), geom)
      if (hole == null || hole.isEmpty()) {
        continue
      }
      if (hole !is LinearRing)
        isAllValidLinearRings = false

      holes.add(hole)
    }

    if (isAllValidLinearRings)
      return factory!!.createPolygon(shell as LinearRing?,
          Array(holes.size) { holes[it] as LinearRing })
    else {
      val components = ArrayList<Geometry>()
      if (shell != null) components.add(shell)
      components.addAll(holes)
      return factory!!.buildGeometry(components)
    }
  }

  protected open fun transformMultiPolygon(geom: MultiPolygon, parent: Geometry?): Geometry? {
    val transGeomList = ArrayList<Geometry>()
    for (i in 0 until geom.getNumGeometries()) {
      val transformGeom = transformPolygon(geom.getGeometryN(i) as Polygon, geom)
      if (transformGeom == null) continue
      if (transformGeom.isEmpty()) continue
      transGeomList.add(transformGeom)
    }
    if (transGeomList.isEmpty()) {
      return factory!!.createMultiPolygon()
    }
    return factory!!.buildGeometry(transGeomList)
  }

  protected open fun transformGeometryCollection(geom: GeometryCollection, parent: Geometry?): Geometry? {
    val transGeomList = ArrayList<Geometry>()
    for (i in 0 until geom.getNumGeometries()) {
      val transformGeom = transform(geom.getGeometryN(i))
      if (transformGeom == null) continue
      if (pruneEmptyGeometry && transformGeom.isEmpty()) continue
      transGeomList.add(transformGeom)
    }
    if (preserveGeometryCollectionType)
      return factory!!.createGeometryCollection(GeometryFactory.toGeometryArray(transGeomList))
    return factory!!.buildGeometry(transGeomList)
  }

}
