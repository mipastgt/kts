/*
 * Kotlin property-style accessors for the JTS geometry API.
 *
 * This is an additive ergonomics layer, not part of the faithful Java port: the ported types keep
 * upstream JTS's explicit Java getters (`getArea()`, `getCentroid()`, …). These extension properties
 * restore the idiom Kotlin callers get for free when using the *Java* JTS artifacts — where the Kotlin
 * compiler synthesizes `geom.area` from `getArea()` — so downstream Kotlin code reads the same against
 * this port. They are pure delegates to the underlying (virtual) getters; behaviour and polymorphism
 * are unchanged, and the Java API is untouched.
 *
 * Usage: import the members, e.g. `import org.locationtech.jts.geom.*`.
 *
 * Notes / deliberate omissions:
 *  - `Coordinate.x/y/z` are already public fields, so property access exists without an accessor here;
 *    only `Coordinate.m` (computed) gets one.
 *  - No `Geometry.envelope`: the name would denote two different types depending on context — inside a
 *    consumer subclass the protected `envelope: Envelope?` field shadows it, while an extension would
 *    return the bounding-box `Geometry`. Use `envelopeInternal` for the `Envelope`, or call
 *    `getEnvelope()` for the bounding-box geometry.
 *  - `factory` / `SRID` map to protected fields of the same value and type, so they resolve to the
 *    extension for ordinary callers and to the (identical) field inside a subclass — no surprise.
 */
package org.locationtech.jts.geom

// ---- Geometry (applies to every geometry subtype) --------------------------------------------------

val Geometry.geometryType: String get() = getGeometryType()
val Geometry.factory: GeometryFactory get() = getFactory()
val Geometry.userData: Any? get() = getUserData()
val Geometry.SRID: Int get() = getSRID()
val Geometry.numGeometries: Int get() = getNumGeometries()
val Geometry.precisionModel: PrecisionModel get() = getPrecisionModel()
val Geometry.coordinate: Coordinate? get() = getCoordinate()
val Geometry.coordinates: Array<Coordinate> get() = getCoordinates()
val Geometry.numPoints: Int get() = getNumPoints()
val Geometry.area: Double get() = getArea()
val Geometry.length: Double get() = getLength()
val Geometry.centroid: Point get() = getCentroid()
val Geometry.interiorPoint: Point get() = getInteriorPoint()
val Geometry.dimension: Int get() = getDimension()
val Geometry.boundary: Geometry get() = getBoundary()
val Geometry.boundaryDimension: Int get() = getBoundaryDimension()
val Geometry.envelopeInternal: Envelope get() = getEnvelopeInternal()
val Geometry.isEmpty: Boolean get() = isEmpty()
val Geometry.isSimple: Boolean get() = isSimple()
val Geometry.isValid: Boolean get() = isValid()
val Geometry.isRectangle: Boolean get() = isRectangle()

// ---- Point -----------------------------------------------------------------------------------------

val Point.x: Double get() = getX()
val Point.y: Double get() = getY()
val Point.coordinateSequence: CoordinateSequence get() = getCoordinateSequence()

// ---- LineString (and LinearRing) -------------------------------------------------------------------

val LineString.coordinateSequence: CoordinateSequence get() = getCoordinateSequence()
val LineString.startPoint: Point? get() = getStartPoint()
val LineString.endPoint: Point? get() = getEndPoint()
val LineString.isClosed: Boolean get() = isClosed()
val LineString.isRing: Boolean get() = isRing()

// ---- Polygon ---------------------------------------------------------------------------------------

val Polygon.exteriorRing: LinearRing get() = getExteriorRing()
val Polygon.numInteriorRing: Int get() = getNumInteriorRing()

// ---- MultiLineString -------------------------------------------------------------------------------

val MultiLineString.isClosed: Boolean get() = isClosed()

// ---- Coordinate ------------------------------------------------------------------------------------
// x / y / z are public fields already; m and isValid are computed.

val Coordinate.m: Double get() = getM()
val Coordinate.isValid: Boolean get() = isValid()

// ---- Envelope --------------------------------------------------------------------------------------

val Envelope.width: Double get() = getWidth()
val Envelope.height: Double get() = getHeight()
val Envelope.diameter: Double get() = getDiameter()
val Envelope.minX: Double get() = getMinX()
val Envelope.maxX: Double get() = getMaxX()
val Envelope.minY: Double get() = getMinY()
val Envelope.maxY: Double get() = getMaxY()
val Envelope.area: Double get() = getArea()
val Envelope.isNull: Boolean get() = isNull()

// ---- LineSegment -----------------------------------------------------------------------------------

val LineSegment.length: Double get() = getLength()
val LineSegment.isHorizontal: Boolean get() = isHorizontal()
val LineSegment.isVertical: Boolean get() = isVertical()

// ---- PrecisionModel --------------------------------------------------------------------------------

val PrecisionModel.scale: Double get() = getScale()
val PrecisionModel.type: PrecisionModel.Type get() = getType()
val PrecisionModel.maximumSignificantDigits: Int get() = getMaximumSignificantDigits()
val PrecisionModel.offsetX: Double get() = getOffsetX()
val PrecisionModel.offsetY: Double get() = getOffsetY()
val PrecisionModel.isFloating: Boolean get() = isFloating()

// ---- Triangle --------------------------------------------------------------------------------------

val Triangle.isAcute: Boolean get() = isAcute()
val Triangle.isCCW: Boolean get() = isCCW()
