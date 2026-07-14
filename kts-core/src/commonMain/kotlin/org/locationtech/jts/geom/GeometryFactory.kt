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
package org.locationtech.jts.geom

import kotlin.jvm.JvmStatic


import org.locationtech.jts.geom.impl.CoordinateArraySequenceFactory
import org.locationtech.jts.geom.util.GeometryEditor
import org.locationtech.jts.util.Assert
import kotlin.reflect.KClass

/**
 * Supplies a set of utility methods for building Geometry objects from lists
 * of Coordinates.
 * 
 * Instances of this class are thread-safe.
 *
 */
open class GeometryFactory(
  precisionModel: PrecisionModel,
  SRID: Int,
  coordinateSequenceFactory: CoordinateSequenceFactory
) {

  private val precisionModel: PrecisionModel = precisionModel

  private val coordinateSequenceFactory: CoordinateSequenceFactory = coordinateSequenceFactory

  private var SRID: Int = SRID

  /**
   * Constructs a GeometryFactory that generates Geometries having the given
   * CoordinateSequence implementation, a double-precision floating PrecisionModel and a
   * spatial-reference ID of 0.
   */
  constructor(coordinateSequenceFactory: CoordinateSequenceFactory) : this(PrecisionModel(), 0, coordinateSequenceFactory)

  /**
   * Constructs a GeometryFactory that generates Geometries having the given
   * [PrecisionModel] and the default CoordinateSequence
   * implementation.
   *
   * @param precisionModel the PrecisionModel to use
   */
  constructor(precisionModel: PrecisionModel) : this(precisionModel, 0, getDefaultCoordinateSequenceFactory())

  /**
   * Constructs a GeometryFactory that generates Geometries having the given
   * [PrecisionModel] and spatial-reference ID, and the default CoordinateSequence
   * implementation.
   *
   * @param precisionModel the PrecisionModel to use
   * @param SRID the SRID to use
   */
  constructor(precisionModel: PrecisionModel, SRID: Int) : this(precisionModel, SRID, getDefaultCoordinateSequenceFactory())

  /**
   * Constructs a GeometryFactory that generates Geometries having a floating
   * PrecisionModel and a spatial-reference ID of 0.
   */
  constructor() : this(PrecisionModel(), 0)

  /**
   * Creates a [Geometry] with the same extent as the given envelope.
   *
   * @param  envelope the `Envelope` to convert
   * @return an empty `Point` (for null `Envelope`s),
   *	a `Point` (when min x = max x and min y = max y) or a
   *      `Polygon` (in all other cases)
   */
  fun toGeometry(envelope: Envelope): Geometry {
    // null envelope - return empty point geometry
    if (envelope.isNull()) {
      return createPoint()
    }

    // point?
    if (envelope.getMinX() == envelope.getMaxX() && envelope.getMinY() == envelope.getMaxY()) {
      return createPoint(Coordinate(envelope.getMinX(), envelope.getMinY()))
    }

    // vertical or horizontal line?
    if (envelope.getMinX() == envelope.getMaxX() || envelope.getMinY() == envelope.getMaxY()) {
      return createLineString(
        arrayOf(
          Coordinate(envelope.getMinX(), envelope.getMinY()),
          Coordinate(envelope.getMaxX(), envelope.getMaxY())
        )
      )
    }

    // create a CW ring for the polygon
    return createPolygon(
      createLinearRing(
        arrayOf(
          Coordinate(envelope.getMinX(), envelope.getMinY()),
          Coordinate(envelope.getMinX(), envelope.getMaxY()),
          Coordinate(envelope.getMaxX(), envelope.getMaxY()),
          Coordinate(envelope.getMaxX(), envelope.getMinY()),
          Coordinate(envelope.getMinX(), envelope.getMinY())
        )
      ), null
    )
  }

  /**
   * Returns the PrecisionModel that Geometries created by this factory
   * will be associated with.
   *
   * @return the PrecisionModel for this factory
   */
  fun getPrecisionModel(): PrecisionModel {
    return precisionModel
  }

  /**
   * Constructs an empty [Point] geometry.
   *
   * @return an empty Point
   */
  fun createPoint(): Point {
    return createPoint(getCoordinateSequenceFactory().create(arrayOf<Coordinate>()))
  }

  /**
   * Creates a Point using the given Coordinate.
   * A null Coordinate creates an empty Geometry.
   *
   * @param coordinate a Coordinate, or null
   * @return the created Point
   */
  fun createPoint(coordinate: Coordinate?): Point {
    return createPoint(if (coordinate != null) getCoordinateSequenceFactory().create(arrayOf(coordinate)) else null)
  }

  /**
   * Creates a Point using the given CoordinateSequence; a null or empty
   * CoordinateSequence will create an empty Point.
   *
   * @param coordinates a CoordinateSequence (possibly empty), or null
   * @return the created Point
   */
  fun createPoint(coordinates: CoordinateSequence?): Point {
    return Point(coordinates, this)
  }

  /**
   * Constructs an empty [MultiLineString] geometry.
   *
   * @return an empty MultiLineString
   */
  fun createMultiLineString(): MultiLineString {
    return MultiLineString(null, this)
  }

  /**
   * Creates a MultiLineString using the given LineStrings; a null or empty
   * array will create an empty MultiLineString.
   *
   * @param lineStrings LineStrings, each of which may be empty but not null
   * @return the created MultiLineString
   */
  fun createMultiLineString(lineStrings: Array<LineString>?): MultiLineString {
    return MultiLineString(lineStrings, this)
  }

  /**
   * Constructs an empty [GeometryCollection] geometry.
   *
   * @return an empty GeometryCollection
   */
  fun createGeometryCollection(): GeometryCollection {
    return GeometryCollection(null, this)
  }

  /**
   * Creates a GeometryCollection using the given Geometries; a null or empty
   * array will create an empty GeometryCollection.
   *
   * @param geometries an array of Geometries, each of which may be empty but not null, or null
   * @return the created GeometryCollection
   */
  fun createGeometryCollection(geometries: Array<Geometry>?): GeometryCollection {
    return GeometryCollection(geometries, this)
  }

  /**
   * Constructs an empty [MultiPolygon] geometry.
   *
   * @return an empty MultiPolygon
   */
  fun createMultiPolygon(): MultiPolygon {
    return MultiPolygon(null, this)
  }

  /**
   * Creates a MultiPolygon using the given Polygons; a null or empty array
   * will create an empty Polygon.
   *
   * @param polygons
   *            Polygons, each of which may be empty but not null
   * @return the created MultiPolygon
   */
  fun createMultiPolygon(polygons: Array<Polygon>?): MultiPolygon {
    return MultiPolygon(polygons, this)
  }

  /**
   * Constructs an empty [LinearRing] geometry.
   *
   * @return an empty LinearRing
   */
  fun createLinearRing(): LinearRing {
    return createLinearRing(getCoordinateSequenceFactory().create(arrayOf<Coordinate>()))
  }

  /**
   * Creates a [LinearRing] using the given [Coordinate]s.
   * A null or empty array creates an empty LinearRing.
   *
   * @param coordinates an array without null elements, or an empty array, or null
   * @return the created LinearRing
   * @throws IllegalArgumentException if the ring is not closed, or has too few points
   */
  fun createLinearRing(coordinates: Array<Coordinate>?): LinearRing {
    return createLinearRing(if (coordinates != null) getCoordinateSequenceFactory().create(coordinates) else null)
  }

  /**
   * Creates a [LinearRing] using the given [CoordinateSequence].
   * A null or empty array creates an empty LinearRing.
   *
   * @param coordinates a CoordinateSequence (possibly empty), or null
   * @return the created LinearRing
   * @throws IllegalArgumentException if the ring is not closed, or has too few points
   */
  fun createLinearRing(coordinates: CoordinateSequence?): LinearRing {
    return LinearRing(coordinates, this)
  }

  /**
   * Constructs an empty [MultiPoint] geometry.
   *
   * @return an empty MultiPoint
   */
  fun createMultiPoint(): MultiPoint {
    return MultiPoint(null as Array<Point>?, this)
  }

  /**
   * Creates a [MultiPoint] using the given [Point]s.
   * A null or empty array will create an empty MultiPoint.
   *
   * @param point an array of Points (without null elements), or an empty array, or `null`
   * @return a MultiPoint object
   */
  fun createMultiPoint(point: Array<Point>?): MultiPoint {
    return MultiPoint(point, this)
  }

  /**
   * Creates a [MultiPoint] using the given [Coordinate]s.
   * A null or empty array will create an empty MultiPoint.
   *
   * @param coordinates an array (without null elements), or an empty array, or `null`
   * @return a MultiPoint object
   * @deprecated Use [GeometryFactory.createMultiPointFromCoords] instead
   */
  fun createMultiPoint(coordinates: Array<Coordinate>?): MultiPoint {
    return createMultiPoint(
      if (coordinates != null) getCoordinateSequenceFactory().create(coordinates) else null
    )
  }

  /**
   * Creates a [MultiPoint] using the given [Coordinate]s.
   * A null or empty array will create an empty MultiPoint.
   *
   * @param coordinates an array (without null elements), or an empty array, or `null`
   * @return a MultiPoint object
   */
  fun createMultiPointFromCoords(coordinates: Array<Coordinate>?): MultiPoint {
    return createMultiPoint(
      if (coordinates != null) getCoordinateSequenceFactory().create(coordinates) else null
    )
  }

  /**
   * Creates a [MultiPoint] using the
   * points in the given [CoordinateSequence].
   * A `null` or empty CoordinateSequence creates an empty MultiPoint.
   *
   * @param coordinates a CoordinateSequence (possibly empty), or `null`
   * @return a MultiPoint geometry
   */
  fun createMultiPoint(coordinates: CoordinateSequence?): MultiPoint {
    if (coordinates == null) {
      return createMultiPoint(arrayOf<Point>())
    }
    val points = arrayOfNulls<Point>(coordinates.size())
    for (i in 0 until coordinates.size()) {
      val ptSeq = getCoordinateSequenceFactory()
        .create(1, coordinates.getDimension(), coordinates.getMeasures())
      CoordinateSequences.copy(coordinates, i, ptSeq, 0, 1)
      points[i] = createPoint(ptSeq)
    }
    @Suppress("UNCHECKED_CAST")
    return createMultiPoint(points as Array<Point>)
  }

  /**
   * Constructs a `Polygon` with the given exterior boundary and
   * interior boundaries.
   *
   * @throws IllegalArgumentException if a ring is invalid
   */
  fun createPolygon(shell: LinearRing?, holes: Array<LinearRing>?): Polygon {
    return Polygon(shell, holes, this)
  }

  /**
   * Constructs a `Polygon` with the given exterior boundary.
   *
   * @throws IllegalArgumentException if the boundary ring is invalid
   */
  fun createPolygon(shell: CoordinateSequence?): Polygon {
    return createPolygon(createLinearRing(shell))
  }

  /**
   * Constructs a `Polygon` with the given exterior boundary.
   *
   * @throws IllegalArgumentException if the boundary ring is invalid
   */
  fun createPolygon(shell: Array<Coordinate>?): Polygon {
    return createPolygon(createLinearRing(shell))
  }

  /**
   * Constructs a `Polygon` with the given exterior boundary.
   *
   * @throws IllegalArgumentException if the boundary ring is invalid
   */
  fun createPolygon(shell: LinearRing?): Polygon {
    return createPolygon(shell, null)
  }

  /**
   * Constructs an empty [Polygon] geometry.
   *
   * @return an empty polygon
   */
  fun createPolygon(): Polygon {
    return createPolygon(null as LinearRing?, null)
  }

  /**
   *  Build an appropriate `Geometry`, `MultiGeometry`, or
   *  `GeometryCollection` to contain the `Geometry`s in
   *  it.
   *
   * @param  geomList  the `Geometry`s to combine
   * @return           a `Geometry` of the "smallest", "most
   *      type-specific" class that can contain the elements of `geomList`
   *      .
   */
  fun buildGeometry(geomList: Collection<*>): Geometry {

    /**
     * Determine some facts about the geometries in the list
     */
    var geomClass: KClass<*>? = null
    var isHeterogeneous = false
    var hasGeometryCollection = false
    val i = geomList.iterator()
    while (i.hasNext()) {
      val geom = i.next() as Geometry
      val partClass: KClass<*> = geom::class
      if (geomClass == null) {
        geomClass = partClass
      }
      if (partClass != geomClass) {
        isHeterogeneous = true
      }
      if (geom is GeometryCollection)
        hasGeometryCollection = true
    }

    /**
     * Now construct an appropriate geometry to return
     */
    // for the empty geometry, return an empty GeometryCollection
    if (geomClass == null) {
      return createGeometryCollection()
    }
    if (isHeterogeneous || hasGeometryCollection) {
      return createGeometryCollection(toGeometryArray(geomList))
    }
    // Determine the type of the result from the first Geometry in the list
    val geom0 = geomList.iterator().next() as Geometry
    val isCollection = geomList.size > 1
    if (isCollection) {
      if (geom0 is Polygon) {
        return createMultiPolygon(toPolygonArray(geomList))
      } else if (geom0 is LineString) {
        return createMultiLineString(toLineStringArray(geomList))
      } else if (geom0 is Point) {
        return createMultiPoint(toPointArray(geomList))
      }
      Assert.shouldNeverReachHere("Unhandled class: " + geom0::class.simpleName)
    }
    return geom0
  }

  /**
   * Constructs an empty [LineString] geometry.
   *
   * @return an empty LineString
   */
  fun createLineString(): LineString {
    return createLineString(getCoordinateSequenceFactory().create(arrayOf<Coordinate>()))
  }

  /**
   * Creates a LineString using the given Coordinates.
   * A null or empty array creates an empty LineString.
   *
   * @param coordinates an array without null elements, or an empty array, or null
   */
  fun createLineString(coordinates: Array<Coordinate>?): LineString {
    return createLineString(if (coordinates != null) getCoordinateSequenceFactory().create(coordinates) else null)
  }

  /**
   * Creates a LineString using the given CoordinateSequence.
   * A null or empty CoordinateSequence creates an empty LineString.
   *
   * @param coordinates a CoordinateSequence (possibly empty), or null
   */
  fun createLineString(coordinates: CoordinateSequence?): LineString {
    return LineString(coordinates, this)
  }

  /**
   * Creates an empty atomic geometry of the given dimension.
   * If passed a dimension of -1 will create an empty [GeometryCollection].
   *
   * @param dimension the required dimension (-1, 0, 1 or 2)
   * @return an empty atomic geometry of given dimension
   */
  fun createEmpty(dimension: Int): Geometry {
    return when (dimension) {
      -1 -> createGeometryCollection()
      0 -> createPoint()
      1 -> createLineString()
      2 -> createPolygon()
      else -> throw IllegalArgumentException("Invalid dimension: " + dimension)
    }
  }

  /**
   * Creates a deep copy of the input [Geometry].
   *
   * @return a deep copy of the input geometry, using the CoordinateSequence type of this factory
   *
   * @see Geometry#copy()
   */
  fun createGeometry(g: Geometry): Geometry {
    val editor = GeometryEditor(this)
    return editor.edit(g, CoordSeqCloneOp(coordinateSequenceFactory))!!
  }

  private class CoordSeqCloneOp(val coordinateSequenceFactory: CoordinateSequenceFactory) : GeometryEditor.CoordinateSequenceOperation() {
    override fun edit(coordSeq: CoordinateSequence, geometry: Geometry): CoordinateSequence {
      return coordinateSequenceFactory.create(coordSeq)
    }
  }

  /**
   * Gets the SRID value defined for this factory.
   *
   * @return the factory SRID value
   */
  fun getSRID(): Int {
    return SRID
  }

  fun getCoordinateSequenceFactory(): CoordinateSequenceFactory {
    return coordinateSequenceFactory
  }

  companion object {

    @JvmStatic
    fun createPointFromInternalCoord(coord: Coordinate, exemplar: Geometry): Point {
      exemplar.getPrecisionModel().makePrecise(coord)
      return exemplar.getFactory().createPoint(coord)
    }

    private fun getDefaultCoordinateSequenceFactory(): CoordinateSequenceFactory {
      return CoordinateArraySequenceFactory.instance()
    }

    /**
     *  Converts the `List` to an array.
     */
    @JvmStatic
    fun toPointArray(points: Collection<*>): Array<Point> {
      @Suppress("UNCHECKED_CAST")
      return (points as Collection<Point>).toTypedArray()
    }

    /**
     *  Converts the `List` to an array.
     */
    @JvmStatic
    fun toGeometryArray(geometries: Collection<*>?): Array<Geometry>? {
      if (geometries == null) return null
      @Suppress("UNCHECKED_CAST")
      return (geometries as Collection<Geometry>).toTypedArray()
    }

    /**
     *  Converts the `List` to an array.
     */
    @JvmStatic
    fun toLinearRingArray(linearRings: Collection<*>): Array<LinearRing> {
      @Suppress("UNCHECKED_CAST")
      return (linearRings as Collection<LinearRing>).toTypedArray()
    }

    /**
     *  Converts the `List` to an array.
     */
    @JvmStatic
    fun toLineStringArray(lineStrings: Collection<*>): Array<LineString> {
      @Suppress("UNCHECKED_CAST")
      return (lineStrings as Collection<LineString>).toTypedArray()
    }

    /**
     *  Converts the `List` to an array.
     */
    @JvmStatic
    fun toPolygonArray(polygons: Collection<*>): Array<Polygon> {
      @Suppress("UNCHECKED_CAST")
      return (polygons as Collection<Polygon>).toTypedArray()
    }

    /**
     *  Converts the `List` to an array.
     */
    @JvmStatic
    fun toMultiPolygonArray(multiPolygons: Collection<*>): Array<MultiPolygon> {
      @Suppress("UNCHECKED_CAST")
      return (multiPolygons as Collection<MultiPolygon>).toTypedArray()
    }

    /**
     *  Converts the `List` to an array.
     */
    @JvmStatic
    fun toMultiLineStringArray(multiLineStrings: Collection<*>): Array<MultiLineString> {
      @Suppress("UNCHECKED_CAST")
      return (multiLineStrings as Collection<MultiLineString>).toTypedArray()
    }

    /**
     *  Converts the `List` to an array.
     */
    @JvmStatic
    fun toMultiPointArray(multiPoints: Collection<*>): Array<MultiPoint> {
      @Suppress("UNCHECKED_CAST")
      return (multiPoints as Collection<MultiPoint>).toTypedArray()
    }
  }
}
