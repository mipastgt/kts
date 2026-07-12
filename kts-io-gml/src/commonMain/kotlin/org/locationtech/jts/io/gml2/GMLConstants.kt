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
package org.locationtech.jts.io.gml2

/**
 * Various constant strings associated with GML format.
 */
object GMLConstants {
    // Namespace constants
    const val GML_NAMESPACE: String = "http://www.opengis.net/gml"
    const val GML_PREFIX: String = "gml"

    // Source Coordinate System
    const val GML_ATTR_SRSNAME: String = "srsName"

    // GML associative types
    const val GML_GEOMETRY_MEMBER: String = "geometryMember"
    const val GML_POINT_MEMBER: String = "pointMember"
    const val GML_POLYGON_MEMBER: String = "polygonMember"
    const val GML_LINESTRING_MEMBER: String = "lineStringMember"
    const val GML_OUTER_BOUNDARY_IS: String = "outerBoundaryIs"
    const val GML_INNER_BOUNDARY_IS: String = "innerBoundaryIs"

    // Primitive Geometries
    const val GML_POINT: String = "Point"
    const val GML_LINESTRING: String = "LineString"
    const val GML_LINEARRING: String = "LinearRing"
    const val GML_POLYGON: String = "Polygon"
    const val GML_BOX: String = "Box"

    // Aggregate Geometries
    const val GML_MULTI_GEOMETRY: String = "MultiGeometry"
    const val GML_MULTI_POINT: String = "MultiPoint"
    const val GML_MULTI_LINESTRING: String = "MultiLineString"
    const val GML_MULTI_POLYGON: String = "MultiPolygon"

    // Coordinates
    const val GML_COORDINATES: String = "coordinates"
    const val GML_COORD: String = "coord"
    const val GML_COORD_X: String = "X"
    const val GML_COORD_Y: String = "Y"
    const val GML_COORD_Z: String = "Z"
}
