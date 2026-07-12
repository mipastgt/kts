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
package org.locationtech.jts.io.geojson

/**
 * Constants for GeoJSON objects
 *
 * @author Martin Davis
 */
object GeoJsonConstants {

    const val NAME_GEOMETRY: String = "geometry"
    const val NAME_FEATURES: String = "features"
    const val NAME_GEOMETRIES: String = "geometries"
    const val NAME_CRS: String = "crs"
    const val NAME_PROPERTIES: String = "properties"
    const val NAME_NAME: String = "name"
    const val NAME_TYPE: String = "type"
    const val NAME_POINT: String = "Point"
    const val NAME_LINESTRING: String = "LineString"
    const val NAME_POLYGON: String = "Polygon"
    const val NAME_COORDINATES: String = "coordinates"
    const val NAME_GEOMETRYCOLLECTION: String = "GeometryCollection"
    const val NAME_MULTIPOLYGON: String = "MultiPolygon"
    const val NAME_MULTILINESTRING: String = "MultiLineString"
    const val NAME_MULTIPOINT: String = "MultiPoint"
    const val NAME_FEATURE: String = "Feature"
    const val NAME_FEATURECOLLECTION: String = "FeatureCollection"
}
