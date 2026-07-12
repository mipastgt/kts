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

/**
 * Models a collection of {@link Polygon}s.
 * <p>
 * As per the OGC SFS specification,
 * the Polygons in a MultiPolygon may not overlap,
 * and may only touch at single points.
 *
 * @version 1.7
 */
class MultiPolygon : GeometryCollection, Polygonal {

  /**
   *  Constructs a <code>MultiPolygon</code>.
   *
   * @deprecated Use GeometryFactory instead
   */
  constructor(polygons: Array<Polygon>?, precisionModel: PrecisionModel, SRID: Int) : this(polygons, GeometryFactory(precisionModel, SRID))

  /**
   * @param polygons
   *            the <code>Polygon</code>s for this <code>MultiPolygon</code>,
   *            or <code>null</code> or an empty array to create the empty
   *            geometry.
   */
  @Suppress("UNCHECKED_CAST")
  constructor(polygons: Array<Polygon>?, factory: GeometryFactory) : super(polygons as Array<Geometry>?, factory)

  override fun getDimension(): Int {
    return 2
  }

  override fun hasDimension(dim: Int): Boolean {
    return dim == 2
  }

  override fun getBoundaryDimension(): Int {
    return 1
  }

  override fun getGeometryType(): String {
    return Geometry.TYPENAME_MULTIPOLYGON
  }

  /*
  public boolean isSimple() {
    return true;
  }
*/

  /**
   * Computes the boundary of this geometry
   *
   * @return a lineal geometry (which may be empty)
   * @see Geometry#getBoundary
   */
  override fun getBoundary(): Geometry {
    if (isEmpty()) {
      return getFactory().createMultiLineString()
    }
    val allRings = ArrayList<LineString>()
    for (i in 0 until geometries.size) {
      val polygon = geometries[i] as Polygon
      val rings = polygon.getBoundary()
      for (j in 0 until rings.getNumGeometries()) {
        allRings.add(rings.getGeometryN(j) as LineString)
      }
    }
    return getFactory().createMultiLineString(allRings.toTypedArray())
  }

  override fun equalsExact(other: Geometry, tolerance: Double): Boolean {
    if (!isEquivalentClass(other)) {
      return false
    }
    return super.equalsExact(other, tolerance)
  }

  /**
   * Creates a {@link MultiPolygon} with
   * every component reversed.
   * The order of the components in the collection are not reversed.
   *
   * @return a MultiPolygon in the reverse order
   */
  override fun reverse(): MultiPolygon {
    return super.reverse() as MultiPolygon
  }

  override fun reverseInternal(): MultiPolygon {
    val polygons = Array(this.geometries.size) { i -> this.geometries[i].reverse() as Polygon }
    return MultiPolygon(polygons, factory)
  }

  override fun copyInternal(): MultiPolygon {
    val polygons = Array(this.geometries.size) { i -> this.geometries[i].copy() as Polygon }
    return MultiPolygon(polygons, factory)
  }

  override fun getTypeCode(): Int {
    return Geometry.TYPECODE_MULTIPOLYGON
  }

  companion object {
  }
}
